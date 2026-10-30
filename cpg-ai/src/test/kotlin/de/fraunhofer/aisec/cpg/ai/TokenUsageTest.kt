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
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeLLMRequestStreaming
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.Prompt
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.prompt.streaming.StreamFrame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

class TokenUsageTest {

    private val model = LLModel(provider = LLMProvider("test", "Test"), id = "test-model")

    private fun usage(input: Int, output: Int) =
        ResponseMetaInfo.Empty.copy(
            inputTokensCount = input,
            outputTokensCount = output,
            totalTokensCount = input + output,
        )

    /**
     * Streams one text answer, and answers every non-streamed call with prose, as a model does that
     * ignores the requested response format.
     */
    private inner class ProseExecutor : PromptExecutor() {
        override fun executeStreaming(
            prompt: Prompt,
            model: LLModel,
            tools: List<ToolDescriptor>,
        ): Flow<StreamFrame> =
            flowOf(StreamFrame.TextDelta("Working on it."), StreamFrame.End("stop", usage(100, 10)))

        override suspend fun execute(
            prompt: Prompt,
            model: LLModel,
            tools: List<ToolDescriptor>,
        ): Message.Assistant =
            Message.Assistant(listOf(MessagePart.Text("| function | done |")), usage(200, 20))

        override suspend fun moderate(prompt: Prompt, model: LLModel): ModerationResult =
            ModerationResult(false, emptyMap())

        override suspend fun models(): List<LLModel> = listOf(this@TokenUsageTest.model)

        override fun close() {}
    }

    @Test
    fun streamedAndUndecodableStructuredResponsesAreCountedOnce() = runTest {
        val usage = TokenUsage()
        val strategy =
            strategy<String, Boolean>("usage") {
                val request by nodeLLMRequestStreaming()
                val collect by
                    node<Flow<StreamFrame>, String>("collect") { frames ->
                        frames.toList()
                        ""
                    }
                val requestStatus by
                    node<String, Boolean>("requestStatus") { _ ->
                        llm.writeSession {
                            appendPrompt { user("Are you done?") }
                            runCatching { requestLLMStructured<TaskStatus>().getOrThrow() }
                                .isSuccess
                        }
                    }
                edge(nodeStart forwardTo request)
                edge(request forwardTo collect)
                edge(collect forwardTo requestStatus)
                edge(requestStatus forwardTo nodeFinish)
            }
        val agent =
            AIAgent(
                ProseExecutor(),
                AIAgentConfig(prompt("test") { system("You are a test assistant.") }, model, 10),
                strategy,
                ToolRegistry.EMPTY,
            ) {
                handleEvents { countTokenUsage(usage) }
            }

        val statusDecoded = agent.run("start")

        assertFalse(statusDecoded, "The prose answer must not decode as a TaskStatus")
        assertEquals(300, usage.input)
        assertEquals(30, usage.output)
        assertEquals(330, usage.total)
    }

    @Test
    fun missingCountsAddNothing() {
        val usage = TokenUsage()
        usage.add(usage(5, 1))
        usage.add(ResponseMetaInfo.Empty)
        assertEquals(5, usage.input)
        assertEquals(1, usage.output)
        assertEquals(6, usage.total)
    }
}
