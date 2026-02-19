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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import kotlin.test.*

class NodeTest {
    @Test
    fun testId() {
        with(TestLanguageFrontend()) {
            val node1 = newLiteral(1)
            val node2 = newLiteral(2)

            // Check that the IDs are unique
            assert(node1.id != node2.id) { "Node IDs should be unique" }
        }
    }

    @Test
    fun testEdgeListComparisonInNodeEquality() {
        with(TestLanguageFrontend()) {
            val stmt1 = newLiteral(1)
            val stmt2 = newLiteral(2)
            val stmt3 = newLiteral(3)

            // Create a block
            val block1 = newBlock()
            block1.statements += stmt1
            block1.statements += stmt2

            // Clone the edge list (same parent node, same child nodes)
            val edgeListCopy = block1.statementEdges

            // Edge list should be equal to itself
            assertEquals(block1.statementEdges, edgeListCopy, "Edge list should be equal to itself")

            // Create a block with different statements
            val block2 = newBlock()
            block2.statements += stmt1
            block2.statements += stmt3

            assertNotEquals(
                block1.statementEdges,
                block2.statementEdges,
                "Edge lists with different nodes should not be equal",
            )

            // Create a block with different order
            val block3 = newBlock()
            block3.statements += stmt2
            block3.statements += stmt1

            assertNotEquals(
                block1.statementEdges,
                block3.statementEdges,
                "Edge lists with different order should not be equal",
            )

            // Create a block with different size
            val block4 = newBlock()
            block4.statements += stmt1

            assertNotEquals(
                block1.statementEdges,
                block4.statementEdges,
                "Edge lists with different sizes should not be equal",
            )

            // Test reference equality (not structural equality)
            val stmt1Copy = newLiteral(1)
            val block5 = newBlock()
            block5.statements += stmt1Copy
            block5.statements += stmt2

            assertNotEquals(
                block1.statementEdges,
                block5.statementEdges,
                "Edge lists with different node instances should not be equal",
            )
        }
    }

    @Test
    fun testEdgeListComparisonNoStackOverflow() {
        with(TestLanguageFrontend()) {
            val stmt1 = newLiteral(1)
            // Create two blocks that reference each other (circular reference)
            val block1 = newBlock()
            val block2 = newBlock()

            block1.statements += stmt1
            block1.statements += block2

            block2.statements += stmt1
            block2.statements += block1

            // Test that comparison completes without stack overflow
            // If Edge.equals used structural equality instead of reference equality,
            // this would cause infinite recursion: block1 -> block2 -> block1 -> ...
            // This should complete without throwing StackOverflowError
            block1.statementEdges == block2.statementEdges
        }
    }

    @Test
    fun testNodeEquals() {
        with(TestLanguageFrontend()) {
            // This test exercises the equals methods to improve code coverage
            // Nodes without locations will not be equal (they use Object identity)

            // FunctionDeclaration
            val func1 = newFunctionDeclaration("foo")
            val func2 = newFunctionDeclaration("foo")
            assertNotEquals(func1, func2, "Functions without location should not be equal")

            // CallExpression
            val call1 = newCallExpression(newReference("foo"))
            val call2 = newCallExpression(newReference("foo"))
            assertNotEquals(call1, call2, "Calls without location should not be equal")

            // RecordDeclaration
            val record1 = newRecordDeclaration("MyClass", kind = "class")
            val record2 = newRecordDeclaration("MyClass", kind = "class")
            assertNotEquals(record1, record2, "Records without location should not be equal")

            // TryStatement
            val try1 = newTryStatement()
            val try2 = newTryStatement()
            assertNotEquals(try1, try2, "Try statements without location should not be equal")

            // Statement
            val stmt1 = newDeclarationStatement()
            val stmt2 = newDeclarationStatement()
            assertNotEquals(stmt1, stmt2, "Statements without location should not be equal")

            // InitializerListExpression
            val init1 = newInitializerListExpression()
            val init2 = newInitializerListExpression()
            assertNotEquals(init1, init2, "Initializers without location should not be equal")

            // ExpressionList
            val exprList1 = newExpressionList()
            val exprList2 = newExpressionList()
            assertNotEquals(
                exprList1,
                exprList2,
                "Expression lists without location should not be equal",
            )

            // NewArrayExpression
            val arr1 = newNewArrayExpression()
            val arr2 = newNewArrayExpression()
            assertNotEquals(arr1, arr2, "Arrays without location should not be equal")
        }
    }
}
