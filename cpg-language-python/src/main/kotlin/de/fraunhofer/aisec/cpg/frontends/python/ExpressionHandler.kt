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

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ProblemExpression
import jep.python.PyObject

class ExpressionHandler(frontend: PythonLanguageFrontend) :
    PythonHandler<Expression, Python.ASTBASEexpr>(::ProblemExpression, frontend) {
    override fun handleNode(node: Python.ASTBASEexpr): Expression {
        return when (node) {
            is Python.ASTName -> handleName(node)
            is Python.ASTCall -> handleCall(node)
            is Python.ASTConstant -> handleConstant(node)
            is Python.ASTAttribute -> handleAttribute(node)
            is Python.ASTBinOp -> handleBinOp(node)
            is Python.ASTCompare -> handleCompare(node)
            is Python.ASTDict -> handleDict(node)
            is Python.ASTIfExp -> handleIfExp(node)
            is Python.ASTTuple -> handleTuple(node)
            is Python.ASTList -> handleList(node)
            is Python.ASTBoolOp -> handleBoolOp(node)
            is Python.ASTSubscript -> handleSubscript(node)
            is Python.ASTSlice -> handleSlice(node)
            else ->
                newProblemExpression(
                    "The expression of class ${node.javaClass} is not supported yet",
                    rawNode = node
                )
        }
    }

    private fun handleSlice(node: Python.ASTSlice): Expression {
        return newRangeExpression(rawNode = node).withChildren(hasScope = false) { slice ->
            slice.floor = node.lower?.let { lower -> handle(lower) }
            slice.ceiling = node.upper?.let { upper -> handle(upper) }
            slice.third = node.step?.let { step -> handle(step) }
        }
    }

    private fun handleSubscript(node: Python.ASTSubscript): Expression {
        return newSubscriptExpression(rawNode = node).withChildren(hasScope = false) { sub ->
            sub.arrayExpression = handle(node.value)
            sub.subscriptExpression = handle(node.slice)
        }
    }

    private fun handleSlice(node: Python.ASTSlice): Expression {
        val slice = newRangeExpression(rawNode = node)
        slice.floor = node.lower?.let { handle(it) }
        slice.ceiling = node.upper?.let { handle(it) }
        slice.third = node.step?.let { handle(it) }
        return slice
    }

    private fun handleSubscript(node: Python.ASTSubscript): Expression {
        val subscriptExpression = newSubscriptExpression(rawNode = node)
        subscriptExpression.arrayExpression = handle(node.value)
        subscriptExpression.subscriptExpression = handle(node.slice)
        return subscriptExpression
    }

    private fun handleBoolOp(node: Python.ASTBoolOp): Expression {
        val op =
            when (node.op) {
                is Python.ASTAnd -> "and"
                is Python.ASTOr -> "or"
                else ->
                    return newProblemExpression(
                        "Unsupported BoolOp operator " + node.op,
                        rawNode = node
                    )
            }
        if (node.values.size != 2) {
            return newProblemExpression(
                "Expected exactly two expressions but got " + node.values.size,
                rawNode = node
            )
        }
        return newBinaryOperator(operatorCode = op, rawNode = node).withChildren(hasScope = false) {
            it.lhs = handle(node.values[0])
            it.rhs = handle(node.values[1])
        }
    }

    private fun handleList(node: Python.ASTList): Expression {
        return newInitializerListExpression(rawNode = node).withChildren(hasScope = false) {
            val lst = mutableListOf<Expression>()
            for (e in node.elts) {
                lst += handle(e)
            }
            it.initializers = lst.toList()
        }
    }

    private fun handleTuple(node: Python.ASTTuple): Expression {
        return newInitializerListExpression(rawNode = node).withChildren(hasScope = false) {
            val lst = mutableListOf<Expression>()
            for (e in node.elts) {
                lst += handle(e)
            }
            it.initializers = lst.toList()
        }
    }

    private fun handleIfExp(node: Python.ASTIfExp): Expression {
        return newConditionalExpression(rawNode = node).withChildren(hasScope = false) {
            it.condition = handle(node.test)
            it.thenExpression = handle(node.body)
            it.elseExpression = handle(node.orelse)
        }
    }

    private fun handleDict(node: Python.ASTDict): Expression {
        return newInitializerListExpression(rawNode = node).withChildren(hasScope = false) {
            val lst = mutableListOf<Expression>()
            for (i in node.values.indices) { // TODO: keys longer than values possible?
                // Here we can not use node as raw node as it spans all keys and values
                lst +=
                    newKeyValueExpression(
                            key = node.keys[i]?.let { key -> handle(key) },
                            value = handle(node.values[i]),
                        )
                        .codeAndLocationFromChildren(node)
            }
            it.initializers = lst.toList()
        }
    }

    private fun handleCompare(node: Python.ASTCompare): Expression {
        if (node.comparators.size != 1 || node.ops.size != 1) {
            return newProblemExpression("Multi compare is not (yet) supported.", rawNode = node)
        }
        val op =
            when (node.ops.first()) {
                is Python.ASTEq -> "=="
                is Python.ASTNotEq -> "!="
                is Python.ASTLt -> "<"
                is Python.ASTLtE -> "<="
                is Python.ASTGt -> ">"
                is Python.ASTGtE -> ">="
                is Python.ASTIs -> "is"
                is Python.ASTIsNot -> "is not"
                is Python.ASTIn -> "in"
                is Python.ASTNotIn -> "not in"
                else ->
                    return newProblemExpression(
                        "The comparison operation ${node.ops.first().javaClass} is not supported yet",
                        rawNode = node
                    )
            }
        return newBinaryOperator(operatorCode = op, rawNode = node).withChildren(hasScope = false) {
            it.lhs = handle(node.left)
            it.rhs = handle(node.comparators.first())
        }
    }

    private fun handleBinOp(node: Python.ASTBinOp): Expression {
        val op =
            when (node.op) {
                is Python.ASTAdd -> "+"
                is Python.ASTSub -> "-"
                is Python.ASTMult -> "*"
                is Python.ASTMatMult -> "*"
                is Python.ASTDiv -> "/"
                is Python.ASTMod -> "%"
                is Python.ASTPow -> "**"
                is Python.ASTLShift -> "<<"
                is Python.ASTRShift -> ">>"
                is Python.ASTBitOr -> "|"
                is Python.ASTBitXor -> "^"
                is Python.ASTBitAnd -> "&"
                is Python.ASTFloorDiv -> "//"
                else ->
                    return newProblemExpression(
                        "The binary operation ${node.op.javaClass} is not supported yet",
                        rawNode = node
                    )
            }
        return newBinaryOperator(operatorCode = op, rawNode = node).withChildren(hasScope = false) {
            it.lhs = handle(node.left)
            it.rhs = handle(node.right)
        }
    }

    private fun handleAttribute(node: Python.ASTAttribute): Expression {
        return newMemberExpression(name = node.attr, base = handle(node.value), rawNode = node)
    }

    private fun handleConstant(node: Python.ASTConstant): Expression {
        // TODO: this is ugly

        return if (
            (node.pyObject.getAttr("value") as? PyObject)?.getAttr("__class__").toString() ==
                "<class 'complex'>"
        ) {
            val tpe = primitiveType("complex")
            return newLiteral(node.pyObject.getAttr("value").toString(), type = tpe, rawNode = node)
        } else if (node.pyObject.getAttr("value") == null) {
            val tpe = objectType("None")

            return newLiteral(null, type = tpe, rawNode = node)
        } else {
            easyConstant(node)
        }
    }

    private fun easyConstant(node: Python.ASTConstant): Expression {
        // TODO check and add missing types
        val tpe =
            when (node.value) {
                is String -> primitiveType("str")
                is Boolean -> primitiveType("bool")
                is Int,
                is Long -> primitiveType("int")
                is Float,
                is Double -> primitiveType("float")
                else -> {
                    autoType()
                }
            }
        return newLiteral(node.value, type = tpe, rawNode = node)
    }

    /**
     * Handles an `ast.Call` Python node. This can be one of
     * - [MemberCallExpression]
     * - [ConstructExpression]
     * - [CastExpression]
     * - [CallExpression]
     *
     * TODO: cast, memberexpression, magic
     */
    private fun handleCall(node: Python.ASTCall): Expression {
        val ret =
            when (node.func) {
                is Python.ASTAttribute -> {
                    newMemberCallExpression(
                        frontend.expressionHandler.handle(node.func),
                        rawNode = node
                    )
                }
                else -> {
                    val func = handle(node.func)

                    // try to resolve -> [ConstructExpression]
                    val currentScope = frontend.scopeManager.currentScope
                    val record =
                        currentScope?.let { frontend.scopeManager.getRecordForName(func.name) }

                    if (record != null) {
                        // construct expression
                        val constructExpr =
                            newConstructExpression(
                                (node.func as? Python.ASTName)?.id,
                                rawNode = node
                            )
                        constructExpr.type = record.toType()
                        constructExpr
                    } else {
                        newCallExpression(func, rawNode = node)
                    }
                }
            }
        return ret.withChildren(hasScope = false) {
            for (arg in node.args) {
                ret.addArgument(handle(arg))
            }

            for (keyword in node.keywords) {
                ret.addArgument(handle(keyword.value), keyword.arg)
            }
        }
    }

    private fun handleName(node: Python.ASTName): Expression {
        val r = newReference(name = node.id, rawNode = node)

        /*
         * TODO: this is not nice... :(
         *
         * Take a little shortcut and set refersTo, in case this is a method receiver. This allows us to play more
         * nicely with member (call) expressions on the current class, since then their base type is known.
         */
        val currentFunction = frontend.scopeManager.currentFunction
        if (currentFunction is MethodDeclaration) {
            val recv = currentFunction.receiver
            recv.let {
                if (node.id == it?.name?.localName) {
                    r.refersTo = it
                    r.type = it.type
                }
            }
        }
        return r
    }
}
