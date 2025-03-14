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

import de.fraunhofer.aisec.codyze.AnalysisResult
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.nodes
import de.fraunhofer.aisec.cpg.passes.concepts.config.ProvideConfigPass
import de.fraunhofer.aisec.cpg.passes.concepts.config.python.PythonStdLibConfigurationPass
import java.io.File
import java.nio.file.Path
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CPGService {
    private var translationResult: AnalysisResultJSON? = null
    var lastConfiguration: TranslationConfiguration? = null

    suspend fun generateCPG(request: GenerateCPGRequest): AnalysisResultJSON =
        withContext(Dispatchers.IO) {
            val path = Path.of(request.sourceDir)
            val builder =
                TranslationConfiguration.builder()
                    .sourceLocations(path.toFile())
                    .defaultPasses()
                    .loadIncludes(true)
                    .registerPass<PythonStdLibConfigurationPass>()
                    .registerPass<ProvideConfigPass>()
                    .codeInNodes(true)
                    .registerLanguage<PythonLanguage>()

            if (request.includeDir != null) {
                builder.includePath(request.includeDir)
            }

            if (request.topLevel != null) {
                builder.topLevel(File(request.topLevel))
            }

            val config = builder.build()

            generateCPGFromConfig(config)
        }

    fun generateCPGFromConfig(config: TranslationConfiguration): AnalysisResultJSON {
        lastConfiguration = config

        val translationManager = TranslationManager.builder().config(config).build()

        val result = translationManager.analyze().get()

        val translationResult =
            with(result) {
                AnalysisResultJSON(
                    components = result.components.map { it.toJSON() },
                    totalNodes = result.nodes.size,
                    cpgResult = result,
                    analysisResult = null,
                    sourceDir = config.sourceLocations.first().absolutePath,
                    findings = listOf(),
                )
            }

        this@CPGService.translationResult = translationResult
        return translationResult
    }

    fun getTranslationResult(): AnalysisResultJSON? {
        return translationResult
    }

    fun getComponent(componentName: String): ComponentJSON? {
        return translationResult?.components?.find { it.name == componentName }
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
            tu.declarations.filter { it != tu }.map { it.toJSON() }
        }
    }

    companion object {
        fun fromAnalysisResult(result: AnalysisResult): CPGService {
            val tr = result.translationResult

            val service = CPGService()
            with(tr) {
                service.translationResult =
                    AnalysisResultJSON(
                        components = tr.components.map { it.toJSON() },
                        totalNodes = tr.nodes.size,
                        cpgResult = tr,
                        analysisResult = result,
                        sourceDir = tr.config.sourceLocations.first().absolutePath,
                        findings =
                            result.sarif.runs.flatMap {
                                it.results?.map { it.toJSON() } ?: emptyList()
                            },
                    )
                service.lastConfiguration = tr.config
            }
            return service
        }
    }
}
