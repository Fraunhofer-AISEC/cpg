/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *                    $$$$$$\  $$$$$$$\   $$$$$$\
 *                   $$  __$$\ $$  __$$\ $$  __$$\
 *                   $$ /  \__|$$ |  $$ |$$ /  \__|
 *                   $$ |      $$$$$$$  |$$ |$$$$\
 *                   $$ |      $$  ____/ $$ |\_$$ |
 *                   $$ |  $$\ $$ |      $$ |  $$ |
 *                   \$$$$$   |$$ |      \$$$$$   |
 *                    \______/ \__|       \______/
 *
 */
package de.fraunhofer.aisec.cpg.ai

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.*
import ai.koog.agents.core.environment.ReceivedToolResult
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.agents.mcp.metadata.McpServerInfo
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.streaming.StreamFrame
import ai.koog.prompt.streaming.toMessageResponse
import ai.koog.serialization.kotlinx.toKotlinxJsonElement
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import de.fraunhofer.aisec.cpg.ai.clients.*
import de.fraunhofer.aisec.cpg.ai.skills.SkillLoader
import de.fraunhofer.aisec.cpg.ai.skills.buildActivateSkillToolRegistry
import de.fraunhofer.aisec.cpg.ai.skills.buildSkillCatalog
import de.fraunhofer.aisec.cpg.ai.skills.defaultSkillDirectories
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.sse.*
import io.ktor.serialization.kotlinx.json.*
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.ClientOptions
import io.modelcontextprotocol.kotlin.sdk.client.StreamableHttpClientTransport
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory

