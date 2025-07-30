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
package de.fraunhofer.aisec.cpg.graph.declarations

import de.fraunhofer.aisec.cpg.*
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.builder.*
import de.fraunhofer.aisec.cpg.graph.edges.flows.FullDataflowGranularity
import de.fraunhofer.aisec.cpg.graph.edges.flows.IndexedDataflowGranularity
import de.fraunhofer.aisec.cpg.graph.objectType
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.types.TupleType
import de.fraunhofer.aisec.cpg.test.*
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class TupleDeclarationTest {
    @Test
    fun testTopLevelTuple() {
        with(
            TestLanguageFrontend(
                ctx = TranslationContext(TranslationConfiguration.builder().defaultPasses().build())
            )
        ) {
            val result = build {
                translationResult {
                    translationUnit {
                        function(
                            "func",
                            returnTypes = listOf(objectType("MyClass"), objectType("error")),
                        )

                        // I fear this is too complex for the fluent DSL; so we just use the node
                        // builder here
                        val tuple =
                            newTupleDeclaration(
                                listOf(newVariableDeclaration("a"), newVariableDeclaration("b")),
                                newCallExpression(newReference("func")),
                            )
                        scopeManager.addDeclaration(tuple)
                        declare { this.singleDeclaration = tuple }
                        // declarations += tuple

                        tuple.elements.forEach { scopeManager.addDeclaration(it) }

                        function("main") { body { call("print") { ref("a") } } }
                    }
                }
            }

            val main = result.allFunctions["main"]
            assertNotNull(main)

            val tuple = result.allVariables["(a,b)"]
            assertNotNull(tuple)
            assertIs<TupleDeclaration>(tuple)
            assertIs<TupleType>(tuple.type)

            val call = tuple.initializer as? CallExpression
            assertNotNull(call)
            assertInvokes(call, result.allFunctions["func"])

            val tuplePrevDFG = tuple.prevDFGEdges.singleOrNull()
            assertNotNull(tuplePrevDFG)
            assertEquals(call, tuplePrevDFG.start)
            assertIs<FullDataflowGranularity>(tuplePrevDFG.granularity)

            val a = tuple.elements["a"]
            assertNotNull(a)
            assertLocalName("MyClass", a.type)
            assertEquals(tuple, a.astParent)
            val aPrevDFG = a.prevDFGEdges.singleOrNull()
            assertNotNull(aPrevDFG)
            assertEquals(tuple, aPrevDFG.start)
            val aPrevDFGGranularity = aPrevDFG.granularity
            assertIs<IndexedDataflowGranularity>(aPrevDFGGranularity)
            assertEquals(0, aPrevDFGGranularity.partialTarget)

            val b = tuple.elements["b"]
            assertNotNull(b)
            assertLocalName("error", b.type)
            assertEquals(tuple, b.astParent)
            val bPrevDFG = b.prevDFGEdges.singleOrNull()
            assertNotNull(bPrevDFG)
            assertEquals(tuple, bPrevDFG.start)
            val bPrevDFGGranularity = bPrevDFG.granularity
            assertIs<IndexedDataflowGranularity>(bPrevDFGGranularity)
            assertEquals(1, bPrevDFGGranularity.partialTarget)

            val callPrint = main.allCalls["print"]
            assertNotNull(callPrint)
            assertIs<CallExpression>(callPrint)

            val arg = callPrint.arguments<Reference>(0)
            assertNotNull(arg)
            assertRefersTo(arg, a)
            assertContains(arg.prevDFG, a)
        }
    }

    @Test
    fun testFunctionLevelTuple() {
        with(
            TestLanguageFrontend(
                ctx = TranslationContext(TranslationConfiguration.builder().defaultPasses().build())
            )
        ) {
            val result = build {
                translationResult {
                    translationUnit {
                        function(
                            "func",
                            returnTypes = listOf(objectType("MyClass"), objectType("error")),
                        )

                        function("main") {
                            body {
                                declare {
                                    // I fear this is too complex for the fluent DSL; so we just use
                                    // the node builder here
                                    val tuple =
                                        newTupleDeclaration(
                                            listOf(
                                                newVariableDeclaration("a"),
                                                newVariableDeclaration("b"),
                                            ),
                                            newCallExpression(newReference("func")),
                                        )
                                    scopeManager.addDeclaration(tuple)
                                    declarations += tuple

                                    tuple.elements.forEach { scopeManager.addDeclaration(it) }
                                }
                                call("print") { ref("a") }
                            }
                        }
                    }
                }
            }

            val main = result.allFunctions["main"]
            assertNotNull(main)

            val tuple = main.allVariables["(a,b)"]
            assertNotNull(tuple)
            assertIs<TupleDeclaration>(tuple)
            assertIs<TupleType>(tuple.type)

            val call = tuple.initializer
            assertIs<CallExpression>(call)
            assertInvokes(call, result.allFunctions["func"])
            assertEquals(setOf<Node>(call), tuple.prevDFG)

            val a = tuple.elements["a"]
            assertNotNull(a)
            assertLocalName("MyClass", a.type)
            assertEquals(tuple, a.astParent)
            val aPrevDFG = a.prevDFGEdges.singleOrNull()
            assertNotNull(aPrevDFG)
            assertEquals(tuple, aPrevDFG.start)
            val aPrevDFGGranularity = aPrevDFG.granularity
            assertIs<IndexedDataflowGranularity>(aPrevDFGGranularity)
            assertEquals(0, aPrevDFGGranularity.partialTarget)

            val b = tuple.elements["b"]
            assertNotNull(b)
            assertLocalName("error", b.type)
            assertEquals(tuple, b.astParent)
            val bPrevDFG = b.prevDFGEdges.singleOrNull()
            assertNotNull(bPrevDFG)
            assertEquals(tuple, bPrevDFG.start)
            val bPrevDFGGranularity = bPrevDFG.granularity
            assertIs<IndexedDataflowGranularity>(bPrevDFGGranularity)
            assertEquals(1, bPrevDFGGranularity.partialTarget)

            val callPrint = main.allCalls["print"]
            assertNotNull(callPrint)
            assertIs<CallExpression>(callPrint)

            val arg = callPrint.arguments<Reference>(0)
            assertNotNull(arg)
            assertRefersTo(arg, a)
            assertContains(arg.prevDFG, a)
        }
    }
}
