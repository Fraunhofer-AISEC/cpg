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
interface Python {

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
    abstract class ASTBASEmod(pyObject: PyObject) : AST(pyObject)

    /**
     * ```
     * ast.Module = class Module(mod)
     *  |  Module(stmt* body, type_ignore* type_ignores)
     * ```
     */
    class ASTModule(pyObject: PyObject) : AST(pyObject) {
        val body: List<ASTBASEstmt> by lazy { "body" of pyObject }

        val type_ignores: List<ASTtype_ignore> by lazy { "type_ignores" of pyObject }
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
    abstract class ASTBASEstmt(pyObject: PyObject) : AST(pyObject), WithPythonLocation

    /**
     * ```
     * ast.FunctionDef = class FunctionDef(stmt)
     *  |  FunctionDef(identifier name, arguments args, stmt* body, expr* decorator_list, expr? returns, string? type_comment)
     * ```
     */
    class ASTFunctionDef(pyObject: PyObject) : ASTBASEstmt(pyObject) {
        val name: String by lazy { "name" of pyObject }

        val args: ASTarguments by lazy { "args" of pyObject }

        val body: List<ASTBASEstmt> by lazy { "body" of pyObject }

        val decorator_list: List<ASTBASEexpr> by lazy { "decorator_list" of pyObject }

        val returns: ASTBASEexpr? by lazy { "returns" of pyObject }

        val type_comment: String? by lazy { "type_comment" of pyObject }
    }

    /**
     * ```
     * ast.AsyncFunctionDef = class AsyncFunctionDef(stmt)
     *  |  AsyncFunctionDef(identifier name, arguments args, stmt* body, expr* decorator_list, expr? returns, string? type_comment)
     * ```
     */
    class ASTAsyncFunctionDef(pyObject: PyObject) : ASTBASEstmt(pyObject) {
        val name: String by lazy { "name" of pyObject }

        val args: ASTarguments by lazy { "args" of pyObject }

        val body: List<ASTBASEstmt> by lazy { "body" of pyObject }

        val decorator_list: List<ASTBASEexpr> by lazy { "decorator_list" of pyObject }

        val returns: ASTBASEexpr? by lazy { "returns" of pyObject }

        val type_comment: String? by lazy { "type_comment" of pyObject }
    }

    /**
     * ```
     * ast.ClassDef = class ClassDef(stmt)
     *  |  ClassDef(identifier name, expr* bases, keyword* keywords, stmt* body, expr* decorator_list)
     * ```
     */
    class ASTClassDef(pyObject: PyObject) : ASTBASEstmt(pyObject) {
        val name: String by lazy { "name" of pyObject }

        val bases: List<ASTBASEexpr> by lazy { "bases" of pyObject }

        val keywords: List<ASTkeyword> by lazy { "keywords" of pyObject }

        val body: List<ASTBASEstmt> by lazy { "body" of pyObject }

        val decorator_list: List<ASTBASEexpr> by lazy { "decorator_list" of pyObject }
    }

    /**
     * ```
     * ast.Return = class Return(stmt)
     *  |  Return(expr? value)
     * ```
     */
    class ASTReturn(pyObject: PyObject) : ASTBASEstmt(pyObject) {
        val value: ASTBASEexpr? by lazy { "value" of pyObject }
    }

    /**
     * ```
     * ast.Delete = class Delete(stmt)
     *  |  Delete(expr* targets)
     * ```
     */
    class ASTDelete(pyObject: PyObject) : ASTBASEstmt(pyObject) {
        val targets: List<ASTBASEexpr> by lazy { "targets" of pyObject }
    }

    /**
     * ```
     * ast.Assign = class Assign(stmt)
     *  |  Assign(expr* targets, expr value, string? type_comment)
     * ```
     */
    class ASTAssign(pyObject: PyObject) : ASTBASEstmt(pyObject) {
        val targets: List<ASTBASEexpr> by lazy { "targets" of pyObject }

        val value: ASTBASEexpr by lazy { "value" of pyObject }

        val type_comment: String? by lazy { "type_comment" of pyObject }
    }

    /**
     * ```
     * ast.AugAssign = class AugAssign(stmt)
     *  |  AugAssign(expr target, operator op, expr value)
     * ```
     */
    class ASTAugAssign(pyObject: PyObject) : ASTBASEstmt(pyObject) {
        val target: ASTBASEexpr by lazy { "target" of pyObject }
        val op: ASTBASEoperator by lazy { "op" of pyObject }
        val value: ASTBASEexpr by lazy { "value" of pyObject }
    }

