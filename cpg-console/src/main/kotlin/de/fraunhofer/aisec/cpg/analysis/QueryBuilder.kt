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

class QueryBuilder {
    // Quantifiers
    fun forall(block: QueryEvaluation.QuantifierExpr.() -> Unit): QueryEvaluation.QuantifierExpr =
        QueryEvaluation.QuantifierExpr(QueryEvaluation.Quantifier.FORALL).apply(block)

    fun forall(
        tr: TranslationResult,
        block: QueryEvaluation.QuantifierExpr.() -> Unit
    ): QueryEvaluation.QuantifierExpr =
        QueryEvaluation.QuantifierExpr(QueryEvaluation.Quantifier.FORALL, tr).apply(block)

    fun exists(block: QueryEvaluation.QuantifierExpr.() -> Unit): QueryEvaluation.QuantifierExpr =
        QueryEvaluation.QuantifierExpr(QueryEvaluation.Quantifier.EXISTS).apply(block)

    // Binary Operators
    fun and(block: QueryEvaluation.BinaryExpr.() -> Unit): QueryEvaluation.BinaryExpr =
        QueryEvaluation.BinaryExpr(QueryEvaluation.QueryOp.AND).apply(block)
    fun or(block: QueryEvaluation.BinaryExpr.() -> Unit): QueryEvaluation.BinaryExpr =
        QueryEvaluation.BinaryExpr(QueryEvaluation.QueryOp.OR).apply(block)
    fun eq(block: QueryEvaluation.BinaryExpr.() -> Unit): QueryEvaluation.BinaryExpr =
        QueryEvaluation.BinaryExpr(QueryEvaluation.QueryOp.EQ).apply(block)
    fun ne(block: QueryEvaluation.BinaryExpr.() -> Unit): QueryEvaluation.BinaryExpr =
        QueryEvaluation.BinaryExpr(QueryEvaluation.QueryOp.NE).apply(block)
    fun gt(block: QueryEvaluation.BinaryExpr.() -> Unit): QueryEvaluation.BinaryExpr =
        QueryEvaluation.BinaryExpr(QueryEvaluation.QueryOp.GT).apply(block)
    fun ge(block: QueryEvaluation.BinaryExpr.() -> Unit): QueryEvaluation.BinaryExpr =
        QueryEvaluation.BinaryExpr(QueryEvaluation.QueryOp.GE).apply(block)
    fun lt(block: QueryEvaluation.BinaryExpr.() -> Unit): QueryEvaluation.BinaryExpr =
        QueryEvaluation.BinaryExpr(QueryEvaluation.QueryOp.LT).apply(block)
    fun le(block: QueryEvaluation.BinaryExpr.() -> Unit): QueryEvaluation.BinaryExpr =
        QueryEvaluation.BinaryExpr(QueryEvaluation.QueryOp.LE).apply(block)
    fun IS(block: QueryEvaluation.BinaryExpr.() -> Unit): QueryEvaluation.BinaryExpr =
        QueryEvaluation.BinaryExpr(QueryEvaluation.QueryOp.IS).apply(block)
    fun implies(block: QueryEvaluation.BinaryExpr.() -> Unit): QueryEvaluation.BinaryExpr =
        QueryEvaluation.BinaryExpr(QueryEvaluation.QueryOp.IMPLIES).apply(block)
    fun IN(block: QueryEvaluation.BinaryExpr.() -> Unit): QueryEvaluation.BinaryExpr =
        QueryEvaluation.BinaryExpr(QueryEvaluation.QueryOp.IN).apply(block)

    // Unary Operators
    fun not(block: QueryEvaluation.UnaryExpr.() -> Unit): QueryEvaluation.UnaryExpr =
        QueryEvaluation.UnaryExpr(QueryEvaluation.QueryOp.NOT).apply(block)
    fun min(block: QueryEvaluation.UnaryExpr.() -> Unit): QueryEvaluation.UnaryExpr =
        QueryEvaluation.UnaryExpr(QueryEvaluation.QueryOp.MIN).apply(block)
    fun sizeof(block: QueryEvaluation.UnaryExpr.() -> Unit): QueryEvaluation.UnaryExpr =
        QueryEvaluation.UnaryExpr(QueryEvaluation.QueryOp.SIZEOF).apply(block)
    fun max(block: QueryEvaluation.UnaryExpr.() -> Unit): QueryEvaluation.UnaryExpr =
        QueryEvaluation.UnaryExpr(QueryEvaluation.QueryOp.MAX).apply(block)

