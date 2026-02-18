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
package de.fraunhofer.aisec.cpg.frontends.experimental.rust

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.nio.file.Path
import kotlin.test.*

class RustControlFlowTest : BaseTest() {

    private val topLevel = Path.of("src", "test", "resources", "rust")

    private fun parseTU(file: String) =
        analyzeAndGetFirstTU(listOf(topLevel.resolve(file).toFile()), topLevel, true) {
            it.registerLanguage<RustLanguage>()
        }

    @Test
    fun testIfLet() {
        val tu = parseTU("control_flow.rs")
        assertNotNull(tu)

        val ifLetFunc = tu.functions["if_let"]
        assertNotNull(ifLetFunc)
        val body = ifLetFunc.body as? Block
        assertNotNull(body)

        val ifStmt = body.statements.getOrNull(1) as? IfStatement
        assertNotNull(ifStmt, "Expected second statement to be IfStatement")

        assertNotNull(ifStmt.condition)
        assertNotNull(ifStmt.thenStatement)

        val thenBlock = ifStmt.thenStatement as? Block
        assertNotNull(thenBlock)

        // Statement 0: Binding for x
        val declX = thenBlock.statements.getOrNull(0) as? DeclarationStatement
        assertNotNull(declX)
        val xVar = declX.declarations.getOrNull(0) as? VariableDeclaration
        assertEquals("x", xVar?.name?.localName)

        // Statement 1: let y = x
        val declStmt = thenBlock.statements.getOrNull(1) as? DeclarationStatement
        assertNotNull(declStmt)
        val y = declStmt.declarations.getOrNull(0) as? VariableDeclaration
        assertEquals("y", y?.name?.localName)

        val init = y?.initializer
        assertNotNull(init)
        assertEquals("x", init.name.localName)
    }

    @Test
    fun testWhileLet() {
        val tu = parseTU("control_flow.rs")
        assertNotNull(tu)

        val whileLetFunc = tu.functions["while_let"]
        assertNotNull(whileLetFunc)
        val body = whileLetFunc.body as? Block
        assertNotNull(body)

        val whileStmt = body.statements.getOrNull(1) as? WhileStatement
        assertNotNull(whileStmt, "Expected second statement to be WhileStatement")

        assertNotNull(whileStmt.condition)
        assertNotNull(whileStmt.statement)

        val loopBody = whileStmt.statement as? Block
        assertNotNull(loopBody)

        // Statement 0: Binding for x
        val declX = loopBody.statements.getOrNull(0) as? DeclarationStatement
        assertNotNull(declX)
        val xVar = declX.declarations.getOrNull(0) as? VariableDeclaration
        assertEquals("x", xVar?.name?.localName)

        // Statement 1: let y = x
        val declStmtLocal = loopBody.statements.getOrNull(1) as? DeclarationStatement
        assertNotNull(declStmtLocal)
        val y = declStmtLocal.declarations.getOrNull(0) as? VariableDeclaration
        assertEquals("y", y?.name?.localName)

        val init = y?.initializer
        assertNotNull(init)
        assertEquals("x", init.name.localName)
    }

