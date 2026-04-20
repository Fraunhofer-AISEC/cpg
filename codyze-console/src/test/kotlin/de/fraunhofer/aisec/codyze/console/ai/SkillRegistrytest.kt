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
package de.fraunhofer.aisec.codyze.console.ai

import de.fraunhofer.aisec.codyze.console.ai.skills.SkillRegistry
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SkillRegistrytest {

    @Test
    fun discoverSingleSkillTest() {
        val tmp = Files.createTempDirectory("skills-test")
        val skillDir = tmp.resolve("tagging").also { it.createDirectories() }
        skillDir
            .resolve("SKILL.md")
            .writeText(
                """
                ---
                name: tagging
                description: Tag concepts and operations.
                ---

                # Tagging concepts and operations
                Use this when the user asks about tagging.
                """
                    .trimIndent()
            )

        val registry = SkillRegistry(skillsDir = tmp)
        val skills = registry.discoverSkills()

        assertEquals(1, skills.size)
        val skill = skills.first()
        assertEquals("tagging", skill.name)
        assertEquals("Tag concepts and operations.", skill.description)
        assertTrue(skill.body.contains("# Tagging concepts and operations"))
    }

    @Test
    fun missingDirectoryTest() {
        val tmp = Files.createTempDirectory("skills-test")
        val registry = SkillRegistry(tmp.resolve("does-not-exist"))
        assertEquals(emptyList(), registry.discoverSkills())
    }

    @Test
    fun directoryWithoutSkillMdFileTest() {
        val tmp = Files.createTempDirectory("skills-test")
        tmp.resolve("no-skill-md").createDirectories()
        val registry = SkillRegistry(tmp)
        assertEquals(emptyList(), registry.discoverSkills())
    }
}