    /**
     * ```
     * ast.AnnAssign = class AnnAssign(stmt)
     *  |  AnnAssign(expr target, expr annotation, expr? value, int simple)
     * ```
     */
    class ASTAnnAssign(pyObject: PyObject) : ASTBASEstmt(pyObject) {
        val target: ASTBASEexpr by lazy { "target" of pyObject }
        val annotation: ASTBASEexpr by lazy { "annotation" of pyObject }
        val value: ASTBASEexpr? by lazy { "value" of pyObject }
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
    class ASTFor(pyObject: PyObject) : ASTBASEstmt(pyObject) {
        val target: ASTBASEexpr by lazy { "target" of pyObject }
        val iter: ASTBASEexpr by lazy { "iter" of pyObject }
        val body: List<ASTBASEstmt> by lazy { "body" of pyObject }
        val orelse: List<ASTBASEstmt> by lazy { "orelse" of pyObject }
        val type_comment: String? by lazy { "type_comment" of pyObject }
    }

    /**
     * ```
     * ast.AsyncFor = class AsyncFor(stmt)
     *  |  AsyncFor(expr target, expr iter, stmt* body, stmt* orelse, string? type_comment)
     * ```
     */
    class ASTAsyncFor(pyObject: PyObject) : ASTBASEstmt(pyObject) {
        val target: ASTBASEexpr by lazy { "target" of pyObject }
        val iter: ASTBASEexpr by lazy { "iter" of pyObject }
        val body: List<ASTBASEstmt> by lazy { "body" of pyObject }
        val orelse: List<ASTBASEstmt> by lazy { "orelse" of pyObject }
        val type_comment: String? by lazy { "type_comment" of pyObject }
    }

    /**
     * ```
     * ast.While = class While(stmt)
     *  |  While(expr test, stmt* body, stmt* orelse)
     * ```
     */
    class ASTWhile(pyObject: PyObject) : ASTBASEstmt(pyObject) {
        val test: ASTBASEexpr by lazy { "test" of pyObject }
        val body: List<ASTBASEstmt> by lazy { "body" of pyObject }
        val orelse: List<ASTBASEstmt> by lazy { "orelse" of pyObject }
    }

    /**
     * ```
     * ast.If = class If(stmt)
     *  |  If(expr test, stmt* body, stmt* orelse)
     * ```
     */
    class ASTIf(pyObject: PyObject) : ASTBASEstmt(pyObject) {
        val test: ASTBASEexpr by lazy { "test" of pyObject }
        val body: List<ASTBASEstmt> by lazy { "body" of pyObject }
        val orelse: List<ASTBASEstmt> by lazy { "orelse" of pyObject }
    }

    /**
     * ```
     * ast.With = class With(stmt)
     *  |  With(withitem* items, stmt* body, string? type_comment)
     * ```
     */
    class ASTWith(pyObject: PyObject) : ASTBASEstmt(pyObject) {
        val items: ASTwithitem by lazy { "items" of pyObject }
        val body: List<ASTBASEstmt> by lazy { "body" of pyObject }
        val type_comment: String? by lazy { "type_comment" of pyObject }
    }

    /**
     * ```
     * ast.AsyncWith = class AsyncWith(stmt)
     *  |  AsyncWith(withitem* items, stmt* body, string? type_comment)
     * ```
     */
    class ASTAsyncWith(pyObject: PyObject) : ASTBASEstmt(pyObject) {
        val items: ASTwithitem by lazy { "items" of pyObject }
        val body: List<ASTBASEstmt> by lazy { "body" of pyObject }
        val type_comment: String? by lazy { "type_comment" of pyObject }
    }

    /**
     * ```
     * ast.Match = class Match(stmt)
     *  |  Match(expr subject, match_case* cases)
     * ```
     */
    class ASTMatch(pyObject: PyObject) : ASTBASEstmt(pyObject) {
        val subject: ASTBASEexpr by lazy { "subject" of pyObject }
        val cases: List<ASTmatch_case> by lazy { "cases" of pyObject }
    }

    /**
     * ```
     * ast.Raise = class Raise(stmt)
     *  |  Raise(expr? exc, expr? cause)
     * ```
     */
    class ASTRaise(pyObject: PyObject) : ASTBASEstmt(pyObject) {
        val exc: ASTBASEexpr? by lazy { "exc" of pyObject }
        val cause: ASTBASEexpr? by lazy { "cause" of pyObject }
    }

    /**
     * ```
     * ast.Try = class Try(stmt)
     *  |  Try(stmt* body, excepthandler* handlers, stmt* orelse, stmt* finalbody)
     * ```
     */
    class ASTTry(pyObject: PyObject) : ASTBASEstmt(pyObject) {
        val body: List<ASTBASEstmt> by lazy { "body" of pyObject }
        val handlers: List<ASTexcepthandler> by lazy { "handlers" of pyObject }
        val orelse: List<ASTBASEstmt> by lazy { "orelse" of pyObject }
        val stmt: List<ASTBASEstmt> by lazy { "StmtBase" of pyObject }
    }

    /**
     * ```
     * ast.TryStar = class TryStar(stmt)
     *  |  TryStar(stmt* body, excepthandler* handlers, stmt* orelse, stmt* finalbody)
     * ```
     */
    class ASTTryStar(pyObject: PyObject) : ASTBASEstmt(pyObject) {
        val body: List<ASTBASEstmt> by lazy { "body" of pyObject }
        val handlers: List<ASTexcepthandler> by lazy { "handlers" of pyObject }
        val orelse: List<ASTBASEstmt> by lazy { "orelse" of pyObject }
        val finalbody: List<ASTBASEstmt> by lazy { "finalbody" of pyObject }
    }

