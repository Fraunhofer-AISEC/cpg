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

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.edges.ast.astEdgeOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.graph.types.HasType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.persistence.Relationship
import java.util.*
import org.slf4j.LoggerFactory

class CastExpression : Expression(), ArgumentHolder, HasType.TypeObserver {
    /**
     * The [Expression] that is cast to [castType].
     *
     * Note: While the [type] will always stay the same (i.e. the [castType]), we still want to
     * register ourselves as a type observer to the expression. The reason for that is that we want
     * to propagate the [assignedTypes] of our [expression] to us and then possibly to other nodes.
     * This way we can still access the original type of expression (e.g., created by a
     * [NewExpression]), even when it is cast.
     */
    @Relationship(value = "EXPRESSION")
    var expressionEdge =
        astEdgeOf<Expression>(
            of = ProblemExpression("could not parse inner expression"),
            onChanged = ::exchangeTypeObserverWithAccessPropagation,
        )
    var expression by unwrapping(CastExpression::expressionEdge)

    var castType: Type = unknownType()
        set(value) {
            field = value
            type = value
            name = value.name
        }

    fun setCastOperator(operatorCode: Int) {
        var localName: String? = null
        when (operatorCode) {
            0 -> localName = "cast"
            1 -> localName = "dynamic_cast"
            2 -> localName = "static_cast"
            3 -> localName = "reinterpret_cast"
            4 -> localName = "const_cast"
            else -> log.error("unknown operator {}", operatorCode)
        }
        if (localName != null) {
            name = Name(localName, null, language)
        }
    }

    override fun addArgument(expression: Expression) {
        this.expression = expression
        this.expression.access = access
    }

    override fun replaceArgument(old: Expression, new: Expression): Boolean {
        if (this.expression == old) {
            this.expression = new
            this.expression.access = access
            return true
        }

        return false
    }

    override fun hasArgument(expression: Expression): Boolean {
        return this.expression == expression
    }

    override fun typeChanged(newType: Type, src: HasType) {
        // Nothing to do, the cast type always stays the same
    }

    override fun assignedTypeChanged(assignedTypes: Set<Type>, src: HasType) {
        // We want to propagate the assigned types, if they come from our expression
        if (src == expression) {
            addAssignedTypes(assignedTypes)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is CastExpression) {
            return false
        }
        return expression == other.expression && castType == other.castType
    }

    override fun hashCode() = Objects.hash(super.hashCode(), expression, castType)

    override var access = AccessValues.READ
        set(value) {
            field = value
            this.expression.access = value
        }

    override fun getStartingPrevEOG(): Collection<Node> {
        return this.expression.getStartingPrevEOG()
    }

    companion object {
        private val log = LoggerFactory.getLogger(CastExpression::class.java)
    }
}
