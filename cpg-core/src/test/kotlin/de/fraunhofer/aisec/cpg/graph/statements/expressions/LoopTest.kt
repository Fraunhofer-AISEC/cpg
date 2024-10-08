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
package de.fraunhofer.aisec.cpg.graph.statements.expressions

import de.fraunhofer.aisec.cpg.GraphExamples
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.callsByName
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.statements
import de.fraunhofer.aisec.cpg.graph.statements.*
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LoopTest {

    @Test
    fun testWhileStatement() {
        val whileTest = GraphExamples.getWhileWithElseAndBreak()
        val func = whileTest.functions["someRecord.func"]
        assertNotNull(func)
        val whileStmt = whileTest.whileLoops.firstOrNull()
        assertNotNull(whileStmt)
        assertContains(func.body.statements, whileStmt)
        val breakStmt = whileStmt.breaks.firstOrNull()
        val elseCall = whileTest.calls["elseCall"]
        val postWhile = whileTest.callsByName("postWhile").getOrNull(0)
        assertNotNull(breakStmt)
        assertNotNull(elseCall)
        assertNotNull(postWhile)
        whileStmt.astChildren.forEach {
            assertContains(whileStmt.toString(), it.toString())
        }
    }

    @Test
    fun testDoStatement() {
        val doTest = GraphExamples.getDoWithElseAndBreak()
        val func = doTest.functions["someRecord.func"]
        assertNotNull(func)
        val doStmt = doTest.doLoops.firstOrNull()
        assertNotNull(doStmt)
        assertContains(func.body.statements, doStmt)
        val breakStmt = doStmt.breaks.firstOrNull()
        val elseCall = doTest.callsByName("elseCall").getOrNull(0)
        val postWhile = doTest.callsByName("postDo").getOrNull(0)
        assertNotNull(breakStmt)
        assertNotNull(elseCall)
        assertNotNull(postWhile)
        assertTrue(doStmt.astChildren.all { doStmt.toString().contains(it.toString()) })
        doStmt.astChildren.forEach {
            assertContains(doStmt.toString(), it.toString())
        }
    }

    @Test
    fun testForStatement() {
        val forTest = GraphExamples.getForWithElseAndBreak()
        val func = forTest.functions["someRecord.func"]
        assertNotNull(func)
        val forStmt = forTest.forLoops.firstOrNull()
        assertNotNull(forStmt)
        val breakStmt = forStmt.breaks.firstOrNull()
        val elseCall = forTest.callsByName("elseCall").getOrNull(0)
        val postFor = forTest.callsByName("postFor").getOrNull(0)
        assertContains(func.body.statements, forStmt)
        assertNotNull(breakStmt)
        assertNotNull(elseCall)
        assertNotNull(postFor)
        forStmt.astChildren.forEach {
            assertContains(forStmt.toString(), it.toString())
        }
    }

    @Test
    fun testForEachStatement() {
        val forTest = GraphExamples.getForEachWithElseAndBreak()
        val func = forTest.functions["someRecord.func"]
        assertNotNull(func)
        val forEachStmt = forTest.forEachLoops.firstOrNull()
        assertNotNull(forEachStmt)
        assertContains(func.body.statements, forEachStmt)
        val breakStmt = forTest.breaks.firstOrNull()
        val elseCall = forTest.callsByName("elseCall").getOrNull(0)
        val postForEach = forTest.callsByName("postForEach").getOrNull(0)
        assertNotNull(breakStmt)
        assertNotNull(elseCall)
        assertNotNull(postForEach)
        forEachStmt.astChildren.forEach {
            assertContains(forEachStmt.toString(),it.toString())
        }
    }
}
