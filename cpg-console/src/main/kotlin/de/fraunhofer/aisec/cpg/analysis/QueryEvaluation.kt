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
import kotlin.reflect.full.allSupertypes
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
        SIZEOF,
        NOT,
        AND,
        OR,
        IS,
        IN
    }

    enum class Quantifier {
        FORALL,
        EXISTS
    }

    abstract class QueryExpression(open val representation: String?) {
        abstract fun evaluate(input: Map<String, Node> = mutableMapOf()): Any?
    }

    class NodesExpression(override val representation: String? = "") :
        QueryExpression(representation) {
        lateinit var nodeType: String
        lateinit var result: TranslationResult
        var kClass: Class<Node>? = null

        constructor(
            nodeType: String,
            result: TranslationResult,
            representation: String? = ""
        ) : this(representation) {
            this.nodeType = nodeType
            this.result = result
        }

        constructor(
            result: TranslationResult,
            representation: String? = ""
        ) : this(representation) {
            this.result = result
        }

        constructor(
            kClass: Class<Node>,
            result: TranslationResult,
            representation: String? = ""
        ) : this(representation) {
            this.result = result
            this.kClass = kClass
            this.nodeType = kClass.simpleName
        }

        override fun evaluate(input: Map<String, Node>): Any {
            if (kClass != null) {
                return SubgraphWalker.flattenAST(result).filter { n ->
                    n::class.allSupertypes.any { t -> t.javaClass == kClass } ||
                        n.javaClass == kClass ||
                        n.javaClass.interfaces.any { i -> i == kClass }
                }
            }
            return SubgraphWalker.flattenAST(result).filter { n ->
                n.javaClass.simpleName == nodeType ||
                    classNamesOfNode(n.javaClass).any { c -> c == nodeType }
            }
        }

        fun classNamesOfNode(jClass: Class<*>): Collection<String> {
            val result = mutableListOf<String>()
            if (jClass.superclass != null) {
                result.add(jClass.superclass.simpleName)
                result.addAll(classNamesOfNode(jClass.superclass))
            }
            result.addAll(jClass.interfaces.map { i -> i.simpleName })
            return result
        }
    }

    class QuantifierExpr(
        var result: TranslationResult? = null,
        override val representation: String? = ""
    ) : QueryExpression(representation) {
        var str: String? = null
            set(value) {
                variableName = value?.split(":")?.get(0)?.strip() ?: ""
                val varClass = value?.split(":")?.get(1)?.strip() ?: ""
                variables = NodesExpression(varClass, result!!)
                field = value
            }
        lateinit var quantifier: Quantifier
        lateinit var variables: QueryExpression
        lateinit var variableName: String
        lateinit var inner: QueryExpression

        constructor(
            quantifier: Quantifier,
            variables: QueryExpression,
            variableName: String,
            inner: QueryExpression,
            result: TranslationResult? = null,
            representation: String? = ""
        ) : this(result, representation) {
            this.quantifier = quantifier
            this.variables = variables
            this.variableName = variableName
            this.inner = inner
        }

        constructor(
            quantifier: Quantifier,
            result: TranslationResult? = null,
            representation: String? = ""
        ) : this(result, representation) {
            this.quantifier = quantifier
        }

        override fun evaluate(input: Map<String, Node>): Any {
            val newInput = input.toMutableMap()
            return if (quantifier == Quantifier.FORALL) {
                var result = true
                for (v in variables.evaluate(input) as Collection<Node>) {
                    newInput[variableName] = v
                    val temp = (inner.evaluate(newInput) as Boolean)
                    if (!temp) {
                        // TODO: Collect potential problems here
                        result = false
                    }
                }
                result
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

    class FieldAccessExpr(override val representation: String? = "") :
        QueryExpression(representation) {

        var str: String? = null
            set(value) {
                variableName = value?.split(".", limit = 2)?.get(0) ?: ""
                fieldSpecifier = value?.split(".", limit = 2)?.get(1) ?: ""
                field = value
            }
        lateinit var variableName: String
        lateinit var fieldSpecifier: String
        var evaluator: ValueEvaluator = ValueEvaluator()

        constructor(
            variableName: String,
            fieldSpecifier: String,
            evaluator: ValueEvaluator,
            representation: String? = ""
        ) : this(representation) {
            this.variableName = variableName
            this.fieldSpecifier = fieldSpecifier
            this.evaluator = evaluator
        }

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
                } else if (currentField is List<*>) {
                    // The query assumes a single value instead of a list. We just return the first
                    // element.
                    currentField = currentField[0]!!
                    // Ugly hack to get the property where the edge points to
                    currentField = readInstanceProperty(currentField, "end")
                }
            }

            return evaluator.evaluate(currentField)!!
        }

        private fun readInstanceProperty(instance: Any, propertyName: String): Any {
            val property =
                instance::class.members.first { it.name == propertyName } as KProperty1<Any, *>
            return property.apply { isAccessible = true }.get(instance)!!
        }
    }

    class ConstExpr(override val representation: String? = "") : QueryExpression(representation) {
        var value: Any? = null

        constructor(value: Any?, representation: String? = "") : this(representation) {
            this.value = value
        }

        override fun evaluate(input: Map<String, Node>): Any? {
            return value
        }
    }

    class UnaryExpr(override val representation: String? = "") : QueryExpression(representation) {
        lateinit var inner: QueryExpression
        lateinit var operator: QueryOp

        constructor(
            inner: QueryExpression,
            operator: QueryOp,
            representation: String? = ""
        ) : this(representation) {
            this.inner = inner
            this.operator = operator
        }

        constructor(operator: QueryOp, representation: String? = "") : this(representation) {
            this.operator = operator
        }

        override fun evaluate(input: Map<String, Node>): Any? {
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
                QueryOp.SIZEOF -> {
                    (inner as? FieldAccessExpr)?.evaluator = SizeEvaluator()
                    inner.evaluate(input)
                }
                else -> throw Exception("Unknown operation $operator on expression $inner")
            }
        }
    }

    class BinaryExpr(override var representation: String? = "") : QueryExpression(representation) {
        var lhs: QueryExpression? = null
        var rhs: QueryExpression? = null
        lateinit var operator: QueryOp

        constructor(
            lhs: QueryExpression,
            rhs: QueryExpression,
            operator: QueryOp,
            representation: String? = ""
        ) : this(representation) {
            this.lhs = lhs
            this.rhs = rhs
            this.operator = operator
        }

        constructor(operator: QueryOp, representation: String? = "") : this(representation) {
            this.operator = operator
        }

        override fun evaluate(input: Map<String, Node>): Boolean {
            return when (operator) {
                QueryOp.AND -> lhs?.evaluate(input) as Boolean && rhs?.evaluate(input) as Boolean
                QueryOp.OR -> lhs?.evaluate(input) as Boolean || rhs?.evaluate(input) as Boolean
                QueryOp.EQ -> lhs?.evaluate(input) == rhs?.evaluate(input)
                QueryOp.NE -> {
                    val lhsVal = lhs?.evaluate(input)
                    val rhsVal = rhs?.evaluate(input)
                    if (lhsVal is Collection<*>) {
                        lhsVal.all { l -> l != rhsVal }
                    } else {
                        lhsVal != rhsVal
                    }
                }
                QueryOp.GT ->
                    (lhs?.evaluate(input) as Number).compareTo(rhs?.evaluate(input) as Number) > 0
                QueryOp.GE ->
                    (lhs?.evaluate(input) as Number).compareTo(rhs?.evaluate(input) as Number) >= 0
                QueryOp.LT ->
                    (lhs?.evaluate(input) as Number).compareTo(rhs?.evaluate(input) as Number) < 0
                QueryOp.LE ->
                    (lhs?.evaluate(input) as Number).compareTo(rhs?.evaluate(input) as Number) <= 0
                QueryOp.IS -> {
                    val rhsVal = rhs?.evaluate(input)
                    if (rhsVal is String) {
                        lhs?.evaluate(input)?.javaClass?.simpleName == rhsVal
                    } else {
                        lhs?.evaluate(input)?.javaClass == rhsVal
                    }
                }
                QueryOp.IMPLIES ->
                    !(lhs?.evaluate(input) as Boolean) || rhs?.evaluate(input) as Boolean
                QueryOp.IN -> lhs?.evaluate(input) in (rhs?.evaluate(input) as Collection<*>)
                else -> false
            }
        }
    }
}
