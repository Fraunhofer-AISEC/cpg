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
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
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
     * We perform the actions for each function.
     *
     * @param node every node in the TranslationResult
     */
    protected fun handle(node: Node) {
        if (node is FunctionDeclaration) {
            clearFlowsOfVariableDeclarations(node)
            handleFunction(node)
        }
    }

    /**
     * Removes all the incoming and outgoing DFG edges for each variable declaration in the function
     * [node].
     */
    private fun clearFlowsOfVariableDeclarations(node: FunctionDeclaration) {
        for (varDecl in node.variables) {
            varDecl.clearPrevDFG()
            varDecl.clearNextDFG()
        }
    }

    /**
     * Performs a forward analysis through the EOG to collect all possible writes to a variable and
     * adds them to the DFG edges to the read operations of that variable. We differentiate between
     * the flows based on the following types of statements/expressions:
     * - VariableDeclaration with an initializer
     * - Unary operators ++ and --
     * - Assignments of the form "variable = rhs"
     * - Assignments with an operation e.g. of the form "variable += rhs"
     * - Read operations on a variable
     */
    private fun handleFunction(node: FunctionDeclaration) {
        // The list of nodes that we have to consider and the last write operations to the different
        // variables.
        val worklist =
            mutableListOf<Pair<Node, MutableMap<Declaration, MutableList<Node>>>>(
                Pair(node, mutableMapOf())
            )

        // Different points which could be the cause of a loop (in a non-broken program). We
        // consider ForStatements, WhileStatements, ForEachStatements, DoStatements and
        // GotoStatements
        val loopPoints = mutableMapOf<Node, MutableSet<Map<Declaration, MutableList<Node>>>>()

        // Iterate through the worklist
        while (worklist.isNotEmpty()) {
            // The node we will analyze now and the map of the last write statements to a variable.
            val (currentNode, previousWrites) = worklist.removeFirst()
            // We will set this if we write to a variable
            var writtenDecl: Declaration? = null
            var currentWritten = currentNode

            if ((currentNode as? VariableDeclaration)?.initializer != null) {
                // A variable declaration with an initializer => The initializer flows to the
                // declaration.
                currentNode.addPrevDFG(currentNode.initializer!!)

                // We wrote something to this variable declaration
                writtenDecl = currentNode

                // Add the node to the list of previous write nodes in this path
                previousWrites[currentNode] = mutableListOf(currentNode)
            } else if (
                currentNode is UnaryOperator &&
                    (currentNode.operatorCode == "++" || currentNode.operatorCode == "--")
            ) {
                // Increment or decrement => Add the prevWrite of the input to the input. After the
                // operation, the prevWrite of the input's variable is this node.
                val input = currentNode.input as? DeclaredReferenceExpression
                // We write to the variable in the input
                writtenDecl = input?.refersTo
                if (input != null && writtenDecl != null) {
                    previousWrites[writtenDecl]?.lastOrNull()?.let { input.addPrevDFG(it) }

                    // TODO: Do we want to have a flow from the input back to the input? One test
                    // says yes but I think this will only cause problems. If we really want it,
                    // comment out the following line:
                    currentNode.removeNextDFG(input)

                    // Add the whole node to the list of previous write nodes in this path. This
                    // prevents some weird circular dependencies.
                    previousWrites.computeIfAbsent(writtenDecl, ::mutableListOf).add(currentNode)
                }
            } else if (
                currentNode is BinaryOperator &&
                    currentNode.operatorCode == "=" &&
                    (currentNode.lhs as? DeclaredReferenceExpression)?.refersTo != null
            ) {
                // We write to the target => the rhs flows to the lhs
                currentNode.rhs?.let { currentNode.lhs.addPrevDFG(it) }

                // Only the lhs is the last write statement here and the variable which is written
                // to.
                writtenDecl = (currentNode.lhs as DeclaredReferenceExpression).refersTo!!
                previousWrites.computeIfAbsent(writtenDecl, ::mutableListOf).add(currentNode.lhs)
                currentWritten = currentNode.lhs
            } else if (
                currentNode is BinaryOperator &&
                    currentNode.operatorCode in BinaryOperator.compoundOperators &&
                    (currentNode.lhs as? DeclaredReferenceExpression)?.refersTo != null
            ) {
                // We write to the lhs, but it also serves as an input => We first get all previous
                // writes to the lhs and then add the flow from lhs and rhs to the current node.

                // The write operation goes to the variable in the lhs
                writtenDecl = (currentNode.lhs as? DeclaredReferenceExpression)?.refersTo!!

                // Data flows from the last writes to the lhs variable to this node
                previousWrites[writtenDecl]?.lastOrNull()?.let { currentNode.lhs.addPrevDFG(it) }
                currentNode.addPrevDFG(currentNode.lhs)

                // Data flows from whatever is the rhs to this node
                currentNode.rhs?.let { currentNode.addPrevDFG(it) }

                // TODO: Similar to the ++ case: Should the DFG edge go back to the reference? I
                // think it shouldn't. If it should, add the following statement:
                // currentNode.lhs.addPrevDFG(currentNode)

                // The whole current node is the place of the last update, not (only) the lhs!
                previousWrites.computeIfAbsent(writtenDecl, ::mutableListOf).add(currentNode)
                currentWritten = currentNode
            } else if ((currentNode as? DeclaredReferenceExpression)?.access == AccessValues.READ) {
                // We only read the variable => Get previous write which have been collected in the
                // other steps
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
                // Loop detection: This is a point which could serve as a loop, so we check all
                // states which we have seen before in this place.
                val state = loopPoints.computeIfAbsent(currentNode) { mutableSetOf() }
                if (previousWrites in state) {
                    // The current state of last write operations has already been seen before =>
                    // Nothing new => Do not add the next eog steps!
                    continue
                }
                // Add the current state for future loop detections.
                state.add(previousWrites)
            }

            if (
                writtenDecl == null ||
                    previousWrites[writtenDecl]!!.filter { it == currentWritten }.size < 2
            ) {
                // If we wrote something, we want to make sure that we haven't seen this instruction
                // too often before otherwise there's a loop.
                // We add all the next steps in the eog to the worklist unless the exact same thing
                // is already included in the list.
                currentNode.nextEOG.forEach {
                    val newPair = Pair(it, copyMap(previousWrites))
                    if (newPair !in worklist) worklist.add(newPair)
                }
            }
        }
    }

    /** Copies the map */
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
