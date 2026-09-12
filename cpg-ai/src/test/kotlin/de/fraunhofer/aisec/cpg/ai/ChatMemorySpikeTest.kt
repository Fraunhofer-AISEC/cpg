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

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.agents.chatMemory.feature.ChatMemory
import ai.koog.agents.chatMemory.feature.ChatMemoryPreProcessor
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.HistoryCompressionStrategy
import ai.koog.agents.core.dsl.extension.ReceivedToolResults
import ai.koog.agents.core.dsl.extension.nodeExecuteTools
import ai.koog.agents.core.dsl.extension.nodeLLMCompressHistory
import ai.koog.agents.core.dsl.extension.nodeLLMRequest
import ai.koog.agents.core.dsl.extension.nodeLLMSendToolResults
import ai.koog.agents.core.dsl.extension.onTextMessage
import ai.koog.agents.core.dsl.extension.onToolCalls
import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolRegistry
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
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

/**
 * Phase 0 spike test for the ChatMemory migration.
 *
 * Verifies that Koog's ChatMemory feature preserves tool-call/tool-result messages across
 * agent.run() call boundaries (the root cause of the 32-35x activate_skill bug), that the system
 * message is not duplicated, that history compression and windowSize work as expected, and that
 * evict() clears the session.
 *
 * This is a HARD GATE: assertions #1, #2, #4, #6 must pass before any migration code.
 */
class ChatMemorySpikeTest {

    /** A ChatHistoryProvider that records all store/load calls and supports evict(). */
    private class RecordingChatHistoryProvider : ChatHistoryProvider {
        val storeCalls = mutableListOf<Pair<String, List<Message>>>()
        val loadCalls = mutableListOf<String>()
        private val storage = mutableMapOf<String, List<Message>>()

        override suspend fun store(sessionId: String, messages: List<Message>) {
            storeCalls.add(sessionId to messages)
            storage[sessionId] = messages
        }

        override suspend fun load(sessionId: String): List<Message> {
            loadCalls.add(sessionId)
            return storage[sessionId] ?: emptyList()
        }

        fun evict(sessionId: String) {
            storage.remove(sessionId)
        }

        fun stored(sessionId: String): List<Message> = storage[sessionId] ?: emptyList()
    }

    /**
     * A mock PromptExecutor that returns canned Message.Assistant responses from a queue, and
     * records every prompt it receives so the test can inspect the full prompt (including any
     * ChatMemory-loaded history) at each LLM call.
     */
    private class MockPromptExecutor(private val responses: List<Message.Assistant>) :
        PromptExecutor() {

        private val responseIndex = AtomicInteger(0)
        val receivedPrompts = mutableListOf<ai.koog.prompt.Prompt>()

        override suspend fun execute(
            prompt: ai.koog.prompt.Prompt,
            model: LLModel,
            tools: List<ToolDescriptor>,
        ): Message.Assistant {
            receivedPrompts.add(prompt)
            val idx = responseIndex.getAndIncrement()
            return responses.getOrElse(idx) {
                Message.Assistant(listOf(MessagePart.Text("done")), ResponseMetaInfo.Empty)
            }
        }

        override fun executeStreaming(
            prompt: ai.koog.prompt.Prompt,
            model: LLModel,
            tools: List<ToolDescriptor>,
        ): Flow<StreamFrame> = flowOf()

        override suspend fun moderate(
            prompt: ai.koog.prompt.Prompt,
            model: LLModel,
        ): ModerationResult = ModerationResult(false, emptyMap())

        override suspend fun models(): List<LLModel> = listOf(mockModel)

        override fun close() {}

        companion object {
            val mockModel = LLModel(provider = LLMProvider("test", "Test"), id = "test-model")
        }
    }

    /** A simple lookup tool for testing. */
    private class LookupTool :
        SimpleTool<Map<String, String>>(
            typeToken<Map<String, String>>(),
            "lookup",
            "A dummy lookup tool for testing",
        ) {
        override suspend fun execute(args: Map<String, String>): String = "lookup-result"
    }

