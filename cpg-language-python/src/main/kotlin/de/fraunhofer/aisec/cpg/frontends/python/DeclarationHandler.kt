/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage.Companion.IDENTIFIER_INIT
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage.Companion.MODIFIER_KEYWORD_ONLY_ARGUMENT
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage.Companion.MODIFIER_POSITIONAL_ONLY_ARGUMENT
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.ast.Annotation
import de.fraunhofer.aisec.cpg.graph.ast.declarations.ConstructorDeclaration
import de.fraunhofer.aisec.cpg.graph.ast.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.ast.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.ast.declarations.MethodDeclaration
import de.fraunhofer.aisec.cpg.graph.ast.declarations.ParameterDeclaration
import de.fraunhofer.aisec.cpg.graph.ast.declarations.ProblemDeclaration
import de.fraunhofer.aisec.cpg.graph.ast.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.graph.scopes.RecordScope
import de.fraunhofer.aisec.cpg.graph.types.FunctionType.Companion.computeType
import de.fraunhofer.aisec.cpg.helpers.Util

/**
 * In Python, all definitions/declarations are statements. This class handles the parsing of
 * [Python.AST.Def] nodes to be represented as [ast.declarations.Declaration] nodes in our CPG.
 *
 * For declarations encountered directly on a namespace and classes, we directly invoke the
 * [DeclarationHandler], for others, the [StatementHandler] will forward these statements to us.
 */
