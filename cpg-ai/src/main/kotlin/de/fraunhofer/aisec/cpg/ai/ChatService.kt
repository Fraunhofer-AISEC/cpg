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
import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.streaming.StreamFrame
import ai.koog.prompt.streaming.toMessageResponse
import ai.koog.serialization.kotlinx.toKotlinxJsonElement
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import de.fraunhofer.aisec.cpg.ai.clients.*
import de.fraunhofer.aisec.cpg.ai.skills.ACTIVATE_SKILL_TOOL_NAME
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
     * Number of most recent messages kept verbatim when [chatStrategy] compresses the history;
     * everything older falls to the [HistoryCompressionStrategy.FromLastNMessages] fallback in
     * [historyCompressionConcepts] once its own dedicated per-concept fact extraction is done.
     */
    private val historyCompressionKeepLastN = 30

    /**
     * Concepts [chatStrategy]'s [HistoryCompressionStrategy.FactRetrieval] compression extracts as
     * explicit facts (one dedicated LLM call per concept, over the full history-so-far) before
     * falling back to [HistoryCompressionStrategy.FromLastNMessages] for anything not captured by
     * these - so specific, decision-relevant progress survives compression as structured facts
     * instead of depending on how much of it a single generic prose summary happens to retain.
     * Deliberately skill-agnostic (tag-library vs. match-library) since [chatStrategy] is built
     * once and shared across every [chat] call.
     */
    private val historyCompressionConcepts =
        listOf(
            Concept(
                keyword = "CompletedWork",
                description =
                    "Functions or concepts/operations already tagged (tag-library) or matched to " +
                        "a substitute (match-library) so far, with their outcome/status and any " +
                        "noted properties or prerequisites.",
                factType = FactType.MULTIPLE,
            ),
            Concept(
                keyword = "OpenIssues",
                description =
                    "Functions, concepts, or operations noted as ambiguous, unmatched, or blocked " +
                        "so far, and why - so they aren't silently dropped from the eventual " +
                        "summary.",
                factType = FactType.MULTIPLE,
            ),
            Concept(
                keyword = "ExploredCpgEntities",
                description =
                    "Functions, records, or files already looked up via CPG tools so far, and a " +
                        "brief note of what was found, to avoid redundant re-querying.",
                factType = FactType.MULTIPLE,
            ),
        )

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
     * Nudge sent to the model when it replies with plain text instead of calling a tool, to
     * distinguish "narrating the next step" from a genuine final answer (see the `buildNudge` node
     * in [chatStrategy]).
     */
    private val continueNudgeMessage =
        "If your task is not yet complete, call the appropriate tool now instead of describing " +
            "what you would do. If you are done, just confirm that no further tool calls are " +
            "needed."

    /**
     * Caps a tool result's textual content to [maxToolResultChars], keeping both the head and the
     * tail rather than chopping off everything past the cap: for most of these tools the result is
     * a JSON array/object, where the head carries the first, often most-relevant items and the tail
     * carries the closing structure and last items - a head-only cut silently drops the latter
     * every time. Both [ReceivedToolResult.output] and any [MessagePart.Text] parts are capped:
     * depending on the tool, either (or both) may be what actually reaches the LLM's prompt, since
     * [ReceivedToolResult.toMessagePart] prefers `parts` and only falls back to wrapping [output]
     * when `parts` is null.
     */
    private fun ReceivedToolResult.truncatedForLlm(): ReceivedToolResult {
        fun truncate(text: String): String {
            if (text.length <= maxToolResultChars) return text
            val headChars = maxToolResultChars * 2 / 3
            val tailChars = maxToolResultChars - headChars
            val droppedChars = text.length - headChars - tailChars
            return "${text.take(headChars)}\n...[$droppedChars chars truncated out of " +
                "${text.length} total]...\n${text.takeLast(tailChars)}"
        }

        return copy(
            output = truncate(output),
            parts =
                parts?.map { part ->
                    if (part is MessagePart.Text) part.copy(text = truncate(part.text)) else part
                },
        )
    }

    /** Logs token usage reported by the LLM provider for one response, if any was reported. */
    private fun logTokenUsage(message: Message.Assistant) {
        val usage = message.metaInfo
        log.info(
            "LLM usage: model={} input={} output={} total={}",
            usage.modelId,
            usage.inputTokensCount,
            usage.outputTokensCount,
            usage.totalTokensCount,
        )
    }

    /**
     * Extracts a tool call from [text] when the model attempted one via free-form text instead of a
     * real structured `tool_calls` response - e.g. because the LLM provider's tool-call parser
     * doesn't recognize this particular model's native tool-call format (observed with some
     * self-hosted models/servers). Rather than special-casing any one model's native syntax (which
     * varies across model families and even across attempts by the same model), this looks for a
     * JSON object - optionally inside a fenced code block or a `<tool_call>` tag, both common
     * conventions - with a name-like key matching one of [validToolNames] and an arguments-like
     * key. Returns null if no such match is found, which is the common case (real tool calls and
     * genuine final answers never match).
     */
    private fun extractFallbackToolCall(
        text: String,
        validToolNames: Set<String>,
    ): MessagePart.Tool.Call? {
        val candidates = buildList {
            FENCED_CODE_BLOCK_REGEX.findAll(text).forEach { add(it.groupValues[1]) }
            TOOL_CALL_TAG_REGEX.findAll(text).forEach { add(it.groupValues[1]) }
            add(text)
        }

        for (candidate in candidates) {
            for (jsonText in findJsonObjects(candidate)) {
                val obj =
                    runCatching { Json.parseToJsonElement(jsonText).jsonObject }.getOrNull()
                        ?: continue
                val name =
                    NAME_KEYS.firstNotNullOfOrNull { key -> obj[key]?.jsonPrimitive?.contentOrNull }
                if (name == null || name !in validToolNames) continue
                val args = ARGUMENT_KEYS.firstNotNullOfOrNull { key -> obj[key]?.jsonObject }
                return MessagePart.Tool.Call(tool = name, args = args ?: JsonObject(emptyMap()))
            }
        }
        return null
    }

    /** Finds all top-level, brace-balanced `{...}` substrings in [text]. */
    private fun findJsonObjects(text: String): List<String> {
        val results = mutableListOf<String>()
        var depth = 0
        var start = -1
        for ((i, c) in text.withIndex()) {
            when (c) {
                '{' -> {
                    if (depth == 0) start = i
                    depth++
                }
                '}' -> {
                    if (depth > 0) {
                        depth--
                        if (depth == 0 && start >= 0) {
                            results.add(text.substring(start, i + 1))
                            start = -1
                        }
                    }
                }
            }
        }
        return results
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
                    frames.toList().toMessageResponse().also { logTokenUsage(it) }
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
                    frames.toList().toMessageResponse().also { logTokenUsage(it) }
                }
            val compressionStrategy =
                FactRetrievalHistoryCompressionStrategy(
                    concepts = historyCompressionConcepts,
                    fallback =
                        HistoryCompressionStrategy.FromLastNMessages(historyCompressionKeepLastN),
                )
            val compressHistory by
                nodeLLMCompressHistory<ReceivedToolResults>(strategy = compressionStrategy)
            // Some models (esp. smaller/local ones) narrate their next step in plain text instead
            // of calling a tool in the same turn (e.g. "Let me check what functions are
            // available:" with no accompanying tool call). Rather than accepting that prose as the
            // final answer, give the model one nudge to actually continue; only if it replies with
            // text *again* do we treat it as truly final. This costs one extra round trip on every
            // genuinely-final answer too, but avoids silently truncating still-in-progress work.
            val buildNudge by node<String, String>("buildNudge") { _ -> continueNudgeMessage }
            // onToolCalls/onTextMessage below only match a response with tool calls or a text
            // part; a response with neither (parts=[], finishReason=stop - seen from some
            // providers) would otherwise leave Koog unable to route the message anywhere, throwing
            // AIAgentStuckInTheNodeException. These two transform nodes let such an empty response
            // join the existing buildNudge/nodeFinish paths, which both expect a String input.
            val emptyResponseToNudge by
                node<Message.Assistant, String>("emptyResponseToNudge") { _ -> "" }
            val emptyResponseToFinish by
                node<Message.Assistant, String>("emptyResponseToFinish") { _ -> "" }
            val nudgeRequestStream by nodeLLMRequestStreaming("nudgeRequestStream")
            val nudgeRequest by
                node<Flow<StreamFrame>, Message.Assistant>("collectNudgeRequestStream") { frames ->
                    frames.toList().toMessageResponse().also { logTokenUsage(it) }
                }

            // Some models attempt a tool call as free-form text instead of a real structured
            // tool_calls response (see extractFallbackToolCall doc). Both "first attempt" and
            // "after the nudge" responses get this same check before falling through to the
            // nudge/finish behavior above, since a text-only reply can happen at either point.
            fun validToolNames() = tools.map { it.name }.toSet() + ACTIVATE_SKILL_TOOL_NAME

            val detectFallbackToolCall by
                node<String, Pair<String, MessagePart.Tool.Call?>>("detectFallbackToolCall") { text
                    ->
                    text to extractFallbackToolCall(text, validToolNames())
                }
            val fallbackToolCallDetected by
                node<Pair<String, MessagePart.Tool.Call?>, ToolCalls>("fallbackToolCallDetected") {
                    (_, call) ->
                    ToolCalls(listOf(requireNotNull(call)))
                }
            val fallbackNoToolCallDetected by
                node<Pair<String, MessagePart.Tool.Call?>, String>("fallbackNoToolCallDetected") {
                    (text, _) ->
                    text
                }

            val detectFallbackToolCallAfterNudge by
                node<String, Pair<String, MessagePart.Tool.Call?>>(
                    "detectFallbackToolCallAfterNudge"
                ) { text ->
                    text to extractFallbackToolCall(text, validToolNames())
                }
            val fallbackToolCallDetectedAfterNudge by
                node<Pair<String, MessagePart.Tool.Call?>, ToolCalls>(
                    "fallbackToolCallDetectedAfterNudge"
                ) { (_, call) ->
                    ToolCalls(listOf(requireNotNull(call)))
                }
            val fallbackNoToolCallDetectedAfterNudge by
                node<Pair<String, MessagePart.Tool.Call?>, String>(
                    "fallbackNoToolCallDetectedAfterNudge"
                ) { (text, _) ->
                    text
                }

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
            edge(requestLlm forwardTo detectFallbackToolCall onTextMessage { true })
            // See emptyResponseToNudge/emptyResponseToFinish docs above: treat a genuinely empty
            // response the same as unhelpful text - nudge the model to actually continue.
            edge(
                requestLlm forwardTo
                    emptyResponseToNudge onCondition
                    { message ->
                        message.parts.isEmpty()
                    }
            )
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
            edge(sendToolResult forwardTo detectFallbackToolCall onTextMessage { true })
            // Same empty-response fallback as after requestLlm above.
            edge(
                sendToolResult forwardTo
                    emptyResponseToNudge onCondition
                    { message ->
                        message.parts.isEmpty()
                    }
            )

            edge(
                detectFallbackToolCall forwardTo
                    fallbackToolCallDetected onCondition
                    { (_, call) ->
                        call != null
                    }
            )
            edge(fallbackToolCallDetected forwardTo executeTool)
            edge(
                detectFallbackToolCall forwardTo
                    fallbackNoToolCallDetected onCondition
                    { (_, call) ->
                        call == null
                    }
            )
            edge(fallbackNoToolCallDetected forwardTo buildNudge)
            edge(emptyResponseToNudge forwardTo buildNudge)

            edge(buildNudge forwardTo nudgeRequestStream)
            edge(nudgeRequestStream forwardTo nudgeRequest)
            edge(nudgeRequest forwardTo executeTool onToolCalls { true })
            edge(nudgeRequest forwardTo detectFallbackToolCallAfterNudge onTextMessage { true })
            // Same empty-response case as above, but this is already the nudge response - there's
            // no further nudge to give, so end the turn, matching how unhelpful text after the
            // nudge already ends at nodeFinish below.
            edge(
                nudgeRequest forwardTo
                    emptyResponseToFinish onCondition
                    { message ->
                        message.parts.isEmpty()
                    }
            )

            edge(
                detectFallbackToolCallAfterNudge forwardTo
                    fallbackToolCallDetectedAfterNudge onCondition
                    { (_, call) ->
                        call != null
                    }
            )
            edge(fallbackToolCallDetectedAfterNudge forwardTo executeTool)
            edge(
                detectFallbackToolCallAfterNudge forwardTo
                    fallbackNoToolCallDetectedAfterNudge onCondition
                    { (_, call) ->
                        call == null
                    }
            )
            edge(fallbackNoToolCallDetectedAfterNudge forwardTo nodeFinish)
            edge(emptyResponseToFinish forwardTo nodeFinish)
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
                prompt(
                    id = "chat-history",
                    params = LLMParams(toolChoice = LLMParams.ToolChoice.Auto),
                ) {
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
                            val args = ctx.toolArgs.toKotlinxJsonElement()
                            val content = ctx.toolResult?.toKotlinxJsonElement() ?: JsonNull
                            send(Events.toolResult(ctx.toolName, args, content))
                        }
                        onToolCallFailed { ctx -> send(Events.text("Tool failed: ${ctx.message}")) }
                        onAgentCompleted { ctx ->
                            val assistantMessages =
                                ctx.context.llm
                                    .readSession { prompt.messages }
                                    .filterIsInstance<Message.Assistant>()
                            var inputTokens = 0
                            var outputTokens = 0
                            var totalTokens = 0
                            var modelId: String? = null
                            for (message in assistantMessages) {
                                val usage = message.metaInfo
                                inputTokens += usage.inputTokensCount ?: 0
                                outputTokens += usage.outputTokensCount ?: 0
                                totalTokens += usage.totalTokensCount ?: 0
                                usage.modelId?.let { modelId = it }
                            }
                            send(Events.usage(modelId, inputTokens, outputTokens, totalTokens))
                        }
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

        /** Matches fenced code blocks, e.g. ` ```json ... ``` ` (see [extractFallbackToolCall]). */
        private val FENCED_CODE_BLOCK_REGEX = Regex("```(?:\\w+)?\\s*([\\s\\S]*?)```")

        /**
         * Matches the common `<tool_call>...</tool_call>` convention (see
         * [extractFallbackToolCall]).
         */
        private val TOOL_CALL_TAG_REGEX =
            Regex("<tool_call>([\\s\\S]*?)</tool_call>", RegexOption.IGNORE_CASE)

        /** Candidate JSON keys for a tool's name, in order of preference. */
        private val NAME_KEYS = listOf("name", "tool", "tool_name", "function")

        /** Candidate JSON keys for a tool's arguments, in order of preference. */
        private val ARGUMENT_KEYS = listOf("arguments", "parameters", "input")

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
