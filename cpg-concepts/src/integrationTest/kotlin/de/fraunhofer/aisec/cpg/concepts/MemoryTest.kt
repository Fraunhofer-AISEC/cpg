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
package de.fraunhofer.aisec.cpg.concepts

import de.fraunhofer.aisec.cpg.TranslationResult.Companion.DEFAULT_APPLICATION_NAME
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.Forward
import de.fraunhofer.aisec.cpg.graph.Interprocedural
import de.fraunhofer.aisec.cpg.graph.concepts.diskEncryption.Encrypt
import de.fraunhofer.aisec.cpg.graph.concepts.diskEncryption.GetSecret
import de.fraunhofer.aisec.cpg.graph.concepts.diskEncryption.newCipher
import de.fraunhofer.aisec.cpg.graph.concepts.diskEncryption.newEncryptOperation
import de.fraunhofer.aisec.cpg.graph.concepts.diskEncryption.newSecret
import de.fraunhofer.aisec.cpg.graph.concepts.memory.DeAllocate
import de.fraunhofer.aisec.cpg.graph.concepts.memory.MemoryManagementMode
import de.fraunhofer.aisec.cpg.graph.concepts.memory.newDeallocate
import de.fraunhofer.aisec.cpg.graph.concepts.memory.newMemory
import de.fraunhofer.aisec.cpg.graph.edges.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeleteExpression
import de.fraunhofer.aisec.cpg.query.Must
import de.fraunhofer.aisec.cpg.query.dataFlow
import de.fraunhofer.aisec.cpg.query.executionPath
import de.fraunhofer.aisec.cpg.test.analyze
import java.io.File
import kotlin.test.*

class MemoryTest {
    @Test
    fun testMemoryDelete() {
        val topLevel = File("src/integrationTest/resources/python")
        val result =
            analyze(listOf(topLevel.resolve("encrypt_with_key.py")), topLevel.toPath(), true) {
                it.registerLanguage<PythonLanguage>()
            }
        // Secrets (key) concepts
        val key = result.newSecret(underlyingNode = assertNotNull(result.variables["key"]))
        val getSecret =
            GetSecret(
                underlyingNode = assertNotNull(result.functions["get_secret_from_server"]),
                concept = key,
            )
        key.ops += getSecret

        // Cipher (encryption) concepts
        val cipher =
            result.newCipher(
                underlyingNode =
                    assertNotNull(
                        assertNotNull(result.calls["encrypt"]).argumentEdges["cipher"]?.end
                    )
            )
        val cipherAndSize = (cipher.underlyingNode?.evaluate() as? String)?.split("-")
        cipher.cipherName = cipherAndSize?.get(0)
        cipher.blockSize = cipherAndSize?.get(1)?.toIntOrNull()
        assertEquals("AES", cipher.cipherName)
        assertEquals(256, cipher.blockSize)
        val encrypt =
            result.newEncryptOperation(
                underlyingNode = assertNotNull(result.functions["encrypt"]),
                concept = cipher,
                key = key,
            )
        cipher.ops += encrypt

        // Memory concepts
        val memory =
            result.newMemory(
                underlyingNode = assertNotNull(result.components[DEFAULT_APPLICATION_NAME]),
                mode = MemoryManagementMode.MANAGED_WITH_GARBAGE_COLLECTION,
            )
        val ops =
            result.allChildren<DeleteExpression>().flatMap { delete ->
                delete.operands.map {
                    result.newDeallocate(underlyingNode = delete, concept = memory, what = it)
                }
            }
        memory.ops += ops

        // Key is used in encryption
        var tree =
            key.underlyingNode?.let {
                dataFlow(it) { node -> node.overlayEdges.any { edge -> edge.end is Encrypt } }
            }
        assertNotNull(tree)
        assertEquals(true, tree.value)

        // Tree is deleted in all paths
        tree =
            key.underlyingNode?.let {
                executionPath(
                    startNode = it,
                    predicate = { node ->
                        node.overlayEdges.any { edge -> edge.end is DeAllocate }
                    },
                    direction = Forward(GraphToFollow.EOG),
                    type = Must,
                    scope = Interprocedural(),
                )
            }
        assertNotNull(tree)
        assertEquals(true, tree.value)
        assertEquals(2, tree.children.size)
    }
}
