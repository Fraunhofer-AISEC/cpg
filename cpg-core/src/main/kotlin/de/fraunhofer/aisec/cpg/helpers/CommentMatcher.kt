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
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
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
                .toMutableSet()

        // When a child has no location we can not properly decide if it encloses the comment, we
        // instead consider its children with locations.
        expandCandidatesByLocation(children)

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
        tu: TranslationUnit,
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

        // When a child has no location we can not properly consider it for comment matching,
        // however, instead we consider its contained children that have a location.
        expandCandidatesByLocation(children)

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

    /**
     * Expands the given list of candidates by exploring their children iteratively until all
     * candidates without a location are expanded by their children with a location.
     */
    fun expandCandidatesByLocation(candidates: MutableSet<AstNode>) {
        var locationLess =
            candidates.filter { node -> node.location == null || node.location?.region == Region() }
        while (locationLess.isNotEmpty()) {
            val containedChildren = locationLess.flatMap { it.astChildren }
            locationLess =
                containedChildren
                    .filter { node -> node.location == null || node.location?.region == Region() }
                    .filter { it !in candidates }
            candidates.addAll(containedChildren)
        }
    }
}
