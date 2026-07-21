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

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import de.fraunhofer.aisec.cpg.ai.clients.*
import de.fraunhofer.aisec.cpg.ai.skills.ACTIVATE_SKILL_TOOL_NAME
import de.fraunhofer.aisec.cpg.ai.skills.SkillLoader
import de.fraunhofer.aisec.cpg.ai.skills.buildActivateSkillTool
import de.fraunhofer.aisec.cpg.ai.skills.buildSkillCatalog
import de.fraunhofer.aisec.cpg.ai.skills.defaultSkillDirectories
import de.fraunhofer.aisec.cpg.ai.skills.wrapActivatedSkill
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
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory

/** ChatService manages LLM client configuration and provides an API for chat interactions. */
class ChatService(
    private val httpClient: HttpClient,
    private val llmProviderConfig: LlmProviderConfig,
    private val mcpServerUrl: String,
) {
    suspend fun listAvailableProviders(): List<LlmProviderWithModels> =
        llmProviderConfig.listAvailableProviders()

    private val mcp: Client =
        Client(
            clientInfo = Implementation(name = "codyze-client", version = "1.0.0"),
            options = ClientOptions(),
        )

    private var tools: List<Tool> = emptyList()
    private var prompts: List<Prompt> = emptyList()
    private var resources: List<Resource> = emptyList()

    /** Connect to the MCP server via streamable HTTP. */
    suspend fun connect() {
        val transport = StreamableHttpClientTransport(url = mcpServerUrl, client = httpClient)
        mcp.connect(transport)
        tools = mcp.listTools().tools
        prompts = mcp.listPrompts().prompts
        resources = mcp.listResources().resources
    }

    private val skillLoader = SkillLoader(defaultSkillDirectories)
    private var skills: List<Skill> = skillLoader.discoverSkills()

    /** Maximum number of tool call iterations before responding a text message. */
    private val maxToolIterations = 50

    /**
     * Maximum size, in characters, of a tool result that is kept in the LLM-facing conversation
     * history (see [truncateForLlm]).
     */
    private val maxToolResultChars = 20000

    /** Return the discovered skills. */
    fun getSkills(): List<Skill> = skills

    /** Process a chat query using the LLM with MCP tool support */
    fun chat(request: ChatRequestJSON): Flow<String> = channelFlow {
        // Used if the LLM needs more time for a "cold-start"
        send(Events.keepalive())

        val userMessage = request.messages.lastOrNull()?.content ?: ""
        val conversationHistory = request.messages

        val llm =
            llmProviderConfig.clientFor(request.client, request.model)
                ?: run {
                    send(Events.text("Unknown or unavailable LLM client"))
                    return@channelFlow
                }

        try {
            val toolCallHistory = mutableListOf<List<ToolCallWithResult>>()
            var iteration = 0

            val allTools = tools + listOfNotNull(buildActivateSkillTool(skills))
            val systemPrompt = buildSystemPrompt(skills)

            var toolCalls =
                llm.sendPrompt(
                    userMessage = userMessage,
                    systemPrompt = systemPrompt,
                    conversationHistory = conversationHistory,
                    tools = allTools,
                    onText = { text -> send(Events.text(text)) },
                    onReasoning = { thought -> send(Events.reasoning(thought)) },
                )

            while (toolCalls.isNotEmpty() && iteration < maxToolIterations) {
                iteration++
                val roundtripResults =
                    toolCalls.map { toolCall ->
                        val result = executeToolCall(toolCall) { jsonEvent -> send(jsonEvent) }
                        ToolCallWithResult(toolCall, result)
                    }
                toolCallHistory.add(roundtripResults)

                toolCalls =
                    llm.sendPrompt(
                        userMessage = userMessage,
                        systemPrompt = systemPrompt,
                        conversationHistory = conversationHistory,
                        toolCallHistory = toolCallHistory,
                        tools = allTools,
                        onText = { text -> send(Events.text(text)) },
                        onReasoning = { thought -> send(Events.reasoning(thought)) },
                    )
            }
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

    /**
     * Caps [text] so it is safe to store in the LLM-facing `toolCallHistory` built up in [chat].
     * That history is resent to the LLM **in full, verbatim, on every subsequent iteration** of
     * the tool-calling loop, so a single oversized tool result (or several moderate ones
     * accumulating over iterations) could otherwise blow the LLM context window and cause a 400
     * error from the provider. This only bounds what is sent back to the LLM: the full,
     * untruncated content has already been emitted to the frontend separately (via `emit`) before
     * this is applied, so the human-visible result is unaffected.
     */
    private fun truncateForLlm(text: String): String {
        if (text.length <= maxToolResultChars) {
            return text
        }
        return text.take(maxToolResultChars) +
            "... [truncated: showing first $maxToolResultChars of ${text.length} characters]"
    }

    /** Execute a tool call and emit result to frontend */
    private suspend fun executeToolCall(
        toolCall: ToolCall,
        emit: suspend (String) -> Unit,
    ): String {
        return try {
            val arguments = Json.parseToJsonElement(toolCall.arguments).jsonObject

            if (toolCall.name == ACTIVATE_SKILL_TOOL_NAME) {
                val skillName = arguments["name"]?.jsonPrimitive?.contentOrNull
                val skill = skills.find { it.name == skillName }
                val resultText =
                    skill?.let { wrapActivatedSkill(it) } ?: "Unknown skill: $skillName"
                emit(Events.toolResult(toolCall.name, JsonPrimitive(resultText)))
                return truncateForLlm(resultText)
            }

            val result = mcp.callTool(name = toolCall.name, arguments = arguments)
            val contentTexts = result.content.mapNotNull { (it as? TextContent)?.text }
            val resultText = contentTexts.joinToString("\n")

            val content = parseToolResultContent(contentTexts)
            val event = Events.toolResult(toolCall.name, content)
            log.debug("Emitting tool result event: {}", event)
            emit(event)

            truncateForLlm(resultText)
        } catch (e: Exception) {
            // e.message is normally a short human-readable message, but exceptions can in
            // principle carry arbitrarily large messages (e.g. embedded response bodies or
            // stack-trace-like details), so truncate here too for consistency and safety.
            val errorMsg = "Tool failed: ${e.message}"
            emit(Events.text(errorMsg))
            truncateForLlm(errorMsg)
        }
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
