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
package de.fraunhofer.aisec.cpg.frontends.golang.verified

import de.fraunhofer.aisec.cpg.frontends.golang.GoStandardLibrary.Ast

/** The predeclared identifiers that the verified translation treats specially. */
val PREDECLARED = setOf("true", "false", "nil", "iota", "new", "make")

/**
 * Creates a translation request (see `CpgVerified/Wire/Codec.lean`) for [expr] in the given
 * context.
 *
 * @param nameScope the name of the current scope, if it is a name scope
 * @param packages the names under which imported packages are visible
 * @param iota the value of `iota`, if we are inside a constant declaration
 * @param shadowed the [PREDECLARED] identifiers that are shadowed by a visible declaration
 */
fun translationRequest(
    expr: Ast.Expr,
    nameScope: String?,
    packages: List<String>,
    iota: Int?,
    shadowed: Collection<String>,
): Sexp =
    sexpOf(
        atom("expr"),
        sexpOf(listOfNotNull(nameScope?.let(::atom))),
        sexpOf(packages.map(::atom)),
        sexpOf(listOfNotNull(iota?.let(::atom))),
        sexpOf(shadowed.map(::atom)),
        encodeExpr(expr),
    )

/** Encodes a Go expression. Expressions outside the verified subset are encoded as unsupported. */
fun encodeExpr(expr: Ast.Expr): Sexp {
    val span = arrayOf(atom(expr.pos), atom(expr.end))
    return when (expr) {
        is Ast.BasicLit ->
            sexpOf(atom("lit"), *span, atom(expr.kind.name.lowercase()), atom(expr.value))
        is Ast.Ident -> sexpOf(atom("ident"), *span, atom(expr.name))
        is Ast.BinaryExpr ->
            sexpOf(
                atom("binary"),
                *span,
                atom(expr.opString),
                encodeExpr(expr.x),
                encodeExpr(expr.y),
            )
        is Ast.UnaryExpr -> sexpOf(atom("unary"), *span, atom(expr.opString), encodeExpr(expr.x))
        is Ast.ParenExpr -> sexpOf(atom("paren"), *span, encodeExpr(expr.x))
        is Ast.CallExpr ->
            sexpOf(atom("call"), *span, encodeExpr(expr.`fun`), sexpOf(expr.args.map(::encodeExpr)))
        is Ast.SelectorExpr ->
            sexpOf(atom("selector"), *span, encodeExpr(expr.x), atom(expr.sel.name))
        else -> sexpOf(atom("unsupported"), *span, atom(expr.goType))
    }
}

/** Whether [expr] is of a kind that the verified translation can handle at all. */
fun isInVerifiedSubset(expr: Ast.Expr): Boolean =
    expr is Ast.BasicLit ||
        expr is Ast.Ident ||
        expr is Ast.BinaryExpr ||
        expr is Ast.UnaryExpr ||
        expr is Ast.ParenExpr ||
        expr is Ast.CallExpr ||
        expr is Ast.SelectorExpr

/** The identifiers occurring in [expr], not descending into expressions outside the subset. */
fun identifiersIn(expr: Ast.Expr): Set<String> =
    when (expr) {
        is Ast.Ident -> setOf(expr.name)
        is Ast.BinaryExpr -> identifiersIn(expr.x) + identifiersIn(expr.y)
        is Ast.UnaryExpr -> identifiersIn(expr.x)
        is Ast.ParenExpr -> identifiersIn(expr.x)
        is Ast.CallExpr -> identifiersIn(expr.`fun`) + expr.args.flatMap(::identifiersIn)
        is Ast.SelectorExpr -> identifiersIn(expr.x)
        else -> emptySet()
    }
