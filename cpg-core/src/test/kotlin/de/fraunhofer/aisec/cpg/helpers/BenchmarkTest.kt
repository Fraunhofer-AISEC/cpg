/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BenchmarkTest {

    @Test
    fun testRelativeOrAbsolute() {
        val relPath = Path("./main.c")
        val absPath = Path("/root/main.c")
        val topLevelRoot = File("/root")
        val topLevelFoo = File("/foo")

        assertEquals(Path("./main.c"), relativeOrAbsolute(relPath, topLevelRoot))
        assertEquals(Path("main.c"), relativeOrAbsolute(absPath, topLevelRoot))

        // This is not what you would expect.
        // But as topLevel is always the largest common path of all source files, this should not
        // happen
        assertEquals(Path("../root/main.c"), relativeOrAbsolute(absPath, topLevelFoo))

        assertEquals(relPath, relativeOrAbsolute(relPath, null))
        assertEquals(absPath, relativeOrAbsolute(absPath, null))
    }

    @Test
    fun testPrintMarkdownTruncation() {
        // Redirect standard output to capture printed markdown table
        val stdout = System.out
        val outputCapture = ByteArrayOutputStream()
        System.setOut(PrintStream(outputCapture))

        // Create test data with short and long columns
        val shortHeader = "Short"
        val longHeader =
            "This is an extremely long header that should be truncated because it exceeds 80 characters by quite a bit"
        val headers = listOf(shortHeader, longHeader)

        val shortValue = "Value"
        val longValue =
            "This is a very long value that definitely exceeds the maximum column width of 80 characters and should be truncated with ellipsis at the end"
        val table =
            listOf(
                listOf("Row1Short", "Row1Normal"),
                listOf(shortValue, longValue),
                listOf("Row3Short", "Row3Normal value that is under 80 characters"),
            )

        // Execute the function being tested
        printMarkdown(table, headers)

        // Restore standard output
        System.setOut(stdout)

        // Get captured output
        val output = outputCapture.toString()

        // Verify truncation behavior
        assertTrue(output.contains("| Short "), "Short header should be displayed as is")
        assertTrue(
            output.contains(
                "| This is an extremely long header that should be truncated because it exceeds ... |"
            ),
            "Long header should be truncated with ellipsis",
        )

        assertTrue(output.contains("| Row1Short "), "Short value should be displayed as is")
        assertTrue(
            output.contains(
                "| This is a very long value that definitely exceeds the maximum column width of... |"
            ),
            "Long value should be truncated with ellipsis",
        )

        // Verify no line in the table exceeds 80 characters per column (plus formatting characters)
        val lines = output.split("\n")
        for (line in lines) {
            val columns = line.split("|")
            for (column in columns) {
                assertTrue(
                    column.length <= 80 + 2 /* 80 characters + 2 for padding */,
                    "Each column should be 80 characters or less (plus potential padding): '$column'",
                )
            }
        }
    }
}
