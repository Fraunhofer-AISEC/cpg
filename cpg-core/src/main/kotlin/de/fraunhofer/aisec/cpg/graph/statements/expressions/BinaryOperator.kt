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
import de.fraunhofer.aisec.cpg.graph.types.NumericType
import de.fraunhofer.aisec.cpg.graph.types.StringType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import java.util.*
import org.apache.commons.lang3.builder.ToStringBuilder

/**
 * A binary operation expression, such as "a + b". It consists of a left hand expression (lhs), a
 * right hand expression (rhs) and an operatorCode.
 */
open class BinaryOperator :
    Expression(), HasType.TypeListener, AssignmentHolder, HasBase, ArgumentHolder {
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
                NodeBuilder.LOGGER.warn(
                    "Creating a BinaryOperator with an assignment operator code is deprecated. The class AssignExpression should be used instead."
                )
            }
        }

    fun <T : Expression?> getLhsAs(clazz: Class<T>): T? {
        return if (clazz.isInstance(lhs)) clazz.cast(lhs) else null
    }

    private fun connectNewLhs(lhs: Expression) {
        lhs.registerTypeListener(this)
        if ("=" == operatorCode) {
            if (lhs is DeclaredReferenceExpression) {
                // declared reference expr is the left-hand side of an assignment -> writing to the
                // var
                lhs.access = AccessValues.WRITE
            }
            if (lhs is HasType.TypeListener) {
                registerTypeListener(lhs as HasType.TypeListener)
                registerTypeListener(this.lhs as HasType.TypeListener)
            }
        } else if (operatorCode in (language?.compoundAssignmentOperators ?: setOf())) {
            if (lhs is DeclaredReferenceExpression) {
                // declared reference expr is the left-hand side of an assignment -> writing to the
                // var
                lhs.access = AccessValues.READWRITE
            }
            if (lhs is HasType.TypeListener) {
                registerTypeListener(lhs as HasType.TypeListener)
                registerTypeListener(this.lhs as HasType.TypeListener)
            }
        }
    }

    private fun disconnectOldLhs() {
        lhs.unregisterTypeListener(this)
        if ("=" == operatorCode && lhs is HasType.TypeListener) {
            unregisterTypeListener(lhs as HasType.TypeListener)
        }
    }

    fun <T : Expression?> getRhsAs(clazz: Class<T>): T? {
        return if (clazz.isInstance(rhs)) clazz.cast(rhs) else null
    }

    private fun connectNewRhs(rhs: Expression) {
        rhs.registerTypeListener(this)
        if ("=" == operatorCode && rhs is HasType.TypeListener) {
            registerTypeListener(rhs as HasType.TypeListener)
        }
    }

    private fun disconnectOldRhs() {
        rhs.unregisterTypeListener(this)
        if ("=" == operatorCode && rhs is HasType.TypeListener) {
            unregisterTypeListener(rhs as HasType.TypeListener)
        }
    }

    override fun typeChanged(src: HasType, root: MutableList<HasType>, oldType: Type) {
        if (!isTypeSystemActive) {
            return
        }
        val previous = type
        if (operatorCode == "=") {
            val srcWidth = (src.type as? NumericType)?.bitWidth
            val lhsWidth = (lhs.type as? NumericType)?.bitWidth
            if (src == rhs && lhsWidth != null && srcWidth != null && lhsWidth < srcWidth) {
                // Do not propagate anything if the new type is too big for the current type.
                return
            }
            setType(src.propagationType, root)
        } else if (
            operatorCode == "+" &&
                (lhs.propagationType is StringType || rhs.propagationType is StringType)
        ) {
            // String + any other type results in a String
            _possibleSubTypes.clear() // TODO: Why do we clear the list here?
            val stringType =
                if (lhs.propagationType is StringType) lhs.propagationType else rhs.propagationType
            setType(stringType, root)
        } else if (operatorCode == ".*" || operatorCode == "->*" && src === rhs) {
            // Propagate the function pointer type to the expression itself. This helps us later in
            // the call resolver, when trying to determine, whether this is a regular call or a
            // function pointer call.
            setType(src.propagationType, root)
        } else {
            val resultingType = language?.propagateTypeOfBinaryOperation(this) ?: newUnknownType()
            if (resultingType !is UnknownType) {
                setType(resultingType, root)
            }
        }

        if (previous != type) {
            type.typeOrigin = Type.Origin.DATAFLOW
        }
    }

    override fun possibleSubTypesChanged(src: HasType, root: MutableList<HasType>) {
        if (!isTypeSystemActive) {
            return
        }
        val subTypes: MutableList<Type> = ArrayList(possibleSubTypes)
        subTypes.addAll(src.possibleSubTypes)
        setPossibleSubTypes(subTypes, root)
    }

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .append("lhs", lhs.name)
            .append("rhs", rhs.name)
            .append("operatorCode", operatorCode)
            .toString()
    }

    @Deprecated("BinaryOperator should not be used for assignments anymore")
    override val assignments: List<Assignment>
        get() {
            return if (isAssignment) {
                listOf(Assignment(rhs, lhs, this))
            } else {
                listOf()
            }
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

    private val isAssignment: Boolean
        get() {
            // TODO(oxisto): We need to discuss, if the other operators are also assignments and if
            // we really want them
            return this.operatorCode.equals("=")
            /*||this.operatorCode.equals("+=") ||this.operatorCode.equals("-=")
            ||this.operatorCode.equals("/=")  ||this.operatorCode.equals("*=")*/
        }

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
