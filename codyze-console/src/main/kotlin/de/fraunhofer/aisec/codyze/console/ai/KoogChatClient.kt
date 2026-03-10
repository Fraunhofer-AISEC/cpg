/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.functionalStrategy
import ai.koog.agents.core.dsl.extension.asAssistantMessage
import ai.koog.agents.core.dsl.extension.containsToolCalls
import ai.koog.agents.core.dsl.extension.executeMultipleTools
import ai.koog.agents.core.dsl.extension.extractToolCalls
import ai.koog.agents.core.dsl.extension.requestLLMMultiple
import ai.koog.agents.core.dsl.extension.sendMultipleToolResults
import ai.koog.agents.features.eventHandler.feature.EventHandler
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.streaming.StreamFrame
import de.fraunhofer.aisec.codyze.console.ai.clients.Events
import de.fraunhofer.aisec.codyze.console.ai.clients.SYSTEM_PROMPT
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive

class KoogChatClient(
    private val provider: String,
    private val model: String,
    private val baseUrl: String,
    private val mcpServerUrl: String,
) {
    fun chat(request: ChatRequestJSON): Flow<String> = channelFlow {
        send(Events.keepalive())

        val userMessage = request.messages.lastOrNull()?.content ?: return@channelFlow

        try {
            val executor =
                when (provider) {
                    "gemini" -> {
                        val key =
                            System.getenv("GEMINI_API_KEY")
                                ?: throw IllegalStateException("GEMINI_API_KEY not set")
                        simpleGoogleAIExecutor(key)
                    }

                    else -> SingleLLMPromptExecutor(OllamaClient(baseUrl = baseUrl))
                }

            val llmModel: LLModel =
                when (provider) {
                    "gemini" -> GoogleModels.Gemini2_5Flash
                    else ->
                        LLModel(
                            provider = LLMProvider.Ollama,
                            id = model,
                            capabilities = listOf(LLMCapability.Temperature, LLMCapability.Tools),
                            contextLength = 8192L,
                        )
                }

            val toolRegistry =
                McpToolRegistryProvider.fromTransport(
                    McpToolRegistryProvider.defaultSseTransport(mcpServerUrl)
                )

            val agentConfig =
                AIAgentConfig(
                    prompt =
                        prompt("codyze-agent") {
                            system(SYSTEM_PROMPT)
                            request.messages.dropLast(1).forEach { msg ->
                                when (msg.role) {
                                    "user" -> user(msg.content)
                                    "assistant" -> assistant(msg.content)
                                }
                            }
                        },
                    model = llmModel,
                    maxAgentIterations = 10,
                )

            // TODO: proof whether we need another strategy.
            // Basic strategy taken from https://docs.koog.ai/functional-agents/
            // Note: It seems that we can just use the predefined `chatAgentStrategy`
            // https://docs.koog.ai/predefined-agent-strategies/#2-reasoning
            // chatAgentStrategy() is only useful for tool calling without chatting in plain text.
            val agentStrategy =
                functionalStrategy<String, String> { input ->
                    // Send the user input to the LLM
                    var responses = requestLLMMultiple(input)
                    // Only loop while the LLM requests tools
                    while (responses.containsToolCalls()) {
                        val pendingCalls = extractToolCalls(responses)
                        val results = executeMultipleTools(pendingCalls)
                        // Send the tool results back to the LLM. The LLM may call more tools or
                        // return a final output
                        responses = sendMultipleToolResults(results)
                    }
                    // When no tool calls remain, extract and return the assistant message content
                    // from the response
                    responses.single().asAssistantMessage().content
                }

            val streamChannel = this.channel
            val agent =
                AIAgent(
                    promptExecutor = executor,
                    strategy = agentStrategy,
                    agentConfig = agentConfig,
                    toolRegistry = toolRegistry,
                ) {
                    install(EventHandler) {
                        onLLMStreamingFrameReceived { ctx ->
                            val frame = ctx.streamFrame
                            if (frame is StreamFrame.Append && frame.text.isNotEmpty()) {
                                streamChannel.trySend(Events.text(frame.text))
                            }
                        }
                        onToolCallStarting { ctx ->
                            streamChannel.trySend(Events.text("\n[Tool: ${ctx.toolName}]\n"))
                        }
                        onToolCallCompleted { ctx ->
                            val resultStr = ctx.toolResult.toString()
                            if (resultStr.isNotEmpty()) {
                                val content =
                                    try {
                                        Json.parseToJsonElement(resultStr)
                                    } catch (_: Exception) {
                                        JsonPrimitive(resultStr)
                                    }
                                streamChannel.trySend(Events.toolResult(ctx.toolName, content))
                            }
                        }
                    }
                }
            println("Registered tools: ${toolRegistry.tools.map { it.name }}")
            agent.run(userMessage)
        } catch (e: Exception) {
            send(Events.text("Error: ${e.message}"))
        }
    }
}
