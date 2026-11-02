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
package de.fraunhofer.aisec.cpg.passes.concepts

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.singleTranslationUnit
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.crypto.encryption.Cipher
import de.fraunhofer.aisec.cpg.graph.concepts.crypto.encryption.Encrypt
import de.fraunhofer.aisec.cpg.graph.concepts.crypto.encryption.Secret
import de.fraunhofer.aisec.cpg.graph.declarations.Record
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.passes.PointsToPass
import java.util.Objects
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TagOverlaysPassTest {

    class CryptoArgument(underlyingNode: Node? = null) : Concept(underlyingNode) {
        override fun equals(other: Any?): Boolean {
            return other is CryptoArgument && super.equals(other)
        }

        override fun hashCode() = Objects.hash(super.hashCode())
    }

    class SecretKey(underlyingNode: Node? = null) : Concept(underlyingNode) {
        override fun equals(other: Any?): Boolean {
            return other is SecretKey && super.equals(other)
        }

        override fun hashCode() = Objects.hash(super.hashCode())
    }

    class Resource(underlyingNode: Node? = null) : Concept(underlyingNode) {
        override fun equals(other: Any?): Boolean {
            return other is Resource && super.equals(other)
        }

        override fun hashCode() = Objects.hash(super.hashCode())
    }

    class Release(underlyingNode: Node? = null) : Concept(underlyingNode) {
        override fun equals(other: Any?): Boolean {
            return other is Release && super.equals(other)
        }

        override fun hashCode() = Objects.hash(super.hashCode())
    }

    /**
     * Tags `release()` in `early` only if `acquire()` in `later`, a function processed after
     * `early`, already has its [Resource]. Returns the result and how often the pass visited
     * `acquire()`, i.e., how often it was executed.
     */
    private fun tagAcrossFunctions(iterateToFixpoint: Boolean): Pair<TranslationResult, Int> {
        val acquireVisits = AtomicInteger()
        val tagging = tag {
            each<Call>("acquire").with {
                acquireVisits.incrementAndGet()
                Resource()
            }
            each<Call>("release").with {
                val resources =
                    node.translationUnit.allChildrenWithOverlays<Resource>() +
                        state.values.flatten().filterIsInstance<Resource>()
                if (resources.isNotEmpty()) Release() else null
            }
        }
        val result =
            with(
                TestLanguageFrontend(
                    ctx =
                        TranslationContext(
                            config =
                                TranslationConfiguration.builder()
                                    .registerPass<TagOverlaysPass>()
                                    .configurePass<TagOverlaysPass>(
                                        TagOverlaysPass.Configuration(tagging, iterateToFixpoint)
                                    )
                                    .build()
                        )
                )
            ) {
                build {
                    singleTranslationUnit { tu ->
                        for ((name, callee) in listOf("early" to "release", "later" to "acquire")) {
                            newFunction(name, holder = tu, enterScope = true) { function ->
                                function.body =
                                    newBlock(enterScope = true) { block ->
                                        block.statements += newCall(newReference(callee))
                                    }
                            }
                        }
                    }
                }
            }
        return Pair(result, acquireVisits.get())
    }

    @Test
    fun testTag() {
        val result =
            with(
                TestLanguageFrontend(
                    ctx =
                        TranslationContext(
                            config =
                                TranslationConfiguration.builder()
                                    .registerPass<TagOverlaysPass>()
                                    .registerPass<PointsToPass>()
                                    .configurePass<TagOverlaysPass>(
                                        TagOverlaysPass.Configuration(
                                            tag =
                                                tag {
                                                    each<Record>("Encryption").with { Cipher() }
                                                    each<Variable>("key").with { Secret() }
                                                    each<Call>("encrypt").withMultiple {
                                                        propagate { node.arguments[0] }
                                                            .with { SecretKey() }

                                                        propagate { node.arguments[0] }
                                                            .with { CryptoArgument() }

                                                        // "By accident", we assign the same overlay
                                                        // twice. This should be filtered out.
                                                        propagate { node.arguments[0] }
                                                            .with { CryptoArgument() }

                                                        val secrets =
                                                            node.getOverlaysByPrevDFG<Secret>(state)
                                                        val encryptOps =
                                                            secrets.flatMap { secret ->
                                                                val ciphers =
                                                                    node.translationUnit
                                                                        .allChildrenWithOverlays<
                                                                            Cipher
                                                                        >() +
                                                                        state.values
                                                                            .flatMap { it }
                                                                            .filterIsInstance<
                                                                                Cipher
                                                                            >()
                                                                ciphers.map { cipher ->
                                                                    Encrypt(
                                                                        concept = cipher,
                                                                        key = secret,
                                                                    )
                                                                }
                                                            }
                                                        encryptOps
                                                    }
                                                }
                                        )
                                    )
                                    .build()
                        )
                )
            ) {
                build {
                    singleTranslationUnit { tu ->
                        newRecord("Encryption", "class", holder = tu, enterScope = true)

                        newFunction("main", holder = tu, enterScope = true) { main ->
                            main.body =
                                newBlock(enterScope = true) { block ->
                                    block.statements += newDeclarationStatement { declStmt ->
                                        newVariable(
                                            "key",
                                            objectType("string"),
                                            holder = declStmt,
                                        ) {
                                            it.initializer = newLiteral("secret")
                                        }
                                    }

                                    block.statements +=
                                        newCall(newReference("encrypt")) {
                                            it.arguments += newReference("key")
                                        }
                                }
                        }
                    }
                }
            }

        val encryption = result.records["Encryption"]
        assertNotNull(encryption)

        val cipher = encryption.conceptNodes.singleOrNull()
        assertIs<Cipher>(cipher)

        val key = result.variables["key"]
        assertNotNull(key)

        val secret = key.conceptNodes.singleOrNull()
        assertIs<Secret>(secret)

        val encryptCall = result.calls["encrypt"]
        assertNotNull(encryptCall)

        val encrypt = encryptCall.operationNodes.singleOrNull()
        assertIs<Encrypt>(encrypt)
        assertSame(cipher, encrypt.concept)

        val arg0 = encryptCall.arguments[0]
        assertIs<Reference>(arg0)
        val arg0OverlaySecretKey = arg0.overlays.filterIsInstance<SecretKey>().singleOrNull()
        assertNotNull(arg0OverlaySecretKey)
        val arg0OverlayCryptoArgument =
            arg0.overlays.filterIsInstance<CryptoArgument>().singleOrNull()
        assertNotNull(arg0OverlayCryptoArgument)
    }

    @Test
    fun testOverlaysOfLaterFunctionsNeedFixpoint() {
        val (result, executions) = tagAcrossFunctions(iterateToFixpoint = false)

        assertEquals(1, executions)
        assertNotNull(result.calls["acquire"]?.conceptNodes?.singleOrNull() as? Resource)
        assertTrue(result.calls["release"]?.conceptNodes.isNullOrEmpty())
    }

    @Test
    fun testIterateToFixpoint() {
        val (result, executions) = tagAcrossFunctions(iterateToFixpoint = true)

        // The first execution tags acquire, the second one release, the third one nothing new
        assertEquals(3, executions)
        assertNotNull(result.calls["acquire"]?.conceptNodes?.singleOrNull() as? Resource)
        assertNotNull(result.calls["release"]?.conceptNodes?.singleOrNull() as? Release)
        assertTrue(result.dirtyNodes.values.none { TagOverlaysPass::class in it })
    }
}
