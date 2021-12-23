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

import de.fraunhofer.aisec.cpg.TestUtils
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.passes.EdgeCachePass
import de.fraunhofer.aisec.cpg.passes.IdentifierPass
import java.nio.file.Path
import kotlin.test.BeforeTest
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ComplexDFAOrderEvaluationTest {

    private var dfa = DFA()
    private lateinit var tu: TranslationUnitDeclaration

    @BeforeTest
    private fun getDFABeforeTest() {
        // allowed: cm.create(), cm.init(), (cm.start(), cm.process()*, cm.finish())+, cm.reset()?
        dfa = DFA()
        val q1 = dfa.addState(isStart = true)
        val q2 = dfa.addState()
        val q3 = dfa.addState()
        val q4 = dfa.addState()
        val q5 = dfa.addState()
        val q6 = dfa.addState(isAcceptingState = true)
        val q7 = dfa.addState(isAcceptingState = true)
        dfa.addEdge(q1, q2, "create()", "cm")
        dfa.addEdge(q2, q3, "init()", "cm")
        dfa.addEdge(q3, q4, "start()", "cm")
        dfa.addEdge(q4, q5, DFA.EPSILON, "cm")
        dfa.addEdge(q5, q5, "process()", "cm")
        dfa.addEdge(q5, q6, "finish()", "cm")
        dfa.addEdge(q6, q4, "start()", "cm")
        dfa.addEdge(q6, q7, "reset()", "cm")
    }

    @BeforeAll
    fun beforeAll() {
        val topLevel = Path.of("src", "test", "resources", "analyses", "ordering")
        TranslationManager.builder().build().analyze()
        tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("ComplexOrder.java").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(
                        JavaLanguageFrontend::class.java,
                        JavaLanguageFrontend.JAVA_EXTENSIONS
                    )
                    .registerPass(IdentifierPass())
                    .registerPass(EdgeCachePass())
            }
    }

    @Test
    fun testSuccessMinimal1FSM() {
        val functionOk =
            tu
                .getDeclarationsByName("ComplexOrder", RecordDeclaration::class.java)
                .firstOrNull()
                ?.declarations
                ?.firstOrNull { d -> d.name == "ok_minimal1" } as
                FunctionDeclaration?

        assertNotNull(functionOk)

        val p1Decl = (functionOk.body as CompoundStatement).statements[0] as? DeclarationStatement
        assertNotNull(p1Decl)
        val consideredDecl = mutableSetOf(p1Decl.declarations[0]?.id!!)

        val nodesToOp = mutableMapOf<Node, String>()
        nodesToOp[(functionOk.body as CompoundStatement).statements[1]] = "create()"
        nodesToOp[(functionOk.body as CompoundStatement).statements[2]] = "init()"
        nodesToOp[(functionOk.body as CompoundStatement).statements[3]] = "start()"
        nodesToOp[(functionOk.body as CompoundStatement).statements[4]] = "finish()"

        val orderEvaluator = DFAOrderEvaluator(consideredDecl, nodesToOp)
        val everythingOk = orderEvaluator.evaluateOrder(dfa, p1Decl)

        assertTrue(everythingOk, "Expected correct order")
    }

    @Test
    fun testSuccessMinimal2FSM() {
        val functionOk =
            tu
                .getDeclarationsByName("ComplexOrder", RecordDeclaration::class.java)
                .firstOrNull()
                ?.declarations
                ?.firstOrNull { d -> d.name == "ok_minimal2" } as
                FunctionDeclaration?

        assertNotNull(functionOk)

        val p1Decl = (functionOk.body as CompoundStatement).statements[0] as? DeclarationStatement
        assertNotNull(p1Decl)
        val consideredDecl = mutableSetOf(p1Decl.declarations[0]?.id!!)

        val nodesToOp = mutableMapOf<Node, String>()
        nodesToOp[(functionOk.body as CompoundStatement).statements[1]] = "create()"
        nodesToOp[(functionOk.body as CompoundStatement).statements[2]] = "init()"
        nodesToOp[(functionOk.body as CompoundStatement).statements[3]] = "start()"
        nodesToOp[(functionOk.body as CompoundStatement).statements[4]] = "process()"
        nodesToOp[(functionOk.body as CompoundStatement).statements[5]] = "finish()"

        val orderEvaluator = DFAOrderEvaluator(consideredDecl, nodesToOp)
        val everythingOk = orderEvaluator.evaluateOrder(dfa, p1Decl)

        assertTrue(everythingOk, "Expected correct order")
    }

    @Test
    fun testSuccessMimimal3FSM() {
        val functionOk =
            tu
                .getDeclarationsByName("ComplexOrder", RecordDeclaration::class.java)
                .firstOrNull()
                ?.declarations
                ?.firstOrNull { d -> d.name == "ok_minimal3" } as
                FunctionDeclaration?

        assertNotNull(functionOk)

        val p1Decl = (functionOk.body as CompoundStatement).statements[0] as? DeclarationStatement
        assertNotNull(p1Decl)
        val consideredDecl = mutableSetOf(p1Decl.declarations[0]?.id!!)

        val nodesToOp = mutableMapOf<Node, String>()
        nodesToOp[(functionOk.body as CompoundStatement).statements[1]] = "create()"
        nodesToOp[(functionOk.body as CompoundStatement).statements[2]] = "init()"
        nodesToOp[(functionOk.body as CompoundStatement).statements[3]] = "start()"
        nodesToOp[(functionOk.body as CompoundStatement).statements[4]] = "process()"
        nodesToOp[(functionOk.body as CompoundStatement).statements[5]] = "finish()"
        nodesToOp[(functionOk.body as CompoundStatement).statements[6]] = "reset()"

        val orderEvaluator = DFAOrderEvaluator(consideredDecl, nodesToOp)
        val everythingOk = orderEvaluator.evaluateOrder(dfa, p1Decl)

        assertTrue(everythingOk, "Expected correct order")
    }

    @Test
    fun testSuccessMultiProcessFSM() {
        val functionOk =
            tu
                .getDeclarationsByName("ComplexOrder", RecordDeclaration::class.java)
                .firstOrNull()
                ?.declarations
                ?.firstOrNull { d -> d.name == "ok2" } as
                FunctionDeclaration?

        assertNotNull(functionOk)

        val p2Decl = (functionOk.body as CompoundStatement).statements[0] as? DeclarationStatement
        assertNotNull(p2Decl)
        val consideredDecl = mutableSetOf(p2Decl.declarations[0]?.id!!)

        val nodesToOp = mutableMapOf<Node, String>()
        nodesToOp[(functionOk.body as CompoundStatement).statements[1]] = "create()"
        nodesToOp[(functionOk.body as CompoundStatement).statements[2]] = "init()"
        nodesToOp[(functionOk.body as CompoundStatement).statements[3]] = "start()"
        nodesToOp[(functionOk.body as CompoundStatement).statements[4]] = "process()"
        nodesToOp[(functionOk.body as CompoundStatement).statements[5]] = "process()"
        nodesToOp[(functionOk.body as CompoundStatement).statements[6]] = "process()"
        nodesToOp[(functionOk.body as CompoundStatement).statements[7]] = "process()"
        nodesToOp[(functionOk.body as CompoundStatement).statements[8]] = "finish()"

        val orderEvaluator = DFAOrderEvaluator(consideredDecl, nodesToOp)
        val everythingOk = orderEvaluator.evaluateOrder(dfa, p2Decl)

        assertTrue(everythingOk, "Expected correct order")
    }

    @Test
    fun testSuccessLoopFSM() {
        val functionOk =
            tu
                .getDeclarationsByName("ComplexOrder", RecordDeclaration::class.java)
                .firstOrNull()
                ?.declarations
                ?.firstOrNull { d -> d.name == "ok3" } as
                FunctionDeclaration?

        assertNotNull(functionOk)

        val p3Decl = (functionOk.body as CompoundStatement).statements[0] as? DeclarationStatement
        assertNotNull(p3Decl)
        val consideredDecl = mutableSetOf(p3Decl.declarations[0]?.id!!)

        val nodesToOp = mutableMapOf<Node, String>()
        nodesToOp[(functionOk.body as CompoundStatement).statements[1]] = "create()"
        nodesToOp[(functionOk.body as CompoundStatement).statements[2]] = "init()"
        nodesToOp[(functionOk.body as CompoundStatement).statements[3]] = "start()"
        nodesToOp[(functionOk.body as CompoundStatement).statements[4]] = "process()"
        nodesToOp[(functionOk.body as CompoundStatement).statements[5]] = "finish()"
        nodesToOp[(functionOk.body as CompoundStatement).statements[6]] = "start()"
        nodesToOp[(functionOk.body as CompoundStatement).statements[7]] = "process()"
        nodesToOp[(functionOk.body as CompoundStatement).statements[8]] = "finish()"

        val orderEvaluator = DFAOrderEvaluator(consideredDecl, nodesToOp)
        val everythingOk = orderEvaluator.evaluateOrder(dfa, p3Decl)

        assertTrue(everythingOk, "Expected correct order")
    }

    @Test
    fun testSuccessLoopResetFSM() {
        val functionOk =
            tu
                .getDeclarationsByName("ComplexOrder", RecordDeclaration::class.java)
                .firstOrNull()
                ?.declarations
                ?.firstOrNull { d -> d.name == "ok4" } as
                FunctionDeclaration?

        assertNotNull(functionOk)

        val p3Decl = (functionOk.body as CompoundStatement).statements[0] as? DeclarationStatement
        assertNotNull(p3Decl)
        val consideredDecl = mutableSetOf(p3Decl.declarations[0]?.id!!)

        val nodes = mutableMapOf<Node, String>()
        nodes[(functionOk.body as CompoundStatement).statements[1]] = "create()"
        nodes[(functionOk.body as CompoundStatement).statements[2]] = "init()"
        nodes[(functionOk.body as CompoundStatement).statements[3]] = "start()"
        nodes[(functionOk.body as CompoundStatement).statements[4]] = "process()"
        nodes[(functionOk.body as CompoundStatement).statements[5]] = "finish()"
        nodes[(functionOk.body as CompoundStatement).statements[6]] = "start()"
        nodes[(functionOk.body as CompoundStatement).statements[7]] = "process()"
        nodes[(functionOk.body as CompoundStatement).statements[8]] = "finish()"
        nodes[(functionOk.body as CompoundStatement).statements[9]] = "reset()"

        val orderEvaluator = DFAOrderEvaluator(consideredDecl, nodes)
        val everythingOk = orderEvaluator.evaluateOrder(dfa, p3Decl)

        assertTrue(everythingOk, "Expected correct order")
    }

    @Test
    fun testFailMissingCreateFSM() {
        val functionOk =
            tu
                .getDeclarationsByName("ComplexOrder", RecordDeclaration::class.java)
                .firstOrNull()
                ?.declarations
                ?.firstOrNull { d -> d.name == "nok1" } as
                FunctionDeclaration?

        assertNotNull(functionOk)

        val p5Decl = (functionOk.body as CompoundStatement).statements[0] as? DeclarationStatement
        assertNotNull(p5Decl)
        val consideredDecl = mutableSetOf(p5Decl.declarations[0]?.id!!)

        val nodesToOp = mutableMapOf<Node, String>()
        nodesToOp[(functionOk.body as CompoundStatement).statements[1]] = "init()"
        nodesToOp[(functionOk.body as CompoundStatement).statements[2]] = "start()"
        nodesToOp[(functionOk.body as CompoundStatement).statements[3]] = "process()"
        nodesToOp[(functionOk.body as CompoundStatement).statements[4]] = "finish()"

        val orderEvaluator = DFAOrderEvaluator(consideredDecl, nodesToOp)
        val everythingOk = orderEvaluator.evaluateOrder(dfa, p5Decl)

        assertFalse(everythingOk, "Expected incorrect order")
    }

    @Test
    fun testFailIfFSM() {
        val functionOk =
            tu
                .getDeclarationsByName("ComplexOrder", RecordDeclaration::class.java)
                .firstOrNull()
                ?.declarations
                ?.firstOrNull { d -> d.name == "nok2" } as
                FunctionDeclaration?

        assertNotNull(functionOk)

        val p6Decl = (functionOk.body as CompoundStatement).statements[0] as? DeclarationStatement
        assertNotNull(p6Decl)
        val consideredDecl = mutableSetOf(p6Decl.declarations[0]?.id!!)

        val nodesToOp = mutableMapOf<Node, String>()
        nodesToOp[(functionOk.body as CompoundStatement).statements[1]] = "create()"
        nodesToOp[(functionOk.body as CompoundStatement).statements[2]] = "init()"

        val thenBranch =
            ((functionOk.body as CompoundStatement).statements[3] as? IfStatement)
                ?.thenStatement as?
                CompoundStatement
        assertNotNull(thenBranch)
        nodesToOp[thenBranch.statements[0]] = "start()"
        nodesToOp[thenBranch.statements[1]] = "process()"
        nodesToOp[thenBranch.statements[2]] = "finish()"

        nodesToOp[(functionOk.body as CompoundStatement).statements[4]] = "reset()"

        val orderEvaluator = DFAOrderEvaluator(consideredDecl, nodesToOp)
        val everythingOk = orderEvaluator.evaluateOrder(dfa, p6Decl)

        assertFalse(everythingOk, "Expected incorrect order")
    }

    @Test
    fun testFailWhileLoopFSM() {
        val functionOk =
            tu
                .getDeclarationsByName("ComplexOrder", RecordDeclaration::class.java)
                .firstOrNull()
                ?.declarations
                ?.firstOrNull { d -> d.name == "nok3" } as
                FunctionDeclaration?

        assertNotNull(functionOk)

        val p6Decl = (functionOk.body as CompoundStatement).statements[0] as? DeclarationStatement
        assertNotNull(p6Decl)
        val consideredDecl = mutableSetOf(p6Decl.declarations[0]?.id!!)

        val nodesToOp = mutableMapOf<Node, String>()
        val loopBody =
            ((functionOk.body as CompoundStatement).statements[1] as? WhileStatement)?.statement as?
                CompoundStatement
        assertNotNull(loopBody)
        nodesToOp[loopBody.statements[0]] = "create()"
        nodesToOp[loopBody.statements[1]] = "init()"
        nodesToOp[loopBody.statements[2]] = "start()"
        nodesToOp[loopBody.statements[3]] = "process()"
        nodesToOp[loopBody.statements[4]] = "finish()"

        nodesToOp[(functionOk.body as CompoundStatement).statements[2]] = "reset()"

        val orderEvaluator = DFAOrderEvaluator(consideredDecl, nodesToOp)
        val everythingOk = orderEvaluator.evaluateOrder(dfa, p6Decl)

        assertFalse(everythingOk, "Expected incorrect order")
    }

    @Test
    fun testFailWhileLoop2FSM() {
        val functionOk =
            tu
                .getDeclarationsByName("ComplexOrder", RecordDeclaration::class.java)
                .firstOrNull()
                ?.declarations
                ?.firstOrNull { d -> d.name == "nokWhile" } as
                FunctionDeclaration?

        assertNotNull(functionOk)

        val p7Decl = (functionOk.body as CompoundStatement).statements[0] as? DeclarationStatement
        assertNotNull(p7Decl)
        val consideredDecl = mutableSetOf(p7Decl.declarations[0]?.id!!)

        val nodesToOp = mutableMapOf<Node, String>()
        nodesToOp[(functionOk.body as CompoundStatement).statements[1]] = "create()"
        nodesToOp[(functionOk.body as CompoundStatement).statements[2]] = "init()"
        val loopBody =
            ((functionOk.body as CompoundStatement).statements[3] as? WhileStatement)?.statement as?
                CompoundStatement
        assertNotNull(loopBody)
        nodesToOp[loopBody.statements[0]] = "start()"
        nodesToOp[loopBody.statements[1]] = "process()"
        nodesToOp[loopBody.statements[2]] = "finish()"

        nodesToOp[(functionOk.body as CompoundStatement).statements[4]] = "reset()"

        val orderEvaluator = DFAOrderEvaluator(consideredDecl, nodesToOp)
        val everythingOk = orderEvaluator.evaluateOrder(dfa, p7Decl)

        assertFalse(everythingOk, "Expected incorrect order")
    }

    @Test
    fun testSuccessWhileLoopFSM() {
        val functionOk =
            tu
                .getDeclarationsByName("ComplexOrder", RecordDeclaration::class.java)
                .firstOrNull()
                ?.declarations
                ?.firstOrNull { d -> d.name == "okWhile" } as
                FunctionDeclaration?

        assertNotNull(functionOk)

        val p8Decl = (functionOk.body as CompoundStatement).statements[0] as? DeclarationStatement
        assertNotNull(p8Decl)
        val consideredDecl = mutableSetOf(p8Decl.declarations[0]?.id!!)

        val nodesToOp = mutableMapOf<Node, String>()
        nodesToOp[(functionOk.body as CompoundStatement).statements[1]] = "create()"
        nodesToOp[(functionOk.body as CompoundStatement).statements[2]] = "init()"
        nodesToOp[(functionOk.body as CompoundStatement).statements[3]] = "start()"
        nodesToOp[(functionOk.body as CompoundStatement).statements[4]] = "process()"
        nodesToOp[(functionOk.body as CompoundStatement).statements[5]] = "finish()"
        val loopBody =
            ((functionOk.body as CompoundStatement).statements[6] as? WhileStatement)?.statement as?
                CompoundStatement
        assertNotNull(loopBody)
        nodesToOp[loopBody.statements[0]] = "start()"
        nodesToOp[loopBody.statements[1]] = "process()"
        nodesToOp[loopBody.statements[2]] = "finish()"

        nodesToOp[(functionOk.body as CompoundStatement).statements[7]] = "reset()"

        val orderEvaluator = DFAOrderEvaluator(consideredDecl, nodesToOp)
        val everythingOk = orderEvaluator.evaluateOrder(dfa, p8Decl)

        assertTrue(everythingOk, "Expected correct order")
    }

    @Test
    fun testSuccessDoWhileLoopFSM() {
        val functionOk =
            tu
                .getDeclarationsByName("ComplexOrder", RecordDeclaration::class.java)
                .firstOrNull()
                ?.declarations
                ?.firstOrNull { d -> d.name == "okDoWhile" } as
                FunctionDeclaration?

        assertNotNull(functionOk)

        val p6Decl = (functionOk.body as CompoundStatement).statements[0] as? DeclarationStatement
        assertNotNull(p6Decl)
        val consideredDecl = mutableSetOf(p6Decl.declarations[0]?.id!!)

        val nodesToOp = mutableMapOf<Node, String>()
        nodesToOp[(functionOk.body as CompoundStatement).statements[1]] = "create()"
        nodesToOp[(functionOk.body as CompoundStatement).statements[2]] = "init()"
        val loopBody =
            ((functionOk.body as CompoundStatement).statements[3] as DoStatement).statement as?
                CompoundStatement
        assertNotNull(loopBody)
        nodesToOp[loopBody.statements[0]] = "start()"
        nodesToOp[loopBody.statements[1]] = "process()"
        nodesToOp[loopBody.statements[2]] = "finish()"

        nodesToOp[(functionOk.body as CompoundStatement).statements[4]] = "reset()"

        val orderEvaluator = DFAOrderEvaluator(consideredDecl, nodesToOp)
        val everythingOk = orderEvaluator.evaluateOrder(dfa, p6Decl)

        assertTrue(everythingOk, "Expected correct order")
    }
}
