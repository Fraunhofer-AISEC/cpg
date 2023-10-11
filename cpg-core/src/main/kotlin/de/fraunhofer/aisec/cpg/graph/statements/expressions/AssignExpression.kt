/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.types.HasType
import de.fraunhofer.aisec.cpg.graph.types.TupleType
import de.fraunhofer.aisec.cpg.graph.types.Type
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Represents an assignment of a group of expressions (in the simplest case: one) from the right
 * hand side to the left-hand side.
 *
 * This is intentionally modelled as an expression, since some languages support using the resulting
 * value of an assignment as an expression. For example C++ allows the following:
 * ```cpp
 * int a;
 * int b = (a = 1);
 * ```
 *
 * In this example, the [type] of the [AssignExpression] is an `int`.
 *
 * However, since not all languages support this model, we explicitly introduce the
 * [usedAsExpression]. When this property is set to true (it defaults to false), we model a dataflow
 * from the (first) rhs to the [AssignExpression] itself.
 */
class AssignExpression :
    Expression(), AssignmentHolder, ArgumentHolder, HasType.TypeObserver, HasOperatorCode {

    override var operatorCode: String = "="

    @AST
    var lhs: List<Expression> = listOf()
        set(value) {
            field = value
            fun unwrapReference(node: Node): Reference? {
                return if (node is Reference) {
                    node.dfgHandlerHint = true
                    node
                } else if (
                    node is UnaryOperator && (node.operatorCode == "*" || node.operatorCode == "&")
                )
                    unwrapReference(node.input)
                else null
            }
            if (operatorCode == "=") {
                field.forEach { unwrapReference(it)?.access = AccessValues.WRITE }
            } else {
                field.forEach { unwrapReference(it)?.access = AccessValues.READWRITE }
            }
        }

    @AST
    var rhs: List<Expression> = listOf()
        set(value) {
            field.forEach { it.unregisterTypeObserver(this) }
            field = value
            value.forEach { it.registerTypeObserver(this) }
        }

    /**
     * This property specifies, that this is actually used as an expression. Not many languages
     * support that. In the regular case, an assignment is a simple statement and does not hold any
     * value itself.
     */
    var usedAsExpression = false

    /**
     * If this node is used an expression, this property contains a reference of the [Expression]
     * (of RHS), which is used to represent its value.
     */
    val expressionValue: Expression?
        get() {
            return if (usedAsExpression) rhs.firstOrNull() else null
        }

    private val isSingleValue: Boolean
        get() {
            return this.lhs.size == 1 && this.rhs.size == 1
        }

    /**
     * We also support compound assignments in this class, but only if the appropriate compound
     * operator is set and only if there is a single-value expression on both sides.
     */
    val isCompoundAssignment: Boolean
        get() {
            return operatorCode in (language?.compoundAssignmentOperators ?: setOf()) &&
                isSingleValue
        }

    /**
     * Some languages, such as Go explicitly allow the definition / declaration of variables in the
     * assignment (known as a "short assignment"). Some languages, such as Python even implicitly
     * declare variables in any assignments if they are not defined. Since we can only decide about
     * this once all frontends are run (because declarations could be spread across multiple files),
     * we need to later resolve this in an additional pass. The declarations are then stored in
     * [declarations].
     */
    @AST override var declarations = mutableListOf<VariableDeclaration>()

    /** Finds the value (of [rhs]) that is assigned to the particular [lhs] expression. */
    fun findValue(lhsExpression: HasType): Expression? {
        return if (lhs.size > 1) {
            rhs.singleOrNull()
        } else {
            // Basically, we need to find out which index on the lhs this variable belongs to and
            // find the corresponding index on the rhs.
            val idx = lhs.indexOf(lhsExpression)
            if (idx == -1) {
                null
            } else {
                rhs.getOrNull(idx)
            }
        }
    }

    /** Finds the targets(s) (within [lhs]) that are assigned to the particular [rhs] expression. */
    fun findTargets(rhsExpression: HasType): List<Expression> {
        val type = rhsExpression.type

        // There are now two possibilities: Either, we have a tuple type, that we need to
        // deconstruct, or we have a singular type
        return if (type is TupleType) {
            // We need to see if there is enough room on the left side. Currently, we only support
            // languages that do not allow to mix tuple and non-tuple types luckily, so we can just
            // assume that all arguments on the left side are assignment targets
            if (lhs.size != type.types.size) {
                log.info("Tuple type size on RHS does not match number of LHS expressions")
                listOf()
            } else {
                lhs
            }
        } else {
            // Basically, we need to find out which index on the rhs this variable belongs to and
            // find the corresponding index on the rhs.
            val idx = rhs.indexOf(rhsExpression)
            if (idx == -1) {
                listOf()
            } else {
                listOfNotNull(lhs.getOrNull(idx))
            }
        }
    }

    override val assignments: List<Assignment>
        get() {
            val list = mutableListOf<Assignment>()

            for (expr in rhs) {
                list.addAll(findTargets(expr).map { Assignment(expr, it, this) })
            }

            return list
        }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(Node::class.java)
    }

    override fun typeChanged(newType: Type, src: HasType) {
        // Double-check, if the src is really from the rhs
        if (!rhs.contains(src)) {
            return
        }

        // There are now two possibilities: Either, we have a tuple type, that we need to
        // deconstruct, or we have a singular type. Now, its getting tricky. We do NOT want
        // to propagate the type to the declared type, but only to the "assigned" type
        if (newType is TupleType) {
            val targets = findTargets(src)
            if (targets.size == newType.types.size) {
                // Set the corresponding type on the left-side
                newType.types.forEachIndexed { idx, t -> lhs.getOrNull(idx)?.addAssignedType(t) }
            }
        } else {
            findTargets(src).forEach { it.addAssignedType(newType) }
        }

        // If this is used as an expression, we also set the type accordingly
        if (usedAsExpression) {
            expressionValue?.type?.let { type = it }
        }
    }

    override fun assignedTypeChanged(assignedTypes: Set<Type>, src: HasType) {
        // Double-check, if the src is really from the rhs
        if (!rhs.contains(src)) {
            return
        }

        // Propagate any assigned types from the source to the target
        findTargets(src).forEach { it.addAssignedTypes(assignedTypes) }
    }

    override fun addArgument(expression: Expression) {
        if (lhs.isEmpty()) {
            lhs = listOf(expression)
        } else {
            rhs = listOf(expression)
        }
    }

    override fun replaceArgument(old: Expression, new: Expression): Boolean {
        return if (lhs == listOf(old)) {
            lhs = listOf(new)
            true
        } else if (rhs == listOf(old)) {
            rhs = listOf(new)
            true
        } else {
            false
        }
    }
}
