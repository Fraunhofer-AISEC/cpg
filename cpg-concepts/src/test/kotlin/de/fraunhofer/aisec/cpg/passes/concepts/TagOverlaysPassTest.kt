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
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.builder.*
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.crypto.encryption.Encrypt
import de.fraunhofer.aisec.cpg.graph.concepts.crypto.encryption.Encryption
import de.fraunhofer.aisec.cpg.graph.concepts.diskEncryption.Secret
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.passes.PointsToPass
import java.util.Objects
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame

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
                                                    each<RecordDeclaration>("Encryption").with {
                                                        Encryption<Node>()
                                                    }
                                                    each<VariableDeclaration>("key").with {
                                                        Secret()
                                                    }
                                                    each<CallExpression>("encrypt").withMultiple {
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
                                                                    state.values
                                                                        .flatMap { it }
                                                                        .filterIsInstance<
                                                                            Encryption<Node>
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
                translationResult {
                    translationUnit {
                        record("Encryption") {}
                        function("main") {
                            body {
                                declare {
                                    variable("key", t("string"), init = { literal("secret") })
                                }
                                call("encrypt") { ref("key") }
                            }
                        }
                    }
                }
            }

        val encryption = result.records["Encryption"]
        assertNotNull(encryption)

        val cipher = encryption.conceptNodes.singleOrNull()
        assertIs<Encryption<Node>>(cipher)

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
}
