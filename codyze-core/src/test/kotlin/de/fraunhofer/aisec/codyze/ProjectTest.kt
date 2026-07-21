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
package de.fraunhofer.aisec.codyze

import de.fraunhofer.aisec.cpg.TranslationResult.Companion.DEFAULT_APPLICATION_NAME
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

class ProjectTest {
    @Test
    fun testTemporaryWithSources(@TempDir tmp: Path) {
        val file = tmp.resolve("main.test")
        file.writeText("")

        val project = AnalysisProject.temporary(projectDir = tmp, sources = listOf(file))

        assertEquals(tmp, project.projectDir)
        assertEquals(tmp.fileName.toString(), project.name)
        assertEquals(
            listOf(file.toFile()),
            project.config.softwareComponents[DEFAULT_APPLICATION_NAME],
        )
    }

    @Test
    fun testTemporaryWithComponents(@TempDir tmp: Path) {
        tmp.resolve("components/backend").createDirectories()

        val project = AnalysisProject.temporary(projectDir = tmp, components = listOf("backend"))

        assertEquals(
            tmp.resolve("components/backend").toFile(),
            project.config.topLevels["backend"],
        )
    }

    @Test
    fun testTemporaryWithExclusionPatterns(@TempDir tmp: Path) {
        val project =
            AnalysisProject.temporary(projectDir = tmp, exclusionPatterns = listOf("tests"))

        assertContains(project.config.exclusionPatternsByString, "tests")
    }

    @Test
    fun testTemporaryLoadsLibrariesAsIncludes(@TempDir tmp: Path) {
        val library = tmp.resolve("libraries/stdlib")
        library.createDirectories()

        val project = AnalysisProject.temporary(projectDir = tmp)

        assertTrue(project.config.loadIncludes)
        assertContains(project.config.includePaths, library)
    }

    @Test
    fun testTemporaryAppliesConfigModifier(@TempDir tmp: Path) {
        var invoked = false

        AnalysisProject.temporary(
            projectDir = tmp,
            configModifier = {
                invoked = true
                it
            },
        )

        assertTrue(invoked)
    }
}
