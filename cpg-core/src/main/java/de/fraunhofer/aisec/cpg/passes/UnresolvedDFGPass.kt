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
import de.fraunhofer.aisec.cpg.graph.Assignment
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ArrayCreationExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ArraySubscriptionExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CastExpression
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.IterativeGraphWalker
import de.fraunhofer.aisec.cpg.helpers.Util

/**
 * Adds DFG edges for unresolved function calls as follows:
 * - from base (if available) to the CallExpression
 * - from all arguments to the CallExpression
 */
class UnresolvedDFGPass : Pass() {
    var inferDfgForUnresolvedCalls: Boolean = false

    override fun accept(tr: TranslationResult) {
        inferDfgForUnresolvedCalls =
            tr.translationManager.config.inferenceConfiguration.inferDfgForUnresolvedCalls
        val walker = IterativeGraphWalker()
        walker.registerOnNodeVisit { handle(it) }
        for (tu in tr.translationUnits) {
            walker.iterate(tu)
        }
    }

    override fun cleanup() {
        // Nothing to do
    }

    private fun handle(node: Node?) {
        when (node) {
            is CallExpression -> handleCallExpression(node)
            is CastExpression -> handleCastExpression(node)
            is BinaryOperator -> handleBinaryOp(node)
            is Assignment -> handleAssignment(node)
            is ArrayCreationExpression -> handleArrayCreationExpression(node)
            is ArraySubscriptionExpression -> handleArraySubscriptionExpression(node)
        }
    }

    /**
     * Adds the DFG edge to an [ArraySubscriptionExpression]. The whole array `x` flows to the
     * result `x[i]`.
     */
    private fun handleArraySubscriptionExpression(node: ArraySubscriptionExpression) {
        node.addPrevDFG(node.arrayExpression)
    }

    /**
     * Adds the DFG edge to an [ArrayCreationExpression]. The initializer flows to the expression.
     */
    private fun handleArrayCreationExpression(node: ArrayCreationExpression) {
        node.initializer?.let { node.addPrevDFG(it) }
    }

    /**
     * Adds the DFG edge to an [BinaryOperator]. The value flows to the target of an assignment or
     * to the whole expression.
     */
    private fun handleBinaryOp(node: BinaryOperator) {
        when (node.operatorCode) {
            "=" -> node.lhs.addPrevDFG(node.rhs)
            "*=",
            "/=",
            "%=",
            "+=",
            "-=",
            "<<=",
            ">>=",
            "&=",
            "^=",
            "|=" -> {
                node.addPrevDFG(node.lhs)
                node.addPrevDFG(node.rhs)
                node.addNextDFG(node.lhs)
            }
            else -> {
                node.addPrevDFG(node.lhs)
                node.addPrevDFG(node.rhs)
            }
        }
    }

    /** Adds the DFG edge to an [Assignment]. The value flows to the target. */
    private fun handleAssignment(node: Assignment) {
        node.value?.let { (node.target as? Node)?.addPrevDFG(it) }
    }

    /**
     * Adds the DFG edge to a [CastExpression]. The inner expression flows to the cast expression.
     */
    private fun handleCastExpression(castExpression: CastExpression) {
        if (castExpression.expression != null) {
            castExpression.addPrevDFG(castExpression.expression)
        }
    }

    /** Adds the DFG edges to a [CallExpression]. */
    private fun handleCallExpression(call: CallExpression) {
        if (call.invokes.isEmpty() && inferDfgForUnresolvedCalls) {
            // Unresolved call expression
            handleUnresolvedCalls(call)
        } else if (call.invokes.isNotEmpty()) {
            call.invokes.forEach {
                Util.attachCallParameters(it, call.arguments)
                call.addPrevDFG(it)
            }
            // TODO: Call expression with resolved function
        }
    }

    /**
     * Adds DFG edges for unresolved function calls as follows:
     * - from base (if available) to the CallExpression
     * - from all arguments to the CallExpression
     */
    private fun handleUnresolvedCalls(call: CallExpression) {
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
