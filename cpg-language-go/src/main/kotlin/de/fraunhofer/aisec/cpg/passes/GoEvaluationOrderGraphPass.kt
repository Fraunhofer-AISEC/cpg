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
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.Namespace
import de.fraunhofer.aisec.cpg.graph.declarations.Record
import de.fraunhofer.aisec.cpg.graph.followEOGEdgesUntilHit
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator

/** This pass contains fine-grained improvements to the EOG for the [GoLanguage]. */
@Description("This pass contains fine-grained improvements to the EOG for the go language.")
class GoEvaluationOrderGraphPass(ctx: TranslationContext) : EvaluationOrderGraphPass(ctx) {

    /**
     * Go allows the automatic execution of certain cleanup calls before we exit the function (using
     * `defer`). We need to gather the appropriate deferred call expressions and then connect them
     * in [handleFunctionDeclaration].
     */
    private var deferredCalls = mutableMapOf<FunctionDeclaration, MutableList<UnaryOperator>>()

    override fun handleUnspecificUnaryOperator(node: UnaryOperator) {
        val input = node.input
        if (node.operatorCode == "defer" && input is CallExpression) {
            handleDeferUnaryOperator(node, input)
        } else {
            super.handleUnspecificUnaryOperator(node)
        }
    }

    /** Handles the EOG for a [`defer`](https://go.dev/ref/spec#Defer_statements) statement. */
    private fun handleDeferUnaryOperator(node: UnaryOperator, input: CallExpression) {
        val function = scopeManager.currentFunction
        if (function != null) {
            // We need to disrupt the regular EOG handling here and store this deferred call. We
            // will pick it up again in handleFunctionDeclaration.
            val calls = deferredCalls.computeIfAbsent(function) { mutableListOf() }
            calls += node

            // Push the node itself to the EOG, not its "input" (the deferred call). However, it
            // seems that the arguments of the deferred call are evaluated at the point of the
            // deferred statement, duh!
            attachToEOG(node)

            // Evaluate the callee
            handleEOG(input.callee)

            // Then the arguments
            for (arg in input.arguments) {
                handleEOG(arg)
            }
        } else {
            log.error(
                "Tried to parse a defer statement but could not retrieve current function from scope manager."
            )
        }
    }

    /**
     * We need to intentionally override [handleRecord] to NOT create the EOG for its
     * children, e.g., [Record.methods]. The reason for this is that Go only has external
     * methods declarations, and we do a little cheat by adding the methods both to the namespace of
     * the current file and to the [Record.methods].
     *
     * But, due to this, the original [EvaluationOrderGraphPass] would create the EOG for methods
     * twice, once for the object in the [Record] and once for the declaration inside the
     * [Namespace] of the current file.
     */
    override fun handleRecord(node: Record) {
        scopeManager.enterScope(node)
        handleStatementHolder(node)
        currentPredecessors.clear()
        scopeManager.leaveScope(node)
    }

    override fun handleFunctionDeclaration(node: FunctionDeclaration) {
        // First, call the regular EOG handler
        super.handleFunctionDeclaration(node)

        // Before we exit the function, we need to call the deferred calls for this function
        val defers = deferredCalls[node]

        // We need to follow the path from the defer statement to all return statements that are
        // reachable from this point.
        for (defer in defers ?: listOf()) {
            val paths = defer.followEOGEdgesUntilHit { it is ReturnStatement }
            for (path in paths.fulfilled) {
                // It is a bit philosophical whether the deferred call happens before or after the
                // return statement in the EOG. For now, it is easier to have it as the last node
                // AFTER the return statement
                addEOGEdge(path.nodes.last(), defer.input)
            }
        }
    }
}
