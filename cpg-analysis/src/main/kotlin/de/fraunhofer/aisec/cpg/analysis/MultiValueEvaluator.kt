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
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDecl
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDecl
import de.fraunhofer.aisec.cpg.graph.invoke
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStmt
import de.fraunhofer.aisec.cpg.graph.statements.ForStmt
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.passes.EdgeCachePass
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
            is FieldDecl -> {
                return evaluateInternal(node.initializer, depth + 1)
            }
            is ArrayExpr -> return evaluateInternal(node.initializer, depth + 1)
            is VariableDecl -> return evaluateInternal(node.initializer, depth + 1)
            // For a literal, we can just take its value, and we are finished
            is Literal<*> -> return node.value
            is Reference -> return handleDeclaredReferenceExpression(node, depth)
            is UnaryOp -> return handleUnaryOp(node, depth)
            is AssignExpr -> return handleAssignExpression(node, depth)
            is BinaryOp -> return handleBinaryOperator(node, depth)
            // Casts are just a wrapper in this case, we are interested in the inner expression
            is CastExpr -> return this.evaluateInternal(node.expression, depth + 1)
            is SubscriptionExpr -> return handleArraySubscriptionExpression(node, depth)
            // While we are not handling different paths of variables with If statements, we can
            // easily be partly path-sensitive in a conditional expression
            is ConditionalExpr -> return handleConditionalExpression(node, depth)
        }

        // At this point, we cannot evaluate, and we are calling our [cannotEvaluate] hook, maybe
        // this helps
        return cannotEvaluate(node, this)
    }

    /**
     * We are handling some basic arithmetic compound assignment operations and string operations
     * that are more or less language-independent.
     */
    override fun handleAssignExpression(node: AssignExpr, depth: Int): Any? {
        // This only works for compound assignments
        if (!node.isCompoundAssignment) {
            return super.handleAssignExpression(node, depth)
        }

        // Resolve lhs
        val lhsValue = evaluateInternal(node.lhs.singleOrNull(), depth + 1)
        // Resolve rhs
        val rhsValue = evaluateInternal(node.rhs.singleOrNull(), depth + 1)

        if (lhsValue !is Collection<*> && rhsValue !is Collection<*>) {
            return computeBinaryOpEffect(lhsValue, rhsValue, node)
        }

        val result = mutableSetOf<Any?>()
        if (lhsValue is Collection<*>) {
            // lhsValue is a collection. We compute the result for all lhsValues with all the
            // rhsValue(s).
            for (lhs in lhsValue) {
                if (rhsValue is Collection<*>) {
                    result.addAll(rhsValue.map { r -> computeBinaryOpEffect(lhs, r, node) })
                } else {
                    result.add(computeBinaryOpEffect(lhs, rhsValue, node))
                }
            }
        } else {
            // lhsValue is not a collection (so rhsValues is because if both wouldn't be a
            // collection, this would be covered by the if-statement some lines above). We compute
            // the result for the lhsValue with all the rhsValues.
            result.addAll(
                (rhsValue as Collection<*>).map { r -> computeBinaryOpEffect(lhsValue, r, node) }
            )
        }

        return result
    }

    /**
     * We are handling some basic arithmetic binary operations and string operations that are more
     * or less language-independent.
     */
    override fun handleBinaryOperator(expr: BinaryOp, depth: Int): Any? {
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

    override fun handleConditionalExpression(expr: ConditionalExpr, depth: Int): Any {
        val result = mutableSetOf<Any?>()
        val elseResult = evaluateInternal(expr.elseExpr, depth + 1)
        val thenResult = evaluateInternal(expr.thenExpr, depth + 1)
        if (thenResult is Collection<*>) result.addAll(thenResult) else result.add(thenResult)
        if (elseResult is Collection<*>) result.addAll(elseResult) else result.add(elseResult)
        return result
    }

    override fun handleUnaryOp(expr: UnaryOp, depth: Int): Any? {
        return when (expr.operatorCode) {
            "-" -> {
                when (val input = evaluateInternal(expr.input, depth + 1)) {
                    is Collection<*> -> input.map { n -> (n as? Number)?.negate() }
                    is Number -> input.negate()
                    else -> cannotEvaluate(expr, this)
                }
            }
            "--" -> {
                if (expr.astParent is ForStmt) {
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
                if (expr.astParent is ForStmt) {
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
        expr: Reference,
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
            val decl = prevDFG.filterIsInstance<VariableDecl>()
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
        var forStmt = node.astParent as? ForStmt
        if (forStmt == null) forStmt = node.astParent?.astParent as? ForStmt

        if (forStmt == null) return false // ...no, we're not.

        val initializerDecl =
            (forStmt.initializerStatement as? DeclarationStmt)?.singleDeclaration

        return initializerDecl == node || // The node is the declaration of the loop variable
        forStmt.initializerStatement == node || // The node is the initialization
            (initializerDecl != null &&
                initializerDecl ==
                    node.astParent) || // The parent of the node is the initializer of the loop
            // variable
            forStmt.iterationStatement ==
                node || // The node or its parent are the iteration statement of the loop
            forStmt.iterationStatement == node.astParent
    }

    private fun handleSimpleLoopVariable(
        expr: Reference,
        depth: Int
    ): Collection<Any?> {
        val loop =
            expr.prevDFG.firstOrNull { e -> e.astParent is ForStmt }?.astParent
                as? ForStmt
        if (loop == null || loop.condition !is BinaryOp) return setOf()

        var loopVar: Any? =
            evaluateInternal(loop.initializerStatement?.declarations?.first(), depth) as? Number
                ?: return setOf()

        val cond = loop.condition as BinaryOp
        val result = mutableSetOf<Any?>()
        var lhs =
            if ((cond.lhs as? Reference)?.refersTo == expr.refersTo) {
                loopVar
            } else {
                evaluateInternal(cond.lhs, depth + 1)
            }
        var rhs =
            if ((cond.rhs as? Reference)?.refersTo == expr.refersTo) {
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
                    is AssignExpr -> {
                        if (
                            loopOp.operatorCode == "=" &&
                                (loopOp.lhs.singleOrNull() as? Reference)
                                    ?.refersTo == expr.refersTo &&
                                loopOp.rhs.singleOrNull() is BinaryOp
                        ) {
                            // Assignment to the variable, take the rhs and see if it's also a
                            // binary operator
                            val opLhs =
                                if (
                                    ((loopOp.rhs<BinaryOp>())?.lhs
                                            as? Reference)
                                        ?.refersTo == expr.refersTo
                                ) {
                                    loopVar
                                } else {
                                    (loopOp.rhs<BinaryOp>())?.lhs
                                }
                            val opRhs =
                                if (
                                    ((loopOp.rhs<BinaryOp>())?.rhs
                                            as? Reference)
                                        ?.refersTo == expr.refersTo
                                ) {
                                    loopVar
                                } else {
                                    evaluateInternal((loopOp.rhs<BinaryOp>())?.rhs, depth + 1)
                                }
                            computeBinaryOpEffect(opLhs, opRhs, (loopOp.rhs<BinaryOp>()))
                                as? Number
                        } else {
                            cannotEvaluate(loopOp, this)
                        }
                    }
                    is BinaryOp -> {

                        // No idea what this is but it's a binary op...
                        val opLhs =
                            if (
                                (loopOp.lhs as? Reference)?.refersTo ==
                                    expr.refersTo
                            ) {
                                loopVar
                            } else {
                                loopOp.lhs
                            }
                        val opRhs =
                            if (
                                (loopOp.rhs as? Reference)?.refersTo ==
                                    expr.refersTo
                            ) {
                                loopVar
                            } else {
                                loopOp.rhs
                            }
                        computeBinaryOpEffect(opLhs, opRhs, loopOp) as? Number
                    }
                    is UnaryOp -> {
                        computeUnaryOpEffect(
                            if (
                                (loopOp.input as? Reference)?.refersTo ==
                                    expr.refersTo
                            ) {
                                loopVar
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

            if ((cond.lhs as? Reference)?.refersTo == expr.refersTo) {
                lhs = loopVar
            }
            if ((cond.rhs as? Reference)?.refersTo == expr.refersTo) {
                rhs = loopVar
            }
            comparisonResult = computeBinaryOpEffect(lhs, rhs, cond)
        }
        return result
    }

    private fun computeUnaryOpEffect(input: Any?, expr: UnaryOp): Any? {
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
