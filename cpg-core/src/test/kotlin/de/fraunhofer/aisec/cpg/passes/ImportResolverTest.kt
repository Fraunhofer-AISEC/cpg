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

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.HasClasses
import de.fraunhofer.aisec.cpg.frontends.HasImportsFromRecords
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.translationResult
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.edges.scopes.ImportStyle
import de.fraunhofer.aisec.cpg.graph.newImport
import de.fraunhofer.aisec.cpg.graph.newNamespace
import de.fraunhofer.aisec.cpg.graph.newTranslationUnit
import de.fraunhofer.aisec.cpg.graph.newVariable
import de.fraunhofer.aisec.cpg.graph.parseName
import de.fraunhofer.aisec.cpg.graph.scopes.RecordScope
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImportResolverTest {
    @Test
    fun testImportOrderResolve() {
        val frontend =
            TestLanguageFrontend(
                ctx = TranslationContext(TranslationConfiguration.builder().defaultPasses().build())
            )
        var result =
            frontend.build {
                // We create two translation units with one namespace each. One file directly
                // imports the other namespace (let's start easy). We intentionally create them in
                // reverse order.
                val tuB = newTranslationUnit("file.b")
                scopeManager.resetToGlobal(tuB)
                newNamespace("b", holder = tuB, enterScope = true) { pkgB ->
                    newImport(parseName("a"), style = ImportStyle.IMPORT_NAMESPACE, holder = pkgB)
                    newImport(
                        parseName("c.bar"),
                        style = ImportStyle.IMPORT_SINGLE_SYMBOL_FROM_NAMESPACE,
                        holder = pkgB,
                    )
                }

                val tuA = newTranslationUnit("file.a")
                scopeManager.resetToGlobal(tuA)
                newNamespace("a", holder = tuA, enterScope = true) { pkgA ->
                    newVariable(parseName("a.foo"), holder = pkgA)
                }

                val tuC = newTranslationUnit("file.c")
                scopeManager.resetToGlobal(tuC)
                newNamespace("c", holder = tuC, enterScope = true) { pkgC ->
                    newVariable(parseName("c.bar"), holder = pkgC)
                }

                translationResult {
                    val app = components.firstOrNull()
                    app?.translationUnits?.add(tuB)
                    app?.translationUnits?.add(tuA)
                    app?.translationUnits?.add(tuC)
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

    /**
     * Builds two files: `file.a` declares a record `a.C` with a static and a non-static method and
     * `file.b` imports from it, both a single symbol and all symbols (like Java's static imports).
     */
    private fun buildRecordImports(language: TestLanguage): TranslationResult {
        val frontend =
            TestLanguageFrontend(
                ctx =
                    TranslationContext(TranslationConfiguration.builder().defaultPasses().build()),
                language = language,
            )
        return frontend.build {
            val tuB = newTranslationUnit("file.b")
            scopeManager.resetToGlobal(tuB)
            newNamespace("b", holder = tuB, enterScope = true) { pkgB ->
                newImport(
                    parseName("a.C.staticMethod"),
                    style = ImportStyle.IMPORT_SINGLE_SYMBOL_FROM_NAMESPACE,
                    holder = pkgB,
                ) {
                    it.isStatic = true
                }
                newImport(
                    parseName("a.C"),
                    style = ImportStyle.IMPORT_ALL_SYMBOLS_FROM_NAMESPACE,
                    holder = pkgB,
                ) {
                    it.isStatic = true
                }
            }

            val tuA = newTranslationUnit("file.a")
            scopeManager.resetToGlobal(tuA)
            newNamespace("a", holder = tuA, enterScope = true) { pkgA ->
                newRecord("C", "class", holder = pkgA, enterScope = true) { record ->
                    newMethod(
                        "staticMethod",
                        isStatic = true,
                        recordDeclaration = record,
                        holder = record,
                    )
                    newMethod("instanceMethod", recordDeclaration = record, holder = record)
                }
            }

            translationResult {
                val app = components.firstOrNull()
                app?.translationUnits?.add(tuB)
                app?.translationUnits?.add(tuA)
            }
        }
    }

    @Test
    fun testImportFromRecord() {
        val result = buildRecordImports(RecordImportTestLanguage())

        val record = result.records["a.C"]
        assertNotNull(record)
        val staticMethod = result.methods["a.C.staticMethod"]
        assertNotNull(staticMethod)
        val instanceMethod = result.methods["a.C.instanceMethod"]
        assertNotNull(instanceMethod)

        // Both imports point to the scope of the record
        val pkgB = result.namespaces["b"]
        assertNotNull(pkgB)
        val recordScope = result.finalCtx.scopeManager.lookupScope(record)
        assertIs<RecordScope>(recordScope)
        assertEquals<Set<Scope>?>(
            setOf(recordScope),
            result.finalCtx.scopeManager.lookupScope(pkgB)?.importedScopes?.toSet(),
        )

        // Only the static method is imported, both as a single symbol and with the wildcard
        val (single, wildcard) =
            result.imports.partition { it.style == ImportStyle.IMPORT_SINGLE_SYMBOL_FROM_NAMESPACE }
        assertEquals(
            listOf<Declaration>(staticMethod),
            single.single().importedSymbols.values.flatten(),
        )
        val wildcardSymbols = wildcard.single().importedSymbols.values.flatten()
        assertContains(wildcardSymbols, staticMethod)
        assertFalse(instanceMethod in wildcardSymbols)

        // The record is the source of the import, so file.b depends on file.a
        val app = result.components.firstOrNull()
        assertNotNull(app)
        val b =
            app.translationUnitDependencies?.entries?.firstOrNull {
                it.key.name.toString() == "file.b"
            }
        assertNotNull(b)
        assertEquals(setOf("file.a"), b.value.map { it.name.toString() }.toSet())
    }

    @Test
    fun testImportFromRecordWithoutTrait() {
        val result = buildRecordImports(TestLanguage())

        // A language without the trait cannot import from a record
        val pkgB = result.namespaces["b"]
        assertNotNull(pkgB)
        assertTrue(
            result.finalCtx.scopeManager.lookupScope(pkgB)?.importedScopes?.none {
                it is RecordScope
            } == true
        )
    }

    @Test
    fun testInferImportTarget() {
        val frontend =
            TestLanguageFrontend(
                ctx =
                    TranslationContext(TranslationConfiguration.builder().defaultPasses().build()),
                language = RecordImportTestLanguage(),
            )
        val result =
            frontend.build {
                val tu = newTranslationUnit("file.b")
                scopeManager.resetToGlobal(tu)
                newNamespace("b", holder = tu, enterScope = true) { pkgB ->
                    // The target of this static import is not part of our graph
                    newImport(
                        parseName("a.D.something"),
                        style = ImportStyle.IMPORT_SINGLE_SYMBOL_FROM_NAMESPACE,
                        holder = pkgB,
                    ) {
                        it.isStatic = true
                    }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }

        // Since this is a static import, we need to infer a record instead of a namespace
        val record = result.records["a.D"]
        assertNotNull(record)
        assertTrue(record.isInferred)
        assertNull(result.namespaces["a.D"])

        val pkgB = result.namespaces["b"]
        assertNotNull(pkgB)
        val recordScope = result.finalCtx.scopeManager.lookupScope(record)
        assertNotNull(recordScope)
        assertEquals<Set<Scope>?>(
            setOf(recordScope),
            result.finalCtx.scopeManager.lookupScope(pkgB)?.importedScopes?.toSet(),
        )
    }
}

/** A [TestLanguage] that supports imports from records, similar to Java's static imports. */
class RecordImportTestLanguage : TestLanguage(), HasClasses, HasImportsFromRecords
