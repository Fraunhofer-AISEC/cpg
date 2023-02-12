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
package de.fraunhofer.aisec.cpg.graph.scopes

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import org.slf4j.LoggerFactory

class LoopScope(loopStatement: Statement) :
    ValueDeclarationScope(loopStatement), Breakable, Continuable {
    /**
     * Statements that constitute the start of the Loop depending on the used pass, mostly of size 1
     */
    val starts: List<Node>
        get() =
            when (val loopStatement = astNode) {
                is WhileStatement -> {
                    if (loopStatement.conditionDeclaration != null)
                        SubgraphWalker.getEOGPathEdges(loopStatement.conditionDeclaration).entries
                    else if (loopStatement.condition != null)
                        SubgraphWalker.getEOGPathEdges(loopStatement.condition).entries
                    else SubgraphWalker.getEOGPathEdges(loopStatement.statement).entries
                }
                is ForStatement -> {
                    if (loopStatement.conditionDeclaration != null)
                        SubgraphWalker.getEOGPathEdges(loopStatement.conditionDeclaration).entries
                    else if (loopStatement.condition != null)
                        SubgraphWalker.getEOGPathEdges(loopStatement.condition).entries
                    else SubgraphWalker.getEOGPathEdges(loopStatement.statement).entries
                }
                is ForEachStatement -> {
                    SubgraphWalker.getEOGPathEdges(loopStatement).entries
                }
                is DoStatement -> {
                    SubgraphWalker.getEOGPathEdges(loopStatement.statement).entries
                }
                else -> {
                    LOGGER.error(
                        "Currently the component {} is not supported as loop scope.",
                        astNode!!.javaClass
                    )
                    ArrayList()
                }
            }

    /** Statements that constitute the start of the Loop condition evaluation, mostly of size 1 */
    val conditions: List<Node>
        get() =
            when (val node = astNode) {
                is WhileStatement ->
                    mutableListOf(node.condition, node.conditionDeclaration).filterNotNull()
                is ForStatement -> mutableListOf(node.condition).filterNotNull()
                is ForEachStatement -> mutableListOf(node.variable).filterNotNull()
                is DoStatement -> mutableListOf(node.condition).filterNotNull()
                is AssertStatement -> mutableListOf(node.condition).filterNotNull()
                null -> {
                    LOGGER.error("Ast node of loop scope is null.")
                    mutableListOf()
                }
                else -> {
                    LOGGER.error(
                        "Currently the component {} is not supported as loop scope.",
                        node.javaClass
                    )
                    mutableListOf()
                }
            }
    private val breaks = mutableListOf<BreakStatement>()
    private val continues = mutableListOf<ContinueStatement>()

    override fun addBreakStatement(breakStatement: BreakStatement) {
        breaks.add(breakStatement)
    }

    override fun addContinueStatement(continueStatement: ContinueStatement) {
        continues.add(continueStatement)
    }

    override val breakStatements: List<BreakStatement>
        get() = breaks

    override val continueStatements: List<ContinueStatement>
        get() = continues

    companion object {
        private val LOGGER = LoggerFactory.getLogger(LoopScope::class.java)
    }
}
