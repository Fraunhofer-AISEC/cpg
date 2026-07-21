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

import de.fraunhofer.aisec.cpg.frontends.CompilationDatabase
import de.fraunhofer.aisec.cpg.project.ComponentDefinition
import de.fraunhofer.aisec.cpg.project.DetectionResult
import java.io.File
import java.nio.file.Path
import kotlin.io.path.exists
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(CLanguage::class.java)

/** The file name of a JSON compilation database, as emitted by CMake, bear and others. */
private const val COMPILE_COMMANDS = "compile_commands.json"

/** Returns the path to a [COMPILE_COMMANDS] file in [directory] or its `build` folder, if any. */
private fun findCompilationDatabase(directory: Path): Path? {
    return sequenceOf(
            directory.resolve(COMPILE_COMMANDS),
            directory.resolve("build").resolve(COMPILE_COMMANDS),
        )
        .firstOrNull { it.exists() }
}

/**
 * Detects C/C++ project structure and settings in one pass. Looks for a [COMPILE_COMMANDS] file in
 * [root] or its `build` folder. If found, the database supplies per-file include paths and
 * preprocessor defines, and its targets are translated into [ComponentDefinition]s rooted in
 * [root]. Returns `null` if no compilation database is found.
 */
internal fun detectCxx(root: Path): DetectionResult? {
    val file = findCompilationDatabase(root) ?: return null
    val db = tryLoadCompilationDatabase(file) ?: return null
    val components =
        db.components.map { (name, files) ->
            ComponentDefinition(name, root = root, sources = files.map(File::toPath))
        }
    return DetectionResult(
        detector = COMPILE_COMMANDS,
        components = components,
        compilationDatabase = db,
        notes = listOf("using compilation database at $file with ${db.size} entries"),
    )
}

private fun tryLoadCompilationDatabase(file: Path): CompilationDatabase? {
    return try {
        CompilationDatabase.fromFile(file.toFile())
    } catch (e: Exception) {
        log.warn("Could not parse compilation database at {}: {}", file, e.message)
        null
    }
}
