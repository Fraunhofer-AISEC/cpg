/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.helpers

import de.fraunhofer.aisec.cpg.graph.AstNode
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.NamespaceDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region

/**
 * Class used to encapsulate functions used for the heuristic comment matching. Comments are matched
 * to the closest AST node with priority on predecessor nodes in the same line as the comment, then
 * successor nodes and lastly enclosing nodes.
 */
class CommentMatcher {

    /**
     * Searches amongst the children of the node to find the child that contains the provided
     * region.
     */
    fun getEnclosingChild(
        node: AstNode,
        location: Region,
        artifactLocation: PhysicalLocation.ArtifactLocation?,
    ): AstNode {
        // If there's an ArtifactLocation specified, it should at least be in the same file.
        val children =
            node.astChildren
                .filter {
                    artifactLocation == null || artifactLocation == it.location?.artifactLocation
                }
                .toMutableList()
        // As some frontends add regional implicit namespaces we have to search amongst its children
        // instead.
        children.addAll(
            children.filterIsInstance<NamespaceDeclaration>().flatMap { namespace ->
                namespace.astChildren.filter { it !in children }
            }
        )
        val enclosing =
            children.firstOrNull {
                val nodeRegion: Region = it.location?.region ?: Region()
                nodeRegion.startLine <= location.startLine &&
                    nodeRegion.endLine >= location.endLine &&
                    (nodeRegion.startLine != location.startLine ||
                        nodeRegion.startColumn <= location.startColumn) &&
                    (nodeRegion.endLine != location.endLine ||
                        nodeRegion.endColumn >= location.endColumn)
            }
        return enclosing ?: node
    }

    /**
     * This function matches a comment to the closest node according to a heuristic: Comments are
     * assigned to the closest successor node on their ast hierarchy level. Only Exception, if they
     * don't have a successor starting in the same line but they have a predecessor in the same
     * line, the comment is matched to that closest predecessor.
     */
    fun matchCommentToNode(
        comment: String,
        location: Region,
        tu: TranslationUnitDeclaration,
        artifactLocation: PhysicalLocation.ArtifactLocation? = null,
    ) {
        var enclosingNode: Node = tu
        var smallestEnclosingNode = getEnclosingChild(tu, location, artifactLocation)
        while (enclosingNode != smallestEnclosingNode) {
            enclosingNode = smallestEnclosingNode
            smallestEnclosingNode =
                getEnclosingChild(smallestEnclosingNode, location, artifactLocation)
        }

        val children =
            smallestEnclosingNode.astChildren
                .filter {
                    artifactLocation == null || artifactLocation == it.location?.artifactLocation
                }
                .toMutableSet()

        // Because we sometimes wrap all elements into a NamespaceDeclaration we have to extract the
        // children with a location
        children.addAll(
            children.filterIsInstance<NamespaceDeclaration>().flatMap { namespace ->
                namespace.astChildren.filter { it !in children }
            }
        )

        // When a child has no location we can not properly consider it for comment matching,
        // however, it might have
        // a child with a location that we want to consider, this can overlap with namespaces but
        // nodes are considered
        // only once in the set
        children.addAll(
            children
                .filter { node -> node.location == null || node.location?.region == Region() }
                .flatMap { locationLess -> locationLess.astChildren.filter { it !in children } }
        )

        // Searching for the closest successor to our comment amongst the children of the smallest
        // enclosing nodes
        val successors =
            children.filter {
                val nodeRegion: Region = it.location?.region ?: Region()
                nodeRegion.startLine >= location.endLine &&
                    (nodeRegion.startLine > location.endLine ||
                        nodeRegion.startColumn >= location.endColumn)
            }
        var closest: Node? =
            successors
                .sortedWith(
                    compareBy(
                        { it.location?.region?.startLine ?: 0 },
                        { it.location?.region?.startColumn ?: 0 },
                    )
                )
                .firstOrNull()
        var closestLine = closest?.location?.region?.startLine ?: (location.endLine + 1)

        // If the closest successor is not in the same line there may be a more adequate predecessor
        // to associate the comment to (Has to be in the same line)
        if (closest == null || closestLine > location.endLine) {
            val predecessor =
                children.filter {
                    val nodeRegion: Region = it.location?.region ?: Region()
                    nodeRegion.endLine <= location.startLine &&
                        (nodeRegion.endLine < location.startLine ||
                            nodeRegion.endColumn <= location.startColumn)
                }
            val closestPredecessor: Node? =
                predecessor
                    .sortedWith(
                        compareBy(
                            { it.location?.region?.endLine ?: 0 },
                            { it.location?.region?.endColumn ?: 0 },
                        )
                    )
                    .lastOrNull()
            closestLine = closestPredecessor?.location?.region?.endLine ?: (location.startLine - 1)
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
