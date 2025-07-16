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
import de.fraunhofer.aisec.cpg.graph.edges.flows.FullDataflowGranularity
import de.fraunhofer.aisec.cpg.graph.edges.flows.IndexedDataflowGranularity
import de.fraunhofer.aisec.cpg.graph.edges.flows.PartialDataflowGranularity
import de.fraunhofer.aisec.cpg.graph.edges.flows.StringIndexedDataflowGranularity
import de.fraunhofer.aisec.cpg.graph.statements.expressions.AssignExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.InitializerListExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.statements.expressions.SubscriptExpression
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

        val c = body.dVariables["c"]
        assertNotNull(c)
        assertTrue(c.prevDFG.isEmpty())

        val d = body.dVariables["d"]
        assertNotNull(d)
        assertTrue(d.prevDFG.isEmpty())
    }

    fun checkReturnTuple(functionDeclaration: FunctionDeclaration) {
        val returnStmt = functionDeclaration.dReturns.singleOrNull()
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
        val getTuple = result.dFunctions["getTuple"]
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
        val returnTuple = result.dFunctions["returnTuple"]
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
        val returnTuple = result.dFunctions["returnTuple2"]
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
        val getTuple = result.dFunctions["getTuple2"]
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
        val getTuple = result.dFunctions["getTuple3"]
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
        val getTuple = result.dFunctions["getTuple4"]
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
        val getTuple = result.dFunctions["getTuple4"]
        assertNotNull(getTuple)
        val cRead = getTuple.dRefs["c"]
        assertNotNull(cRead)

        val returnTuple = result.dFunctions["returnTuple"]
        assertNotNull(returnTuple)
        val aReturned = returnTuple.dRefs["a"]
        assertNotNull(aReturned)
        val bReturned = returnTuple.dRefs["b"]
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

        val keyStartRef = result.dCalls["retrieve_key_from_server"]?.nextDFG?.first()
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
        assertEquals(22, path.nodes.size)
    }

    @Test
    fun testDictAccess() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val result =
            analyze(listOf(topLevel.resolve("dict_dfg.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)

        val dInitialization =
            result.dRefs[{ it.name.localName == "d" && it.location?.region?.startLine == 2 }]
        assertIs<Reference>(
            dInitialization,
            "We expect that there is a reference called \"d\" in line 2 of the file.",
        )

        // Test the DFG edges for the reference d and the access d['b'] in line 9 of the file.
        val dLine9 =
            result.dRefs[{ it.name.localName == "d" && it.location?.region?.startLine == 9 }]
        assertIs<Reference>(
            dLine9,
            "We expect that there is a reference called \"d\" in line 9 of the file.",
        )
        val dBLine9 =
            result
                .descendants<SubscriptExpression> { it.location?.region?.startLine == 9 }
                .singleOrNull()
        assertIs<SubscriptExpression>(
            dBLine9,
            "We expect that there is a subscript expression representing \"d['b']\" in line 9 of the file.",
        )
        val literal1 = result.dLiterals.singleOrNull { it.value == 1L }
        assertIs<Literal<*>>(
            literal1,
            "We expect that there is a Literal<Int> representing \"1\" in line 9 of the file.",
        )
        assertEquals(
            2,
            dLine9.prevDFGEdges.size,
            "We expect that there are two DFG edges to the reference \"d\" line 9: The partial flow and the full flow from line 2.",
        )
        val dToDLine9 =
            dLine9.prevDFGEdges.singleOrNull { it.granularity is FullDataflowGranularity }
        assertNotNull(
            dToDLine9,
            "We expect that there is a full DFG edge between the access \"d\" in line 9 the initialization in line 2.",
        )
        assertIs<FullDataflowGranularity>(
            dToDLine9.granularity,
            "We expect that there is a full DFG edge between the access \"d\" in line 9 the initialization in line 2.",
        )
        assertEquals(
            dInitialization,
            dToDLine9.start,
            "We expect that there is a full DFG edge between the access \"d\" in line 9 the initialization in line 2.",
        )
        val dBToD =
            dLine9.prevDFGEdges.singleOrNull { it.granularity is PartialDataflowGranularity<*> }
        assertNotNull(
            dBToD,
            "We expect that there is a partial DFG edge between the access \"d['b']\" in line 9 of the file and its reference \"d\" with granularity StringIndexedGranularity and index \"b\".",
        )
        val dBToDGranularity = dBToD.granularity
        assertIs<StringIndexedDataflowGranularity>(
            dBToDGranularity,
            "We expect that there is a partial DFG edge between the access \"d['b']\" in line 9 of the file and its reference \"d\" with granularity StringIndexedGranularity and index \"b\".",
        )
        assertEquals(
            "b",
            dBToDGranularity.partialTarget,
            "We expect that there is a partial DFG edge between the access \"d['b']\" in line 9 of the file and its reference \"d\" with granularity StringIndexedGranularity and index \"b\".",
        )
        val literal1ToDB = dBLine9.prevDFGEdges.singleOrNull()
        assertNotNull(
            literal1ToDB,
            "We expect that there is a full DFG edge between the literal 1 and the access \"d['b']\" in line 9 of the file.",
        )
        assertIs<FullDataflowGranularity>(
            literal1ToDB.granularity,
            "We expect that there is a full DFG edge between the literal 1 and the access \"d['b']\" in line 9 of the file.",
        )

        // Test the DFG edges for the reference d and the access d['b'] printed in line 10 of the
        // file.
        val printDLine10 =
            result.dRefs[{ it.name.localName == "d" && it.location?.region?.startLine == 10 }]
        assertIs<Reference>(
            printDLine10,
            "We expect that there is a reference called \"d\" in line 10 of the file.",
        )
        assertEquals(
            1,
            printDLine10.prevDFGEdges.size,
            "We expect one incoming DFG edges: The full edge from line 9.",
        )
        val dfgToInitializationLine10 = printDLine10.prevDFGEdges.singleOrNull()
        assertNotNull(
            dfgToInitializationLine10,
            "We expect one incoming DFG edges: The full edge from line 9.",
        )
        assertIs<FullDataflowGranularity>(
            dfgToInitializationLine10.granularity,
            "We expect one incoming DFG edges: The full edge from line 9.",
        )
        assertEquals(
            dLine9,
            dfgToInitializationLine10.start,
            "We expect one incoming DFG edges: The full edge from line 9.",
        )

        val subscriptLine10 =
            result
                .descendants<SubscriptExpression> { it.location?.region?.startLine == 10 }
                .singleOrNull()
        assertIs<SubscriptExpression>(
            subscriptLine10,
            "We expect that there is a subscript expression simulating \"d['b']\" in line 10 of the file.",
        )
        assertEquals(
            2,
            subscriptLine10.prevDFGEdges.size,
            "We expect two incoming DFG edges: The partial edge from the reference \"d\" with partial granularity and index \"b\" and the full edge from the assignment in line 9.",
        )
        val dBWriteToSubscriptDB =
            subscriptLine10.prevDFGEdges.singleOrNull { it.granularity is FullDataflowGranularity }
        assertNotNull(
            dBWriteToSubscriptDB,
            "We expect two incoming DFG edges: The partial edge from the reference \"d\" with partial granularity and index \"b\" and the full edge from the assignment in line 9.",
        )
        assertEquals(
            dBLine9,
            dBWriteToSubscriptDB.start,
            "We expect two incoming DFG edges: The partial edge from the reference \"d\" with partial granularity and index \"b\" and the full edge from the assignment in line 9.",
        )
        val dToSubscriptDB =
            subscriptLine10.prevDFGEdges.singleOrNull {
                it.granularity is PartialDataflowGranularity<*>
            }
        assertNotNull(
            dToSubscriptDB,
            "We expect two incoming DFG edges: The partial edge from the reference \"d\" with partial granularity and index \"b\" and the full edge from the assignment in line 9.",
        )
        assertEquals(
            printDLine10,
            dToSubscriptDB.start,
            "We expect two incoming DFG edges: The partial edge from the reference \"d\" with partial granularity and index \"b\" and the full edge from the assignment in line 9.",
        )
        val dToSubscriptDBGranularity = dToSubscriptDB.granularity
        assertIs<StringIndexedDataflowGranularity>(
            dToSubscriptDBGranularity,
            "We expect two incoming DFG edges The partial edge from the reference \"d\" with partial granularity and index \"b\" and the full edge from the assignment in line 9.",
        )
        assertEquals(
            "b",
            dToSubscriptDBGranularity.partialTarget,
            "We expect two incoming DFG edges: The partial edge from the reference \"d\" with partial granularity and index \"b\" and the full edge from the assignment in line 9.",
        )

        // Test the DFG edges for the reference d and the access d['a'] printed in line 11 of the
        // file.
        val printDLine11 =
            result.dRefs[{ it.name.localName == "d" && it.location?.region?.startLine == 11 }]
        assertIs<Reference>(
            printDLine11,
            "We expect that there is a reference called \"d\" in line 11 of the file.",
        )
        assertEquals(
            1,
            printDLine11.prevDFGEdges.size,
            "We expect one incoming DFG edges: The full edge from the initialization and the partial edge from line 9.",
        )
        val dfgToPartialWriteLine11 = printDLine11.prevDFGEdges.singleOrNull()
        assertNotNull(
            dfgToPartialWriteLine11,
            "We expect two incoming DFG edges: The full edge from the initialization and the partial edge from line 9.",
        )
        assertIs<FullDataflowGranularity>(
            dfgToPartialWriteLine11.granularity,
            "We expect one incoming DFG edges: The full edge from the initialization and the partial edge from line 9.",
        )
        assertEquals(
            dLine9,
            dfgToPartialWriteLine11.start,
            "We expect two incoming DFG edges: The full edge from the initialization and the partial edge from line 9.",
        )

        val subscriptLine11 =
            result
                .descendants<SubscriptExpression> { it.location?.region?.startLine == 11 }
                .singleOrNull()
        assertIs<SubscriptExpression>(
            subscriptLine11,
            "We expect that there is a subscript expression simulating \"d['a']\" in line 11 of the file.",
        )
        val dToSubscriptDA = subscriptLine11.prevDFGEdges.singleOrNull()
        assertNotNull(
            dToSubscriptDA,
            "We expect a single incoming DFG edges: The partial edge from the reference \"d\" with partial granularity and index \"a\".",
        )
        assertEquals(
            printDLine11,
            dToSubscriptDA.start,
            "We expect a single incoming DFG edges: The partial edge from the reference \"d\" with partial granularity and index \"a\".",
        )
        val dToSubscriptDAGranularity = dToSubscriptDA.granularity
        assertIs<StringIndexedDataflowGranularity>(
            dToSubscriptDAGranularity,
            "We expect a single incoming DFG edges: The partial edge from the reference \"d\" with partial granularity and index \"a\".",
        )
        assertEquals(
            "a",
            dToSubscriptDAGranularity.partialTarget,
            "We expect a single incoming DFG edges: The partial edge from the reference \"d\" with partial granularity and index \"a\".",
        )

        // Test the DFG edges for the reference d printed in line 12 of the file.
        val printD =
            result.dRefs[{ it.name.localName == "d" && it.location?.region?.startLine == 12 }]
        assertIs<Reference>(
            printD,
            "We expect that there is a reference called \"d\" in line 12 of the file.",
        )
        assertEquals(
            1,
            printD.prevDFGEdges.size,
            "We expect two incoming DFG edges: The full edge from line 9.",
        )
        val dfgTodB = printD.prevDFGEdges.singleOrNull()
        assertNotNull(dfgTodB, "We expect two incoming DFG edges: The full edge from line 9.")
        assertIs<FullDataflowGranularity>(
            dfgTodB.granularity,
            "We expect two incoming DFG edges: The full edge from line 9.",
        )
        assertEquals(
            dLine9,
            dfgTodB.start,
            "We expect two incoming DFG edges: The full edge from line 9.",
        )
    }

    @Test
    fun testListAccess() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val result =
            analyze(listOf(topLevel.resolve("array_dfg.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)

        val dInitialization =
            result.dRefs[{ it.name.localName == "d" && it.location?.region?.startLine == 2 }]
        assertIs<Reference>(
            dInitialization,
            "We expect that there is a reference called \"d\" in line 2 of the file.",
        )

        // Test the DFG edges for the reference d and the access d[1] in line 9 of the file.
        val dLine9 =
            result.dRefs[{ it.name.localName == "d" && it.location?.region?.startLine == 9 }]
        assertIs<Reference>(
            dLine9,
            "We expect that there is a reference called \"d\" in line 9 of the file.",
        )
        val d1Line9 =
            result
                .descendants<SubscriptExpression> { it.location?.region?.startLine == 9 }
                .singleOrNull()
        assertIs<SubscriptExpression>(
            d1Line9,
            "We expect that there is a subscript expression representing \"d[1]\" in line 9 of the file.",
        )
        val literal1 = result.dLiterals.singleOrNull { it.value == 10L }
        assertIs<Literal<*>>(
            literal1,
            "We expect that there is a Literal<Int> representing \"1\" in line 9 of the file.",
        )
        assertEquals(
            2,
            dLine9.prevDFGEdges.size,
            "We expect that there are two DFG edges to the reference \"d\" line 9: The partial flow and the full flow from line 2.",
        )
        val dToDLine9 =
            dLine9.prevDFGEdges.singleOrNull { it.granularity is FullDataflowGranularity }
        assertNotNull(
            dToDLine9,
            "We expect that there is a full DFG edge between the access \"d\" in line 9 and the initialization in line 2.",
        )
        assertIs<FullDataflowGranularity>(
            dToDLine9.granularity,
            "We expect that there is a full DFG edge between the access \"d\" in line 9 and the initialization in line 2.",
        )
        assertEquals(
            dInitialization,
            dToDLine9.start,
            "We expect that there is a full DFG edge between the access \"d\" in line 9 and the initialization in line 2.",
        )
        val d1ToD =
            dLine9.prevDFGEdges.singleOrNull { it.granularity is PartialDataflowGranularity<*> }
        assertNotNull(
            d1ToD,
            "We expect that there is a partial DFG edge between the access \"d[1]\" in line 9 of the file and its reference \"d\" with granularity IndexedGranularity and index 1.",
        )
        val d1ToDGranularity = d1ToD.granularity
        assertIs<IndexedDataflowGranularity>(
            d1ToDGranularity,
            "We expect that there is a partial DFG edge between the access \"d[1]\" in line 9 of the file and its reference \"d\" with granularity IndexedGranularity and index 1.",
        )
        assertEquals(
            1L,
            d1ToDGranularity.partialTarget,
            "We expect that there is a partial DFG edge between the access \"d[1]\" in line 9 of the file and its reference \"d\" with granularity IndexedGranularity and index 1.",
        )
        val literal1ToD1 = d1Line9.prevDFGEdges.singleOrNull()
        assertNotNull(
            literal1ToD1,
            "We expect that there is a full DFG edge between the literal 1 and the access \"d[1]\" in line 9 of the file.",
        )
        assertIs<FullDataflowGranularity>(
            literal1ToD1.granularity,
            "We expect that there is a full DFG edge between the literal 1 and the access \"d[1]\" in line 9 of the file.",
        )

        // Test the DFG edges for the reference d and the access d[1] printed in line 10 of the
        // file.
        val printDLine10 =
            result.dRefs[{ it.name.localName == "d" && it.location?.region?.startLine == 10 }]
        assertIs<Reference>(
            printDLine10,
            "We expect that there is a reference called \"d\" in line 10 of the file.",
        )
        assertEquals(
            1,
            printDLine10.prevDFGEdges.size,
            "We expect one incoming DFG edges: The full edge from line 9.",
        )
        val dfgToInitializationLine10 = printDLine10.prevDFGEdges.singleOrNull()
        assertNotNull(
            dfgToInitializationLine10,
            "We expect one incoming DFG edges: The full edge from line 9.",
        )
        assertIs<FullDataflowGranularity>(
            dfgToInitializationLine10.granularity,
            "We expect one incoming DFG edges: The full edge from line 9.",
        )
        assertEquals(
            dLine9,
            dfgToInitializationLine10.start,
            "We expect one incoming DFG edges: The full edge from line 9.",
        )

        val subscriptLine10 =
            result
                .descendants<SubscriptExpression> { it.location?.region?.startLine == 10 }
                .singleOrNull()
        assertIs<SubscriptExpression>(
            subscriptLine10,
            "We expect that there is a subscript expression simulating \"d[1]\" in line 10 of the file.",
        )
        assertEquals(
            2,
            subscriptLine10.prevDFGEdges.size,
            "We expect two incoming DFG edges: The partial edge from the reference \"d\" with partial granularity and index 1 and the full edge from the assignment in line 9.",
        )
        val d1WriteToSubscriptDB =
            subscriptLine10.prevDFGEdges.singleOrNull { it.granularity is FullDataflowGranularity }
        assertNotNull(
            d1WriteToSubscriptDB,
            "We expect two incoming DFG edges: The partial edge from the reference \"d\" with partial granularity and index 1 and the full edge from the assignment in line 9.",
        )
        assertEquals(
            d1Line9,
            d1WriteToSubscriptDB.start,
            "We expect two incoming DFG edges: The partial edge from the reference \"d\" with partial granularity and index 1 and the full edge from the assignment in line 9.",
        )
        val dToSubscriptD1 =
            subscriptLine10.prevDFGEdges.singleOrNull {
                it.granularity is PartialDataflowGranularity<*>
            }
        assertNotNull(
            dToSubscriptD1,
            "We expect two incoming DFG edges: The partial edge from the reference \"d\" with partial granularity and index 1 and the full edge from the assignment in line 9.",
        )
        assertEquals(
            printDLine10,
            dToSubscriptD1.start,
            "We expect two incoming DFG edges: The partial edge from the reference \"d\" with partial granularity and index 1 and the full edge from the assignment in line 9.",
        )
        val dToSubscriptD1Granularity = dToSubscriptD1.granularity
        assertIs<IndexedDataflowGranularity>(
            dToSubscriptD1Granularity,
            "We expect two incoming DFG edges The partial edge from the reference \"d\" with partial granularity and index 1 and the full edge from the assignment in line 9.",
        )
        assertEquals(
            1L,
            dToSubscriptD1Granularity.partialTarget,
            "We expect two incoming DFG edges: The partial edge from the reference \"d\" with partial granularity and index 1 and the full edge from the assignment in line 9.",
        )

        // Test the DFG edges for the reference d and the access d[0] printed in line 11 of the
        // file.
        val printDLine11 =
            result.dRefs[{ it.name.localName == "d" && it.location?.region?.startLine == 11 }]
        assertIs<Reference>(
            printDLine11,
            "We expect that there is a reference called \"d\" in line 11 of the file.",
        )
        assertEquals(
            1,
            printDLine11.prevDFGEdges.size,
            "We expect one incoming DFG edges: The full edge from the initialization and the partial edge from line 9.",
        )
        val dfgToPartialWriteLine11 = printDLine11.prevDFGEdges.singleOrNull()
        assertNotNull(
            dfgToPartialWriteLine11,
            "We expect two incoming DFG edges: The full edge from the initialization and the partial edge from line 9.",
        )
        assertIs<FullDataflowGranularity>(
            dfgToPartialWriteLine11.granularity,
            "We expect one incoming DFG edges: The full edge from the initialization and the partial edge from line 9.",
        )
        assertEquals(
            dLine9,
            dfgToPartialWriteLine11.start,
            "We expect two incoming DFG edges: The full edge from the initialization and the partial edge from line 9.",
        )

        val subscriptLine11 =
            result
                .descendants<SubscriptExpression> { it.location?.region?.startLine == 11 }
                .singleOrNull()
        assertIs<SubscriptExpression>(
            subscriptLine11,
            "We expect that there is a subscript expression simulating \"d[0]\" in line 11 of the file.",
        )
        val dToSubscriptD0 = subscriptLine11.prevDFGEdges.singleOrNull()
        assertNotNull(
            dToSubscriptD0,
            "We expect a single incoming DFG edges: The partial edge from the reference \"d\" with partial granularity and index \"0\".",
        )
        assertEquals(
            printDLine11,
            dToSubscriptD0.start,
            "We expect a single incoming DFG edges: The partial edge from the reference \"d\" with partial granularity and index \"0\".",
        )
        val dToSubscriptD0Granularity = dToSubscriptD0.granularity
        assertIs<IndexedDataflowGranularity>(
            dToSubscriptD0Granularity,
            "We expect a single incoming DFG edges: The partial edge from the reference \"d\" with partial granularity and index \"0\".",
        )
        assertEquals(
            0L,
            dToSubscriptD0Granularity.partialTarget,
            "We expect a single incoming DFG edges: The partial edge from the reference \"d\" with partial granularity and index \"0\".",
        )

        // Test the DFG edges for the reference d printed in line 12 of the file.
        val printD =
            result.dRefs[{ it.name.localName == "d" && it.location?.region?.startLine == 12 }]
        assertIs<Reference>(
            printD,
            "We expect that there is a reference called \"d\" in line 12 of the file.",
        )
        assertEquals(
            1,
            printD.prevDFGEdges.size,
            "We expect two incoming DFG edges: The full edge from line 9.",
        )
        val dfgToD = printD.prevDFGEdges.singleOrNull()
        assertNotNull(dfgToD, "We expect two incoming DFG edges: The full edge from line 9.")
        assertIs<FullDataflowGranularity>(
            dfgToD.granularity,
            "We expect two incoming DFG edges: The full edge from line 9.",
        )
        assertEquals(
            dLine9,
            dfgToD.start,
            "We expect two incoming DFG edges: The full edge from line 9.",
        )
    }

    @Test
    fun testMemberAccess() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val result =
            analyze(listOf(topLevel.resolve("member_dfg.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)

        val fieldA = result.dFields["a"]
        assertNotNull(fieldA, "We expect that there is a field called \"a\".")
        val fieldB = result.dFields["b"]
        assertNotNull(fieldB, "We expect that there is a field called \"b\".")

        val dInitialization =
            result.dRefs[{ it.name.localName == "d" && it.location?.region?.startLine == 2 }]
        assertIs<Reference>(
            dInitialization,
            "We expect that there is a reference called \"d\" in line 2 of the file.",
        )

        // Test the DFG edges for the reference d and the access d.b in line 9 of the file.
        val dLine9 =
            result.dRefs[{ it.name.localName == "d" && it.location?.region?.startLine == 9 }]
        assertIs<Reference>(
            dLine9,
            "We expect that there is a reference called \"d\" in line 9 of the file.",
        )
        val dBLine9 =
            result
                .descendants<MemberExpression> { it.location?.region?.startLine == 9 }
                .singleOrNull()
        assertIs<MemberExpression>(
            dBLine9,
            "We expect that there is a subscript expression representing \"d.b\" in line 9 of the file.",
        )
        val literal1 = result.dLiterals.singleOrNull { it.value == 1L }
        assertIs<Literal<*>>(
            literal1,
            "We expect that there is a Literal<Int> representing \"1\" in line 9 of the file.",
        )
        assertEquals(
            2,
            dLine9.prevDFGEdges.size,
            "We expect that there are two DFG edges to the reference \"d\" line 9: The partial flow and the full flow from line 2.",
        )
        val dToDLine9 =
            dLine9.prevDFGEdges.singleOrNull { it.granularity is FullDataflowGranularity }
        assertNotNull(
            dToDLine9,
            "We expect that there is a full DFG edge between the access \"d\" in line 9 the initialization in line 2.",
        )
        assertIs<FullDataflowGranularity>(
            dToDLine9.granularity,
            "We expect that there is a full DFG edge between the access \"d\" in line 9 the initialization in line 2.",
        )
        assertEquals(
            dInitialization,
            dToDLine9.start,
            "We expect that there is a full DFG edge between the access \"d\" in line 9 the initialization in line 2.",
        )
        val dBToD =
            dLine9.prevDFGEdges.singleOrNull { it.granularity is PartialDataflowGranularity<*> }
        assertNotNull(
            dBToD,
            "We expect that there is a partial DFG edge between the access \"d.b\" in line 9 of the file and its reference \"d\" with granularity StringIndexedGranularity and index \"b\".",
        )
        val dBToDGranularity = dBToD.granularity
        assertIs<PartialDataflowGranularity<*>>(
            dBToDGranularity,
            "We expect that there is a partial DFG edge between the access \"d.b\" in line 9 of the file and its reference \"d\" with granularity StringIndexedGranularity and index \"b\".",
        )
        assertEquals(
            fieldB,
            dBToDGranularity.partialTarget,
            "We expect that there is a partial DFG edge between the access \"d.b\" in line 9 of the file and its reference \"d\" with granularity StringIndexedGranularity and index \"b\".",
        )
        val literal1ToDB = dBLine9.prevDFGEdges.singleOrNull()
        assertNotNull(
            literal1ToDB,
            "We expect that there is a full DFG edge between the literal 1 and the access \"d.b\" in line 9 of the file.",
        )
        assertIs<FullDataflowGranularity>(
            literal1ToDB.granularity,
            "We expect that there is a full DFG edge between the literal 1 and the access \"d.b\" in line 9 of the file.",
        )

        // Test the DFG edges for the reference d and the access d.b printed in line 10 of the
        // file.
        val printDLine10 =
            result.dRefs[{ it.name.localName == "d" && it.location?.region?.startLine == 10 }]
        assertIs<Reference>(
            printDLine10,
            "We expect that there is a reference called \"d\" in line 10 of the file.",
        )
        assertEquals(
            1,
            printDLine10.prevDFGEdges.size,
            "We expect one incoming DFG edges: The full edge from line 9.",
        )
        val dfgToInitializationLine10 = printDLine10.prevDFGEdges.singleOrNull()
        assertNotNull(
            dfgToInitializationLine10,
            "We expect one incoming DFG edges: The full edge from line 9.",
        )
        assertIs<FullDataflowGranularity>(
            dfgToInitializationLine10.granularity,
            "We expect one incoming DFG edges: The full edge from line 9.",
        )
        assertEquals(
            dLine9,
            dfgToInitializationLine10.start,
            "We expect one incoming DFG edges: The full edge from line 9.",
        )

        val subscriptLine10 =
            result
                .descendants<MemberExpression> { it.location?.region?.startLine == 10 }
                .singleOrNull()
        assertIs<MemberExpression>(
            subscriptLine10,
            "We expect that there is a subscript expression simulating \"d.b\" in line 10 of the file.",
        )
        assertEquals(
            2,
            subscriptLine10.prevDFGEdges.size,
            "We expect two incoming DFG edges: The partial edge from the reference \"d\" with partial granularity and index \"b\" and the full edge from the assignment in line 9.",
        )
        val dBWriteToMemberDB =
            subscriptLine10.prevDFGEdges.singleOrNull { it.granularity is FullDataflowGranularity }
        assertNotNull(
            dBWriteToMemberDB,
            "We expect two incoming DFG edges: The partial edge from the reference \"d\" with partial granularity and index \"b\" and the full edge from the assignment in line 9.",
        )
        assertEquals(
            dBLine9,
            dBWriteToMemberDB.start,
            "We expect two incoming DFG edges: The partial edge from the reference \"d\" with partial granularity and index \"b\" and the full edge from the assignment in line 9.",
        )
        val dToMemberDB =
            subscriptLine10.prevDFGEdges.singleOrNull {
                it.granularity is PartialDataflowGranularity<*>
            }
        assertNotNull(
            dToMemberDB,
            "We expect two incoming DFG edges: The partial edge from the reference \"d\" with partial granularity and index \"b\" and the full edge from the assignment in line 9.",
        )
        assertEquals(
            printDLine10,
            dToMemberDB.start,
            "We expect two incoming DFG edges: The partial edge from the reference \"d\" with partial granularity and index \"b\" and the full edge from the assignment in line 9.",
        )
        val dToMemberDBGranularity = dToMemberDB.granularity
        assertIs<PartialDataflowGranularity<*>>(
            dToMemberDBGranularity,
            "We expect two incoming DFG edges The partial edge from the reference \"d\" with partial granularity and index \"b\" and the full edge from the assignment in line 9.",
        )
        assertEquals(
            fieldB,
            dToMemberDBGranularity.partialTarget,
            "We expect two incoming DFG edges: The partial edge from the reference \"d\" with partial granularity and index \"b\" and the full edge from the assignment in line 9.",
        )

        // Test the DFG edges for the reference d and the access d.a printed in line 11 of the
        // file.
        val printDLine11 =
            result.dRefs[{ it.name.localName == "d" && it.location?.region?.startLine == 11 }]
        assertIs<Reference>(
            printDLine11,
            "We expect that there is a reference called \"d\" in line 11 of the file.",
        )
        assertEquals(
            1,
            printDLine11.prevDFGEdges.size,
            "We expect one incoming DFG edges: The full edge from the initialization and the partial edge from line 9.",
        )
        val dfgToPartialWriteLine11 = printDLine11.prevDFGEdges.singleOrNull()
        assertNotNull(
            dfgToPartialWriteLine11,
            "We expect two incoming DFG edges: The full edge from the initialization and the partial edge from line 9.",
        )
        assertIs<FullDataflowGranularity>(
            dfgToPartialWriteLine11.granularity,
            "We expect one incoming DFG edges: The full edge from the initialization and the partial edge from line 9.",
        )
        assertEquals(
            dLine9,
            dfgToPartialWriteLine11.start,
            "We expect two incoming DFG edges: The full edge from the initialization and the partial edge from line 9.",
        )

        val subscriptLine11 =
            result
                .descendants<MemberExpression> { it.location?.region?.startLine == 11 }
                .singleOrNull()
        assertIs<MemberExpression>(
            subscriptLine11,
            "We expect that there is a subscript expression simulating \"d.a\" in line 11 of the file.",
        )
        val fieldAToMemberDA =
            subscriptLine11.prevDFGEdges.single { it.granularity is FullDataflowGranularity }
        assertNotNull(
            fieldAToMemberDA,
            "We expect a full DFG edge between the field \"a\" and the member access \"d.a\".",
        )
        assertEquals(
            fieldA,
            fieldAToMemberDA.start,
            "We expect a full DFG edge between the field \"a\" and the member access \"d.a\".",
        )

        val dToMemberDA =
            subscriptLine11.prevDFGEdges.singleOrNull {
                it.granularity is PartialDataflowGranularity<*>
            }
        assertNotNull(
            dToMemberDA,
            "We expect a single partial incoming DFG edges: The partial edge from the reference \"d\" with partial granularity and index \"a\".",
        )
        assertEquals(
            printDLine11,
            dToMemberDA.start,
            "We expect a single partial incoming DFG edges: The partial edge from the reference \"d\" with partial granularity and index \"a\".",
        )

        assertEquals(
            fieldA,
            (dToMemberDA.granularity as? PartialDataflowGranularity<*>)?.partialTarget,
            "We expect an incoming DFG edge: The partial edge from the reference \"d\" with partial granularity and index \"a\".",
        )

        val dToMemberDAFull =
            subscriptLine11.prevDFGEdges.singleOrNull { it.granularity is FullDataflowGranularity }
        assertNotNull(
            dToMemberDAFull,
            "We expect a full incoming DFG edge: The edge from the declaration of \"a\" in \"d\".",
        )
        assertEquals(
            fieldA,
            dToMemberDAFull.start,
            "We expect a full incoming DFG edge: The edge from the declaration of \"a\" in \"d\".",
        )

        // Test the DFG edges for the reference d printed in line 12 of the file.
        val printD =
            result.dRefs[{ it.name.localName == "d" && it.location?.region?.startLine == 12 }]
        assertIs<Reference>(
            printD,
            "We expect that there is a reference called \"d\" in line 12 of the file.",
        )
        assertEquals(
            1,
            printD.prevDFGEdges.size,
            "We expect two incoming DFG edges: The full edge from line 9.",
        )
        val dfgTodB = printD.prevDFGEdges.singleOrNull()
        assertNotNull(dfgTodB, "We expect two incoming DFG edges: The full edge from line 9.")
        assertIs<FullDataflowGranularity>(
            dfgTodB.granularity,
            "We expect two incoming DFG edges: The full edge from line 9.",
        )
        assertEquals(
            dLine9,
            dfgTodB.start,
            "We expect two incoming DFG edges: The full edge from line 9.",
        )
    }
}
