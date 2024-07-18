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

import de.fraunhofer.aisec.cpg.graph.AST
import de.fraunhofer.aisec.cpg.graph.AccessValues
import de.fraunhofer.aisec.cpg.graph.ArgumentHolder
import de.fraunhofer.aisec.cpg.graph.pointer
import de.fraunhofer.aisec.cpg.graph.types.HasType
import de.fraunhofer.aisec.cpg.graph.types.Type
import org.apache.commons.lang3.builder.ToStringBuilder

/** A unary operator expression, involving one expression and an operator, such as `a++`. */
class UnaryOperator : Expression(), ArgumentHolder, HasType.TypeObserver {
    /** The expression on which the operation is applied. */
    @AST
    var input: Expression = ProblemExpression("could not parse input")
        set(value) {
            field.unregisterTypeObserver(this)
            field = value
            input.registerTypeObserver(this)
            changeExpressionAccess()
        }

    /** The operator code. */
    var operatorCode: String? = null
        set(value) {
            field = value
            changeExpressionAccess()
        }

    /** Specifies, whether this a post fix operation. */
    var isPostfix = false

    /** Specifies, whether this a pre fix operation. */
    var isPrefix = false

    private fun changeExpressionAccess() {
        var access = AccessValues.READ
        if (operatorCode == "++" || operatorCode == "--") {
            access = AccessValues.READWRITE
        }
        if (input is Reference) {
            (input as? Reference)?.access = access
        }
    }

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("operatorCode", operatorCode)
            .append("postfix", isPostfix)
            .append("prefix", isPrefix)
            .toString()
    }

    override fun typeChanged(newType: Type, src: HasType) {
        // Only accept type changes from out input
        if (src != input) {
            return
        }

        val type =
            when (operatorCode) {
                "*" -> newType.dereference()
                "&" -> newType.pointer()
                else -> newType
            }

        this.type = type
    }

    override fun assignedTypeChanged(assignedTypes: Set<Type>, src: HasType) {
        // Only accept type changes from out input
        if (src != input) {
            return
        }

        // Apply our operator to all assigned types and forward them to us
        this.addAssignedTypes(
            assignedTypes
                .map {
                    when (operatorCode) {
                        "*" -> it.dereference()
                        "&" -> it.pointer()
                        else -> it
                    }
                }
                .toSet()
        )
    }

    override fun addArgument(expression: Expression) {
        this.input = expression
    }

    override fun replaceArgument(old: Expression, new: Expression): Boolean {
        if (this.input == old) {
            this.input = new
            return true
        }

        return false
    }

    override fun hasArgument(expression: Expression): Boolean {
        return this.input == expression
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is UnaryOperator) {
            return false
        }
        return super.equals(other) &&
            isPostfix == other.isPostfix &&
            isPrefix == other.isPrefix &&
            input == other.input &&
            operatorCode == other.operatorCode
    }

    override fun hashCode() = super.hashCode()

    companion object {
        const val OPERATOR_POSTFIX_INCREMENT = "++"
        const val OPERATOR_POSTFIX_DECREMENT = "--"
    }
}
