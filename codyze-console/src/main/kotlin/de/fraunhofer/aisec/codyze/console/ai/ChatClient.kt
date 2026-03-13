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
package de.fraunhofer.aisec.codyze.console.ai

import de.fraunhofer.aisec.codyze.console.ai.clients.*
import io.ktor.client.*
import io.modelcontextprotocol.kotlin.sdk.client.Client as McpSdkClient
import io.modelcontextprotocol.kotlin.sdk.client.ClientOptions
import io.modelcontextprotocol.kotlin.sdk.client.StreamableHttpClientTransport
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory

class ChatClient(
    private val httpClient: HttpClient,
    private val llm: LlmClient,
    private val mcpServerUrl: String,
) {

    private val log = LoggerFactory.getLogger(ChatClient::class.java)

    private val mcp: McpSdkClient =
        McpSdkClient(
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

    /** Maximum number of tool call iterations before forcing a text response. */
    private val maxToolIterations = 8

    /** Process a chat query using the LLM with MCP tool support */
    fun chat(request: ChatRequestJSON): Flow<String> = channelFlow {
        // Used if the LLM needs more time for a "cold-start"
        send(Events.keepalive())

        val userMessage = request.messages.lastOrNull()?.content ?: ""
        val conversationHistory = request.messages

        try {
            val toolCallHistory = mutableListOf<List<ToolCallWithResult>>()
            var iteration = 0

            var toolCalls =
                llm.sendPrompt(
                    userMessage = userMessage,
                    conversationHistory = conversationHistory,
                    tools = tools,
                    onText = { text -> send(Events.text(text)) },
                    onReasoning = { thought -> send(Events.reasoning(thought)) },
                )
            log.info(
                "Initial prompt returned {} tool calls: {}",
                toolCalls.size,
                toolCalls.map { it.name },
            )

            while (toolCalls.isNotEmpty() && iteration < maxToolIterations) {
                iteration++
                log.info("Agent: Round {}: executing {}", iteration, toolCalls.map { it.name })
                val roundtripResults =
                    toolCalls.map { toolCall ->
                        val result = executeToolCall(toolCall) { jsonEvent -> send(jsonEvent) }
                        ToolCallWithResult(toolCall, result)
                    }
                toolCallHistory.add(roundtripResults)

                toolCalls =
                    llm.sendPrompt(
                        userMessage = userMessage,
                        conversationHistory = conversationHistory,
                        toolCallHistory = toolCallHistory,
                        tools = tools,
                        onText = { text -> send(Events.text(text)) },
                        onReasoning = { thought -> send(Events.reasoning(thought)) },
                    )
            }
        } catch (e: Exception) {
            log.error("Chat error: {}", e.message, e)
            send(Events.text("Error: ${e.message}"))
        }
    }

    /** Execute a tool call and emit result to frontend */
    private suspend fun executeToolCall(
        toolCall: ToolCall,
        emit: suspend (String) -> Unit,
    ): String {
        return try {
            val arguments = Json.parseToJsonElement(toolCall.arguments).jsonObject

            val result = mcp.callTool(name = toolCall.name, arguments = arguments)
            val contentTexts = result.content.mapNotNull { (it as? TextContent)?.text }
            val resultText = contentTexts.joinToString("\n")

            val content =
                if (contentTexts.isEmpty()) {
                    JsonArray(emptyList())
                } else {
                    val parsedItems =
                        contentTexts.map { text ->
                            try {
                                Json.parseToJsonElement(text)
                            } catch (_: Exception) {
                                JsonPrimitive(text)
                            }
                        }
                    if (parsedItems.size == 1) parsedItems[0] else JsonArray(parsedItems)
                }
            val event = Events.toolResult(toolCall.name, content)
            log.debug("Emitting tool result event: {}", event)
            emit(event)

            resultText
        } catch (e: Exception) {
            val errorMsg = "Tool failed: ${e.message}"
            emit(Events.text(errorMsg))
            errorMsg
        }
    }
}
