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
package de.fraunhofer.aisec.cpg.frontends.python

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.edges.flows.IndexedDataflowGranularity
import de.fraunhofer.aisec.cpg.graph.statements.expressions.AssignExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.InitializerListExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.test.analyze
import de.fraunhofer.aisec.cpg.test.assertLocalName
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DFGTest {
    fun checkCallFlowsToTupleElements(body: Block, functionName: String) {
        val assignment = body.statements[0]
        assertIs<AssignExpression>(assignment)
        assertEquals(1, assignment.lhs.size)
        assertEquals(1, assignment.rhs.size)
        val lhsTuple = assignment.lhs[0]
        assertIs<InitializerListExpression>(lhsTuple)
        assertEquals(2, lhsTuple.initializers.size)

        val cRef = lhsTuple.initializers[0]
        assertIs<Reference>(cRef)
        val dRef = lhsTuple.initializers[1]
        assertIs<Reference>(dRef)

        assertLocalName("c", cRef)
        val cRefPrevDFG = cRef.prevDFG.singleOrNull()
        assertIs<InitializerListExpression>(cRefPrevDFG)
        val cRefPrevDFGGranularity = cRef.prevDFGEdges.single().granularity
        assertIs<IndexedDataflowGranularity>(cRefPrevDFGGranularity)
        assertEquals(0, cRefPrevDFGGranularity.partialTarget)

        assertLocalName("d", dRef)
        val dRefPrevDFG = dRef.prevDFG.singleOrNull()
        assertSame(cRefPrevDFG, dRefPrevDFG)
        val dRefPrevDFGGranularity = dRef.prevDFGEdges.single().granularity
        assertIs<IndexedDataflowGranularity>(dRefPrevDFGGranularity)
        assertEquals(1, dRefPrevDFGGranularity.partialTarget)

        val tuplePrevDFG = cRefPrevDFG.prevDFG.singleOrNull()
        assertIs<CallExpression>(tuplePrevDFG)
        assertLocalName(functionName, tuplePrevDFG)

        val c = body.variables["c"]
        assertNotNull(c)
        assertTrue(c.prevDFG.isEmpty())

        val d = body.variables["d"]
        assertNotNull(d)
        assertTrue(d.prevDFG.isEmpty())
    }

    fun checkReturnTuple(functionDeclaration: FunctionDeclaration) {
        val returnStmt = functionDeclaration.returns.singleOrNull()
        assertNotNull(returnStmt)
        val returnVal = returnStmt.returnValue
        assertIs<InitializerListExpression>(returnVal)
        assertEquals(2, returnVal.initializers.size)
        val a = returnVal.initializers[0]
        assertIs<Reference>(a)
        val b = returnVal.initializers[1]
        assertIs<Reference>(b)
        val aNextDFGGranularity = a.nextDFGEdges.singleOrNull()?.granularity
        assertIs<IndexedDataflowGranularity>(aNextDFGGranularity)
        assertEquals(0, aNextDFGGranularity.partialTarget)
        val bNextDFGGranularity = b.nextDFGEdges.singleOrNull()?.granularity
        assertIs<IndexedDataflowGranularity>(bNextDFGGranularity)
        assertEquals(1, bNextDFGGranularity.partialTarget)
    }

    @Test
    fun testListComprehensionsBracketToBracket() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val result =
            analyze(listOf(topLevel.resolve("tuple_assign.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)
        val getTuple = result.functions["getTuple"]
        assertNotNull(getTuple)

        val body = getTuple.body
        assertIs<Block>(body)
        checkCallFlowsToTupleElements(body, "returnTuple")
    }

    @Test
    fun testListComprehensionsReturnBracket() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val result =
            analyze(listOf(topLevel.resolve("tuple_assign.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)
        val returnTuple = result.functions["returnTuple"]
        assertNotNull(returnTuple)
        checkReturnTuple(returnTuple)
    }

    @Test
    fun testListComprehensionsReturnNoBracket() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val result =
            analyze(listOf(topLevel.resolve("tuple_assign.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)
        val returnTuple = result.functions["returnTuple2"]
        assertNotNull(returnTuple)
        checkReturnTuple(returnTuple)
    }

    @Test
    fun testListComprehensionsNoBracketToNoBracket() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val result =
            analyze(listOf(topLevel.resolve("tuple_assign.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)
        val getTuple = result.functions["getTuple2"]
        assertNotNull(getTuple)

        val body = getTuple.body
        assertIs<Block>(body)

        checkCallFlowsToTupleElements(body, "returnTuple2")
    }

    @Test
    fun testListComprehensionsNoBracketToBracket() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val result =
            analyze(listOf(topLevel.resolve("tuple_assign.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)
        val getTuple = result.functions["getTuple3"]
        assertNotNull(getTuple)

        val body = getTuple.body
        assertIs<Block>(body)
        checkCallFlowsToTupleElements(body, "returnTuple2")
    }

    @Test
    fun testListComprehensionsBracketToNoBracket() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val result =
            analyze(listOf(topLevel.resolve("tuple_assign.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)
        val getTuple = result.functions["getTuple4"]
        assertNotNull(getTuple)

        val body = getTuple.body
        assertIs<Block>(body)
        checkCallFlowsToTupleElements(body, "returnTuple")
    }

    @Test
    fun testFollowFunctions() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val result =
            analyze(listOf(topLevel.resolve("tuple_assign.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)
        val getTuple = result.functions["getTuple4"]
        assertNotNull(getTuple)
        val cRead = getTuple.refs["c"]
        assertNotNull(cRead)

        val returnTuple = result.functions["returnTuple"]
        assertNotNull(returnTuple)
        val aReturned = returnTuple.refs["a"]
        assertNotNull(aReturned)
        val bReturned = returnTuple.refs["b"]
        assertNotNull(bReturned)
        val backwardsPathCToA =
            cRead
                .followDFGEdgesUntilHit(direction = Backward(GraphToFollow.DFG)) { it == aReturned }
                .fulfilled
        assertEquals(1, backwardsPathCToA.size)
        val backwardsPathCToB =
            cRead
                .followDFGEdgesUntilHit(direction = Backward(GraphToFollow.DFG)) { it == bReturned }
                .fulfilled
        assertEquals(0, backwardsPathCToB.size)

        val forwardsPathAToC = aReturned.followDFGEdgesUntilHit { it == cRead }.fulfilled
        assertEquals(1, forwardsPathAToC.size)
        val forwardsPathBToC = bReturned.followDFGEdgesUntilHit { it == cRead }.fulfilled
        assertEquals(0, forwardsPathBToC.size)
    }

    @Test
    fun testContextSensitive() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val result =
            analyze(listOf(topLevel.resolve("context_sensitive.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)

        val keyStartRef = result.calls["retrieve_key_from_server"]?.nextDFG?.first()
        assertNotNull(keyStartRef)

        val paths =
            keyStartRef.followDFGEdgesUntilHit(collectFailedPaths = false) {
                it is CallExpression &&
                    it.name.localName == "cipher_operation" &&
                    it.arguments[0].evaluate() == "encrypt"
            }
        assertNotNull(paths)
        assertEquals(1, paths.fulfilled.size)
        val path = paths.fulfilled.first()
        assertEquals(22, path.size)
    }
}
