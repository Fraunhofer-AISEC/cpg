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
package de.fraunhofer.aisec.cpg.frontends.ruby

import de.fraunhofer.aisec.cpg.graph.newBlock
import de.fraunhofer.aisec.cpg.graph.newReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Problem
import org.jruby.ast.*

class StatementHandler(lang: RubyLanguageFrontend) :
    RubyHandler<Statement, Node>({ Problem() }, lang) {

    override fun handleNode(node: Node): Statement {
        return when (node) {
            is BlockNode -> handleBlockNode(node)
            is ReturnNode -> handleReturnNode(node)
            else -> {
                // We do not have an explicit statement wrapper around expressions, so we first try
                // to parse the remaining nodes as an expression
                frontend.expressionHandler.handleNode(node)
            }
        }
    }

    private fun handleBlockNode(blockNode: BlockNode): Block {
        val compoundStatement = newBlock()

        for (node in blockNode.filterNotNull()) {
            compoundStatement.statements += handle(node)
        }

        return compoundStatement
    }

    private fun handleReturnNode(node: ReturnNode): ReturnStatement {
        val stmt = newReturnStatement()
        stmt.returnValue = frontend.expressionHandler.handleNode(node.valueNode)

        return stmt
    }
}
