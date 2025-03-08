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
package de.fraunhofer.aisec.cpg.frontends.java

import com.github.javaparser.JavaToken
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.stmt.*
import com.github.javaparser.ast.stmt.CatchClause
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.type.UnionType
import com.github.javaparser.utils.Pair
import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.frontends.HandlerInterface
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.AssertStatement
import de.fraunhofer.aisec.cpg.graph.statements.BreakStatement
import de.fraunhofer.aisec.cpg.graph.statements.ContinueStatement
import de.fraunhofer.aisec.cpg.graph.statements.DoStatement
import de.fraunhofer.aisec.cpg.graph.statements.EmptyStatement
import de.fraunhofer.aisec.cpg.graph.statements.ForEachStatement
import de.fraunhofer.aisec.cpg.graph.statements.ForStatement
import de.fraunhofer.aisec.cpg.graph.statements.IfStatement
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.SwitchStatement
import de.fraunhofer.aisec.cpg.graph.statements.SynchronizedStatement
import de.fraunhofer.aisec.cpg.graph.statements.TryStatement
import de.fraunhofer.aisec.cpg.graph.statements.WhileStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.util.function.Supplier
import kotlin.collections.set
import org.slf4j.LoggerFactory

class StatementHandler(lang: JavaLanguageFrontend?) :
    Handler<de.fraunhofer.aisec.cpg.graph.statements.Statement, Statement, JavaLanguageFrontend>(
        Supplier { ProblemExpression() },
        lang!!,
    ) {
    fun handleExpressionStatement(
        stmt: Statement
    ): de.fraunhofer.aisec.cpg.graph.statements.Statement? {
        // We want to use the code of the stmt, rather than the expression
        val expr =
            frontend.expressionHandler
                .handle(stmt.asExpressionStmt().expression)
                ?.codeAndLocationFromOtherRawNode(stmt)

        return expr
    }

    private fun handleThrowStmt(
        stmt: Statement
    ): de.fraunhofer.aisec.cpg.graph.statements.Statement {
        val throwStmt = stmt as ThrowStmt
        val throwOperation = newThrowExpression(rawNode = stmt)
        throwOperation.exception =
            frontend.expressionHandler.handle(throwStmt.expression)
                as de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
        return throwOperation
    }

    private fun handleReturnStatement(stmt: Statement): ReturnStatement {
        val returnStmt = stmt.asReturnStmt()
        val optionalExpression = returnStmt.expression
        var expression: de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression? = null
        if (optionalExpression.isPresent) {
            val expr = optionalExpression.get()

            // handle the expression as the first argument
            expression =
                frontend.expressionHandler.handle(expr)
                    as? de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
        }
        val returnStatement = this.newReturnStatement(rawNode = stmt)
        // JavaParser seems to add implicit return statements, that are not part of the original
        // source code. We mark it as such
        returnStatement.isImplicit = !returnStmt.tokenRange.isPresent

        // expressionRefersToDeclaration to arguments, if there are any
        expression?.let { returnStatement.returnValue = it }

        return returnStatement
    }

    private fun handleIfStatement(stmt: Statement): IfStatement {
        val ifStmt = stmt.asIfStmt()
        val conditionExpression = ifStmt.condition
        val thenStatement = ifStmt.thenStmt
        val optionalElseStatement = ifStmt.elseStmt
        val ifStatement = newIfStatement(rawNode = stmt)
        enterScope(ifStatement)
        ifStatement.thenStatement = handle(thenStatement)
        ifStatement.condition =
            frontend.expressionHandler.handle(conditionExpression)
                as de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
        optionalElseStatement.ifPresent { ifStatement.elseStatement = handle(it) }
        leaveScope(ifStatement)
        return ifStatement
    }

    private fun handleAssertStatement(stmt: Statement): AssertStatement {
        val assertStmt = stmt.asAssertStmt()
        val conditionExpression = assertStmt.check
        val thenStatement = assertStmt.message
        val assertStatement = newAssertStatement(rawNode = stmt)
        assertStatement.condition =
            frontend.expressionHandler.handle(conditionExpression)
                as de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
        thenStatement.ifPresent {
            assertStatement.message = frontend.expressionHandler.handle(thenStatement.get())
        }
        return assertStatement
    }

    private fun handleWhileStatement(stmt: Statement): WhileStatement {
        val whileStmt = stmt.asWhileStmt()
        val conditionExpression = whileStmt.condition
        val statement = whileStmt.body
        val whileStatement = newWhileStatement(rawNode = stmt)
        enterScope(whileStatement)
        whileStatement.statement = handle(statement)
        whileStatement.condition =
            frontend.expressionHandler.handle(conditionExpression)
                as de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
        leaveScope(whileStatement)
        return whileStatement
    }

    private fun handleForEachStatement(stmt: Statement): ForEachStatement {
        val statement = newForEachStatement(rawNode = stmt)
        enterScope(statement)
        val forEachStmt = stmt.asForEachStmt()
        val variable = frontend.expressionHandler.handle(forEachStmt.variable)
        val iterable = frontend.expressionHandler.handle(forEachStmt.iterable)
        if (variable !is DeclarationStatement) {
            log.error("Expected a DeclarationStatement but received: {}", variable?.name)
        } else {
            statement.variable = variable
        }
        statement.iterable = iterable
        statement.statement = handle(forEachStmt.body)
        leaveScope(statement)
        return statement
    }

    private fun handleForStatement(stmt: Statement): ForStatement {
        val forStmt = stmt.asForStmt()
        val statement = this.newForStatement(rawNode = stmt)
        enterScope(statement)
        if (forStmt.initialization.size > 1) {
            // code will be set later
            val initExprList = this.newExpressionList()
            for (initExpr in forStmt.initialization) {
                val s = frontend.expressionHandler.handle(initExpr)
                s?.let { initExprList.expressions += it }

                // can not update location
                if (s?.location == null) {
                    continue
                }
            }

            statement.initializerStatement = initExprList.codeAndLocationFromChildren(stmt)
        } else if (forStmt.initialization.size == 1) {
            statement.initializerStatement =
                frontend.expressionHandler.handle(forStmt.initialization[0])
        }
        forStmt.compare.ifPresent { condition: Expression ->
            statement.condition =
                frontend.expressionHandler.handle(condition)
                    as de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
        }

        // Adds true expression node where default empty condition evaluates to true, remove here
        // and in cpp StatementHandler
        if (statement.condition == null) {
            val literal: Literal<*> =
                this.newLiteral(true, this.primitiveType("boolean")).implicit("true")
            statement.condition = literal
        }
        if (forStmt.update.size > 1) {
            // code will be set later
            val iterationExprList = this.newExpressionList()
            for (updateExpr in forStmt.update) {
                val s = frontend.expressionHandler.handle(updateExpr)
                s?.let {
                    // make sure location is set
                    iterationExprList.expressions += it
                }

                // can not update location
                if (s?.location == null) {
                    continue
                }
            }

            statement.iterationStatement = iterationExprList.codeAndLocationFromChildren(stmt)
        } else if (forStmt.update.size == 1) {
            statement.iterationStatement = frontend.expressionHandler.handle(forStmt.update[0])
        }
        statement.statement = handle(forStmt.body)
        leaveScope(statement)
        return statement
    }

    private fun handleDoStatement(stmt: Statement): DoStatement {
        val doStmt = stmt.asDoStmt()
        val conditionExpression = doStmt.condition
        val statement = doStmt.body
        val doStatement = newDoStatement(rawNode = stmt)
        enterScope(doStatement)
        doStatement.statement = handle(statement)
        doStatement.condition =
            frontend.expressionHandler.handle(conditionExpression)
                as de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
        leaveScope(doStatement)
        return doStatement
    }

    private fun handleEmptyStatement(stmt: Statement): EmptyStatement {
        return this.newEmptyStatement(rawNode = stmt)
    }

    private fun handleSynchronizedStatement(stmt: Statement): SynchronizedStatement {
        val synchronizedJava = stmt.asSynchronizedStmt()
        val synchronizedCPG = newSynchronizedStatement(rawNode = stmt)
        synchronizedCPG.expression =
            frontend.expressionHandler.handle(synchronizedJava.expression)
                as de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
        synchronizedCPG.block = handle(synchronizedJava.body) as Block?
        return synchronizedCPG
    }

    private fun handleLabelStatement(stmt: Statement): LabelStatement {
        val labelStmt = stmt.asLabeledStmt()
        val label = labelStmt.label.identifier
        val statement = labelStmt.statement
        val labelStatement = newLabelStatement(rawNode = stmt)
        labelStatement.subStatement = handle(statement)
        labelStatement.label = label
        return labelStatement
    }

    private fun handleBreakStatement(stmt: Statement): BreakStatement {
        val breakStmt = stmt.asBreakStmt()
        val breakStatement = newBreakStatement(rawNode = stmt)
        breakStmt.label.ifPresent { label -> breakStatement.label = label.toString() }
        return breakStatement
    }

    private fun handleContinueStatement(stmt: Statement): ContinueStatement {
        val continueStmt = stmt.asContinueStmt()
        val continueStatement = newContinueStatement(rawNode = stmt)
        continueStmt.label.ifPresent { label -> continueStatement.label = label.toString() }
        return continueStatement
    }

    fun handleBlockStatement(stmt: Statement): Block {
        val blockStmt = stmt.asBlockStmt()

        // first of, all we need a compound statement
        val compoundStatement = newBlock(rawNode = stmt)
        enterScope(compoundStatement)
        for (child in blockStmt.statements) {
            val statement = handle(child)
            statement?.let { compoundStatement.statements += it }
        }
        leaveScope(compoundStatement)
        return compoundStatement
    }

    fun handleCaseDefaultStatement(
        caseExpression: Expression?,
        sEntry: SwitchEntry,
    ): de.fraunhofer.aisec.cpg.graph.statements.Statement {
        val parentLocation = frontend.locationOf(sEntry)
        val optionalTokenRange = sEntry.tokenRange
        var caseTokens = Pair<JavaToken?, JavaToken?>(null, null)
        if (optionalTokenRange.isEmpty) {
            log.error("Token for Region for Default case not available")
        }
        if (caseExpression == null) {
            if (optionalTokenRange.isPresent) {
                /*
                TODO: not sure if this is really necessary, it seems to be the same location as
                 parentLocation, except that column starts 1 character later and I am not sure if
                 this is correct anyway
                */
                // Compute region and code for self generated default statement to match the c++
                // versions
                caseTokens =
                    Pair(
                        getNextTokenWith("default", optionalTokenRange.get().begin),
                        getNextTokenWith(":", optionalTokenRange.get().begin),
                    )
            }
            val defaultStatement = newDefaultStatement()
            defaultStatement.location =
                getLocationsFromTokens(parentLocation, caseTokens.a, caseTokens.b)
            return defaultStatement
        }
        val caseExprTokenRange = caseExpression.tokenRange
        if (optionalTokenRange.isPresent && caseExprTokenRange.isPresent) {
            // Compute region and code for self generated case statement to match the c++ versions
            caseTokens =
                Pair(
                    getPreviousTokenWith("case", optionalTokenRange.get().begin),
                    getNextTokenWith(":", caseExprTokenRange.get().end),
                )
        }
        val caseStatement = this.newCaseStatement()
        caseStatement.caseExpression =
            frontend.expressionHandler.handle(caseExpression)
                as de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
        caseStatement.location = getLocationsFromTokens(parentLocation, caseTokens.a, caseTokens.b)
        return caseStatement
    }

    fun getPreviousTokenWith(text: String, startToken: JavaToken): JavaToken {
        var token = startToken
        var optional = token.previousToken
        while (token.text != text && optional.isPresent) {
            token = optional.get()
            optional = token.previousToken
        }
        return token
    }

    fun getNextTokenWith(text: String, startToken: JavaToken): JavaToken {
        var token = startToken
        var optional = token.nextToken
        while (token.text != text && optional.isPresent) {
            token = optional.get()
            optional = token.nextToken
        }
        return token
    }

    fun getLocationsFromTokens(
        parentLocation: PhysicalLocation?,
        startToken: JavaToken?,
        endToken: JavaToken?,
    ): PhysicalLocation? {
        // cannot construct location without parent location
        if (parentLocation == null) {
            return null
        }
        if (startToken != null && endToken != null) {
            val startOpt = startToken.range
            val endOpt = endToken.range
            if (startOpt.isPresent && endOpt.isPresent) {
                val rstart = startOpt.get()
                val rend = endOpt.get()
                val region =
                    Region(
                        rstart.begin.line,
                        rstart.begin.column,
                        rend.end.line,
                        rend.end.column + 1,
                    )
                return PhysicalLocation(parentLocation.artifactLocation.uri, region)
            }
        }
        return null
    }

    fun getCodeBetweenTokens(startToken: JavaToken?, endToken: JavaToken?): String {
        if (startToken == null || endToken == null) {
            return Type.UNKNOWN_TYPE_STRING
        }
        val newCode = StringBuilder(startToken.text)
        var current = startToken
        do {
            current = current?.nextToken?.orElse(null)
            if (current == null) {
                break
            }
            newCode.append(current.text)
        } while (current !== endToken)
        return newCode.toString()
    }

    fun handleSwitchStatement(stmt: Statement): SwitchStatement {
        val switchStmt = stmt.asSwitchStmt()
        val switchStatement = newSwitchStatement(rawNode = stmt)

        enterScope(switchStatement)
        switchStatement.selector =
            frontend.expressionHandler.handle(switchStmt.selector)
                as de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression

        // Compute region and code for self generated compound statement to match the c++ versions
        var start: JavaToken? = null
        var end: JavaToken? = null
        val tokenRange = switchStmt.tokenRange
        val tokenRangeSelector = switchStmt.selector.tokenRange
        if (tokenRange.isPresent && tokenRangeSelector.isPresent) {
            start = getNextTokenWith("{", tokenRangeSelector.get().end)
            end = getPreviousTokenWith("}", tokenRange.get().end)
        }
        val compoundStatement = this.newBlock()
        compoundStatement.code = getCodeBetweenTokens(start, end)
        compoundStatement.location = getLocationsFromTokens(switchStatement.location, start, end)
        for (sentry in switchStmt.entries) {
            if (sentry.labels.isEmpty()) {
                compoundStatement.statements += handleCaseDefaultStatement(null, sentry)
            }
            for (caseExp in sentry.labels) {
                compoundStatement.statements += handleCaseDefaultStatement(caseExp, sentry)
            }
            for (subStmt in sentry.statements) {
                compoundStatement.statements +=
                    handle(subStmt) ?: ProblemExpression("Could not parse statement")
            }
        }
        switchStatement.statement = compoundStatement
        leaveScope(switchStatement)
        return switchStatement
    }

    private fun handleExplicitConstructorInvocation(stmt: Statement): ConstructExpression {
        val explicitConstructorInvocationStmt = stmt.asExplicitConstructorInvocationStmt()
        var containingClass = ""
        val currentRecord = currentRecord
        if (currentRecord == null) {
            log.error(
                "Explicit constructor invocation has to be located inside a record declaration!"
            )
        } else {
            containingClass = currentRecord.name.toString()
        }

        val name = containingClass
        val node = this.newConstructExpression(name, rawNode = null)
        node.type = unknownType()

        // Create a reference either to "this"
        if (explicitConstructorInvocationStmt.isThis) {
            currentRecord?.toType()?.let { node.type = it }
            node.callee = this.newReference(name)
        } else {
            // or to our direct (first) super type
            currentRecord?.superTypes?.firstOrNull()?.let {
                node.type = it
                node.callee = this.newReference(it.name)
            }
        }

        val arguments =
            explicitConstructorInvocationStmt.arguments
                .map(frontend.expressionHandler::handle)
                .filterIsInstance<de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression>()
                .toMutableList()
        node.arguments = arguments

        return node
    }

    private fun handleTryStatement(stmt: Statement): TryStatement {
        val tryStmt = stmt.asTryStmt()
        val tryStatement = newTryStatement(rawNode = stmt)
        enterScope(tryStatement)
        val resources =
            tryStmt.resources
                .mapNotNull { ctx -> frontend.expressionHandler.handle(ctx) }
                .toMutableList()
        val tryBlock = handleBlockStatement(tryStmt.tryBlock)
        val catchClauses = tryStmt.catchClauses.map(::handleCatchClause).toMutableList()
        val finallyBlock = tryStmt.finallyBlock.map(::handleBlockStatement).orElse(null)
        leaveScope(tryStatement)
        tryStatement.resources = resources
        tryStatement.tryBlock = tryBlock
        tryStatement.finallyBlock = finallyBlock
        tryStatement.catchClauses = catchClauses
        for (r in resources) {
            if (r is DeclarationStatement) {
                for (d in r.declarations) {
                    if (d is VariableDeclaration) {
                        declareSymbol(d)
                    }
                }
            }
        }
        return tryStatement
    }

    private fun handleCatchClause(
        catchCls: CatchClause
    ): de.fraunhofer.aisec.cpg.graph.statements.CatchClause {
        val cClause = newCatchClause(rawNode = catchCls)
        enterScope(cClause)
        val possibleTypes = mutableSetOf<Type>()
        val concreteType: Type
        if (catchCls.parameter.type is UnionType) {
            for (t in (catchCls.parameter.type as UnionType).elements) {
                possibleTypes.add(frontend.getTypeAsGoodAsPossible(t))
            }
            // we do not know which of the exceptions was actually thrown, so we assume this might
            // be any
            concreteType = this.objectType("java.lang.Throwable")
            concreteType.typeOrigin = Type.Origin.GUESSED
        } else {
            concreteType = frontend.getTypeAsGoodAsPossible(catchCls.parameter.type)
            possibleTypes.add(concreteType)
        }
        val parameter =
            this.newVariableDeclaration(
                catchCls.parameter.name.toString(),
                concreteType,
                rawNode = catchCls.parameter,
            )
        parameter.addAssignedTypes(possibleTypes)
        val body = handleBlockStatement(catchCls.body)
        cClause.body = body
        cClause.parameter = parameter
        declareSymbol(parameter)
        leaveScope(cClause)
        return cClause
    }

    companion object {
        private val log = LoggerFactory.getLogger(StatementHandler::class.java)
    }

    init {
        map[com.github.javaparser.ast.stmt.IfStmt::class.java] =
            HandlerInterface { stmt: Statement ->
                handleIfStatement(stmt)
            }
        map[com.github.javaparser.ast.stmt.AssertStmt::class.java] =
            HandlerInterface { stmt: Statement ->
                handleAssertStatement(stmt)
            }
        map[com.github.javaparser.ast.stmt.WhileStmt::class.java] =
            HandlerInterface { stmt: Statement ->
                handleWhileStatement(stmt)
            }
        map[com.github.javaparser.ast.stmt.DoStmt::class.java] =
            HandlerInterface { stmt: Statement ->
                handleDoStatement(stmt)
            }
        map[com.github.javaparser.ast.stmt.ForEachStmt::class.java] =
            HandlerInterface { stmt: Statement ->
                handleForEachStatement(stmt)
            }
        map[com.github.javaparser.ast.stmt.ForStmt::class.java] =
            HandlerInterface { stmt: Statement ->
                handleForStatement(stmt)
            }
        map[com.github.javaparser.ast.stmt.BreakStmt::class.java] =
            HandlerInterface { stmt: Statement ->
                handleBreakStatement(stmt)
            }
        map[com.github.javaparser.ast.stmt.ContinueStmt::class.java] =
            HandlerInterface { stmt: Statement ->
                handleContinueStatement(stmt)
            }
        map[com.github.javaparser.ast.stmt.ReturnStmt::class.java] =
            HandlerInterface { stmt: Statement ->
                handleReturnStatement(stmt)
            }
        map[BlockStmt::class.java] = HandlerInterface { stmt: Statement ->
            handleBlockStatement(stmt)
        }
        map[LabeledStmt::class.java] = HandlerInterface { stmt: Statement ->
            handleLabelStatement(stmt)
        }
        map[ExplicitConstructorInvocationStmt::class.java] = HandlerInterface { stmt: Statement ->
            handleExplicitConstructorInvocation(stmt)
        }
        map[ExpressionStmt::class.java] = HandlerInterface { stmt: Statement ->
            handleExpressionStatement(stmt)
        }
        map[com.github.javaparser.ast.stmt.SwitchStmt::class.java] =
            HandlerInterface { stmt: Statement ->
                handleSwitchStatement(stmt)
            }
        map[com.github.javaparser.ast.stmt.EmptyStmt::class.java] =
            HandlerInterface { stmt: Statement ->
                handleEmptyStatement(stmt)
            }
        map[com.github.javaparser.ast.stmt.SynchronizedStmt::class.java] =
            HandlerInterface { stmt: Statement ->
                handleSynchronizedStatement(stmt)
            }
        map[com.github.javaparser.ast.stmt.TryStmt::class.java] =
            HandlerInterface { stmt: Statement ->
                handleTryStatement(stmt)
            }
        map[ThrowStmt::class.java] = HandlerInterface { stmt: Statement -> handleThrowStmt(stmt) }
    }
}
