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

/**
 * Detects components based on a [COMPILE_COMMANDS] file directly inside [directory]. The
 * compilation database groups its entries into components (e.g., based on CMake targets or the
 * directory layout), which we translate into [ComponentDefinition]s.
 */
internal fun detectCxxComponents(directory: Path): List<ComponentDefinition> {
    val file = directory.resolve(COMPILE_COMMANDS)
    if (!file.exists()) {
        return listOf()
    }

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
    val file =
        sequenceOf(
                directory.resolve(COMPILE_COMMANDS),
                directory.resolve("build").resolve(COMPILE_COMMANDS),
            )
            .firstOrNull { it.exists() } ?: return null

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
