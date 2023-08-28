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
package de.fraunhofer.aisec.cpg.frontends.golang

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.DeclSequence
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*

class StatementHandler(frontend: GoLanguageFrontend) :
    GoHandler<Statement, GoStandardLibrary.Ast.Stmt>(::ProblemExpr, frontend) {

    override fun handleNode(stmt: GoStandardLibrary.Ast.Stmt): Statement {
        return when (stmt) {
            is GoStandardLibrary.Ast.AssignStmt -> handleAssignStmt(stmt)
            is GoStandardLibrary.Ast.BranchStmt -> handleBranchStmt(stmt)
            is GoStandardLibrary.Ast.BlockStmt -> handleBlockStmt(stmt)
            is GoStandardLibrary.Ast.CaseClause -> handleCaseClause(stmt)
            is GoStandardLibrary.Ast.DeclStmt -> handleDeclStmt(stmt)
            is GoStandardLibrary.Ast.DeferStmt -> handleDeferStmt(stmt)
            is GoStandardLibrary.Ast.ExprStmt -> {
                return frontend.expressionHandler.handle(stmt.x)
            }
            is GoStandardLibrary.Ast.ForStmt -> handleForStmt(stmt)
            is GoStandardLibrary.Ast.GoStmt -> handleGoStmt(stmt)
            is GoStandardLibrary.Ast.IncDecStmt -> handleIncDecStmt(stmt)
            is GoStandardLibrary.Ast.IfStmt -> handleIfStmt(stmt)
            is GoStandardLibrary.Ast.LabeledStmt -> handleLabeledStmt(stmt)
            is GoStandardLibrary.Ast.RangeStmt -> handleRangeStmt(stmt)
            is GoStandardLibrary.Ast.ReturnStmt -> handleReturnStmt(stmt)
            is GoStandardLibrary.Ast.SwitchStmt -> handleSwitchStmt(stmt)
            else -> handleNotSupported(stmt, stmt.goType)
        }
    }

    private fun handleAssignStmt(assignStmt: GoStandardLibrary.Ast.AssignStmt): AssignExpr {
        val lhs = assignStmt.lhs.map { frontend.expressionHandler.handle(it) }
        val rhs = assignStmt.rhs.map { frontend.expressionHandler.handle(it) }

        // We need to explicitly set the operator code on this assignment as
        // something which potentially declares a variable, so we can resolve this
        // in our extra pass.
        val operatorCode =
            if (assignStmt.tok == 47) {
                ":="
            } else {
                ""
            }

        return newAssignExpr(operatorCode, lhs, rhs, rawNode = assignStmt)
    }

    private fun handleBranchStmt(branchStmt: GoStandardLibrary.Ast.BranchStmt): Statement {
        when (branchStmt.tokString) {
            "break" -> {
                val stmt = newBreakStmt(rawNode = branchStmt)
                branchStmt.label?.let { stmt.label = it.name }
                return stmt
            }
            "continue" -> {
                val stmt = newContinueStmt(rawNode = branchStmt)
                branchStmt.label?.let { stmt.label = it.name }
                return stmt
            }
            "goto" -> {
                val stmt = newGotoStmt(rawNode = branchStmt)
                branchStmt.label?.let { stmt.labelName = it.name }
                return stmt
            }
        }

        return newProblemExpr("unknown token \"${branchStmt.tokString}\" in branch statement")
    }

    private fun handleBlockStmt(blockStmt: GoStandardLibrary.Ast.BlockStmt): Statement {
        val compound = newCompoundStmt(rawNode = blockStmt)

        frontend.scopeManager.enterScope(compound)

        for (stmt in blockStmt.list) {
            val node = handle(stmt)
            // Do not add case statements to the block because the already add themselves in
            // handleCaseClause. Otherwise, the order of case's would be wrong
            if (node !is CaseStmt) {
                compound += node
            }
        }

        frontend.scopeManager.leaveScope(compound)

        return compound
    }

    private fun handleCaseClause(caseClause: GoStandardLibrary.Ast.CaseClause): Statement {
        val case =
            if (caseClause.list.isEmpty()) {
                newDefaultStmt(rawNode = caseClause)
            } else {
                val case = newCaseStmt(rawNode = caseClause)
                case.caseExpression = frontend.expressionHandler.handle(caseClause.list[0])
                case
            }

        // We need to find the current block / scope and add the statements to it
        val block = frontend.scopeManager.currentBlock

        if (block == null) {
            log.error("could not find block to add case clauses")
            return newProblemExpr("could not find block to add case clauses")
        }

        // Add the case statement
        block += case

        for (s in caseClause.body) {
            block += handle(s)
        }

        // this is a little trick, to not add the case statement in handleStmt because we added it
        // already. otherwise, the order is screwed up.
        return case
    }

    private fun handleDeclStmt(declStmt: GoStandardLibrary.Ast.DeclStmt): DeclarationStmt {
        // Let's create a variable declaration (wrapped with a declaration stmt) with
        // this, because we define the variable here
        val stmt = newDeclarationStmt(rawNode = declStmt)
        val sequence = frontend.declarationHandler.handle(declStmt.decl)
        if (sequence is DeclSequence) {
            for (declaration in sequence.declarations) {
                frontend.scopeManager.addDeclaration(declaration)
            }
            stmt.declarations = sequence.asList()
        } else {
            frontend.scopeManager.addDeclaration(sequence)
            stmt.singleDeclaration = sequence
        }

        return stmt
    }

    /**
     * // handleDeferStmt handles the `defer` statement, which is a special keyword in go // that
     * the supplied callee is executed once the function it is called in exists. // We cannot model
     * this 1:1, so we basically we create a call expression to a built-in call. // We adjust the
     * EOG of the call later in an extra pass.
     */
    private fun handleDeferStmt(deferStmt: GoStandardLibrary.Ast.DeferStmt): UnaryOp {
        val op = newUnaryOp("defer", postfix = false, prefix = true, rawNode = deferStmt)
        op.input = frontend.expressionHandler.handle(deferStmt.call)
        return op
    }

    /**
     * This function handles the `go` statement, which is a special keyword in go that starts the
     * supplied call expression in a separate Go routine. We cannot model this 1:1, so we basically
     * we create a call expression to a built-in call.
     */
    private fun handleGoStmt(goStmt: GoStandardLibrary.Ast.GoStmt): CallExpr {
        val ref = newReference("go")
        val call = newCallExpr(ref, "go", rawNode = goStmt)
        call += frontend.expressionHandler.handle(goStmt.call)

        return call
    }

    private fun handleForStmt(forStmt: GoStandardLibrary.Ast.ForStmt): ForStmt {
        val stmt = newForStmt(rawNode = forStmt)

        frontend.scopeManager.enterScope(stmt)

        forStmt.init?.let { stmt.initializerStatement = handle(it) }
        forStmt.cond?.let { stmt.condition = frontend.expressionHandler.handle(it) }
        forStmt.post?.let { stmt.iterationStatement = handle(it) }
        forStmt.body?.let { stmt.statement = handle(it) }

        frontend.scopeManager.leaveScope(stmt)

        return stmt
    }

    private fun handleIncDecStmt(incDecStmt: GoStandardLibrary.Ast.IncDecStmt): UnaryOp {
        val op =
            newUnaryOp(incDecStmt.tokString, postfix = true, prefix = false, rawNode = incDecStmt)
        op.input = frontend.expressionHandler.handle(incDecStmt.x)

        return op
    }

    private fun handleIfStmt(ifStmt: GoStandardLibrary.Ast.IfStmt): IfStmt {
        val stmt = newIfStmt(rawNode = ifStmt)

        frontend.scopeManager.enterScope(stmt)

        ifStmt.init?.let { stmt.initializerStatement = frontend.statementHandler.handle(it) }

        stmt.condition = frontend.expressionHandler.handle(ifStmt.cond)
        stmt.thenStatement = frontend.statementHandler.handle(ifStmt.body)

        ifStmt.`else`?.let { stmt.elseStatement = frontend.statementHandler.handle(it) }

        frontend.scopeManager.leaveScope(stmt)

        return stmt
    }

    private fun handleLabeledStmt(labeledStmt: GoStandardLibrary.Ast.LabeledStmt): LabelStmt {
        val stmt = newLabelStmt(rawNode = labeledStmt)
        stmt.subStatement = handle(labeledStmt.stmt)
        stmt.label = labeledStmt.label.name

        return stmt
    }

    private fun handleRangeStmt(rangeStmt: GoStandardLibrary.Ast.RangeStmt): ForEachStmt {
        val forEach = newForEachStmt(rawNode = rangeStmt)

        frontend.scopeManager.enterScope(forEach)

        // TODO: Support other use cases that do not use DEFINE
        if (rangeStmt.tokString == ":=") {
            val stmt = newDeclarationStmt()

            // TODO: not really the best way to deal with this
            // TODO: key type is always int. we could set this
            var ref = rangeStmt.key?.let { frontend.expressionHandler.handle(it) }
            if (ref is Reference) {
                val key = newVariableDecl(ref.name, rawNode = rangeStmt.key)
                frontend.scopeManager.addDeclaration(key)
                stmt.addToPropertyEdgeDeclaration(key)
            }

            // TODO: not really the best way to deal with this
            ref = rangeStmt.value?.let { frontend.expressionHandler.handle(it) }
            if (ref is Reference) {
                val key = newVariableDecl(ref.name, rawNode = rangeStmt.key)
                frontend.scopeManager.addDeclaration(key)
                stmt.addToPropertyEdgeDeclaration(key)
            }

            forEach.variable = stmt
        }

        forEach.iterable = frontend.expressionHandler.handle(rangeStmt.x)
        forEach.statement = frontend.statementHandler.handle(rangeStmt.body)

        frontend.scopeManager.leaveScope(forEach)

        return forEach
    }

    private fun handleReturnStmt(returnStmt: GoStandardLibrary.Ast.ReturnStmt): ReturnStmt {
        val `return` = newReturnStmt(rawNode = returnStmt)

        val results = returnStmt.results
        if (results.isNotEmpty()) {
            val expr = frontend.expressionHandler.handle(results[0])

            // TODO: parse more than one result expression
            if (expr != null) {
                `return`.returnValue = expr
            }
        } else {
            // TODO: connect result statement to result variables
        }

        return `return`
    }

    private fun handleSwitchStmt(switchStmt: GoStandardLibrary.Ast.SwitchStmt): Statement {
        val switch = newSwitchStmt(rawNode = switchStmt)

        frontend.scopeManager.enterScope(switch)

        switchStmt.init?.let { switch.initializerStatement = handle(it) }
        switchStmt.tag?.let { switch.selector = frontend.expressionHandler.handle(it) }

        val block =
            handle(switchStmt.body) as? CompoundStmt ?: return newProblemExpr("missing switch body")

        // Because of the way we parse the statements, the case statement turns out to be the last
        // statement. However, we need it to be the first statement, so we need to switch first and
        // last items
        /*val statements = block.statements.toMutableList()
        val tmp = statements.first()
        statements[0] = block.statements.last()
        statements[(statements.size - 1).coerceAtLeast(0)] = tmp
        block.statements = statements*/

        switch.statement = block

        frontend.scopeManager.leaveScope(switch)

        return switch
    }
}
