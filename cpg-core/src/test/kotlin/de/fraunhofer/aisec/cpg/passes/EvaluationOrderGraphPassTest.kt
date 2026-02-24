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

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CollectionComprehension
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.test.GraphExamples
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EvaluationOrderGraphPassTest {

    @Test
    fun testWhileStatement() {
        val whileTest = GraphExamples.getWhileWithElseAndBreak()

        val whileStmt = whileTest.whileLoops.firstOrNull()
        assertNotNull(whileStmt)

        val breakStmt = whileStmt.breaks.firstOrNull()
        assertNotNull(breakStmt)

        val elseCall = whileTest.calls["elseCall"]
        assertNotNull(elseCall)

        val postWhile = whileTest.calls["postWhile"]
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

        val doStmt = doTest.doLoops.firstOrNull()
        assertNotNull(doStmt)

        val breakStmt = doStmt.breaks.firstOrNull()
        assertNotNull(breakStmt)

        val elseCall = doTest.calls["elseCall"]
        assertNotNull(elseCall)

        val postWhile = doTest.calls["postDo"]
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

        val forStmt = forTest.forLoops.firstOrNull()
        assertNotNull(forStmt)

        val breakStmt = forStmt.breaks.firstOrNull()
        assertNotNull(breakStmt)

        val elseCall = forTest.calls["elseCall"]
        assertNotNull(elseCall)

        val postFor = forTest.calls["postFor"]
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

        val forEachStmt = forTest.forEachLoops.firstOrNull()
        assertNotNull(forEachStmt)

        val breakStmt = forTest.breaks.firstOrNull()
        assertNotNull(breakStmt)

        val elseCall = forTest.calls["elseCall"]
        assertNotNull(elseCall)

        val postForEach = forTest.calls["postForEach"]
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
        val compExample = GraphExamples.getNestedComprehensions()

        val listComp = compExample.allChildren<CollectionComprehension>().first()
        assertNotNull(listComp)

        val preCall = compExample.calls["preComprehensions"]
        assertNotNull(preCall)

        val postCall = compExample.calls["postComprehensions"]
        assertNotNull(postCall)

        assertTrue { listComp.comprehensionExpressions.size == 2 }

        val outerComprehension = listComp.comprehensionExpressions.first()
        assertNotNull(outerComprehension)

        val innerComprehension = listComp.comprehensionExpressions.last()
        assertNotNull(innerComprehension)

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
                startNode = outerComprehension,
                endNodes = listOf(innerComprehension, listComp, outerComprehension.variable),
                connectEnd = Util.Connect.SUBTREE,
            )
        )
        assertTrue(
            Util.eogConnect(
                quantifier = Util.Quantifier.ANY,
                edgeDirection = Util.Edge.EXITS,
                startNode = outerComprehension,
                endNodes = listOf(innerComprehension),
                connectEnd = Util.Connect.SUBTREE,
                predicate = { it.branch == true },
            )
        )

        assertTrue(
            Util.eogConnect(
                quantifier = Util.Quantifier.ANY,
                edgeDirection = Util.Edge.EXITS,
                startNode = outerComprehension,
                endNodes = listOf(listComp),
                connectEnd = Util.Connect.SUBTREE,
                predicate = { it.branch == false },
            )
        )

        assertTrue(
            Util.eogConnect(
                edgeDirection = Util.Edge.EXITS,
                startNode = innerComprehension,
                endNodes = listOf(outerComprehension, listComp.statement),
                connectEnd = Util.Connect.SUBTREE,
            )
        )

        assertTrue(
            Util.eogConnect(
                quantifier = Util.Quantifier.ANY,
                edgeDirection = Util.Edge.EXITS,
                startNode = innerComprehension,
                endNodes = listOf(listComp.statement),
                connectEnd = Util.Connect.SUBTREE,
                predicate = { it.branch == true },
            )
        )

        assertTrue(
            Util.eogConnect(
                quantifier = Util.Quantifier.ANY,
                edgeDirection = Util.Edge.EXITS,
                startNode = innerComprehension,
                endNodes = listOf(outerComprehension),
                connectEnd = Util.Connect.SUBTREE,
                predicate = { it.branch == false },
            )
        )

        assertTrue(
            Util.eogConnect(
                quantifier = Util.Quantifier.ANY,
                edgeDirection = Util.Edge.EXITS,
                startNode = outerComprehension.iterable,
                endNodes = listOf(outerComprehension.variable),
                connectEnd = Util.Connect.SUBTREE,
                predicate = { it.branch == true },
            )
        )

        assertTrue(
            Util.eogConnect(
                quantifier = Util.Quantifier.ANY,
                edgeDirection = Util.Edge.EXITS,
                startNode = innerComprehension.iterable,
                endNodes = listOf(innerComprehension.variable),
                connectEnd = Util.Connect.SUBTREE,
                predicate = { it.branch == true },
            )
        )
    }
}
