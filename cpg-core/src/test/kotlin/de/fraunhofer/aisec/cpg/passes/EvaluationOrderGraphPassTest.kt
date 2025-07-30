/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.GraphExamples
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CollectionComprehension
import de.fraunhofer.aisec.cpg.helpers.Util
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EvaluationOrderGraphPassTest {

    @Test
    fun testWhileStatement() {
        val whileTest = GraphExamples.getWhileWithElseAndBreak()

        val whileStmt = whileTest.allWhileLoops.firstOrNull()
        assertNotNull(whileStmt)

        val breakStmt = whileStmt.allBreaks.firstOrNull()
        assertNotNull(breakStmt)

        val elseCall = whileTest.allCalls["elseCall"]
        assertNotNull(elseCall)

        val postWhile = whileTest.allCalls["postWhile"]
        assertNotNull(postWhile)

        assertTrue(
            Util.eogConnect(
                edgeDirection = Util.Edge.ENTRIES,
                startNode = elseCall,
                endNodes = listOf(whileStmt),
                connectEnd = Util.Connect.NODE,
            )
        )
        assertTrue(
            Util.eogConnect(
                edgeDirection = Util.Edge.ENTRIES,
                startNode = postWhile,
                endNodes = listOf(whileStmt.elseStatement, breakStmt),
                connectEnd = Util.Connect.NODE,
            )
        )
        assertTrue(
            Util.eogConnect(
                edgeDirection = Util.Edge.EXITS,
                startNode = whileStmt.elseStatement,
                endNodes = listOf(postWhile),
                connectEnd = Util.Connect.SUBTREE,
            )
        )
        assertTrue(
            Util.eogConnect(
                edgeDirection = Util.Edge.EXITS,
                startNode = breakStmt,
                endNodes = listOf(postWhile),
                connectEnd = Util.Connect.SUBTREE,
            )
        )
    }

    @Test
    fun testDoStatement() {
        val doTest = GraphExamples.getDoWithElseAndBreak()

        val doStmt = doTest.allDoLoops.firstOrNull()
        assertNotNull(doStmt)

        val breakStmt = doStmt.allBreaks.firstOrNull()
        assertNotNull(breakStmt)

        val elseCall = doTest.allCalls["elseCall"]
        assertNotNull(elseCall)

        val postWhile = doTest.allCalls["postDo"]
        assertNotNull(postWhile)

        assertTrue(
            Util.eogConnect(
                edgeDirection = Util.Edge.ENTRIES,
                startNode = elseCall,
                endNodes = listOf(doStmt),
                connectEnd = Util.Connect.NODE,
            )
        )
        assertTrue(
            Util.eogConnect(
                edgeDirection = Util.Edge.ENTRIES,
                startNode = postWhile,
                endNodes = listOf(doStmt.elseStatement, breakStmt),
                connectEnd = Util.Connect.NODE,
            )
        )
        assertTrue(
            Util.eogConnect(
                edgeDirection = Util.Edge.EXITS,
                startNode = doStmt.elseStatement,
                endNodes = listOf(postWhile),
                connectEnd = Util.Connect.SUBTREE,
            )
        )
        assertTrue(
            Util.eogConnect(
                edgeDirection = Util.Edge.EXITS,
                startNode = breakStmt,
                endNodes = listOf(postWhile),
                connectEnd = Util.Connect.SUBTREE,
            )
        )
    }

    @Test
    fun testForStatement() {
        val forTest = GraphExamples.getForWithElseAndBreak()

        val forStmt = forTest.allForLoops.firstOrNull()
        assertNotNull(forStmt)

        val breakStmt = forStmt.allBreaks.firstOrNull()
        assertNotNull(breakStmt)

        val elseCall = forTest.allCalls["elseCall"]
        assertNotNull(elseCall)

        val postFor = forTest.allCalls["postFor"]
        assertNotNull(postFor)

        assertTrue(
            Util.eogConnect(
                edgeDirection = Util.Edge.ENTRIES,
                startNode = elseCall,
                endNodes = listOf(forStmt),
                connectEnd = Util.Connect.NODE,
            )
        )
        assertTrue(
            Util.eogConnect(
                edgeDirection = Util.Edge.ENTRIES,
                startNode = postFor,
                endNodes = listOf(forStmt.elseStatement, breakStmt),
                connectEnd = Util.Connect.NODE,
            )
        )
        assertTrue(
            Util.eogConnect(
                edgeDirection = Util.Edge.EXITS,
                startNode = forStmt.elseStatement,
                endNodes = listOf(postFor),
                connectEnd = Util.Connect.SUBTREE,
            )
        )
        assertTrue(
            Util.eogConnect(
                edgeDirection = Util.Edge.EXITS,
                startNode = breakStmt,
                endNodes = listOf(postFor),
                connectEnd = Util.Connect.SUBTREE,
            )
        )
    }

    @Test
    fun testForEachStatement() {
        val forTest = GraphExamples.getForEachWithElseAndBreak()

        val forEachStmt = forTest.allForEachLoops.firstOrNull()
        assertNotNull(forEachStmt)

        val breakStmt = forTest.allBreaks.firstOrNull()
        assertNotNull(breakStmt)

        val elseCall = forTest.allCalls["elseCall"]
        assertNotNull(elseCall)

        val postForEach = forTest.allCalls["postForEach"]
        assertNotNull(postForEach)

        assertTrue(
            Util.eogConnect(
                edgeDirection = Util.Edge.ENTRIES,
                startNode = elseCall,
                endNodes = listOf(forEachStmt),
                connectEnd = Util.Connect.NODE,
            )
        )
        assertTrue(
            Util.eogConnect(
                edgeDirection = Util.Edge.ENTRIES,
                startNode = postForEach,
                endNodes = listOf(forEachStmt.elseStatement, breakStmt),
                connectEnd = Util.Connect.NODE,
            )
        )
        assertTrue(
            Util.eogConnect(
                edgeDirection = Util.Edge.EXITS,
                startNode = forEachStmt.elseStatement,
                endNodes = listOf(postForEach),
                connectEnd = Util.Connect.SUBTREE,
            )
        )
        assertTrue(
            Util.eogConnect(
                edgeDirection = Util.Edge.EXITS,
                startNode = breakStmt,
                endNodes = listOf(postForEach),
                connectEnd = Util.Connect.SUBTREE,
            )
        )
    }

    @Test
    fun testCollectionComprehensionStatement() {
        val compExample = GraphExamples.getNestedComprehensionExpressions()

        val listComp = compExample.allDescendants<CollectionComprehension>().first()
        assertNotNull(listComp)

        val preCall = compExample.allCalls["preComprehensions"]
        assertNotNull(preCall)

        val postCall = compExample.allCalls["postComprehensions"]
        assertNotNull(postCall)

        assertTrue { listComp.comprehensionExpressions.size == 2 }

        val outerComprehensionExpression = listComp.comprehensionExpressions.first()
        assertNotNull(outerComprehensionExpression)

        val innerComprehensionExpression = listComp.comprehensionExpressions.last()
        assertNotNull(innerComprehensionExpression)

        assertTrue(
            Util.eogConnect(
                edgeDirection = Util.Edge.EXITS,
                startNode = preCall,
                endNodes = listOf(listComp),
                connectEnd = Util.Connect.SUBTREE,
            )
        )
        assertTrue(
            Util.eogConnect(
                edgeDirection = Util.Edge.EXITS,
                startNode = listComp,
                endNodes = listOf(postCall),
                connectEnd = Util.Connect.SUBTREE,
            )
        )
        assertTrue(
            Util.eogConnect(
                edgeDirection = Util.Edge.EXITS,
                startNode = outerComprehensionExpression,
                endNodes =
                    listOf(
                        innerComprehensionExpression,
                        listComp,
                        outerComprehensionExpression.variable,
                    ),
                connectEnd = Util.Connect.SUBTREE,
            )
        )
        assertTrue(
            Util.eogConnect(
                quantifier = Util.Quantifier.ANY,
                edgeDirection = Util.Edge.EXITS,
                startNode = outerComprehensionExpression,
                endNodes = listOf(innerComprehensionExpression),
                connectEnd = Util.Connect.SUBTREE,
                predicate = { it.branch == true },
            )
        )

        assertTrue(
            Util.eogConnect(
                quantifier = Util.Quantifier.ANY,
                edgeDirection = Util.Edge.EXITS,
                startNode = outerComprehensionExpression,
                endNodes = listOf(listComp),
                connectEnd = Util.Connect.SUBTREE,
                predicate = { it.branch == false },
            )
        )

        assertTrue(
            Util.eogConnect(
                edgeDirection = Util.Edge.EXITS,
                startNode = innerComprehensionExpression,
                endNodes = listOf(outerComprehensionExpression, listComp.statement),
                connectEnd = Util.Connect.SUBTREE,
            )
        )

        assertTrue(
            Util.eogConnect(
                quantifier = Util.Quantifier.ANY,
                edgeDirection = Util.Edge.EXITS,
                startNode = innerComprehensionExpression,
                endNodes = listOf(listComp.statement),
                connectEnd = Util.Connect.SUBTREE,
                predicate = { it.branch == true },
            )
        )

        assertTrue(
            Util.eogConnect(
                quantifier = Util.Quantifier.ANY,
                edgeDirection = Util.Edge.EXITS,
                startNode = innerComprehensionExpression,
                endNodes = listOf(outerComprehensionExpression),
                connectEnd = Util.Connect.SUBTREE,
                predicate = { it.branch == false },
            )
        )

        assertTrue(
            Util.eogConnect(
                quantifier = Util.Quantifier.ANY,
                edgeDirection = Util.Edge.EXITS,
                startNode = outerComprehensionExpression.iterable,
                endNodes = listOf(outerComprehensionExpression.variable),
                connectEnd = Util.Connect.SUBTREE,
                predicate = { it.branch == true },
            )
        )

        assertTrue(
            Util.eogConnect(
                quantifier = Util.Quantifier.ANY,
                edgeDirection = Util.Edge.EXITS,
                startNode = innerComprehensionExpression.iterable,
                endNodes = listOf(innerComprehensionExpression.variable),
                connectEnd = Util.Connect.SUBTREE,
                predicate = { it.branch == true },
            )
        )
    }
}
