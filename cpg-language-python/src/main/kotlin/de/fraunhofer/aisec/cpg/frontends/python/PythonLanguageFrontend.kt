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
package de.fraunhofer.aisec.cpg.frontends.python

import de.fraunhofer.aisec.cpg.ExperimentalPython
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
import java.nio.file.Path
import jep.JepException
import jep.SubInterpreter

@ExperimentalPython
class PythonLanguageFrontend(config: TranslationConfiguration, scopeManager: ScopeManager?) :
    LanguageFrontend(config, scopeManager, ".") {
    companion object {
        @kotlin.jvm.JvmField var PY_EXTENSIONS: List<String> = listOf(".py")
    }
    private val jep = JepSingleton // configure Jep

    @Throws(TranslationException::class)
    override fun parse(file: File): TranslationUnitDeclaration {
        TypeManager.getInstance().setLanguageFrontend(this)
        return parseInternal(file.readText(Charsets.UTF_8), file.path)
    }

    override fun <T> getCodeFromRawNode(astNode: T): String? {
        // will be invoked by native function
        return null
    }

    override fun <T> getLocationFromRawNode(astNode: T): PhysicalLocation? {
        // will be invoked by native function
        return null
    }

    override fun <S, T> setComment(s: S, ctx: T) {
        // will be invoked by native function
    }

    private fun parseInternal(code: String, path: String): TranslationUnitDeclaration {
        // check, if the cpg.py is either directly available in the current directory or in the
        // src/main/python folder
        val modulePath = Path.of("cpg.py")

        val possibleLocations =
            listOf(
                Path.of(".").resolve(modulePath),
                Path.of("src/main/python").resolve(modulePath),
                Path.of("cpg-library/src/main/python").resolve(modulePath)
            )

        var found = false

        var entryScript: Path? = null
        possibleLocations.forEach {
            if (it.toFile().exists()) {
                found = true
                entryScript = it.toAbsolutePath()
            }
        }

        val tu: TranslationUnitDeclaration
        var interp: SubInterpreter? = null
        try {
            interp = SubInterpreter(jep.config)

            // load script
            if (found) {
                interp.runScript(entryScript.toString())
            } else {
                // fall back to the cpg.py in the class's resources
                val classLoader = javaClass
                val pyInitFile = classLoader.getResource("/cpg.py")
                interp.exec(pyInitFile?.readText())
            }

            // run python function parse_code()
            tu = interp.invoke("parse_code", code, path, this) as TranslationUnitDeclaration
        } catch (e: JepException) {
            e.printStackTrace()
            throw TranslationException("Python failed with message: $e")
        } catch (e: Exception) {
            throw e
        } finally {
            interp?.close()
        }

        return tu
    }

    fun getEnclosingChild(node: Node, location: Region): Node {
        var children = SubgraphWalker.getAstChildren(node)
        children.addAll(
            children.filterIsInstance<NamespaceDeclaration>().flatMap {
                SubgraphWalker.getAstChildren(it).filter { !children.contains(it) }
            }
        )
        var enclosing =
            children
                .filter {
                    val nodeRegion: Region = it.location?.let { it.region } ?: Region()
                    nodeRegion.startLine <= location.startLine &&
                        nodeRegion.endLine >= location.endLine &&
                        (nodeRegion.startLine != location.startLine ||
                            nodeRegion.startColumn <= location.startColumn) &&
                        (nodeRegion.endLine != location.endLine ||
                            nodeRegion.endColumn >= location.endColumn)
                }
                .firstOrNull()
        return enclosing ?: node
    }

    /**
     * This function matches a comment to the closest node according to a heuristic: Comments are
     * assigned to the closest successor node on their ast hierarchy level. Only Exception, if they
     * don't have a successor starting in the same line but they have a predecessor in the same
     * line, the comment is matched to that closest predecessor.
     */
    fun matchCommentToNode(comment: String, location: Region, tu: TranslationUnitDeclaration) {
        var enclosingNode: Node = tu
        var smallestEnclosingNode: Node = getEnclosingChild(tu, location)
        while (enclosingNode != smallestEnclosingNode) {
            enclosingNode = smallestEnclosingNode
            smallestEnclosingNode = getEnclosingChild(smallestEnclosingNode, location)
        }

        var children = SubgraphWalker.getAstChildren(smallestEnclosingNode)

        // Because we sometimes wrap all elements into a NamespaceDeclaration we have to extract the
        // children with a location
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
            if (closestPredecessor != null && closestLine == location.startLine) {
                closest = closestPredecessor
            }
        }
        // If we have neither have identified a predecessor nor a successor, we associate the
        // comment to the enclosing node
        if (closest == null) {
            closest = smallestEnclosingNode
        }

        closest.comment = (closest.comment ?: "") + comment
    }
}
