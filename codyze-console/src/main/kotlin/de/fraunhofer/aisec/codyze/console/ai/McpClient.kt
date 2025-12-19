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

import com.typesafe.config.ConfigFactory
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.ClientOptions
import io.modelcontextprotocol.kotlin.sdk.client.SseClientTransport
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.json.*

/** MCP client that connects to the local MCP server and uses a LLM for interactions. */
class McpClient : AutoCloseable {
    private val config = ConfigFactory.load()
    private val mcpServerUrl: String = config.getString("mcp.serverUrl")
    private val llmBaseUrl: String = config.getString("llm.ollama.baseUrl")
    private val llmModel: String = config.getString("llm.ollama.model")

    private val httpClient =
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    }
                )
            }
            // Install SSE plugin for MCP client transport
            install(SSE)

            // Configure timeouts
            install(HttpTimeout) {
                requestTimeoutMillis = 600_000 // 10 minutes for long tool executions
                connectTimeoutMillis = 30_000 // 30 seconds for connection
                socketTimeoutMillis = 600_000 // 10 minutes for socket operations
            }
        }

    private val mcp: Client =
        Client(
            clientInfo = Implementation(name = "codyze-custom-client", version = "1.0.0"),
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
                    queryLlm(
                        messages = request.messages,
                        systemPrompt = request.systemPrompt,
                        maxTokens = request.maxTokens,
                    )

                // Return result to server
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

    /** Query the LLM for sampling (non-streaming) */
    private suspend fun queryLlm(
        messages: List<SamplingMessage>,
        systemPrompt: String?,
        maxTokens: Int,
    ): String {
        // Convert MCP messages to LLM format
        val llmMessages = buildJsonArray {
            // Add system prompt if provided
            if (systemPrompt != null) {
                add(
                    buildJsonObject {
                        put("role", "system")
                        put("content", systemPrompt)
                    }
                )
            }

            // Add conversation messages
            messages.forEach { msg ->
                add(
                    buildJsonObject {
                        put("role", msg.role.toString().lowercase())
                        val content = (msg.content as? TextContent)?.text ?: ""
                        put("content", content)
                    }
                )
            }
        }

        val response =
            httpClient.post("$llmBaseUrl/v1/chat/completions") {
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("model", llmModel)
                        put("messages", llmMessages)
                        put("max_tokens", maxTokens)
                        put("stream", false)
                    }
                )
            }

        val result = response.body<JsonObject>()

        // Extract response text
        val choices = result["choices"]?.jsonArray
        val firstChoice = choices?.firstOrNull()?.jsonObject
        val message = firstChoice?.get("message")?.jsonObject
        val content = message?.get("content")?.jsonPrimitive?.content

        return content ?: "No response"
    }

    /** Process a chat query using the LLM with MCP tool support */
    fun chat(request: ChatRequestJSON): Flow<String> = channelFlow {
        // Check for cancellation before starting
        ensureActive()
        val userMessage = request.messages.lastOrNull()?.content ?: ""

        // Convert MCP tools to LLM format
        val llmTools =
            tools.map { tool ->
                buildJsonObject {
                    put("type", "function")
                    put(
                        "function",
                        buildJsonObject {
                            put("name", tool.name)
                            put("description", tool.description ?: "")
                            put("parameters", tool.inputSchema.properties ?: buildJsonObject {})
                        },
                    )
                }
            }

        // Build messages for LLM
        val messages = buildJsonArray {
            add(
                buildJsonObject {
                    put("role", "system")
                    put("content", "You are a code analysis assistant with access to CPG tools.")
                }
            )
            add(
                buildJsonObject {
                    put("role", "user")
                    put("content", userMessage)
                }
            )
        }

        try {
            val accumulatedToolCalls = mutableListOf<JsonObject>()

            httpClient
                .preparePost("$llmBaseUrl/v1/chat/completions") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        buildJsonObject {
                            put("model", llmModel)
                            put("messages", messages)
                            put("tools", JsonArray(llmTools))
                            put("stream", true)
                        }
                    )
                }
                .execute { response ->
                    val channel = response.body<ByteReadChannel>()

                    try {
                        while (!channel.isClosedForRead) {
                            val line =
                                try {
                                    channel.readUTF8Line()
                                } catch (e: Exception) {
                                    break
                                }

                            // If line is null, check if channel is closed
                            if (line == null) {
                                if (channel.isClosedForRead) {
                                    break
                                }
                                continue
                            }

                            if (line.isBlank()) continue
                            if (line.startsWith("data: ")) {
                                val jsonStr = line.substringAfter("data: ").trim()
                                if (jsonStr == "[DONE]") {
                                    break
                                }

                                try {
                                    val chunk = Json.parseToJsonElement(jsonStr).jsonObject
                                    val choices = chunk["choices"]?.jsonArray
                                    val firstChoice = choices?.firstOrNull()?.jsonObject
                                    val delta = firstChoice?.get("delta")?.jsonObject

                                    val content =
                                        delta?.get("content")?.jsonPrimitive?.contentOrNull
                                    if (!content.isNullOrEmpty()) {
                                        val jsonEvent = buildJsonObject {
                                            put("type", "text")
                                            put("content", content)
                                        }
                                        try {
                                            send(Json.encodeToString(jsonEvent))
                                        } catch (e: Exception) {
                                            throw e
                                        }
                                    }

                                    val toolCalls = delta?.get("tool_calls")?.jsonArray
                                    if (!toolCalls.isNullOrEmpty()) {
                                        toolCalls.forEach {
                                            accumulatedToolCalls.add(it.jsonObject)
                                        }
                                    }
                                } catch (e: Exception) {
                                    // Continue on parsing errors
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Continue with tool calls
                    }
                }

            // Handle accumulated tool calls
            for (toolCallObj in accumulatedToolCalls) {
                // Check if the flow is still active before processing tool calls
                ensureActive()

                val function = toolCallObj["function"]?.jsonObject
                val toolName = function?.get("name")?.jsonPrimitive?.contentOrNull
                val argumentsStr = function?.get("arguments")?.jsonPrimitive?.contentOrNull ?: "{}"

                if (toolName != null) {
                    try {
                        val jsonArgs = Json.parseToJsonElement(argumentsStr).jsonObject
                        val arguments: Map<String, Any?> = jsonArgs.toMap()

                        val result = mcp.callTool(name = toolName, arguments = arguments)

                        val resultText =
                            result.content.joinToString("\n") { (it as? TextContent)?.text ?: "" }

                        val resultArray =
                            try {
                                val items =
                                    resultText
                                        .trim()
                                        .split("\n")
                                        .filter { it.isNotBlank() }
                                        .map { Json.parseToJsonElement(it) }
                                JsonArray(items)
                            } catch (e: Exception) {
                                JsonArray(listOf(Json.parseToJsonElement(resultText)))
                            }

                        val jsonEvent = buildJsonObject {
                            put("type", "tool_result")
                            put("toolName", toolName)
                            put("content", resultArray)
                        }
                        try {
                            send(Json.encodeToString(jsonEvent))
                        } catch (e: Exception) {
                            // Continue with next tool call
                        }
                    } catch (e: Exception) {
                        val errorEvent = buildJsonObject {
                            put("type", "text")
                            put("content", "Tool failed: ${e.message}")
                        }
                        send(Json.encodeToString(errorEvent))
                    }
                }
            }
        } catch (e: Exception) {
            val errorEvent = buildJsonObject {
                put("type", "text")
                put("content", "Error: ${e.message}")
            }
            send(Json.encodeToString(errorEvent))
        }
    }

    override fun close() {
        httpClient.close()
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
                        else -> it.toString()
                    }
                }

            else -> value.toString()
        }
    }
}
