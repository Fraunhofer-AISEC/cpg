/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.passes.scopes

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.translationResult
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.edges.scopes.ImportStyle
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

/**
 * Tests that the imports of a file (in its [de.fraunhofer.aisec.cpg.graph.scopes.FileScope]) are
 * visible to an unqualified lookup in this file, even if the file scope is not part of the parent
 * chain of the scope where the lookup starts. This is the case for languages, where a namespace
 * (package) is shared between multiple files, e.g., Java and Go.
 */
class FileScopeLookupTest {
    private fun frontend() =
        TestLanguageFrontend(
            ctx = TranslationContext(TranslationConfiguration.builder().defaultPasses().build())
        )

    /** Declares the variables `q.foo` and `r.bar` in a separate file. */
    private fun LanguageFrontend<*, *>.declareTargets(): TranslationUnit {
        val tu = newTranslationUnit("targets.file")
        scopeManager.resetToGlobal(tu)
        newNamespace("q", holder = tu, enterScope = true) {
            newVariable(parseName("q.foo"), holder = it)
        }
        newNamespace("r", holder = tu, enterScope = true) {
            newVariable(parseName("r.bar"), holder = it)
        }
        return tu
    }

    /** Creates a function `name` in the current scope that references [refs]. */
    private fun LanguageFrontend<*, *>.functionWithRefs(
        name: String,
        holder: de.fraunhofer.aisec.cpg.graph.DeclarationHolder,
        vararg refs: String,
    ) {
        newFunction(name, holder = holder, enterScope = true) { function ->
            function.body =
                newBlock(enterScope = true) { block ->
                    refs.forEach { block.statements += newReference(it) }
                }
        }
    }

    private fun TranslationResult.ref(function: String, name: String): Reference {
        val ref = functions[function]?.refs?.singleOrNull { it.name.localName == name }
        assertNotNull(ref)
        return ref
    }

    @Test
    fun testFileScopeBelowNamespace() {
        // Java-like: the file scope (with the imports) is entered after (i.e., below) the namespace
        val result =
            frontend().build {
                val targets = declareTargets()

                val tuA = newTranslationUnit("a.file")
                scopeManager.resetToGlobal(tuA)
                newNamespace("p", holder = tuA, enterScope = true) { pkg ->
                    functionWithRefs("useA", pkg, "foo")

                    scopeManager.enterScope(tuA)
                    newImport(
                        parseName("q.foo"),
                        ImportStyle.IMPORT_SINGLE_SYMBOL_FROM_NAMESPACE,
                        holder = tuA,
                    )
                    scopeManager.leaveScope(tuA)
                }

                translationResult {
                    components.firstOrNull()?.translationUnits?.addAll(listOf(targets, tuA))
                }
            }

        assertEquals(result.variables["q.foo"], result.ref("useA", "foo").refersTo)
    }

    @Test
    fun testFileScopeAboveSharedNamespace() {
        // Go-like: the file scope is entered first and the namespace below it. Since the namespace
        // scope is shared between both files, its parent is the file scope of the first file.
        val result =
            frontend().build {
                val targets = declareTargets()

                val tuA = newTranslationUnit("a.file")
                scopeManager.resetToGlobal(tuA)
                scopeManager.enterScope(tuA)
                newImport(
                    parseName("q.foo"),
                    ImportStyle.IMPORT_SINGLE_SYMBOL_FROM_NAMESPACE,
                    holder = tuA,
                )
                newNamespace("p", holder = tuA, enterScope = true) { pkg ->
                    functionWithRefs("useA", pkg, "foo")
                }
                scopeManager.leaveScope(tuA)

                val tuB = newTranslationUnit("b.file")
                scopeManager.resetToGlobal(tuB)
                scopeManager.enterScope(tuB)
                newImport(
                    parseName("r.bar"),
                    ImportStyle.IMPORT_SINGLE_SYMBOL_FROM_NAMESPACE,
                    holder = tuB,
                )
                newNamespace("p", holder = tuB, enterScope = true) { pkg ->
                    functionWithRefs("useB", pkg, "foo", "bar")
                }
                scopeManager.leaveScope(tuB)

                translationResult {
                    components.firstOrNull()?.translationUnits?.addAll(listOf(targets, tuA, tuB))
                }
            }

        assertEquals(result.variables["q.foo"], result.ref("useA", "foo").refersTo)

        // b.file can see its own import, but not the one of a.file
        assertEquals(result.variables["r.bar"], result.ref("useB", "bar").refersTo)
        assertNotEquals(result.variables["q.foo"], result.ref("useB", "foo").refersTo)
    }
}
