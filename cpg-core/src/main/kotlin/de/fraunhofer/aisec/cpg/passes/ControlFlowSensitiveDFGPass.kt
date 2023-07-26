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

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.helpers.*
import de.fraunhofer.aisec.cpg.passes.order.DependsOn

/**
 * This pass determines the data flows of DeclaredReferenceExpressions which refer to a
 * VariableDeclaration (not a field) while considering the control flow of a function. After this
 * path, only such data flows are left which can occur when following the control flow (in terms of
 * the EOG) of the program.
 */
@DependsOn(EvaluationOrderGraphPass::class)
@DependsOn(DFGPass::class)
open class ControlFlowSensitiveDFGPass(ctx: TranslationContext) : TranslationUnitPass(ctx) {

    override fun cleanup() {
        // Nothing to do
    }

    override fun accept(tu: TranslationUnitDeclaration) {
        tu.functions.forEach(::handle)
    }

    /**
     * We perform the actions for each [FunctionDeclaration].
     *
     * @param node every node in the TranslationResult
     */
    protected fun handle(node: Node) {
        if (node is FunctionDeclaration) {
            clearFlowsOfVariableDeclarations(node)
            val startState = DFGPassState<Set<Node>>()
            startState.declarationsState.push(node, PowersetLattice(setOf()))
            val finalState =
                iterateEOG(node.nextEOGEdges, startState, ::transfer) as? DFGPassState ?: return

            removeUnreachableImplicitReturnStatement(
                node,
                finalState.returnStatements.values.flatMap {
                    it.elements.filterIsInstance<ReturnStatement>()
                }
            )

            for ((key, value) in finalState.generalState) {
                key.addAllPrevDFG(
                    value.elements.filterNot { it is VariableDeclaration && key == it }
                )
            }
        }
    }

    /**
     * Removes all the incoming and outgoing DFG edges for each variable declaration in the block of
     * code [node].
     */
    protected fun clearFlowsOfVariableDeclarations(node: Node) {
        for (varDecl in node.variables) {
            varDecl.clearPrevDFG()
            varDecl.clearNextDFG()
        }
    }

