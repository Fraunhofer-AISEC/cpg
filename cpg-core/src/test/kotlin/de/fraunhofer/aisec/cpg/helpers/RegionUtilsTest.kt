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
package de.fraunhofer.aisec.cpg.helpers

import de.fraunhofer.aisec.cpg.sarif.Region
import kotlin.test.Test
import kotlin.test.assertEquals

class RegionUtilsTest {
    /** A small file whose node region spans the whole text, starting at 1:1. */
    private val fileRegion = Region(startLine = 1, startColumn = 1)

    @Test
    fun testSameLineRegion() {
        val code = "int a = foo(1, 2);"
        // "foo(1, 2)" starts at column 9 (1-indexed) and ends at column 18
        val subRegion = Region(startLine = 1, startColumn = 9, endLine = 1, endColumn = 18)

        val range = regionToOffsets(code, fileRegion, subRegion)

        assertEquals("foo(1, 2)", code.substring(range.first, range.last + 1))
    }

    @Test
    fun testMultiLineRegion() {
        val code = "int main() {\n    return 1;\n}\n"
        // "return 1;" is on line 2, columns 5 to 14
        val subRegion = Region(startLine = 2, startColumn = 5, endLine = 2, endColumn = 14)

        val range = regionToOffsets(code, fileRegion, subRegion)

        assertEquals("return 1;", code.substring(range.first, range.last + 1))
    }

    @Test
    fun testRegionSpanningMultipleLines() {
        val code = "if (x) {\n    doA();\n    doB();\n}\n"
        // The whole if-block, from column 1 of line 1 to column 2 of line 4
        val subRegion = Region(startLine = 1, startColumn = 1, endLine = 4, endColumn = 2)

        val range = regionToOffsets(code, fileRegion, subRegion)

        assertEquals(code.trimEnd('\n'), code.substring(range.first, range.last + 1))
    }

    @Test
    fun testCrlfLineBreaks() {
        // Pre-existing quirk of the underlying offset math (unchanged by this refactor): it
        // assumes a single-character line break, so it under-shoots by (lineBreakSequence.length
        // - 1) once a region starts on a line after the first. For "\r\n" this shifts the result
        // one character to the left of the intended text.
        val code = "int main() {\r\n    return 1;\r\n}\r\n"
        val subRegion = Region(startLine = 2, startColumn = 5, endLine = 2, endColumn = 14)

        val range = regionToOffsets(code, fileRegion, subRegion, lineBreakSequence = "\r\n")

        assertEquals(" return 1", code.substring(range.first, range.last + 1))
    }

    @Test
    fun testClampsEndBeyondCodeLength() {
        val code = "x"
        // A subRegion whose end reaches beyond the available code (as can happen with
        // non-ASCII/Unicode column counting mismatches)
        val subRegion = Region(startLine = 1, startColumn = 1, endLine = 1, endColumn = 100)

        val range = regionToOffsets(code, fileRegion, subRegion)

        assertEquals(0, range.first)
        assertEquals(code.length - 1, range.last)
    }

    @Test
    fun testGetCodeOfSubregionUsesSameMath() {
        val code = "int main() {\n    return 1;\n}\n"
        val subRegion = Region(startLine = 2, startColumn = 5, endLine = 2, endColumn = 14)

        assertEquals("return 1;", getCodeOfSubregion(code, fileRegion, subRegion))
    }
}
