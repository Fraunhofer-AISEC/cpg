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
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage.Companion.MODIFIER_KEYWORD_ONLY_ARGUMENT
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage.Companion.MODIFIER_POSITIONAL_ONLY_ARGUMENT
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.scopes.FunctionScope
import de.fraunhofer.aisec.cpg.graph.scopes.RecordScope
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.types.FunctionType
import de.fraunhofer.aisec.cpg.helpers.Util

/**
 * In Python, all declarations are statements. This class handles the parsing of the statements
 * represented as [Declaration] nodes in our CPG.
 *
 * For declarations encountered directly on a namespace and classes, we directly invoke the
 * [DeclarationHandler], for others, the [StatementHandler] will forward these statements to us.
 */
class DeclarationHandler(frontend: PythonLanguageFrontend) :
    PythonHandler<Declaration, Python.AST.BaseStmt>(::ProblemDeclaration, frontend) {
    override fun handleNode(node: Python.AST.BaseStmt): Declaration {
        return when (node) {
            is Python.AST.FunctionDef -> handleFunctionDef(node)
            is Python.AST.AsyncFunctionDef -> handleFunctionDef(node)
            else -> {
                return handleNotSupported(node, node.javaClass.simpleName)
            }
        }
    }

    private fun handleNotSupported(node: Python.AST.BaseStmt, name: String): Declaration {
        Util.errorWithFileLocation(
            frontend,
            node,
            log,
            "Parsing of type $name is not supported (yet)",
        )

        val cpgNode = this.configConstructor.get()
        if (cpgNode is ProblemNode) {
            cpgNode.problem = "Parsing of type $name is not supported (yet)"
        }

        return cpgNode
    }

    /**
     * We have to consider multiple things when matching Python's FunctionDef to the CPG:
     * - A [Python.AST.FunctionDef] could be one of
     *     - a [ConstructorDeclaration] if it appears in a record and its [name] is `__init__`
     *     - a [MethodeDeclaration] if it appears in a record, and it isn't a
     *       [ConstructorDeclaration]
     *     - a [FunctionDeclaration] if neither of the above apply
     *
     * In case of a [ConstructorDeclaration] or[MethodDeclaration]: the first argument is the
     * `receiver` (most often called `self`).
     */
    private fun handleFunctionDef(s: Python.AST.NormalOrAsyncFunctionDef): FunctionDeclaration {
        var recordDeclaration =
            (frontend.scopeManager.currentScope as? RecordScope)?.astNode as? RecordDeclaration
        val language = language
        val result =
            if (recordDeclaration != null) {
                if (s.name == "__init__") {
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
        frontend.scopeManager.enterScope(result)

        frontend.statementHandler.addAsyncWarning(s, result)

        // Handle decorators (which are translated into CPG "annotations")
        result.annotations += frontend.statementHandler.handleAnnotations(s)

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
            result.body = frontend.statementHandler.makeBlock(s.body, parentNode = s)
        }

        frontend.scopeManager.leaveScope(result)

        // We want functions inside functions to be wrapped in a declaration statement, so we are
        // not adding them to scope's AST
        if (frontend.scopeManager.currentScope is FunctionScope) {
            frontend.scopeManager.addDeclaration(result, addToAST = false)
        } else {
            // In any other cases, we add them to the "declarations", otherwise we will not
            // correctly resolve them.
            frontend.scopeManager.addDeclaration(result)
        }

        return result
    }

    /** Adds the arguments to [result] which might be located in a [recordDeclaration]. */
    private fun handleArguments(
        args: Python.AST.arguments,
        result: FunctionDeclaration,
        recordDeclaration: RecordDeclaration?,
    ) {
        // We can merge posonlyargs and args because both are positional arguments. We do not
        // enforce that posonlyargs can ONLY be used in a positional style, whereas args can be used
        // both in positional and keyword style.
        var positionalArguments = args.posonlyargs + args.args

        // Handle receiver if it exists and if it is not a static method
        if (
            recordDeclaration != null &&
                result.annotations.none { it.name.localName == "staticmethod" }
        ) {
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
     * This function creates a [newParameterDeclaration] for the argument, setting any modifiers
     * (like positional-only or keyword-only) and [defaultValue] if applicable.
     */
    internal fun handleArgument(
        node: Python.AST.arg,
        isPosOnly: Boolean = false,
        isVariadic: Boolean = false,
        isKwoOnly: Boolean = false,
        defaultValue: Expression? = null,
    ): ParameterDeclaration {
        val type = frontend.typeOf(node.annotation)
        val arg =
            newParameterDeclaration(
                name = node.arg,
                type = type,
                variadic = isVariadic,
                rawNode = node,
            )
        defaultValue?.let { arg.default = it }
        if (isPosOnly) {
            arg.modifiers += MODIFIER_POSITIONAL_ONLY_ARGUMENT
        }

        if (isKwoOnly) {
            arg.modifiers += MODIFIER_KEYWORD_ONLY_ARGUMENT
        }

        frontend.scopeManager.addDeclaration(arg)

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
                defaultValue = default?.let { frontend.expressionHandler.handle(it) },
            )
        }
    }
}
