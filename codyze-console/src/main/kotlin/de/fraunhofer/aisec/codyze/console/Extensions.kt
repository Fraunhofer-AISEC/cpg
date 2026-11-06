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

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.sarif.Region

/**
 * Returns the innermost node of this translation unit whose region contains the given position, so
 * that clicking anywhere in the code selects the most specific node. Among nodes with the same
 * region, explicit nodes are preferred over implicit ones and deeper nodes over their parents.
 *
 * If an end position is given, the node has to contain the whole range from the position to the end
 * (whose column is exclusive), e.g. the call that contains a selected text.
 */
fun TranslationUnit.nodeAt(
    line: Int,
    column: Int,
    endLine: Int? = null,
    endColumn: Int? = null,
): Node? {
    var best: Node? = null
    var bestSpan = Long.MAX_VALUE
    // The nodes are in pre-order, so among nodes with the same region, a later one is deeper
    for (node in nodes) {
        if (node is TranslationUnit || !node.isInFileOf(this)) continue
        val region = node.location?.region ?: continue
        if (!region.contains(line, column)) continue
        if (
            endLine != null &&
                endColumn != null &&
                !region.contains(endLine, maxOf(endColumn - 1, 1))
        )
            continue
        // The size of the region, where any additional line outweighs any number of columns
        val span =
            (region.endLine - region.startLine).toLong() * Int.MAX_VALUE +
                (region.endColumn - region.startColumn)
        if (span < bestSpan || span == bestSpan && (best?.isImplicit == true || !node.isImplicit)) {
            best = node
            bestSpan = span
        }
    }
    return best
}

private fun Region.contains(line: Int, column: Int): Boolean {
    if (line < startLine || line > endLine) return false
    if (line == startLine && column < startColumn) return false
    if (line == endLine && column >= endColumn) return false
    return true
}

/**
 * Whether this node is located in the file of the given translation unit. The AST of a translation
 * unit can also contain nodes of other files, e.g. the declarations of included headers in C (see
 * `loadIncludes`), whose positions refer to these files. If the unit has no file, any node with a
 * location is considered to be in it.
 */
fun Node.isInFileOf(translationUnit: TranslationUnit): Boolean {
    val location = location ?: return false
    val file = translationUnit.location?.artifactLocation ?: return true
    return location.artifactLocation == file
}
