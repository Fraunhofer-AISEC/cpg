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

import de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.UNPARSABLE_ARGUMENTS_KEY
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class FallbackToolCallTest {
    private val tools = setOf("cpg_get_node", "cpg_apply_concepts")

    private fun extract(text: String) = ChatService.extractFallbackToolCall(text, tools)

    private fun args(json: String) = Json.parseToJsonElement(json) as JsonObject

    @Test
    fun fencedObjectWithNameAndArguments() {
        val call =
            extract(
                "Let me look:\n```json\n{\"name\": \"cpg_get_node\", \"arguments\": {\"id\": \"1\"}}\n```"
            )

        assertNotNull(call)
        assertEquals("cpg_get_node", call.tool)
        assertEquals(args("""{"id": "1"}"""), call.argsJson)
    }

    @Test
    fun openAiFormatWithArgumentsAsJsonStringDoesNotThrow() {
        val call =
            extract(
                """
                ```json
                {"function": {"name": "cpg_get_node", "arguments": "{\"id\": \"1\"}"}}
                ```
                """
                    .trimIndent()
            )

        assertNotNull(call)
        assertEquals("cpg_get_node", call.tool)
        assertEquals(args("""{"id": "1"}"""), call.argsJson)
    }

    @Test
    fun functionKeyThatIsAPlainString() {
        val call =
            extract("""<tool_call>{"function": "cpg_get_node", "input": {"id": "7"}}</tool_call>""")

        assertNotNull(call)
        assertEquals("cpg_get_node", call.tool)
        assertEquals(args("""{"id": "7"}"""), call.argsJson)
    }

    @Test
    fun messageThatIsOnlyOneObject() {
        val call = extract("""  {"tool": "cpg_get_node", "parameters": {"id": "2"}}  """)

        assertNotNull(call)
        assertEquals("cpg_get_node", call.tool)
    }

    @Test
    fun anObjectInsideProseIsNotACall() {
        // The model explains how to call a tool; that must not run it.
        val text =
            """To fetch a node you would send {"name": "cpg_get_node", "arguments": {"id": "1"}} and read the result."""

        assertNull(extract(text))
    }

    @Test
    fun unknownToolNamesAreIgnored() {
        assertNull(extract("```json\n{\"name\": \"rm_rf\", \"arguments\": {}}\n```"))
    }

    @Test
    fun missingArgumentsBecomeAnEmptyObject() {
        val call = extract("""{"name": "cpg_get_node"}""")

        assertNotNull(call)
        assertEquals(JsonObject(emptyMap()), call.argsJson)
    }

    @Test
    fun argumentsThatAreNotJsonAreHandedToTheToolAsUnparsable() {
        val call = extract("""{"name": "cpg_get_node", "arguments": "id=1"}""")

        assertNotNull(call)
        assertEquals("id=1", call.argsJson[UNPARSABLE_ARGUMENTS_KEY]?.jsonPrimitive?.content)
    }

    @Test
    fun bracesInsideStringsDoNotBreakTheObject() {
        val call =
            extract(
                "```json\n" +
                    """{"name": "cpg_get_node", "arguments": {"code": "if (x) { y(); } else {"}}""" +
                    "\n```"
            )

        assertNotNull(call)
        assertEquals("if (x) { y(); } else {", call.argsJson["code"]?.jsonPrimitive?.content)
    }

    @Test
    fun findJsonObjectsIgnoresBracesInStringsAndEscapedQuotes() {
        val objects = ChatService.findJsonObjects("""a {"s": "}\"{"} b {"t": 1}""")

        assertEquals(listOf("""{"s": "}\"{"}""", """{"t": 1}"""), objects)
    }

    @Test
    fun plainAnswersAreNotCalls() {
        assertNull(extract("All functions have been tagged."))
        assertNull(extract("```kotlin\nfun main() {}\n```"))
    }
}
