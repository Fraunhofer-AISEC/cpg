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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.golang.GoLanguage
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDecl
import de.fraunhofer.aisec.cpg.graph.followNextEOGEdgesUntilHit
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStmt
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpr
import de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOp

/** This pass contains fine-grained improvements to the EOG for the [GoLanguage]. */
class GoEvaluationOrderGraphPass(ctx: TranslationContext) : EvaluationOrderGraphPass(ctx) {

    /**
     * Go allows the automatic execution of certain cleanup calls before we exit the function (using
     * `defer`). We need to gather the appropriate deferred call expressions and then connect them
     * in [handleFunctionDeclaration].
     */
    private var deferredCalls = mutableMapOf<FunctionDecl, MutableList<UnaryOp>>()

    override fun handleUnspecificUnaryOperator(node: UnaryOp) {
        val input = node.input
        if (node.operatorCode == "defer" && input is CallExpr) {
            handleDeferUnaryOperator(node, input)
        } else {
            super.handleUnspecificUnaryOperator(node)
        }
    }

    /** Handles the EOG for a [`defer`](https://go.dev/ref/spec#Defer_statements) statement. */
    private fun handleDeferUnaryOperator(node: UnaryOp, input: CallExpr) {
        val function = scopeManager.currentFunction
        if (function != null) {
            // We need to disrupt the regular EOG handling here and store this deferred call. We
            // will pick it up again in handleFunctionDeclaration.
            val calls = deferredCalls.computeIfAbsent(function) { mutableListOf() }
            calls += node

            // Push the node itself to the EOG, not its "input" (the deferred call). However, it
            // seems that the arguments of the deferred call are evaluated at the point of the
            // deferred statement, duh!
            pushToEOG(node)

            // Evaluate the callee
            input.callee?.let { createEOG(it) }

            // Then the arguments
            for (arg in input.arguments) {
                createEOG(arg)
            }
        } else {
            log.error(
                "Tried to parse a defer statement but could not retrieve current function from scope manager."
            )
        }
    }

    override fun handleFunctionDeclaration(node: FunctionDecl) {
        // First, call the regular EOG handler
        super.handleFunctionDeclaration(node)

        // Before we exit the function, we need to call the deferred calls for this function
        val defers = deferredCalls[node]

        // We need to follow the path from the defer statement to all return statements that are
        // reachable from this point.
        for (defer in defers ?: listOf()) {
            val paths = defer.followNextEOGEdgesUntilHit { it is ReturnStmt }
            for (path in paths.fulfilled) {
                // It is a bit philosophical whether the deferred call happens before or after the
                // return statement in the EOG. For now, it is easier to have it as the last node
                // AFTER the return statement
                addEOGEdge(path.last(), defer.input)
            }
        }
    }
}
