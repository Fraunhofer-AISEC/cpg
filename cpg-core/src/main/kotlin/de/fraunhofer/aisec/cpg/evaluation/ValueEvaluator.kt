/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.evaluation

import de.fraunhofer.aisec.cpg.graph.AccessValues
import de.fraunhofer.aisec.cpg.graph.HasInitializer
import de.fraunhofer.aisec.cpg.graph.HasOperatorCode
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.helpers.Util
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class CouldNotResolve

/**
 * The value evaluator tries to evaluate the (constant) value of an [Expression] basically by
 * following DFG edges until we reach a [Literal]. It also evaluates simple binary operations, such
 * as arithmetic operations, as well as simple string concatenations.
 *
 * The result can be retrieved in two ways:
 * * The result of the [resolve] function is a JVM object which represents the constant value
 * * Furthermore, after the execution of [evaluateInternal], the latest evaluation path can be
 *   retrieved in the [path] property of the evaluator.
 *
 * It contains some advanced mechanics such as resolution of values of arrays, if they contain
 * literal values. Furthermore, its behaviour can be adjusted by implementing the [cannotEvaluate]
 * function, which is called when the default behaviour would not be able to resolve the value. This
 * way, language specific features such as string formatting can be modelled.
 */
open class ValueEvaluator(
    /**
     * Contains a reference to a function that gets called if the value cannot be resolved by the
     * standard behaviour.
     */
    open val cannotEvaluate: (Node?, ValueEvaluator) -> Any? = { node: Node?, _: ValueEvaluator ->
        // end of the line, lets just keep the expression name
        if (node != null) {
            "{${node.name}}"
        } else {
            CouldNotResolve()
        }
    }
) {
    open val log: Logger
        get() = LoggerFactory.getLogger(ValueEvaluator::class.java)

    /** This property contains the path of the latest execution of [evaluateInternal]. */
    val path: MutableList<Node> = mutableListOf()

    open fun evaluate(node: Any?): Any? {
        if (node !is Node) return node
        clearPath()

        return evaluateInternal(node, 0)
    }

    /**
     * Tries to evaluate this node and returns the result as the specified type [T]. If the
     * evaluation fails, the result is "null".
     *
     * @return The result of the evaluation. If the evaluation fails, the result is `null`.
     */
    inline fun <reified T> evaluateAs(node: Node?): T? {
        if (node == null) return null // Nothing to do, return null right away
        clearPath() // clear the path before evaluating or we may start with old data if re-using
        // the ValueEvaluator object

        val result = evaluateInternal(node, 0)
        return if (result !is T) {
            Util.errorWithFileLocation(
                node,
                log,
                "Evaluated the node to type \"{}\". Expected type \"{}\". Returning \"null\".",
                result?.let { it::class.simpleName },
                T::class.simpleName,
            )
            null
        } else {
            result
        }
    }

    fun clearPath() {
        path.clear()
    }

    /** Tries to evaluate this node. Anything can happen. */
    open fun evaluateInternal(node: Node?, depth: Int): Any? {
        if (node == null) {
            return null
        }

        // Add the expression to the current path
        node.let { this.path += it }

        when (node) {
            is NewArrayExpression -> return handleHasInitializer(node, depth)
            is Variable -> return handleHasInitializer(node, depth)
            // For a literal, we can just take its value, and we are finished
            is Literal<*> -> return node.value
            is UnaryOperator -> return handleUnaryOp(node, depth)
            is BinaryOperator -> return handleBinaryOperator(node, depth)
            // Casts are just a wrapper in this case, we are interested in the inner expression
            is CastExpression -> return this.evaluateInternal(node.expression, depth + 1)
            is SubscriptExpression -> return handleSubscriptExpression(node, depth)
            // While we are not handling different paths of variables with If statements, we can
            // easily be partly path-sensitive in a conditional expression
            is ConditionalExpression -> return handleConditionalExpression(node, depth)
            is AssignExpression -> return handleAssignExpression(node, depth)
            is Reference -> return handleReference(node, depth)
            is CallExpression -> return handleCallExpression(node, depth)
            else -> return handlePrevDFG(node, depth)
        }

        // At this point, we cannot evaluate, and we are calling our [cannotEvaluate] hook, maybe
        // this helps
        return cannotEvaluate(node, this)
    }

    /** Handles a [CallExpression]. Default behaviour is to call [handlePrevDFG] */
    protected open fun handleCallExpression(node: CallExpression, depth: Int): Any? {
        return handlePrevDFG(node, depth)
    }

    /** Handles a [Reference]. Default behaviour is to call [handlePrevDFG] */
    protected open fun handleReference(node: Reference, depth: Int): Any? {
        return handlePrevDFG(node, depth)
    }

    /**
     * If a node declaration implements [HasInitializer], we can use the initializer to evaluate
     * their value. If not, we can try to use [handlePrevDFG].
     */
    protected fun handleHasInitializer(node: HasInitializer, depth: Int): Any? {
        // If we have an initializer, we can use it. Otherwise, we can fall back to the prevDFG
        return if (node.initializer != null) {
            evaluateInternal(node.initializer, depth + 1)
        } else {
            handlePrevDFG(node as Node, depth)
        }
    }

    /** Under certain circumstances, an assignment can also be used as an expression. */
    protected open fun handleAssignExpression(node: AssignExpression, depth: Int): Any? {
        // Handle compound assignments. Only possible with single values
        val lhs = node.lhs.singleOrNull()
        val rhs = node.rhs.singleOrNull()
        if (lhs != null && rhs != null && node.isCompoundAssignment) {
            // Resolve rhs
            val rhsValue = evaluateInternal(rhs, depth + 1)

            // Resolve lhs
            val lhsValue = evaluateInternal(lhs, depth + 1)

            return computeBinaryOpEffect(lhsValue, rhsValue, node)
        } else if (node.usedAsExpression) {
            return node.expressionValue
        }

        return cannotEvaluate(node, this)
    }

    /**
     * We are handling some basic arithmetic binary operations and string operations that are more
     * or less language-independent.
     */
    protected open fun handleBinaryOperator(expr: BinaryOperator, depth: Int): Any? {
        // Resolve rhs
        val rhsValue = evaluateInternal(expr.rhs, depth + 1)

        // Resolve lhs
        val lhsValue = evaluateInternal(expr.lhs, depth + 1)

        return computeBinaryOpEffect(lhsValue, rhsValue, expr)
    }

    /**
     * Computes the effect of basic "binary" operators.
     *
     * Note: this is both used by a [BinaryOperator] with basic arithmetic operations as well as
     * [AssignExpression], if [AssignExpression.isCompoundAssignment] is true.
     */
    protected open fun computeBinaryOpEffect(
        lhsValue: Any?,
        rhsValue: Any?,
        has: HasOperatorCode?,
    ): Any? {
        val expr = has as? Expression
        return when (has?.operatorCode) {
            "+",
            "+=" -> handlePlus(lhsValue, rhsValue, expr)
            "-",
            "-=" -> handleMinus(lhsValue, rhsValue, expr)
            "/",
            "/=" -> handleDiv(lhsValue, rhsValue, expr)
            "*",
            "*=" -> handleTimes(lhsValue, rhsValue, expr)
            "<<" -> handleShiftLeft(lhsValue, rhsValue, expr)
            ">>" -> handleShiftRight(lhsValue, rhsValue, expr)
            "&" -> handleBitwiseAnd(lhsValue, rhsValue, expr)
            "|" -> handleBitwiseOr(lhsValue, rhsValue, expr)
            "^" -> handleBitwiseXor(lhsValue, rhsValue, expr)
            ">" -> handleGreater(lhsValue, rhsValue, expr)
            ">=" -> handleGEq(lhsValue, rhsValue, expr)
            "<" -> handleLess(lhsValue, rhsValue, expr)
            "<=" -> handleLEq(lhsValue, rhsValue, expr)
            "==" -> handleEq(lhsValue, rhsValue, expr)
            "!=" -> handleNEq(lhsValue, rhsValue, expr)
            else -> cannotEvaluate(expr, this)
        }
    }

    private fun handlePlus(lhsValue: Any?, rhsValue: Any?, expr: Expression?): Any? {
        return when {
            lhsValue is String -> lhsValue + rhsValue
            lhsValue is Number && rhsValue is Number -> lhsValue + rhsValue
            else -> cannotEvaluate(expr, this)
        }
    }

    private fun handleMinus(lhsValue: Any?, rhsValue: Any?, expr: Expression?): Any? {
        return when {
            lhsValue is Number && rhsValue is Number -> lhsValue - rhsValue
            else -> cannotEvaluate(expr, this)
        }
    }

    private fun handleDiv(lhsValue: Any?, rhsValue: Any?, expr: Expression?): Any? {
        return when {
            rhsValue == 0 -> cannotEvaluate(expr, this)
            lhsValue is Number && rhsValue is Number -> lhsValue / rhsValue
            else -> cannotEvaluate(expr, this)
        }
    }

    private fun handleTimes(lhsValue: Any?, rhsValue: Any?, expr: Expression?): Any? {
        return when {
            lhsValue is Number && rhsValue is Number -> lhsValue * rhsValue
            else -> cannotEvaluate(expr, this)
        }
    }

    private fun handleShiftLeft(lhsValue: Any?, rhsValue: Any?, expr: Expression?): Any? {
        return when {
            // right side must always be an int
            lhsValue is Int && rhsValue is Int -> lhsValue shl rhsValue
            lhsValue is Long && rhsValue is Int -> lhsValue shl rhsValue
            else -> cannotEvaluate(expr, this)
        }
    }

    private fun handleShiftRight(lhsValue: Any?, rhsValue: Any?, expr: Expression?): Any? {
        return when {
            // right side must always be an int
            lhsValue is Int && rhsValue is Int -> lhsValue shr rhsValue
            lhsValue is Long && rhsValue is Int -> lhsValue shr rhsValue
            else -> cannotEvaluate(expr, this)
        }
    }

    private fun handleBitwiseAnd(lhsValue: Any?, rhsValue: Any?, expr: Expression?): Any? {
        return when {
            // left and right must be equal and only long and int are supported
            lhsValue is Int && rhsValue is Int -> lhsValue and rhsValue
            lhsValue is Long && rhsValue is Long -> lhsValue and rhsValue
            else -> cannotEvaluate(expr, this)
        }
    }

    private fun handleBitwiseOr(lhsValue: Any?, rhsValue: Any?, expr: Expression?): Any? {
        return when {
            // left and right must be equal and only long and int are supported
            lhsValue is Int && rhsValue is Int -> lhsValue or rhsValue
            lhsValue is Long && rhsValue is Long -> lhsValue or rhsValue
            else -> cannotEvaluate(expr, this)
        }
    }

    private fun handleBitwiseXor(lhsValue: Any?, rhsValue: Any?, expr: Expression?): Any? {
        return when {
            // left and right must be equal and only long and int are supported
            lhsValue is Int && rhsValue is Int -> lhsValue xor rhsValue
            lhsValue is Long && rhsValue is Long -> lhsValue xor rhsValue
            else -> cannotEvaluate(expr, this)
        }
    }

    protected open fun handleGreater(lhsValue: Any?, rhsValue: Any?, expr: Expression?): Any? {
        return if (lhsValue is Number && rhsValue is Number) {
            lhsValue.compareTo(rhsValue) > 0
        } else {
            cannotEvaluate(expr, this)
        }
    }

    protected open fun handleGEq(lhsValue: Any?, rhsValue: Any?, expr: Expression?): Any? {
        return if (lhsValue is Number && rhsValue is Number) {
            lhsValue.compareTo(rhsValue) >= 0
        } else {
            cannotEvaluate(expr, this)
        }
    }

    protected open fun handleLess(lhsValue: Any?, rhsValue: Any?, expr: Expression?): Any? {
        return if (lhsValue is Number && rhsValue is Number) {
            lhsValue.compareTo(rhsValue) < 0
        } else {
            cannotEvaluate(expr, this)
        }
    }

    protected open fun handleLEq(lhsValue: Any?, rhsValue: Any?, expr: Expression?): Any? {
        return if (lhsValue is Number && rhsValue is Number) {
            lhsValue.compareTo(rhsValue) <= 0
        } else {
            cannotEvaluate(expr, this)
        }
    }

    protected open fun handleEq(lhsValue: Any?, rhsValue: Any?, expr: Expression?): Any? {
        return when {
            lhsValue is Number && rhsValue is Number -> {
                lhsValue.compareTo(rhsValue) == 0
            }
            lhsValue is String && rhsValue is String -> {
                lhsValue == rhsValue
            }
            else -> {
                cannotEvaluate(expr, this)
            }
        }
    }

    protected open fun handleNEq(lhsValue: Any?, rhsValue: Any?, expr: Expression?): Any? {
        return when {
            lhsValue is Number && rhsValue is Number -> {
                lhsValue.compareTo(rhsValue) != 0
            }
            lhsValue is String && rhsValue is String -> {
                lhsValue != rhsValue
            }
            else -> {
                cannotEvaluate(expr, this)
            }
        }
    }

    /**
     * We handle some basic unary operators. These also affect pointers and dereferences for
     * languages that support them.
     */
    protected open fun handleUnaryOp(expr: UnaryOperator, depth: Int): Any? {
        return when (expr.operatorCode) {
            "-" -> {
                when (val input = evaluateInternal(expr.input, depth + 1)) {
                    is Number -> -input
                    else -> cannotEvaluate(expr, this)
                }
            }
            "--" -> {
                return when (val input = evaluateInternal(expr.input, depth + 1)) {
                    is Number -> input.dec()
                    else -> cannotEvaluate(expr, this)
                }
            }
            "++" -> {
                when (val input = evaluateInternal(expr.input, depth + 1)) {
                    is Number -> input.inc()
                    else -> cannotEvaluate(expr, this)
                }
            }
            "*" -> evaluateInternal(expr.input, depth + 1)
            "&" -> evaluateInternal(expr.input, depth + 1)
            else -> cannotEvaluate(expr, this)
        }
    }

    /**
     * For arrays, we check whether we can actually access the contents of the array. This is
     * basically the case if the base of the subscript expression is a list of [KeyValueExpression]
     * s.
     */
    protected fun handleSubscriptExpression(expr: SubscriptExpression, depth: Int): Any? {
        val array = (expr.arrayExpression as? Reference)?.refersTo as? Variable
        val ile = array?.initializer as? InitializerListExpression

        ile?.let {
            return evaluateInternal(
                it.initializers
                    .filterIsInstance<KeyValueExpression>()
                    .firstOrNull { kve ->
                        (kve.key as? Literal<*>)?.value ==
                            (expr.subscriptExpression as? Literal<*>)?.value
                    }
                    ?.value,
                depth + 1,
            )
        }
        if (array?.initializer is Literal<*>) {
            return (array.initializer as Literal<*>).value
        }

        return handlePrevDFG(expr, depth + 1)
    }

    protected open fun handleConditionalExpression(expr: ConditionalExpression, depth: Int): Any? {
        var condition = expr.condition

        // Assume that condition is a binary operator
        if (condition is BinaryOperator) {
            val lhs = evaluateInternal(condition.lhs, depth)
            val rhs = evaluateInternal(condition.rhs, depth)

            // Compute the effect of the comparison
            val comparison = computeBinaryOpEffect(lhs, rhs, condition)

            return if (comparison == true) {
                evaluateInternal(expr.thenExpression, depth + 1)
            } else {
                evaluateInternal(expr.elseExpression, depth + 1)
            }
        }

        return cannotEvaluate(expr, this)
    }

    /** Tries to compute the constant value of a node based on its [Node.prevDFG]. */
    protected open fun handlePrevDFG(node: Node, depth: Int): Any? {
        // For a reference, we are interested into its last assignment into the reference
        // denoted by the previous DFG edge. We need to filter out any self-references for READWRITE
        // references.
        val prevDFG =
            if (node is Reference) {
                filterSelfReferences(node, node.prevDFG.toList())
            } else {
                node.prevDFG
            }

        return if (prevDFG.size == 1) {
            // There's only one incoming DFG edge, so we follow this one.
            evaluateInternal(prevDFG.first(), depth + 1)
        } else if (prevDFG.size > 1) {
            // We cannot have more than ONE valid solution, so we need to abort
            log.warn(
                "We cannot evaluate {}: It has more than 1 previous DFG edges, meaning that the value is probably affected by a branch.",
                node,
            )
            cannotEvaluate(node, this)
        } else {
            // No previous DFG node
            log.warn("We cannot evaluate {}: It has no previous DFG edges.", node)
            cannotEvaluate(node, this)
        }
    }

    /**
     * If a reference has READWRITE access, ignore any "self-references", e.g. from a
     * plus/minus/div/times-assign or a plusplus/minusminus, etc.
     */
    protected fun filterSelfReferences(ref: Reference, inDFG: List<Node>): List<Node> {
        var list = inDFG

        // The ops +=, -=, ... and ++, -- have in common that we see the ref twice: Once to reach
        // the operator and once to leave it. We have to differentiate between these two cases.
        // Example: i = 3 -- DFG --> i++ -- DFG --> print(i)
        // - We want to get i in the print, so we go backwards to "i" in "i++".
        // - We now have to evaluate the whole statement (one more DFG edge back). Here, we only
        // consider the statement where we already are (case 1)
        // - To evaluate i++, we go one DFG edge back again and reach "i" for a second time
        // - We now remove the statement where we already are (the "selfReference") to continue
        // before it (case 2)

        // Determines if we are in case 2
        val isCase2 = path.size > 2 && ref in path.subList(0, path.size - 2)

        if (ref.access == AccessValues.READWRITE && isCase2) {
            // Remove the self reference
            list =
                list.filter {
                    !((it is AssignExpression && it.lhs.singleOrNull() == ref) ||
                        (it is UnaryOperator && it.input == ref))
                }
        } else if (ref.access == AccessValues.READWRITE && !isCase2) {
            // Consider only the self reference
            list =
                list.filter {
                    ((it is AssignExpression && it.lhs.singleOrNull() == ref) ||
                        (it is UnaryOperator && it.input == ref))
                }
        }

        return list
    }
}

