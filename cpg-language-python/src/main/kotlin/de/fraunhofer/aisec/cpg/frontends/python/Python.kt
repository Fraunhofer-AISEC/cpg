/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.python

import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import jep.python.PyObject

/**
 * This interface encapsulates Python <-> Kotlin translation objects. It consists mainly, of
 * translation objects from Python's `ast` class (see [Python.AST]), but other Python classes (like
 * `complex`, ...) can be included, too.
 */
interface Python {

    /**
     * This is an abstract class that is common to all our python objects. Represents python's
     * `object`.
     */
    abstract class BaseObject(var pyObject: PyObject)

    /** The `ellipsis` class. */
    class Ellipsis(pyObject: PyObject) : BaseObject(pyObject)

    /** The `complex` class. */
    class Complex(pyObject: PyObject) : BaseObject(pyObject)

    /**
     * This interface makes Python's `ast` nodes accessible to Kotlin. It does not contain any
     * complex logic but rather aims at making all Python `ast` properties accessible to Kotlin
     * (under the same name as in Python).
     *
     * Python's AST object are mapped as close as possible to the original. Exceptions:
     * - `identifier` fields are mapped as Kotlin [String]s
     * - Python's `int` is mapped to [Int]
     * - Constants are mapped as [Any] (thus Jep's conversion to Java makes the translation)
     */
    interface AST {

        /**
         * Represents a `ast.AST` node as returned by Python's `ast` parser.
         *
         * @param pyObject The Python object returned by jep.
         */
        interface AST {
            var pyObject: PyObject
        }

        /**
         * Some nodes, such as `ast.stmt` [AST.BaseStmt] and `ast.expr` [AST.BaseExpr] nodes have
         * extra location properties as implemented here.
         */
        interface WithLocation : AST { // TODO make the fields accessible `by lazy`
            /** Maps to the `lineno` filed from Python's ast. */
            val lineno: Int
                get() {
                    return try {
                        (pyObject.getAttr("lineno") as? Long)?.toInt() ?: 0
                    } catch (e: Exception) {
                        0
                    }
                }

            /** Maps to the `col_offset` filed from Python's ast. */
            val col_offset: Int
                get() {
                    return try {
                        (pyObject.getAttr("col_offset") as? Long)?.toInt() ?: 0
                    } catch (e: Exception) {
                        0
                    }
                }

            /** Maps to the `end_lineno` filed from Python's ast. */
            val end_lineno: Int
                get() {
                    return try {
                        (pyObject.getAttr("end_lineno") as? Long)?.toInt() ?: 0
                    } catch (e: Exception) {
                        0
                    }
                }

            /** Maps to the `end_col_offset` filed from Python's ast. */
            val end_col_offset: Int
                get() {
                    return try {
                        (pyObject.getAttr("end_col_offset") as? Long)?.toInt() ?: 0
                    } catch (e: Exception) {
                        0
                    }
                }
        }

        /**
         * Python does not really have "declarations", but it has "definitions". Instead of having
         * their own AST class, they are also [AST.BaseStmt]s. In order to be compatible with the
         * remaining languages we need to ensure that elements such as functions or classes, still
         * turn out to be [Declaration]s, not [Statement]s
         *
         * This interface should be attached to all such statements that we consider to be
         * definitions, and thus [Declaration]s.
         */
        sealed interface Def : AST

        /**
         * ```
         * ast.mod = class mod(AST)
         *  |  mod = Module(stmt* body, type_ignore* type_ignores)
         *  |  | Interactive(stmt* body)
         *  |  | Expression(expr body)
         *  |  | FunctionType(expr* argtypes, expr returns)
         * ```
         *
         * Note: We currently only support `Module`s.
         */
        abstract class BaseMod(pyObject: PyObject) : AST, BaseObject(pyObject)

        /**
         * ```
         * ast.Module = class Module(mod)
         *  |  Module(stmt* body, type_ignore* type_ignores)
         * ```
         */
        class Module(pyObject: PyObject) : AST, BaseObject(pyObject) {
            val body: kotlin.collections.List<BaseStmt> by lazy { "body" of pyObject }

            val type_ignores: kotlin.collections.List<type_ignore> by lazy {
                "type_ignores" of pyObject
            }
        }

        /**
         * ```
         * ast.stmt = class stmt(AST)
         *  |  stmt = FunctionDef(identifier name, arguments args, stmt* body, expr* decorator_list, expr? returns, string? type_comment)
         *  |  | AsyncFunctionDef(identifier name, arguments args, stmt* body, expr* decorator_list, expr? returns, string? type_comment)
         *  |  | ClassDef(identifier name, expr* bases, keyword* keywords, stmt* body, expr* decorator_list)
         *  |  | Return(expr? value)
         *  |  | Delete(expr* targets)
         *  |  | Assign(expr* targets, expr value, string? type_comment)
         *  |  | AugAssign(expr target, operator op, expr value)
         *  |  | AnnAssign(expr target, expr annotation, expr? value, int simple)
         *  |  | For(expr target, expr iter, stmt* body, stmt* orelse, string? type_comment)
         *  |  | AsyncFor(expr target, expr iter, stmt* body, stmt* orelse, string? type_comment)
         *  |  | While(expr test, stmt* body, stmt* orelse)
         *  |  | If(expr test, stmt* body, stmt* orelse)
         *  |  | With(withitem* items, stmt* body, string? type_comment)
         *  |  | AsyncWith(withitem* items, stmt* body, string? type_comment)
         *  |  | Match(expr subject, match_case* cases)
         *  |  | Raise(expr? exc, expr? cause)
         *  |  | Try(stmt* body, excepthandler* handlers, stmt* orelse, stmt* finalbody)
         *  |  | TryStar(stmt* body, excepthandler* handlers, stmt* orelse, stmt* finalbody)
         *  |  | Assert(expr test, expr? msg)
         *  |  | Import(alias* names)
         *  |  | ImportFrom(identifier? module, alias* names, int? level)
         *  |  | Global(identifier* names)
         *  |  | Nonlocal(identifier* names)
         *  |  | Expr(expr value)
         *  |  | Pass
         *  |  | Break
         *  |  | Continue
         * ```
         */
        sealed class BaseStmt(pyObject: PyObject) : AST, BaseObject(pyObject), WithLocation

