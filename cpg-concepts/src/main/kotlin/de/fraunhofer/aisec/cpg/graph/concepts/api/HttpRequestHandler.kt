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
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation

/**
 * Represents a group of HTTP endpoints bundled together also known as Controller in some
 * frameworks.
 */
class HttpRequestHandler(
    underlyingNode: Node,
    val basePath: String,
    val endpoints: MutableList<HttpEndpoint>,
) : Concept<HttpRequestHandlerOperation>(underlyingNode = underlyingNode)

abstract class HttpRequestHandlerOperation(
    underlyingNode: Node,
    concept: Concept<HttpRequestHandlerOperation>,
) : Operation(underlyingNode, concept) {}

class RegisterHttpEndpoint(
    underlyingNode: Node,
    concept: Concept<HttpRequestHandlerOperation>,
    val httpEndpoint: HttpEndpoint,
) : HttpRequestHandlerOperation(underlyingNode, concept)
