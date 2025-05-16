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
package de.fraunhofer.aisec.cpg.concepts

import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage
import de.fraunhofer.aisec.cpg.graph.concepts.policy.Boundary
import de.fraunhofer.aisec.cpg.graph.concepts.policy.CheckAccess
import de.fraunhofer.aisec.cpg.graph.concepts.policy.Context
import de.fraunhofer.aisec.cpg.graph.concepts.policy.Principal
import de.fraunhofer.aisec.cpg.graph.concepts.policy.ProtectedAsset
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.returns
import de.fraunhofer.aisec.cpg.graph.statements.IfStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.passes.concepts.TagOverlaysPass
import de.fraunhofer.aisec.cpg.passes.concepts.each
import de.fraunhofer.aisec.cpg.passes.concepts.getOverlaysByPrevDFG
import de.fraunhofer.aisec.cpg.passes.concepts.tag
import de.fraunhofer.aisec.cpg.passes.concepts.with
import de.fraunhofer.aisec.cpg.test.analyze
import kotlin.io.path.Path
import kotlin.io.resolve
import kotlin.test.Test
import kotlin.test.assertNotNull

class PolicyTest {
    @Test
    fun testPolicy() {
        val topLevel = Path("src/integrationTest/resources/python")
        val result =
            analyze(listOf(topLevel.resolve("policy.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<TagOverlaysPass>()
                it.configurePass<TagOverlaysPass>(
                    TagOverlaysPass.Configuration(
                        tag =
                            tag {
                                each<RecordDeclaration>(
                                        predicate = { node -> node.name.localName == "Team" }
                                    )
                                    .with { Boundary(underlyingNode = node) }
                                each<FieldDeclaration>(
                                        predicate = { node -> node.name.localName == "__members" }
                                    )
                                    .with {
                                        ProtectedAsset(underlyingNode = node, scope = node.scope)
                                    }
                                each<FieldDeclaration>(
                                        predicate = { node -> node.name.localName == "manager" }
                                    )
                                    .with { Principal(underlyingNode = node) }
                                each<Reference>(
                                        predicate = { node -> node.name.localName == "whoami" }
                                    )
                                    .with { Context(underlyingNode = node) }
                                each<IfStatement>().with {
                                    val condition = node.condition as? BinaryOperator
                                    val lhs = condition?.lhs
                                    val rhs = condition?.rhs
                                    val context =
                                        lhs?.getOverlaysByPrevDFG<Context>(state)?.singleOrNull()
                                    val principal =
                                        rhs?.getOverlaysByPrevDFG<Principal>(state)?.singleOrNull()
                                    val returns = condition.returns
                                    val protectedAsset =
                                        returns
                                            .mapNotNull { it.returnValue }
                                            .flatMap {
                                                it.getOverlaysByPrevDFG<ProtectedAsset>(state)
                                            }
                                            .singleOrNull()

                                    CheckAccess(underlyingNode = condition, asset = protectedAsset)
                                }
                            }
                    )
                )
            }
        assertNotNull(result)
    }
}
