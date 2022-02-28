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

import de.fraunhofer.aisec.cpg.TestUtils
import java.io.File
import kotlin.io.path.Path
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Test

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
    fun testGetBenchmarkResult() {
        val file = File("src/test/resources/components/foreachstmt.cpp")
        val tr = TestUtils.analyzeWithResult(listOf(file), file.parentFile.toPath(), true)

        assertNotNull(tr)
        val res = tr.benchmarkResults
        assertNotNull(res)

        val resMap = res.entries.associate { it[0] to it[1] }
        assertEquals(1, resMap["Number of files translated"])

        val files = resMap["Translated file(s)"] as List<*>
        assertNotNull(files)
        assertEquals(1, files.size)
        assertEquals(Path("foreachstmt.cpp"), files[0])

        val json = res.json
        assertContains(json, "{")
    }

    @Test
    fun testPrintBenchmark() {
        val file = File("src/test/resources/components/foreachstmt.cpp")
        val tr = TestUtils.analyzeWithResult(listOf(file), file.parentFile.toPath(), true)

        assertNotNull(tr)
        tr.benchmarkResults.print()
    }
}
