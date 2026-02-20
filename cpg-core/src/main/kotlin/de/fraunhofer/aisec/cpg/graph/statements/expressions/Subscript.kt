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
import java.util.*
import org.neo4j.ogm.annotation.Relationship

/**
 * Represents the subscription or access of an array of the form `array[index]`, where both `array`
 * ([arrayExpression]) and `index` ([subscriptExpression]) are of type [Expression]. CPP can
 * overload operators thus changing semantics of array access.
 */
class Subscript : Expression(), HasBase, HasType.TypeObserver, ArgumentHolder {
    override var access = AccessValues.READ
        set(value) {
            field = value
            // Do not propagate the access value to the array expression
            // arrayExpression.access = value
        }

    @Relationship("ARRAY_EXPRESSION")
    var arrayExpressionEdge =
        astEdgeOf<Expression>(
            of = ProblemExpression("could not parse array expression"),
            onChanged = ::exchangeTypeObserverWithoutAccessPropagation,
        )
    /** The array on which the access is happening. This is most likely a [Reference]. */
    var arrayExpression by unwrapping(Subscript::arrayExpressionEdge)

    @Relationship("SUBSCRIPT_EXPRESSION")
    var subscriptExpressionEdge =
        astEdgeOf<Expression>(ProblemExpression("could not parse index expression"))
    /**
     * The expression which represents the "subscription" or index on which the array is accessed.
     * This can for example be a reference to another variable ([Reference]), a [Literal] or a
     * [Range].
     */
    var subscriptExpression by unwrapping(Subscript::subscriptExpressionEdge)

    override val base: Expression
        get() = arrayExpression

    override val operatorCode: String
        get() = "[]"

    /**
     * This helper function returns the subscript type of the [arrayType]. We have to differentiate
     * here between to types of subscripts:
     * * Slices (in the form of a [Range] return the same type as the array
     * * Everything else (for example a [Literal] or any other [Expression] that is being evaluated)
     *   returns the de-referenced type
     */
    private fun getSubscriptType(arrayType: Type): Type {
        return when (subscriptExpression) {
            is Range -> arrayType
            else -> arrayType.dereference()
        }
    }

    override fun typeChanged(newType: Type, src: HasType) {
        // Make sure the source is really our array
        if (src != arrayExpression) {
            return
        }

        this.type = getSubscriptType(newType)
    }

    override fun assignedTypeChanged(assignedTypes: Set<Type>, src: HasType) {
        // Make sure the source is really our array
        if (src != arrayExpression) {
            return
        }

        addAssignedTypes(assignedTypes.map { getSubscriptType(it) }.toSet())
    }

    override fun addArgument(expression: Expression) {
        if (arrayExpression is ProblemExpression) {
            arrayExpression = expression
        } else if (subscriptExpression is ProblemExpression) {
            subscriptExpression = expression
        }
    }

    override fun replaceArgument(old: Expression, new: Expression): Boolean {
        return if (arrayExpression == old) {
            arrayExpression = new
            true
        } else if (subscriptExpression == old) {
            subscriptExpression = new
            true
        } else {
            false
        }
    }

    override fun hasArgument(expression: Expression): Boolean {
        return arrayExpression == expression || subscriptExpression == expression
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Subscript) return false
        return super.equals(other) &&
            arrayExpression == other.arrayExpression &&
            subscriptExpression == other.subscriptExpression
    }

    override fun hashCode() = Objects.hash(super.hashCode(), arrayExpression, subscriptExpression)

    override fun getStartingPrevEOG(): Collection<Node> {
        return this.arrayExpression.getStartingPrevEOG()
    }
}