    // Constant expression
    fun const(value: Any?): QueryEvaluation.ConstExpr {
        val constExpr = QueryEvaluation.ConstExpr()
        constExpr.value = value
        return constExpr
    }

    // Unary expressions
    fun sizeof(inner: QueryEvaluation.QueryExpression): QueryEvaluation.UnaryExpr {
        return QueryEvaluation.UnaryExpr(inner, QueryEvaluation.QueryOp.SIZEOF)
    }
    fun min(inner: QueryEvaluation.QueryExpression): QueryEvaluation.UnaryExpr {
        return QueryEvaluation.UnaryExpr(inner, QueryEvaluation.QueryOp.MIN)
    }
    fun max(inner: QueryEvaluation.QueryExpression): QueryEvaluation.UnaryExpr {
        return QueryEvaluation.UnaryExpr(inner, QueryEvaluation.QueryOp.MAX)
    }
    fun not(inner: QueryEvaluation.QueryExpression): QueryEvaluation.UnaryExpr {
        return QueryEvaluation.UnaryExpr(inner, QueryEvaluation.QueryOp.NOT)
    }

    // Nodes expression
    fun queryNodes(
        result: TranslationResult,
        block: QueryEvaluation.NodesExpression.() -> Unit
    ): QueryEvaluation.NodesExpression = QueryEvaluation.NodesExpression(result).apply(block)

    // access a field
    fun fieldAccess(
        block: QueryEvaluation.FieldAccessExpr.() -> Unit
    ): QueryEvaluation.FieldAccessExpr = QueryEvaluation.FieldAccessExpr().apply(block)
}

fun forall(
    translationResult: TranslationResult,
    block: QueryEvaluation.QuantifierExpr.() -> Unit
): QueryEvaluation.QuantifierExpr {
    return QueryBuilder().forall(translationResult, block)
}

fun forall(block: QueryEvaluation.QuantifierExpr.() -> Unit): QueryEvaluation.QuantifierExpr {
    return QueryBuilder().forall(block)
}

fun exists(block: QueryEvaluation.QuantifierExpr.() -> Unit): QueryEvaluation.QuantifierExpr {
    return QueryBuilder().exists(block)
}

// forall | exists (queryNodes): <not | and | or | eq | ne | gt | lt | ge | le | implies | is | in>
fun QueryEvaluation.QuantifierExpr.queryNodes(
    result: TranslationResult,
    block: QueryEvaluation.NodesExpression.() -> Unit
): QueryEvaluation.QuantifierExpr {
    variables = QueryBuilder().queryNodes(result, block)
    return this
}

fun QueryEvaluation.QuantifierExpr.field(
    block: QueryEvaluation.FieldAccessExpr.() -> Unit
): QueryEvaluation.QuantifierExpr {
    inner = QueryBuilder().fieldAccess(block)
    return this
}

fun QueryEvaluation.QuantifierExpr.not(
    block: QueryEvaluation.UnaryExpr.() -> Unit
): QueryEvaluation.QuantifierExpr {
    inner = QueryBuilder().not(block)
    return this
}

fun QueryEvaluation.QuantifierExpr.and(
    block: QueryEvaluation.BinaryExpr.() -> Unit
): QueryEvaluation.QuantifierExpr {
    inner = QueryBuilder().and(block)
    return this
}

fun QueryEvaluation.QuantifierExpr.or(
    block: QueryEvaluation.BinaryExpr.() -> Unit
): QueryEvaluation.QuantifierExpr {
    inner = QueryBuilder().or(block)
    return this
}

fun QueryEvaluation.QuantifierExpr.eq(
    block: QueryEvaluation.BinaryExpr.() -> Unit
): QueryEvaluation.QuantifierExpr {
    inner = QueryBuilder().eq(block)
    return this
}

fun QueryEvaluation.QuantifierExpr.ne(
    block: QueryEvaluation.BinaryExpr.() -> Unit
): QueryEvaluation.QuantifierExpr {
    inner = QueryBuilder().ne(block)
    return this
}

fun QueryEvaluation.QuantifierExpr.gt(
    block: QueryEvaluation.BinaryExpr.() -> Unit
): QueryEvaluation.QuantifierExpr {
    inner = QueryBuilder().gt(block)
    return this
}

fun QueryEvaluation.QuantifierExpr.ge(
    block: QueryEvaluation.BinaryExpr.() -> Unit
): QueryEvaluation.QuantifierExpr {
    inner = QueryBuilder().ge(block)
    return this
}

fun QueryEvaluation.QuantifierExpr.lt(
    block: QueryEvaluation.BinaryExpr.() -> Unit
): QueryEvaluation.QuantifierExpr {
    inner = QueryBuilder().lt(block)
    return this
}

