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
package de.fraunhofer.aisec.cpg.frontends.experimental.rust

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.nio.file.Path
import kotlin.test.*

class RustEnumTest : BaseTest() {
    @Test
    fun testEnumDeclaration() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("enums.rs").toFile()), topLevel, true) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        // Color enum with unit variants
        val color = tu.records["Color"]
        assertNotNull(color, "Should find Color enum")
        assertIs<EnumDeclaration>(color)
        assertEquals("enum", color.kind)
        assertEquals(3, color.entries.size)
        assertEquals("Red", color.entries[0].name.localName)
        assertEquals("Green", color.entries[1].name.localName)
        assertEquals("Blue", color.entries[2].name.localName)

        // Shape enum with tuple variants
        val shape = tu.records["Shape"]
        assertNotNull(shape, "Should find Shape enum")
        assertIs<EnumDeclaration>(shape)
        assertEquals(2, shape.entries.size)
        assertEquals("Circle", shape.entries[0].name.localName)
        assertEquals("Rectangle", shape.entries[1].name.localName)

        // Message enum with mixed variants
        val message = tu.records["Message"]
        assertNotNull(message, "Should find Message enum")
        assertIs<EnumDeclaration>(message)
        assertEquals(3, message.entries.size)
        assertEquals("Quit", message.entries[0].name.localName)
        assertEquals("Move", message.entries[1].name.localName)
        assertEquals("Write", message.entries[2].name.localName)
    }
}