    /**
     * Computes the previous write access of [currentEdge].end if it is a
     * [DeclaredReferenceExpression] or [ValueDeclaration] based on the given [state] (which maps
     * all variables to its last write instruction). It also updates the [state] if
     * [currentEdge].end performs a write-operation to a variable.
     *
     * It further determines unnecessary implicit return statement which are added by some frontends
     * even if every path reaching this point already contains a return statement.
     */
    protected fun transfer(
        currentEdge: PropertyEdge<Node>,
        state: State<Node, Set<Node>>,
        worklist: Worklist<PropertyEdge<Node>, Node, Set<Node>>
    ): State<Node, Set<Node>> {
        // We will set this if we write to a variable
        val writtenDecl: Declaration?
        val currentNode = currentEdge.end

        val doubleState = state as DFGPassState

        val initializer = (currentNode as? VariableDeclaration)?.initializer
        if (initializer != null) {
            // A variable declaration with an initializer => The initializer flows to the
            // declaration.
            // We also wrote something to this variable declaration
            state.push(currentNode, PowersetLattice(setOf(initializer)))

            doubleState.pushToDeclarationsState(currentNode, PowersetLattice(setOf(currentNode)))
        } else if (currentNode is AssignExpression) {
            // It's an assignment which can have one or multiple things on the lhs and on the
            // rhs. The lhs could be a declaration or a reference (or multiple of these things).
            // The rhs can be anything. The rhs flows to the respective lhs. To identify the
            // correct mapping, we use the "assignments" property which already searches for us.
            currentNode.assignments.forEach { assignment ->
                // This was the last write to the respective declaration.
                (assignment.target as? Declaration
                        ?: (assignment.target as? DeclaredReferenceExpression)?.refersTo)
                    ?.let {
                        doubleState.declarationsState[it] =
                            PowersetLattice(setOf(assignment.target as Node))
                    }
            }
        } else if (isIncOrDec(currentNode)) {
            // Increment or decrement => Add the prevWrite of the input to the input. After the
            // operation, the prevWrite of the input's variable is this node.
            val input = (currentNode as UnaryOperator).input as DeclaredReferenceExpression
            // We write to the variable in the input
            writtenDecl = input.refersTo

            if (writtenDecl != null) {
                state.push(input, doubleState.declarationsState[writtenDecl])
                doubleState.declarationsState[writtenDecl] = PowersetLattice(setOf(input))
            }
        } else if (isSimpleAssignment(currentNode)) {
            // Only the lhs is the last write statement here and the variable which is written
            // to.
            writtenDecl =
                ((currentNode as BinaryOperator).lhs as DeclaredReferenceExpression).refersTo

            if (writtenDecl != null) {
                doubleState.declarationsState[writtenDecl] = PowersetLattice(setOf(currentNode.lhs))
            }
        } else if (isCompoundAssignment(currentNode)) {
            // We write to the lhs, but it also serves as an input => We first get all previous
            // writes to the lhs and then add the flow from lhs and rhs to the current node.

            // The write operation goes to the variable in the lhs
            writtenDecl =
                ((currentNode as BinaryOperator).lhs as? DeclaredReferenceExpression)?.refersTo

            if (writtenDecl != null) {
                // Data flows from the last writes to the lhs variable to this node
                state.push(currentNode.lhs, doubleState.declarationsState[writtenDecl])

                // The whole current node is the place of the last update, not (only) the lhs!
                doubleState.declarationsState[writtenDecl] = PowersetLattice(setOf(currentNode.lhs))
            }
        } else if (
            (currentNode as? DeclaredReferenceExpression)?.access == AccessValues.READ &&
                currentNode.refersTo is VariableDeclaration
        ) {
            // We can only find a change if there's a state for the variable
            doubleState.declarationsState[currentNode.refersTo]?.let {
                // We only read the variable => Get previous write which have been collected in
                // the other steps
                state.push(currentNode, it)
            }
        } else if (currentNode is ForEachStatement && currentNode.variable != null) {
            // The VariableDeclaration in the ForEachStatement doesn't have an initializer, so
            // the "normal" case won't work. We handle this case separately here...
            // This is what we write to the declaration
            val iterable = currentNode.iterable as? Expression

            val writtenTo =
                when (currentNode.variable) {
                    is DeclarationStatement ->
                        (currentNode.variable as DeclarationStatement).singleDeclaration
                    else -> currentNode.variable
                }

            // We wrote something to this variable declaration
            writtenDecl =
                when (writtenTo) {
                    is Declaration -> writtenTo
                    is DeclaredReferenceExpression -> writtenTo.refersTo
                    else -> {
                        log.error(
                            "The variable of type ${writtenTo?.javaClass} is not yet supported in the foreach loop"
                        )
                        null
                    }
                }

            if (writtenTo is DeclaredReferenceExpression) {
                // This is a special case: We add the nextEOGEdge which goes out of the loop but
                // with the old previousWrites map.
                val nodesOutsideTheLoop =
                    currentNode.nextEOGEdges.filter {
                        it.getProperty(Properties.UNREACHABLE) != true &&
                            it.end != currentNode.statement &&
                            it.end !in currentNode.statement.allChildren<Node>()
                    }
                nodesOutsideTheLoop.forEach { worklist.push(it, state.duplicate()) }
            }

            iterable?.let {
                writtenTo?.let {
                    state.push(writtenTo, PowersetLattice(setOf(iterable)))
                    // Add the variable declaration (or the reference) to the list of previous
                    // write nodes in this path
                    state.declarationsState[writtenDecl] = PowersetLattice(setOf(writtenTo))
                }
            }
        } else if (currentNode is FunctionDeclaration) {
            // We have to add the parameters
            currentNode.parameters.forEach {
                doubleState.pushToDeclarationsState(it, PowersetLattice(setOf(it)))
            }
        } else if (currentNode is ReturnStatement) {
            doubleState.returnStatements.push(currentNode, PowersetLattice(setOf(currentNode)))
        } else {
            doubleState.declarationsState.push(
                currentNode,
                doubleState.declarationsState[currentEdge.start]
            )
        }
        return state
    }

