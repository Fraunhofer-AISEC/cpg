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
    abstract class AST(val pyObject: PyObject)

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
    abstract class mod(pyObject: PyObject) : AST(pyObject)

    /**
     * ```
     * ast.Module = class Module(mod)
     *  |  Module(stmt* body, type_ignore* type_ignores)
     * ```
     */
    class Module(pyObject: PyObject) : AST(pyObject) {
        val body: kotlin.collections.List<stmt> by lazy { "body" ofList pyObject }

        val type_ignores: kotlin.collections.List<type_ignore> by lazy {
            "type_ignores" ofList pyObject
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
    abstract class stmt(pyObject: PyObject) : AST(pyObject), WithPythonLocation

    /**
     * ```
     * ast.FunctionDef = class FunctionDef(stmt)
     *  |  FunctionDef(identifier name, arguments args, stmt* body, expr* decorator_list, expr? returns, string? type_comment)
     * ```
     */
    class FunctionDef(pyObject: PyObject) : stmt(pyObject) {
        val name: String by lazy { "name" ofSingle pyObject }

        val args: arguments by lazy { "args" ofSingle pyObject }

        val body: kotlin.collections.List<stmt> by lazy { "body" ofList pyObject }

        val decorator_list: kotlin.collections.List<expr> by lazy {
            "decorator_list" ofList pyObject
        }

        val returns: expr? by lazy { "returns" ofSingle pyObject }

        val type_comment: String? by lazy { "type_comment" ofSingle pyObject }
    }

    /**
     * ```
     * ast.AsyncFunctionDef = class AsyncFunctionDef(stmt)
     *  |  AsyncFunctionDef(identifier name, arguments args, stmt* body, expr* decorator_list, expr? returns, string? type_comment)
     * ```
     */
    class AsyncFunctionDef(pyObject: PyObject) : stmt(pyObject) {
        val name: String by lazy { "name" ofSingle pyObject }

        val args: arguments by lazy { "args" ofSingle pyObject }

        val body: kotlin.collections.List<stmt> by lazy { "body" ofList pyObject }

        val decorator_list: kotlin.collections.List<expr> by lazy {
            "decorator_list" ofList pyObject
        }

        val returns: expr? by lazy { "returns" ofSingle pyObject }

        val type_comment: String? by lazy { "type_comment" ofSingle pyObject }
    }

    /**
     * ```
     * ast.ClassDef = class ClassDef(stmt)
     *  |  ClassDef(identifier name, expr* bases, keyword* keywords, stmt* body, expr* decorator_list)
     * ```
     */
    class ClassDef(pyObject: PyObject) : stmt(pyObject) {
        val name: String by lazy { "name" ofSingle pyObject }

        val bases: kotlin.collections.List<expr> by lazy { "bases" ofList pyObject }

        val keywords: kotlin.collections.List<keyword> by lazy { "keywords" ofList pyObject }

        val body: kotlin.collections.List<stmt> by lazy { "body" ofList pyObject }

        val decorator_list: kotlin.collections.List<expr> by lazy {
            "decorator_list" ofList pyObject
        }
    }

    /**
     * ```
     * ast.Return = class Return(stmt)
     *  |  Return(expr? value)
     * ```
     */
    class Return(pyObject: PyObject) : stmt(pyObject) {
        val value: expr? by lazy { "value" ofSingle pyObject }
    }

    /**
     * ```
     * ast.Delete = class Delete(stmt)
     *  |  Delete(expr* targets)
     * ```
     */
    class Delete(pyObject: PyObject) : stmt(pyObject) {
        val targets: kotlin.collections.List<expr> by lazy { "targets" ofList pyObject }
    }

    /**
     * ```
     * ast.Assign = class Assign(stmt)
     *  |  Assign(expr* targets, expr value, string? type_comment)
     * ```
     */
    class Assign(pyObject: PyObject) : stmt(pyObject) {
        val targets: kotlin.collections.List<expr> by lazy { "targets" ofList pyObject }

        val value: expr by lazy { "value" ofSingle pyObject }

        val type_comment: String? by lazy { "type_comment" ofSingle pyObject }
    }

    /**
     * ```
     * ast.AugAssign = class AugAssign(stmt)
     *  |  AugAssign(expr target, operator op, expr value)
     * ```
     */
    class AugAssign(pyObject: PyObject) : stmt(pyObject) {
        val target: expr by lazy { "target" ofSingle pyObject }
        val op: operator by lazy { "op" ofSingle pyObject }
        val value: expr by lazy { "value" ofSingle pyObject }
    }

    /**
     * ```
     * ast.AnnAssign = class AnnAssign(stmt)
     *  |  AnnAssign(expr target, expr annotation, expr? value, int simple)
     * ```
     */
    class AnnAssign(pyObject: PyObject) : stmt(pyObject) {
        val target: expr by lazy { "target" ofSingle pyObject }
        val annotation: expr by lazy { "annotation" ofSingle pyObject }
        val value: expr? by lazy { "value" ofSingle pyObject }
        val simple: Int by lazy {
            "simple" ofSingle pyObject
        } // TODO: is this an `Int` from Kotlins perspective?
    }

    /**
     * ```
     * ast.For = class For(stmt)
     *  |  For(expr target, expr iter, stmt* body, stmt* orelse, string? type_comment)
     * ```
     */
    class For(pyObject: PyObject) : stmt(pyObject) {
        val target: expr by lazy { "target" ofSingle pyObject }
        val iter: expr by lazy { "iter" ofSingle pyObject }
        val body: kotlin.collections.List<stmt> by lazy { "body" ofList pyObject }
        val orelse: kotlin.collections.List<stmt> by lazy { "orelse" ofList pyObject }
        val type_comment: String? by lazy { "type_comment" ofSingle pyObject }
    }

    /**
     * ```
     * ast.AsyncFor = class AsyncFor(stmt)
     *  |  AsyncFor(expr target, expr iter, stmt* body, stmt* orelse, string? type_comment)
     * ```
     */
    class AsyncFor(pyObject: PyObject) : stmt(pyObject) {
        val target: expr by lazy { "target" ofSingle pyObject }
        val iter: expr by lazy { "iter" ofSingle pyObject }
        val body: kotlin.collections.List<stmt> by lazy { "body" ofList pyObject }
        val orelse: kotlin.collections.List<stmt> by lazy { "orelse" ofList pyObject }
        val type_comment: String? by lazy { "type_comment" ofSingle pyObject }
    }

    /**
     * ```
     * ast.While = class While(stmt)
     *  |  While(expr test, stmt* body, stmt* orelse)
     * ```
     */
    class While(pyObject: PyObject) : stmt(pyObject) {
        val test: expr by lazy { "test" ofSingle pyObject }
        val body: kotlin.collections.List<stmt> by lazy { "body" ofList pyObject }
        val orelse: kotlin.collections.List<stmt> by lazy { "orelse" ofList pyObject }
    }

    /**
     * ```
     * ast.If = class If(stmt)
     *  |  If(expr test, stmt* body, stmt* orelse)
     * ```
     */
    class If(pyObject: PyObject) : stmt(pyObject) {
        val test: expr by lazy { "test" ofSingle pyObject }
        val body: kotlin.collections.List<stmt> by lazy { "body" ofList pyObject }
        val orelse: kotlin.collections.List<stmt> by lazy { "orelse" ofList pyObject }
    }

    /**
     * ```
     * ast.With = class With(stmt)
     *  |  With(withitem* items, stmt* body, string? type_comment)
     * ```
     */
    class With(pyObject: PyObject) : stmt(pyObject) {
        val items: withitem by lazy { "items" ofSingle pyObject }
        val body: kotlin.collections.List<stmt> by lazy { "body" ofList pyObject }
        val type_comment: String? by lazy { "type_comment" ofSingle pyObject }
    }

    /**
     * ```
     * ast.AsyncWith = class AsyncWith(stmt)
     *  |  AsyncWith(withitem* items, stmt* body, string? type_comment)
     * ```
     */
    class AsyncWith(pyObject: PyObject) : stmt(pyObject) {
        val items: withitem by lazy { "items" ofSingle pyObject }
        val body: kotlin.collections.List<stmt> by lazy { "body" ofList pyObject }
        val type_comment: String? by lazy { "type_comment" ofSingle pyObject }
    }

    /**
     * ```
     * ast.Match = class Match(stmt)
     *  |  Match(expr subject, match_case* cases)
     * ```
     */
    class Match(pyObject: PyObject) : stmt(pyObject) {
        val subject: expr by lazy { "subject" ofSingle pyObject }
        val cases: kotlin.collections.List<match_case> by lazy { "cases" ofSingle pyObject }
    }

    /**
     * ```
     * ast.Raise = class Raise(stmt)
     *  |  Raise(expr? exc, expr? cause)
     * ```
     */
    class Raise(pyObject: PyObject) : stmt(pyObject) {
        val exc: expr? by lazy { "exc" ofSingle pyObject }
        val cause: expr? by lazy { "cause" ofSingle pyObject }
    }

    /**
     * ```
     * ast.Try = class Try(stmt)
     *  |  Try(stmt* body, excepthandler* handlers, stmt* orelse, stmt* finalbody)
     * ```
     */
    class Try(pyObject: PyObject) : stmt(pyObject) {
        val body: kotlin.collections.List<stmt> by lazy { "body" ofList pyObject }
        val handlers: kotlin.collections.List<excepthandler> by lazy { "handlers" ofList pyObject }
        val orelse: kotlin.collections.List<stmt> by lazy { "orelse" ofList pyObject }
        val stmt: kotlin.collections.List<stmt> by lazy { "stmt" ofList pyObject }
    }

    /**
     * ```
     * ast.TryStar = class TryStar(stmt)
     *  |  TryStar(stmt* body, excepthandler* handlers, stmt* orelse, stmt* finalbody)
     * ```
     */
    class TryStar(pyObject: PyObject) : stmt(pyObject) {
        val body: kotlin.collections.List<stmt> by lazy { "body" ofList pyObject }
        val handlers: kotlin.collections.List<excepthandler> by lazy { "handlers" ofList pyObject }
        val orelse: kotlin.collections.List<stmt> by lazy { "orelse" ofList pyObject }
        val finalbody: kotlin.collections.List<stmt> by lazy { "finalbody" ofList pyObject }
    }

    /**
     * ```
     * ast.Assert = class Assert(stmt)
     *  |  Assert(expr test, expr? msg)
     * ```
     */
    class Assert(pyObject: PyObject) : stmt(pyObject) {
        val test: expr by lazy { "test" ofSingle pyObject }
        val msg: expr? by lazy { "msg" ofSingle pyObject }
    }

    /**
     * ```
     * ast.Import = class Import(stmt)
     *  |  Import(alias* names)
     * ```
     */
    class Import(pyObject: PyObject) : stmt(pyObject) {
        val names: kotlin.collections.List<alias> by lazy { "names" ofList pyObject }
    }

    /**
     * ```
     * ast.ImportFrom = class ImportFrom(stmt)
     *  |  ImportFrom(identifier? module, alias* names, int? level)
     * ```
     */
    class ImportFrom(pyObject: PyObject) : stmt(pyObject) {
        val module: String? by lazy { "module" ofSingle pyObject }
        val names: kotlin.collections.List<alias> by lazy { "names" ofList pyObject }
        val level: Int? by lazy {
            "level" ofSingle pyObject
        } // TODO: is this an `Int` from Kotlins perspective?
    }

    /**
     * ```
     * ast.Global = class Global(stmt)
     *  |  Global(identifier* names)
     * ```
     */
    class Global(pyObject: PyObject) : stmt(pyObject) {
        val names: kotlin.collections.List<String> by lazy { "names" ofList pyObject }
    }

    /**
     * ```
     * ast.Nonlocal = class Nonlocal(stmt)
     *  |  Nonlocal(identifier* names)
     * ```
     */
    class Nonlocal(pyObject: PyObject) : stmt(pyObject) {
        val names: kotlin.collections.List<String> by lazy { "names" ofList pyObject }
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
    class Expr(pyObject: PyObject) : stmt(pyObject) {
        val value: expr by lazy { "value" ofSingle pyObject }
    }

    /**
     * ```
     * ast.Pass = class Pass(stmt)
     *  |  Pass
     * ```
     */
    class Pass(pyObject: PyObject) : stmt(pyObject)

    /**
     * ```
     * ast.Break = class Break(stmt)
     *  |  Break
     * ```
     */
    class Break(pyObject: PyObject) : stmt(pyObject)

    /**
     * ```
     * ast.Continue = class Continue(stmt)
     *  |  Continue
     * ```
     */
    class Continue(pyObject: PyObject) : stmt(pyObject)

    /**
     * Represents `ast.expr` expressions. Note: do not confuse with
     * - [Expr] -> the expression statement
     * - [Expression] -> the expression as part of `mod`
     *
     * ast.expr = class expr(AST)
     */
    abstract class expr(pyObject: PyObject) : AST(pyObject)

    /**
     * ```
     * ast.BoolOp = class BoolOp(expr)
     *  |  BoolOp(boolop op, expr* values)
     * ```
     */
    class BoolOp(pyObject: PyObject) : expr(pyObject) {
        val op: boolop by lazy { "op" ofSingle pyObject }
        val values: kotlin.collections.List<expr> by lazy { "values" ofList pyObject }
    }

    /**
     * ```
     * ast.NamedExpr = class NamedExpr(expr)
     *  |  NamedExpr(expr target, expr value)
     * ```
     */
    class NamedExpr(pyObject: PyObject) : expr(pyObject) {
        val target: expr by lazy { "target" ofSingle pyObject }
        val value: expr by lazy { "value" ofSingle pyObject }
    }

    /**
     * ```
     * ast.BinOp = class BinOp(expr)
     *  |  BinOp(expr left, operator op, expr right)
     * ```
     */
    class BinOp(pyObject: PyObject) : expr(pyObject) {
        val left: expr by lazy { "left" ofSingle pyObject }
        val op: operator by lazy { "op" ofSingle pyObject }
        val right: expr by lazy { "right" ofSingle pyObject }
    }

    /**
     * ```
     * ast.UnaryOp = class UnaryOp(expr)
     *  |  UnaryOp(unaryop op, expr operand)
     * ```
     */
    class UnaryOp(pyObject: PyObject) : expr(pyObject) {
        val op: unaryop by lazy { "op" ofSingle pyObject }
        val operand: expr by lazy { "operand" ofSingle pyObject }
    }

    /**
     * ```
     * ast.Lambda = class Lambda(expr)
     *  |  Lambda(arguments args, expr body)
     * ```
     */
    class Lambda(pyObject: PyObject) : expr(pyObject) {
        val args: arguments by lazy { "args" ofSingle pyObject }
        val body: expr by lazy { "body" ofSingle pyObject }
    }

    /**
     * ```
     * ast.IfExp = class IfExp(expr)
     *  |  IfExp(expr test, expr body, expr orelse)
     * ```
     */
    class IfExp(pyObject: PyObject) : expr(pyObject) {
        val test: expr by lazy { "test" ofSingle pyObject }
        val body: expr by lazy { "body" ofSingle pyObject }
        val orelse: expr by lazy { "orelse" ofSingle pyObject }
    }

    /**
     * ```
     * ast.Dict = class Dict(expr)
     *  |  Dict(expr* keys, expr* values)
     * ```
     */
    class Dict(pyObject: PyObject) : expr(pyObject) {
        val keys: kotlin.collections.List<expr> by lazy { "keys" ofList pyObject }
        val values: kotlin.collections.List<expr> by lazy { "values" ofList pyObject }
    }

    /**
     * ```
     * ast.Set = class Set(expr)
     *  |  Set(expr* elts)
     * ```
     */
    class Set(pyObject: PyObject) : expr(pyObject) {
        val elts: kotlin.collections.List<expr> by lazy { "elts" ofList pyObject }
    }

    /**
     * ```
     * ast.ListComp = class ListComp(expr)
     *  |  ListComp(expr elt, comprehension* generators)
     * ```
     */
    class ListComp(pyObject: PyObject) : expr(pyObject) {
        val elt: expr by lazy { "elt" ofSingle pyObject }
        val generators: kotlin.collections.List<comprehension> by lazy {
            "generators" ofList pyObject
        }
    }

    /**
     * ```
     * ast.SetComp = class SetComp(expr)
     *  |  SetComp(expr elt, comprehension* generators)
     * ```
     */
    class SetComp(pyObject: PyObject) : expr(pyObject) {
        val elt: expr by lazy { "elt" ofSingle pyObject }
        val generators: kotlin.collections.List<comprehension> by lazy {
            "generators" ofList pyObject
        }
    }

    /**
     * ```
     * ast.DictComp = class DictComp(expr)
     *  |  DictComp(expr key, expr value, comprehension* generators)
     * ```
     */
    class DictComp(pyObject: PyObject) : expr(pyObject) {
        val key: expr by lazy { "key" ofSingle pyObject }
        val value: expr by lazy { "value" ofSingle pyObject }
        val generators: kotlin.collections.List<comprehension> by lazy {
            "generators" ofList pyObject
        }
    }

    /**
     * ```
     * ast.GeneratorExp = class GeneratorExp(expr)
     *  |  GeneratorExp(expr elt, comprehension* generators)
     * ```
     */
    class GeneratorExp(pyObject: PyObject) : expr(pyObject) {
        val elt: expr by lazy { "elt" ofSingle pyObject }
        val generators: kotlin.collections.List<comprehension> by lazy {
            "generators" ofList pyObject
        }
    }

    /**
     * ```
     * ast.Await = class Await(expr)
     *  |  Await(expr value)
     * ```
     */
    class Await(pyObject: PyObject) : expr(pyObject) {
        val value: expr by lazy { "value" ofSingle pyObject }
    }

    /**
     * ```
     * ast.Yield = class Yield(expr)
     *  |  Yield(expr? value)
     * ```
     */
    class Yield(pyObject: PyObject) : expr(pyObject) {
        val value: expr? by lazy { "value" ofSingle pyObject }
    }

    /**
     * ```
     * ast.YieldFrom = class YieldFrom(expr)
     *  |  YieldFrom(expr value)
     * ```
     */
    class YieldFrom(pyObject: PyObject) : expr(pyObject) {
        val value: expr by lazy { "value" ofSingle pyObject }
    }

    /**
     * ```
     * ast.Compare = class Compare(expr)
     *  |  Compare(expr left, cmpop* ops, expr* comparators)
     * ```
     */
    class Compare(pyObject: PyObject) : expr(pyObject) {
        val left: expr by lazy { "left" ofSingle pyObject }
        val ops: kotlin.collections.List<cmpop> by lazy { "ops" ofList pyObject }
        val comparators: kotlin.collections.List<expr> by lazy { "comparators" ofList pyObject }
    }

    /**
     * ```
     * ast.Call = class Call(expr)
     *  |  Call(expr func, expr* args, keyword* keywords)
     * ```
     */
    class Call(pyObject: PyObject) : expr(pyObject) {
        val func: expr by lazy { "func" ofSingle pyObject }

        val args: kotlin.collections.List<expr> by lazy { "args" ofList pyObject }

        val keywords: kotlin.collections.List<keyword> by lazy { "keywords" ofList pyObject }
    }

    /**
     * ```
     * ast.FormattedValue = class FormattedValue(expr)
     *  |  FormattedValue(expr value, int conversion, expr? format_spec)
     * ```
     */
    class FormattedValue(pyObject: PyObject) : expr(pyObject) {
        val value: expr by lazy { "value" ofSingle pyObject }
        val conversion: Int? by lazy { "value" ofSingle pyObject } // TODO: int in Kotlin as well?
        val format_spec: expr? by lazy { "format_spec" ofSingle pyObject }
    }

    /**
     * ```
     * ast.JoinedStr = class JoinedStr(expr)
     *  |  JoinedStr(expr* values)
     * ```
     */
    class JoinedStr(pyObject: PyObject) : expr(pyObject) {
        val values: kotlin.collections.List<expr> by lazy { "values" ofList pyObject }
    }

    /**
     * ```
     * ast.Constant = class Constant(expr)
     *  |  Constant(constant value, string? kind)
     * ```
     */
    class Constant(pyObject: PyObject) : expr(pyObject) {
        val value: Any? by lazy { "value" ofSingle pyObject }
        val kind: String? by lazy { "kind" ofSingle pyObject }
    }

    /**
     * ```
     * ast.Attribute = class Attribute(expr)
     *  |  Attribute(expr value, identifier attr, expr_context ctx)
     * ```
     */
    class Attribute(pyObject: PyObject) : expr(pyObject) {
        val value: expr by lazy { "value" ofSingle pyObject }
        val attr: String by lazy { "attr" ofSingle pyObject }
        val ctx: expr_context by lazy { "ctx" ofSingle pyObject }
    }

    /**
     * ```
     * ast.Subscript = class Subscript(expr)
     *  |  Subscript(expr value, expr slice, expr_context ctx)
     * ```
     */
    class Subscript(pyObject: PyObject) : expr(pyObject) {
        val value: expr by lazy { "value" ofSingle pyObject }
        val slice: expr by lazy { "slice" ofSingle pyObject }
        val ctx: expr_context by lazy { "ctx" ofSingle pyObject }
    }

    /**
     * ```
     * ast.Starred = class Starred(expr)
     *  |  Starred(expr value, expr_context ctx)
     * ```
     */
    class Starred(pyObject: PyObject) : expr(pyObject) {
        val value: expr by lazy { "value" ofSingle pyObject }
        val ctx: expr_context by lazy { "ctx" ofSingle pyObject }
    }

    /**
     * ```
     * ast.Name = class Name(expr)
     *  |  Name(identifier id, expr_context ctx)
     * ```
     */
    class Name(pyObject: PyObject) : expr(pyObject) {
        val id: String by lazy { "id" ofSingle pyObject }
        val ctx: expr_context by lazy { "ctx" ofSingle pyObject }
    }

    /**
     * ```
     * ast.List = class List(expr)
     *  |  List(expr* elts, expr_context ctx)
     * ```
     */
    class List(pyObject: PyObject) : expr(pyObject) {
        val elts: kotlin.collections.List<expr> by lazy { "elts" ofSingle pyObject }
        val ctx: expr_context by lazy { "ctx" ofSingle pyObject }
    }

    /**
     * ```
     * ast.Tuple = class Tuple(expr)
     *  |  Tuple(expr* elts, expr_context ctx)
     * ```
     */
    class Tuple(pyObject: PyObject) : expr(pyObject) {
        val elts: kotlin.collections.List<expr> by lazy { "elts" ofSingle pyObject }
        val ctx: expr_context by lazy { "ctx" ofSingle pyObject }
    }

    /**
     * ```
     * ast.Slice = class Slice(expr)
     *  |  Slice(expr? lower, expr? upper, expr? step)
     * ```
     */
    class Slice(pyObject: PyObject) : expr(pyObject) {
        val lower: expr? by lazy { "lower" ofSingle pyObject }
        val upper: expr? by lazy { "upper" ofSingle pyObject }
        val step: expr? by lazy { "step" ofSingle pyObject }
    }

    /**
     * ```
     * ast.boolop = class boolop(AST)
     *  |  boolop = And | Or
     * ```
     */
    abstract class boolop(pyObject: PyObject) : AST(pyObject)

    /**
     * ```
     * ast.And = class And(boolop)
     *  |  And
     * ```
     */
    class And(pyObject: PyObject) : boolop(pyObject)

    /**
     * ```
     * ast.Or = class Or(boolop)
     *  |  Or
     */
    class Or(pyObject: PyObject) : boolop(pyObject)

    /**
     * ```
     * ast.cmpop = class cmpop(AST)
     *  |  cmpop = Eq | NotEq | Lt | LtE | Gt | GtE | Is | IsNot | In | NotIn
     * ```
     */
    abstract class cmpop(pyObject: PyObject) : AST(pyObject)

    /**
     * ```
     * ast.Eq = class Eq(cmpop)
     *  |  Eq
     * ```
     */
    class Eq(pyObject: PyObject) : cmpop(pyObject)

    /**
     * ```
     * ast.NotEq = class NotEq(cmpop)
     *  |  NotEq
     * ```
     */
    class NotEq(pyObject: PyObject) : cmpop(pyObject)

    /**
     * ```
     * ast.Lt = class Lt(cmpop)
     *  |  Lt
     * ```
     */
    class Lt(pyObject: PyObject) : cmpop(pyObject)

    /**
     * ```
     * ast.LtE = class LtE(cmpop)
     *  |  LtE
     * ```
     */
    class LtE(pyObject: PyObject) : cmpop(pyObject)

    /**
     * ```
     * ast.Gt = class Gt(cmpop)
     *  |  Gt
     * ```
     */
    class Gt(pyObject: PyObject) : cmpop(pyObject)

    /**
     * ```
     * ast.GtE = class GtE(cmpop)
     *  |  GtE
     * ```
     */
    class GtE(pyObject: PyObject) : cmpop(pyObject)

    /**
     * ```
     * ast.Is = class Is(cmpop)
     *  |  Is
     * ```
     */
    class Is(pyObject: PyObject) : cmpop(pyObject)

    /**
     * ```
     * ast.IsNot = class IsNot(cmpop)
     *  |  IsNot
     * ```
     */
    class IsNot(pyObject: PyObject) : cmpop(pyObject)

    /**
     * ```
     * ast.In = class In(cmpop)
     *  |  In
     * ```
     */
    class In(pyObject: PyObject) : cmpop(pyObject)

    /**
     * ```
     * ast.NotIn = class NotIn(cmpop)
     *  |  NotIn
     * ```
     */
    class NotIn(pyObject: PyObject) : cmpop(pyObject)

    /**
     * ```
     * ast.expr_context = class expr_context(AST)
     *  |  expr_context = Load | Store | Del
     * ```
     */
    abstract class expr_context(pyObject: PyObject) : AST(pyObject)

    /**
     * ```
     * ast.Load = class Load(expr_context)
     *  |  Load
     * ```
     */
    class Load(pyObject: PyObject) : expr_context(pyObject)

    /**
     * ```
     * ast.Store = class Store(expr_context)
     *  |  Store
     * ```
     */
    class Store(pyObject: PyObject) : expr_context(pyObject)

    /**
     * ```
     * ast.Del = class Del(expr_context)
     *  |  Del
     * ```
     */
    class Del(pyObject: PyObject) : expr_context(pyObject)

    /**
     * ```
     * ast.operator = class operator(AST)
     *  |  operator = Add | Sub | Mult | MatMult | Div | Mod | Pow | LShift | RShift | BitOr | BitXor | BitAnd | FloorDiv
     * ```
     */
    abstract class operator(pyObject: PyObject) : AST(pyObject)

    /**
     * ```
     * ast.Add = class Add(operator)
     *  |  Add
     * ```
     */
    class Add(pyObject: PyObject) : operator(pyObject)

    /**
     * ```
     * ast.Sub = class Sub(operator)
     *  |  Sub
     * ```
     */
    class Sub(pyObject: PyObject) : operator(pyObject)

    /**
     * ```
     * ast.Mult = class Mult(operator)
     *  |  Mult
     * ```
     */
    class Mult(pyObject: PyObject) : operator(pyObject)

    /**
     * ```
     * ast.MatMult = class MatMult(operator)
     *  |  MatMult
     * ```
     */
    class MatMult(pyObject: PyObject) : operator(pyObject)

    /**
     * ```
     * ast.Div = class Div(operator)
     *  |  Div
     * ```
     */
    class Div(pyObject: PyObject) : operator(pyObject)

    /**
     * ```
     * ast.Mod = class Mod(operator)
     *  |  Mod
     * ```
     */
    class Mod(pyObject: PyObject) : operator(pyObject)

    /**
     * ```
     * ast.Pow = class Pow(operator)
     *  |  Pow
     * ```
     */
    class Pow(pyObject: PyObject) : operator(pyObject)

    /**
     * ```
     * ast.LShift = class LShift(operator)
     *  |  LShift
     * ```
     */
    class LShift(pyObject: PyObject) : operator(pyObject)

    /**
     * ```
     * ast.RShift = class RShift(operator)
     *  |  RShift
     * ```
     */
    class RShift(pyObject: PyObject) : operator(pyObject)

    /**
     * ```
     * ast.BitOr = class BitOr(operator)
     *  |  BitOr
     * ```
     */
    class BitOr(pyObject: PyObject) : operator(pyObject)

    /**
     * ```
     * ast.BitXor = class BitXor(operator)
     *  |  BitXor
     * ```
     */
    class BitXor(pyObject: PyObject) : operator(pyObject)

    /**
     * ```
     * ast.BitAnd = class BitAnd(operator)
     *  |  BitAnd
     * ```
     */
    class BitAnd(pyObject: PyObject) : operator(pyObject)

    /**
     * ```
     * ast.FloorDiv = class FloorDiv(operator)
     *  |  FloorDiv
     * ```
     */
    class FloorDiv(pyObject: PyObject) : operator(pyObject)

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
    abstract class pattern(pyObject: PyObject) : AST(pyObject)

    /**
     * ```
     * ast.MatchValue = class MatchValue(pattern)
     *  |  MatchValue(expr value)
     * ```
     */
    class MatchValue(pyObject: PyObject) : pattern(pyObject) {
        val value: expr by lazy { "value" ofSingle pyObject }
    }

    /**
     * ```
     * ast.MatchSingleton = class MatchSingleton(pattern)
     *  |  MatchSingleton(constant value)
     * ```
     */
    class MatchSingleton(pyObject: PyObject) : pattern(pyObject) {
        val value: Any by lazy { "value" ofSingle pyObject }
    }

    /**
     * ```
     * ast.MatchSequence = class MatchSequence(pattern)
     *  |  MatchSequence(pattern* patterns)
     * ```
     */
    class MatchSequence(pyObject: PyObject) : pattern(pyObject) {
        val patterns: kotlin.collections.List<pattern> by lazy { "patterns" ofList pyObject }
    }

    /**
     * ```
     * ast.MatchMapping = class MatchMapping(pattern)
     *  |  MatchMapping(expr* keys, pattern* patterns, identifier? rest)
     * ```
     */
    class MatchMapping(pyObject: PyObject) : pattern(pyObject) {
        val key: kotlin.collections.List<expr> by lazy { "keys" ofList pyObject }
        val patterns: kotlin.collections.List<pattern> by lazy { "patterns" ofList pyObject }
        val rest: String? by lazy { "rest" ofSingle pyObject }
    }

    /**
     * ```
     * ast.MatchClass = class MatchClass(pattern)
     *  |  MatchClass(expr cls, pattern* patterns, identifier* kwd_attrs, pattern* kwd_patterns)
     * ```
     */
    class MatchClass(pyObject: PyObject) : pattern(pyObject) {
        val cls: expr by lazy { "cls" ofSingle pyObject }
        val patterns: kotlin.collections.List<pattern> by lazy { "patterns" ofList pyObject }
        val kwd_attrs: kotlin.collections.List<String> by lazy { "kwd_attrs" ofList pyObject }
        val kwd_patterns: kotlin.collections.List<pattern> by lazy {
            "kwd_patterns" ofList pyObject
        }
    }

    /**
     * ```
     * ast.MatchStar = class MatchStar(pattern)
     *  |  MatchStar(identifier? name)
     * ```
     */
    class MatchStar(pyObject: PyObject) : pattern(pyObject) {
        val name: String? by lazy { "name" ofSingle pyObject }
    }

    /**
     * ```
     * ast.MatchAs = class MatchAs(pattern)
     *  |  MatchAs(pattern? pattern, identifier? name)
     * ```
     */
    class MatchAs(pyObject: PyObject) : pattern(pyObject) {
        val pattern: pattern? by lazy { "pattern" ofSingle pyObject }
        val name: String? by lazy { "name" ofSingle pyObject }
    }

    /**
     * ```
     * ast.MatchOr = class MatchOr(pattern)
     *  |  MatchOr(pattern* patterns)
     * ```
     */
    class MatchOr(pyObject: PyObject) : pattern(pyObject) {
        val patterns: kotlin.collections.List<pattern> by lazy { "patterns" ofList pyObject }
    }

    /**
     * ```
     * ast.unaryop = class unaryop(AST)
     *  |  unaryop = Invert | Not | UAdd | USub
     * ```
     */
    abstract class unaryop(pyObject: PyObject) : AST(pyObject)

    /**
     * ```
     * ast.Invert = class Invert(unaryop)
     *  |  Invert
     * ```
     */
    class Invert(pyObject: PyObject) : unaryop(pyObject)

    /**
     * ```
     * ast.Not = class Not(unaryop)
     *  |  Not
     * ```
     */
    class Not(pyObject: PyObject) : unaryop(pyObject)
    /**
     * ```
     * ast.UAdd = class UAdd(unaryop)
     *  |  UAdd
     * ```
     */
    class UAdd(pyObject: PyObject) : unaryop(pyObject)

    /**
     * ```
     * ast.USub = class USub(unaryop)
     *  |  USub
     * ```
     */
    class USub(pyObject: PyObject) : unaryop(pyObject)

    /**
     * ```
     * ast.alias = class alias(AST)
     *  |  alias(identifier name, identifier? asname)
     * ```
     */
    class alias(pyObject: PyObject) : AST(pyObject) {
        val name: String by lazy { "name" ofSingle pyObject }
        val asname: String? by lazy { "asname" ofSingle pyObject }
    }

    /**
     * ```
     * ast.arg = class arg(AST)
     *  |  arg(identifier arg, expr? annotation, string? type_comment)
     * ```
     */
    class arg(pyObject: PyObject) : AST(pyObject) {
        val arg: String by lazy { "arg" ofSingle pyObject }
        val annotation: expr? by lazy { "annotation" ofSingle pyObject }
        val type_comment: String? by lazy { "type_comment" ofSingle pyObject }
    }

    /**
     * ```
     * ast.arguments = class arguments(AST)
     *  |  arguments(arg* posonlyargs, arg* args, arg? vararg, arg* kwonlyargs, expr* kw_defaults, arg? kwarg, expr* defaults)
     * ```
     */
    class arguments(pyObject: PyObject) : AST(pyObject) {
        val posonlyargs: kotlin.collections.List<arg> by lazy { "posonlyargs" ofList pyObject }
        val args: kotlin.collections.List<arg> by lazy { "args" ofList pyObject }
        val vararg: arg? by lazy { "vararg" ofSingle pyObject }
        val kwonlyargs: kotlin.collections.List<arg> by lazy { "kwonlyargs" ofList pyObject }
        val kw_defaults: kotlin.collections.List<expr> by lazy { "kw_defaults" ofList pyObject }
        val kwarg: arg? by lazy { "kwarg" ofSingle pyObject }
        val defaults: kotlin.collections.List<expr> by lazy { "defaults" ofList pyObject }
    }

    /**
     * ```
     * ast.comprehension = class comprehension(AST)
     *  |  comprehension(expr target, expr iter, expr* ifs, int is_async)
     * ```
     */
    class comprehension(pyObject: PyObject) : AST(pyObject) {
        val target: expr by lazy { "target" ofSingle pyObject }
        val iter: expr by lazy { "iter" ofSingle pyObject }
        val ifs: kotlin.collections.List<expr> by lazy { "ifs" ofList pyObject }
        val is_async: Int by lazy {
            "is_async" ofSingle pyObject
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
    class excepthandler(pyObject: PyObject) : AST(pyObject) {
        val type: expr by lazy { "type" ofSingle pyObject }
        val name: String by lazy { "name" ofSingle pyObject }
        val body: kotlin.collections.List<stmt> by lazy { "body" ofList pyObject }
    }

    /**
     * ```
     * ast.keyword = class keyword(AST)
     *  |  keyword(identifier? arg, expr value)
     * ```
     */
    class keyword(pyObject: PyObject) : AST(pyObject) {
        val arg: String? by lazy { "arg" ofSingle pyObject }
        val value: expr by lazy { "value" ofSingle pyObject }
    }

    /**
     * ```
     * ast.match_case = class match_case(AST)
     *  |  match_case(pattern pattern, expr? guard, stmt* body)
     * ```
     */
    class match_case(pyObject: PyObject) : AST(pyObject) {
        val pattern: pattern by lazy { "pattern" ofSingle pyObject }
        val guard: expr? by lazy { "guard" ofSingle pyObject }
        val body: kotlin.collections.List<stmt> by lazy { "body" ofList pyObject }
    }

    /**
     * ```
     * ast.type_ignore = class type_ignore(AST)
     *  |  type_ignore = TypeIgnore(int lineno, string tag)
     * ```
     *
     * TODO
     */
    class type_ignore(pyObject: PyObject) : AST(pyObject)

    /**
     * ```
     * ast.withitem = class withitem(AST)
     *  |  withitem(expr context_expr, expr? optional_vars)
     * ```
     */
    class withitem(pyObject: PyObject) : AST(pyObject) {
        val context_expr: expr by lazy { "context_expr" ofSingle pyObject }
        val optional_vars: expr? by lazy { "optional_vars" ofSingle pyObject }
    }
}

private inline infix fun <reified T> String.ofSingle(pyObject: PyObject): T {
    val ret =
        pyObject.getAttr(this).let {
            if (it is PyObject) {
                fromPython(it)
            } else {
                it
            }
        }
    if (ret !is T) {
        TODO("Expected a " + T::class.java + " but received a " + ret::class.java)
    }
    return ret
}

private inline infix fun <reified T> String.ofList(pyObject: PyObject): List<T> {
    val tmp = pyObject.getAttr(this) as? ArrayList<*> ?: TODO("Expected a list")
    return tmp.map {
        val item =
            if (it is PyObject) {
                fromPython(it)
            } else {
                it
            }
        if (item !is T) {
            TODO("Expected a " + T::class.java + " but received a " + item::class.java)
        }
        item
    }
}
