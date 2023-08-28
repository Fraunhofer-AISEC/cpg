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
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDecl
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDecl
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDecl
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDecl
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStmt
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStmt
import de.fraunhofer.aisec.cpg.graph.statements.IfStmt
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.passes.EdgeCachePass
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
            result.translationUnits[0]
                .byNameOrNull<RecordDecl>("Dataflow")
                ?.byNameOrNull<MethodDecl>("print")

        val (fulfilled, failed) =
            toStringCall.followNextDFGEdgesUntilHit { it == printDecl!!.parameters[0] }

        assertEquals(1, fulfilled.size)
        assertEquals(0, failed.size)
    }

    @Test
    fun testCalls() {
        val actual = shortcutClassResult.calls

        val expected = mutableListOf<CallExpr>()
        val classDecl = shortcutClassResult.records["ShortcutClass"]
        assertNotNull(classDecl)
        val main = classDecl.byNameOrNull<MethodDecl>("main")
        assertNotNull(main)
        expected.add(
            ((((main.body as CompoundStmt).statements[0] as DeclarationStmt).declarations[0]
                        as VariableDecl)
                    .initializer as NewExpr)
                .initializer as ConstructExpr
        )
        expected.add((main.body as CompoundStmt).statements[1] as MemberCallExpr)
        expected.add((main.body as CompoundStmt).statements[2] as MemberCallExpr)
        expected.add((main.body as CompoundStmt).statements[3] as MemberCallExpr)

        val print = classDecl.byNameOrNull<MethodDecl>("print")
        assertNotNull(print)
        expected.add(print.bodyOrNull(0)!!)
        expected.add(print.bodyOrNull<CallExpr>(0)?.arguments?.get(0) as CallExpr)

        assertTrue(expected.containsAll(actual))
        assertTrue(actual.containsAll(expected))

        assertEquals(
            listOf((main.body as CompoundStmt).statements[1] as MemberCallExpr),
            expected("print")
        )
    }

    @Test
    fun testCallsByName() {
        val result = GraphExamples.getShortcutClass()

        val actual = result.callsByName("print")

        val expected = mutableListOf<CallExpr>()
        val classDecl = result.records["ShortcutClass"]
        assertNotNull(classDecl)
        val main = classDecl.byNameOrNull<MethodDecl>("main")
        assertNotNull(main)
        expected.add((main.body as CompoundStmt).statements[1] as MemberCallExpr)
        assertTrue(expected.containsAll(actual))
        assertTrue(actual.containsAll(expected))
    }

    @Test
    fun testCalleesOf() {
        val expected = mutableListOf<FunctionDecl>()
        val classDecl = shortcutClassResult.records["ShortcutClass"]
        assertNotNull(classDecl)
        val print = classDecl.byNameOrNull<MethodDecl>("print")
        assertNotNull(print)
        expected.add(print)

        val magic = classDecl.byNameOrNull<MethodDecl>("magic")
        assertNotNull(magic)
        expected.add(magic)

        val magic2 = classDecl.byNameOrNull<MethodDecl>("magic2")
        assertNotNull(magic2)
        expected.add(magic2)

        val main = classDecl.byNameOrNull<MethodDecl>("main")
        assertNotNull(main)
        val actual = main.callees

        expected.add(
            (((((main.body as CompoundStmt).statements[0] as DeclarationStmt).declarations[0]
                            as VariableDecl)
                        .initializer as NewExpr)
                    .initializer as ConstructExpr)
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

        val expected = mutableListOf<FunctionDecl>()
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
        val magic = classDecl.byNameOrNull<MethodDecl>("magic")
        assertNotNull(magic)
        val ifStmt = (magic.body as CompoundStmt).statements[0] as IfStmt

        val actual = ifStmt.controls()
        ifStmt.thenStatement?.let { expected.add(it) }
        val thenStatement = (ifStmt.thenStatement as CompoundStmt).statements[0] as IfStmt
        expected.add(thenStatement)
        thenStatement.condition?.let { expected.add(it) }
        expected.add((thenStatement.condition as BinaryOp).lhs)
        expected.add(((thenStatement.condition as BinaryOp).lhs as MemberExpr).base)
        expected.add((thenStatement.condition as BinaryOp).rhs)
        val nestedThen = thenStatement.thenStatement as CompoundStmt
        expected.add(nestedThen)
        expected.add(nestedThen.statements[0])
        expected.add((nestedThen.statements[0] as AssignExpr).lhs.first())
        expected.add(((nestedThen.statements[0] as AssignExpr).lhs.first() as MemberExpr).base)
        expected.add((nestedThen.statements[0] as AssignExpr).rhs.first())
        val nestedElse = thenStatement.elseStatement as CompoundStmt
        expected.add(nestedElse)
        expected.add(nestedElse.statements[0])
        expected.add((nestedElse.statements[0] as AssignExpr).lhs.first())
        expected.add(((nestedElse.statements[0] as AssignExpr).lhs.first() as MemberExpr).base)
        expected.add((nestedElse.statements[0] as AssignExpr).rhs.first())

        ifStmt.elseStatement?.let { expected.add(it) }
        expected.add((ifStmt.elseStatement as CompoundStmt).statements[0])
        expected.add(
            ((ifStmt.elseStatement as CompoundStmt).statements[0] as AssignExpr).lhs.first()
        )
        expected.add(
            (((ifStmt.elseStatement as CompoundStmt).statements[0] as AssignExpr).lhs.first()
                    as MemberExpr)
                .base
        )
        expected.add(
            ((ifStmt.elseStatement as CompoundStmt).statements[0] as AssignExpr).rhs.first()
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
                    .registerPass<EdgeCachePass>()
                    .registerLanguage(TestLanguage("."))
                    .build()
            )

        val expected = mutableListOf<Node>()
        val classDecl = result.records["ShortcutClass"]
        assertNotNull(classDecl)
        val magic = classDecl.byNameOrNull<MethodDecl>("magic")
        assertNotNull(magic)

        // get the statement attr = 3;
        val ifStmt = (magic.body as CompoundStmt).statements[0] as IfStmt
        val thenStatement = (ifStmt.thenStatement as CompoundStmt).statements[0] as IfStmt
        val nestedThen = thenStatement.thenStatement as CompoundStmt
        val interestingNode = nestedThen.statements[0]
        val actual = interestingNode.controlledBy()

        expected.add(ifStmt)
        expected.add(thenStatement)

        assertTrue(expected.containsAll(actual))
        assertTrue(actual.containsAll(expected))
    }

    @Test
    fun testFollowPrevDFGEdgesUntilHit() {
        val classDecl = shortcutClassResult.records["ShortcutClass"]
        assertNotNull(classDecl)
        val magic2 = classDecl.byNameOrNull<MethodDecl>("magic2")
        assertNotNull(magic2)

        val aAssignment2 =
            ((((magic2.body as CompoundStmt).statements[1] as IfStmt).elseStatement as CompoundStmt)
                    .statements[0]
                    as AssignExpr)
                .lhs
                .first()

        val paramPassed2 = aAssignment2.followPrevDFGEdgesUntilHit { it is Literal<*> }
        assertEquals(1, paramPassed2.fulfilled.size)
        assertEquals(0, paramPassed2.failed.size)
        assertEquals(5, (paramPassed2.fulfilled[0].last() as? Literal<*>)?.value)

        val magic = classDecl.byNameOrNull<MethodDecl>("magic")
        assertNotNull(magic)

        val attrAssignment =
            ((((magic.body as CompoundStmt).statements[0] as IfStmt).elseStatement as CompoundStmt)
                    .statements[0]
                    as AssignExpr)
                .lhs
                .first()

        val paramPassed = attrAssignment.followPrevDFGEdgesUntilHit { it is Literal<*> }
        assertEquals(1, paramPassed.fulfilled.size)
        assertEquals(0, paramPassed.failed.size)
        assertEquals(3, (paramPassed.fulfilled[0].last() as? Literal<*>)?.value)
    }

    @Test
    fun testFollowPrevEOGEdgesUntilHit() {
        val classDecl = shortcutClassResult.records["ShortcutClass"]
        assertNotNull(classDecl)
        val magic = classDecl.byNameOrNull<MethodDecl>("magic")
        assertNotNull(magic)

        val attrAssignment =
            ((((magic.body as CompoundStmt).statements[0] as IfStmt).elseStatement as CompoundStmt)
                    .statements[0]
                    as AssignExpr)
                .lhs
                .first()

        val paramPassed = attrAssignment.followPrevEOGEdgesUntilHit { it is Literal<*> }
        assertEquals(1, paramPassed.fulfilled.size)
        assertEquals(0, paramPassed.failed.size)
        assertEquals(
            5,
            (paramPassed.fulfilled[0].last() as? Literal<*>)?.value
        ) // It's the comparison
    }

    @Test
    fun testFollowNextEOGEdgesUntilHit() {
        val classDecl = shortcutClassResult.records["ShortcutClass"]
        assertNotNull(classDecl)
        val magic = classDecl.byNameOrNull<MethodDecl>("magic")
        assertNotNull(magic)

        val ifCondition =
            ((magic.body as CompoundStmt).statements[0] as IfStmt).condition as BinaryOp

        val paramPassed =
            ifCondition.followNextEOGEdgesUntilHit {
                it is AssignExpr &&
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
        val magic = classDecl.byNameOrNull<MethodDecl>("magic")
        assertNotNull(magic)

        val attrAssignment =
            ((((magic.body as CompoundStmt).statements[0] as IfStmt).elseStatement as CompoundStmt)
                    .statements[0]
                    as AssignExpr)
                .lhs
                .first()

        val paramPassed = attrAssignment.followPrevDFG { it is Literal<*> }
        assertNotNull(paramPassed)
        assertEquals(3, (paramPassed.last() as? Literal<*>)?.value)
    }

    private lateinit var shortcutClassResult: TranslationResult

    @BeforeAll
    fun getShortcutClass() {
        shortcutClassResult = GraphExamples.getShortcutClass()
    }
}
