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
package de.fraunhofer.aisec.cpg.ai

import ai.koog.skills.discovery.discoverSkills
import de.fraunhofer.aisec.cpg.ai.skills.buildSkillCatalog
import de.fraunhofer.aisec.cpg.ai.skills.jailedSkillsFileSystem
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * Frontmatter parsing/discovery-edge-case coverage now lives upstream (Koog's own
 * `SkillsDiscoveryTest`) since [ai.koog.skills.discovery.discoverSkills] replaced our hand-rolled
 * parser. What's left to test here is our own code: that discovery is wired up correctly against
 * the real fixtures, and - the security-relevant part - that [jailedSkillsFileSystem] actually
 * rejects paths outside the skills directory, including normalized `..` escapes.
 */
class SkillFileToolsTest {

    private fun absolute(path: String) = Paths.get(path).toAbsolutePath().normalize()

    @Test
    fun discoverSingleSkillTest() = runTest {
        val skillsDir = absolute("src/test/resources/skills")
        val fs = jailedSkillsFileSystem(listOf(skillsDir))
        val skills = discoverSkills(fs, listOf(skillsDir.toString()))

        assertEquals(1, skills.size)
        val skill = skills.first()
        assertEquals("tagging", skill.name)
        assertEquals("Tag concepts and operations.", skill.description)
    }

    @Test
    fun discoverMultipleSkillsTest() = runTest {
        val skillsDir = absolute("src/test/resources/multi-skills")
        val fs = jailedSkillsFileSystem(listOf(skillsDir))
        val skills = discoverSkills(fs, listOf(skillsDir.toString()))

        assertEquals(setOf("foo", "bar"), skills.map { it.name }.toSet())
    }

    @Test
    fun missingDirectoryTest() = runTest {
        val skillsDir = absolute("src/test/resources/does-not-exist")
        val fs = jailedSkillsFileSystem(listOf(skillsDir))

        assertEquals(emptyList(), discoverSkills(fs, listOf(skillsDir.toString())))
    }

    @Test
    fun jailAllowsPathInsideRootTest() = runTest {
        val skillsDir = absolute("src/test/resources/skills")
        val fs = jailedSkillsFileSystem(listOf(skillsDir))
        val inside = absolute("src/test/resources/skills/tagging/SKILL.md")

        assertTrue(fs.exists(inside))
    }

    @Test
    fun jailRejectsSiblingPathTest() = runTest {
        val skillsDir = absolute("src/test/resources/skills")
        val fs = jailedSkillsFileSystem(listOf(skillsDir))
        val sibling = absolute("src/test/resources/multi-skills/foo/SKILL.md")

        assertFalse(fs.exists(sibling), "jailed fs must not see paths outside its root")
        assertNull(fs.metadata(sibling))
    }

    @Test
    fun jailRejectsNormalizedTraversalEscapeTest() = runTest {
        val skillsDir = absolute("src/test/resources/skills")
        val fs = jailedSkillsFileSystem(listOf(skillsDir))
        // Resolves (after normalization) to multi-skills/foo/SKILL.md - a sibling, not a
        // descendant - so the jail must still reject it despite the string starting under root.
        val escapee = fs.fromAbsolutePathString("$skillsDir/../multi-skills/foo/SKILL.md")

        assertFalse(fs.exists(escapee))
    }

    @Test
    fun catalogIncludesLocationForFileToolsToUseTest() = runTest {
        val skillsDir = absolute("src/test/resources/skills")
        val fs = jailedSkillsFileSystem(listOf(skillsDir))
        val skills = discoverSkills(fs, listOf(skillsDir.toString()))

        val catalog = buildSkillCatalog(skills)
        assertTrue(catalog != null)
        checkNotNull(catalog)
        assertTrue(catalog.contains("tagging"))
        assertTrue(catalog.contains("location"), "model needs the location to list/read a skill")
    }

    @Test
    fun emptyCatalogTest() {
        assertNull(buildSkillCatalog(emptyList()))
    }
}
