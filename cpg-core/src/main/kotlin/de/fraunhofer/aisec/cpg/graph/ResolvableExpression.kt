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

import de.fraunhofer.aisec.cpg.PopulatedByPass
import de.fraunhofer.aisec.cpg.frontends.HasOperatorOverloading
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.Companion.unwrap
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.Companion.wrap
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator
import de.fraunhofer.aisec.cpg.graph.types.HasType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import org.neo4j.ogm.annotation.Relationship

/**
 * This abstract class is a root for all [Expression] nodes which can be resolved. The most common
 * example would be a [CallExpression], which is resolved to a [FunctionDeclaration]. but if
 * languages have the trait [HasOperatorOverloading], also operators, such as an [UnaryOperator]
 */
abstract class ResolvableExpression<T : FunctionDeclaration> :
    Expression(), HasBase, HasType.TypeObserver {
    abstract val signature: List<Type>

    abstract val arguments: List<Expression>?

    /**
     * Connection to its [T]. This will be populated by the [SymbolResolver]. This will have an
     * effect on the [type]
     */
    @PopulatedByPass(SymbolResolver::class)
    @Relationship(value = "INVOKES", direction = Relationship.Direction.OUTGOING)
    var invokeEdges = mutableListOf<PropertyEdge<T>>()
        protected set

    /**
     * A virtual property to quickly access the list of declarations that this call invokes without
     * property edges.
     */
    @PopulatedByPass(SymbolResolver::class)
    var invokes: List<T>
        get(): List<T> {
            return unwrap(invokeEdges)
        }
        set(value) {
            unwrap(invokeEdges).forEach { it.unregisterTypeObserver(this) }
            invokeEdges = wrap(value, this)
            value.forEach { it.registerTypeObserver(this) }
        }
}
