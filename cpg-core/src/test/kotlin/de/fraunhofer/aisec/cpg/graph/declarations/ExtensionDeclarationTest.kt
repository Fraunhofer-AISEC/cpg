/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.declarations

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.builder.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExtensionDeclarationTest {
    @Test
    fun testDeclarationPlacement() {

        with(
            TestLanguageFrontend(
                ctx = TranslationContext(TranslationConfiguration.builder().defaultPasses().build())
            )
        ) {
            val tr = build {
                translationResult {
                    translationUnit {
                        val rec = record("Record") {}
                        extension("Extension") {
                            this.extendedDeclaration = rec
                            method("extFunc") {}
                            field("extField") {}
                        }
                    }
                }
            }

            // find the TU and extension
            val tu = tr.components.first().translationUnits.first()
            val extensions = tu.declarations.filterIsInstance<ExtensionDeclaration>()
            assertEquals(1, extensions.size, "One extension should be present")

            val records = tu.declarations.filterIsInstance<RecordDeclaration>()
            assertEquals(1, records.size, "One record should be present")

            extensions.first().astChildren.filterIsInstance<Declaration>().forEach { declaration ->
                assertTrue(
                    records.first().declarations.contains(declaration),
                    "Record should contain the extension's declaration",
                )
            }
        }
    }
}
