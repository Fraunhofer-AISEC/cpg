/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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
import de.fraunhofer.aisec.cpg.TestUtils
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DFGTest {
    // Test DFGPass and ControlFlowSensitiveDFGPass

    /**
     * To test assignments of different value in an expression that then has a joinPoint. a = a == b
     * ? b = 2: b = 3;
     *
     * @throws Exception
     */
    @Test
    @Throws(Exception::class)
    fun testConditionalExpression() {
        val topLevel = Path.of("src", "test", "resources", "dfg")
        val result =
            TestUtils.analyze(
                listOf(topLevel.resolve("conditional_expression.cpp").toFile()),
                topLevel,
                true
            )
        val bJoin = result.refs[{ it.name.localName == "b" && it.location?.region?.startLine == 6 }]
        val a5 = result.refs[{ it.name.localName == "a" && it.location?.region?.startLine == 5 }]
        val a6 = result.refs[{ it.name.localName == "a" && it.location?.region?.startLine == 6 }]
        val bCond =
            result.refs[
                    {
                        it.name.localName == "b" &&
                            it.location?.region?.startLine == 5 &&
                            it.location?.region?.startColumn == 16
                    }]
        val b2 =
            result.refs[
                    {
                        it.name.localName == "b" &&
                            it.location?.region?.startLine == 5 &&
                            it.location?.region?.startColumn == 16
                    }]
        val b3 =
            result.refs[
                    {
                        it.name.localName == "b" &&
                            it.location?.region?.startLine == 5 &&
                            it.location?.region?.startColumn == 23
                    }]
        assertNotNull(bJoin)
        assertNotNull(bCond)
        assertNotNull(b2)
        assertNotNull(b3)
        assertNotNull(a5)
        assertNotNull(a6)

        val val2 = result.literals[{ it.value == 2 }]
        assertNotNull(val2)

        val val3 = result.literals[{ it.value == 3 }]
        assertNotNull(val3)

        assertEquals(1, b2.prevDFG.size)
        assertTrue(b2.prevDFG.contains(val2))
        assertEquals(1, b3.prevDFG.size)
        assertTrue(b3.prevDFG.contains(val3))

        // We want the ConditionalExpression
        assertEquals(1, a5.prevDFG.size)
        assertTrue(a5.prevDFG.first() is ConditionalExpression)
        assertTrue(flattenDFGGraph(a5, false).contains(val2))
        assertTrue(flattenDFGGraph(a5, false).contains(val3))

        assertEquals(1, a6.prevDFG.size)
        assertTrue(a6.prevDFG.contains(bJoin))
        assertEquals(2, bJoin.prevDFG.size)
        // The b which got assigned 2 flows to the b in line 6
        assertTrue(bJoin.prevDFG.contains(b2))
        // The b which got assigned 3 flows to the b in line 6
        assertTrue(bJoin.prevDFG.contains(b3))
    }

    /**
     * Ensures that if there is an assignment like a = a + b the replacement of the current value of
     * the VariableDeclaration is delayed until the entire assignment has been traversed. This is
     * necessary, since if the replacement was not delayed the rhs a would have an incoming dfg edge
     * from a + b
     *
     * @throws Exception
     */
    @Test
    @Throws(Exception::class)
    fun testDelayedAssignment() {
        val result = GraphExamples.getDelayedAssignmentAfterRHS()

        val binaryOperatorAssignment =
            TestUtils.findByUniqueName(result.allChildren<BinaryOperator>(), "=")
        assertNotNull(binaryOperatorAssignment)

        val binaryOperatorAddition =
            TestUtils.findByUniqueName(result.allChildren<BinaryOperator>(), "+")
        assertNotNull(binaryOperatorAddition)

        val varA = TestUtils.findByUniqueName(result.variables, "a")
        assertNotNull(varA)

        val varB = TestUtils.findByUniqueName(result.variables, "b")
        assertNotNull(varB)

        val lhsA = binaryOperatorAssignment.lhs as DeclaredReferenceExpression
        val rhsA = binaryOperatorAddition.lhs as DeclaredReferenceExpression
        val b = TestUtils.findByUniqueName(result.refs, "b")
        assertNotNull(b)

        val literal0 = result.literals[{ it.value == 0 }]
        assertNotNull(literal0)

        val literal1 = result.literals[{ it.value == 1 }]
        assertNotNull(literal1)
        // a and b flow to the DeclaredReferenceExpressions in (a+b)
        assertEquals(1, varA.nextDFG.size)
        assertEquals(1, varB.nextDFG.size)
        assertTrue(varA.nextDFG.contains(rhsA))
        assertTrue(varB.nextDFG.contains(b))
        assertEquals(1, rhsA.prevDFG.size)
        assertTrue(rhsA.prevDFG.contains(varA))
        assertEquals(1, b.prevDFG.size)
        assertTrue(b.prevDFG.contains(varB))

        // The literals flow to the VariableDeclarationExpression
        assertEquals(1, literal0.nextDFG.size)
        assertEquals(varA, literal0.nextDFG.first())
        assertEquals(1, literal0.nextDFG.size)
        assertEquals(varB, literal1.nextDFG.first())

        // a and b flow to the + Binary Op
        assertEquals(2, binaryOperatorAddition.prevDFG.size)
        assertTrue(binaryOperatorAddition.prevDFG.contains(b))
        assertTrue(binaryOperatorAddition.prevDFG.contains(rhsA))

        // The + binary op flows to the lhs
        assertEquals(1, binaryOperatorAddition.nextDFG.size)
        assertTrue(binaryOperatorAddition.nextDFG.contains(lhsA))
    }

    /** Test DFG when ReadWrite access occurs, such as compound operators or unary operators. */
    @Test
    @Throws(Exception::class)
    fun testCompoundOperatorDFG() {
        val topLevel = Path.of("src", "test", "resources", "dfg")
        val result =
            TestUtils.analyze(
                listOf(topLevel.resolve("compoundoperator.cpp").toFile()),
                topLevel,
                true
            )
        val rwCompoundOperator = TestUtils.findByUniqueName(result.allChildren(), "+=")
        assertNotNull(rwCompoundOperator)

        val expression = TestUtils.findByUniqueName(result.refs, "i")
        assertNotNull(expression)

        val prevDFGOperator = rwCompoundOperator.prevDFG
        assertNotNull(prevDFGOperator)
        assertTrue(prevDFGOperator.contains(expression))

        val nextDFGOperator = rwCompoundOperator.nextDFG
        assertNotNull(nextDFGOperator)
        assertTrue(nextDFGOperator.contains(expression))
    }

    @Test
    @Throws(Exception::class)
    fun testUnaryOperatorDFG() {
        val topLevel = Path.of("src", "test", "resources", "dfg")
        val result =
            TestUtils.analyze(
                listOf(topLevel.resolve("unaryoperator.cpp").toFile()),
                topLevel,
                true
            )
        val rwUnaryOperator = TestUtils.findByUniqueName(result.allChildren<UnaryOperator>(), "++")
        assertNotNull(rwUnaryOperator)

        val expression = TestUtils.findByUniqueName(result.refs, "i")
        assertNotNull(expression)

        val prevDFGOperator: Set<Node> = rwUnaryOperator.prevDFG
        val nextDFGOperator: Set<Node> = rwUnaryOperator.nextDFG
        assertTrue(prevDFGOperator.contains(expression))
        assertTrue(nextDFGOperator.contains(expression))
    }

    /**
     * Gets Integer Literal from the List of nodes to simplify the test syntax. The Literal is
     * expected to be contained in the list and the function will throw an
     * [IndexOutOfBoundsException] otherwise.
     *
     * @param nodes
     * - The list of nodes to filter for the Literal.
     *
     * @param v
     * - The integer value expected from the Literal.
     *
     * @return The Literal with the specified value.
     */
    private fun getLiteral(nodes: List<Node>, v: Int): Literal<*> {
        return nodes.filter { n: Node? -> n is Literal<*> && n.value == Integer.valueOf(v) }[0]
            as Literal<*>
    }

    /**
     * Traverses the DFG Graph induced by the provided node in the specified direction and retrieves
     * all nodes that are passed by and are therefore part of the incoming or outgoing data-flow.
     *
     * @param node
     * - The node that induces the DFG-subgraph for which nodes are retrieved
     *
     * @param outgoing
     * - true if the Data-Flow from this node should be considered, false if the data-flow is to
     *   this node.
     *
     * @return A set of nodes that are part of the data-flow
     */
    private fun flattenDFGGraph(node: Node?, outgoing: Boolean): Set<Node?> {
        if (node == null) {
            return setOf()
        }

        val dfgNodes = mutableSetOf<Node>()

        dfgNodes.add(node)
        val worklist = LinkedHashSet<Node>()
        worklist.add(node)

        while (worklist.isNotEmpty()) {
            val toProcess = worklist.iterator().next()
            worklist.remove(toProcess)
            // DataFlow direction
            val nextDFGNodes =
                if (outgoing) {
                    toProcess.nextDFG
                } else {
                    toProcess.prevDFG
                }
            // Adding all NEWLY discovered df-nodes to the work-list.
            for (dfgNode in nextDFGNodes) {
                if (!dfgNodes.contains(dfgNode)) {
                    worklist.add(dfgNode)
                    dfgNodes.add(dfgNode)
                }
            }
        }
        return dfgNodes
    }

    /**
     * Tests if the last artificial (implicit) return statement is removed by the
     * [ControlFlowSensitiveDFGPass].
     */
    @Test
    fun testReturnStatement() {
        val result = GraphExamples.getReturnTest()

        val returnFunction = result.functions["testReturn"]
        assertNotNull(returnFunction)

        assertEquals(2, returnFunction.prevDFG.size)

        val allRealReturns = returnFunction.allChildren<ReturnStatement> { it.location != null }
        assertEquals(allRealReturns.toSet() as Set<Node>, returnFunction.prevDFG)

        assertEquals(1, allRealReturns[0].prevDFG.size)
        assertTrue(returnFunction.literals.first { it.value == 2 } in allRealReturns[0].prevDFG)
        assertEquals(1, allRealReturns[1].prevDFG.size)
        assertTrue(
            returnFunction.refs.last { it.name.localName == "a" } in allRealReturns[1].prevDFG
        )
    }
}
