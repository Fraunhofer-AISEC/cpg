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

    /**
     * Handles an assignment statement. We need to explicitly set the operator code on this
     * assignment as something which potentially declares a variable, so we can resolve this in our
     * extra pass.
     */
    private fun handleAssignStmt(assignStmt: GoStandardLibrary.Ast.AssignStmt) =
        newAssignExpression(
                operatorCode =
                    if (assignStmt.tok == 47) {
                        ":="
                    } else {
                        "="
                    },
                rawNode = assignStmt
            )
            .withChildren { expr ->
                expr.rhs = frontend.expressionHandler.handle(assignStmt.rhs)
                expr.lhs = frontend.expressionHandler.handle(assignStmt.lhs)
            }

    private fun handleBranchStmt(branchStmt: GoStandardLibrary.Ast.BranchStmt) =
        when (branchStmt.tokString) {
            "break" -> {
                newBreakStatement(rawNode = branchStmt).withChildren { stmt ->
                    branchStmt.label?.let { stmt.label = it.name }
                }
            }
            "continue" -> {
                newContinueStatement(rawNode = branchStmt).withChildren { stmt ->
                    branchStmt.label?.let { stmt.label = it.name }
                }
            }
            "goto" -> {
                newGotoStatement(rawNode = branchStmt).withChildren { stmt ->
                    branchStmt.label?.let { stmt.labelName = it.name }
                }
            }
            else ->
                newProblemExpression(
                    "unknown token \"${branchStmt.tokString}\" in branch statement"
                )
        }

    private fun handleBlockStmt(blockStmt: GoStandardLibrary.Ast.BlockStmt) =
        newBlock(rawNode = blockStmt).withChildren(hasScope = true) {
            for (stmt in blockStmt.list) {
                val node = handle(stmt)
                // Do not add case statements to the block because the already add themselves in
                // handleCaseClause. Otherwise, the order of case's would be wrong
                if (node !is CaseStatement) {
                    it += node
                }
            }
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
        if (isTypeSwitch && typeSwitchRhs != null && typeSwitchLhs != null) {
            val stmt = newDeclarationStatement()
            stmt.isImplicit = true

            val decl =
                newVariableDeclaration(typeSwitchLhs.name).withChildren {
                    if (case is CaseStatement) {
                        it.type = (case.caseExpression as? TypeExpression)?.type ?: unknownType()
                    } else {
                        // We need to work with type listeners here because they might not have
                        // their type
                        // yet
                        typeSwitchRhs.registerTypeObserver(
                            object : HasType.TypeObserver {
                                override fun typeChanged(newType: Type, src: HasType) {
                                    it.type = newType
                                }

                                override fun assignedTypeChanged(
                                    assignedTypes: Set<Type>,
                                    src: HasType
                                ) {
                                    // Nothing to do
                                }
                            }
                        )
                    }
                    it.initializer = typeSwitchRhs
                }

            // Add the variable to the declaration statement as well as to the current scope (aka
            // our block wrapper)
            stmt.addToPropertyEdgeDeclaration(decl)
            decl.declare()

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
        return newDeclarationStatement(rawNode = declStmt).withChildren { stmt ->
            val sequence = frontend.declarationHandler.handle(declStmt.decl)
            if (sequence is DeclarationSequence) {
                for (declaration in sequence.declarations) {
                    frontend.scopeManager.addDeclaration(declaration)
                }
                stmt.declarations = sequence.asList()
            } else {
                frontend.scopeManager.addDeclaration(sequence)
                stmt.singleDeclaration = sequence
            }
        }
    }

    /**
     * // handleDeferStmt handles the `defer` statement, which is a special keyword in go // that
     * the supplied callee is executed once the function it is called in exists. // We cannot model
     * this 1:1, so we basically we create a call expression to a built-in call. // We adjust the
     * EOG of the call later in an extra pass.
     */
    private fun handleDeferStmt(deferStmt: GoStandardLibrary.Ast.DeferStmt) =
        newUnaryOperator("defer", postfix = false, prefix = true, rawNode = deferStmt)
            .withChildren { op -> op.input = frontend.expressionHandler.handle(deferStmt.call) }

    /**
     * This function handles the `go` statement, which is a special keyword in go that starts the
     * supplied call expression in a separate Go routine. We cannot model this 1:1, so we basically
     * we create a call expression to a built-in call.
     */
    private fun handleGoStmt(goStmt: GoStandardLibrary.Ast.GoStmt): CallExpression {
        // TODO: this will not set the ast parent of the callee correctly
        val ref = newReference("go")
        return newCallExpression(ref, "go", rawNode = goStmt).withChildren {
            it += frontend.expressionHandler.handle(goStmt.call)
        }
    }

    private fun handleForStmt(forStmt: GoStandardLibrary.Ast.ForStmt) =
        newForStatement(rawNode = forStmt).withChildren(hasScope = true) { stmt ->
            forStmt.init?.let { stmt.initializerStatement = handle(it) }
            forStmt.cond?.let { stmt.condition = frontend.expressionHandler.handle(it) }
            forStmt.post?.let { stmt.iterationStatement = handle(it) }
            forStmt.body?.let { stmt.statement = handle(it) }
        }

    private fun handleIncDecStmt(incDecStmt: GoStandardLibrary.Ast.IncDecStmt) =
        newUnaryOperator(incDecStmt.tokString, postfix = true, prefix = false, rawNode = incDecStmt)
            .withChildren { op -> op.input = frontend.expressionHandler.handle(incDecStmt.x) }

    private fun handleIfStmt(ifStmt: GoStandardLibrary.Ast.IfStmt) =
        newIfStatement(rawNode = ifStmt).withChildren(hasScope = true) { stmt ->
            ifStmt.init?.let { stmt.initializerStatement = frontend.statementHandler.handle(it) }

            stmt.condition = frontend.expressionHandler.handle(ifStmt.cond)
            stmt.thenStatement = frontend.statementHandler.handle(ifStmt.body)

            ifStmt.`else`?.let { stmt.elseStatement = frontend.statementHandler.handle(it) }
        }

    private fun handleLabeledStmt(labeledStmt: GoStandardLibrary.Ast.LabeledStmt) =
        newLabelStatement(rawNode = labeledStmt).withChildren { stmt ->
            stmt.subStatement = handle(labeledStmt.stmt)
            stmt.label = labeledStmt.label.name
        }

    private fun handleRangeStmt(rangeStmt: GoStandardLibrary.Ast.RangeStmt) =
        newForEachStatement(rawNode = rangeStmt).withChildren(hasScope = true) { forEach ->
            // TODO: Support other use cases that do not use DEFINE
            if (rangeStmt.tokString == ":=") {
                forEach.variable =
                    newDeclarationStatement().withChildren { stmt ->
                        // TODO: not really the best way to deal with this
                        // TODO: key type is always int. we could set this
                        rangeStmt.key?.let {
                            val ref = frontend.expressionHandler.handle(it)
                            if (ref is Reference) {
                                stmt += newVariableDeclaration(ref.name, rawNode = it).declare()
                            }
                        }

                        // TODO: not really the best way to deal with this
                        rangeStmt.value?.let {
                            val ref = frontend.expressionHandler.handle(it)
                            if (ref is Reference) {
                                stmt += newVariableDeclaration(ref.name, rawNode = it).declare()
                            }
                        }
                    }
            }

            forEach.iterable = frontend.expressionHandler.handle(rangeStmt.x)
            forEach.statement = frontend.statementHandler.handle(rangeStmt.body)
        }

    private fun handleReturnStmt(returnStmt: GoStandardLibrary.Ast.ReturnStmt) =
        newReturnStatement(rawNode = returnStmt).withChildren { `return` ->
            val results = returnStmt.results
            if (results.isNotEmpty()) {
                val expr = frontend.expressionHandler.handle(results[0])

                // TODO: parse more than one result expression
                `return`.returnValue = expr
            } else {
                // TODO: connect result statement to result variables
            }
        }

    private fun handleSendStmt(sendStmt: GoStandardLibrary.Ast.SendStmt) =
        newBinaryOperator("<-", rawNode = sendStmt).withChildren { op ->
            op.lhs = frontend.expressionHandler.handle(sendStmt.chan)
            op.rhs = frontend.expressionHandler.handle(sendStmt.value)
        }

    private fun handleSwitchStmt(switchStmt: GoStandardLibrary.Ast.SwitchStmt) =
        newSwitchStatement(rawNode = switchStmt).withChildren(hasScope = true) { switch ->
            switchStmt.init?.let { switch.initializerStatement = handle(it) }
            switchStmt.tag?.let { switch.selector = frontend.expressionHandler.handle(it) }

            switch.statement =
                handle(switchStmt.body) as? Block ?: newProblemExpression("missing switch body")
        }

    private fun handleTypeSwitchStmt(typeSwitchStmt: GoStandardLibrary.Ast.TypeSwitchStmt) =
        newSwitchStatement(rawNode = typeSwitchStmt).withChildren(hasScope = true) { switch ->
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

            switch.statement =
                newBlock(rawNode = typeSwitchStmt.body).withChildren(hasScope = true) {
                    for (c in
                        typeSwitchStmt.body.list.filterIsInstance<
                            GoStandardLibrary.Ast.CaseClause
                        >()) {
                        handleCaseClause(c, lhs, rhs)
                    }
                }
        }
}
