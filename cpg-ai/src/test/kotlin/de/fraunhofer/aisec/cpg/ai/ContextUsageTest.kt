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

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.tokenizer.SimpleRegexBasedTokenizer
import de.fraunhofer.aisec.cpg.ai.skills.READ_FILE_TOOL_NAME
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContextUsageTest {
    private val catalog =
        "The following skills provide specialized instructions: analysis-patterns."

    private val prompt =
        prompt("test") {
            system("You are a code analysis agent.\n\n$catalog")
            user("Which functions handle external input?")
            toolCall(
                READ_FILE_TOOL_NAME,
                """{"path": ".agents/skills/analysis-patterns/SKILL.md"}""",
            )
            toolResult(READ_FILE_TOOL_NAME, "Follow the data from its source to its sink.")
            toolCall("cpg_list_calls", """{"projectName": "wifi_demo"}""")
            toolResult("cpg_list_calls", "net_send, net_open, get_key, printf, socket, send")
            assistant("Only networking is external input.")
        }

    private val tools =
        listOf(
            ToolDescriptor("cpg_list_calls", "Lists the calls of the analyzed code."),
            ToolDescriptor(READ_FILE_TOOL_NAME, "Reads a file."),
        )

    @Test
    fun splitsTheContextIntoItsParts() {
        val usage = contextUsage(prompt, tools, catalog, null, 1000, SimpleRegexBasedTokenizer())

        assertTrue(usage.estimated)
        assertEquals(1000, usage.windowTokens)
        val names = usage.categories.map { it.name }
        assertEquals(
            listOf("System prompt", "Tool definitions", "Skills", "Tool results", "Conversation"),
            names,
        )
        assertEquals(usage.usedTokens, usage.categories.sumOf { it.tokens })
    }

    @Test
    fun scalesTheEstimateToTheReportedTokens() {
        val usage = contextUsage(prompt, tools, catalog, 5000, 128_000, SimpleRegexBasedTokenizer())

        assertFalse(usage.estimated)
        assertEquals(5000, usage.usedTokens)
        // Rounding down each part may lose a token per part, but no more
        val sum = usage.categories.sumOf { it.tokens }
        assertTrue(sum in 5000 - usage.categories.size..5000, "The parts sum up to $sum")
    }
}
