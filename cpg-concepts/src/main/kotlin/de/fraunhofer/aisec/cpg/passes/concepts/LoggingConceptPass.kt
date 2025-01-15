/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.passes.concepts

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.logging.LoggingNode
import de.fraunhofer.aisec.cpg.graph.concepts.logging.newLogOperationNode
import de.fraunhofer.aisec.cpg.graph.concepts.logging.newLoggingNode
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.ImportDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.ComponentPass
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteLate

@ExecuteLate
class LoggingConceptPass(ctx: TranslationContext) : ComponentPass(ctx) {

    /** A storage connecting CPG nodes with [LoggingNode]s. */
    private val loggers = mutableMapOf<Declaration, LoggingNode>()

    override fun cleanup() {
        // nothing to do
    }

    override fun accept(comp: Component) {
        // Now we start with the real pass work
        val walker = SubgraphWalker.ScopedWalker(ctx.scopeManager)

        walker.registerHandler { _, _, currNode -> handle(currNode) }

        for (tu in comp.translationUnits) {
            walker.iterate(tu)
        }
    }

    private fun handle(node: Node?) {
        when (node) {
            is ImportDeclaration -> handleImport(node)
            is CallExpression -> handleCall(node)
            else -> {
                // nothing to do
            }
        }
    }

    private fun handleImport(importDeclaration: ImportDeclaration) {
        if (
            importDeclaration.import.toString() == "logging"
        ) { // TODO what about import logging as foo
            val newNode = newLoggingNode(underlyingNode = importDeclaration)
            loggers += importDeclaration to newNode
        }
    }

    private fun handleCall(callExpression: CallExpression) {
        val callee = callExpression.callee
        if (
            callee is MemberExpression && (callee.base as Reference).refersTo in loggers.keys
        ) { // TODO join with helper in FileConceptPass
            val logger = loggers[(callee.base as Reference).refersTo]!! // TODO !!
            logOpHelper(callExpression, logger)
        } else if (callee is Reference && callee.name.toString().startsWith("logging")) {
            val logger =
                loggers
                    .filterKeys { it is ImportDeclaration && it.name.toString() == "logging" }
                    .values
                    .singleOrNull() ?: TODO("Did not find an `import logging` logger.")
            logOpHelper(callExpression = callExpression, logger = logger)
        } else if (callee.name.toString() == "logging.getLogger") {
            val newNode = newLoggingNode(underlyingNode = callExpression)

            if (callExpression.astParent is AssignExpression) {
                val assign = callExpression.astParent

                (assign as? AssignExpression)?.let {
                    if (assign.isCompoundAssignment) {
                        TODO("Cannot handle complex assignments yet.")
                    } else {
                        assign.declarations.singleOrNull()?.let { loggers += it to newNode }
                    }
                }
            } else {
                TODO("Logger not created as part of an assign expr. This is not yet supported.")
            }
        } else if (callExpression is MemberCallExpression) {
            // TODO MemberExpression base refersTo
            val log = ((callExpression.callee as MemberExpression).base as Reference).refersTo
        }
    }

    private fun logOpHelper(callExpression: CallExpression, logger: LoggingNode) {
        val name = callExpression.name.localName.toString()
        when (name) {
            "critical",
            "error",
            "warning",
            "info",
            "debug" -> {
                newLogOperationNode(
                    underlyingNode = callExpression,
                    logger = logger,
                    logArguments = callExpression.arguments,
                    level = name,
                )
            }
            else -> {}
        }
    }
}
