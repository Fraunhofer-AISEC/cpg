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
import io.modelcontextprotocol.kotlin.sdk.client.SseClientTransport
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.json.*

class ChatClient(
    private val httpClient: HttpClient,
    private val llm: LlmClient,
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
                    llm.query(
                        messages = request.params.messages,
                        systemPrompt = request.params.systemPrompt,
                    )

                CreateMessageResult(
                    role = Role.Assistant,
                    content = TextContent(text = llmResponse),
                    model = llm.modelName,
                    stopReason = StopReason.EndTurn,
                )
            } catch (e: Exception) {
                CreateMessageResult(
                    role = Role.Assistant,
                    content = TextContent(text = "Error: ${e.message}"),
                    model = llm.modelName,
                    stopReason = StopReason.EndTurn,
                )
            }
        }
    }

    /** Maximum number of tool call iterations before forcing a text response. */
    private val maxToolIterations = 8

    /** Process a chat query using the LLM with MCP tool support */
    fun chat(request: ChatRequestJSON): Flow<String> = channelFlow {
        send(Events.keepalive())

        val userMessage = request.messages.lastOrNull()?.content ?: ""
        val conversationHistory = request.messages

        try {
            val maxAgentSteps = mutableListOf<List<ToolCallWithResult>>()
            var counter = 0

            var toolCalls =
                llm.sendPrompt(
                    userMessage = userMessage,
                    conversationHistory = conversationHistory,
                    tools = tools,
                    onText = { text -> send(Events.text(text)) },
                    onReasoning = { thought -> send(Events.reasoning(thought)) },
                )

            while (toolCalls.isNotEmpty() && counter < maxToolIterations) {
                counter++
                val roundtripResults =
                    toolCalls.map { toolCall ->
                        val result = executeToolCall(toolCall) { jsonEvent -> send(jsonEvent) }
                        ToolCallWithResult(toolCall, result)
                    }
                maxAgentSteps.add(roundtripResults)

                toolCalls =
                    llm.sendPrompt(
                        userMessage = userMessage,
                        conversationHistory = conversationHistory,
                        maxAgentSteps = maxAgentSteps,
                        tools = tools,
                        onText = { text -> send(Events.text(text)) },
                        onReasoning = { thought -> send(Events.reasoning(thought)) },
                    )
            }
        } catch (e: Exception) {
            println("[LLM] Error: ${e.message}")
            send(Events.text("Error: ${e.message}"))
        }
    }

    /** Execute a tool call and emit result to frontend */
    private suspend fun executeToolCall(
        toolCall: ToolCall,
        emit: suspend (String) -> Unit,
    ): String {
        return try {
            val jsonArgs = Json.parseToJsonElement(toolCall.arguments).jsonObject
            val arguments: Map<String, Any?> = jsonArgs.toMap()

            val result = mcp.callTool(name = toolCall.name, arguments = arguments)
            val contentTexts = result.content.mapNotNull { (it as? TextContent)?.text }
            val resultText = contentTexts.joinToString("\n")

            val content = buildToolContentPayload(contentTexts)
            val event = Events.toolResult(toolCall.name, content)
            println("[Tool] Emitting event: $event")
            emit(event)

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
