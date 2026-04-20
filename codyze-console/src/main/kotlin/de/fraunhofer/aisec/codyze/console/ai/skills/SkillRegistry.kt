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
package de.fraunhofer.aisec.codyze.console.ai.skills

import java.nio.file.Files
import java.nio.file.Path
import org.slf4j.LoggerFactory

data class Skill(val name: String, val description: String, val body: String, val location: Path)

class SkillRegistry(private val skillsDir: Path) {
    private val log = LoggerFactory.getLogger(SkillRegistry::class.java)

    private var skills: Map<String, Skill> = emptyMap()

    fun discoverSkills(): List<Skill> {
        // The skills directory doesn't exist or is not a directory
        if (!Files.isDirectory(skillsDir)) {
            log.debug("Skills directory does not exist: {}", skillsDir)
            return emptyList()
        }
        val discovered =
            Files.list(skillsDir).use { stream ->
                stream
                    .filter { Files.isDirectory(it) }
                    .map { it.resolve("SKILL.md") }
                    .filter { Files.isRegularFile(it) }
                    .map { parseSkill(it) }
                    .toList()
                    .filterNotNull()
            }
        skills = discovered.associateBy { it.name }
        return discovered
    }

    fun parseSkill(skillMd: Path): Skill? {
        val content = Files.readString(skillMd).trim()
        if (!content.startsWith("---")) return null

        // Expected layout:
        //   ---
        //   <frontmatter>
        //   ---
        //   <body>
        // limit = 3 so that any `---` inside the body (in markdown) stays in parts[2].
        val parts = content.split("---", limit = 3)
        if (parts.size < 3) return null

        val frontMatter = parts[1].trim()
        val body = parts[2].trim()

        // Frontmatter is parsed line-by-line as `key: value` pairs.
        // We split on the first `:` only, so values may
        // contain further colons (e.g. "description: Use when: ...").
        // Lines without a `:` are skipped.
        val fields =
            frontMatter
                .lines()
                .mapNotNull { line ->
                    val lineIndex = line.indexOf(':')
                    if (lineIndex < 0) null
                    else line.substring(0, lineIndex).trim() to line.substring(lineIndex + 1).trim()
                }
                .toMap()

        val name = fields["name"] ?: return null
        val description = fields["description"] ?: return null

        return Skill(name = name, description = description, body = body, location = skillMd)
    }

    fun getSkill(name: String): Skill? = skills[name]

    fun allSkills(): List<Skill> = skills.values.toList()
}
