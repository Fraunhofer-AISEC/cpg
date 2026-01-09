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
import de.fraunhofer.aisec.cpg.graph.concepts.newConcept
import de.fraunhofer.aisec.cpg.graph.concepts.newOperation
import de.fraunhofer.aisec.cpg.graph.concepts.ontology.*

/**
 * Creates a new [HttpClient] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param protocol The [TransportEncryption] protocol used by the client.
 * @param isTLS Whether the client uses TLS.
 * @param authenticity The [Authenticity] method used by the client.
 * @param connect If `true`, the created [Concept] will be connected to the underlying node by
 *   setting its `underlyingNode`.
 * @return The created [HttpClient] concept.
 */
fun MetadataProvider.newHttpClient(
    underlyingNode: Node,
    protocol: TransportEncryption?,
    isTLS: Boolean?,
    authenticity: Authenticity?,
    connect: Boolean,
) =
    newConcept(
        { HttpClient(protocol = protocol, isTLS = isTLS, authenticity = authenticity) },
        underlyingNode = underlyingNode,
        connect = connect,
    )

/**
 * Creates a new [HttpRequestHandler] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param path The base path of the [HttpRequestHandler].
 * @param application The [Application] associated with the [HttpRequestHandler].
 * @param httpEndpoints A list of the [HttpEndpoint]s exposed by the [HttpRequestHandler].
 * @param connect If `true`, the created [Concept] will be connected to the underlying node by
 *   setting its `underlyingNode`.
 * @return The created [HttpRequestHandler] concept.
 */
fun MetadataProvider.newHttpRequestHandler(
    underlyingNode: Node,
    path: String?,
    application: Application?,
    httpEndpoints: MutableList<HttpEndpoint?>,
    connect: Boolean,
) =
    newConcept(
        {
            HttpRequestHandler(
                path = path,
                application = application,
                httpEndpoints = httpEndpoints,
            )
        },
        underlyingNode = underlyingNode,
        connect = connect,
    )

/**
 * Creates a new [HttpEndpoint] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param rateLimiting The [RateLimiting] configuration for the endpoint.
 * @param maxInputSize The maximum input size allowed for the endpoint.
 * @param userInput A list of the [Node]s representing the user input passed to the [HttpEndpoint].
 * @param handler The handler name for the endpoint.
 * @param method The [HttpMethod] the created [HttpEndpoint] listens to.
 * @param path The path of the created [HttpEndpoint].
 * @param url The URL of the created [HttpEndpoint].
 * @param authenticity The [Authenticity] method used by the [HttpEndpoint].
 * @param authorization The [Authorization] mechanism defining access control.
 * @param httpRequestContext The [HttpRequestContext] providing additional contextual metadata.
 * @param proxyTarget The [HttpEndpoint] this endpoint proxies to.
 * @param transportEncryption The [TransportEncryption] configuration for the endpoint.
 * @param connect If `true`, the created [Concept] will be connected to the underlying node by
 *   setting its `underlyingNode`.
 * @return The created [HttpEndpoint] concept.
 */
fun MetadataProvider.newHttpEndpoint(
    underlyingNode: Node,
    rateLimiting: RateLimiting?,
    maxInputSize: Int?,
    userInput: MutableList<Node>,
    handler: String?,
    method: HttpMethod,
    path: String?,
    url: String?,
    authenticity: Authenticity?,
    authorization: Authorization?,
    httpRequestContext: HttpRequestContext?,
    proxyTarget: HttpEndpoint?,
    transportEncryption: TransportEncryption?,
    connect: Boolean,
) =
    newConcept(
        {
            HttpEndpoint(
                rateLimiting = rateLimiting,
                maxInputSize = maxInputSize,
                userInput = userInput,
                handler = handler,
                method = method,
                path = path,
                url = url,
                authenticity = authenticity,
                authorization = authorization,
                httpRequestContext = httpRequestContext,
                proxyTarget = proxyTarget,
                transportEncryption = transportEncryption,
            )
        },
        underlyingNode = underlyingNode,
        connect = connect,
    )

/**
 * Creates a new [HttpRequest] operation for the given [HttpClient].
 *
 * @param underlyingNode The underlying [Node] representing the request.
 * @param linkedConcept The [HttpClient] concept this operation belongs to.
 * @param arguments A list of [Node]s representing the arguments passed to the request.
 * @param method The [HttpMethod] of the request.
 * @param call The call identifier for the request.
 * @param reqBody The request body content.
 * @param httpEndpoint The [HttpEndpoint] being requested.
 * @param connect If `true`, the created [Operation] will be connected to the underlying node by
 *   setting its `underlyingNode` and inserting it in the EOG , to [linkedConcept] by its edge
 *   [Concept.ops].
 * @return The created [HttpRequest] operation.
 */
fun MetadataProvider.newHttpRequest(
    underlyingNode: Node,
    linkedConcept: HttpClient,
    arguments: List<Node>,
    method: HttpMethod,
    call: String?,
    reqBody: String?,
    httpEndpoint: HttpEndpoint?,
    connect: Boolean,
) =
    newOperation(
        { concept ->
            HttpRequest(
                arguments = arguments,
                method = method,
                call = call,
                reqBody = reqBody,
                httpEndpoint = httpEndpoint,
                linkedConcept = concept,
            )
        },
        underlyingNode = underlyingNode,
        concept = linkedConcept,
        connect = connect,
    )

/**
 * Creates a new [RegisterHttpEndpoint] operation for the given [HttpEndpoint].
 *
 * @param underlyingNode The underlying [Node] registering the endpoint method.
 * @param linkedConcept The [HttpRequestHandler] concept to which this operation belongs.
 * @param httpEndpoint The [HttpEndpoint] which is registered by this operation.
 * @param connect If `true`, the created [Operation] will be connected to the underlying node by
 *   setting its `underlyingNode` and inserting it in the EOG , to [linkedConcept] by its edge
 *   [Concept.ops].
 * @return The created [RegisterHttpEndpoint] operation.
 */
fun MetadataProvider.newRegisterHttpEndpoint(
    underlyingNode: Node,
    linkedConcept: HttpRequestHandler,
    httpEndpoint: HttpEndpoint?,
    connect: Boolean,
) =
    newOperation(
        { concept -> RegisterHttpEndpoint(httpEndpoint = httpEndpoint, linkedConcept = concept) },
        underlyingNode = underlyingNode,
        concept = linkedConcept,
        connect = connect,
    )
