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
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.file.FileNode
import de.fraunhofer.aisec.cpg.graph.concepts.file.newFileNode
import de.fraunhofer.aisec.cpg.graph.concepts.file.newFileReadNode
import de.fraunhofer.aisec.cpg.graph.concepts.file.newFileWriteNode
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.ComponentPass
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteLate

@ExecuteLate
class FileConceptPass(ctx: TranslationContext) : ComponentPass(ctx) {
    private val fileNodes = mutableMapOf<Node, FileNode>()
    private lateinit var result: TranslationResult

    override fun cleanup() {
        // nothing to do
    }

    override fun accept(comp: Component) {
        val parent = comp.astParent
        if (parent is TranslationResult) {
            result = parent
        } else {
            TODO("Failed to find a translation result.")
        }

        val walker = SubgraphWalker.ScopedWalker(ctx.scopeManager)
        walker.registerHandler { _, _, currNode -> handle(currNode) }

        for (tu in comp.translationUnits) {
            walker.iterate(tu)
        }
    }

    private fun handle(node: Node?) {
        when (node) {
            is CallExpression -> handleCall(node)
            else -> {
                // nothing to do
            }
        }
    }

    private fun handleCall(callExpression: CallExpression) {
        val name = callExpression.name

        if (name.toString() == "open") {
            val fileName = getFileName(callExpression)
            val newFileNode =
                newFileNode(
                    underlyingNode = callExpression,
                    result = result,
                    accessMode = getAccessMode(callExpression),
                    fileName = fileName,
                )
            if (callExpression.astParent is AssignExpression) {
                val assign: AssignExpression =
                    callExpression.astParent as? AssignExpression
                        ?: TODO("Cast failed unexpectedly.")
                if (assign.isSimpleAssignment) {
                    val singleAssignment =
                        assign.assignments.singleOrNull()
                            ?: TODO("Expected exactly one assignment.")

                    val target =
                        singleAssignment.target as? Node ?: TODO("Cast failed unexpectedly.")
                    fileNodes += target to newFileNode
                } else {
                    TODO("Cannot handle complex assignments yet.")
                }
            } else {
                // what do we use as a key? think of code like open().write().close()
                TODO("Open call not part of assign expr. Maybe directly used?")
            }
        } else if (callExpression is MemberCallExpression) {
            val fileNode = findConceptNode(callExpression, fileNodes)
            fileNode?.let {
                val localName = name.localName.toString()
                when (localName) {
                    "read" -> {
                        newFileReadNode(
                            underlyingNode = callExpression,
                            result = result,
                            fileNode = fileNode,
                        )
                    }
                    "write" -> {
                        newFileWriteNode(
                            underlyingNode = callExpression,
                            result = result,
                            fileNode = fileNode,
                        )
                    }
                    else -> {}
                }
            }
        } else {}
    }

    private fun <T> findConceptNode(
        callExpression: MemberCallExpression,
        map: MutableMap<Node, T>,
    ): T? {

        val callee = callExpression.callee
        if ((callee is MemberExpression)) {
            val refTo = (callee.base as? Reference)?.refersTo
            refTo?.let {
                return map[it]
            }
        }
        return null
    }

    private fun getFileName(call: CallExpression): String {
        return if (call.arguments.isNotEmpty()) {
            val arg = call.arguments[0]
            if (arg is Literal<*>) {
                arg.value.toString()
            } else {
                TODO("Cannot handle non-literal filenames yet.")
            }
        } else {
            TODO("Expected a filename argument.")
        }
    }

    private fun getAccessMode(call: CallExpression): String {
        // TODO named parameter `mode`
        return if (call.arguments.size >= 2) {
            val arg = call.arguments[1]
            if (arg is Literal<*>) {
                arg.value.toString()
            } else {
                TODO("Cannot handle non-literal access modes yet.")
            }
        } else {
            TODO("Expected a filename as 2nd argument.")
        }
    }
}
