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

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import java.util.Objects

/** Represents a high-level concept for authentication. */
abstract class Authentication(underlyingNode: Node) : Concept(underlyingNode)

/**
 * Represents a token-based authentication.
 *
 * @param token The authentication token, which may be an opaque token.
 */
open class TokenBasedAuth(underlyingNode: Node, val token: Node) : Authentication(underlyingNode) {
    override fun equals(other: Any?): Boolean {
        return other is TokenBasedAuth && super.equals(other) && other.token == this.token
    }

    override fun hashCode() = Objects.hash(super.hashCode(), token)
}

/**
 * Represents a JWT-based authentication, which extends the [TokenBasedAuth].
 *
 * @param jwt The JWT containing encoded authentication information.
 * @param payload The payload.
 */
class JwtAuth(underlyingNode: Node, val jwt: Node, val payload: Node) :
    TokenBasedAuth(underlyingNode, jwt) {
    override fun equals(other: Any?): Boolean {
        return other is JwtAuth &&
            super.equals(other) &&
            other.jwt == this.jwt &&
            other.payload == this.payload
    }

    override fun hashCode() = Objects.hash(super.hashCode(), jwt, payload)
}

/** Abstract base class for authentication operations. */
abstract class AuthenticationOperation(underlyingNode: Node, concept: Authentication) :
    Operation(underlyingNode, concept)

/**
 * Represents an authentication operation.
 *
 * @param credential The credential can be a call (e.g., a function call that reads a header) or a
 *   variable that holds the value, e.g. the token
 */
class Authenticate(underlyingNode: Node, concept: Authentication, val credential: Node) :
    AuthenticationOperation(underlyingNode, concept) {
    override fun equals(other: Any?): Boolean {
        return other is Authenticate && super.equals(other) && other.credential == this.credential
    }

    override fun hashCode() = Objects.hash(super.hashCode(), credential)
}
