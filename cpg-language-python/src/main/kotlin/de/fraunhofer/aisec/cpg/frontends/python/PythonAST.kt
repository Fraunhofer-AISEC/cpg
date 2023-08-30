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

import java.net.URI
import jep.python.PyObject

/**
 * This interface makes Python's `ast` nodes accessible to Kotlin. It does not contain any complex
 * logic but rather aims at making all Python `ast` properties accessible to Kotlin (under the same
 * name as in Python).
 *
 * Python's AST object are mapped as close as possible to the original. Exceptions:
 * - `identifier` fields are mapped as Kotlin `String`s
 * - Python's `int` is mapped to `Int`
 * - Constants are mapped as `Any` (thus Jep's conversion to Java makes the translation)
 */
interface PythonAST {

    /** `ast.stmt` and `ast.expr` nodes have extra location properties as implemented here. */
    interface WithPythonLocation { // TODO make the fields accessible `by lazy`
        val pyObject: PyObject

        /** Maps to the `lineno` filed from Pyhon's ast. */
        val lineno: Int
            get() {
                return (pyObject.getAttr("lineno") as? Long)?.toInt() ?: TODO()
            }

        /** Maps to the `col_offset` filed from Pyhon's ast. */
        val col_offset: Int
            get() {
                return (pyObject.getAttr("col_offset") as? Long)?.toInt() ?: TODO()
            }

        /** Maps to the `end_lineno` filed from Pyhon's ast. */
        val end_lineno: Int
            get() {
                return (pyObject.getAttr("end_lineno") as? Long)?.toInt() ?: TODO()
            }

        /** Maps to the `end_col_offset` filed from Pyhon's ast. */
        val end_col_offset: Int
            get() {
                return (pyObject.getAttr("end_col_offset") as? Long)?.toInt() ?: TODO()
            }
    }

    /**
     * Represents a `ast.AST` node as returned by Python's `ast` parser.
     *
     * @param pyObject The Python object returned by jep.
     */
    abstract class AST(val pyObject: PyObject, val uri: URI)

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
    abstract class mod(pyObject: PyObject, uri: URI) : AST(pyObject, uri)

    /**
     * ```
     * ast.Module = class Module(mod)
     *  |  Module(stmt* body, type_ignore* type_ignores)
     * ```
     */
    class Module(pyObject: PyObject, uri: URI) : AST(pyObject, uri) {
        val body: kotlin.collections.List<stmt> by lazy { getList(pyObject, uri, "body") }

