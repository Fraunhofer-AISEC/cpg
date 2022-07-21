/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.analysis

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.ValueEvaluator
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.negate
import de.fraunhofer.aisec.cpg.graph.statements.ForStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.passes.astParent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class MultiValueEvaluator : ValueEvaluator() {
    companion object {
        const val MAX_DEPTH: Int = 10
    }

    override val log: Logger
        get() = LoggerFactory.getLogger(MultiValueEvaluator::class.java)

    override fun evaluate(node: Any?): Any? {
        val result = evaluateInternal(node as? Node, 0)
        return if (result is List<*> && result.all { r -> r is Number })
            ConcreteNumberSet(result.map { r -> (r as Number).toLong() }.toMutableSet())
        else result
    }

    /** Tries to evaluate this node. Anything can happen. */
    override fun evaluateInternal(node: Node?, depth: Int): Any? {
        if (node == null) {
            return null
        }

        if (depth > MAX_DEPTH) {
            return cannotEvaluate(node, this)
        }
        // Add the expression to the current path
        this.path += node

        when (node) {
            is FieldDeclaration -> {
                return evaluateInternal(node.initializer, depth + 1)
            }
            is ArrayCreationExpression -> return evaluateInternal(node.initializer, depth + 1)
            is VariableDeclaration -> return evaluateInternal(node.initializer, depth + 1)
            // For a literal, we can just take its value, and we are finished
            is Literal<*> -> return node.value
            is DeclaredReferenceExpression -> return handleDeclaredReferenceExpression(node, depth)
            is UnaryOperator -> return handleUnaryOp(node, depth)
            is BinaryOperator -> return handleBinaryOperator(node, depth)
            // Casts are just a wrapper in this case, we are interested in the inner expression
            is CastExpression -> return this.evaluateInternal(node.expression, depth + 1)
            is ArraySubscriptionExpression -> handleArraySubscriptionExpression(node, depth)
            // While we are not handling different paths of variables with If statements, we can
            // easily be partly path-sensitive in a conditional expression
            is ConditionalExpression -> handleConditionalExpression(node, depth)
        }

        // At this point, we cannot evaluate, and we are calling our [cannotEvaluate] hook, maybe
        // this helps
        return cannotEvaluate(node, this)
    }

    /**
     * We are handling some basic arithmetic binary operations and string operations that are more
     * or less language-independent.
     */
    override fun handleBinaryOperator(expr: BinaryOperator, depth: Int): Any? {
        // Resolve lhs
        val lhsValue = evaluateInternal(expr.lhs, depth + 1)
        // Resolve rhs
        val rhsValue = evaluateInternal(expr.rhs, depth + 1)

        if (lhsValue !is List<*> && rhsValue !is List<*>) {
            return computeBinaryOpEffect(lhsValue, rhsValue, expr)
        }

        val result = mutableListOf<Any?>()
        if (lhsValue is List<*>) {
            for (lhs in lhsValue) {
                if (rhsValue is List<*>) {
                    result.addAll(rhsValue.map { r -> computeBinaryOpEffect(lhs, r, expr) })
                } else {
                    result.add(computeBinaryOpEffect(lhs, rhsValue, expr))
                }
            }
        } else {
            result.addAll(
                (rhsValue as List<*>).map { r -> computeBinaryOpEffect(lhsValue, r, expr) }
            )
        }

        return result
    }

    override fun handleConditionalExpression(expr: ConditionalExpression, depth: Int): Any? {
        // Assume that condition is a binary operator
        if (expr.condition is BinaryOperator) {
            val lhs = evaluateInternal((expr.condition as? BinaryOperator)?.lhs, depth + 1)
            val rhs = evaluateInternal((expr.condition as? BinaryOperator)?.rhs, depth + 1)

            return if (lhs is List<*> && lhs.size > 1 && rhs is List<*> && rhs.size > 1) {
                val result = mutableListOf<Any?>()
                val elseResult = evaluateInternal(expr.elseExpr, depth + 1)
                if (elseResult is List<*>) result.addAll(elseResult) else result.add(elseResult)
                if (lhs.any { l -> l in rhs }) {
                    val thenResult = evaluateInternal(expr.thenExpr, depth + 1)
                    if (thenResult is List<*>) result.addAll(thenResult) else result.add(thenResult)
                }
                result
            } else if (
                lhs is List<*> && rhs is List<*> && lhs.firstOrNull() == rhs.firstOrNull() ||
                    lhs is List<*> && lhs.firstOrNull() == rhs ||
                    rhs is List<*> && rhs.firstOrNull() == lhs ||
                    lhs == rhs
            ) {
                evaluateInternal(expr.thenExpr, depth + 1)
            } else {
                evaluateInternal(expr.elseExpr, depth + 1)
            }
        }

        return cannotEvaluate(expr, this)
    }

    override fun handleUnaryOp(expr: UnaryOperator, depth: Int): Any? {
        return when (expr.operatorCode) {
            "-" -> {
                when (val input = evaluateInternal(expr.input, depth + 1)) {
                    is List<*> -> input.map { n -> (n as? Number)?.negate() }
                    is Number -> input.negate()
                    else -> cannotEvaluate(expr, this)
                }
            }
            "++" -> {
                if (expr.astParent is ForStatement) {
                    evaluateInternal(expr.input, depth + 1)
                } else {
                    when (val input = evaluateInternal(expr.input, depth + 1)) {
                        is Number -> input.toLong() + 1
                        is List<*> -> input.map { n -> (n as? Number)?.toLong()?.plus(1) }
                        else -> cannotEvaluate(expr, this)
                    }
                }
            }
            "*" -> evaluateInternal(expr.input, depth + 1)
            "&" -> evaluateInternal(expr.input, depth + 1)
            else -> cannotEvaluate(expr, this)
        }
    }

    /**
     * Tries to compute the value of a reference. It therefore checks the incoming data flow edges.
     *
     * In contrast to the implementation of [ValueEvaluator], this one can handle more than one
     * value.
     */
    override fun handleDeclaredReferenceExpression(
        expr: DeclaredReferenceExpression,
        depth: Int
    ): List<Any?> {
        // For a reference, we are interested in its last assignment into the reference
        // denoted by the previous DFG edge
        val prevDFG = expr.prevDFG

        if (prevDFG.size == 1) {
            // There's only one incoming DFG edge, so we follow this one.
            return mutableListOf(evaluateInternal(prevDFG.first(), depth + 1))
        }

        // We are only interested in expressions
        val expressions = prevDFG.filterIsInstance<Expression>()

        if (
            expressions.size == 2 &&
                expressions.all { e ->
                    (e.astParent?.astParent as? ForStatement)?.initializerStatement == e ||
                        (e.astParent as? ForStatement)?.iterationStatement == e
                }
        ) {
            return handleSimpleLoopVariable(expr, depth)
        }

        val result = mutableListOf<Any?>()
        if (expressions.isEmpty()) {
            // No previous expression?? Let's try with a variable declaration and its initialization
            val decl = prevDFG.filterIsInstance<VariableDeclaration>()
            for (declaration in decl) {
                val res = evaluateInternal(declaration, depth + 1)
                if (res is Collection<*>) {
                    result.addAll(res)
                } else {
                    result.add(res)
                }
            }
        }

        for (expression in expressions) {
            val res = evaluateInternal(expression, depth + 1)
            if (res is Collection<*>) {
                result.addAll(res)
            } else {
                result.add(res)
            }
        }
        return result
    }

    private fun handleSimpleLoopVariable(
        expr: DeclaredReferenceExpression,
        depth: Int
    ): List<Any?> {
        val loop =
            expr.prevDFG.firstOrNull { e -> e.astParent is ForStatement }?.astParent
                as? ForStatement
        if (loop == null || loop.condition !is BinaryOperator) return listOf()

        var loopVar =
            evaluateInternal(loop.initializerStatement.declarations.first(), depth) as? Number

        if (loopVar == null) return listOf()

        val cond = loop.condition as BinaryOperator
        val result = mutableListOf<Any?>()
        var lhs =
            if ((cond.lhs as? DeclaredReferenceExpression)?.refersTo == expr.refersTo) {
                loopVar
            } else {
                evaluateInternal(cond.lhs, depth + 1)
            }
        var rhs =
            if ((cond.rhs as? DeclaredReferenceExpression)?.refersTo == expr.refersTo) {
                loopVar
            } else {
                evaluateInternal(cond.rhs, depth + 1)
            }

        var comparisonResult = computeBinaryOpEffect(lhs, rhs, cond)
        while (comparisonResult == true) {
            result.add(
                loopVar
            ) // We skip the last iteration on purpose because that last operation will be added by
            // the statement which made us end up here.

            val loopOp = loop.iterationStatement
            loopVar =
                when (loopOp) {
                    is BinaryOperator -> {
                        val opLhs =
                            if (
                                (loopOp.lhs as? DeclaredReferenceExpression)?.refersTo ==
                                    expr.refersTo
                            ) {
                                loopVar
                            } else {
                                loopOp.lhs
                            }
                        val opRhs =
                            if (
                                (loopOp.rhs as? DeclaredReferenceExpression)?.refersTo ==
                                    expr.refersTo
                            ) {
                                loopVar
                            } else {
                                loopOp.rhs
                            }
                        computeBinaryOpEffect(opLhs, opRhs, loopOp) as? Number
                    }
                    is UnaryOperator -> {
                        computeUnaryOpEffect(
                            if (
                                (loopOp.input as? DeclaredReferenceExpression)?.refersTo ==
                                    expr.refersTo
                            ) {
                                loopVar!!
                            } else {
                                loopOp.input
                            },
                            loopOp
                        )
                            as? Number
                    }
                    else -> {
                        null
                    }
                }
            if (loopVar == null) {
                return result
            }
            // result.add(loopVar)

            if ((cond.lhs as? DeclaredReferenceExpression)?.refersTo == expr.refersTo) {
                lhs = loopVar
            }
            if ((cond.rhs as? DeclaredReferenceExpression)?.refersTo == expr.refersTo) {
                rhs = loopVar
            }
            comparisonResult = computeBinaryOpEffect(lhs, rhs, cond)
        }
        return result
    }

    private fun computeUnaryOpEffect(input: Any, expr: UnaryOperator): Any? {
        return when (expr.operatorCode) {
            "-" -> {
                when (input) {
                    is List<*> -> input.map { n -> (n as? Number)?.negate() }
                    is Number -> input.negate()
                    else -> cannotEvaluate(expr, this)
                }
            }
            "++" -> {
                when (input) {
                    is Number -> input.toLong() + 1
                    is List<*> -> input.map { n -> (n as? Number)?.toLong()?.plus(1) }
                    else -> cannotEvaluate(expr, this)
                }
            }
            else -> cannotEvaluate(expr, this)
        }
    }
}
