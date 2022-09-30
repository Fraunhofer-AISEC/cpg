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
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.DoStatement
import de.fraunhofer.aisec.cpg.graph.statements.ForEachStatement
import de.fraunhofer.aisec.cpg.graph.statements.ForStatement
import de.fraunhofer.aisec.cpg.graph.statements.GotoStatement
import de.fraunhofer.aisec.cpg.graph.statements.WhileStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.IterativeGraphWalker

@DependsOn(EvaluationOrderGraphPass::class)
@DependsOn(DFGPass::class)
open class ControlFlowSensitiveDFGPass : Pass() {
    override fun cleanup() {
        // Nothing to do
    }

    override fun accept(translationResult: TranslationResult) {
        val walker = IterativeGraphWalker()
        walker.registerOnNodeVisit(::handle)
        for (tu in translationResult.translationUnits) {
            walker.iterate(tu)
        }
    }

    /**
     * ControlFlowSensitiveDFG Pass is performed on every method.
     *
     * @param node every node in the TranslationResult
     */
    protected fun handle(node: Node) {
        if (node is FunctionDeclaration) {
            handleFunction(node)
        }
    }

    private fun handleFunction(node: FunctionDeclaration) {
        for (varDecl in node.variables) {
            varDecl.clearPrevDFG()
            varDecl.clearNextDFG()
        }

        val worklist =
            mutableListOf<Pair<Node, MutableMap<Declaration, MutableList<Node>>>>(
                Pair(node, mutableMapOf())
            )

        val loopPoints = mutableMapOf<Node, MutableSet<Map<Declaration, MutableList<Node>>>>()

        while (worklist.isNotEmpty()) {
            val (currentNode, previousWrites) = worklist.removeFirst()
            var writtenDecl: Declaration? = null
            var currentWritten = currentNode

            if ((currentNode as? VariableDeclaration)?.initializer != null) {
                // A variable declaration with an initializer => The initializer flows to the
                // declaration.
                val initValue = currentNode.initializer
                writtenDecl = currentNode

                initValue?.let { currentNode.addPrevDFG(it) }

                // Add the node to the list of previous write nodes in this path
                previousWrites[currentNode] = mutableListOf(currentNode)
            } else if (
                currentNode is UnaryOperator &&
                    (currentNode.operatorCode == "++" || currentNode.operatorCode == "--")
            ) {
                // Increment or decrement => Add the prevWrite of the input to the input. After the
                // operation, the prevWrite of the input's variable is this node.
                val input = currentNode.input as? DeclaredReferenceExpression
                writtenDecl = input?.refersTo
                if (input != null && writtenDecl != null) {
                    previousWrites[writtenDecl]?.lastOrNull()?.let { input.addPrevDFG(it) }

                    // TODO: Do we want to have a flow from the input back to the input? One test
                    // says yes but I'm not sure. If not, comment in the following line:
                    // currentNode.removeNextDFG(input)

                    // Add the whole node to the list of previous write nodes in this path. This
                    // prevents some weird circular dependencies.
                    previousWrites.computeIfAbsent(writtenDecl, ::mutableListOf).add(currentNode)
                }
            } else if (
                currentNode is Assignment &&
                    (currentNode.target as? DeclaredReferenceExpression)?.refersTo != null
            ) {
                // We write to the target => value flows to target
                writtenDecl = (currentNode.target as? DeclaredReferenceExpression)?.refersTo!!

                previousWrites
                    .computeIfAbsent(writtenDecl, ::mutableListOf)
                    .add(currentNode.target as Node)
                currentWritten = currentNode.target as Node

                currentNode.value?.let {
                    (currentNode.target as DeclaredReferenceExpression).addPrevDFG(it)
                }
            } else if ((currentNode as? DeclaredReferenceExpression)?.access == AccessValues.READ) {
                // Get previous write
                previousWrites[currentNode.refersTo]?.lastOrNull()?.let {
                    currentNode.addPrevDFG(it)
                }
            }

            if (
                currentNode is ForStatement ||
                    currentNode is WhileStatement ||
                    currentNode is ForEachStatement ||
                    currentNode is DoStatement ||
                    currentNode is GotoStatement
            ) {
                val state = loopPoints.computeIfAbsent(currentNode) { mutableSetOf() }
                if (previousWrites in state) {
                    continue
                }
                state.add(previousWrites)
            }

            if (
                writtenDecl == null ||
                    previousWrites[writtenDecl]!!.filter { it == currentWritten }.size < 2
            ) {
                currentNode.nextEOG.forEach {
                    val newPair = Pair(it, copyMap(previousWrites))
                    if (newPair !in worklist) worklist.add(newPair)
                }
            }
        }
    }

    private fun copyMap(
        map: Map<Declaration, MutableList<Node>>
    ): MutableMap<Declaration, MutableList<Node>> {
        val result = mutableMapOf<Declaration, MutableList<Node>>()
        for ((k, v) in map) {
            result[k] = mutableListOf()
            result[k]?.addAll(v)
        }
        return result
    }
}
