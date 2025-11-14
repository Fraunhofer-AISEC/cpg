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
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.ast.declarations.MethodDeclaration
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.ConstructExpression
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.test.*
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
                it.registerLanguage<CPPLanguage>()
            }
        assertNotNull(tu)

        val main = tu.functions["main"]
        assertNotNull(main)

        val realCalls = main.calls.filter { it !is ConstructExpression }

        // 0, 1 and 2 are construct expressions -> our "real" calls start at index 3
        val aFoo = realCalls.getOrNull(0)
        assertIs<MemberCallExpression>(aFoo)
        assertLocalName("foo", aFoo)
        assertLocalName("a", aFoo.base)
        // a.foo should connect to A::foo
        assertLocalName("A", (aFoo.invokes.firstOrNull() as? MethodDeclaration)?.recordDeclaration)

        val bFoo = realCalls.getOrNull(1)
        assertIs<MemberCallExpression>(bFoo)
        assertLocalName("foo", bFoo)
        assertLocalName("b", bFoo.base)
        // b.foo should connect to B::foo
        assertLocalName("B", (bFoo.invokes.firstOrNull() as? MethodDeclaration)?.recordDeclaration)

        val foo = realCalls.getOrNull(2)
        assertNotNull(foo)

        // foo should be connected to an inferred non-method function
        val func = foo.invokes.firstOrNull()
        assertNotNull(func)
        assertLocalName("foo", func)
        assertFalse(func is MethodDeclaration)
        assertTrue(func.isInferred)

        val cFoo = main.calls.getOrNull(6)
        assertNotNull(cFoo)

        // c.foo should connect to C::foo
        // and C as well as C:foo should be inferred
        val method = cFoo.invokes.firstOrNull() as? MethodDeclaration
        assertNotNull(method)
        assertLocalName("foo", method)
        assertTrue(method.isInferred)

        val c = method.recordDeclaration
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
                it.registerLanguage<CPPLanguage>()
            }
        assertNotNull(tu)

        val main = tu.functions["main"]
        assertNotNull(main)

        val foo = main.bodyOrNull<CallExpression>(0)
        assertNotNull(foo)

        var func = foo.invokes.firstOrNull()
        assertNotNull(func)
        assertFalse(func.isInferred)
        assertFalse(func is MethodDeclaration)

        val cFoo = main.bodyOrNull<MemberCallExpression>(2)
        assertNotNull(cFoo)

        func = cFoo.invokes.firstOrNull()
        assertNotNull(func)
        assertTrue(func.isInferred)
        assertTrue(func is MethodDeclaration)
    }
}