    private val toolRegistry = ToolRegistry { tool(LookupTool()) }

    private fun assistantToolCall(tool: String, args: String): Message.Assistant =
        Message.Assistant(
            listOf(MessagePart.Tool.Call("call-${System.nanoTime()}", tool, args)),
            ResponseMetaInfo.Empty,
        )

    private fun assistantText(text: String): Message.Assistant =
        Message.Assistant(listOf(MessagePart.Text(text)), ResponseMetaInfo.Empty)

    /** Build the chat strategy without compression. */
    private fun buildStrategy() =
        strategy<String, String>("spike-chat-memory") {
            val requestLlm by nodeLLMRequest("requestLlm")
            val executeTool by nodeExecuteTools("executeTool")
            val sendToolResult by nodeLLMSendToolResults("sendToolResult")

            edge(nodeStart forwardTo requestLlm)
            edge(requestLlm forwardTo executeTool onToolCalls { true })
            edge(requestLlm forwardTo nodeFinish onTextMessage { true })
            edge(executeTool forwardTo sendToolResult)
            edge(sendToolResult forwardTo executeTool onToolCalls { true })
            edge(sendToolResult forwardTo nodeFinish onTextMessage { true })
        }

    /** Build the chat strategy with history compression after tool execution. */
    private fun buildStrategyWithCompression() =
        strategy<String, String>("spike-chat-memory-compress") {
            val requestLlm by nodeLLMRequest("requestLlm")
            val executeTool by nodeExecuteTools("executeTool")
            val compressHistory by
                nodeLLMCompressHistory<ReceivedToolResults>(
                    "compressHistory",
                    HistoryCompressionStrategy.FromLastNMessages(2),
                    MockPromptExecutor.mockModel,
                )
            val sendToolResult by nodeLLMSendToolResults("sendToolResult")

            edge(nodeStart forwardTo requestLlm)
            edge(requestLlm forwardTo executeTool onToolCalls { true })
            edge(requestLlm forwardTo nodeFinish onTextMessage { true })
            edge(executeTool forwardTo compressHistory)
            edge(compressHistory forwardTo sendToolResult)
            edge(sendToolResult forwardTo executeTool onToolCalls { true })
            edge(sendToolResult forwardTo nodeFinish onTextMessage { true })
        }

    private fun buildAgent(
        executor: MockPromptExecutor,
        strategy: ai.koog.agents.core.agent.entity.AIAgentGraphStrategy<String, String>,
        provider: RecordingChatHistoryProvider,
        windowSize: Int? = null,
        preProcessor: ChatMemoryPreProcessor? = null,
    ): AIAgent<String, String> =
        AIAgent(
            executor,
            AIAgentConfig(
                prompt("test") { system("You are a test assistant.") },
                MockPromptExecutor.mockModel,
                100,
            ),
            strategy,
            toolRegistry,
        ) {
            install(ChatMemory.Feature) {
                chatHistoryProvider(provider)
                if (windowSize != null) {
                    windowSize(windowSize)
                }
                if (preProcessor != null) {
                    addPreProcessor(preProcessor)
                }
            }
        }