        /**
         * Several classes are duplicated in the python AST for async and non-async variants. This
         * interface is a common interface for those AST classes.
         */
        sealed interface AsyncOrNot : WithLocation

        /** This interface denotes that this is an "async" node. */
        sealed interface IsAsync : AsyncOrNot

        /**
         * ast.FunctionDef and ast.AsyncFunctionDef are not related according to the Python syntax.
         * However, they are so similar, that we make use of this interface to avoid a lot of
         * duplicate code.
         */
        sealed interface NormalOrAsyncFunctionDef : AsyncOrNot, Def {
            val name: String
            val args: arguments
            val body: kotlin.collections.List<BaseStmt>
            val decorator_list: kotlin.collections.List<BaseExpr>
            val returns: BaseExpr?
            val type_comment: String?
        }

        /**
         * ```
         * ast.FunctionDef = class FunctionDef(stmt)
         *  |  FunctionDef(identifier name, arguments args, stmt* body, expr* decorator_list, expr? returns, string? type_comment)
         * ```
         */
        class FunctionDef(pyObject: PyObject) : BaseStmt(pyObject), NormalOrAsyncFunctionDef {
            override val name: String by lazy { "name" of pyObject }

            override val args: arguments by lazy { "args" of pyObject }

            override val body: kotlin.collections.List<BaseStmt> by lazy { "body" of pyObject }

            override val decorator_list: kotlin.collections.List<BaseExpr> by lazy {
                "decorator_list" of pyObject
            }

            override val returns: BaseExpr? by lazy { "returns" of pyObject }

            override val type_comment: String? by lazy { "type_comment" of pyObject }
        }

        /**
         * ```
         * ast.AsyncFunctionDef = class AsyncFunctionDef(stmt)
         *  |  AsyncFunctionDef(identifier name, arguments args, stmt* body, expr* decorator_list, expr? returns, string? type_comment)
         * ```
         */
        class AsyncFunctionDef(pyObject: PyObject) :
            BaseStmt(pyObject), NormalOrAsyncFunctionDef, IsAsync {
            override val name: String by lazy { "name" of pyObject }

            override val args: arguments by lazy { "args" of pyObject }

            override val body: kotlin.collections.List<BaseStmt> by lazy { "body" of pyObject }

            override val decorator_list: kotlin.collections.List<BaseExpr> by lazy {
                "decorator_list" of pyObject
            }

            override val returns: BaseExpr? by lazy { "returns" of pyObject }

            override val type_comment: String? by lazy { "type_comment" of pyObject }
        }

        /**
         * ```
         * ast.ClassDef = class ClassDef(stmt)
         *  |  ClassDef(identifier name, expr* bases, keyword* keywords, stmt* body, expr* decorator_list)
         * ```
         */
        class ClassDef(pyObject: PyObject) : BaseStmt(pyObject), Def {
            val name: String by lazy { "name" of pyObject }

            val bases: kotlin.collections.List<BaseExpr> by lazy { "bases" of pyObject }

            val keywords: kotlin.collections.List<keyword> by lazy { "keywords" of pyObject }

            val body: kotlin.collections.List<BaseStmt> by lazy { "body" of pyObject }

            val decorator_list: kotlin.collections.List<BaseExpr> by lazy {
                "decorator_list" of pyObject
            }
        }

        /**
         * ```
         * ast.Return = class Return(stmt)
         *  |  Return(expr? value)
         * ```
         */
        class Return(pyObject: PyObject) : BaseStmt(pyObject) {
            val value: BaseExpr? by lazy { "value" of pyObject }
        }

        /**
         * ```
         * ast.Delete = class Delete(stmt)
         *  |  Delete(expr* targets)
         * ```
         */
        class Delete(pyObject: PyObject) : BaseStmt(pyObject) {
            val targets: kotlin.collections.List<BaseExpr> by lazy { "targets" of pyObject }
        }

        /**
         * ```
         * ast.Assign = class Assign(stmt)
         *  |  Assign(expr* targets, expr value, string? type_comment)
         * ```
         */
        class Assign(pyObject: PyObject) : BaseStmt(pyObject) {
            val targets: kotlin.collections.List<BaseExpr> by lazy { "targets" of pyObject }

            val value: BaseExpr by lazy { "value" of pyObject }

            val type_comment: String? by lazy { "type_comment" of pyObject }
        }

        /**
         * ```
         * ast.AugAssign = class AugAssign(stmt)
         *  |  AugAssign(expr target, operator op, expr value)
         * ```
         */
        class AugAssign(pyObject: PyObject) : BaseStmt(pyObject) {
            val target: BaseExpr by lazy { "target" of pyObject }
            val op: BaseOperator by lazy { "op" of pyObject }
            val value: BaseExpr by lazy { "value" of pyObject }
        }

        /**
         * ```
         * ast.AnnAssign = class AnnAssign(stmt)
         *  |  AnnAssign(expr target, expr annotation, expr? value, int simple)
         * ```
         */
        class AnnAssign(pyObject: PyObject) : BaseStmt(pyObject) {
            val target: BaseExpr by lazy { "target" of pyObject }
            val annotation: BaseExpr by lazy { "annotation" of pyObject }
            val value: BaseExpr? by lazy { "value" of pyObject }
            val simple: Long by lazy { "simple" of pyObject }
        }

        /**
         * ast.For and ast.AsyncFor are not related according to the Python syntax. However, they
         * are so similar, that we make use of this interface to avoid a lot of duplicate code.
         */
        interface NormalOrAsyncFor : AsyncOrNot {
            val target: BaseExpr
            val iter: BaseExpr
            val body: kotlin.collections.List<BaseStmt>
            val orelse: kotlin.collections.List<BaseStmt>
            val type_comment: String?
        }

