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
package de.fraunhofer.aisec.cpg.frontends.golang

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.*

class VisibilityTest {
    /**
     * Unit test for the pure casing-to-[Visibility] mapping, including the tricky cases: the blank
     * identifier, empty/unnamed declarations and non-ASCII (Unicode) first runes.
     */
    @Test
    fun testVisibilityOfMapping() {
        // Exported: upper-case first rune -> PUBLIC
        assertEquals(Visibility.PUBLIC, GoLanguage.visibilityOf("Foo"))
        assertEquals(Visibility.PUBLIC, GoLanguage.visibilityOf("F"))
        // Unexported: lower-case first rune -> PACKAGE
        assertEquals(Visibility.PACKAGE, GoLanguage.visibilityOf("foo"))
        assertEquals(Visibility.PACKAGE, GoLanguage.visibilityOf("f"))
        // Leading underscore is not an upper-case letter -> unexported -> PACKAGE
        assertEquals(Visibility.PACKAGE, GoLanguage.visibilityOf("_foo"))

        // The blank identifier and unnamed declarations have no export semantics -> UNKNOWN
        assertEquals(Visibility.UNKNOWN, GoLanguage.visibilityOf("_"))
        assertEquals(Visibility.UNKNOWN, GoLanguage.visibilityOf(""))

        // Unicode: the decision is made on the first *rune*, not the first UTF-16 char.
        assertEquals(Visibility.PUBLIC, GoLanguage.visibilityOf("Über"))
        assertEquals(Visibility.PACKAGE, GoLanguage.visibilityOf("über"))
    }

    @Test
    fun testVisibility() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("visibility.go").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<GoLanguage>()
            }
        assertNotNull(tu)

        val p = tu.namespaces["p"]
        assertNotNull(p)

        // Top-level functions
        val exportedFunc = p.functions["ExportedFunc"]
        assertNotNull(exportedFunc)
        assertEquals(Visibility.PUBLIC, exportedFunc.visibility)

        val unexportedFunc = p.functions["unexportedFunc"]
        assertNotNull(unexportedFunc)
        assertEquals(Visibility.PACKAGE, unexportedFunc.visibility)

        // Top-level variables
        val exportedVar = p.variables["p.ExportedVar"]
        assertNotNull(exportedVar)
        assertEquals(Visibility.PUBLIC, exportedVar.visibility)

        val unexportedVar = p.variables["p.unexportedVar"]
        assertNotNull(unexportedVar)
        assertEquals(Visibility.PACKAGE, unexportedVar.visibility)

        // Top-level constants
        val exportedConst = p.variables["p.ExportedConst"]
        assertNotNull(exportedConst)
        assertEquals(Visibility.PUBLIC, exportedConst.visibility)

        val unexportedConst = p.variables["p.unexportedConst"]
        assertNotNull(unexportedConst)
        assertEquals(Visibility.PACKAGE, unexportedConst.visibility)

        // Unicode top-level variables
        val ueber = p.variables["p.Über"]
        assertNotNull(ueber)
        assertEquals(Visibility.PUBLIC, ueber.visibility)

        val ueberLower = p.variables["p.über"]
        assertNotNull(ueberLower)
        assertEquals(Visibility.PACKAGE, ueberLower.visibility)

        // Exported struct type and its fields
        val exportedStruct = p.records["p.ExportedStruct"]
        assertNotNull(exportedStruct)
        assertEquals(Visibility.PUBLIC, exportedStruct.visibility)

        val exportedField = exportedStruct.fields["ExportedField"]
        assertNotNull(exportedField)
        assertEquals(Visibility.PUBLIC, exportedField.visibility)

        val unexportedField = exportedStruct.fields["unexportedField"]
        assertNotNull(unexportedField)
        assertEquals(Visibility.PACKAGE, unexportedField.visibility)

        // Unexported struct type
        val unexportedStruct = p.records["p.unexportedStruct"]
        assertNotNull(unexportedStruct)
        assertEquals(Visibility.PACKAGE, unexportedStruct.visibility)

        // Methods (declared outside the record in Go)
        val exportedMethod = exportedStruct.toType().methods["ExportedMethod"]
        assertNotNull(exportedMethod)
        assertEquals(Visibility.PUBLIC, exportedMethod.visibility)

        val unexportedMethod = exportedStruct.toType().methods["unexportedMethod"]
        assertNotNull(unexportedMethod)
        assertEquals(Visibility.PACKAGE, unexportedMethod.visibility)

        // Exported interface type and its methods
        val exportedInterface = p.records["p.ExportedInterface"]
        assertNotNull(exportedInterface)
        assertEquals(Visibility.PUBLIC, exportedInterface.visibility)

        val exportedDo = exportedInterface.methods["ExportedDo"]
        assertNotNull(exportedDo)
        assertEquals(Visibility.PUBLIC, exportedDo.visibility)

        val unexportedDo = exportedInterface.methods["unexportedDo"]
        assertNotNull(unexportedDo)
        assertEquals(Visibility.PACKAGE, unexportedDo.visibility)

        // Type aliases are exported/unexported like any other named type.
        val aliases = tu.allChildren<Typedef>()
        val exportedAlias = aliases.singleOrNull { it.name.localName == "ExportedAlias" }
        assertNotNull(exportedAlias)
        assertEquals(Visibility.PUBLIC, exportedAlias.visibility)

        val unexportedAlias = aliases.singleOrNull { it.name.localName == "unexportedAlias" }
        assertNotNull(unexportedAlias)
        assertEquals(Visibility.PACKAGE, unexportedAlias.visibility)

        // Local variables are block-scoped and carry no visibility restriction.
        val local = p.functions["local"]
        assertNotNull(local)
        val notExported = local.variables["notExported"]
        assertNotNull(notExported)
        assertEquals(Visibility.UNKNOWN, notExported.visibility)

        // The same holds for a classic `var` inside a block: this drives the value-spec path with a
        // non-package scope, so even the upper-case name must stay UNKNOWN. This is what keeps the
        // scope guard in GoLanguage.applyModifiers honest.
        val blockScoped = local.variables["Exported"]
        assertNotNull(blockScoped)
        assertEquals(Visibility.UNKNOWN, blockScoped.visibility)

        val blockScopedLower = local.variables["unexported"]
        assertNotNull(blockScopedLower)
        assertEquals(Visibility.UNKNOWN, blockScopedLower.visibility)
    }
}
