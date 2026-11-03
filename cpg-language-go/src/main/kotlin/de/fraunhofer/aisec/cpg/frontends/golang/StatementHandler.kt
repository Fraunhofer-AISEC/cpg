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
import de.fraunhofer.aisec.cpg.graph.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.HasType
import de.fraunhofer.aisec.cpg.graph.types.Type

class StatementHandler(frontend: GoLanguageFrontend) :
    GoHandler<Expression, GoStandardLibrary.Ast.Stmt>(::ProblemExpression, frontend) {

    override fun handleNode(node: GoStandardLibrary.Ast.Stmt): Expression {
        return when (node) {
            is GoStandardLibrary.Ast.AssignStmt -> handleAssignStmt(node)
            is GoStandardLibrary.Ast.BranchStmt -> handleBranchStmt(node)
            is GoStandardLibrary.Ast.BlockStmt -> handleBlockStmt(node)
            is GoStandardLibrary.Ast.CaseClause -> handleCaseClause(node)
            is GoStandardLibrary.Ast.DeclStmt -> handleDeclStmt(node)
            is GoStandardLibrary.Ast.DeferStmt -> handleDeferStmt(node)
            is GoStandardLibrary.Ast.ExprStmt -> {
                frontend.expressionHandler.handle(node.x)
            }
            is GoStandardLibrary.Ast.ForStmt -> handleForStmt(node)
            is GoStandardLibrary.Ast.GoStmt -> handleGoStmt(node)
            is GoStandardLibrary.Ast.IncDecStmt -> handleIncDecStmt(node)
            is GoStandardLibrary.Ast.IfStmt -> handleIfStmt(node)
            is GoStandardLibrary.Ast.LabeledStmt -> handleLabeledStmt(node)
            is GoStandardLibrary.Ast.RangeStmt -> handleRangeStmt(node)
            is GoStandardLibrary.Ast.ReturnStmt -> handleReturnStmt(node)
            is GoStandardLibrary.Ast.SendStmt -> handleSendStmt(node)
            is GoStandardLibrary.Ast.SelectStmt -> handleSelectStmt(node)
            is GoStandardLibrary.Ast.CommClause -> handleCommClause(node)
            is GoStandardLibrary.Ast.SwitchStmt -> handleSwitchStmt(node)
            is GoStandardLibrary.Ast.TypeSwitchStmt -> handleTypeSwitchStmt(node)
            else -> handleNotSupported(node, node.goType)
        }
    }

    private fun handleAssignStmt(assignStmt: GoStandardLibrary.Ast.AssignStmt): Assign {
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

        return newAssign(operatorCode, lhs, rhs, rawNode = assignStmt)
    }

    private fun handleBranchStmt(branchStmt: GoStandardLibrary.Ast.BranchStmt): Expression {
        when (branchStmt.tokString) {
            "break" -> {
                return newBreak(rawNode = branchStmt) { stmt ->
                    branchStmt.label?.let { stmt.label = it.name }
                }
            }
            "continue" -> {
                return newContinue(rawNode = branchStmt) { stmt ->
                    branchStmt.label?.let { stmt.label = it.name }
                }
            }
            "goto" -> {
                return newGoto(rawNode = branchStmt) { stmt ->
                    branchStmt.label?.let { stmt.labelName = it.name }
                }
            }
            "fallthrough" -> {
                // It continues with the next case which is the default behavior in the EOG.
                return newEmpty(rawNode = branchStmt)
            }
        }

        return newProblemExpression("unknown token \"${branchStmt.tokString}\" in branch statement")
    }

    private fun handleBlockStmt(blockStmt: GoStandardLibrary.Ast.BlockStmt): Expression {
        return newBlock(rawNode = blockStmt, enterScope = true) { compound ->
            for (stmt in blockStmt.list) {
                val node = handle(stmt)
                // Do not add case statements to the block because the already add themselves in
                // handleCaseClause. Otherwise, the order of case's would be wrong
                if (node !is Case) {
                    compound.statements += node
                }
            }
        }
    }

    private fun handleCaseClause(
        caseClause: GoStandardLibrary.Ast.CaseClause,
        typeSwitchLhs: Node? = null,
        typeSwitchRhs: Expression? = null,
        // Whether this clause belongs to a type switch. This cannot be derived from
        // `typeSwitchRhs`, because the guard of a type switch need not bind the value to a
        // variable: in `switch v.(type)` -- as opposed to `switch x := v.(type)` -- there is no
        // assignment and hence no right-hand side, yet the case list still holds types rather
        // than expressions.
        isTypeSwitch: Boolean = typeSwitchRhs != null,
    ): Expression {

        val case =
            if (caseClause.list.isEmpty()) {
                newDefault(rawNode = caseClause)
            } else {
                newCase(rawNode = caseClause) { case ->
                    if (isTypeSwitch) {
                        // If this case is within a type switch, we want to wrap the case expression
                        // in a TypeExpression
                        val type = frontend.typeOf(caseClause.list[0])
                        case.caseExpression = newTypeExpression(type.name, type)
                    } else {
                        case.caseExpression = frontend.expressionHandler.handle(caseClause.list[0])
                    }
                }
            }

        // We need to find the current block / scope and add the statements to it
        val currentBlock = frontend.scopeManager.currentBlock

        if (currentBlock == null) {
            log.error("could not find block to add case clauses")
            return newProblemExpression("could not find block to add case clauses")
        }

        // Add the case statement
        currentBlock.statements += case

        // Go gives every case clause its own implicit block (see "Expression switches" and
        // "Type switches" in the language specification), so a declaration in one arm is not
        // visible in any other. Without a scope, the declarations of all arms land in the
        // enclosing function scope, and a name that several arms re-declare -- the
        // `var et SomeEType` idiom of a factory function, for instance -- produces several
        // candidates for a single reference. That is reported as an ambiguous resolution and
        // leaves the reference, and with it its type, unresolved.
        //
        // We only introduce the block when the arm actually declares something, so that the much
        // more common declaration-free arm keeps its flat representation and the evaluation order
        // of the switch is left untouched.
        val declaresSomething =
            caseClause.body.any {
                it is GoStandardLibrary.Ast.DeclStmt ||
                    // 47 is the DEFINE token, i.e. a short variable declaration.
                    (it is GoStandardLibrary.Ast.AssignStmt && it.tok == 47)
            }
        val block =
            if (isTypeSwitch || declaresSomething) {
                newBlock()
            } else {
                null
            }

        block?.let { frontend.scopeManager.enterScope(it) }

        // TODO(oxisto): This variable is not yet resolvable
        if (isTypeSwitch && typeSwitchLhs != null) {
            val stmt = newDeclarationStatement()
            stmt.isImplicit = true

            val decl = newVariable(typeSwitchLhs.name)
            if (case is Case) {
                decl.type = (case.caseExpression as? TypeExpression)?.type ?: unknownType()
            } else {
                // We need to work with type listeners here because they might not have their type
                // yet
                typeSwitchRhs?.registerTypeObserver(
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
                block.statements += stmt
            }
        }

        for (s in caseClause.body) {
            if (block != null) {
                block.statements += handle(s)
            } else {
                currentBlock.statements += handle(s)
            }
        }

        if (block != null) {
            currentBlock.statements += block
        }

        block?.let { frontend.scopeManager.leaveScope(it) }

        // this is a little trick, to not add the case statement in handleStmt because we added it
        // already. otherwise, the order is screwed up.
        return case
    }

    private fun handleDeclStmt(declStmt: GoStandardLibrary.Ast.DeclStmt): DeclarationStatement {
        // Let's create a variable declaration (wrapped with a declaration stmt) with
        // this, because we define the variable here
        return newDeclarationStatement(rawNode = declStmt) { stmt ->
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
        }
    }

    /**
     * // handleDeferStmt handles the `defer` statement, which is a special keyword in go // that
     * the supplied callee is executed once the function it is called in exists. // We cannot model
     * this 1:1, so we basically we create a call expression to a built-in call. // We adjust the
     * EOG of the call later in an extra pass.
     */
    private fun handleDeferStmt(deferStmt: GoStandardLibrary.Ast.DeferStmt): UnaryOperator {
        return newUnaryOperator("defer", postfix = false, prefix = true, rawNode = deferStmt) { op
            ->
            op.input = frontend.expressionHandler.handle(deferStmt.call)
        }
    }

    /**
     * This function handles the `go` statement, which is a special keyword in go that starts the
     * supplied call expression in a separate Go routine. We cannot model this 1:1, so we basically
     * we create a call expression to a built-in call.
     */
    private fun handleGoStmt(goStmt: GoStandardLibrary.Ast.GoStmt): Call {
        val ref = newReference("go")
        return newCall(ref, "go", rawNode = goStmt) { call ->
            call.arguments += frontend.expressionHandler.handle(goStmt.call)
        }
    }

    private fun handleForStmt(forStmt: GoStandardLibrary.Ast.ForStmt): For {
        return newFor(rawNode = forStmt, enterScope = true) { stmt ->
            forStmt.init?.let { stmt.initializerStatement = handle(it) }
            forStmt.cond?.let { stmt.condition = frontend.expressionHandler.handle(it) }
            forStmt.post?.let { stmt.iterationStatement = handle(it) }
            forStmt.body?.let { stmt.statement = handle(it) }
        }
    }

    private fun handleIncDecStmt(incDecStmt: GoStandardLibrary.Ast.IncDecStmt): UnaryOperator {
        return newUnaryOperator(
            incDecStmt.tokString,
            postfix = true,
            prefix = false,
            rawNode = incDecStmt,
        ) { op ->
            op.input = frontend.expressionHandler.handle(incDecStmt.x)
        }
    }

    private fun handleIfStmt(ifStmt: GoStandardLibrary.Ast.IfStmt): IfElse {
        return newIfElse(rawNode = ifStmt, enterScope = true) { stmt ->
            ifStmt.init?.let { stmt.initializerStatement = frontend.statementHandler.handle(it) }

            stmt.condition = frontend.expressionHandler.handle(ifStmt.cond)
            stmt.thenStatement = frontend.statementHandler.handle(ifStmt.body)

            ifStmt.`else`?.let { stmt.elseStatement = frontend.statementHandler.handle(it) }
        }
    }

    private fun handleLabeledStmt(labeledStmt: GoStandardLibrary.Ast.LabeledStmt): Label {
        return newLabel(rawNode = labeledStmt) { stmt ->
            stmt.subStatement = handle(labeledStmt.stmt)
            stmt.label = labeledStmt.label.name
        }
    }

    private fun handleRangeStmt(rangeStmt: GoStandardLibrary.Ast.RangeStmt): ForEach {
        return newForEach(rawNode = rangeStmt, enterScope = true) { forEach ->
            // TODO: Support other use cases that do not use DEFINE
            if (rangeStmt.tokString == ":=") {
                val stmt = newDeclarationStatement()

                // TODO: not really the best way to deal with this
                // TODO: key type is always int. we could set this
                rangeStmt.key?.let {
                    val ref = frontend.expressionHandler.handle(it)
                    if (ref is Reference) {
                        val key = newVariable(ref.name, rawNode = it)
                        frontend.scopeManager.addDeclaration(key)
                        stmt.declarationEdges += key
                    }
                }

                // TODO: not really the best way to deal with this
                rangeStmt.value?.let {
                    val ref = frontend.expressionHandler.handle(it)
                    if (ref is Reference) {
                        val key = newVariable(ref.name, rawNode = it)
                        frontend.scopeManager.addDeclaration(key)
                        stmt.declarationEdges += key
                    }
                }

                forEach.variable = stmt
            }

            forEach.iterable = frontend.expressionHandler.handle(rangeStmt.x)
            forEach.statement = frontend.statementHandler.handle(rangeStmt.body)
        }
    }

    private fun handleReturnStmt(returnStmt: GoStandardLibrary.Ast.ReturnStmt): Return {
        return newReturn(rawNode = returnStmt) { `return` ->
            // TODO: connect an empty return statement to named result variables
            `return`.returnValues =
                returnStmt.results.map { frontend.expressionHandler.handle(it) }.toMutableList()
        }
    }

    private fun handleSendStmt(sendStmt: GoStandardLibrary.Ast.SendStmt): BinaryOperator {
        return newBinaryOperator("<-", rawNode = sendStmt) { op ->
            op.lhs = frontend.expressionHandler.handle(sendStmt.chan)
            op.rhs = frontend.expressionHandler.handle(sendStmt.value)
        }
    }

    /**
     * Handles a `select` statement, which waits for one of several channel operations to become
     * ready. Control flow wise this behaves like a `switch` whose cases are the communication
     * operations, so we model it as a [Switch] with one case per [GoStandardLibrary.Ast.CommClause]
     * and no selector -- there is no single value being switched on.
     */
    private fun handleSelectStmt(selectStmt: GoStandardLibrary.Ast.SelectStmt): Expression {
        val switch = newSwitch(rawNode = selectStmt)

        frontend.scopeManager.enterScope(switch)

        val block =
            handle(selectStmt.body) as? Block
                ?: run {
                    frontend.scopeManager.leaveScope(switch)
                    return newProblemExpression("missing select body")
                }
        switch.statement = block

        frontend.scopeManager.leaveScope(switch)

        return switch
    }

    /**
     * Handles one arm of a `select`. The guarding communication operation becomes the case
     * expression, so that a receive such as `case v := <-ch:` still contributes its declaration and
     * its data flow. An arm without a communication operation is the `default` arm.
     */
    private fun handleCommClause(commClause: GoStandardLibrary.Ast.CommClause): Expression {
        val comm = commClause.comm
        val case =
            if (comm == null) {
                newDefault(rawNode = commClause)
            } else {
                newCase(rawNode = commClause) { case ->
                    // The guard is a send or a receive (possibly assigning, as in
                    // `case v := <-ch:`). Statements are expressions in the graph, so it can be
                    // used as the case expression directly, which keeps its calls and
                    // declarations reachable.
                    case.caseExpression = handle(comm)
                }
            }

        val currentBlock = frontend.scopeManager.currentBlock
        if (currentBlock == null) {
            log.error("could not find block to add comm clauses")
            return newProblemExpression("could not find block to add comm clauses")
        }

        currentBlock.statements += case

        for (statement in commClause.body) {
            currentBlock.statements += handle(statement)
        }

        // The case was already added to the enclosing block, so the caller must not add it again.
        return case
    }

    private fun handleSwitchStmt(switchStmt: GoStandardLibrary.Ast.SwitchStmt): Expression {
        val switch = newSwitch(rawNode = switchStmt)

        frontend.scopeManager.enterScope(switch)

        switchStmt.init?.let { switch.initializerStatement = handle(it) }
        switchStmt.tag?.let { switch.selector = frontend.expressionHandler.handle(it) }

        val block =
            handle(switchStmt.body) as? Block ?: return newProblemExpression("missing switch body")

        switch.statement = block

        frontend.scopeManager.leaveScope(switch)

        return switch
    }

    private fun handleTypeSwitchStmt(typeSwitchStmt: GoStandardLibrary.Ast.TypeSwitchStmt): Switch {
        return newSwitch(rawNode = typeSwitchStmt, enterScope = true) { switch ->
            typeSwitchStmt.init?.let { switch.initializerStatement = handle(it) }

            val assign = frontend.statementHandler.handle(typeSwitchStmt.assign)
            val (lhs, rhs) =
                if (assign is Assign) {
                    val rhs = assign.rhs.singleOrNull()
                    switch.selector = rhs
                    Pair(assign.lhs.singleOrNull(), (rhs as? UnaryOperator)?.input)
                } else {
                    // A type switch does not have to bind the value to a variable; `switch
                    // v.(type)` is a guard without an assignment. The handled statement is then
                    // the type assertion itself, which we still use as the switch's selector --
                    // otherwise it would be dropped from the graph entirely.
                    switch.selector = assign as? Expression
                    Pair(null, null)
                }

            switch.statement =
                newBlock(rawNode = typeSwitchStmt.body, enterScope = true) { body ->
                    for (c in
                        typeSwitchStmt.body.list.filterIsInstance<
                            GoStandardLibrary.Ast.CaseClause
                        >()) {
                        handleCaseClause(c, lhs, rhs, isTypeSwitch = true)
                    }
                }
        }
    }
}
