/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.mcp

import de.fraunhofer.aisec.cpg.InferenceConfiguration
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.passes.ControlDependenceGraphPass
import de.fraunhofer.aisec.cpg.passes.ControlFlowSensitiveDFGPass
import de.fraunhofer.aisec.cpg.passes.PrepareSerialization
import de.fraunhofer.aisec.cpg.passes.ProgramDependenceGraphPass
import de.fraunhofer.aisec.cpg.passes.concepts.file.python.PythonFileConceptPass
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.collections.forEach

private const val DEBUG_PARSER = true

/** Checks if all elements in the parameter are a valid file and returns a list of files. */
private fun getFilesOfList(filenames: Collection<String>): List<File> {
    val filePaths = filenames.map { Paths.get(it).toAbsolutePath().normalize().toFile() }
    filePaths.forEach { require(it.exists()) { "Please use a correct path. It was: ${it.path}" } }
    return filePaths
}

fun setupTranslationConfiguration(
    topLevel: File?,
    files: Collection<String>,
    includePaths: List<Path>,
    includesFile: File? = null,
    maxComplexity: Int = -1,
    loadIncludes: Boolean = true,
    exclusionPatterns: Collection<String> = listOf(),
    useUnityBuild: Boolean = false,
): TranslationConfiguration {
    val translationConfiguration =
        TranslationConfiguration.builder()
            .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage")
            .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.cxx.CPPLanguage")
            .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.java.JavaLanguage")
            .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.golang.GoLanguage")
            .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.llvm.LLVMIRLanguage")
            .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage")
            .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.typescript.TypeScriptLanguage")
            .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.ruby.RubyLanguage")
            .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.jvm.JVMLanguage")
            .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.ini.IniFileLanguage")
            .loadIncludes(loadIncludes)
            .exclusionPatterns(*exclusionPatterns.toTypedArray())
            .addIncludesToGraph(loadIncludes)
            .debugParser(DEBUG_PARSER)
            .useUnityBuild(useUnityBuild)
            .useParallelPasses(false)

    topLevel?.let { translationConfiguration.topLevel(it) }

    if (maxComplexity != -1) {
        translationConfiguration.configurePass<ControlFlowSensitiveDFGPass>(
            ControlFlowSensitiveDFGPass.Configuration(maxComplexity = maxComplexity)
        )
    }

    includePaths.forEach { translationConfiguration.includePath(it) }

    val filePaths = getFilesOfList(files)
    translationConfiguration.sourceLocations(filePaths)

    translationConfiguration.defaultPasses()
    translationConfiguration.registerPass<ControlDependenceGraphPass>()
    translationConfiguration.registerPass<ProgramDependenceGraphPass>()
    translationConfiguration.registerPass<PythonFileConceptPass>()

    translationConfiguration.registerPass(PrepareSerialization::class)

    includesFile?.let { theFile ->
        val baseDir = File(theFile.toString()).parentFile?.toString() ?: ""
        theFile
            .inputStream()
            .bufferedReader()
            .lines()
            .map(String::trim)
            .map { if (Paths.get(it).isAbsolute) it else Paths.get(baseDir, it).toString() }
            .forEach { translationConfiguration.includePath(it) }
    }

    translationConfiguration.inferenceConfiguration(
        InferenceConfiguration.builder().inferRecords(true).build()
    )
    return translationConfiguration.build()
}