    /**
     * ```
     * ast.Assert = class Assert(stmt)
     *  |  Assert(expr test, expr? msg)
     * ```
     */
    class ASTAssert(pyObject: PyObject) : ASTBASEstmt(pyObject) {
        val test: ASTBASEexpr by lazy { "test" of pyObject }
        val msg: ASTBASEexpr? by lazy { "msg" of pyObject }
    }

    /**
     * ```
     * ast.Import = class Import(stmt)
     *  |  Import(alias* names)
     * ```
     */
    class ASTImport(pyObject: PyObject) : ASTBASEstmt(pyObject) {
        val names: List<ASTalias> by lazy { "names" of pyObject }
    }

    /**
     * ```
     * ast.ImportFrom = class ImportFrom(stmt)
     *  |  ImportFrom(identifier? module, alias* names, int? level)
     * ```
     */
    class ASTImportFrom(pyObject: PyObject) : ASTBASEstmt(pyObject) {
        val module: String? by lazy { "module" of pyObject }
        val names: List<ASTalias> by lazy { "names" of pyObject }
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
    class ASTGlobal(pyObject: PyObject) : ASTBASEstmt(pyObject) {
        val names: List<String> by lazy { "names" of pyObject }
    }

    /**
     * ```
     * ast.Nonlocal = class Nonlocal(stmt)
     *  |  Nonlocal(identifier* names)
     * ```
     */
    class ASTNonlocal(pyObject: PyObject) : ASTBASEstmt(pyObject) {
        val names: List<String> by lazy { "names" of pyObject }
    }

    /**
     * Represents `ast.Expr` expressions. Note: do not confuse with
     * - [ASTBASEexpr] -> the expression class
     * - [Expression] -> the expression as part of `mod`
     *
     * ```
     * ast.Expr = class Expr(stmt)
     *  |  Expr(expr value)
     * ```
     */
    class ASTExpr(pyObject: PyObject) : ASTBASEstmt(pyObject) {
        val value: ASTBASEexpr by lazy { "value" of pyObject }
    }

    /**
     * ```
     * ast.Pass = class Pass(stmt)
     *  |  Pass
     * ```
     */
    class ASTPass(pyObject: PyObject) : ASTBASEstmt(pyObject)

    /**
     * ```
     * ast.Break = class Break(stmt)
     *  |  Break
     * ```
     */
    class ASTBreak(pyObject: PyObject) : ASTBASEstmt(pyObject)

    /**
     * ```
     * ast.Continue = class Continue(stmt)
     *  |  Continue
     * ```
     */
    class ASTContinue(pyObject: PyObject) : ASTBASEstmt(pyObject)

    /**
     * Represents `ast.expr` expressions. Note: do not confuse with
     * - [ASTExpr] -> the expression statement
     * - [Expression] -> the expression as part of `mod`
     *
     * ast.expr = class expr(AST)
     */
    abstract class ASTBASEexpr(pyObject: PyObject) : AST(pyObject), WithPythonLocation

    /**
     * ```
     * ast.BoolOp = class BoolOp(expr)
     *  |  BoolOp(boolop op, expr* values)
     * ```
     */
    class ASTBoolOp(pyObject: PyObject) : ASTBASEexpr(pyObject) {
        val op: ASTBASEboolop by lazy { "op" of pyObject }
        val values: List<ASTBASEexpr> by lazy { "values" of pyObject }
    }

    /**
     * ```
     * ast.NamedExpr = class NamedExpr(expr)
     *  |  NamedExpr(expr target, expr value)
     * ```
     */
    class ASTNamedExpr(pyObject: PyObject) : ASTBASEexpr(pyObject) {
        val target: ASTBASEexpr by lazy { "target" of pyObject }
        val value: ASTBASEexpr by lazy { "value" of pyObject }
    }

    /**
     * ```
     * ast.BinOp = class BinOp(expr)
     *  |  BinOp(expr left, operator op, expr right)
     * ```
     */
    class ASTBinOp(pyObject: PyObject) : ASTBASEexpr(pyObject) {
        val left: ASTBASEexpr by lazy { "left" of pyObject }
        val op: ASTBASEoperator by lazy { "op" of pyObject }
        val right: ASTBASEexpr by lazy { "right" of pyObject }
    }

    /**
     * ```
     * ast.UnaryOp = class UnaryOp(expr)
     *  |  UnaryOp(unaryop op, expr operand)
     * ```
     */
    class ASTUnaryOp(pyObject: PyObject) : ASTBASEexpr(pyObject) {
        val op: ASTBASEunaryop by lazy { "op" of pyObject }
        val operand: ASTBASEexpr by lazy { "operand" of pyObject }
    }

