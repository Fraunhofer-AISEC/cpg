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
package de.fraunhofer.aisec.cpg.frontends.csharp.expressionhandler

import de.fraunhofer.aisec.cpg.frontends.csharp.CSharpLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Method
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.expressions.Block
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.expressions.MemberAccess
import de.fraunhofer.aisec.cpg.graph.expressions.MemberCall
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.expressions.Return
import de.fraunhofer.aisec.cpg.graph.types.FunctionType
import de.fraunhofer.aisec.cpg.graph.types.IntegerType
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CallTest : BaseTest() {

    @Test
    fun testMemberCall() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("Calls.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val bar = tu.records["Bar"]
        assertNotNull(bar)

        val addMethod = tu.methods["Add"]
        assertNotNull(addMethod)

        val memberCallmethod = bar.methods["memberCall"]
        assertNotNull(memberCallmethod)
        val body = memberCallmethod.body
        assertIs<Block>(body)

        // this.Add(1, 2);
        val memberCall = body.statements.firstOrNull()
        assertNotNull(memberCall)
        assertIs<MemberCall>(memberCall)
        val invokesEdge = memberCall.invokes.firstOrNull()
        assertNotNull(invokesEdge)
        assertEquals(addMethod, invokesEdge)

        val callee = memberCall.callee
        assertIs<MemberAccess>(callee)

        val base = callee.base
        assertIs<Reference>(base)
        assertIs<Variable>(base.refersTo)
        assertEquals(base.type.name, bar.name)

        val memberCallArgs = memberCall.arguments
        assertEquals(2, memberCallArgs.size)
        val arg0 = memberCallArgs.firstOrNull()
        assertNotNull(arg0)
        assertIs<Literal<*>>(arg0)
        assertIs<IntegerType>(arg0.type)
        assertEquals(1, arg0.value)
        val arg1 = memberCallArgs.getOrNull(1)
        assertNotNull(arg1)
        assertIs<Literal<*>>(arg1)
        assertIs<IntegerType>(arg0.type)
        assertEquals(2, arg1.value)
    }

    @Test
    fun testSimpleCall() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("Calls.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val bar = tu.records["Bar"]
        assertNotNull(bar)

        val addMethod = tu.methods["Add"]
        assertNotNull(addMethod)

        val simpleCallMethod = bar.methods["simpleCall"]
        assertNotNull(simpleCallMethod)
        val body = simpleCallMethod.body
        assertIs<Block>(body)

        // Add(3, 4);
        val call = body.statements.firstOrNull()
        assertNotNull(call)
        assertIs<Call>(call)
        assertEquals("Add", call.name.localName)
        val invokesEdge = call.invokes.firstOrNull()
        assertNotNull(invokesEdge)
        assertEquals(addMethod, invokesEdge)

        val callArgs = call.arguments
        assertEquals(2, callArgs.size)
        val arg0 = callArgs.firstOrNull()
        assertNotNull(arg0)
        assertIs<Literal<*>>(arg0)
        assertIs<IntegerType>(arg0.type)
        assertEquals(3, arg0.value)
        val arg1 = callArgs.getOrNull(1)
        assertNotNull(arg1)
        assertIs<Literal<*>>(arg1)
        assertIs<IntegerType>(arg1.type)
        assertEquals(4, arg1.value)
    }

    @Test
    fun testCallWithOmittedArgument() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("Calls.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val bar = tu.records["Bar"]
        assertNotNull(bar)

        // int Offset(int a, int b = 10)
        val offsetMethod = bar.methods["Offset"]
        assertNotNull(offsetMethod)

        val method = bar.methods["callWithOmittedArgument"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // Offset(1);
        val call = body.statements.single()
        assertIs<Call>(call)
        assertEquals(1, call.arguments.size)

        // the call passes fewer arguments than the method has parameters, which is only a match
        // because the missing one has a default value
        assertInvokes(call, offsetMethod)
    }

    @Test
    fun testNestedCall() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("Calls.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val addMethod = tu.methods["Add"]
        assertNotNull(addMethod)
        val type = addMethod.type
        assertIs<FunctionType>(type)
        assertIs<IntegerType>(type.returnTypes.singleOrNull())

        val method = tu.methods["nestedCall"]
        assertNotNull(method)

        // var sum = Add(1, 2); and Add(Add(sum, 3), 4); each call takes the return type of `Add`,
        // so a call or `var` as argument still resolves to the declared method
        val calls = method.calls("Add")
        assertEquals(3, calls.size)
        for (call in calls) {
            assertInvokes(call, addMethod)
            assertIs<IntegerType>(call.type)
        }
    }

    @Test
    fun testLocalFunctionCall() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(listOf(topLevel.resolve("Calls.cs").toFile()), topLevel, true) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val method = tu.methods["localFunctions"]
        assertNotNull(method)

        // Both local functions are declared in the body of the method and are not methods of the
        // record
        val twice = method.functions["Twice"]
        val square = method.functions["Square"]
        assertNotNull(twice)
        assertNotNull(square)
        assertTrue(twice !is Method)
        assertTrue(square !is Method)
        assertIs<IntegerType>((twice.type as? FunctionType)?.returnTypes?.singleOrNull())

        // `int Twice(int v) => v * 2;` returns its expression body
        val twiceBody = twice.body
        assertIs<Block>(twiceBody)
        val twiceReturn = twiceBody.statements.singleOrNull()
        assertIs<Return>(twiceReturn)
        val v = twiceReturn.returnValue?.refs?.singleOrNull()
        assertNotNull(v)
        assertEquals(twice.parameters.single(), v.refersTo)

        // A local function is visible in its whole block, so `Square` is resolved although it is
        // declared after the call
        assertInvokes(method.calls["Twice"], twice)
        assertInvokes(method.calls["Square"], square)
    }
}
