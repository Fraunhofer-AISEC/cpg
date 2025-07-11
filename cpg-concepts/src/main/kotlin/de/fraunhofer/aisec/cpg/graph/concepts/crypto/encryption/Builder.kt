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
package de.fraunhofer.aisec.cpg.graph.concepts.crypto.encryption

import de.fraunhofer.aisec.cpg.graph.MetadataProvider
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.concepts.newConcept
import de.fraunhofer.aisec.cpg.graph.concepts.newOperation

/**
 * Creates a new [Cipher] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param connect If `true`, the created [Concept] will be connected to the underlying node by
 *   setting its `underlyingNode`.
 * @return The created [Cipher] concept.
 */
fun MetadataProvider.newCipher(underlyingNode: Node, connect: Boolean) =
    newConcept(::Cipher, underlyingNode = underlyingNode, connect = connect)

/**
 * Creates a new [Secret] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param connect If `true`, the created [Concept] will be connected to the underlying node by
 *   setting its `underlyingNode`.
 * @return The created [Secret] concept.
 */
fun MetadataProvider.newSecret(underlyingNode: Node, connect: Boolean) =
    newConcept(::Secret, underlyingNode = underlyingNode, connect = connect)

/**
 * Creates a new [de.fraunhofer.aisec.cpg.graph.concepts.crypto.encryption.CreateSecret] operation.
 *
 * @param underlyingNode The underlying node representing this operation.
 * @param concept The [Secret] concept to which the operation belongs.
 * @param connect If `true`, the created [Operation] will be connected to the underlying node by
 *   setting its `underlyingNode` and inserting it in the EOG , to [concept] by its edge
 *   [Concept.ops].
 * @return The created [de.fraunhofer.aisec.cpg.graph.concepts.crypto.encryption.CreateSecret]
 *   operation.
 */
fun MetadataProvider.newCreateSecret(underlyingNode: Node, concept: Secret, connect: Boolean) =
    newOperation(
        { concept -> CreateSecret(concept = concept) },
        underlyingNode = underlyingNode,
        concept = concept,
        connect = connect,
    )

/**
 * Creates a new [de.fraunhofer.aisec.cpg.graph.concepts.crypto.encryption.GetSecret] operation.
 *
 * @param underlyingNode The underlying node representing this operation.
 * @param concept The [Secret] concept to which the operation belongs.
 * @param connect If `true`, the created [Operation] will be connected to the underlying node by
 *   setting its `underlyingNode` and inserting it in the EOG , to [concept] by its edge
 *   [Concept.ops].
 * @return The created [de.fraunhofer.aisec.cpg.graph.concepts.crypto.encryption.GetSecret]
 *   operation.
 */
fun MetadataProvider.newGetSecret(underlyingNode: Node, concept: Secret, connect: Boolean) =
    newOperation(
        { concept -> GetSecret(concept = concept) },
        underlyingNode = underlyingNode,
        concept = concept,
        connect = connect,
    )

/**
 * Creates a new [Encrypt] operation.
 *
 * @param underlyingNode The underlying node representing this operation.
 * @param concept The [Cipher] concept to which the operation belongs.
 * @param key The secret key used for encryption.
 * @param connect If `true`, the created [Operation] will be connected to the underlying node by
 *   setting its `underlyingNode` and inserting it in the EOG , to [concept] by its edge
 *   [Concept.ops].
 * @return The created [Encrypt] operation.
 */
fun MetadataProvider.newEncryptOperation(
    underlyingNode: Node,
    concept: Cipher,
    key: Secret,
    connect: Boolean,
) =
    newOperation(
        { concept -> Encrypt(concept = concept, key = key) },
        underlyingNode = underlyingNode,
        concept = concept,
        connect = connect,
    )
