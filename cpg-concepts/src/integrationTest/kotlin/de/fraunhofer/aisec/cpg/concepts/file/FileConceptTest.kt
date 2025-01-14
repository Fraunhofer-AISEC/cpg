/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.concepts.file

import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage
import de.fraunhofer.aisec.cpg.graph.conceptNodes
import de.fraunhofer.aisec.cpg.passes.concepts.FileConceptEOGPass
import de.fraunhofer.aisec.cpg.passes.concepts.FileConceptPass
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FileConceptTest : BaseTest() {
    @Test
    fun testRead() {
        val topLevel = Path.of("src", "integrationTest", "resources", "concepts", "file", "python")

        val result =
            analyze(
                files = listOf(topLevel.resolve("file_read.py").toFile()),
                topLevel = topLevel,
                usePasses = false,
            ) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<FileConceptPass>()
                it.registerPass<FileConceptEOGPass>()
            }
        assertNotNull(result)

        val conceptNodes = result.conceptNodes
        assertTrue(conceptNodes.isNotEmpty())
    }

    @Test
    fun testWrite() {
        val topLevel = Path.of("src", "integrationTest", "resources", "concepts", "file", "python")

        val result =
            analyze(
                files = listOf(topLevel.resolve("file_write.py").toFile()),
                topLevel = topLevel,
                usePasses = true,
            ) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<FileConceptPass>()
                it.registerPass<FileConceptEOGPass>()
            }
        assertNotNull(result)

        val conceptNodes = result.conceptNodes
        assertTrue(conceptNodes.isNotEmpty())
    }

    @Test
    fun testEOG() {
        val topLevel = Path.of("src", "integrationTest", "resources", "concepts", "file", "python")

        val result =
            analyze(
                files = listOf(topLevel.resolve("testEOG.py").toFile()),
                topLevel = topLevel,
                usePasses = true,
            ) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<FileConceptPass>()
                it.registerPass<FileConceptEOGPass>()
            }
        assertNotNull(result)

        val conceptNodes = result.conceptNodes
        assertTrue(conceptNodes.isNotEmpty())
    }
}
