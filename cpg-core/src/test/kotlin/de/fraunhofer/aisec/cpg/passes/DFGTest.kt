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
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.test.*
import kotlin.test.*

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
        val result = GraphExamples.getConditionalExpression()

        val bJoin = result.refs[{ it.name.localName == "b" && it.location?.region?.startLine == 6 }]
        val a5 =
            result.refs[
                    {
                        it.name.localName == "a" &&
                            it.location?.region?.startLine == 5 &&
                            it.location?.region?.startColumn == 3
                    }]
        val a6 = result.refs[{ it.name.localName == "a" && it.location?.region?.startLine == 6 }]
        val bCond =
            result.refs[
                    {
                        it.name.localName == "b" &&
                            it.location?.region?.startLine == 5 &&
                            it.location?.region?.startColumn == 12
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

        val binaryOperatorAssignment = findByUniqueName(result.allChildren<AssignExpression>(), "=")
        assertNotNull(binaryOperatorAssignment)

        val binaryOperatorAddition = findByUniqueName(result.allChildren<BinaryOperator>(), "+")
        assertNotNull(binaryOperatorAddition)

        val varA = findByUniqueName(result.variables, "a")
        assertNotNull(varA)

        val varB = findByUniqueName(result.variables, "b")
        assertNotNull(varB)

        val lhsA = binaryOperatorAssignment.lhs.first() as Reference
        val rhsA = binaryOperatorAddition.lhs as Reference
        val b = findByUniqueName(result.refs, "b")
        assertNotNull(b)

        val literal0 = result.literals[{ it.value == 0 }]
        assertNotNull(literal0)

        val literal1 = result.literals[{ it.value == 1 }]
        assertNotNull(literal1)
        // a and b flow to the References in (a+b)
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
        val result = GraphExamples.getCompoundOperator()

        val rwCompoundOperator = findByUniqueName(result.allChildren(), "+=")
        assertNotNull(rwCompoundOperator)

        val expression = findByUniqueName(result.refs, "i")
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
        val result = GraphExamples.getUnaryOperator()

        val rwUnaryOperator = findByUniqueName(result.allChildren<UnaryOperator>(), "++")
        assertNotNull(rwUnaryOperator)

        val expression = findByUniqueName(result.refs, "i")
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
    // TODO for merge
    @Ignore
    @Test
    fun testReturnStatement() {
        val result = GraphExamples.getReturnTest()

        val returnFunction = result.functions["testReturn"]
        assertNotNull(returnFunction)

        assertEquals(2, returnFunction.prevDFG.size)

        val allRealReturns = returnFunction.allChildren<ReturnStatement> { !it.isImplicit }
        assertEquals(allRealReturns.toSet() as Set<Node>, returnFunction.prevDFG)

        assertEquals(1, allRealReturns[0].prevDFG.size)
        assertTrue(returnFunction.literals.first { it.value == 2 } in allRealReturns[0].prevDFG)
        assertEquals(1, allRealReturns[1].prevDFG.size)
        assertTrue(
            returnFunction.refs.last { it.name.localName == "a" } in allRealReturns[1].prevDFG
        )
    }

    @Test
    @Throws(Exception::class)
    fun testSensitivityThroughLoop() {
        val result = GraphExamples.getLoopingDFG()
        val looping = result.methods["looping"]
        val methodNodes = SubgraphWalker.flattenAST(looping)
        val l0 = getLiteral(methodNodes, 0)
        val l1 = getLiteral(methodNodes, 1)
        val l2 = getLiteral(methodNodes, 2)
        val l3 = getLiteral(methodNodes, 3)
        val calls =
            SubgraphWalker.flattenAST(looping).filter { n: Node ->
                n is CallExpression && n.name.localName == "println"
            }
        val dfgNodes = flattenDFGGraph(calls[0].refs["a"], false)
        assertTrue(dfgNodes.contains(l0))
        assertTrue(dfgNodes.contains(l1))
        assertTrue(dfgNodes.contains(l2))
        assertFalse(dfgNodes.contains(l3))
    }

    // TODO for merge
    @Ignore
    @Test
    @Throws(Exception::class)
    fun testSensitivityWithLabels() {
        val result = GraphExamples.getLabeledBreakContinueLoopDFG()
        val looping = result.methods["labeledBreakContinue"]
        val methodNodes = SubgraphWalker.flattenAST(looping)
        val l0 = getLiteral(methodNodes, 0)
        val l1 = getLiteral(methodNodes, 1)
        val l2 = getLiteral(methodNodes, 2)
        val l3 = getLiteral(methodNodes, 3)
        val l4 = getLiteral(methodNodes, 4)
        val calls =
            SubgraphWalker.flattenAST(looping)
                .filter { n: Node -> n is CallExpression && n.name.localName == "println" }
                .toMutableList()
        val dfgNodesA0 = flattenDFGGraph(calls[0].refs["a"], false)
        val dfgNodesA1 = flattenDFGGraph(calls[1].refs["a"], false)
        val dfgNodesA2 = flattenDFGGraph(calls[2].refs["a"], false)
        assertEquals(3, calls[0].refs["a"]?.prevDFG?.size)
        assertTrue(dfgNodesA0.contains(l0))
        assertTrue(dfgNodesA0.contains(l1))
        assertTrue(dfgNodesA0.contains(l3))
        assertFalse(dfgNodesA0.contains(l4))
        assertTrue(dfgNodesA1.contains(l0))
        assertTrue(dfgNodesA1.contains(l1))
        assertTrue(dfgNodesA1.contains(l3))
        assertFalse(dfgNodesA1.contains(l4))
        assertTrue(dfgNodesA2.contains(l0))
        assertTrue(dfgNodesA2.contains(l1))
        assertTrue(dfgNodesA2.contains(l2))
        assertTrue(dfgNodesA2.contains(l3))
        assertFalse(dfgNodesA2.contains(l4))
    }

    /**
     * Tests the ControlFlowSensitiveDFGPass and checks if an assignment located within one block
     * clears the values from the map and includes only the new (assigned) value.
     *
     * @throws Exception Any exception that happens during the analysis process
     */
    @Test
    @Throws(Exception::class)
    fun testControlSensitiveDFGPassIfNoMerge() {
        val result = GraphExamples.getControlFlowSensitiveDFGIfNoMerge()

        val b = result.variables["b"]
        assertNotNull(b)

        val ab = b.nextEOG[0] as Reference
        val literal4 = result.literals[{ it.value == 4 }]
        assertNotNull(literal4)

        val a4 = ab.prevDFG.first { it is Reference }
        assertTrue(literal4.nextDFG.contains(a4))
        assertEquals(1, ab.prevDFG.size)
    }

    @Test
    @Throws(Exception::class)
    fun testControlSensitiveDFGPassIfMerge() {
        val result = GraphExamples.getControlFlowSensitiveDFGIfMerge()

        // Test If-Block
        val literal2 = result.literals[{ it.value == 2 }]
        assertNotNull(literal2)

        val b = result.variables["b"]
        assertNotNull(b)

        val a2 = result.refs[{ it.access == AccessValues.WRITE }]
        assertNotNull(a2)
        assertTrue(literal2.nextDFG.contains(a2))
        assertEquals(
            1,
            a2.nextDFG.size,
        ) // Outgoing DFG Edges only to the Reference in the assignment to b
        assertEquals(b.initializer!!, a2.nextDFG.first())

        val refersTo = a2.getRefersToAs(VariableDeclaration::class.java)
        assertNotNull(refersTo)
        assertEquals(2, refersTo.nextDFG.size) // The print and assignment to b
        // Outgoing DFG Edge to the Reference in the assignment of b
        assertTrue(refersTo.nextDFG.contains(b.initializer!!))

        // Test Else-Block with System.out.println()
        val literal1 = result.literals[{ it.value == 1 }]
        assertNotNull(literal1)
        val println = result.calls["println"]
        assertNotNull(println)
        val aPrintln = println.arguments[0]
        assertTrue(refersTo.nextDFG.contains(aPrintln))

        assertEquals(1, aPrintln.prevDFG.size)
        assertEquals(refersTo, aPrintln.prevDFG.first())
        assertEquals(1, aPrintln.nextEOG.size)
        assertEquals(println, aPrintln.nextEOG[0])

        val ab = b.nextEOG[0] as Reference
        assertTrue(refersTo.nextDFG.contains(ab))
        assertTrue(a2.nextDFG.contains(ab))
    }

    @Test
    @Throws(Exception::class)
    fun testControlSensitiveDFGPassSwitch() {
        val result = GraphExamples.getControlFlowSesitiveDFGSwitch()

        val a = result.variables["a"]
        assertNotNull(a)

        val b = result.variables["b"]
        assertNotNull(b)

        val ab = b.nextEOG[0] as Reference
        val a10 = result.refs[{ compareLineFromLocationIfExists(it, true, 8) }]
        val a11 = result.refs[{ compareLineFromLocationIfExists(it, true, 11) }]
        val a12 = result.refs[{ compareLineFromLocationIfExists(it, true, 14) }]
        assertNotNull(a10)
        assertNotNull(a11)
        assertNotNull(a12)

        val literal0 = result.literals[{ it.value == 0 }]
        val literal10 = result.literals[{ it.value == 10 }]
        val literal11 = result.literals[{ it.value == 11 }]
        val literal12 = result.literals[{ it.value == 12 }]
        assertNotNull(literal0)
        assertNotNull(literal10)
        assertNotNull(literal11)
        assertNotNull(literal12)

        assertEquals(1, literal10.nextDFG.size)
        assertTrue(literal10.nextDFG.contains(a10))
        assertEquals(1, literal11.nextDFG.size)
        assertTrue(literal11.nextDFG.contains(a11))
        assertEquals(1, literal12.nextDFG.size)
        assertTrue(literal12.nextDFG.contains(a12))
        assertEquals(1, a.prevDFG.size)
        assertTrue(a.prevDFG.contains(literal0))
        assertFalse(a.prevDFG.contains(a10))
        assertFalse(a.prevDFG.contains(a11))
        assertFalse(a.prevDFG.contains(a12))

        assertEquals(1, ab.nextDFG.size)
        assertTrue(ab.nextDFG.contains(b))

        // Fallthrough test
        val println = result.calls["println"]
        assertNotNull(println)

        val aPrintln = result.refs[{ it.nextEOG.contains(println) }]
        assertNotNull(aPrintln)
        assertEquals(2, aPrintln.prevDFG.size)
        assertTrue(aPrintln.prevDFG.contains(a))
        assertTrue(aPrintln.prevDFG.contains(a12))
    }

    /**
     * Tests that the outgoing DFG edges from a VariableDeclaration go to references with a path
     * without a new assignment to the variable.
     *
     * @throws Exception
     */
    @Test
    @Throws(Exception::class)
    fun testOutgoingDFGFromVariableDeclaration() {
        // TODO: IMHO this test is quite useless and can be merged into another one (e.g.
        //   testControlSensitiveDFGPassIfMerge).
        val result = GraphExamples.getBasicSlice()

        val varA = findByUniqueName(result.variables, "a")
        assertNotNull(varA)
        // The variable can flow to lines 19, 23, 24, 26, 31, 34 without modifications.
        assertEquals(6, varA.nextDFG.size)
        assertEquals(1, varA.prevDFG.size) // Only the initializer should flow there.
    }

    @Test
    fun testInitializerListExpression() {
        val result = GraphExamples.getInitializerListExprDFG()
        val variable = result.variables["i"]
        assertNotNull(variable)
        assertEquals(1, variable.prevDFG.size)
        val initializer = variable.prevDFG.first()
        assertEquals(1, initializer.prevDFG.size)
    }
}
