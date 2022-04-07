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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
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
 * * Furthermore, after the execution of [evaluate], the latest evaluation path can be retrieved in
 * the [path] property of the evaluator.
 *
 * It contains some advanced mechanics such as resolution of values of arrays, if they contain
 * literal values. Furthermore, its behaviour can be adjusted by implementing the [cannotEvaluate]
 * function, which is called when the default behaviour would not be able to resolve the value. This
 * way, language specific features such as string formatting can be modelled.
 */
class ValueEvaluator(
    /**
     * Contains a reference to a function that gets called if the value cannot be resolved by the
     * standard behaviour.
     */
    val cannotEvaluate: (Node?, ValueEvaluator) -> Any? = { node: Node?, _: ValueEvaluator ->
        // end of the line, lets just keep the expression name
        if (node != null) {
            "{${node.name}}"
        } else {
            CouldNotResolve()
        }
    }
) {
    private val log: Logger
        get() = LoggerFactory.getLogger(ValueEvaluator::class.java)

    /** This property contains the path of the latest execution of [evaluate]. */
    val path: MutableList<Node> = mutableListOf()

    /** Tries to evaluate this node. Anything can happen. */
    fun evaluate(node: Node?): Any? {
        // Add the expression to the current path
        node?.let { this.path += it }

        when (node) {
            is ArrayCreationExpression -> return evaluate(node.initializer)
            is VariableDeclaration -> return evaluate(node.initializer)
            // For a literal, we can just take its value, and we are finished
            is Literal<*> -> return node.value
            is DeclaredReferenceExpression -> return handleDeclaredReferenceExpression(node)
            is UnaryOperator -> return handleUnaryOp(node)
            is BinaryOperator -> return handleBinaryOperator(node)
            // Casts are just a wrapper in this case, we are interested in the inner expression
            is CastExpression -> return this.evaluate(node.expression)
            is ArraySubscriptionExpression -> handleArraySubscriptionExpression(node)
            // While we are not handling different paths of variables with If statements, we can
            // easily be partly path-sensitive in a conditional expression
            is ConditionalExpression -> handleConditionalExpression(node)
        }

        // At this point, we cannot evaluate, and we are calling our [cannotEvaluate] hook, maybe
        // this helps
        return cannotEvaluate(node, this)
    }

    /**
     * We are handling some basic arithmetic binary operations and string operations that are more
     * or less language-independent.
     */
    private fun handleBinaryOperator(expr: BinaryOperator): Any? {
        // Resolve lhs
        val lhsValue = evaluate(expr.lhs)
        // Resolve rhs
        val rhsValue = evaluate(expr.rhs)

        return when (expr.operatorCode) {
            "+" -> handlePlus(lhsValue, rhsValue, expr)
            "-" -> handleMinus(lhsValue, rhsValue, expr)
            "/" -> handleDiv(lhsValue, rhsValue, expr)
            "*" -> handleTimes(lhsValue, rhsValue, expr)
            ">" -> handleGreater(lhsValue, rhsValue, expr)
            ">=" -> handleGEq(lhsValue, rhsValue, expr)
            "<" -> handleLess(lhsValue, rhsValue, expr)
            "<=" -> handleLEq(lhsValue, rhsValue, expr)
            "==" -> handleEq(lhsValue, rhsValue, expr)
            else -> cannotEvaluate(expr, this)
        }
    }

    private fun handlePlus(lhsValue: Any?, rhsValue: Any?, expr: BinaryOperator): Any? {
        return when {
            lhsValue is String -> lhsValue + rhsValue
            lhsValue is Int && (rhsValue is Double || rhsValue is Float) ->
                lhsValue + (rhsValue as Number).toDouble()
            lhsValue is Int && rhsValue is Number -> lhsValue + rhsValue.toLong()
            lhsValue is Long && (rhsValue is Double || rhsValue is Float) ->
                lhsValue + (rhsValue as Number).toDouble()
            lhsValue is Long && rhsValue is Number -> lhsValue + rhsValue.toLong()
            lhsValue is Short && (rhsValue is Double || rhsValue is Float) ->
                lhsValue + (rhsValue as Number).toDouble()
            lhsValue is Short && rhsValue is Number -> lhsValue + rhsValue.toLong()
            lhsValue is Byte && (rhsValue is Double || rhsValue is Float) ->
                lhsValue + (rhsValue as Number).toDouble()
            lhsValue is Byte && rhsValue is Number -> lhsValue + rhsValue.toLong()
            lhsValue is Double && rhsValue is Number -> lhsValue + rhsValue.toDouble()
            lhsValue is Float && rhsValue is Number -> lhsValue + rhsValue.toDouble()
            else -> cannotEvaluate(expr, this)
        }
    }

    private fun handleMinus(lhsValue: Any?, rhsValue: Any?, expr: BinaryOperator): Any? {
        return when {
            lhsValue is Int && (rhsValue is Double || rhsValue is Float) ->
                lhsValue - (rhsValue as Number).toDouble()
            lhsValue is Int && rhsValue is Number -> lhsValue - rhsValue.toLong()
            lhsValue is Long && (rhsValue is Double || rhsValue is Float) ->
                lhsValue - (rhsValue as Number).toDouble()
            lhsValue is Long && rhsValue is Number -> lhsValue - rhsValue.toLong()
            lhsValue is Short && (rhsValue is Double || rhsValue is Float) ->
                lhsValue - (rhsValue as Number).toDouble()
            lhsValue is Short && rhsValue is Number -> lhsValue - rhsValue.toShort()
            lhsValue is Byte && (rhsValue is Double || rhsValue is Float) ->
                lhsValue - (rhsValue as Number).toDouble()
            lhsValue is Byte && rhsValue is Number -> lhsValue - rhsValue.toByte()
            lhsValue is Double && rhsValue is Number -> lhsValue - rhsValue.toDouble()
            lhsValue is Float && rhsValue is Number -> lhsValue - rhsValue.toDouble()
            else -> cannotEvaluate(expr, this)
        }
    }

    private fun handleDiv(lhsValue: Any?, rhsValue: Any?, expr: BinaryOperator): Any? {
        return when {
            lhsValue is Int && (rhsValue is Double || rhsValue is Float) ->
                lhsValue / (rhsValue as Number).toDouble()
            lhsValue is Int && rhsValue is Number -> lhsValue / rhsValue.toLong()
            lhsValue is Long && (rhsValue is Double || rhsValue is Float) ->
                lhsValue / (rhsValue as Number).toDouble()
            lhsValue is Long && rhsValue is Number -> lhsValue / rhsValue.toLong()
            lhsValue is Short && (rhsValue is Double || rhsValue is Float) ->
                lhsValue / (rhsValue as Number).toDouble()
            lhsValue is Short && rhsValue is Number -> lhsValue / rhsValue.toLong()
            lhsValue is Byte && (rhsValue is Double || rhsValue is Float) ->
                lhsValue / (rhsValue as Number).toDouble()
            lhsValue is Byte && rhsValue is Number -> lhsValue / rhsValue.toLong()
            lhsValue is Double && rhsValue is Number -> lhsValue / rhsValue.toDouble()
            lhsValue is Float && rhsValue is Number -> lhsValue / rhsValue.toDouble()
            else -> cannotEvaluate(expr, this)
        }
    }

    private fun handleTimes(lhsValue: Any?, rhsValue: Any?, expr: BinaryOperator): Any? {
        return when {
            lhsValue is Int && (rhsValue is Double || rhsValue is Float) ->
                lhsValue * (rhsValue as Number).toDouble()
            lhsValue is Int && rhsValue is Number -> lhsValue * rhsValue.toLong()
            lhsValue is Long && (rhsValue is Double || rhsValue is Float) ->
                lhsValue * (rhsValue as Number).toDouble()
            lhsValue is Long && rhsValue is Number -> lhsValue * rhsValue.toLong()
            lhsValue is Short && (rhsValue is Double || rhsValue is Float) ->
                lhsValue * (rhsValue as Number).toDouble()
            lhsValue is Short && rhsValue is Number -> lhsValue * rhsValue.toLong()
            lhsValue is Byte && (rhsValue is Double || rhsValue is Float) ->
                lhsValue * (rhsValue as Number).toDouble()
            lhsValue is Byte && rhsValue is Number -> lhsValue * rhsValue.toLong()
            lhsValue is Double && rhsValue is Number -> lhsValue * rhsValue.toDouble()
            lhsValue is Float && rhsValue is Number -> lhsValue * rhsValue.toDouble()
            else -> cannotEvaluate(expr, this)
        }
    }

    private fun handleGreater(lhsValue: Any?, rhsValue: Any?, expr: BinaryOperator): Any? {
        return if (lhsValue is Number && rhsValue is Number) {
            lhsValue.compareTo(rhsValue) > 0
        } else {
            cannotEvaluate(expr, this)
        }
    }

    private fun handleGEq(lhsValue: Any?, rhsValue: Any?, expr: BinaryOperator): Any? {
        return if (lhsValue is Number && rhsValue is Number) {
            lhsValue.compareTo(rhsValue) >= 0
        } else {
            cannotEvaluate(expr, this)
        }
    }

    private fun handleLess(lhsValue: Any?, rhsValue: Any?, expr: BinaryOperator): Any? {
        return if (lhsValue is Number && rhsValue is Number) {
            lhsValue.compareTo(rhsValue) < 0
        } else {
            cannotEvaluate(expr, this)
        }
    }

    private fun handleLEq(lhsValue: Any?, rhsValue: Any?, expr: BinaryOperator): Any? {
        return if (lhsValue is Number && rhsValue is Number) {
            lhsValue.compareTo(rhsValue) <= 0
        } else {
            cannotEvaluate(expr, this)
        }
    }

    private fun handleEq(lhsValue: Any?, rhsValue: Any?, expr: BinaryOperator): Any? {
        return if (lhsValue is Number && rhsValue is Number) {
            lhsValue.compareTo(rhsValue) == 0
        } else {
            cannotEvaluate(expr, this)
        }
    }

    /**
     * We handle some basic unary operators. These also affect pointers and dereferences for
     * languages that support them.
     */
    private fun handleUnaryOp(expr: UnaryOperator): Any? {
        return when (expr.operatorCode) {
            "-" -> {
                when (val input = evaluate(expr.input)) {
                    is Int -> -input
                    is Long -> -input
                    is Short -> -input
                    is Byte -> -input
                    is Double -> -input
                    is Float -> -input
                    else -> cannotEvaluate(expr, this)
                }
            }
            "*" -> evaluate(expr.input)
            "&" -> evaluate(expr.input)
            else -> cannotEvaluate(expr, this)
        }
    }

    /**
     * For arrays, we check whether we can actually access the contents of the array. This is
     * basically the case if the base of the subscript expression is a list of [KeyValueExpression]
     * s.
     */
    private fun handleArraySubscriptionExpression(expr: ArraySubscriptionExpression): Any? {
        val array =
            (expr.arrayExpression as? DeclaredReferenceExpression)?.refersTo as? VariableDeclaration
        val ile = array?.initializer as? InitializerListExpression

        ile?.let {
            return evaluate(
                it.initializers
                    .filterIsInstance(KeyValueExpression::class.java)
                    .firstOrNull { kve ->
                        (kve.key as? Literal<*>)?.value ==
                            (expr.subscriptExpression as? Literal<*>)?.value
                    }
                    ?.value
            )
        }
        if (array?.initializer is Literal<*>) {
            return (array.initializer as Literal<*>).value
        }

        if (expr.arrayExpression is ArraySubscriptionExpression) {
            return evaluate(expr.arrayExpression)
        }

        return cannotEvaluate(expr, this)
    }

    private fun handleConditionalExpression(expr: ConditionalExpression): Any? {
        // Assume that condition is a binary operator
        if (expr.condition is BinaryOperator) {
            val lhs = evaluate((expr.condition as? BinaryOperator)?.lhs)
            val rhs = evaluate((expr.condition as? BinaryOperator)?.rhs)

            return if (lhs == rhs) {
                evaluate(expr.thenExpr)
            } else {
                evaluate(expr.elseExpr)
            }
        }

        return cannotEvaluate(expr, this)
    }

    /**
     * Tries to compute the constant value of a reference. It therefore checks the incoming data
     * flow edges.
     */
    private fun handleDeclaredReferenceExpression(expr: DeclaredReferenceExpression): Any? {
        // For a reference, we are interested into its last assignment into the reference
        // denoted by the previous DFG edge
        val prevDFG = expr.prevDFG

        if (prevDFG.size == 1)
        // There's only one incoming DFG edge, so we follow this one.
        return evaluate(prevDFG.first())

        // We are only interested in expressions
        val expressions = prevDFG.filterIsInstance<Expression>()

        if (expressions.size > 1) {
            // We cannot have more than ONE valid solution, so we need to abort
            log.warn(
                "We cannot evaluate {}: It has more than more previous DFG edges, meaning that the value is probably affected by a branch.",
                expr
            )
            return cannotEvaluate(expr, this)
        }

        if (expressions.isEmpty()) {
            // No previous expression?? Let's try with a variable declaration and its initialization
            val decl = prevDFG.filterIsInstance<VariableDeclaration>()
            if (decl.size > 1) {
                // We cannot have more than ONE valid solution, so we need to abort
                log.warn(
                    "We cannot evaluate {}: It has more than more previous DFG edges, meaning that the value is probably affected by a branch.",
                    expr
                )
                return cannotEvaluate(expr, this)
            }
            return evaluate(decl.firstOrNull())
        }

        return evaluate(expressions.firstOrNull())
    }
}

/**
 * This function is a piece of pure magic. It is one of the missing pieces in the Kotlin language
 * and compares an arbitrary [Number] with another [Number] using the dedicated compareTo functions
 * for the individual implementations of [Number], such as [Int.compareTo].
 */
private fun <T : Number> Number.compareTo(other: T): Int {
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
