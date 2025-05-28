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

import de.fraunhofer.aisec.cpg.graph.Node

/**
 * An operation that retrieves a secret from a (remote) location. This can be a local keystore, a
 * remote key server or a hardware device such as a TPM or HSM.
 */
open class GetSecret(underlyingNode: Node? = null, concept: Secret) :
    SecretOperation(underlyingNode = underlyingNode, concept = concept) {
    override fun setDFG() {
        // Since we are retrieving a secret, we want to model a data flow from the secret to the
        // operation that retrieves it. This is done by adding the secret to the prevDFG edge.
        this.prevDFG += concept
    }
}
