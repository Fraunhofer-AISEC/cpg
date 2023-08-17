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
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ProblemExpression
import de.fraunhofer.aisec.cpg.graph.types.Type

class StatementHandler(frontend: PythonLanguageFrontend) :
    PythonHandler<Statement, PythonAST.Statement>(::ProblemExpression, frontend) {
    override fun handleNode(node: PythonAST.Statement): Statement {
        return when (node) {
            is PythonAST.ClassDef -> handleClassDef(node)
            is PythonAST.FunctionDef -> handleFunctionDef(node)
            is PythonAST.Pass ->
                return newEmptyStatement(code = frontend.codeOf(node), rawNode = node)
            is PythonAST.ImportFrom -> handleImportFrom(node)
            is PythonAST.Assign -> handleAssign(node)
            is PythonAST.Return -> handleReturn(node)
            is PythonAST.If -> handleIf(node)
            is PythonAST.AnnAssign -> handleAnnAssign(node)
            is PythonAST.Expr -> handleExpressionStatement(node)
            is PythonAST.For -> handleFor(node)
            is PythonAST.While -> handleWhile(node)
            else -> TODO()
        }
    }

    private fun handleWhile(node: PythonAST.While): Statement {
        return newEmptyStatement() // TODO
    }

    private fun handleFor(node: PythonAST.For): Statement {
        return newEmptyStatement() // TODO
    }

    private fun handleExpressionStatement(node: PythonAST.Expr): Statement {
        return newEmptyStatement() // TODO
    }

    private fun handleAnnAssign(node: PythonAST.AnnAssign): Statement {
        return newEmptyStatement() // TODO
    }

    private fun handleIf(node: PythonAST.If): Statement {
        return newEmptyStatement() // TODO
    }

    private fun handleReturn(node: PythonAST.Return): Statement {
        return newEmptyStatement() // TODO
    }

    private fun handleAssign(node: PythonAST.Assign): Statement {
        return newEmptyStatement() // TODO
    }

    private fun handleImportFrom(node: PythonAST.ImportFrom): Statement {
        return newEmptyStatement() // TODO
    }

    private fun handleClassDef(stmt: PythonAST.ClassDef): Statement {
        val cls =
            newRecordDeclaration(stmt.name, "class", code = frontend.codeOf(stmt), rawNode = stmt)
        stmt.bases.map { cls.superClasses.add(getTypeInCurrentNamespace(it)) }

        frontend.scopeManager.enterScope(cls)

        stmt.keywords.map { TODO() }

        for (s in stmt.body) {
            when (s) {
                is PythonAST.FunctionDef -> handleFunctionDef(s, cls)
                else -> cls.addStatement(handleNode(s))
            }
        }

        frontend.scopeManager.leaveScope(cls)
        frontend.scopeManager.addDeclaration(cls)

        return wrapDeclarationToStatement(cls)
    }

    /**
     * We have to consider multiple things when matching Python's FunctionDef to the CPG:
     * - A [PythonAST.FunctionDef] is a [Statement] from Python's point of view. The CPG sees it as
     *   a declaration -> we have to wrap the result in a [DeclarationStatement].
     * - A [PythonAST.FunctionDef] could be one of
     *     - a [ConstructorDeclaration] if it appears in a record and its [name] is `__init__`
     *     - a [MethodeDeclaration] if it appears in a record, and it isn't a
     *       [ConstructorDeclaration]
     *     - a [FunctionDeclaration] if neither of the above apply
     *
     * In case of a [ConstructorDeclaration] or[MethodDeclaration]: the first argument is the
     * `receiver` (most often called `self`).
     */
    private fun handleFunctionDef(
        s: PythonAST.FunctionDef,
        recordDeclaration: RecordDeclaration? = null
    ): DeclarationStatement {
        val result =
            if (recordDeclaration != null) {
                if (s.name == "__init__") {
                    newConstructorDeclaration(
                        name = s.name,
                        code = frontend.codeOf(s),
                        recordDeclaration = recordDeclaration,
                        rawNode = s
                    )
                } else {
                    newMethodDeclaration(
                        name = s.name,
                        code = frontend.codeOf(s),
                        recordDeclaration = recordDeclaration,
                        isStatic = false,
                        rawNode = s
                    )
                }
            } else {
                newFunctionDeclaration(name = s.name, code = frontend.codeOf(s), rawNode = s)
            }
        frontend.scopeManager.enterScope(result)

        // HANDLE ARGUMENTS

        if (s.args.posonlyargs.isNotEmpty()) {
            val problem =
                newProblemDeclaration(
                    "`posonlyargs` are not yet supported",
                    problemType = ProblemNode.ProblemType.TRANSLATION,
                    code = frontend.codeOf(s.args),
                    rawNode = s.args
                )
            frontend.scopeManager.addDeclaration(problem)
        }

        if (recordDeclaration != null) {
            // first argument is the `receiver`
            if (s.args.args.isEmpty()) {
                val problem =
                    newProblemDeclaration(
                        "Expected a receiver",
                        problemType = ProblemNode.ProblemType.TRANSLATION,
                        code = frontend.codeOf(s.args),
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
                        code = frontend.codeOf(recvPythonNode),
                        implicitInitializerAllowed = false,
                        rawNode = recvPythonNode
                    )
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
                    code = frontend.codeOf(it),
                    rawNode = it
                )
            frontend.scopeManager.addDeclaration(problem)
        }

        if (s.args.kwonlyargs.isNotEmpty()) {
            val problem =
                newProblemDeclaration(
                    "`kwonlyargs` are not yet supported",
                    problemType = ProblemNode.ProblemType.TRANSLATION,
                    code = frontend.codeOf(s.args),
                    rawNode = s.args
                )
            frontend.scopeManager.addDeclaration(problem)
        }

        if (s.args.kw_defaults.isNotEmpty()) {
            val problem =
                newProblemDeclaration(
                    "`kw_defaults` are not yet supported",
                    problemType = ProblemNode.ProblemType.TRANSLATION,
                    code = frontend.codeOf(s.args),
                    rawNode = s.args
                )
            frontend.scopeManager.addDeclaration(problem)
        }

        s.args.kwarg?.let {
            val problem =
                newProblemDeclaration(
                    "`kwarg` is not yet supported",
                    problemType = ProblemNode.ProblemType.TRANSLATION,
                    code = frontend.codeOf(it),
                    rawNode = it
                )
            frontend.scopeManager.addDeclaration(problem)
        }

        if (s.args.defaults.isNotEmpty()) {
            val problem =
                newProblemDeclaration(
                    "`defaults` are not yet supported",
                    problemType = ProblemNode.ProblemType.TRANSLATION,
                    code = frontend.codeOf(s.args),
                    rawNode = s.args
                )
            frontend.scopeManager.addDeclaration(problem)
        }
        // END HANDLE ARGUMENTS

        if (s.body.isNotEmpty()) {
            result.body = makeCompoundStmt(s.body)
        }

        frontend.scopeManager.leaveScope(result)
        frontend.scopeManager.addDeclaration(result)

        return wrapDeclarationToStatement(result)
    }

    private fun makeCompoundStmt(
        stmts: List<PythonAST.Statement>,
        code: String? = null,
        rawNode: PythonAST.Node? = null
    ): CompoundStatement {
        val result = newCompoundStatement(code, rawNode)
        for (stmt in stmts) {
            when (val r = handle(stmt)) {
                is Declaration -> result.addDeclaration(r)
                is Statement -> result.addStatement(r)
                else -> TODO()
            }
        }
        return result
    }

    private fun handleArgument(arg: PythonAST.arg) {}

    /**
     * Wrap a declaration in a [DeclarationStatement]
     *
     * @param decl The [Declaration] to be wrapped
     * @return The wrapped [decl]
     */
    private fun wrapDeclarationToStatement(decl: Declaration): DeclarationStatement {
        val declStmt = newDeclarationStatement(code = decl.code, rawNode = null) // TODO: rawNode
        declStmt.addDeclaration(decl)
        return declStmt
    }

    private fun getTypeInCurrentNamespace(it: PythonAST.Node): Type {
        TODO()
    }
}
