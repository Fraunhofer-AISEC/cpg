/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.golang

import de.fraunhofer.aisec.cpg.frontends.golang.GoStandardLibrary.Ast
import de.fraunhofer.aisec.cpg.frontends.golang.verified.LeanTranslator
import de.fraunhofer.aisec.cpg.frontends.golang.verified.PREDECLARED
import de.fraunhofer.aisec.cpg.frontends.golang.verified.Sexp
import de.fraunhofer.aisec.cpg.frontends.golang.verified.identifiersIn
import de.fraunhofer.aisec.cpg.frontends.golang.verified.isInVerifiedSubset
import de.fraunhofer.aisec.cpg.frontends.golang.verified.translationRequest
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.scopes.NameScope
import de.fraunhofer.aisec.cpg.graph.types.Type
import java.math.BigInteger
import org.slf4j.LoggerFactory

private val log =
    LoggerFactory.getLogger("de.fraunhofer.aisec.cpg.frontends.golang.VerifiedExpressions")

/**
 * Translates [node] with the verified translation of `cpg-verified`, if it is enabled in the
 * [GoFrontendConfiguration]. The context of the translation (name scope, imports, `iota`, shadowed
 * predeclared identifiers) is taken from the current state of the frontend.
 *
 * Returns `null` if the expression is not in the verified subset, in which case the regular handler
 * is responsible. Sub-expressions outside the subset are handled by the regular handler as well.
 */
internal fun ExpressionHandler.handleVerified(node: Ast.Expr): Expression? {
    val library = frontend.frontendConfiguration.verifiedTranslationLibrary ?: return null
    if (!isInVerifiedSubset(node)) return null

    val request =
        translationRequest(
            node,
            nameScope = (scope as? NameScope)?.name?.toString(),
            packages = visiblePackages,
            iota =
                frontend.declCtx.iotaValue.takeIf {
                    frontend.declCtx.currentDecl?.tok == ExpressionHandler.CONST_TOKEN
                },
            shadowed = PREDECLARED.intersect(identifiersIn(node)).filter(::isShadowed),
        )

    val result = LeanTranslator.translate(library, listOf(request)).single() as Sexp.SList
    return when (result.kind) {
        "problem" -> null
        "error" -> {
            log.warn("Verified translation failed: {}", result.items.getOrNull(1))
            null
        }
        else -> materialize(result, node)
    }
}

private val Sexp.SList.kind: String
    get() = items[0].toString()

/**
 * Creates the CPG nodes described by the translation [result] of [raw]. The result mirrors the
 * structure of [raw] (without parentheses), which provides the raw nodes for code and location.
 */
