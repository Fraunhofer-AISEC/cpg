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
package de.fraunhofer.aisec.cpg.webconsole

import de.fraunhofer.aisec.codyze.AnalysisResult
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.nodes
import de.fraunhofer.aisec.cpg.passes.concepts.config.ProvideConfigPass
import de.fraunhofer.aisec.cpg.passes.concepts.config.python.PythonStdLibConfigurationPass
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CPGService {
    private var translationResult: TranslationResultJSON? = null

    suspend fun generateCPG(sourceDir: String): TranslationResultJSON =
        withContext(Dispatchers.IO) {
            val path = Path.of(sourceDir)
            val config =
                TranslationConfiguration.builder()
                    .sourceLocations(path.toFile())
                    .defaultPasses()
                    .registerPass<PythonStdLibConfigurationPass>()
                    .registerPass<ProvideConfigPass>()
                    .codeInNodes(true)
                    .registerLanguage<PythonLanguage>()
                    .build()

            val translationManager = TranslationManager.builder().config(config).build()

            val result = translationManager.analyze().get()

            val translationResult =
                TranslationResultJSON(
                    components = result.components.map { it.toJSON() },
                    totalNodes = result.nodes.size,
                    cpgResult = result,
                    sourceDir = config.sourceLocations.first().absolutePath,
                )

            this@CPGService.translationResult = translationResult
            translationResult
        }

    fun getTranslationResult(): TranslationResultJSON? {
        return translationResult
    }

    fun getComponent(componentName: String): ComponentJSON? {
        return translationResult?.components?.find { it.name == componentName }
    }

    fun getTranslationUnit(componentName: String, unitPath: String): TranslationUnitJSON? {
        return getComponent(componentName)?.translationUnits?.find { it.path == unitPath }
    }

    fun getNodesForTranslationUnit(componentName: String, unitPath: String): List<NodeJSON> {
        val result = translationResult ?: return emptyList()

        val cpgResult = result.cpgResult ?: return emptyList()
        val tu =
            cpgResult.components
                .find { it.name == Name(componentName) }
                ?.translationUnits
                ?.find { it.location?.artifactLocation?.uri.toString() == unitPath }
                ?: return emptyList()

        return extractNodes(tu)
    }

    private fun extractNodes(tu: TranslationUnitDeclaration): List<NodeJSON> {
        return tu.nodes.map { it.toJSON() }
    }

    companion object {
        fun fromAnalysisResult(result: AnalysisResult): CPGService {
            val tr = result.translationResult

            val service = CPGService()
            service.translationResult =
                TranslationResultJSON(
                    components = tr.components.map { it.toJSON() },
                    totalNodes = tr.nodes.size,
                    cpgResult = tr,
                    sourceDir = tr.config.sourceLocations.first().absolutePath,
                )
            return service
        }
    }
}
