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
package de.fraunhofer.aisec.cpg.ai

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeExecuteTools
import ai.koog.agents.core.dsl.extension.nodeLLMRequest
import ai.koog.agents.core.dsl.extension.nodeLLMSendToolResults
import ai.koog.agents.core.dsl.extension.onTextMessage
import ai.koog.agents.core.dsl.extension.onToolCalls
import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.prompt.streaming.StreamFrame
import ai.koog.serialization.typeToken
import de.fraunhofer.aisec.cpg.ai.clients.LlmProviderConfig
import io.ktor.client.HttpClient
import java.util.Collections
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** The `tool_call` and `tool_result` events of [ChatService.sendToolEvents]. */
class ToolEventsTest {
    private val model = LLModel(provider = LLMProvider("test", "Test"), id = "test-model")

    /** Answers with the given responses, one per request. */
    private inner class ScriptedExecutor(private val responses: List<Message.Assistant>) :
        PromptExecutor() {
        private var next = 0

        override suspend fun execute(
            prompt: ai.koog.prompt.Prompt,
            model: LLModel,
            tools: List<ToolDescriptor>,
        ): Message.Assistant =
            responses.getOrElse(next++) {
                Message.Assistant(listOf(MessagePart.Text("done")), ResponseMetaInfo.Empty)
            }

        override fun executeStreaming(
            prompt: ai.koog.prompt.Prompt,
            model: LLModel,
            tools: List<ToolDescriptor>,
        ): Flow<StreamFrame> = flowOf()

        override suspend fun moderate(prompt: ai.koog.prompt.Prompt, model: LLModel) =
            ModerationResult(false, emptyMap())

        override suspend fun models(): List<LLModel> = listOf(model)

        override fun close() {}
    }

    private class LookupTool :
        SimpleTool<Map<String, String>>(typeToken<Map<String, String>>(), "lookup", "Looks up") {
        override suspend fun execute(args: Map<String, String>): String = "found"
    }

    private fun toolCalls(vararg ids: String): Message.Assistant =
        Message.Assistant(
            ids.map { MessagePart.Tool.Call(it, "lookup", """{"name":"$it"}""") },
            ResponseMetaInfo.Empty,
        )

    @Test
    fun theCallsOfOneResponseShareTheirRound() = runTest {
        val service =
            ChatService(
                httpClient = HttpClient(),
                llmProviderConfig = LlmProviderConfig(HttpClient(), emptyList()),
                mcpServerUrl = "localhost",
            )
        // Two calls in the first response, run in parallel, and one in the second
        val executor =
            ScriptedExecutor(
                listOf(
                    toolCalls("a", "b"),
                    toolCalls("c"),
                    Message.Assistant(listOf(MessagePart.Text("done")), ResponseMetaInfo.Empty),
                )
            )
        val strategy =
            strategy<String, String>("tools") {
                val request by nodeLLMRequest()
                val execute by nodeExecuteTools("execute", true)
                val sendResults by nodeLLMSendToolResults()
                edge(nodeStart forwardTo request)
                edge(request forwardTo execute onToolCalls { true })
                edge(request forwardTo nodeFinish onTextMessage { true })
                edge(execute forwardTo sendResults)
                edge(sendResults forwardTo execute onToolCalls { true })
                edge(sendResults forwardTo nodeFinish onTextMessage { true })
            }
        val events = Collections.synchronizedList(mutableListOf<String>())
        val agent =
            AIAgent(
                executor,
                AIAgentConfig(prompt("test") { system("test") }, model, 20),
                strategy,
                ToolRegistry { tool(LookupTool()) },
            ) {
                handleEvents { with(service) { sendToolEvents { events += it } } }
            }

        agent.run("question")

        // The round of each call, by its id, for the events of both types
        val rounds =
            events
                .map { Json.parseToJsonElement(it).jsonObject }
                .groupBy({ it["type"]!!.jsonPrimitive.content }) {
                    it["toolCallId"]!!.jsonPrimitive.content to it["round"]!!.jsonPrimitive.int
                }
                .mapValues { (_, calls) -> calls.toMap() }
        assertEquals(setOf("a", "b", "c"), rounds["tool_call"]?.keys)
        assertEquals(rounds["tool_call"], rounds["tool_result"], "a result has its call's round")
        val callRounds = rounds.getValue("tool_call")
        assertEquals(callRounds["a"], callRounds["b"])
        assertNotEquals(callRounds["a"], callRounds["c"])
    }
}