/** ChatService manages LLM client configuration and provides an API for chat interactions. */
class ChatService(
    private val httpClient: HttpClient,
    private val llmProviderConfig: LlmProviderConfig,
    private val mcpServerUrl: String,
    /** Maximum number of tool-calling round trips the agent may take before it must respond. */
    private val maxAgentIterations: Int = 100,
) {
    suspend fun listAvailableProviders(): List<LlmProviderWithModels> =
        llmProviderConfig.listAvailableProviders()

    /**
     * Raw MCP client used for capability discovery (prompts/resources/tool schemas) and the direct,
     * non-agentic [getPrompt]/[callTool] endpoints. Koog's MCP integration
     * ([McpToolRegistryProvider]) only models MCP *tools*, so it can't replace this for
     * prompts/resources listing. The LLM-driven tool-calling loop in [chat], however, goes through
     * [mcpToolRegistry] below, which wraps this very same connected client.
     */
    private val mcp: Client =
        Client(
            clientInfo = Implementation(name = "codyze-client", version = "1.0.0"),
            options = ClientOptions(),
        )

    private var tools: List<Tool> = emptyList()
    private var prompts: List<Prompt> = emptyList()
    private var resources: List<Resource> = emptyList()

    /** The MCP tools, wrapped as a Koog [ToolRegistry] for use by the agent in [chat]. */
    private var mcpToolRegistry: ToolRegistry = ToolRegistry.EMPTY

    /** Connect to the MCP server via streamable HTTP. */
    suspend fun connect() {
        val transport = StreamableHttpClientTransport(url = mcpServerUrl, client = httpClient)
        mcp.connect(transport)
        tools = mcp.listTools().tools
        prompts = mcp.listPrompts().prompts
        resources = mcp.listResources().resources
        mcpToolRegistry =
            McpToolRegistryProvider.fromClient(
                mcpClient = mcp,
                serverInfo = McpServerInfo(url = mcpServerUrl),
            )
    }

    private val skillLoader = SkillLoader(defaultSkillDirectories)
    private var skills: List<Skill> = skillLoader.discoverSkills()

    /**
     * Once the running prompt grows beyond this many messages, [chatStrategy] compresses the
     * history before continuing the tool-calling loop. With up to [maxAgentIterations] round trips
     * per [chat] call, a single oversized tool result (or many moderate ones accumulating over
     * iterations) can otherwise grow the prompt past the LLM provider's context window and cause a
     * 400 error.
     */
    private val historyCompressionThreshold = 60

    /**
     * Number of most recent messages kept when [chatStrategy] compresses the history: everything
     * older is summarized away, per [HistoryCompressionStrategy.FromLastNMessages].
     */
    private val historyCompressionKeepLastN = 30

    /**
     * Maximum size, in characters, of a single tool result's text content admitted into the
     * LLM-facing conversation (see the `truncateToolResults` node in [chatStrategy]). A single MCP
     * tool call can return an unbounded amount of data (e.g. `cpg_list_functions` without narrow
     * filters); left uncapped, one such result can alone exceed the LLM provider's context window
     * and cause a hard 400 error - before [historyCompressionThreshold] ever triggers, since that
     * gates on message *count*, not the size of an individual message. This cap only affects what
     * the LLM sees: the frontend always receives the full, untruncated result via the `tool_result`
     * SSE event, which is emitted from within tool execution itself - i.e. strictly before
     * [chatStrategy]'s `truncateToolResults` node ever runs.
     */
    private val maxToolResultChars = 20_000

    /**
     * Caps a tool result's textual content to [maxToolResultChars], appending a truncation marker.
     * Both [ReceivedToolResult.output] and any [MessagePart.Text] parts are capped: depending on
     * the tool, either (or both) may be what actually reaches the LLM's prompt, since
     * [ReceivedToolResult.toMessagePart] prefers `parts` and only falls back to wrapping [output]
     * when `parts` is null.
     */
    private fun ReceivedToolResult.truncatedForLlm(): ReceivedToolResult {
        fun truncate(text: String): String =
            if (text.length <= maxToolResultChars) text
            else "${text.take(maxToolResultChars)}...[truncated, ${text.length} chars total]"

        return copy(
            output = truncate(output),
            parts =
                parts?.map { part ->
                    if (part is MessagePart.Text) part.copy(text = truncate(part.text)) else part
                },
        )
    }

    /**
     * The agent's tool-calling loop: request the LLM, and if it calls tool(s), execute them and
     * send the results back, repeating until the LLM responds with text. This mirrors Koog's
     * built-in single-run strategy shape, extended with:
     * - streaming LLM request/send-tool-result nodes ([nodeLLMRequestStreaming],
     *   [nodeLLMSendToolResultsStreaming]) so [chat]'s `onLLMStreamingFrameReceived` handler
     *   actually receives frames - the plain (non-streaming) node variants never call the streaming
     *   client path at all, so that handler would otherwise never fire and no text would ever reach
     *   the frontend. Each is paired with a small collector node
     *   (`collectRequestLlmStream`/`collectSendToolResultStream`) that only *drains* the frame
     *   [Flow] (`onLLMStreamingFrameReceived` fires as a side effect of collection, driven by
     *   Koog's `ContextualPromptExecutor` - the collector must not re-emit frames itself, or every
     *   token would reach the frontend twice) and reduces it back to a [Message.Assistant] via
     *   [toMessageResponse], so every downstream edge below is unchanged from the non-streaming
     *   version.
     * - a `truncateToolResults` node that caps any oversized tool result (see [maxToolResultChars])
     *   before it can reach the LLM-facing prompt, running right after tool execution (and thus
     *   after the tool-call-completed event, which still carries the full, untruncated result to
     *   the frontend) and before both downstream edges.
     * - a history-compression node that fires once the prompt exceeds [historyCompressionThreshold]
     *   messages (see class docs above), so long tool-calling loops don't blow the LLM's context
     *   window either.
     *
     * Built once and reused across [chat] calls, since the graph itself carries no per-request
     * state.
     */
    private val chatStrategy =
        strategy<String, String>("chat-with-history-compression") {
            val requestLlmStream by nodeLLMRequestStreaming()
            val requestLlm by
                node<Flow<StreamFrame>, Message.Assistant>("collectRequestLlmStream") { frames ->
                    frames.toList().toMessageResponse()
                }
            val executeTool by nodeExecuteTools()
            val truncateToolResults by
                node<ReceivedToolResults, ReceivedToolResults>("truncateToolResults") { received ->
                    ReceivedToolResults(received.toolResults.map { it.truncatedForLlm() })
                }
            val sendToolResultStream by nodeLLMSendToolResultsStreaming()
            val sendToolResult by
                node<Flow<StreamFrame>, Message.Assistant>("collectSendToolResultStream") { frames
                    ->
                    frames.toList().toMessageResponse()
                }
            val compressionStrategy =
                HistoryCompressionStrategy.FromLastNMessages(historyCompressionKeepLastN)
            val compressHistory by
                nodeLLMCompressHistory<ReceivedToolResults>(strategy = compressionStrategy)

            edge(nodeStart forwardTo requestLlmStream)
            edge(requestLlmStream forwardTo requestLlm)
            // onToolCalls is checked before onTextMessage (matching Koog's own singleRunStrategy
            // convention): some providers' streaming responses include a harmless empty text part
            // (e.g. vLLM sends an explicit `content: ""` on the role-establishing and
            // finish-reason chunks) alongside a real tool call in the same reconstructed message.
            // Since these edge predicates aren't mutually exclusive (onTextMessage just checks
            // "any Text part present", regardless of tool calls), checking onTextMessage first
            // would let that empty text part win the race and silently skip the tool call.
            edge(requestLlm forwardTo executeTool onToolCalls { true })
            edge(requestLlm forwardTo nodeFinish onTextMessage { true })
            edge(executeTool forwardTo truncateToolResults)
            // If the history has grown too large, compress it before sending the tool result.
            edge(
                truncateToolResults forwardTo
                    compressHistory onCondition
                    { _ ->
                        llm.readSession { prompt.messages.size > historyCompressionThreshold }
                    }
            )
            edge(compressHistory forwardTo sendToolResultStream)
            // Otherwise, send the tool result directly.
            edge(
                truncateToolResults forwardTo
                    sendToolResultStream onCondition
                    { _ ->
                        llm.readSession { prompt.messages.size <= historyCompressionThreshold }
                    }
            )
            edge(sendToolResultStream forwardTo sendToolResult)
            edge(sendToolResult forwardTo executeTool onToolCalls { true })
            edge(sendToolResult forwardTo nodeFinish onTextMessage { true })
        }

    /** Return the discovered skills. */
    fun getSkills(): List<Skill> = skills

    /** Process a chat query using the LLM with MCP tool support */
    fun chat(request: ChatRequestJSON): Flow<String> = channelFlow {
        // Used if the LLM needs more time for a "cold-start"
        send(Events.keepalive())

        val userMessage = request.messages.lastOrNull()?.content ?: ""
        val priorMessages = request.messages.dropLast(1)

        val chatLlm =
            llmProviderConfig.clientFor(request.client, request.model)
                ?: run {
                    send(Events.text("Unknown or unavailable LLM client"))
                    return@channelFlow
                }

        try {
            // Re-derive the full conversation as the agent's initial history: the frontend sends
            // the
            // complete message list on every request (ChatService itself is stateless across
            // calls),
            // so a fresh AIAgent/prompt is built per request, mirroring the old per-request
            // LlmClient.
            val history =
                prompt(id = "chat-history") {
                    system(buildSystemPrompt(skills))
                    priorMessages.forEach { msg ->
                        if (msg.content.isNotBlank()) {
                            if (msg.role == "assistant") assistant(msg.content)
                            else user(msg.content)
                        }
                    }
                }

            val toolRegistry = mcpToolRegistry + buildActivateSkillToolRegistry(skills)

            val agent =
                AIAgent(
                    promptExecutor = chatLlm.executor,
                    agentConfig =
                        AIAgentConfig(
                            prompt = history,
                            model = chatLlm.model,
                            maxAgentIterations = maxAgentIterations,
                        ),
                    strategy = chatStrategy,
                    toolRegistry = toolRegistry,
                ) {
                    handleEvents {
                        onLLMStreamingFrameReceived { ctx ->
                            when (val frame = ctx.streamFrame) {
                                is StreamFrame.TextDelta -> send(Events.text(frame.text))
                                is StreamFrame.ReasoningDelta ->
                                    frame.text?.let { send(Events.reasoning(it)) }
                                else -> {}
                            }
                        }
                        onToolCallCompleted { ctx ->
                            val content = ctx.toolResult?.toKotlinxJsonElement() ?: JsonNull
                            send(Events.toolResult(ctx.toolName, content))
                        }
                        onToolCallFailed { ctx -> send(Events.text("Tool failed: ${ctx.message}")) }
                    }
                }

            // The final text is already streamed out via onLLMStreamingFrameReceived above; the
            // agent's return value only matters if the run finishes without ever streaming (e.g. an
            // immediate tool-only response), so we don't need to re-emit it here.
            agent.run(userMessage)
        } catch (e: Exception) {
            log.error("Chat error: {}", e.message, e)
            send(Events.text("Error: ${e.message}"))
        }
    }

    /**
     * Compose the system prompt sent to the LLM: the base prompt followed by the skill catalog when
     * skills are available.
     */
    private fun buildSystemPrompt(skills: List<Skill>): String {
        val catalog = buildSkillCatalog(skills) ?: return SYSTEM_PROMPT
        return "$SYSTEM_PROMPT\n\n$catalog"
    }

    /** Return the MCP capabilities: tools, prompts, and resources. */
    fun getMcpCapabilities(): McpCapabilitiesJSON =
        McpCapabilitiesJSON(
            serverName = mcp.serverVersion?.name ?: "MCP Server",
            serverVersion = mcp.serverVersion?.version ?: "",
            tools =
                tools.map { tool ->
                    McpToolJSON(
                        name = tool.name,
                        description = tool.description,
                        inputSchema =
                            ToolSchemaJSON(
                                properties = tool.inputSchema.properties,
                                required = tool.inputSchema.required,
                            ),
                    )
                },
            prompts =
                prompts.map { prompt ->
                    McpPromptJSON(
                        name = prompt.name,
                        description = prompt.description,
                        arguments =
                            prompt.arguments?.map { arg ->
                                PromptArgumentJSON(
                                    name = arg.name,
                                    description = arg.description,
                                    required = arg.required,
                                )
                            },
                    )
                },
            resources =
                resources.map { resource ->
                    McpResourceJSON(
                        uri = resource.uri,
                        name = resource.name,
                        description = resource.description,
                        mimeType = resource.mimeType,
                    )
                },
        )

    /** Resolve an MCP prompt and return its messages as [ChatMessageJSON]. */
    suspend fun getPrompt(
        name: String,
        arguments: Map<String, String> = emptyMap(),
    ): List<ChatMessageJSON> {
        val result =
            mcp.getPrompt(
                GetPromptRequest(
                    GetPromptRequestParams(name = name, arguments = arguments.ifEmpty { null })
                )
            )
        return result.messages.map { msg ->
            ChatMessageJSON(
                role = if (msg.role == Role.User) "user" else "assistant",
                content = (msg.content as? TextContent)?.text ?: "",
            )
        }
    }

    /**
     * Parse a list of text content items from an MCP tool result into a [JsonElement]. JSON strings
     * are parsed into their structured form; plain text is wrapped as [JsonPrimitive]. A single
     * item is returned directly; multiple items are wrapped in a [JsonArray].
     */
    fun parseToolResultContent(contentTexts: List<String>): JsonElement {
        if (contentTexts.isEmpty()) {
            return JsonArray(emptyList())
        }
        val parsedItems =
            contentTexts.map { text ->
                try {
                    Json.parseToJsonElement(text)
                } catch (_: Exception) {
                    JsonPrimitive(text)
                }
            }
        return if (parsedItems.size == 1) parsedItems[0] else JsonArray(parsedItems)
    }

    /** Call an MCP tool directly and return the result as a parsed JSON element. */
    suspend fun callTool(name: String, arguments: JsonObject): JsonElement {
        val result = mcp.callTool(name = name, arguments = arguments)
        val contentTexts = result.content.mapNotNull { (it as? TextContent)?.text }
        return parseToolResultContent(contentTexts)
    }

    fun close() {
        httpClient.close()
    }

    companion object {
        private val log = LoggerFactory.getLogger(ChatService::class.java)

        fun createIfConfigExist(): ChatService? {
            val config = ConfigFactory.load()
            if (!config.hasPath("llm.clients")) {
                log.warn(
                    "No application.conf found, AI chat features disabled. " +
                        "Copy application.conf.example to application.conf to enable them."
                )
                return null
            }
            return fromConfig(config)
        }

        private fun fromConfig(config: Config): ChatService {
            val mcpServerUrl = config.getString("mcp.serverUrl")

            val httpClient =
                HttpClient(CIO) {
                    install(ContentNegotiation) {
                        json(
                            Json {
                                ignoreUnknownKeys = true
                                isLenient = true
                            }
                        )
                    }
                    install(SSE)
                    install(HttpTimeout) {
                        requestTimeoutMillis = 600_000
                        connectTimeoutMillis = 30_000
                        socketTimeoutMillis = 600_000
                    }
                }

            return ChatService(
                httpClient = httpClient,
                llmProviderConfig = config.toLlmProviderConfig(httpClient),
                mcpServerUrl = mcpServerUrl,
            )
        }
    }
}
