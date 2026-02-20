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
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.nio.file.Path
import kotlin.test.*

class RustBlocksTest : BaseTest() {

    private val topLevel = Path.of("src", "test", "resources", "rust", "control_flow")

    private fun parseTU(file: String) =
        analyzeAndGetFirstTU(listOf(topLevel.resolve(file).toFile()), topLevel, true) {
            it.registerLanguage<RustLanguage>()
        }

    @Test
    fun testUnsafeBlock() {
        val tu = parseTU("blocks.rs")
        assertNotNull(tu)

        val func = tu.functions["test_unsafe"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val statements = body.statements
        assertNotNull(statements.getOrNull(0), "Should have statement for let x = unsafe { 42 }")

        // The unsafe block should not produce a ProblemExpression
        val problems =
            func.allChildren<ProblemExpression>().filter { it.problem.contains("unsafe") }
        assertTrue(
            problems.isEmpty(),
            "unsafe block should be handled, not produce ProblemExpression",
        )

        // The unsafe block should produce a Block with "unsafe" annotation
        val unsafeBlocks =
            func.allChildren<Block>().filter { block ->
                block.annotations.any { it.name.localName == "unsafe" }
            }
        assertTrue(unsafeBlocks.isNotEmpty(), "Should have a block with unsafe annotation")
    }

    @Test
    fun testAsyncBlock() {
        val tu = parseTU("blocks.rs")
        assertNotNull(tu)

        val func = tu.functions["test_async_block"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        val statements = body.statements
        assertNotNull(statements.getOrNull(0), "Should have statement for let fut = async { 42 }")

        // The async block should not produce a ProblemExpression
        val problems = func.allChildren<ProblemExpression>().filter { it.problem.contains("async") }
        assertTrue(
            problems.isEmpty(),
            "async block should be handled, not produce ProblemExpression",
        )

        // The async block should produce a Block with "async" annotation
        val asyncBlocks =
            func.allChildren<Block>().filter { block ->
                block.annotations.any { it.name.localName == "async" }
            }
        assertTrue(asyncBlocks.isNotEmpty(), "Should have a block with async annotation")
    }

    @Test
    fun testImplicitReturn() {
        val tu = parseTU("blocks.rs")
        assertNotNull(tu)

        val func = tu.functions["implicit_return"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        // The function body should contain a statement (the implicit return value 42)
        assertTrue(body.statements.isNotEmpty(), "Function body should have statements")
    }

    @Test
    fun testLabeledBlock() {
        val tu = parseTU("labeled_block.rs")
        assertNotNull(tu)

        // Test labeled block used as a statement (test_labeled_block_stmt)
        val stmtFunc = tu.functions["test_labeled_block_stmt"]
        assertNotNull(stmtFunc, "Should find test_labeled_block_stmt function")
        val stmtBody = stmtFunc.body as? Block
        assertNotNull(stmtBody)

        // No ProblemExpressions with "Unknown" in the function body
        val stmtProblems = stmtBody.allChildren<ProblemExpression>()
        assertTrue(
            stmtProblems.none { it.problem.contains("Unknown") },
            "Labeled block should not produce ProblemExpression with 'Unknown': ${stmtProblems.map { it.problem }}",
        )

        // A LabelStatement should exist with label "block" (without the leading ')
        val labelStmts = stmtBody.allChildren<LabelStatement>()
        assertTrue(labelStmts.isNotEmpty(), "Should have a LabelStatement for the labeled block")
        val labelStmt = labelStmts.first()
        assertEquals("block", labelStmt.label, "Label should be 'block' without the leading quote")

        // The LabelStatement's subStatement should be a Block
        val sub = labelStmt.subStatement
        assertIs<Block>(sub, "LabelStatement subStatement should be a Block")

        // Test labeled block used as an expression in a let binding (test_labeled_block)
        val exprFunc = tu.functions["test_labeled_block"]
        assertNotNull(exprFunc, "Should find test_labeled_block function")
        val exprBody = exprFunc.body as? Block
        assertNotNull(exprBody)

        // No ProblemExpressions with "Unknown"
        val exprProblems = exprBody.allChildren<ProblemExpression>()
        assertTrue(
            exprProblems.none { it.problem.contains("Unknown") },
            "Labeled block expr should not produce ProblemExpression with 'Unknown': ${exprProblems.map { it.problem }}",
        )

        // A LabelStatement should exist with label "outer"
        val exprLabelStmts = exprBody.allChildren<LabelStatement>()
        assertTrue(
            exprLabelStmts.isNotEmpty(),
            "Should have a LabelStatement for the labeled block expression",
        )
        val exprLabelStmt = exprLabelStmts.first()
        assertEquals(
            "outer",
            exprLabelStmt.label,
            "Label should be 'outer' without the leading quote",
        )
    }
}
