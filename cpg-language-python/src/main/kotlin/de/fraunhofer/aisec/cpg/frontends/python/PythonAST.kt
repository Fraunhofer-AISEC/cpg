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
 */
interface PythonAST {

    /** `ast.stmt` and `ast.expr` nodes have extra location properties as implemented here. */
    interface WithPythonLocation {
        val pyObject: PyObject

        /** Maps to the `lineno` filed from Pyhon's ast. */
        val lineno: Int
            get() {
                return (pyObject.getAttr("lineno") as? Long)?.toInt() ?: TODO()
            }

        val col_offset: Int
            get() {
                return (pyObject.getAttr("col_offset") as? Long)?.toInt() ?: TODO()
            }

        val end_lineno: Int
            get() {
                return (pyObject.getAttr("end_lineno") as? Long)?.toInt() ?: TODO()
            }

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

    abstract class mod(pyObject: PyObject) : AST(pyObject)

    abstract class stmt(pyObject: PyObject) : AST(pyObject), WithPythonLocation

    /**
     * class ast.Expression(body) Represents `ast.Expression` expressions. Note: do not confuse with
     * - [Expr] -> the expression statement
     * - [expr] -> the expression class
     */
    class Expression(pyObject: PyObject) : mod(pyObject) {
        val body: Expression
            get() {
                return fromPython(pyObject.getAttr("body")) as? Expression ?: TODO()
            }
    }

    /** ast.Module = class Module(mod) | Module(stmt* body, type_ignore* type_ignores) */
    class Module(pyObject: PyObject) : AST(pyObject) {
        val body: kotlin.collections.List<stmt>
            get() {
                val listOfPyStmt = pyObject.getAttr("body") as? ArrayList<*> ?: TODO()
                return listOfPyStmt.map { fromPython(it) as? stmt ?: TODO() }
            }

        val type_ignores: kotlin.collections.List<*>
            get() {
                TODO()
            }
    }

    /** ast.FunctionType = class FunctionType(mod) | FunctionType(expr* argtypes, expr returns) */
    class FunctionType(pyObject: PyObject) : mod(pyObject) {
        val argtypes: kotlin.collections.List<Expression>
            get() {
                val listOfPyStmt = pyObject.getAttr("argtypes") as? ArrayList<*> ?: TODO()
                return listOfPyStmt.map { fromPython(it) as? Expression ?: TODO() }
            }

        val returns: Expression
            get() {
                return fromPython(pyObject.getAttr("argtypes")) as? Expression ?: TODO()
            }
    }

    /**
     * ast.FunctionDef = class FunctionDef(stmt) | FunctionDef(identifier name, arguments args,
     * stmt* body, expr* decorator_list, expr? returns, string? type_comment)
     */
    class FunctionDef(pyObject: PyObject) : stmt(pyObject) {
        val name: String
            get() {
                return pyObject.getAttr("name") as? String ?: TODO()
            }

        val args: arguments
            get() {
                return fromPython(pyObject.getAttr("args")) as? arguments ?: TODO()
            }

        val body: kotlin.collections.List<stmt>
            get() {
                val listOfPyStmt = pyObject.getAttr("body") as? ArrayList<*> ?: TODO()
                return listOfPyStmt.map { fromPython(it) as? stmt ?: TODO() }
            }

        val decoratorList: kotlin.collections.List<Expression>
            get() {
                val listOfDecorators = pyObject.getAttr("decorator_list") as? ArrayList<*> ?: TODO()
                return listOfDecorators.map { fromPython(it) as? Expression ?: TODO() }
            }

        val returns: Expression?
            get() {
                return fromPython(pyObject.getAttr("returns")) as? Expression
            }

        val type_comment: String?
            get() {
                TODO()
            }
    }

    /**
     * ast.arguments = class arguments(AST) | arguments(arg* posonlyargs, arg* args, arg? vararg,
     * arg* kwonlyargs, expr* kw_defaults, arg? kwarg, expr* defaults)
     */
    class arguments(pyObject: PyObject) : AST(pyObject) {
        val posonlyargs: kotlin.collections.List<arg>
            get() {
                val listOfPyArgs = pyObject.getAttr("posonlyargs") as? ArrayList<*> ?: TODO()
                return listOfPyArgs.map { fromPython(it) as? arg ?: TODO() }
            }

