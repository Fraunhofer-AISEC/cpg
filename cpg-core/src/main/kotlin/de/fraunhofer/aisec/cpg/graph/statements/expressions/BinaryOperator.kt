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
import de.fraunhofer.aisec.cpg.graph.ArgumentHolder
import de.fraunhofer.aisec.cpg.graph.HasOverloadedOperation
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.edges.ast.astEdgeOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.graph.types.HasType
import de.fraunhofer.aisec.cpg.graph.types.Type
import java.util.*
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/**
 * A binary operation expression, such as "a + b". It consists of a left hand expression (lhs), a
 * right hand expression (rhs) and an operatorCode.
 *
 * Note: For assignments, i.e., using an `=` or `+=`, etc. the [AssignExpression] MUST be used.
 */
open class BinaryOperator :
    Expression(), HasOverloadedOperation, ArgumentHolder, HasType.TypeObserver {

    /** The left-hand expression. */
    @Relationship("LHS")
    var lhsEdge =
        astEdgeOf<Expression>(
            of = ProblemExpression("could not parse lhs"),
            onChanged = ::exchangeTypeObserverWithAccessPropagation,
        )
    var lhs by unwrapping(BinaryOperator::lhsEdge)

    /** The right-hand expression. */
    @Relationship("RHS")
    var rhsEdge =
        astEdgeOf<Expression>(
            of = ProblemExpression("could not parse rhs"),
            onChanged = ::exchangeTypeObserverWithAccessPropagation,
        )
    var rhs by unwrapping(BinaryOperator::rhsEdge)

    /** The operator code. */
    override var operatorCode: String? = null
        set(value) {
            field = value
            if (
                (operatorCode in language.compoundAssignmentOperators) ||
                    (operatorCode in language.simpleAssignmentOperators)
            ) {
                throw TranslationException(
                    "Creating a BinaryOperator with an assignment operator code is not allowed. The class AssignExpression must be used instead."
                )
            }
        }

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .append("lhs", lhs.name)
            .append("rhs", rhs.name)
            .append("operatorCode", operatorCode)
            .append("location", location)
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
            val type =
                language.propagateTypeOfBinaryOperation(this.operatorCode, lhs.type, rhs.type, this)
            this.type = type
        }
    }

    override fun assignedTypeChanged(assignedTypes: Set<Type>, src: HasType) {
        // TODO: replicate something similar like propagateTypeOfBinaryOperation for assigned types
    }

    /** The binary operator operators on the [lhs]. [rhs] is part of the [operatorArguments]. */
    override val operatorArguments: List<Expression>
        get() = listOf(rhs)

    /** The binary operator operators on the [lhs]. [rhs] is part of the [operatorArguments]. */
    override val operatorBase: Expression
        get() = lhs

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

    override fun hasArgument(expression: Expression): Boolean {
        return lhs == expression || rhs == expression
    }

    override fun getStartingPrevEOG(): Collection<Node> {
        return this.lhs.getStartingPrevEOG()
    }

    val base: Expression?
        get() {
            return if (operatorCode == ".*" || operatorCode == "->*") {
                lhs
            } else {
                null
            }
        }
}