private fun ExpressionHandler.materialize(result: Sexp, raw: Ast.Expr): Expression {
    var node = raw
    while (node is Ast.ParenExpr) node = node.x

    result as Sexp.SList
    val items = result.items
    return when (result.kind) {
        "literal" -> {
            val value = items[3] as Sexp.SList
            val type = items[4] as Sexp.SList
            val name = (items[5] as Sexp.SList).items.firstOrNull()
            newLiteral(literalValue(value), materializeType(type), rawNode = node) { literal ->
                name?.let { literal.name = parseName(it.toString()) }
            }
        }
        "reference" -> newReference(parseName(items[3].toString()), rawNode = node)
        "binary" -> {
            node as Ast.BinaryExpr
            newBinaryOperator(items[3].toString(), rawNode = node) { op ->
                op.lhs = materialize(items[4], node.x)
                op.rhs = materialize(items[5], node.y)
            }
        }
        "unary" -> {
            node as Ast.UnaryExpr
            newUnaryOperator(
                items[3].toString(),
                postfix = false,
                prefix = false,
                rawNode = node,
            ) { op ->
                op.input = materialize(items[4], node.x)
            }
        }
        "call" -> {
            node as Ast.CallExpr
            val callee = materialize(items[3], node.`fun`)
            val call = newCall(callee, callee.name.localName, rawNode = node)
            call.type = unknownType()
            (items[4] as Sexp.SList).items.zip(node.args).forEach { (arg, rawArg) ->
                call.arguments += materialize(arg, rawArg)
            }
            call
        }
        "member" -> {
            node as Ast.SelectorExpr
            newMemberAccess(items[3].toString(), materialize(items[4], node.x), rawNode = node)
        }
        "membercall" -> {
            node as Ast.CallExpr
            val call = newMemberCall(materialize(items[3], node.`fun`), rawNode = node)
            call.type = unknownType()
            (items[4] as Sexp.SList).items.zip(node.args).forEach { (arg, rawArg) ->
                call.arguments += materialize(arg, rawArg)
            }
            call
        }
        "subscription" -> {
            newSubscription(rawNode = node) { subscription ->
                when (node) {
                    is Ast.IndexExpr -> {
                        subscription.arrayExpression = materialize(items[3], node.x)
                        subscription.subscriptExpression = materialize(items[4], node.index)
                    }
                    is Ast.SliceExpr -> {
                        subscription.arrayExpression = materialize(items[3], node.x)
                        subscription.subscriptExpression = materializeRange(items[4], node)
                    }
                    else -> error("unexpected raw node ${node.goType} for a subscription")
                }
            }
        }
        "deref" -> {
            node as Ast.StarExpr
            val input = materialize(items[3], node.x)
            newPointerDereference(input.name, unknownType(), rawNode = node).apply {
                this.input = input
            }
        }
        "cast" -> {
            val type = materializeType(items[3])
            when (node) {
                is Ast.CallExpr ->
                    newCast(rawNode = node) { cast ->
                        cast.castType = type
                        cast.expression = materialize(items[4], node.args[0])
                    }
                is Ast.TypeAssertExpr ->
                    newCast(rawNode = node) { cast ->
                        cast.expression = materialize(items[4], node.x)
                        cast.castType = type
                    }
                else -> error("unexpected raw node ${node.goType} for a cast")
            }
        }
        "new" -> {
            node as Ast.CallExpr
            newNew(rawNode = node) { new ->
                new.type = materializeType(items[3])
                new.initializer = materialize(items[4], node)
            }
        }
        "construction" -> {
            node as Ast.CallExpr
            newConstruction(rawNode = node).apply {
                // The first argument of make is the type
                (items[4] as Sexp.SList).items.zip(node.args.drop(1)).forEach { (arg, rawArg) ->
                    arguments += materialize(arg, rawArg)
                }
                type = materializeType(items[3])
            }
        }
        "arrayconstruction" -> {
            node as Ast.CallExpr
            newArrayConstruction(rawNode = node).apply {
                (items[4] as Sexp.SList).items.zip(node.args.drop(1)).forEach { (dim, rawDim) ->
                    addDimension(materialize(dim, rawDim))
                }
                type = materializeType(items[3])
            }
        }
        "initializerlist" -> {
            node as Ast.CompositeLit
            val type = materializeType(items[3])
            newInitializerList(type, rawNode = node) { list ->
                list.type = type
                list.initializers =
                    (items[4] as Sexp.SList)
                        .items
                        .zip(node.elts)
                        .map { (elt, rawElt) -> materialize(elt, rawElt) }
                        .toMutableList()
            }
        }
        "keyvalue" -> {
            node as Ast.KeyValueExpr
            newKeyValue(
                materialize(items[3], node.key),
                materialize(items[4], node.value),
                rawNode = node,
            )
        }
        // Outside the verified subset, so the regular handler is responsible
        else -> handle(node)
    }
}

/**
 * Creates the type described by [type] with the type builders, like `GoLanguageFrontend.typeOf`.
 */
private fun ExpressionHandler.materializeType(type: Sexp): Type {
    val items = (type as Sexp.SList).items
    return when (type.kind) {
        "primitive" -> primitiveType(items[1].toString())
        "object" ->
            objectType(
                parseName(items[1].toString()),
                (items[2] as Sexp.SList).items.map { materializeType(it) },
            )
        "pointer" -> materializeType(items[1]).pointer()
        "array" -> materializeType(items[1]).array()
        "resolved" ->
            frontend.typeManager.resolvePossibleTypedef(
                materializeType(items[1]),
                frontend.scopeManager,
            )
        else -> unknownType()
    }
}

/** Creates the range of a slice expression from its translation [result]. */
private fun ExpressionHandler.materializeRange(result: Sexp, raw: Ast.SliceExpr): Expression {
    val bounds = (result as Sexp.SList).items.drop(3).map { (it as Sexp.SList).items.firstOrNull() }
    return newRange(rawNode = raw) { range ->
        raw.low?.let { range.floor = materialize(bounds[0]!!, it) }
        raw.high?.let { range.ceiling = materialize(bounds[1]!!, it) }
        raw.max?.let { range.third = materialize(bounds[2]!!, it) }
    }
}

/** The Kotlin value of a literal, using the same integer representation as the regular handler. */
private fun literalValue(value: Sexp.SList): Any? =
    when (value.kind) {
        "int" -> {
            val i = BigInteger(value.items[1].toString())
            when {
                i > BigInteger.valueOf(Long.MAX_VALUE) -> i
                i.toLong() > Int.MAX_VALUE -> i.toLong()
                else -> i.toInt()
            }
        }
        "bool" -> value.items[1].toString().toBoolean()
        "str" -> value.items[1].toString()
        "float" -> value.items[1].toString().toDouble()
        else -> null
    }
