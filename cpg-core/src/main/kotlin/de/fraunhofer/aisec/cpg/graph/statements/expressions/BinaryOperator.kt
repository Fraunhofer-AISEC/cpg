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

import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.types.HasType
import de.fraunhofer.aisec.cpg.graph.types.Type
import java.util.*
import org.apache.commons.lang3.builder.ToStringBuilder

/**
 * A binary operation expression, such as "a + b". It consists of a left hand expression (lhs), a
 * right hand expression (rhs) and an operatorCode.
 *
 * Note: For assignments, i.e., using an `=` or `+=`, etc. the [AssignExpression] MUST be used.
 */
open class BinaryOperator :
    Expression(), HasBase, HasOperatorCode, ArgumentHolder, HasType.TypeObserver {
    /** The left-hand expression. */
    @AST
    var lhs: Expression = ProblemExpression("could not parse lhs")
        set(value) {
            disconnectOldLhs()
            field = value
            connectNewLhs(value)
        }

    /** The right-hand expression. */
    @AST
    var rhs: Expression = ProblemExpression("could not parse rhs")
        set(value) {
            disconnectOldRhs()
            field = value
            connectNewRhs(value)
        }

    /** The operator code. */
    override var operatorCode: String? = null
        set(value) {
            field = value
            if (
                (operatorCode in (language?.compoundAssignmentOperators ?: setOf())) ||
                    (operatorCode == "=")
            ) {
                throw TranslationException(
                    "Creating a BinaryOperator with an assignment operator code is not allowed. The class AssignExpression should be used instead."
                )
            }
        }

    private fun connectNewLhs(lhs: Expression) {
        lhs.registerTypeObserver(this)
        if (lhs is Reference && "=" == operatorCode) {
            // declared reference expr is the left-hand side of an assignment -> writing to the var
            lhs.access = AccessValues.WRITE
        } else if (
            lhs is Reference && operatorCode in (language?.compoundAssignmentOperators ?: setOf())
        ) {
            // declared reference expr is the left-hand side of an assignment -> writing to the var
            lhs.access = AccessValues.READWRITE
        }
    }

    private fun disconnectOldLhs() {
        lhs.unregisterTypeObserver(this)
    }

    private fun connectNewRhs(rhs: Expression) {
        rhs.registerTypeObserver(this)
    }

    private fun disconnectOldRhs() {
        rhs.unregisterTypeObserver(this)
    }

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .append("lhs", lhs.name)
            .append("rhs", rhs.name)
            .append("operatorCode", operatorCode)
            .toString()
    }

    override fun typeChanged(newType: Type, src: HasType) {
        // We need to do some special dealings for function pointer calls
        if (operatorCode == ".*" || operatorCode == "->*" && src === rhs) {
            // Propagate the function pointer type to the expression itself. This helps us later in
            // the call resolver, when trying to determine, whether this is a regular call or a
            // function pointer call.
            this.type = newType
        } else {
            // Otherwise, we have a special language-specific function to deal with type propagation
            val type = language?.propagateTypeOfBinaryOperation(this)
            if (type != null) {
                this.type = type
            } else {
                // If we don't know how to propagate the types of this particular binary operation,
                // we just leave the type alone. We cannot take newType because it is just "half" of
                // the operation (either from lhs or rhs) and would lead to very incorrect results.
            }
        }
    }

    override fun assignedTypeChanged(assignedTypes: Set<Type>, src: HasType) {
        // TODO: replicate something similar like propagateTypeOfBinaryOperation for assigned types
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is BinaryOperator) {
            return false
        }
        return super.equals(other) &&
            lhs == other.lhs &&
            rhs == other.rhs &&
            operatorCode == other.operatorCode
    }

    override fun hashCode() = Objects.hash(super.hashCode(), lhs, rhs, operatorCode)

    override fun addArgument(expression: Expression) {
        if (lhs is ProblemExpression) {
            lhs = expression
        } else {
            rhs = expression
        }
    }

    override fun replaceArgument(old: Expression, new: Expression): Boolean {
        return if (lhs == old) {
            lhs = new
            true
        } else if (rhs == old) {
            rhs = new
            true
        } else {
            false
        }
    }

    override val base: Expression?
        get() {
            return if (operatorCode == ".*" || operatorCode == "->*") {
                lhs
            } else {
                null
            }
        }
}
