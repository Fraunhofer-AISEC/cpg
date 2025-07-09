/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.sarif

import java.util.*

/** Code source location, in a SASP/SARIF-compliant "Region" format. */
class Region(
    @JvmField var startLine: Int = -1,
    @JvmField var startColumn: Int = -1,
    var endLine: Int = -1,
    var endColumn: Int = -1,
) : Comparable<Region> {

    override fun toString(): String {
        return "$startLine:$startColumn-$endLine:$endColumn"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Region) return false
        return startLine == other.startLine &&
            startColumn == other.startColumn &&
            endLine == other.endLine &&
            endColumn == other.endColumn
    }

    override fun compareTo(other: Region): Int {
        var comparisonValue: Int
        if (startLine.compareTo(other.startLine).also { comparisonValue = it } != 0)
            return comparisonValue
        if (startColumn.compareTo(other.startColumn).also { comparisonValue = it } != 0)
            return comparisonValue
        if (endLine.compareTo(other.endLine).also { comparisonValue = it } != 0)
            return -comparisonValue
        return if (endColumn.compareTo(other.endColumn).also { comparisonValue = it } != 0)
            -comparisonValue
        else comparisonValue
    }

    override fun hashCode() = Objects.hash(startColumn, startLine, endColumn, endLine)
}