        val type_ignores: kotlin.collections.List<type_ignore> by lazy {
            getList(pyObject, uri, "type_ignores")
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
    abstract class stmt(pyObject: PyObject, uri: URI) : AST(pyObject, uri), WithPythonLocation

    /**
     * ```
     * ast.FunctionDef = class FunctionDef(stmt)
     *  |  FunctionDef(identifier name, arguments args, stmt* body, expr* decorator_list, expr? returns, string? type_comment)
     * ```
     */
    class FunctionDef(pyObject: PyObject, uri: URI) : stmt(pyObject, uri) {
        val name: String by lazy { getSingle(pyObject, uri, "name") }

        val args: arguments by lazy { getSingle(pyObject, uri, "args") }

        val body: kotlin.collections.List<stmt> by lazy { getList(pyObject, uri, "body") }

        val decorator_list: kotlin.collections.List<expr> by lazy {
            getList(pyObject, uri, "decorator_list")
        }

        val returns: expr? by lazy { getSingle(pyObject, uri, "returns") }

        val type_comment: String? by lazy { getSingle(pyObject, uri, "type_comment") }
    }

    /**
     * ```
     * ast.AsyncFunctionDef = class AsyncFunctionDef(stmt)
     *  |  AsyncFunctionDef(identifier name, arguments args, stmt* body, expr* decorator_list, expr? returns, string? type_comment)
     * ```
     */
    class AsyncFunctionDef(pyObject: PyObject, uri: URI) : stmt(pyObject, uri) {
        val name: String by lazy { getSingle(pyObject, uri, "name") }

        val args: arguments by lazy { getSingle(pyObject, uri, "args") }

        val body: kotlin.collections.List<stmt> by lazy { getList(pyObject, uri, "body") }

        val decorator_list: kotlin.collections.List<expr> by lazy {
            getList(pyObject, uri, "decorator_list")
        }

        val returns: expr? by lazy { getSingle(pyObject, uri, "returns") }

        val type_comment: String? by lazy { getSingle(pyObject, uri, "type_comment") }
    }

    /**
     * ```
     * ast.ClassDef = class ClassDef(stmt)
     *  |  ClassDef(identifier name, expr* bases, keyword* keywords, stmt* body, expr* decorator_list)
     * ```
     */
    class ClassDef(pyObject: PyObject, uri: URI) : stmt(pyObject, uri) {
        val name: String by lazy { getSingle(pyObject, uri, "name") }

        val bases: kotlin.collections.List<expr> by lazy { getList(pyObject, uri, "bases") }

        val keywords: kotlin.collections.List<keyword> by lazy {
            getList(pyObject, uri, "keywords")
        }

        val body: kotlin.collections.List<stmt> by lazy { getList(pyObject, uri, "body") }

        val decorator_list: kotlin.collections.List<expr> by lazy {
            getList(pyObject, uri, "decorator_list")
        }
    }

    /**
     * ```
     * ast.Return = class Return(stmt)
     *  |  Return(expr? value)
     * ```
     */
    class Return(pyObject: PyObject, uri: URI) : stmt(pyObject, uri) {
        val value: expr? by lazy { getSingle(pyObject, uri, "value") }
    }

    /**
     * ```
     * ast.Delete = class Delete(stmt)
     *  |  Delete(expr* targets)
     * ```
     */
    class Delete(pyObject: PyObject, uri: URI) : stmt(pyObject, uri) {
        val targets: kotlin.collections.List<expr> by lazy { getList(pyObject, uri, "targets") }
    }

    /**
     * ```
     * ast.Assign = class Assign(stmt)
     *  |  Assign(expr* targets, expr value, string? type_comment)
     * ```
     */
    class Assign(pyObject: PyObject, uri: URI) : stmt(pyObject, uri) {
        val targets: kotlin.collections.List<expr> by lazy { getList(pyObject, uri, "targets") }

        val value: expr by lazy { getSingle(pyObject, uri, "value") }

        val type_comment: String? by lazy { getSingle(pyObject, uri, "type_comment") }
    }

    /**
     * ```
     * ast.AugAssign = class AugAssign(stmt)
     *  |  AugAssign(expr target, operator op, expr value)
     * ```
     */
    class AugAssign(pyObject: PyObject, uri: URI) : stmt(pyObject, uri) {
        val target: expr by lazy { getSingle(pyObject, uri, "target") }
        val op: operator by lazy { getSingle(pyObject, uri, "op") }
        val value: expr by lazy { getSingle(pyObject, uri, "value") }
    }

    /**
     * ```
     * ast.AnnAssign = class AnnAssign(stmt)
     *  |  AnnAssign(expr target, expr annotation, expr? value, int simple)
     * ```
     */
    class AnnAssign(pyObject: PyObject, uri: URI) : stmt(pyObject, uri) {
        val target: expr by lazy { getSingle(pyObject, uri, "target") }
        val annotation: expr by lazy { getSingle(pyObject, uri, "annotation") }
        val value: expr? by lazy { getSingle(pyObject, uri, "value") }
        val simple: Int by lazy {
            getSingle(pyObject, uri, "simple")
        } // TODO: is this an `Int` from Kotlins perspective?
    }

    /**
     * ```
     * ast.For = class For(stmt)
     *  |  For(expr target, expr iter, stmt* body, stmt* orelse, string? type_comment)
     * ```
     */
    class For(pyObject: PyObject, uri: URI) : stmt(pyObject, uri) {
        val target: expr by lazy { getSingle(pyObject, uri, "target") }
        val iter: expr by lazy { getSingle(pyObject, uri, "iter") }
        val body: kotlin.collections.List<stmt> by lazy { getList(pyObject, uri, "body") }
        val orelse: kotlin.collections.List<stmt> by lazy { getList(pyObject, uri, "orelse") }
        val type_comment: String? by lazy { getSingle(pyObject, uri, "type_comment") }
    }

    /**
     * ```
     * ast.AsyncFor = class AsyncFor(stmt)
     *  |  AsyncFor(expr target, expr iter, stmt* body, stmt* orelse, string? type_comment)
     * ```
     */
    class AsyncFor(pyObject: PyObject, uri: URI) : stmt(pyObject, uri) {
        val target: expr by lazy { getSingle(pyObject, uri, "target") }
        val iter: expr by lazy { getSingle(pyObject, uri, "iter") }
        val body: kotlin.collections.List<stmt> by lazy { getList(pyObject, uri, "body") }
        val orelse: kotlin.collections.List<stmt> by lazy { getList(pyObject, uri, "orelse") }
        val type_comment: String? by lazy { getSingle(pyObject, uri, "type_comment") }
    }

    /**
     * ```
     * ast.While = class While(stmt)
     *  |  While(expr test, stmt* body, stmt* orelse)
     * ```
     */
    class While(pyObject: PyObject, uri: URI) : stmt(pyObject, uri) {
        val test: expr by lazy { getSingle(pyObject, uri, "test") }
        val body: kotlin.collections.List<stmt> by lazy { getList(pyObject, uri, "body") }
        val orelse: kotlin.collections.List<stmt> by lazy { getList(pyObject, uri, "orelse") }
    }

    /**
     * ```
     * ast.If = class If(stmt)
     *  |  If(expr test, stmt* body, stmt* orelse)
     * ```
     */
    class If(pyObject: PyObject, uri: URI) : stmt(pyObject, uri) {
        val test: expr by lazy { getSingle(pyObject, uri, "test") }
        val body: kotlin.collections.List<stmt> by lazy { getList(pyObject, uri, "body") }
        val orelse: kotlin.collections.List<stmt> by lazy { getList(pyObject, uri, "orelse") }
    }

    /**
     * ```
     * ast.With = class With(stmt)
     *  |  With(withitem* items, stmt* body, string? type_comment)
     * ```
     */
    class With(pyObject: PyObject, uri: URI) : stmt(pyObject, uri) {
        val items: withitem by lazy { getSingle(pyObject, uri, "items") }
        val body: kotlin.collections.List<stmt> by lazy { getList(pyObject, uri, "body") }
        val type_comment: String? by lazy { getSingle(pyObject, uri, "type_comment") }
    }

    /**
     * ```
     * ast.AsyncWith = class AsyncWith(stmt)
     *  |  AsyncWith(withitem* items, stmt* body, string? type_comment)
     * ```
     */
    class AsyncWith(pyObject: PyObject, uri: URI) : stmt(pyObject, uri) {
        val items: withitem by lazy { getSingle(pyObject, uri, "items") }
        val body: kotlin.collections.List<stmt> by lazy { getList(pyObject, uri, "body") }
        val type_comment: String? by lazy { getSingle(pyObject, uri, "type_comment") }
    }

    /**
     * ```
     * ast.Match = class Match(stmt)
     *  |  Match(expr subject, match_case* cases)
     * ```
     */
    class Match(pyObject: PyObject, uri: URI) : stmt(pyObject, uri) {
        val subject: expr by lazy { getSingle(pyObject, uri, "subject") }
        val cases: kotlin.collections.List<match_case> by lazy { getSingle(pyObject, uri, "cases") }
    }

    /**
     * ```
     * ast.Raise = class Raise(stmt)
     *  |  Raise(expr? exc, expr? cause)
     * ```
     */
    class Raise(pyObject: PyObject, uri: URI) : stmt(pyObject, uri) {
        val exc: expr? by lazy { getSingle(pyObject, uri, "exc") }
        val cause: expr? by lazy { getSingle(pyObject, uri, "cause") }
    }

    /**
     * ```
     * ast.Try = class Try(stmt)
     *  |  Try(stmt* body, excepthandler* handlers, stmt* orelse, stmt* finalbody)
     * ```
     */
    class Try(pyObject: PyObject, uri: URI) : stmt(pyObject, uri) {
        val body: kotlin.collections.List<stmt> by lazy { getList(pyObject, uri, "body") }
        val handlers: kotlin.collections.List<excepthandler> by lazy {
            getList(pyObject, uri, "handlers")
        }
        val orelse: kotlin.collections.List<stmt> by lazy { getList(pyObject, uri, "orelse") }
        val stmt: kotlin.collections.List<stmt> by lazy { getList(pyObject, uri, "stmt") }
    }

    /**
     * ```
     * ast.TryStar = class TryStar(stmt)
     *  |  TryStar(stmt* body, excepthandler* handlers, stmt* orelse, stmt* finalbody)
     * ```
     */
    class TryStar(pyObject: PyObject, uri: URI) : stmt(pyObject, uri) {
        val body: kotlin.collections.List<stmt> by lazy { getList(pyObject, uri, "body") }
        val handlers: kotlin.collections.List<excepthandler> by lazy {
            getList(pyObject, uri, "handlers")
        }
        val orelse: kotlin.collections.List<stmt> by lazy { getList(pyObject, uri, "orelse") }
        val finalbody: kotlin.collections.List<stmt> by lazy { getList(pyObject, uri, "finalbody") }
    }

    /**
     * ```
     * ast.Assert = class Assert(stmt)
     *  |  Assert(expr test, expr? msg)
     * ```
     */
    class Assert(pyObject: PyObject, uri: URI) : stmt(pyObject, uri) {
        val test: expr by lazy { getSingle(pyObject, uri, "test") }
        val msg: expr? by lazy { getSingle(pyObject, uri, "msg") }
    }

    /**
     * ```
     * ast.Import = class Import(stmt)
     *  |  Import(alias* names)
     * ```
     */
    class Import(pyObject: PyObject, uri: URI) : stmt(pyObject, uri) {
        val names: kotlin.collections.List<alias> by lazy { getList(pyObject, uri, "names") }
    }

    /**
     * ```
     * ast.ImportFrom = class ImportFrom(stmt)
     *  |  ImportFrom(identifier? module, alias* names, int? level)
     * ```
     */
    class ImportFrom(pyObject: PyObject, uri: URI) : stmt(pyObject, uri) {
        val module: String? by lazy { getSingle(pyObject, uri, "module") }
        val names: kotlin.collections.List<alias> by lazy { getList(pyObject, uri, "names") }
        val level: Int? by lazy {
            getSingle(pyObject, uri, "level")
        } // TODO: is this an `Int` from Kotlins perspective?
    }

    /**
     * ```
     * ast.Global = class Global(stmt)
     *  |  Global(identifier* names)
     * ```
     */
    class Global(pyObject: PyObject, uri: URI) : stmt(pyObject, uri) {
        val names: kotlin.collections.List<String> by lazy { getList(pyObject, uri, "names") }
    }

    /**
     * ```
     * ast.Nonlocal = class Nonlocal(stmt)
     *  |  Nonlocal(identifier* names)
     * ```
     */
    class Nonlocal(pyObject: PyObject, uri: URI) : stmt(pyObject, uri) {
        val names: kotlin.collections.List<String> by lazy { getList(pyObject, uri, "names") }
    }

    /**
     * Represents `ast.Expr` expressions. Note: do not confuse with
     * - [expr] -> the expression class
     * - [Expression] -> the expression as part of `mod`
     *
     * ```
     * ast.Expr = class Expr(stmt)
     *  |  Expr(expr value)
     * ```
     */
    class Expr(pyObject: PyObject, uri: URI) : stmt(pyObject, uri) {
        val value: expr by lazy { getSingle(pyObject, uri, "value") }
    }

    /**
     * ```
     * ast.Pass = class Pass(stmt)
     *  |  Pass
     * ```
     */
    class Pass(pyObject: PyObject, uri: URI) : stmt(pyObject, uri)

    /**
     * ```
     * ast.Break = class Break(stmt)
     *  |  Break
     * ```
     */
    class Break(pyObject: PyObject, uri: URI) : stmt(pyObject, uri)

    /**
     * ```
     * ast.Continue = class Continue(stmt)
     *  |  Continue
     * ```
     */
    class Continue(pyObject: PyObject, uri: URI) : stmt(pyObject, uri)

    /**
     * Represents `ast.expr` expressions. Note: do not confuse with
     * - [Expr] -> the expression statement
     * - [Expression] -> the expression as part of `mod`
     *
     * ast.expr = class expr(AST)
     */
    abstract class expr(pyObject: PyObject, uri: URI) : AST(pyObject, uri)

    /**
     * ```
     * ast.BoolOp = class BoolOp(expr)
     *  |  BoolOp(boolop op, expr* values)
     * ```
     */
    class BoolOp(pyObject: PyObject, uri: URI) : expr(pyObject, uri) {
        val op: boolop by lazy { getSingle(pyObject, uri, "op") }
        val values: kotlin.collections.List<expr> by lazy { getList(pyObject, uri, "values") }
    }

    /**
     * ```
     * ast.NamedExpr = class NamedExpr(expr)
     *  |  NamedExpr(expr target, expr value)
     * ```
     */
    class NamedExpr(pyObject: PyObject, uri: URI) : expr(pyObject, uri) {
        val target: expr by lazy { getSingle(pyObject, uri, "target") }
        val value: expr by lazy { getSingle(pyObject, uri, "value") }
    }

    /**
     * ```
     * ast.BinOp = class BinOp(expr)
     *  |  BinOp(expr left, operator op, expr right)
     * ```
     */
    class BinOp(pyObject: PyObject, uri: URI) : expr(pyObject, uri) {
        val left: expr by lazy { getSingle(pyObject, uri, "left") }
        val op: operator by lazy { getSingle(pyObject, uri, "op") }
        val right: expr by lazy { getSingle(pyObject, uri, "right") }
    }

    /**
     * ```
     * ast.UnaryOp = class UnaryOp(expr)
     *  |  UnaryOp(unaryop op, expr operand)
     * ```
     */
    class UnaryOp(pyObject: PyObject, uri: URI) : expr(pyObject, uri) {
        val op: unaryop by lazy { getSingle(pyObject, uri, "op") }
        val operand: expr by lazy { getSingle(pyObject, uri, "operand") }
    }

    /**
     * ```
     * ast.Lambda = class Lambda(expr)
     *  |  Lambda(arguments args, expr body)
     * ```
     */
    class Lambda(pyObject: PyObject, uri: URI) : expr(pyObject, uri) {
        val args: arguments by lazy { getSingle(pyObject, uri, "args") }
        val body: expr by lazy { getSingle(pyObject, uri, "body") }
    }

    /**
     * ```
     * ast.IfExp = class IfExp(expr)
     *  |  IfExp(expr test, expr body, expr orelse)
     * ```
     */
    class IfExp(pyObject: PyObject, uri: URI) : expr(pyObject, uri) {
        val test: expr by lazy { getSingle(pyObject, uri, "test") }
        val body: expr by lazy { getSingle(pyObject, uri, "body") }
        val orelse: expr by lazy { getSingle(pyObject, uri, "orelse") }
    }

    /**
     * ```
     * ast.Dict = class Dict(expr)
     *  |  Dict(expr* keys, expr* values)
     * ```
     */
    class Dict(pyObject: PyObject, uri: URI) : expr(pyObject, uri) {
        val keys: kotlin.collections.List<expr> by lazy { getList(pyObject, uri, "keys") }
        val values: kotlin.collections.List<expr> by lazy { getList(pyObject, uri, "values") }
    }

    /**
     * ```
     * ast.Set = class Set(expr)
     *  |  Set(expr* elts)
     * ```
     */
    class Set(pyObject: PyObject, uri: URI) : expr(pyObject, uri) {
        val elts: kotlin.collections.List<expr> by lazy { getList(pyObject, uri, "elts") }
    }

    /**
     * ```
     * ast.ListComp = class ListComp(expr)
     *  |  ListComp(expr elt, comprehension* generators)
     * ```
     */
    class ListComp(pyObject: PyObject, uri: URI) : expr(pyObject, uri) {
        val elt: expr by lazy { getSingle(pyObject, uri, "elt") }
        val generators: kotlin.collections.List<comprehension> by lazy {
            getList(pyObject, uri, "generators")
        }
    }

    /**
     * ```
     * ast.SetComp = class SetComp(expr)
     *  |  SetComp(expr elt, comprehension* generators)
     * ```
     */
    class SetComp(pyObject: PyObject, uri: URI) : expr(pyObject, uri) {
        val elt: expr by lazy { getSingle(pyObject, uri, "elt") }
        val generators: kotlin.collections.List<comprehension> by lazy {
            getList(pyObject, uri, "generators")
        }
    }

    /**
     * ```
     * ast.DictComp = class DictComp(expr)
     *  |  DictComp(expr key, expr value, comprehension* generators)
     * ```
     */
    class DictComp(pyObject: PyObject, uri: URI) : expr(pyObject, uri) {
        val key: expr by lazy { getSingle(pyObject, uri, "key") }
        val value: expr by lazy { getSingle(pyObject, uri, "value") }
        val generators: kotlin.collections.List<comprehension> by lazy {
            getList(pyObject, uri, "generators")
        }
    }

    /**
     * ```
     * ast.GeneratorExp = class GeneratorExp(expr)
     *  |  GeneratorExp(expr elt, comprehension* generators)
     * ```
     */
    class GeneratorExp(pyObject: PyObject, uri: URI) : expr(pyObject, uri) {
        val elt: expr by lazy { getSingle(pyObject, uri, "elt") }
        val generators: kotlin.collections.List<comprehension> by lazy {
            getList(pyObject, uri, "generators")
        }
    }

    /**
     * ```
     * ast.Await = class Await(expr)
     *  |  Await(expr value)
     * ```
     */
    class Await(pyObject: PyObject, uri: URI) : expr(pyObject, uri) {
        val value: expr by lazy { getSingle(pyObject, uri, "value") }
    }

    /**
     * ```
     * ast.Yield = class Yield(expr)
     *  |  Yield(expr? value)
     * ```
     */
    class Yield(pyObject: PyObject, uri: URI) : expr(pyObject, uri) {
        val value: expr? by lazy { getSingle(pyObject, uri, "value") }
    }

    /**
     * ```
     * ast.YieldFrom = class YieldFrom(expr)
     *  |  YieldFrom(expr value)
     * ```
     */
    class YieldFrom(pyObject: PyObject, uri: URI) : expr(pyObject, uri) {
        val value: expr by lazy { getSingle(pyObject, uri, "value") }
    }

    /**
     * ```
     * ast.Compare = class Compare(expr)
     *  |  Compare(expr left, cmpop* ops, expr* comparators)
     * ```
     */
    class Compare(pyObject: PyObject, uri: URI) : expr(pyObject, uri) {
        val left: expr by lazy { getSingle(pyObject, uri, "left") }
        val ops: kotlin.collections.List<cmpop> by lazy { getList(pyObject, uri, "ops") }
        val comparators: kotlin.collections.List<expr> by lazy {
            getList(pyObject, uri, "comparators")
        }
    }

    /**
     * ```
     * ast.Call = class Call(expr)
     *  |  Call(expr func, expr* args, keyword* keywords)
     * ```
     */
    class Call(pyObject: PyObject, uri: URI) : expr(pyObject, uri) {
        val func: expr by lazy { getSingle(pyObject, uri, "func") }

        val args: kotlin.collections.List<expr> by lazy { getList(pyObject, uri, "args") }

        val keywords: kotlin.collections.List<keyword> by lazy {
            getList(pyObject, uri, "keywords")
        }
    }

    /**
     * ```
     * ast.FormattedValue = class FormattedValue(expr)
     *  |  FormattedValue(expr value, int conversion, expr? format_spec)
     * ```
     */
    class FormattedValue(pyObject: PyObject, uri: URI) : expr(pyObject, uri) {
        val value: expr by lazy { getSingle(pyObject, uri, "value") }
        val conversion: Int? by lazy {
            getSingle(pyObject, uri, "value")
        } // TODO: int in Kotlin as well?
        val format_spec: expr? by lazy { getSingle(pyObject, uri, "format_spec") }
    }

    /**
     * ```
     * ast.JoinedStr = class JoinedStr(expr)
     *  |  JoinedStr(expr* values)
     * ```
     */
    class JoinedStr(pyObject: PyObject, uri: URI) : expr(pyObject, uri) {
        val values: kotlin.collections.List<expr> by lazy { getList(pyObject, uri, "values") }
    }

    /**
     * ```
     * ast.Constant = class Constant(expr)
     *  |  Constant(constant value, string? kind)
     * ```
     */
    class Constant(pyObject: PyObject, uri: URI) : expr(pyObject, uri) {
        val value: Any? by lazy { getSingle(pyObject, uri, "value") }
        val kind: String? by lazy { getSingle(pyObject, uri, "kind") }
    }

    /**
     * ```
     * ast.Attribute = class Attribute(expr)
     *  |  Attribute(expr value, identifier attr, expr_context ctx)
     * ```
     */
    class Attribute(pyObject: PyObject, uri: URI) : expr(pyObject, uri) {
        val value: expr by lazy { getSingle(pyObject, uri, "value") }
        val attr: String by lazy { getSingle(pyObject, uri, "attr") }
        val ctx: expr_context by lazy { getSingle(pyObject, uri, "ctx") }
    }

    /**
     * ```
     * ast.Subscript = class Subscript(expr)
     *  |  Subscript(expr value, expr slice, expr_context ctx)
     * ```
     */
    class Subscript(pyObject: PyObject, uri: URI) : expr(pyObject, uri) {
        val value: expr by lazy { getSingle(pyObject, uri, "value") }
        val slice: expr by lazy { getSingle(pyObject, uri, "slice") }
        val ctx: expr_context by lazy { getSingle(pyObject, uri, "ctx") }
    }

    /**
     * ```
     * ast.Starred = class Starred(expr)
     *  |  Starred(expr value, expr_context ctx)
     * ```
     */
    class Starred(pyObject: PyObject, uri: URI) : expr(pyObject, uri) {
        val value: expr by lazy { getSingle(pyObject, uri, "value") }
        val ctx: expr_context by lazy { getSingle(pyObject, uri, "ctx") }
    }

    /**
     * ```
     * ast.Name = class Name(expr)
     *  |  Name(identifier id, expr_context ctx)
     * ```
     */
    class Name(pyObject: PyObject, uri: URI) : expr(pyObject, uri) {
        val id: String by lazy { getSingle(pyObject, uri, "id") }
        val ctx: expr_context by lazy { getSingle(pyObject, uri, "ctx") }
    }

    /**
     * ```
     * ast.List = class List(expr)
     *  |  List(expr* elts, expr_context ctx)
     * ```
     */
    class List(pyObject: PyObject, uri: URI) : expr(pyObject, uri) {
        val elts: kotlin.collections.List<expr> by lazy { getSingle(pyObject, uri, "elts") }
        val ctx: expr_context by lazy { getSingle(pyObject, uri, "ctx") }
    }

    /**
     * ```
     * ast.Tuple = class Tuple(expr)
     *  |  Tuple(expr* elts, expr_context ctx)
     * ```
     */
    class Tuple(pyObject: PyObject, uri: URI) : expr(pyObject, uri) {
        val elts: kotlin.collections.List<expr> by lazy { getSingle(pyObject, uri, "elts") }
        val ctx: expr_context by lazy { getSingle(pyObject, uri, "ctx") }
    }

    /**
     * ```
     * ast.Slice = class Slice(expr)
     *  |  Slice(expr? lower, expr? upper, expr? step)
     * ```
     */
    class Slice(pyObject: PyObject, uri: URI) : expr(pyObject, uri) {
        val lower: expr? by lazy { getSingle(pyObject, uri, "lower") }
        val upper: expr? by lazy { getSingle(pyObject, uri, "upper") }
        val step: expr? by lazy { getSingle(pyObject, uri, "step") }
    }

    /**
     * ```
     * ast.boolop = class boolop(AST)
     *  |  boolop = And | Or
     * ```
     */
    abstract class boolop(pyObject: PyObject, uri: URI) : AST(pyObject, uri)

    /**
     * ```
     * ast.And = class And(boolop)
     *  |  And
     * ```
     */
    class And(pyObject: PyObject, uri: URI) : boolop(pyObject, uri)

    /**
     * ```
     * ast.Or = class Or(boolop)
     *  |  Or
     */
    class Or(pyObject: PyObject, uri: URI) : boolop(pyObject, uri)

    /**
     * ```
     * ast.cmpop = class cmpop(AST)
     *  |  cmpop = Eq | NotEq | Lt | LtE | Gt | GtE | Is | IsNot | In | NotIn
     * ```
     */
    abstract class cmpop(pyObject: PyObject, uri: URI) : AST(pyObject, uri)

    /**
     * ```
     * ast.Eq = class Eq(cmpop)
     *  |  Eq
     * ```
     */
    class Eq(pyObject: PyObject, uri: URI) : cmpop(pyObject, uri)

    /**
     * ```
     * ast.NotEq = class NotEq(cmpop)
     *  |  NotEq
     * ```
     */
    class NotEq(pyObject: PyObject, uri: URI) : cmpop(pyObject, uri)

    /**
     * ```
     * ast.Lt = class Lt(cmpop)
     *  |  Lt
     * ```
     */
    class Lt(pyObject: PyObject, uri: URI) : cmpop(pyObject, uri)

    /**
     * ```
     * ast.LtE = class LtE(cmpop)
     *  |  LtE
     * ```
     */
    class LtE(pyObject: PyObject, uri: URI) : cmpop(pyObject, uri)

    /**
     * ```
     * ast.Gt = class Gt(cmpop)
     *  |  Gt
     * ```
     */
    class Gt(pyObject: PyObject, uri: URI) : cmpop(pyObject, uri)

    /**
     * ```
     * ast.GtE = class GtE(cmpop)
     *  |  GtE
     * ```
     */
    class GtE(pyObject: PyObject, uri: URI) : cmpop(pyObject, uri)

    /**
     * ```
     * ast.Is = class Is(cmpop)
     *  |  Is
     * ```
     */
    class Is(pyObject: PyObject, uri: URI) : cmpop(pyObject, uri)

    /**
     * ```
     * ast.IsNot = class IsNot(cmpop)
     *  |  IsNot
     * ```
     */
    class IsNot(pyObject: PyObject, uri: URI) : cmpop(pyObject, uri)

    /**
     * ```
     * ast.In = class In(cmpop)
     *  |  In
     * ```
     */
    class In(pyObject: PyObject, uri: URI) : cmpop(pyObject, uri)

    /**
     * ```
     * ast.NotIn = class NotIn(cmpop)
     *  |  NotIn
     * ```
     */
    class NotIn(pyObject: PyObject, uri: URI) : cmpop(pyObject, uri)

    /**
     * ```
     * ast.expr_context = class expr_context(AST)
     *  |  expr_context = Load | Store | Del
     * ```
     */
    abstract class expr_context(pyObject: PyObject, uri: URI) : AST(pyObject, uri)

    /**
     * ```
     * ast.Load = class Load(expr_context)
     *  |  Load
     * ```
     */
    class Load(pyObject: PyObject, uri: URI) : expr_context(pyObject, uri)

    /**
     * ```
     * ast.Store = class Store(expr_context)
     *  |  Store
     * ```
     */
    class Store(pyObject: PyObject, uri: URI) : expr_context(pyObject, uri)

    /**
     * ```
     * ast.Del = class Del(expr_context)
     *  |  Del
     * ```
     */
    class Del(pyObject: PyObject, uri: URI) : expr_context(pyObject, uri)

    /**
     * ```
     * ast.operator = class operator(AST)
     *  |  operator = Add | Sub | Mult | MatMult | Div | Mod | Pow | LShift | RShift | BitOr | BitXor | BitAnd | FloorDiv
     * ```
     */
    abstract class operator(pyObject: PyObject, uri: URI) : AST(pyObject, uri)

    /**
     * ```
     * ast.Add = class Add(operator)
     *  |  Add
     * ```
     */
    class Add(pyObject: PyObject, uri: URI) : operator(pyObject, uri)

    /**
     * ```
     * ast.Sub = class Sub(operator)
     *  |  Sub
     * ```
     */
    class Sub(pyObject: PyObject, uri: URI) : operator(pyObject, uri)

    /**
     * ```
     * ast.Mult = class Mult(operator)
     *  |  Mult
     * ```
     */
    class Mult(pyObject: PyObject, uri: URI) : operator(pyObject, uri)

    /**
     * ```
     * ast.MatMult = class MatMult(operator)
     *  |  MatMult
     * ```
     */
    class MatMult(pyObject: PyObject, uri: URI) : operator(pyObject, uri)

    /**
     * ```
     * ast.Div = class Div(operator)
     *  |  Div
     * ```
     */
    class Div(pyObject: PyObject, uri: URI) : operator(pyObject, uri)

    /**
     * ```
     * ast.Mod = class Mod(operator)
     *  |  Mod
     * ```
     */
    class Mod(pyObject: PyObject, uri: URI) : operator(pyObject, uri)

    /**
     * ```
     * ast.Pow = class Pow(operator)
     *  |  Pow
     * ```
     */
    class Pow(pyObject: PyObject, uri: URI) : operator(pyObject, uri)

    /**
     * ```
     * ast.LShift = class LShift(operator)
     *  |  LShift
     * ```
     */
    class LShift(pyObject: PyObject, uri: URI) : operator(pyObject, uri)

    /**
     * ```
     * ast.RShift = class RShift(operator)
     *  |  RShift
     * ```
     */
    class RShift(pyObject: PyObject, uri: URI) : operator(pyObject, uri)

    /**
     * ```
     * ast.BitOr = class BitOr(operator)
     *  |  BitOr
     * ```
     */
    class BitOr(pyObject: PyObject, uri: URI) : operator(pyObject, uri)

    /**
     * ```
     * ast.BitXor = class BitXor(operator)
     *  |  BitXor
     * ```
     */
    class BitXor(pyObject: PyObject, uri: URI) : operator(pyObject, uri)

    /**
     * ```
     * ast.BitAnd = class BitAnd(operator)
     *  |  BitAnd
     * ```
     */
    class BitAnd(pyObject: PyObject, uri: URI) : operator(pyObject, uri)

    /**
     * ```
     * ast.FloorDiv = class FloorDiv(operator)
     *  |  FloorDiv
     * ```
     */
    class FloorDiv(pyObject: PyObject, uri: URI) : operator(pyObject, uri)

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
    abstract class pattern(pyObject: PyObject, uri: URI) : AST(pyObject, uri)

    /**
     * ```
     * ast.MatchValue = class MatchValue(pattern)
     *  |  MatchValue(expr value)
     * ```
     */
    class MatchValue(pyObject: PyObject, uri: URI) : pattern(pyObject, uri) {
        val value: expr by lazy { getSingle(pyObject, uri, "value") }
    }

    /**
     * ```
     * ast.MatchSingleton = class MatchSingleton(pattern)
     *  |  MatchSingleton(constant value)
     * ```
     */
    class MatchSingleton(pyObject: PyObject, uri: URI) : pattern(pyObject, uri) {
        val value: Any by lazy { getSingle(pyObject, uri, "value") }
    }

    /**
     * ```
     * ast.MatchSequence = class MatchSequence(pattern)
     *  |  MatchSequence(pattern* patterns)
     * ```
     */
    class MatchSequence(pyObject: PyObject, uri: URI) : pattern(pyObject, uri) {
        val patterns: kotlin.collections.List<pattern> by lazy {
            getList(pyObject, uri, "patterns")
        }
    }

    /**
     * ```
     * ast.MatchMapping = class MatchMapping(pattern)
     *  |  MatchMapping(expr* keys, pattern* patterns, identifier? rest)
     * ```
     */
    class MatchMapping(pyObject: PyObject, uri: URI) : pattern(pyObject, uri) {
        val key: kotlin.collections.List<expr> by lazy { getList(pyObject, uri, "keys") }
        val patterns: kotlin.collections.List<pattern> by lazy {
            getList(pyObject, uri, "patterns")
        }
        val rest: String? by lazy { getSingle(pyObject, uri, "rest") }
    }

    /**
     * ```
     * ast.MatchClass = class MatchClass(pattern)
     *  |  MatchClass(expr cls, pattern* patterns, identifier* kwd_attrs, pattern* kwd_patterns)
     * ```
     */
    class MatchClass(pyObject: PyObject, uri: URI) : pattern(pyObject, uri) {
        val cls: expr by lazy { getSingle(pyObject, uri, "cls") }
        val patterns: kotlin.collections.List<pattern> by lazy {
            getList(pyObject, uri, "patterns")
        }
        val kwd_attrs: kotlin.collections.List<String> by lazy {
            getList(pyObject, uri, "kwd_attrs")
        }
        val kwd_patterns: kotlin.collections.List<pattern> by lazy {
            getList(pyObject, uri, "kwd_patterns")
        }
    }

    /**
     * ```
     * ast.MatchStar = class MatchStar(pattern)
     *  |  MatchStar(identifier? name)
     * ```
     */
    class MatchStar(pyObject: PyObject, uri: URI) : pattern(pyObject, uri) {
        val name: String? by lazy { getSingle(pyObject, uri, "name") }
    }

    /**
     * ```
     * ast.MatchAs = class MatchAs(pattern)
     *  |  MatchAs(pattern? pattern, identifier? name)
     * ```
     */
    class MatchAs(pyObject: PyObject, uri: URI) : pattern(pyObject, uri) {
        val pattern: pattern? by lazy { getSingle(pyObject, uri, "pattern") }
        val name: String? by lazy { getSingle(pyObject, uri, "name") }
    }

    /**
     * ```
     * ast.MatchOr = class MatchOr(pattern)
     *  |  MatchOr(pattern* patterns)
     * ```
     */
    class MatchOr(pyObject: PyObject, uri: URI) : pattern(pyObject, uri) {
        val patterns: kotlin.collections.List<pattern> by lazy {
            getList(pyObject, uri, "patterns")
        }
    }

    /**
     * ```
     * ast.unaryop = class unaryop(AST)
     *  |  unaryop = Invert | Not | UAdd | USub
     * ```
     */
    abstract class unaryop(pyObject: PyObject, uri: URI) : AST(pyObject, uri)

    /**
     * ```
     * ast.Invert = class Invert(unaryop)
     *  |  Invert
     * ```
     */
    class Invert(pyObject: PyObject, uri: URI) : unaryop(pyObject, uri)

    /**
     * ```
     * ast.Not = class Not(unaryop)
     *  |  Not
     * ```
     */
    class Not(pyObject: PyObject, uri: URI) : unaryop(pyObject, uri)
    /**
     * ```
     * ast.UAdd = class UAdd(unaryop)
     *  |  UAdd
     * ```
     */
    class UAdd(pyObject: PyObject, uri: URI) : unaryop(pyObject, uri)

    /**
     * ```
     * ast.USub = class USub(unaryop)
     *  |  USub
     * ```
     */
    class USub(pyObject: PyObject, uri: URI) : unaryop(pyObject, uri)

    /**
     * ```
     * ast.alias = class alias(AST)
     *  |  alias(identifier name, identifier? asname)
     * ```
     */
    class alias(pyObject: PyObject, uri: URI) : AST(pyObject, uri) {
        val name: String by lazy { getSingle(pyObject, uri, "name") }
        val asname: String? by lazy { getSingle(pyObject, uri, "asname") }
    }

    /**
     * ```
     * ast.arg = class arg(AST)
     *  |  arg(identifier arg, expr? annotation, string? type_comment)
     * ```
     */
    class arg(pyObject: PyObject, uri: URI) : AST(pyObject, uri) {
        val arg: String by lazy { getSingle(pyObject, uri, "arg") }
        val annotation: expr? by lazy { getSingle(pyObject, uri, "annotation") }
        val type_comment: String? by lazy { getSingle(pyObject, uri, "type_comment") }
    }

    /**
     * ```
     * ast.arguments = class arguments(AST)
     *  |  arguments(arg* posonlyargs, arg* args, arg? vararg, arg* kwonlyargs, expr* kw_defaults, arg? kwarg, expr* defaults)
     * ```
     */
    class arguments(pyObject: PyObject, uri: URI) : AST(pyObject, uri) {
        val posonlyargs: kotlin.collections.List<arg> by lazy {
            getList(pyObject, uri, "posonlyargs")
        }
        val args: kotlin.collections.List<arg> by lazy { getList(pyObject, uri, "args") }
        val vararg: arg? by lazy { getSingle(pyObject, uri, "vararg") }
        val kwonlyargs: kotlin.collections.List<arg> by lazy {
            getList(pyObject, uri, "kwonlyargs")
        }
        val kw_defaults: kotlin.collections.List<expr> by lazy {
            getList(pyObject, uri, "kw_defaults")
        }
        val kwarg: arg? by lazy { getSingle(pyObject, uri, "kwarg") }
        val defaults: kotlin.collections.List<expr> by lazy { getList(pyObject, uri, "defaults") }
    }

    /**
     * ```
     * ast.comprehension = class comprehension(AST)
     *  |  comprehension(expr target, expr iter, expr* ifs, int is_async)
     * ```
     */
    class comprehension(pyObject: PyObject, uri: URI) : AST(pyObject, uri) {
        val target: expr by lazy { getSingle(pyObject, uri, "target") }
        val iter: expr by lazy { getSingle(pyObject, uri, "iter") }
        val ifs: kotlin.collections.List<expr> by lazy { getList(pyObject, uri, "ifs") }
        val is_async: Int by lazy {
            getSingle(pyObject, uri, "is_async")
        } // TODO: is this an `Int` in Kotlin?
    }

    /**
     * ```
     * ast.excepthandler = class excepthandler(AST)
     *  |  excepthandler = ExceptHandler(expr? type, identifier? name, stmt* body)
     * ```
     *
     * TODO: excepthandler <-> ExceptHandler
     */
    class excepthandler(pyObject: PyObject, uri: URI) : AST(pyObject, uri) {
        val type: expr by lazy { getSingle(pyObject, uri, "type") }
        val name: String by lazy { getSingle(pyObject, uri, "name") }
        val body: kotlin.collections.List<stmt> by lazy { getList(pyObject, uri, "body") }
    }

    /**
     * ```
     * ast.keyword = class keyword(AST)
     *  |  keyword(identifier? arg, expr value)
     * ```
     */
    class keyword(pyObject: PyObject, uri: URI) : AST(pyObject, uri) {
        val arg: String? by lazy { getSingle(pyObject, uri, "arg") }
        val value: expr by lazy { getSingle(pyObject, uri, "value") }
    }

    /**
     * ```
     * ast.match_case = class match_case(AST)
     *  |  match_case(pattern pattern, expr? guard, stmt* body)
     * ```
     */
    class match_case(pyObject: PyObject, uri: URI) : AST(pyObject, uri) {
        val pattern: pattern by lazy { getSingle(pyObject, uri, "pattern") }
        val guard: expr? by lazy { getSingle(pyObject, uri, "guard") }
        val body: kotlin.collections.List<stmt> by lazy { getList(pyObject, uri, "body") }
    }

    /**
     * ```
     * ast.type_ignore = class type_ignore(AST)
     *  |  type_ignore = TypeIgnore(int lineno, string tag)
     * ```
     *
     * TODO
     */
    class type_ignore(pyObject: PyObject, uri: URI) : AST(pyObject, uri)

    /**
     * ```
     * ast.withitem = class withitem(AST)
     *  |  withitem(expr context_expr, expr? optional_vars)
     * ```
     */
    class withitem(pyObject: PyObject, uri: URI) : AST(pyObject, uri) {
        val context_expr: expr by lazy { getSingle(pyObject, uri, "context_expr") }
        val optional_vars: expr? by lazy { getSingle(pyObject, uri, "optional_vars") }
    }
}

inline fun <reified T> getSingle(pyObject: PyObject, uri: URI, identifier: String): T {
    val ret =
        pyObject.getAttr(identifier).let {
            if (it is PyObject) {
                fromPython(it, uri)
            } else {
                it
            }
        }
    if (ret !is T) {
        TODO("Expected a " + T::class.java + " but received a " + ret::class.java)
    }
    return ret
}

inline fun <reified T> getList(pyObject: PyObject, uri: URI, identifier: String): List<T> {
    val tmp = pyObject.getAttr(identifier) as? ArrayList<*> ?: TODO("Expected a list")
    return tmp.map {
        val item =
            if (it is PyObject) {
                fromPython(it, uri)
            } else {
                it
            }
        if (item !is T) {
            TODO("Expected a " + T::class.java + " but received a " + item::class.java)
        }
        item
    }
}
