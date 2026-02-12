/*
 * Copyright (c) 2020, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.statements.expressions

import de.fraunhofer.aisec.cpg.graph.HasInitializer
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.edges.ast.astEdgesOf
import de.fraunhofer.aisec.cpg.graph.edges.ast.astOptionalEdgeOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import java.util.Objects
import org.neo4j.ogm.annotation.Relationship

/**
 * Expressions of the form `new Type[]` that represents the creation of an array, mostly used in
 * combination with a [VariableDeclaration].
 */
// TODO Merge and/or refactor with new Expression
class NewArrayExpression : Expression(), HasInitializer {
    @Relationship("INITIALIZER") var initializerEdge = astOptionalEdgeOf<Expression>()

    /**
     * The initializer of the expression, if present. Many languages, such as Java, either specify
     * [dimensions] or an initializer.
     */
    override var initializer by unwrapping(NewArrayExpression::initializerEdge)

    /**
     * Specifies the dimensions of the array that is to be created. Many languages, such as Java,
     * either explicitly specify dimensions or an [.initializer], which is used to calculate
     * dimensions. In the graph, this will NOT be done.
     */
    @Relationship(value = "DIMENSIONS", direction = Relationship.Direction.OUTGOING)
    var dimensionEdges = astEdgesOf<Expression>()

    /** Virtual property to access [dimensionEdges] without property edges. */
    var dimensions by unwrapping(NewArrayExpression::dimensionEdges)

    /** Adds an [Expression] to the existing [dimensions]. */
    fun addDimension(expression: Expression) {
        addIfNotContains(dimensionEdges, expression)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NewArrayExpression) return false
        return (super.equals(other) &&
            initializer == other.initializer &&
            dimensionEdges == other.dimensionEdges)
    }

    override fun hashCode() = Objects.hash(super.hashCode(), initializer, dimensions)

    override fun getStartingPrevEOG(): Collection<Node> {
        return this.dimensions.firstOrNull()?.getStartingPrevEOG()
            ?: this.initializer?.getStartingPrevEOG()
            ?: this.prevEOG
    }
}
