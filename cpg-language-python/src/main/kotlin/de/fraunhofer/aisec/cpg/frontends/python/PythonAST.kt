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

interface PythonAST {
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
    abstract class Node(val pyObject: PyObject)

    abstract class Mod(pyObject: PyObject) : Node(pyObject)

    abstract class Statement(pyObject: PyObject) : Node(pyObject), WithPythonLocation

    /**
     * class ast.Expression(body) Represents `ast.Expression` expressions. Note: do not confuse with
     * - [Expr] -> the expression statement
     * - [expr] -> the expression class
     */
    open class Expression(pyObject: PyObject) : Node(pyObject), WithPythonLocation {
        val body: Expression
            get() {
                return fromPython(pyObject.getAttr("body")) as? Expression ?: TODO()
            }
    }

    /*
    class ast.Module(body, type_ignores)
     */
    class Module(pyObject: PyObject) : Node(pyObject) {
        val body: kotlin.collections.List<Statement>
            get() {
                val listOfPyStmt = pyObject.getAttr("body") as? ArrayList<*> ?: TODO()
                return listOfPyStmt.map { fromPython(it) as? Statement ?: TODO() }
            }

        val type_ignores: kotlin.collections.List<*>
            get() {
                TODO()
            }
    }

    /*
    class ast.Interactive(body)
     */
    class Interactive(pyObject: PyObject) : Node(pyObject) {
        val body: kotlin.collections.List<Statement>
            get() {
                val listOfPyStmt = pyObject.getAttr("body") as? ArrayList<*> ?: TODO()
                return listOfPyStmt.map { fromPython(it) as? Statement ?: TODO() }
            }
    }

    class ModExpression(pyObject: PyObject) : Mod(pyObject) {
        val body: Expression
            get() {
                return fromPython(pyObject.getAttr("body")) as? Expression ?: TODO()
            }
    }

    /*
    class ast.FunctionType(argtypes, returns)
     */
    class FunctionType(pyObject: PyObject) : Mod(pyObject) {
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

    /*
    class ast.FunctionDef(name, args, body, decorator_list, returns, type_comment)
    */
    class FunctionDef(pyObject: PyObject) : Statement(pyObject) {
        val name: String
            get() {
                return pyObject.getAttr("name") as? String ?: TODO()
            }

        val args: Arguments
            get() {
                return fromPython(pyObject.getAttr("args")) as? Arguments ?: TODO()
            }

        val body: kotlin.collections.List<Statement>
            get() {
                val listOfPyStmt = pyObject.getAttr("body") as? ArrayList<*> ?: TODO()
                return listOfPyStmt.map {
                    fromPython(it) as? Statement ?: TODO("Failed for ${it.toString()}")
                }
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

    /*
    class ast.arguments(posonlyargs, args, vararg, kwonlyargs, kw_defaults, kwarg, defaults)

    arguments = (arg* posonlyargs, arg* args, arg? vararg, arg* kwonlyargs,
                 expr* kw_defaults, arg? kwarg, expr* defaults)
     */
    class Arguments(pyObject: PyObject) : Node(pyObject) {
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

    /*
    class ast.arg(arg, annotation, type_comment)
     */
    class arg(pyObject: PyObject) : Node(pyObject) {
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

    /*
    class ast.AsyncFunctionDef(name, args, body, decorator_list, returns, type_comment)
    */
    class AsyncFunctionDef(pyObject: PyObject) : Statement(pyObject) {
        val name: String
            get() {
                return pyObject.getAttr("name") as? String ?: TODO()
            }

        val args: Arguments
            get() {
                return fromPython(pyObject.getAttr("args")) as? Arguments ?: TODO()
            }

        val body: kotlin.collections.List<Statement>
            get() {
                val listOfPyStmt = pyObject.getAttr("body") as? ArrayList<*> ?: TODO()
                return listOfPyStmt.map { fromPython(it) as? Statement ?: TODO() }
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

    /*
    class ast.ClassDef(name, bases, keywords, body, decorator_list)
    */
    class ClassDef(pyObject: PyObject) : Statement(pyObject) {
        val name: String
            get() {
                return pyObject.getAttr("name") as? String ?: TODO()
            }

        val bases: kotlin.collections.List<Node>
            get() {
                val listOfBases = pyObject.getAttr("bases") as? ArrayList<*> ?: TODO()
                return listOfBases.map { fromPython(it) as? Node ?: TODO() }
            }

        val keywords: kotlin.collections.List<Keyword>
            get() {
                val listOfKeywords = pyObject.getAttr("keywords") as? ArrayList<*> ?: TODO()
                return listOfKeywords.map { fromPython(it) as? Keyword ?: TODO() }
            }

        val body: kotlin.collections.List<Statement>
            get() {
                val listOfPyStmt = pyObject.getAttr("body") as? ArrayList<*> ?: TODO()
                return listOfPyStmt.map { fromPython(it) as? Statement ?: TODO() }
            }

        val decoratorList: kotlin.collections.List<Expression>
            get() {
                val listOfDecorators = pyObject.getAttr("decorator_list") as? ArrayList<*> ?: TODO()
                return listOfDecorators.map { fromPython(it) as? Expression ?: TODO() }
            }
    }

    class Return(pyObject: PyObject) : Statement(pyObject)

    class Delete(pyObject: PyObject) : Statement(pyObject)

    class Assign(pyObject: PyObject) : Statement(pyObject)

    class AugAssign(pyObject: PyObject) : Statement(pyObject)

    class AnnAssign(pyObject: PyObject) : Statement(pyObject)

    class For(pyObject: PyObject) : Statement(pyObject)

    class AsyncFor(pyObject: PyObject) : Statement(pyObject)

    class While(pyObject: PyObject) : Statement(pyObject)

    class If(pyObject: PyObject) : Statement(pyObject)

    class With(pyObject: PyObject) : Statement(pyObject)

    class AsyncWith(pyObject: PyObject) : Statement(pyObject)

    class Match(pyObject: PyObject) : Statement(pyObject)

    class Raise(pyObject: PyObject) : Statement(pyObject)

    class Try(pyObject: PyObject) : Statement(pyObject)

    class TryStar(pyObject: PyObject) : Statement(pyObject)

    class Assert(pyObject: PyObject) : Statement(pyObject)

    class Import(pyObject: PyObject) : Statement(pyObject)

    class ImportFrom(pyObject: PyObject) : Statement(pyObject)

    class Global(pyObject: PyObject) : Statement(pyObject)

    class Nonlocal(pyObject: PyObject) : Statement(pyObject)

    /**
     * Represents `ast.Expr` expressions. Note: do not confuse with
     * - [expr] -> the expression class
     * - [Expression] -> the expression as part of `mod`
     */
    class Expr(pyObject: PyObject) : Statement(pyObject)

    class Pass(pyObject: PyObject) : Statement(pyObject)

    class Break(pyObject: PyObject) : Statement(pyObject)

    class Continue(pyObject: PyObject) : Statement(pyObject)

    /**
     * Represents `ast.expr` expressions. Note: do not confuse with
     * - [Expr] -> the expression statement
     * - [Expression] -> the expression as part of `mod`
     */
    abstract class expr(pyObject: PyObject) : Node(pyObject)

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

    class Keyword(pyObject: PyObject) : Node(pyObject) {
        // TODO()
    }

    class AssignStmt(pyObject: PyObject) : Statement(pyObject) {
        val lhs: Node
            get() {
                return fromPython(pyObject.getAttr("lhs") as PyObject)
            }
    }
}
