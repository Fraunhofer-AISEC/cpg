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

import de.fraunhofer.aisec.cpg.frontends.*
import de.fraunhofer.aisec.cpg.graph.NodeBuilder
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import java.util.*
import java.util.function.BiConsumer
import java.util.function.Supplier
import java.util.stream.Collectors
import org.eclipse.cdt.core.dom.ast.IASTStatement
import org.eclipse.cdt.core.dom.ast.IBinding
import org.eclipse.cdt.core.dom.ast.ILabel
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCatchHandler
import org.eclipse.cdt.internal.core.dom.parser.cpp.*

class StatementHandler(lang: CXXLanguageFrontend) :
    Handler<Statement?, IASTStatement, CXXLanguageFrontend>(
        Supplier { ProblemExpression() },
        lang
    ) {

    init {
        map[CPPASTCompoundStatement::class.java] = HandlerInterface {
            handleCompoundStatement(it as CPPASTCompoundStatement)
        }
        map[CPPASTReturnStatement::class.java] = HandlerInterface {
            handleReturnStatement(it as CPPASTReturnStatement)
        }
        map[CPPASTDeclarationStatement::class.java] = HandlerInterface {
            handleDeclarationStatement(it as CPPASTDeclarationStatement)
        }
        map[CPPASTExpressionStatement::class.java] = HandlerInterface {
            handleExpressionStatement(it as CPPASTExpressionStatement)
        }
        map[CPPASTIfStatement::class.java] = HandlerInterface {
            handleIfStatement(it as CPPASTIfStatement)
        }
        map[CPPASTWhileStatement::class.java] = HandlerInterface {
            handleWhileStatement(it as CPPASTWhileStatement)
        }
        map[CPPASTDoStatement::class.java] = HandlerInterface {
            handleDoStatement(it as CPPASTDoStatement)
        }
        map[CPPASTForStatement::class.java] = HandlerInterface {
            handleForStatement(it as CPPASTForStatement)
        }
        map[CPPASTRangeBasedForStatement::class.java] = HandlerInterface {
            handleForEachStatement(it as CPPASTRangeBasedForStatement)
        }
        map[CPPASTContinueStatement::class.java] = HandlerInterface {
            handleContinueStatement(it as CPPASTContinueStatement)
        }
        map[CPPASTBreakStatement::class.java] = HandlerInterface {
            handleBreakStatement(it as CPPASTBreakStatement)
        }
        map[CPPASTLabelStatement::class.java] = HandlerInterface {
            handleLabelStatement(it as CPPASTLabelStatement)
        }
        map[CPPASTSwitchStatement::class.java] = HandlerInterface {
            handleSwitchStatement(it as CPPASTSwitchStatement)
        }
        map[CPPASTCaseStatement::class.java] = HandlerInterface {
            handleCaseStatement(it as CPPASTCaseStatement)
        }
        map[CPPASTDefaultStatement::class.java] = HandlerInterface {
            handleDefaultStatement(it as CPPASTDefaultStatement)
        }
        map[CPPASTNullStatement::class.java] = HandlerInterface {
            handleEmptyStatement(it as CPPASTNullStatement)
        }
        map[CPPASTGotoStatement::class.java] = HandlerInterface {
            handleGotoStatement(it as CPPASTGotoStatement)
        }
        map[CPPASTTryBlockStatement::class.java] = HandlerInterface {
            handleTryBlockStatement(it as CPPASTTryBlockStatement)
        }
        map[CPPASTCatchHandler::class.java] = HandlerInterface {
            handleCatchHandler(it as ICPPASTCatchHandler)
        }
    }

    private fun handleEmptyStatement(emptyStmt: CPPASTNullStatement): EmptyStatement {
        return NodeBuilder.newEmptyStatement(emptyStmt.rawSignature)
    }

    private fun handleTryBlockStatement(tryStmt: CPPASTTryBlockStatement): TryStatement {
        val tryStatement = NodeBuilder.newTryStatement(tryStmt.toString())
        lang.scopeManager.enterScope(tryStatement)
        val statement = handle(tryStmt.tryBody) as CompoundStatement?
        val catchClauses =
            Arrays.stream(tryStmt.catchHandlers)
                .map { catchHandler: ICPPASTCatchHandler -> handleCatchHandler(catchHandler) }
                .collect(Collectors.toList())
        tryStatement.tryBlock = statement
        tryStatement.catchClauses = catchClauses
        lang.scopeManager.leaveScope(tryStatement)
        return tryStatement
    }

    private fun handleCatchHandler(catchHandler: ICPPASTCatchHandler): CatchClause {
        val catchClause = NodeBuilder.newCatchClause(catchHandler.rawSignature)
        lang.scopeManager.enterScope(catchClause)

        val body = lang.statementHandler.handle(catchHandler.catchBody)

        // TODO: can also be an 'unnamed' parameter. In this case we should not declare a variable
        var decl: Declaration? = null
        if (catchHandler.declaration != null) { // can be null for "catch(...)"
            decl = lang.declarationHandler.handle(catchHandler.declaration)
        }

        catchClause.body = body as? CompoundStatement

        if (decl != null) {
            catchClause.setParameter((decl as VariableDeclaration?)!!)
        }
        lang.scopeManager.leaveScope(catchClause)
        return catchClause
    }

    private fun handleIfStatement(ctx: CPPASTIfStatement): IfStatement {
        val statement = NodeBuilder.newIfStatement(ctx.rawSignature)
        lang.scopeManager.enterScope(statement)
        if (ctx.initializerStatement != null) {
            statement.initializerStatement = handle(ctx.initializerStatement)
        }
        if (ctx.conditionDeclaration != null) {
            statement.conditionDeclaration =
                lang.declarationHandler.handle(ctx.conditionDeclaration)
        }
        statement.isConstExpression = ctx.isConstexpr
        if (ctx.conditionExpression != null)
            statement.condition = lang.expressionHandler.handle(ctx.conditionExpression)
        statement.thenStatement = handle(ctx.thenClause)
        if (ctx.elseClause != null) {
            statement.elseStatement = handle(ctx.elseClause)
        }
        lang.scopeManager.leaveScope(statement)
        return statement
    }

    private fun handleLabelStatement(ctx: CPPASTLabelStatement): LabelStatement {
        val statement = NodeBuilder.newLabelStatement(ctx.rawSignature)
        statement.subStatement = handle(ctx.nestedStatement)
        statement.label = ctx.name.toString()
        return statement
    }

    private fun handleGotoStatement(ctx: CPPASTGotoStatement): GotoStatement {
        val statement = NodeBuilder.newGotoStatement(ctx.rawSignature)
        val assigneeTargetLabel = BiConsumer { _: Any, to: Any? ->
            statement.targetLabel = to as LabelStatement?
        }
        val b: IBinding?
        try {
            b = ctx.name.resolveBinding()
            if (b is ILabel) {
                b.labelStatement
                // If the bound AST node is/or was transformed into a CPG node the cpg node is bound
                // to the
                // CPG goto statement
                lang.registerObjectListener(b.labelStatement, assigneeTargetLabel)
            }
        } catch (e: Exception) {
            // If the Label AST node was could not be resolved, the matchign is done based on label
            // names
            // of CPG nodes using the predicate listeners
            lang.registerPredicateListener(
                { _: Any?, to: Any? -> (to is LabelStatement && to.label == statement.labelName) },
                assigneeTargetLabel
            )
        }
        return statement
    }

    private fun handleWhileStatement(ctx: CPPASTWhileStatement): WhileStatement {
        val statement = NodeBuilder.newWhileStatement(ctx.rawSignature)
        lang.scopeManager.enterScope(statement)
        if (ctx.conditionDeclaration != null)
            statement.conditionDeclaration =
                lang.declarationHandler.handle(ctx.conditionDeclaration)
        if (ctx.condition != null)
            statement.condition = lang.expressionHandler.handle(ctx.condition)
        statement.statement = handle(ctx.body)
        lang.scopeManager.leaveScope(statement)
        return statement
    }

    private fun handleDoStatement(ctx: CPPASTDoStatement): DoStatement {
        val statement = NodeBuilder.newDoStatement(ctx.rawSignature)
        lang.scopeManager.enterScope(statement)
        statement.condition = lang.expressionHandler.handle(ctx.condition)
        statement.statement = handle(ctx.body)
        lang.scopeManager.leaveScope(statement)
        return statement
    }

    private fun handleForStatement(ctx: CPPASTForStatement): ForStatement {
        val statement = NodeBuilder.newForStatement(ctx.rawSignature)
        lang.scopeManager.enterScope(statement)
        statement.initializerStatement = handle(ctx.initializerStatement)
        if (ctx.conditionDeclaration != null)
            statement.conditionDeclaration =
                lang.declarationHandler.handle(ctx.conditionDeclaration)
        if (ctx.conditionExpression != null)
            statement.condition = lang.expressionHandler.handle(ctx.conditionExpression)

        // Adds true expression node where default empty condition evaluates to true, remove here
        // and in
        // java StatementAnalyzer
        if (statement.conditionDeclaration == null && statement.condition == null) {
            val literal: Literal<*> =
                NodeBuilder.newLiteral(true, TypeParser.createFrom("bool", true), "true")
            statement.condition = literal
        }
        if (ctx.iterationExpression != null)
            statement.iterationStatement = lang.expressionHandler.handle(ctx.iterationExpression)
        statement.statement = handle(ctx.body)
        lang.scopeManager.leaveScope(statement)
        return statement
    }

    private fun handleForEachStatement(ctx: CPPASTRangeBasedForStatement): ForEachStatement {
        val statement = NodeBuilder.newForEachStatement(ctx.rawSignature)
        lang.scopeManager.enterScope(statement)
        val decl = lang.declarationHandler.handle(ctx.declaration)
        val `var` = NodeBuilder.newDeclarationStatement(decl!!.code)
        `var`.singleDeclaration = decl
        val iterable: Statement? = lang.expressionHandler.handle(ctx.initializerClause)
        statement.variable = `var`
        statement.iterable = iterable
        statement.statement = handle(ctx.body)
        lang.scopeManager.leaveScope(statement)
        return statement
    }

    private fun handleBreakStatement(ctx: CPPASTBreakStatement): BreakStatement? {
        return NodeBuilder.newBreakStatement(ctx.rawSignature)
        // C++ has no labeled break
    }

    private fun handleContinueStatement(ctx: CPPASTContinueStatement): ContinueStatement? {
        return NodeBuilder.newContinueStatement(ctx.rawSignature)
        // C++ has no labeled continue
    }

    private fun handleExpressionStatement(ctx: CPPASTExpressionStatement): Expression? {
        val expression = lang.expressionHandler.handle(ctx.expression)

        // update the code and region to include the whole statement
        if (expression != null) {
            lang.setCodeAndRegion(expression, ctx)
        }

        return expression
    }

    private fun handleDeclarationStatement(ctx: CPPASTDeclarationStatement): DeclarationStatement? {
        return if (ctx.declaration is CPPASTASMDeclaration) {
            NodeBuilder.newASMDeclarationStatement(ctx.rawSignature)
        } else if (ctx.rawSignature.contains("typedef")) {
            TypeManager.getInstance().handleTypedef(lang, ctx.rawSignature)
            null
        } else {
            val declarationStatement = NodeBuilder.newDeclarationStatement(ctx.rawSignature)
            val declaration = lang.declarationHandler.handle(ctx.declaration)
            if (declaration is DeclarationSequence) {
                declarationStatement.declarations = declaration.asList()
            } else {
                declarationStatement.singleDeclaration = declaration
            }
            declarationStatement
        }
    }

    private fun handleReturnStatement(ctx: CPPASTReturnStatement): ReturnStatement {
        val returnStatement = NodeBuilder.newReturnStatement(ctx.rawSignature)
        // parse the return value
        // Todo Handle ReturnArgument
        if (ctx.returnValue != null)
            returnStatement.returnValue = lang.expressionHandler.handle(ctx.returnValue)
        return returnStatement
    }

    private fun handleCompoundStatement(ctx: CPPASTCompoundStatement): CompoundStatement? {
        val compoundStatement = NodeBuilder.newCompoundStatement(ctx.rawSignature)
        lang.scopeManager.enterScope(compoundStatement)
        for (statement in ctx.statements) {
            val handled = handle(statement)
            if (handled != null) {
                compoundStatement.addStatement(handled)
            }
        }
        lang.scopeManager.leaveScope(compoundStatement)
        return compoundStatement
    }

    private fun handleSwitchStatement(ctx: CPPASTSwitchStatement): SwitchStatement {
        val switchStatement = NodeBuilder.newSwitchStatement(ctx.rawSignature)
        lang.scopeManager.enterScope(switchStatement)
        if (ctx.initializerStatement != null)
            switchStatement.initializerStatement = handle(ctx.initializerStatement)
        if (ctx.controllerDeclaration != null)
            switchStatement.selectorDeclaration =
                lang.declarationHandler.handle(ctx.controllerDeclaration)
        if (ctx.controllerExpression != null)
            switchStatement.setSelector(lang.expressionHandler.handle(ctx.controllerExpression))
        switchStatement.statement = handle(ctx.body)
        lang.scopeManager.leaveScope(switchStatement)
        return switchStatement
    }

    private fun handleCaseStatement(ctx: CPPASTCaseStatement): CaseStatement {
        val caseStatement = NodeBuilder.newCaseStatement(ctx.rawSignature)
        caseStatement.setCaseExpression(lang.expressionHandler.handle(ctx.expression))
        return caseStatement
    }

    private fun handleDefaultStatement(ctx: CPPASTDefaultStatement): DefaultStatement {
        return NodeBuilder.newDefaultStatement(ctx.rawSignature)
    }
}