        /**
         * ```
         * ast.For = class For(stmt)
         *  |  For(expr target, expr iter, stmt* body, stmt* orelse, string? type_comment)
         * ```
         */
        class For(pyObject: PyObject) : BaseStmt(pyObject), NormalOrAsyncFor {
            override val target: BaseExpr by lazy { "target" of pyObject }
            override val iter: BaseExpr by lazy { "iter" of pyObject }
            override val body: kotlin.collections.List<BaseStmt> by lazy { "body" of pyObject }
            override val orelse: kotlin.collections.List<BaseStmt> by lazy { "orelse" of pyObject }
            override val type_comment: String? by lazy { "type_comment" of pyObject }
        }

        /**
         * ```
         * ast.AsyncFor = class AsyncFor(stmt)
         *  |  AsyncFor(expr target, expr iter, stmt* body, stmt* orelse, string? type_comment)
         * ```
         */
        class AsyncFor(pyObject: PyObject) : BaseStmt(pyObject), NormalOrAsyncFor, IsAsync {
            override val target: BaseExpr by lazy { "target" of pyObject }
            override val iter: BaseExpr by lazy { "iter" of pyObject }
            override val body: kotlin.collections.List<BaseStmt> by lazy { "body" of pyObject }
            override val orelse: kotlin.collections.List<BaseStmt> by lazy { "orelse" of pyObject }
            override val type_comment: String? by lazy { "type_comment" of pyObject }
        }

        /**
         * ```
         * ast.While = class While(stmt)
         *  |  While(expr test, stmt* body, stmt* orelse)
         * ```
         */
        class While(pyObject: PyObject) : BaseStmt(pyObject) {
            val test: BaseExpr by lazy { "test" of pyObject }
            val body: kotlin.collections.List<BaseStmt> by lazy { "body" of pyObject }
            val orelse: kotlin.collections.List<BaseStmt> by lazy { "orelse" of pyObject }
        }

        /**
         * ```
         * ast.If = class If(stmt)
         *  |  If(expr test, stmt* body, stmt* orelse)
         * ```
         */
        class If(pyObject: PyObject) : BaseStmt(pyObject) {
            val test: BaseExpr by lazy { "test" of pyObject }
            val body: kotlin.collections.List<BaseStmt> by lazy { "body" of pyObject }
            val orelse: kotlin.collections.List<BaseStmt> by lazy { "orelse" of pyObject }
        }

        /**
         * ast.With and ast.AsyncWith are not related according to the Python syntax. However, they
         * are so similar, that we make use of this interface to avoid a lot of duplicate code.
         */
        interface NormalOrAsyncWith : AsyncOrNot {
            val items: kotlin.collections.List<withitem>
            val body: kotlin.collections.List<BaseStmt>
            val type_comment: String?
        }

        /**
         * ```
         * ast.With = class With(stmt)
         *  |  With(withitem* items, stmt* body, string? type_comment)
         * ```
         */
        class With(pyObject: PyObject) : BaseStmt(pyObject), NormalOrAsyncWith {
            override val items: kotlin.collections.List<withitem> by lazy { "items" of pyObject }
            override val body: kotlin.collections.List<BaseStmt> by lazy { "body" of pyObject }
            override val type_comment: String? by lazy { "type_comment" of pyObject }
        }

        /**
         * ```
         * ast.AsyncWith = class AsyncWith(stmt)
         *  |  AsyncWith(withitem* items, stmt* body, string? type_comment)
         * ```
         */
        class AsyncWith(pyObject: PyObject) : BaseStmt(pyObject), NormalOrAsyncWith, IsAsync {
            override val items: kotlin.collections.List<withitem> by lazy { "items" of pyObject }
            override val body: kotlin.collections.List<BaseStmt> by lazy { "body" of pyObject }
            override val type_comment: String? by lazy { "type_comment" of pyObject }
        }

        /**
         * ```
         * ast.Match = class Match(stmt)
         *  |  Match(expr subject, match_case* cases)
         * ```
         */
        class Match(pyObject: PyObject) : BaseStmt(pyObject) {
            val subject: BaseExpr by lazy { "subject" of pyObject }
            val cases: kotlin.collections.List<match_case> by lazy { "cases" of pyObject }
        }

        /**
         * ```
         * ast.Raise = class Raise(stmt)
         *  |  Raise(expr? exc, expr? cause)
         * ```
         */
        class Raise(pyObject: PyObject) : BaseStmt(pyObject) {
            val exc: BaseExpr? by lazy { "exc" of pyObject }
            val cause: BaseExpr? by lazy { "cause" of pyObject }
        }

        /**
         * ```
         * ast.Try = class Try(stmt)
         *  |  Try(stmt* body, excepthandler* handlers, stmt* orelse, stmt* finalbody)
         * ```
         */
        class Try(pyObject: PyObject) : BaseStmt(pyObject) {
            val body: kotlin.collections.List<BaseStmt> by lazy { "body" of pyObject }
            val handlers: kotlin.collections.List<BaseExcepthandler> by lazy {
                "handlers" of pyObject
            }
            val orelse: kotlin.collections.List<BaseStmt> by lazy { "orelse" of pyObject }
            val finalbody: kotlin.collections.List<BaseStmt> by lazy { "finalbody" of pyObject }
        }

        /**
         * ```
         * ast.TryStar = class TryStar(stmt)
         *  |  TryStar(stmt* body, excepthandler* handlers, stmt* orelse, stmt* finalbody)
         * ```
         */
        class TryStar(pyObject: PyObject) : BaseStmt(pyObject) {
            val body: kotlin.collections.List<BaseStmt> by lazy { "body" of pyObject }
            val handlers: kotlin.collections.List<BaseExcepthandler> by lazy {
                "handlers" of pyObject
            }
            val orelse: kotlin.collections.List<BaseStmt> by lazy { "orelse" of pyObject }
            val finalbody: kotlin.collections.List<BaseStmt> by lazy { "finalbody" of pyObject }
        }

