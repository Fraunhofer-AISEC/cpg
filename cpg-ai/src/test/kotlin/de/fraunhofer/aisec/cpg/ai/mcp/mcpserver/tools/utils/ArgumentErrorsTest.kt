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
package de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class ArgumentErrorsTest {
    private val tool = "cpg_add_llm_concept_and_operations"

    /**
     * What the tool returns for [json] given as arguments of a `cpg_add_llm_concept_and_operations`
     * call.
     */
    private fun messageFor(json: String): String {
        val arguments = Json.parseToJsonElement(json).jsonObject
        val cause =
            assertFailsWith<SerializationException> { arguments.toPayload<LLMConceptList>() }
        return describeInvalidArguments(
            tool,
            LLMConceptList.serializer().descriptor,
            arguments,
            cause,
        )
    }

    @Test
    fun everyProblemIsListedInOneMessage() {
        // The shape behind a 4-attempt chain in a real run: kotlinx reports only the first of
        // these.
        val message =
            messageFor(
                """
                {"concepts": [{
                  "name": "Decoding", "nodeId": "n1",
                  "properties": [{"name": "codec", "type": "String", "value": "FLAC"},
                                 {"name": "mode", "type": "String", "description": "d"}],
                  "operations": [{"name": "create", "description": "d", "nodeId": "n2",
                                  "properties": "[{\"name\": \"handle\"}]"}]
                }]}
                """
            )

        val expected =
            """
            Invalid arguments for $tool: 4 problem(s) - fix all of them, then call again.
            - concepts[0]: missing required field "description" (string)
            - concepts[0].properties[0]: missing required field "description" (string)
            - concepts[0].properties[1]: missing required field "value" (string)
            - concepts[0].operations[0].properties: expected array of objects, got a string: "[{\"name\": \"handle\"}]"
            Expected shape:
            - concepts[]: {"name": string, "description": string, "nodeId": string, "properties": array of objects, "operations": array of objects, "notes": string (optional)}
            - concepts[].properties[]: {"name": string, "type": string, "description": string, "value": string}
            - concepts[].operations[]: {"name": string, "description": string, "nodeId": string, "properties": array of objects, "notes": string (optional)}
            """
                .trimIndent()
        assertEquals(expected, message)
    }

    @Test
    fun nullAndWrongPrimitiveTypesAreReported() {
        val message =
            messageFor(
                """
                {"concepts": [{"name": "C", "description": null, "nodeId": 42,
                               "properties": [], "operations": []}]}
                """
            )

        assertTrue(
            "- concepts[0].description: must not be null, expected string" in message,
            message,
        )
        assertTrue("- concepts[0].nodeId: expected string, got a number: 42" in message, message)
    }

    @Test
    fun fieldsWithADefaultAreNotReportedAsMissing() {
        // LLMPropertyDescription.type and LLMOperationDescription.properties have defaults.
        val arguments =
            Json.parseToJsonElement(
                    """
                    {"name": "C", "properties": [{"name": "p", "description": "d"}],
                     "operations": [{"name": "op"}]}
                    """
                )
                .jsonObject
        val cause =
            assertFailsWith<SerializationException> { arguments.toPayload<LLMConceptDescription>() }

        val message =
            describeInvalidArguments(
                "cpg_add_or_update_llm_concept",
                LLMConceptDescription.serializer().descriptor,
                arguments,
                cause,
            )

        assertTrue(
            "- (arguments): missing required field \"description\" (string)" in message,
            message,
        )
        assertTrue(
            "- operations[0]: missing required field \"description\" (string)" in message,
            message,
        )
        assertFalse("\"type\" (string)" in message, message)
        assertFalse("missing required field \"properties\"" in message, message)
        assertTrue("\"properties\": array of objects (optional)" in message, message)
    }

    @Test
    fun missingArgumentsListTheRequiredTopLevelFields() {
        val cause =
            assertFailsWith<SerializationException> {
                (null as JsonObject?).toPayload<LLMConceptList>()
            }

        val message =
            describeInvalidArguments(tool, LLMConceptList.serializer().descriptor, null, cause)

        assertTrue(
            "- (arguments): missing required field \"concepts\" (array of objects)" in message,
            message,
        )
    }

    @Test
    fun anErrorTheWalkDoesNotModelFallsBackToTheCleanedDecoderMessage() {
        val cause =
            SerializationException(
                "Something odd for type with serial name 'de.fraunhofer.aisec.cpg.ai.mcp.mcpserver.tools.utils.LLMProperty' at path: \$.concepts\nJSON input: {...}"
            )
        val arguments = Json.parseToJsonElement("""{"concepts": []}""").jsonObject

        val message =
            describeInvalidArguments(tool, LLMConceptList.serializer().descriptor, arguments, cause)

        assertEquals(
            "Invalid arguments for $tool: Something odd for type with serial name 'LLMProperty' at path: \$.concepts",
            message,
        )
    }

    @Test
    fun theDecoderMessageIsKeptWhenItPointsSomewhereTheWalkDidNot() {
        val cause = SerializationException("Unexpected thing at path: \$.concepts[0].name")
        val arguments =
            Json.parseToJsonElement(
                    """{"concepts": [{"name": "C", "nodeId": "n", "properties": [], "operations": []}]}"""
                )
                .jsonObject

        val message =
            describeInvalidArguments(tool, LLMConceptList.serializer().descriptor, arguments, cause)

        assertTrue(
            "- concepts[0]: missing required field \"description\" (string)" in message,
            message,
        )
        assertTrue(
            "- concepts[0].name: Unexpected thing at path: \$.concepts[0].name" in message,
            message,
        )
    }
}
