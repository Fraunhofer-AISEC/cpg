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

import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage.Companion.MODIFIER_POSITIONAL_ONLY_ARGUMENT
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.Annotation
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ProblemExpression
import de.fraunhofer.aisec.cpg.graph.types.FunctionType
import kotlin.collections.plusAssign

class StatementHandler(frontend: PythonLanguageFrontend) :
    PythonHandler<Statement, Python.ASTBASEstmt>(::ProblemExpression, frontend) {

    override fun handleNode(node: Python.ASTBASEstmt): Statement {
        return when (node) {
            is Python.ASTClassDef -> handleClassDef(node)
            is Python.ASTFunctionDef -> handleFunctionDef(node)
            is Python.ASTAsyncFunctionDef -> handleAsyncFunctionDef(node)
            is Python.ASTPass -> return newEmptyStatement(rawNode = node)
            is Python.ASTImportFrom -> handleImportFrom(node)
            is Python.ASTAssign -> handleAssign(node)
            is Python.ASTAugAssign -> handleAugAssign(node)
            is Python.ASTReturn -> handleReturn(node)
            is Python.ASTIf -> handleIf(node)
            is Python.ASTAnnAssign -> handleAnnAssign(node)
            is Python.ASTExpr -> handleExpressionStatement(node)
            is Python.ASTFor -> handleFor(node)
            is Python.ASTAsyncFor -> handleAsyncFor(node)
            is Python.ASTWhile -> handleWhile(node)
            is Python.ASTImport -> handleImport(node)
            is Python.ASTBreak -> newBreakStatement(rawNode = node)
            is Python.ASTContinue -> newContinueStatement(rawNode = node)
            is Python.ASTAssert,
            is Python.ASTDelete,
            is Python.ASTGlobal,
            is Python.ASTMatch,
            is Python.ASTNonlocal,
            is Python.ASTRaise,
            is Python.ASTTry,
            is Python.ASTTryStar,
            is Python.ASTWith,
            is Python.ASTAsyncWith ->
                newProblemExpression(
                    "The statement of class ${node.javaClass} is not supported yet",
                    rawNode = node
                )
        }
    }

    private fun handleImport(node: Python.ASTImport): Statement {
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

    private fun handleImportFrom(node: Python.ASTImportFrom): Statement {
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
                    // In the wildcard case, our "import" is the module name and we set "wildcard"
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

    private fun handleWhile(node: Python.ASTWhile): Statement {
        val ret = newWhileStatement(rawNode = node)
        ret.condition = frontend.expressionHandler.handle(node.test)
        ret.statement = makeBlock(node.body).codeAndLocationFromChildren(node)
        node.orelse.firstOrNull()?.let { TODO("Not supported") }
        return ret
    }

    private fun handleFor(node: Python.ASTFor): Statement {
        val ret = newForEachStatement(rawNode = node)
        ret.iterable = frontend.expressionHandler.handle(node.iter)
        ret.variable = frontend.expressionHandler.handle(node.target)
        ret.statement = makeBlock(node.body).codeAndLocationFromChildren(node)
        node.orelse.firstOrNull()?.let { TODO("Not supported") }
        return ret
    }

    private fun handleAsyncFor(node: Python.ASTAsyncFor): Statement {
        val ret = newForEachStatement(rawNode = node)
        ret.iterable = frontend.expressionHandler.handle(node.iter)
        ret.variable = frontend.expressionHandler.handle(node.target)
        ret.statement = makeBlock(node.body).codeAndLocationFromChildren(node)
        node.orelse.firstOrNull()?.let { TODO("Not supported") }
        return ret
    }

    private fun handleExpressionStatement(node: Python.ASTExpr): Statement {
        return frontend.expressionHandler.handle(node.value)
    }

    private fun handleAnnAssign(node: Python.ASTAnnAssign): Statement {
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

    private fun handleIf(node: Python.ASTIf): Statement {
        val ret = newIfStatement(rawNode = node)
        ret.condition = frontend.expressionHandler.handle(node.test)
        ret.thenStatement =
            if (node.body.isNotEmpty()) {
                makeBlock(node.body).codeAndLocationFromChildren(node)
            } else {
                null
            }
        ret.elseStatement =
            if (node.orelse.isNotEmpty()) {
                makeBlock(node.orelse).codeAndLocationFromChildren(node)
            } else {
                null
            }
        return ret
    }

    private fun handleReturn(node: Python.ASTReturn): Statement {
        val ret = newReturnStatement(rawNode = node)
        node.value?.let { ret.returnValue = frontend.expressionHandler.handle(it) }
        return ret
    }

    private fun handleAssign(node: Python.ASTAssign): Statement {
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

    private fun handleAugAssign(node: Python.ASTAugAssign): Statement {
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

    private fun handleClassDef(stmt: Python.ASTClassDef): Statement {
        val cls = newRecordDeclaration(stmt.name, "class", rawNode = stmt)
        stmt.bases.map { cls.superClasses.add(frontend.typeOf(it)) }

        frontend.scopeManager.enterScope(cls)

        stmt.keywords.forEach {
            frontend.currentTU?.addDeclaration(
                newProblemDeclaration("could not parse keyword $it in class")
            )
        }

        for (s in stmt.body) {
            when (s) {
                is Python.ASTFunctionDef -> handleFunctionDef(s, cls)
                else -> cls.statements += handleNode(s)
            }
        }

        frontend.scopeManager.leaveScope(cls)
        frontend.scopeManager.addDeclaration(cls)

        return wrapDeclarationToStatement(cls)
    }

    /**
     * We have to consider multiple things when matching Python's FunctionDef to the CPG:
     * - A [Python.ASTFunctionDef] is a [Statement] from Python's point of view. The CPG sees it as
     *   a declaration -> we have to wrap the result in a [DeclarationStatement].
     * - A [Python.ASTFunctionDef] could be one of
     *     - a [ConstructorDeclaration] if it appears in a record and its [name] is `__init__`
     *     - a [MethodeDeclaration] if it appears in a record, and it isn't a
     *       [ConstructorDeclaration]
     *     - a [FunctionDeclaration] if neither of the above apply
     *
     * In case of a [ConstructorDeclaration] or[MethodDeclaration]: the first argument is the
     * `receiver` (most often called `self`).
     */
    private fun handleFunctionDef(
        s: Python.ASTFunctionDef,
        recordDeclaration: RecordDeclaration? = null
    ): DeclarationStatement {
        val result =
            if (recordDeclaration != null) {
                if (s.name == "__init__") {
                    newConstructorDeclaration(
                        name = s.name,
                        recordDeclaration = recordDeclaration,
                        rawNode = s
                    )
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
            result.body = makeBlock(s.body).codeAndLocationFromChildren(s)
        }

        frontend.scopeManager.leaveScope(result)
        frontend.scopeManager.addDeclaration(result)

        return wrapDeclarationToStatement(result)
    }

    /**
     * We have to consider multiple things when matching Python's FunctionDef to the CPG:
     * - A [Python.ASTFunctionDef] is a [Statement] from Python's point of view. The CPG sees it as
     *   a declaration -> we have to wrap the result in a [DeclarationStatement].
     * - A [Python.ASTFunctionDef] could be one of
     *     - a [ConstructorDeclaration] if it appears in a record and its [name] is `__init__`
     *     - a [MethodeDeclaration] if it appears in a record, and it isn't a
     *       [ConstructorDeclaration]
     *     - a [FunctionDeclaration] if neither of the above apply
     *
     * In case of a [ConstructorDeclaration] or[MethodDeclaration]: the first argument is the
     * `receiver` (most often called `self`).
     */
    private fun handleAsyncFunctionDef(
        s: Python.ASTAsyncFunctionDef,
        recordDeclaration: RecordDeclaration? = null
    ): DeclarationStatement {
        val result =
            if (recordDeclaration != null) {
                if (s.name == "__init__") {
                    newConstructorDeclaration(
                        name = s.name,
                        recordDeclaration = recordDeclaration,
                        rawNode = s
                    )
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
            result.body = makeBlock(s.body).codeAndLocationFromChildren(s)
        }

        frontend.scopeManager.leaveScope(result)
        frontend.scopeManager.addDeclaration(result)

        return wrapDeclarationToStatement(result)
    }

    /** Adds the arguments to [result] which might be located in a [recordDeclaration]. */
    private fun handleArguments(
        args: Python.ASTarguments,
        result: FunctionDeclaration,
        recordDeclaration: RecordDeclaration?
    ) {
        // We can merge posonlyargs and args because both are positional arguments. We do not
        // enforce that posonlyargs can ONLY be used in a positional style, whereas args can be used
        // both in positional as well as keyword style.
        var positionalArguments = args.posonlyargs + args.args
        // Handle arguments
        if (recordDeclaration != null) {
            // first argument is the `receiver`
            val recvPythonNode = positionalArguments.firstOrNull()
            if (recvPythonNode == null) {
                val problem =
                    newProblemDeclaration(
                        "Expected a receiver",
                        problemType = ProblemNode.ProblemType.TRANSLATION,
                        rawNode = args
                    )
                frontend.scopeManager.addDeclaration(problem)
            } else {
                val tpe = recordDeclaration.toType()
                val recvNode =
                    newVariableDeclaration(
                        name = recvPythonNode.arg,
                        type = tpe,
                        implicitInitializerAllowed = false,
                        rawNode = recvPythonNode
                    )
                frontend.scopeManager.addDeclaration(recvNode)
                when (result) {
                    is ConstructorDeclaration -> result.receiver = recvNode
                    is MethodDeclaration -> result.receiver = recvNode
                    else -> TODO()
                }
            }
        }

        if (recordDeclaration != null) {
            // first argument is the receiver
            for (arg in positionalArguments.subList(1, positionalArguments.size)) {
                handleArgument(arg, arg in args.posonlyargs)
            }
        } else {
            for (arg in positionalArguments) {
                handleArgument(arg, arg in args.posonlyargs)
            }
        }

        args.vararg?.let { handleArgument(it, isPosOnly = false, isVariadic = true) }

        if (args.kwonlyargs.isNotEmpty()) {
            val problem =
                newProblemDeclaration(
                    "`kwonlyargs` are not yet supported",
                    problemType = ProblemNode.ProblemType.TRANSLATION,
                    rawNode = args
                )
            frontend.scopeManager.addDeclaration(problem)
        }

        if (args.kw_defaults.isNotEmpty()) {
            val problem =
                newProblemDeclaration(
                    "`kw_defaults` are not yet supported",
                    problemType = ProblemNode.ProblemType.TRANSLATION,
                    rawNode = args
                )
            frontend.scopeManager.addDeclaration(problem)
        }

        args.kwarg?.let {
            val problem =
                newProblemDeclaration(
                    "`kwarg` is not yet supported",
                    problemType = ProblemNode.ProblemType.TRANSLATION,
                    rawNode = it
                )
            frontend.scopeManager.addDeclaration(problem)
        }

        if (args.defaults.isNotEmpty()) {
            val problem =
                newProblemDeclaration(
                    "`defaults` are not yet supported",
                    problemType = ProblemNode.ProblemType.TRANSLATION,
                    rawNode = args
                )
            frontend.scopeManager.addDeclaration(problem)
        }
    }

    private fun handleAnnotations(node: Python.ASTAsyncFunctionDef): Collection<Annotation> {
        return handleDeclaratorList(node, node.decorator_list)
    }

    private fun handleAnnotations(node: Python.ASTFunctionDef): Collection<Annotation> {
        return handleDeclaratorList(node, node.decorator_list)
    }

    fun handleDeclaratorList(
        node: Python.AST,
        decoratorList: List<Python.ASTBASEexpr>
    ): List<Annotation> {
        val annotations = mutableListOf<Annotation>()
        for (decorator in decoratorList) {
            if (decorator !is Python.ASTCall) {
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

    private fun makeBlock(stmts: List<Python.ASTBASEstmt>, rawNode: Python.AST? = null): Block {
        val result = newBlock(rawNode = rawNode)
        for (stmt in stmts) {
            result.statements += handle(stmt)
        }
        return result
    }

    internal fun handleArgument(
        node: Python.ASTarg,
        isPosOnly: Boolean = false,
        isVariadic: Boolean = false
    ) {
        val type = frontend.typeOf(node.annotation)
        val arg =
            newParameterDeclaration(
                name = node.arg,
                type = type,
                variadic = isVariadic,
                rawNode = node
            )
        if (isPosOnly) {
            arg.modifiers += MODIFIER_POSITIONAL_ONLY_ARGUMENT
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
