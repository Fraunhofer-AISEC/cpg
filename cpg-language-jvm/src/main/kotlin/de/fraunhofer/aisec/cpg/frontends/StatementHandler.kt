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

import de.fraunhofer.aisec.cpg.graph.newAssignExpression
import de.fraunhofer.aisec.cpg.graph.newBlock
import de.fraunhofer.aisec.cpg.graph.newDeclarationStatement
import de.fraunhofer.aisec.cpg.graph.newReference
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.AssignExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ProblemExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import sootup.core.jimple.basic.Local
import sootup.core.jimple.common.stmt.JAssignStmt
import sootup.core.model.Body

class StatementHandler(frontend: JVMLanguageFrontend) :
    Handler<Statement, Any, JVMLanguageFrontend>(::ProblemExpression, frontend) {
    init {
        map.put(Body::class.java) { handleBody(it as Body) }
        map.put(Local::class.java) {handleLocal(it as Local)}
        map.put(JAssignStmt::class.java) { handleAssignStmt(it as JAssignStmt<*, *>) }

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
            }
        }

        // Parse statements
        for (sootStmt in body.stmts) {
            handle(sootStmt)?.let { block += it }
        }

        return block
    }

    private fun handleAssignStmt(assignStmt: JAssignStmt<*, *>): AssignExpression {
        val assign = newAssignExpression("=", rawNode = assignStmt)
        assign.lhs = listOf(handle(assignStmt.leftOp))

        return assign
    }

    private fun handleLocal(local: Local): Reference {
        val ref = newReference(local.name, rawNode = local)

        return ref
    }
}
