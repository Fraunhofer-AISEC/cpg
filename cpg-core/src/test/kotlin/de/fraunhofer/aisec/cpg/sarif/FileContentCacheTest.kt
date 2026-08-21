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
package de.fraunhofer.aisec.cpg.sarif

import java.net.URI
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class FileContentCacheTest {
    private fun tempFileWithContent(content: String): URI {
        val file = Files.createTempFile("FileContentCacheTest", ".txt")
        Files.writeString(file, content)
        file.toFile().deleteOnExit()
        return file.toUri()
    }

    @Test
    fun testReturnsMatchingRange() {
        val uri = tempFileWithContent("int main() {\n    return 1;\n}\n")
        val location =
            PhysicalLocation(
                uri,
                Region(startLine = 2, startColumn = 5, endLine = 2, endColumn = 14),
            )

        val span = FileContentCache.rangeOf(location, "return 1;")

        assertNotNull(span)
        assertEquals("return 1;", span.materialize())
    }

    @Test
    fun testReturnsNullOnMismatch() {
        val uri = tempFileWithContent("int main() {\n    return 1;\n}\n")
        // A region that doesn't correspond to the given code (simulates a frontend whose
        // location/code disagree, e.g. due to byte- vs. UTF-16-indexed columns).
        val location =
            PhysicalLocation(
                uri,
                Region(startLine = 2, startColumn = 5, endLine = 2, endColumn = 14),
            )

        val span = FileContentCache.rangeOf(location, "something else")

        assertNull(span)
    }

    @Test
    fun testReturnsNullForNonexistentFile() {
        val uri = URI("file:///does/not/exist/${System.nanoTime()}.txt")
        val location =
            PhysicalLocation(
                uri,
                Region(startLine = 1, startColumn = 1, endLine = 1, endColumn = 1),
            )

        val span = FileContentCache.rangeOf(location, "x")

        assertNull(span)
    }

    @Test
    fun testSharesContentAcrossNodes() {
        val uri = tempFileWithContent("int a = 1;\nint b = 2;\n")
        val firstLocation =
            PhysicalLocation(
                uri,
                Region(startLine = 1, startColumn = 1, endLine = 1, endColumn = 11),
            )
        val secondLocation =
            PhysicalLocation(
                uri,
                Region(startLine = 2, startColumn = 1, endLine = 2, endColumn = 11),
            )

        val first = FileContentCache.rangeOf(firstLocation, "int a = 1;")
        val second = FileContentCache.rangeOf(secondLocation, "int b = 2;")

        assertNotNull(first)
        assertNotNull(second)
        // Both spans should share the exact same cached content instance.
        assertSame(first.content, second.content)
    }
}
