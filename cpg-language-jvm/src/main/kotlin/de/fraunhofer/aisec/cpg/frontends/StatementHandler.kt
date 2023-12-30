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
package de.fraunhofer.aisec.cpg.frontends

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import sootup.core.jimple.common.expr.JVirtualInvokeExpr
import sootup.core.jimple.common.stmt.*
import sootup.core.model.Body

class StatementHandler(frontend: JVMLanguageFrontend) :
    Handler<Statement, Any, JVMLanguageFrontend>(::ProblemExpression, frontend) {
    init {
        map.put(Body::class.java) { handleBody(it as Body) }
        map.put(JAssignStmt::class.java) { handleAbstractDefinitionStmt(it as JAssignStmt<*, *>) }
        map.put(JIdentityStmt::class.java) { handleAbstractDefinitionStmt(it as JIdentityStmt<*>) }
        map.put(JInvokeStmt::class.java) { handleInvokeStmt(it as JInvokeStmt) }
        map.put(JReturnVoidStmt::class.java) { handleReturnVoidStmt(it as JReturnVoidStmt) }
    }

    private fun handleBody(body: Body): Block {
        val block = newBlock(rawNode = body)

        // Parse locals, these are always at the beginning of the function
        for (local in body.locals) {
            val decl = frontend.declarationHandler.handle(local)

            if (decl != null) {
                // We need to wrap them into a declaration statement
                val stmt = newDeclarationStatement(rawNode = local)
                stmt.addToPropertyEdgeDeclaration(decl)
                frontend.scopeManager.addDeclaration(decl)
                block += stmt
            }
        }

        // Parse statements
        for (sootStmt in body.stmts) {
            handle(sootStmt)?.let { block += it }
        }

        return block
    }

    private fun handleAbstractDefinitionStmt(
        defStmt: AbstractDefinitionStmt<*, *>
    ): AssignExpression {
        val assign = newAssignExpression("=", rawNode = defStmt)
        assign.lhs = listOfNotNull(frontend.expressionHandler.handle(defStmt.leftOp))
        assign.rhs = listOfNotNull(frontend.expressionHandler.handle(defStmt.rightOp))

        return assign
    }

    private fun handleInvokeStmt(invokeStmt: JInvokeStmt): Expression? {
        // For now, we only parse virtualinvoke (more or less) correctly
        if (invokeStmt.invokeExpr is JVirtualInvokeExpr) {
            return frontend.expressionHandler.handle(invokeStmt.invokeExpr)
        }

        return newProblemExpression("cannot parse invoke stmt yet")
    }

    private fun handleReturnVoidStmt(returnStmt: JReturnVoidStmt): ReturnStatement {
        val stmt = newReturnStatement(rawNode = returnStmt)

        return stmt
    }
}
