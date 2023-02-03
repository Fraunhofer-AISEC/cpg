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
import com.github.javaparser.ast.expr.SimpleName
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
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ExplicitConstructorInvocation
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ProblemExpression
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.util.function.Supplier
import java.util.stream.Collectors
import kotlin.collections.ArrayList
import kotlin.collections.MutableList
import kotlin.collections.mapNotNull
import kotlin.collections.set
import org.slf4j.LoggerFactory

class StatementHandler(lang: JavaLanguageFrontend?) :
    Handler<de.fraunhofer.aisec.cpg.graph.statements.Statement, Statement, JavaLanguageFrontend>(
        Supplier { ProblemExpression() },
        lang!!
    ) {
    fun handleExpressionStatement(
        stmt: Statement
    ): de.fraunhofer.aisec.cpg.graph.statements.Statement? {
        val expression = frontend.expressionHandler.handle(stmt.asExpressionStmt().expression)

        // update expression's code and location to match the statement
        frontend.setCodeAndLocation(expression, stmt)
        return expression
    }

    private fun handleThrowStmt(
        stmt: Statement
    ): de.fraunhofer.aisec.cpg.graph.statements.Statement {
        val throwStmt = stmt as ThrowStmt
        val throwOperation =
            this.newUnaryOperator(
                "throw",
                postfix = false,
                prefix = true,
                code = throwStmt.toString()
            )
        throwOperation.input =
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
        val returnStatement = this.newReturnStatement(returnStmt.toString())

        // expressionRefersToDeclaration to arguments, if there are any
        expression?.let { returnStatement.returnValue = it }

        frontend.setCodeAndLocation(returnStatement, stmt)
        return returnStatement
    }

    private fun handleIfStatement(stmt: Statement): IfStatement {
        val ifStmt = stmt.asIfStmt()
        val conditionExpression = ifStmt.condition
        val thenStatement = ifStmt.thenStmt
        val optionalElseStatement = ifStmt.elseStmt
        val ifStatement = this.newIfStatement(ifStmt.toString())
        frontend.scopeManager.enterScope(ifStatement)
        ifStatement.thenStatement = handle(thenStatement)
        ifStatement.condition =
            frontend.expressionHandler.handle(conditionExpression)
                as de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
        optionalElseStatement.ifPresent { ifStatement.elseStatement = handle(it) }
        frontend.scopeManager.leaveScope(ifStatement)
        return ifStatement
    }

    private fun handleAssertStatement(stmt: Statement): AssertStatement {
        val assertStmt = stmt.asAssertStmt()
        val conditionExpression = assertStmt.check
        val thenStatement = assertStmt.message
        val assertStatement = this.newAssertStatement(stmt.toString())
        assertStatement.condition =
            frontend.expressionHandler.handle(conditionExpression)
                as de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
        thenStatement.ifPresent { statement: Expression? ->
            assertStatement.message = frontend.expressionHandler.handle(thenStatement.get())
        }
        return assertStatement
    }

    private fun handleWhileStatement(stmt: Statement): WhileStatement {
        val whileStmt = stmt.asWhileStmt()
        val conditionExpression = whileStmt.condition
        val statement = whileStmt.body
        val whileStatement = this.newWhileStatement(whileStmt.toString())
        frontend.scopeManager.enterScope(whileStatement)
        whileStatement.statement = handle(statement)
        whileStatement.condition =
            frontend.expressionHandler.handle(conditionExpression)
                as de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
        frontend.scopeManager.leaveScope(whileStatement)
        return whileStatement
    }

    private fun handleForEachStatement(stmt: Statement): ForEachStatement {
        val statement = this.newForEachStatement(stmt.toString())
        frontend.scopeManager.enterScope(statement)
        val forEachStmt = stmt.asForEachStmt()
        val variable = frontend.expressionHandler.handle(forEachStmt.variable)
        val iterable = frontend.expressionHandler.handle(forEachStmt.iterable)
        if (variable !is DeclarationStatement) {
            log.error("Expected a DeclarationStatement but received: {}", variable!!.name)
        } else {
            statement.variable = variable
        }
        statement.iterable = iterable
        statement.statement = handle(forEachStmt.body)
        frontend.scopeManager.leaveScope(statement)
        return statement
    }

    private fun handleForStatement(stmt: Statement): ForStatement {
        val forStmt = stmt.asForStmt()
        val code: String
        val tokenRange = forStmt.tokenRange
        code =
            if (tokenRange.isPresent) {
                tokenRange.get().toString()
            } else {
                stmt.toString()
            }
        val statement = this.newForStatement(code)
        frontend.setCodeAndLocation(statement, stmt)
        frontend.scopeManager.enterScope(statement)
        if (forStmt.initialization.size > 1) {
            var ofExprList: PhysicalLocation? = null

            // code will be set later
            val initExprList = this.newExpressionList()
            for (initExpr in forStmt.initialization) {
                val s = frontend.expressionHandler.handle(initExpr)

                // make sure location is set
                frontend.setCodeAndLocation(s, initExpr)
                s?.let { initExprList.addExpression(it) }

                // can not update location
                if (s!!.location == null) {
                    continue
                }
                if (ofExprList == null) {
                    ofExprList = s.location
                }
                ofExprList!!.region = frontend.mergeRegions(ofExprList.region, s.location!!.region)
            }

            // set code and location of init list
            if (statement.location != null && ofExprList != null) {
                val initCode =
                    frontend.getCodeOfSubregion(
                        statement,
                        statement.location!!.region,
                        ofExprList.region
                    )
                initExprList.location = ofExprList
                initExprList.code = initCode
            }
            statement.initializerStatement = initExprList
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
        // and in
        // cpp StatementHandler
        if (statement.condition == null) {
            val literal: Literal<*> = this.newLiteral(true, this.parseType("boolean"), "true")
            statement.condition = literal
        }
        if (forStmt.update.size > 1) {
            var ofExprList = statement.location

            // code will be set later
            val iterationExprList = this.newExpressionList()
            for (updateExpr in forStmt.update) {
                val s = frontend.expressionHandler.handle(updateExpr)

                // make sure location is set
                frontend.setCodeAndLocation(s, updateExpr)
                s?.let { iterationExprList.addExpression(it) }

                // can not update location
                if (s!!.location == null) {
                    continue
                }
                if (ofExprList == null) {
                    ofExprList = s.location
                }
                ofExprList!!.region = frontend.mergeRegions(ofExprList.region, s.location!!.region)
            }

            // set code and location of init list
            if (statement.location != null && ofExprList != null) {
                val updateCode =
                    frontend.getCodeOfSubregion(
                        statement,
                        statement.location!!.region,
                        ofExprList.region
                    )
                iterationExprList.location = ofExprList
                iterationExprList.code = updateCode
            }
            statement.iterationStatement = iterationExprList
        } else if (forStmt.update.size == 1) {
            statement.iterationStatement = frontend.expressionHandler.handle(forStmt.update[0])
        }
        statement.statement = handle(forStmt.body)
        frontend.scopeManager.leaveScope(statement)
        return statement
    }

    private fun handleDoStatement(stmt: Statement): DoStatement {
        val doStmt = stmt.asDoStmt()
        val conditionExpression = doStmt.condition
        val statement = doStmt.body
        val doStatement = this.newDoStatement(doStmt.toString())
        frontend.scopeManager.enterScope(doStatement)
        doStatement.statement = handle(statement)
        doStatement.condition =
            frontend.expressionHandler.handle(conditionExpression)
                as de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
        frontend.scopeManager.leaveScope(doStatement)
        return doStatement
    }

    private fun handleEmptyStatement(stmt: Statement): EmptyStatement {
        val emptyStmt = stmt.asEmptyStmt()
        return this.newEmptyStatement(emptyStmt.toString())
    }

    private fun handleSynchronizedStatement(stmt: Statement): SynchronizedStatement {
        val synchronizedJava = stmt.asSynchronizedStmt()
        val synchronizedCPG = this.newSynchronizedStatement(stmt.toString())
        synchronizedCPG.expression =
            frontend.expressionHandler.handle(synchronizedJava.expression)
                as de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
        synchronizedCPG.blockStatement = handle(synchronizedJava.body) as CompoundStatement?
        return synchronizedCPG
    }

    private fun handleLabelStatement(stmt: Statement): LabelStatement {
        val labelStmt = stmt.asLabeledStmt()
        val label = labelStmt.label.identifier
        val statement = labelStmt.statement
        val labelStatement = this.newLabelStatement(labelStmt.toString())
        labelStatement.subStatement = handle(statement)
        labelStatement.label = label
        return labelStatement
    }

    private fun handleBreakStatement(stmt: Statement): BreakStatement {
        val breakStmt = stmt.asBreakStmt()
        val breakStatement = this.newBreakStatement()
        breakStmt.label.ifPresent { label: SimpleName -> breakStatement.label = label.toString() }
        return breakStatement
    }

    private fun handleContinueStatement(stmt: Statement): ContinueStatement {
        val continueStmt = stmt.asContinueStmt()
        val continueStatement = this.newContinueStatement()
        continueStmt.label.ifPresent { label: SimpleName ->
            continueStatement.label = label.toString()
        }
        return continueStatement
    }

    fun handleBlockStatement(stmt: Statement): CompoundStatement {
        val blockStmt = stmt.asBlockStmt()

        // first of, all we need a compound statement
        val compoundStatement = this.newCompoundStatement(stmt.toString())
        frontend.scopeManager.enterScope(compoundStatement)
        for (child in blockStmt.statements) {
            val statement = handle(child)
            compoundStatement.addStatement(statement!!)
        }
        frontend.setCodeAndLocation(compoundStatement, stmt)
        frontend.scopeManager.leaveScope(compoundStatement)
        return compoundStatement
    }

    fun handleCaseDefaultStatement(
        caseExpression: Expression?,
        sEntry: SwitchEntry
    ): de.fraunhofer.aisec.cpg.graph.statements.Statement {
        val parentLocation = frontend.getLocationFromRawNode(sEntry)
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
                        getNextTokenWith(":", optionalTokenRange.get().begin)
                    )
            }
            val defaultStatement =
                this.newDefaultStatement(getCodeBetweenTokens(caseTokens.a, caseTokens.b))
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
                    getNextTokenWith(":", caseExprTokenRange.get().end)
                )
        }
        val caseStatement = this.newCaseStatement(getCodeBetweenTokens(caseTokens.a, caseTokens.b))
        caseStatement.caseExpression =
            frontend.expressionHandler.handle(caseExpression)
                as de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
        caseStatement.location = getLocationsFromTokens(parentLocation, caseTokens.a, caseTokens.b)
        return caseStatement
    }

    fun getPreviousTokenWith(text: String, token: JavaToken): JavaToken {
        var token = token
        var optional = token.previousToken
        while (token.text != text && optional.isPresent) {
            token = optional.get()
            optional = token.previousToken
        }
        return token
    }

    fun getNextTokenWith(text: String, token: JavaToken): JavaToken {
        var token = token
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
        endToken: JavaToken?
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
                        rend.end.column + 1
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
            current = current!!.nextToken.orElse(null)
            if (current == null) {
                break
            }
            newCode.append(current.text)
        } while (current !== endToken)
        return newCode.toString()
    }

    fun handleSwitchStatement(stmt: Statement): SwitchStatement {
        val switchStmt = stmt.asSwitchStmt()
        val switchStatement = this.newSwitchStatement(stmt.toString())

        // make sure location is set
        frontend.setCodeAndLocation(switchStatement, switchStmt)
        frontend.scopeManager.enterScope(switchStatement)
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
        val compoundStatement = this.newCompoundStatement(getCodeBetweenTokens(start, end))
        compoundStatement.location = getLocationsFromTokens(switchStatement.location, start, end)
        for (sentry in switchStmt.entries) {
            if (sentry.labels.isEmpty()) {
                compoundStatement.addStatement(handleCaseDefaultStatement(null, sentry))
            }
            for (caseExp in sentry.labels) {
                compoundStatement.addStatement(handleCaseDefaultStatement(caseExp, sentry))
            }
            for (subStmt in sentry.statements) {
                compoundStatement.addStatement(handle(subStmt)!!)
            }
        }
        switchStatement.statement = compoundStatement
        frontend.scopeManager.leaveScope(switchStatement)
        return switchStatement
    }

    private fun handleExplicitConstructorInvocation(
        stmt: Statement
    ): ExplicitConstructorInvocation {
        val eciStatement = stmt.asExplicitConstructorInvocationStmt()
        var containingClass = ""
        val currentRecord = frontend.scopeManager.currentRecord
        if (currentRecord == null) {
            log.error(
                "Explicit constructor invocation has to be located inside a record declaration!"
            )
        } else {
            containingClass = currentRecord.name.toString()
        }
        val node = this.newExplicitConstructorInvocation(containingClass, eciStatement.toString())
        val arguments =
            eciStatement.arguments
                .stream()
                .map { ctx: Expression -> frontend.expressionHandler.handle(ctx) }
                .map { obj: de.fraunhofer.aisec.cpg.graph.statements.Statement? ->
                    de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression::class
                        .java
                        .cast(obj)
                }
                .collect(Collectors.toList())
        node.arguments = arguments
        return node
    }

    private fun handleTryStatement(stmt: Statement): TryStatement {
        val tryStmt = stmt.asTryStmt()
        val tryStatement = this.newTryStatement(stmt.toString())
        frontend.scopeManager.enterScope(tryStatement)
        val resources =
            tryStmt.resources.mapNotNull { ctx: Expression ->
                frontend.expressionHandler.handle(ctx)
            }
        val tryBlock = handleBlockStatement(tryStmt.tryBlock)
        val catchClauses =
            tryStmt.catchClauses
                .stream()
                .map { catchCls: CatchClause -> handleCatchClause(catchCls) }
                .collect(Collectors.toList())
        val finallyBlock =
            tryStmt.finallyBlock.map { stmt: BlockStmt -> handleBlockStatement(stmt) }.orElse(null)
        frontend.scopeManager.leaveScope(tryStatement)
        tryStatement.resources = resources
        tryStatement.tryBlock = tryBlock
        tryStatement.finallyBlock = finallyBlock
        tryStatement.catchClauses = catchClauses
        for (r in resources) {
            if (r is DeclarationStatement) {
                for (d in r.declarations) {
                    if (d is VariableDeclaration) {
                        frontend.scopeManager.addDeclaration(d)
                    }
                }
            }
        }
        return tryStatement
    }

    private fun handleCatchClause(
        catchCls: CatchClause
    ): de.fraunhofer.aisec.cpg.graph.statements.CatchClause {
        val cClause = this.newCatchClause(catchCls.toString())
        frontend.scopeManager.enterScope(cClause)
        val possibleTypes: MutableList<Type> = ArrayList()
        val concreteType: Type
        if (catchCls.parameter.type is UnionType) {
            for (t in (catchCls.parameter.type as UnionType).elements) {
                possibleTypes.add(frontend.getTypeAsGoodAsPossible(t))
            }
            // we do not know which of the exceptions was actually thrown, so we assume this might
            // be any
            concreteType = this.parseType("java.lang.Throwable")
            concreteType.typeOrigin = Type.Origin.GUESSED
        } else {
            concreteType = frontend.getTypeAsGoodAsPossible(catchCls.parameter.type)
            possibleTypes.add(concreteType)
        }
        val parameter =
            this.newVariableDeclaration(
                catchCls.parameter.name.toString(),
                concreteType,
                catchCls.parameter.toString(),
                false
            )
        parameter.possibleSubTypes = possibleTypes
        val body = handleBlockStatement(catchCls.body)
        cClause.body = body
        cClause.parameter = parameter
        frontend.scopeManager.addDeclaration(parameter)
        frontend.scopeManager.leaveScope(cClause)
        return cClause
    }

    companion object {
        private val log = LoggerFactory.getLogger(StatementHandler::class.java)
    }

    init {
        map[IfStmt::class.java] = HandlerInterface { stmt: Statement -> handleIfStatement(stmt) }
        map[AssertStmt::class.java] =
            HandlerInterface<de.fraunhofer.aisec.cpg.graph.statements.Statement, Statement> {
                stmt: Statement ->
                handleAssertStatement(stmt)
            }
        map[WhileStmt::class.java] =
            HandlerInterface<de.fraunhofer.aisec.cpg.graph.statements.Statement, Statement> {
                stmt: Statement ->
                handleWhileStatement(stmt)
            }
        map[DoStmt::class.java] =
            HandlerInterface<de.fraunhofer.aisec.cpg.graph.statements.Statement, Statement> {
                stmt: Statement ->
                handleDoStatement(stmt)
            }
        map[ForEachStmt::class.java] =
            HandlerInterface<de.fraunhofer.aisec.cpg.graph.statements.Statement, Statement> {
                stmt: Statement ->
                handleForEachStatement(stmt)
            }
        map[ForStmt::class.java] =
            HandlerInterface<de.fraunhofer.aisec.cpg.graph.statements.Statement, Statement> {
                stmt: Statement ->
                handleForStatement(stmt)
            }
        map[BreakStmt::class.java] =
            HandlerInterface<de.fraunhofer.aisec.cpg.graph.statements.Statement, Statement> {
                stmt: Statement ->
                handleBreakStatement(stmt)
            }
        map[ContinueStmt::class.java] =
            HandlerInterface<de.fraunhofer.aisec.cpg.graph.statements.Statement, Statement> {
                stmt: Statement ->
                handleContinueStatement(stmt)
            }
        map[ReturnStmt::class.java] =
            HandlerInterface<de.fraunhofer.aisec.cpg.graph.statements.Statement, Statement> {
                stmt: Statement ->
                handleReturnStatement(stmt)
            }
        map[BlockStmt::class.java] =
            HandlerInterface<de.fraunhofer.aisec.cpg.graph.statements.Statement, Statement> {
                stmt: Statement ->
                handleBlockStatement(stmt)
            }
        map[LabeledStmt::class.java] =
            HandlerInterface<de.fraunhofer.aisec.cpg.graph.statements.Statement, Statement> {
                stmt: Statement ->
                handleLabelStatement(stmt)
            }
        map[ExplicitConstructorInvocationStmt::class.java] =
            HandlerInterface<de.fraunhofer.aisec.cpg.graph.statements.Statement, Statement> {
                stmt: Statement ->
                handleExplicitConstructorInvocation(stmt)
            }
        map[ExpressionStmt::class.java] =
            HandlerInterface<de.fraunhofer.aisec.cpg.graph.statements.Statement, Statement> {
                stmt: Statement ->
                handleExpressionStatement(stmt)
            }
        map[SwitchStmt::class.java] =
            HandlerInterface<de.fraunhofer.aisec.cpg.graph.statements.Statement, Statement> {
                stmt: Statement ->
                handleSwitchStatement(stmt)
            }
        map[EmptyStmt::class.java] =
            HandlerInterface<de.fraunhofer.aisec.cpg.graph.statements.Statement, Statement> {
                stmt: Statement ->
                handleEmptyStatement(stmt)
            }
        map[SynchronizedStmt::class.java] =
            HandlerInterface<de.fraunhofer.aisec.cpg.graph.statements.Statement, Statement> {
                stmt: Statement ->
                handleSynchronizedStatement(stmt)
            }
        map[TryStmt::class.java] =
            HandlerInterface<de.fraunhofer.aisec.cpg.graph.statements.Statement, Statement> {
                stmt: Statement ->
                handleTryStatement(stmt)
            }
        map[ThrowStmt::class.java] =
            HandlerInterface<de.fraunhofer.aisec.cpg.graph.statements.Statement, Statement> {
                stmt: Statement ->
                handleThrowStmt(stmt)
            }
    }
}
