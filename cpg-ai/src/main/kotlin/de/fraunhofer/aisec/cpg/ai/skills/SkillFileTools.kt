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
package de.fraunhofer.aisec.cpg.ai.skills

import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.tool.file.ListDirectoryTool
import ai.koog.agents.ext.tool.file.ReadFileTool
import ai.koog.rag.base.files.FileSystemProvider
import ai.koog.rag.base.files.JVMFileSystemProvider
import ai.koog.rag.base.files.filter
import ai.koog.rag.base.files.filter.PathFilters
import ai.koog.skills.model.Skill
import ai.koog.skills.prompt.SkillsPromptFormat
import ai.koog.skills.prompt.generateSkillsPrompt
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Directories to scan for skills. By default, we look in `.agents/skills`, which is the recommended
 * location for project-specific skills. However, in the future we might also scan client-specific
 * locations used by other clients (e.g. `~/.claude/skills` or Gemini equivalents).
 *
 * See
 * [Where to scan](https://agentskills.io/client-implementation/adding-skills-support#where-to-scan)
 */
val defaultSkillDirectories: List<Path> = listOf(Paths.get(".agents", "skills"))

/** Koog's [ListDirectoryTool] tool name - a synthetic name it sets itself, not configurable. */
const val LIST_DIRECTORY_TOOL_NAME = "__list_directory__"

/** Koog's [ReadFileTool] tool name - a synthetic name it sets itself, not configurable. */
const val READ_FILE_TOOL_NAME = "__read_file__"

/**
 * A read-only filesystem view jailed to [dirs]: every operation on a path that isn't inside (or an
 * ancestor of) one of [dirs] is rejected by Koog's own [PathFilters.byRoot] containment check -
 * including `../`-style traversal attempts, since paths get normalized before the check runs.
 * Shared by skill discovery and [buildSkillFileToolRegistry] so the file tools below can never read
 * anything outside the skills directory, even though they're otherwise generic/unrestricted tools.
 */
fun jailedSkillsFileSystem(dirs: List<Path>): FileSystemProvider.ReadOnly<Path> {
    val combinedFilter =
        dirs.map { PathFilters.byRoot(it.toAbsolutePath().normalize()) }.reduce { a, b -> a or b }
    return JVMFileSystemProvider.ReadOnly.filter(combinedFilter)
}

/**
 * Build the [ToolRegistry] the model uses to "activate" a discovered skill. Koog's Agent Skills
 * module has no built-in activation tool of its own - its recommended pattern
 * (`docs/docs/skills.md` in the Koog repo) is to register generic file tools and let the model read
 * `SKILL.md` itself. [fs] should be a jailed view (see [jailedSkillsFileSystem]) so that this
 * pattern can't be used to read outside the skills directory - these are otherwise general-purpose
 * file tools.
 */
fun buildSkillFileToolRegistry(fs: FileSystemProvider.ReadOnly<Path>): ToolRegistry = ToolRegistry {
    tool(ListDirectoryTool(fs))
    tool(ReadFileTool(fs))
}

/**
 * Build the skill-catalog section appended to the system prompt: Koog's own [generateSkillsPrompt]
 * (with `includeLocation = true`, since the model needs each skill's absolute path to actually
 * list/read it), plus a short instruction on how to load a skill using the tools from
 * [buildSkillFileToolRegistry] - adapted from Koog's own recommended system-prompt wording in
 * `docs/docs/skills.md`.
 */
fun buildSkillCatalog(skills: List<Skill>): String? {
    if (skills.isEmpty()) return null
    val catalog = generateSkillsPrompt(skills, SkillsPromptFormat.XML, includeLocation = true)
    return """
        The following skills provide specialized instructions for specific tasks. Use the
        available skills listed below. Before following a skill, disclose it by listing its
        directory and reading its SKILL.md file (at the listed location) with your file tools.

        $catalog
    """
        .trimIndent()
}