    /**
     * ```
     * ast.Lambda = class Lambda(expr)
     *  |  Lambda(arguments args, expr body)
     * ```
     */
    class ASTLambda(pyObject: PyObject) : ASTBASEexpr(pyObject) {
        val args: ASTarguments by lazy { "args" of pyObject }
        val body: ASTBASEexpr by lazy { "body" of pyObject }
    }

    /**
     * ```
     * ast.IfExp = class IfExp(expr)
     *  |  IfExp(expr test, expr body, expr orelse)
     * ```
     */
    class ASTIfExp(pyObject: PyObject) : ASTBASEexpr(pyObject) {
        val test: ASTBASEexpr by lazy { "test" of pyObject }
        val body: ASTBASEexpr by lazy { "body" of pyObject }
        val orelse: ASTBASEexpr by lazy { "orelse" of pyObject }
    }

    /**
     * ```
     * ast.Dict = class Dict(expr)
     *  |  Dict(expr* keys, expr* values)
     * ```
     */
    class ASTDict(pyObject: PyObject) : ASTBASEexpr(pyObject) {
        val keys: List<ASTBASEexpr?> by lazy { "keys" of pyObject }
        val values: List<ASTBASEexpr> by lazy { "values" of pyObject }
    }

    /**
     * ```
     * ast.Set = class Set(expr)
     *  |  Set(expr* elts)
     * ```
     */
    class ASTSet(pyObject: PyObject) : ASTBASEexpr(pyObject) {
        val elts: List<ASTBASEexpr> by lazy { "elts" of pyObject }
    }

    /**
     * ```
     * ast.ListComp = class ListComp(expr)
     *  |  ListComp(expr elt, comprehension* generators)
     * ```
     */
    class ASTListComp(pyObject: PyObject) : ASTBASEexpr(pyObject) {
        val elt: ASTBASEexpr by lazy { "elt" of pyObject }
        val generators: List<ASTcomprehension> by lazy { "generators" of pyObject }
    }

    /**
     * ```
     * ast.SetComp = class SetComp(expr)
     *  |  SetComp(expr elt, comprehension* generators)
     * ```
     */
    class ASTSetComp(pyObject: PyObject) : ASTBASEexpr(pyObject) {
        val elt: ASTBASEexpr by lazy { "elt" of pyObject }
        val generators: List<ASTcomprehension> by lazy { "generators" of pyObject }
    }

    /**
     * ```
     * ast.DictComp = class DictComp(expr)
     *  |  DictComp(expr key, expr value, comprehension* generators)
     * ```
     */
    class ASTDictComp(pyObject: PyObject) : ASTBASEexpr(pyObject) {
        val key: ASTBASEexpr by lazy { "key" of pyObject }
        val value: ASTBASEexpr by lazy { "value" of pyObject }
        val generators: List<ASTcomprehension> by lazy { "generators" of pyObject }
    }

    /**
     * ```
     * ast.GeneratorExp = class GeneratorExp(expr)
     *  |  GeneratorExp(expr elt, comprehension* generators)
     * ```
     */
    class ASTGeneratorExp(pyObject: PyObject) : ASTBASEexpr(pyObject) {
        val elt: ASTBASEexpr by lazy { "elt" of pyObject }
        val generators: List<ASTcomprehension> by lazy { "generators" of pyObject }
    }

    /**
     * ```
     * ast.Await = class Await(expr)
     *  |  Await(expr value)
     * ```
     */
    class ASTAwait(pyObject: PyObject) : ASTBASEexpr(pyObject) {
        val value: ASTBASEexpr by lazy { "value" of pyObject }
    }

    /**
     * ```
     * ast.Yield = class Yield(expr)
     *  |  Yield(expr? value)
     * ```
     */
    class ASTYield(pyObject: PyObject) : ASTBASEexpr(pyObject) {
        val value: ASTBASEexpr? by lazy { "value" of pyObject }
    }

    /**
     * ```
     * ast.YieldFrom = class YieldFrom(expr)
     *  |  YieldFrom(expr value)
     * ```
     */
    class ASTYieldFrom(pyObject: PyObject) : ASTBASEexpr(pyObject) {
        val value: ASTBASEexpr by lazy { "value" of pyObject }
    }

    /**
     * ```
     * ast.Compare = class Compare(expr)
     *  |  Compare(expr left, cmpop* ops, expr* comparators)
     * ```
     */
    class ASTCompare(pyObject: PyObject) : ASTBASEexpr(pyObject) {
        val left: ASTBASEexpr by lazy { "left" of pyObject }
        val ops: List<ASTBASEcmpop> by lazy { "ops" of pyObject }
        val comparators: List<ASTBASEexpr> by lazy { "comparators" of pyObject }
    }

    /**
     * ```
     * ast.Call = class Call(expr)
     *  |  Call(expr func, expr* args, keyword* keywords)
     * ```
     */
    class ASTCall(pyObject: PyObject) : ASTBASEexpr(pyObject) {
        val func: ASTBASEexpr by lazy { "func" of pyObject }

