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
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.ClientOptions
import io.modelcontextprotocol.kotlin.sdk.client.SseClientTransport
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.json.*

/**
 * Custom MCP client that connects to the local MCP server and uses Ollama for LLM interactions.
 * This is a prototype implementation for testing.
 */
class CustomMcpClient : AutoCloseable {
    private val config = ConfigFactory.load()
    private val mcpServerUrl: String = config.getString("mcp.serverUrl")
    private val ollamaBaseUrl: String = config.getString("llm.ollama.baseUrl")
    private val ollamaModel: String = config.getString("llm.ollama.model")

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

            // Configure timeouts - critical for long-running operations!
            install(HttpTimeout) {
                requestTimeoutMillis = 600_000 // 10 minutes for long tool executions
                connectTimeoutMillis = 30_000 // 30 seconds for connection
                socketTimeoutMillis = 600_000 // 10 minutes for socket operations
            }

            // Optional: Enable logging for debugging
            install(Logging) {
                level = LogLevel.INFO
                logger =
                    object : Logger {
                        override fun log(message: String) {
                            println("[HTTP] $message")
                        }
                    }
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
        println("[CustomMCP] Connecting to MCP server at $mcpServerUrl...")

        // Create SSE transport - requires HttpClient
        val transport = SseClientTransport(urlString = mcpServerUrl, client = httpClient)

        // Register sampling request handler BEFORE connecting
        registerSamplingHandler()

        mcp.connect(transport)

        // List available tools
        val toolsResult = mcp.listTools()
        tools = toolsResult.tools

        println("[CustomMCP] Connected! Available tools: ${tools.map { it.name }}")
    }

    /** Register handler for incoming sampling requests from the server */
    private fun registerSamplingHandler() {
        println("[CustomMCP] Registering sampling request handler...")

        mcp.setRequestHandler<CreateMessageRequest>(Method.Defined.SamplingCreateMessage) {
            request,
            _ ->
            try {
                // Send to Ollama (non-streaming)
                val llmResponse =
                    sendToOllamaNonStreaming(
                        messages = request.messages,
                        systemPrompt = request.systemPrompt,
                        maxTokens = request.maxTokens,
                    )

                println("[CustomMCP] LLM response received: ${llmResponse.take(100)}...")

                // Return result to server
                CreateMessageResult(
                    role = Role.Assistant,
                    content = TextContent(text = llmResponse),
                    model = ollamaModel,
                    stopReason = StopReason.EndTurn,
                )
            } catch (e: Exception) {
                println("[CustomMCP] Error in sampling handler: ${e.message}")
                e.printStackTrace()

                CreateMessageResult(
                    role = Role.Assistant,
                    content = TextContent(text = "Error: ${e.message}"),
                    model = ollamaModel,
                    stopReason = StopReason.EndTurn,
                )
            }
        }
    }

    /** Send messages to Ollama without streaming (for sampling) */
    private suspend fun sendToOllamaNonStreaming(
        messages: List<SamplingMessage>,
        systemPrompt: String?,
        maxTokens: Int,
    ): String {
        println("[CustomMCP] Sending to Ollama (non-streaming)...")

        // Convert MCP messages to Ollama format
        val ollamaMessages = buildJsonArray {
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

        // Call Ollama API
        val response =
            httpClient.post("$ollamaBaseUrl/v1/chat/completions") {
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("model", ollamaModel)
                        put("messages", ollamaMessages)
                        put("max_tokens", maxTokens)
                        put("stream", false) // Important: NO streaming for sampling
                    }
                )
            }

        val result = response.body<JsonObject>()

        // Extract response text
        val choices = result["choices"]?.jsonArray
        val firstChoice = choices?.firstOrNull()?.jsonObject
        val message = firstChoice?.get("message")?.jsonObject
        val content = message?.get("content")?.jsonPrimitive?.content

