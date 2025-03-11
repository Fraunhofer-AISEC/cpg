/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.jvm

import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.AssignExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ProblemExpression
import sootup.core.jimple.common.stmt.*
import sootup.core.model.Body
import sootup.core.util.printer.NormalStmtPrinter

class StatementHandler(frontend: JVMLanguageFrontend) :
    Handler<Statement, Any, JVMLanguageFrontend>(::ProblemExpression, frontend) {
    init {
        map.put(Body::class.java) { handleBody(it as Body) }
        map.put(JAssignStmt::class.java) { handleAbstractDefinitionStmt(it as JAssignStmt) }
        map.put(JIdentityStmt::class.java) { handleAbstractDefinitionStmt(it as JIdentityStmt) }
        map.put(JIfStmt::class.java) { handleIfStmt(it as JIfStmt) }
        map.put(JGotoStmt::class.java) { handleGotoStmt(it as JGotoStmt) }
        map.put(JInvokeStmt::class.java) { handleInvokeStmt(it as JInvokeStmt) }
        map.put(JReturnStmt::class.java) { handleReturnStmt(it as JReturnStmt) }
        map.put(JReturnVoidStmt::class.java) { handleReturnVoidStmt(it as JReturnVoidStmt) }
    }

    private fun handleBody(body: Body): Block {
        // The first block contains all our other blocks and this will be the one we return
        val outerBlock = newBlock(rawNode = body)

        val printer = NormalStmtPrinter()
        printer.initializeSootMethod(body.stmtGraph)

        frontend.printer = printer
        frontend.body = body

        // Parse locals, these are always at the beginning of the function
        for (local in body.locals) {
            val decl = frontend.declarationHandler.handle(local)

            if (decl != null) {
                // We need to wrap them into a declaration statement and put them into the outer
                // block
                val stmt = newDeclarationStatement(rawNode = local)
                frontend.scopeManager.addDeclaration(decl)
                stmt.declarations += decl
                outerBlock += stmt
            }
        }

        // Parse statements and segment them into (sub)-blocks.
        var block = outerBlock
        for (sootStmt in body.stmts) {
            val label = printer.labels[sootStmt]
            if (label != null) {
                // If we have a label, we need to create a new label statement, that starts a new
                // block
                val stmt = newLabelStatement()
                block = newBlock()
                stmt.label = label
                stmt.subStatement = block

                // We need to inform our processing system, since we do it outside of a handler, so
                // the created goto statements will be informed about our new label
                frontend.process(Any(), stmt)

                // Always add it to the outer block
                outerBlock += stmt
            }

            // Parse the statement
            val stmt = handle(sootStmt)
            if (stmt != null) {
                block += stmt
            }
        }

        // Always return the outer block, since it comprises all the other sub-blocks.
        return outerBlock
    }

    private fun handleAbstractDefinitionStmt(defStmt: AbstractDefinitionStmt): AssignExpression {
        val assign = newAssignExpression("=", rawNode = defStmt)
        assign.lhs =
            listOfNotNull(frontend.expressionHandler.handle(defStmt.leftOp)).toMutableList()
        assign.rhs =
            listOfNotNull(frontend.expressionHandler.handle(defStmt.rightOp)).toMutableList()

        return assign
    }

    private fun handleIfStmt(ifStmt: JIfStmt): IfStatement {
        val stmt = newIfStatement(rawNode = ifStmt)
        stmt.condition =
            frontend.expressionHandler.handle(ifStmt.condition)
                ?: newProblemExpression("missing condition")
        stmt.thenStatement = handleBranchingStmt(ifStmt)

        return stmt
    }

    private fun handleGotoStmt(gotoStmt: JGotoStmt): GotoStatement {
        return handleBranchingStmt(gotoStmt)
    }

    private fun handleBranchingStmt(branchingStmt: BranchingStmt): GotoStatement {
        val stmt = newGotoStatement(rawNode = branchingStmt)

        frontend.body?.let {
            val target = branchingStmt.getTargetStmts(it).firstOrNull()
            val label = frontend.printer?.labels?.get(target)
            if (label != null) {
                stmt.labelName = label
            }

            // Register a predicate listener that informs us as soon as new label statement that
            // matches our label name is created.
            frontend.registerPredicateListener({ _, to ->
                (to is LabelStatement && to.label == stmt.labelName)
            }) { _, to ->
                stmt.targetLabel = to as LabelStatement
            }
        }

        return stmt
    }

    private fun handleInvokeStmt(invokeStmt: JInvokeStmt) =
        frontend.expressionHandler.handle(invokeStmt.invokeExpr)

    private fun handleReturnStmt(returnStmt: JReturnStmt): ReturnStatement {
        val stmt = newReturnStatement(rawNode = returnStmt)
        stmt.returnValue =
            frontend.expressionHandler.handle(returnStmt.op)
                ?: newProblemExpression("missing return value")

        return stmt
    }

    private fun handleReturnVoidStmt(returnStmt: JReturnVoidStmt) =
        newReturnStatement(rawNode = returnStmt)
}
