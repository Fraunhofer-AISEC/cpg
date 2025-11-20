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

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import com.typesafe.config.ConfigFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.Serializable

@Serializable data class ChatMessageJSON(val role: String, val content: String)

@Serializable data class ChatRequestJSON(val messages: List<ChatMessageJSON>)

class ChatService() {
    val config = ConfigFactory.load()
    val provider: String = config.getString("llm.provider")
    val mcpServerUrl: String = config.getString("mcp.serverUrl")

    fun chat(request: ChatRequestJSON): Flow<String> = channelFlow {
        val userMessage = request.messages.lastOrNull()?.content ?: ""

        // Connect to MCP server via SSE
        val transport = McpToolRegistryProvider.defaultSseTransport(mcpServerUrl)

        // Create tool registry from MCP server
        val toolRegistry =
            McpToolRegistryProvider.fromTransport(
                transport = transport,
                name = "codyze-console",
                version = "1.0.0",
            )

        // Get provider-specific configuration
        val (executor, llmModel) =
            when (provider.lowercase()) {
                "gemini" -> {
                    val apiKey =
                        System.getenv("GEMINI_API_KEY")
                            ?: throw IllegalStateException(
                                "GEMINI_API_KEY environment variable not set"
                            )
                    val model = config.getString("llm.gemini.model")

                    val executor = simpleGoogleAIExecutor(apiKey = apiKey)
                    val llmModel =
                        LLModel(
                            provider = LLMProvider.Google,
                            id = model,
                            capabilities = listOf(LLMCapability.Temperature, LLMCapability.Tools),
                            contextLength = 128000,
                            maxOutputTokens = 8192,
                        )

                    executor to llmModel
                }

                "ollama" -> {
                    val baseUrl = config.getString("llm.ollama.baseUrl")
                    val model = config.getString("llm.ollama.model")

                    val executor = simpleOllamaAIExecutor(baseUrl = baseUrl)
                    val llmModel =
                        LLModel(
                            provider = LLMProvider.Ollama,
                            id = model,
                            capabilities = listOf(LLMCapability.Tools),
                            contextLength = 65536,
                            maxOutputTokens = 8192,
                        )

                    executor to llmModel
                }

                "vLLM" -> {
                    val baseUrl = config.getString("llm.vLLM.baseUrl")
                    val model = config.getString("llm.vLLM.model")

                    val executor = simpleOllamaAIExecutor(baseUrl = baseUrl)
                    val llmModel =
                        LLModel(
                            provider = LLMProvider.OpenAI,
                            id = model,
                            capabilities = listOf(LLMCapability.Tools),
                            contextLength = 65536,
                            maxOutputTokens = 8192,
                        )

                    executor to llmModel
                }

                else -> throw IllegalArgumentException("Unsupported LLM provider: $provider")
            }

        // Create the agent
        val agent =
            AIAgent(
                promptExecutor = executor,
                llmModel = llmModel,
                systemPrompt =
                    """
                    You are a helpful assistant for code analysis using the Code Property Graph (CPG).
                    You have access to various CPG analysis tools through MCP.
                    Use these tools proactively to analyze code and answer questions about code structure.

                    When multiple independent tools are needed, call them in a single turn if possible.
                """
                        .trimIndent(),
                toolRegistry = toolRegistry,
                maxIterations = 100,
            )

        // Send initial empty data to establish the SSE connection.
        send("")

        val result = agent.run(userMessage)

        println("Debug: Agent completed with result length: ${result.length}")

        // Send the complete result
        send(result)
        send("[DONE]")
    }
}
