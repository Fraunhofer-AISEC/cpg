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
package de.fraunhofer.aisec.cpg.graph.concepts.http

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.concepts.auth.Authentication
import java.util.Objects

/** Represents an [HttpClient]. */
class HttpClient(
    underlyingNode: Node,
    val isTLS: Boolean? = false,
    val authentication: Authentication? = null,
) : Concept(underlyingNode = underlyingNode) {
    override fun equals(other: Any?): Boolean {
        return other is HttpClient &&
            super.equals(other) &&
            other.isTLS == this.isTLS &&
            other.authentication == this.authentication
    }

    override fun hashCode() = Objects.hash(super.hashCode(), isTLS, authentication)
}

/** Base class for operations on an [HttpClient]. */
abstract class HttpClientOperation(underlyingNode: Node, concept: Concept) :
    Operation(underlyingNode = underlyingNode, concept = concept)
