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
import de.fraunhofer.aisec.cpg.graph.declarations.DeclarationSequence
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.HasType
import de.fraunhofer.aisec.cpg.graph.types.Type

class StatementHandler(frontend: GoLanguageFrontend) :
    GoHandler<Statement, GoStandardLibrary.Ast.Stmt>(::ProblemExpression, frontend) {

    override fun handleNode(node: GoStandardLibrary.Ast.Stmt): Statement {
        return when (node) {
            is GoStandardLibrary.Ast.AssignStmt -> handleAssignStmt(node)
            is GoStandardLibrary.Ast.BranchStmt -> handleBranchStmt(node)
            is GoStandardLibrary.Ast.BlockStmt -> handleBlockStmt(node)
            is GoStandardLibrary.Ast.CaseClause -> handleCaseClause(node)
            is GoStandardLibrary.Ast.DeclStmt -> handleDeclStmt(node)
            is GoStandardLibrary.Ast.DeferStmt -> handleDeferStmt(node)
            is GoStandardLibrary.Ast.ExprStmt -> {
                return frontend.expressionHandler.handle(node.x)
            }
            is GoStandardLibrary.Ast.ForStmt -> handleForStmt(node)
            is GoStandardLibrary.Ast.GoStmt -> handleGoStmt(node)
            is GoStandardLibrary.Ast.IncDecStmt -> handleIncDecStmt(node)
            is GoStandardLibrary.Ast.IfStmt -> handleIfStmt(node)
            is GoStandardLibrary.Ast.LabeledStmt -> handleLabeledStmt(node)
            is GoStandardLibrary.Ast.RangeStmt -> handleRangeStmt(node)
            is GoStandardLibrary.Ast.ReturnStmt -> handleReturnStmt(node)
            is GoStandardLibrary.Ast.SendStmt -> handleSendStmt(node)
            is GoStandardLibrary.Ast.SwitchStmt -> handleSwitchStmt(node)
            is GoStandardLibrary.Ast.TypeSwitchStmt -> handleTypeSwitchStmt(node)
            else -> handleNotSupported(node, node.goType)
        }
    }

    private fun handleAssignStmt(assignStmt: GoStandardLibrary.Ast.AssignStmt): AssignExpression {
        val lhs = assignStmt.lhs.map { frontend.expressionHandler.handle(it) }
        val rhs = assignStmt.rhs.map { frontend.expressionHandler.handle(it) }

        // We need to explicitly set the operator code on this assignment as
        // something which potentially declares a variable, so we can resolve this
        // in our extra pass.
        val operatorCode =
            if (assignStmt.tok == 47) {
                ":="
            } else {
                "="
            }

        return newAssignExpression(operatorCode, lhs, rhs, rawNode = assignStmt)
    }

    private fun handleBranchStmt(branchStmt: GoStandardLibrary.Ast.BranchStmt): Statement {
        when (branchStmt.tokString) {
            "break" -> {
                val stmt = newBreakStatement(rawNode = branchStmt)
                branchStmt.label?.let { stmt.label = it.name }
                return stmt
            }
            "continue" -> {
                val stmt = newContinueStatement(rawNode = branchStmt)
                branchStmt.label?.let { stmt.label = it.name }
                return stmt
            }
            "goto" -> {
                val stmt = newGotoStatement(rawNode = branchStmt)
                branchStmt.label?.let { stmt.labelName = it.name }
                return stmt
            }
        }

        return newProblemExpression("unknown token \"${branchStmt.tokString}\" in branch statement")
    }

    private fun handleBlockStmt(blockStmt: GoStandardLibrary.Ast.BlockStmt): Statement {
        val compound = newBlock(rawNode = blockStmt)

        frontend.scopeManager.enterScope(compound)

        for (stmt in blockStmt.list) {
            val node = handle(stmt)
            // Do not add case statements to the block because the already add themselves in
            // handleCaseClause. Otherwise, the order of case's would be wrong
            if (node !is CaseStatement) {
                compound += node
            }
        }

        frontend.scopeManager.leaveScope(compound)

        return compound
    }

    private fun handleCaseClause(
        caseClause: GoStandardLibrary.Ast.CaseClause,
        typeSwitchLhs: Node? = null,
        typeSwitchRhs: Expression? = null,
    ): Statement {
        val isTypeSwitch = typeSwitchRhs != null

        val case =
            if (caseClause.list.isEmpty()) {
                newDefaultStatement(rawNode = caseClause)
            } else {
                val case = newCaseStatement(rawNode = caseClause)
                if (isTypeSwitch) {
                    // If this case is within a type switch, we want to wrap the case expression in
                    // a TypeExpression
                    val type = frontend.typeOf(caseClause.list[0])
                    case.caseExpression = newTypeExpression(type.name, type)
                } else {
                    case.caseExpression = frontend.expressionHandler.handle(caseClause.list[0])
                }
                case
            }

        // We need to find the current block / scope and add the statements to it
        val currentBlock = frontend.scopeManager.currentBlock

        if (currentBlock == null) {
            log.error("could not find block to add case clauses")
            return newProblemExpression("could not find block to add case clauses")
        }

        // Add the case statement
        currentBlock += case

        // Wrap everything inside the case in a block statement, if this is a type-switch, so that
        // we can re-declare the variable locally in the block.
        val block =
            if (isTypeSwitch) {
                newBlock()
            } else {
                null
            }

        block?.let { frontend.scopeManager.enterScope(it) }

        // TODO(oxisto): This variable is not yet resolvable
        if (isTypeSwitch && typeSwitchLhs != null) {
            val stmt = newDeclarationStatement()
            stmt.isImplicit = true

            val decl = newVariableDeclaration(typeSwitchLhs.name)
            if (case is CaseStatement) {
                decl.type = (case.caseExpression as? TypeExpression)?.type ?: unknownType()
            } else {
                // We need to work with type listeners here because they might not have their type
                // yet
                typeSwitchRhs.registerTypeObserver(
                    object : HasType.TypeObserver {
                        override fun typeChanged(newType: Type, src: HasType) {
                            decl.type = newType
                        }

                        override fun assignedTypeChanged(assignedTypes: Set<Type>, src: HasType) {
                            // Nothing to do
                        }
                    }
                )
            }
            decl.initializer = typeSwitchRhs

            // Add the variable to the declaration statement as well as to the current scope (aka
            // our block wrapper)
            frontend.scopeManager.addDeclaration(decl)
            stmt.declarations += decl

            if (block != null) {
                block += stmt
            }
        }

        for (s in caseClause.body) {
            if (block != null) {
                block += handle(s)
            } else {
                currentBlock += handle(s)
            }
        }

        if (block != null) {
            currentBlock += block
        }

        block?.let { frontend.scopeManager.leaveScope(it) }

        // this is a little trick, to not add the case statement in handleStmt because we added it
        // already. otherwise, the order is screwed up.
        return case
    }

    private fun handleDeclStmt(declStmt: GoStandardLibrary.Ast.DeclStmt): DeclarationStatement {
        // Let's create a variable declaration (wrapped with a declaration stmt) with
        // this, because we define the variable here
        val stmt = newDeclarationStatement(rawNode = declStmt)
        val declaration = frontend.declarationHandler.handle(declStmt.decl)
        if (declaration is DeclarationSequence) {
            for (declaration in declaration.declarations) {
                frontend.scopeManager.addDeclaration(declaration)
            }
            stmt.declarations = declaration.asMutableList()
        } else if (declaration != null) {
            frontend.scopeManager.addDeclaration(declaration)
            stmt.singleDeclaration = declaration
        }

        return stmt
    }

    /**
     * // handleDeferStmt handles the `defer` statement, which is a special keyword in go // that
     * the supplied callee is executed once the function it is called in exists. // We cannot model
     * this 1:1, so we basically we create a call expression to a built-in call. // We adjust the
     * EOG of the call later in an extra pass.
     */
    private fun handleDeferStmt(deferStmt: GoStandardLibrary.Ast.DeferStmt): UnaryOperator {
        val op = newUnaryOperator("defer", postfix = false, prefix = true, rawNode = deferStmt)
        op.input = frontend.expressionHandler.handle(deferStmt.call)
        return op
    }

    /**
     * This function handles the `go` statement, which is a special keyword in go that starts the
     * supplied call expression in a separate Go routine. We cannot model this 1:1, so we basically
     * we create a call expression to a built-in call.
     */
    private fun handleGoStmt(goStmt: GoStandardLibrary.Ast.GoStmt): CallExpression {
        val ref = newReference("go")
        val call = newCallExpression(ref, "go", rawNode = goStmt)
        call += frontend.expressionHandler.handle(goStmt.call)

        return call
    }

    private fun handleForStmt(forStmt: GoStandardLibrary.Ast.ForStmt): ForStatement {
        val stmt = newForStatement(rawNode = forStmt)

        frontend.scopeManager.enterScope(stmt)

        forStmt.init?.let { stmt.initializerStatement = handle(it) }
        forStmt.cond?.let { stmt.condition = frontend.expressionHandler.handle(it) }
        forStmt.post?.let { stmt.iterationStatement = handle(it) }
        forStmt.body?.let { stmt.statement = handle(it) }

        frontend.scopeManager.leaveScope(stmt)

        return stmt
    }

    private fun handleIncDecStmt(incDecStmt: GoStandardLibrary.Ast.IncDecStmt): UnaryOperator {
        val op =
            newUnaryOperator(
                incDecStmt.tokString,
                postfix = true,
                prefix = false,
                rawNode = incDecStmt,
            )
        op.input = frontend.expressionHandler.handle(incDecStmt.x)

        return op
    }

    private fun handleIfStmt(ifStmt: GoStandardLibrary.Ast.IfStmt): IfStatement {
        val stmt = newIfStatement(rawNode = ifStmt)

        frontend.scopeManager.enterScope(stmt)

        ifStmt.init?.let { stmt.initializerStatement = frontend.statementHandler.handle(it) }

        stmt.condition = frontend.expressionHandler.handle(ifStmt.cond)
        stmt.thenStatement = frontend.statementHandler.handle(ifStmt.body)

        ifStmt.`else`?.let { stmt.elseStatement = frontend.statementHandler.handle(it) }

        frontend.scopeManager.leaveScope(stmt)

        return stmt
    }

    private fun handleLabeledStmt(labeledStmt: GoStandardLibrary.Ast.LabeledStmt): LabelStatement {
        val stmt = newLabelStatement(rawNode = labeledStmt)
        stmt.subStatement = handle(labeledStmt.stmt)
        stmt.label = labeledStmt.label.name

        return stmt
    }

    private fun handleRangeStmt(rangeStmt: GoStandardLibrary.Ast.RangeStmt): ForEachStatement {
        val forEach = newForEachStatement(rawNode = rangeStmt)

        frontend.scopeManager.enterScope(forEach)

        // TODO: Support other use cases that do not use DEFINE
        if (rangeStmt.tokString == ":=") {
            val stmt = newDeclarationStatement()

            // TODO: not really the best way to deal with this
            // TODO: key type is always int. we could set this
            rangeStmt.key?.let {
                val ref = frontend.expressionHandler.handle(it)
                if (ref is Reference) {
                    val key = newVariableDeclaration(ref.name, rawNode = it)
                    frontend.scopeManager.addDeclaration(key)
                    stmt.declarationEdges += key
                }
            }

            // TODO: not really the best way to deal with this
            rangeStmt.value?.let {
                val ref = frontend.expressionHandler.handle(it)
                if (ref is Reference) {
                    val key = newVariableDeclaration(ref.name, rawNode = it)
                    frontend.scopeManager.addDeclaration(key)
                    stmt.declarationEdges += key
                }
            }

            forEach.variable = stmt
        }

        forEach.iterable = frontend.expressionHandler.handle(rangeStmt.x)
        forEach.statement = frontend.statementHandler.handle(rangeStmt.body)

        frontend.scopeManager.leaveScope(forEach)

        return forEach
    }

    private fun handleReturnStmt(returnStmt: GoStandardLibrary.Ast.ReturnStmt): ReturnStatement {
        val `return` = newReturnStatement(rawNode = returnStmt)

        val results = returnStmt.results
        if (results.isNotEmpty()) {
            val expr = frontend.expressionHandler.handle(results[0])

            // TODO: parse more than one result expression
            `return`.returnValue = expr
        } else {
            // TODO: connect result statement to result variables
        }

        return `return`
    }

    private fun handleSendStmt(sendStmt: GoStandardLibrary.Ast.SendStmt): BinaryOperator {
        val op = newBinaryOperator("<-", rawNode = sendStmt)
        op.lhs = frontend.expressionHandler.handle(sendStmt.chan)
        op.rhs = frontend.expressionHandler.handle(sendStmt.value)

        return op
    }

    private fun handleSwitchStmt(switchStmt: GoStandardLibrary.Ast.SwitchStmt): Statement {
        val switch = newSwitchStatement(rawNode = switchStmt)

        frontend.scopeManager.enterScope(switch)

        switchStmt.init?.let { switch.initializerStatement = handle(it) }
        switchStmt.tag?.let { switch.selector = frontend.expressionHandler.handle(it) }

        val block =
            handle(switchStmt.body) as? Block ?: return newProblemExpression("missing switch body")

        switch.statement = block

        frontend.scopeManager.leaveScope(switch)

        return switch
    }

    private fun handleTypeSwitchStmt(
        typeSwitchStmt: GoStandardLibrary.Ast.TypeSwitchStmt
    ): SwitchStatement {
        val switch = newSwitchStatement(rawNode = typeSwitchStmt)

        frontend.scopeManager.enterScope(switch)

        typeSwitchStmt.init?.let { switch.initializerStatement = handle(it) }

        val assign = frontend.statementHandler.handle(typeSwitchStmt.assign)
        val (lhs, rhs) =
            if (assign is AssignExpression) {
                val rhs = assign.rhs.singleOrNull()
                switch.selector = rhs
                Pair(assign.lhs.singleOrNull(), (rhs as? UnaryOperator)?.input)
            } else {
                Pair(null, null)
            }

        val body = newBlock(rawNode = typeSwitchStmt.body)

        frontend.scopeManager.enterScope(body)

        for (c in typeSwitchStmt.body.list.filterIsInstance<GoStandardLibrary.Ast.CaseClause>()) {
            handleCaseClause(c, lhs, rhs)
        }

        frontend.scopeManager.leaveScope(body)

        switch.statement = body

        frontend.scopeManager.leaveScope(switch)

        return switch
    }
}
