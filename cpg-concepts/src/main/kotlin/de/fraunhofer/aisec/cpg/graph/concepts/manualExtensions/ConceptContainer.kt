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
package de.fraunhofer.aisec.cpg.graph.concepts.manualExtensions

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.concepts.ontology.Authenticity
import de.fraunhofer.aisec.cpg.graph.concepts.ontology.Authorization
import de.fraunhofer.aisec.cpg.graph.concepts.ontology.AutomaticUpdates
import de.fraunhofer.aisec.cpg.graph.concepts.ontology.HttpEndpoint
import de.fraunhofer.aisec.cpg.graph.concepts.ontology.HttpRequestContext
import de.fraunhofer.aisec.cpg.graph.concepts.ontology.TransportEncryption

/** The [underlyingNode] gets the current time of the system. */
class GetCurrentTime(concept: Concept, underlyingNode: Node?) :
    Operation(concept = concept, underlyingNode = underlyingNode)

class InputValidation(
    val input: Node?,
    val validatedOutput: Node?,
    concept: Concept,
    underlyingNode: Node?,
) : Operation(concept, underlyingNode)

class RateLimiting(val maxRequests: Int, val timeWindowSeconds: Int, underlyingNode: Node?) :
    Concept(underlyingNode)

class ExtendedHttpEndpoint(
    val rateLimiting: RateLimiting?,
    val maxInputSize: Int?,
    val userInput: MutableList<Node>,
    handler: String?,
    method: String?,
    path: String?,
    url: String?,
    authenticity: Authenticity?,
    authorization: Authorization?,
    httpRequestContext: HttpRequestContext?,
    proxyTarget: HttpEndpoint?,
    transportEncryption: TransportEncryption?,
    underlyingNode: Node?,
) :
    HttpEndpoint(
        handler,
        method,
        path,
        url,
        authenticity,
        authorization,
        httpRequestContext,
        proxyTarget,
        transportEncryption,
        underlyingNode,
    )

class InstallUpdate(val update: AutomaticUpdates, underlyingNode: Node?) :
    Operation(concept = update, underlyingNode = underlyingNode)
