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
package de.fraunhofer.aisec.cpg.passes.scopes

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import org.slf4j.LoggerFactory

class LoopScope(loopStatement: Statement) :
    ValueDeclarationScope(loopStatement), Breakable, Continuable {
    /**
     * Statements that constitute the start of the Loop depending on the used pass, mostly of size 1
     */
    var starts: List<Node> = ArrayList()

    /** Statements that constitute the start of the Loop condition evaluation, mostly of size 1 */
    var conditions: List<Node> = ArrayList()
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

    fun starts(): List<Node> {
        return when (astNode) {
            is WhileStatement -> {
                val ws = astNode as WhileStatement
                if (ws.conditionDeclaration != null)
                    return SubgraphWalker.getEOGPathEdges(ws.conditionDeclaration).entries
                else if (ws.condition != null)
                    return SubgraphWalker.getEOGPathEdges(ws.condition).entries
                SubgraphWalker.getEOGPathEdges(ws.statement).entries
            }
            is ForStatement -> {
                val fs = astNode as ForStatement
                if (fs.conditionDeclaration != null)
                    return SubgraphWalker.getEOGPathEdges(fs.conditionDeclaration).entries
                else if (fs.condition != null)
                    return SubgraphWalker.getEOGPathEdges(fs.condition).entries
                SubgraphWalker.getEOGPathEdges(fs.statement).entries
            }
            is ForEachStatement -> {
                val fs = astNode as ForEachStatement
                SubgraphWalker.getEOGPathEdges(fs).entries
            }
            is DoStatement -> {
                SubgraphWalker.getEOGPathEdges((astNode as DoStatement).statement).entries
            }
            else -> {
                LOGGER.error(
                    "Currently the component {} is not supported as loop scope.",
                    astNode!!.javaClass
                )
                ArrayList()
            }
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(LoopScope::class.java)
    }
}
