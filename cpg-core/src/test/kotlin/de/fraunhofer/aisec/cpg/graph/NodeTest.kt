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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.sarif.CodeSpan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NodeTest {
    @Test
    fun testId() {
        with(TestLanguageFrontend()) {
            val node1 = newLiteral(1)
            val node2 = newLiteral(2)

            // Check that the IDs are unique
            assert(node1.id != node2.id) { "Node IDs should be unique" }
        }
    }

    @Test
    fun testCodeDefaultsToNull() {
        val node = with(TestLanguageFrontend()) { newLiteral(1) }

        assertNull(node.code)
    }

    @Test
    fun testCodeLiteralRoundTrip() {
        val node = with(TestLanguageFrontend()) { newLiteral(1) }

        node.code = "1"

        assertEquals("1", node.code)
    }

    @Test
    fun testCodeInternedSpanRoundTrip() {
        val node = with(TestLanguageFrontend()) { newLiteral(1) }
        val content = "int a = 1;"

        node.setCodeSpan(CodeSpan(content, 8, 9))

        assertEquals("1", node.code)
    }

    @Test
    fun testSettingLiteralCodeAfterSpanOverridesSpan() {
        val node = with(TestLanguageFrontend()) { newLiteral(1) }
        node.setCodeSpan(CodeSpan("int a = 1;", 8, 9))

        node.code = "2"

        assertEquals("2", node.code)
    }

    @Test
    fun testSettingSpanAfterLiteralCodeOverridesLiteral() {
        val node = with(TestLanguageFrontend()) { newLiteral(1) }
        node.code = "unrelated"

        node.setCodeSpan(CodeSpan("int a = 1;", 8, 9))

        assertEquals("1", node.code)
    }

    @Test
    fun testIsCodeInterned() {
        val node = with(TestLanguageFrontend()) { newLiteral(1) }
        assertFalse(node.isCodeInterned)

        node.setCodeSpan(CodeSpan("int a = 1;", 8, 9))
        assertTrue(node.isCodeInterned)

        node.code = "1"
        assertFalse(node.isCodeInterned)
    }
}
