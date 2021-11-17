/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.golang

import de.fraunhofer.aisec.cpg.ExperimentalGolang
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.declarations.NamespaceDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.scopes.ScopeManager
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.io.File
import kotlin.Throws

@ExperimentalGolang
class GoLanguageFrontend(config: TranslationConfiguration, scopeManager: ScopeManager?) :
    LanguageFrontend(config, scopeManager, ".") {
    companion object {
        @kotlin.jvm.JvmField var GOLANG_EXTENSIONS: List<String> = listOf(".go")

        init {
            System.loadLibrary("cpgo")
        }
    }

    @Throws(TranslationException::class)
    override fun parse(file: File): TranslationUnitDeclaration {
        TypeManager.getInstance().setLanguageFrontend(this)

        return parseInternal(file.readText(Charsets.UTF_8), file.path, config.topLevel.absolutePath)
    }

    /**
     * This function matches a comment to the closest node according to a heuristic: Comments are
     * assigned to the closest successor node on their ast hierarchy level. Only Exception, if they
     * don't have a successor starting in the same line but they have a predecessor in the same
     * line, the comment is matched to that closest predecessor.
     */
    fun matchCommentToNode(comment: String, location: Region, tu: TranslationUnitDeclaration) {
        val nodes: List<Node> = SubgraphWalker.flattenAST(tu)

        // Get a List of all Nodes that enclose the comment
        var enclosingNodes =
            nodes.filter {
                val nodeRegion: Region = it.location?.let { it.region } ?: Region()
                nodeRegion.startLine <= location.startLine &&
                    nodeRegion.endLine >= location.endLine &&
                    (nodeRegion.startLine != location.startLine ||
                        nodeRegion.startColumn <= location.startColumn) &&
                    (nodeRegion.endLine != location.endLine ||
                        nodeRegion.endColumn >= location.endColumn)
            }
        if (!enclosingNodes.contains(tu)) {
            enclosingNodes += tu
        }

        // Order then by code length to find the nearest ast-parent
        val smallestEnclosingNode =
            enclosingNodes.sortedWith(compareBy { it.code?.length ?: 10000 }).first()

        var children = SubgraphWalker.getAstChildren(smallestEnclosingNode)

        // Because in GO we wrap all elements into a NamespaceDeclaration we have to extract the
        // natural children
        children.addAll(
            children.filterIsInstance<NamespaceDeclaration>().flatMap {
                SubgraphWalker.getAstChildren(it).filter { !children.contains(it) }
            }
        )

        // Searching for the closest successor to our comment amongst the children of the smallest
        // enclosing nodes
        var successors =
            children.filter {
                val nodeRegion: Region = it.location?.region?.let { it } ?: Region()
                nodeRegion.startLine >= location.endLine &&
                    (nodeRegion.startLine > location.endLine ||
                        nodeRegion.startColumn >= location.endColumn)
            }
        var closest: Node? =
            successors
                .sortedWith(
                    compareBy(
                        { it.location?.region?.startLine ?: 0 },
                        { it.location?.region?.startColumn ?: 0 }
                    )
                )
                .firstOrNull()
        val closestLine = closest?.location?.region?.startLine ?: location.endLine + 1

        // If the closest successor is not in the same line there may be a more adequate predecessor
        // to associated the
        // comment to (Has to be in the same line)
        if (closest == null || closestLine > location.endLine) {
            var predecessor =
                children.filter {
                    val nodeRegion: Region = it.location?.region?.let { it } ?: Region()
                    nodeRegion.endLine <= location.startLine &&
                        (nodeRegion.endLine < location.startLine ||
                            nodeRegion.endColumn <= location.startColumn)
                }
            val closestPredecessor: Node? =
                predecessor
                    .sortedWith(
                        compareBy(
                            { it.location?.region?.endLine ?: 0 },
                            { it.location?.region?.endColumn ?: 0 }
                        )
                    )
                    .lastOrNull()
            val closestLine =
                closestPredecessor?.location?.region?.endLine ?: location.startLine - 1
            if (closestPredecessor != null && closestLine == location.startLine)
                closest = closestPredecessor
        }
        // If we have neither have identified a predecessor nor a successor, we associate the
        // comment to the enclosing node
        if (closest == null) {
            closest = smallestEnclosingNode
        }

        closest.comment = (closest.comment ?: "") + comment
    }

    override fun <T> getCodeFromRawNode(astNode: T): String? {
        // this is handled by native code
        return null
    }

    override fun <T> getLocationFromRawNode(astNode: T): PhysicalLocation? {
        // this is handled by native code
        return null
    }

    override fun <S, T> setComment(s: S, ctx: T) {}

    private external fun parseInternal(
        s: String?,
        path: String,
        topLevel: String
    ): TranslationUnitDeclaration
}
