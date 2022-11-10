/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.assertFullName
import de.fraunhofer.aisec.cpg.assertLocalName
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class NameTest {

    @Test
    fun testToString() {
        val simple = Name("myFunc")
        assertEquals("myFunc", simple.toString())

        val name = Name("string", Name("std"), "::")
        assertEquals("std::string", name.toString())

        val complex = Name("function", Name("namespace", Name("my")))
        assertEquals("my.namespace.function", complex.toString())
    }

    @Test
    fun testEquals() {
        val a = Name("string", Name("std"), "::")
        val b = Name("string", Name("std"), "::")
        val c = Name("vector", Name("std"), "::")

        assertEquals(a, b)
        assertNotEquals(a, c)
        assertNotEquals(b, c)
    }

    @Test
    fun testParse() {
        val fqn = "std::string"

        val name = Name.parse(fqn, "::")
        assertEquals(fqn, name.toString())
    }

    @Test
    fun testParentNames() {
        with(TestLanguageFrontend()) {
            val tu = newTranslationUnitDeclaration("file.extension")
            this.scopeManager.resetToGlobal(tu)

            val func = newFunctionDeclaration("main")
            assertLocalName("main", func)

            val myClass = newRecordDeclaration("MyClass", "class")
            assertLocalName("MyClass", myClass)

            this.scopeManager.enterScope(myClass)

            val method =
                newMethodDeclaration("doSomething", isStatic = false, recordDeclaration = myClass)
            assertLocalName("doSomething", method)
            assertFullName("MyClass::doSomething", method)

            this.scopeManager.leaveScope(myClass)
        }
    }
}
