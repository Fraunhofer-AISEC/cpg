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
import de.fraunhofer.aisec.cpg.graph.edge.*
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.ForEachStatement
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.helpers.*
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import kotlin.collections.set
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * This pass determines the data flows of References which refer to a VariableDeclaration (not a
 * field) while considering the control flow of a function. After this path, only such data flows
 * are left which can occur when following the control flow (in terms of the EOG) of the program.
 */
@OptIn(ExperimentalContracts::class)
@DependsOn(EvaluationOrderGraphPass::class)
@DependsOn(DFGPass::class)
open class ControlFlowSensitiveDFGPass(ctx: TranslationContext) : EOGStarterPass(ctx) {

    class Configuration(
        /**
         * This specifies the maximum complexity (as calculated per
         * [Statement.cyclomaticComplexity]) a [FunctionDeclaration] must have in order to be
         * considered.
         */
        var maxComplexity: Int? = null,
        val parallel: Boolean = true
    ) : PassConfiguration()

    override fun cleanup() {
        // Nothing to do
    }

    /** We perform the actions for each [FunctionDeclaration]. */
    override fun accept(node: Node) {
        // For now, we only execute this for function declarations, we will support all EOG starters
        // in the future.
        if (node !is FunctionDeclaration) {
            return
        }

        // Skip empty functions
        if (node.body == null) {
            return
        }

        // Calculate the complexity of the function and see, if it exceeds our threshold
        val max = passConfig<Configuration>()?.maxComplexity
        val c = node.body?.cyclomaticComplexity ?: 0
        if (max != null && c > max) {
            log.info(
                "Ignoring function ${node.name} because its complexity (${c}) is greater than the configured maximum (${max})"
            )
            return
        }

        log.trace("Handling {} (complexity: {})", node.name, c)

        clearFlowsOfVariableDeclarations(node)
        val startState = DFGPassState<Set<Node>>()

        startState.declarationsState.push(node, PowersetLattice(identitySetOf()))
        node.parameters.forEach {
            startState.declarationsState.push(it, PowersetLattice(identitySetOf(it)))
        }
        val finalState =
            iterateEOG(node.nextEOGEdges, startState, ::transfer) as? DFGPassState ?: return

        removeUnreachableImplicitReturnStatement(
            node,
            finalState.returnStatements.values.flatMap {
                it.elements.filterIsInstance<ReturnStatement>()
            }
        )

        for ((key, value) in finalState.generalState) {
            if (key is TupleDeclaration) {
                // We need a little hack for tuple statements to set the index. We have the
                // outer part (i.e., the tuple) here, but we generate the DFG edges to the
                // elements. We have the indices here, so it's amazingly easy to find the partial
                // target.
                key.elements.forEach { element ->
                    element.addAllPrevDFG(
                        value.elements.filterNot {
                            (it is VariableDeclaration || it is ParameterDeclaration) && key == it
                        },
                        // mutableMapOf(Properties.INDEX to i)
                        granularity = partial(element)
                    )
                }
            } else {
                value.elements.forEach {
                    if ((it is VariableDeclaration || it is ParameterDeclaration) && key == it) {
                        // Nothing to do
                    } else if (
                        Triple(it, key, true) in edgePropertiesMap &&
                            edgePropertiesMap[Triple(it, key, true)] is CallingContext
                    ) {
                        key.addPrevDFG(
                            it,
                            callingContext =
                                (edgePropertiesMap[Triple(it, key, true)] as? CallingContext)
                        )
                    } else {
                        val granularity =
                            if (it is PointerReference) PointerDataflowGranularity() else default()
                        key.addPrevDFG(it, granularity)
                    }
                }
            }
        }
    }

    /**
     * Checks if there's an entry in [edgePropertiesMap] with key `(x, null, <any>)` where `x` is in
     * [from] and, if so, adds an entry with key `(x, to, true)` and the same value
     */
    protected fun findAndSetProperties(from: Set<Node>, to: Node) {
        edgePropertiesMap
            .filter { entry ->
                entry.key.first in from && (to as HasAliases).aliases.any { it == entry.key.second }
            }
            .forEach { edgePropertiesMap[Triple(it.key.first, to, true)] = it.value }
    }

