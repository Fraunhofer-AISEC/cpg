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
import de.fraunhofer.aisec.cpg.graph.dFunctions
import de.fraunhofer.aisec.cpg.graph.dStatements
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class LoopTest {

    @Test
    fun testWhileStatement() {
        val whileTest = GraphExamples.getWhileWithElseAndBreak()
        val func = whileTest.dFunctions["someRecord.func"]
        assertNotNull(func)

        val whileStmt = whileTest.dWhileLoops.firstOrNull()
        assertNotNull(whileStmt)
        assertContains(func.body.dStatements, whileStmt)
        whileStmt.astChildren.forEach { assertContains(whileStmt.toString(), it.toString()) }
        val secondWhileStmt = whileTest.dWhileLoops.lastOrNull()
        assertNotNull(secondWhileStmt)
        assertNotEquals(whileStmt, secondWhileStmt)

        val breakStmt = whileStmt.dBreaks.firstOrNull()
        assertNotNull(breakStmt)

        val elseCall = whileTest.dCalls["elseCall"]
        assertNotNull(elseCall)

        val postWhile = whileTest.dCalls["postWhile"]
        assertNotNull(postWhile)
    }

    @Test
    fun testDoStatement() {
        val doTest = GraphExamples.getDoWithElseAndBreak()
        val func = doTest.dFunctions["someRecord.func"]
        assertNotNull(func)

        val doStmt = doTest.dDoLoops.firstOrNull()
        assertNotNull(doStmt)
        assertContains(func.body.dStatements, doStmt)
        doStmt.astChildren.forEach { assertContains(doStmt.toString(), it.toString()) }
        val secondDoStmt = doTest.dDoLoops.lastOrNull()
        assertNotNull(secondDoStmt)
        assertNotEquals(doStmt, secondDoStmt)

        val breakStmt = doStmt.dBreaks.firstOrNull()
        assertNotNull(breakStmt)

        val elseCall = doTest.dCalls["elseCall"]
        assertNotNull(elseCall)

        val postWhile = doTest.dCalls["postDo"]
        assertNotNull(postWhile)
    }

    @Test
    fun testForStatement() {
        val forTest = GraphExamples.getForWithElseAndBreak()
        val func = forTest.dFunctions["someRecord.func"]
        assertNotNull(func)

        val forStmt = forTest.dForLoops.firstOrNull()
        assertNotNull(forStmt)
        assertContains(func.body.dStatements, forStmt)
        forStmt.astChildren.forEach { assertContains(forStmt.toString(), it.toString()) }
        val secondForStmt = forTest.dForLoops.lastOrNull()
        assertNotNull(secondForStmt)
        assertNotEquals(forStmt, secondForStmt)

        val breakStmt = forStmt.dBreaks.firstOrNull()
        assertNotNull(breakStmt)

        val elseCall = forTest.dCalls["elseCall"]
        assertNotNull(elseCall)

        val postFor = forTest.dCalls["postFor"]
        assertNotNull(postFor)
    }

    @Test
    fun testForEachStatement() {
        val forEachTest = GraphExamples.getForEachWithElseAndBreak()
        val func = forEachTest.dFunctions["someRecord.func"]
        assertNotNull(func)

        val forEachStmt = forEachTest.dForEachLoops.firstOrNull()
        assertNotNull(forEachStmt)
        assertContains(func.body.dStatements, forEachStmt)
        forEachStmt.astChildren.forEach { assertContains(forEachStmt.toString(), it.toString()) }
        val secondForEachStmt = forEachTest.dForEachLoops.lastOrNull()
        assertNotNull(secondForEachStmt)
        assertNotEquals(forEachStmt, secondForEachStmt)

        val breakStmt = forEachTest.dBreaks.firstOrNull()
        assertNotNull(breakStmt)

        val elseCall = forEachTest.dCalls["elseCall"]
        assertNotNull(elseCall)

        val postForEach = forEachTest.dCalls["postForEach"]
        assertNotNull(postForEach)
    }
}