        return content ?: "No response from Ollama"
    }

    /** Process a chat query using Ollama with MCP tool support */
    fun chat(request: ChatRequestJSON): Flow<String> = channelFlow {
        println("[CustomMCP] Processing query...")

        val userMessage = request.messages.lastOrNull()?.content ?: ""
        println("[CustomMCP] User message: $userMessage")

        // Convert MCP tools to Ollama format
        val ollamaTools =
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

        println("[CustomMCP] Converted ${ollamaTools.size} tools for Ollama")

        // Build messages for Ollama
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

        // Call Ollama with streaming
        println(
            "[CustomMCP] Calling Ollama at $ollamaBaseUrl with model $ollamaModel (streaming)..."
        )

        try {
            val accumulatedToolCalls = mutableListOf<JsonObject>()

            httpClient
                .preparePost("$ollamaBaseUrl/v1/chat/completions") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        buildJsonObject {
                            put("model", ollamaModel)
                            put("messages", messages)
                            put("tools", JsonArray(ollamaTools))
                            put("stream", true)
                        }
                    )
                }
                .execute { response ->
                    val channel = response.body<ByteReadChannel>()

                    println("[CustomMCP] Receiving streaming response...")

                    try {
                        while (!channel.isClosedForRead) {
                            val line =
                                try {
                                    channel.readUTF8Line()
                                } catch (e: Exception) {
                                    println("[CustomMCP] Error reading line: ${e.message}")
                                    break
                                }

                            // If line is null, check if channel is closed
                            if (line == null) {
                                if (channel.isClosedForRead) {
                                    println("[CustomMCP] Channel closed, ending stream")
                                    break
                                }
                                // Otherwise, continue reading (might be temporary buffer issue)
                                continue
                            }

                            if (line.isBlank()) continue
                            if (line.startsWith("data: ")) {
                                val jsonStr = line.substringAfter("data: ").trim()
                                if (jsonStr == "[DONE]") {
                                    println("[CustomMCP] Stream completed")
                                    break
                                }

                                try {
                                    val chunk = Json.parseToJsonElement(jsonStr).jsonObject
                                    val choices = chunk["choices"]?.jsonArray
                                    val firstChoice = choices?.firstOrNull()?.jsonObject
                                    val delta = firstChoice?.get("delta")?.jsonObject

                                    // Stream content tokens
                                    val content =
                                        delta?.get("content")?.jsonPrimitive?.contentOrNull
                                    if (content != null) {
                                        // Send as JSON-wrapped event
                                        val jsonEvent = buildJsonObject {
                                            put("type", "text")
                                            put("content", content)
                                        }
                                        send(Json.encodeToString(jsonEvent))
                                    }

                                    // Collect tool calls from delta
                                    val toolCalls = delta?.get("tool_calls")?.jsonArray
                                    if (toolCalls != null && toolCalls.isNotEmpty()) {
                                        println(
                                            "[CustomMCP] ========== TOOL CALL DETECTED =========="
                                        )
                                        println("[CustomMCP] Full chunk: $chunk")
                                        println("[CustomMCP] Tool calls: $toolCalls")
                                        println(
                                            "[CustomMCP] ========================================"
                                        )
                                        toolCalls.forEach {
                                            accumulatedToolCalls.add(it.jsonObject)
                                        }
                                    }
                                } catch (e: Exception) {
                                    println("[CustomMCP] Error parsing chunk: ${e.message}")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        println("[CustomMCP] Stream reading error: ${e.message}")
                        e.printStackTrace()
                        // Don't throw - just break the loop and continue with tool calls
                    }
                }

            // Now handle the accumulated tool calls
            if (accumulatedToolCalls.isNotEmpty()) {
                println(
                    "[CustomMCP] Processing ${accumulatedToolCalls.size} accumulated tool calls..."
                )

                for (toolCallObj in accumulatedToolCalls) {
                    val function = toolCallObj["function"]?.jsonObject
                    val toolName = function?.get("name")?.jsonPrimitive?.contentOrNull
                    val argumentsStr =
                        function?.get("arguments")?.jsonPrimitive?.contentOrNull ?: "{}"

                    if (toolName != null) {
                        println("[CustomMCP] Calling MCP tool: $toolName")

                        try {
                            // Parse arguments as JsonObject and convert to Map
                            val jsonArgs = Json.parseToJsonElement(argumentsStr).jsonObject
                            val arguments: Map<String, Any?> = jsonArgs.toMap()

                            // Call MCP tool
                            val result = mcp.callTool(name = toolName, arguments = arguments)

                            // Extract and send result
                            val resultText =
                                result?.content?.joinToString("\n") {
                                    (it as? TextContent)?.text ?: ""
                                } ?: "No result"

                            println("[CustomMCP] Tool result length: ${resultText.length} chars")
                            println("[CustomMCP] Tool result preview: ${resultText.take(500)}...")

                            // Parse newline-separated JSON objects into an array
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
                                    println(
                                        "[CustomMCP] Warning: Could not parse as newline-separated JSON, treating as single item"
                                    )
                                    // Fallback: try to parse as single JSON
                                    JsonArray(listOf(Json.parseToJsonElement(resultText)))
                                }

                            // Send tool result with parsed array
                            val jsonEvent = buildJsonObject {
                                put("type", "tool_result")
                                put("tool", toolName)
                                put("data", resultArray)
                            }
                            send(Json.encodeToString(jsonEvent))
                        } catch (e: Exception) {
                            println("[CustomMCP] Tool call failed: ${e.message}")
                            e.printStackTrace()
                            val errorEvent = buildJsonObject {
                                put("type", "text")
                                put("content", "❌ **Tool failed:** ${e.message}\n\n")
                            }
                            send(Json.encodeToString(errorEvent))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("[CustomMCP] Error: ${e.message}")
            e.printStackTrace()
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
