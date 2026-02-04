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
package de.fraunhofer.aisec.cpg.evaluation

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.invoke
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.ForStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/** This [ValueEvaluator] can resolve multiple possible values of a node. */
class MultiValueEvaluator : ValueEvaluator() {
    companion object {
        const val MAX_DEPTH: Int = 20
        /** Cache calculated values so that we don't have to calculate them each time */
        private val valuesCache = ConcurrentHashMap<Int, Any>()
    }

    override val log: Logger
        get() = LoggerFactory.getLogger(MultiValueEvaluator::class.java)

    override fun evaluate(node: Any?, useCache: Boolean): Any? {
        clearPath()

        val result =
            if (useCache)
                valuesCache.getOrPut(node.hashCode()) { evaluateInternal(node as? Node, 0) }
            else evaluateInternal(node as? Node, 0)
        return if (result is Collection<*> && result.all { r -> r is Number }) {
            ConcreteNumberSet(result.map { r -> (r as Number).toLong() }.toMutableSet())
        } else {
            result
        }
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
            is FieldDeclaration -> return handleHasInitializer(node, depth)
            is NewArrayExpression -> return handleHasInitializer(node, depth)
            is VariableDeclaration -> return handleHasInitializer(node, depth)
            // For a literal, we can just take its value, and we are finished
            is Literal<*> -> return node.value
            is UnaryOperator -> return handleUnaryOp(node, depth)
            is AssignExpression -> return handleAssignExpression(node, depth)
            is BinaryOperator -> return handleBinaryOperator(node, depth)
            // Casts are just a wrapper in this case, we are interested in the inner expression
            is CastExpression -> return this.evaluateInternal(node.expression, depth + 1)
            is SubscriptExpression -> return handleSubscriptExpression(node, depth)
            // While we are not handling different paths of variables with If statements, we can
            // easily be partly path-sensitive in a conditional expression
            is ConditionalExpression -> return handleConditionalExpression(node, depth)
            else -> return handlePrevDFG(node, depth)
        }

