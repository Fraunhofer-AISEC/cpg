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
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.logging.LoggingNode
import de.fraunhofer.aisec.cpg.graph.concepts.logging.newLogOperationNode
import de.fraunhofer.aisec.cpg.graph.concepts.logging.newLoggingNode
import de.fraunhofer.aisec.cpg.graph.declarations.ImportDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.TranslationResultPass
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteLate

// TODO: TranslationResultPass is an ugly hack. However, we need this to access tr.additionalNodes
@ExecuteLate
class LoggingConceptPass(ctx: TranslationContext) :
    TranslationResultPass(ctx) { // TODO: componentpass astParent -> result
    private val loggers = mutableMapOf<Node, LoggingNode>()
    private lateinit var result: TranslationResult

    override fun cleanup() {
        // nothing to do
    }

    override fun accept(result: TranslationResult) {
        this.result = result
        val walker = SubgraphWalker.ScopedWalker(ctx.scopeManager)
        walker.registerHandler { _, _, currNode -> handle(currNode) }

        for (tu in result.components.flatMap { it.translationUnits }) {
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
        if (importDeclaration.import.toString() == "logging") {
            val newNode = newLoggingNode(cpgNode = importDeclaration, result)
            loggers +=
                importDeclaration to
                    newNode // TODO using the importDeclaration feels wrong -> this should somehow
            // be the imported symbol...
        }
    }

    private fun handleCall(callExpression: CallExpression) {
        if (
            callExpression.callee.name.startsWith("logging.")
        ) { // TODO this assumes `logging` always refers to `import logging`
            when (callExpression.callee.name.toString()) {
                "logging.critical",
                "logging.error",
                "logging.warning",
                "logging.info",
                "logging.debug", -> {
                    val logger =
                        loggers
                            .filterKeys { node -> node.name.toString() == "logging" }
                            .values
                            .singleOrNull()
                            ?: TODO("Expected to find exactly one \"logging\" logger.")

                    newLogOperationNode(
                        cpgNode = callExpression,
                        result = result,
                        logger = logger,
                        logArguments = callExpression.arguments,
                        level = callExpression.callee.name.toString().substringAfterLast('.')
                    )
                }
                "logging.getLogger" -> {
                    val newNode = newLoggingNode(cpgNode = callExpression, result)
                    if (callExpression.astParent is AssignExpression) {
                        val assign = callExpression.astParent
                        (assign as? AssignExpression)?.let {
                            if (assign.isCompoundAssignment) {
                                TODO("Cannot handle complex assignments yet.")
                            } else {
                                assign.lhs.singleOrNull()?.let { loggers += it to newNode }
                            }
                        }
                    } else {
                        TODO(
                            "Logger not created as part of an assign expr. This is not yet supported."
                        )
                    }
                }
                else -> {}
            }
        } else if (callExpression is MemberCallExpression) {
            // TODO MemberExpression base refersTo
            val log = ((callExpression.callee as MemberExpression).base as Reference).refersTo
        }
    }
}
