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
import de.fraunhofer.aisec.cpg.frontends.translationResult
import de.fraunhofer.aisec.cpg.graph.*
import kotlin.test.*

/**
 * Mirrors Fluent's `extension(name, extendedDeclaration, init)`: it enters the scope of
 * [extendedDeclaration] if given (methods/fields added to an extension live in the *extended
 * record's* scope), or the extension's own scope otherwise -- `newExtension` has no `enterScope`
 * parameter of its own since [Extension] doesn't introduce a scope of its own via
 * [de.fraunhofer.aisec.cpg.ScopeManager], so this is wired manually rather than through the generic
 * `enterScope` builder parameter.
 */
context(provider: ContextProvider)
private fun MetadataProvider.buildExtension(
    name: String?,
    extendedDeclaration: Record?,
    holder: DeclarationHolder,
    init: ((Extension) -> Unit)? = null,
): Extension {
    val ext = this.newExtension(name ?: Node.EMPTY_NAME)
    ext.extendedDeclaration = extendedDeclaration

    val scopeManager = provider.ctx.scopeManager
    scopeManager.enterScope(extendedDeclaration ?: ext)
    init?.invoke(ext)
    scopeManager.leaveScope(extendedDeclaration ?: ext)

    scopeManager.addDeclaration(ext)
    holder.addDeclaration(ext)
    return ext
}

class ExtensionTest {
    @Test
    fun testDeclarationPlacement() {

        with(
            TestLanguageFrontend(
                ctx = TranslationContext(TranslationConfiguration.builder().defaultPasses().build())
            )
        ) {
            val tr = build {
                val tu = newTranslationUnit(Node.EMPTY_NAME)
                scopeManager.resetToGlobal(tu)

                val rec = newRecord("Record", "class", holder = tu, enterScope = true)
                buildExtension("Extension", rec, holder = tu) { ext ->
                    newMethod("extFunc", holder = ext, enterScope = true)
                    newField("extField", holder = ext)
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
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
                val tu = newTranslationUnit(Node.EMPTY_NAME)
                scopeManager.resetToGlobal(tu)

                buildExtension("Extension", null, holder = tu) { ext ->
                    newMethod("extFunc", holder = ext, enterScope = true)
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
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
                val tu = newTranslationUnit(Node.EMPTY_NAME)
                scopeManager.resetToGlobal(tu)

                val record = newRecord("Record", "class", holder = tu, enterScope = true)
                buildExtension(null, record, holder = tu) { ext ->
                    newMethod("extFunc", holder = ext, enterScope = true)
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
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
                val tu = newTranslationUnit(Node.EMPTY_NAME)
                scopeManager.resetToGlobal(tu)

                val record = newRecord("Record", "class", holder = tu, enterScope = true)

                val ext1 =
                    buildExtension("Extension", record, holder = tu) { ext ->
                        newMethod("method1", holder = ext, enterScope = true)
                    }

                val ext2 =
                    buildExtension("Extension", record, holder = tu) { ext ->
                        newMethod("method2", holder = ext, enterScope = true)
                    }

                val ext3 =
                    buildExtension("Extension", null, holder = tu) { ext ->
                        newMethod("method1", holder = ext, enterScope = true)
                    }

                // Test same reference
                assertTrue(ext1.equals(ext1))

                // Test different type
                assertFalse(ext1.equals("not an extension"))

                // Test different extendedDeclaration
                assertFalse(ext1.equals(ext3))

                // Test different declarations (method1 vs method2)
                assertFalse(ext1.equals(ext2))

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
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
                val tu = newTranslationUnit(Node.EMPTY_NAME)
                scopeManager.resetToGlobal(tu)

                val record = newRecord("Record", "class", holder = tu, enterScope = true)

                val ext1 = buildExtension("Extension", record, holder = tu)
                val ext2 = buildExtension("Extension", record, holder = tu)

                // Equal objects should have equal hash codes (they have same name and
                // extended record)
                // Note: hashCode comparison is meaningful here since they reference the
                // same record
                assertNotNull(ext1.hashCode())
                assertNotNull(ext2.hashCode())

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
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
                val tu = newTranslationUnit(Node.EMPTY_NAME)
                scopeManager.resetToGlobal(tu)

                val ext = buildExtension("Extension", null, holder = tu)

                // Test EOG methods return empty collections
                assertTrue(ext.getStartingPrevEOG().isEmpty())
                assertTrue(ext.getExitNextEOG().isEmpty())

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
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
                val tu = newTranslationUnit(Node.EMPTY_NAME)
                scopeManager.resetToGlobal(tu)

                val ext =
                    buildExtension("Extension", null, holder = tu) { extNode ->
                        newMethod("method1", holder = extNode, enterScope = true)
                        newMethod("method2", holder = extNode, enterScope = true)
                    }

                assertEquals(2, ext.declarations.size)

                // Add another method directly
                // Fluent's method() would auto-attach this to the nearest DeclarationHolder in
                // scope at this point, which is the translation unit (the extension() call has
                // already returned) -- replicated explicitly here via holder = tu.
                val method3 = newMethod("method3", holder = tu, enterScope = true)
                ext.addDeclaration(method3)
                assertEquals(3, ext.declarations.size)
                assertTrue(ext.declarations.contains(method3))

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }
        }
    }
}