        // At this point, we cannot evaluate, and we are calling our [cannotEvaluate] hook, maybe
        // this helps
        return cannotEvaluate(node, this)
    }

    /**
     * We are handling some basic arithmetic compound assignment operations and string operations
     * that are more or less language-independent.
     */
    override fun handleAssignExpression(node: AssignExpression, depth: Int): Any? {
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
        val elseResult = evaluateInternal(expr.elseExpression, depth + 1)
        val thenResult = evaluateInternal(expr.thenExpression, depth + 1)
        result.addAnything(thenResult)
        result.addAnything(elseResult)
        return result
    }

    override fun handleUnaryOp(expr: UnaryOperator, depth: Int): Any? {
        return when (expr.operatorCode) {
            "-" -> {
                when (val input = evaluateInternal(expr.input, depth + 1)) {
                    is Collection<*> -> input.map { n -> (n as? Number)?.unaryMinus() }
                    else -> super.handleUnaryOp(expr, depth)
                }
            }
            "--" -> {
                if (expr.astParent is ForStatement) {
                    evaluateInternal(expr.input, depth + 1)
                } else {
                    when (val input = evaluateInternal(expr.input, depth + 1)) {
                        is Collection<*> -> input.map { n -> (n as? Number)?.dec() }
                        else -> super.handleUnaryOp(expr, depth)
                    }
                }
            }
            "++" -> {
                if (expr.astParent is ForStatement) {
                    evaluateInternal(expr.input, depth + 1)
                } else {
                    when (val input = evaluateInternal(expr.input, depth + 1)) {
                        is Collection<*> -> input.map { n -> (n as? Number)?.inc() }
                        else -> super.handleUnaryOp(expr, depth)
                    }
                }
            }
            else -> super.handleUnaryOp(expr, depth)
        }
    }

    /**
     * Tries to compute the value of a node based on its [Node.prevDFG].
     *
     * In contrast to the implementation of [ValueEvaluator], this one can handle more than one
     * value.
     */
    override fun handlePrevDFG(node: Node, depth: Int): Collection<Any?> {
        // For a reference, we are interested in its last assignment into the reference
        // denoted by the previous DFG edge. We need to filter out any self-references for READWRITE
        // references.
        val prevDFG =
            if (node is Reference) {
                filterSelfReferences(node, node.prevDFG.toList())
            } else {
                node.prevDFG
            }

        if (prevDFG.size == 1) {
            // There's only one incoming DFG edge, so we follow this one.
            val internalRes = evaluateInternal(prevDFG.first(), depth + 1)
            return (internalRes as? Collection<*>) ?: mutableSetOf(internalRes)
        }

        if (node is Reference && prevDFG.size == 2 && prevDFG.all(::isSimpleForLoop)) {
            return handleSimpleLoopVariable(node, depth)
        }

        val result = mutableSetOf<Any?>()
        if (prevDFG.isEmpty()) {
            // No previous expression?? Let's try with a variable declaration and its initialization
            val decl = prevDFG.filterIsInstance<VariableDeclaration>()
            for (declaration in decl) {
                val res = evaluateInternal(declaration, depth + 1)
                result.addAnything(res)
            }
        }

        for (expression in prevDFG) {
            val res = evaluateInternal(expression, depth + 1)
            result.addAnything(res)
        }
        return result
    }

    private fun MutableSet<Any?>.addAnything(element: Any?) {
        if (element is Collection<*>) this.addAll(element) else this.add(element)
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
                    node.astParent) || // The parent of the node is the initializer of the
            // loop
            // variable
            forStatement.iterationStatement ==
                node || // The node or its parent are the iteration statement of the loop
            forStatement.iterationStatement == node.astParent
    }

    private fun handleSimpleLoopVariable(expr: Reference, depth: Int): Collection<Any?> {
        val loop =
            expr.prevDFG.firstOrNull { it.astParent is ForStatement }?.astParent as? ForStatement
                ?: expr.prevDFG
                    .firstOrNull { it.astParent?.astParent is ForStatement }
                    ?.astParent
                    ?.astParent as? ForStatement
        if (loop == null || loop.condition !is BinaryOperator) return setOf()

        var loopVar: Any? =
            evaluateInternal(loop.initializerStatement?.declarations?.first(), depth) as? Number
                ?: return setOf()

        val cond = loop.condition as BinaryOperator
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
                    is AssignExpression -> {
                        if (
                            loopOp.operatorCode == "=" &&
                                (loopOp.lhs.singleOrNull() as? Reference)?.refersTo ==
                                    expr.refersTo &&
                                loopOp.rhs.singleOrNull() is BinaryOperator
                        ) {
                            // Assignment to the variable, take the rhs and see if it's also a
                            // binary operator
                            val opLhs =
                                if (
                                    ((loopOp.rhs<BinaryOperator>())?.lhs as? Reference)?.refersTo ==
                                        expr.refersTo
                                ) {
                                    loopVar
                                } else {
                                    (loopOp.rhs<BinaryOperator>())?.lhs
                                }
                            val opRhs =
                                if (
                                    ((loopOp.rhs<BinaryOperator>())?.rhs as? Reference)?.refersTo ==
                                        expr.refersTo
                                ) {
                                    loopVar
                                } else {
                                    evaluateInternal((loopOp.rhs<BinaryOperator>())?.rhs, depth + 1)
                                }
                            computeBinaryOpEffect(opLhs, opRhs, (loopOp.rhs<BinaryOperator>()))
                                as? Number
                        } else {
                            cannotEvaluate(loopOp, this)
                        }
                    }
                    is BinaryOperator -> {

                        // No idea what this is but it's a binary op...
                        val opLhs =
                            if ((loopOp.lhs as? Reference)?.refersTo == expr.refersTo) {
                                loopVar
                            } else {
                                loopOp.lhs
                            }
                        val opRhs =
                            if ((loopOp.rhs as? Reference)?.refersTo == expr.refersTo) {
                                loopVar
                            } else {
                                loopOp.rhs
                            }
                        computeBinaryOpEffect(opLhs, opRhs, loopOp) as? Number
                    }
                    is UnaryOperator -> {
                        computeUnaryOpEffect(
                            if ((loopOp.input as? Reference)?.refersTo == expr.refersTo) {
                                loopVar
                            } else {
                                loopOp.input
                            },
                            loopOp,
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

    private fun computeUnaryOpEffect(input: Any?, expr: UnaryOperator): Any? {
        return when (expr.operatorCode) {
            "-" -> {
                when (input) {
                    is Collection<*> -> input.map { n -> (n as? Number)?.unaryMinus() }
                    is Number -> -input
                    else -> cannotEvaluate(expr, this)
                }
            }
            "--" -> {
                when (input) {
                    is Number -> input.dec()
                    is Collection<*> -> input.map { n -> (n as? Number)?.dec() }
                    else -> cannotEvaluate(expr, this)
                }
            }
            "++" -> {
                when (input) {
                    is Number -> input.inc()
                    is Collection<*> -> input.map { n -> (n as? Number)?.inc() }
                    else -> cannotEvaluate(expr, this)
                }
            }
            else -> cannotEvaluate(expr, this)
        }
    }
}