        /**
         * ```
         * ast.Assert = class Assert(stmt)
         *  |  Assert(expr test, expr? msg)
         * ```
         */
        class Assert(pyObject: PyObject) : BaseStmt(pyObject) {
            val test: BaseExpr by lazy { "test" of pyObject }
            val msg: BaseExpr? by lazy { "msg" of pyObject }
        }

        /**
         * ```
         * ast.Import = class Import(stmt)
         *  |  Import(alias* names)
         * ```
         */
        class Import(pyObject: PyObject) : BaseStmt(pyObject) {
            val names: kotlin.collections.List<alias> by lazy { "names" of pyObject }
        }

        /**
         * ```
         * ast.ImportFrom = class ImportFrom(stmt)
         *  |  ImportFrom(identifier? module, alias* names, int? level)
         * ```
         */
        class ImportFrom(pyObject: PyObject) : BaseStmt(pyObject) {
            val module: String? by lazy { "module" of pyObject }
            val names: kotlin.collections.List<alias> by lazy { "names" of pyObject }
            val level: Long? by lazy { "level" of pyObject }
        }

        /**
         * ```
         * ast.Global = class Global(stmt)
         *  |  Global(identifier* names)
         * ```
         */
        class Global(pyObject: PyObject) : BaseStmt(pyObject) {
            val names: kotlin.collections.List<String> by lazy { "names" of pyObject }
        }

        /**
         * ```
         * ast.Nonlocal = class Nonlocal(stmt)
         *  |  Nonlocal(identifier* names)
         * ```
         */
        class Nonlocal(pyObject: PyObject) : BaseStmt(pyObject) {
            val names: kotlin.collections.List<String> by lazy { "names" of pyObject }
        }

        /**
         * Represents `ast.Expr` expressions. Note: do not confuse with
         * - [BaseExpr] -> the expression class
         * - [Expression] -> the expression as part of `mod`
         *
         * ```
         * ast.Expr = class Expr(stmt)
         *  |  Expr(expr value)
         * ```
         */
        class Expr(pyObject: PyObject) : BaseStmt(pyObject) {
            val value: BaseExpr by lazy { "value" of pyObject }
        }

        /**
         * ```
         * ast.Pass = class Pass(stmt)
         *  |  Pass
         * ```
         */
        class Pass(pyObject: PyObject) : BaseStmt(pyObject)

        /**
         * ```
         * ast.Break = class Break(stmt)
         *  |  Break
         * ```
         */
        class Break(pyObject: PyObject) : BaseStmt(pyObject)

        /**
         * ```
         * ast.Continue = class Continue(stmt)
         *  |  Continue
         * ```
         */
        class Continue(pyObject: PyObject) : BaseStmt(pyObject)

        /**
         * Represents `ast.expr` expressions. Note: do not confuse with
         * - [Expr] -> the expression statement
         * - [Expression] -> the expression as part of `mod`
         *
         * ast.expr = class expr(AST)
         */
        sealed class BaseExpr(pyObject: PyObject) : AST, BaseObject(pyObject), WithLocation

        /**
         * ```
         * ast.BoolOp = class BoolOp(expr)
         *  |  BoolOp(boolop op, expr* values)
         * ```
         */
        class BoolOp(pyObject: PyObject) : BaseExpr(pyObject) {
            val op: BaseBoolOp by lazy { "op" of pyObject }
            val values: kotlin.collections.List<BaseExpr> by lazy { "values" of pyObject }
        }

        /**
         * ```
         * ast.NamedExpr = class NamedExpr(expr)
         *  |  NamedExpr(expr target, expr value)
         * ```
         */
        class NamedExpr(pyObject: PyObject) : BaseExpr(pyObject) {
            val target: BaseExpr by lazy { "target" of pyObject }
            val value: BaseExpr by lazy { "value" of pyObject }
        }

        /**
         * ```
         * ast.BinOp = class BinOp(expr)
         *  |  BinOp(expr left, operator op, expr right)
         * ```
         */
        class BinOp(pyObject: PyObject) : BaseExpr(pyObject) {
            val left: BaseExpr by lazy { "left" of pyObject }
            val op: BaseOperator by lazy { "op" of pyObject }
            val right: BaseExpr by lazy { "right" of pyObject }
        }

        /**
         * ```
         * ast.UnaryOp = class UnaryOp(expr)
         *  |  UnaryOp(unaryop op, expr operand)
         * ```
         */
        class UnaryOp(pyObject: PyObject) : BaseExpr(pyObject) {
            val op: BaseUnaryOp by lazy { "op" of pyObject }
            val operand: BaseExpr by lazy { "operand" of pyObject }
        }

        /**
         * ```
         * ast.Lambda = class Lambda(expr)
         *  |  Lambda(arguments args, expr body)
         * ```
         */
        class Lambda(pyObject: PyObject) : BaseExpr(pyObject) {
            val args: arguments by lazy { "args" of pyObject }
            val body: BaseExpr by lazy { "body" of pyObject }
        }

        /**
         * ```
         * ast.IfExp = class IfExp(expr)
         *  |  IfExp(expr test, expr body, expr orelse)
         * ```
         */
        class IfExp(pyObject: PyObject) : BaseExpr(pyObject) {
            val test: BaseExpr by lazy { "test" of pyObject }
            val body: BaseExpr by lazy { "body" of pyObject }
            val orelse: BaseExpr by lazy { "orelse" of pyObject }
        }

        /**
         * ```
         * ast.Dict = class Dict(expr)
         *  |  Dict(expr* keys, expr* values)
         * ```
         */
        class Dict(pyObject: PyObject) : BaseExpr(pyObject) {
            val keys: kotlin.collections.List<BaseExpr?> by lazy { "keys" of pyObject }
            val values: kotlin.collections.List<BaseExpr> by lazy { "values" of pyObject }
        }

        /**
         * ```
         * ast.Set = class Set(expr)
         *  |  Set(expr* elts)
         * ```
         */
        class Set(pyObject: PyObject) : BaseExpr(pyObject) {
            val elts: kotlin.collections.List<BaseExpr> by lazy { "elts" of pyObject }
        }

