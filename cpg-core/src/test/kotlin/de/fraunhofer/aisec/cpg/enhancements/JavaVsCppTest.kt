/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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

import com.github.javaparser.utils.Pair
import de.fraunhofer.aisec.cpg.BaseTest
import de.fraunhofer.aisec.cpg.TestUtils.analyzeAndGetFirstTU
import de.fraunhofer.aisec.cpg.assertLocalName
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.IfStatement
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import java.io.File
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class JavaVsCppTest : BaseTest() {
    @Test
    @Throws(Exception::class)
    fun cpp() {
        analyzeAndSave("src/test/resources/javaVsCpp/simple.cpp")
    }

    @Test
    @Throws(Exception::class)
    fun java() {
        analyzeAndSave("src/test/resources/javaVsCpp/simple.java")
    }

    @Throws(Exception::class)
    private fun analyzeAndSave(pathname: String) {
        val toTranslate = File(pathname)
        val topLevel = toTranslate.parentFile.toPath()
        val tu = analyzeAndGetFirstTU(listOf(toTranslate), topLevel, true)
        assertEquals(1, tu.declarations.size)

        val decl = tu.declarations[0]
        assertTrue(decl is RecordDeclaration)
        assertLocalName("Simple", decl)
        assertEquals(2, decl.methods.size)
        assertEquals("class", decl.kind)

        val methodDeclaration = decl.methods[0]
        var worklist = methodDeclaration.nextEOG
        val nodes = HashMap<Node, Int>()
        val edges = HashSet<Pair<Int, Int>>()
        var currentId = 0
        while (worklist.isNotEmpty()) {
            val next: MutableList<Node> = ArrayList()
            for (n in worklist) {
                val nodeId = currentId
                if (!nodes.containsKey(n)) {
                    nodes[n] = nodeId
                    currentId++
                }
                assertNotNull(n.code)
                for (successor in n.nextEOG) {
                    var successorId = nodes[successor]
                    if (successorId == null) {
                        successorId = currentId
                        nodes[successor] = currentId
                        currentId++
                    }
                    edges.add(Pair(nodeId, successorId))
                    next.add(successor)
                }
            }
            worklist = next
        }
        val sorted = TreeMap<Int, Node>()
        nodes.forEach { (n: Node, i: Int) -> sorted[i] = n }

        val nodeNames = mutableListOf<Class<out Node>>()
        sorted.forEach { (_: Int, n: Node) -> nodeNames += n.javaClass }
        assertEquals(
            listOf(
                Literal::class.java,
                VariableDeclaration::class.java,
                DeclarationStatement::class.java,
                DeclaredReferenceExpression::class.java,
                Literal::class.java,
                BinaryOperator::class.java,
                IfStatement::class.java,
                DeclaredReferenceExpression::class.java,
                DeclaredReferenceExpression::class.java,
                DeclaredReferenceExpression::class.java,
                CallExpression::class.java,
                BinaryOperator::class.java,
                ReturnStatement::class.java,
                CompoundStatement::class.java
            ),
            nodeNames
        )

        val collect = edges.sortedWith(Comparator.comparing(Pair<Int, Int>::a))
        assertEquals(
            listOf(
                Pair(0, 1),
                Pair(2, 2),
                Pair(3, 3),
                Pair(4, 4),
                Pair(5, 5),
                Pair(6, 6),
                Pair(7, 7),
                Pair(7, 8),
                Pair(9, 9),
                Pair(10, 10),
                Pair(11, 11),
                Pair(12, 12),
                Pair(13, 13),
                Pair(14, 8),
                Pair(14, 10),
                Pair(14, 12)
            ),
            collect
        )
    }
}
