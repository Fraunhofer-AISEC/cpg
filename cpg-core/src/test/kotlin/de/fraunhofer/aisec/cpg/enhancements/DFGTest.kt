/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.enhancements

import de.fraunhofer.aisec.cpg.TestUtils.analyze
import de.fraunhofer.aisec.cpg.TestUtils.compareLineFromLocationIfExists
import de.fraunhofer.aisec.cpg.TestUtils.findByUniqueName
import de.fraunhofer.aisec.cpg.TestUtils.flattenListIsInstance
import de.fraunhofer.aisec.cpg.TestUtils.getSubnodeOfTypeWithName
import de.fraunhofer.aisec.cpg.graph.AccessValues
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import java.nio.file.Path
import kotlin.test.*

internal class DFGTest {
    // Test DFG
    // Test ControlFlowSensitiveDFGPass
    @Test
    @Throws(Exception::class)
    fun testControlSensitiveDFGPassIfMerge() {
        val topLevel = Path.of("src", "test", "resources", "dfg")
        val result =
            analyze(
                java.util.List.of(topLevel.resolve("ControlFlowSensitiveDFGIfMerge.java").toFile()),
                topLevel,
                true
            )

        // Test If-Block
        val literal2 =
            flattenListIsInstance<Literal<*>>(result).filter { it: Literal<*> -> it.value == 2 }[0]
        val a2 =
            flattenListIsInstance<DeclaredReferenceExpression>(result)
                .filter { it: DeclaredReferenceExpression -> it.access == AccessValues.WRITE }[0]
        assertTrue(literal2.nextDFG.contains(a2))
        assertEquals(1, a2.nextDFG.size) // Outgoing DFG Edges only to VariableDeclaration

        val refersTo = a2.getRefersToAs(VariableDeclaration::class.java)
        assertNotNull(refersTo)
        assertEquals(0, refersTo.nextDFG.size)
        assertEquals(a2.nextDFG.iterator().next(), refersTo)

        // Test Else-Block with System.out.println()
        val literal1 =
            flattenListIsInstance<Literal<*>>(result).filter { it: Literal<*> -> it.value == 1 }[0]
        val println =
            flattenListIsInstance<CallExpression>(result)
                .filter { it: CallExpression -> it.name == "println" }[0]
        val a1 =
            flattenListIsInstance<DeclaredReferenceExpression>(result)
                .filter { it: DeclaredReferenceExpression -> it.nextEOG.contains(println) }[0]
        assertEquals(1, a1.prevDFG.size)
        assertEquals(literal1, a1.prevDFG.iterator().next())
        assertEquals(1, a1.nextEOG.size)
        assertEquals(println, a1.nextEOG[0])

        // Test Merging
        val b =
            flattenListIsInstance<VariableDeclaration>(result)
                .filter { it: VariableDeclaration -> it.name == "b" }[0]
        val ab = b.prevEOG[0] as DeclaredReferenceExpression
        assertTrue(literal1.nextDFG.contains(ab))
        assertTrue(literal2.nextDFG.contains(ab))
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
        val topLevel = Path.of("src", "test", "resources", "dfg")
        val result =
            analyze(
                listOf(topLevel.resolve("ControlFlowSensitiveDFGIfNoMerge.java").toFile()),
                topLevel,
                true
            )
        val b =
            flattenListIsInstance<VariableDeclaration>(result)
                .filter { it: VariableDeclaration -> it.name == "b" }
                .firstOrNull()
        assertNotNull(b)

        val ab = b.prevEOG[0] as DeclaredReferenceExpression
        val literal4 =
            flattenListIsInstance<Literal<*>>(result).filter { it: Literal<*> -> it.value == 4 }[0]
        assertTrue(literal4.nextDFG.contains(ab))
        assertEquals(1, ab.prevDFG.size)
    }

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
            analyze(listOf(topLevel.resolve("conditional_expression.cpp").toFile()), topLevel, true)
        val b =
            flattenListIsInstance<DeclaredReferenceExpression>(result)
                .filter { it: DeclaredReferenceExpression ->
                    it.name == "b" && it.location?.region?.startLine == 6
                }[0]
        val val2 =
            flattenListIsInstance<Literal<*>>(result).filter { it: Literal<*> -> it.value == 2 }[0]
        val val3 =
            flattenListIsInstance<Literal<*>>(result).filter { it: Literal<*> -> it.value == 3 }[0]
        assertEquals(2, b.prevDFG.size)
        assertTrue(b.prevDFG.contains(val2))
        assertTrue(b.prevDFG.contains(val3))
    }

    @Test
    @Throws(Exception::class)
    fun testControlSensitiveDFGPassSwitch() {
        val topLevel = Path.of("src", "test", "resources", "dfg")
        val result =
            analyze(
                listOf(topLevel.resolve("ControlFlowSensitiveDFGSwitch.java").toFile()),
                topLevel,
                true
            )
        val a =
            flattenListIsInstance<VariableDeclaration>(result)
                .filter { it: VariableDeclaration -> it.name == "a" }[0]
        val b =
            flattenListIsInstance<VariableDeclaration>(result)
                .filter { it: VariableDeclaration -> it.name == "b" }[0]
        val ab = b.prevEOG[0] as DeclaredReferenceExpression
        val a10 =
            flattenListIsInstance<DeclaredReferenceExpression>(result)
                .filter { it: DeclaredReferenceExpression ->
                    compareLineFromLocationIfExists(it, true, 8)
                }[0]
        val a11 =
            flattenListIsInstance<DeclaredReferenceExpression>(result)
                .filter { it: DeclaredReferenceExpression ->
                    compareLineFromLocationIfExists(it, true, 11)
                }[0]
        val a12 =
            flattenListIsInstance<DeclaredReferenceExpression>(result)
                .filter { it: DeclaredReferenceExpression ->
                    compareLineFromLocationIfExists(it, true, 14)
                }[0]
        val literal0 =
            flattenListIsInstance<Literal<*>>(result).filter { it: Literal<*> -> it.value == 0 }[0]
        val literal10 =
            flattenListIsInstance<Literal<*>>(result).filter { it: Literal<*> -> it.value == 10 }[0]
        val literal11 =
            flattenListIsInstance<Literal<*>>(result).filter { it: Literal<*> -> it.value == 11 }[0]
        val literal12 =
            flattenListIsInstance<Literal<*>>(result).filter { it: Literal<*> -> it.value == 12 }[0]
        assertEquals(3, literal10.nextDFG.size)
        assertTrue(literal10.nextDFG.contains(a10))
        assertEquals(3, literal11.nextDFG.size)
        assertTrue(literal11.nextDFG.contains(a11))
        assertEquals(4, literal12.nextDFG.size)
        assertTrue(literal12.nextDFG.contains(a12))
        assertEquals(4, a.prevDFG.size)
        assertTrue(a.prevDFG.contains(literal0))
        assertTrue(a.prevDFG.contains(a10))
        assertTrue(a.prevDFG.contains(a11))
        assertTrue(a.prevDFG.contains(a12))
        assertTrue(ab.prevDFG.contains(literal0))
        assertTrue(ab.prevDFG.contains(literal10))
        assertTrue(ab.prevDFG.contains(literal11))
        assertTrue(ab.prevDFG.contains(literal12))
        assertEquals(1, ab.nextDFG.size)
        assertTrue(ab.nextDFG.contains(b))

        // Fallthrough test
        val println =
            flattenListIsInstance<CallExpression>(result)
                .filter { it: CallExpression -> it.name == "println" }[0]
        val aPrintln =
            flattenListIsInstance<DeclaredReferenceExpression>(result)
                .filter { it: DeclaredReferenceExpression -> it.nextEOG.contains(println) }[0]
        assertEquals(2, aPrintln.prevDFG.size)
        assertTrue(aPrintln.prevDFG.contains(literal0))
        assertTrue(aPrintln.prevDFG.contains(literal12))
    }

    // Test DFG when ReadWrite access occurs, such as compoundoperators or unaryoperators
    @Test
    @Throws(Exception::class)
    fun testCompoundOperatorDFG() {
        val topLevel = Path.of("src", "test", "resources", "dfg")
        val result =
            analyze(listOf(topLevel.resolve("compoundoperator.cpp").toFile()), topLevel, true)
        val rwCompoundOperator =
            findByUniqueName<BinaryOperator>(flattenListIsInstance(result), "+=")
        assertNotNull(rwCompoundOperator)

        val expression =
            findByUniqueName(flattenListIsInstance<DeclaredReferenceExpression>(result), "i")
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
        val result = analyze(listOf(topLevel.resolve("unaryoperator.cpp").toFile()), topLevel, true)
        val rwUnaryOperator = findByUniqueName(flattenListIsInstance<UnaryOperator>(result), "++")
        assertNotNull(rwUnaryOperator)

        val expression =
            findByUniqueName(flattenListIsInstance<DeclaredReferenceExpression>(result), "i")
        assertNotNull(expression)

        val prevDFGOperator: Set<Node> = rwUnaryOperator.prevDFG
        val nextDFGOperator: Set<Node> = rwUnaryOperator.nextDFG
        assertTrue(prevDFGOperator.contains(expression))
        assertTrue(nextDFGOperator.contains(expression))
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
        val topLevel = Path.of("src", "test", "resources", "dfg")
        val result =
            analyze(
                java.util.List.of(topLevel.resolve("DelayedAssignmentAfterRHS.java").toFile()),
                topLevel,
                true
            )
        val binaryOperatorAssignment =
            findByUniqueName(flattenListIsInstance<BinaryOperator>(result), "=")
        assertNotNull(binaryOperatorAssignment)

        val binaryOperatorAddition =
            findByUniqueName(flattenListIsInstance<BinaryOperator>(result), "+")
        assertNotNull(binaryOperatorAddition)

        val varA = findByUniqueName(flattenListIsInstance<VariableDeclaration>(result), "a")
        assertNotNull(varA)

        val varB = findByUniqueName(flattenListIsInstance<VariableDeclaration>(result), "b")
        assertNotNull(varB)

        val lhsA = binaryOperatorAssignment.lhs as DeclaredReferenceExpression
        val rhsA = binaryOperatorAddition.lhs as DeclaredReferenceExpression
        val b = findByUniqueName(flattenListIsInstance<DeclaredReferenceExpression>(result), "b")
        assertNotNull(b)

        val literal0 =
            flattenListIsInstance<Literal<*>>(result).filter { it: Literal<*> -> it.value == 0 }[0]
        val literal1 =
            flattenListIsInstance<Literal<*>>(result).filter { it: Literal<*> -> it.value == 1 }[0]
        assertEquals(0, varA.nextDFG.size) // No outgoing DFG edges from VariableDeclaration a
        assertEquals(0, varB.nextDFG.size) // No outgoing DFG edges from VariableDeclaration b

        // Check that the replacement of the current value for VariableDeclaration a is delayed
        // until
        // the assignment is completed. This means that the DeclaredReferenceExpression on the rhs
        // must
        // contain a prev dfg edge to the previous valid value for VariableDeclaration a (literal 0)
        assertEquals(1, rhsA.prevDFG.size)
        assertTrue(rhsA.prevDFG.contains(literal0))

        // Check outgoing dfg edges of literal 0 (VariableDeclaration a initializer and rhs
        // expression
        // of a = a + b
        assertEquals(2, literal0.nextDFG.size)
        assertEquals(0, literal0.prevDFG.size)
        assertTrue(literal0.nextDFG.contains(varA))

        // Check incoming dfg edges of VariableDeclaration a (lhs of a = a + b, 0 and expr a + b
        assertEquals(2, varA.prevDFG.size)
        assertTrue(varA.prevDFG.contains(lhsA))
        assertTrue(varA.prevDFG.contains(literal0))

        // Check incoming dfg edges in binaryOperator + (DeclaredReferenceExpression a and b)
        assertEquals(2, binaryOperatorAddition.prevDFG.size)
        assertTrue(binaryOperatorAddition.prevDFG.contains(b))
        assertTrue(binaryOperatorAddition.prevDFG.contains(rhsA))

        // Check outgoing dfg edges from a of a = a + b and into
        // VariableDeclaration a)
        assertEquals(2, binaryOperatorAddition.nextDFG.size)
        assertTrue(binaryOperatorAddition.nextDFG.contains(lhsA))

        // Check outgoing dfg edges of literal1 (VariableDeclaration b and
        // DeclaredReferenceExpression
        // b)
        assertEquals(2, literal1.nextDFG.size)
        assertTrue(literal1.nextDFG.contains(varB))
        assertTrue(literal1.nextDFG.contains(b))
    }

    /**
     * Tests that there are no outgoing DFG edges from a VariableDeclaration
     *
     * @throws Exception
     */
    @Test
    @Throws(Exception::class)
    fun testNoOutgoingDFGFromVariableDeclaration() {
        val topLevel = Path.of("src", "test", "resources", "dfg")
        val result =
            analyze(java.util.List.of(topLevel.resolve("BasicSlice.java").toFile()), topLevel, true)
        val varA = findByUniqueName(flattenListIsInstance<VariableDeclaration>(result), "a")
        assertNotNull(varA)
        assertEquals(0, varA.nextDFG.size)
        assertEquals(7, varA.prevDFG.size)
    }

    @Test
    @Throws(Exception::class)
    fun testSensitivityThroughLoop() {
        val topLevel = Path.of("src", "test", "resources", "dfg")
        val result = analyze(listOf(topLevel.resolve("LoopDFGs.java").toFile()), topLevel, true)[0]
        val looping = getSubnodeOfTypeWithName(result, MethodDeclaration::class.java, "looping")
        val methodNodes = SubgraphWalker.flattenAST(looping)
        val l0 = getLiteral(methodNodes, 0)
        val l1 = getLiteral(methodNodes, 1)
        val l2 = getLiteral(methodNodes, 2)
        val l3 = getLiteral(methodNodes, 3)
        val calls =
            SubgraphWalker.flattenAST(looping).filter { n: Node ->
                n is CallExpression && n.name == "println"
            }
        val dfgNodes =
            flattenDFGGraph(
                getSubnodeOfTypeWithName(calls[0], DeclaredReferenceExpression::class.java, "a"),
                false
            )
        assertTrue(dfgNodes.contains(l0))
        assertTrue(dfgNodes.contains(l1))
        assertTrue(dfgNodes.contains(l2))
        assertFalse(dfgNodes.contains(l3))
    }

    /**
     * Gets Integer Literal from the List of nodes to simplify testsyntax. The Literal is expected
     * to be contained in the list and the function will throw an [IndexOutOfBoundsException]
     * otherwise.
     *
     * @param nodes
     * - The list of nodes to filter for the Literal.
     * @param v
     * - The integer value expected from the Literal.
     * @return The Literal with the specified value.
     */
    private fun getLiteral(nodes: List<Node>, v: Int): Literal<*> {
        return nodes.filter { n: Node? -> n is Literal<*> && n.value == Integer.valueOf(v) }[0]
            as Literal<*>
    }

    @Test
    @Throws(Exception::class)
    fun testSensitivityWithLabels() {
        val topLevel = Path.of("src", "test", "resources", "dfg")
        val result = analyze(listOf(topLevel.resolve("LoopDFGs.java").toFile()), topLevel, true)[0]
        val looping =
            getSubnodeOfTypeWithName(result, MethodDeclaration::class.java, "labeledBreakContinue")
        val methodNodes = SubgraphWalker.flattenAST(looping)
        val l0 = getLiteral(methodNodes, 0)
        val l1 = getLiteral(methodNodes, 1)
        val l2 = getLiteral(methodNodes, 2)
        val l3 = getLiteral(methodNodes, 3)
        val l4 = getLiteral(methodNodes, 4)
        val calls =
            SubgraphWalker.flattenAST(looping)
                .filter { n: Node -> n is CallExpression && n.name == "println" }
                .toMutableList()
        val dfgNodesA0 =
            flattenDFGGraph(
                getSubnodeOfTypeWithName(calls[0], DeclaredReferenceExpression::class.java, "a"),
                false
            )
        val dfgNodesA1 =
            flattenDFGGraph(
                getSubnodeOfTypeWithName(calls[1], DeclaredReferenceExpression::class.java, "a"),
                false
            )
        val dfgNodesA2 =
            flattenDFGGraph(
                getSubnodeOfTypeWithName(calls[2], DeclaredReferenceExpression::class.java, "a"),
                false
            )
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
     * Traverses the DFG Graph induced by the provided node in the specified direction and retrieves
     * all nodes that are passed by and are therefore part of the incoming or outgoing data-flow.
     *
     * @param node
     * - The node that induces the DFG-subgraph for which nodes are retrieved
     * @param outgoing
     * - true if the Data-Flow from this node should be considered, false if the data-flow is to
     * this node.
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
}
