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
import de.fraunhofer.aisec.cpg.graph.declarations.ConstructorDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ProblemExpression
import de.fraunhofer.aisec.cpg.graph.types.FunctionType

class StatementHandler(frontend: PythonLanguageFrontend) :
    PythonHandler<Statement, Python.ASTBASEstmt>(::ProblemExpression, frontend) {
    override fun handleNode(node: Python.ASTBASEstmt): Statement {
        return when (node) {
            is Python.ASTClassDef -> handleClassDef(node)
            is Python.ASTFunctionDef -> handleFunctionDef(node)
            is Python.ASTPass -> return newEmptyStatement(rawNode = node)
            is Python.ASTImportFrom -> handleImportFrom(node)
            is Python.ASTAssign -> handleAssign(node)
            is Python.ASTReturn -> handleReturn(node)
            is Python.ASTIf -> handleIf(node)
            is Python.ASTAnnAssign -> handleAnnAssign(node)
            is Python.ASTExpr -> handleExpressionStatement(node)
            is Python.ASTFor -> handleFor(node)
            is Python.ASTWhile -> handleWhile(node)
            is Python.ASTImport -> handleImport(node)
            is Python.ASTBreak -> newBreakStatement(rawNode = node)
            is Python.ASTContinue -> newContinueStatement(rawNode = node)
            else -> TODO()
        }
    }

    private fun handleImport(node: Python.ASTImport): Statement {
        val declStmt = newDeclarationStatement(rawNode = node)
        for (imp in node.names) {
            val v =
                if (imp.asname != null) {
                    newVariableDeclaration(imp.asname, rawNode = imp) // TODO refers to original????
                } else {
                    newVariableDeclaration(imp.name, rawNode = imp)
                }
            frontend.scopeManager.addDeclaration(v)
            declStmt.addDeclaration(v)
        }
        return declStmt
    }

    private fun handleWhile(node: Python.ASTWhile): Statement {
        val ret = newWhileStatement(rawNode = node)
        ret.condition = frontend.expressionHandler.handle(node.test)
        ret.statement = makeBlock(node.body)
        node.orelse.firstOrNull()?.let { TODO("Not supported") }
        return ret
    }

    private fun handleFor(node: Python.ASTFor): Statement {
        val ret = newForEachStatement(rawNode = node)
        ret.iterable = frontend.expressionHandler.handle(node.iter)
        ret.variable = frontend.expressionHandler.handle(node.target)
        ret.statement = makeBlock(node.body)
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
                makeBlock(node.body)
            } else {
                null
            }
        ret.elseStatement =
            if (node.orelse.isNotEmpty()) {
                makeBlock(node.orelse)
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
        if (rhs is List<*>) TODO()
        return newAssignExpression(lhs = lhs, rhs = listOf(rhs), rawNode = node)
    }

    private fun handleImportFrom(node: Python.ASTImportFrom): Statement {
        val declStmt = newDeclarationStatement(rawNode = node)
        for (stmt in node.names) {
            val name =
                if (stmt.asname != null) {
                    stmt.asname
                } else {
                    stmt.name
                }
            val decl = newVariableDeclaration(name = name, rawNode = node)
            frontend.scopeManager.addDeclaration(decl)
            declStmt.addDeclaration(decl)
        }
        return declStmt
    }

    private fun handleClassDef(stmt: Python.ASTClassDef): Statement {
        val cls = newRecordDeclaration(stmt.name, "class", rawNode = stmt)
        stmt.bases.map { cls.superClasses.add(frontend.typeOf(it)) }

        frontend.scopeManager.enterScope(cls)

        stmt.keywords.map { TODO() }

        for (s in stmt.body) {
            when (s) {
                is Python.ASTFunctionDef -> handleFunctionDef(s, cls)
                else -> cls.addStatement(handleNode(s))
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
        result.addAnnotations(handleAnnotations(s))

        // Handle arguments
        if (s.args.posonlyargs.isNotEmpty()) {
            val problem =
                newProblemDeclaration(
                    "`posonlyargs` are not yet supported",
                    problemType = ProblemNode.ProblemType.TRANSLATION,
                    rawNode = s.args
                )
            frontend.scopeManager.addDeclaration(problem)
        }

        // Handle return type and calculate function type
        if (result is ConstructorDeclaration) {
            // Return type of the constructor is always its record declaration type
            result.returnTypes = listOf(recordDeclaration?.toType() ?: unknownType())
        } else {
            result.returnTypes = listOf(frontend.typeOf(s.returns))
        }
        result.type = FunctionType.computeType(result)

        if (recordDeclaration != null) {
            // first argument is the `receiver`
            if (s.args.args.isEmpty()) {
                val problem =
                    newProblemDeclaration(
                        "Expected a receiver",
                        problemType = ProblemNode.ProblemType.TRANSLATION,
                        rawNode = s.args
                    )
                frontend.scopeManager.addDeclaration(problem)
            } else {
                val recvPythonNode = s.args.args.first()
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
            for (arg in s.args.args.subList(1, s.args.args.size)) {
                handleArgument(arg)
            }
        } else {
            for (arg in s.args.args) {
                handleArgument(arg)
            }
        }

        s.args.vararg?.let {
            val problem =
                newProblemDeclaration(
                    "`vararg` is not yet supported",
                    problemType = ProblemNode.ProblemType.TRANSLATION,
                    rawNode = it
                )
            frontend.scopeManager.addDeclaration(problem)
        }

        if (s.args.kwonlyargs.isNotEmpty()) {
            val problem =
                newProblemDeclaration(
                    "`kwonlyargs` are not yet supported",
                    problemType = ProblemNode.ProblemType.TRANSLATION,
                    rawNode = s.args
                )
            frontend.scopeManager.addDeclaration(problem)
        }

        if (s.args.kw_defaults.isNotEmpty()) {
            val problem =
                newProblemDeclaration(
                    "`kw_defaults` are not yet supported",
                    problemType = ProblemNode.ProblemType.TRANSLATION,
                    rawNode = s.args
                )
            frontend.scopeManager.addDeclaration(problem)
        }

        s.args.kwarg?.let {
            val problem =
                newProblemDeclaration(
                    "`kwarg` is not yet supported",
                    problemType = ProblemNode.ProblemType.TRANSLATION,
                    rawNode = it
                )
            frontend.scopeManager.addDeclaration(problem)
        }

        if (s.args.defaults.isNotEmpty()) {
            val problem =
                newProblemDeclaration(
                    "`defaults` are not yet supported",
                    problemType = ProblemNode.ProblemType.TRANSLATION,
                    rawNode = s.args
                )
            frontend.scopeManager.addDeclaration(problem)
        }
        // END HANDLE ARGUMENTS

        if (s.body.isNotEmpty()) {
            result.body = makeBlock(s.body, s)
        }

        frontend.scopeManager.leaveScope(result)
        frontend.scopeManager.addDeclaration(result)

        return wrapDeclarationToStatement(result)
    }

    private fun handleAnnotations(
        node: Python.ASTFunctionDef
    ): Collection<de.fraunhofer.aisec.cpg.graph.Annotation> {
        val annotations = mutableListOf<de.fraunhofer.aisec.cpg.graph.Annotation>()
        for (decorator in node.decorator_list) {
            if (decorator !is Python.ASTCall) {
                TODO()
            }

            val decFuncParsed = frontend.expressionHandler.handle(decorator.func)
            if (decFuncParsed !is MemberExpression) {
                TODO()
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
            result.addStatement(handle(stmt))
        }
        return result
    }

    private fun handleArgument(node: Python.ASTarg) {
        val type = frontend.typeOf(node.annotation)
        val arg = newParameterDeclaration(name = node.arg, type = type, rawNode = node)

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
