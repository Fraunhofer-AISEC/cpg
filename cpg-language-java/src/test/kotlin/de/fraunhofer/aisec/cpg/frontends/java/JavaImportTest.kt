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
package de.fraunhofer.aisec.cpg.frontends.java

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.edges.scopes.ImportStyle
import de.fraunhofer.aisec.cpg.graph.scopes.FileScope
import de.fraunhofer.aisec.cpg.graph.scopes.NamespaceScope
import de.fraunhofer.aisec.cpg.graph.scopes.RecordScope
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class JavaImportTest : BaseTest() {
    private val topLevel = Path.of("src", "test", "resources", "java", "imports")

    @Test
    fun testImports() {
        val result =
            analyze(
                listOf(
                    topLevel.resolve("a/C.java").toFile(),
                    topLevel.resolve("b/B.java").toFile(),
                ),
                topLevel,
                true,
            ) {
                it.registerLanguage<JavaLanguage>()
            }

        val tu = result.translationUnits.firstOrNull { it.name.endsWith("B.java") }
        assertNotNull(tu)

        // All four imports (plus the implicit `java.lang.*`) are real imports
        val imports = tu.imports.filter { !it.isImplicit }
        assertEquals(4, imports.size)

        val single = imports.single { it.import.toString() == "a.C" && !it.isStatic }
        assertEquals(ImportStyle.IMPORT_SINGLE_SYMBOL_FROM_NAMESPACE, single.style)

        val wildcard = imports.single { it.import.toString() == "a" }
        assertEquals(ImportStyle.IMPORT_ALL_SYMBOLS_FROM_NAMESPACE, wildcard.style)
        assertFalse(wildcard.isStatic)

        val staticSingle = imports.single { it.import.toString() == "a.C.staticMethod" }
        assertEquals(ImportStyle.IMPORT_SINGLE_SYMBOL_FROM_NAMESPACE, staticSingle.style)
        assertTrue(staticSingle.isStatic)

        val staticWildcard = imports.single { it.import.toString() == "a.C" && it.isStatic }
        assertEquals(ImportStyle.IMPORT_ALL_SYMBOLS_FROM_NAMESPACE, staticWildcard.style)

        // The non-static imports import from the package, the static ones from the record
        val scopeManager = result.finalCtx.scopeManager
        val fileScope = staticSingle.scope
        assertIs<FileScope>(fileScope)
        val record = result.records["a.C"]
        assertNotNull(record)
        val importedScopes = fileScope.importedScopes
        assertTrue(importedScopes.any { it is NamespaceScope && it.name.toString() == "a" })
        assertContains(importedScopes, scopeManager.lookupScope(record))
        assertTrue(importedScopes.none { it is RecordScope && it.astNode != record })

        // Only the static members (and nested types) of the record are imported
        val staticMethod = result.methods["a.C.staticMethod"]
        assertNotNull(staticMethod)
        assertEquals(
            listOf<Declaration>(staticMethod),
            staticSingle.importedSymbols.values.flatten(),
        )
        val wildcardSymbols =
            staticWildcard.importedSymbols.values.flatten().map { it.name.localName }
        assertEquals(setOf("staticField", "staticMethod", "Nested"), wildcardSymbols.toSet())

        // And the call to the statically imported method is resolved
        val call = result.calls["staticMethod"]
        assertNotNull(call)
        assertEquals(staticMethod, call.invokes.singleOrNull())
    }
}
