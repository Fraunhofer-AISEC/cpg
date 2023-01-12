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
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguage
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.bodyOrNull
import de.fraunhofer.aisec.cpg.graph.byNameOrNull
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.IfStatement
import de.fraunhofer.aisec.cpg.passes.EdgeCachePass
import de.fraunhofer.aisec.cpg.passes.UnreachableEOGPass
import java.nio.file.Path
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SimpleDFAOrderEvaluationTest {
    private var dfa = DFA()
    private lateinit var tu: TranslationUnitDeclaration

    @BeforeTest
    fun getDFABeforeTest() {
        // allowed: cm.start(), cm.finish()
        dfa = DFA()
        val q1 = dfa.addState(isStart = true)
        val q2 = dfa.addState()
        val q3 = dfa.addState(isAcceptingState = true)
        dfa.addEdge(q1, Edge("start()", "cm", q2))
        dfa.addEdge(q2, Edge("finish()", "cm", q3))
    }

    @BeforeAll
    fun beforeAll() {
        val topLevel = Path.of("src", "test", "resources", "analyses", "ordering")
        TranslationManager.builder().build().analyze()
        tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("SimpleOrder.java").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage<JavaLanguage>()
                    .registerPass(UnreachableEOGPass())
                    .registerPass(EdgeCachePass())
            }
    }

    @Test
    fun testSuccessForFSM() {
        val functionOk =
            tu.byNameOrNull<RecordDeclaration>("SimpleOrder")
                ?.byNameOrNull<FunctionDeclaration>("ok")
        assertNotNull(functionOk)

        val p4Decl = functionOk.bodyOrNull<DeclarationStatement>(0)
        assertNotNull(p4Decl)
        val consideredDecl = mutableSetOf(p4Decl.declarations[0])

        val nodesToOp = mutableMapOf<Node, Set<String>>()
        nodesToOp[(functionOk.body as CompoundStatement).statements[1]] = setOf("start()")
        nodesToOp[(functionOk.body as CompoundStatement).statements[2]] = setOf("finish()")

        val orderEvaluator = DFAOrderEvaluator(dfa, consideredDecl, nodesToOp)
        val everythingOk = orderEvaluator.evaluateOrder(p4Decl)

        assertTrue(everythingOk, "Expected correct order")
    }

    @Test
    fun testSuccessWithIgnoredFunctionFSM() {
        val functionOk =
            tu.byNameOrNull<RecordDeclaration>("SimpleOrder")
                ?.byNameOrNull<FunctionDeclaration>("ok2")
        assertNotNull(functionOk)

        val p4Decl = functionOk.bodyOrNull<DeclarationStatement>(0)
        assertNotNull(p4Decl)
        val consideredDecl = mutableSetOf(p4Decl.declarations[0])

        val nodesToOp = mutableMapOf<Node, Set<String>>()
        nodesToOp[(functionOk.body as CompoundStatement).statements[1]] = setOf("start()")
        // We do not model the call to foo() because it does not exist in our model.
        nodesToOp[(functionOk.body as CompoundStatement).statements[3]] = setOf("finish()")

        val orderEvaluator = DFAOrderEvaluator(dfa, consideredDecl, nodesToOp)
        val everythingOk = orderEvaluator.evaluateOrder(p4Decl)

        assertTrue(everythingOk, "Expected correct order")
    }

    @Test
    fun testSuccessWithIfElseFSM() {
        val functionOk =
            tu.byNameOrNull<RecordDeclaration>("SimpleOrder")
                ?.byNameOrNull<FunctionDeclaration>("ok3")
        assertNotNull(functionOk)

        val p4Decl = functionOk.bodyOrNull<DeclarationStatement>(0)
        assertNotNull(p4Decl)
        val consideredDecl = mutableSetOf(p4Decl.declarations[0])

        val nodesToOp = mutableMapOf<Node, Set<String>>()
        // We model the calls to start() for the then and the else branch
        val thenBranch =
            ((functionOk.body as CompoundStatement).statements[2] as? IfStatement)?.thenStatement
                as? CompoundStatement
        assertNotNull(thenBranch)
        nodesToOp[thenBranch.statements[0]] = setOf("start()")
        val elseBranch =
            ((functionOk.body as CompoundStatement).statements[2] as? IfStatement)?.elseStatement
                as? CompoundStatement
        assertNotNull(elseBranch)
        nodesToOp[elseBranch.statements[0]] = setOf("start()")

        // We do not model the call to foo() because it does not exist in our model.
        nodesToOp[(functionOk.body as CompoundStatement).statements[3]] = setOf("finish()")

        val orderEvaluator = DFAOrderEvaluator(dfa, consideredDecl, nodesToOp)
        val everythingOk = orderEvaluator.evaluateOrder(p4Decl)

        assertTrue(everythingOk, "Expected correct order")
    }

    @Test
    fun testFailWrongStartFSM() {
        val functionOk =
            tu.byNameOrNull<RecordDeclaration>("SimpleOrder")
                ?.byNameOrNull<FunctionDeclaration>("nok1")
        assertNotNull(functionOk)

        val pDecl = functionOk.bodyOrNull<DeclarationStatement>(0)
        assertNotNull(pDecl)
        val consideredBases = mutableSetOf(pDecl.declarations[0])

        val nodesToOp = mutableMapOf<Node, Set<String>>()
        nodesToOp[(functionOk.body as CompoundStatement).statements[1]] = setOf("set_key()")
        nodesToOp[(functionOk.body as CompoundStatement).statements[2]] = setOf("start()")
        nodesToOp[(functionOk.body as CompoundStatement).statements[3]] = setOf("finish()")
        // We do not model the call to foo() because it does not exist in our model.
        nodesToOp[(functionOk.body as CompoundStatement).statements[5]] = setOf("set_key()")

        val orderEvaluator = DFAOrderEvaluator(dfa, consideredBases, nodesToOp)
        val everythingOk = orderEvaluator.evaluateOrder(pDecl)

        assertFalse(everythingOk, "Expected incorrect order")
    }

    @Test
    fun testFailIncompleteFSM() {
        val functionOk =
            tu.byNameOrNull<RecordDeclaration>("SimpleOrder")
                ?.byNameOrNull<FunctionDeclaration>("nok2")
        assertNotNull(functionOk)

        val p2Decl = functionOk.bodyOrNull<DeclarationStatement>(0)
        assertNotNull(p2Decl)
        val consideredBases = mutableSetOf(p2Decl.declarations[0])

        val nodesToOp = mutableMapOf<Node, Set<String>>()
        nodesToOp[(functionOk.body as CompoundStatement).statements[1]] = setOf("start()")

        val orderEvaluator = DFAOrderEvaluator(dfa, consideredBases, nodesToOp)
        val everythingOk = orderEvaluator.evaluateOrder(p2Decl)

        assertFalse(everythingOk, "Expected incorrect order")
    }

    @Test
    fun testFailConditionallyIncompleteFSM() {
        val functionOk =
            tu.byNameOrNull<RecordDeclaration>("SimpleOrder")
                ?.byNameOrNull<FunctionDeclaration>("nok3")
        assertNotNull(functionOk)

        val p3Decl = functionOk.bodyOrNull<DeclarationStatement>(0)
        assertNotNull(p3Decl)
        val consideredDecl = mutableSetOf(p3Decl.declarations[0])

        val nodesToOp = mutableMapOf<Node, Set<String>>()
        val thenBranch =
            ((functionOk.body as CompoundStatement).statements[1] as? IfStatement)?.thenStatement
                as? CompoundStatement
        assertNotNull(thenBranch)
        nodesToOp[thenBranch.statements[0]] = setOf("start()")
        nodesToOp[(functionOk.body as CompoundStatement).statements[2]] = setOf("finish()")

        val orderEvaluator = DFAOrderEvaluator(dfa, consideredDecl, nodesToOp)
        val everythingOk = orderEvaluator.evaluateOrder(p3Decl)

        assertFalse(everythingOk, "Expected incorrect order")
    }

    @Test
    fun testFailDoubleInitFSM() {
        val functionOk =
            tu.byNameOrNull<RecordDeclaration>("SimpleOrder")
                ?.byNameOrNull<FunctionDeclaration>("nok4")
        assertNotNull(functionOk)

        val p4Decl = functionOk.bodyOrNull<DeclarationStatement>(0)
        assertNotNull(p4Decl)
        val consideredDecl = mutableSetOf(p4Decl.declarations[0])

        val nodesToOp = mutableMapOf<Node, Set<String>>()
        val thenBranch =
            ((functionOk.body as CompoundStatement).statements[1] as? IfStatement)?.thenStatement
                as? CompoundStatement
        assertNotNull(thenBranch)
        nodesToOp[thenBranch.statements[0]] = setOf("start()")
        nodesToOp[thenBranch.statements[1]] = setOf("finish()")
        nodesToOp[(functionOk.body as CompoundStatement).statements[2]] = setOf("start()")
        nodesToOp[(functionOk.body as CompoundStatement).statements[4]] = setOf("finish()")

        val orderEvaluator = DFAOrderEvaluator(dfa, consideredDecl, nodesToOp)
        val everythingOk = orderEvaluator.evaluateOrder(p4Decl)

        assertFalse(everythingOk, "Expected incorrect order")
    }
}
