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
import de.fraunhofer.aisec.cpg.graph.concepts.newConcept
import de.fraunhofer.aisec.cpg.graph.concepts.newOperation

fun MetadataProvider.newTokenBasedAuth(underlyingNode: Node, token: Node) =
    newConcept({ TokenBasedAuth(it, token = token) }, underlyingNode = underlyingNode)

fun MetadataProvider.newJwtAuth(underlyingNode: Node, jwt: Node, payload: Node) =
    newConcept({ JwtAuth(it, jwt = jwt, payload = payload) }, underlyingNode = underlyingNode)

fun Authentication.newAuthenticate(underlyingNode: Node, credential: Node) =
    newOperation(
        { underlyingNode, concept ->
            Authenticate(
                underlyingNode = underlyingNode,
                concept = concept,
                credential = credential,
            )
        },
        underlyingNode = underlyingNode,
        concept = this,
    )