        val args: kotlin.collections.List<arg>
            get() {
                val listOfPyArgs = pyObject.getAttr("args") as? ArrayList<*> ?: TODO()
                return listOfPyArgs.map { fromPython(it) as? arg ?: TODO() }
            }

        val vararg: arg?
            get() {
                return pyObject.getAttr("vararg") as? arg
            }

        val kwonlyargs: kotlin.collections.List<arg>
            get() {
                val listOfPyArgs = pyObject.getAttr("kwonlyargs") as? ArrayList<*> ?: TODO()
                return listOfPyArgs.map { fromPython(it) as? arg ?: TODO() }
            }

        val kw_defaults: kotlin.collections.List<Expression>
            get() {
                val listOfPyExpr = pyObject.getAttr("kw_defaults") as? ArrayList<*> ?: TODO()
                return listOfPyExpr.map { fromPython(it) as? Expression ?: TODO() }
            }

        val kwarg: arg?
            get() {
                return pyObject.getAttr("kwarg") as? arg
            }

        val defaults: kotlin.collections.List<Expression>
            get() {
                val listOfPyExpr = pyObject.getAttr("defaults") as? ArrayList<*> ?: TODO()
                return listOfPyExpr.map { fromPython(it) as? Expression ?: TODO() }
            }
    }

    /** ast.arg = class arg(AST) | arg(identifier arg, expr? annotation, string? type_comment) */
    class arg(pyObject: PyObject) : AST(pyObject) {
        val arg: String
            get() {
                return pyObject.getAttr("arg") as? String ?: TODO()
            }

        val annotation: Any
            get() {
                TODO()
            }

        val type_comment: String?
            get() {
                return pyObject.getAttr("type_comment") as? String
            }
    }

    /**
     * ast.AsyncFunctionDef = class AsyncFunctionDef(stmt) | AsyncFunctionDef(identifier name,
     * arguments args, stmt* body, expr* decorator_list, expr? returns, string? type_comment)
     */
    class AsyncFunctionDef(pyObject: PyObject) : stmt(pyObject) {
        val name: String
            get() {
                return pyObject.getAttr("name") as? String ?: TODO()
            }

        val args: arguments
            get() {
                return fromPython(pyObject.getAttr("args")) as? arguments ?: TODO()
            }

        val body: kotlin.collections.List<stmt>
            get() {
                val listOfPyStmt = pyObject.getAttr("body") as? ArrayList<*> ?: TODO()
                return listOfPyStmt.map { fromPython(it) as? stmt ?: TODO() }
            }

        val decoratorList: kotlin.collections.List<Expression>
            get() {
                val listOfDecorators = pyObject.getAttr("decorator_list") as? ArrayList<*> ?: TODO()
                return listOfDecorators.map { fromPython(it) as? Expression ?: TODO() }
            }

        val returns: Expression?
            get() {
                val ret = pyObject.getAttr("returns")
                return if (ret != null) (fromPython(ret as PyObject) as? Expression ?: TODO())
                else null
            }

        val type_comment: String?
            get() {
                TODO()
            }
    }

    /**
     * ast.ClassDef = class ClassDef(stmt) | ClassDef(identifier name, expr* bases, keyword*
     * keywords, stmt* body, expr* decorator_list)
     */
    class ClassDef(pyObject: PyObject) : stmt(pyObject) {
        val name: String
            get() {
                return pyObject.getAttr("name") as? String ?: TODO()
            }

        val bases: kotlin.collections.List<AST>
            get() {
                val listOfBases = pyObject.getAttr("bases") as? ArrayList<*> ?: TODO()
                return listOfBases.map { fromPython(it) as? AST ?: TODO() }
            }

        val keywords: kotlin.collections.List<Keyword>
            get() {
                val listOfKeywords = pyObject.getAttr("keywords") as? ArrayList<*> ?: TODO()
                return listOfKeywords.map { fromPython(it) as? Keyword ?: TODO() }
            }

        val body: kotlin.collections.List<stmt>
            get() {
                val listOfPyStmt = pyObject.getAttr("body") as? ArrayList<*> ?: TODO()
                return listOfPyStmt.map { fromPython(it) as? stmt ?: TODO() }
            }

        val decoratorList: kotlin.collections.List<Expression>
            get() {
                val listOfDecorators = pyObject.getAttr("decorator_list") as? ArrayList<*> ?: TODO()
                return listOfDecorators.map { fromPython(it) as? Expression ?: TODO() }
            }
    }