    /**
     * Assertion #1 (HARD GATE): Tool-call and tool-result messages survive across agent.run() call
     * boundaries. This is the core bug - the old toChatMessageJsonOrNull() drops them.
     *
     * Assertion #2 (HARD GATE): The system message is not duplicated across calls.
     */
    @Test
    fun toolCallAndToolResultSurviveAcrossCallBoundary() = runTest {
        val provider = RecordingChatHistoryProvider()
        val sessionId = "session-1"

        val executor =
            MockPromptExecutor(
                listOf(
                    // Run 1, first LLM call: a tool call
                    assistantToolCall("lookup", """{"query":"foo"}"""),
                    // Run 1, second LLM call (after tool result): text response
                    assistantText("done"),
                    // Run 2, first LLM call: text response
                    assistantText("done"),
                )
            )

        val agent = buildAgent(executor, buildStrategy(), provider)
        agent.run("query1", sessionId)

        // Verify provider stored something after run 1
        assertTrue(provider.storeCalls.isNotEmpty(), "Provider should have stored after run 1")
        val storedAfterRun1 = provider.stored(sessionId)
        assertTrue(storedAfterRun1.isNotEmpty(), "Stored history after run 1 should not be empty")

        // Assertion #1: stored history must contain a tool-call message
        val hasToolCall =
            storedAfterRun1.any { msg ->
                msg is Message.Assistant && msg.parts.any { it is MessagePart.Tool.Call }
            }
        assertTrue(hasToolCall, "Stored history must contain a tool-call message (assertion #1)")

        // Assertion #1: stored history must contain a tool-result message (in a Message.User)
        val hasToolResult =
            storedAfterRun1.any { msg ->
                msg is Message.User && msg.parts.any { it is MessagePart.Tool.Result }
            }
        assertTrue(
            hasToolResult,
            "Stored history must contain a tool-result message (assertion #1)",
        )

        // Run 2: input "query2", same sessionId
        agent.run("query2", sessionId)

        // The second run's first LLM call must have received the tool-call and tool-result
        // from run 1 in its prompt (loaded by ChatMemory).
        assertTrue(
            executor.receivedPrompts.size >= 3,
            "Expected at least 3 LLM calls (2 from run 1, 1 from run 2), got ${executor.receivedPrompts.size}",
        )
        val run2FirstPrompt = executor.receivedPrompts[2]

        val run2HasToolCall =
            run2FirstPrompt.messages.any { msg ->
                msg is Message.Assistant && msg.parts.any { it is MessagePart.Tool.Call }
            }
        assertTrue(
            run2HasToolCall,
            "Run 2's prompt must contain the tool-call from run 1 (loaded by ChatMemory) - this is the core bug fix",
        )

        val run2HasToolResult =
            run2FirstPrompt.messages.any { msg ->
                msg is Message.User && msg.parts.any { it is MessagePart.Tool.Result }
            }
        assertTrue(
            run2HasToolResult,
            "Run 2's prompt must contain the tool-result from run 1 (loaded by ChatMemory)",
        )

        // Assertion #2 (HARD GATE): system message not duplicated across calls.
        val systemCount = run2FirstPrompt.messages.count { it is Message.System }
        assertTrue(
            systemCount <= 1,
            "Run 2's prompt should have at most 1 system message (not duplicated), got $systemCount",
        )
    }

    /** Assertion #3: History compression (FromLastNMessages) runs and reduces the message count. */
    @Test
    fun compressionReducesMessageCount() = runTest {
        val provider = RecordingChatHistoryProvider()
        val sessionId = "session-comp"

        // Build a sequence of responses: many tool calls, then a final text response.
        val responses = mutableListOf<Message.Assistant>()
        repeat(10) { responses.add(assistantToolCall("lookup", """{"query":"item-$it"}""")) }
        responses.add(assistantText("done"))

        val executor = MockPromptExecutor(responses)
        val agent = buildAgent(executor, buildStrategyWithCompression(), provider)

        agent.run("start", sessionId)

        // Compression (FromLastNMessages(2)) should have kept history small.
        val stored = provider.stored(sessionId)
        assertTrue(stored.isNotEmpty(), "Should have stored history after compression run")
        assertTrue(
            stored.size < 20,
            "Compressed history should be small (FromLastNMessages(2)), got ${stored.size} messages",
        )
    }

