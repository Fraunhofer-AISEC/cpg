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
package de.fraunhofer.aisec.cpg.query

import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.test.analyze
import kotlin.io.path.Path
import kotlin.test.*

class FlowQueriesTest {

    @Suppress("UNCHECKED_CAST")
    @Test
    fun testDataflowWithContext() {
        val topLevel = Path("src/integrationTest/resources/python")
        val result =
            analyze(listOf(topLevel.resolve("context.py").toFile()), topLevel, usePasses = true) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)

        // First, without context
        var q =
            result.allExtended<FunctionDeclaration>(
                sel = { it.name.localName.startsWith("endpoint") }
            ) { func ->
                val innerAuthorizeCalls =
                    func.followEOGEdgesUntilHit(
                        collectFailedPaths = false,
                        predicate = { node ->
                            node is CallExpression && node.name.localName == "inner_authorize"
                        },
                    )
                innerAuthorizeCalls.fulfilled
                    .map { path ->
                        val call = path.nodes.last()
                        val flow =
                            dataFlow(
                                startNode = call,
                                type = Must,
                                direction = Backward(GraphToFollow.DFG),
                                predicate = { it is Literal<*> && it.value == func.name.localName },
                            )
                        flow
                    }
                    .mergeWithAll()
            }
        assertNotNull(q)
        assertEquals(3, q.children.size, "Expected 3 paths for the 3 endpoints")

        var path0 = q.children[0].children[0] as QueryTree<Boolean>
        assertFalse(path0.value, "Without context, expected false for the first endpoint")

        var path1 = q.children[1].children[0] as QueryTree<Boolean>
        assertFalse(path1.value, "Without context, expected false for the second endpoint")

        var path2 = q.children[2].children[0] as QueryTree<Boolean>
        assertFalse(path2.value, "Without context, expected false for the third endpoint")

        // Now, with context
        q =
            result.allExtended<FunctionDeclaration>(
                sel = { it.name.localName.startsWith("endpoint") }
            ) { func ->
                val innerAuthorizeCalls =
                    func.followEOGEdgesUntilHit(
                        collectFailedPaths = false,
                        predicate = { node ->
                            node is CallExpression && node.name.localName == "inner_authorize"
                        },
                    )
                innerAuthorizeCalls.fulfilled
                    .map { path ->
                        val call = path.nodes.lastOrNull() as? CallExpression
                        assertNotNull(call, "Expected last node to be a CallExpression")

                        val flow =
                            dataFlow(
                                startNode = call,
                                type = Must,
                                direction = Backward(GraphToFollow.DFG),
                                ctx = Context.ofCallStack(assertNotNull(func.dCalls["authorize"])),
                                predicate = { node ->
                                    node is Literal<*> && node.value == func.name.localName
                                },
                            )
                        flow
                    }
                    .mergeWithAll()
            }
        assertNotNull(q)

        assertEquals(3, q.children.size, "Expected 3 paths for the 3 endpoints")

        path0 = q.children[0].children[0] as QueryTree<Boolean>
        assertTrue(path0.value, "With context, expected true for the first endpoint")

        path1 = q.children[1].children[0] as QueryTree<Boolean>
        assertTrue(path1.value, "With context, expected true for the second endpoint")

        path2 = q.children[2].children[0] as QueryTree<Boolean>
        assertFalse(path2.value, "With context, expected false for the third endpoint")
    }
}
