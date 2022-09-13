/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.allChildren
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression

/**
 * Adds DFG edges for unresolved function calls as follows:
 * - from base (if available) to the CallExpression
 * - from all arguments to the CallExpression
 */
class UnresolvedDFGPass : Pass() {
    override fun accept(t: TranslationResult) {
        if (t.translationManager.config.inferenceConfiguration.inferDfgForUnresolvedCalls) {
            addDfgEdgesForUnresolvedCalls(t)
        }
    }

    override fun cleanup() {
        // Nothing to do
    }

    /**
     * Adds DFG edges for unresolved function calls as follows:
     * - from base (if available) to the CallExpression
     * - from all arguments to the CallExpression
     */
    private fun addDfgEdgesForUnresolvedCalls(t: TranslationResult) {
        val unresolvedCalls = t.allChildren<CallExpression> { it.invokes.isEmpty() }
        for (call in unresolvedCalls) {
            call.base?.let {
                call.addPrevDFG(it)
                it.addNextDFG(call)
            }
            call.arguments.forEach {
                call.addPrevDFG(it)
                it.addNextDFG(call)
            }
        }
    }
}
