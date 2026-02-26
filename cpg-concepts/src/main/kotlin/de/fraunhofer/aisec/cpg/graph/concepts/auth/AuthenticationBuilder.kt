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
package de.fraunhofer.aisec.cpg.graph.concepts.auth

import de.fraunhofer.aisec.cpg.graph.MetadataProvider
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.concepts.newConcept
import de.fraunhofer.aisec.cpg.graph.concepts.newOperation
import de.fraunhofer.aisec.cpg.graph.concepts.ontology.*

/**
 * Creates a new [TokenBasedAuthentication] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param token The authentication token.
 * @param enabled Whether the authentication is enabled.
 * @param enforced Whether the authentication is enforced.
 * @param contextIsChecked Whether the context is checked.
 * @param rotationInterval The token rotation interval.
 * @param connect If `true`, the created [Concept] will be connected to the underlying node by
 *   setting its `underlyingNode`.
 * @return The created [TokenBasedAuthentication] concept.
 */
fun MetadataProvider.newTokenBasedAuth(
    underlyingNode: Node,
    token: Token? = null,
    enabled: Boolean? = null,
    enforced: Boolean? = null,
    contextIsChecked: Boolean? = null,
    rotationInterval: Int? = null,
    connect: Boolean,
) =
    newConcept(
        {
            TokenBasedAuthentication(
                enabled = enabled,
                enforced = enforced,
                token = token,
                contextIsChecked = contextIsChecked,
                rotationInterval = rotationInterval,
            )
        },
        underlyingNode = underlyingNode,
        connect = connect,
    )

/**
 * Creates a new [JwtAuthentication] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param token The JWT token.
 * @param enabled Whether the authentication is enabled.
 * @param enforced Whether the authentication is enforced.
 * @param contextIsChecked Whether the context is checked.
 * @param rotationInterval The token rotation interval.
 * @param connect If `true`, the created [Concept] will be connected to the underlying node by
 *   setting its `underlyingNode`.
 * @return The created [JwtAuthentication] concept.
 */
fun MetadataProvider.newJwtAuth(
    underlyingNode: Node,
    token: Token? = null,
    enabled: Boolean? = null,
    enforced: Boolean? = null,
    contextIsChecked: Boolean? = null,
    rotationInterval: Int? = null,
    connect: Boolean,
) =
    newConcept(
        {
            JwtAuthentication(
                enabled = enabled,
                enforced = enforced,
                token = token,
                contextIsChecked = contextIsChecked,
                rotationInterval = rotationInterval,
            )
        },
        underlyingNode = underlyingNode,
        connect = connect,
    )

/**
 * Creates a new [Authenticate] operation belonging to a certain [Authenticity] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param concept The [Authenticity] concept to which the operation belongs.
 * @param credential The credential used for authentication.
 * @param connect If `true`, the created [Operation] will be connected to the underlying node by
 *   setting its `underlyingNode` and inserting it in the EOG , to [concept] by its edge
 *   [Concept.ops].
 * @return The created [Authenticate] operation.
 */
fun MetadataProvider.newAuthenticate(
    underlyingNode: Node,
    concept: Authenticity,
    credential: Credential? = null,
    connect: Boolean,
) =
    newOperation(
        { linkedConcept -> Authenticate(credential = credential, linkedConcept = linkedConcept) },
        underlyingNode = underlyingNode,
        concept = concept,
        connect = connect,
    )

/**
 * Creates a new [IssueJwt] operation belonging to a certain [Authenticity] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param concept The [Authenticity] concept to which the operation belongs.
 * @param connect If `true`, the created [Concept] will be connected to the underlying node by
 *   setting its `underlyingNode`.
 */
fun MetadataProvider.newIssueJwt(underlyingNode: Node, concept: Authenticity, connect: Boolean) =
    newOperation(
        { linkedConcept -> IssueJwt(linkedConcept = linkedConcept) },
        underlyingNode = underlyingNode,
        concept = concept,
        connect = connect,
    )

/**
 * Creates a new [ValidateJwt] operation belonging to a certain [Authenticity] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param concept The [Authenticity] concept to which the operation belongs.
 * @param connect If `true`, the created [Concept] will be connected to the underlying node by
 *   setting its `underlyingNode`.
 */
fun MetadataProvider.newValidateJwt(underlyingNode: Node, concept: Authenticity, connect: Boolean) =
    newOperation(
        { linkedConcept -> ValidateJwt(linkedConcept = linkedConcept) },
        underlyingNode = underlyingNode,
        concept = concept,
        connect = connect,
    )

/**
 * Creates a new [AuthorizeJwt] operation belonging to a certain [Authenticity] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param concept The [Authenticity] concept to which the operation belongs.
 * @param connect If `true`, the created [Concept] will be connected to the underlying node by
 *   setting its `underlyingNode`.
 */
fun MetadataProvider.newAuthorizeJwt(
    underlyingNode: Node,
    concept: Authenticity,
    connect: Boolean,
) =
    newOperation(
        { linkedConcept -> AuthorizeJwt(linkedConcept = linkedConcept) },
        underlyingNode = underlyingNode,
        concept = concept,
        connect = connect,
    )
