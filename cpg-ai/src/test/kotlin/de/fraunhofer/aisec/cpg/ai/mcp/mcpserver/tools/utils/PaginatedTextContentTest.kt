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
import kotlin.test.assertTrue

class PaginatedTextContentTest {

    @Test
    fun defaultsToFullListWhenUnderTheLimit() {
        val texts = listOf("a", "b", "c")

        val result = paginatedTextContent(texts, CpgListPayload())

        assertEquals(listOf("a", "b", "c"), result.map { it.text })
    }

    @Test
    fun defaultLimitCapsAtTwentyAndAddsSummary() {
        val texts = (1..30).map { "item$it" }

        val result = paginatedTextContent(texts, CpgListPayload())

        assertEquals(21, result.size, "20 items + 1 summary entry")
        assertEquals("item1", result.first().text)
        assertEquals("item20", result[19].text)
        assertTrue(result.last().text.contains("offset=20"))
    }

    @Test
    fun explicitLimitAndOffsetPaginate() {
        val texts = (1..10).map { "item$it" }

        val result = paginatedTextContent(texts, CpgListPayload(limit = 3, offset = 4))

        assertEquals(listOf("item5", "item6", "item7"), result.take(3).map { it.text })
        assertTrue(result.last().text.contains("offset=7"))
    }

    @Test
    fun noSummaryWhenPageReachesTheEnd() {
        val texts = listOf("a", "b", "c")

        val result = paginatedTextContent(texts, CpgListPayload(limit = 10, offset = 0))

        assertEquals(listOf("a", "b", "c"), result.map { it.text })
    }

    @Test
    fun negativeOffsetAndZeroLimitAreClamped() {
        val texts = listOf("a", "b", "c")

        val result = paginatedTextContent(texts, CpgListPayload(limit = 0, offset = -5))

        assertEquals("a", result.first().text, "limit clamped up to 1, offset up to 0")
        assertTrue(result.last().text.contains("offset=1"))
    }
}
