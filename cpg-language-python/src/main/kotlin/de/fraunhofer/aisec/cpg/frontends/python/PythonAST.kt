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

        /** Maps to the `lineno` filed from Python's ast. */
        val lineno: Int
            get() {
                return (pyObject.getAttr("lineno") as? Long)?.toInt() ?: TODO()
            }

        /** Maps to the `col_offset` filed from Python's ast. */
        val col_offset: Int
            get() {
                return (pyObject.getAttr("col_offset") as? Long)?.toInt() ?: TODO()
            }

        /** Maps to the `end_lineno` filed from Python's ast. */
        val end_lineno: Int
            get() {
                return (pyObject.getAttr("end_lineno") as? Long)?.toInt() ?: TODO()
            }

        /** Maps to the `end_col_offset` filed from Python's ast. */
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
        val body: List<StmtBase> by lazy { "body" of pyObject }

        val type_ignores: List<type_ignore> by lazy { "type_ignores" of pyObject }
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
    abstract class StmtBase(pyObject: PyObject) : AST(pyObject), WithPythonLocation

    /**
     * ```
     * ast.FunctionDef = class FunctionDef(stmt)
     *  |  FunctionDef(identifier name, arguments args, stmt* body, expr* decorator_list, expr? returns, string? type_comment)
     * ```
     */
    class FunctionDef(pyObject: PyObject) : StmtBase(pyObject) {
        val name: String by lazy { "name" of pyObject }

        val args: arguments by lazy { "args" of pyObject }

        val body: List<StmtBase> by lazy { "body" of pyObject }

        val decorator_list: List<ExprBase> by lazy { "decorator_list" of pyObject }

        val returns: ExprBase? by lazy { "returns" of pyObject }

        val type_comment: String? by lazy { "type_comment" of pyObject }
    }

    /**
     * ```
     * ast.AsyncFunctionDef = class AsyncFunctionDef(stmt)
     *  |  AsyncFunctionDef(identifier name, arguments args, stmt* body, expr* decorator_list, expr? returns, string? type_comment)
     * ```
     */
    class AsyncFunctionDef(pyObject: PyObject) : StmtBase(pyObject) {
        val name: String by lazy { "name" of pyObject }

        val args: arguments by lazy { "args" of pyObject }

        val body: List<StmtBase> by lazy { "body" of pyObject }

        val decorator_list: List<ExprBase> by lazy { "decorator_list" of pyObject }

        val returns: ExprBase? by lazy { "returns" of pyObject }

        val type_comment: String? by lazy { "type_comment" of pyObject }
    }

    /**
     * ```
     * ast.ClassDef = class ClassDef(stmt)
     *  |  ClassDef(identifier name, expr* bases, keyword* keywords, stmt* body, expr* decorator_list)
     * ```
     */
    class ClassDef(pyObject: PyObject) : StmtBase(pyObject) {
        val name: String by lazy { "name" of pyObject }

        val bases: List<ExprBase> by lazy { "bases" of pyObject }

        val keywords: List<keyword> by lazy { "keywords" of pyObject }

        val body: List<StmtBase> by lazy { "body" of pyObject }

        val decorator_list: List<ExprBase> by lazy { "decorator_list" of pyObject }
    }

    /**
     * ```
     * ast.Return = class Return(stmt)
     *  |  Return(expr? value)
     * ```
     */
    class Return(pyObject: PyObject) : StmtBase(pyObject) {
        val value: ExprBase? by lazy { "value" of pyObject }
    }

    /**
     * ```
     * ast.Delete = class Delete(stmt)
     *  |  Delete(expr* targets)
     * ```
     */
    class Delete(pyObject: PyObject) : StmtBase(pyObject) {
        val targets: List<ExprBase> by lazy { "targets" of pyObject }
    }

    /**
     * ```
     * ast.Assign = class Assign(stmt)
     *  |  Assign(expr* targets, expr value, string? type_comment)
     * ```
     */
    class Assign(pyObject: PyObject) : StmtBase(pyObject) {
        val targets: List<ExprBase> by lazy { "targets" of pyObject }

        val value: ExprBase by lazy { "value" of pyObject }

        val type_comment: String? by lazy { "type_comment" of pyObject }
    }

    /**
     * ```
     * ast.AugAssign = class AugAssign(stmt)
     *  |  AugAssign(expr target, operator op, expr value)
     * ```
     */
    class AugAssign(pyObject: PyObject) : StmtBase(pyObject) {
        val target: ExprBase by lazy { "target" of pyObject }
        val op: OperatorBase by lazy { "op" of pyObject }
        val value: ExprBase by lazy { "value" of pyObject }
    }

    /**
     * ```
     * ast.AnnAssign = class AnnAssign(stmt)
     *  |  AnnAssign(expr target, expr annotation, expr? value, int simple)
     * ```
     */
    class AnnAssign(pyObject: PyObject) : StmtBase(pyObject) {
        val target: ExprBase by lazy { "target" of pyObject }
        val annotation: ExprBase by lazy { "annotation" of pyObject }
        val value: ExprBase? by lazy { "value" of pyObject }
        val simple: Int by lazy {
            "simple" of pyObject
        } // TODO: is this an `Int` from Kotlins perspective?
    }

    /**
     * ```
     * ast.For = class For(stmt)
     *  |  For(expr target, expr iter, stmt* body, stmt* orelse, string? type_comment)
     * ```
     */
    class For(pyObject: PyObject) : StmtBase(pyObject) {
        val target: ExprBase by lazy { "target" of pyObject }
        val iter: ExprBase by lazy { "iter" of pyObject }
        val body: List<StmtBase> by lazy { "body" of pyObject }
        val orelse: List<StmtBase> by lazy { "orelse" of pyObject }
        val type_comment: String? by lazy { "type_comment" of pyObject }
    }

    /**
     * ```
     * ast.AsyncFor = class AsyncFor(stmt)
     *  |  AsyncFor(expr target, expr iter, stmt* body, stmt* orelse, string? type_comment)
     * ```
     */
    class AsyncFor(pyObject: PyObject) : StmtBase(pyObject) {
        val target: ExprBase by lazy { "target" of pyObject }
        val iter: ExprBase by lazy { "iter" of pyObject }
        val body: List<StmtBase> by lazy { "body" of pyObject }
        val orelse: List<StmtBase> by lazy { "orelse" of pyObject }
        val type_comment: String? by lazy { "type_comment" of pyObject }
    }

    /**
     * ```
     * ast.While = class While(stmt)
     *  |  While(expr test, stmt* body, stmt* orelse)
     * ```
     */
    class While(pyObject: PyObject) : StmtBase(pyObject) {
        val test: ExprBase by lazy { "test" of pyObject }
        val body: List<StmtBase> by lazy { "body" of pyObject }
        val orelse: List<StmtBase> by lazy { "orelse" of pyObject }
    }

    /**
     * ```
     * ast.If = class If(stmt)
     *  |  If(expr test, stmt* body, stmt* orelse)
     * ```
     */
    class If(pyObject: PyObject) : StmtBase(pyObject) {
        val test: ExprBase by lazy { "test" of pyObject }
        val body: List<StmtBase> by lazy { "body" of pyObject }
        val orelse: List<StmtBase> by lazy { "orelse" of pyObject }
    }

    /**
     * ```
     * ast.With = class With(stmt)
     *  |  With(withitem* items, stmt* body, string? type_comment)
     * ```
     */
    class With(pyObject: PyObject) : StmtBase(pyObject) {
        val items: withitem by lazy { "items" of pyObject }
        val body: List<StmtBase> by lazy { "body" of pyObject }
        val type_comment: String? by lazy { "type_comment" of pyObject }
    }

    /**
     * ```
     * ast.AsyncWith = class AsyncWith(stmt)
     *  |  AsyncWith(withitem* items, stmt* body, string? type_comment)
     * ```
     */
    class AsyncWith(pyObject: PyObject) : StmtBase(pyObject) {
        val items: withitem by lazy { "items" of pyObject }
        val body: List<StmtBase> by lazy { "body" of pyObject }
        val type_comment: String? by lazy { "type_comment" of pyObject }
    }

    /**
     * ```
     * ast.Match = class Match(stmt)
     *  |  Match(expr subject, match_case* cases)
     * ```
     */
    class Match(pyObject: PyObject) : StmtBase(pyObject) {
        val subject: ExprBase by lazy { "subject" of pyObject }
        val cases: List<match_case> by lazy { "cases" of pyObject }
    }

    /**
     * ```
     * ast.Raise = class Raise(stmt)
     *  |  Raise(expr? exc, expr? cause)
     * ```
     */
    class Raise(pyObject: PyObject) : StmtBase(pyObject) {
        val exc: ExprBase? by lazy { "exc" of pyObject }
        val cause: ExprBase? by lazy { "cause" of pyObject }
    }

    /**
     * ```
     * ast.Try = class Try(stmt)
     *  |  Try(stmt* body, excepthandler* handlers, stmt* orelse, stmt* finalbody)
     * ```
     */
    class Try(pyObject: PyObject) : StmtBase(pyObject) {
        val body: List<StmtBase> by lazy { "body" of pyObject }
        val handlers: List<excepthandler> by lazy { "handlers" of pyObject }
        val orelse: List<StmtBase> by lazy { "orelse" of pyObject }
        val stmt: List<StmtBase> by lazy { "StmtBase" of pyObject }
    }

    /**
     * ```
     * ast.TryStar = class TryStar(stmt)
     *  |  TryStar(stmt* body, excepthandler* handlers, stmt* orelse, stmt* finalbody)
     * ```
     */
    class TryStar(pyObject: PyObject) : StmtBase(pyObject) {
        val body: List<StmtBase> by lazy { "body" of pyObject }
        val handlers: List<excepthandler> by lazy { "handlers" of pyObject }
        val orelse: List<StmtBase> by lazy { "orelse" of pyObject }
        val finalbody: List<StmtBase> by lazy { "finalbody" of pyObject }
    }

    /**
     * ```
     * ast.Assert = class Assert(stmt)
     *  |  Assert(expr test, expr? msg)
     * ```
     */
    class Assert(pyObject: PyObject) : StmtBase(pyObject) {
        val test: ExprBase by lazy { "test" of pyObject }
        val msg: ExprBase? by lazy { "msg" of pyObject }
    }

    /**
     * ```
     * ast.Import = class Import(stmt)
     *  |  Import(alias* names)
     * ```
     */
    class Import(pyObject: PyObject) : StmtBase(pyObject) {
        val names: List<alias> by lazy { "names" of pyObject }
    }

    /**
     * ```
     * ast.ImportFrom = class ImportFrom(stmt)
     *  |  ImportFrom(identifier? module, alias* names, int? level)
     * ```
     */
    class ImportFrom(pyObject: PyObject) : StmtBase(pyObject) {
        val module: String? by lazy { "module" of pyObject }
        val names: List<alias> by lazy { "names" of pyObject }
        val level: Int? by lazy {
            "level" of pyObject
        } // TODO: is this an `Int` from Kotlins perspective?
    }

    /**
     * ```
     * ast.Global = class Global(stmt)
     *  |  Global(identifier* names)
     * ```
     */
    class Global(pyObject: PyObject) : StmtBase(pyObject) {
        val names: List<String> by lazy { "names" of pyObject }
    }

    /**
     * ```
     * ast.Nonlocal = class Nonlocal(stmt)
     *  |  Nonlocal(identifier* names)
     * ```
     */
    class Nonlocal(pyObject: PyObject) : StmtBase(pyObject) {
        val names: List<String> by lazy { "names" of pyObject }
    }

    /**
     * Represents `ast.Expr` expressions. Note: do not confuse with
     * - [ExprBase] -> the expression class
     * - [Expression] -> the expression as part of `mod`
     *
     * ```
     * ast.Expr = class Expr(stmt)
     *  |  Expr(expr value)
     * ```
     */
    class Expr(pyObject: PyObject) : StmtBase(pyObject) {
        val value: ExprBase by lazy { "value" of pyObject }
    }

    /**
     * ```
     * ast.Pass = class Pass(stmt)
     *  |  Pass
     * ```
     */
    class Pass(pyObject: PyObject) : StmtBase(pyObject)

    /**
     * ```
     * ast.Break = class Break(stmt)
     *  |  Break
     * ```
     */
    class Break(pyObject: PyObject) : StmtBase(pyObject)

    /**
     * ```
     * ast.Continue = class Continue(stmt)
     *  |  Continue
     * ```
     */
    class Continue(pyObject: PyObject) : StmtBase(pyObject)

    /**
     * Represents `ast.expr` expressions. Note: do not confuse with
     * - [Expr] -> the expression statement
     * - [Expression] -> the expression as part of `mod`
     *
     * ast.expr = class expr(AST)
     */
    abstract class ExprBase(pyObject: PyObject) : AST(pyObject), WithPythonLocation

    /**
     * ```
     * ast.BoolOp = class BoolOp(expr)
     *  |  BoolOp(boolop op, expr* values)
     * ```
     */
    class BoolOp(pyObject: PyObject) : ExprBase(pyObject) {
        val op: BoolOpBase by lazy { "op" of pyObject }
        val values: List<ExprBase> by lazy { "values" of pyObject }
    }

    /**
     * ```
     * ast.NamedExpr = class NamedExpr(expr)
     *  |  NamedExpr(expr target, expr value)
     * ```
     */
    class NamedExpr(pyObject: PyObject) : ExprBase(pyObject) {
        val target: ExprBase by lazy { "target" of pyObject }
        val value: ExprBase by lazy { "value" of pyObject }
    }

    /**
     * ```
     * ast.BinOp = class BinOp(expr)
     *  |  BinOp(expr left, operator op, expr right)
     * ```
     */
    class BinOp(pyObject: PyObject) : ExprBase(pyObject) {
        val left: ExprBase by lazy { "left" of pyObject }
        val op: OperatorBase by lazy { "op" of pyObject }
        val right: ExprBase by lazy { "right" of pyObject }
    }

    /**
     * ```
     * ast.UnaryOp = class UnaryOp(expr)
     *  |  UnaryOp(unaryop op, expr operand)
     * ```
     */
    class UnaryOp(pyObject: PyObject) : ExprBase(pyObject) {
        val op: UnaryOpBase by lazy { "op" of pyObject }
        val operand: ExprBase by lazy { "operand" of pyObject }
    }

    /**
     * ```
     * ast.Lambda = class Lambda(expr)
     *  |  Lambda(arguments args, expr body)
     * ```
     */
    class Lambda(pyObject: PyObject) : ExprBase(pyObject) {
        val args: arguments by lazy { "args" of pyObject }
        val body: ExprBase by lazy { "body" of pyObject }
    }

    /**
     * ```
     * ast.IfExp = class IfExp(expr)
     *  |  IfExp(expr test, expr body, expr orelse)
     * ```
     */
    class IfExp(pyObject: PyObject) : ExprBase(pyObject) {
        val test: ExprBase by lazy { "test" of pyObject }
        val body: ExprBase by lazy { "body" of pyObject }
        val orelse: ExprBase by lazy { "orelse" of pyObject }
    }

    /**
     * ```
     * ast.Dict = class Dict(expr)
     *  |  Dict(expr* keys, expr* values)
     * ```
     */
    class Dict(pyObject: PyObject) : ExprBase(pyObject) {
        val keys: List<ExprBase?> by lazy { "keys" of pyObject }
        val values: List<ExprBase> by lazy { "values" of pyObject }
    }

    /**
     * ```
     * ast.Set = class Set(expr)
     *  |  Set(expr* elts)
     * ```
     */
    class Set(pyObject: PyObject) : ExprBase(pyObject) {
        val elts: List<ExprBase> by lazy { "elts" of pyObject }
    }

    /**
     * ```
     * ast.ListComp = class ListComp(expr)
     *  |  ListComp(expr elt, comprehension* generators)
     * ```
     */
    class ListComp(pyObject: PyObject) : ExprBase(pyObject) {
        val elt: ExprBase by lazy { "elt" of pyObject }
        val generators: List<comprehension> by lazy { "generators" of pyObject }
    }

    /**
     * ```
     * ast.SetComp = class SetComp(expr)
     *  |  SetComp(expr elt, comprehension* generators)
     * ```
     */
    class SetComp(pyObject: PyObject) : ExprBase(pyObject) {
        val elt: ExprBase by lazy { "elt" of pyObject }
        val generators: List<comprehension> by lazy { "generators" of pyObject }
    }

    /**
     * ```
     * ast.DictComp = class DictComp(expr)
     *  |  DictComp(expr key, expr value, comprehension* generators)
     * ```
     */
    class DictComp(pyObject: PyObject) : ExprBase(pyObject) {
        val key: ExprBase by lazy { "key" of pyObject }
        val value: ExprBase by lazy { "value" of pyObject }
        val generators: List<comprehension> by lazy { "generators" of pyObject }
    }

    /**
     * ```
     * ast.GeneratorExp = class GeneratorExp(expr)
     *  |  GeneratorExp(expr elt, comprehension* generators)
     * ```
     */
    class GeneratorExp(pyObject: PyObject) : ExprBase(pyObject) {
        val elt: ExprBase by lazy { "elt" of pyObject }
        val generators: List<comprehension> by lazy { "generators" of pyObject }
    }

    /**
     * ```
     * ast.Await = class Await(expr)
     *  |  Await(expr value)
     * ```
     */
    class Await(pyObject: PyObject) : ExprBase(pyObject) {
        val value: ExprBase by lazy { "value" of pyObject }
    }

    /**
     * ```
     * ast.Yield = class Yield(expr)
     *  |  Yield(expr? value)
     * ```
     */
    class Yield(pyObject: PyObject) : ExprBase(pyObject) {
        val value: ExprBase? by lazy { "value" of pyObject }
    }

    /**
     * ```
     * ast.YieldFrom = class YieldFrom(expr)
     *  |  YieldFrom(expr value)
     * ```
     */
    class YieldFrom(pyObject: PyObject) : ExprBase(pyObject) {
        val value: ExprBase by lazy { "value" of pyObject }
    }

    /**
     * ```
     * ast.Compare = class Compare(expr)
     *  |  Compare(expr left, cmpop* ops, expr* comparators)
     * ```
     */
    class Compare(pyObject: PyObject) : ExprBase(pyObject) {
        val left: ExprBase by lazy { "left" of pyObject }
        val ops: List<cmpop> by lazy { "ops" of pyObject }
        val comparators: List<ExprBase> by lazy { "comparators" of pyObject }
    }

    /**
     * ```
     * ast.Call = class Call(expr)
     *  |  Call(expr func, expr* args, keyword* keywords)
     * ```
     */
    class Call(pyObject: PyObject) : ExprBase(pyObject) {
        val func: ExprBase by lazy { "func" of pyObject }

        val args: List<ExprBase> by lazy { "args" of pyObject }

        val keywords: List<keyword> by lazy { "keywords" of pyObject }
    }

    /**
     * ```
     * ast.FormattedValue = class FormattedValue(expr)
     *  |  FormattedValue(expr value, int conversion, expr? format_spec)
     * ```
     */
    class FormattedValue(pyObject: PyObject) : ExprBase(pyObject) {
        val value: ExprBase by lazy { "value" of pyObject }
        val conversion: Int? by lazy { "value" of pyObject } // TODO: int in Kotlin as well?
        val format_spec: ExprBase? by lazy { "format_spec" of pyObject }
    }

    /**
     * ```
     * ast.JoinedStr = class JoinedStr(expr)
     *  |  JoinedStr(expr* values)
     * ```
     */
    class JoinedStr(pyObject: PyObject) : ExprBase(pyObject) {
        val values: List<ExprBase> by lazy { "values" of pyObject }
    }

    /**
     * ```
     * ast.Constant = class Constant(expr)
     *  |  Constant(constant value, string? kind)
     * ```
     */
    class Constant(pyObject: PyObject) : ExprBase(pyObject) {
        val value: Any by lazy { "value" of pyObject }
        val kind: String? by lazy { "kind" of pyObject }
    }

    /**
     * ```
     * ast.Attribute = class Attribute(expr)
     *  |  Attribute(expr value, identifier attr, expr_context ctx)
     * ```
     */
    class Attribute(pyObject: PyObject) : ExprBase(pyObject) {
        val value: ExprBase by lazy { "value" of pyObject }
        val attr: String by lazy { "attr" of pyObject }
        val ctx: expr_context by lazy { "ctx" of pyObject }
    }

    /**
     * ```
     * ast.Subscript = class Subscript(expr)
     *  |  Subscript(expr value, expr slice, expr_context ctx)
     * ```
     */
    class Subscript(pyObject: PyObject) : ExprBase(pyObject) {
        val value: ExprBase by lazy { "value" of pyObject }
        val slice: ExprBase by lazy { "slice" of pyObject }
        val ctx: expr_context by lazy { "ctx" of pyObject }
    }

    /**
     * ```
     * ast.Starred = class Starred(expr)
     *  |  Starred(expr value, expr_context ctx)
     * ```
     */
    class Starred(pyObject: PyObject) : ExprBase(pyObject) {
        val value: ExprBase by lazy { "value" of pyObject }
        val ctx: expr_context by lazy { "ctx" of pyObject }
    }

    /**
     * ```
     * ast.Name = class Name(expr)
     *  |  Name(identifier id, expr_context ctx)
     * ```
     */
    class Name(pyObject: PyObject) : ExprBase(pyObject) {
        val id: String by lazy { "id" of pyObject }
        val ctx: expr_context by lazy { "ctx" of pyObject }
    }

    /**
     * ```
     * ast.List = class List(expr)
     *  |  List(expr* elts, expr_context ctx)
     * ```
     */
    class PyList(pyObject: PyObject) : ExprBase(pyObject) {
        val elts: List<ExprBase> by lazy { "elts" of pyObject }
        val ctx: expr_context by lazy { "ctx" of pyObject }
    }

    /**
     * ```
     * ast.Tuple = class Tuple(expr)
     *  |  Tuple(expr* elts, expr_context ctx)
     * ```
     */
    class Tuple(pyObject: PyObject) : ExprBase(pyObject) {
        val elts: List<ExprBase> by lazy { "elts" of pyObject }
        val ctx: expr_context by lazy { "ctx" of pyObject }
    }

    /**
     * ```
     * ast.Slice = class Slice(expr)
     *  |  Slice(expr? lower, expr? upper, expr? step)
     * ```
     */
    class Slice(pyObject: PyObject) : ExprBase(pyObject) {
        val lower: ExprBase? by lazy { "lower" of pyObject }
        val upper: ExprBase? by lazy { "upper" of pyObject }
        val step: ExprBase? by lazy { "step" of pyObject }
    }

    /**
     * ```
     * ast.boolop = class boolop(AST)
     *  |  boolop = And | Or
     * ```
     *
     * To avoid conflicts with [BoolOp], we need to rename this class.
     */
    abstract class BoolOpBase(pyObject: PyObject) : AST(pyObject)

    /**
     * ```
     * ast.And = class And(boolop)
     *  |  And
     * ```
     */
    class And(pyObject: PyObject) : BoolOpBase(pyObject)

    /**
     * ```
     * ast.Or = class Or(boolop)
     *  |  Or
     */
    class Or(pyObject: PyObject) : BoolOpBase(pyObject)

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
    abstract class OperatorBase(pyObject: PyObject) : AST(pyObject)

    /**
     * ```
     * ast.Add = class Add(operator)
     *  |  Add
     * ```
     */
    class Add(pyObject: PyObject) : OperatorBase(pyObject)

    /**
     * ```
     * ast.Sub = class Sub(operator)
     *  |  Sub
     * ```
     */
    class Sub(pyObject: PyObject) : OperatorBase(pyObject)

    /**
     * ```
     * ast.Mult = class Mult(operator)
     *  |  Mult
     * ```
     */
    class Mult(pyObject: PyObject) : OperatorBase(pyObject)

    /**
     * ```
     * ast.MatMult = class MatMult(operator)
     *  |  MatMult
     * ```
     */
    class MatMult(pyObject: PyObject) : OperatorBase(pyObject)

    /**
     * ```
     * ast.Div = class Div(operator)
     *  |  Div
     * ```
     */
    class Div(pyObject: PyObject) : OperatorBase(pyObject)

    /**
     * ```
     * ast.Mod = class Mod(operator)
     *  |  Mod
     * ```
     */
    class Mod(pyObject: PyObject) : OperatorBase(pyObject)

    /**
     * ```
     * ast.Pow = class Pow(operator)
     *  |  Pow
     * ```
     */
    class Pow(pyObject: PyObject) : OperatorBase(pyObject)

    /**
     * ```
     * ast.LShift = class LShift(operator)
     *  |  LShift
     * ```
     */
    class LShift(pyObject: PyObject) : OperatorBase(pyObject)

    /**
     * ```
     * ast.RShift = class RShift(operator)
     *  |  RShift
     * ```
     */
    class RShift(pyObject: PyObject) : OperatorBase(pyObject)

    /**
     * ```
     * ast.BitOr = class BitOr(operator)
     *  |  BitOr
     * ```
     */
    class BitOr(pyObject: PyObject) : OperatorBase(pyObject)

    /**
     * ```
     * ast.BitXor = class BitXor(operator)
     *  |  BitXor
     * ```
     */
    class BitXor(pyObject: PyObject) : OperatorBase(pyObject)

    /**
     * ```
     * ast.BitAnd = class BitAnd(operator)
     *  |  BitAnd
     * ```
     */
    class BitAnd(pyObject: PyObject) : OperatorBase(pyObject)

    /**
     * ```
     * ast.FloorDiv = class FloorDiv(operator)
     *  |  FloorDiv
     * ```
     */
    class FloorDiv(pyObject: PyObject) : OperatorBase(pyObject)

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
        val value: ExprBase by lazy { "value" of pyObject }
    }

    /**
     * ```
     * ast.MatchSingleton = class MatchSingleton(pattern)
     *  |  MatchSingleton(constant value)
     * ```
     */
    class MatchSingleton(pyObject: PyObject) : pattern(pyObject) {
        val value: Any by lazy { "value" of pyObject }
    }

    /**
     * ```
     * ast.MatchSequence = class MatchSequence(pattern)
     *  |  MatchSequence(pattern* patterns)
     * ```
     */
    class MatchSequence(pyObject: PyObject) : pattern(pyObject) {
        val patterns: List<pattern> by lazy { "patterns" of pyObject }
    }

    /**
     * ```
     * ast.MatchMapping = class MatchMapping(pattern)
     *  |  MatchMapping(expr* keys, pattern* patterns, identifier? rest)
     * ```
     */
    class MatchMapping(pyObject: PyObject) : pattern(pyObject) {
        val key: List<ExprBase> by lazy { "keys" of pyObject }
        val patterns: List<pattern> by lazy { "patterns" of pyObject }
        val rest: String? by lazy { "rest" of pyObject }
    }

    /**
     * ```
     * ast.MatchClass = class MatchClass(pattern)
     *  |  MatchClass(expr cls, pattern* patterns, identifier* kwd_attrs, pattern* kwd_patterns)
     * ```
     */
    class MatchClass(pyObject: PyObject) : pattern(pyObject) {
        val cls: ExprBase by lazy { "cls" of pyObject }
        val patterns: List<pattern> by lazy { "patterns" of pyObject }
        val kwd_attrs: List<String> by lazy { "kwd_attrs" of pyObject }
        val kwd_patterns: List<pattern> by lazy { "kwd_patterns" of pyObject }
    }

    /**
     * ```
     * ast.MatchStar = class MatchStar(pattern)
     *  |  MatchStar(identifier? name)
     * ```
     */
    class MatchStar(pyObject: PyObject) : pattern(pyObject) {
        val name: String? by lazy { "name" of pyObject }
    }

    /**
     * ```
     * ast.MatchAs = class MatchAs(pattern)
     *  |  MatchAs(pattern? pattern, identifier? name)
     * ```
     */
    class MatchAs(pyObject: PyObject) : pattern(pyObject) {
        val pattern: pattern? by lazy { "pattern" of pyObject }
        val name: String? by lazy { "name" of pyObject }
    }

    /**
     * ```
     * ast.MatchOr = class MatchOr(pattern)
     *  |  MatchOr(pattern* patterns)
     * ```
     */
    class MatchOr(pyObject: PyObject) : pattern(pyObject) {
        val patterns: List<pattern> by lazy { "patterns" of pyObject }
    }

    /**
     * ```
     * ast.unaryop = class unaryop(AST)
     *  |  unaryop = Invert | Not | UAdd | USub
     * ```
     *
     * To avoid conflicts with [UnaryOp], we need to rename this class.
     */
    abstract class UnaryOpBase(pyObject: PyObject) : AST(pyObject)

    /**
     * ```
     * ast.Invert = class Invert(unaryop)
     *  |  Invert
     * ```
     */
    class Invert(pyObject: PyObject) : UnaryOpBase(pyObject)

    /**
     * ```
     * ast.Not = class Not(unaryop)
     *  |  Not
     * ```
     */
    class Not(pyObject: PyObject) : UnaryOpBase(pyObject)
    /**
     * ```
     * ast.UAdd = class UAdd(unaryop)
     *  |  UAdd
     * ```
     */
    class UAdd(pyObject: PyObject) : UnaryOpBase(pyObject)

    /**
     * ```
     * ast.USub = class USub(unaryop)
     *  |  USub
     * ```
     */
    class USub(pyObject: PyObject) : UnaryOpBase(pyObject)

    /**
     * ```
     * ast.alias = class alias(AST)
     *  |  alias(identifier name, identifier? asname)
     * ```
     */
    class alias(pyObject: PyObject) : AST(pyObject) {
        val name: String by lazy { "name" of pyObject }
        val asname: String? by lazy { "asname" of pyObject }
    }

    /**
     * ```
     * ast.arg = class arg(AST)
     *  |  arg(identifier arg, expr? annotation, string? type_comment)
     * ```
     */
    class arg(pyObject: PyObject) : AST(pyObject) {
        val arg: String by lazy { "arg" of pyObject }
        val annotation: ExprBase? by lazy { "annotation" of pyObject }
        val type_comment: String? by lazy { "type_comment" of pyObject }
    }

    /**
     * ```
     * ast.arguments = class arguments(AST)
     *  |  arguments(arg* posonlyargs, arg* args, arg? vararg, arg* kwonlyargs, expr* kw_defaults, arg? kwarg, expr* defaults)
     * ```
     */
    class arguments(pyObject: PyObject) : AST(pyObject) {
        val posonlyargs: List<arg> by lazy { "posonlyargs" of pyObject }
        val args: List<arg> by lazy { "args" of pyObject }
        val vararg: arg? by lazy { "vararg" of pyObject }
        val kwonlyargs: List<arg> by lazy { "kwonlyargs" of pyObject }
        val kw_defaults: List<ExprBase> by lazy { "kw_defaults" of pyObject }
        val kwarg: arg? by lazy { "kwarg" of pyObject }
        val defaults: List<ExprBase> by lazy { "defaults" of pyObject }
    }

    /**
     * ```
     * ast.comprehension = class comprehension(AST)
     *  |  comprehension(expr target, expr iter, expr* ifs, int is_async)
     * ```
     */
    class comprehension(pyObject: PyObject) : AST(pyObject) {
        val target: ExprBase by lazy { "target" of pyObject }
        val iter: ExprBase by lazy { "iter" of pyObject }
        val ifs: List<ExprBase> by lazy { "ifs" of pyObject }
        val is_async: Int by lazy { "is_async" of pyObject } // TODO: is this an `Int` in Kotlin?
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
        val type: ExprBase by lazy { "type" of pyObject }
        val name: String by lazy { "name" of pyObject }
        val body: List<StmtBase> by lazy { "body" of pyObject }
    }

    /**
     * ```
     * ast.keyword = class keyword(AST)
     *  |  keyword(identifier? arg, expr value)
     * ```
     */
    class keyword(pyObject: PyObject) : AST(pyObject) {
        val arg: String? by lazy { "arg" of pyObject }
        val value: ExprBase by lazy { "value" of pyObject }
    }

    /**
     * ```
     * ast.match_case = class match_case(AST)
     *  |  match_case(pattern pattern, expr? guard, stmt* body)
     * ```
     */
    class match_case(pyObject: PyObject) : AST(pyObject) {
        val pattern: pattern by lazy { "pattern" of pyObject }
        val guard: ExprBase? by lazy { "guard" of pyObject }
        val body: List<StmtBase> by lazy { "body" of pyObject }
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
        val context_expr: ExprBase by lazy { "context_expr" of pyObject }
        val optional_vars: ExprBase? by lazy { "optional_vars" of pyObject }
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
