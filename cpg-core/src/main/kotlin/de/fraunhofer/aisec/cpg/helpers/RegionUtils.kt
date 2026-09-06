/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.nodes
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import kotlin.math.min
import org.apache.commons.lang3.StringUtils

/**
 * Computes the `[start, end)` char offset range of [subRegion] inside [code], given that `code[0]`
 * corresponds to `nodeRegion.startLine:nodeRegion.startColumn`. [lineBreakSequence] can be used to
 * specify the type of new-line char(s) used on the platform.
 *
 * This performs no bounds-checking beyond clamping `end` to `code.length` (matching the previous
 * behavior of [getCodeOfSubregion]); callers that cannot guarantee a well-formed [subRegion] (e.g.
 * one derived from an untrusted/independently-computed source) must validate the result themselves.
 */
internal fun regionToOffsets(
    code: String,
    nodeRegion: Region,
    subRegion: Region,
    lineBreakSequence: CharSequence = "\n",
): IntRange {
    val start =
        if (subRegion.startLine == nodeRegion.startLine) {
            subRegion.startColumn - nodeRegion.startColumn
        } else {
            (StringUtils.ordinalIndexOf(
                code,
                lineBreakSequence,
                subRegion.startLine - nodeRegion.startLine,
            ) + subRegion.startColumn)
        }
    var end =
        if (subRegion.endLine == nodeRegion.startLine) {
            subRegion.endColumn - nodeRegion.startColumn
        } else {
            (StringUtils.ordinalIndexOf(
                code,
                lineBreakSequence,
                subRegion.endLine - nodeRegion.startLine,
            ) + subRegion.endColumn)
        }

    // Unfortunately, we sometimes have issues with (non)-Unicode characters in code, where the
    // python AST thinks that multiple characters are needed and reports a position that is actually
    // beyond our "end"
    end = min(end, code.length)

    return start until end
}

/**
 * Returns the part of the [code] described by [subRegion], embedded in [nodeRegion]. [newLineType]
 * can be used to specify the type of new-line char(s) used on the platform.
 */
fun getCodeOfSubregion(
    code: String,
    nodeRegion: Region,
    subRegion: Region,
    lineBreakSequence: CharSequence = "\n",
): String {
    val range = regionToOffsets(code, nodeRegion, subRegion, lineBreakSequence)
    return code.substring(range.first, range.last + 1)
}

/**
 * This function returns the [Node]s matching the provided [PhysicalLocation] in the given
 * [TranslationResult].
 *
 * @param location The [PhysicalLocation] to match against.
 * @param clsName The type of [Node] to match against.
 * @return A list of [Node]s that match the provided [PhysicalLocation] and requested type
 *   [clsName].
 */
fun TranslationResult.getNodesByRegion(
    location: PhysicalLocation,
    clsName: String? = null,
): List<Node> {
    return this.nodes.filter { node ->
        node.location == location && (clsName == null || node.javaClass.name == clsName)
    }
}
