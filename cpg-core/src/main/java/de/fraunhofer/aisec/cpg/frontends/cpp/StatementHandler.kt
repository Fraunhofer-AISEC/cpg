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
package de.fraunhofer.aisec.cpg.frontends.cpp

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.DeclarationSequence
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.*
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
    CXXHandler<Statement?, IASTStatement>(Supplier(::ProblemExpression), lang) {

    override fun handleNode(node: IASTStatement): Statement? {
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

    private fun handleProblemStatement(problemStmt: IASTProblemStatement): ProblemExpression {
        Util.errorWithFileLocation(frontend, problemStmt, log, problemStmt.problem.message)

        return newProblemExpression(
            problemStmt.problem.message,
        )
    }

    private fun handleEmptyStatement(emptyStmt: IASTNullStatement): EmptyStatement {
        return newEmptyStatement(emptyStmt.rawSignature)
    }

    private fun handleTryBlockStatement(tryStmt: CPPASTTryBlockStatement): TryStatement {
        val tryStatement = newTryStatement(tryStmt.toString())
        frontend.scopeManager.enterScope(tryStatement)
        val statement = handle(tryStmt.tryBody) as CompoundStatement?
        val catchClauses =
            Arrays.stream(tryStmt.catchHandlers)
                .map { handleCatchHandler(it) }
                .collect(Collectors.toList())
        tryStatement.tryBlock = statement
        tryStatement.catchClauses = catchClauses
        frontend.scopeManager.leaveScope(tryStatement)
        return tryStatement
    }

    private fun handleCatchHandler(catchHandler: ICPPASTCatchHandler): CatchClause {
        val catchClause = newCatchClause(catchHandler.rawSignature)
        frontend.scopeManager.enterScope(catchClause)

        val body = frontend.statementHandler.handle(catchHandler.catchBody)

        // TODO: can also be an 'unnamed' parameter. In this case we should not declare a variable
        var decl: Declaration? = null
        if (catchHandler.declaration != null) { // can be null for "catch(...)"
            decl = frontend.declarationHandler.handle(catchHandler.declaration)
        }

        catchClause.body = body as? CompoundStatement

        if (decl != null) {
            catchClause.setParameter((decl as VariableDeclaration?)!!)
        }
        frontend.scopeManager.leaveScope(catchClause)
        return catchClause
    }

    private fun handleIfStatement(ctx: IASTIfStatement): IfStatement {
        val statement = newIfStatement(ctx.rawSignature)

        frontend.scopeManager.enterScope(statement)

        // We need some special treatment for C++ IfStatements
        if (ctx is CPPASTIfStatement) {
            if (ctx.initializerStatement != null) {
                statement.initializerStatement = handle(ctx.initializerStatement)
            }
            if (ctx.conditionDeclaration != null) {
                statement.conditionDeclaration =
                    frontend.declarationHandler.handle(ctx.conditionDeclaration)
            }

            statement.isConstExpression = ctx.isConstexpr
        }

        if (ctx.conditionExpression != null)
            statement.condition = frontend.expressionHandler.handle(ctx.conditionExpression)
        statement.thenStatement = handle(ctx.thenClause)
        if (ctx.elseClause != null) {
            statement.elseStatement = handle(ctx.elseClause)
        }

        frontend.scopeManager.leaveScope(statement)

        return statement
    }

    private fun handleLabelStatement(ctx: IASTLabelStatement): LabelStatement {
        val statement = newLabelStatement(ctx.rawSignature)
        statement.subStatement = handle(ctx.nestedStatement)
        statement.label = ctx.name.toString()
        return statement
    }

    private fun handleGotoStatement(ctx: IASTGotoStatement): GotoStatement {
        val statement = newGotoStatement(ctx.rawSignature)
        val assigneeTargetLabel = BiConsumer { _: Any, to: Any? ->
            statement.targetLabel = to as LabelStatement?
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
        val statement = newWhileStatement(ctx.rawSignature)

        frontend.scopeManager.enterScope(statement)

        // Special treatment for C++ while
        if (ctx is CPPASTWhileStatement && ctx.conditionDeclaration != null) {
            statement.conditionDeclaration =
                frontend.declarationHandler.handle(ctx.conditionDeclaration)
        }

        if (ctx.condition != null) {
            statement.condition = frontend.expressionHandler.handle(ctx.condition)
        }

        statement.statement = handle(ctx.body)

        frontend.scopeManager.leaveScope(statement)

        return statement
    }

    private fun handleDoStatement(ctx: IASTDoStatement): DoStatement {
        val statement = newDoStatement(ctx.rawSignature)
        frontend.scopeManager.enterScope(statement)
        statement.condition = frontend.expressionHandler.handle(ctx.condition)
        statement.statement = handle(ctx.body)
        frontend.scopeManager.leaveScope(statement)
        return statement
    }

    private fun handleForStatement(ctx: IASTForStatement): ForStatement {
        val statement = newForStatement(ctx.rawSignature)

        frontend.scopeManager.enterScope(statement)

        statement.initializerStatement = handle(ctx.initializerStatement)

        // Special treatment for C++ while
        if (ctx is CPPASTForStatement && ctx.conditionDeclaration != null) {
            statement.conditionDeclaration =
                frontend.declarationHandler.handle(ctx.conditionDeclaration)
        }

        if (ctx.conditionExpression != null) {
            statement.condition = frontend.expressionHandler.handle(ctx.conditionExpression)
        }

        // Adds true expression node where default empty condition evaluates to true, remove here
        // and in java StatementAnalyzer
        if (statement.conditionDeclaration == null && statement.condition == null) {
            val literal: Literal<*> = newLiteral(true, parseType("bool"), "true")
            statement.condition = literal
        }

        if (ctx.iterationExpression != null) {
            statement.iterationStatement =
                frontend.expressionHandler.handle(ctx.iterationExpression)
        }

        statement.statement = handle(ctx.body)

        frontend.scopeManager.leaveScope(statement)

        return statement
    }

    private fun handleForEachStatement(ctx: CPPASTRangeBasedForStatement): ForEachStatement {
        val statement = newForEachStatement(ctx.rawSignature)
        frontend.scopeManager.enterScope(statement)
        val decl = frontend.declarationHandler.handle(ctx.declaration)
        val `var` = newDeclarationStatement(decl!!.code)
        `var`.singleDeclaration = decl
        val iterable: Statement? = frontend.expressionHandler.handle(ctx.initializerClause)
        statement.variable = `var`
        statement.iterable = iterable
        statement.statement = handle(ctx.body)
        frontend.scopeManager.leaveScope(statement)
        return statement
    }

    private fun handleBreakStatement(ctx: IASTBreakStatement): BreakStatement {
        return newBreakStatement(ctx.rawSignature)
        // C++ has no labeled break
    }

    private fun handleContinueStatement(ctx: IASTContinueStatement): ContinueStatement {
        return newContinueStatement(ctx.rawSignature)
        // C++ has no labeled continue
    }

    private fun handleExpressionStatement(ctx: IASTExpressionStatement): Expression? {
        val expression = frontend.expressionHandler.handle(ctx.expression)

        // update the code and region to include the whole statement
        if (expression != null) {
            frontend.setCodeAndLocation(expression, ctx)
        }

        return expression
    }

    private fun handleDeclarationStatement(ctx: IASTDeclarationStatement): DeclarationStatement? {
        return if (ctx.declaration is IASTASMDeclaration) {
            newASMDeclarationStatement(ctx.rawSignature)
        } else {
            val declarationStatement = newDeclarationStatement(ctx.rawSignature)
            val declaration = frontend.declarationHandler.handle(ctx.declaration)
            if (declaration is DeclarationSequence) {
                declarationStatement.declarations = declaration.asList()
            } else {
                declarationStatement.singleDeclaration = declaration
            }
            declarationStatement
        }
    }

    private fun handleReturnStatement(ctx: IASTReturnStatement): ReturnStatement {
        val returnStatement = newReturnStatement(ctx.rawSignature)

        // Parse the return value
        if (ctx.returnValue != null) {
            returnStatement.returnValue = frontend.expressionHandler.handle(ctx.returnValue)
        }

        return returnStatement
    }

    private fun handleCompoundStatement(ctx: IASTCompoundStatement): CompoundStatement {
        val compoundStatement = newCompoundStatement(ctx.rawSignature)

        frontend.scopeManager.enterScope(compoundStatement)

        for (statement in ctx.statements) {
            val handled = handle(statement)
            if (handled != null) {
                compoundStatement.addStatement(handled)
            }
        }

        frontend.scopeManager.leaveScope(compoundStatement)

        return compoundStatement
    }

    private fun handleSwitchStatement(ctx: IASTSwitchStatement): SwitchStatement {
        val switchStatement = newSwitchStatement(ctx.rawSignature)

        frontend.scopeManager.enterScope(switchStatement)

        // Special treatment for C++ switch
        if (ctx is CPPASTSwitchStatement) {
            if (ctx.initializerStatement != null) {
                switchStatement.initializerStatement = handle(ctx.initializerStatement)
            }
            if (ctx.controllerDeclaration != null) {
                switchStatement.selectorDeclaration =
                    frontend.declarationHandler.handle(ctx.controllerDeclaration)
            }
        }

        if (ctx.controllerExpression != null) {
            switchStatement.setSelector(frontend.expressionHandler.handle(ctx.controllerExpression))
        }

        switchStatement.statement = handle(ctx.body)

        frontend.scopeManager.leaveScope(switchStatement)

        return switchStatement
    }

    private fun handleCaseStatement(ctx: IASTCaseStatement): CaseStatement {
        val caseStatement = newCaseStatement(ctx.rawSignature)
        caseStatement.setCaseExpression(frontend.expressionHandler.handle(ctx.expression))
        return caseStatement
    }

    private fun handleDefaultStatement(ctx: IASTDefaultStatement): DefaultStatement {
        return newDefaultStatement(ctx.rawSignature)
    }
}
