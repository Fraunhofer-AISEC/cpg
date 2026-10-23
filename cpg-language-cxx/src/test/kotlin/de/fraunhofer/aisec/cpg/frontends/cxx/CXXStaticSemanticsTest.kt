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
package de.fraunhofer.aisec.cpg.frontends.cxx

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests that the CXX frontend eagerly interprets the `static` storage-class specifier into the
 * canonical [Visibility] and [ValueDeclaration.isStatic] properties, according to the syntactic
 * context it appears in.
 */
class CXXStaticSemanticsTest {
    @Test
    fun testCppStaticSemantics() {
        val file = File("src/test/resources/cxx/static_semantics.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        assertNotNull(tu)

        // A file-scope `static` variable/function has internal linkage, i.e. INTERNAL visibility.
        val internalGlobal = tu.variables["internalGlobal"]
        assertNotNull(internalGlobal)
        assertEquals(Visibility.INTERNAL, internalGlobal.visibility)
        assertTrue(internalGlobal.hasInternalLinkage)
        assertFalse(internalGlobal.isStatic, "file-scope static is linkage, not a static member")

        val exportedGlobal = tu.variables["exportedGlobal"]
        assertNotNull(exportedGlobal)
        assertEquals(Visibility.UNKNOWN, exportedGlobal.visibility)
        assertFalse(exportedGlobal.hasInternalLinkage)

        val internalFunction = tu.functions["internalFunction"]
        assertNotNull(internalFunction)
        assertEquals(Visibility.INTERNAL, internalFunction.visibility)

        val exportedFunction = tu.functions["exportedFunction"]
        assertNotNull(exportedFunction)
        assertEquals(Visibility.UNKNOWN, exportedFunction.visibility)

        // A `static` record member is a static (class-level) member, not internal linkage.
        val myClass = tu.records["MyClass"]
        assertNotNull(myClass)

        val staticField = myClass.fields["staticField"]
        assertNotNull(staticField)
        assertTrue(staticField.isStatic, "a static data member must be marked static")

        val instanceField = myClass.fields["instanceField"]
        assertNotNull(instanceField)
        assertFalse(instanceField.isStatic)

        val staticMethod = myClass.methods["staticMethod"]
        assertNotNull(staticMethod)
        assertTrue(staticMethod.isStatic)

        val instanceMethod = myClass.methods["instanceMethod"]
        assertNotNull(instanceMethod)
        assertFalse(instanceMethod.isStatic)

        // A function-local `static` says nothing about linkage or member binding.
        val localStatic =
            tu.functions["useLocalStatic"].variables["localStatic"] ?: tu.variables["localStatic"]
        assertNotNull(localStatic)
        assertEquals(Visibility.UNKNOWN, localStatic.visibility)
        assertFalse(localStatic.isStatic)
    }

    @Test
    fun testCStaticGivesInternalLinkage() {
        val file = File("src/test/resources/c/static_semantics.c")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
            }
        assertNotNull(tu)

        val internalGlobal = tu.variables["internalGlobal"]
        assertNotNull(internalGlobal)
        assertEquals(Visibility.INTERNAL, internalGlobal.visibility)

        val exportedGlobal = tu.variables["exportedGlobal"]
        assertNotNull(exportedGlobal)
        assertFalse(exportedGlobal.hasInternalLinkage)

        val internalFunction = tu.functions["internalFunction"]
        assertNotNull(internalFunction)
        assertEquals(Visibility.INTERNAL, internalFunction.visibility)

        val localStatic =
            tu.functions["useLocalStatic"].variables["localStatic"] ?: tu.variables["localStatic"]
        assertNotNull(localStatic)
        assertFalse(localStatic.hasInternalLinkage)
    }
}
