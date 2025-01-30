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

import de.fraunhofer.aisec.cpg.*
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.IfStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.test.*
import kotlin.test.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShortcutsTest {
    @Test
    fun followDFGUntilHitTest() {
        val result = GraphExamples.getDataflowClass()

        val toStringCall = result.callsByName("toString")[0]
        val printDecl =
            result.components
                .flatMap { it.translationUnits }
                .first()
                .records["Dataflow"]
                .methods["print"]

        val (fulfilled, failed) =
            toStringCall.followNextFullDFGEdgesUntilHit { it == printDecl?.parameters?.first() }

        assertEquals(1, fulfilled.size)
        assertEquals(0, failed.size)
    }

    @Test
    fun testCalls() {
        val actual = shortcutClassResult.calls

        val expected = mutableListOf<CallExpression>()
        val classDecl = shortcutClassResult.records["ShortcutClass"]
        assertNotNull(classDecl)

        val main = classDecl.methods["main"]
        assertNotNull(main)
        val mainBody = main.body
        assertIs<Block>(mainBody)
        val declarationStatement = mainBody.statements[0]
        assertIs<DeclarationStatement>(declarationStatement)
        val variable = declarationStatement.declarations[0]
        assertIs<VariableDeclaration>(variable)
        val newExpr = variable.initializer
        assertIs<NewExpression>(newExpr)
        val constructExpr = newExpr.initializer
        assertIs<ConstructExpression>(constructExpr)
        expected.add(constructExpr)
        val memberCall1 = mainBody.statements[1]
        assertIs<MemberCallExpression>(memberCall1)
        expected.add(memberCall1)
        val memberCall2 = mainBody.statements[2]
        assertIs<MemberCallExpression>(memberCall2)
        expected.add(memberCall2)
        val memberCall3 = mainBody.statements[3]
        assertIs<MemberCallExpression>(memberCall3)
        expected.add(memberCall3)

        val print = classDecl.methods["print"]
        assertNotNull(print)
        val printBody0 = print.bodyOrNull<CallExpression>(0)
        assertNotNull(printBody0)
        expected.add(printBody0)
        val printArg = printBody0.arguments[0]
        assertIs<CallExpression>(printArg)
        expected.add(printArg)

        assertTrue(expected.containsAll(actual))
        assertTrue(actual.containsAll(expected))

        val body1Stmt = mainBody.statements[1]
        assertIs<MemberCallExpression>(body1Stmt)
        assertEquals(listOf(body1Stmt), expected("print"))
    }

    @Test
    fun testCallsByName() {
        val result = GraphExamples.getShortcutClass()

        val actual = result.callsByName("print")

        val expected = mutableListOf<CallExpression>()
        val classDecl = result.records["ShortcutClass"]
        assertNotNull(classDecl)

        val main = classDecl.methods["main"]
        assertNotNull(main)
        val mainBody = main.body
        assertIs<Block>(mainBody)
        val stmt1 = mainBody.statements[1]
        assertIs<MemberCallExpression>(stmt1)
        expected.add(stmt1)
        assertTrue(expected.containsAll(actual))
        assertTrue(actual.containsAll(expected))
    }

    @Test
    fun testCalleesOf() {
        val expected = mutableListOf<FunctionDeclaration>()
        val classDecl = shortcutClassResult.records["ShortcutClass"]
        assertNotNull(classDecl)

        val print = classDecl.methods["print"]
        assertNotNull(print)
        expected.add(print)

        val magic = classDecl.methods["magic"]
        assertNotNull(magic)
        expected.add(magic)

        val magic2 = classDecl.methods["magic2"]
        assertNotNull(magic2)
        expected.add(magic2)

        val main = classDecl.methods["main"]
        assertNotNull(main)
        val actual = main.callees

        expected.add(
            (((((main.body as Block).statements[0] as DeclarationStatement).declarations[0]
                            as VariableDeclaration)
                        .initializer as NewExpression)
                    .initializer as ConstructExpression)
                .constructor!!
        )

        assertTrue(expected.containsAll(actual))
        assertTrue(actual.containsAll(expected))
    }

    @Test
    fun testCallersOf() {
        val classDecl = shortcutClassResult.records["ShortcutClass"]
        assertNotNull(classDecl)
        val print = classDecl.methods["print"]
        assertNotNull(print)

        val expected = mutableListOf<FunctionDeclaration>()
        val main = classDecl.functions["main"]
        assertNotNull(main)

        val scRefs = main.refs("sc")
        scRefs.forEach {
            assertNotNull(it)
            assertLocalName("ShortcutClass", it.type)
        }

        val printCall = main.calls["print"]
        assertFullName("ShortcutClass.print", printCall)
        expected.add(main)

        val actual = shortcutClassResult.callersOf(print)

        assertTrue(expected.containsAll(actual))
        assertTrue(actual.containsAll(expected))
    }

    @Test
    fun testControls() {
        val expected = mutableListOf<Node>()
        val classDecl = shortcutClassResult.records["ShortcutClass"]
        assertNotNull(classDecl)

        val magic = classDecl.methods["magic"]
        assertNotNull(magic)

        val ifStatement = (magic.body as Block).statements[0] as IfStatement

        val actual = ifStatement.controls()
        ifStatement.thenStatement?.let { expected.add(it) }
        val thenStatement = (ifStatement.thenStatement as Block).statements[0] as IfStatement
        expected.add(thenStatement)
        thenStatement.condition?.let { expected.add(it) }
        expected.add((thenStatement.condition as BinaryOperator).lhs)
        expected.add(((thenStatement.condition as BinaryOperator).lhs as MemberExpression).base)
        expected.add((thenStatement.condition as BinaryOperator).rhs)
        val nestedThen = thenStatement.thenStatement as Block
        expected.add(nestedThen)
        expected.add(nestedThen.statements[0])
        expected.add((nestedThen.statements[0] as AssignExpression).lhs.first())
        expected.add(
            ((nestedThen.statements[0] as AssignExpression).lhs.first() as MemberExpression).base
        )
        expected.add((nestedThen.statements[0] as AssignExpression).rhs.first())
        val nestedElse = thenStatement.elseStatement as Block
        expected.add(nestedElse)
        expected.add(nestedElse.statements[0])
        expected.add((nestedElse.statements[0] as AssignExpression).lhs.first())
        expected.add(
            ((nestedElse.statements[0] as AssignExpression).lhs.first() as MemberExpression).base
        )
        expected.add((nestedElse.statements[0] as AssignExpression).rhs.first())

        ifStatement.elseStatement?.let { expected.add(it) }
        expected.add((ifStatement.elseStatement as Block).statements[0])
        expected.add(
            ((ifStatement.elseStatement as Block).statements[0] as AssignExpression).lhs.first()
        )
        expected.add(
            (((ifStatement.elseStatement as Block).statements[0] as AssignExpression).lhs.first()
                    as MemberExpression)
                .base
        )
        expected.add(
            ((ifStatement.elseStatement as Block).statements[0] as AssignExpression).rhs.first()
        )

        assertTrue(expected.containsAll(actual))
        assertTrue(actual.containsAll(expected))
    }

    @Test
    fun testControlledBy() {
        val result =
            GraphExamples.getShortcutClass(
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage(TestLanguage("."))
                    .build()
            )

        val expected = mutableListOf<Node>()
        val classDecl = result.records["ShortcutClass"]
        assertNotNull(classDecl)

        val magic = classDecl.methods["magic"]
        assertNotNull(magic)

        // get the statement attr = 3;
        val ifStatement = (magic.body as Block).statements[0] as IfStatement
        val thenStatement = (ifStatement.thenStatement as Block).statements[0] as IfStatement
        val nestedThen = thenStatement.thenStatement as Block
        val interestingNode = nestedThen.statements[0]
        val actual = interestingNode.controlledBy()

        expected.add(ifStatement)
        expected.add(thenStatement)

        assertTrue(expected.containsAll(actual))
        assertTrue(actual.containsAll(expected))
    }

    @Test
    fun testFollowPrevDFGEdgesUntilHit() {
        val classDecl = shortcutClassResult.records["ShortcutClass"]
        assertNotNull(classDecl)

        val magic2 = classDecl.methods["magic2"]
        assertNotNull(magic2)

        val aAssignment2 =
            ((((magic2.body as Block).statements[1] as IfStatement).elseStatement as Block)
                    .statements[0]
                    as AssignExpression)
                .lhs
                .first()

        val paramPassed2 = aAssignment2.followPrevFullDFGEdgesUntilHit { it is Literal<*> }
        assertEquals(1, paramPassed2.fulfilled.size)
        assertEquals(0, paramPassed2.failed.size)
        assertEquals(5, (paramPassed2.fulfilled[0].last() as? Literal<*>)?.value)

        val magic = classDecl.methods["magic"]
        assertNotNull(magic)

        val attrAssignment =
            ((((magic.body as Block).statements[0] as IfStatement).elseStatement as Block)
                    .statements[0]
                    as AssignExpression)
                .lhs
                .first()

        val paramPassed = attrAssignment.followPrevFullDFGEdgesUntilHit { it is Literal<*> }
        assertEquals(1, paramPassed.fulfilled.size)
        assertEquals(0, paramPassed.failed.size)
        assertEquals(3, (paramPassed.fulfilled[0].last() as? Literal<*>)?.value)
    }

    @Test
    fun testFollowPrevEOGEdgesUntilHit() {
        val classDecl = shortcutClassResult.records["ShortcutClass"]
        assertNotNull(classDecl)

        val magic = classDecl.methods["magic"]
        assertNotNull(magic)

        val attrAssignment =
            ((((magic.body as Block).statements[0] as IfStatement).elseStatement as Block)
                    .statements[0]
                    as AssignExpression)
                .lhs
                .first()

        val paramPassed = attrAssignment.followPrevEOGEdgesUntilHit { it is Literal<*> }
        assertEquals(1, paramPassed.fulfilled.size)
        assertEquals(0, paramPassed.failed.size)
        assertEquals(
            5,
            (paramPassed.fulfilled[0].last() as? Literal<*>)?.value,
        ) // It's the comparison
    }

    @Test
    fun testFollowNextEOGEdgesUntilHit() {
        val classDecl = shortcutClassResult.records["ShortcutClass"]
        assertNotNull(classDecl)

        val magic = classDecl.methods["magic"]
        assertNotNull(magic)

        val ifCondition =
            ((magic.body as Block).statements[0] as IfStatement).condition as BinaryOperator

        val paramPassed =
            ifCondition.followNextEOGEdgesUntilHit {
                it is AssignExpression &&
                    it.operatorCode == "=" &&
                    (it.rhs.first() as? Reference)?.refersTo ==
                        (ifCondition.lhs as Reference).refersTo
            }
        assertEquals(1, paramPassed.fulfilled.size)
        assertEquals(2, paramPassed.failed.size)
    }

    @Test
    fun testFollowPrevDFGEdges() {
        val classDecl = shortcutClassResult.records["ShortcutClass"]
        assertNotNull(classDecl)

        val magic = classDecl.methods["magic"]
        assertNotNull(magic)

        val attrAssignment =
            ((((magic.body as Block).statements[0] as IfStatement).elseStatement as Block)
                    .statements[0]
                    as AssignExpression)
                .lhs
                .first()

        val paramPassed = attrAssignment.followPrevFullDFG { it is Literal<*> }
        assertNotNull(paramPassed)
        assertEquals(3, (paramPassed.last() as? Literal<*>)?.value)
    }

    @Test
    fun testUnwrapReference() {
        with(TestLanguageFrontend()) {
            val a = newReference("a")
            val op = newUnaryOperator("&", prefix = true, postfix = false)
            op.input = a
            val cast = newCastExpression()
            cast.castType = objectType("int64")
            cast.expression = op

            assertEquals(a, cast.unwrapReference())
        }
    }

    private lateinit var shortcutClassResult: TranslationResult

    @BeforeAll
    fun getShortcutClass() {
        shortcutClassResult = GraphExamples.getShortcutClass()
    }
}
