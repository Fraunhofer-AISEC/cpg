/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String

public open class CodeRegion(
    code: String?,
    public val endColumn: Int?,
    public val endLine: Int?,
    public val `file`: String?,
    public val startColumn: Int?,
    public val startLine: Int?,
    underlyingNode: Node? = null,
) : Functionality(underlyingNode) {
    init {
        code?.let { this.code = it }
    }

    override fun equals(other: Any?): Boolean =
        other is CodeRegion &&
            super.equals(other) &&
            other.code == this.code &&
            other.endColumn == this.endColumn &&
            other.endLine == this.endLine &&
            other.file == this.file &&
            other.startColumn == this.startColumn &&
            other.startLine == this.startLine

    override fun hashCode(): Int =
        Objects.hash(super.hashCode(), code, endColumn, endLine, file, startColumn, startLine)
}
