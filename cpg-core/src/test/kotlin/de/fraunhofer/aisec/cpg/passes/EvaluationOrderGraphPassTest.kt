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
import de.fraunhofer.aisec.cpg.graph.callsByName
import de.fraunhofer.aisec.cpg.graph.statements
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.helpers.Util
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EvaluationOrderGraphPassTest {

    @Test
    fun testWhileStatement() {
        val whileTest = GraphExamples.getWhileWithElseAndBreak()

        val whileStmt = whileTest.statements.filterIsInstance<WhileStatement>().firstOrNull()
        assertNotNull(whileStmt)
        val breakStmt = whileStmt.statements.filterIsInstance<BreakStatement>().firstOrNull()
        val elseCall = whileTest.callsByName("elseCall").getOrNull(0)
        val postWhile = whileTest.callsByName("postWhile").getOrNull(0)
        assertNotNull(breakStmt)
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

        val doStmt = doTest.statements.filterIsInstance<DoStatement>().firstOrNull()
        assertNotNull(doStmt)
        val breakStmt = doStmt.statements.filterIsInstance<BreakStatement>().firstOrNull()
        val elseCall = doTest.callsByName("elseCall").getOrNull(0)
        val postWhile = doTest.callsByName("postDo").getOrNull(0)
        assertNotNull(breakStmt)
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

        val forStmt = forTest.statements.filterIsInstance<ForStatement>().firstOrNull()
        assertNotNull(forStmt)
        val breakStmt = forStmt.statements.filterIsInstance<BreakStatement>().firstOrNull()
        val elseCall = forTest.callsByName("elseCall").getOrNull(0)
        val postFor = forTest.callsByName("postFor").getOrNull(0)
        assertNotNull(breakStmt)
        Util.eogConnect(
            en = Util.Edge.ENTRIES,
            n = elseCall,
            refs = listOf(forStmt),
            cr = Util.Connect.NODE
        )
        Util.eogConnect(
            en = Util.Edge.ENTRIES,
            n = postFor,
            refs = listOf(forStmt.elseStatement, breakStmt),
            cr = Util.Connect.NODE
        )
        Util.eogConnect(
            en = Util.Edge.EXITS,
            n = forStmt.elseStatement,
            refs = listOf(postFor),
            cr = Util.Connect.SUBTREE
        )
        Util.eogConnect(
            en = Util.Edge.EXITS,
            n = breakStmt,
            refs = listOf(postFor),
            cr = Util.Connect.SUBTREE
        )
    }

    @Test
    fun testForEachStatement() {
        val forTest = GraphExamples.getForEachWithElseAndBreak()

        val forEachStmt = forTest.statements.filterIsInstance<ForEachStatement>().firstOrNull()
        assertNotNull(forEachStmt)
        val breakStmt = forTest.statements.filterIsInstance<BreakStatement>().firstOrNull()
        val elseCall = forTest.callsByName("elseCall").getOrNull(0)
        val postForEach = forTest.callsByName("postForEach").getOrNull(0)
        assertNotNull(breakStmt)
        Util.eogConnect(
            en = Util.Edge.ENTRIES,
            n = elseCall,
            refs = listOf(forEachStmt),
            cr = Util.Connect.NODE
        )
        Util.eogConnect(
            en = Util.Edge.ENTRIES,
            n = postForEach,
            refs = listOf(forEachStmt.elseStatement, breakStmt),
            cr = Util.Connect.NODE
        )
        Util.eogConnect(
            en = Util.Edge.EXITS,
            n = forEachStmt.elseStatement,
            refs = listOf(postForEach),
            cr = Util.Connect.SUBTREE
        )
        Util.eogConnect(
            en = Util.Edge.EXITS,
            n = breakStmt,
            refs = listOf(postForEach),
            cr = Util.Connect.SUBTREE
        )
    }
}
