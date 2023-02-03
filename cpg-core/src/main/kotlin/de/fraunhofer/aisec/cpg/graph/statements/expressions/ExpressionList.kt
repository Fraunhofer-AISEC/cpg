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
import de.fraunhofer.aisec.cpg.graph.LegacyTypeManager
import de.fraunhofer.aisec.cpg.graph.SubGraph
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.Companion.propertyEqualsList
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.Companion.transformIntoOutgoingPropertyEdgeList
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.Companion.unwrap
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.types.Type
import java.util.*
import kotlin.collections.ArrayList
import org.neo4j.ogm.annotation.Relationship

class ExpressionList : Expression(), HasType.TypeListener {
    @Relationship(value = "SUBEXPR", direction = Relationship.Direction.OUTGOING)
    @field:SubGraph("AST")
    var expressionEdges: MutableList<PropertyEdge<Statement>> = ArrayList()

    var expressions: List<Statement>
        get() {
            return unwrap(expressionEdges)
        }
        set(value) {
            if (this.expressionEdges.isNotEmpty()) {
                val lastExpression = this.expressionEdges[this.expressionEdges.size - 1].end
                if (lastExpression is HasType)
                    (lastExpression as HasType).unregisterTypeListener(this)
            }
            this.expressionEdges = transformIntoOutgoingPropertyEdgeList(value, this)
            if (this.expressionEdges.isNotEmpty()) {
                val lastExpression = this.expressionEdges[this.expressionEdges.size - 1].end
                if (lastExpression is HasType)
                    (lastExpression as HasType).registerTypeListener(this)
            }
        }

    fun addExpression(expression: Statement) {
        if (!expressionEdges.isEmpty()) {
            val lastExpression = expressionEdges[expressionEdges.size - 1].end
            if (lastExpression is HasType) (lastExpression as HasType).unregisterTypeListener(this)
        }
        val propertyEdge = PropertyEdge(this, expression)
        propertyEdge.addProperty(Properties.INDEX, expressionEdges.size)
        expressionEdges.add(propertyEdge)
        if (expression is HasType) {
            (expression as HasType).registerTypeListener(this)
        }
    }

    override fun typeChanged(src: HasType, root: MutableList<HasType>, oldType: Type) {
        if (!LegacyTypeManager.isTypeSystemActive()) {
            return
        }
        val previous = type
        setType(src.propagationType, root)
        setPossibleSubTypes(ArrayList(src.possibleSubTypes), root)
        if (previous != type) {
            type.typeOrigin = Type.Origin.DATAFLOW
        }
    }

    override fun possibleSubTypesChanged(src: HasType, root: MutableList<HasType>) {
        if (!LegacyTypeManager.isTypeSystemActive()) {
            return
        }
        setPossibleSubTypes(ArrayList(src.possibleSubTypes), root)
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o !is ExpressionList) {
            return false
        }
        return (super.equals(o) &&
            expressions == o.expressions &&
            propertyEqualsList(expressionEdges, o.expressionEdges))
    }

    override fun hashCode(): Int {
        return Objects.hash(super.hashCode(), expressions)
    }
}
