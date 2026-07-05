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
 * Detects components based on a [COMPILE_COMMANDS] file in [directory] or its `build` folder. The
 * compilation database groups its entries into components (e.g., based on CMake targets or the
 * directory layout), which we translate into [ComponentDefinition]s rooted in [directory].
 *
 * Note: Looking into the `build` folder means that a database in `<project>/build` yields
 * components both when visiting `<project>` (rooted there) and later when the directory walk enters
 * `build` (rooted in `build`). Since the walk visits parents first and components are de-duplicated
 * by name, the ones rooted in the project directory win, which keeps translation unit names
 * relative to the project rather than to `build`.
 */
internal fun detectCxxComponents(directory: Path): List<ComponentDefinition> {
    val file = findCompilationDatabase(directory) ?: return listOf()
    val db = tryLoadCompilationDatabase(file) ?: return listOf()
    return db.components.map { (name, files) ->
        ComponentDefinition(name, root = directory, sources = files.map(File::toPath))
    }
}

/**
 * Detects a compilation database for the project, either directly in [directory] or in its `build`
 * folder. The database supplies per-file include paths and preprocessor defines to the C/C++
 * frontend.
 */
internal fun detectCxxSettings(directory: Path): DetectionResult? {
    val file = findCompilationDatabase(directory) ?: return null
    val db = tryLoadCompilationDatabase(file) ?: return null
    return DetectionResult(
        detector = COMPILE_COMMANDS,
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
