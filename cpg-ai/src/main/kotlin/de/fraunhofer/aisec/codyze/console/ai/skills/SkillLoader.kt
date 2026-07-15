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

import de.fraunhofer.aisec.codyze.console.ai.Skill
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.slf4j.LoggerFactory

/**
 * Directories to scan for skills. By default, we look in `.agents/skills`, which is the recommended
 * location for project-specific skills. However, in the future we might also scan client-specific
 * locations used by other clients (e.g. `~/.claude/skills` or Gemini equivalents).
 *
 * See
 * [Where to scan](https://agentskills.io/client-implementation/adding-skills-support#where-to-scan)
 */
val defaultSkillDirectories: List<Path> = listOf(Paths.get(".agents", "skills"))

class SkillLoader(private val skillsDirs: List<Path>) {
    private val log = LoggerFactory.getLogger(SkillLoader::class.java)

    private var skills: Map<String, Skill> = emptyMap()

    /**
     * Scan the configured skills directory and parse each `<skill>/SKILL.md` it finds. Missing
     * directories are skipped.
     *
     * See
     * [Parse SKILL.md files](https://agentskills.io/client-implementation/adding-skills-support#step-2-parse-skill-md-files)
     */
    fun discoverSkills(): List<Skill> {
        val discovered = mutableListOf<Skill>()
        for (dir in skillsDirs) {
            if (!Files.isDirectory(dir)) {
                log.debug("Skills directory does not exist: {}", dir)
                continue
            }
            Files.newDirectoryStream(dir).use { entries ->
                for (entry in entries) {
                    if (!Files.isDirectory(entry)) continue
                    val skillMd = entry.resolve("SKILL.md")
                    if (!Files.isRegularFile(skillMd)) continue
                    parseSkill(skillMd)?.let { discovered.add(it) }
                }
            }
        }
        skills = discovered.associateBy { it.name }
        return discovered
    }

    /**
     * Parse a single `SKILL.md` file. Expected layout:
     * ```
     * ---
     * <frontmatter>
     * ---
     * <body>
     * ```
     *
     * Returns `null` if the file does not start with `---` or is missing required fields (`name`,
     * `description`).
     *
     * See
     * [Frontmatter extraction](https://agentskills.io/client-implementation/adding-skills-support#frontmatter-extraction)
     */
    fun parseSkill(skillMd: Path): Skill? {
        val content = Files.readString(skillMd).trim()

        // A valid SKILL.md must start with `---` to open the frontmatter block.
        if (!content.startsWith("---")) {
            return null
        }

        // limit = 3 so that any `---` inside the body (in markdown) stays in parts[2].
        val parts = content.split("---", limit = 3)
        // We need three parts: the opening `---`, the frontmatter,
        // and the body.
        if (parts.size < 3) {
            return null
        }

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
                    if (lineIndex < 0) {
                        return@mapNotNull null
                    } else {
                        line.substring(0, lineIndex).trim() to line.substring(lineIndex + 1).trim()
                    }
                }
                .toMap()

        // `name` and `description` are required fields
        val name = fields["name"] ?: return null
        val description = fields["description"] ?: return null

        return Skill(name = name, description = description, body = body, location = skillMd)
    }
}