        val args: List<ASTBASEexpr> by lazy { "args" of pyObject }

        val keywords: List<ASTkeyword> by lazy { "keywords" of pyObject }
    }

    /**
     * ```
     * ast.FormattedValue = class FormattedValue(expr)
     *  |  FormattedValue(expr value, int conversion, expr? format_spec)
     * ```
     */
    class ASTFormattedValue(pyObject: PyObject) : ASTBASEexpr(pyObject) {
        val value: ASTBASEexpr by lazy { "value" of pyObject }
        val conversion: Int? by lazy { "value" of pyObject } // TODO: int in Kotlin as well?
        val format_spec: ASTBASEexpr? by lazy { "format_spec" of pyObject }
    }

    /**
     * ```
     * ast.JoinedStr = class JoinedStr(expr)
     *  |  JoinedStr(expr* values)
     * ```
     */
    class ASTJoinedStr(pyObject: PyObject) : ASTBASEexpr(pyObject) {
        val values: List<ASTBASEexpr> by lazy { "values" of pyObject }
    }

    /**
     * ```
     * ast.Constant = class Constant(expr)
     *  |  Constant(constant value, string? kind)
     * ```
     */
    class ASTConstant(pyObject: PyObject) : ASTBASEexpr(pyObject) {
        val value: Any by lazy { "value" of pyObject }
        val kind: String? by lazy { "kind" of pyObject }
    }

    /**
     * ```
     * ast.Attribute = class Attribute(expr)
     *  |  Attribute(expr value, identifier attr, expr_context ctx)
     * ```
     */
    class ASTAttribute(pyObject: PyObject) : ASTBASEexpr(pyObject) {
        val value: ASTBASEexpr by lazy { "value" of pyObject }
        val attr: String by lazy { "attr" of pyObject }
        val ctx: ASTBASEexpr_context by lazy { "ctx" of pyObject }
    }

    /**
     * ```
     * ast.Subscript = class Subscript(expr)
     *  |  Subscript(expr value, expr slice, expr_context ctx)
     * ```
     */
    class ASTSubscript(pyObject: PyObject) : ASTBASEexpr(pyObject) {
        val value: ASTBASEexpr by lazy { "value" of pyObject }
        val slice: ASTBASEexpr by lazy { "slice" of pyObject }
        val ctx: ASTBASEexpr_context by lazy { "ctx" of pyObject }
    }

    /**
     * ```
     * ast.Starred = class Starred(expr)
     *  |  Starred(expr value, expr_context ctx)
     * ```
     */
    class ASTStarred(pyObject: PyObject) : ASTBASEexpr(pyObject) {
        val value: ASTBASEexpr by lazy { "value" of pyObject }
        val ctx: ASTBASEexpr_context by lazy { "ctx" of pyObject }
    }

    /**
     * ```
     * ast.Name = class Name(expr)
     *  |  Name(identifier id, expr_context ctx)
     * ```
     */
    class ASTName(pyObject: PyObject) : ASTBASEexpr(pyObject) {
        val id: String by lazy { "id" of pyObject }
        val ctx: ASTBASEexpr_context by lazy { "ctx" of pyObject }
    }

    /**
     * ```
     * ast.List = class List(expr)
     *  |  List(expr* elts, expr_context ctx)
     * ```
     */
    class ASTList(pyObject: PyObject) : ASTBASEexpr(pyObject) {
        val elts: List<ASTBASEexpr> by lazy { "elts" of pyObject }
        val ctx: ASTBASEexpr_context by lazy { "ctx" of pyObject }
    }

    /**
     * ```
     * ast.Tuple = class Tuple(expr)
     *  |  Tuple(expr* elts, expr_context ctx)
     * ```
     */
    class ASTTuple(pyObject: PyObject) : ASTBASEexpr(pyObject) {
        val elts: List<ASTBASEexpr> by lazy { "elts" of pyObject }
        val ctx: ASTBASEexpr_context by lazy { "ctx" of pyObject }
    }

    /**
     * ```
     * ast.Slice = class Slice(expr)
     *  |  Slice(expr? lower, expr? upper, expr? step)
     * ```
     */
    class ASTSlice(pyObject: PyObject) : ASTBASEexpr(pyObject) {
        val lower: ASTBASEexpr? by lazy { "lower" of pyObject }
        val upper: ASTBASEexpr? by lazy { "upper" of pyObject }
        val step: ASTBASEexpr? by lazy { "step" of pyObject }
    }

    /**
     * ```
     * ast.boolop = class boolop(AST)
     *  |  boolop = And | Or
     * ```
     *
     * To avoid conflicts with [ASTBoolOp], we need to rename this class.
     */
    abstract class ASTBASEboolop(pyObject: PyObject) : AST(pyObject)

    /**
     * ```
     * ast.And = class And(boolop)
     *  |  And
     * ```
     */
    class ASTAnd(pyObject: PyObject) : ASTBASEboolop(pyObject)

