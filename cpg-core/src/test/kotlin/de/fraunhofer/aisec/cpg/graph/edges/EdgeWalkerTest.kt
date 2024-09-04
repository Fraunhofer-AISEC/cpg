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
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.graph.edges.allEdges
import de.fraunhofer.aisec.cpg.graph.edges.ast.AstEdge
import de.fraunhofer.aisec.cpg.graph.edges.astEdges
import de.fraunhofer.aisec.cpg.graph.edges.edges
import de.fraunhofer.aisec.cpg.graph.newBlock
import de.fraunhofer.aisec.cpg.graph.newCallExpression
import de.fraunhofer.aisec.cpg.graph.newFunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.newReference
import kotlin.test.Test
import kotlin.test.assertEquals

class EdgeWalkerTest {
    @Test
    fun testExtract() {
        with(TestLanguageFrontend()) {
            var node = newFunctionDeclaration("do")
            node.body = newBlock()

            node.prevDFG += newCallExpression(newReference("do"))

            var edges = node.edges<Edge<*>>()
            assertEquals(2, edges.size)

            edges = node.edges { it is AstEdge }
            assertEquals(1, edges.size)

            var all = node.allEdges<Edge<*>>()
            assertEquals(3, all.size)

            var allAst = node.astEdges
            assertEquals(1, allAst.size)
        }
    }
}