fun QueryEvaluation.QuantifierExpr.le(
    block: QueryEvaluation.BinaryExpr.() -> Unit
): QueryEvaluation.QuantifierExpr {
    inner = QueryBuilder().le(block)
    return this
}

fun QueryEvaluation.QuantifierExpr.IS(
    block: QueryEvaluation.BinaryExpr.() -> Unit
): QueryEvaluation.QuantifierExpr {
    inner = QueryBuilder().IS(block)
    return this
}

fun QueryEvaluation.QuantifierExpr.implies(
    block: QueryEvaluation.BinaryExpr.() -> Unit
): QueryEvaluation.QuantifierExpr {
    inner = QueryBuilder().implies(block)
    return this
}

fun QueryEvaluation.QuantifierExpr.IN(
    block: QueryEvaluation.BinaryExpr.() -> Unit
): QueryEvaluation.QuantifierExpr {
    inner = QueryBuilder().IN(block)
    return this
}

fun const(value: Any?): QueryEvaluation.ConstExpr {
    return QueryBuilder().const(value)
}

fun sizeof(inner: QueryEvaluation.QueryExpression): QueryEvaluation.UnaryExpr {
    return QueryBuilder().sizeof(inner)
}

fun field(str: String, valueEvaluator: ValueEvaluator): QueryEvaluation.FieldAccessExpr {
    return QueryEvaluation.FieldAccessExpr(str, valueEvaluator)
}

fun field(str: String): QueryEvaluation.FieldAccessExpr {
    val res = QueryEvaluation.FieldAccessExpr()
    res.str = str
    return res
}

fun field(node: Node): QueryEvaluation.FieldAccessExpr {
    // TODO!!
    val res = QueryEvaluation.FieldAccessExpr()
    return res
}

fun forall(
    str: String,
    inner: QueryEvaluation.QueryExpression,
    result: TranslationResult
): QueryEvaluation.QuantifierExpr {
    val res = QueryEvaluation.QuantifierExpr()
    res.result = result
    res.quantifier = QueryEvaluation.Quantifier.FORALL
    res.str = str
    res.inner = inner
    return res
}

// <const | fieldAccess | not | and | or | eq | ne | gt | lt | ge | le | implies | is | in | forall
// | exists>
//      and | or | eq | ne | gt | lt | ge | le | implies | is | in
// <const | fieldAccess | not | and | or | eq | ne | gt | lt | ge | le | implies | is | in | forall
// | exists>
fun QueryEvaluation.BinaryExpr.const(value: Any?): QueryEvaluation.BinaryExpr {
    // Automagically pick lhs or rhs
    if (lhs == null) {
        lhs = QueryBuilder().const(value)
    } else {
        rhs = QueryBuilder().const(value)
    }
    return this
}

fun QueryEvaluation.BinaryExpr.field(str: String): QueryEvaluation.BinaryExpr {
    if (lhs == null) {
        lhs = QueryEvaluation.FieldAccessExpr()
        (lhs as QueryEvaluation.FieldAccessExpr).str = str
    } else {
        rhs = QueryEvaluation.FieldAccessExpr()
        (rhs as QueryEvaluation.FieldAccessExpr).str = str
    }
    return this
}

fun QueryEvaluation.BinaryExpr.field(
    block: QueryEvaluation.FieldAccessExpr.() -> Unit
): QueryEvaluation.BinaryExpr {
    if (lhs == null) {
        lhs = QueryBuilder().fieldAccess(block)
    } else {
        rhs = QueryBuilder().fieldAccess(block)
    }
    return this
}

fun QueryEvaluation.BinaryExpr.not(
    block: QueryEvaluation.UnaryExpr.() -> Unit
): QueryEvaluation.BinaryExpr {
    if (lhs == null) {
        lhs = QueryBuilder().not(block)
    } else {
        rhs = QueryBuilder().not(block)
    }
    return this
}

fun QueryEvaluation.BinaryExpr.max(
    block: QueryEvaluation.UnaryExpr.() -> Unit
): QueryEvaluation.BinaryExpr {
    if (lhs == null) {
        lhs = QueryBuilder().max(block)
    } else {
        rhs = QueryBuilder().max(block)
    }
    return this
}

fun QueryEvaluation.BinaryExpr.min(
    block: QueryEvaluation.UnaryExpr.() -> Unit
): QueryEvaluation.BinaryExpr {
    if (lhs == null) {
        lhs = QueryBuilder().min(block)
    } else {
        rhs = QueryBuilder().min(block)
    }
    return this
}