    /**
     * Removes all the incoming and outgoing DFG edges for each variable declaration in the block of
     * code [node].
     */
    protected fun clearFlowsOfVariableDeclarations(node: Node) {
        // for (varDecl in node.variables.filter { it !is FieldDeclaration && !it.isGlobal }) {
        for (varDecl in node.variables /*.filter { it !is FieldDeclaration }*/) {
            varDecl.clearPrevDFG()
            varDecl.clearNextDFG()
        }
        val allChildrenOfFunction = node.allChildren<Node>()
        for (varDecl in node.parameters) {
            // Clear only prev and next inside this function!
            varDecl.nextDFG
                .filter { it in allChildrenOfFunction }
                .forEach { varDecl.removeNextDFG(it) }
            varDecl.prevDFG
                .filter { it in allChildrenOfFunction }
                .forEach { varDecl.removePrevDFG(it) }
        }
    }

    /**
     * Computes the previous write access of [currentEdge].end if it is a [Reference] or
     * [ValueDeclaration] based on the given [state] (which maps all variables to its last write
     * instruction). It also updates the [state] if [currentEdge].end performs a write-operation to
     * a variable.
     *
     * It further determines unnecessary implicit return statement which are added by some frontends
     * even if every path reaching this point already contains a return statement.
     */
    protected open fun transfer(
        currentEdge: PropertyEdge<Node>,
        state: State<Node, Set<Node>>,
        worklist: Worklist<PropertyEdge<Node>, Node, Set<Node>>
    ): State<Node, Set<Node>> {
        println("In transfer")
        // We will set this if we write to a variable
        val writtenDeclaration: Declaration?
        val currentNode = currentEdge.end

        val doubleState = state as DFGPassState

        if (currentNode is VariableDeclaration) {
            val initializer = currentNode.initializer
            if (initializer != null) {
                // A variable declaration with an initializer => The initializer flows to the
                // declaration. This also affects tuples. We split it up later.
                state.push(currentNode, PowersetLattice(identitySetOf(initializer)))
            }

            if (currentNode is TupleDeclaration) {
                // For a tuple declaration, we write the elements in this statement. We do not
                // really care about the tuple when using the elements subsequently.
                currentNode.elements.forEach {
                    doubleState.pushToDeclarationsState(it, PowersetLattice(identitySetOf(it)))
                }
            } else {
                // We also wrote something to this variable declaration here.
                doubleState.pushToDeclarationsState(
                    currentNode,
                    PowersetLattice(identitySetOf(currentNode))
                )
            }
        } else if (currentNode is MemberExpression && currentNode.access == AccessValues.WRITE) {
            // already set in DFG pass, because otherwise we cannot set the field property
            // state.push(currentNode.base, PowersetLattice(identitySetOf(currentNode)))

            writtenDeclaration = (currentNode.base as? Reference)?.refersTo

            if (writtenDeclaration != null) {
                // we also want to set the last write to our base here.
                doubleState.declarationsState[writtenDeclaration] =
                    PowersetLattice(identitySetOf(currentNode.base))

                // Update the state identifier of this node, so that the data flows to later member
                // expressions accessing the same object/field combination.
                doubleState.declarationsState[currentNode.objectIdentifier()] =
                    PowersetLattice(identitySetOf(currentNode))
            }
        } else if (currentNode is MemberExpression && currentNode.access == AccessValues.READ) {
            writtenDeclaration = (currentNode.base as? Reference)?.refersTo
            val fieldDeclaration = currentNode.refersTo

            if (writtenDeclaration != null && fieldDeclaration != null) {
                // We do an ugly hack here: We store a (unique) hash out of field declaration and
                // the variable declaration in the declaration state so that we can retrieve it
                // later for READ accesses.
                val declState = doubleState.declarationsState[currentNode.objectIdentifier()]
                if (declState != null) {
                    // We check if we have something relevant for this node (because there was an
                    // entry for the incoming edge) in the edgePropertiesMap and, if so, we generate
                    // a dedicated entry for the edge between declState and currentNode.
                    findAndSetProperties(declState.elements, currentNode)
                    state.push(currentNode, declState)
                } else {
                    // If we do not have a stored state of our object+field, we can use the field
                    // declaration. This will help us follow a data flow from field initializers (if
                    // they exist in the language)
                    state.push(currentNode, PowersetLattice(identitySetOf(fieldDeclaration)))
                }
            }
        } else if (
            currentNode is MemberExpression && currentNode.access == AccessValues.READWRITE
        ) {
            writtenDeclaration = (currentNode.base as? Reference)?.refersTo
            val fieldDeclaration = currentNode.refersTo

            if (writtenDeclaration != null && fieldDeclaration != null) {
                // We do an ugly hack here: We store a (unique) hash out of field declaration and
                // the variable declaration in the declaration state so that we can retrieve it
                // later for READ accesses.
                val declState = doubleState.declarationsState[currentNode.objectIdentifier()]
                if (declState != null) {
                    // We check if we have something relevant for this node (because there was an
                    // entry for the incoming edge) in the edgePropertiesMap and, if so, we generate
                    // a dedicated entry for the edge between declState and currentNode.
                    findAndSetProperties(declState.elements, currentNode)
                    state.push(currentNode, declState)
                } else {
                    // If we do not have a stored state of our object+field, we can use the field
                    // declaration. This will help us follow a data flow from field initializers (if
                    // they exist in the language)
                    state.push(currentNode, PowersetLattice(identitySetOf(fieldDeclaration)))
                }
            }

            if (writtenDeclaration != null) {
                // we also want to set the last write to our base here.
                doubleState.declarationsState[writtenDeclaration] =
                    PowersetLattice(identitySetOf(currentNode.base))

                // Update the state identifier of this node, so that the data flows to later member
                // expressions accessing the same object/field combination.
                doubleState.declarationsState[currentNode.objectIdentifier()] =
                    PowersetLattice(identitySetOf(currentNode))
            }
        } else if (isSimpleAssignment(currentNode)) {
            // It's an assignment which can have one or multiple things on the lhs and on the
            // rhs. The lhs could be a declaration or a reference (or multiple of these things).
            // The rhs can be anything. The rhs flows to the respective lhs. To identify the
            // correct mapping, we use the "assignments" property which already searches for us.
            currentNode.assignments.forEach { assignment ->
                // This was the last write to the respective declaration.
                val declPair: Pair<Declaration, Node?>? =
                    if (assignment.target is Declaration)
                        Pair(assignment.target as Declaration, assignment.target)
                    else {
                        val unwrappedTarget = (assignment.target as? Expression).unwrapReference()
                        if (unwrappedTarget is Reference) {
                            if (assignment.target is SubscriptExpression) {
                                val subscriptExpression = assignment.target as? SubscriptExpression
                                val unwrappedBufTarget =
                                    subscriptExpression?.arrayExpression?.unwrapReference()
                                if (unwrappedBufTarget is Reference) {
                                    unwrappedBufTarget.refersTo?.let { Pair(it, assignment.target) }
                                } else null
                            } else if (unwrappedTarget?.refersTo == null) {
                                null
                            } else {
                                Pair(unwrappedTarget.refersTo!!, unwrappedTarget)
                            }
                        } else null
                    }
                declPair?.let { (decl, target) ->
                    if (target != null) {
                        doubleState.declarationsState[decl] = PowersetLattice(identitySetOf(target))
                    }
                }
            }
        } else if (isIncOrDec(currentNode)) {
            // Increment or decrement => Add the prevWrite of the input to the input. After the
            // operation, the prevWrite of the input's variable is this node.
            val input = (currentNode as UnaryOperator).input as Reference
            // We write to the variable in the input
            writtenDeclaration = input.refersTo

            if (writtenDeclaration != null) {
                val prev = doubleState.declarationsState[writtenDeclaration]
                // We check if we have something relevant for this node (because there was an entry
                // for the incoming edge) in the edgePropertiesMap and, if so, we generate a
                // dedicated entry for the edge between declState and currentNode.
                findAndSetProperties(prev?.elements ?: setOf(), currentNode)
                state.push(input, prev)
                doubleState.declarationsState[writtenDeclaration] =
                    PowersetLattice(identitySetOf(input))
            }
        } else if (isCompoundAssignment(currentNode)) {
            // We write to the lhs, but it also serves as an input => We first get all previous
            // writes to the lhs and then add the flow from lhs and rhs to the current node.

            // The write operation goes to the variable in the lhs
            val lhs = currentNode.lhs.singleOrNull()
            val lhsref = lhs.unwrapReference()
            if (lhsref is Reference) {
                writtenDeclaration = lhsref.refersTo

                if (writtenDeclaration != null && lhs != null) {
                    val prev = doubleState.declarationsState[writtenDeclaration]
                    findAndSetProperties(prev?.elements ?: setOf(), currentNode)
                    // Data flows from the last writes to the lhs variable to this node
                    state.push(lhs, prev)

                    // The whole current node is the place of the last update, not (only) the lhs!
                    doubleState.declarationsState[writtenDeclaration] =
                        PowersetLattice(identitySetOf(lhs))
                }
            }
        } else if (
            (currentNode as? Reference)?.access == AccessValues.READ &&
                (currentNode.refersTo is VariableDeclaration ||
                    currentNode.refersTo is ParameterDeclaration) &&
                currentNode.refersTo !is FieldDeclaration &&
                (currentNode.refersTo as? VariableDeclaration)?.isGlobal != true
        ) {
            // We can only find a change if there's a state for the variable
            doubleState.declarationsState[currentNode.refersTo]?.let {
                // We only read the variable => Get previous write which have been collected in
                // the other steps
                // We check if we have something relevant for this node (because there was an entry
                // for the incoming edge) in the edgePropertiesMap and, if so, we generate a
                // dedicated entry for the edge between declState and currentNode.
                findAndSetProperties(it.elements, currentNode)
                state.push(currentNode, it)
            }
        } else if (
            (currentNode as? Reference)?.access == AccessValues.READWRITE &&
                (currentNode.refersTo is VariableDeclaration ||
                    currentNode.refersTo is ParameterDeclaration) &&
                currentNode.refersTo !is FieldDeclaration
        ) {
            // We can only find a change if there's a state for the variable
            doubleState.declarationsState[currentNode.refersTo]?.let {
                // We only read the variable => Get previous write which have been collected in
                // the other steps
                state.push(currentNode, it)
            }
        } else if (
            (currentNode as? Reference)?.access == AccessValues.READWRITE &&
                !currentNode.dfgHandlerHint
        ) {
            /* This branch collects all READWRITE accesses which are not handled separately as compoundAssignment or inc/dec unary operation. This could for example be a pointer passed to an unknown function which is modified in this function but other things are also possible. */
            // We can only find a change if there's a state for the variable
            doubleState.declarationsState[currentNode.refersTo]?.let {
                // We only read the variable => Get previous write which have been collected in
                // the other steps
                state.push(currentNode, it)
            }
            // We read and write to the variable => Update the declarationState accordingly because
            // there was probably some other kind of DFG edge into the reference
            doubleState.declarationsState[currentNode.refersTo] =
                PowersetLattice(identitySetOf(currentNode))
        } else if (
            (currentNode as? Reference)?.access == AccessValues.WRITE && !currentNode.dfgHandlerHint
        ) {
            /* Also here, we want/have to filter out variables in ForEachStatements because this must be handled separately.  */
            // We write to the variable => Update the declarationState accordingly because
            // there was probably some other kind of DFG edge into the reference
            doubleState.declarationsState[currentNode.refersTo] =
                PowersetLattice(identitySetOf(currentNode))
        } else if (currentNode is ForEachStatement && currentNode.variable != null) {
            // The VariableDeclaration in the ForEachStatement doesn't have an initializer, so
            // the "normal" case won't work. We handle this case separately here...
            // This is what we write to the declaration
            val iterable = currentNode.iterable as? Expression
            val writtenTo =
                when (val variable = currentNode.variable) {
                    is DeclarationStatement -> {
                        if (variable.isSingleDeclaration()) {
                            variable.singleDeclaration
                        } else if (variable.variables.size == 2) {
                            // If there are two variables, we just blindly assume that the order is
                            // (key, value), so we return the second one
                            variable.declarations[1]
                        } else {
                            null
                        }
                    }
                    else -> variable
                }

            // We wrote something to this variable declaration
            writtenDeclaration =
                when (writtenTo) {
                    is Declaration -> writtenTo
                    is Reference -> writtenTo.refersTo
                    else -> {
                        log.error(
                            "The variable of type ${writtenTo?.javaClass} is not yet supported in the foreach loop"
                        )
                        null
                    }
                }

            if (writtenTo is Reference) {
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
                    state.push(writtenTo, PowersetLattice(identitySetOf(iterable)))
                    // Add the variable declaration (or the reference) to the list of previous
                    // write nodes in this path
                    state.declarationsState[writtenDeclaration] =
                        PowersetLattice(identitySetOf(writtenTo))
                }
            }
        } else if (currentNode is FunctionDeclaration) {
            // We have to add the parameters
            currentNode.parameters.forEach {
                doubleState.pushToDeclarationsState(it, PowersetLattice(identitySetOf(it)))
            }
        } else if (currentNode is ReturnStatement) {
            doubleState.returnStatements.push(
                currentNode,
                PowersetLattice(identitySetOf(currentNode))
            )
        } else if (currentNode is CallExpression) {
            // If the CallExpression invokes a function for which we have a function summary, we use
            // the summary to identify the last write to a parameter (or receiver) and match it to
            // the respective argument or the base.
            // Since this Reference r is manipulated inside the invoked function, the next
            // read-access of a Reference r' with r'.refersTo == r.refersTo will be affected by the
            // node that has been stored inside the function summary for this particular
            // parameter/receiver, and we store this last write-access in the state.
            // As the node is in another function, we also store the CallingContext of the call
            // expression in the edgePropertiesMap.
            val functionsWithSummaries =
                currentNode.invokes.filter { ctx.config.functionSummaries.hasSummary(it) }
            if (functionsWithSummaries.isNotEmpty()) {
                for (invoked in functionsWithSummaries) {
                    val changedParams = ctx.config.functionSummaries.getLastWrites(invoked)
                    for ((param, _) in changedParams) {
                        val arg =
                            when (param) {
                                (invoked as? MethodDeclaration)?.receiver ->
                                    (currentNode as? MemberCallExpression)?.base as? Reference
                                is ParameterDeclaration ->
                                    currentNode.arguments[param.argumentIndex] as? Reference
                                else -> null
                            }
                        doubleState.declarationsState[arg?.refersTo] =
                            PowersetLattice(identitySetOf(param))
                        /*edgePropertiesMap[Triple(param, arg?.refersTo, false)] =
                        CallingContextOut(currentNode)*/
                        if (arg != null) {
                            for (alias in arg.aliases) {
                                edgePropertiesMap[Triple(param, alias as Node?, false)] =
                                    CallingContextOut(currentNode)
                            }
                        }
                    }
                }
            } else {
                // The default behavior so we continue with the next EOG thing.
                doubleState.declarationsState.push(
                    currentNode,
                    doubleState.declarationsState[currentEdge.start]
                )
            }
        } else {
            doubleState.declarationsState.push(
                currentNode,
                doubleState.declarationsState[currentEdge.start]
            )
        }
        return state
    }

    /**
     * We use this map to store additional information on the DFG edges which we cannot keep in the
     * state. This is for example the case to identify if the resulting edge will receive a
     * context-sensitivity label (i.e., if the node used as key is somehow inside the called
     * function and the next usage happens inside the function under analysis right now). The key of
     * an entry works as follows: The 1st item in the triple is the prevDFG of the 2nd item. If the
     * 2nd item is null, it's obviously not relevant. Ultimately, it will be 2nd -prevDFG-> 1st. If
     * the third item is false, we also don't consider it.
     */
    val edgePropertiesMap = mutableMapOf<Triple<Node, Node?, Boolean>, Any>()

    /**
     * Checks if the node performs an operation and an assignment at the same time e.g. with the
     * operators +=, -=, *=, ...
     */
    protected fun isCompoundAssignment(currentNode: Node): Boolean {
        contract { returns(true) implies (currentNode is AssignExpression) }
        return currentNode is AssignExpression &&
            currentNode.operatorCode in
                (currentNode.language?.compoundAssignmentOperators ?: setOf()) &&
            (currentNode.lhs.singleOrNull() as? Reference)?.refersTo != null
    }

    protected fun isSimpleAssignment(currentNode: Node): Boolean {
        contract { returns(true) implies (currentNode is AssignExpression) }
        return currentNode is AssignExpression && currentNode.isSimpleAssignment
    }

    /** Checks if the node is an increment or decrement operator (e.g. i++, i--, ++i, --i) */
    protected fun isIncOrDec(currentNode: Node) =
        currentNode is UnaryOperator &&
            (currentNode.operatorCode == "++" || currentNode.operatorCode == "--") &&
            (currentNode.input as? Reference)?.refersTo != null

    /**
     * Removes the DFG edges for a potential implicit return statement if it is not in
     * [reachableReturnStatements].
     */
    protected fun removeUnreachableImplicitReturnStatement(
        node: Node,
        reachableReturnStatements: Collection<ReturnStatement>
    ) {
        val lastStatement =
            ((node as? FunctionDeclaration)?.body as? Block)?.statements?.lastOrNull()
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
        var generalState: State<de.fraunhofer.aisec.cpg.graph.Node, V> = State(),
        /**
         * It's main purpose is to store the most recent mapping of a [Declaration] to its
         * [LatticeElement]. However, it is also used to figure out if we have to continue with the
         * iteration (something in the declarationState has changed) which is why we store all nodes
         * here. However, since we never use them except from determining if we changed something,
         * it won't affect the result.
         */
        var declarationsState: State<Any?, V> = State(),

        /** The [returnStatements] which are reachable. */
        var returnStatements: State<de.fraunhofer.aisec.cpg.graph.Node, V> = State()
    ) : State<Node, V>() {
        override fun duplicate(): DFGPassState<V> {
            return DFGPassState(generalState.duplicate(), declarationsState.duplicate())
        }

        override fun get(key: de.fraunhofer.aisec.cpg.graph.Node): LatticeElement<V>? {
            return generalState[key] ?: declarationsState[key]
        }

        override fun lub(
            other: State<de.fraunhofer.aisec.cpg.graph.Node, V>
        ): Pair<State<de.fraunhofer.aisec.cpg.graph.Node, V>, Boolean> {
            return if (other is DFGPassState) {
                val (_, generalUpdate) = generalState.lub(other.generalState)
                val (_, declUpdate) = declarationsState.lub(other.declarationsState)
                Pair(this, generalUpdate || declUpdate)
            } else {
                val (_, generalUpdate) = generalState.lub(other)
                Pair(this, generalUpdate)
            }
        }

        override fun needsUpdate(other: State<de.fraunhofer.aisec.cpg.graph.Node, V>): Boolean {
            return if (other is DFGPassState) {
                generalState.needsUpdate(other.generalState) ||
                    declarationsState.needsUpdate(other.declarationsState)
            } else {
                generalState.needsUpdate(other)
            }
        }

        override fun push(
            newNode: de.fraunhofer.aisec.cpg.graph.Node,
            newLatticeElement: LatticeElement<V>?
        ): Boolean {
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

/**
 * The "object identifier" of a node can be used to differentiate different "objects" that a node
 * (most likely a [Reference]) refers to.
 *
 * In the most basic use-case the [objectIdentifier] of a simple variable reference is the hash-code
 * of its [VariableDeclaration]. Consider the following code:
 * ```c
 * int a = 1;
 * printf(a);
 * ```
 *
 * In this case, the "object identifier" of the [Reference] `a` in the second line is the hash-code
 * of the [VariableDeclaration] `a` in the first line.
 *
 * However, we also need to differentiate between different objects that are used as fields as well
 * as different instances of the fields. Consider the second example:
 * ```c
 * struct myStruct {
 *   int field;
 * };
 *
 * struct myStruct a;
 * a.field = 1;
 *
 * struct myStruct b;
 * b.field = 2;
 * ```
 *
 * In this case, the [objectIdentifier] of the [MemberExpression] `a` is a combination of the
 * hash-code of the [VariableDeclaration] `a` as well as the [FieldDeclaration] of `field`. The same
 * applies for `b`. If we would only rely on the [VariableDeclaration], we would not be sensitive to
 * fields, if we would only rely on the [FieldDeclaration], we would not be sensitive to different
 * object instances. Therefore, we consider both.
 *
 * Please note however, that this current, very basic implementation does not consider perform any
 * kind of pointer or alias analysis. This means that even though the "contents" of two variables
 * that are the same (for example, because one is assigned into the other), they will be considered
 * different "objects".
 */
fun Node.objectIdentifier(): Int? {
    return when (this) {
        is MemberExpression -> this.objectIdentifier()
        is Reference -> this.objectIdentifier()
        is UnaryOperator -> this.objectIdentifier()
        is Declaration -> this.hashCode()
        else -> null
    }
}

/** Implements [Node.objectIdentifier] for a [MemberExpression]. */
fun MemberExpression.objectIdentifier(): Int? {
    val ref = this.refersTo
    return if (ref == null) {
        null
    } else {
        val baseIdentifier = base.objectIdentifier()
        if (baseIdentifier != null) {
            ref.hashCode() + baseIdentifier
        } else {
            null
        }
    }
}

/** Implements [Node.objectIdentifier] for a [Reference]. */
fun Reference.objectIdentifier(): Int? {
    return this.refersTo?.hashCode()
}

/** Implements [Node.objectIdentifier] for a [UnaryOperator]. */
fun UnaryOperator.objectIdentifier(): Int? {
    val op = this.operatorCode
    return if (op == null) {
        null
    } else {
        val inputIdentifier = input.objectIdentifier()
        if (inputIdentifier != null) {
            op.hashCode() + inputIdentifier
        } else {
            null
        }
    }
}
