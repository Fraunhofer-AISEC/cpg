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

import de.fraunhofer.aisec.codyze.console.ai.clients.Events
import de.fraunhofer.aisec.codyze.console.ai.clients.LlmClient
import de.fraunhofer.aisec.codyze.console.ai.clients.ToolCall
import de.fraunhofer.aisec.codyze.console.ai.clients.ToolCallWithResult
import io.ktor.client.*
import io.modelcontextprotocol.kotlin.sdk.types.Tool
import kotlin.test.*
import kotlinx.serialization.json.*

class ChatServiceTest {

    private fun createChatService(llm: LlmClient): ChatService {
        return ChatService(httpClient = HttpClient(), llm = llm, mcpServerUrl = "localhost")
    }

    @Test
    fun parseToolResultContentEmptyTest() {
        val service = createChatService(TestLlmClient())
        val result = service.parseToolResultContent(emptyList())
        assertIs<JsonArray>(result)
        assertEquals(0, result.size)
    }

    @Test
    fun parseToolResultContentSinglePlainTextTest() {
        val service = createChatService(TestLlmClient())
        val result = service.parseToolResultContent(listOf("hello world"))
        assertIs<JsonPrimitive>(result)
        assertEquals("hello world", result.content)
    }

    @Test
    fun parseToolResultContentSingleJsonObjectTest() {
        val service = createChatService(TestLlmClient())
        val result = service.parseToolResultContent(listOf("""{"key": "value"}"""))
        assertIs<JsonObject>(result)
        assertEquals(JsonPrimitive("value"), result["key"])
    }

    @Test
    fun parseToolResultContentSingleJsonArrayTest() {
        val service = createChatService(TestLlmClient())
        val result = service.parseToolResultContent(listOf("""[1, 2, 3]"""))
        assertIs<JsonArray>(result)
        assertEquals(3, result.size)
    }

    @Test
    fun parseToolResultContentMultipleItemsTest() {
        val service = createChatService(TestLlmClient())
        val result = service.parseToolResultContent(listOf("""{"a": 1}""", "plain text", """[1]"""))
        assertIs<JsonArray>(result)
        assertEquals(3, result.size)
        assertIs<JsonObject>(result[0])
        assertIs<JsonPrimitive>(result[1])
        assertIs<JsonArray>(result[2])
    }

    @Test
    fun parseToolResultContentInvalidJsonFallsBackToPrimitiveTest() {
        val service = createChatService(TestLlmClient())
        val result = service.parseToolResultContent(listOf("{invalid json"))
        assertIs<JsonPrimitive>(result)
        assertEquals("{invalid json", result.content)
    }

    @Test
    fun eventsTextTest() {
        val event = Events.text("hello")
        val json = Json.parseToJsonElement(event).jsonObject
        assertEquals("text", json["type"]?.jsonPrimitive?.content)
        assertEquals("hello", json["content"]?.jsonPrimitive?.content)
    }

    @Test
    fun eventsReasoningTest() {
        val event = Events.reasoning("thinking...")
        val json = Json.parseToJsonElement(event).jsonObject
        assertEquals("reasoning", json["type"]?.jsonPrimitive?.content)
        assertEquals("thinking...", json["content"]?.jsonPrimitive?.content)
    }

    @Test
    fun eventsKeepaliveTest() {
        val event = Events.keepalive()
        val json = Json.parseToJsonElement(event).jsonObject
        assertEquals("keepalive", json["type"]?.jsonPrimitive?.content)
    }

    @Test
    fun eventsToolResultTest() {
        val content = buildJsonObject { put("result", "data") }
        val event = Events.toolResult("my_tool", content)
        val json = Json.parseToJsonElement(event).jsonObject
        assertEquals("tool_result", json["type"]?.jsonPrimitive?.content)
        assertEquals("my_tool", json["toolName"]?.jsonPrimitive?.content)
        assertEquals("data", json["content"]?.jsonObject?.get("result")?.jsonPrimitive?.content)
    }

    @Test
    fun eventsToolResultWithEmptyArrayTest() {
        val content = JsonArray(emptyList())
        val event = Events.toolResult("empty_tool", content)
        val json = Json.parseToJsonElement(event).jsonObject
        assertEquals("tool_result", json["type"]?.jsonPrimitive?.content)
        assertIs<JsonArray>(json["content"])
        assertEquals(0, json["content"]?.jsonArray?.size)
    }
}

class TestLlmClient(private val responses: MutableList<List<ToolCall>> = mutableListOf()) :
    LlmClient {
    override val modelName = "test-model"

    val receivedMessages = mutableListOf<String>()

    override suspend fun sendPrompt(
        userMessage: String,
        conversationHistory: List<ChatMessageJSON>,
        tools: List<Tool>,
        toolCallHistory: List<List<ToolCallWithResult>>?,
        onText: suspend (String) -> Unit,
        onReasoning: suspend (String) -> Unit,
    ): List<ToolCall> {
        receivedMessages.add(userMessage)
        return if (responses.isNotEmpty()) {
            responses.removeFirst()
        } else {
            onText("Final answer from LLM")
            emptyList()
        }
    }
}
