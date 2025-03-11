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
package de.fraunhofer.aisec.cpg.graph.concepts.diskEncryption

import de.fraunhofer.aisec.cpg.graph.MetadataProvider
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.newConcept
import de.fraunhofer.aisec.cpg.graph.concepts.newOperation

fun MetadataProvider.newDiskEncryption(underlyingNode: Node, cipher: Cipher?, key: Secret?) =
    newConcept(
        {
            val node = DiskEncryption(it)
            key?.let { node.key = it }
            cipher?.let { node.cipher = it }
            node
        },
        underlyingNode = underlyingNode,
    )

fun MetadataProvider.newCipher(underlyingNode: Node) =
    newConcept({ Cipher(underlyingNode = underlyingNode) }, underlyingNode = underlyingNode)

fun MetadataProvider.newSecret(underlyingNode: Node) =
    newConcept({ Secret(underlyingNode = underlyingNode) }, underlyingNode = underlyingNode)

fun MetadataProvider.newBlockStorage(underlyingNode: Node) =
    newConcept({ BlockStorage(underlyingNode = underlyingNode) }, underlyingNode = underlyingNode)

fun Cipher.newEncryptOperation(underlyingNode: Node, key: Secret) =
    newOperation(
        { node, concept -> Encrypt(underlyingNode, concept, key) },
        underlyingNode = underlyingNode,
        concept = this,
    )

fun DiskEncryption.newCreateEncryptedDisk(underlyingNode: Node) =
    newOperation(
        { node, concept -> CreateEncryptedDisk(underlyingNode, concept) },
        underlyingNode = underlyingNode,
        concept = this,
    )

fun DiskEncryption.newUnlockEncryptedDisk(underlyingNode: Node) =
    newOperation(
        { node, concept -> UnlockEncryptedDisk(underlyingNode, concept) },
        underlyingNode = underlyingNode,
        concept = this,
    )

fun Secret.newCreateSecret(underlyingNode: Node) =
    newOperation(
        { node, concept -> CreateSecret(underlyingNode, concept) },
        underlyingNode = underlyingNode,
        concept = this,
    )

fun Secret.newGetSecret(underlyingNode: Node) =
    newOperation(
        { node, concept -> GetSecret(underlyingNode, concept) },
        underlyingNode = underlyingNode,
        concept = this,
    )
