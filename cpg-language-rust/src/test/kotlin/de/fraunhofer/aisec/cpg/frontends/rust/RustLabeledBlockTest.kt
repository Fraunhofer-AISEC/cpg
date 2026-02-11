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
package de.fraunhofer.aisec.cpg.frontends.rust

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.nio.file.Path
import kotlin.test.*

class RustLabeledBlockTest : BaseTest() {
    @Test
    fun testLabeledBlock() {
        val topLevel = Path.of("src", "test", "resources", "rust")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("labeled_block.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
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