fun QueryEvaluation.BinaryExpr.sizeof(
    block: QueryEvaluation.UnaryExpr.() -> Unit
): QueryEvaluation.BinaryExpr {
    if (lhs == null) {
        lhs = QueryBuilder().sizeof(block)
    } else {
        rhs = QueryBuilder().sizeof(block)
    }
    return this
}

fun QueryEvaluation.BinaryExpr.and(
    block: QueryEvaluation.BinaryExpr.() -> Unit
): QueryEvaluation.BinaryExpr {
    if (lhs == null) {
        lhs = QueryBuilder().and(block)
    } else {
        rhs = QueryBuilder().and(block)
    }
    return this
}

fun QueryEvaluation.BinaryExpr.or(
    block: QueryEvaluation.BinaryExpr.() -> Unit
): QueryEvaluation.BinaryExpr {
    if (lhs == null) {
        lhs = QueryBuilder().or(block)
    } else {
        rhs = QueryBuilder().or(block)
    }
    return this
}

fun QueryEvaluation.BinaryExpr.eq(
    block: QueryEvaluation.BinaryExpr.() -> Unit
): QueryEvaluation.BinaryExpr {
    if (lhs == null) {
        lhs = QueryBuilder().eq(block)
    } else {
        rhs = QueryBuilder().eq(block)
    }
    return this
}

fun QueryEvaluation.BinaryExpr.ne(
    block: QueryEvaluation.BinaryExpr.() -> Unit
): QueryEvaluation.BinaryExpr {
    if (lhs == null) {
        lhs = QueryBuilder().ne(block)
    } else {
        rhs = QueryBuilder().ne(block)
    }
    return this
}

fun QueryEvaluation.BinaryExpr.gt(
    block: QueryEvaluation.BinaryExpr.() -> Unit
): QueryEvaluation.BinaryExpr {
    if (lhs == null) {
        lhs = QueryBuilder().gt(block)
    } else {
        rhs = QueryBuilder().gt(block)
    }
    return this
}

fun QueryEvaluation.BinaryExpr.ge(
    block: QueryEvaluation.BinaryExpr.() -> Unit
): QueryEvaluation.BinaryExpr {
    if (lhs == null) {
        lhs = QueryBuilder().ge(block)
    } else {
        rhs = QueryBuilder().ge(block)
    }
    return this
}

fun QueryEvaluation.BinaryExpr.lt(
    block: QueryEvaluation.BinaryExpr.() -> Unit
): QueryEvaluation.BinaryExpr {
    if (lhs == null) {
        lhs = QueryBuilder().lt(block)
    } else {
        rhs = QueryBuilder().lt(block)
    }
    return this
}

fun QueryEvaluation.BinaryExpr.le(
    block: QueryEvaluation.BinaryExpr.() -> Unit
): QueryEvaluation.BinaryExpr {
    if (lhs == null) {
        lhs = QueryBuilder().le(block)
    } else {
        rhs = QueryBuilder().le(block)
    }
    return this
}

fun QueryEvaluation.BinaryExpr.IS(
    block: QueryEvaluation.BinaryExpr.() -> Unit
): QueryEvaluation.BinaryExpr {
    if (lhs == null) {
        lhs = QueryBuilder().IS(block)
    } else {
        rhs = QueryBuilder().IS(block)
    }
    return this
}

infix fun QueryEvaluation.BinaryExpr.implies(
    block: QueryEvaluation.BinaryExpr.() -> Unit
): QueryEvaluation.BinaryExpr {
    if (lhs == null) {
        lhs = QueryBuilder().implies(block)
    } else {
        rhs = QueryBuilder().implies(block)
    }
    return this
}

fun QueryEvaluation.BinaryExpr.IN(
    block: QueryEvaluation.BinaryExpr.() -> Unit
): QueryEvaluation.BinaryExpr {
    if (lhs == null) {
        lhs = QueryBuilder().IN(block)
    } else {
        rhs = QueryBuilder().IN(block)
    }
    return this
}

fun QueryEvaluation.BinaryExpr.forall(
    block: QueryEvaluation.QuantifierExpr.() -> Unit
): QueryEvaluation.BinaryExpr {
    if (lhs == null) {
        lhs = QueryBuilder().forall(block)
    } else {
        rhs = QueryBuilder().forall(block)
    }
    return this
}

fun QueryEvaluation.BinaryExpr.exists(
    block: QueryEvaluation.QuantifierExpr.() -> Unit
): QueryEvaluation.BinaryExpr {
    if (lhs == null) {
        lhs = QueryBuilder().exists(block)
    } else {
        rhs = QueryBuilder().exists(block)
    }
    return this
}

