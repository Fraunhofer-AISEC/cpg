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
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.stmt.*
import com.github.javaparser.ast.stmt.CatchClause
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.type.UnionType
import com.github.javaparser.utils.Pair
import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.frontends.HandlerInterface
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newAssertStatement
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newCaseStatement
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newCatchClause
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newCompoundStatement
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newDefaultStatement
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newDoStatement
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newEmptyStatement
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newExplicitConstructorInvocation
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newExpressionList
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newForEachStatement
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newForStatement
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newIfStatement
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newLabelStatement
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newLiteral
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newReturnStatement
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newSwitchStatement
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newSynchronizedStatement
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newTryStatement
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newUnaryOperator
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newVariableDeclaration
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.newWhileStatement
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ExplicitConstructorInvocation
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ProblemExpression
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.util.function.Supplier
import java.util.stream.Collectors
import org.slf4j.LoggerFactory

open class StatementHandler(lang: JavaLanguageFrontend) :
    Handler<de.fraunhofer.aisec.cpg.graph.statements.Statement, Statement, JavaLanguageFrontend>(
        Supplier { ProblemExpression() },
        lang
    ) {

    open fun handleExpressionStatement(
        stmt: Statement
    ): de.fraunhofer.aisec.cpg.graph.statements.Statement {
        val expression = lang.expressionHandler.handle(stmt.asExpressionStmt().expression)

        // update expression's code and location to match the statement
        lang.setCodeAndRegion(expression, stmt)
        return expression
    }

    private fun handleThrowStmt(
        stmt: Statement
    ): de.fraunhofer.aisec.cpg.graph.statements.Statement {
        val throwStmt = stmt as ThrowStmt
        val throwOperation =
            newUnaryOperator("throw", postfix = false, prefix = true, code = throwStmt.toString())
        throwOperation.input = lang.expressionHandler.handle(throwStmt.expression) as Expression
        return throwOperation
    }

    private fun handleReturnStatement(stmt: Statement): ReturnStatement {
        val returnStmt = stmt.asReturnStmt()
        val optionalExpression = returnStmt.expression
        var expression: Expression? = null
        if (optionalExpression.isPresent) {
            val expr = optionalExpression.get()

            // handle the expression as the first argument
            expression = lang.expressionHandler.handle(expr) as Expression
        }
        val returnStatement = newReturnStatement(returnStmt.toString())

        // expressionRefersToDeclaration to arguments, if there are any
        if (expression != null) {
            returnStatement.returnValue = expression
        }
        lang.setCodeAndRegion(returnStatement, stmt)
        return returnStatement
    }

    private fun handleIfStatement(stmt: Statement): IfStatement {
        val ifStmt = stmt.asIfStmt()
        val conditionExpression = ifStmt.condition
        val thenStatement = ifStmt.thenStmt
        val optionalElseStatement = ifStmt.elseStmt
        val ifStatement = newIfStatement(ifStmt.toString())
        lang.scopeManager.enterScope(ifStatement)
        ifStatement.thenStatement = handle(thenStatement)
        ifStatement.condition = lang.expressionHandler.handle(conditionExpression) as Expression
        optionalElseStatement.ifPresent { statement: Statement? ->
            ifStatement.elseStatement = handle(statement)
        }
        lang.scopeManager.leaveScope(ifStatement)
        return ifStatement
    }

    private fun handleAssertStatement(stmt: Statement): AssertStatement {
        val assertStmt = stmt.asAssertStmt()
        val conditionExpression = assertStmt.check
        val thenStatement = assertStmt.message
        val assertStatement = newAssertStatement(stmt.toString())
        assertStatement.condition = lang.expressionHandler.handle(conditionExpression) as Expression
        thenStatement.ifPresent {
            assertStatement.setMessage(lang.expressionHandler.handle(thenStatement.get()))
        }
        return assertStatement
    }

    private fun handleWhileStatement(stmt: Statement): WhileStatement {
        val whileStmt = stmt.asWhileStmt()
        val conditionExpression = whileStmt.condition
        val statement = whileStmt.body
        val whileStatement = newWhileStatement(whileStmt.toString())
        lang.scopeManager.enterScope(whileStatement)
        whileStatement.statement = handle(statement)
        whileStatement.condition = lang.expressionHandler.handle(conditionExpression) as Expression
        lang.scopeManager.leaveScope(whileStatement)
        return whileStatement
    }

    private fun handleForEachStatement(stmt: Statement): ForEachStatement {
        val statement = newForEachStatement(stmt.toString())
        lang.scopeManager.enterScope(statement)
        val forEachStmt = stmt.asForEachStmt()
        val variable = lang.expressionHandler.handle(forEachStmt.variable)
        val iterable = lang.expressionHandler.handle(forEachStmt.iterable)
        if (variable !is DeclarationStatement) {
            log.error("Expected a DeclarationStatement but received: {}", variable.name)
        } else {
            statement.variable = variable
        }
        statement.iterable = iterable
        statement.statement = handle(forEachStmt.body)
        lang.scopeManager.leaveScope(statement)
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
        val statement = newForStatement(code)
        lang.setCodeAndRegion(statement, stmt)
        lang.scopeManager.enterScope(statement)
        if (forStmt.initialization.size > 1) {
            var ofExprList: PhysicalLocation? = null

            // code will be set later
            val initExprList = newExpressionList(null)
            for (initExpr in forStmt.initialization) {
                val s = lang.expressionHandler.handle(initExpr)

                // make sure location is set
                lang.setCodeAndRegion(s, initExpr)
                initExprList.addExpression(s)

                // can not update location
                if (s.location == null) {
                    continue
                }
                if (ofExprList == null) {
                    ofExprList = s.location
                }
                ofExprList!!.region = lang.mergeRegions(ofExprList.region, s.location!!.region)
            }

            // set code and location of init list
            if (statement.location != null && ofExprList != null) {
                val initCode =
                    lang.getCodeOfSubregion(
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
                lang.expressionHandler.handle(forStmt.initialization[0])
        }
        forStmt.compare.ifPresent { condition: com.github.javaparser.ast.expr.Expression ->
            statement.condition = lang.expressionHandler.handle(condition) as Expression
        }

        // Adds true expression node where default empty condition evaluates to true, remove here
        // and in
        // cpp StatementHandler
        if (statement.condition == null) {
            val literal: Literal<*> =
                newLiteral(true, TypeParser.createFrom("boolean", true), "true")
            statement.condition = literal
        }
        if (forStmt.update.size > 1) {
            var ofExprList = statement.location

            // code will be set later
            val iterationExprList = newExpressionList(null)
            for (updateExpr in forStmt.update) {
                val s = lang.expressionHandler.handle(updateExpr)

                // make sure location is set
                lang.setCodeAndRegion(s, updateExpr)
                iterationExprList.addExpression(s)

                // can not update location
                if (s.location == null) {
                    continue
                }
                if (ofExprList == null) {
                    ofExprList = s.location
                }
                ofExprList!!.region = lang.mergeRegions(ofExprList.region, s.location!!.region)
            }

            // set code and location of init list
            if (statement.location != null && ofExprList != null) {
                val updateCode =
                    lang.getCodeOfSubregion(
                        statement,
                        statement.location!!.region,
                        ofExprList.region
                    )
                iterationExprList.location = ofExprList
                iterationExprList.code = updateCode
            }
            statement.iterationStatement = iterationExprList
        } else if (forStmt.update.size == 1) {
            statement.iterationStatement = lang.expressionHandler.handle(forStmt.update[0])
        }
        statement.statement = handle(forStmt.body)
        lang.scopeManager.leaveScope(statement)
        return statement
    }

    private fun handleDoStatement(stmt: Statement): DoStatement {
        val doStmt = stmt.asDoStmt()
        val conditionExpression = doStmt.condition
        val statement = doStmt.body
        val doStatement = newDoStatement(doStmt.toString())
        lang.scopeManager.enterScope(doStatement)
        doStatement.statement = handle(statement)
        doStatement.condition = lang.expressionHandler.handle(conditionExpression) as Expression
        lang.scopeManager.leaveScope(doStatement)
        return doStatement
    }

    private fun handleEmptyStatement(stmt: Statement): EmptyStatement {
        val emptyStmt = stmt.asEmptyStmt()
        return newEmptyStatement(emptyStmt.toString())
    }

    private fun handleSynchronizedStatement(stmt: Statement): SynchronizedStatement {
        val synchronizedJava = stmt.asSynchronizedStmt()
        val synchronizedCPG = newSynchronizedStatement(stmt.toString())
        synchronizedCPG.setExpression(
            lang.expressionHandler.handle(synchronizedJava.expression) as Expression
        )
        synchronizedCPG.setBlockStatement(handle(synchronizedJava.body) as CompoundStatement?)
        return synchronizedCPG
    }

    private fun handleLabelStatement(stmt: Statement): LabelStatement {
        val labelStmt = stmt.asLabeledStmt()
        val label = labelStmt.label.identifier
        val statement = labelStmt.statement
        val labelStatement = newLabelStatement(labelStmt.toString())
        labelStatement.subStatement = handle(statement)
        labelStatement.label = label
        return labelStatement
    }

    private fun handleBreakStatement(stmt: Statement): BreakStatement {
        val breakStmt = stmt.asBreakStmt()
        val breakStatement = BreakStatement()
        breakStmt.label.ifPresent { label: SimpleName -> breakStatement.label = label.toString() }
        return breakStatement
    }

    private fun handleContinueStatement(stmt: Statement): ContinueStatement {
        val continueStmt = stmt.asContinueStmt()
        val continueStatement = ContinueStatement()
        continueStmt.label.ifPresent { label: SimpleName ->
            continueStatement.label = label.toString()
        }
        return continueStatement
    }

    fun handleBlockStatement(stmt: Statement): CompoundStatement {
        val blockStmt = stmt.asBlockStmt()

        // first of, all we need a compound statement
        val compoundStatement = newCompoundStatement(stmt.toString())
        lang.scopeManager.enterScope(compoundStatement)
        for (child in blockStmt.statements) {
            val statement = handle(child)
            compoundStatement.addStatement(statement)
        }
        lang.setCodeAndRegion(compoundStatement, stmt)
        lang.scopeManager.leaveScope(compoundStatement)
        return compoundStatement
    }

    fun handleCaseDefaultStatement(
        caseExpression: com.github.javaparser.ast.expr.Expression?,
        sEntry: SwitchEntry
    ): de.fraunhofer.aisec.cpg.graph.statements.Statement {
        val parentLocation = lang.getLocationFromRawNode(sEntry)
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
                newDefaultStatement(getCodeBetweenTokens(caseTokens.a, caseTokens.b))
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
        val caseStatement = newCaseStatement(getCodeBetweenTokens(caseTokens.a, caseTokens.b))
        caseStatement.setCaseExpression(lang.expressionHandler.handle(caseExpression) as Expression)
        caseStatement.location = getLocationsFromTokens(parentLocation, caseTokens.a, caseTokens.b)
        return caseStatement
    }

    private fun getPreviousTokenWith(text: String, token: JavaToken): JavaToken {
        var newToken = token
        var optional = newToken.previousToken
        while (newToken.text != text && optional.isPresent) {
            newToken = optional.get()
            optional = newToken.previousToken
        }
        return newToken
    }

    private fun getNextTokenWith(text: String, token: JavaToken): JavaToken {
        var newToken = token
        var optional = newToken.nextToken
        while (newToken.text != text && optional.isPresent) {
            newToken = optional.get()
            optional = newToken.nextToken
        }
        return newToken
    }

    private fun getLocationsFromTokens(
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

    open fun handleSwitchStatement(stmt: Statement): SwitchStatement {
        val switchStmt = stmt.asSwitchStmt()
        val switchStatement = newSwitchStatement(stmt.toString())

        // make sure location is set
        lang.setCodeAndRegion(switchStatement, switchStmt)
        lang.scopeManager.enterScope(switchStatement)
        switchStatement.setSelector(
            lang.expressionHandler.handle(switchStmt.selector) as Expression
        )

        // Compute region and code for self generated compound statement to match the c++ versions
        var start: JavaToken? = null
        var end: JavaToken? = null
        val tokenRange = switchStmt.tokenRange
        val tokenRangeSelector = switchStmt.selector.tokenRange
        if (tokenRange.isPresent && tokenRangeSelector.isPresent) {
            start = getNextTokenWith("{", tokenRangeSelector.get().end)
            end = getPreviousTokenWith("}", tokenRange.get().end)
        }
        val compoundStatement = newCompoundStatement(getCodeBetweenTokens(start, end))
        compoundStatement.location = getLocationsFromTokens(switchStatement.location, start, end)
        for (sentry in switchStmt.entries) {
            if (sentry.labels.isEmpty()) {
                compoundStatement.addStatement(handleCaseDefaultStatement(null, sentry))
            }
            for (caseExp in sentry.labels) {
                compoundStatement.addStatement(handleCaseDefaultStatement(caseExp, sentry))
            }
            for (subStmt in sentry.statements) {
                compoundStatement.addStatement(handle(subStmt))
            }
        }
        switchStatement.statement = compoundStatement
        lang.scopeManager.leaveScope(switchStatement)
        return switchStatement
    }

    private fun handleExplicitConstructorInvocation(
        stmt: Statement
    ): ExplicitConstructorInvocation {
        val eciStatement = stmt.asExplicitConstructorInvocationStmt()
        var containingClass = ""
        val currentRecord = lang.scopeManager.currentRecord
        if (currentRecord == null) {
            log.error(
                "Explicit constructor invocation has to be located inside a record declaration!"
            )
        } else {
            containingClass = currentRecord.name
        }
        val node = newExplicitConstructorInvocation(containingClass, eciStatement.toString())
        val arguments =
            eciStatement
                .arguments
                .stream()
                .map { ctx: com.github.javaparser.ast.expr.Expression ->
                    lang.expressionHandler.handle(ctx)
                }
                .map { obj: de.fraunhofer.aisec.cpg.graph.statements.Statement? ->
                    Expression::class.java.cast(obj)
                }
                .collect(Collectors.toList())
        node.arguments = arguments
        return node
    }

    private fun handleTryStatement(stmt: Statement): TryStatement {
        val tryStmt = stmt.asTryStmt()
        val tryStatement = newTryStatement(stmt.toString())
        lang.scopeManager.enterScope(tryStatement)
        val resources =
            tryStmt
                .resources
                .stream()
                .map { ctx: com.github.javaparser.ast.expr.Expression ->
                    lang.expressionHandler.handle(ctx)
                }
                .collect(Collectors.toList())
        val tryBlock = handleBlockStatement(tryStmt.tryBlock)
        val catchClauses =
            tryStmt
                .catchClauses
                .stream()
                .map { catchCls: CatchClause -> handleCatchClause(catchCls) }
                .collect(Collectors.toList())
        val finallyBlock = tryStmt.finallyBlock.map { handleBlockStatement(it) }.orElse(null)
        lang.scopeManager.leaveScope(tryStatement)
        tryStatement.resources = resources
        tryStatement.tryBlock = tryBlock
        tryStatement.finallyBlock = finallyBlock
        tryStatement.catchClauses = catchClauses
        for (r in resources) {
            if (r is DeclarationStatement) {
                for (d in r.getDeclarations()) {
                    if (d is VariableDeclaration) {
                        lang.scopeManager.addDeclaration(d)
                    }
                }
            }
        }
        return tryStatement
    }

    private fun handleCatchClause(
        catchCls: CatchClause
    ): de.fraunhofer.aisec.cpg.graph.statements.CatchClause {
        val cClause = newCatchClause(catchCls.toString())
        lang.scopeManager.enterScope(cClause)
        val possibleTypes = HashSet<Type>()
        val concreteType: Type
        if (catchCls.parameter.type is UnionType) {
            for (t in (catchCls.parameter.type as UnionType).elements) {
                possibleTypes.add(lang.getTypeAsGoodAsPossible(t))
            }
            // we do not know which of the exceptions was actually thrown, so we assume this might
            // be any
            concreteType = TypeParser.createFrom("java.lang.Throwable", true)
            concreteType.typeOrigin = Type.Origin.GUESSED
        } else {
            concreteType = lang.getTypeAsGoodAsPossible(catchCls.parameter.type)
            possibleTypes.add(concreteType)
        }
        val parameter =
            newVariableDeclaration(
                catchCls.parameter.name.toString(),
                concreteType,
                catchCls.parameter.toString(),
                false
            )
        parameter.possibleSubTypes = possibleTypes
        val body = handleBlockStatement(catchCls.body)
        cClause.body = body
        cClause.setParameter(parameter)
        lang.scopeManager.addDeclaration(parameter)
        lang.scopeManager.leaveScope(cClause)
        return cClause
    }

    companion object {
        private val log = LoggerFactory.getLogger(StatementHandler::class.java)
    }

    init {
        map[IfStmt::class.java] = HandlerInterface { stmt: Statement -> handleIfStatement(stmt) }
        map[AssertStmt::class.java] = HandlerInterface { stmt: Statement ->
            handleAssertStatement(stmt)
        }
        map[WhileStmt::class.java] = HandlerInterface { stmt: Statement ->
            handleWhileStatement(stmt)
        }
        map[DoStmt::class.java] = HandlerInterface { stmt: Statement -> handleDoStatement(stmt) }
        map[ForEachStmt::class.java] = HandlerInterface { stmt: Statement ->
            handleForEachStatement(stmt)
        }
        map[ForStmt::class.java] = HandlerInterface { stmt: Statement -> handleForStatement(stmt) }
        map[BreakStmt::class.java] = HandlerInterface { stmt: Statement ->
            handleBreakStatement(stmt)
        }
        map[ContinueStmt::class.java] = HandlerInterface { stmt: Statement ->
            handleContinueStatement(stmt)
        }
        map[ReturnStmt::class.java] = HandlerInterface { stmt: Statement ->
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
        map[SwitchStmt::class.java] = HandlerInterface { stmt: Statement ->
            handleSwitchStatement(stmt)
        }
        map[EmptyStmt::class.java] = HandlerInterface { stmt: Statement ->
            handleEmptyStatement(stmt)
        }
        map[SynchronizedStmt::class.java] = HandlerInterface { stmt: Statement ->
            handleSynchronizedStatement(stmt)
        }
        map[TryStmt::class.java] = HandlerInterface { stmt: Statement -> handleTryStatement(stmt) }
        map[ThrowStmt::class.java] = HandlerInterface { stmt: Statement -> handleThrowStmt(stmt) }
    }
}