/**
 * This function is a piece of pure magic. It is one of the missing pieces in the Kotlin language
 * and compares an arbitrary [Number] with another [Number] using the dedicated compareTo functions
 * for the individual implementations of [Number], such as [Int.compareTo].
 */
fun <T : Number> Number.compareTo(other: T): Int {
    return when {
        this is Byte && other is Double -> this.compareTo(other)
        this is Byte && other is Float -> this.compareTo(other)
        this is Byte && other is Byte -> this.compareTo(other)
        this is Byte && other is Short -> this.compareTo(other)
        this is Byte && other is Int -> this.compareTo(other)
        this is Byte && other is Long -> this.compareTo(other)
        this is Short && other is Double -> this.compareTo(other)
        this is Short && other is Float -> this.compareTo(other)
        this is Short && other is Byte -> this.compareTo(other)
        this is Short && other is Short -> this.compareTo(other)
        this is Short && other is Int -> this.compareTo(other)
        this is Short && other is Long -> this.compareTo(other)
        this is Int && other is Double -> this.compareTo(other)
        this is Int && other is Float -> this.compareTo(other)
        this is Int && other is Byte -> this.compareTo(other)
        this is Int && other is Short -> this.compareTo(other)
        this is Int && other is Int -> this.compareTo(other)
        this is Int && other is Long -> this.compareTo(other)
        this is Long && other is Double -> this.compareTo(other)
        this is Long && other is Float -> this.compareTo(other)
        this is Long && other is Byte -> this.compareTo(other)
        this is Long && other is Short -> this.compareTo(other)
        this is Long && other is Int -> this.compareTo(other)
        this is Long && other is Long -> this.compareTo(other)
        this is Float && other is Double -> this.compareTo(other)
        this is Float && other is Float -> this.compareTo(other)
        this is Float && other is Byte -> this.compareTo(other)
        this is Float && other is Short -> this.compareTo(other)
        this is Float && other is Int -> this.compareTo(other)
        this is Float && other is Long -> this.compareTo(other)
        this is Double && other is Double -> this.compareTo(other)
        this is Double && other is Float -> this.compareTo(other)
        this is Double && other is Byte -> this.compareTo(other)
        this is Double && other is Short -> this.compareTo(other)
        this is Double && other is Int -> this.compareTo(other)
        this is Double && other is Long -> this.compareTo(other)
        else -> 1
    }
}
