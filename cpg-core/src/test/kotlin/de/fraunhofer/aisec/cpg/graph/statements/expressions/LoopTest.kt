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
import de.fraunhofer.aisec.cpg.graph.allFunctions
import de.fraunhofer.aisec.cpg.graph.allStatements
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class LoopTest {

    @Test
    fun testWhileStatement() {
        val whileTest = GraphExamples.getWhileWithElseAndBreak()
        val func = whileTest.allFunctions["someRecord.func"]
        assertNotNull(func)

        val whileStmt = whileTest.allWhileLoops.firstOrNull()
        assertNotNull(whileStmt)
        assertContains(func.body.allStatements, whileStmt)
        whileStmt.astChildren.forEach { assertContains(whileStmt.toString(), it.toString()) }
        val secondWhileStmt = whileTest.allWhileLoops.lastOrNull()
        assertNotNull(secondWhileStmt)
        assertNotEquals(whileStmt, secondWhileStmt)

        val breakStmt = whileStmt.allBreaks.firstOrNull()
        assertNotNull(breakStmt)

        val elseCall = whileTest.allCalls["elseCall"]
        assertNotNull(elseCall)

        val postWhile = whileTest.allCalls["postWhile"]
        assertNotNull(postWhile)
    }

    @Test
    fun testDoStatement() {
        val doTest = GraphExamples.getDoWithElseAndBreak()
        val func = doTest.allFunctions["someRecord.func"]
        assertNotNull(func)

        val doStmt = doTest.allDoLoops.firstOrNull()
        assertNotNull(doStmt)
        assertContains(func.body.allStatements, doStmt)
        doStmt.astChildren.forEach { assertContains(doStmt.toString(), it.toString()) }
        val secondDoStmt = doTest.allDoLoops.lastOrNull()
        assertNotNull(secondDoStmt)
        assertNotEquals(doStmt, secondDoStmt)

        val breakStmt = doStmt.allBreaks.firstOrNull()
        assertNotNull(breakStmt)

        val elseCall = doTest.allCalls["elseCall"]
        assertNotNull(elseCall)

        val postWhile = doTest.allCalls["postDo"]
        assertNotNull(postWhile)
    }

    @Test
    fun testForStatement() {
        val forTest = GraphExamples.getForWithElseAndBreak()
        val func = forTest.allFunctions["someRecord.func"]
        assertNotNull(func)

        val forStmt = forTest.allForLoops.firstOrNull()
        assertNotNull(forStmt)
        assertContains(func.body.allStatements, forStmt)
        forStmt.astChildren.forEach { assertContains(forStmt.toString(), it.toString()) }
        val secondForStmt = forTest.allForLoops.lastOrNull()
        assertNotNull(secondForStmt)
        assertNotEquals(forStmt, secondForStmt)

        val breakStmt = forStmt.allBreaks.firstOrNull()
        assertNotNull(breakStmt)

        val elseCall = forTest.allCalls["elseCall"]
        assertNotNull(elseCall)

        val postFor = forTest.allCalls["postFor"]
        assertNotNull(postFor)
    }

    @Test
    fun testForEachStatement() {
        val forEachTest = GraphExamples.getForEachWithElseAndBreak()
        val func = forEachTest.allFunctions["someRecord.func"]
        assertNotNull(func)

        val forEachStmt = forEachTest.allForEachLoops.firstOrNull()
        assertNotNull(forEachStmt)
        assertContains(func.body.allStatements, forEachStmt)
        forEachStmt.astChildren.forEach { assertContains(forEachStmt.toString(), it.toString()) }
        val secondForEachStmt = forEachTest.allForEachLoops.lastOrNull()
        assertNotNull(secondForEachStmt)
        assertNotEquals(forEachStmt, secondForEachStmt)

        val breakStmt = forEachTest.allBreaks.firstOrNull()
        assertNotNull(breakStmt)

        val elseCall = forEachTest.allCalls["elseCall"]
        assertNotNull(elseCall)

        val postForEach = forEachTest.allCalls["postForEach"]
        assertNotNull(postForEach)
    }
}
