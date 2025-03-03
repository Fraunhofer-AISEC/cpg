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
abstract class Authentication(underlyingNode: Node) : Concept(underlyingNode)

class TokenBasedAuth(underlyingNode: Node, val token: Node) : Authentication(underlyingNode)

class CertificateBasedAuth(underlyingNode: Node, val certificate: Node) :
    Authentication(underlyingNode)

class JwtAuth(underlyingNode: Node, val jwt: Node) : Authentication(underlyingNode)

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
    AuthenticationOperation(underlyingNode, concept)
