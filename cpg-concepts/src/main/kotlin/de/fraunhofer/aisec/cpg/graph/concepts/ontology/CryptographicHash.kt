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
package de.fraunhofer.aisec.cpg.graph.concepts.ontology

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.manualExtensions.HashFunction
import de.fraunhofer.aisec.cpg.graph.edges.flows.insertNodeAfterwardInDFGPath
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String

open class CryptographicHash(
    val algorithm: String?,
    val hashFunction: HashFunction,
    val usesSalt: Boolean?,
    linkedConcept: Confidentiality,
    underlyingNode: Node? = null,
) : CryptographicOperation(linkedConcept, underlyingNode) {
    override fun equals(other: Any?): Boolean =
        other is CryptographicHash &&
            super.equals(other) &&
            other.hashFunction == this.hashFunction &&
            other.algorithm == this.algorithm &&
            other.usesSalt == this.usesSalt

    override fun hashCode(): Int = Objects.hash(super.hashCode(), algorithm, usesSalt)

    override fun setDFG() {
        underlyingNode?.insertNodeAfterwardInDFGPath(this)
    }
}
