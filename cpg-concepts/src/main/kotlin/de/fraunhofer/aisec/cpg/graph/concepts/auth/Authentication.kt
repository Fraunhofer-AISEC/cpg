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

/** Represents a high-level concept for authentication. */
class AuthenticationConcept(underlyingNode: Node, val authStrategy: AuthenticationStrategy) :
    Concept(underlyingNode)

/** General authentication strategies. */
enum class AuthenticationStrategy {
    TOKEN_BASED,
    CERTIFICATE_BASED,
    JWT_BASED,
    OTP_BASED,
    PASSWORD_BASED,
    NO_AUTH,
}

/** Abstract base class for authentication operations. */
abstract class AuthenticationOperation(underlyingNode: Node, concept: AuthenticationConcept) :
    Operation(underlyingNode, concept)

/// **
// * Represents an operation that sets up the authentication context.
// * This node captures which fields (headers, environment variables) are used to build the context.
// *
// * @param fields A list of fields used to set up the authentication context.
// */
// class AuthenticationSetupContext(
//    underlyingNode: Node,
//    concept: AuthenticationConcept,
//    val fields: List<String>
// ) : AuthenticationOperation(underlyingNode, concept)

/**
 * Represents an authentication operation.
 *
 * @param credential The credential can be a call (e.g., a function call that reads a header) or a
 *   variable that holds the value, e.g. the token
 */
class Authentication(
    underlyingNode: Node,
    concept: AuthenticationConcept,
    //    val context: AuthenticationSetupContext,
    val credential: Node,
) : AuthenticationOperation(underlyingNode, concept)

/**
 * Represents an operation where authentication is bypassed. This is used for cases where no
 * authentication is required, e.g., for development, testing or public endpoints.
 */
class NoAuthentication(underlyingNode: Node, concept: AuthenticationConcept) :
    AuthenticationOperation(underlyingNode, concept)
