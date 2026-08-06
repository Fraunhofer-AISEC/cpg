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
package de.fraunhofer.aisec.cpg.frontends.cxx

import de.fraunhofer.aisec.cpg.project.Project
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CXXProjectDetectionTest {
    @Test
    fun testDetectCompilationDatabaseInBuildFolder() {
        val root = Path.of("src/test/resources/cxxProjectDetection/buildFolder")

        val project = Project.from(root) { registerLanguage<CLanguage>() }

        // The component must be rooted in the project directory, not in "build", so that
        // translation unit names stay relative to the project
        val component = project.components.singleOrNull()
        assertNotNull(component)
        assertEquals("libfoo", component.name)
        assertEquals(root, component.root)
        assertEquals(root.toFile(), project.config.topLevels["libfoo"])
    }

    @Test
    fun testDetectCompilationDatabase() {
        val root = Path.of("src/test/resources/cxxProjectDetection/multiComponent")

        val project =
            Project.from(root) {
                registerLanguage<CLanguage>()
                registerLanguage<CPPLanguage>()
            }

        // One component per compilation database component (derived from the src/ layout here)
        assertEquals(listOf("libfoo", "tool"), project.components.map { it.name }.sorted())

        // Even though both CLanguage and CPPLanguage share the detection logic, the result must
        // only appear once
        val result = project.detectionResults.singleOrNull()
        assertNotNull(result)
        assertEquals("compile_commands.json", result.detector)

        val db = project.config.compilationDatabase
        assertNotNull(db)
        assertEquals(2, db.size)
        assertEquals(mapOf("FOO" to "1"), db.getAllSymbols("libfoo").filterKeys { it == "FOO" })
    }

    @Test
    fun testNoCompilationDatabaseFound() {
        val root = Path.of("src/test/resources/cxxProjectDetection/noDatabase")

        val project = Project.from(root) { registerLanguage<CLanguage>() }

        assertTrue(project.detectionResults.isEmpty())
    }

    @Test
    fun testMalformedCompilationDatabaseIsIgnored() {
        val root = Path.of("src/test/resources/cxxProjectDetection/malformedDatabase")

        val project = Project.from(root) { registerLanguage<CLanguage>() }

        assertTrue(project.detectionResults.isEmpty())
    }
}
