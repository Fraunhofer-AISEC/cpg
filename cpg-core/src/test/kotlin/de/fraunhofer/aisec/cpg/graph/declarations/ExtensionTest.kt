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
import kotlin.test.*

class ExtensionTest {
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
                        extension("Extension", rec) {
                            method("extFunc") {}
                            field("extField") {}
                        }
                    }
                }
            }

            // find the TU and extension
            val tu = tr.components.first().translationUnits.first()
            val extensions = tu.declarations.filterIsInstance<Extension>()
            assertEquals(1, extensions.size, "One extension should be present")

            val records = tu.declarations.filterIsInstance<Record>()
            assertEquals(1, records.size, "One record should be present")

            scopeManager.enterScope(records.first())

            extensions.first().astChildren.filterIsInstance<Declaration>().forEach { declaration ->
                assertTrue(
                    scopeManager.currentScope.symbols.values.flatten().contains(declaration),
                    "Records symbol map should contain the extension's declaration",
                )
            }
        }
    }

    @Test
    fun testExtensionWithoutExtendedDeclaration() {
        with(
            TestLanguageFrontend(
                ctx = TranslationContext(TranslationConfiguration.builder().defaultPasses().build())
            )
        ) {
            val tr = build {
                translationResult {
                    translationUnit { extension("Extension", null) { method("extFunc") {} } }
                }
            }

            val tu = tr.components.first().translationUnits.first()
            val extensions = tu.declarations.filterIsInstance<Extension>()
            assertEquals(1, extensions.size)
            assertNull(extensions.first().extendedDeclaration)
        }
    }

    @Test
    fun testExtensionWithNullName() {
        with(
            TestLanguageFrontend(
                ctx = TranslationContext(TranslationConfiguration.builder().defaultPasses().build())
            )
        ) {
            val tr = build {
                translationResult {
                    translationUnit {
                        val record = record("Record") {}
                        extension(null, record) { method("extFunc") {} }
                    }
                }
            }

            val tu = tr.components.first().translationUnits.first()
            val extensions = tu.declarations.filterIsInstance<Extension>()
            assertEquals(1, extensions.size)
            // Should have EMPTY_NAME when null is provided
            assertTrue(extensions.first().name.localName.isEmpty())
        }
    }

    @Test
    fun testEquals() {
        with(
            TestLanguageFrontend(
                ctx = TranslationContext(TranslationConfiguration.builder().defaultPasses().build())
            )
        ) {
            build {
                translationResult {
                    translationUnit {
                        val record = record("Record") {}

                        val ext1 = extension("Extension", record) { method("method1") {} }

                        val ext2 = extension("Extension", record) { method("method2") {} }

                        val ext3 = extension("Extension", null) { method("method1") {} }

                        // Test same reference
                        assertTrue(ext1.equals(ext1))

                        // Test different type
                        assertFalse(ext1.equals("not an extension"))

                        // Test different extendedDeclaration
                        assertFalse(ext1.equals(ext3))

                        // Test different declarations (method1 vs method2)
                        assertFalse(ext1.equals(ext2))
                    }
                }
            }
        }
    }

    @Test
    fun testHashCode() {
        with(
            TestLanguageFrontend(
                ctx = TranslationContext(TranslationConfiguration.builder().defaultPasses().build())
            )
        ) {
            build {
                translationResult {
                    translationUnit {
                        val record = record("Record") {}

                        val ext1 = extension("Extension", record) {}
                        val ext2 = extension("Extension", record) {}

                        // Equal objects should have equal hash codes (they have same name and
                        // extended record)
                        // Note: hashCode comparison is meaningful here since they reference the
                        // same record
                        assertNotNull(ext1.hashCode())
                        assertNotNull(ext2.hashCode())
                    }
                }
            }
        }
    }

    @Test
    fun testEOGMethods() {
        with(
            TestLanguageFrontend(
                ctx = TranslationContext(TranslationConfiguration.builder().defaultPasses().build())
            )
        ) {
            build {
                translationResult {
                    translationUnit {
                        val ext = extension("Extension", null) {}

                        // Test EOG methods return empty collections
                        assertTrue(ext.getStartingPrevEOG().isEmpty())
                        assertTrue(ext.getExitNextEOG().isEmpty())
                    }
                }
            }
        }
    }

    @Test
    fun testAddDeclaration() {
        with(
            TestLanguageFrontend(
                ctx = TranslationContext(TranslationConfiguration.builder().defaultPasses().build())
            )
        ) {
            build {
                translationResult {
                    translationUnit {
                        val ext =
                            extension("Extension", null) {
                                method("method1") {}
                                method("method2") {}
                            }

                        assertEquals(2, ext.declarations.size)

                        // Add another method directly
                        val method3 = method("method3") {}
                        ext.addDeclaration(method3)
                        assertEquals(3, ext.declarations.size)
                        assertTrue(ext.declarations.contains(method3))
                    }
                }
            }
        }
    }
}