        /**
         * ```
         * ast.ListComp = class ListComp(expr)
         *  |  ListComp(expr elt, comprehension* generators)
         * ```
         */
        class ListComp(pyObject: PyObject) : BaseExpr(pyObject) {
            val elt: BaseExpr by lazy { "elt" of pyObject }
            val generators: kotlin.collections.List<comprehension> by lazy {
                "generators" of pyObject
            }
        }

        /**
         * ```
         * ast.SetComp = class SetComp(expr)
         *  |  SetComp(expr elt, comprehension* generators)
         * ```
         */
        class SetComp(pyObject: PyObject) : BaseExpr(pyObject) {
            val elt: BaseExpr by lazy { "elt" of pyObject }
            val generators: kotlin.collections.List<comprehension> by lazy {
                "generators" of pyObject
            }
        }

        /**
         * ```
         * ast.DictComp = class DictComp(expr)
         *  |  DictComp(expr key, expr value, comprehension* generators)
         * ```
         */
        class DictComp(pyObject: PyObject) : BaseExpr(pyObject) {
            val key: BaseExpr by lazy { "key" of pyObject }
            val value: BaseExpr by lazy { "value" of pyObject }
            val generators: kotlin.collections.List<comprehension> by lazy {
                "generators" of pyObject
            }
        }

        /**
         * ```
         * ast.GeneratorExp = class GeneratorExp(expr)
         *  |  GeneratorExp(expr elt, comprehension* generators)
         * ```
         */
        class GeneratorExp(pyObject: PyObject) : BaseExpr(pyObject) {
            val elt: BaseExpr by lazy { "elt" of pyObject }
            val generators: kotlin.collections.List<comprehension> by lazy {
                "generators" of pyObject
            }
        }

        /**
         * ```
         * ast.Await = class Await(expr)
         *  |  Await(expr value)
         * ```
         */
        class Await(pyObject: PyObject) : BaseExpr(pyObject) {
            val value: BaseExpr by lazy { "value" of pyObject }
        }

        /**
         * ```
         * ast.Yield = class Yield(expr)
         *  |  Yield(expr? value)
         * ```
         */
        class Yield(pyObject: PyObject) : BaseExpr(pyObject) {
            val value: BaseExpr? by lazy { "value" of pyObject }
        }

        /**
         * ```
         * ast.YieldFrom = class YieldFrom(expr)
         *  |  YieldFrom(expr value)
         * ```
         */
        class YieldFrom(pyObject: PyObject) : BaseExpr(pyObject) {
            val value: BaseExpr by lazy { "value" of pyObject }
        }

        /**
         * ```
         * ast.Compare = class Compare(expr)
         *  |  Compare(expr left, cmpop* ops, expr* comparators)
         * ```
         */
        class Compare(pyObject: PyObject) : BaseExpr(pyObject) {
            val left: BaseExpr by lazy { "left" of pyObject }
            val ops: kotlin.collections.List<BaseCmpOp> by lazy { "ops" of pyObject }
            val comparators: kotlin.collections.List<BaseExpr> by lazy { "comparators" of pyObject }
        }

        /**
         * ```
         * ast.Call = class Call(expr)
         *  |  Call(expr func, expr* args, keyword* keywords)
         * ```
         */
        class Call(pyObject: PyObject) : BaseExpr(pyObject) {
            val func: BaseExpr by lazy { "func" of pyObject }

            val args: kotlin.collections.List<BaseExpr> by lazy { "args" of pyObject }

            val keywords: kotlin.collections.List<keyword> by lazy { "keywords" of pyObject }
        }

        /**
         * ```
         * ast.FormattedValue = class FormattedValue(expr)
         *  |  FormattedValue(expr value, int conversion, expr? format_spec)
         * ```
         */
        class FormattedValue(pyObject: PyObject) : BaseExpr(pyObject) {
            val value: BaseExpr by lazy { "value" of pyObject }
            val conversion: Long? by lazy { "conversion" of pyObject }
            val format_spec: BaseExpr? by lazy { "format_spec" of pyObject }
        }

        /**
         * ```
         * ast.JoinedStr = class JoinedStr(expr)
         *  |  JoinedStr(expr* values)
         * ```
         */
        class JoinedStr(pyObject: PyObject) : BaseExpr(pyObject) {
            val values: kotlin.collections.List<BaseExpr> by lazy { "values" of pyObject }
        }

        /**
         * ```
         * ast.Constant = class Constant(expr)
         *  |  Constant(constant value, string? kind)
         * ```
         */
        class Constant(pyObject: PyObject) : BaseExpr(pyObject) {
            val value: Any by lazy { "value" of pyObject }
            val kind: String? by lazy { "kind" of pyObject }
        }

        /**
         * ```
         * ast.Attribute = class Attribute(expr)
         *  |  Attribute(expr value, identifier attr, expr_context ctx)
         * ```
         */
        class Attribute(pyObject: PyObject) : BaseExpr(pyObject) {
            val value: BaseExpr by lazy { "value" of pyObject }
            val attr: String by lazy { "attr" of pyObject }
            val ctx: BaseExprContext by lazy { "ctx" of pyObject }
        }

        /**
         * ```
         * ast.Subscript = class Subscript(expr)
         *  |  Subscript(expr value, expr slice, expr_context ctx)
         * ```
         */
        class Subscript(pyObject: PyObject) : BaseExpr(pyObject) {
            val value: BaseExpr by lazy { "value" of pyObject }
            val slice: BaseExpr by lazy { "slice" of pyObject }
            val ctx: BaseExprContext by lazy { "ctx" of pyObject }
        }

        /**
         * ```
         * ast.Starred = class Starred(expr)
         *  |  Starred(expr value, expr_context ctx)
         * ```
         */
        class Starred(pyObject: PyObject) : BaseExpr(pyObject) {
            val value: BaseExpr by lazy { "value" of pyObject }
            val ctx: BaseExprContext by lazy { "ctx" of pyObject }
        }

