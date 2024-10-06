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

import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.sarif.Region
import kotlin.math.min
import org.apache.commons.lang3.StringUtils

/**
 * To prevent issues with different newline types and formatting.
 *
 * @param multilineCode The newline type is extracted from the code assuming it contains newlines
 * @return the String of the newline or \n as default
 */
fun getNewLineType(multilineCode: String, region: Region? = null): String {
    var code = multilineCode
    region?.let {
        if (it.startLine != it.endLine) {
            code = code.substring(0, code.length - it.endColumn + 1)
        }
    }

    val nls = listOf("\n\r", "\r\n", "\n")
    for (nl in nls) {
        if (code.endsWith(nl)) {
            return nl
        }
    }
    LanguageFrontend.log.debug("Could not determine newline type. Assuming \\n.")
    return "\n"
}

fun getCodeOfSubregion(code: String, nodeRegion: Region, subRegion: Region): String {
    val nlType = getNewLineType(code, nodeRegion)
    val start =
        if (subRegion.startLine == nodeRegion.startLine) {
            subRegion.startColumn - nodeRegion.startColumn
        } else {
            (StringUtils.ordinalIndexOf(code, nlType, subRegion.startLine - nodeRegion.startLine) +
                subRegion.startColumn)
        }
    var end =
        if (subRegion.endLine == nodeRegion.startLine) {
            subRegion.endColumn - nodeRegion.startColumn
        } else {
            (StringUtils.ordinalIndexOf(code, nlType, subRegion.endLine - nodeRegion.startLine) +
                subRegion.endColumn)
        }

    // Unfortunately, we sometimes have issues with (non)-Unicode characters in code, where the
    // python AST thinks that multiple characters are needed and reports a position that is actually
    // beyond our "end"
    end = min(end, code.length)
    return code.substring(start, end)
}
