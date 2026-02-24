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

        val mainBody = main.body
        assertIs<Block>(mainBody)
        val stmt0 = mainBody.statements[0]
        assertIs<DeclarationStatement>(stmt0)
        val variable = stmt0.declarations[0]
        assertIs<VariableDeclaration>(variable)
        val newExpr = variable.initializer
        assertIs<NewExpression>(newExpr)
        val constructExpr = newExpr.initializer
        assertIs<ConstructExpression>(constructExpr)
        val constructor = constructExpr.constructor
        assertNotNull(constructor)
        expected.add(constructor)

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

        val magicBody = magic.body
        assertIs<Block>(magicBody)
        val ifStatement = magicBody.statements[0]
        assertIs<IfStatement>(ifStatement)

        val actual = ifStatement.controls()
        val thenStmt = ifStatement.thenStatement
        assertIs<Block>(thenStmt)
        expected.add(thenStmt)
        val innerIfStmt = thenStmt.statements[0]
        assertIs<IfStatement>(innerIfStmt)
        expected.add(innerIfStmt)
        val condition = innerIfStmt.condition
        assertIs<BinaryOperator>(condition)
        expected.add(condition)
        val conditionLhs = condition.lhs
        assertIs<MemberExpression>(conditionLhs)
        expected.add(conditionLhs)
        expected.add(conditionLhs.base)
        expected.add(condition.rhs)
        val nestedThen = innerIfStmt.thenStatement
        assertIs<Block>(nestedThen)
        expected.add(nestedThen)
        val nestedThenStmt0 = nestedThen.statements[0]
        assertIs<AssignExpression>(nestedThenStmt0)
        expected.add(nestedThenStmt0)
        val nestedThenStmt0Lhs = nestedThenStmt0.lhs.singleOrNull()
        assertIs<MemberExpression>(nestedThenStmt0Lhs)
        expected.add(nestedThenStmt0Lhs)
        expected.add(nestedThenStmt0Lhs.base)

        val nestedThenStmt0Rhs = nestedThenStmt0.rhs.singleOrNull()
        assertNotNull(nestedThenStmt0Rhs)
        expected.add(nestedThenStmt0Rhs)
        val nestedElse = innerIfStmt.elseStatement
        assertIs<Block>(nestedElse)
        expected.add(nestedElse)
        val nestedElseStmt0 = nestedElse.statements[0]
        assertIs<AssignExpression>(nestedElseStmt0)
        expected.add(nestedElseStmt0)
        val nestedElseStmt0Lhs = nestedElseStmt0.lhs.singleOrNull()
        assertIs<MemberExpression>(nestedElseStmt0Lhs)
        expected.add(nestedElseStmt0Lhs)
        expected.add(nestedElseStmt0Lhs.base)
        val nestedElseStmt0Rhs = nestedElseStmt0.rhs.singleOrNull()
        assertNotNull(nestedElseStmt0Rhs)
        expected.add(nestedElseStmt0Rhs)

        val outerElse = ifStatement.elseStatement
        assertIs<Block>(outerElse)
        expected.add(outerElse)
        val outerElseStmt0 = outerElse.statements[0]
        assertIs<AssignExpression>(outerElseStmt0)
        expected.add(outerElseStmt0)
        val outerElseStmt0Lhs = outerElseStmt0.lhs.singleOrNull()
        assertIs<MemberExpression>(outerElseStmt0Lhs)
        expected.add(outerElseStmt0Lhs)
        expected.add(outerElseStmt0Lhs.base)
        val outerElseStmt0Rhs = outerElseStmt0.rhs.singleOrNull()
        assertNotNull(outerElseStmt0Rhs)
        expected.add(outerElseStmt0Rhs)

        assertTrue(expected.containsAll(actual))
        assertTrue(actual.containsAll(expected))
    }

    @Test
    fun testControlledBy() {
        val result =
            GraphExamples.getShortcutClass(
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
            )

        val expected = mutableListOf<Node>()
        val classDecl = result.records["ShortcutClass"]
        assertNotNull(classDecl)

        val magic = classDecl.methods["magic"]
        assertNotNull(magic)

        // get the statement attr = 3;
        val magicBody = magic.body
        assertIs<Block>(magicBody)
        val ifStatement = magicBody.statements[0]
        assertIs<IfStatement>(ifStatement)
        val thenStmt = ifStatement.thenStatement
        assertIs<Block>(thenStmt)
        val thenStatement0 = thenStmt.statements[0]
        assertIs<IfStatement>(thenStatement0)
        val nestedThen = thenStatement0.thenStatement
        assertIs<Block>(nestedThen)
        val interestingNode = nestedThen.statements[0]
        val actual = interestingNode.controlledBy()

        expected.add(ifStatement)
        expected.add(thenStatement0)

        assertTrue(expected.containsAll(actual))
        assertTrue(actual.containsAll(expected))
    }

    @Test
    fun testFollowPrevDFGEdgesUntilHit() {
        val classDecl = shortcutClassResult.records["ShortcutClass"]
        assertNotNull(classDecl)

        val magic2 = classDecl.methods["magic2"]
        assertNotNull(magic2)

        val magic2Body = magic2.body
        assertIs<Block>(magic2Body)
        val ifStatement2 = magic2Body.statements[1]
        assertIs<IfStatement>(ifStatement2)
        val elseStmt2 = ifStatement2.elseStatement
        assertIs<Block>(elseStmt2)
        val assignExpr2 = elseStmt2.statements[0]
        assertIs<AssignExpression>(assignExpr2)
        val aAssignment2 = assignExpr2.lhs.first()

        val paramPassed2 = aAssignment2.followPrevFullDFGEdgesUntilHit { it is Literal<*> }
        assertEquals(1, paramPassed2.fulfilled.size)
        assertEquals(0, paramPassed2.failed.size)

        val lastFulfilled2 = paramPassed2.fulfilled[0].nodes.last()
        assertIs<Literal<*>>(lastFulfilled2)
        assertLiteralValue(5, lastFulfilled2)

        val magic = classDecl.methods["magic"]
        assertNotNull(magic)

        val magicBody = magic.body
        assertIs<Block>(magicBody)
        val ifStatement = magicBody.statements[0]
        assertIs<IfStatement>(ifStatement)
        val elseStmt = ifStatement.elseStatement
        assertIs<Block>(elseStmt)
        val assignExpr = elseStmt.statements[0]
        assertIs<AssignExpression>(assignExpr)
        val attrAssignment = assignExpr.lhs.first()

        val paramPassed = attrAssignment.followPrevFullDFGEdgesUntilHit { it is Literal<*> }
        assertEquals(1, paramPassed.fulfilled.size)
        assertEquals(0, paramPassed.failed.size)

        val lastFulfilled = paramPassed.fulfilled[0].nodes.last()
        assertIs<Literal<*>>(lastFulfilled)
        assertLiteralValue(3, lastFulfilled)
    }

    @Test
    fun testFollowPrevEOGEdgesUntilHit() {
        val classDecl = shortcutClassResult.records["ShortcutClass"]
        assertNotNull(classDecl)

        val magic = classDecl.methods["magic"]
        assertNotNull(magic)

        val magicBody = magic.body
        assertIs<Block>(magicBody)
        val ifStatement = magicBody.statements[0]
        assertIs<IfStatement>(ifStatement)
        val elseStmt = ifStatement.elseStatement
        assertIs<Block>(elseStmt)
        val assignExpr = elseStmt.statements[0]
        assertIs<AssignExpression>(assignExpr)

        val attrAssignment = assignExpr.lhs.first()

        val paramPassed =
            attrAssignment.followEOGEdgesUntilHit(direction = Backward(GraphToFollow.EOG)) {
                it is Literal<*>
            }
        assertEquals(1, paramPassed.fulfilled.size)
        assertEquals(0, paramPassed.failed.size)
        val lastFulfilled = paramPassed.fulfilled[0].nodes.last()
        assertIs<Literal<*>>(lastFulfilled)
        assertLiteralValue(5, lastFulfilled) // It's the comparison
    }

    @Test
    fun testFollowNextEOGEdgesUntilHit() {
        val classDecl = shortcutClassResult.records["ShortcutClass"]
        assertNotNull(classDecl)

        val magic = classDecl.methods["magic"]
        assertNotNull(magic)

        val magicBody = magic.body
        assertIs<Block>(magicBody)
        val ifStatement = magicBody.statements[0]
        assertIs<IfStatement>(ifStatement)
        val ifCondition = ifStatement.condition
        assertIs<BinaryOperator>(ifCondition)

        // There are the following paths:
        // - the else branch (which fulfills the requirement)
        // - the then/then (fails)
        // - the then/else (fails)
        val paramPassedIntraproceduralOnly =
            ifCondition.followEOGEdgesUntilHit(
                direction = Forward(GraphToFollow.EOG),
                scope = Intraprocedural(),
            ) {
                it is AssignExpression &&
                    it.operatorCode == "=" &&
                    (it.rhs.first() as? Reference)?.refersTo ==
                        (ifCondition.lhs as? Reference)?.refersTo
            }
        assertEquals(1, paramPassedIntraproceduralOnly.fulfilled.size)
        assertEquals(2, paramPassedIntraproceduralOnly.failed.size)

        // There are the following paths:
        // - the else branch (which fulfills the requirement)
        // - the then/then and 3 paths when we enter magic2 through this path (=> 3 fails)
        // - the then/else and 3 paths when we enter magic2 through this path (=> 3 fails)
        val paramPassedInterprocedural =
            ifCondition.followEOGEdgesUntilHit(
                direction = Forward(GraphToFollow.EOG),
                scope = Interprocedural(),
            ) {
                it is AssignExpression &&
                    it.operatorCode == "=" &&
                    (it.rhs.first() as? Reference)?.refersTo ==
                        (ifCondition.lhs as Reference).refersTo
            }
        assertEquals(1, paramPassedInterprocedural.fulfilled.size)
        assertEquals(6, paramPassedInterprocedural.failed.size)
    }

    @Test
    fun testFollowPrevDFGEdges() {
        val classDecl = shortcutClassResult.records["ShortcutClass"]
        assertNotNull(classDecl)

        val magic = classDecl.methods["magic"]
        assertNotNull(magic)

        val magicBody = magic.body
        assertIs<Block>(magicBody)
        val ifStmt0 = magicBody.statements[0]
        assertIs<IfStatement>(ifStmt0)
        val elseStmt = ifStmt0.elseStatement
        assertIs<Block>(elseStmt)
        val assignExpr = elseStmt.statements[0]
        assertIs<AssignExpression>(assignExpr)
        val attrAssignment = assignExpr.lhs.first()

        val paramPassed = attrAssignment.followPrevFullDFG { it is Literal<*> }
        assertNotNull(paramPassed)
        assertEquals(3, (paramPassed.nodes.last() as? Literal<*>)?.value)
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
