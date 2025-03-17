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
package de.fraunhofer.aisec.codyze.console

import de.fraunhofer.aisec.codyze.AnalysisProject
import de.fraunhofer.aisec.codyze.AnalysisResult
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.nodes
import de.fraunhofer.aisec.cpg.passes.concepts.config.python.PythonStdLibConfigurationPass
import java.io.File
import java.nio.file.Path
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val AD_HOC_PROJECT_NAME = "ad-hoc"

/**
 * Service class for the console. This class is responsible for managing the translation process and
 * storing the results.
 *
 * @property analysisResult The result of the last translation process
 * @property lastProject The [AnalysisProject] of the last run.
 */
class ConsoleService {
    private var analysisResult: AnalysisResultJSON? = null
    var lastProject: AnalysisProject? = null

    /** Analyzes the given source directory and returns the analysis result. */
    suspend fun analyze(request: AnalyzeRequestJSON): AnalysisResultJSON =
        withContext(Dispatchers.IO) {
            val path = Path.of(request.sourceDir)
            val builder =
                TranslationConfiguration.builder()
                    .sourceLocations(path.toFile())
                    .defaultPasses()
                    .loadIncludes(true)
                    .registerPass<PythonStdLibConfigurationPass>()
                    .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage")
                    .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.cxx.CPPLanguage")
                    .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.java.JavaLanguage")
                    .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.golang.GoLanguage")
                    .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.llvm.LLVMIRLanguage")
                    .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage")
                    .optionalLanguage(
                        "de.fraunhofer.aisec.cpg.frontends.typescript.TypeScriptLanguage"
                    )
                    .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.ruby.RubyLanguage")
                    .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.jvm.JVMLanguage")
                    .optionalLanguage("de.fraunhofer.aisec.cpg.frontends.ini.IniFileLanguage")
                    .codeInNodes(true)

            if (request.includeDir != null) {
                builder.includePath(request.includeDir)
            }

            if (request.topLevel != null) {
                builder.topLevel(File(request.topLevel))
            }

            val config = builder.build()

            // Build an ad-hoc project
            val project =
                AnalysisProject(name = AD_HOC_PROJECT_NAME, projectDir = null, config = config)
            analyzeProject(project)
        }

    fun analyzeProject(project: AnalysisProject): AnalysisResultJSON {
        lastProject = project

        val result = project.analyze()

        val json = result.toJSON()
        this@ConsoleService.analysisResult = json
        return json
    }

    fun getTranslationResult(): AnalysisResultJSON? {
        return analysisResult
    }

    fun getComponent(componentName: String): ComponentJSON? {
        return analysisResult?.components?.find { it.name == componentName }
    }

    fun getTranslationUnit(componentName: String, id: String): TranslationUnitJSON? {
        return getComponent(componentName)?.translationUnits?.find { it.id == Uuid.parse(id) }
    }

    fun getNodesForTranslationUnit(
        componentName: String,
        id: String,
        overlayNodes: Boolean,
    ): List<NodeJSON> {
        return getComponent(componentName)
            ?.translationUnits
            ?.find { it.id == Uuid.parse(id) }
            ?.cpgTU
            ?.let { extractNodes(it, overlayNodes) } ?: emptyList()
    }

    private fun extractNodes(
        tu: TranslationUnitDeclaration,
        overlayNodes: Boolean,
    ): List<NodeJSON> {
        return if (overlayNodes) {
            tu.nodes.flatMap { it.overlays }.map { it.toJSON() }
        } else {
            tu.astChildren.filter { it != tu }.map { it.toJSON() }
        }
    }

    companion object {
        fun fromAnalysisResult(result: AnalysisResult): ConsoleService {
            val service = ConsoleService()
            service.analysisResult = result.toJSON()
            service.lastProject = result.project
            return service
        }
    }
}