class DeclarationHandler(frontend: PythonLanguageFrontend) :
    PythonHandler<Declaration, Python.AST.Def>(::ProblemDeclaration, frontend) {
    override fun handleNode(node: Python.AST.Def): Declaration {
        return when (node) {
            is Python.AST.FunctionDef -> handleFunctionDef(node)
            is Python.AST.AsyncFunctionDef -> handleFunctionDef(node)
            is Python.AST.ClassDef -> handleClassDef(node)
        }
    }

    /**
     * Translates a Python [`ClassDef`](https://docs.python.org/3/library/ast.html#ast.ClassDef)
     * into an [ast.declarations.RecordDeclaration].
     */
    private fun handleClassDef(stmt: Python.AST.ClassDef): RecordDeclaration {
        val cls = newRecordDeclaration(stmt.name, "class", rawNode = stmt)
        stmt.bases.map { cls.superClasses.add(frontend.typeOf(it)) }

        frontend.scopeManager.enterScope(cls)

        stmt.keywords.forEach {
            cls.additionalProblems +=
                newProblemExpression(problem = "could not parse keyword $it in class", rawNode = it)
        }

        for (s in stmt.body) {
            when (s) {
                // In order to be as compatible as possible with existing languages, we try to add
                // declarations directly to the class
                is Python.AST.Def -> {
                    val decl = handle(s)
                    frontend.scopeManager.addDeclaration(decl)
                    cls.addDeclaration(decl)
                }
                // All other statements are added to the statements block of the class
                else -> cls.statements += frontend.statementHandler.handle(s)
            }
        }

        frontend.scopeManager.leaveScope(cls)

        return cls
    }

    /**
     * We have to consider multiple things when matching Python's FunctionDef to the CPG:
     * - A [Python.AST.FunctionDef] could be one of
     *     - a [ConstructorDeclaration] if it appears in a record and its [name] is `__init__`
     *     - a [MethodDeclaration] if it appears in a record, and it isn't a
     *       [ConstructorDeclaration]
     *     - a [FunctionDeclaration] if neither of the above apply
     *
     * In case of a [ast.declarations.ConstructorDeclaration]
     * or[ast.declarations.MethodDeclaration]: the first argument is the `receiver` (most often
     * called `self`).
     */
    private fun handleFunctionDef(s: Python.AST.NormalOrAsyncFunctionDef): FunctionDeclaration {
        var recordDeclaration =
            (frontend.scopeManager.currentScope as? RecordScope)?.astNode as? RecordDeclaration
        val language = language
        val func =
            if (recordDeclaration != null) {
                if (s.name == IDENTIFIER_INIT) {
                    newConstructorDeclaration(
                        name = s.name,
                        recordDeclaration = recordDeclaration,
                        rawNode = s,
                    )
                } else if (language is HasOperatorOverloading && s.name.isKnownOperatorName) {
                    var decl =
                        newOperatorDeclaration(
                            name = s.name,
                            recordDeclaration = recordDeclaration,
                            operatorCode = language.operatorCodeFor(s.name) ?: "",
                            rawNode = s,
                        )
                    if (decl.operatorCode == "") {
                        Util.warnWithFileLocation(
                            decl,
                            log,
                            "Could not find operator code for operator {}. This will most likely result in a failure",
                            s.name,
                        )
                    }
                    decl
                } else {
                    newMethodDeclaration(
                        name = s.name,
                        recordDeclaration = recordDeclaration,
                        isStatic = false,
                        rawNode = s,
                    )
                }
            } else {
                newFunctionDeclaration(name = s.name, rawNode = s)
            }
        frontend.scopeManager.enterScope(func)

        frontend.statementHandler.addAsyncWarning(s, func)

        // Handle decorators (which are translated into CPG "annotations")
        func.annotations += handleAnnotations(s)

        // Handle return type and calculate function type
        if (func is ConstructorDeclaration) {
            // Return type of the constructor is always its record declaration type
            func.returnTypes = listOf(recordDeclaration?.toType() ?: unknownType())
        } else {
            func.returnTypes = listOf(frontend.typeOf(s.returns))
        }
        func.type = computeType(func)

        handleArguments(s.args, func, recordDeclaration)

        if (s.body.isNotEmpty()) {
            func.body = frontend.statementHandler.makeBlock(s.body, parentNode = s)
        }

        frontend.scopeManager.leaveScope(func)

        return func
    }

    /** Adds the arguments to [func] which might be located in a [recordDeclaration]. */
    private fun handleArguments(
        args: Python.AST.arguments,
        func: FunctionDeclaration,
        recordDeclaration: RecordDeclaration?,
    ) {
        // We can merge posonlyargs and args because both are positional arguments. We do not
        // enforce that posonlyargs can ONLY be used in a positional style, whereas args can be used
        // both in positional and keyword style.
        var positionalArguments = args.posonlyargs + args.args

        // Handle receiver if it exists and if it is not a static method
        if (
            recordDeclaration != null &&
                func.annotations.none { it.name.localName == "staticmethod" }
        ) {
            handleReceiverArgument(positionalArguments, args, func, recordDeclaration)
            // Skip the receiver argument for further processing
            positionalArguments = positionalArguments.drop(1)
        }

        // Handle remaining arguments
        handlePositionalArguments(func, positionalArguments, args)

        args.vararg?.let { handleArgument(func, it, isPosOnly = false, isVariadic = true) }
        args.kwarg?.let { handleArgument(func, it, isPosOnly = false, isVariadic = true) }

        handleKeywordOnlyArguments(func, args)
    }

    /**
     * This function creates a [newParameterDeclaration] for the argument, setting any modifiers
     * (like positional-only or keyword-only) and [defaultValue] if applicable.
     *
     * This also adds the [ast.declarations.ParameterDeclaration] to the
     * [ast.declarations.FunctionDeclaration.parameters].
     */
    internal fun handleArgument(
        func: FunctionDeclaration,
        node: Python.AST.arg,
        isPosOnly: Boolean = false,
        isVariadic: Boolean = false,
        isKwoOnly: Boolean = false,
        defaultValue: Expression? = null,
    ): ParameterDeclaration {
        val arg =
            newParameterDeclaration(
                name = node.arg,
                type = dynamicType(),
                variadic = isVariadic,
                rawNode = node,
            )
        arg.assignedTypes += frontend.typeOf(node.annotation)
        defaultValue?.let { arg.default = it }
        if (isPosOnly) {
            arg.modifiers += MODIFIER_POSITIONAL_ONLY_ARGUMENT
        }

        if (isKwoOnly) {
            arg.modifiers += MODIFIER_KEYWORD_ONLY_ARGUMENT
        }

        frontend.scopeManager.addDeclaration(arg)
        func.parameters += arg

        return arg
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
        recordDeclaration: RecordDeclaration,
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
                    rawNode = recvPythonNode,
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
                            rawNode = result,
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
        func: FunctionDeclaration,
        positionalArguments: List<Python.AST.arg>,
        args: Python.AST.arguments,
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
            handleArgument(
                func,
                arg,
                isPosOnly = arg in args.posonlyargs,
                defaultValue = defaultValue,
            )
        }
    }

    /**
     * This method extracts the keyword-only arguments from [args] and maps them to the
     * corresponding function parameters.
     */
    private fun handleKeywordOnlyArguments(func: FunctionDeclaration, args: Python.AST.arguments) {
        for (idx in args.kwonlyargs.indices) {
            val arg = args.kwonlyargs[idx]
            val default = args.kw_defaults.getOrNull(idx)
            handleArgument(
                func,
                arg,
                isPosOnly = false,
                isKwoOnly = true,
                defaultValue = default?.let { frontend.expressionHandler.handle(it) },
            )
        }
    }

    internal fun handleAnnotations(
        node: Python.AST.NormalOrAsyncFunctionDef
    ): Collection<Annotation> {
        return handleDeclaratorList(node, node.decorator_list)
    }

    fun handleDeclaratorList(
        node: Python.AST.WithLocation,
        decoratorList: List<Python.AST.BaseExpr>,
    ): List<Annotation> {
        val annotations = mutableListOf<Annotation>()
        for (decorator in decoratorList) {
            var annotation =
                when (decorator) {
                    is Python.AST.Name -> {
                        val parsedDecorator = frontend.expressionHandler.handle(decorator)
                        newAnnotation(name = parsedDecorator.name, rawNode = decorator)
                    }
                    is Python.AST.Attribute -> {
                        val parsedDecorator = frontend.expressionHandler.handle(decorator)
                        val name =
                            if (parsedDecorator is MemberExpression) {
                                parsedDecorator.base.name.fqn(parsedDecorator.name.localName)
                            } else {
                                parsedDecorator.name
                            }
                        newAnnotation(name = name, rawNode = decorator)
                    }
                    is Python.AST.Call -> {
                        val parsedDecorator = frontend.expressionHandler.handle(decorator.func)
                        val name =
                            if (parsedDecorator is MemberExpression) {
                                parsedDecorator.base.name.fqn(parsedDecorator.name.localName)
                            } else {
                                parsedDecorator.name
                            }
                        val annotation = newAnnotation(name = name, rawNode = decorator)
                        for (arg in decorator.args) {
                            val argParsed = frontend.expressionHandler.handle(arg)
                            annotation.members +=
                                newAnnotationMember(
                                    "annotationArg" + decorator.args.indexOf(arg), // TODO
                                    argParsed,
                                    rawNode = arg,
                                )
                        }
                        for (keyword in decorator.keywords) {
                            annotation.members +=
                                newAnnotationMember(
                                    name = keyword.arg,
                                    value = frontend.expressionHandler.handle(keyword.value),
                                    rawNode = keyword,
                                )
                        }
                        annotation
                    }
                    else -> {
                        Util.warnWithFileLocation(
                            frontend,
                            decorator,
                            log,
                            "Decorator is of type ${decorator::class}, cannot handle this (yet).",
                        )
                        continue
                    }
                }

            annotations += annotation
        }

        return annotations
    }
}
