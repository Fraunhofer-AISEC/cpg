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
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.ForStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.passes.astParent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This [ValueEvaluator] can resolve multiple possible values of a node.
 *
 * It requires running the [EdgeCachePass] after the translation to add all necessary edges.
 */
class MultiValueEvaluator : ValueEvaluator() {
    companion object {
        const val MAX_DEPTH: Int = 20
    }

    override val log: Logger
        get() = LoggerFactory.getLogger(MultiValueEvaluator::class.java)

    override fun evaluate(node: Any?): Any? {
        val result = evaluateInternal(node as? Node, 0)
        return if (result is Collection<*> && result.all { r -> r is Number })
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
            is ArraySubscriptionExpression -> return handleArraySubscriptionExpression(node, depth)
            // While we are not handling different paths of variables with If statements, we can
            // easily be partly path-sensitive in a conditional expression
            is ConditionalExpression -> return handleConditionalExpression(node, depth)
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

        if (lhsValue !is Collection<*> && rhsValue !is Collection<*>) {
            return computeBinaryOpEffect(lhsValue, rhsValue, expr)
        }

        val result = mutableSetOf<Any?>()
        if (lhsValue is Collection<*>) {
            for (lhs in lhsValue) {
                if (rhsValue is Collection<*>) {
                    result.addAll(rhsValue.map { r -> computeBinaryOpEffect(lhs, r, expr) })
                } else {
                    result.add(computeBinaryOpEffect(lhs, rhsValue, expr))
                }
            }
        } else {
            result.addAll(
                (rhsValue as Collection<*>).map { r -> computeBinaryOpEffect(lhsValue, r, expr) }
            )
        }

        return result
    }

    override fun handleConditionalExpression(expr: ConditionalExpression, depth: Int): Any {
        val result = mutableSetOf<Any?>()
        val elseResult = evaluateInternal(expr.elseExpr, depth + 1)
        val thenResult = evaluateInternal(expr.thenExpr, depth + 1)
        if (thenResult is Collection<*>) result.addAll(thenResult) else result.add(thenResult)
        if (elseResult is Collection<*>) result.addAll(elseResult) else result.add(elseResult)
        return result
    }

    override fun handleUnaryOp(expr: UnaryOperator, depth: Int): Any? {
        return when (expr.operatorCode) {
            "-" -> {
                when (val input = evaluateInternal(expr.input, depth + 1)) {
                    is Collection<*> -> input.map { n -> (n as? Number)?.negate() }
                    is Number -> input.negate()
                    else -> cannotEvaluate(expr, this)
                }
            }
            "--" -> {
                if (expr.astParent is ForStatement) {
                    evaluateInternal(expr.input, depth + 1)
                } else {
                    when (val input = evaluateInternal(expr.input, depth + 1)) {
                        is Number -> input.decrement()
                        is Collection<*> -> input.map { n -> (n as? Number)?.decrement() }
                        else -> cannotEvaluate(expr, this)
                    }
                }
            }
            "++" -> {
                if (expr.astParent is ForStatement) {
                    evaluateInternal(expr.input, depth + 1)
                } else {
                    when (val input = evaluateInternal(expr.input, depth + 1)) {
                        is Number -> input.increment()
                        is Collection<*> -> input.map { n -> (n as? Number)?.increment() }
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
    ): Collection<Any?> {
        // For a reference, we are interested in its last assignment into the reference
        // denoted by the previous DFG edge. We need to filter out any self-references for READWRITE
        // references.
        val prevDFG = filterSelfReferences(expr, expr.prevDFG.toList())

        if (prevDFG.size == 1) {
            // There's only one incoming DFG edge, so we follow this one.
            val internalRes = evaluateInternal(prevDFG.first(), depth + 1)
            return if (internalRes is Collection<*>) internalRes else mutableSetOf(internalRes)
        }

        if (prevDFG.size == 2 && prevDFG.all(::isSimpleForLoop)) {
            return handleSimpleLoopVariable(expr, depth)
        }

        val result = mutableSetOf<Any?>()
        if (prevDFG.isEmpty()) {
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

        for (expression in prevDFG) {
            val res = evaluateInternal(expression, depth + 1)
            if (res is Collection<*>) {
                result.addAll(res)
            } else {
                result.add(res)
            }
        }
        return result
    }

    private fun isSimpleForLoop(node: Node): Boolean {
        // Are we in the for statement somehow?
        var forStatement = node.astParent as? ForStatement
        if (forStatement == null) forStatement = node.astParent?.astParent as? ForStatement

        if (forStatement == null) return false // ...no, we're not.

        val initializerDecl =
            (forStatement.initializerStatement as? DeclarationStatement)?.singleDeclaration

        return initializerDecl == node || // The node is the declaration of the loop variable
        forStatement.initializerStatement == node || // The node is the initialization
            (initializerDecl != null &&
                initializerDecl ==
                    node.astParent) || // The parent of the node is the initializer of the loop
            // variable
            forStatement.iterationStatement ==
                node || // The node or its parent are the iteration statement of the loop
            forStatement.iterationStatement == node.astParent
    }

    private fun handleSimpleLoopVariable(
        expr: DeclaredReferenceExpression,
        depth: Int
    ): Collection<Any?> {
        val loop =
            expr.prevDFG.firstOrNull { e -> e.astParent is ForStatement }?.astParent
                as? ForStatement
        if (loop == null || loop.condition !is BinaryOperator) return setOf()

        var loopVar: Number? =
            evaluateInternal(loop.initializerStatement.declarations.first(), depth) as? Number
                ?: return setOf()

        val cond = loop.condition as BinaryOperator
        val result = mutableSetOf<Any?>()
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
                        if (
                            loopOp.operatorCode == "=" &&
                                (loopOp.lhs as? DeclaredReferenceExpression)?.refersTo ==
                                    expr.refersTo &&
                                loopOp.rhs is BinaryOperator
                        ) {
                            // Assignment to the variable, take the rhs and see if it's also a
                            // binary operator
                            val opLhs =
                                if (
                                    ((loopOp.rhs as BinaryOperator).lhs
                                            as? DeclaredReferenceExpression)
                                        ?.refersTo == expr.refersTo
                                ) {
                                    loopVar
                                } else {
                                    (loopOp.rhs as BinaryOperator).lhs
                                }
                            val opRhs =
                                if (
                                    ((loopOp.rhs as BinaryOperator).rhs
                                            as? DeclaredReferenceExpression)
                                        ?.refersTo == expr.refersTo
                                ) {
                                    loopVar
                                } else {
                                    evaluateInternal((loopOp.rhs as BinaryOperator).rhs, depth + 1)
                                }
                            computeBinaryOpEffect(opLhs, opRhs, (loopOp.rhs as BinaryOperator))
                                as? Number
                        } else {
                            // No idea what this is but it's a binary op...
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
                    is Collection<*> -> input.map { n -> (n as? Number)?.negate() }
                    is Number -> input.negate()
                    else -> cannotEvaluate(expr, this)
                }
            }
            "--" -> {
                when (input) {
                    is Number -> input.decrement()
                    is Collection<*> -> input.map { n -> (n as? Number)?.decrement() }
                    else -> cannotEvaluate(expr, this)
                }
            }
            "++" -> {
                when (input) {
                    is Number -> input.increment()
                    is Collection<*> -> input.map { n -> (n as? Number)?.increment() }
                    else -> cannotEvaluate(expr, this)
                }
            }
            else -> cannotEvaluate(expr, this)
        }
    }
}