    /** ast.Return = class Return(stmt) | Return(expr? value) */
    class Return(pyObject: PyObject) : stmt(pyObject) {
        val value: expr?
            get() {
                return pyObject.getAttr("value") as? expr
            }
    }

    /*
    ast.Delete = class Delete(stmt)
    |  Delete(expr* targets)
     */
    class Delete(pyObject: PyObject) : stmt(pyObject)

    class Assign(pyObject: PyObject) : stmt(pyObject)

    class AugAssign(pyObject: PyObject) : stmt(pyObject)

    class AnnAssign(pyObject: PyObject) : stmt(pyObject)

    class For(pyObject: PyObject) : stmt(pyObject)

    class AsyncFor(pyObject: PyObject) : stmt(pyObject)

    class While(pyObject: PyObject) : stmt(pyObject)

    class If(pyObject: PyObject) : stmt(pyObject)

    class With(pyObject: PyObject) : stmt(pyObject)

    class AsyncWith(pyObject: PyObject) : stmt(pyObject)

    class Match(pyObject: PyObject) : stmt(pyObject)

    class Raise(pyObject: PyObject) : stmt(pyObject)

    class Try(pyObject: PyObject) : stmt(pyObject)

    class TryStar(pyObject: PyObject) : stmt(pyObject)

    class Assert(pyObject: PyObject) : stmt(pyObject)

    class Import(pyObject: PyObject) : stmt(pyObject)

    class ImportFrom(pyObject: PyObject) : stmt(pyObject)

    class Global(pyObject: PyObject) : stmt(pyObject)

    class Nonlocal(pyObject: PyObject) : stmt(pyObject)

    /**
     * Represents `ast.Expr` expressions. Note: do not confuse with
     * - [expr] -> the expression class
     * - [Expression] -> the expression as part of `mod`
     *
     * ast.Expr = class Expr(stmt) | Expr(expr value)
     */
    class Expr(pyObject: PyObject) : stmt(pyObject)

    class Pass(pyObject: PyObject) : stmt(pyObject)

    class Break(pyObject: PyObject) : stmt(pyObject)

    class Continue(pyObject: PyObject) : stmt(pyObject)

    /**
     * Represents `ast.expr` expressions. Note: do not confuse with
     * - [Expr] -> the expression statement
     * - [Expression] -> the expression as part of `mod`
     *
     * ast.expr = class expr(AST)
     */
    abstract class expr(pyObject: PyObject) : AST(pyObject)

    class BoolOp(pyObject: PyObject) : expr(pyObject)

    class NamedExpr(pyObject: PyObject) : expr(pyObject)

    class BinOp(pyObject: PyObject) : expr(pyObject)

    class UnaryOp(pyObject: PyObject) : expr(pyObject)

    class Lambda(pyObject: PyObject) : expr(pyObject)

    class IfExp(pyObject: PyObject) : expr(pyObject)

    class Dict(pyObject: PyObject) : expr(pyObject)

    class Set(pyObject: PyObject) : expr(pyObject)

    class ListComp(pyObject: PyObject) : expr(pyObject)

    class SetComp(pyObject: PyObject) : expr(pyObject)

    class DictComp(pyObject: PyObject) : expr(pyObject)

    class GeneratorExp(pyObject: PyObject) : expr(pyObject)

    class Await(pyObject: PyObject) : expr(pyObject)

    class Yield(pyObject: PyObject) : expr(pyObject)

    class YieldFrom(pyObject: PyObject) : expr(pyObject)

    class Compare(pyObject: PyObject) : expr(pyObject)

    class Call(pyObject: PyObject) : expr(pyObject)

    class FormattedValue(pyObject: PyObject) : expr(pyObject)

    class JoinedStar(pyObject: PyObject) : expr(pyObject)

    class Constant(pyObject: PyObject) : expr(pyObject)

    class Attribute(pyObject: PyObject) : expr(pyObject)

    class Subscript(pyObject: PyObject) : expr(pyObject)

    class Starred(pyObject: PyObject) : expr(pyObject)

    class Name(pyObject: PyObject) : expr(pyObject)

    class List(pyObject: PyObject) : expr(pyObject)

    class Tuple(pyObject: PyObject) : expr(pyObject)

    class Slice(pyObject: PyObject) : expr(pyObject)

    class Keyword(pyObject: PyObject) : AST(pyObject) {
        // TODO()
    }
}