    @Test
    fun testLoopLabels() {
        val tu = parseTU("loop_labels.rs")
        assertNotNull(tu)

        val func = tu.functions["loop_labels"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        // Outer loop (loop { ... })
        // Mapped to LabelStatement -> WhileStatement
        val outerLabel = body.statements.getOrNull(0) as? LabelStatement
        assertNotNull(outerLabel, "Expected outer to be LabelStatement")
        assertEquals("outer", outerLabel.label)

        val outerLoop = outerLabel.subStatement as? WhileStatement
        assertNotNull(outerLoop, "Expected outer loop to be WhileStatement")

        val innerBlock = outerLoop.statement as? Block
        assertNotNull(innerBlock)

        // Inner loop (while true { ... })
        val innerLabel = innerBlock.statements.getOrNull(0) as? LabelStatement
        assertNotNull(innerLabel, "Expected inner to be LabelStatement")
        assertEquals("inner", innerLabel.label)

        val innerLoop = innerLabel.subStatement as? WhileStatement
        assertNotNull(innerLoop, "Expected inner loop to be WhileStatement")

        val innerBody = innerLoop.statement as? Block
        assertNotNull(innerBody, "Expected inner body to be Block")

        val breakStmt = innerBody.statements.getOrNull(0) as? BreakStatement
        assertNotNull(breakStmt, "Expected break statement")
        assertEquals("outer", breakStmt.label)
    }

    @Test
    fun testAsync() {
        val tu = parseTU("async.rs")
        assertNotNull(tu)

        val asyncFn = tu.functions["async_fn"]
        assertNotNull(asyncFn)
        assertTrue("async" in asyncFn.modifiers)

        val caller = tu.functions["caller"]
        assertNotNull(caller)
        assertTrue("async" in caller.modifiers)

        val body = caller.body as? Block
        assertNotNull(body)

        val expr = body.statements.getOrNull(0)
        val awaitExpr = expr as? UnaryOperator
        assertNotNull(awaitExpr)
        assertEquals("await", awaitExpr.operatorCode)
        assertTrue(awaitExpr.isPostfix)

        val call = awaitExpr.input as? CallExpression
        assertNotNull(call)
        assertEquals("async_fn", call.name.localName)
    }

    @Test
    fun testContinueWithLabel() {
        val tu = parseTU("control_flow_advanced.rs")
        assertNotNull(tu)
        val func = tu.functions["test_continue_with_label"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val continueStmts = body.allChildren<ContinueStatement>()
        assertTrue(continueStmts.isNotEmpty(), "Should have continue statement")
        assertEquals("outer", continueStmts.first().label, "Continue should have label 'outer'")
    }

    @Test
    fun testContinueSimple() {
        val tu = parseTU("control_flow_advanced.rs")
        assertNotNull(tu)
        val func = tu.functions["test_continue_simple"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val continueStmts = body.allChildren<ContinueStatement>()
        assertTrue(continueStmts.isNotEmpty(), "Should have continue statement")
    }

    @Test
    fun testBreakWithValue() {
        val tu = parseTU("control_flow_advanced.rs")
        assertNotNull(tu)
        val func = tu.functions["test_break_with_value"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        // loop { break 42; } is used as expression — verify it parses without unknown problems
        val problems = body.allChildren<ProblemExpression>()
        assertTrue(
            problems.none { it.problem.contains("Unknown") },
            "Break with value should not produce unknown problems: ${problems.map { it.problem }}",
        )
    }

    @Test
    fun testIfElseChain() {
        val tu = parseTU("control_flow_advanced.rs")
        assertNotNull(tu)
        val func = tu.functions["test_if_else_chain"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        // if-else chains are modeled as nested ConditionalExpressions
        val condExprs = body.allChildren<ConditionalExpression>()
        assertTrue(condExprs.isNotEmpty(), "Should have conditional expressions")
        // The outer conditional should have an else branch
        val outerCond = condExprs.first()
        assertNotNull(outerCond.elseExpression, "First conditional should have else branch")
    }

    @Test
    fun testWhileLetLoop() {
        val tu = parseTU("control_flow_advanced.rs")
        assertNotNull(tu)
        val func = tu.functions["test_while_let"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val whileStmts = body.allChildren<WhileStatement>()
        assertTrue(whileStmts.isNotEmpty(), "Should have while-let loop")
    }

    @Test
    fun testBranchWhileLet() {
        val tu = parseTU("branch_coverage_statements.rs")
        assertNotNull(tu)
        val func = tu.functions["test_while_let"]
        assertNotNull(func, "Should have test_while_let function")
        val body = func.body as? Block
        assertNotNull(body)
        val whiles = body.allChildren<WhileStatement>()
        assertTrue(whiles.isNotEmpty(), "Should have while let loop")
    }

    @Test
    fun testBranchWhileLetOption() {
        val tu = parseTU("branch_coverage_edge_cases.rs")
        assertNotNull(tu)
        val func = tu.functions["test_while_let_option"]
        assertNotNull(func, "Should have test_while_let_option function")
        val body = func.body as? Block
        assertNotNull(body)
        val whiles = body.allChildren<WhileStatement>()
        assertTrue(whiles.isNotEmpty(), "Should have while-let loop")
    }

    @Test
    fun testBranchNestedIf() {
        val tu = parseTU("branch_coverage_edge_cases.rs")
        assertNotNull(tu)
        val func = tu.functions["test_nested_if"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val condExprs = body.allChildren<ConditionalExpression>()
        assertTrue(condExprs.size >= 3, "Should have nested conditional chain: ${condExprs.size}")
    }

    @Test
    fun testBranchLabeledWhile() {
        val tu = parseTU("branch_coverage_edge_cases.rs")
        assertNotNull(tu)
        val func = tu.functions["test_labeled_while"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val labels = body.allChildren<LabelStatement>()
        assertTrue(labels.isNotEmpty(), "Should have labeled while loop")
        val breaks = body.allChildren<BreakStatement>()
        assertTrue(breaks.any { it.label == "outer" }, "Should have break 'outer")
    }

    @Test
    fun testBranchLabeledFor() {
        val tu = parseTU("branch_coverage_edge_cases.rs")
        assertNotNull(tu)
        val func = tu.functions["test_labeled_for"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val labels = body.allChildren<LabelStatement>()
        assertTrue(labels.isNotEmpty(), "Should have labeled for loop")
    }

    @Test
    fun testBranchLoopWithBreakValue() {
        val tu = parseTU("branch_coverage_targeted.rs")
        assertNotNull(tu)
        val func = tu.functions["test_loop_with_break_value"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val allStmts = body.allChildren<Statement>()
        assertTrue(allStmts.isNotEmpty(), "Should have statements from loop with break value")
    }

    @Test
    fun testBranchLabeledLoopBreak() {
        val tu = parseTU("branch_coverage_targeted.rs")
        assertNotNull(tu)
        val func = tu.functions["test_labeled_loop_break"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val allStmts = body.allChildren<Statement>()
        assertTrue(allStmts.isNotEmpty(), "Should have statements from labeled loop")
    }

    @Test
    fun testBranchIfLet() {
        val tu = parseTU("branch_coverage_targeted.rs")
        assertNotNull(tu)
        val func = tu.functions["test_if_let"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val condExprs = body.allChildren<ConditionalExpression>()
        assertTrue(condExprs.isNotEmpty(), "Should have if-let conditional expression")
    }

    @Test
    fun testBranchIfLetChain() {
        val tu = parseTU("branch_coverage_targeted.rs")
        assertNotNull(tu)
        val func = tu.functions["test_if_let_chain"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val condExprs = body.allChildren<ConditionalExpression>()
        assertTrue(condExprs.size >= 2, "Should have nested if-let conditionals: ${condExprs.size}")
    }

    @Test
    fun testBranchContinueWithLabel() {
        val tu = parseTU("branch_coverage_targeted.rs")
        assertNotNull(tu)
        val func = tu.functions["test_continue_with_label"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val continues = body.allChildren<ContinueStatement>()
        assertTrue(continues.any { it.label == "outer" }, "Should have continue 'outer")
    }

    @Test
    fun testBranchAsyncFunction() {
        val tu = parseTU("branch_coverage_edge_cases.rs")
        assertNotNull(tu)
        val func = tu.functions["async_helper"]
        assertNotNull(func, "Should have async_helper function")
    }

    @Test
    fun testBranchAsyncAwait() {
        val tu = parseTU("branch_coverage_edge_cases.rs")
        assertNotNull(tu)
        val func = tu.functions["test_async_await"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val unaryOps = body.allChildren<UnaryOperator>()
        assertTrue(unaryOps.any { it.operatorCode == "await" }, "Should have await operator")
    }

    @Test
    fun testBranchAsyncExpr() {
        val tu = parseTU("branch_coverage_targeted.rs")
        assertNotNull(tu)
        val func = tu.functions["test_async_expr"]
        assertNotNull(func)
    }

    @Test
    fun testBranchLetWhileValue() {
        val tu = parseTU("branch_coverage_targeted.rs")
        assertNotNull(tu)
        val func = tu.functions["test_let_while_value"]
        assertNotNull(func)
    }

    @Test
    fun testBranchLetForValue() {
        val tu = parseTU("branch_coverage_targeted.rs")
        assertNotNull(tu)
        val func = tu.functions["test_let_for_value"]
        assertNotNull(func)
    }

    @Test
    fun testBranchExpressionStatementVariants() {
        val tu = parseTU("branch_coverage_targeted.rs")
        assertNotNull(tu)
        val func = tu.functions["test_expression_statement_variants"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val assigns = body.allChildren<AssignExpression>()
        assertTrue(assigns.size >= 2, "Should have assignment statements")
    }

    @Test
    fun testBranchMultipleReturns() {
        val tu = parseTU("branch_coverage_targeted.rs")
        assertNotNull(tu)
        val func = tu.functions["test_multiple_returns"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val returns = body.allChildren<ReturnStatement>()
        assertTrue(returns.size >= 2, "Should have 2+ return statements")
    }

    @Test
    fun testBranchUnsafeBlock() {
        val tu = parseTU("branch_coverage_edge_cases.rs")
        assertNotNull(tu)
        val func = tu.functions["test_unsafe_block"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val blocks = body.allChildren<Block>()
        assertTrue(
            blocks.any { block -> block.annotations.any { it.name.localName == "unsafe" } },
            "Should have unsafe block",
        )
    }

    @Test
    fun testBranchAsyncBlock() {
        val tu = parseTU("branch_coverage_edge_cases.rs")
        assertNotNull(tu)
        val func = tu.functions["test_async_block"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val blocks = body.allChildren<Block>()
        assertTrue(
            blocks.any { block -> block.annotations.any { it.name.localName == "async" } },
            "Should have async block",
        )
    }

    @Test
    fun testDeepIfLet() {
        val tu = parseTU("if_expressions_deep.rs")
        assertNotNull(tu)
        val func = tu.functions["test_if_let"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val condExprs = body.allChildren<ConditionalExpression>()
        assertTrue(condExprs.isNotEmpty(), "Should have if-let conditional expression")
    }

    @Test
    fun testDeepIfLetNoElse() {
        val tu = parseTU("if_expressions_deep.rs")
        assertNotNull(tu)
        val func = tu.functions["test_if_let_no_else"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val ifStmts = body.allChildren<IfStatement>()
        assertTrue(ifStmts.isNotEmpty(), "Should have if-let statement")
    }

    @Test
    fun testDeepIfElseIf() {
        val tu = parseTU("if_expressions_deep.rs")
        assertNotNull(tu)
        val func = tu.functions["test_if_else_if"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val condExprs = body.allChildren<ConditionalExpression>()
        assertTrue(condExprs.size >= 2, "Should have nested if-else-if chain")
    }

    @Test
    fun testDeepIfSimple() {
        val tu = parseTU("if_expressions_deep.rs")
        assertNotNull(tu)
        val func = tu.functions["test_if_simple"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val condExprs = body.allChildren<ConditionalExpression>()
        assertTrue(condExprs.isNotEmpty(), "Should have conditional expression")
    }

    @Test
    fun testDeepLabeledLoops() {
        val tu = parseTU("loops_labeled.rs")
        assertNotNull(tu)
        val func = tu.functions["test_labeled_loops"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val breaks = body.allChildren<BreakStatement>()
        val continues = body.allChildren<ContinueStatement>()
        assertTrue(breaks.isNotEmpty(), "Should have break statement")
        assertTrue(continues.isNotEmpty(), "Should have continue statement")
    }

    @Test
    fun testDeepLoopWithBreak() {
        val tu = parseTU("loops_labeled.rs")
        assertNotNull(tu)
        val func = tu.functions["test_loop_with_break"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val whiles = body.allChildren<WhileStatement>()
        assertTrue(whiles.isNotEmpty(), "Loop should produce WhileStatement")
    }

    @Test
    fun testDeepWhileCondition() {
        val tu = parseTU("loops_labeled.rs")
        assertNotNull(tu)
        val func = tu.functions["test_while_condition"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)
        val whiles = body.allChildren<WhileStatement>()
        assertTrue(whiles.isNotEmpty(), "Should have while loop")
        assertNotNull(whiles.first().condition, "While should have condition")
    }
}
