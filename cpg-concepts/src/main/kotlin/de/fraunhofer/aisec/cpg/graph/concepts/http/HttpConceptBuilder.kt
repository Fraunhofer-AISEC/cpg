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

import de.fraunhofer.aisec.cpg.graph.MetadataProvider
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.concepts.auth.Authentication
import de.fraunhofer.aisec.cpg.graph.concepts.auth.Authorization
import de.fraunhofer.aisec.cpg.graph.concepts.newConcept
import de.fraunhofer.aisec.cpg.graph.concepts.newOperation
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration

/**
 * Creates a new [HttpClient] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param isTLS Whether the client uses TLS.
 * @param authentication The [Authentication] method used by the client.
 * @param connect If `true`, the created [Concept] will be connected to the underlying node by
 *   setting its `underlyingNode`.
 * @return The created [HttpClient] concept.
 */
fun MetadataProvider.newHttpClient(
    underlyingNode: Node,
    isTLS: Boolean?,
    authentication: Authentication?,
    connect: Boolean,
) =
    newConcept(
        { HttpClient(isTLS = isTLS, authentication = authentication) },
        underlyingNode = underlyingNode,
        connect = connect,
    )

/**
 * Creates a new [HttpRequestHandler] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param basePath The base path of the [HttpRequestHandler].
 * @param endpoints A list of the [HttpEndpoint]s exposed by the [HttpRequestHandler].
 * @param connect If `true`, the created [Concept] will be connected to the underlying node by
 *   setting its `underlyingNode`.
 * @return The created [HttpRequestHandler] concept.
 */
fun MetadataProvider.newHttpRequestHandler(
    underlyingNode: Node,
    basePath: String,
    endpoints: MutableList<HttpEndpoint>,
    connect: Boolean,
) =
    newConcept(
        { HttpRequestHandler(basePath = basePath, endpoints = endpoints) },
        underlyingNode = underlyingNode,
        connect = connect,
    )

/**
 * Creates a new [HttpEndpoint] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param httpMethod The [HttpMethod] the created [HttpEndpoint] listens to.
 * @param path The path of the created [HttpEndpoint].
 * @param arguments A list of the [Node]s representing the arguments passed to the [HttpEndpoint].
 * @param authentication The [Authentication] method used by the [HttpEndpoint].
 * @param connect If `true`, the created [Concept] will be connected to the underlying node by
 *   setting its `underlyingNode`.
 * @return The created [HttpEndpoint] concept.
 */
fun MetadataProvider.newHttpEndpoint(
    underlyingNode: FunctionDeclaration,
    httpMethod: HttpMethod,
    path: String,
    arguments: List<Node>,
    authentication: Authentication?,
    authorization: Authorization?,
    connect: Boolean,
) =
    newConcept(
        {
            HttpEndpoint(
                httpMethod = httpMethod,
                path = path,
                arguments = arguments,
                authentication = authentication,
                authorization = authorization,
            )
        },
        underlyingNode = underlyingNode,
        connect = connect,
    )

/**
 * Creates a new [HttpRequest] operation for the given [HttpEndpoint].
 *
 * @param underlyingNode The underlying [Node] representing the request.
 * @param concept The [HttpClient] concept this operation belongs to.
 * @param connect If `true`, the created [Operation] will be connected to the underlying node by
 *   setting its `underlyingNode` and inserting it in the EOG , to [concept] by its edge
 *   [Concept.ops].
 * @return The created [HttpRequest] concept.
 */
fun MetadataProvider.newHttpRequest(
    underlyingNode: Node,
    concept: HttpClient,
    url: String,
    arguments: List<Node>,
    httpMethod: HttpMethod,
    connect: Boolean,
) =
    newOperation(
        { concept ->
            HttpRequest(
                url = url,
                arguments = arguments,
                httpMethod = httpMethod,
                concept = concept,
            )
        },
        underlyingNode = underlyingNode,
        concept = concept,
        connect = connect,
    )

/**
 * Creates a new [RegisterHttpEndpoint] operation for the given [HttpEndpoint].
 *
 * @param underlyingNode The underlying [Node] registering the endpoint method.
 * @param concept The [Concept] to which this operation belongs.
 * @param httpEndpoint The [HttpEndpoint] which is registered by this operation.
 * @param connect If `true`, the created [Operation] will be connected to the underlying node by
 *   setting its `underlyingNode` and inserting it in the EOG , to [concept] by its edge
 *   [Concept.ops].
 * @return The created [RegisterHttpEndpoint] concept.
 */
fun MetadataProvider.newRegisterHttpEndpoint(
    underlyingNode: Node,
    concept: Concept,
    httpEndpoint: HttpEndpoint,
    connect: Boolean,
) =
    newOperation(
        { concept -> RegisterHttpEndpoint(concept = concept, httpEndpoint = httpEndpoint) },
        underlyingNode = underlyingNode,
        concept = concept,
        connect = connect,
    )
