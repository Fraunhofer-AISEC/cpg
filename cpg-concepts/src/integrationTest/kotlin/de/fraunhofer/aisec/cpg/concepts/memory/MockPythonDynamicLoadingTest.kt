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
package de.fraunhofer.aisec.cpg.concepts.memory

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.concepts.memory.DynamicLoading
import de.fraunhofer.aisec.cpg.graph.concepts.memory.newDynamicLoading
import de.fraunhofer.aisec.cpg.graph.concepts.memory.newLoadSymbol
import de.fraunhofer.aisec.cpg.graph.declarations.ConstructorDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.edges.flows.CallingContextOut
import de.fraunhofer.aisec.cpg.graph.edges.flows.FullDataflowGranularity
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import de.fraunhofer.aisec.cpg.passes.concepts.ConceptPass
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.test.analyze
import de.fraunhofer.aisec.cpg.test.assertFullName
import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull

@DependsOn(SymbolResolver::class)
class MockPythonDynamicPass(ctx: TranslationContext) : ConceptPass(ctx) {
    override fun handleNode(node: Node, tu: TranslationUnitDeclaration) {
        when {
            node is CallExpression && node.name.toString() == "loader.Loader" -> {
                // Create a new DynamicLoading concept
                val dynamicLoading = newDynamicLoading(node)
                node.prevDFG += dynamicLoading
            }
            node is MemberExpression && node.name.localName == "impl" -> {
                var paths =
                    node.followDFGEdgesUntilHit(direction = Backward(GraphToFollow.DFG)) {
                        it is DynamicLoading
                    }
                paths.fulfilled.forEach { path ->
                    val dynamicLoading = path.last() as DynamicLoading

                    // Create an implicit construct expression
                    val construct = newConstructExpression("impl.simple.SimpleImplClass").implicit()
                    construct.type = with(node) { objectType("impl.simple.SimpleImplClass") }
                    node.prevDFG += construct

                    val loadSymbol =
                        newLoadSymbol<ConstructorDeclaration>(
                            node,
                            dynamicLoading,
                            what = null,
                            loader = null,
                            os = null,
                        )
                    node.prevDFGEdges.addContextSensitive(
                        node = construct,
                        granularity = FullDataflowGranularity,
                        callingContext =
                            CallingContextOut(dynamicLoading.underlyingNode as CallExpression),
                    )
                    println(loadSymbol)
                }
            }
        }
    }
}

class MockPythonDynamicLoadingTest {
    @Test
    fun testLoading() {
        val topLevel = File("src/integrationTest/resources/python")
        val result =
            analyze(listOf(), topLevel.toPath(), true) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<MockPythonDynamicPass>()
                it.softwareComponents(mutableMapOf("memory" to listOf(topLevel.resolve("memory"))))
            }
        assertNotNull(result)

        val barRefs = result.refs("bar")
        assertNotNull(barRefs)

        barRefs.forEach { bar ->
            assertFullName(
                "impl.simple.SimpleImplClass",
                bar.assignedTypes.singleOrNull { it !is UnknownType },
                "Assigned type should be 'impl.simple.SimpleImplClass'",
            )
        }
    }
}
