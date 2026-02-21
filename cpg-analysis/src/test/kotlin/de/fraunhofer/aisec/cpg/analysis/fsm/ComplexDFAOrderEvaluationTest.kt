/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.analysis.fsm

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.testcases.GraphExamples
import kotlin.test.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ComplexDFAOrderEvaluationTest {

    private var dfa = DFA()
    private lateinit var tu: TranslationUnit

    @BeforeTest
    fun getDFABeforeTest() {
        // allowed: cm.create(), cm.init(), (cm.start(), cm.process()*, cm.finish())+, cm.reset()?
        dfa = DFA()
        val q1 = dfa.addState(isStart = true)
        val q2 = dfa.addState()
        val q3 = dfa.addState()
        val q4 = dfa.addState()
        val q5 = dfa.addState()
        val q6 = dfa.addState(isAcceptingState = true)
        val q7 = dfa.addState(isAcceptingState = true)
        dfa.addEdge(q1, Edge("create()", "cm", q2))
        dfa.addEdge(q2, Edge("init()", "cm", q3))
        dfa.addEdge(q3, Edge("start()", "cm", q4))
        dfa.addEdge(q4, Edge("process()", "cm", q5))
        dfa.addEdge(q4, Edge("finish()", "cm", q6))
        dfa.addEdge(q5, Edge("process()", "cm", q5))
        dfa.addEdge(q5, Edge("finish()", "cm", q6))
        dfa.addEdge(q6, Edge("start()", "cm", q4))
        dfa.addEdge(q6, Edge("reset()", "cm", q7))
    }

    @BeforeAll
    fun beforeAll() {
        tu = GraphExamples.getComplexOrder().components.first().translationUnits.first()
    }

    @Test
    fun testSuccessMinimal1FSM() {
        val functionOk = tu.functions["ok_minimal1"]
        assertNotNull(functionOk)

        val p1Decl = functionOk.bodyOrNull<DeclarationStatement>(0)
        assertNotNull(p1Decl)
        val consideredDecl = mutableSetOf(p1Decl.declarations[0])

        val nodesToOp = mutableMapOf<Node, Set<String>>()
        nodesToOp[(functionOk.body as Block).statements[1]] = setOf("create()")
        nodesToOp[(functionOk.body as Block).statements[2]] = setOf("init()")
        nodesToOp[(functionOk.body as Block).statements[3]] = setOf("start()")
        nodesToOp[(functionOk.body as Block).statements[4]] = setOf("finish()")

        val orderEvaluator = DFAOrderEvaluator(dfa, consideredDecl, nodesToOp)
        val everythingOk = orderEvaluator.evaluateOrder(p1Decl)

        assertTrue(everythingOk, "Expected correct order")
    }

    @Test
    fun testSuccessMinimal2FSM() {
        val functionOk = tu.functions["ok_minimal2"]
        assertNotNull(functionOk)

        val p1Decl = functionOk.bodyOrNull<DeclarationStatement>(0)
        assertNotNull(p1Decl)
        val consideredDecl = mutableSetOf(p1Decl.declarations[0])

        val nodesToOp = mutableMapOf<Node, Set<String>>()
        nodesToOp[(functionOk.body as Block).statements[1]] = setOf("create()")
        nodesToOp[(functionOk.body as Block).statements[2]] = setOf("init()")
        nodesToOp[(functionOk.body as Block).statements[3]] = setOf("start()")
        nodesToOp[(functionOk.body as Block).statements[4]] = setOf("process()")
        nodesToOp[(functionOk.body as Block).statements[5]] = setOf("finish()")

        val orderEvaluator = DFAOrderEvaluator(dfa, consideredDecl, nodesToOp)
        val everythingOk = orderEvaluator.evaluateOrder(p1Decl)

        assertTrue(everythingOk, "Expected correct order")
    }

    @Test
    fun testSuccessMimimal3FSM() {
        val functionOk = tu.functions["ok_minimal3"]
        assertNotNull(functionOk)

        val p1Decl = functionOk.bodyOrNull<DeclarationStatement>(0)
        assertNotNull(p1Decl)
        val consideredDecl = mutableSetOf(p1Decl.declarations[0])

        val nodesToOp = mutableMapOf<Node, Set<String>>()
        nodesToOp[(functionOk.body as Block).statements[1]] = setOf("create()")
        nodesToOp[(functionOk.body as Block).statements[2]] = setOf("init()")
        nodesToOp[(functionOk.body as Block).statements[3]] = setOf("start()")
        nodesToOp[(functionOk.body as Block).statements[4]] = setOf("process()")
        nodesToOp[(functionOk.body as Block).statements[5]] = setOf("finish()")
        nodesToOp[(functionOk.body as Block).statements[6]] = setOf("reset()")

        val orderEvaluator = DFAOrderEvaluator(dfa, consideredDecl, nodesToOp)
        val everythingOk = orderEvaluator.evaluateOrder(p1Decl)

        assertTrue(everythingOk, "Expected correct order")
    }

    @Test
    fun testSuccessMultiProcessFSM() {
        val functionOk = tu.functions["ok2"]
        assertNotNull(functionOk)

        val p2Decl = functionOk.bodyOrNull<DeclarationStatement>(0)
        assertNotNull(p2Decl)
        val consideredDecl = mutableSetOf(p2Decl.declarations[0])

        val nodesToOp = mutableMapOf<Node, Set<String>>()
        nodesToOp[(functionOk.body as Block).statements[1]] = setOf("create()")
        nodesToOp[(functionOk.body as Block).statements[2]] = setOf("init()")
        nodesToOp[(functionOk.body as Block).statements[3]] = setOf("start()")
        nodesToOp[(functionOk.body as Block).statements[4]] = setOf("process()")
        nodesToOp[(functionOk.body as Block).statements[5]] = setOf("process()")
        nodesToOp[(functionOk.body as Block).statements[6]] = setOf("process()")
        nodesToOp[(functionOk.body as Block).statements[7]] = setOf("process()")
        nodesToOp[(functionOk.body as Block).statements[8]] = setOf("finish()")

        val orderEvaluator = DFAOrderEvaluator(dfa, consideredDecl, nodesToOp)
        val everythingOk = orderEvaluator.evaluateOrder(p2Decl)

        assertTrue(everythingOk, "Expected correct order")
    }

    @Test
    fun testSuccessLoopFSM() {
        val functionOk = tu.functions["ok3"]
        assertNotNull(functionOk)

        val p3Decl = functionOk.bodyOrNull<DeclarationStatement>(0)
        assertNotNull(p3Decl)
        val consideredDecl = mutableSetOf(p3Decl.declarations[0])

        val nodesToOp = mutableMapOf<Node, Set<String>>()
        nodesToOp[(functionOk.body as Block).statements[1]] = setOf("create()")
        nodesToOp[(functionOk.body as Block).statements[2]] = setOf("init()")
        nodesToOp[(functionOk.body as Block).statements[3]] = setOf("start()")
        nodesToOp[(functionOk.body as Block).statements[4]] = setOf("process()")
        nodesToOp[(functionOk.body as Block).statements[5]] = setOf("finish()")
        nodesToOp[(functionOk.body as Block).statements[6]] = setOf("start()")
        nodesToOp[(functionOk.body as Block).statements[7]] = setOf("process()")
        nodesToOp[(functionOk.body as Block).statements[8]] = setOf("finish()")

        val orderEvaluator = DFAOrderEvaluator(dfa, consideredDecl, nodesToOp)
        val everythingOk = orderEvaluator.evaluateOrder(p3Decl)

        assertTrue(everythingOk, "Expected correct order")
    }

    @Test
    fun testSuccessLoopResetFSM() {
        val functionOk = tu.functions["ok4"]
        assertNotNull(functionOk)

        val p3Decl = functionOk.bodyOrNull<DeclarationStatement>(0)
        assertNotNull(p3Decl)
        val consideredDecl = mutableSetOf(p3Decl.declarations[0])

        val nodes = mutableMapOf<Node, Set<String>>()
        nodes[(functionOk.body as Block).statements[1]] = setOf("create()")
        nodes[(functionOk.body as Block).statements[2]] = setOf("init()")
        nodes[(functionOk.body as Block).statements[3]] = setOf("start()")
        nodes[(functionOk.body as Block).statements[4]] = setOf("process()")
        nodes[(functionOk.body as Block).statements[5]] = setOf("finish()")
        nodes[(functionOk.body as Block).statements[6]] = setOf("start()")
        nodes[(functionOk.body as Block).statements[7]] = setOf("process()")
        nodes[(functionOk.body as Block).statements[8]] = setOf("finish()")
        nodes[(functionOk.body as Block).statements[9]] = setOf("reset()")

        val orderEvaluator = DFAOrderEvaluator(dfa, consideredDecl, nodes)
        val everythingOk = orderEvaluator.evaluateOrder(p3Decl)

        assertTrue(everythingOk, "Expected correct order")
    }

    @Test
    fun testFailMissingCreateFSM() {
        val functionOk = tu.functions["nok1"]
        assertNotNull(functionOk)

        val p5Decl = functionOk.bodyOrNull<DeclarationStatement>(0)
        assertNotNull(p5Decl)
        val consideredDecl = mutableSetOf(p5Decl.declarations[0])

        val nodesToOp = mutableMapOf<Node, Set<String>>()
        nodesToOp[(functionOk.body as Block).statements[1]] = setOf("init()")
        nodesToOp[(functionOk.body as Block).statements[2]] = setOf("start()")
        nodesToOp[(functionOk.body as Block).statements[3]] = setOf("process()")
        nodesToOp[(functionOk.body as Block).statements[4]] = setOf("finish()")

        val orderEvaluator = DFAOrderEvaluator(dfa, consideredDecl, nodesToOp)
        val everythingOk = orderEvaluator.evaluateOrder(p5Decl)

        assertFalse(everythingOk, "Expected incorrect order")
    }

    @Test
    fun testFailIfFSM() {
        val functionOk = tu.functions["nok2"]
        assertNotNull(functionOk)

        val p6Decl = functionOk.bodyOrNull<DeclarationStatement>(0)
        assertNotNull(p6Decl)
        val consideredDecl = mutableSetOf(p6Decl.declarations[0])

        val nodesToOp = mutableMapOf<Node, Set<String>>()
        nodesToOp[(functionOk.body as Block).statements[1]] = setOf("create()")
        nodesToOp[(functionOk.body as Block).statements[2]] = setOf("init()")

        val thenBranch =
            ((functionOk.body as Block).statements[3] as? IfStatement)?.thenStatement as? Block
        assertNotNull(thenBranch)
        nodesToOp[thenBranch.statements[0]] = setOf("start()")
        nodesToOp[thenBranch.statements[1]] = setOf("process()")
        nodesToOp[thenBranch.statements[2]] = setOf("finish()")

        nodesToOp[(functionOk.body as Block).statements[4]] = setOf("reset()")

        val orderEvaluator = DFAOrderEvaluator(dfa, consideredDecl, nodesToOp)
        val everythingOk = orderEvaluator.evaluateOrder(p6Decl)

        assertFalse(everythingOk, "Expected incorrect order")
    }

    @Test
    fun testFailWhileLoopFSM() {
        val functionOk = tu.functions["nok3"]

        assertNotNull(functionOk)

        val p6Decl = functionOk.bodyOrNull<DeclarationStatement>(0)
        assertNotNull(p6Decl)
        val consideredDecl = mutableSetOf(p6Decl.declarations[0])

        val nodesToOp = mutableMapOf<Node, Set<String>>()
        val loopBody =
            ((functionOk.body as Block).statements[1] as? WhileStatement)?.statement as? Block
        assertNotNull(loopBody)
        nodesToOp[loopBody.statements[0]] = setOf("create()")
        nodesToOp[loopBody.statements[1]] = setOf("init()")
        nodesToOp[loopBody.statements[2]] = setOf("start()")
        nodesToOp[loopBody.statements[3]] = setOf("process()")
        nodesToOp[loopBody.statements[4]] = setOf("finish()")

        nodesToOp[(functionOk.body as Block).statements[2]] = setOf("reset()")

        val orderEvaluator = DFAOrderEvaluator(dfa, consideredDecl, nodesToOp)
        val everythingOk = orderEvaluator.evaluateOrder(p6Decl)

        assertFalse(everythingOk, "Expected incorrect order")
    }

    @Test
    fun testFailWhileLoop2FSM() {
        val functionOk = tu.functions["nokWhile"]

        assertNotNull(functionOk)

        val p7Decl = functionOk.bodyOrNull<DeclarationStatement>(0)
        assertNotNull(p7Decl)
        val consideredDecl = mutableSetOf(p7Decl.declarations[0])

        val nodesToOp = mutableMapOf<Node, Set<String>>()
        nodesToOp[(functionOk.body as Block).statements[1]] = setOf("create()")
        nodesToOp[(functionOk.body as Block).statements[2]] = setOf("init()")
        val loopBody =
            ((functionOk.body as Block).statements[3] as? WhileStatement)?.statement as? Block
        assertNotNull(loopBody)
        nodesToOp[loopBody.statements[0]] = setOf("start()")
        nodesToOp[loopBody.statements[1]] = setOf("process()")
        nodesToOp[loopBody.statements[2]] = setOf("finish()")

        nodesToOp[(functionOk.body as Block).statements[4]] = setOf("reset()")

        val orderEvaluator = DFAOrderEvaluator(dfa, consideredDecl, nodesToOp)
        val everythingOk = orderEvaluator.evaluateOrder(p7Decl)

        assertFalse(everythingOk, "Expected incorrect order")
    }

    @Test
    fun testSuccessWhileLoop2FSM() {
        val functionOk = tu.functions["okWhile2"]
        assertNotNull(functionOk)

        val p7Decl = functionOk.bodyOrNull<DeclarationStatement>(0)
        assertNotNull(p7Decl)
        val consideredDecl = mutableSetOf(p7Decl.declarations[0])

        val nodesToOp = mutableMapOf<Node, Set<String>>()
        nodesToOp[(functionOk.body as Block).statements[1]] = setOf("create()")
        nodesToOp[(functionOk.body as Block).statements[2]] = setOf("init()")
        val loopBody =
            ((functionOk.body as Block).statements[3] as? WhileStatement)?.statement as? Block
        assertNotNull(loopBody)
        nodesToOp[loopBody.statements[0]] = setOf("start()")
        nodesToOp[loopBody.statements[1]] = setOf("process()")
        nodesToOp[loopBody.statements[2]] = setOf("finish()")

        nodesToOp[(functionOk.body as Block).statements[4]] = setOf("reset()")

        val orderEvaluator = DFAOrderEvaluator(dfa, consideredDecl, nodesToOp)
        val everythingOk = orderEvaluator.evaluateOrder(p7Decl)

        assertTrue(everythingOk, "Expected correct order")
    }

    @Test
    fun testSuccessWhileLoopFSM() {
        val functionOk = tu.functions["okWhile"]
        assertNotNull(functionOk)

        val p8Decl = functionOk.bodyOrNull<DeclarationStatement>(0)
        assertNotNull(p8Decl)
        val consideredDecl = mutableSetOf(p8Decl.declarations[0])

        val nodesToOp = mutableMapOf<Node, Set<String>>()
        nodesToOp[(functionOk.body as Block).statements[1]] = setOf("create()")
        nodesToOp[(functionOk.body as Block).statements[2]] = setOf("init()")
        nodesToOp[(functionOk.body as Block).statements[3]] = setOf("start()")
        nodesToOp[(functionOk.body as Block).statements[4]] = setOf("process()")
        nodesToOp[(functionOk.body as Block).statements[5]] = setOf("finish()")
        val loopBody =
            ((functionOk.body as Block).statements[6] as? WhileStatement)?.statement as? Block
        assertNotNull(loopBody)
        nodesToOp[loopBody.statements[0]] = setOf("start()")
        nodesToOp[loopBody.statements[1]] = setOf("process()")
        nodesToOp[loopBody.statements[2]] = setOf("finish()")

        nodesToOp[(functionOk.body as Block).statements[7]] = setOf("reset()")

        val orderEvaluator = DFAOrderEvaluator(dfa, consideredDecl, nodesToOp)
        val everythingOk = orderEvaluator.evaluateOrder(p8Decl)

        assertTrue(everythingOk, "Expected correct order")
    }

    @Test
    fun testSuccessDoWhileLoopFSM() {
        val functionOk = tu.functions["okDoWhile"]
        assertNotNull(functionOk)

        val p6Decl = functionOk.bodyOrNull<DeclarationStatement>(0)
        assertNotNull(p6Decl)
        val consideredDecl = mutableSetOf(p6Decl.declarations[0])

        val nodesToOp = mutableMapOf<Node, Set<String>>()
        nodesToOp[(functionOk.body as Block).statements[1]] = setOf("create()")
        nodesToOp[(functionOk.body as Block).statements[2]] = setOf("init()")
        val loopBody = ((functionOk.body as Block).statements[3] as DoStatement).statement as? Block
        assertNotNull(loopBody)
        nodesToOp[loopBody.statements[0]] = setOf("start()")
        nodesToOp[loopBody.statements[1]] = setOf("process()")
        nodesToOp[loopBody.statements[2]] = setOf("finish()")

        nodesToOp[(functionOk.body as Block).statements[4]] = setOf("reset()")

        val orderEvaluator = DFAOrderEvaluator(dfa, consideredDecl, nodesToOp)
        val everythingOk = orderEvaluator.evaluateOrder(p6Decl)

        assertTrue(everythingOk, "Expected correct order")
    }

    @Test
    fun testSuccessMinimalInterprocUnclearFSM() {
        val functionOk = tu.functions["minimalInterprocUnclear"]
        assertNotNull(functionOk)

        val p1Decl = functionOk.bodyOrNull<DeclarationStatement>(0)
        assertNotNull(p1Decl)
        val consideredDecl = mutableSetOf(p1Decl.declarations[0])

        val nodesToOp = mutableMapOf<Node, Set<String>>()
        nodesToOp[(functionOk.body as Block).statements[1]] = setOf("create()")
        nodesToOp[(functionOk.body as Block).statements[3]] = setOf("start()")
        nodesToOp[(functionOk.body as Block).statements[4]] = setOf("finish()")

        val afterInterprocNodes = mutableListOf<Node>()
        val withoutInterprocNodes = mutableListOf<Node>()
        val orderEvaluator =
            DummyDFAOrderEvaluator(
                dfa,
                consideredDecl,
                nodesToOp,
                mutableMapOf(),
                afterInterprocNodes,
                withoutInterprocNodes,
            )
        val everythingOk = orderEvaluator.evaluateOrder(p1Decl)

        assertFalse(everythingOk, "Expected incorrect order")
        assertContains(
            afterInterprocNodes,
            (functionOk.body as Block).statements[3],
            "Expected start() node in list of unknown nodes",
        )
        assertTrue(withoutInterprocNodes.isEmpty(), "No node should be clearly violating the rule")
    }

    @Test
    fun testSuccessMinimalInterprocUnclearArgumentFSM() {
        val functionOk = tu.functions["minimalInterprocUnclearArgument"]
        assertNotNull(functionOk)

        val p1Decl = functionOk.parameters[0]
        assertNotNull(p1Decl)
        val consideredDecl = mutableSetOf(p1Decl)

        val nodesToOp = mutableMapOf<Node, Set<String>>()
        nodesToOp[(functionOk.body as Block).statements[0]] = setOf("init()")
        nodesToOp[(functionOk.body as Block).statements[1]] = setOf("start()")
        nodesToOp[(functionOk.body as Block).statements[2]] = setOf("finish()")

        val afterInterprocNodes = mutableListOf<Node>()
        val withoutInterprocNodes = mutableListOf<Node>()
        val orderEvaluator =
            DummyDFAOrderEvaluator(
                dfa,
                consideredDecl,
                nodesToOp,
                mutableMapOf(),
                afterInterprocNodes,
                withoutInterprocNodes,
            )
        // We cannot use p1Decl as start of the analysis because it has no nextEOG edges. Instead,
        // we want to start with the first instruction of the function.
        val everythingOk = orderEvaluator.evaluateOrder((functionOk.body as Block).statements[0])

        assertFalse(everythingOk, "Expected incorrect order")
        assertContains(
            afterInterprocNodes,
            (functionOk.body as Block).statements[0],
            "Expected init() node in list of unknown nodes",
        )
        assertTrue(withoutInterprocNodes.isEmpty(), "No node should be clearly violating the rule")
    }

    @Test
    fun testSuccessMinimalInterprocUnclearReturnFSM() {
        val functionOk = tu.functions["minimalInterprocUnclearReturn"]
        assertNotNull(functionOk)

        val p1Decl = functionOk.bodyOrNull<DeclarationStatement>(0)
        assertNotNull(p1Decl)
        val consideredDecl = mutableSetOf(p1Decl.declarations[0])

        val nodesToOp = mutableMapOf<Node, Set<String>>()
        nodesToOp[(functionOk.body as Block).statements[1]] = setOf("create()")
        nodesToOp[(functionOk.body as Block).statements[2]] = setOf("init()")
        nodesToOp[(functionOk.body as Block).statements[3]] = setOf("start()")

        val possibleInterprocFailures = mutableListOf<Node>()
        val withoutInterprocNodes = mutableListOf<Node>()
        val orderEvaluator =
            DummyDFAOrderEvaluator(
                dfa,
                consideredDecl,
                nodesToOp,
                mutableMapOf(),
                possibleInterprocFailures,
                withoutInterprocNodes,
            )
        val everythingOk = orderEvaluator.evaluateOrder(p1Decl)

        assertFalse(everythingOk, "Expected incorrect order")
        assertContains(
            possibleInterprocFailures,
            (functionOk.body as Block).statements[3],
            "Expected start() node in list of unknown nodes",
        )
        assertTrue(withoutInterprocNodes.isEmpty(), "No node should be clearly violating the rule")
    }

    @Test
    fun testSuccessMinimalInterprocFailFSM() {
        val functionOk = tu.functions["minimalInterprocFail"]
        assertNotNull(functionOk)

        val p1Decl = functionOk.bodyOrNull<DeclarationStatement>(0)
        assertNotNull(p1Decl)
        val consideredDecl = mutableSetOf(p1Decl.declarations[0])

        val nodesToOp = mutableMapOf<Node, Set<String>>()
        nodesToOp[(functionOk.body as Block).statements[1]] = setOf("create()")
        nodesToOp[(functionOk.body as Block).statements[3]] = setOf("start()")
        nodesToOp[(functionOk.body as Block).statements[4]] = setOf("finish()")

        val afterInterprocNodes = mutableListOf<Node>()
        val withoutInterprocNodes = mutableListOf<Node>()
        val orderEvaluator =
            DummyDFAOrderEvaluator(
                dfa,
                consideredDecl,
                nodesToOp,
                mutableMapOf(),
                afterInterprocNodes,
                withoutInterprocNodes,
            )
        val everythingOk = orderEvaluator.evaluateOrder(p1Decl)

        assertFalse(everythingOk, "Expected incorrect order")
        assertContains(
            afterInterprocNodes,
            (functionOk.body as Block).statements[3],
            "Expected start() node in list of unknown nodes",
        )
        assertContains(
            withoutInterprocNodes,
            (functionOk.body as Block).statements[3],
            "Expected start() node in list of unknown nodes",
        )
    }

    @Test
    fun testSuccessMinimalInterprocFail2FSM() {
        val functionOk = tu.functions["minimalInterprocFail2"]
        assertNotNull(functionOk)

        val p1Decl = functionOk.bodyOrNull<DeclarationStatement>(0)
        assertNotNull(p1Decl)
        val consideredDecl = mutableSetOf(p1Decl.declarations[0])

        val nodesToOp = mutableMapOf<Node, Set<String>>()
        nodesToOp[(functionOk.body as Block).statements[2]] = setOf("create()")
        nodesToOp[(functionOk.body as Block).statements[5]] = setOf("start()")
        nodesToOp[(functionOk.body as Block).statements[6]] = setOf("finish()")

        val afterInterprocNodes = mutableListOf<Node>()
        val withoutInterprocNodes = mutableListOf<Node>()
        val orderEvaluator =
            DummyDFAOrderEvaluator(
                dfa,
                consideredDecl,
                nodesToOp,
                mutableMapOf(),
                afterInterprocNodes,
                withoutInterprocNodes,
            )
        val everythingOk = orderEvaluator.evaluateOrder(p1Decl)

        assertFalse(everythingOk, "Expected incorrect order")
        assertTrue(afterInterprocNodes.isEmpty(), "All nodes clearly violate the rule")
        assertContains(
            withoutInterprocNodes,
            (functionOk.body as Block).statements[5],
            "Expected start() node in list of unknown nodes",
        )
    }

    /**
     * Class to test if the logic separating between edges which fail because of interprocedural
     * flows works. Collects the respective nodes and they can be used by the tests later.
     */
    class DummyDFAOrderEvaluator(
        dfa: DFA,
        referencedVertices: Set<Node>,
        nodesToOp: Map<Node, Set<String>>,
        thisPositionOfNode: Map<Node, Int>,
        private val possibleInterprocFailures: MutableList<Node>,
        private val withoutInterprocNodes: MutableList<Node>,
    ) :
        DFAOrderEvaluator(
            dfa = dfa,
            consideredBases = referencedVertices,
            nodeToRelevantMethod = nodesToOp,
            thisPositionOfNode = thisPositionOfNode,
        ) {
        private val log: Logger = LoggerFactory.getLogger(DummyDFAOrderEvaluator::class.java)

        override fun actionMissingTransitionForNode(
            node: Node,
            fsm: DFA,
            interproceduralFlow: Boolean,
        ) {
            if (interproceduralFlow) {
                possibleInterprocFailures.add(node)
            } else {
                withoutInterprocNodes.add(node)
            }
            log.error(
                "There was a failure in the order of statements at node: $node interproceduralFlow = $interproceduralFlow"
            )
        }

        override fun actionNonAcceptingTermination(
            base: String,
            fsm: DFA,
            interproceduralFlow: Boolean,
        ) {
            val lastNode = fsm.executionTrace.last().cpgNode as CallExpression
            var baseOfLastNode = getBaseOfNode(lastNode)
            if (baseOfLastNode is Reference) {
                baseOfLastNode = baseOfLastNode.refersTo
            }
            val returnStatements =
                lastNode.followNextEOG { edge ->
                    edge.end is ReturnStatement &&
                        ((edge.end as ReturnStatement).returnValue as? Reference)?.refersTo ==
                            baseOfLastNode
                }
            if (returnStatements?.isNotEmpty() == true) {
                // There was a return statement returning the respective variable. The flow of
                // execution could continue in the callee, so we mark this as unsure problem.
                log.error(
                    "The DFA did not terminate in an accepted state but the variable could be returned"
                )
                possibleInterprocFailures.add(lastNode)
            } else {
                log.error(
                    "The DFA did not terminate in an accepted state. interproceduralFlow = $interproceduralFlow"
                )
            }
        }
    }
}
