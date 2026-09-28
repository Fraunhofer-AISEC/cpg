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
import de.fraunhofer.aisec.cpg.graph.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.expressions.Block
import de.fraunhofer.aisec.cpg.graph.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.expressions.MemberAccess
import de.fraunhofer.aisec.cpg.graph.expressions.MemberCall
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.expressions.Return
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class InterpolatedStringTest : BaseTest() {

    @Test
    fun testInterpolation() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("InterpolatedString.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val foo = tu.records["Foo"]
        assertNotNull(foo)
        val method = foo.methods["greet"]
        assertNotNull(method)
        val param = method.parameters["name"]
        assertNotNull(param)
        val body = method.body
        assertIs<Block>(body)

        // return $"Hello {name}!";
        val ret = body.statements.single()
        assertIs<Return>(ret)

        // ("Hello " + name) + "!"
        val outer = ret.returnValue
        assertIs<BinaryOperator>(outer)
        assertEquals("+", outer.operatorCode)
        val exclamation = outer.rhs
        assertIs<Literal<*>>(exclamation)
        assertEquals("!", exclamation.value)

        val inner = outer.lhs
        assertIs<BinaryOperator>(inner)
        assertEquals("+", inner.operatorCode)
        val hello = inner.lhs
        assertIs<Literal<*>>(hello)
        assertEquals("Hello ", hello.value)
        val nameRef = inner.rhs
        assertIs<Reference>(nameRef)
        assertRefersTo(nameRef, param)
    }

    @Test
    fun testConstantText() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("InterpolatedString.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val foo = tu.records["Foo"]
        assertNotNull(foo)
        val method = foo.methods["constant"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return $"Hello"; no interpolations, so there is nothing to concatenate
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val literal = ret.returnValue
        assertIs<Literal<*>>(literal)
        assertEquals("Hello", literal.value)
    }

    @Test
    fun testFormatClause() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("InterpolatedString.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val foo = tu.records["Foo"]
        assertNotNull(foo)
        val method = foo.methods["formatted"]
        assertNotNull(method)
        val value = method.parameters["value"]
        assertNotNull(value)
        val body = method.body
        assertIs<Block>(body)

        // return $"Value: {value:F2}";
        val ret = body.statements.single()
        assertIs<Return>(ret)

        // "Value: " + value.ToString("F2")
        val outer = ret.returnValue
        assertIs<BinaryOperator>(outer)
        assertEquals("+", outer.operatorCode)
        val prefix = outer.lhs
        assertIs<Literal<*>>(prefix)
        assertEquals("Value: ", prefix.value)

        val call = outer.rhs
        assertIs<MemberCall>(call)
        assertLocalName("ToString", call)
        val base = (call.callee as MemberAccess).base
        assertIs<Reference>(base)
        assertRefersTo(base, value)
        val arg = call.arguments.single()
        assertIs<Literal<*>>(arg)
        assertEquals("F2", arg.value)
    }
}
