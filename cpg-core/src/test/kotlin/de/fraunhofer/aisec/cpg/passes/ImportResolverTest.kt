/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.ScopeManager
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.TypeManager
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.builder.translationResult
import de.fraunhofer.aisec.cpg.graph.edges.scopes.ImportStyle
import de.fraunhofer.aisec.cpg.graph.newImportDeclaration
import de.fraunhofer.aisec.cpg.graph.newNamespaceDeclaration
import de.fraunhofer.aisec.cpg.graph.newTranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.newVariableDeclaration
import de.fraunhofer.aisec.cpg.graph.parseName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ImportResolverTest {
    @Test
    fun testImportOrderResolve() {
        val frontend =
            TestLanguageFrontend(
                ctx =
                    TranslationContext(
                        TranslationConfiguration.builder().defaultPasses().build(),
                        ScopeManager(),
                        TypeManager(),
                    )
            )
        var result =
            frontend.build {
                translationResult {
                    with(frontend) {
                            // We create two translation units with one namespace each. One file
                            // directly imports the other namespace (let's start easy). We
                            // intentionally
                            // create them in reverse order
                            var tuB = newTranslationUnitDeclaration("file.b")
                            scopeManager.resetToGlobal(tuB)
                            var pkgB = newNamespaceDeclaration("b")
                            scopeManager.addDeclaration(pkgB)
                            scopeManager.enterScope(pkgB)
                            var import =
                                newImportDeclaration(
                                    parseName("a"),
                                    style = ImportStyle.IMPORT_NAMESPACE,
                                )
                            scopeManager.addDeclaration(import)
                            import =
                                newImportDeclaration(
                                    parseName("c.bar"),
                                    style = ImportStyle.IMPORT_SINGLE_SYMBOL_FROM_NAMESPACE,
                                )
                            scopeManager.addDeclaration(import)
                            scopeManager.leaveScope(pkgB)
                            tuB
                        }
                        .also { this.addTranslationUnit(it) }

                    with(frontend) {
                            var tuA = newTranslationUnitDeclaration("file.a")
                            scopeManager.resetToGlobal(tuA)
                            var pkgA = newNamespaceDeclaration("a")
                            scopeManager.addDeclaration(pkgA)
                            scopeManager.enterScope(pkgA)
                            var foo = newVariableDeclaration(parseName("a.foo"))
                            scopeManager.addDeclaration(foo)
                            scopeManager.leaveScope(pkgA)
                            tuA
                        }
                        .also { this.addTranslationUnit(it) }

                    with(frontend) {
                            var tuA = newTranslationUnitDeclaration("file.c")
                            scopeManager.resetToGlobal(tuA)
                            var pkgA = newNamespaceDeclaration("c")
                            scopeManager.addDeclaration(pkgA)
                            scopeManager.enterScope(pkgA)
                            var foo = newVariableDeclaration(parseName("c.bar"))
                            scopeManager.addDeclaration(foo)
                            scopeManager.leaveScope(pkgA)
                            tuA
                        }
                        .also { this.addTranslationUnit(it) }
                }
            }

        assertNotNull(result)
        var foo = result.variables["a.foo"]
        assertNotNull(foo)

        var app = result.components.firstOrNull()
        assertNotNull(app)

        // a has 0 dependencies
        var a =
            app.translationUnitDependencies?.entries?.firstOrNull {
                it.key.name.toString() == "file.a"
            }
        assertNotNull(a)
        assertEquals(0, a.value.size)

        // c has 0 dependencies
        var c =
            app.translationUnitDependencies?.entries?.firstOrNull {
                it.key.name.toString() == "file.c"
            }
        assertNotNull(c)
        assertEquals(0, c.value.size)

        // b has two dependencies (a, c)
        var b =
            app.translationUnitDependencies?.entries?.firstOrNull {
                it.key.name.toString() == "file.b"
            }
        assertNotNull(b)
        assertEquals(2, b.value.size)
        assertEquals(setOf(a.key, c.key), b.value)
    }
}
