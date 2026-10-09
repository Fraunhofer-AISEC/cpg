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
import ai.koog.rag.base.files.filter.TraversalFilter
import ai.koog.skills.model.Skill
import ai.koog.skills.prompt.SkillsPromptFormat
import ai.koog.skills.prompt.generateSkillsPrompt
import java.io.IOException
import java.nio.file.Files
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
 * ancestor of) one of [dirs] is rejected. Two checks must both pass: Koog's [PathFilters.byRoot] on
 * the normalized path (which catches `../`-style traversal), and [realPathWithin] on the path with
 * symlinks resolved - `byRoot` alone is lexical, so a symlink under a skills directory pointing
 * elsewhere would otherwise make its target readable. Shared by skill discovery and
 * [buildSkillFileToolRegistry] so the file tools below can never read anything outside the skills
 * directory, even though they're otherwise generic/unrestricted tools.
 */
fun jailedSkillsFileSystem(dirs: List<Path>): FileSystemProvider.ReadOnly<Path> {
    val lexical =
        dirs.map { PathFilters.byRoot(it.toAbsolutePath().normalize()) }.reduce { a, b -> a or b }
    return JVMFileSystemProvider.ReadOnly.filter(lexical and realPathWithin(dirs))
}

/**
 * Accepts a path whose real location (symlinks resolved) lies inside one of the real [roots], or is
 * an ancestor of one - the same shape as [PathFilters.byRoot], which lets a client walk down to a
 * root.
 */
private fun realPathWithin(roots: List<Path>): TraversalFilter<Path> {
    val realRoots = roots.map { realPath(it.toAbsolutePath().normalize()) }
    return object : TraversalFilter<Path> {
        override suspend fun show(path: Path, fs: FileSystemProvider.ReadOnly<Path>): Boolean {
            val real = realPath(path.toAbsolutePath().normalize())
            return realRoots.any { root -> real.startsWith(root) || root.startsWith(real) }
        }
    }
}

/**
 * [path] with symlinks resolved. A path that does not exist (yet) is resolved through its deepest
 * existing ancestor, so a missing file under a symlinked directory is judged by where it would
 * really be.
 */
private fun realPath(path: Path): Path {
    var existing: Path? = path
    while (existing != null && !Files.exists(existing)) existing = existing.parent
    if (existing == null) return path
    val real =
        try {
            existing.toRealPath()
        } catch (_: IOException) {
            return path
        }
    return real.resolve(existing.relativize(path))
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
 * (with `includeLocation = true`, since the model needs each skill's absolute path to actually read
 * it), plus a short instruction on how to load a skill using the tools from
 * [buildSkillFileToolRegistry].
 *
 * Two things tuned here:
 * - Says "read" only, not "list its directory and read" - the model tends to do both per
 *   activation, costing an extra tool round trip it doesn't need (the catalog already gives the
 *   exact file path).
 * - States the access restriction explicitly, since [ReadFileTool]/[ListDirectoryTool]'s own
 *   descriptions can't be changed (they're Koog's own non-`open` classes) and the rejection message
 *   a jailed path actually gets back - "File not found: ... (ensure the path is absolute)" - reads
 *   like the file doesn't exist rather than that it's out of bounds. Telling the model the boundary
 *   upfront is cheaper than letting it find out by trial and error: without it the model wanders
 *   into listing `/home/cpg` and reading an unrelated `/tmp/...` path before giving up and
 *   proceeding correctly - both safely rejected by [jailedSkillsFileSystem], but wasted
 *   tokens/round trips getting there.
 */
fun buildSkillCatalog(skills: List<Skill>): String? {
    if (skills.isEmpty()) return null
    val catalog = generateSkillsPrompt(skills, SkillsPromptFormat.XML, includeLocation = true)
    return """
        The following skills provide specialized instructions for specific tasks. Use the
        available skills listed below. To follow a skill, read its SKILL.md file directly at
        the listed location with your file tools - no need to list the directory first. Your
        file tools are restricted to `.agents/skills/` only; listing or reading any other path
        will fail.

        $catalog
    """
        .trimIndent()
}
