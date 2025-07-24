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
package de.fraunhofer.aisec.cpg.concepts

import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage
import de.fraunhofer.aisec.cpg.graph.allCalls
import de.fraunhofer.aisec.cpg.graph.conceptNodes
import de.fraunhofer.aisec.cpg.graph.concepts.file.File
import de.fraunhofer.aisec.cpg.graph.invoke
import de.fraunhofer.aisec.cpg.passes.concepts.LoadPersistedConcepts
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import kotlin.test.*

class LoadPersistedConceptsTest : BaseTest() {
    @Test
    fun testReadConceptByLocation() {
        val topLevel = Path.of("src", "integrationTest", "resources", "python", "file")

        val result =
            analyze(
                files = listOf(topLevel.resolve("file_read.py").toFile()),
                topLevel = topLevel,
                usePasses = true,
            ) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<LoadPersistedConcepts>()
                it.symbols(mapOf("PYTHON_PLATFORM" to "linux"))
                it.configurePass<LoadPersistedConcepts>(
                    LoadPersistedConcepts.Configuration(
                        conceptFiles =
                            listOf(topLevel.resolve("file-concept-location.yaml").toFile())
                    )
                )
            }
        assertNotNull(result)

        val fileConcept = result.conceptNodes.singleOrNull { it is File }
        assertIs<File>(fileConcept)

        val openCall = result.allCalls("open").singleOrNull()
        assertNotNull(openCall)

        assertTrue(
            fileConcept.nextDFG.contains(openCall),
            "NextDFG: `File` concept should contain `open` call.",
        )
        assertFalse(
            openCall.nextDFG.contains(fileConcept),
            "NextDFG: `open` call should not contain `File` concept.",
        )

        assertEquals("foo", fileConcept.fileName, "Expected name of the file concept to be `foo`.")
    }

    @Test
    fun testReadConceptBySignature() {
        val topLevel = Path.of("src", "integrationTest", "resources", "python", "file")

        val result =
            analyze(
                files = listOf(topLevel.resolve("file_read.py").toFile()),
                topLevel = topLevel,
                usePasses = true,
            ) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<LoadPersistedConcepts>()
                it.symbols(mapOf("PYTHON_PLATFORM" to "linux"))
                it.configurePass<LoadPersistedConcepts>(
                    LoadPersistedConcepts.Configuration(
                        conceptFiles =
                            listOf(topLevel.resolve("file-concept-signature.yaml").toFile())
                    )
                )
            }
        assertNotNull(result)

        val fileConcept = result.conceptNodes.singleOrNull { it is File }
        assertIs<File>(fileConcept)

        val openCall = result.allCalls("open").singleOrNull()
        assertNotNull(openCall)

        assertTrue(
            fileConcept.nextDFG.contains(openCall),
            "NextDFG: `File` concept should contain `open` call.",
        )
        assertFalse(
            openCall.nextDFG.contains(fileConcept),
            "NextDFG: `open` call should not contain `File` concept.",
        )

        assertEquals("foo", fileConcept.fileName, "Expected name of the file concept to be `foo`.")
    }
}