        /**
         * ```
         * ast.Name = class Name(expr)
         *  |  Name(identifier id, expr_context ctx)
         * ```
         */
        class Name(pyObject: PyObject) : BaseExpr(pyObject) {
            val id: String by lazy { "id" of pyObject }
            val ctx: BaseExprContext by lazy { "ctx" of pyObject }
        }

        /**
         * ```
         * ast.List = class List(expr)
         *  |  List(expr* elts, expr_context ctx)
         * ```
         */
        class List(pyObject: PyObject) : BaseExpr(pyObject) {
            val elts: kotlin.collections.List<BaseExpr> by lazy { "elts" of pyObject }
            val ctx: BaseExprContext by lazy { "ctx" of pyObject }
        }

        /**
         * ```
         * ast.Tuple = class Tuple(expr)
         *  |  Tuple(expr* elts, expr_context ctx)
         * ```
         */
        class Tuple(pyObject: PyObject) : BaseExpr(pyObject) {
            val elts: kotlin.collections.List<BaseExpr> by lazy { "elts" of pyObject }
            val ctx: BaseExprContext by lazy { "ctx" of pyObject }
        }

        /**
         * ```
         * ast.Slice = class Slice(expr)
         *  |  Slice(expr? lower, expr? upper, expr? step)
         * ```
         */
        class Slice(pyObject: PyObject) : BaseExpr(pyObject) {
            val lower: BaseExpr? by lazy { "lower" of pyObject }
            val upper: BaseExpr? by lazy { "upper" of pyObject }
            val step: BaseExpr? by lazy { "step" of pyObject }
        }

        /**
         * ```
         * ast.boolop = class boolop(AST)
         *  |  boolop = And | Or
         * ```
         */
        sealed class BaseBoolOp(pyObject: PyObject) : AST, BaseObject(pyObject)

        /**
         * ```
         * ast.And = class And(boolop)
         *  |  And
         * ```
         */
        class And(pyObject: PyObject) : BaseBoolOp(pyObject)

        /**
         * ```
         * ast.Or = class Or(boolop)
         *  |  Or
         */
        class Or(pyObject: PyObject) : BaseBoolOp(pyObject)

        /**
         * ```
         * ast.cmpop = class cmpop(AST)
         *  |  cmpop = Eq | NotEq | Lt | LtE | Gt | GtE | Is | IsNot | In | NotIn
         * ```
         */
        sealed class BaseCmpOp(pyObject: PyObject) : AST, BaseObject(pyObject)

        /**
         * ```
         * ast.Eq = class Eq(cmpop)
         *  |  Eq
         * ```
         */
        class Eq(pyObject: PyObject) : BaseCmpOp(pyObject)

        /**
         * ```
         * ast.NotEq = class NotEq(cmpop)
         *  |  NotEq
         * ```
         */
        class NotEq(pyObject: PyObject) : BaseCmpOp(pyObject)

        /**
         * ```
         * ast.Lt = class Lt(cmpop)
         *  |  Lt
         * ```
         */
        class Lt(pyObject: PyObject) : BaseCmpOp(pyObject)

        /**
         * ```
         * ast.LtE = class LtE(cmpop)
         *  |  LtE
         * ```
         */
        class LtE(pyObject: PyObject) : BaseCmpOp(pyObject)

        /**
         * ```
         * ast.Gt = class Gt(cmpop)
         *  |  Gt
         * ```
         */
        class Gt(pyObject: PyObject) : BaseCmpOp(pyObject)

        /**
         * ```
         * ast.GtE = class GtE(cmpop)
         *  |  GtE
         * ```
         */
        class GtE(pyObject: PyObject) : BaseCmpOp(pyObject)

        /**
         * ```
         * ast.Is = class Is(cmpop)
         *  |  Is
         * ```
         */
        class Is(pyObject: PyObject) : BaseCmpOp(pyObject)

        /**
         * ```
         * ast.IsNot = class IsNot(cmpop)
         *  |  IsNot
         * ```
         */
        class IsNot(pyObject: PyObject) : BaseCmpOp(pyObject)

        /**
         * ```
         * ast.In = class In(cmpop)
         *  |  In
         * ```
         */
        class In(pyObject: PyObject) : BaseCmpOp(pyObject)

        /**
         * ```
         * ast.NotIn = class NotIn(cmpop)
         *  |  NotIn
         * ```
         */
        class NotIn(pyObject: PyObject) : BaseCmpOp(pyObject)

        /**
         * ```
         * ast.expr_context = class expr_context(AST)
         *  |  expr_context = Load | Store | Del
         * ```
         */
        sealed class BaseExprContext(pyObject: PyObject) : AST, BaseObject(pyObject)

        /**
         * ```
         * ast.Load = class Load(expr_context)
         *  |  Load
         * ```
         */
        class Load(pyObject: PyObject) : BaseExprContext(pyObject)

        /**
         * ```
         * ast.Store = class Store(expr_context)
         *  |  Store
         * ```
         */
        class Store(pyObject: PyObject) : BaseExprContext(pyObject)

        /**
         * ```
         * ast.Del = class Del(expr_context)
         *  |  Del
         * ```
         */
        class Del(pyObject: PyObject) : BaseExprContext(pyObject)

        /**
         * ```
         * ast.operator = class operator(AST)
         *  |  operator = Add | Sub | Mult | MatMult | Div | Mod | Pow | LShift | RShift | BitOr | BitXor | BitAnd | FloorDiv
         * ```
         */
        sealed class BaseOperator(pyObject: PyObject) : AST, BaseObject(pyObject)

        /**
         * ```
         * ast.Add = class Add(operator)
         *  |  Add
         * ```
         */
        class Add(pyObject: PyObject) : BaseOperator(pyObject)

        /**
         * ```
         * ast.Sub = class Sub(operator)
         *  |  Sub
         * ```
         */
        class Sub(pyObject: PyObject) : BaseOperator(pyObject)

        /**
         * ```
         * ast.Mult = class Mult(operator)
         *  |  Mult
         * ```
         */
        class Mult(pyObject: PyObject) : BaseOperator(pyObject)