    /** Assertion #4 (HARD GATE): windowSize safety floor limits the number of messages loaded. */
    @Test
    fun windowSizeLimitsLoadedMessages() = runTest {
        val provider = RecordingChatHistoryProvider()
        val sessionId = "session-window"

        // Run 1: produce a long history (many tool calls + final text).
        val responses = mutableListOf<Message.Assistant>()
        repeat(8) { responses.add(assistantToolCall("lookup", """{"q":"$it"}""")) }
        responses.add(assistantText("done"))

        val executor = MockPromptExecutor(responses)

        // windowSize = 4: ChatMemory should only load the last 4 messages on the next run.
        val agent = buildAgent(executor, buildStrategy(), provider, windowSize = 4)
        agent.run("first", sessionId)

        val storedAfterRun1 = provider.stored(sessionId)
        assertTrue(
            storedAfterRun1.size <= 4,
            "Run 1 stored history should be trimmed to <= windowSize=4 by WindowSizePreProcessor on store, got ${storedAfterRun1.size}",
        )

        // Now do run 2 with a fresh executor so we can inspect the loaded prompt.
        val executor2 = MockPromptExecutor(listOf(assistantText("done")))
        val agent2 = buildAgent(executor2, buildStrategy(), provider, windowSize = 4)
        agent2.run("second", sessionId)

        assertTrue(
            provider.loadCalls.contains(sessionId),
            "Run 2 should have triggered a history load for session $sessionId",
        )

        // The first (and only) LLM call in run 2 should have loaded at most 4 history messages
        // (windowSize=4), plus the caller's system + user message.
        val run2Prompt = executor2.receivedPrompts[0]
        assertTrue(
            run2Prompt.messages.size <= 6,
            "Run 2 prompt should have at most 6 messages (system + user + <=4 windowed), got ${run2Prompt.messages.size}",
        )
    }

    /** Assertion #5: evict() clears the session, so the next run starts with no loaded history. */
    @Test
    fun evictClearsSession() = runTest {
        val provider = RecordingChatHistoryProvider()
        val sessionId = "session-evict"

        val executor = MockPromptExecutor(listOf(assistantText("done")))
        val agent = buildAgent(executor, buildStrategy(), provider)

        // Run 1: stores history
        agent.run("first", sessionId)
        assertTrue(provider.stored(sessionId).isNotEmpty(), "Should have history after run 1")

        // Evict
        provider.evict(sessionId)
        assertTrue(provider.stored(sessionId).isEmpty(), "History should be empty after evict")

        // Run 2: should start with no loaded history
        val executor2 = MockPromptExecutor(listOf(assistantText("done")))
        val agent2 = buildAgent(executor2, buildStrategy(), provider)
        agent2.run("second", sessionId)

        // After evict + run 2, the run 2 prompt should only have the caller's messages
        // (system + user), no loaded history from run 1.
        val run2Prompt = executor2.receivedPrompts[0]
        assertEquals(
            2,
            run2Prompt.messages.size,
            "After evict, run 2 should start with only caller's system + user message (no loaded history), got ${run2Prompt.messages.size}",
        )
    }

    /**
     * Assertion #6 (HARD GATE): prompt.messages.size is observable/loggable per turn (the
     * pre-processor runs and records message sizes, confirming the plumbing works for per-turn
     * logging in the real ChatService).
     */
    @Test
    fun promptMessagesSizeObservablePerTurn() = runTest {
        val provider = RecordingChatHistoryProvider()
        val sessionId = "session-observe"
        val observedSizes = mutableListOf<Int>()

        val preProcessor =
            object : ChatMemoryPreProcessor {
                override fun preprocess(messages: List<Message>): List<Message> {
                    observedSizes.add(messages.size)
                    return messages
                }
            }

        val executor =
            MockPromptExecutor(
                listOf(assistantToolCall("lookup", """{"q":"1"}"""), assistantText("done"))
            )

        val agent = buildAgent(executor, buildStrategy(), provider, preProcessor = preProcessor)
        agent.run("query", sessionId)

        // The pre-processor runs on load (start of run) and/or store (end of run).
        assertTrue(
            observedSizes.isNotEmpty(),
            "Pre-processor should have been called, recording prompt.messages.size",
        )
        assertTrue(observedSizes.any { it > 0 }, "Observed message sizes should be non-zero")
    }
}
