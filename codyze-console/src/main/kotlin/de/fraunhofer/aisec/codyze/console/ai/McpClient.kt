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

import de.fraunhofer.aisec.codyze.console.ai.clients.Events
import de.fraunhofer.aisec.codyze.console.ai.clients.GeminiClient
import de.fraunhofer.aisec.codyze.console.ai.clients.OpenAiClient
import de.fraunhofer.aisec.codyze.console.ai.clients.ToolCall
import de.fraunhofer.aisec.codyze.console.ai.clients.ToolCallWithResult
import io.ktor.client.*
import io.modelcontextprotocol.kotlin.sdk.client.Client as McpSdkClient
import io.modelcontextprotocol.kotlin.sdk.client.ClientOptions
import io.modelcontextprotocol.kotlin.sdk.client.SseClientTransport
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.json.*

class McpClient(
    private val httpClient: HttpClient,
    private val geminiClient: GeminiClient?,
    private val openAiClient: OpenAiClient?,
    private val llmModel: String,
    private val mcpServerUrl: String,
) {

    private val mcp: McpSdkClient =
        McpSdkClient(
            clientInfo = Implementation(name = "codyze-client", version = "1.0.0"),
            options =
                ClientOptions(capabilities = ClientCapabilities(sampling = buildJsonObject {})),
        )

    private var tools: List<Tool> = emptyList()

    /** Connect to the MCP server via SSE */
    suspend fun connect() {
        val transport = SseClientTransport(urlString = mcpServerUrl, client = httpClient)
        registerSamplingHandler()
        mcp.connect(transport)
        val toolsResult = mcp.listTools()
        tools = toolsResult.tools
    }

    /** Register handler for incoming sampling requests from the server */
    private fun registerSamplingHandler() {
        mcp.setRequestHandler<CreateMessageRequest>(Method.Defined.SamplingCreateMessage) {
            request,
            _ ->
            try {
                val llmResponse =
                    when {
                        geminiClient != null ->
                            geminiClient.query(
                                messages = request.params.messages,
                                systemPrompt = request.params.systemPrompt,
                            )
                        openAiClient != null ->
                            openAiClient.query(
                                messages = request.params.messages,
                                systemPrompt = request.params.systemPrompt,
                                maxTokens = request.params.maxTokens,
                            )
                        else -> throw IllegalStateException("No LLM client configured")
                    }

                CreateMessageResult(
                    role = Role.Assistant,
                    content = TextContent(text = llmResponse),
                    model = llmModel,
                    stopReason = StopReason.EndTurn,
                )
            } catch (e: Exception) {
                CreateMessageResult(
                    role = Role.Assistant,
                    content = TextContent(text = "Error: ${e.message}"),
                    model = llmModel,
                    stopReason = StopReason.EndTurn,
                )
            }
        }
    }

    /** Process a chat query using the LLM with MCP tool support */
    fun chat(request: ChatRequestJSON): Flow<String> = channelFlow {
        // Send keepalive to prevent browser timeout
        // TODO: Why is this needed even though the HTTP client has a timeout configured?
        // Apparently, it is only needed when prompting our local models
        send(Events.keepalive())

        val userMessage = request.messages.lastOrNull()?.content ?: ""
        val conversationHistory = request.messages

        try {
            // First call: check for tool calls, stream everything
            val toolCalls =
                sendPrompt(userMessage, conversationHistory = conversationHistory, tools = tools) {
                    event ->
                    // Stream all events (reasoning and text) directly
                    send(event)
                }

            if (toolCalls.isNotEmpty()) {
                val toolResults =
                    toolCalls.map { toolCall ->
                        val result = executeToolCall(toolCall) { event -> send(event) }
                        ToolCallWithResult(toolCall, result)
                    }

                sendPrompt(
                    userMessage,
                    conversationHistory = conversationHistory,
                    toolResults = toolResults,
                ) { event ->
                    send(event)
                }
            }
        } catch (e: Exception) {
            send(Events.text("Error: ${e.message}"))
        }
    }

    /** Send a prompt to the configured LLM */
    private suspend fun sendPrompt(
        userMessage: String,
        conversationHistory: List<ChatMessageJSON> = emptyList(),
        tools: List<Tool> = emptyList(),
        toolResults: List<ToolCallWithResult>? = null,
        emit: suspend (String) -> Unit,
    ): List<ToolCall> {
        return when {
            geminiClient != null ->
                geminiClient.sendPrompt(userMessage, conversationHistory, tools, toolResults, emit)
            openAiClient != null ->
                openAiClient.sendPrompt(userMessage, conversationHistory, tools, toolResults, emit)
            else -> throw IllegalStateException("No LLM client configured")
        }
    }

    /** Execute a tool call and emit result to frontend */
    private suspend fun executeToolCall(
        toolCall: ToolCall,
        emit: suspend (String) -> Unit,
    ): String {
        return try {
            // Emit pending state so frontend can show loading indicator
            emit(Events.toolPending(toolCall))

            val jsonArgs = Json.parseToJsonElement(toolCall.arguments).jsonObject
            val arguments: Map<String, Any?> = jsonArgs.toMap()

            val result = mcp.callTool(name = toolCall.name, arguments = arguments)
            val contentTexts = result.content.mapNotNull { (it as? TextContent)?.text }
            val resultText = contentTexts.joinToString("\n")

            val content = buildToolContentPayload(contentTexts)
            emit(Events.toolResult(toolCall.name, content))

            resultText
        } catch (e: Exception) {
            val errorMsg = "Tool failed: ${e.message}"
            emit(Events.text(errorMsg))
            errorMsg
        }
    }
}

// Helper to convert JsonObject to Map
private fun JsonObject.toMap(): Map<String, Any?> {
    return this.mapValues { (_, value) ->
        when (value) {
            is JsonPrimitive -> {
                when {
                    value.isString -> value.content
                    value.booleanOrNull != null -> value.boolean
                    value.intOrNull != null -> value.int
                    value.longOrNull != null -> value.long
                    value.doubleOrNull != null -> value.double
                    else -> value.content
                }
            }
            is JsonObject -> value.toMap()
            is JsonArray ->
                value.map {
                    when (it) {
                        is JsonPrimitive -> it.content
                        is JsonObject -> it.toMap()
                        is JsonArray -> it.toString()
                        is JsonNull -> null
                    }
                }
            is JsonNull -> null
        }
    }
}

private fun buildToolContentPayload(contentTexts: List<String>): JsonElement {
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
