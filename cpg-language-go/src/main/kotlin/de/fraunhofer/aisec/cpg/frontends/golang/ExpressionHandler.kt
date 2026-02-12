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
import de.fraunhofer.aisec.cpg.graph.scopes.NameScope
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.PointerType
import de.fraunhofer.aisec.cpg.graph.types.Type
import java.math.BigInteger

class ExpressionHandler(frontend: GoLanguageFrontend) :
    GoHandler<Expression, GoStandardLibrary.Ast.Expr>(::ProblemExpression, frontend) {

    override fun handleNode(node: GoStandardLibrary.Ast.Expr): Expression {
        return when (node) {
            is GoStandardLibrary.Ast.BasicLit -> handleBasicLit(node)
            is GoStandardLibrary.Ast.BinaryExpr -> handleBinaryExpr(node)
            is GoStandardLibrary.Ast.CompositeLit -> handleCompositeLit(node)
            is GoStandardLibrary.Ast.FuncLit -> handleFuncLit(node)
            is GoStandardLibrary.Ast.Ident -> handleIdent(node)
            is GoStandardLibrary.Ast.IndexExpr -> handleIndexExpr(node)
            is GoStandardLibrary.Ast.CallExpr -> handleCallExpr(node)
            is GoStandardLibrary.Ast.KeyValueExpr -> handleKeyValueExpr(node)
            is GoStandardLibrary.Ast.ParenExpr -> {
                handle(node.x)
            }
            is GoStandardLibrary.Ast.SelectorExpr -> handleSelectorExpr(node)
            is GoStandardLibrary.Ast.SliceExpr -> handleSliceExpr(node)
            is GoStandardLibrary.Ast.StarExpr -> handleStarExpr(node)
            is GoStandardLibrary.Ast.TypeAssertExpr -> handleTypeAssertExpr(node)
            is GoStandardLibrary.Ast.UnaryExpr -> handleUnaryExpr(node)
            else -> {
                handleNotSupported(node, node.goType)
            }
        }
    }

    private fun handleBasicLit(basicLit: GoStandardLibrary.Ast.BasicLit): Literal<*> {
        var rawValue = basicLit.value
        var value: Any?
        val type: Type
        when (basicLit.kind) {
            STRING -> {
                value =
                    rawValue.substring(
                        1.coerceAtMost(rawValue.length - 1),
                        (rawValue.length - 1).coerceAtLeast(0),
                    )
                type = primitiveType("string")
            }
            INT -> {
                // Get rid of all underscores
                rawValue = rawValue.replace("_", "")
                val prefix = rawValue.substring(0, 2.coerceAtMost(rawValue.length))
                val postfix = rawValue.substring(2.coerceAtMost(rawValue.length), rawValue.length)

                value =
                    when (prefix) {
                        "0x" -> BigInteger(postfix, 16)
                        "0o" -> BigInteger(postfix, 10)
                        "0b" -> BigInteger(postfix, 2)
                        else -> BigInteger(rawValue, 10)
                    }

                value =
                    when {
                        value > BigInteger.valueOf(Long.MAX_VALUE) -> {
                            value
                        }
                        value.toLong() > Int.MAX_VALUE -> {
                            value.toLong()
                        }
                        else -> {
                            value.toInt()
                        }
                    }
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
        binOp.lhs = handle(binaryExpr.x)
        binOp.rhs = handle(binaryExpr.y)

        return binOp
    }

    private fun handleIdent(ident: GoStandardLibrary.Ast.Ident): Expression {
        val builtinLiterals =
            mapOf(
                "nil" to Pair(unknownType(), null),
                "true" to Pair(primitiveType("bool"), true),
                "false" to Pair(primitiveType("bool"), false),
                "iota" to Pair(primitiveType("int"), frontend.declCtx.iotaValue),
            )

        // Check, if this is one of the builtinLiterals and handle them as a literal
        val literalPair = builtinLiterals[ident.name]
        if (literalPair != null) {
            val (type, value) = literalPair
            val literal = newLiteral(value, type, rawNode = ident)
            literal.name = parseName(ident.name)

            return literal
        }

        // If we are directly in a name scope, make sure we FQN'ize the name, to help with
        // resolution; unless it is already an FQN or a package name.
        var name: CharSequence = ident.name
        name =
            when {
                name in builtins -> name
                isPackageName(name) -> name
                language.namespaceDelimiter in name -> name
                else -> parseName((scope as? NameScope)?.name?.fqn(ident.name) ?: ident.name)
            }

        return newReference(name, rawNode = ident)
    }

    private fun handleIndexExpr(indexExpr: GoStandardLibrary.Ast.IndexExpr): SubscriptExpression {
        val ase = newSubscriptExpression(rawNode = indexExpr)
        ase.arrayExpression = frontend.expressionHandler.handle(indexExpr.x)
        ase.subscriptExpression = frontend.expressionHandler.handle(indexExpr.index)

        return ase
    }

    private fun handleCallExpr(callExpr: GoStandardLibrary.Ast.CallExpr): Expression {
        // In Go, regular cast expressions (not type asserts are modeled as calls).
        // In this case, the Fun contains a type expression.
        when (val unwrapped = unwrap(callExpr.`fun`)) {
            is GoStandardLibrary.Ast.ArrayType,
            is GoStandardLibrary.Ast.ChanType,
            is GoStandardLibrary.Ast.FuncType,
            is GoStandardLibrary.Ast.InterfaceType,
            is GoStandardLibrary.Ast.StructType,
            is GoStandardLibrary.Ast.MapType -> {
                val cast = newCastExpression(rawNode = callExpr)
                cast.castType = frontend.typeOf(unwrapped)

                if (callExpr.args.isNotEmpty()) {
                    cast.expression = frontend.expressionHandler.handle(callExpr.args[0])
                }

                return cast
            }
        }

        val typeConstraints = mutableListOf<Node>()

        val callee =
            when (val `fun` = callExpr.`fun`) {
                // If "fun" is either an index or an index list expression, this is a call with type
                // constraints. We do not fully support that yet, but we can at least try to set
                // some of the parameters as template parameters
                is GoStandardLibrary.Ast.IndexExpr -> {
                    (frontend.typeOf(`fun`) as? ObjectType)?.generics?.let { typeConstraints += it }
                    this.handle(`fun`.x)
                }
                is GoStandardLibrary.Ast.IndexListExpr -> {
                    (frontend.typeOf(`fun`) as? ObjectType)?.generics?.let { typeConstraints += it }
                    this.handle(`fun`.x)
                }
                else -> {
                    this.handle(callExpr.`fun`)
                }
            }

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
        call.type = unknownType()

        // TODO(oxisto) Add type constraints
        if (typeConstraints.isNotEmpty()) {
            log.debug(
                "Call {} has type constraints ({}), but we cannot add them to the call expression yet",
                call.name,
                typeConstraints.joinToString(", ") { it.name },
            )
        }

        // Parse and add call arguments
        for (arg in callExpr.args) {
            call += handle(arg)
        }

        return call
    }

    /**
     * Unwrap is a small helper that unwraps an AST element that is wrapped into parenthesis. This
     * is needed because we sometimes need to "peek" into the AST and this is not possible if the
     * expression we are looking for is wrapped in parenthesis.
     */
    private fun unwrap(expr: GoStandardLibrary.Ast.Expr): GoStandardLibrary.Ast.Expr {
        return if (expr is GoStandardLibrary.Ast.ParenExpr) {
            unwrap(expr.x)
        } else {
            expr
        }
    }

    private fun handleKeyValueExpr(
        keyValueExpr: GoStandardLibrary.Ast.KeyValueExpr
    ): KeyValueExpression {
        val key = handle(keyValueExpr.key)
        val value = handle(keyValueExpr.value)

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
                val array = newNewArrayExpression(rawNode = callExpr)

                // second argument is a dimension (if this is an array), usually a literal
                if (args.size > 1) {
                    array.addDimension(handle(args[1]))
                }
                array
            } else {
                // Create at least a generic construct expression for the given map or channel type
                // and provide the remaining arguments
                val construct = newConstructExpression(rawNode = callExpr)

                // Pass the remaining arguments
                for (expr in args.subList(1.coerceAtMost(args.size - 1), args.size - 1)) {
                    handle(expr).let { construct += it }
                }

                construct
            }

        // First argument is always the type
        expression.type = frontend.typeOf(callExpr.args[0])

        return expression
    }

    private fun handleSelectorExpr(selectorExpr: GoStandardLibrary.Ast.SelectorExpr): Reference {
        val base = handle(selectorExpr.x)

        // Check, if this just a regular reference to a variable with a package scope and not a
        // member expression
        val isMemberExpression = !isPackageName(base.name.localName)

        val ref =
            if (isMemberExpression) {
                newMemberExpression(selectorExpr.sel.name, base, rawNode = selectorExpr)
            } else {
                // we need to set the name to a FQN-style, including the package scope. the call
                // resolver will then resolve this
                val fqn = "${base.name}.${selectorExpr.sel.name}"

                newReference(fqn, rawNode = selectorExpr)
            }

        return ref
    }

    private fun isPackageName(name: CharSequence): Boolean {
        for (imp in frontend.currentFile?.imports ?: listOf()) {
            // If we have an alias, we need to check it instead of the import name
            val packageName = imp.name?.name ?: imp.importName

            if (name == packageName) {
                // found a package name
                return true
            }
        }

        return false
    }

    /**
     * This function handles an ast.SliceExpr, which is an extended version of ast.IndexExpr. We are
     * modeling this as a combination of a [SubscriptExpression] that contains a [RangeExpression]
     * as its subscriptExpression to share some code between this and an index expression.
     */
    private fun handleSliceExpr(sliceExpr: GoStandardLibrary.Ast.SliceExpr): SubscriptExpression {
        val ase = newSubscriptExpression(rawNode = sliceExpr)
        ase.arrayExpression = frontend.expressionHandler.handle(sliceExpr.x)

        // Build the slice expression
        val range = newRangeExpression(rawNode = sliceExpr)
        sliceExpr.low?.let { range.floor = frontend.expressionHandler.handle(it) }
        sliceExpr.high?.let { range.ceiling = frontend.expressionHandler.handle(it) }
        sliceExpr.max?.let { range.third = frontend.expressionHandler.handle(it) }

        ase.subscriptExpression = range

        return ase
    }

    private fun handleStarExpr(starExpr: GoStandardLibrary.Ast.StarExpr): UnaryOperator {
        val op = newUnaryOperator("*", postfix = false, prefix = false, rawNode = starExpr)
        op.input = handle(starExpr.x)

        return op
    }

    private fun handleTypeAssertExpr(
        typeAssertExpr: GoStandardLibrary.Ast.TypeAssertExpr
    ): Expression {
        // This can either be a regular type assertion, which we handle as a cast expression or the
        // "special" type assertion `.(type)`, which is used in a type switch to retrieve the type
        // of the variable. In this case we treat it as a special unary operator.
        return if (typeAssertExpr.type == null) {
            val op =
                newUnaryOperator(
                    ".(type)",
                    postfix = true,
                    prefix = false,
                    rawNode = typeAssertExpr,
                )
            op.input = handle(typeAssertExpr.x)
            op
        } else {
            val cast = newCastExpression(rawNode = typeAssertExpr)

            // Parse the inner expression
            cast.expression = handle(typeAssertExpr.x)

            // The type can be null, but only in certain circumstances, i.e, a type switch
            typeAssertExpr.type?.let { cast.castType = frontend.typeOf(it) }
            cast
        }
    }

    private fun handleUnaryExpr(unaryExpr: GoStandardLibrary.Ast.UnaryExpr): UnaryOperator {
        val op =
            newUnaryOperator(
                unaryExpr.opString,
                postfix = false,
                prefix = false,
                rawNode = unaryExpr,
            )
        op.input = handle(unaryExpr.x)

        return op
    }

    /**
     * handleCompositeLit handles a composite literal, which we need to translate into a combination
     * of a ConstructExpression and a list of KeyValueExpressions. The problem is that we need to
     * add the list as a first argument of the construct expression.
     */
    private fun handleCompositeLit(compositeLit: GoStandardLibrary.Ast.CompositeLit): Expression {
        // Parse the type field, to see which kind of expression it is. The type of a composite
        // literal can be omitted if this is an "inner" composite literal, so we need to set it from
        // the "outer" one. See below
        val type = compositeLit.type?.let { frontend.typeOf(it) } ?: unknownType()

        val list = newInitializerListExpression(type, rawNode = compositeLit)
        list.type = type

        val expressions = mutableListOf<Expression>()
        for (elem in compositeLit.elts) {
            val expression = handle(elem)
            expressions += expression
        }

        list.initializers = expressions

        return list
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

    companion object {
        val builtins =
            listOf(
                "bool",
                "uint8",
                "uint16",
                "uint32",
                "uint64",
                "int8",
                "int16",
                "int32",
                "int64",
                "float32",
                "float64",
                "complex64",
                "complex128",
                "string",
                "int",
                "uint",
                "uintptr",
                "byte",
                "rune",
                "any",
                "comparable",
                "iota",
                "nil",
                "append",
                "copy",
                "delete",
                "len",
                "cap",
                "make",
                "max",
                "min",
                "new",
                "complex",
                "real",
                "imag",
                "clear",
                "close",
                "panic",
                "recover",
                "print",
                "println",
                "error",
            )
    }
}
