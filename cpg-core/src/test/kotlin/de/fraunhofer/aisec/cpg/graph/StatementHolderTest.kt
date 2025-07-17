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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.GraphExamples
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class StatementHolderTest {

    @Test
    fun testStatementsAndEdgesForEach() {

        val forEachStatement = GraphExamples.getForEachWithElseAndBreak()

        val forEachStmt = forEachStatement.dForEachLoops.firstOrNull()
        assertNotNull(forEachStmt)

        val statements = forEachStmt.statements
        assertEquals(statements.size, forEachStmt.statementEdges.map { it.end }.distinct().size)

        forEachStmt.statementEdges.map { it.end }.forEach { assertContains(statements, it) }
    }

    @Test
    fun testStatementsAndEdgesFor() {

        val forStatement = GraphExamples.getForWithElseAndBreak()

        val forStmt = forStatement.dForLoops.firstOrNull()
        assertNotNull(forStmt)

        val statements = forStmt.statements
        assertEquals(statements.size, forStmt.statementEdges.map { it.end }.distinct().size)

        forStmt.statementEdges.map { it.end }.forEach { assertContains(statements, it) }
    }

    @Test
    fun testStatementsAndEdgesLabel() {

        val labelStatement = GraphExamples.getLabeledBreakContinueLoopDFG()

        val labelStmt = labelStatement.dLabels.firstOrNull()
        assertNotNull(labelStmt)

        val statements = labelStmt.statements
        assertEquals(
            labelStmt.statements.size,
            labelStmt.statementEdges.map { it.end }.distinct().size,
        )

        labelStmt.statementEdges.map { it.end }.forEach { assertContains(statements, it) }
    }
}
