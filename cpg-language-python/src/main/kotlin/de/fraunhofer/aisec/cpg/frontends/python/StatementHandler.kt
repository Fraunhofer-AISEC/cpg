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

import de.fraunhofer.aisec.cpg.frontends.HasOperatorOverloading
import de.fraunhofer.aisec.cpg.frontends.isKnownOperatorName
import de.fraunhofer.aisec.cpg.frontends.python.Python.AST.IsAsync
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage.Companion.MODIFIER_KEYWORD_ONLY_ARGUMENT
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage.Companion.MODIFIER_POSITIONAL_ONLY_ARGUMENT
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.Annotation
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.AssertStatement
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ProblemExpression
import de.fraunhofer.aisec.cpg.graph.types.FunctionType
import de.fraunhofer.aisec.cpg.helpers.Util
import kotlin.collections.plusAssign

class StatementHandler(frontend: PythonLanguageFrontend) :
    PythonHandler<Statement, Python.AST.BaseStmt>(::ProblemExpression, frontend) {

    override fun handleNode(node: Python.AST.BaseStmt): Statement {
        return when (node) {
            is Python.AST.ClassDef -> handleClassDef(node)
            is Python.AST.FunctionDef -> handleFunctionDef(node)
            is Python.AST.AsyncFunctionDef -> handleFunctionDef(node)
            is Python.AST.Pass -> return newEmptyStatement(rawNode = node)
            is Python.AST.ImportFrom -> handleImportFrom(node)
            is Python.AST.Assign -> handleAssign(node)
            is Python.AST.AugAssign -> handleAugAssign(node)
            is Python.AST.Return -> handleReturn(node)
            is Python.AST.If -> handleIf(node)
            is Python.AST.AnnAssign -> handleAnnAssign(node)
            is Python.AST.Expr -> handleExpressionStatement(node)
            is Python.AST.For -> handleFor(node)
            is Python.AST.AsyncFor -> handleFor(node)
            is Python.AST.While -> handleWhile(node)
            is Python.AST.Import -> handleImport(node)
            is Python.AST.Break -> newBreakStatement(rawNode = node)
            is Python.AST.Continue -> newContinueStatement(rawNode = node)
            is Python.AST.Assert -> handleAssert(node)
            is Python.AST.Delete,
            is Python.AST.Global,
            is Python.AST.Match,
            is Python.AST.Nonlocal,
            is Python.AST.Raise,
            is Python.AST.Try,
            is Python.AST.TryStar,
            is Python.AST.With,
            is Python.AST.AsyncWith ->
                newProblemExpression(
                    "The statement of class ${node.javaClass} is not supported yet",
                    rawNode = node
                )
        }
    }

    /**
     * Translates a Python (https://docs.python.org/3/library/ast.html#ast.Assert] into a
     * [AssertStatement].
     */
    private fun handleAssert(node: Python.AST.Assert): AssertStatement {
        val assertStatement = newAssertStatement(rawNode = node)
        val testExpression = frontend.expressionHandler.handle(node.test)
        assertStatement.condition = testExpression
        node.msg?.let { assertStatement.message = frontend.expressionHandler.handle(it) }

        return assertStatement
    }

    private fun handleImport(node: Python.AST.Import): Statement {
        val declStmt = newDeclarationStatement(rawNode = node)
        for (imp in node.names) {
            val alias = imp.asname
            val decl =
                if (alias != null) {
                    newImportDeclaration(
                        parseName(imp.name),
                        false,
                        parseName(alias),
                        rawNode = imp
                    )
                } else {
                    newImportDeclaration(parseName(imp.name), false, rawNode = imp)
                }
            frontend.scopeManager.addDeclaration(decl)
            declStmt.declarationEdges += decl
        }
        return declStmt
    }

    private fun handleImportFrom(node: Python.AST.ImportFrom): Statement {
        val declStmt = newDeclarationStatement(rawNode = node)
        val level = node.level
        if (level == null || level > 0) {
            return newProblemExpression(
                "not supporting relative paths in from (...) import syntax yet"
            )
        }

        val module = parseName(node.module ?: "")
        for (imp in node.names) {
            // We need to differentiate between a wildcard import and an individual symbol.
            // Wildcards luckily do not have aliases
            val decl =
                if (imp.name == "*") {
                    // In the wildcard case, our "import" is the module name, and we set "wildcard"
                    // to true
                    newImportDeclaration(module, true, rawNode = imp)
                } else {
                    // If we import an individual symbol, we need to FQN the symbol with our module
                    // name and import that. We also need to take care of any alias
                    val name = module.fqn(imp.name)
                    val alias = imp.asname
                    if (alias != null) {
                        newImportDeclaration(name, false, parseName(alias), rawNode = imp)
                    } else {
                        newImportDeclaration(name, false, rawNode = imp)
                    }
                }

            // Finally, add our declaration to the scope and the declaration statement
            frontend.scopeManager.addDeclaration(decl)
            declStmt.declarationEdges += decl
        }
        return declStmt
    }

    private fun handleWhile(node: Python.AST.While): Statement {
        val ret = newWhileStatement(rawNode = node)
        ret.condition = frontend.expressionHandler.handle(node.test)
        ret.statement = makeBlock(node.body, parentNode = node)
        if (node.orelse.isNotEmpty()) {
            ret.additionalProblems +=
                newProblemExpression(
                    problem = "Cannot handle \"orelse\" in while loops.",
                    rawNode = node
                )
        }
        return ret
    }

    private fun handleFor(node: Python.AST.NormalOrAsyncFor): Statement {
        val ret = newForEachStatement(rawNode = node)
        if (node is IsAsync) {
            ret.addDeclaration(
                newProblemDeclaration(
                    problem = "The \"async\" keyword is not yet supported.",
                    rawNode = node
                )
            )
        }

        ret.iterable = frontend.expressionHandler.handle(node.iter)
        ret.variable = frontend.expressionHandler.handle(node.target)
        ret.statement = makeBlock(node.body, parentNode = node)
        if (node.orelse.isNotEmpty()) {
            ret.additionalProblems +=
                newProblemExpression(
                    problem = "Cannot handle \"orelse\" in for loops.",
                    rawNode = node
                )
        }
        return ret
    }

    private fun handleExpressionStatement(node: Python.AST.Expr): Statement {
        return frontend.expressionHandler.handle(node.value)
    }

    private fun handleAnnAssign(node: Python.AST.AnnAssign): Statement {
        // TODO: annotations
        val lhs = frontend.expressionHandler.handle(node.target)
        return if (node.value != null) {
            newAssignExpression(
                lhs = listOf(lhs),
                rhs = listOf(frontend.expressionHandler.handle(node.value!!)), // TODO !!
                rawNode = node
            )
        } else {
            lhs
        }
    }

    private fun handleIf(node: Python.AST.If): Statement {
        val ret = newIfStatement(rawNode = node)
        ret.condition = frontend.expressionHandler.handle(node.test)
        ret.thenStatement =
            if (node.body.isNotEmpty()) {
                makeBlock(node.body, parentNode = node)
            } else {
                null
            }
        ret.elseStatement =
            if (node.orelse.isNotEmpty()) {
                makeBlock(node.orelse, parentNode = node)
            } else {
                null
            }
        return ret
    }

    private fun handleReturn(node: Python.AST.Return): Statement {
        val ret = newReturnStatement(rawNode = node)
        node.value?.let { ret.returnValue = frontend.expressionHandler.handle(it) }
        return ret
    }

    private fun handleAssign(node: Python.AST.Assign): Statement {
        val lhs = node.targets.map { frontend.expressionHandler.handle(it) }
        val rhs = frontend.expressionHandler.handle(node.value)
        if (rhs is List<*>)
            newAssignExpression(
                lhs = lhs,
                rhs =
                    rhs.map {
                        (it as? Expression)
                            ?: newProblemExpression(
                                "There was an issue with an argument.",
                                rawNode = node
                            )
                    },
                rawNode = node
            )
        return newAssignExpression(lhs = lhs, rhs = listOf(rhs), rawNode = node)
    }

    private fun handleAugAssign(node: Python.AST.AugAssign): Statement {
        val lhs = frontend.expressionHandler.handle(node.target)
        val rhs = frontend.expressionHandler.handle(node.value)
        val op = frontend.operatorToString(node.op) + "="
        return newAssignExpression(
            operatorCode = op,
            lhs = listOf(lhs),
            rhs = listOf(rhs),
            rawNode = node
        )
    }

    private fun handleClassDef(stmt: Python.AST.ClassDef): Statement {
        val cls = newRecordDeclaration(stmt.name, "class", rawNode = stmt)
        stmt.bases.map { cls.superClasses.add(frontend.typeOf(it)) }

        frontend.scopeManager.enterScope(cls)

        stmt.keywords.forEach {
            cls.additionalProblems +=
                newProblemExpression(problem = "could not parse keyword $it in class", rawNode = it)
        }

        for (s in stmt.body) {
            when (s) {
                is Python.AST.FunctionDef -> handleFunctionDef(s, cls)
                else -> cls.statements += handleNode(s)
            }
        }

        frontend.scopeManager.leaveScope(cls)
        frontend.scopeManager.addDeclaration(cls)

        return wrapDeclarationToStatement(cls)
    }

    /**
     * We have to consider multiple things when matching Python's FunctionDef to the CPG:
     * - A [Python.AST.FunctionDef] is a [Statement] from Python's point of view. The CPG sees it as
     *   a declaration -> we have to wrap the result in a [DeclarationStatement].
     * - A [Python.AST.FunctionDef] could be one of
     *     - a [ConstructorDeclaration] if it appears in a record and its [name] is `__init__`
     *     - a [MethodeDeclaration] if it appears in a record, and it isn't a
     *       [ConstructorDeclaration]
     *     - a [FunctionDeclaration] if neither of the above apply
     *
     * In case of a [ConstructorDeclaration] or[MethodDeclaration]: the first argument is the
     * `receiver` (most often called `self`).
     */
    private fun handleFunctionDef(
        s: Python.AST.NormalOrAsyncFunctionDef,
        recordDeclaration: RecordDeclaration? = null
    ): DeclarationStatement {
        val language = language
        val result =
            if (recordDeclaration != null) {
                if (s.name == "__init__") {
                    newConstructorDeclaration(
                        name = s.name,
                        recordDeclaration = recordDeclaration,
                        rawNode = s
                    )
                } else if (language is HasOperatorOverloading && s.name.isKnownOperatorName) {
                    var decl =
                        newOperatorDeclaration(
                            name = s.name,
                            recordDeclaration = recordDeclaration,
                            operatorCode = language.operatorCodeFor(s.name) ?: "",
                            rawNode = s
                        )
                    if (decl.operatorCode == "") {
                        Util.warnWithFileLocation(
                            decl,
                            log,
                            "Could not find operator code for operator {}. This will most likely result in a failure",
                            s.name
                        )
                    }
                    decl
                } else {
                    newMethodDeclaration(
                        name = s.name,
                        recordDeclaration = recordDeclaration,
                        isStatic = false,
                        rawNode = s
                    )
                }
            } else {
                newFunctionDeclaration(name = s.name, rawNode = s)
            }
        frontend.scopeManager.enterScope(result)

        if (s is Python.AST.AsyncFunctionDef) {
            result.addDeclaration(
                newProblemDeclaration(
                    problem = "The \"async\" keyword is not yet supported.",
                    rawNode = s
                )
            )
        }

        // Handle decorators (which are translated into CPG "annotations")
        result.annotations += handleAnnotations(s)

        // Handle return type and calculate function type
        if (result is ConstructorDeclaration) {
            // Return type of the constructor is always its record declaration type
            result.returnTypes = listOf(recordDeclaration?.toType() ?: unknownType())
        } else {
            result.returnTypes = listOf(frontend.typeOf(s.returns))
        }
        result.type = FunctionType.computeType(result)

        handleArguments(s.args, result, recordDeclaration)

        if (s.body.isNotEmpty()) {
            result.body = makeBlock(s.body, parentNode = s)
        }

        frontend.scopeManager.leaveScope(result)
        frontend.scopeManager.addDeclaration(result)

        return wrapDeclarationToStatement(result)
    }

    /** Adds the arguments to [result] which might be located in a [recordDeclaration]. */
    private fun handleArguments(
        args: Python.AST.arguments,
        result: FunctionDeclaration,
        recordDeclaration: RecordDeclaration?
    ) {
        // We can merge posonlyargs and args because both are positional arguments. We do not
        // enforce that posonlyargs can ONLY be used in a positional style, whereas args can be used
        // both in positional and keyword style.
        var positionalArguments = args.posonlyargs + args.args

        // Handle receiver if it exists
        if (recordDeclaration != null) {
            handleReceiverArgument(positionalArguments, args, result, recordDeclaration)
            // Skip the receiver argument for further processing
            positionalArguments = positionalArguments.drop(1)
        }

        // Handle remaining arguments
        handlePositionalArguments(positionalArguments, args)

        args.vararg?.let { handleArgument(it, isPosOnly = false, isVariadic = true) }
        args.kwarg?.let { handleArgument(it, isPosOnly = false, isVariadic = false) }

        handleKeywordOnlyArguments(args)
    }

    /**
     * This method retrieves the first argument of the [positionalArguments], which is typically the
     * receiver object.
     *
     * A receiver can also have a default value. However, this case is not handled and is therefore
     * passed with a problem expression.
     */
    private fun handleReceiverArgument(
        positionalArguments: List<Python.AST.arg>,
        args: Python.AST.arguments,
        result: FunctionDeclaration,
        recordDeclaration: RecordDeclaration
    ) {
        // first argument is the receiver
        val recvPythonNode = positionalArguments.firstOrNull()
        if (recvPythonNode == null) {
            result.additionalProblems += newProblemExpression("Expected a receiver", rawNode = args)
        } else {
            val tpe = recordDeclaration.toType()
            val recvNode =
                newVariableDeclaration(
                    name = recvPythonNode.arg,
                    type = tpe,
                    implicitInitializerAllowed = false,
                    rawNode = recvPythonNode
                )

            // If the number of defaults equals the number of positional arguments, the receiver has
            // a default value
            if (args.defaults.size == positionalArguments.size) {
                val defaultValue =
                    args.defaults.getOrNull(0)?.let { frontend.expressionHandler.handle(it) }
                defaultValue?.let {
                    frontend.scopeManager.addDeclaration(recvNode)
                    result.additionalProblems +=
                        newProblemExpression("Receiver with default value", rawNode = args)
                }
            }

            when (result) {
                is ConstructorDeclaration,
                is MethodDeclaration -> result.receiver = recvNode
                else ->
                    result.additionalProblems +=
                        newProblemExpression(
                            problem =
                                "Expected a constructor or method declaration. Got something else.",
                            rawNode = result
                        )
            }
        }
    }

    /**
     * This method extracts the [positionalArguments] including those that may have default values.
     *
     * In Python only the arguments with default values are stored in `args.defaults`.
     * https://docs.python.org/3/library/ast.html#ast.arguments
     *
     * For example: `def my_func(a, b=1, c=2): pass`
     *
     * In this case, `args.defaults` contains only the defaults for `b` and `c`, while `args.args`
     * includes all arguments (`a`, `b` and `c`). The number of arguments without defaults is
     * determined by subtracting the size of `args.defaults` from the total number of arguments.
     * This matches each default to its corresponding argument.
     *
     * From the Python docs: "If there are fewer defaults, they correspond to the last n arguments."
     */
    private fun handlePositionalArguments(
        positionalArguments: List<Python.AST.arg>,
        args: Python.AST.arguments
    ) {
        val nonDefaultArgsCount = positionalArguments.size - args.defaults.size

        for (idx in positionalArguments.indices) {
            val arg = positionalArguments[idx]
            val defaultIndex = idx - nonDefaultArgsCount
            val defaultValue =
                if (defaultIndex >= 0) {
                    args.defaults.getOrNull(defaultIndex)?.let {
                        frontend.expressionHandler.handle(it)
                    }
                } else {
                    null
                }
            handleArgument(arg, isPosOnly = arg in args.posonlyargs, defaultValue = defaultValue)
        }
    }

    /**
     * This method extracts the keyword-only arguments from [args] and maps them to the
     * corresponding function parameters.
     */
    private fun handleKeywordOnlyArguments(args: Python.AST.arguments) {
        for (idx in args.kwonlyargs.indices) {
            val arg = args.kwonlyargs[idx]
            val default = args.kw_defaults.getOrNull(idx)
            handleArgument(
                arg,
                isPosOnly = false,
                isKwoOnly = true,
                defaultValue = default?.let { frontend.expressionHandler.handle(it) }
            )
        }
    }

    private fun handleAnnotations(
        node: Python.AST.NormalOrAsyncFunctionDef
    ): Collection<Annotation> {
        return handleDeclaratorList(node, node.decorator_list)
    }

    fun handleDeclaratorList(
        node: Python.AST.WithLocation,
        decoratorList: List<Python.AST.BaseExpr>
    ): List<Annotation> {
        val annotations = mutableListOf<Annotation>()
        for (decorator in decoratorList) {
            if (decorator !is Python.AST.Call) {
                log.warn(
                    "Decorator (${decorator::class}) is not ASTCall, cannot handle this (yet)."
                )
                continue
            }

            val decFuncParsed = frontend.expressionHandler.handle(decorator.func)
            if (decFuncParsed !is MemberExpression) {
                log.warn(
                    "parsed function expression (${decFuncParsed::class}) is not a member expression, cannot handle this (yet)."
                )
                continue
            }

            val annotation =
                newAnnotation(
                    name =
                        Name(
                            localName = decFuncParsed.name.localName,
                            parent = decFuncParsed.base.name
                        ),
                    rawNode = node
                )
            for (arg in decorator.args) {
                val argParsed = frontend.expressionHandler.handle(arg)
                annotation.members +=
                    newAnnotationMember(
                        "annotationArg" + decorator.args.indexOf(arg), // TODO
                        argParsed,
                        rawNode = arg
                    )
            }
            for (keyword in decorator.keywords) {
                annotation.members +=
                    newAnnotationMember(
                        name = keyword.arg,
                        value = frontend.expressionHandler.handle(keyword.value),
                        rawNode = keyword
                    )
            }

            annotations += annotation
        }

        return annotations
    }

    /**
     * This function "wraps" a list of [Python.AST.BaseStmt] nodes into a [Block]. Since the list
     * itself does not have a code/location, we need to employ [codeAndLocationFromChildren] on the
     * [parentNode].
     */
    private fun makeBlock(
        stmts: List<Python.AST.BaseStmt>,
        parentNode: Python.AST.WithLocation
    ): Block {
        val result = newBlock()
        for (stmt in stmts) {
            result.statements += handle(stmt)
        }

        // Try to retrieve the code and location from the parent node, if it is a base stmt
        var baseStmt = parentNode as? Python.AST.BaseStmt
        return if (baseStmt != null) {
            result.codeAndLocationFromChildren(baseStmt)
        } else {
            // Otherwise, continue without setting the location
            log.warn(
                "Could not set location on wrapped block because the parent node is not a python statement"
            )
            result
        }
    }

    /**
     * This function creates a [newParameterDeclaration] for the argument, setting any modifiers
     * (like positional-only or keyword-only) and [defaultValue] if applicable.
     */
    internal fun handleArgument(
        node: Python.AST.arg,
        isPosOnly: Boolean = false,
        isVariadic: Boolean = false,
        isKwoOnly: Boolean = false,
        defaultValue: Expression? = null
    ) {
        val type = frontend.typeOf(node.annotation)
        val arg =
            newParameterDeclaration(
                name = node.arg,
                type = type,
                variadic = isVariadic,
                rawNode = node
            )
        defaultValue?.let { arg.default = it }
        if (isPosOnly) {
            arg.modifiers += MODIFIER_POSITIONAL_ONLY_ARGUMENT
        }

        if (isKwoOnly) {
            arg.modifiers += MODIFIER_KEYWORD_ONLY_ARGUMENT
        }

        frontend.scopeManager.addDeclaration(arg)
    }

    /**
     * Wrap a declaration in a [DeclarationStatement]
     *
     * @param decl The [Declaration] to be wrapped
     * @return The wrapped [decl]
     */
    private fun wrapDeclarationToStatement(decl: Declaration): DeclarationStatement {
        val declStmt = newDeclarationStatement().codeAndLocationFrom(decl)
        declStmt.addDeclaration(decl)
        return declStmt
    }
}
