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
package de.fraunhofer.aisec.cpg.graph.concepts.iam

import de.fraunhofer.aisec.cpg.graph.MetadataProvider
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.concepts.newConcept
import de.fraunhofer.aisec.cpg.graph.concepts.newOperation

/**
 * Creates a new [TokenBasedAuth] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param token The authentication token, which may be an opaque token.
 * @param connect If `true`, the created [Concept] will be connected to the underlying node by
 *   setting its `underlyingNode`.
 * @return The created [TokenBasedAuth] concept.
 */
fun MetadataProvider.newTokenBasedAuth(underlyingNode: Node, token: Node, connect: Boolean) =
    newConcept(
        { TokenBasedAuth(token = token) },
        underlyingNode = underlyingNode,
        connect = connect,
    )

/**
 * Creates a new [JwtAuth] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param jwt The JWT containing encoded authentication information.
 * @param payload The payload.
 * @param connect If `true`, the created [Concept] will be connected to the underlying node by
 *   setting its `underlyingNode`.
 * @return The created [JwtAuth] concept.
 */
fun MetadataProvider.newJwtAuth(underlyingNode: Node, jwt: Node, payload: Node, connect: Boolean) =
    newConcept(
        { JwtAuth(jwt = jwt, payload = payload) },
        underlyingNode = underlyingNode,
        connect = connect,
    )

/**
 * Creates a new [Authenticate] operation belonging to a certain [IdentityAccessManagement] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param concept The [IdentityAccessManagement] concept to which the operation belongs.
 * @param credential The credential can be a call (e.g., a function call that reads a header) or a
 *   variable that holds the value, e.g. the token * @return The created [Authenticate] operation.
 * @param connect If `true`, the created [Operation] will be connected to the underlying node by
 *   setting its `underlyingNode` and inserting it in the EOG , to [concept] by its edge
 *   [Concept.ops].
 * @return The created [Authenticate] operation.
 */
fun MetadataProvider.newAuthenticate(
    underlyingNode: Node,
    concept: IdentityAccessManagement,
    credential: Node,
    connect: Boolean,
) =
    newOperation(
        { concept -> Authenticate(concept = concept, credential = credential) },
        underlyingNode = underlyingNode,
        concept = concept,
        connect = connect,
    )

/**
 * Creates a new [IssueJwt] operation belonging to a certain [JwtAuth] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param concept The [JwtAuth] concept to which the operation belongs.
 */
fun MetadataProvider.newIssueJwt(underlyingNode: Node, concept: JwtAuth) =
    newOperation(
        { underlyingNode, concept -> IssueJwt(underlyingNode = underlyingNode, jwt = concept) },
        underlyingNode = underlyingNode,
        concept = concept,
    )

/**
 * Creates a new [ValidateJwt] operation belonging to a certain [JwtAuth] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param concept The [JwtAuth] concept to which the operation belongs.
 */
fun MetadataProvider.newValidateJwt(underlyingNode: Node, concept: JwtAuth) =
    newOperation(
        { underlyingNode, concept -> ValidateJwt(underlyingNode = underlyingNode, jwt = concept) },
        underlyingNode = underlyingNode,
        concept = concept,
    )

/**
 * Creates a new [AuthorizeJwt] operation belonging to a certain [JwtAuth] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param concept The [JwtAuth] concept to which the operation belongs.
 */
fun MetadataProvider.newAuthorizeJwt(underlyingNode: Node, concept: JwtAuth) =
    newOperation(
        { underlyingNode, concept -> AuthorizeJwt(underlyingNode = underlyingNode, jwt = concept) },
        underlyingNode = underlyingNode,
        concept = concept,
    )
