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
package de.fraunhofer.aisec.cpg.graph.concepts.api

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.Operation

/**
 * Represents an operation performed on a [RestApiConcept]. This operation corresponds to an
 * [HttpMethod] and holds the method's [arguments].
 *
 * @param httpMethod HTTP method (e.g., GET, POST, PUT, DELETE).
 * @param arguments The arguments passed during the operation.
 * @param concept The [RestApiConcept] this operation is associated with.
 * @property invokes A list of other [RestApiOperation]s that this operation invokes.
 */
class RestApiOperation(
    underlyingNode: Node,
    val httpMethod: HttpMethod,
    val arguments: List<Node>,
    override val concept: RestApiConcept,
) : Operation(underlyingNode = underlyingNode, concept = concept) {
    val invokes: MutableList<RestApiOperation> = mutableListOf()
}

enum class HttpMethod {
    GET,
    POST,
    PUT,
    DELETE,
    UNKNOWN,
}
