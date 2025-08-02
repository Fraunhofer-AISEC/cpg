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
import de.fraunhofer.aisec.cpg.graph.ast.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.concepts.auth.Authentication
import de.fraunhofer.aisec.cpg.graph.concepts.auth.Authorization
import de.fraunhofer.aisec.cpg.graph.concepts.auth.RequestContext
import de.fraunhofer.aisec.cpg.graph.concepts.flows.RemoteEntryPoint
import java.util.Objects

/** Represents a single [HttpEndpoint] on the server */
open class HttpEndpoint(
    underlyingNode: FunctionDeclaration? = null,
    val httpMethod: HttpMethod,
    val path: String,
    val arguments: List<Node>,
    var authentication: Authentication?,
    var authorization: Authorization?,
    var requestContext: RequestContext?,
) : RemoteEntryPoint(underlyingNode = underlyingNode) {
    override fun equals(other: Any?): Boolean {
        return other is HttpEndpoint &&
            super.equals(other) &&
            other.httpMethod == this.httpMethod &&
            other.path == this.path &&
            other.arguments == this.arguments &&
            other.authentication == this.authentication &&
            other.authorization == this.authorization &&
            other.requestContext == this.requestContext
    }

    override fun hashCode() =
        Objects.hash(
            super.hashCode(),
            httpMethod,
            path,
            arguments,
            authentication,
            authorization,
            requestContext,
        )
}

enum class HttpMethod {
    GET,
    POST,
    PUT,
    HEAD,
    PATCH,
    OPTIONS,
    CONNECT,
    TRACE,
    DELETE,
    UNKNOWN,
}

/** Base class for operations on an [HttpEndpoint]. */
abstract class HttpEndpointOperation(underlyingNode: Node, concept: Concept) :
    Operation(underlyingNode, concept)
