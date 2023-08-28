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
package de.fraunhofer.aisec.cpg.frontends.cxx

import de.fraunhofer.aisec.cpg.InferenceConfiguration
import de.fraunhofer.aisec.cpg.TestUtils.analyzeAndGetFirstTU
import de.fraunhofer.aisec.cpg.assertLocalName
import de.fraunhofer.aisec.cpg.graph.bodyOrNull
import de.fraunhofer.aisec.cpg.graph.byNameOrNull
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDecl
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDecl
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpr
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpr
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

        val main = tu.byNameOrNull<FunctionDecl>("main")
        assertNotNull(main)

        val aFoo = main.bodyOrNull<MemberCallExpr>(0)
        assertNotNull(aFoo)
        assertLocalName("foo", aFoo)
        assertLocalName("a", aFoo.base)
        // a.foo should connect to A::foo
        assertLocalName("A", (aFoo.invokes.firstOrNull() as? MethodDecl)?.recordDecl)

        val bFoo = main.bodyOrNull<MemberCallExpr>(1)
        assertNotNull(bFoo)
        assertLocalName("foo", bFoo)
        assertLocalName("b", bFoo.base)
        // b.foo should connect to B::foo
        assertLocalName("B", (bFoo.invokes.firstOrNull() as? MethodDecl)?.recordDecl)

        val foo = main.bodyOrNull<CallExpr>(2)
        assertNotNull(foo)

        // foo should be connected to an inferred non-method function
        val func = foo.invokes.firstOrNull()
        assertNotNull(func)
        assertLocalName("foo", func)
        assertFalse(func is MethodDecl)
        assertTrue(func.isInferred)

        val cFoo = main.bodyOrNull<CallExpr>(3)
        assertNotNull(cFoo)

        // c.foo should connect to C::foo
        // and C as well as C:foo should be inferred
        val method = cFoo.invokes.firstOrNull() as? MethodDecl
        assertNotNull(method)
        assertLocalName("foo", method)
        assertTrue(method.isInferred)

        val c = method.recordDecl
        assertNotNull(c)
        assertLocalName("C", c)
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

        val main = tu.byNameOrNull<FunctionDecl>("main")
        assertNotNull(main)

        val foo = main.bodyOrNull<CallExpr>(0)
        assertNotNull(foo)

        var func = foo.invokes.firstOrNull()
        assertNotNull(func)
        assertFalse(func.isInferred)
        assertFalse(func is MethodDecl)

        val cFoo = main.bodyOrNull<MemberCallExpr>(0)
        assertNotNull(cFoo)

        func = cFoo.invokes.firstOrNull()
        assertNotNull(func)
        assertTrue(func.isInferred)
        assertTrue(func is MethodDecl)
    }
}
