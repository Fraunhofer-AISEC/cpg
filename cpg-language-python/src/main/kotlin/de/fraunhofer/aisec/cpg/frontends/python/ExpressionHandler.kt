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
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import jep.python.PyObject

class ExpressionHandler(frontend: PythonLanguageFrontend) :
    PythonHandler<Expression, Python.AST.BaseExpr>(::ProblemExpression, frontend) {

    override fun handleNode(node: Python.AST.BaseExpr): Expression {
        return when (node) {
            is Python.AST.Name -> handleName(node)
            is Python.AST.Call -> handleCall(node)
            is Python.AST.Constant -> handleConstant(node)
            is Python.AST.Attribute -> handleAttribute(node)
            is Python.AST.BinOp -> handleBinOp(node)
            is Python.AST.UnaryOp -> handleUnaryOp(node)
            is Python.AST.Compare -> handleCompare(node)
            is Python.AST.Dict -> handleDict(node)
            is Python.AST.IfExp -> handleIfExp(node)
            is Python.AST.Tuple -> handleTuple(node)
            is Python.AST.List -> handleList(node)
            is Python.AST.BoolOp -> handleBoolOp(node)
            is Python.AST.Subscription -> handleSubscription(node)
            is Python.AST.Slice -> handleSlice(node)
            is Python.AST.Lambda -> handleLambda(node)
            is Python.AST.Set -> handleSet(node)
            is Python.AST.FormattedValue -> handleFormattedValue(node)
            is Python.AST.JoinedStr -> handleJoinedStr(node)
            is Python.AST.Starred -> handleStarred(node)
            is Python.AST.NamedExpr -> handleNamedExpr(node)
            is Python.AST.ListComp -> handleListComprehension(node)
            is Python.AST.SetComp -> handleSetComprehension(node)
            is Python.AST.DictComp -> handleDictComprehension(node)
            is Python.AST.GeneratorExp -> handleGeneratorExp(node)
            is Python.AST.Await,
            is Python.AST.Yield,
            is Python.AST.YieldFrom ->
                newProblemExpression(
                    "The expression of class ${node.javaClass} is not supported yet",
                    rawNode = node,
                )
        }
    }

    /**
     * Translates a Python
     * [`comprehension`](https://docs.python.org/3/library/ast.html#ast.comprehension) into a
     * [Comprehension].
     *
     * Connects multiple predicates by `and`.
     */
    private fun handleComprehension(
        node: Python.AST.comprehension,
        parent: Python.AST.BaseExpr,
    ): Comprehension {
        return newComprehension(rawNode = parent).apply {
            variable = handle(node.target)
            iterable = handle(node.iter)
            val predicates = node.ifs.map { handle(it) }
            if (predicates.size == 1) {
                predicate = predicates.single()
            } else if (predicates.size > 1) {
                predicate =
                    joinListWithBinOp(operatorCode = "and", nodes = predicates, rawNode = parent)
            }
            if (node.is_async != 0L)
                additionalProblems +=
                    newProblemExpression(
                        "Node marked as is_async but we don't support this yet",
                        rawNode = node,
                    )
        }
    }

    /**
     * Translates a Python
     * [`GeneratorExp`](https://docs.python.org/3/library/ast.html#ast.GeneratorExp) into a
     * [CollectionComprehension].
     */
    private fun handleGeneratorExp(node: Python.AST.GeneratorExp): CollectionComprehension {
        return newCollectionComprehension(rawNode = node).applyWithScope {
            statement = handle(node.elt)
            comprehensionExpressions += node.generators.map { handleComprehension(it, node) }
            type = objectType("Generator")
        }
    }

    /**
     * Translates a Python [`ListComp`](https://docs.python.org/3/library/ast.html#ast.ListComp)
     * into a [CollectionComprehension].
     */
    private fun handleListComprehension(node: Python.AST.ListComp): CollectionComprehension {
        return newCollectionComprehension(rawNode = node).applyWithScope {
            statement = handle(node.elt)
            comprehensionExpressions += node.generators.map { handleComprehension(it, node) }
            type = primitiveType("list")
        }
    }

    /**
     * Translates a Python [`SetComp`](https://docs.python.org/3/library/ast.html#ast.SetComp) into
     * a [CollectionComprehension].
     */
    private fun handleSetComprehension(node: Python.AST.SetComp): CollectionComprehension {
        return newCollectionComprehension(rawNode = node).applyWithScope {
            this.statement = handle(node.elt)
            this.comprehensionExpressions += node.generators.map { handleComprehension(it, node) }
            this.type = primitiveType("set")
        }
    }

    /**
     * Translates a Python [`DictComp`](https://docs.python.org/3/library/ast.html#ast.DictComp)
     * into a [CollectionComprehension].
     */
    private fun handleDictComprehension(node: Python.AST.DictComp): CollectionComprehension {
        return newCollectionComprehension(rawNode = node).applyWithScope {
            this.statement =
                newKeyValue(key = handle(node.key), value = handle(node.value), rawNode = node)
            this.comprehensionExpressions += node.generators.map { handleComprehension(it, node) }
            this.type = primitiveType("dict")
        }
    }

    /**
     * Translates a Python [`NamedExpr`](https://docs.python.org/3/library/ast.html#ast.NamedExpr)
     * into an [Assign].
     *
     * As opposed to the Assign node, both target and value must be single nodes.
     */
    private fun handleNamedExpr(node: Python.AST.NamedExpr): Assign {
        val assignExpression =
            newAssign(
                operatorCode = ":=",
                lhs = listOf(handle(node.target)),
                rhs = listOf(handle(node.value)),
                rawNode = node,
            )
        assignExpression.usedAsExpression = true
        return assignExpression
    }

    /**
     * Translates a Python
     * [`FormattedValue`](https://docs.python.org/3/library/ast.html#ast.FormattedValue) into an
     * [Expression].
     *
     * We are handling the format handling, following [PEP 3101](https://peps.python.org/pep-3101).
     *
     * The following example
     *
     * ```python
     *  f"{value:.2f}"
     * ```
     *
     * is modeled:
     * 1. The value `value` is wrapped in a `format()` call.
     * 2. The `format()` call has two arguments:
     *     - The value to format (`value`).
     *     - The format specification (`".2f"`).
     *
     * CPG Representation:
     * - `Call` node:
     *         - `callee`: `Reference` to `format`.
     *         - `arguments`:
     *             1. A node representing `value`.
     *             2. A node representing the string `".2f"`.
     */
    private fun handleFormattedValue(node: Python.AST.FormattedValue): Expression {
        /*
        Magic numbers (https://docs.python.org/3/library/ast.html#ast.FormattedValue):
        conversion is an integer:
        -1: no formatting
        115: !s string formatting
        114: !r repr formatting
        97: !a ascii formatting
        */
        val formattedValConversionNoFormatting = -1L
        val formattedValConversionString = 115L
        val formattedValConversionRepr = 114L
        val formattedValConversionASCII = 97L

        val formatSpec = node.format_spec?.let { handle(it) }
        val valueExpression = handle(node.value)
        val conversion =
            when (node.conversion) {
                formattedValConversionNoFormatting -> {
                    // No formatting, just return the value.
                    valueExpression
                }
                formattedValConversionString -> {
                    // String representation: wrap in `str()` call.
                    val strCall =
                        newCall(
                                callee = newReference(name = "str", rawNode = node),
                                fqn = "str",
                                rawNode = node,
                            )
                            .implicit()
                    strCall.addArgument(valueExpression)
                    strCall
                }
                formattedValConversionRepr -> {
                    // Repr-String representation: wrap in `repr()` call.
                    val reprCall =
                        newCall(
                                callee = newReference(name = "repr", rawNode = node),
                                fqn = "repr",
                                rawNode = node,
                            )
                            .implicit()
                    reprCall.addArgument(valueExpression)
                    reprCall
                }
                formattedValConversionASCII -> {
                    // ASCII-String representation: wrap in `ascii()` call.
                    val asciiCall =
                        newCall(newReference("ascii", rawNode = node), "ascii", rawNode = node)
                            .implicit()
                    asciiCall.addArgument(handle(node.value))
                    asciiCall
                }
                else ->
                    newProblemExpression(
                        problem =
                            "Cannot handle formatted value with conversion code ${node.conversion} yet",
                        rawNode = node,
                    )
            }
        if (formatSpec != null) {
            return newCall(
                    callee = newReference(name = "format", rawNode = node),
                    fqn = "format",
                    rawNode = node,
                )
                .implicit()
                .apply {
                    addArgument(conversion)
                    addArgument(formatSpec)
                }
        }
        return conversion
    }

    /**
     * Translates a Python [`JoinedStr`](https://docs.python.org/3/library/ast.html#ast.JoinedStr)
     * into a [Expression].
     */
    private fun handleJoinedStr(node: Python.AST.JoinedStr): Expression {
        val values = node.values.map(::handle)
        return if (values.isEmpty()) {
            newLiteral("", primitiveType("str"), rawNode = node)
        } else if (values.size == 1) {
            values.first()
        } else {
            joinListWithBinOp(operatorCode = "+", nodes = values, rawNode = node)
        }
    }

    /**
     * Joins the [nodes] with a [BinaryOperator] with the [operatorCode]. Nests the whole thing,
     * where the first element in [nodes] is the lhs of the root of the tree of binary operators.
     * The last operands are further down the tree.
     */
    internal fun joinListWithBinOp(
        operatorCode: String,
        nodes: List<Expression>,
        rawNode: Python.AST.AST? = null,
        isImplicit: Boolean = true,
    ): BinaryOperator {
        val lastTwo =
            newBinaryOperator(operatorCode = operatorCode, rawNode = rawNode).apply {
                rhs = nodes.last()
                lhs = nodes[nodes.size - 2]
                this.isImplicit = isImplicit
            }
        return nodes.subList(0, nodes.size - 2).foldRight(lastTwo) { newVal, start ->
            newBinaryOperator(operatorCode = operatorCode, rawNode = rawNode).apply {
                rhs = start
                lhs = newVal
                this.isImplicit = isImplicit
            }
        }
    }

    private fun handleStarred(node: Python.AST.Starred): Expression {
        val unaryOp = newUnaryOperator("*", postfix = false, prefix = false, rawNode = node)
        unaryOp.input = handle(node.value)
        return unaryOp
    }

    private fun handleSlice(node: Python.AST.Slice): Expression {
        val slice = newRange(rawNode = node)
        slice.floor = node.lower?.let { handle(it) }
        slice.ceiling = node.upper?.let { handle(it) }
        slice.third = node.step?.let { handle(it) }
        return slice
    }

    private fun handleSubscription(node: Python.AST.Subscription): Expression {
        val subscriptExpression = newSubscription(rawNode = node)
        subscriptExpression.arrayExpression = handle(node.value)
        subscriptExpression.subscriptExpression = handle(node.slice)
        return subscriptExpression
    }

    /**
     * This method handles the python
     * [`BoolOp`](https://docs.python.org/3/library/ast.html#ast.BoolOp).
     *
     * Generates a (potentially nested) [BinaryOperator] from a `BoolOp`. Less than two operands in
     * [Python.AST.BoolOp.values] don't make sense and will generate a [ProblemExpression]. If only
     * two operands exist, a simple [BinaryOperator] will be generated. More than two operands will
     * lead to a nested [BinaryOperator]. E.g., if [Python.AST.BoolOp.values] contains the operators
     * `[a, b, c]`, the result will be `a OP (b OP c)`.
     */
    private fun handleBoolOp(node: Python.AST.BoolOp): Expression {
        val op =
            when (node.op) {
                is Python.AST.And -> "and"
                is Python.AST.Or -> "or"
            }

        return if (node.values.size <= 1) {
            newProblemExpression(
                "Expected exactly two expressions but got ${node.values.size}",
                rawNode = node,
            )
        } else {
            joinListWithBinOp(
                operatorCode = op,
                nodes = node.values.map(::handle),
                rawNode = node,
                isImplicit = true,
            )
        }
    }

    private fun handleList(node: Python.AST.List): Expression {
        val lst = mutableListOf<Expression>()
        for (e in node.elts) {
            lst += handle(e)
        }
        val ile = newInitializerList(rawNode = node)
        ile.type = frontend.objectType("list")
        ile.initializers = lst
        return ile
    }

    private fun handleSet(node: Python.AST.Set): Expression {
        val lst = mutableListOf<Expression>()
        for (e in node.elts) {
            lst += handle(e)
        }
        val ile = newInitializerList(rawNode = node)
        ile.type = frontend.objectType("set")
        ile.initializers = lst
        return ile
    }

    private fun handleTuple(node: Python.AST.Tuple): Expression {
        val lst = mutableListOf<Expression>()
        for (e in node.elts) {
            lst += handle(e)
        }
        val ile = newInitializerList(rawNode = node)
        ile.type = frontend.objectType("tuple")
        ile.initializers = lst
        return ile
    }

    private fun handleIfExp(node: Python.AST.IfExp): Expression {
        return newConditional(
            condition = handle(node.test),
            thenExpression = handle(node.body),
            elseExpression = handle(node.orelse),
            rawNode = node,
        )
    }

    private fun handleDict(node: Python.AST.Dict): Expression {
        val lst = mutableListOf<Expression>()
        for (i in node.values.indices) { // TODO: keys longer than values possible?
            // Here we can not use node as raw node as it spans all keys and values
            lst +=
                newKeyValue(
                        key =
                            node.keys[i]?.let { handle(it) } ?: newProblemExpression("missing key"),
                        value = handle(node.values[i]),
                    )
                    .codeAndLocationFromChildren(node, frontend.lineSeparator)
        }
        val ile = newInitializerList(rawNode = node)
        ile.type = frontend.objectType("dict")
        ile.initializers = lst
        return ile
    }

    private fun handleCompare(node: Python.AST.Compare): Expression {
        if (node.comparators.size != 1 || node.ops.size != 1) {
            return newProblemExpression("Multi compare is not (yet) supported.", rawNode = node)
        }
        val op =
            when (node.ops.first()) {
                is Python.AST.Eq -> "=="
                is Python.AST.NotEq -> "!="
                is Python.AST.Lt -> "<"
                is Python.AST.LtE -> "<="
                is Python.AST.Gt -> ">"
                is Python.AST.GtE -> ">="
                is Python.AST.Is -> "is"
                is Python.AST.IsNot -> "is not"
                is Python.AST.In -> "in"
                is Python.AST.NotIn -> "not in"
            }
        val ret = newBinaryOperator(op, rawNode = node)
        ret.lhs = handle(node.left)
        ret.rhs = handle(node.comparators.first())
        return ret
    }

    private fun handleBinOp(node: Python.AST.BinOp): Expression {
        val op = frontend.operatorToString(node.op)
        val ret = newBinaryOperator(operatorCode = op, rawNode = node)
        ret.lhs = handle(node.left)
        ret.rhs = handle(node.right)
        return ret
    }

    private fun handleUnaryOp(node: Python.AST.UnaryOp): Expression {
        val op = frontend.operatorUnaryToString(node.op)
        val ret =
            newUnaryOperator(operatorCode = op, postfix = false, prefix = false, rawNode = node)
        ret.input = handle(node.operand)
        return ret
    }

    private fun handleAttribute(node: Python.AST.Attribute): Expression {
        val base = handle(node.value)

        return newMemberAccess(name = node.attr, base = base, rawNode = node)
    }

    private fun handleConstant(node: Python.AST.Constant): Expression {
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

    private fun easyConstant(node: Python.AST.Constant): Expression {
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
                    unknownType()
                }
            }
        return newLiteral(node.value, type = tpe, rawNode = node)
    }

    /**
     * Handles an `ast.Call` Python node. This can be one of
     * - [MemberCall]
     * - [Construction]
     * - [Cast]
     * - [Call]
     *
     * TODO: cast, memberexpression, magic
     */
    private fun handleCall(node: Python.AST.Call): Expression {
        val callee = frontend.expressionHandler.handle(node.func)

        val ret =
            if (callee is MemberAccess) {
                newMemberCall(callee, rawNode = node)
            } else {
                newCall(callee, rawNode = node)
            }

        for (arg in node.args) {
            ret.addArgument(handle(arg))
        }

        for (keyword in node.keywords) {
            ret.argumentEdges.add(handle(keyword.value)) { name = keyword.arg }
        }

        return ret
    }

    private fun handleName(node: Python.AST.Name): Expression {
        val r = newReference(name = node.id, rawNode = node)
        return r
    }

    private fun handleLambda(node: Python.AST.Lambda): Expression {
        val lambda = newLambda(rawNode = node)
        val function = newFunction(name = "", rawNode = node)
        frontend.scopeManager.enterScope(function)
        for (arg in node.args.args) {
            this.frontend.declarationHandler.handleArgument(function, arg)
        }

        function.body = handle(node.body)
        frontend.scopeManager.leaveScope(function)
        lambda.function = function
        return lambda
    }
}