    /**
     * ```
     * ast.Or = class Or(boolop)
     *  |  Or
     */
    class ASTOr(pyObject: PyObject) : ASTBASEboolop(pyObject)

    /**
     * ```
     * ast.cmpop = class cmpop(AST)
     *  |  cmpop = Eq | NotEq | Lt | LtE | Gt | GtE | Is | IsNot | In | NotIn
     * ```
     */
    abstract class ASTBASEcmpop(pyObject: PyObject) : AST(pyObject)

    /**
     * ```
     * ast.Eq = class Eq(cmpop)
     *  |  Eq
     * ```
     */
    class ASTEq(pyObject: PyObject) : ASTBASEcmpop(pyObject)

    /**
     * ```
     * ast.NotEq = class NotEq(cmpop)
     *  |  NotEq
     * ```
     */
    class ASTNotEq(pyObject: PyObject) : ASTBASEcmpop(pyObject)

    /**
     * ```
     * ast.Lt = class Lt(cmpop)
     *  |  Lt
     * ```
     */
    class ASTLt(pyObject: PyObject) : ASTBASEcmpop(pyObject)

    /**
     * ```
     * ast.LtE = class LtE(cmpop)
     *  |  LtE
     * ```
     */
    class ASTLtE(pyObject: PyObject) : ASTBASEcmpop(pyObject)

    /**
     * ```
     * ast.Gt = class Gt(cmpop)
     *  |  Gt
     * ```
     */
    class ASTGt(pyObject: PyObject) : ASTBASEcmpop(pyObject)

    /**
     * ```
     * ast.GtE = class GtE(cmpop)
     *  |  GtE
     * ```
     */
    class ASTGtE(pyObject: PyObject) : ASTBASEcmpop(pyObject)

    /**
     * ```
     * ast.Is = class Is(cmpop)
     *  |  Is
     * ```
     */
    class ASTIs(pyObject: PyObject) : ASTBASEcmpop(pyObject)

    /**
     * ```
     * ast.IsNot = class IsNot(cmpop)
     *  |  IsNot
     * ```
     */
    class ASTIsNot(pyObject: PyObject) : ASTBASEcmpop(pyObject)

    /**
     * ```
     * ast.In = class In(cmpop)
     *  |  In
     * ```
     */
    class ASTIn(pyObject: PyObject) : ASTBASEcmpop(pyObject)

    /**
     * ```
     * ast.NotIn = class NotIn(cmpop)
     *  |  NotIn
     * ```
     */
    class ASTNotIn(pyObject: PyObject) : ASTBASEcmpop(pyObject)

    /**
     * ```
     * ast.expr_context = class expr_context(AST)
     *  |  expr_context = Load | Store | Del
     * ```
     */
    abstract class ASTBASEexpr_context(pyObject: PyObject) : AST(pyObject)

    /**
     * ```
     * ast.Load = class Load(expr_context)
     *  |  Load
     * ```
     */
    class ASTLoad(pyObject: PyObject) : ASTBASEexpr_context(pyObject)

    /**
     * ```
     * ast.Store = class Store(expr_context)
     *  |  Store
     * ```
     */
    class ASTStore(pyObject: PyObject) : ASTBASEexpr_context(pyObject)

    /**
     * ```
     * ast.Del = class Del(expr_context)
     *  |  Del
     * ```
     */
    class ASTDel(pyObject: PyObject) : ASTBASEexpr_context(pyObject)

    /**
     * ```
     * ast.operator = class operator(AST)
     *  |  operator = Add | Sub | Mult | MatMult | Div | Mod | Pow | LShift | RShift | BitOr | BitXor | BitAnd | FloorDiv
     * ```
     */
    abstract class ASTBASEoperator(pyObject: PyObject) : AST(pyObject)

    /**
     * ```
     * ast.Add = class Add(operator)
     *  |  Add
     * ```
     */
    class ASTAdd(pyObject: PyObject) : ASTBASEoperator(pyObject)

    /**
     * ```
     * ast.Sub = class Sub(operator)
     *  |  Sub
     * ```
     */
    class ASTSub(pyObject: PyObject) : ASTBASEoperator(pyObject)

    /**
     * ```
     * ast.Mult = class Mult(operator)
     *  |  Mult
     * ```
     */
    class ASTMult(pyObject: PyObject) : ASTBASEoperator(pyObject)

    /**
     * ```
     * ast.MatMult = class MatMult(operator)
     *  |  MatMult
     * ```
     */
    class ASTMatMult(pyObject: PyObject) : ASTBASEoperator(pyObject)

    /**
     * ```
     * ast.Div = class Div(operator)
     *  |  Div
     * ```
     */
    class ASTDiv(pyObject: PyObject) : ASTBASEoperator(pyObject)

    /**
     * ```
     * ast.Mod = class Mod(operator)
     *  |  Mod
     * ```
     */
    class ASTMod(pyObject: PyObject) : ASTBASEoperator(pyObject)

