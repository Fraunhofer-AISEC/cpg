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
import de.fraunhofer.aisec.cpg.graph.concepts.auth.IdentityAccessManagement
import de.fraunhofer.aisec.cpg.graph.concepts.newConcept
import de.fraunhofer.aisec.cpg.graph.concepts.newOperation
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration

/**
 * Creates a new [HttpClient] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param isTLS Whether the client uses TLS.
 * @param authentication The [IdentityAccessManagement] method used by the client.
 * @return The created [HttpClient] concept.
 */
fun MetadataProvider.newHttpClient(
    underlyingNode: Node,
    isTLS: Boolean?,
    authentication: IdentityAccessManagement?,
) =
    newConcept(
        { HttpClient(it, isTLS = isTLS, authentication = authentication) },
        underlyingNode = underlyingNode,
    )

/**
 * Creates a new [HttpRequestHandler] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param basePath The base path of the [HttpRequestHandler].
 * @param endpoints A list of the [HttpEndpoint]s exposed by the [HttpRequestHandler].
 * @return The created [HttpRequestHandler] concept.
 */
fun MetadataProvider.newHttpRequestHandler(
    underlyingNode: Node,
    basePath: String,
    endpoints: MutableList<HttpEndpoint>,
) =
    newConcept(
        { HttpRequestHandler(it, basePath = basePath, endpoints = endpoints) },
        underlyingNode = underlyingNode,
    )

/**
 * Creates a new [HttpEndpoint] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param httpMethod The [HttpMethod] the created [HttpEndpoint] listens to.
 * @param path The path of the created [HttpEndpoint].
 * @param arguments A list of the [Node]s representing the arguments passed to the [HttpEndpoint].
 * @param authentication The [IdentityAccessManagement] method used by the [HttpEndpoint].
 * @return The created [HttpEndpoint] concept.
 */
fun MetadataProvider.newHttpEndpoint(
    underlyingNode: FunctionDeclaration,
    httpMethod: HttpMethod,
    path: String,
    arguments: List<Node>,
    authentication: IdentityAccessManagement?,
) =
    newConcept(
        {
            HttpEndpoint(
                underlyingNode = underlyingNode,
                httpMethod = httpMethod,
                path = path,
                arguments = arguments,
                authentication = authentication,
            )
        },
        underlyingNode = underlyingNode,
    )

/**
 * Creates a new [HttpRequest] operation for the given [HttpEndpoint].
 *
 * @param underlyingNode The underlying [Node] representing the request.
 * @param concept The [HttpClient] concept this operation belongs to.
 * @return The created [HttpRequest] concept.
 */
fun MetadataProvider.newHttpRequest(
    underlyingNode: Node,
    concept: HttpClient,
    url: String,
    arguments: List<Node>,
    httpMethod: HttpMethod,
) =
    newOperation(
        { underlyingNode, concept ->
            HttpRequest(
                underlyingNode = underlyingNode,
                url = url,
                arguments = arguments,
                httpMethod = httpMethod,
                concept = concept,
            )
        },
        underlyingNode = underlyingNode,
        concept = concept,
    )

/**
 * Creates a new [RegisterHttpEndpoint] operation for the given [HttpEndpoint].
 *
 * @param underlyingNode The underlying [Node] registering the endpoint method.
 * @param concept The [Concept] to which this operation belongs.
 * @param httpEndpoint The [HttpEndpoint] which is registered by this operation.
 * @return The created [RegisterHttpEndpoint] concept.
 */
fun MetadataProvider.newRegisterHttpEndpoint(
    underlyingNode: Node,
    concept: Concept,
    httpEndpoint: HttpEndpoint,
) =
    newOperation(
        { underlyingNode, concept ->
            RegisterHttpEndpoint(
                underlyingNode = underlyingNode,
                concept = concept,
                httpEndpoint = httpEndpoint,
            )
        },
        underlyingNode = underlyingNode,
        concept = concept,
    )
