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

import de.fraunhofer.aisec.cpg.frontends.golang.ExpressionHandler
import de.fraunhofer.aisec.cpg.frontends.golang.GoStandardLibrary.Ast

/**
 * Collects the translation requests for the verified translation (see
 * `CpgVerified/Wire/Codec.lean`) from a parsed Go file: every maximal expression that the Go
 * frontend translates as a value, together with the context the frontend would have at that point.
 */
class GoAstEncoder(private val file: Ast.File) {

    /** A translation request for the expression [expr]. */
    data class Request(val expr: Ast.Expr, val record: Sexp)

    val requests = mutableListOf<Request>()

    /** The names under which imported packages are visible. */
    private val packages =
        file.imports.map {
            it.name?.name
                ?: it.path.value
                    .removeSurrounding("\"")
                    .removeSurrounding("`")
                    .substringAfterLast('/')
        }

    /** The name scope at package level, i.e., the package name. */
    private val packageScope = file.name.name

    /**
     * The predeclared identifiers that are declared in each currently visible scope, innermost
     * last. Only these matter for the translation, so other declarations are not tracked.
     */
    private val scopes = ArrayDeque<MutableSet<String>>()

    fun encode(): GoAstEncoder {
        scoped {
            for (decl in file.decls) {
                when (decl) {
                    is Ast.GenDecl -> genDecl(decl, packageScope)
                    is Ast.FuncDecl -> {
                        // Methods are not declared in the package scope
                        if (decl.recv == null) declare(decl.name)
                        scoped {
                            decl.recv?.list?.forEach { it.names.forEach(::declare) }
                            decl.type.params.list.forEach { it.names.forEach(::declare) }
                            decl.type.results?.list?.forEach { it.names.forEach(::declare) }
                            stmt(decl.body)
                        }
                    }
                }
            }
        }
        return this
    }

    private fun scoped(block: () -> Unit) {
        scopes.addLast(mutableSetOf())
        block()
        scopes.removeLast()
    }

    private fun declare(ident: Ast.Ident) {
        if (ident.name in PREDECLARED) scopes.last() += ident.name
    }

    private fun genDecl(decl: Ast.GenDecl, nameScope: String?) {
        decl.specs.forEachIndexed { index, spec ->
            when (spec) {
                is Ast.ValueSpec -> {
                    val iota = if (decl.tok == ExpressionHandler.CONST_TOKEN) index else null
                    spec.values.forEach { emit(it, nameScope, iota) }
                    spec.names.forEach(::declare)
                }
                is Ast.TypeSpec -> declare(spec.name)
            }
        }
    }

    /**
     * Collects the value expressions of a statement inside a function, where there is no name
     * scope.
     */
    private fun stmt(stmt: Ast.Stmt?) {
        when (stmt) {
            null -> {}
            is Ast.BlockStmt -> scoped { stmt.list.forEach(::stmt) }
            is Ast.ExprStmt -> emit(stmt.x)
            is Ast.AssignStmt -> {
                stmt.rhs.forEach { emit(it) }
                // With := the left-hand side declares variables instead of referring to them
                if (stmt.tok != DEFINE_TOKEN) {
                    stmt.lhs.forEach { emit(it) }
                } else {
                    stmt.lhs.filterIsInstance<Ast.Ident>().forEach(::declare)
                }
            }
            is Ast.DeclStmt -> (stmt.decl as? Ast.GenDecl)?.let { genDecl(it, null) }
            is Ast.ReturnStmt -> stmt.results.forEach { emit(it) }
            is Ast.IfStmt ->
                scoped {
                    stmt(stmt.init)
                    emit(stmt.cond)
                    stmt(stmt.body)
                    stmt(stmt.`else`)
                }
            is Ast.ForStmt ->
                scoped {
                    stmt(stmt.init)
                    stmt.cond?.let { emit(it) }
                    stmt(stmt.post)
                    stmt(stmt.body)
                }
            is Ast.RangeStmt -> {
                emit(stmt.x)
                scoped {
                    if (stmt.tokString == ":=") {
                        listOfNotNull(stmt.key, stmt.value)
                            .filterIsInstance<Ast.Ident>()
                            .forEach(::declare)
                    }
                    stmt(stmt.body)
                }
            }
            is Ast.SwitchStmt ->
                scoped {
                    stmt(stmt.init)
                    stmt.tag?.let { emit(it) }
                    stmt(stmt.body)
                }
            is Ast.CaseClause -> {
                stmt.list.forEach { emit(it) }
                scoped { stmt.body.forEach(::stmt) }
            }
            is Ast.IncDecStmt -> emit(stmt.x)
            is Ast.SendStmt -> {
                emit(stmt.chan)
                emit(stmt.value)
            }
            is Ast.GoStmt -> emit(stmt.call)
            is Ast.DeferStmt -> emit(stmt.call)
            is Ast.LabeledStmt -> stmt(stmt.stmt)
            else -> {}
        }
    }

    private fun emit(expr: Ast.Expr, nameScope: String? = null, iota: Int? = null) {
        requests +=
            Request(
                expr,
                sexpOf(
                    atom("expr"),
                    sexpOf(listOfNotNull(nameScope?.let(::atom))),
                    sexpOf(packages.map(::atom)),
                    sexpOf(listOfNotNull(iota?.let(::atom))),
                    sexpOf(scopes.flatten().distinct().map(::atom)),
                    encode(expr),
                ),
            )
    }

    private fun encode(expr: Ast.Expr): Sexp {
        val span = arrayOf(atom(expr.pos), atom(expr.end))
        return when (expr) {
            is Ast.BasicLit ->
                sexpOf(atom("lit"), *span, atom(expr.kind.name.lowercase()), atom(expr.value))
            is Ast.Ident -> sexpOf(atom("ident"), *span, atom(expr.name))
            is Ast.BinaryExpr ->
                sexpOf(atom("binary"), *span, atom(expr.opString), encode(expr.x), encode(expr.y))
            is Ast.UnaryExpr -> sexpOf(atom("unary"), *span, atom(expr.opString), encode(expr.x))
            is Ast.ParenExpr -> sexpOf(atom("paren"), *span, encode(expr.x))
            is Ast.CallExpr ->
                sexpOf(atom("call"), *span, encode(expr.`fun`), sexpOf(expr.args.map(::encode)))
            else -> sexpOf(atom("unsupported"), *span, atom(expr.goType))
        }
    }

    companion object {
        /** The value of `token.DEFINE`, i.e., `:=`. */
        const val DEFINE_TOKEN = 47

        /** The predeclared identifiers that the translation treats specially. */
        val PREDECLARED = setOf("true", "false", "nil", "iota", "new", "make")
    }
}
