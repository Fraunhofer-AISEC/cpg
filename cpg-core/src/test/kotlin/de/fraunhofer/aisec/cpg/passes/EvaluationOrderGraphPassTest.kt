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
                en = Util.Edge.ENTRIES,
                n = elseCall,
                refs = listOf(whileStmt),
                cr = Util.Connect.NODE
            )
        )
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.ENTRIES,
                n = postWhile,
                refs = listOf(whileStmt.elseStatement, breakStmt),
                cr = Util.Connect.NODE
            )
        )
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.EXITS,
                n = whileStmt.elseStatement,
                refs = listOf(postWhile),
                cr = Util.Connect.SUBTREE
            )
        )
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.EXITS,
                n = breakStmt,
                refs = listOf(postWhile),
                cr = Util.Connect.SUBTREE
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
                en = Util.Edge.ENTRIES,
                n = elseCall,
                refs = listOf(doStmt),
                cr = Util.Connect.NODE
            )
        )
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.ENTRIES,
                n = postWhile,
                refs = listOf(doStmt.elseStatement, breakStmt),
                cr = Util.Connect.NODE
            )
        )
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.EXITS,
                n = doStmt.elseStatement,
                refs = listOf(postWhile),
                cr = Util.Connect.SUBTREE
            )
        )
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.EXITS,
                n = breakStmt,
                refs = listOf(postWhile),
                cr = Util.Connect.SUBTREE
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
                en = Util.Edge.ENTRIES,
                n = elseCall,
                refs = listOf(forStmt),
                cr = Util.Connect.NODE
            )
        )
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.ENTRIES,
                n = postFor,
                refs = listOf(forStmt.elseStatement, breakStmt),
                cr = Util.Connect.NODE
            )
        )
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.EXITS,
                n = forStmt.elseStatement,
                refs = listOf(postFor),
                cr = Util.Connect.SUBTREE
            )
        )
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.EXITS,
                n = breakStmt,
                refs = listOf(postFor),
                cr = Util.Connect.SUBTREE
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
                en = Util.Edge.ENTRIES,
                n = elseCall,
                refs = listOf(forEachStmt),
                cr = Util.Connect.NODE
            )
        )
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.ENTRIES,
                n = postForEach,
                refs = listOf(forEachStmt.elseStatement, breakStmt),
                cr = Util.Connect.NODE
            )
        )
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.EXITS,
                n = forEachStmt.elseStatement,
                refs = listOf(postForEach),
                cr = Util.Connect.SUBTREE
            )
        )
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.EXITS,
                n = breakStmt,
                refs = listOf(postForEach),
                cr = Util.Connect.SUBTREE
            )
        )
    }

    @Test
    fun testCollectionComprehensionStatement() {
        val compExample = GraphExamples.getNestedComprehensionExpressions()

        val listComp = compExample.allChildren<CollectionComprehension>().first()
        assertNotNull(listComp)

        val preCall = compExample.calls["preComprehensions"]
        assertNotNull(preCall)

        val postCall = compExample.calls["postComprehensions"]
        assertNotNull(postCall)

        assertTrue { listComp.comprehensionExpressions.size == 2 }

        val outerComprehensionExpression = listComp.comprehensionExpressions.first()
        assertNotNull(outerComprehensionExpression)

        val innerComprehensionExpression = listComp.comprehensionExpressions.last()
        assertNotNull(innerComprehensionExpression)

        assertTrue(
            Util.eogConnect(
                en = Util.Edge.EXITS,
                n = preCall,
                refs = listOf(listComp),
                cr = Util.Connect.SUBTREE
            )
        )
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.EXITS,
                n = listComp,
                refs = listOf(postCall),
                cr = Util.Connect.SUBTREE
            )
        )
        assertTrue(
            Util.eogConnect(
                en = Util.Edge.EXITS,
                n = outerComprehensionExpression,
                refs =
                    listOf(
                        innerComprehensionExpression,
                        listComp,
                        outerComprehensionExpression.variable
                    ),
                cr = Util.Connect.SUBTREE
            )
        )
        assertTrue(
            Util.eogConnect(
                q = Util.Quantifier.ANY,
                en = Util.Edge.EXITS,
                n = outerComprehensionExpression,
                refs =
                    listOf(
                        innerComprehensionExpression,
                    ),
                cr = Util.Connect.SUBTREE,
                predicate = { it.branch == true }
            )
        )

        assertTrue(
            Util.eogConnect(
                q = Util.Quantifier.ANY,
                en = Util.Edge.EXITS,
                n = outerComprehensionExpression,
                refs = listOf(listComp),
                cr = Util.Connect.SUBTREE,
                predicate = { it.branch == false }
            )
        )

        assertTrue(
            Util.eogConnect(
                en = Util.Edge.EXITS,
                n = innerComprehensionExpression,
                refs = listOf(outerComprehensionExpression, listComp.statement),
                cr = Util.Connect.SUBTREE
            )
        )

        assertTrue(
            Util.eogConnect(
                q = Util.Quantifier.ANY,
                en = Util.Edge.EXITS,
                n = innerComprehensionExpression,
                refs = listOf(listComp.statement),
                cr = Util.Connect.SUBTREE,
                predicate = { it.branch == true }
            )
        )

        assertTrue(
            Util.eogConnect(
                q = Util.Quantifier.ANY,
                en = Util.Edge.EXITS,
                n = innerComprehensionExpression,
                refs = listOf(outerComprehensionExpression),
                cr = Util.Connect.SUBTREE,
                predicate = { it.branch == false }
            )
        )

        assertTrue(
            Util.eogConnect(
                q = Util.Quantifier.ANY,
                en = Util.Edge.EXITS,
                n = outerComprehensionExpression.iterable,
                refs = listOf(outerComprehensionExpression.variable),
                cr = Util.Connect.SUBTREE,
                predicate = { it.branch == true }
            )
        )

        assertTrue(
            Util.eogConnect(
                q = Util.Quantifier.ANY,
                en = Util.Edge.EXITS,
                n = innerComprehensionExpression.iterable,
                refs = listOf(innerComprehensionExpression.variable),
                cr = Util.Connect.SUBTREE,
                predicate = { it.branch == true }
            )
        )
    }
}
