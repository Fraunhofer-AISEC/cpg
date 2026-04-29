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

import de.fraunhofer.aisec.codyze.console.ai.skills.SkillLoader
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SkillLoaderTest {

    @Test
    fun discoverSingleSkillTest() {
        val skillsDir = Paths.get("src/test/resources/skills")
        val loader = SkillLoader(listOf(skillsDir))
        val skills = loader.discoverSkills()

        assertEquals(1, skills.size)
        val skill = skills.first()
        assertEquals("tagging", skill.name)
        assertEquals("Tag concepts and operations.", skill.description)
        assertTrue(skill.body.contains("# Tagging concepts and operations"))
    }

    @Test
    fun discoverMultipleSkillsTest() {
        val skillsDir = Paths.get("src/test/resources/multi-skills")
        val loader = SkillLoader(listOf(skillsDir))
        val skills = loader.discoverSkills()

        assertEquals(setOf("foo", "bar"), skills.map { it.name }.toSet())
    }

    @Test
    fun missingDirectoryTest() {
        val loader = SkillLoader(listOf(Paths.get("src/test/resources/does-not-exist")))
        assertEquals(emptyList(), loader.discoverSkills())
    }

    @Test
    fun skipsSubdirectoryWithoutSkillMdTest() {
        val skillsDir = Paths.get("src/test/resources/empty-skills")
        val loader = SkillLoader(listOf(skillsDir))
        assertEquals(emptyList(), loader.discoverSkills())
    }

    @Test
    fun skipsFileWithoutFrontmatterTest() {
        val loader = SkillLoader(emptyList())
        val skill = loader.parseSkill(Paths.get("src/test/resources/skill-files/no-frontmatter.md"))
        assertNull(skill)
    }

    @Test
    fun skipsFileWithoutNameTest() {
        val loader = SkillLoader(emptyList())
        val skill = loader.parseSkill(Paths.get("src/test/resources/skill-files/missing-name.md"))
        assertNull(skill)
    }

    @Test
    fun skipsFileWithoutDescriptionTest() {
        val loader = SkillLoader(emptyList())
        val skill =
            loader.parseSkill(Paths.get("src/test/resources/skill-files/missing-description.md"))
        assertNull(skill)
    }

    @Test
    fun keepsColonsInValueTest() {
        val loader = SkillLoader(emptyList())
        val skill =
            loader.parseSkill(Paths.get("src/test/resources/skill-files/colon-in-description.md"))

        assertEquals("foo", skill?.name)
        assertEquals("bar: baz", skill?.description)
    }
}
