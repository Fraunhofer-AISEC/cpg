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
import de.fraunhofer.aisec.cpg.graph.concepts.diskEncryption.Cipher
import de.fraunhofer.aisec.cpg.graph.concepts.diskEncryption.Encrypt
import de.fraunhofer.aisec.cpg.graph.concepts.diskEncryption.Secret
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class TagOverlaysPassTest {

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
                                                    each<VariableDeclaration>("key").with {
                                                        Secret()
                                                    }
                                                    each<CallExpression>("encrypt").withMultiple {
                                                        val secrets =
                                                            node.getOverlaysByPrevDFG<Secret>(state)
                                                        secrets.map { secret ->
                                                            Encrypt(
                                                                concept = Cipher(),
                                                                key = secret,
                                                            )
                                                        }
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
                        function("main") {
                            body {
                                declare {
                                    variable("key", t("string"), init = { literal("secret") })
                                    call("encrypt") { ref("key") }
                                }
                            }
                        }
                    }
                }
            }

        val key = result.variables["key"]
        assertNotNull(key)

        val secret = key.conceptNodes.singleOrNull()
        assertIs<Secret>(secret)
    }
}
