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
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.nio.file.Path
import kotlin.test.*

class RustBlockExprTest : BaseTest() {
    @Test
    fun testBlockAsExpression() {
        val topLevel = Path.of("src", "test", "resources", "rust", "expressions")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("block_expr.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val func = tu.functions["test_block_expr"]
        assertNotNull(func)
        val body = func.body as? Block
        assertNotNull(body)

        // The block expression { let temp = 10; temp + 1 } should not produce ProblemExpressions
        // for the trailing expression `temp + 1`
        val problems = body.allChildren<ProblemExpression>()
        assertTrue(
            problems.none { it.problem.contains("Unknown statement type") },
            "Block trailing expression should not produce ProblemExpression: ${problems.map { it.problem }}",
        )

        // The inner block should contain a BinaryOperator for `temp + 1`
        val innerBlocks = body.allChildren<Block>().filter { it !== body }
        assertTrue(innerBlocks.isNotEmpty(), "Should have inner block expression")
        val binOps = innerBlocks.flatMap { it.allChildren<BinaryOperator>() }
        assertTrue(
            binOps.any { it.operatorCode == "+" },
            "Inner block should contain temp + 1 as trailing expression",
        )
    }
}