//      not | min | max | sizeof
// <const | fieldAccess | not | and | or | eq | ne | gt | lt | ge | le | implies | is | in | forall
// | exists>
fun QueryEvaluation.UnaryExpr.const(value: Any): QueryEvaluation.UnaryExpr {
    inner = QueryBuilder().const(value)
    return this
}

fun QueryEvaluation.UnaryExpr.field(
    block: QueryEvaluation.FieldAccessExpr.() -> Unit
): QueryEvaluation.UnaryExpr {
    inner = QueryBuilder().fieldAccess(block)
    return this
}

fun QueryEvaluation.UnaryExpr.not(
    block: QueryEvaluation.UnaryExpr.() -> Unit
): QueryEvaluation.UnaryExpr {
    inner = QueryBuilder().not(block)
    return this
}

fun QueryEvaluation.UnaryExpr.max(
    block: QueryEvaluation.UnaryExpr.() -> Unit
): QueryEvaluation.UnaryExpr {
    inner = QueryBuilder().max(block)
    return this
}

fun QueryEvaluation.UnaryExpr.min(
    block: QueryEvaluation.UnaryExpr.() -> Unit
): QueryEvaluation.UnaryExpr {
    inner = QueryBuilder().min(block)
    return this
}

fun QueryEvaluation.UnaryExpr.sizeof(
    block: QueryEvaluation.UnaryExpr.() -> Unit
): QueryEvaluation.UnaryExpr {
    inner = QueryBuilder().sizeof(block)
    return this
}

fun QueryEvaluation.UnaryExpr.and(
    block: QueryEvaluation.BinaryExpr.() -> Unit
): QueryEvaluation.UnaryExpr {
    inner = QueryBuilder().and(block)
    return this
}

fun QueryEvaluation.UnaryExpr.or(
    block: QueryEvaluation.BinaryExpr.() -> Unit
): QueryEvaluation.UnaryExpr {
    inner = QueryBuilder().or(block)
    return this
}

fun QueryEvaluation.UnaryExpr.eq(
    block: QueryEvaluation.BinaryExpr.() -> Unit
): QueryEvaluation.UnaryExpr {
    inner = QueryBuilder().eq(block)
    return this
}

fun QueryEvaluation.UnaryExpr.ne(
    block: QueryEvaluation.BinaryExpr.() -> Unit
): QueryEvaluation.UnaryExpr {
    inner = QueryBuilder().ne(block)
    return this
}

fun QueryEvaluation.UnaryExpr.gt(
    block: QueryEvaluation.BinaryExpr.() -> Unit
): QueryEvaluation.UnaryExpr {
    inner = QueryBuilder().gt(block)
    return this
}

fun QueryEvaluation.UnaryExpr.ge(
    block: QueryEvaluation.BinaryExpr.() -> Unit
): QueryEvaluation.UnaryExpr {
    inner = QueryBuilder().ge(block)
    return this
}

fun QueryEvaluation.UnaryExpr.lt(
    block: QueryEvaluation.BinaryExpr.() -> Unit
): QueryEvaluation.UnaryExpr {
    inner = QueryBuilder().lt(block)
    return this
}

fun QueryEvaluation.UnaryExpr.le(
    block: QueryEvaluation.BinaryExpr.() -> Unit
): QueryEvaluation.UnaryExpr {
    inner = QueryBuilder().le(block)
    return this
}

fun QueryEvaluation.UnaryExpr.IS(
    block: QueryEvaluation.BinaryExpr.() -> Unit
): QueryEvaluation.UnaryExpr {
    inner = QueryBuilder().IS(block)
    return this
}

fun QueryEvaluation.UnaryExpr.implies(
    block: QueryEvaluation.BinaryExpr.() -> Unit
): QueryEvaluation.UnaryExpr {
    inner = QueryBuilder().implies(block)
    return this
}

fun QueryEvaluation.UnaryExpr.IN(
    block: QueryEvaluation.BinaryExpr.() -> Unit
): QueryEvaluation.UnaryExpr {
    inner = QueryBuilder().IN(block)
    return this
}

fun QueryEvaluation.UnaryExpr.forall(
    block: QueryEvaluation.QuantifierExpr.() -> Unit
): QueryEvaluation.UnaryExpr {
    inner = QueryBuilder().forall(block)
    return this
}

fun QueryEvaluation.UnaryExpr.exists(
    block: QueryEvaluation.QuantifierExpr.() -> Unit
): QueryEvaluation.UnaryExpr {
    inner = QueryBuilder().exists(block)
    return this
}