    /**
     * ```
     * ast.Pow = class Pow(operator)
     *  |  Pow
     * ```
     */
    class ASTPow(pyObject: PyObject) : ASTBASEoperator(pyObject)

    /**
     * ```
     * ast.LShift = class LShift(operator)
     *  |  LShift
     * ```
     */
    class ASTLShift(pyObject: PyObject) : ASTBASEoperator(pyObject)

    /**
     * ```
     * ast.RShift = class RShift(operator)
     *  |  RShift
     * ```
     */
    class ASTRShift(pyObject: PyObject) : ASTBASEoperator(pyObject)

    /**
     * ```
     * ast.BitOr = class BitOr(operator)
     *  |  BitOr
     * ```
     */
    class ASTBitOr(pyObject: PyObject) : ASTBASEoperator(pyObject)

    /**
     * ```
     * ast.BitXor = class BitXor(operator)
     *  |  BitXor
     * ```
     */
    class ASTBitXor(pyObject: PyObject) : ASTBASEoperator(pyObject)

    /**
     * ```
     * ast.BitAnd = class BitAnd(operator)
     *  |  BitAnd
     * ```
     */
    class ASTBitAnd(pyObject: PyObject) : ASTBASEoperator(pyObject)

    /**
     * ```
     * ast.FloorDiv = class FloorDiv(operator)
     *  |  FloorDiv
     * ```
     */
    class ASTFloorDiv(pyObject: PyObject) : ASTBASEoperator(pyObject)

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
    abstract class ASTBASEpattern(pyObject: PyObject) : AST(pyObject)

    /**
     * ```
     * ast.MatchValue = class MatchValue(pattern)
     *  |  MatchValue(expr value)
     * ```
     */
    class ASTMatchValue(pyObject: PyObject) : ASTBASEpattern(pyObject) {
        val value: ASTBASEexpr by lazy { "value" of pyObject }
    }

    /**
     * ```
     * ast.MatchSingleton = class MatchSingleton(pattern)
     *  |  MatchSingleton(constant value)
     * ```
     */
    class ASTMatchSingleton(pyObject: PyObject) : ASTBASEpattern(pyObject) {
        val value: Any by lazy { "value" of pyObject }
    }

    /**
     * ```
     * ast.MatchSequence = class MatchSequence(pattern)
     *  |  MatchSequence(pattern* patterns)
     * ```
     */
    class ASTMatchSequence(pyObject: PyObject) : ASTBASEpattern(pyObject) {
        val patterns: List<ASTBASEpattern> by lazy { "patterns" of pyObject }
    }

    /**
     * ```
     * ast.MatchMapping = class MatchMapping(pattern)
     *  |  MatchMapping(expr* keys, pattern* patterns, identifier? rest)
     * ```
     */
    class ASTMatchMapping(pyObject: PyObject) : ASTBASEpattern(pyObject) {
        val key: List<ASTBASEexpr> by lazy { "keys" of pyObject }
        val patterns: List<ASTBASEpattern> by lazy { "patterns" of pyObject }
        val rest: String? by lazy { "rest" of pyObject }
    }

    /**
     * ```
     * ast.MatchClass = class MatchClass(pattern)
     *  |  MatchClass(expr cls, pattern* patterns, identifier* kwd_attrs, pattern* kwd_patterns)
     * ```
     */
    class ASTMatchClass(pyObject: PyObject) : ASTBASEpattern(pyObject) {
        val cls: ASTBASEexpr by lazy { "cls" of pyObject }
        val patterns: List<ASTBASEpattern> by lazy { "patterns" of pyObject }
        val kwd_attrs: List<String> by lazy { "kwd_attrs" of pyObject }
        val kwd_patterns: List<ASTBASEpattern> by lazy { "kwd_patterns" of pyObject }
    }

    /**
     * ```
     * ast.MatchStar = class MatchStar(pattern)
     *  |  MatchStar(identifier? name)
     * ```
     */
    class ASTMatchStar(pyObject: PyObject) : ASTBASEpattern(pyObject) {
        val name: String? by lazy { "name" of pyObject }
    }

    /**
     * ```
     * ast.MatchAs = class MatchAs(pattern)
     *  |  MatchAs(pattern? pattern, identifier? name)
     * ```
     */
    class ASTMatchAs(pyObject: PyObject) : ASTBASEpattern(pyObject) {
        val pattern: ASTBASEpattern? by lazy { "pattern" of pyObject }
        val name: String? by lazy { "name" of pyObject }
    }

    /**
     * ```
     * ast.MatchOr = class MatchOr(pattern)
     *  |  MatchOr(pattern* patterns)
     * ```
     */
    class ASTMatchOr(pyObject: PyObject) : ASTBASEpattern(pyObject) {
        val patterns: List<ASTBASEpattern> by lazy { "patterns" of pyObject }
    }

    /**
     * ```
     * ast.unaryop = class unaryop(AST)
     *  |  unaryop = Invert | Not | UAdd | USub
     * ```
     *
     * To avoid conflicts with [ASTUnaryOp], we need to rename this class.
     */
    abstract class ASTBASEunaryop(pyObject: PyObject) : AST(pyObject)