    /**
     * Checks if the node performs an operation and an assignment at the same time e.g. with the
     * operators +=, -=, *=, ...
     */
    protected fun isCompoundAssignment(currentNode: Node) =
        currentNode is BinaryOperator &&
            currentNode.operatorCode in
                (currentNode.language?.compoundAssignmentOperators ?: setOf()) &&
            (currentNode.lhs as? DeclaredReferenceExpression)?.refersTo != null

    /** Checks if the node is a simple assignment of the form `var = ...` */
    protected fun isSimpleAssignment(currentNode: Node) =
        currentNode is BinaryOperator &&
            currentNode.operatorCode == "=" &&
            (currentNode.lhs as? DeclaredReferenceExpression)?.refersTo != null

    /** Checks if the node is an increment or decrement operator (e.g. i++, i--, ++i, --i) */
    protected fun isIncOrDec(currentNode: Node) =
        currentNode is UnaryOperator &&
            (currentNode.operatorCode == "++" || currentNode.operatorCode == "--") &&
            (currentNode.input as? DeclaredReferenceExpression)?.refersTo != null

    /**
     * Removes the DFG edges for a potential implicit return statement if it is not in
     * [reachableReturnStatements].
     */
    protected fun removeUnreachableImplicitReturnStatement(
        node: Node,
        reachableReturnStatements: Collection<ReturnStatement>
    ) {
        val lastStatement =
            ((node as? FunctionDeclaration)?.body as? CompoundStatement)?.statements?.lastOrNull()
        if (
            lastStatement is ReturnStatement &&
                lastStatement.isImplicit &&
                lastStatement !in reachableReturnStatements
        )
            lastStatement.removeNextDFG(node)
    }

    /**
     * A state which actually holds a state for all nodes, one only for declarations and one for
     * ReturnStatements.
     */
    protected class DFGPassState<V>(
        /**
         * A mapping of a [Node] to its [LatticeElement]. The keys of this state will later get the
         * DFG edges from the value!
         */
        var generalState: State<Node, V> = State(),
        /**
         * It's main purpose is to store the most recent mapping of a [Declaration] to its
         * [LatticeElement]. However, it is also used to figure out if we have to continue with the
         * iteration (something in the declarationState has changed) which is why we store all nodes
         * here. However, since we never use them except from determining if we changed something,
         * it won't affect the result.
         */
        var declarationsState: State<Node, V> = State(),
        /** The [returnStatements] which are reachable. */
        var returnStatements: State<Node, V> = State()
    ) : State<Node, V>() {
        override fun duplicate(): DFGPassState<V> {
            return DFGPassState(generalState.duplicate(), declarationsState.duplicate())
        }

        override fun get(key: Node?): LatticeElement<V>? {
            return generalState[key] ?: declarationsState[key]
        }

        override fun lub(other: State<Node, V>): Pair<State<Node, V>, Boolean> {
            return if (other is DFGPassState) {
                val (_, generalUpdate) = generalState.lub(other.generalState)
                val (_, declUpdate) = declarationsState.lub(other.declarationsState)
                Pair(this, generalUpdate || declUpdate)
            } else {
                val (_, generalUpdate) = generalState.lub(other)
                Pair(this, generalUpdate)
            }
        }

        override fun needsUpdate(other: State<Node, V>): Boolean {
            return if (other is DFGPassState) {
                generalState.needsUpdate(other.generalState) ||
                    declarationsState.needsUpdate(other.declarationsState)
            } else {
                generalState.needsUpdate(other)
            }
        }

        override fun push(newNode: Node, newLatticeElement: LatticeElement<V>?): Boolean {
            return generalState.push(newNode, newLatticeElement)
        }

        /** Pushes the [newNode] and its [newLatticeElement] to the [declarationsState]. */
        fun pushToDeclarationsState(
            newNode: Declaration,
            newLatticeElement: LatticeElement<V>?
        ): Boolean {
            return declarationsState.push(newNode, newLatticeElement)
        }
    }
}
