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

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.ValueEvaluator
import de.fraunhofer.aisec.cpg.graph.compareTo
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.isAccessible

class QueryEvaluation {

    enum class QueryOp {
        GT,
        GE,
        LT,
        LE,
        EQ,
        NE,
        IMPLIES,
        MAX,
        MIN,
        NOT,
        AND,
        OR,
        IS
    }

    enum class Quantifier {
        FORALL,
        EXISTS
    }

    abstract class QueryExpression(open val representation: String) {
        abstract fun evaluate(input: Map<String, Node> = mutableMapOf()): Any
    }

    class NodesExpression(
        override val representation: String,
        val name: String,
        private val nodeType: String,
        private val result: TranslationResult
    ) : QueryExpression(representation) {
        override fun evaluate(input: Map<String, Node>): Any {
            return SubgraphWalker.flattenAST(result).filter { n ->
                n.javaClass.simpleName == nodeType
            }
        }
    }

    class QuantifierExpr(
        override val representation: String,
        private val quantifier: Quantifier,
        private val variables: QueryExpression,
        private val variableName: String,
        private val inner: QueryExpression
    ) : QueryExpression(representation) {
        override fun evaluate(input: Map<String, Node>): Any {
            val newInput = input.toMutableMap()
            return if (quantifier == Quantifier.FORALL) {
                (variables.evaluate(input) as Collection<Node>).all { v ->
                    newInput[variableName] = v
                    inner.evaluate(newInput) as Boolean
                }
            } else if (quantifier == Quantifier.EXISTS) {
                (variables.evaluate(input) as Collection<Node>).any { v ->
                    newInput[variableName] = v
                    inner.evaluate(newInput) as Boolean
                }
            } else {
                false
            }
        }
    }

    class FieldAccessExpr(
        override val representation: String,
        private val variableName: String,
        private val fieldSpecifier: String,
        private val evaluator: ValueEvaluator
    ) : QueryExpression(representation) {
        override fun evaluate(input: Map<String, Node>): Any {
            var currentField: Any = input[variableName]!!
            for (fs in fieldSpecifier.split(".")) {
                val arrayIndex =
                    if ("[" !in fs) {
                        -1
                    } else {
                        fs.split("[")[1].dropLast(1).toInt()
                    }
                val fieldName = if (arrayIndex > -1) fs.split("[")[0] else fs
                currentField = readInstanceProperty(currentField, fieldName)
                if (arrayIndex != -1 && currentField is Array<*>) {
                    currentField = currentField[arrayIndex]!!
                } else if (arrayIndex != -1 && currentField is List<*>) {
                    currentField = currentField[arrayIndex]!!
                    // Ugly hack to get the property where the edge points to
                    currentField = readInstanceProperty(currentField, "end")
                }
            }
            return evaluator.evaluate(currentField as Node)!!
        }

        private fun readInstanceProperty(instance: Any, propertyName: String): Any {
            val property =
                instance::class.members.first { it.name == propertyName } as KProperty1<Any, *>
            return property.apply { isAccessible = true }.get(instance)!!
        }
    }

    class ConstExpr(override val representation: String, private val value: Any) :
        QueryExpression(representation) {
        override fun evaluate(input: Map<String, Node>): Any {
            return value
        }
    }

    class UnaryExpr(
        override val representation: String,
        private val inner: QueryExpression,
        private val operator: QueryOp
    ) : QueryExpression(representation) {
        override fun evaluate(input: Map<String, Node>): Any {
            return when (operator) {
                QueryOp.NOT -> !(inner.evaluate(input) as Boolean)
                QueryOp.MAX -> {
                    val result = inner.evaluate(input)
                    if (result is Number) {
                        result.toLong()
                    } else {
                        (result as NumberSet).max()
                    }
                }
                QueryOp.MIN -> {
                    val result = inner.evaluate(input)
                    if (result is Number) {
                        result.toLong()
                    } else {
                        (result as NumberSet).min()
                    }
                }
                else -> throw Exception("Unknown operation $operator on expression $inner")
            }
        }
    }

    class BinaryExpr(
        override val representation: String,
        private val lhs: QueryExpression,
        private val rhs: QueryExpression,
        private val operator: QueryOp
    ) : QueryExpression(representation) {
        override fun evaluate(input: Map<String, Node>): Boolean {
            return when (operator) {
                QueryOp.AND -> lhs.evaluate(input) as Boolean && rhs.evaluate(input) as Boolean
                QueryOp.OR -> lhs.evaluate(input) as Boolean || rhs.evaluate(input) as Boolean
                QueryOp.EQ -> lhs.evaluate(input) == rhs.evaluate(input)
                QueryOp.NE -> lhs.evaluate(input) != rhs.evaluate(input)
                QueryOp.GT ->
                    (lhs.evaluate(input) as Number).compareTo(rhs.evaluate(input) as Number) > 0
                QueryOp.GE ->
                    (lhs.evaluate(input) as Number).compareTo(rhs.evaluate(input) as Number) >= 0
                QueryOp.LT ->
                    (lhs.evaluate(input) as Number).compareTo(rhs.evaluate(input) as Number) < 0
                QueryOp.LE ->
                    (lhs.evaluate(input) as Number).compareTo(rhs.evaluate(input) as Number) <= 0
                QueryOp.IS ->
                    lhs.evaluate(input).javaClass.simpleName == rhs.evaluate(input) as String
                QueryOp.IMPLIES ->
                    !(lhs.evaluate(input) as Boolean) || rhs.evaluate(input) as Boolean
                else -> false
            }
        }
    }
}
