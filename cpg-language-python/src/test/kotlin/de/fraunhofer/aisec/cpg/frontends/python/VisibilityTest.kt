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
package de.fraunhofer.aisec.cpg.frontends.python

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class VisibilityTest : BaseTest() {

    private val topLevel = Path.of("src", "test", "resources", "python")

    /**
     * Pure unit test for the language-level mapping [PythonLanguage.visibilityForName], covering
     * the tricky cases (single vs. double leading underscore, dunders) independently of the
     * frontend.
     */
    @Test
    fun testVisibilityForName() {
        val language = PythonLanguage()

        // Public names.
        assertEquals(Visibility.PUBLIC, language.visibilityForName("foo"))
        assertEquals(Visibility.PUBLIC, language.visibilityForName("public_attr"))

        // Single leading underscore -> non-public convention -> PROTECTED.
        assertEquals(Visibility.PROTECTED, language.visibilityForName("_x"))
        assertEquals(Visibility.PROTECTED, language.visibilityForName("_protected_method"))

        // Double leading underscore, no trailing dunder -> name-mangled -> PRIVATE.
        assertEquals(Visibility.PRIVATE, language.visibilityForName("__x"))
        assertEquals(Visibility.PRIVATE, language.visibilityForName("__private_method"))

        // Dunders are public despite the leading double underscore.
        assertEquals(Visibility.PUBLIC, language.visibilityForName("__init__"))
        assertEquals(Visibility.PUBLIC, language.visibilityForName("__str__"))
        assertEquals(Visibility.PUBLIC, language.visibilityForName("__magic__"))
    }

    @Test
    fun testMemberVisibility() {
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("visibility.py").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(tu)

        val myClass = tu.records["MyClass"]
        assertNotNull(myClass)

        // Class attributes (fields declared in the class body).
        assertEquals(Visibility.PUBLIC, myClass.fields["public_attr"]?.visibility)
        assertEquals(Visibility.PROTECTED, myClass.fields["_protected_attr"]?.visibility)
        // Python mangles `__private_attr` to `_MyClass__private_attr`, but the local name still
        // starts with a double underscore, so it is mapped to PRIVATE.
        assertEquals(Visibility.PRIVATE, myClass.fields["__private_attr"]?.visibility)
        assertEquals(Visibility.PUBLIC, myClass.fields["__magic__"]?.visibility)

        // Instance attributes (self.<name> assignments inside __init__).
        assertEquals(Visibility.PUBLIC, myClass.fields["public_field"]?.visibility)
        assertEquals(Visibility.PROTECTED, myClass.fields["_protected_field"]?.visibility)
        assertEquals(Visibility.PRIVATE, myClass.fields["__private_field"]?.visibility)

        // Methods.
        assertEquals(Visibility.PUBLIC, myClass.methods["public_method"]?.visibility)
        assertEquals(Visibility.PROTECTED, myClass.methods["_protected_method"]?.visibility)
        assertEquals(Visibility.PRIVATE, myClass.methods["__private_method"]?.visibility)

        // The constructor and the dunder method are public.
        assertEquals(Visibility.PUBLIC, myClass.constructors.singleOrNull()?.visibility)
        assertEquals(Visibility.PUBLIC, myClass.methods["__str__"]?.visibility)

        // Free (non-member) functions keep the default: Python has no module-level access control
        // that we model here, so visibility stays UNKNOWN.
        val freeFunction = tu.functions["free_function"]
        assertNotNull(freeFunction)
        assertEquals(Visibility.UNKNOWN, freeFunction.visibility)
    }
}
