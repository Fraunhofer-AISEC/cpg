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
package de.fraunhofer.aisec.cpg.frontends

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.Namespace
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.sarif.Region
import kotlin.math.min
import org.slf4j.LoggerFactory

class FrontendUtils {

    companion object {

        private val LOGGER = LoggerFactory.getLogger(FrontendUtils::class.java)

        /**
         * The proper columns are computed for the given nodeOffset and its length
         *
         * @param fileContent the text in the file, needed to see the whitespaces
         * @param nodeLength Length of the node
         * @param nodeOffset start position of the node as character index
         * @params startingLineNumber line where the starting position is in
         * @param endingLineNumber line where the node should end
         */
        fun parseColumnPositionsFromFile(
            fileContent: String,
            nodeLength: Int,
            nodeOffset: Int,
            startingLineNumber: Int,
            endingLineNumber: Int,
        ): Region? {
            // Get start column by stepping backwards from begin of node to first occurrence of
            // '\n'
            var startColumn = 1
            for (i in nodeOffset - 1 downTo 2) {
                if (i >= fileContent.length) {
                    // Fail gracefully, so that we can at least find out why this fails
                    LOGGER.warn(
                        "Requested index {} exceeds length of translation unit code ({})",
                        i,
                        fileContent.length,
                    )
                    return null
                }
                if (fileContent[i] == '\n') {
                    break
                }
                startColumn++
            }

            val endColumn = getEndColumnIndex(fileContent, nodeOffset + nodeLength)
            return Region(startingLineNumber, startColumn, endingLineNumber, endColumn)
        }

        /**
         * Searches in posPrefix to the left until first occurrence of line break and returns the
         * number of characters.
         *
         * This corresponds to the column number of "end" within "posPrefix".
         *
         * @param posPrefix
         * - the positional prefix, which is the string before the column and contains the column
         *   defining newline.
         */
        private fun getEndColumnIndex(posPrefix: String, end: Int): Int {
            var mutableEnd = end
            var column = 1

            // In case the current element goes until EOF, we need to back up "end" by one.
            try {
                if (mutableEnd - 1 >= posPrefix.length || posPrefix[mutableEnd - 1] == '\n') {
                    mutableEnd = min(mutableEnd - 1, posPrefix.length - 1)
                }
            } catch (e: ArrayIndexOutOfBoundsException) {
                LanguageFrontend.log.error("could not update end ", e)
            }
            for (i in mutableEnd - 1 downTo 2) {
                if (posPrefix[i] == '\n') {
                    break
                }
                column++
            }
            return column
        }

        /**
         * This function matches a comment to the closest node according to a heuristic: Comments
         * are assigned to the closest successor node on their ast hierarchy level. Only Exception,
         * if they don't have a successor starting in the same line but they have a predecessor in
         * the same line, the comment is matched to that closest predecessor.
         */
        fun matchCommentToNode(comment: String, location: Region, tu: TranslationUnit) {
            val nodes = SubgraphWalker.flattenAST(tu)

            // Get a List of all Nodes that enclose the comment
            var enclosingNodes =
                nodes.filter {
                    val nodeRegion: Region = it.location?.region ?: Region()
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

            val children = smallestEnclosingNode.astChildren.toMutableList()

            // Because in GO we wrap all elements into a Namespace we have to extract the
            // natural children
            children.addAll(
                children.filterIsInstance<Namespace>().flatMap { namespace ->
                    namespace.astChildren.filter { it !in children }
                }
            )

            // Searching for the closest successor to our comment amongst the children of the
            // smallest
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
            var closestLine = closest?.location?.region?.startLine ?: location.endLine + 1

            // If the closest successor is not in the same line there may be a more adequate
            // predecessor to associate the comment to (Has to be in the same line)
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
                closestLine =
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
    }
}
