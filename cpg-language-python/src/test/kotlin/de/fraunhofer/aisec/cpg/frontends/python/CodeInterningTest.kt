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
package de.fraunhofer.aisec.cpg.frontends.python

import de.fraunhofer.aisec.cpg.graph.nodes
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies that `Node.code` interning (always on, see
 * `de.fraunhofer.aisec.cpg.sarif.tryInternCode`) is engaging for this frontend, and that every
 * interned node's `code` is byte-for-byte identical to an independently computed substring of the
 * raw file -- a cross-check using a completely different code path (this test's own line/column
 * math) than the production interning machinery. Nodes that fall back to a literal (e.g. because
 * Python's own `codeOf` trims leading whitespace) are simply skipped here, not asserted on.
 */
class CodeInterningTest {
    @Test
    fun testInternedCodeMatchesRawFileSubstring() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val file = topLevel.resolve("complex_class.py").toFile()
        val content = file.readText()
        val lineStarts = mutableListOf(0)
        content.forEachIndexed { i, c -> if (c == '\n') lineStarts += i + 1 }
        fun offsetOf(line: Int, column: Int) = lineStarts.getOrNull(line - 1)?.plus(column - 1)

        val result =
            analyze(listOf(file), topLevel, false) { it.registerLanguage<PythonLanguage>() }

        val internedNodes = result.nodes.filter { it.isCodeInterned }
        assertTrue(internedNodes.isNotEmpty(), "expected at least one node to use interned code")

        for (node in internedNodes) {
            val region = node.location?.region ?: continue
            val start = offsetOf(region.startLine, region.startColumn) ?: continue
            val end = offsetOf(region.endLine, region.endColumn) ?: continue
            assertEquals(content.substring(start, end), node.code, "mismatch for $node")
        }
    }
}
