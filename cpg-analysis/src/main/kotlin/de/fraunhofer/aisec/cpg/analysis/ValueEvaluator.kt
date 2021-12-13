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

import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
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
 * * Furthermore, after the execution of [evaluate] or [evaluateDeclaration], the latest evaluation
 * path can be retrieved in the [path] property of the evaluator.
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

    /**
     * This property contains the path of the latest execution of [evaluate] or
     * [evaluateDeclaration].
     */
    val path: MutableList<Node> = mutableListOf()

    /** Tries to evaluate this declaration, basically using [evaluate] on its initializer. */
    fun evaluateDeclaration(decl: Declaration?): Any? {
        decl?.let { this.path += it }
        when (decl) {
            is VariableDeclaration -> return evaluate(decl.initializer)
            is FieldDeclaration -> {
                return evaluate(decl.initializer)
            }
        }

        return cannotEvaluate(decl, this)
    }

    /** Tries to evaluate this expression. Anything can happen. */
    fun evaluate(expr: Expression?): Any? {
        // Add the expression to the current path
        expr?.let { this.path += it }

        when (expr) {
            // For a literal, we can just take its value, and we are finished
            is Literal<*> -> {
                return expr.value
            }
            // For a reference, we are interested into its last assignment into the reference
            // denoted by the previous DFG edge
            is DeclaredReferenceExpression -> {
                val prevDFG = expr.prevDFG

                // We are only interested in expressions
                val expressions = prevDFG.filterIsInstance<Expression>()

                if (expressions.size > 1) {
                    // We cannot have ONE valid solution, so we need to abort
                    log.warn(
                        "We cannot evaluate {}: It has more than more previous DFG edges, meaning that the value is probably affected by a branch.",
                        expr
                    )
                    return cannotEvaluate(expr, this)
                }

                return evaluate(expressions.firstOrNull())
            }
            // We are handling some basic arithmetic binary operations and string operations that
            // are more or less language-independent
            is BinaryOperator -> {
                // Resolve lhs
                val lhsValue = evaluate(expr.lhs)

                // Resolve rhs
                val rhsValue = evaluate(expr.rhs)

                if (expr.operatorCode == "+") {
                    if (lhsValue is String) {
                        return lhsValue + rhsValue
                    } else if (lhsValue is Int && rhsValue is Number) {
                        return lhsValue + rhsValue.toInt()
                    } else if (lhsValue is Long && rhsValue is Number) {
                        return lhsValue + rhsValue.toLong()
                    } else if (lhsValue is Short && rhsValue is Number) {
                        return lhsValue + rhsValue.toShort()
                    } else if (lhsValue is Byte && rhsValue is Number) {
                        return lhsValue + rhsValue.toByte()
                    } else if (lhsValue is Double && rhsValue is Number) {
                        return lhsValue + rhsValue.toDouble()
                    } else if (lhsValue is Float && rhsValue is Number) {
                        return lhsValue + rhsValue.toDouble()
                    }
                } else if (expr.operatorCode == "-") {
                    if (lhsValue is Int && rhsValue is Number) {
                        return lhsValue - rhsValue.toInt()
                    } else if (lhsValue is Long && rhsValue is Number) {
                        return lhsValue - rhsValue.toLong()
                    } else if (lhsValue is Short && rhsValue is Number) {
                        return lhsValue - rhsValue.toShort()
                    } else if (lhsValue is Byte && rhsValue is Number) {
                        return lhsValue - rhsValue.toByte()
                    } else if (lhsValue is Double && rhsValue is Number) {
                        return lhsValue - rhsValue.toDouble()
                    } else if (lhsValue is Float && rhsValue is Number) {
                        return lhsValue - rhsValue.toDouble()
                    }
                } else if (expr.operatorCode == "/") {
                    if (lhsValue is Int && rhsValue is Number) {
                        return lhsValue / rhsValue.toInt()
                    } else if (lhsValue is Long && rhsValue is Number) {
                        return lhsValue / rhsValue.toLong()
                    } else if (lhsValue is Short && rhsValue is Number) {
                        return lhsValue / rhsValue.toShort()
                    } else if (lhsValue is Byte && rhsValue is Number) {
                        return lhsValue / rhsValue.toByte()
                    } else if (lhsValue is Double && rhsValue is Number) {
                        return lhsValue / rhsValue.toDouble()
                    } else if (lhsValue is Float && rhsValue is Number) {
                        return lhsValue / rhsValue.toDouble()
                    }
                } else if (expr.operatorCode == "*") {
                    if (lhsValue is Int && rhsValue is Number) {
                        return lhsValue * rhsValue.toInt()
                    } else if (lhsValue is Long && rhsValue is Number) {
                        return lhsValue * rhsValue.toLong()
                    } else if (lhsValue is Short && rhsValue is Number) {
                        return lhsValue * rhsValue.toShort()
                    } else if (lhsValue is Byte && rhsValue is Number) {
                        return lhsValue * rhsValue.toByte()
                    } else if (lhsValue is Double && rhsValue is Number) {
                        return lhsValue * rhsValue.toDouble()
                    } else if (lhsValue is Float && rhsValue is Number) {
                        return lhsValue * rhsValue.toDouble()
                    }
                }

                return cannotEvaluate(expr, this)
            }
            // Casts are just a wrapper in this case, we are interested in the inner expression
            is CastExpression -> {
                return this.evaluate(expr.expression)
            }
            // For arrays, we check whether we can actually access the contents of the array. This
            // is basically the case if the base of the subscript expression is a
            // list of [KeyValueExpression]s.
            is ArraySubscriptionExpression -> {
                val array =
                    (expr.arrayExpression as? DeclaredReferenceExpression)?.refersTo as?
                        VariableDeclaration
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

                return cannotEvaluate(expr, this)
            }
            // While we are not handling different paths of variables with If statements, we can
            // easily be partly path-sensitive in a conditional expression
            is ConditionalExpression -> {
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
        }

        // At this point, we cannot evaluate, and we are calling our [cannotEvaluate] hook, maybe
        // this helps
        return cannotEvaluate(expr, this)
    }
}
