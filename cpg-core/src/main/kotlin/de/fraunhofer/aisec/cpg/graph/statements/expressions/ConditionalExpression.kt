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

import de.fraunhofer.aisec.cpg.commonType
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.types.HasType
import de.fraunhofer.aisec.cpg.graph.types.Type
import java.util.Objects
import org.apache.commons.lang3.builder.ToStringBuilder

/**
 * Represents an expression containing a ternary operator: `var x = condition ? valueIfTrue :
 * valueIfFalse`;
 */
class ConditionalExpression : Expression(), ArgumentHolder, BranchingNode, HasType.TypeObserver {
    @AST var condition: Expression = ProblemExpression("could not parse condition expression")

    @AST
    var thenExpression: Expression? = null
        set(value) {
            field?.unregisterTypeObserver(this)
            field = value
            value?.registerTypeObserver(this)
        }

    @AST
    var elseExpression: Expression? = null
        set(value) {
            field?.unregisterTypeObserver(this)
            field = value
            value?.registerTypeObserver(this)
        }

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("condition", condition)
            .append("thenExpr", thenExpression)
            .append("elseExpr", elseExpression)
            .build()
    }

    override val branchedBy: Node
        get() = condition

    override fun addArgument(expression: Expression) {
        if (condition is ProblemExpression) {
            condition = expression
        } else if (thenExpression == null) {
            thenExpression = expression
        } else {
            elseExpression = expression
        }
    }

    override fun replaceArgument(old: Expression, new: Expression): Boolean {
        return when (old) {
            thenExpression -> {
                thenExpression = new
                true
            }
            elseExpression -> {
                elseExpression = new
                true
            }
            else -> {
                false
            }
        }
    }

    override fun hasArgument(expression: Expression): Boolean {
        return this.thenExpression == expression || elseExpression == expression
    }

    override fun typeChanged(newType: Type, src: HasType) {
        val types = mutableSetOf<Type>()

        thenExpression?.type?.let { types.add(it) }
        elseExpression?.type?.let { types.add(it) }

        val alternative = if (types.isNotEmpty()) types.first() else unknownType()
        this.type = types.commonType ?: alternative
    }

    override fun assignedTypeChanged(assignedTypes: Set<Type>, src: HasType) {
        // Merge and propagate the assigned types of our branches
        if (src == thenExpression || src == elseExpression) {
            val types = mutableSetOf<Type>()
            thenExpression?.assignedTypes?.let { types.addAll(it) }
            elseExpression?.assignedTypes?.let { types.addAll(it) }
            addAssignedTypes(types)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConditionalExpression) return false
        return super.equals(other) &&
            condition == other.condition &&
            thenExpression == other.thenExpression &&
            elseExpression == other.elseExpression
    }

    override fun hashCode() =
        Objects.hash(super.hashCode(), condition, thenExpression, elseExpression)
}
