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
import de.fraunhofer.aisec.cpg.graph.declarations.ImportDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ProblemExpression
import jep.python.PyObject

class ExpressionHandler(frontend: PythonLanguageFrontend) :
    PythonHandler<Expression, Python.ASTBASEexpr>(::ProblemExpression, frontend) {

    /*
    Magic numbers (https://docs.python.org/3/library/ast.html#ast.FormattedValue):
    conversion is an integer:
        -1: no formatting
        115: !s string formatting
        114: !r repr formatting
        97: !a ascii formatting
     */
    private val formattedValConversionNoFormatting = -1L
    private val formattedValConversionString = 115L
    private val formattedValConversionRepr = 114L
    private val formattedValConversionASCII = 97L

    override fun handleNode(node: Python.ASTBASEexpr): Expression {
        return when (node) {
            is Python.ASTName -> handleName(node)
            is Python.ASTCall -> handleCall(node)
            is Python.ASTConstant -> handleConstant(node)
            is Python.ASTAttribute -> handleAttribute(node)
            is Python.ASTBinOp -> handleBinOp(node)
            is Python.ASTUnaryOp -> handleUnaryOp(node)
            is Python.ASTCompare -> handleCompare(node)
            is Python.ASTDict -> handleDict(node)
            is Python.ASTIfExp -> handleIfExp(node)
            is Python.ASTTuple -> handleTuple(node)
            is Python.ASTList -> handleList(node)
            is Python.ASTBoolOp -> handleBoolOp(node)
            is Python.ASTSubscript -> handleSubscript(node)
            is Python.ASTSlice -> handleSlice(node)
            is Python.ASTLambda -> handleLambda(node)
            is Python.ASTSet -> handleSet(node)
            is Python.ASTFormattedValue -> handleFormattedValue(node)
            is Python.ASTJoinedStr -> handleJoinedStr(node)
            is Python.ASTStarred -> handleStarred(node)
            is Python.ASTNamedExpr,
            is Python.ASTGeneratorExp,
            is Python.ASTListComp,
            is Python.ASTSetComp,
            is Python.ASTDictComp,
            is Python.ASTAwait,
            is Python.ASTYield,
            is Python.ASTYieldFrom ->
                newProblemExpression(
                    "The expression of class ${node.javaClass} is not supported yet",
                    rawNode = node
                )
        }
    }

    private fun handleFormattedValue(node: Python.ASTFormattedValue): Expression {
        if (node.format_spec != null) {
            return newProblemExpression(
                "Cannot handle formatted value with format_spec ${node.format_spec} yet",
                rawNode = node
            )
        }
        return when (node.conversion) {
            formattedValConversionNoFormatting -> {
                // No formatting, just return the value.
                handle(node.value)
            }
            formattedValConversionString -> {
                // String representation. wrap in str() call.
                val strCall =
                    newCallExpression(newReference("str", rawNode = node), "str", rawNode = node)
                strCall.addArgument(handle(node.value))
                strCall
            }
            formattedValConversionRepr -> {
                newProblemExpression(
                    "Cannot handle conversion '114: !r repr formatting', yet.",
                    rawNode = node
                )
            }
            formattedValConversionASCII -> {
                newProblemExpression(
                    "Cannot handle conversion '97: !a ascii formatting', yet.",
                    rawNode = node
                )
            }
            else ->
                newProblemExpression(
                    "Cannot handle formatted value with conversion ${node.conversion} yet",
                    rawNode = node
                )
        }
    }

    private fun handleJoinedStr(node: Python.ASTJoinedStr): Expression {
        val values = node.values.map(::handle)
        return if (values.isEmpty()) {
            newLiteral("", primitiveType("str"), rawNode = node)
        } else if (values.size == 1) {
            values.first()
        } else {
            val lastTwo = newBinaryOperator("+", rawNode = node)
            lastTwo.rhs = values.last()
            lastTwo.lhs = values[values.size - 2]
            values.subList(0, values.size - 2).foldRight(lastTwo) { newVal, start ->
                val nextValue = newBinaryOperator("+")
                nextValue.rhs = start
                nextValue.lhs = newVal
                nextValue
            }
        }
    }

    private fun handleStarred(node: Python.ASTStarred): Expression {
        val unaryOp = newUnaryOperator("*", postfix = false, prefix = false, rawNode = node)
        unaryOp.input = handle(node.value)
        return unaryOp
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

    private fun handleBoolOp(node: Python.ASTBoolOp): Expression {
        val op =
            when (node.op) {
                is Python.ASTAnd -> "and"
                is Python.ASTOr -> "or"
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
            it.initializers = lst
            it.type = frontend.objectType("list")
        }
    }

    private fun handleSet(node: Python.ASTSet): Expression {
        return newInitializerListExpression(rawNode = node).withChildren(hasScope = false) {
            val lst = mutableListOf<Expression>()
            for (e in node.elts) {
                lst += handle(e)
            }
            it.initializers = lst
            it.type = frontend.objectType("set")
        }
    }

    private fun handleTuple(node: Python.ASTTuple): Expression {
        return newInitializerListExpression(rawNode = node).withChildren(hasScope = false) {
            val lst = mutableListOf<Expression>()
            for (e in node.elts) {
                lst += handle(e)
            }
            it.initializers = lst
            it.type = frontend.objectType("tuple")
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
        return newInitializerListExpression(rawNode = node).withChildren(hasScope = false) { ile ->
            val lst = mutableListOf<Expression>()
            for (i in node.values.indices) { // TODO: keys longer than values possible?
                // Here we can not use node as raw node as it spans all keys and values
                lst +=
                    newKeyValueExpression().codeAndLocationFromChildren(node).withChildren { kve ->
                        kve.key = node.keys[i]?.let { key -> handle(key) }
                        kve.value = handle(node.values[i])
                    }
            }
            ile.initializers = lst
            ile.type = frontend.objectType("dict")
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
            }
        return newBinaryOperator(operatorCode = op, rawNode = node).withChildren(hasScope = false) {
            it.lhs = handle(node.left)
            it.rhs = handle(node.comparators.first())
        }
    }

    private fun handleBinOp(node: Python.ASTBinOp): Expression {
        val op = frontend.operatorToString(node.op)
        return newBinaryOperator(operatorCode = op, rawNode = node).withChildren(hasScope = false) {
            it.lhs = handle(node.left)
            it.rhs = handle(node.right)
        }
    }

    private fun handleUnaryOp(node: Python.ASTUnaryOp): Expression {
        val op = frontend.operatorUnaryToString(node.op)
        return newUnaryOperator(
                operatorCode = op,
                postfix = false,
                prefix = false,
                rawNode = node
            ) // TODO prefix?
            .withChildren(hasScope = false) { it.input = handle(node.operand) }
    }

    private fun handleAttribute(node: Python.ASTAttribute): Expression {
        var base = handle(node.value)

        // We do a quick check, if this refers to an import. This is faster than doing
        // this in a pass and most likely valid, since we are under the assumption that
        // our current file is (more or less) complete, but we might miss some
        // additional dependencies
        var ref =
            if (isImport(base.name)) {
                // Yes, it's an import, so we need to construct a reference with an FQN
                newReference(base.name.fqn(node.attr), rawNode = node)
            } else {
                newMemberExpression(name = node.attr, base = base, rawNode = node).withChildren(
                    hasScope = false
                ) {
                    it.base.withParent()
                }
            }

        return ref
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
        var callee = frontend.expressionHandler.handle(node.func)

        val ret =
            if (callee is MemberExpression) {
                newMemberCallExpression(callee, rawNode = node).withChildren(hasScope = false) {
                    it.callee.withParent()
                }
            } else {
                // try to resolve -> [ConstructExpression]
                val currentScope = frontend.scopeManager.currentScope
                val record =
                    currentScope?.let { frontend.scopeManager.getRecordForName(callee.name) }

                if (record != null) {
                    // construct expression
                    val constructExpr =
                        newConstructExpression((node.func as? Python.ASTName)?.id, rawNode = node)
                    constructExpr.type = record.toType()
                    constructExpr
                } else {
                    newCallExpression(callee, rawNode = node).withChildren(hasScope = false) {
                        it.callee.withParent()
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

    private fun isImport(name: Name): Boolean {
        val decl =
            frontend.scopeManager.currentScope
                ?.lookupSymbol(name.localName, replaceImports = false)
                ?.filterIsInstance<ImportDeclaration>()
        return decl?.isNotEmpty() ?: false
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

    private fun handleLambda(
        node: Python.ASTLambda
    ): Expression { // TODO: scope for lambda / function or both?
        return newLambdaExpression(rawNode = node).withChildren(hasScope = false) { lambda ->
            lambda.function =
                newFunctionDeclaration(name = "", rawNode = node).withChildren(hasScope = true) {
                    function ->
                    for (arg in node.args.args) {
                        this.frontend.statementHandler.handleArgument(arg)
                    }
                    function.body = handle(node.body)
                }
        }
    }
}
