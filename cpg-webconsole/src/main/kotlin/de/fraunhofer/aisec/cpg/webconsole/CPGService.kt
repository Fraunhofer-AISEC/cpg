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

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.nodes
import java.io.File
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CPGService {
    private var translationResult: TranslationResult? = null

    suspend fun generateCPG(sourceDir: String): TranslationResult =
        withContext(Dispatchers.IO) {
            val path = Path.of(sourceDir)
            val config =
                TranslationConfiguration.builder()
                    .sourceLocations(path.toFile())
                    .defaultPasses()
                    .codeInNodes(true)
                    .registerLanguage<PythonLanguage>()
                    .build()

            val translationManager = TranslationManager.builder().config(config).build()

            val result = translationManager.analyze().get()

            // Convert to our simplified model
            val components =
                result.components.map { component ->
                    Component(
                        name = component.name.toString(),
                        translationUnits =
                            component.translationUnits.map { tu ->
                                TranslationUnit(
                                    name = tu.name.toString(),
                                    path = sourceDir,
                                    code = loadSourceCode(tu, sourceDir),
                                )
                            },
                    )
                }

            val translationResult =
                TranslationResult(
                    components = components,
                    totalNodes = result.nodes.size,
                    cpgResult = result,
                )

            this@CPGService.translationResult = translationResult
            translationResult
        }

    fun getTranslationResult(): TranslationResult? {
        return translationResult
    }

    fun getComponent(componentName: String): Component? {
        return translationResult?.components?.find { it.name == componentName }
    }

    fun getTranslationUnit(componentName: String, unitPath: String): TranslationUnit? {
        return getComponent(componentName)?.translationUnits?.find { it.path == unitPath }
    }

    fun getNodesForTranslationUnit(componentName: String, unitPath: String): List<NodeInfo> {
        val result = translationResult ?: return emptyList()

        val cpgResult = result.cpgResult ?: return emptyList()
        val tu =
            cpgResult.components
                .find { it.name == Name(componentName) }
                ?.translationUnits
                ?.find { it.name.toString() == unitPath } ?: return emptyList()

        return extractNodes(tu)
    }

    private fun extractNodes(tu: TranslationUnitDeclaration): List<NodeInfo> {
        val nodes = mutableListOf<NodeInfo>()

        // Extract nodes with location information
        fun traverse(node: de.fraunhofer.aisec.cpg.graph.Node) {
            val location = node.location
            if (location != null && location.region != null) {
                nodes.add(
                    NodeInfo(
                        id = node.id.toString(),
                        type = node.javaClass.simpleName,
                        startLine = location.region.startLine,
                        startColumn = location.region.startColumn,
                        endLine = location.region.endLine,
                        endColumn = location.region.endColumn,
                        code = node.code ?: "",
                        name = node.name.toString(),
                    )
                )
            }

            node.astChildren.forEach { traverse(it) }
        }

        traverse(tu)
        return nodes
    }

    private fun loadSourceCode(tu: TranslationUnitDeclaration, sourceDir: String): String {
        val path = sourceDir
        return try {
            File(path).readText()
        } catch (e: Exception) {
            ""
        }
    }
}
