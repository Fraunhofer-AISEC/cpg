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
import de.fraunhofer.aisec.cpg.graph.concepts.auth.Authentication
import de.fraunhofer.aisec.cpg.graph.concepts.newConcept
import de.fraunhofer.aisec.cpg.graph.concepts.newOperation
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration

fun MetadataProvider.newHttpClient(
    underlyingNode: Node,
    isTLS: Boolean?,
    authentication: Authentication?,
) =
    newConcept(
        { HttpClient(it, isTLS = isTLS, authentication = authentication) },
        underlyingNode = underlyingNode,
    )

fun MetadataProvider.newHttpRequestHandler(
    underlyingNode: Node,
    basePath: String,
    endpoints: MutableList<HttpEndpoint>,
) =
    newConcept(
        { HttpRequestHandler(it, basePath = basePath, endpoints = endpoints) },
        underlyingNode = underlyingNode,
    )

fun MetadataProvider.newHttpEndpoint(
    underlyingNode: FunctionDeclaration,
    httpMethod: HttpMethod,
    path: String,
    arguments: List<Node>,
    authentication: Authentication?,
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

fun HttpClient.newHttpRequest(
    underlyingNode: Node,
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
        concept = this,
    )

fun HttpEndpoint.newHttpRegisterHttpEndpoint(underlyingNode: Node) =
    newOperation(
        { underlyingNode, concept ->
            RegisterHttpEndpoint(
                underlyingNode = underlyingNode,
                concept = concept,
                httpEndpoint = concept,
            )
        },
        underlyingNode = underlyingNode,
        concept = this,
    )
