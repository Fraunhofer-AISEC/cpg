/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.cxx

import de.fraunhofer.aisec.cpg.graph.allChildren
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.statements.ForStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.passes.ControlDependenceGraphPass
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyze
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.assertNotNull

internal class StronglyConnectedComponentTest : BaseTest() {
    @Test
    fun testNestedLoop() {
        val file = File("src/test/resources/sccTest.cpp")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
                it.registerPass<ControlDependenceGraphPass>()
            }
        assertNotNull(result)

        val nestedFD = result.functions.singleOrNull { it.name.localName == "nested" }
        assertNotNull(nestedFD)

        for (level in 0..2) {
            val forStmt = nestedFD.allChildren<ForStatement>()[level]
            assertNotNull(forStmt)
            ///////// First, check on node-level
            // All 3 ForStatements should have one edge with an SCC of priority respective to their
            // level, and one without SCC (exiting the loop)
            assertEquals(1, forStmt.nextEOGEdges.filter { it.scc?.priority == level }.size)
            assertEquals(1, forStmt.nextEOGEdges.filter { it.scc == null }.size)

            // The respective merge points are the conditions. Those should have one incoming edge
            // with SCC-Label, and one without
            val mergeNode = (forStmt.condition as BinaryOperator).lhs
            assertNotNull(mergeNode)
            assertEquals(1, mergeNode.prevEOGEdges.filter { it.scc?.priority == level }.size)
            assertEquals(1, mergeNode.prevEOGEdges.filter { it.scc == null }.size)

            // The same applies on BB-Level
            val forLoopBlock = forStmt.basicBlock.singleOrNull()
            assertNotNull(forLoopBlock)
            // The forLoop BB has 2 next Edges. On into the loop (with SCC), and one to the outside
            assertEquals(1, forLoopBlock.nextEOGEdges.filter { it.scc?.priority == level }.size)
            assertEquals(1, forLoopBlock.nextEOGEdges.filter { it.scc == null }.size)

            val mergeBlock = mergeNode.basicBlock.singleOrNull()
            assertNotNull(mergeBlock)
            assertEquals(1, mergeBlock.prevEOGEdges.filter { it.scc?.priority == level }.size)
            assertEquals(1, mergeBlock.prevEOGEdges.filter { it.scc == null }.size)
        }
    }
}
