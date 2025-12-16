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

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.GraphAIAgent.FeatureContext
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeExecuteMultipleTools
import ai.koog.agents.core.dsl.extension.nodeLLMRequestStreamingAndSendResults
import ai.koog.agents.core.dsl.extension.onMultipleToolCalls
import ai.koog.agents.core.environment.ReceivedToolResult
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.agents.mcp.McpTool
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.RequestMetaInfo
import ai.koog.prompt.streaming.StreamFrame
import com.typesafe.config.ConfigFactory
import de.fraunhofer.aisec.cpg.graph.invoke
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.Serializable

@Serializable data class ChatMessageJSON(val role: String, val content: String)

@Serializable data class ChatRequestJSON(val messages: List<ChatMessageJSON>)

class ChatService() {
    private val config = ConfigFactory.load()
    private val provider: String = config.getString("llm.provider")
    private val mcpServerUrl: String = config.getString("mcp.serverUrl")

    fun chat(request: ChatRequestJSON): Flow<String> = channelFlow {
        val userMessage = request.messages.lastOrNull()?.content ?: ""
        simpleGoogleAIExecutor("REDACTED_API_KEY").use { executor ->
            // Create tool registry
            val transport = McpToolRegistryProvider.defaultSseTransport(mcpServerUrl)
            val toolRegistry =
                McpToolRegistryProvider.fromTransport(
                    transport = transport,
                    name = "codyze-console",
                    version = "1.0.0",
                )

            // Simple agent with event handlers to capture tool results
            val agent =
                AIAgent(
                    promptExecutor = executor,
                    llmModel = GoogleModels.Gemini2_5Pro,
                    toolRegistry = toolRegistry,
                    systemPrompt = "You are a code analysis assistant with access to CPG tools.",
                    temperature = 0.0,
                    installFeatures = {
                        handleEvents {
                            onToolCallCompleted { event ->
                                println("[DEBUG] 🔧 Tool completed: ${event.tool.name}")
                                try {
                                    // Extract JSON strings from TextContent items
                                    val jsonItems =
                                        (event.result as? McpTool.Result)
                                            ?.promptMessageContents
                                            ?.map {
                                                (it
                                                        as?
                                                        io.modelcontextprotocol.kotlin.sdk.TextContent)
                                                    ?.text
                                            }

                                    // Create JSON array string
                                    val jsonArray = "[${jsonItems?.joinToString(",")}]"

                                    println(
                                        "[DEBUG] Tool result JSON array with ${jsonItems?.size} items"
                                    )

                                    // Send it wrapped in the markers the frontend expects
                                    send("__TOOL_RESULT_START__")
                                    send(jsonArray)
                                    send("__TOOL_RESULT_END__")
                                } catch (e: Exception) {
                                    println("[ERROR] Failed to send tool result: ${e.message}")
                                    e.printStackTrace()
                                }
                            }

                            onLLMStreamingFrameReceived { context ->
                                // Stream LLM responses
                                (context.streamFrame as? StreamFrame.Append)?.let { frame ->
                                    print(frame.text)
                                }
                            }
                        }
                    },
                )

            // Run the agent and get the result
            println("[DEBUG] Running agent...")
            val result = agent.run(userMessage)
            println("[DEBUG] Agent result: $result")

            // Send the final result if not empty
            if (result.isNotEmpty()) {
                send(result)
            }
            send("[DONE]")
        }
    }
}

private fun createAgent(
    toolRegistry: ToolRegistry,
    executor: PromptExecutor,
    installFeatures: FeatureContext.() -> Unit = {},
) =
    AIAgent(
        promptExecutor = executor,
        strategy = streamingWithToolsStrategy(),
        llmModel = GoogleModels.Gemini2_5Pro,
        systemPrompt =
            """
            You are a helpful assistant for code analysis using the Code Property Graph (CPG).
            You have access to various CPG analysis tools through MCP.
            Use these tools proactively to analyze code and answer questions about code structure.

            When multiple independent tools are needed, call them in a single turn if possible.
        """
                .trimIndent(),
        temperature = 0.0,
        toolRegistry = toolRegistry,
        installFeatures = installFeatures,
    )

// private fun getLLMModel(): LLModel {
//    val model =
//        when (provider.lowercase()) {
//            "gemini" -> config.getString("llm.gemini.model")
//            "ollama" -> config.getString("llm.ollama.model")
//            "vLLM" -> config.getString("llm.vLLM.model")
//            else -> throw IllegalArgumentException("Unsupported LLM provider: $provider")
//        }
//
//    return when (provider.lowercase()) {
//        "gemini" ->
//            LLModel(
//                provider = LLMProvider.Google,
//                id = GoogleModels.Gemini2_5Pro.id,
//                capabilities = listOf(LLMCapability.Temperature, LLMCapability.Tools),
//                contextLength = 128000,
//                maxOutputTokens = 8192,
//            )
//
//        "ollama" ->
//            LLModel(
//                provider = LLMProvider.Ollama,
//                id = model,
//                capabilities = listOf(LLMCapability.Tools),
//                contextLength = 65536,
//                maxOutputTokens = 8192,
//            )
//
//        "vLLM" ->
//            LLModel(
//                provider = LLMProvider.OpenAI,
//                id = model,
//                capabilities = listOf(LLMCapability.Tools),
//                contextLength = 65536,
//                maxOutputTokens = 8192,
//            )
//
//        else -> throw IllegalArgumentException("Unsupported LLM provider: $provider")
//    }
// }

fun streamingWithToolsStrategy() =
    strategy("streaming_loop") {
        val executeMultipleTools by nodeExecuteMultipleTools(parallelTools = true)
        val nodeStreaming by nodeLLMRequestStreamingAndSendResults()

        val mapStringToRequests by
            node<String, List<Message.Request>> { input ->
                listOf(Message.User(content = input, metaInfo = RequestMetaInfo.Empty))
            }

        val applyRequestToSession by
            node<List<Message.Request>, List<Message.Request>> { input ->
                llm.writeSession {
                    appendPrompt {
                        input.filterIsInstance<Message.User>().forEach { user(it.content) }

                        tool {
                            input.filterIsInstance<Message.Tool.Result>().forEach { result(it) }
                        }
                    }
                    input
                }
            }

        val mapToolCallsToRequests by
            node<List<ReceivedToolResult>, List<Message.Request>> { input ->
                input.map { it.toMessage() }
            }

        edge(nodeStart forwardTo mapStringToRequests)
        edge(mapStringToRequests forwardTo applyRequestToSession)
        edge(applyRequestToSession forwardTo nodeStreaming)
        edge(nodeStreaming forwardTo executeMultipleTools onMultipleToolCalls { true })
        edge(executeMultipleTools forwardTo mapToolCallsToRequests)
        edge(mapToolCallsToRequests forwardTo applyRequestToSession)
        edge(
            nodeStreaming forwardTo
                nodeFinish onCondition
                {
                    it.filterIsInstance<Message.Tool.Call>().isEmpty()
                }
        )
    }
