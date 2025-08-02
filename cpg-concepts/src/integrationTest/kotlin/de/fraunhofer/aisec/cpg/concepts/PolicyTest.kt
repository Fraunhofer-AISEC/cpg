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
import de.fraunhofer.aisec.cpg.graph.allChildrenWithOverlays
import de.fraunhofer.aisec.cpg.graph.ast.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.ast.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.ast.statements.IfStatement
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.concepts.policy.Boundary
import de.fraunhofer.aisec.cpg.graph.concepts.policy.CheckAccess
import de.fraunhofer.aisec.cpg.graph.concepts.policy.Context
import de.fraunhofer.aisec.cpg.graph.concepts.policy.Equals
import de.fraunhofer.aisec.cpg.graph.concepts.policy.ExitBoundary
import de.fraunhofer.aisec.cpg.graph.concepts.policy.Principal
import de.fraunhofer.aisec.cpg.graph.concepts.policy.ProtectedAsset
import de.fraunhofer.aisec.cpg.graph.returns
import de.fraunhofer.aisec.cpg.passes.ControlDependenceGraphPass
import de.fraunhofer.aisec.cpg.passes.concepts.TagOverlaysPass
import de.fraunhofer.aisec.cpg.passes.concepts.each
import de.fraunhofer.aisec.cpg.passes.concepts.getOverlaysByPrevDFG
import de.fraunhofer.aisec.cpg.passes.concepts.propagate
import de.fraunhofer.aisec.cpg.passes.concepts.tag
import de.fraunhofer.aisec.cpg.passes.concepts.with
import de.fraunhofer.aisec.cpg.queries.concepts.policy.assetsAreProtected
import de.fraunhofer.aisec.cpg.query.dataFlow
import de.fraunhofer.aisec.cpg.test.analyze
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
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
                                    .with {
                                        val boundary = Boundary()
                                        node.returns.forEach { ret ->
                                            propagate { ret }.with { ExitBoundary(boundary) }
                                        }
                                        boundary
                                    }
                                // Tag the field "__members" with the concept "ProtectedAsset"
                                each<FieldDeclaration>(
                                        predicate = { node -> node.name.localName == "__members" }
                                    )
                                    .with { ProtectedAsset(scope = node.scope) }
                                // Tag the field "manager" with the concept "Principal"
                                each<FieldDeclaration>(
                                        predicate = { node -> node.name.localName == "manager" }
                                    )
                                    .with { Principal() }
                                // Tag the reference "whoami" with the concept "Context"
                                each<Reference>(
                                        predicate = { node -> node.name.localName == "whoami" }
                                    )
                                    .with { Context() }
                                // Tag IfStatements that compare a Context with a Principal and that
                                // check access to a ProtectedResource with "CheckAccess"
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
                                            asset = protectedAsset,
                                            predicate = Equals(left = context, right = principal),
                                        )
                                    } else {
                                        null
                                    }
                                }
                            }
                    )
                )
            }
        assertNotNull(result)
        val boundary = result.allChildrenWithOverlays<Boundary>().singleOrNull()
        assertNotNull(boundary)
        assertTrue(boundary.ops.isNotEmpty(), "Boundary should have operations")
        assertTrue(boundary.exits.isNotEmpty(), "Boundary should have exits")

        val asset = result.allChildrenWithOverlays<ProtectedAsset>().singleOrNull()
        assertNotNull(asset)
        assertTrue(
            dataFlow(asset.underlyingNode!!, predicate = { it is ExitBoundary }).value,
            "Data should flow from the asset to the exit boundary",
        )

        with(result) {
            val q = assetsAreProtected()
            println(q.printNicely())
            assertFalse(q.value)
        }
    }
}
