/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.cxx

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.DeclarationSequence
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ProblemExpression
import de.fraunhofer.aisec.cpg.helpers.Util
import java.util.*
import java.util.function.BiConsumer
import java.util.function.Supplier
import java.util.stream.Collectors
import org.eclipse.cdt.core.dom.ast.*
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCatchHandler
import org.eclipse.cdt.internal.core.dom.parser.cpp.*

class StatementHandler(lang: CXXLanguageFrontend) :
    CXXHandler<Statement, IASTStatement>(Supplier(::ProblemExpression), lang) {

    override fun handleNode(node: IASTStatement): Statement {
        return when (node) {
            is IASTCompoundStatement -> handleCompoundStatement(node)
            is IASTReturnStatement -> handleReturnStatement(node)
            is IASTDeclarationStatement -> handleDeclarationStatement(node)
            is IASTExpressionStatement -> handleExpressionStatement(node)
            is IASTIfStatement -> handleIfStatement(node)
            is IASTWhileStatement -> handleWhileStatement(node)
            is IASTDoStatement -> handleDoStatement(node)
            is IASTForStatement -> handleForStatement(node)
            is IASTContinueStatement -> handleContinueStatement(node)
            is IASTBreakStatement -> handleBreakStatement(node)
            is IASTLabelStatement -> handleLabelStatement(node)
            is IASTSwitchStatement -> handleSwitchStatement(node)
            is IASTCaseStatement -> handleCaseStatement(node)
            is IASTDefaultStatement -> handleDefaultStatement(node)
            is IASTNullStatement -> handleEmptyStatement(node)
            is IASTGotoStatement -> handleGotoStatement(node)
            is IASTProblemStatement -> handleProblemStatement(node)
            is CPPASTRangeBasedForStatement -> handleForEachStatement(node)
            is CPPASTTryBlockStatement -> handleTryBlockStatement(node)
            is CPPASTCatchHandler -> handleCatchHandler(node)
            else -> {
                return handleNotSupported(node, node.javaClass.name)
            }
        }
    }

    private fun handleProblemStatement(problemStatement: IASTProblemStatement): ProblemExpression {
        Util.errorWithFileLocation(
            frontend,
            problemStatement,
            log,
            problemStatement.problem.message
        )

        return newProblemExpression(
            problemStatement.problem.message,
        )
    }

    private fun handleEmptyStatement(nullStatement: IASTNullStatement): EmptyStatement {
        return newEmptyStatement(rawNode = nullStatement)
    }

    private fun handleTryBlockStatement(tryBlockStatement: CPPASTTryBlockStatement): TryStatement {
        val tryStatement = newTryStatement().withChildren(true) {
            val statement = handle(tryBlockStatement.tryBody) as Block?
            val catchClauses =
                Arrays.stream(tryBlockStatement.catchHandlers)
                    .map { handleCatchHandler(it) }
                    .collect(Collectors.toList())
            it.tryBlock = statement
            it.catchClauses = catchClauses
        }
        return tryStatement
    }

    private fun handleCatchHandler(catchHandler: ICPPASTCatchHandler): CatchClause {
        val catchClause = newCatchClause(rawNode = catchHandler).withChildren(true) {
            val body = frontend.statementHandler.handle(catchHandler.catchBody)

            // TODO: can also be an 'unnamed' parameter. In this case we should not declare a variable
            var decl: Declaration? = null
            if (catchHandler.declaration != null) { // can be null for "catch(...)"
                decl = frontend.declarationHandler.handle(catchHandler.declaration)
            }

            it.body = body as? Block

            if (decl != null) {
                it.parameter = decl as? VariableDeclaration
            }
        }
        return catchClause
    }

    private fun handleIfStatement(ctx: IASTIfStatement): IfStatement {
        val statement = newIfStatement(rawNode = ctx).withChildren(true) {
            // We need some special treatment for C++ IfStatements
            if (ctx is CPPASTIfStatement) {
                if (ctx.initializerStatement != null) {
                    it.initializerStatement = handle(ctx.initializerStatement)
                }
                if (ctx.conditionDeclaration != null) {
                    it.conditionDeclaration =
                        frontend.declarationHandler.handle(ctx.conditionDeclaration)
                }

                it.isConstExpression = ctx.isConstexpr
            }

            if (ctx.conditionExpression != null)
                it.condition = frontend.expressionHandler.handle(ctx.conditionExpression)
            it.thenStatement = handle(ctx.thenClause)
            if (ctx.elseClause != null) {
                it.elseStatement = handle(ctx.elseClause)
            }
        }

        return statement
    }

    private fun handleLabelStatement(ctx: IASTLabelStatement): LabelStatement {
        val statement = newLabelStatement(rawNode = ctx).withChildren {
            it.subStatement = handle(ctx.nestedStatement)
        }
        statement.label = ctx.name.toString()
        return statement
    }

    private fun handleGotoStatement(ctx: IASTGotoStatement): GotoStatement {
        val statement = newGotoStatement(rawNode = ctx)
        val assigneeTargetLabel = BiConsumer { _: Any, to: Node ->
            statement.targetLabel = to as LabelStatement
        }
        val b: IBinding?
        try {
            b = ctx.name.resolveBinding()
            if (b is ILabel) {
                b.labelStatement
                // If the bound AST node is/or was transformed into a CPG node the cpg node is bound
                // to the CPG goto statement
                frontend.registerObjectListener(b.labelStatement, assigneeTargetLabel)
            }
        } catch (e: Exception) {
            // If the Label AST node could not be resolved, the matching is done based on label
            // names of CPG nodes using the predicate listeners
            frontend.registerPredicateListener(
                { _, to -> (to is LabelStatement && to.label == statement.labelName) },
                assigneeTargetLabel
            )
        }
        return statement
    }

    private fun handleWhileStatement(ctx: IASTWhileStatement): WhileStatement {
        val statement = newWhileStatement(rawNode = ctx).withChildren(true) {
            // Special treatment for C++ while
            if (ctx is CPPASTWhileStatement && ctx.conditionDeclaration != null) {
                it.conditionDeclaration =
                    frontend.declarationHandler.handle(ctx.conditionDeclaration)
            }

            if (ctx.condition != null) {
                it.condition = frontend.expressionHandler.handle(ctx.condition)
            }

            it.statement = handle(ctx.body)
        }

        return statement
    }

    private fun handleDoStatement(ctx: IASTDoStatement): DoStatement {
        val statement = newDoStatement(rawNode = ctx).withChildren {
            it.condition = frontend.expressionHandler.handle(ctx.condition)
            it.statement = handle(ctx.body)
        }
        return statement
    }

    private fun handleForStatement(ctx: IASTForStatement): ForStatement {
        val statement = newForStatement(rawNode = ctx).withChildren(true) {
            it.initializerStatement = handle(ctx.initializerStatement)

            // Special treatment for C++ while
            if (ctx is CPPASTForStatement && ctx.conditionDeclaration != null) {
                it.conditionDeclaration =
                    frontend.declarationHandler.handle(ctx.conditionDeclaration)
            }

            if (ctx.conditionExpression != null) {
                it.condition = frontend.expressionHandler.handle(ctx.conditionExpression)
            }

            // Adds true expression node where default empty condition evaluates to true, remove here
            // and in java StatementAnalyzer
            if (it.conditionDeclaration == null && it.condition == null) {
                val literal: Literal<*> =
                    newLiteral(true, primitiveType("bool")).implicit(code = "true")
                it.condition = literal
            }

            if (ctx.iterationExpression != null) {
                it.iterationStatement =
                    frontend.expressionHandler.handle(ctx.iterationExpression)
            }

            it.statement = handle(ctx.body)
        }

        return statement
    }

    private fun handleForEachStatement(ctx: CPPASTRangeBasedForStatement): ForEachStatement {
        val statement = newForEachStatement(rawNode = ctx).withChildren(true) {
            val decl = frontend.declarationHandler.handle(ctx.declaration)
            val `var` = newDeclarationStatement()
            `var`.singleDeclaration = decl
            val iterable: Statement? = frontend.expressionHandler.handle(ctx.initializerClause)
            it.variable = `var`
            it.iterable = iterable
            it.statement = handle(ctx.body)
        }
        return statement
    }

    private fun handleBreakStatement(ctx: IASTBreakStatement): BreakStatement {
        return newBreakStatement(rawNode = ctx)
        // C++ has no labeled break
    }

    private fun handleContinueStatement(ctx: IASTContinueStatement): ContinueStatement {
        return newContinueStatement(rawNode = ctx)
        // C++ has no labeled continue
    }

    private fun handleExpressionStatement(ctx: IASTExpressionStatement): Expression {
        val expression =
            frontend.expressionHandler.handle(ctx.expression)?.codeAndLocationFromOtherRawNode(ctx)
                ?: ProblemExpression("could not parse expression in statement")

        return expression
    }

    private fun handleDeclarationStatement(ctx: IASTDeclarationStatement): Statement {
        return if (ctx.declaration is IASTASMDeclaration) {
            // TODO: Specify the contained language through a language node and find a way to run a
            //  frontend for sub-block if available
            newDistinctLanguageBlock(rawNode = ctx)
        } else {
            val declarationStatement = newDeclarationStatement(rawNode = ctx).withChildren {
                val declaration = frontend.declarationHandler.handle(ctx.declaration)
                if (declaration is DeclarationSequence) {
                    it.declarations = declaration.asList()
                } else {
                    it.singleDeclaration = declaration
                }
            }
            declarationStatement
        }
    }

    private fun handleReturnStatement(ctx: IASTReturnStatement): ReturnStatement {
        val returnStatement = newReturnStatement(rawNode = ctx).withChildren {
            // Parse the return value
            if (ctx.returnValue != null) {
                it.returnValue = frontend.expressionHandler.handle(ctx.returnValue)
            }
        }
        return returnStatement
    }

    private fun handleCompoundStatement(ctx: IASTCompoundStatement): Block {
        val block = newBlock(rawNode = ctx).withChildren(true){
            for (statement in ctx.statements) {
                val handled = handle(statement)
                if (handled != null) {
                    it.addStatement(handled)
                }
            }
        }

        return block
    }

    private fun handleSwitchStatement(ctx: IASTSwitchStatement): SwitchStatement {
        val switchStatement = newSwitchStatement(rawNode = ctx).withChildren(true) {
            // Special treatment for C++ switch
            if (ctx is CPPASTSwitchStatement) {
                if (ctx.initializerStatement != null) {
                    it.initializerStatement = handle(ctx.initializerStatement)
                }
                if (ctx.controllerDeclaration != null) {
                    it.selectorDeclaration =
                        frontend.declarationHandler.handle(ctx.controllerDeclaration)
                }
            }

            if (ctx.controllerExpression != null) {
                it.selector = frontend.expressionHandler.handle(ctx.controllerExpression)
            }

            it.statement = handle(ctx.body)
        }

        return switchStatement
    }

    private fun handleCaseStatement(ctx: IASTCaseStatement): CaseStatement {
        val caseStatement = newCaseStatement(rawNode = ctx).withChildren {
            it.caseExpression = frontend.expressionHandler.handle(ctx.expression)
        }
        return caseStatement
    }

    private fun handleDefaultStatement(ctx: IASTDefaultStatement): DefaultStatement {
        return newDefaultStatement(rawNode = ctx)
    }
}
