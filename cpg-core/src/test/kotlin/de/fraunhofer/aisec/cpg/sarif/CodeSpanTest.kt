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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class CodeSpanTest {
    private fun locationWithContent(content: String, region: Region): PhysicalLocation {
        val location = PhysicalLocation(URI("file:///${System.nanoTime()}.txt"), region)
        location.artifactLocation.indexedContent = IndexedContent(content)
        return location
    }

    @Test
    fun testReturnsMatchingRange() {
        val location =
            locationWithContent(
                "int main() {\n    return 1;\n}\n",
                Region(startLine = 2, startColumn = 5, endLine = 2, endColumn = 14),
            )

        val span = tryInternCode(location, "return 1;")

        assertNotNull(span)
        assertEquals("return 1;", span.materialize())
    }

    @Test
    fun testReturnsNullOnMismatch() {
        val location =
            locationWithContent(
                "int main() {\n    return 1;\n}\n",
                Region(startLine = 2, startColumn = 5, endLine = 2, endColumn = 14),
            )

        val span = tryInternCode(location, "something else")

        assertNull(span)
    }

    @Test
    fun testReturnsNullWhenNoContentRegisteredYet() {
        // No indexedContent set on this location's ArtifactLocation.
        val location =
            PhysicalLocation(
                URI("file:///${System.nanoTime()}-unregistered.txt"),
                Region(startLine = 1, startColumn = 1, endLine = 1, endColumn = 1),
            )

        val span = tryInternCode(location, "x")

        assertNull(span)
    }

    @Test
    fun testSharesContentAcrossNodes() {
        val uri = URI("file:///${System.nanoTime()}-shared.txt")
        val content = "int a = 1;\nint b = 2;\n"
        val firstLocation = PhysicalLocation(uri, Region(1, 1, 1, 11))
        firstLocation.artifactLocation.indexedContent = IndexedContent(content)
        val secondLocation = PhysicalLocation(uri, Region(2, 1, 2, 11))

        val first = tryInternCode(firstLocation, "int a = 1;")
        val second = tryInternCode(secondLocation, "int b = 2;")

        assertNotNull(first)
        assertNotNull(second)
        // Both spans should share the exact same content instance, since firstLocation and
        // secondLocation's ArtifactLocations are the same interned instance (same URI).
        assertSame(first.content, second.content)
    }
}
