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
 * Returns the part of the [code] described by [subRegion], embedded in [nodeRegion]. [newLineType]
 * can be used to specify the type of new-line char(s) used on the platform.
 */
fun getCodeOfSubregion(
    code: String,
    nodeRegion: Region,
    subRegion: Region,
    lineBreakSequence: CharSequence = "\n",
): String {
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
    return code.substring(start, end)
}

// TODO: not sure if this is the right place for this function
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
