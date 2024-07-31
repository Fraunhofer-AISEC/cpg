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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class NodeBuilderTest {

    @Test
    fun testNestedNodeBuilder() {
        with(TestLanguageFrontend()) {
            val tu =
                newTranslationUnitDeclaration("my.file").withChildren(isGlobalScope = true) {
                    val func =
                        newFunctionDeclaration("main").withChildren(hasScope = true) {
                            val param = newParameterDeclaration("param1")

                            scopeManager.addDeclaration(param)
                        }

                    scopeManager.addDeclaration(func)
                }

            tu.nodes
                .filter { it != tu }
                .forEach { assertNotNull(it.astParent, "${it.name} has no parent") }
        }
    }

    @Test
    fun testWithParent() {
        with(TestLanguageFrontend()) {
            fun create(isDeref: Boolean): Expression {
                return newBlock().withChildren {
                    val expr = newReference("p")

                    it +=
                        if (isDeref) {
                            newUnaryOperator("*", prefix = true, postfix = false).withChildren {
                                it.input = expr.withParent()
                            }
                        } else {
                            expr
                        }
                }
            }

            val node1 = create(false)
            var ref = node1.refs.firstOrNull()
            assertNotNull(ref)
            assertIs<Block>(ref.astParent)

            val node2 = create(true)
            ref = node2.refs.firstOrNull()
            assertNotNull(ref)
            assertIs<UnaryOperator>(ref.astParent)
        }
    }
}