        /**
         * ```
         * ast.MatMult = class MatMult(operator)
         *  |  MatMult
         * ```
         */
        class MatMult(pyObject: PyObject) : BaseOperator(pyObject)

        /**
         * ```
         * ast.Div = class Div(operator)
         *  |  Div
         * ```
         */
        class Div(pyObject: PyObject) : BaseOperator(pyObject)

        /**
         * ```
         * ast.Mod = class Mod(operator)
         *  |  Mod
         * ```
         */
        class Mod(pyObject: PyObject) : BaseOperator(pyObject)

        /**
         * ```
         * ast.Pow = class Pow(operator)
         *  |  Pow
         * ```
         */
        class Pow(pyObject: PyObject) : BaseOperator(pyObject)

        /**
         * ```
         * ast.LShift = class LShift(operator)
         *  |  LShift
         * ```
         */
        class LShift(pyObject: PyObject) : BaseOperator(pyObject)

        /**
         * ```
         * ast.RShift = class RShift(operator)
         *  |  RShift
         * ```
         */
        class RShift(pyObject: PyObject) : BaseOperator(pyObject)

        /**
         * ```
         * ast.BitOr = class BitOr(operator)
         *  |  BitOr
         * ```
         */
        class BitOr(pyObject: PyObject) : BaseOperator(pyObject)

        /**
         * ```
         * ast.BitXor = class BitXor(operator)
         *  |  BitXor
         * ```
         */
        class BitXor(pyObject: PyObject) : BaseOperator(pyObject)

        /**
         * ```
         * ast.BitAnd = class BitAnd(operator)
         *  |  BitAnd
         * ```
         */
        class BitAnd(pyObject: PyObject) : BaseOperator(pyObject)

        /**
         * ```
         * ast.FloorDiv = class FloorDiv(operator)
         *  |  FloorDiv
         * ```
         */
        class FloorDiv(pyObject: PyObject) : BaseOperator(pyObject)

        /**
         * ```
         * ast.pattern = class pattern(AST)
         *  |  pattern = MatchValue(expr value)
         *  |  | MatchSingleton(constant value)
         *  |  | MatchSequence(pattern* patterns)
         *  |  | MatchMapping(expr* keys, pattern* patterns, identifier? rest)
         *  |  | MatchClass(expr cls, pattern* patterns, identifier* kwd_attrs, pattern* kwd_patterns)
         *  |  | MatchStar(identifier? name)
         *  |  | MatchAs(pattern? pattern, identifier? name)
         *  |  | MatchOr(pattern* patterns)
         * ```
         */
        abstract class BasePattern(pyObject: PyObject) : AST, BaseObject(pyObject), WithLocation

        /**
         * ```
         * ast.MatchValue = class MatchValue(pattern)
         *  |  MatchValue(expr value)
         * ```
         */
        class MatchValue(pyObject: PyObject) : BasePattern(pyObject) {
            val value: BaseExpr by lazy { "value" of pyObject }
        }

        /**
         * ```
         * ast.MatchSingleton = class MatchSingleton(pattern)
         *  |  MatchSingleton(constant value)
         * ```
         */
        class MatchSingleton(pyObject: PyObject) : BasePattern(pyObject) {
            /**
             * [value] is not optional. We have to make it nullable though because the value will be
             * set to `null` if the case matches on `None`. This is known behavior of jep (similar
             * to literals/constants).
             */
            val value: Any? by lazy { "value" of pyObject }
        }

        /**
         * ```
         * ast.MatchSequence = class MatchSequence(pattern)
         *  |  MatchSequence(pattern* patterns)
         * ```
         */
        class MatchSequence(pyObject: PyObject) : BasePattern(pyObject) {
            val patterns: kotlin.collections.List<BasePattern> by lazy { "patterns" of pyObject }
        }

        /**
         * ```
         * ast.MatchMapping = class MatchMapping(pattern)
         *  |  MatchMapping(expr* keys, pattern* patterns, identifier? rest)
         * ```
         */
        class MatchMapping(pyObject: PyObject) : BasePattern(pyObject) {
            val key: kotlin.collections.List<BaseExpr> by lazy { "keys" of pyObject }
            val patterns: kotlin.collections.List<BasePattern> by lazy { "patterns" of pyObject }
            val rest: String? by lazy { "rest" of pyObject }
        }

        /**
         * ```
         * ast.MatchClass = class MatchClass(pattern)
         *  |  MatchClass(expr cls, pattern* patterns, identifier* kwd_attrs, pattern* kwd_patterns)
         * ```
         */
        class MatchClass(pyObject: PyObject) : BasePattern(pyObject) {
            val cls: BaseExpr by lazy { "cls" of pyObject }
            val patterns: kotlin.collections.List<BasePattern> by lazy { "patterns" of pyObject }
            val kwd_attrs: kotlin.collections.List<String> by lazy { "kwd_attrs" of pyObject }
            val kwd_patterns: kotlin.collections.List<BasePattern> by lazy {
                "kwd_patterns" of pyObject
            }
        }

        /**
         * ```
         * ast.MatchStar = class MatchStar(pattern)
         *  |  MatchStar(identifier? name)
         * ```
         */
        class MatchStar(pyObject: PyObject) : BasePattern(pyObject) {
            val name: String? by lazy { "name" of pyObject }
        }

        /**
         * ```
         * ast.MatchAs = class MatchAs(pattern)
         *  |  MatchAs(pattern? pattern, identifier? name)
         * ```
         */
        class MatchAs(pyObject: PyObject) : BasePattern(pyObject) {
            val pattern: BasePattern? by lazy { "pattern" of pyObject }
            val name: String? by lazy { "name" of pyObject }
        }

        /**
         * ```
         * ast.MatchOr = class MatchOr(pattern)
         *  |  MatchOr(pattern* patterns)
         * ```
         */
        class MatchOr(pyObject: PyObject) : BasePattern(pyObject) {
            val patterns: kotlin.collections.List<BasePattern> by lazy { "patterns" of pyObject }
        }

