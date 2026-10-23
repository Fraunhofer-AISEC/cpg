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
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.expressions.*
import de.fraunhofer.aisec.cpg.graph.expressions.Block
import de.fraunhofer.aisec.cpg.graph.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.expressions.ProblemExpression
import de.fraunhofer.aisec.cpg.helpers.Util
import java.util.*
import java.util.function.BiConsumer
import java.util.function.Supplier
import java.util.stream.Collectors
import org.eclipse.cdt.core.dom.ast.*
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCatchHandler
import org.eclipse.cdt.internal.core.dom.parser.cpp.*

class StatementHandler(lang: CXXLanguageFrontend) :
    CXXHandler<Expression, IASTStatement>(Supplier(::ProblemExpression), lang) {

    override fun handleNode(node: IASTStatement): Expression {
        return when (node) {
            is IASTCompoundStatement -> handleCompoundStatement(node)
            is IASTReturnStatement -> handleReturn(node)
            is IASTDeclarationStatement -> handleDeclarationStatement(node)
            is IASTExpressionStatement -> handleExpressionStatement(node)
            is IASTIfStatement -> handleIf(node)
            is IASTWhileStatement -> handleWhile(node)
            is IASTDoStatement -> handleDo(node)
            is IASTForStatement -> handleFor(node)
            is IASTContinueStatement -> handleContinue(node)
            is IASTBreakStatement -> handleBreak(node)
            is IASTLabelStatement -> handleLabel(node)
            is IASTSwitchStatement -> handleSwitch(node)
            is IASTCaseStatement -> handleCase(node)
            is IASTDefaultStatement -> handleDefault(node)
            is IASTNullStatement -> handleEmpty(node)
            is IASTGotoStatement -> handleGoto(node)
            is IASTProblemStatement -> handleProblemStatement(node)
            is CPPASTRangeBasedForStatement -> handleForEach(node)
            is CPPASTTryBlockStatement -> handleTryBlockStatement(node)
            is CPPASTCatchHandler -> handleCatchHandler(node)
            else -> {
                handleNotSupported(node, node.javaClass.name)
            }
        }
    }

    private fun handleProblemStatement(problemStatement: IASTProblemStatement): ProblemExpression {
        Util.errorWithFileLocation(
            frontend,
            problemStatement,
            log,
            problemStatement.problem.message,
        )

        return newProblemExpression(problemStatement.problem.message)
    }

    private fun handleEmpty(nullStatement: IASTNullStatement): Empty {
        return newEmpty(rawNode = nullStatement)
    }

    private fun handleTryBlockStatement(tryBlockStatement: CPPASTTryBlockStatement): Try {
        return newTry(enterScope = true) { tryStatement ->
            val statement = handle(tryBlockStatement.tryBody) as Block?
            val catchClauses =
                Arrays.stream(tryBlockStatement.catchHandlers)
                    .map { handleCatchHandler(it) }
                    .collect(Collectors.toList())
            tryStatement.tryBlock = statement
            tryStatement.catchClauses = catchClauses
        }
    }

    private fun handleCatchHandler(catchHandler: ICPPASTCatchHandler): CatchClause {
        return newCatchClause(rawNode = catchHandler, enterScope = true) { catchClause ->
            val body = frontend.statementHandler.handle(catchHandler.catchBody)

            // TODO: can also be an 'unnamed' parameter. In this case we should not declare a
            // variable
            var decl: Declaration? = null
            if (catchHandler.declaration != null) { // can be null for "catch(...)"
                decl = frontend.declarationHandler.handle(catchHandler.declaration)
            }

            catchClause.body = body as? Block

            if (decl is Variable) {
                frontend.scopeManager.addDeclaration(decl)
                catchClause.parameter = decl
            }
        }
    }

    private fun handleIf(ctx: IASTIfStatement): IfElse {
        return newIfElse(rawNode = ctx, enterScope = true) { statement ->
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
        }
    }

    private fun handleLabel(ctx: IASTLabelStatement): Label {
        return newLabel(rawNode = ctx) { statement ->
            statement.subStatement = handle(ctx.nestedStatement)
            statement.label = ctx.name.toString()
            statement.name = newName(name = ctx.name.toString())
        }
    }

    private fun handleGoto(ctx: IASTGotoStatement): Goto {
        return newGoto(rawNode = ctx) { statement ->
            val assigneeTargetLabel = BiConsumer { _: Any, to: Node ->
                statement.targetLabel = to as Label
                to.label?.let {
                    statement.labelName = it
                    statement.name = newName(it)
                }
            }
            val b: IBinding?
            try {
                b = ctx.name.resolveBinding()
                if (b is ILabel) {
                    // If the bound AST node is/or was transformed into a CPG node the cpg node is
                    // bound to the CPG goto statement
                    frontend.registerObjectListener(b.labelStatement, assigneeTargetLabel)
                }
            } catch (_: Exception) {
                // If the Label AST node could not be resolved, the matching is done based on label
                // names of CPG nodes using the predicate listeners
                frontend.registerPredicateListener(
                    { _, to -> (to is Label && to.label == statement.labelName) },
                    assigneeTargetLabel,
                )
            }
        }
    }

    private fun handleWhile(ctx: IASTWhileStatement): While {
        return newWhile(rawNode = ctx, enterScope = true) { statement ->
            // Special treatment for C++ while
            if (ctx is CPPASTWhileStatement && ctx.conditionDeclaration != null) {
                statement.conditionDeclaration =
                    frontend.declarationHandler.handle(ctx.conditionDeclaration)
            }

            if (ctx.condition != null) {
                statement.condition = frontend.expressionHandler.handle(ctx.condition)
            }

            statement.statement = handle(ctx.body)
        }
    }

    private fun handleDo(ctx: IASTDoStatement): DoWhile {
        return newDoWhile(rawNode = ctx, enterScope = true) { statement ->
            statement.condition = frontend.expressionHandler.handle(ctx.condition)
            statement.statement = handle(ctx.body)
        }
    }

    private fun handleFor(ctx: IASTForStatement): For {
        return newFor(rawNode = ctx, enterScope = true) { statement ->
            statement.initializerStatement = handle(ctx.initializerStatement)

            // Special treatment for C++ while
            if (ctx is CPPASTForStatement && ctx.conditionDeclaration != null) {
                statement.conditionDeclaration =
                    frontend.declarationHandler.handle(ctx.conditionDeclaration)
            }

            if (ctx.conditionExpression != null) {
                statement.condition = frontend.expressionHandler.handle(ctx.conditionExpression)
            }

            // Adds true expression node where default empty condition evaluates to true, remove
            // here and in java StatementAnalyzer
            if (statement.conditionDeclaration == null && statement.condition == null) {
                val literal: Literal<*> =
                    newLiteral(true, primitiveType("bool")).implicit(code = "true")
                statement.condition = literal
            }

            if (ctx.iterationExpression != null) {
                statement.iterationStatement =
                    frontend.expressionHandler.handle(ctx.iterationExpression)
            }

            statement.statement = handle(ctx.body)
        }
    }

    private fun handleForEach(ctx: CPPASTRangeBasedForStatement): ForEach {
        return newForEach(rawNode = ctx, enterScope = true) { statement ->
            val decl = frontend.declarationHandler.handle(ctx.declaration)
            val `var` = newDeclarationStatement()
            `var`.singleDeclaration = decl
            val iterable: Expression? = frontend.expressionHandler.handle(ctx.initializerClause)
            statement.variable = `var`
            statement.iterable = iterable
            statement.statement = handle(ctx.body)
        }
    }

    private fun handleBreak(ctx: IASTBreakStatement): Break {
        return newBreak(rawNode = ctx)
        // C++ has no labeled break
    }

    private fun handleContinue(ctx: IASTContinueStatement): Continue {
        return newContinue(rawNode = ctx)
        // C++ has no labeled continue
    }

    private fun handleExpressionStatement(ctx: IASTExpressionStatement): Expression {
        val expression =
            frontend.expressionHandler.handle(ctx.expression)?.codeAndLocationFromOtherRawNode(ctx)
                ?: ProblemExpression("could not parse expression in statement")

        return expression
    }

    private fun handleDeclarationStatement(ctx: IASTDeclarationStatement): Expression {
        return if (ctx.declaration is IASTASMDeclaration) {
            // TODO: Specify the contained language through a language node and find a way to run a
            //  frontend for sub-block if available
            newDistinctLanguageBlock(rawNode = ctx)
        } else {
            newDeclarationStatement(rawNode = ctx) { declarationStatement ->
                val declaration = frontend.declarationHandler.handle(ctx.declaration)
                val declarations =
                    if (declaration is DeclarationSequence) {
                        declaration.asMutableList()
                    } else {
                        listOfNotNull(declaration)
                    }
                declarations.forEach {
                    frontend.scopeManager.addDeclaration(it)
                    declarationStatement.addDeclaration(it)
                }
            }
        }
    }

    private fun handleReturn(ctx: IASTReturnStatement): Return {
        return newReturn(rawNode = ctx) { returnStatement ->
            // Parse the return value
            if (ctx.returnValue != null) {
                returnStatement.returnValue = frontend.expressionHandler.handle(ctx.returnValue)
            }
        }
    }

    private fun handleCompoundStatement(ctx: IASTCompoundStatement): Block {
        return newBlock(rawNode = ctx, enterScope = true) { block ->
            for (statement in ctx.statements) {
                val handled = handle(statement)
                if (handled != null) {
                    block.statements += handled
                }
            }
        }
    }

    private fun handleSwitch(ctx: IASTSwitchStatement): Switch {
        return newSwitch(rawNode = ctx, enterScope = true) { switchStatement ->
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
                switchStatement.selector =
                    frontend.expressionHandler.handle(ctx.controllerExpression)
            }

            switchStatement.statement = handle(ctx.body)
        }
    }

    private fun handleCase(ctx: IASTCaseStatement): Case {
        return newCase(rawNode = ctx) { caseStatement ->
            caseStatement.caseExpression = frontend.expressionHandler.handle(ctx.expression)
        }
    }

    private fun handleDefault(ctx: IASTDefaultStatement): Default {
        return newDefault(rawNode = ctx)
    }
}
