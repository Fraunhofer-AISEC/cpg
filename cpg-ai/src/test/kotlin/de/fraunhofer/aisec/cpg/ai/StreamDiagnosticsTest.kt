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

import ai.koog.prompt.streaming.StreamFrame
import ai.koog.prompt.streaming.toMessageResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.serialization.SerializationException

class StreamDiagnosticsTest {

    @Test
    fun aToolCallWithEmptyArgumentsIsAssembledWithAnEmptyObject() {
        // Assembled as is, this fails exactly like in log_review_fixes ("Cannot read Json element
        // because of unexpected end of the input"), ending the agent run.
        val frames =
            listOf(StreamFrame.ToolCallComplete("call-1", "cpg_persist_to_neo4j", "", null))
        assertFailsWith<SerializationException> { frames.toMessageResponse() }

        val message = frames.assembleStreamedAnswer("test")

        assertTrue("cpg_persist_to_neo4j" in message.toString(), message.toString())
    }

    @Test
    fun argumentsThatAreNotJsonStillFailAndAreRethrown() {
        val frames =
            listOf(StreamFrame.ToolCallComplete("call-1", "cpg_get_node", """{"id":""", null))

        val e = assertFailsWith<SerializationException> { frames.assembleStreamedAnswer("test") }
        assertTrue("id" in describeFrames(frames), e.message)
    }

    @Test
    fun theDescriptionShowsToolCallsInFullAndCountsTheRest() {
        val frames =
            listOf(
                StreamFrame.TextDelta("thinking about it"),
                StreamFrame.TextDelta(" more"),
                StreamFrame.ToolCallComplete("call-1", "cpg_get_node", "", null),
                StreamFrame.ToolCallComplete(
                    "call-2",
                    "cpg_list_functions",
                    """{"limit":20}""",
                    null,
                ),
            )

        assertEquals(
            "counts={TextDelta=2, ToolCallComplete=2}, tool calls=[" +
                "complete(id=call-1, name=cpg_get_node, arguments=\"\" (0 chars)), " +
                "complete(id=call-2, name=cpg_list_functions, arguments=\"{\\\"limit\\\":20}\" (12 chars))]",
            describeFrames(frames),
        )
    }

    @Test
    fun aWellFormedToolCallIsAssembledAsBefore() {
        val frames =
            listOf(StreamFrame.ToolCallComplete("call-1", "cpg_get_node", """{"id":"n1"}""", null))

        val message = frames.assembleStreamedAnswer("test")

        assertTrue("cpg_get_node" in message.toString(), message.toString())
    }
}