        /**
         * ```
         * ast.unaryop = class unaryop(AST)
         *  |  unaryop = Invert | Not | UAdd | USub
         * ```
         */
        sealed class BaseUnaryOp(pyObject: PyObject) : AST, BaseObject(pyObject)

        /**
         * ```
         * ast.Invert = class Invert(unaryop)
         *  |  Invert
         * ```
         */
        class Invert(pyObject: PyObject) : BaseUnaryOp(pyObject)

        /**
         * ```
         * ast.Not = class Not(unaryop)
         *  |  Not
         * ```
         */
        class Not(pyObject: PyObject) : BaseUnaryOp(pyObject)

        /**
         * ```
         * ast.UAdd = class UAdd(unaryop)
         *  |  UAdd
         * ```
         */
        class UAdd(pyObject: PyObject) : BaseUnaryOp(pyObject)

        /**
         * ```
         * ast.USub = class USub(unaryop)
         *  |  USub
         * ```
         */
        class USub(pyObject: PyObject) : BaseUnaryOp(pyObject)

        /**
         * ```
         * ast.alias = class alias(AST)
         *  |  alias(identifier name, identifier? asname)
         * ```
         */
        class alias(pyObject: PyObject) : AST, BaseObject(pyObject), WithLocation {
            val name: String by lazy { "name" of pyObject }
            val asname: String? by lazy { "asname" of pyObject }
        }

        /**
         * ```
         * ast.arg = class arg(AST)
         *  |  arg(identifier arg, expr? annotation, string? type_comment)
         * ```
         */
        class arg(pyObject: PyObject) : AST, BaseObject(pyObject), WithLocation {
            val arg: String by lazy { "arg" of pyObject }
            val annotation: BaseExpr? by lazy { "annotation" of pyObject }
            val type_comment: String? by lazy { "type_comment" of pyObject }
        }

        /**
         * ```
         * ast.arguments = class arguments(AST)
         *  |  arguments(arg* posonlyargs, arg* args, arg? vararg, arg* kwonlyargs, expr* kw_defaults, arg? kwarg, expr* defaults)
         * ```
         */
        class arguments(pyObject: PyObject) : AST, BaseObject(pyObject) {
            val posonlyargs: kotlin.collections.List<arg> by lazy { "posonlyargs" of pyObject }
            val args: kotlin.collections.List<arg> by lazy { "args" of pyObject }
            val vararg: arg? by lazy { "vararg" of pyObject }
            val kwonlyargs: kotlin.collections.List<arg> by lazy { "kwonlyargs" of pyObject }
            val kw_defaults: kotlin.collections.List<BaseExpr> by lazy { "kw_defaults" of pyObject }
            val kwarg: arg? by lazy { "kwarg" of pyObject }
            val defaults: kotlin.collections.List<BaseExpr> by lazy { "defaults" of pyObject }
        }

        /**
         * ```
         * ast.comprehension = class comprehension(AST)
         *  |  comprehension(expr target, expr iter, expr* ifs, int is_async)
         * ```
         */
        class comprehension(pyObject: PyObject) : AST, BaseObject(pyObject) {
            val target: BaseExpr by lazy { "target" of pyObject }
            val iter: BaseExpr by lazy { "iter" of pyObject }
            val ifs: kotlin.collections.List<BaseExpr> by lazy { "ifs" of pyObject }
            val is_async: Long by lazy { "is_async" of pyObject }
        }

        /**
         * ```
         * ast.excepthandler = class excepthandler(AST)
         *  |  excepthandler = ExceptHandler(expr? type, identifier? name, stmt* body)
         * ```
         */
        sealed class BaseExcepthandler(pyObject: PyObject) :
            AST, BaseObject(pyObject), WithLocation

        /**
         * ast.ExceptHandler = class ExceptHandler(excepthandler) | ExceptHandler(expr? type,
         * identifier? name, stmt* body)
         */
        class ExceptHandler(pyObject: PyObject) : BaseExcepthandler(pyObject) {
            val type: BaseExpr? by lazy { "type" of pyObject }
            val name: String? by lazy { "name" of pyObject }
            val body: kotlin.collections.List<BaseStmt> by lazy { "body" of pyObject }
        }

        /**
         * ```
         * ast.keyword = class keyword(AST)
         *  |  keyword(identifier? arg, expr value)
         * ```
         */
        class keyword(pyObject: PyObject) : AST, BaseObject(pyObject), WithLocation {
            val arg: String? by lazy { "arg" of pyObject }
            val value: BaseExpr by lazy { "value" of pyObject }
        }

        /**
         * ```
         * ast.match_case = class match_case(AST)
         *  |  match_case(pattern pattern, expr? guard, stmt* body)
         * ```
         */
        class match_case(pyObject: PyObject) : AST, BaseObject(pyObject) {
            val pattern: BasePattern by lazy { "pattern" of pyObject }
            val guard: BaseExpr? by lazy { "guard" of pyObject }
            val body: kotlin.collections.List<BaseStmt> by lazy { "body" of pyObject }
        }

        /**
         * ```
         * ast.type_ignore = class type_ignore(AST)
         *  |  type_ignore = TypeIgnore(int lineno, string tag)
         * ```
         *
         * TODO
         */
        class type_ignore(pyObject: PyObject) : AST, BaseObject(pyObject)

        /**
         * ```
         * ast.withitem = class withitem(AST)
         *  |  withitem(expr context_expr, expr? optional_vars)
         * ```
         */
        class withitem(pyObject: PyObject) : AST, BaseObject(pyObject) {
            val context_expr: BaseExpr by lazy { "context_expr" of pyObject }
            val optional_vars: BaseExpr? by lazy { "optional_vars" of pyObject }
        }
    }
}

private inline infix fun <reified T> String.of(pyObject: PyObject): T {
    val ret =
        pyObject.getAttr(this).let { value ->
            if (value is List<*>) {
                value.map { if (it is PyObject) fromPython(it) else it }
            } else {
                if (value is PyObject) fromPython(value) else value
            }
        }
    if (ret !is T) {
        TODO("Expected a " + T::class.java + " but received a " + ret::class.java)
    }

    return ret
}
