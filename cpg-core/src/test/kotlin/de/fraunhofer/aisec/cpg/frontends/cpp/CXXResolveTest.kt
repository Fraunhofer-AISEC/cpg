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
package de.fraunhofer.aisec.cpg.frontends.cpp

import de.fraunhofer.aisec.cpg.InferenceConfiguration
import de.fraunhofer.aisec.cpg.TestUtils.analyzeAndGetFirstTU
import de.fraunhofer.aisec.cpg.graph.bodyOrNull
import de.fraunhofer.aisec.cpg.graph.byNameOrNull
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import java.io.File
import kotlin.test.*

class CXXResolveTest {
    @Test
    fun testMethodResolve() {
        val file = File("src/test/resources/foo.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                // we want to infer records (namely class C in our file)
                it.inferenceConfiguration(
                    InferenceConfiguration.builder().inferRecords(true).build()
                )
            }
        assertNotNull(tu)

        val main = tu.byNameOrNull<FunctionDeclaration>("main")
        assertNotNull(main)

        val aFoo = main.bodyOrNull<MemberCallExpression>(0)
        assertNotNull(aFoo)
        assertEquals("foo", aFoo.fullName.localName)
        assertEquals("a", aFoo.base?.fullName?.localName)
        // a.foo should connect to A::foo
        assertEquals(
            "A",
            (aFoo.invokes.firstOrNull() as? MethodDeclaration)
                ?.recordDeclaration
                ?.fullName
                ?.localName
        )

        val bFoo = main.bodyOrNull<MemberCallExpression>(1)
        assertNotNull(bFoo)
        assertEquals("foo", bFoo.fullName.localName)
        assertEquals("b", bFoo.base?.fullName?.localName)
        // b.foo should connect to B::foo
        assertEquals(
            "B",
            (bFoo.invokes.firstOrNull() as? MethodDeclaration)
                ?.recordDeclaration
                ?.fullName
                ?.localName
        )

        val foo = main.bodyOrNull<CallExpression>(2)
        assertNotNull(foo)

        // foo should be connected to an inferred non-method function
        val func = foo.invokes.firstOrNull()
        assertNotNull(func)
        assertEquals("foo", func.fullName.localName)
        assertFalse(func is MethodDeclaration)
        assertTrue(func.isInferred)

        val cFoo = main.bodyOrNull<CallExpression>(3)
        assertNotNull(cFoo)

        // c.foo should connect to C::foo
        // and C as well as C:foo should be inferred
        val method = cFoo.invokes.firstOrNull() as? MethodDeclaration
        assertNotNull(method)
        assertEquals("foo", method.fullName.localName)
        assertTrue(method.isInferred)

        val c = method.recordDeclaration
        assertNotNull(c)
        assertEquals("C", c.fullName.localName)
        assertTrue(c.isInferred)
    }

    @Test
    fun testMethodResolve2() {
        val file = File("src/test/resources/foo2.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                // we want to infer records (namely class C in our file)
                it.inferenceConfiguration(
                    InferenceConfiguration.builder().inferRecords(true).build()
                )
            }
        assertNotNull(tu)

        val main = tu.byNameOrNull<FunctionDeclaration>("main")
        assertNotNull(main)

        val foo = main.bodyOrNull<CallExpression>(0)
        assertNotNull(foo)

        var func = foo.invokes.firstOrNull()
        assertNotNull(func)
        assertFalse(func.isInferred)
        assertFalse(func is MethodDeclaration)

        val cFoo = main.bodyOrNull<MemberCallExpression>(0)
        assertNotNull(cFoo)

        func = cFoo.invokes.firstOrNull()
        assertNotNull(func)
        assertTrue(func.isInferred)
        assertTrue(func is MethodDeclaration)
    }
}
