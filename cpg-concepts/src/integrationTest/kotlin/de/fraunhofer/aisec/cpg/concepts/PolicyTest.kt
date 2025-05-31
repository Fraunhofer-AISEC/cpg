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
import de.fraunhofer.aisec.cpg.graph.Backward
import de.fraunhofer.aisec.cpg.graph.GraphToFollow
import de.fraunhofer.aisec.cpg.graph.concepts.policy.Boundary
import de.fraunhofer.aisec.cpg.graph.concepts.policy.CheckAccess
import de.fraunhofer.aisec.cpg.graph.concepts.policy.Context
import de.fraunhofer.aisec.cpg.graph.concepts.policy.Principal
import de.fraunhofer.aisec.cpg.graph.concepts.policy.ProtectedAsset
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.followPrevCDGUntilHit
import de.fraunhofer.aisec.cpg.graph.returns
import de.fraunhofer.aisec.cpg.graph.statements.IfStatement
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.passes.ControlDependenceGraphPass
import de.fraunhofer.aisec.cpg.passes.concepts.TagOverlaysPass
import de.fraunhofer.aisec.cpg.passes.concepts.each
import de.fraunhofer.aisec.cpg.passes.concepts.getOverlaysByPrevDFG
import de.fraunhofer.aisec.cpg.passes.concepts.tag
import de.fraunhofer.aisec.cpg.passes.concepts.with
import de.fraunhofer.aisec.cpg.query.QueryTree
import de.fraunhofer.aisec.cpg.query.allExtended
import de.fraunhofer.aisec.cpg.query.dataFlow
import de.fraunhofer.aisec.cpg.test.analyze
import kotlin.io.path.Path
import kotlin.io.resolve
import kotlin.test.Test
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Assertions.assertFalse

class PolicyTest {
    @Test
    fun testPolicy() {
        val topLevel = Path("src/integrationTest/resources/python")
        val result =
            analyze(listOf(topLevel.resolve("policy.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<TagOverlaysPass>()
                it.registerPass<ControlDependenceGraphPass>()
                it.configurePass<TagOverlaysPass>(
                    TagOverlaysPass.Configuration(
                        tag =
                            tag {
                                // Tag the class "Team" with the concept "Boundary"
                                each<RecordDeclaration>(
                                        predicate = { node -> node.name.localName == "Team" }
                                    )
                                    .with { Boundary(underlyingNode = node) }
                                // Tag the field "__members" with the concept "ProtectedAsset"
                                each<FieldDeclaration>(
                                        predicate = { node -> node.name.localName == "__members" }
                                    )
                                    .with {
                                        ProtectedAsset(underlyingNode = node, scope = node.scope)
                                    }
                                // Tag the field "manager" with the concept "Principal"
                                each<FieldDeclaration>(
                                        predicate = { node -> node.name.localName == "manager" }
                                    )
                                    .with { Principal(underlyingNode = node) }
                                // Tag the reference "whoami" with the concept "Context"
                                each<Reference>(
                                        predicate = { node -> node.name.localName == "whoami" }
                                    )
                                    .with { Context(underlyingNode = node) }
                                // Tag IfStatements that compare a Context with a Principal and that
                                // check access to a ProtectedRessource with "CheckAccess"
                                each<IfStatement>().with {
                                    val condition = node.condition as? BinaryOperator
                                    val lhs = condition?.lhs
                                    val rhs = condition?.rhs
                                    val context =
                                        lhs?.getOverlaysByPrevDFG<Context>(state)?.singleOrNull()
                                    val principal =
                                        rhs?.getOverlaysByPrevDFG<Principal>(state)?.singleOrNull()
                                    val thenReturns = node.thenStatement.returns
                                    val protectedAsset =
                                        thenReturns
                                            .mapNotNull { it.returnValue }
                                            .flatMap {
                                                it.getOverlaysByPrevDFG<ProtectedAsset>(state)
                                            }
                                            .single()
                                    if (context != null && principal != null) {
                                        CheckAccess(
                                            underlyingNode = condition,
                                            asset = protectedAsset,
                                        )
                                    } else {
                                        CheckAccess(null, ProtectedAsset(null, null))
                                    }
                                }
                            }
                    )
                )
            }
        assertNotNull(result)
        val q =
            /*
            Run through all ReturnStatements (as those will exit the Boundary) and check if they return a ProtectedAsset.
            If so, verify if a CheckAccess is performed before the ProtectedAsset is returned
             */
            result.allExtended<ReturnStatement>(
                sel = {
                    it.returnValue
                        ?.let { returnValue ->
                            dataFlow(
                                startNode = returnValue,
                                direction = Backward(GraphToFollow.DFG),
                                predicate = { node ->
                                    node.overlays.filterIsInstance<ProtectedAsset>().isNotEmpty()
                                },
                            )
                        }
                        ?.value == true
                },
                mustSatisfy = {
                    val paths =
                        it.followPrevCDGUntilHit(
                            predicate = { node ->
                                (node as? IfStatement)
                                    ?.overlays
                                    ?.filterIsInstance<CheckAccess>()
                                    ?.isNotEmpty() ?: false
                            }
                        )
                    QueryTree<Boolean>(paths.failed.isEmpty())
                },
            )
        println(q.printNicely())
        assertFalse(q.value)
    }
}
