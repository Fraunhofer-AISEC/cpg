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
package de.fraunhofer.aisec.cpg.frontends.golang

import de.fraunhofer.aisec.cpg.frontends.golang.GoStandardLibrary.Ast.BasicLit.Kind.*
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.PointerType
import de.fraunhofer.aisec.cpg.graph.types.Type

class ExpressionHandler(frontend: GoLanguageFrontend) :
    GoHandler<Expression, GoStandardLibrary.Ast.Expr>(::ProblemExpression, frontend) {

    override fun handleNode(expr: GoStandardLibrary.Ast.Expr): Expression {
        return when (expr) {
            is GoStandardLibrary.Ast.BasicLit -> handleBasicLit(expr)
            is GoStandardLibrary.Ast.BinaryExpr -> handleBinaryExpr(expr)
            is GoStandardLibrary.Ast.CompositeLit -> handleCompositeLit(expr)
            is GoStandardLibrary.Ast.FuncLit -> handleFuncLit(expr)
            is GoStandardLibrary.Ast.Ident -> handleIdent(expr)
            is GoStandardLibrary.Ast.IndexExpr -> handleIndexExpr(expr)
            is GoStandardLibrary.Ast.CallExpr -> handleCallExpr(expr)
            is GoStandardLibrary.Ast.KeyValueExpr -> handleKeyValueExpr(expr)
            is GoStandardLibrary.Ast.SelectorExpr -> handleSelectorExpr(expr)
            is GoStandardLibrary.Ast.SliceExpr -> handleSliceExpr(expr)
            is GoStandardLibrary.Ast.TypeAssertExpr -> handleTypeAssertExpr(expr)
            is GoStandardLibrary.Ast.UnaryExpr -> handleUnaryExpr(expr)
            else -> {
                return handleNotSupported(expr, expr.goType)
            }
        }
    }

    private fun handleBasicLit(basicLit: GoStandardLibrary.Ast.BasicLit): Literal<*> {
        val rawValue = basicLit.value
        val value: Any?
        val type: Type
        when (basicLit.kind) {
            STRING -> {
                value =
                    rawValue.substring(
                        1.coerceAtMost(rawValue.length - 1),
                        (rawValue.length - 1).coerceAtLeast(0)
                    )
                type = primitiveType("string")
            }
            INT -> {
                value = rawValue.toInt()
                type = primitiveType("int")
            }
            FLOAT -> {
                value = rawValue.toDouble()
                type = primitiveType("float64")
            }
            CHAR -> {
                value = rawValue.firstOrNull()
                type = primitiveType("rune")
            }
            else -> {
                value = rawValue
                type = unknownType()
            }
        }

        val lit = newLiteral(value, rawNode = basicLit)
        lit.type = type

        return lit
    }

    private fun handleBinaryExpr(binaryExpr: GoStandardLibrary.Ast.BinaryExpr): BinaryOperator {
        val binOp = newBinaryOperator(binaryExpr.opString, rawNode = binaryExpr)
        binOp.lhs = handle(binaryExpr.x) ?: newProblemExpression("missing LHS")
        binOp.rhs = handle(binaryExpr.y) ?: newProblemExpression("missing RHS")

        return binOp
    }

    private fun handleIdent(ident: GoStandardLibrary.Ast.Ident): Expression {
        // Check, if this is 'nil', because then we handle it as a literal in the graph
        if (ident.name == "nil") {
            val literal = newLiteral(null, rawNode = ident)
            literal.name = parseName(ident.name)

            return literal
        }

        val ref = newDeclaredReferenceExpression(ident.name, rawNode = ident)

        // Check, if this refers to a package import
        val import = frontend.currentTU?.getIncludeByName(ident.name)
        // Then set the refersTo, because our regular CPG passes will not resolve them
        if (import != null) {
            ref.refersTo = import
        }

        return ref
    }

    private fun handleIndexExpr(
        indexExpr: GoStandardLibrary.Ast.IndexExpr
    ): ArraySubscriptionExpression {
        val ase = newArraySubscriptionExpression(rawNode = indexExpr)
        ase.arrayExpression = frontend.expressionHandler.handle(indexExpr.x)
        ase.subscriptExpression = frontend.expressionHandler.handle(indexExpr.index)

        return ase
    }

    private fun handleCallExpr(callExpr: GoStandardLibrary.Ast.CallExpr): Expression {
        // In Go, regular cast expressions (not type asserts are modelled as calls).
        // In this case, the Fun contains a type expression.
        when (callExpr.`fun`) {
            is GoStandardLibrary.Ast.ArrayType,
            is GoStandardLibrary.Ast.ChanType,
            is GoStandardLibrary.Ast.FuncType,
            is GoStandardLibrary.Ast.InterfaceType,
            is GoStandardLibrary.Ast.StructType,
            is GoStandardLibrary.Ast.MapType, -> {
                val cast = newCastExpression(rawNode = callExpr)
                cast.castType = frontend.typeOf(callExpr.`fun`)

                if (callExpr.args.isNotEmpty()) {
                    frontend.expressionHandler.handle(callExpr.args[0])?.let {
                        cast.expression = it
                    }
                }

                return cast
            }
        }

        // Parse the Fun field, to see which kind of expression it is
        val callee =
            this.handle(callExpr.`fun`)
                ?: return ProblemExpression("Could not parse call expr without fun")

        // Handle special functions, such as make and new in a special way
        val name = callee.name.localName
        if (name == "new") {
            return handleNewExpr(callExpr)
        } else if (name == "make") {
            return handleMakeExpr(callExpr)
        }

        // Differentiate between calls and member calls based on the callee
        val call =
            if (callee is MemberExpression) {
                newMemberCallExpression(callee, rawNode = callExpr)
            } else {
                newCallExpression(callee, name, rawNode = callExpr)
            }

        // Parse and add call arguments
        for (arg in callExpr.args) {
            handle(arg)?.let { call += it }
        }

        return call
    }

    private fun handleKeyValueExpr(
        keyValueExpr: GoStandardLibrary.Ast.KeyValueExpr
    ): KeyValueExpression {
        val key = handle(keyValueExpr.key) ?: newProblemExpression("could not parse key")
        val value = handle(keyValueExpr.value) ?: newProblemExpression("could not parse value")

        return newKeyValueExpression(key, value, rawNode = keyValueExpr)
    }

    private fun handleNewExpr(callExpr: GoStandardLibrary.Ast.CallExpr): Expression {
        if (callExpr.args.isEmpty()) {
            return newProblemExpression("could not create NewExpression with empty arguments")
        }

        val n = newNewExpression(rawNode = callExpr)

        // First argument is type
        val type = frontend.typeOf(callExpr.args[0])

        // new is a pointer, so need to reference the type with a pointer
        n.type = type.reference(PointerType.PointerOrigin.POINTER)

        // a new expression also needs an initializer, which is usually a ConstructExpression
        val construct = newConstructExpression(rawNode = callExpr)
        construct.type = type

        n.initializer = construct

        return n
    }

    private fun handleMakeExpr(callExpr: GoStandardLibrary.Ast.CallExpr): Expression {
        val args = callExpr.args

        if (args.isEmpty()) {
            return newProblemExpression("too few arguments for make expression")
        }

        val expression =
            // Actually make() can make more than just arrays, i.e. channels and maps
            if (args[0] is GoStandardLibrary.Ast.ArrayType) {
                val array = newArrayCreationExpression(rawNode = callExpr)

                // second argument is a dimension (if this is an array), usually a literal
                if (args.size > 1) {
                    handle(args[1])?.let { array.addDimension(it) }
                }
                array
            } else {
                // Create at least a generic construct expression for the given map or channel type
                // and provide the remaining arguments
                val construct = newConstructExpression(rawNode = callExpr)

                // Pass the remaining arguments
                for (expr in args.subList(1.coerceAtMost(args.size - 1), args.size - 1)) {
                    handle(expr)?.let { construct += it }
                }

                construct
            }

        // First argument is always the type
        expression.type = frontend.typeOf(callExpr.args[0])

        return expression
    }

    private fun handleSelectorExpr(
        selectorExpr: GoStandardLibrary.Ast.SelectorExpr
    ): DeclaredReferenceExpression {
        val base = handle(selectorExpr.x) ?: newProblemExpression("missing base")

        // Check, if this just a regular reference to a variable with a package scope and not a
        // member expression
        var isMemberExpression = true
        for (imp in frontend.currentFile?.imports ?: listOf()) {
            if (base.name.localName == frontend.getImportName(imp)) {
                // found a package name, so this is NOT a member expression
                isMemberExpression = false
            }
        }

        val ref =
            if (isMemberExpression) {
                newMemberExpression(selectorExpr.sel.name, base, rawNode = selectorExpr)
            } else {
                // we need to set the name to a FQN-style, including the package scope. the call
                // resolver will then resolve this
                val fqn = "${base.name}.${selectorExpr.sel.name}"

                newDeclaredReferenceExpression(fqn, rawNode = selectorExpr)
            }

        return ref
    }

    /**
     * This function handles a ast.SliceExpr, which is an extended version of ast.IndexExpr. We are
     * modelling this as a combination of a [ArraySubscriptionExpression] that contains a
     * [RangeExpression] as its subscriptExpression to share some code between this and an index
     * expression.
     */
    private fun handleSliceExpr(
        sliceExpr: GoStandardLibrary.Ast.SliceExpr
    ): ArraySubscriptionExpression {
        val ase = newArraySubscriptionExpression(rawNode = sliceExpr)
        ase.arrayExpression =
            frontend.expressionHandler.handle(sliceExpr.x)
                ?: newProblemExpression("missing array expression")

        // Build the slice expression
        val range = newRangeExpression(rawNode = sliceExpr)
        sliceExpr.low?.let { range.floor = frontend.expressionHandler.handle(it) }
        sliceExpr.high?.let { range.ceiling = frontend.expressionHandler.handle(it) }
        sliceExpr.max?.let { range.third = frontend.expressionHandler.handle(it) }

        ase.subscriptExpression = range

        return ase
    }

    private fun handleTypeAssertExpr(
        typeAssertExpr: GoStandardLibrary.Ast.TypeAssertExpr
    ): CastExpression {
        val cast = newCastExpression(rawNode = typeAssertExpr)

        // Parse the inner expression
        cast.expression =
            handle(typeAssertExpr.x) ?: newProblemExpression("missing inner expression")

        // The type can be null, but only in certain circumstances, i.e, a type switch (which we do
        // not support yet)
        typeAssertExpr.type?.let { cast.castType = frontend.typeOf(it) }

        return cast
    }

    private fun handleUnaryExpr(unaryExpr: GoStandardLibrary.Ast.UnaryExpr): UnaryOperator {
        val op =
            newUnaryOperator(
                unaryExpr.opString,
                postfix = false,
                prefix = false,
                rawNode = unaryExpr
            )
        handle(unaryExpr.x)?.let { op.input = it }

        return op
    }

    /**
     * handleCompositeLit handles a composite literal, which we need to translate into a combination
     * of a ConstructExpression and a list of KeyValueExpressions. The problem is that we need to
     * add the list as a first argument of the construct expression.
     */
    private fun handleCompositeLit(
        compositeLit: GoStandardLibrary.Ast.CompositeLit
    ): ConstructExpression {
        // Parse the type field, to see which kind of expression it is
        val type = frontend.typeOf(compositeLit.type)

        val construct = newConstructExpression(type.name, rawNode = compositeLit)
        construct.type = type

        val list = newInitializerListExpression(rawNode = compositeLit)
        construct += list

        // Normally, the construct expression would not have DFG edge, but in this case we are
        // mis-using it to simulate an object literal, so we need to add a DFG here, otherwise a
        // declaration is disconnected from its initialization.
        construct.addPrevDFG(list)

        val expressions = mutableListOf<Expression>()
        for (elem in compositeLit.elts) {
            handle(elem)?.let { expressions += it }
        }

        list.initializers = expressions

        return construct
    }

    /*
        // handleFuncLit handles a function literal, which we need to translate into a combination of a
    // LambdaExpression and a function declaration.
         */
    fun handleFuncLit(funcLit: GoStandardLibrary.Ast.FuncLit): LambdaExpression {
        val lambda = newLambdaExpression(rawNode = funcLit)
        // Parse the expression as a function declaration with a little trick
        lambda.function =
            frontend.declarationHandler.handle(funcLit.toDecl()) as? FunctionDeclaration

        return lambda
    }
}
