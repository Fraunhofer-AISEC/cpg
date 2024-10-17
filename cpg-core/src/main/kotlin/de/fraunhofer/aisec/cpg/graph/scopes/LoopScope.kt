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

import de.fraunhofer.aisec.cpg.graph.statements.*
import org.slf4j.LoggerFactory

class LoopScope(loopStatement: Statement) :
    ValueDeclarationScope(loopStatement), Breakable, Continuable {

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
