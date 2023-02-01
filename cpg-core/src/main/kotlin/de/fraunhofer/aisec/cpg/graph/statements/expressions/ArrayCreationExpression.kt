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

import de.fraunhofer.aisec.cpg.graph.HasType
import de.fraunhofer.aisec.cpg.graph.SubGraph
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.Companion.propertyEqualsList
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdgeDelegate
import de.fraunhofer.aisec.cpg.graph.types.Type
import java.util.*
import org.neo4j.ogm.annotation.Relationship

/**
 * Expressions of the form `new Type[]` that represents the creation of an array, mostly used in
 * combination with a [VariableDeclaration].
 */
class ArrayCreationExpression : Expression(), HasType.TypeListener {
    /**
     * The initializer of the expression, if present. Many languages, such as Java, either specify
     * [dimensions] or an initializer.
     */
    @field:SubGraph("AST")
    var initializer: Expression? = null
        set(value) {
            field?.unregisterTypeListener(this)
            field = value
            value?.registerTypeListener(this)
        }

    /**
     * Specifies the dimensions of the array that is to be created. Many languages, such as Java,
     * either explicitly specify dimensions or an [.initializer], which is used to calculate
     * dimensions. In the graph, this will NOT be done.
     */
    @Relationship(value = "DIMENSIONS", direction = Relationship.Direction.OUTGOING)
    @field:SubGraph("AST")
    var dimensionEdges = mutableListOf<PropertyEdge<Expression>>()

    /** Virtual property to access [dimensionEdges] without property edges. */
    var dimensions by PropertyEdgeDelegate(ArrayCreationExpression::dimensionEdges)

    /** Adds an [Expression] to the existing [dimensions]. */
    fun addDimension(expression: Expression) {
        addIfNotContains(dimensionEdges, expression)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArrayCreationExpression) return false
        return (super.equals(other) &&
            initializer == other.initializer &&
            dimensions == other.dimensions &&
            propertyEqualsList(dimensionEdges, other.dimensionEdges))
    }

    override fun hashCode() = Objects.hash(super.hashCode(), initializer, dimensions)

    override fun typeChanged(src: HasType, root: List<HasType>, oldType: Type) {
        if (!TypeManager.isTypeSystemActive()) {
            return
        }
        val previous = type
        setType(src.propagationType, root)
        if (previous != type) {
            type.typeOrigin = Type.Origin.DATAFLOW
        }
    }

    override fun possibleSubTypesChanged(src: HasType, root: List<HasType>) {
        if (!TypeManager.isTypeSystemActive()) {
            return
        }
        val subTypes: MutableList<Type> = ArrayList(possibleSubTypes)
        subTypes.addAll(src.possibleSubTypes)
        setPossibleSubTypes(subTypes, root)
    }
}
