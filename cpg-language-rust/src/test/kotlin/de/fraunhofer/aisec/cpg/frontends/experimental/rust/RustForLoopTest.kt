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

class RustForLoopTest : BaseTest() {
    @Test
    fun testForLoop() {
        val topLevel = Path.of("src", "test", "resources", "rust", "control_flow")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("for_loops.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val basicFor = tu.functions["basic_for"]
        assertNotNull(basicFor)
        val body = basicFor.body as? Block
        assertNotNull(body)

        val forEachStmt = body.allChildren<ForEachStatement>()
        assertTrue(forEachStmt.isNotEmpty(), "Should have a for-each statement")

        val forEach = forEachStmt.first()
        // variable should be a declaration of x
        val variable = forEach.variable
        assertNotNull(variable, "ForEach should have a loop variable")

        // iterable should reference items
        val iterable = forEach.iterable
        assertNotNull(iterable, "ForEach should have an iterable")

        // body should be a block
        assertNotNull(forEach.statement, "ForEach should have a body")
    }

    @Test
    fun testForLoopPattern() {
        val topLevel = Path.of("src", "test", "resources", "rust", "control_flow")
        val tu =
            analyzeAndGetFirstTU(
                listOf(topLevel.resolve("for_loops.rs").toFile()),
                topLevel,
                true,
            ) {
                it.registerLanguage<RustLanguage>()
            }
        assertNotNull(tu)

        val labeledFor = tu.functions["labeled_for"]
        assertNotNull(labeledFor)
        val body = labeledFor.body as? Block
        assertNotNull(body)

        val labelStmts = body.allChildren<LabelStatement>()
        assertTrue(labelStmts.any { it.label == "outer" }, "Should have 'outer' label")
    }
}
