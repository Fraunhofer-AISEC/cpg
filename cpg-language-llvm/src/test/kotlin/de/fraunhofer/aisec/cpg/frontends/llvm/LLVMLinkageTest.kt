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
package de.fraunhofer.aisec.cpg.frontends.llvm

import de.fraunhofer.aisec.cpg.frontends.DeclarationContext
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests that LLVM IR [linkage types](https://llvm.org/docs/LangRef.html#linkage-types) are mapped
 * onto the canonical [Visibility] model, both on global variables and on functions.
 */
class LLVMLinkageTest {

    @Test
    fun testGlobalVariableLinkage() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("linkage.ll").toFile()), topLevel, true) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        // `external` is the default linkage in LLVM and is not spelled out. It is visible
        // everywhere
        // and therefore maps onto PUBLIC without adding a raw modifier.
        val publicGlobal = tu.variables["publicGlobal"]
        assertNotNull(publicGlobal)
        assertEquals(Visibility.PUBLIC, publicGlobal.visibility)
        assertTrue(publicGlobal.modifiers.isEmpty())

        // `internal` linkage behaves like C's file-scope `static`: confined to this module.
        val internalGlobal = tu.variables["internalGlobal"]
        assertNotNull(internalGlobal)
        assertEquals(Visibility.INTERNAL, internalGlobal.visibility)
        assertTrue("internal" in internalGlobal.modifiers)

        // `private` linkage is also confined to this module (INTERNAL) and the raw keyword is kept.
        val privateGlobal = tu.variables["privateGlobal"]
        assertNotNull(privateGlobal)
        assertEquals(Visibility.INTERNAL, privateGlobal.visibility)
        assertTrue("private" in privateGlobal.modifiers)

        // `weak` linkage carries no canonical visibility meaning, so the visibility stays UNKNOWN,
        // but the raw keyword is still recorded losslessly in the modifiers.
        val weakGlobal = tu.variables["weakGlobal"]
        assertNotNull(weakGlobal)
        assertEquals(Visibility.UNKNOWN, weakGlobal.visibility)
        assertTrue("weak" in weakGlobal.modifiers)
    }

    @Test
    fun testFunctionLinkage() {
        val topLevel = Path.of("src", "test", "resources", "llvm")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("linkage.ll").toFile()), topLevel, true) {
                it.registerLanguage<LLVMIRLanguage>()
            }

        val publicFunc = tu.functions["publicFunc"]
        assertNotNull(publicFunc)
        assertEquals(Visibility.PUBLIC, publicFunc.visibility)
        assertTrue(publicFunc.modifiers.isEmpty())

        val internalFunc = tu.functions["internalFunc"]
        assertNotNull(internalFunc)
        assertEquals(Visibility.INTERNAL, internalFunc.visibility)
        assertTrue("internal" in internalFunc.modifiers)

        val privateFunc = tu.functions["privateFunc"]
        assertNotNull(privateFunc)
        assertEquals(Visibility.INTERNAL, privateFunc.visibility)
        assertTrue("private" in privateFunc.modifiers)
    }

    @Test
    fun testInterpretKeyword() {
        val language = LLVMIRLanguage()

        // The two module-confining linkages map onto INTERNAL, regardless of context.
        for (keyword in LLVM_INTERNAL_LINKAGES) {
            assertEquals(
                Visibility.INTERNAL,
                language.interpretKeyword(keyword, DeclarationContext.GLOBAL).visibility,
            )
        }

        // The default `external` linkage maps onto PUBLIC.
        assertEquals(
            Visibility.PUBLIC,
            language.interpretKeyword(LLVM_EXTERNAL_LINKAGE, DeclarationContext.GLOBAL).visibility,
        )

        // A linkage type without a canonical meaning leaves the visibility untouched (null).
        assertEquals(null, language.interpretKeyword("weak", DeclarationContext.GLOBAL).visibility)
    }
}
