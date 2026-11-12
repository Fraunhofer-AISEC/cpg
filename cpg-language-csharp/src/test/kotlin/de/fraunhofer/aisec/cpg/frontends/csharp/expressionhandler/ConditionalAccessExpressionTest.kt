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
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.expressions.*
import de.fraunhofer.aisec.cpg.test.*
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/**
 * `a?.b` has no direct representation in the graph, so it is destructured into the conditional
 * expression the C# spec defines it as: a [DeclarationStatement] for a temporary holding `a`,
 * followed by a [Conditional] `tmp == null ? null : tmp.b`, both wrapped in an implicit
 * [ExpressionList]. See
 * [de.fraunhofer.aisec.cpg.frontends.csharp.ExpressionHandler.handleConditionalAccessExpression].
 */
class ConditionalAccessExpressionTest : BaseTest() {

    @Test
    fun testMemberAccess() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("ConditionalAccess.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val foo = tu.records["Foo"]
        assertNotNull(foo)
        val method = foo.methods["memberAccess"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return bar?.b;
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val exprList = ret.returnValue
        assertIs<ExpressionList>(exprList)

        // var tmp = bar;
        val declStmt = exprList.expressions[0]
        assertIs<DeclarationStatement>(declStmt)
        val tmpVar = declStmt.singleDeclaration
        assertIs<Variable>(tmpVar)

        // tmp == null ? null : tmp.b
        val conditional = exprList.expressions[1]
        assertIs<Conditional>(conditional)

        val condition = conditional.condition
        assertIs<BinaryOperator>(condition)
        assertEquals("==", condition.operatorCode)
        val conditionLhs = condition.lhs
        assertIs<Reference>(conditionLhs)
        assertRefersTo(conditionLhs, tmpVar)

        val memberAccess = conditional.elseExpression
        assertIs<MemberAccess>(memberAccess)
        assertLocalName("b", memberAccess)
        val base = memberAccess.base
        assertIs<Reference>(base)
        assertRefersTo(base, tmpVar)
    }

    @Test
    fun testInvocation() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("ConditionalAccess.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val foo = tu.records["Foo"]
        assertNotNull(foo)
        val method = foo.methods["invocation"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return bar?.ToString();
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val exprList = ret.returnValue
        assertIs<ExpressionList>(exprList)

        val declStmt = exprList.expressions[0]
        assertIs<DeclarationStatement>(declStmt)
        val tmpVar = declStmt.singleDeclaration
        assertIs<Variable>(tmpVar)

        val conditional = exprList.expressions[1]
        assertIs<Conditional>(conditional)

        // tmp.ToString()
        val call = conditional.elseExpression
        assertIs<MemberCall>(call)
        assertLocalName("ToString", call)
        val base = (call.callee as MemberAccess).base
        assertIs<Reference>(base)
        assertRefersTo(base, tmpVar)
    }

    @Test
    fun testElementAccess() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("ConditionalAccess.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val foo = tu.records["Foo"]
        assertNotNull(foo)
        val method = foo.methods["elementAccess"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return items?[0];
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val exprList = ret.returnValue
        assertIs<ExpressionList>(exprList)

        val declStmt = exprList.expressions[0]
        assertIs<DeclarationStatement>(declStmt)
        val tmpVar = declStmt.singleDeclaration
        assertIs<Variable>(tmpVar)

        val conditional = exprList.expressions[1]
        assertIs<Conditional>(conditional)

        // tmp[0]
        val subscription = conditional.elseExpression
        assertIs<Subscription>(subscription)
        val base = subscription.arrayExpression
        assertIs<Reference>(base)
        assertRefersTo(base, tmpVar)
        val index = subscription.subscriptExpression
        assertIs<Literal<*>>(index)
        assertEquals(0, index.value)
    }

    @Test
    fun testChainedMemberAccess() {
        val topLevel = Path.of("src", "test", "resources", "csharp")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("ConditionalAccess.cs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<CSharpLanguage>()
            }
        assertNotNull(tu)

        val foo = tu.records["Foo"]
        assertNotNull(foo)
        val method = foo.methods["chainedMemberAccess"]
        assertNotNull(method)
        val body = method.body
        assertIs<Block>(body)

        // return bar?.nested.c;
        val ret = body.statements.single()
        assertIs<Return>(ret)
        val exprList = ret.returnValue
        assertIs<ExpressionList>(exprList)

        val declStmt = exprList.expressions[0]
        assertIs<DeclarationStatement>(declStmt)
        val tmpVar = declStmt.singleDeclaration
        assertIs<Variable>(tmpVar)

        val conditional = exprList.expressions[1]
        assertIs<Conditional>(conditional)

        // tmp.nested.c
        val cAccess = conditional.elseExpression
        assertIs<MemberAccess>(cAccess)
        assertLocalName("c", cAccess)

        val nestedAccess = cAccess.base
        assertIs<MemberAccess>(nestedAccess)
        assertLocalName("nested", nestedAccess)
        val base = nestedAccess.base
        assertIs<Reference>(base)
        assertRefersTo(base, tmpVar)
    }
}
