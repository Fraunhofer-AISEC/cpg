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
import de.fraunhofer.aisec.cpg.graph.concepts.crypto.encryption.Cipher
import de.fraunhofer.aisec.cpg.graph.concepts.crypto.encryption.Encrypt
import de.fraunhofer.aisec.cpg.graph.concepts.diskEncryption.Secret
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class TagOverlaysPassTest {

    class SecretKey(underlyingNode: Node? = null) : Concept(underlyingNode)

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
                                    .configurePass<TagOverlaysPass>(
                                        TagOverlaysPass.Configuration(
                                            tag =
                                                tag {
                                                    each<RecordDeclaration>("Encryption").with {
                                                        Cipher()
                                                    }
                                                    each<VariableDeclaration>("key").with {
                                                        Secret()
                                                    }
                                                    each<CallExpression>("encrypt").withMultiple {
                                                        propagateWith(
                                                            transformation = { node.arguments[0] },
                                                            with = { SecretKey() },
                                                        )

                                                        val secrets =
                                                            node.getOverlaysByPrevDFG<Secret>(state)
                                                        val encryptOps =
                                                            secrets.flatMap { secret ->
                                                                val ciphers =
                                                                    state.values
                                                                        .flatMap { it }
                                                                        .filterIsInstance<Cipher>()
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
        val arg0Overlay = arg0.overlays.singleOrNull()
        assertIs<SecretKey>(arg0Overlay)
    }
}
