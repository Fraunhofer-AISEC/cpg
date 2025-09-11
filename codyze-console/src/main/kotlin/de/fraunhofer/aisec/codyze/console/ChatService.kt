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
package de.fraunhofer.aisec.codyze.console

import com.typesafe.config.ConfigFactory
import de.fraunhofer.aisec.cpg.mcp.mcpserver.configureServer
import de.fraunhofer.aisec.cpg.mcp.mcpserver.listTools
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

@Serializable data class ChatMessageJSON(val role: String, val content: String)

@Serializable data class ChatRequestJSON(val messages: List<ChatMessageJSON>)

enum class LLMProvider {
    OLLAMA,
    OPENAI,
    ANTHROPIC,
}

@Serializable
private data class LLMRequest(
    val model: String,
    val messages: List<ChatMessageJSON>,
    val maxTokens: Int? = null,
    val stream: Boolean = false,
    val tools: List<JsonObject>? = null,
)

@Serializable private data class MCPToolCall(val function: MCPToolCallFunction)

@Serializable private data class MCPToolCallFunction(val name: String, val arguments: JsonObject)

@Serializable
private data class LLMResponse(
    val model: String? = null,
    val message: LLMMessage? = null,
    val done: Boolean? = null,
)

@Serializable
private data class LLMMessage(
    val role: String? = null,
    val content: String? = null,
    val tool_calls: List<MCPToolCall>? = null,
)

class ChatService {
    private val httpClient =
        HttpClient(CIO) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
    val config = ConfigFactory.load()

    val provider = LLMProvider.valueOf(config.getString("llm.provider").uppercase())
    val baseUrl = config.getString("llm.baseUrl")
    val model = config.getString("llm.model")

    // MCP Server Integration
    private val mcpServer = configureServer()

    /** Get available tools from MCP server (tools/list) */
    private fun getAvailableTools(): List<JsonObject> {
        return mcpServer.listTools().map { registeredTool ->
            Json.encodeToJsonElement(registeredTool).jsonObject
        }
    }

    /** Execute MCP tool call (tools/call) */
    private fun executeToolCall(toolCall: MCPToolCall): String {
        return try {
            val toolName = toolCall.function.name
            val mcpTool = mcpServer.tools[toolName]

            if (mcpTool != null) {
                // TODO: Invoke tool call properly
                // val result = mcpTool.handler.invoke(toolCall.function.arguments)
                // For now, simulate execution
                "Tool '$toolName' executed with args: ${toolCall.function.arguments}"
            } else {
                "Error: Tool '$toolName' not found"
            }
        } catch (e: Exception) {
            "Error executing tool '${toolCall.function.name}': ${e.message}"
        }
    }

    fun chat(request: ChatRequestJSON): Flow<String> = flow {
        if (baseUrl.isNullOrEmpty()) {
            emit("{\"error\":\"LLM configuration missing: LLM_BASE_URL not set\"}")
            return@flow
        }

        if (model.isNullOrEmpty()) {
            emit("{\"error\":\"LLM configuration missing: LLM_MODEL not set\"}")
            return@flow
        }

        try {
            when (provider) {
                LLMProvider.OLLAMA -> {
                    chatOllamaStream(request.messages).collect { chunk -> emit(chunk) }
                }

                LLMProvider.OPENAI -> TODO()
                LLMProvider.ANTHROPIC -> TODO()
            }
        } catch (e: Exception) {
            emit("{\"error\":\"Chat error: ${e.message}\"}")
        }
    }

    private fun chatOllamaStream(messages: List<ChatMessageJSON>): Flow<String> = flow {
        try {
            // Get available MCP tools
            val availableTools = getAvailableTools()

            val response =
                httpClient.post("$baseUrl/api/chat") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        LLMRequest(
                            model = model!!,
                            messages = messages,
                            stream = true,
                            tools = availableTools.ifEmpty { null },
                        )
                    )
                }

            if (response.status.isSuccess()) {
                val channel = response.bodyAsChannel()
                val jsonParser = Json { ignoreUnknownKeys = true }

                while (!channel.isClosedForRead) {
                    val chunk = channel.readUTF8Line()
                    if (chunk != null && chunk.isNotBlank()) {
                        try {
                            val llmResponse = jsonParser.decodeFromString<LLMResponse>(chunk)

                            if (llmResponse.done == true) {
                                break
                            }

                            val message = llmResponse.message

                            // Handle text content
                            val content = message?.content
                            if (!content.isNullOrEmpty()) {
                                emit(content)
                            }

                            // Handle tool calls
                            val toolCalls = message?.tool_calls
                            if (!toolCalls.isNullOrEmpty()) {
                                emit("\n\n**Tools Called:**\n")
                                for (toolCall in toolCalls) {
                                    emit("- **${toolCall.function.name}**\n")
                                    val result = executeToolCall(toolCall)
                                    emit("  Result: $result\n")
                                }

                                emit("\n")
                            }
                        } catch (e: Exception) {
                            // Skip invalid JSON lines
                            continue
                        }
                    }
                }
            } else {
                emit(
                    "{\"error\":\"Ollama request failed: ${response.status} - Check if Ollama server is running at $baseUrl\"}"
                )
            }
        } catch (e: Exception) {
            emit("{\"error\":\"Failed to connect to Ollama server at $baseUrl: ${e.message}\"}")
        }
    }
}