    /**
     * ```
     * ast.Invert = class Invert(unaryop)
     *  |  Invert
     * ```
     */
    class ASTInvert(pyObject: PyObject) : ASTBASEunaryop(pyObject)

    /**
     * ```
     * ast.Not = class Not(unaryop)
     *  |  Not
     * ```
     */
    class ASTNot(pyObject: PyObject) : ASTBASEunaryop(pyObject)
    /**
     * ```
     * ast.UAdd = class UAdd(unaryop)
     *  |  UAdd
     * ```
     */
    class ASTUAdd(pyObject: PyObject) : ASTBASEunaryop(pyObject)

    /**
     * ```
     * ast.USub = class USub(unaryop)
     *  |  USub
     * ```
     */
    class ASTUSub(pyObject: PyObject) : ASTBASEunaryop(pyObject)

    /**
     * ```
     * ast.alias = class alias(AST)
     *  |  alias(identifier name, identifier? asname)
     * ```
     */
    class ASTalias(pyObject: PyObject) : AST(pyObject) {
        val name: String by lazy { "name" of pyObject }
        val asname: String? by lazy { "asname" of pyObject }
    }

    /**
     * ```
     * ast.arg = class arg(AST)
     *  |  arg(identifier arg, expr? annotation, string? type_comment)
     * ```
     */
    class ASTarg(pyObject: PyObject) : AST(pyObject) {
        val arg: String by lazy { "arg" of pyObject }
        val annotation: ASTBASEexpr? by lazy { "annotation" of pyObject }
        val type_comment: String? by lazy { "type_comment" of pyObject }
    }

    /**
     * ```
     * ast.arguments = class arguments(AST)
     *  |  arguments(arg* posonlyargs, arg* args, arg? vararg, arg* kwonlyargs, expr* kw_defaults, arg? kwarg, expr* defaults)
     * ```
     */
    class ASTarguments(pyObject: PyObject) : AST(pyObject) {
        val posonlyargs: List<ASTarg> by lazy { "posonlyargs" of pyObject }
        val args: List<ASTarg> by lazy { "args" of pyObject }
        val vararg: ASTarg? by lazy { "vararg" of pyObject }
        val kwonlyargs: List<ASTarg> by lazy { "kwonlyargs" of pyObject }
        val kw_defaults: List<ASTBASEexpr> by lazy { "kw_defaults" of pyObject }
        val kwarg: ASTarg? by lazy { "kwarg" of pyObject }
        val defaults: List<ASTBASEexpr> by lazy { "defaults" of pyObject }
    }

    /**
     * ```
     * ast.comprehension = class comprehension(AST)
     *  |  comprehension(expr target, expr iter, expr* ifs, int is_async)
     * ```
     */
    class ASTcomprehension(pyObject: PyObject) : AST(pyObject) {
        val target: ASTBASEexpr by lazy { "target" of pyObject }
        val iter: ASTBASEexpr by lazy { "iter" of pyObject }
        val ifs: List<ASTBASEexpr> by lazy { "ifs" of pyObject }
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
    class ASTexcepthandler(pyObject: PyObject) : AST(pyObject) {
        val type: ASTBASEexpr by lazy { "type" of pyObject }
        val name: String by lazy { "name" of pyObject }
        val body: List<ASTBASEstmt> by lazy { "body" of pyObject }
    }

    /**
     * ```
     * ast.keyword = class keyword(AST)
     *  |  keyword(identifier? arg, expr value)
     * ```
     */
    class ASTkeyword(pyObject: PyObject) : AST(pyObject) {
        val arg: String? by lazy { "arg" of pyObject }
        val value: ASTBASEexpr by lazy { "value" of pyObject }
    }

    /**
     * ```
     * ast.match_case = class match_case(AST)
     *  |  match_case(pattern pattern, expr? guard, stmt* body)
     * ```
     */
    class ASTmatch_case(pyObject: PyObject) : AST(pyObject) {
        val pattern: ASTBASEpattern by lazy { "pattern" of pyObject }
        val guard: ASTBASEexpr? by lazy { "guard" of pyObject }
        val body: List<ASTBASEstmt> by lazy { "body" of pyObject }
    }

    /**
     * ```
     * ast.type_ignore = class type_ignore(AST)
     *  |  type_ignore = TypeIgnore(int lineno, string tag)
     * ```
     *
     * TODO
     */
    class ASTtype_ignore(pyObject: PyObject) : AST(pyObject)

    /**
     * ```
     * ast.withitem = class withitem(AST)
     *  |  withitem(expr context_expr, expr? optional_vars)
     * ```
     */
    class ASTwithitem(pyObject: PyObject) : AST(pyObject) {
        val context_expr: ASTBASEexpr by lazy { "context_expr" of pyObject }
        val optional_vars: ASTBASEexpr? by lazy { "optional_vars" of pyObject }
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
