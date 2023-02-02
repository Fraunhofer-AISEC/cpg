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
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.IterativeGraphWalker
import de.fraunhofer.aisec.cpg.passes.order.DependsOn

/**
 * This pass determines the data flows of DeclaredReferenceExpressions which refer to a
 * VariableDeclaration (not a field) while considering the control flow of a function. After this
 * path, only such data flows are left which can occur when following the control flow (in terms of
 * the EOG) of the program.
 */
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
     * We perform the actions for each [FunctionDeclaration].
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

        val alreadyProcessed = mutableSetOf<Pair<Node, Map<Declaration, Node>>>()

        // Different points which could be the cause of a loop (in a non-broken program). We
        // consider ForStatements, WhileStatements, ForEachStatements, DoStatements and
        // GotoStatements
        val loopPoints = mutableMapOf<Node, MutableMap<Declaration, MutableSet<Node>>>()

        // Iterate through the worklist
        while (worklist.isNotEmpty()) {
            // The node we will analyze now and the map of the last write statements to a variable.
            val (currentNode, previousWrites) = worklist.removeFirst()
            if (
                !alreadyProcessed.add(
                    Pair(currentNode, previousWrites.mapValues { (_, v) -> v.last() })
                )
            ) {
                // The entry did already exist. This means that the changes won't have any effects
                // and we don't have to run the loop.
                continue
            }
            // We will set this if we write to a variable
            var writtenDecl: Declaration? = null
            var currentWritten = currentNode

            val initializer = (currentNode as? VariableDeclaration)?.initializer
            if (initializer != null) {
                // A variable declaration with an initializer => The initializer flows to the
                // declaration.
                currentNode.addPrevDFG(initializer)

                // We wrote something to this variable declaration
                writtenDecl = currentNode

                // Add the node to the list of previous write nodes in this path
                previousWrites[currentNode] = mutableListOf(currentNode)
            } else if (isIncOrDec(currentNode)) {
                // Increment or decrement => Add the prevWrite of the input to the input. After the
                // operation, the prevWrite of the input's variable is this node.
                val input = (currentNode as UnaryOperator).input as DeclaredReferenceExpression
                // We write to the variable in the input
                writtenDecl = input.refersTo

                if (writtenDecl != null) {
                    previousWrites[writtenDecl]?.lastOrNull()?.let { input.addPrevDFG(it) }

                    // TODO: Do we want to have a flow from the input back to the input? This can
                    //  cause problems if the DFG is not iterated through appropriately. The
                    //  following line would remove it:
                    // currentNode.removeNextDFG(input)

                    // Add the whole node to the list of previous write nodes in this path. This
                    // prevents some weird circular dependencies.
                    previousWrites
                        .computeIfAbsent(writtenDecl, ::mutableListOf)
                        .add(currentNode.input)
                    currentWritten = currentNode.input
                }
            } else if (isSimpleAssignment(currentNode)) {
                // We write to the target => the rhs flows to the lhs
                (currentNode as BinaryOperator).rhs?.let { currentNode.lhs.addPrevDFG(it) }

                // Only the lhs is the last write statement here and the variable which is written
                // to.
                writtenDecl = (currentNode.lhs as DeclaredReferenceExpression).refersTo

                if (writtenDecl != null) {
                    previousWrites
                        .computeIfAbsent(writtenDecl, ::mutableListOf)
                        .add(currentNode.lhs)
                    currentWritten = currentNode.lhs
                }
            } else if (isCompoundAssignment(currentNode)) {
                // We write to the lhs, but it also serves as an input => We first get all previous
                // writes to the lhs and then add the flow from lhs and rhs to the current node.

                // The write operation goes to the variable in the lhs
                writtenDecl =
                    ((currentNode as BinaryOperator).lhs as? DeclaredReferenceExpression)?.refersTo

                if (writtenDecl != null) {
                    // Data flows from the last writes to the lhs variable to this node
                    previousWrites[writtenDecl]?.lastOrNull()?.let {
                        currentNode.lhs.addPrevDFG(it)
                    }
                    currentNode.addPrevDFG(currentNode.lhs)

                    // Data flows from whatever is the rhs to this node
                    currentNode.rhs?.let { currentNode.addPrevDFG(it) }

                    // TODO: Similar to the ++ case: Should the DFG edge go back to the reference?
                    //  If it shouldn't, remove the following statement:
                    currentNode.lhs.addPrevDFG(currentNode)

                    // The whole current node is the place of the last update, not (only) the lhs!
                    previousWrites
                        .computeIfAbsent(writtenDecl, ::mutableListOf)
                        .add(currentNode.lhs)
                    currentWritten = currentNode.lhs
                }
            } else if ((currentNode as? DeclaredReferenceExpression)?.access == AccessValues.READ) {
                // We only read the variable => Get previous write which have been collected in the
                // other steps
                previousWrites[currentNode.refersTo]?.lastOrNull()?.let {
                    currentNode.addPrevDFG(it)
                }
            } else if (currentNode is ForEachStatement) {
                // The VariableDeclaration in the ForEachStatement doesn't have an initializer, so
                // the "normal" case won't work. We handle this case separately here...

                // This is what we write to the declaration
                val iterable = currentNode.iterable as? Expression

                // We wrote something to this variable declaration
                writtenDecl =
                    (currentNode.variable as? DeclarationStatement)?.singleDeclaration
                        as? VariableDeclaration

                writtenDecl?.let { wd ->
                    iterable?.let { wd.addPrevDFG(it) }
                    // Add the variable declaration to the list of previous write nodes in this path
                    previousWrites[wd] = mutableListOf(wd)
                }
            }

            // Check for loops: No loop statement with the same state as before and no write which
            // is already in the current chain of writes too often (=twice).
            if (
                !loopDetection(currentNode, writtenDecl, currentWritten, previousWrites, loopPoints)
            ) {
                // We add all the next steps in the eog to the worklist unless the exact same thing
                // is already included in the list.
                currentNode.nextEOGEdges
                    .filter { it.getProperty(Properties.UNREACHABLE) != true }
                    .map { it.end }
                    .forEach {
                        val newPair = Pair(it, copyMap(previousWrites))
                        if (!worklistHasSimilarPair(worklist, alreadyProcessed, newPair))
                            worklist.add(newPair)
                    }
            }
        }
    }

    /**
     * Determines if there's an item in the [worklist] which has the same last write for each
     * declaration in the [newPair]. If this is the case, we can ignore it because all that changed
     * was the path through the EOG to reach this state but apparently, all the writes in the
     * different branches are obsoleted by one common write access which happens afterwards.
     */
    private fun worklistHasSimilarPair(
        worklist: MutableList<Pair<Node, MutableMap<Declaration, MutableList<Node>>>>,
        alreadyProcessed: MutableSet<Pair<Node, Map<Declaration, Node>>>,
        newPair: Pair<Node, MutableMap<Declaration, MutableList<Node>>>
    ): Boolean {
        // We collect all states in the worklist which are only a subset of the new pair. We will
        // remove them to avoid unnecessary computations.
        val subsets = mutableSetOf<Pair<Node, MutableMap<Declaration, MutableList<Node>>>>()
        val newPairLastMap = newPair.second.mapValues { (_, v) -> v.last() }
        for (existingPair in worklist) {
            if (existingPair.first == newPair.first) {
                // The next nodes match. Now check the last writes for each declaration.
                var allWritesMatch = true
                var allExistingWritesMatch = true
                for ((lastWriteDecl, lastWriteList) in newPairLastMap) {

                    // We ignore FieldDeclarations because we cannot be sure how interprocedural
                    // data flows affect the field. Handling them in the state would only blow up
                    // the number of paths unnecessarily.
                    if (lastWriteDecl is FieldDeclaration) continue

                    // Will we generate the same "prev DFG" with the item that is already in the
                    // list?
                    allWritesMatch =
                        allWritesMatch &&
                            existingPair.second[lastWriteDecl]?.last() == lastWriteList
                    // All last writes which exist in the "existing pair" match but we have new
                    // declarations in the current one
                    allExistingWritesMatch =
                        allExistingWritesMatch &&
                            (lastWriteDecl !in existingPair.second ||
                                existingPair.second[lastWriteDecl]?.last() == lastWriteList)
                }
                // We found a matching pair in the worklist? Done. Otherwise, maybe there's another
                // pair...
                if (allWritesMatch) return true
                // The new state is a superset of the old one? We delete the old one.
                if (allExistingWritesMatch) {
                    subsets.add(existingPair)
                }
            }
        }

        // Check the "subsets" again, and add the missing declarations
        if (subsets.isNotEmpty()) {
            for (s in subsets) {
                for ((k, v) in newPair.second) {
                    if (k !in s.second) {
                        s.second[k] = v
                    }
                }
            }
            return true // We cover it in the respective subsets, so do not add this state again.
        }

        return false
    }

    /**
     * Checks if the node performs an operation and an assignment at the same time e.g. with the
     * operators +=, -=, *=, ...
     */
    private fun isCompoundAssignment(currentNode: Node) =
        currentNode is BinaryOperator &&
            currentNode.operatorCode in BinaryOperator.compoundOperators &&
            (currentNode.lhs as? DeclaredReferenceExpression)?.refersTo != null

    /** Checks if the node is a simple assignment of the form `var = ...` */
    private fun isSimpleAssignment(currentNode: Node) =
        currentNode is BinaryOperator &&
            currentNode.operatorCode == "=" &&
            (currentNode.lhs as? DeclaredReferenceExpression)?.refersTo != null

    /** Checks if the node is an increment or decrement operator (e.g. i++, i--, ++i, --i) */
    private fun isIncOrDec(currentNode: Node) =
        currentNode is UnaryOperator &&
            (currentNode.operatorCode == "++" || currentNode.operatorCode == "--") &&
            (currentNode.input as? DeclaredReferenceExpression)?.refersTo != null

    /**
     * Determines if the [currentNode] is a loop point has already been visited with the exact same
     * state before. Changes the state saved in the [loopPoints] by adding the current
     * [previousWrites].
     *
     * @return true if a loop was detected, false otherwise
     */
    private fun loopDetection(
        currentNode: Node,
        writtenDecl: Declaration?,
        currentWritten: Node,
        previousWrites: MutableMap<Declaration, MutableList<Node>>,
        loopPoints: MutableMap<Node, MutableMap<Declaration, MutableSet<Node>>>
    ): Boolean {
        if (
            currentNode is ForStatement ||
                currentNode is WhileStatement ||
                currentNode is ForEachStatement ||
                currentNode is DoStatement ||
                currentNode is GotoStatement ||
                currentNode is ContinueStatement
        ) {
            // Loop detection: This is a point which could serve as a loop, so we check all
            // states which we have seen before in this place.
            val state = loopPoints.computeIfAbsent(currentNode) { mutableMapOf() }
            if (
                previousWrites.all { (decl, prevs) ->
                    decl in state && prevs.last() in state[decl]!!
                }
            ) {
                // The current state of last write operations has already been seen before =>
                // Nothing new => Do not add the next eog steps!
                return true
            }
            // Add the current state for future loop detections.
            previousWrites.forEach { (decl, prevs) ->
                state.computeIfAbsent(decl, ::mutableSetOf).add(prevs.last())
            }
        }
        return writtenDecl != null &&
            previousWrites[writtenDecl]!!.filter { it == currentWritten }.size >= 2
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
