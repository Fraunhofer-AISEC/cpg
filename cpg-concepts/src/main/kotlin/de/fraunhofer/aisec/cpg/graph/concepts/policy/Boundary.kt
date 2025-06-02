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
package de.fraunhofer.aisec.cpg.graph.concepts.policy

import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.concepts.http.HttpEndpoint

/**
 * Represents a boundary in data processing, which can be used to define the scope of a policy or
 * the separation between different policies.
 *
 * For example, a boundary could be defined around an [HttpEndpoint], so that the policy applies
 * once data either comes in or goes out of the HTTP endpoint.
 */
class Boundary() : Concept() {

    /** All exit operations that are part of this boundary. */
    val exits: List<ExitBoundary>
        get() {
            return ops.filterIsInstance<ExitBoundary>()
        }
}

/**
 * Represents an exit operation that is part of a [Boundary]. This operation is used to define the
 * point at which data leaves the boundary.
 */
class ExitBoundary(concept: Boundary) : Operation(concept = concept) {
    init {
        concept.ops += this
    }

    override fun setDFG() {
        underlyingNode?.let { this.prevDFG.add(it) }
    }
}
