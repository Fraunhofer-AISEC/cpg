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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.edges.flows.*
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.UnknownMemoryValue
import de.fraunhofer.aisec.cpg.graph.types.NumericType
import de.fraunhofer.aisec.cpg.graph.types.PointerType
import de.fraunhofer.aisec.cpg.helpers.IdentitySet
import de.fraunhofer.aisec.cpg.helpers.functional.*
import de.fraunhofer.aisec.cpg.helpers.identitySetOf
import de.fraunhofer.aisec.cpg.helpers.toIdentitySet
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import java.util.concurrent.ConcurrentHashMap
import kotlin.Pair
import kotlin.collections.filter
import kotlin.collections.map
import kotlin.let
import kotlin.text.contains

val nodesCreatingUnknownValues = ConcurrentHashMap<Pair<Node, Name>, MemoryAddress>()

typealias StateEntry = TupleLattice<PowersetLattice.Element<Node>, PowersetLattice.Element<Node>>

typealias StateEntryElement =
    TupleLattice.Element<PowersetLattice.Element<Node>, PowersetLattice.Element<Node>>

typealias SingleStateElement =
    MapLattice.Element<
        Node,
        TupleLattice.Element<PowersetLattice.Element<Node>, PowersetLattice.Element<Node>>,
    >

typealias SingleState =
    MapLattice<
        Node,
        TupleLattice.Element<PowersetLattice.Element<Node>, PowersetLattice.Element<Node>>,
    >

typealias PointsToStateElement = TupleLattice.Element<SingleStateElement, SingleStateElement>

typealias PointsToState = TupleLattice<SingleStateElement, SingleStateElement>

/**
 * Returns a string that allows a human to identify the node. Mostly, this is simply the node's
 * localName, but for Literals, it is their value
 */
fun nodeNameToString(node: Node): Name {
    return when (node) {
        is Literal<*> -> Name(node.value.toString())
        is UnknownMemoryValue -> Name(node.name.localName, Name("UnknownMemoryValue"))
        else -> node.name
    }
}

/**
 * Resolve a MemberExpression as long as it's base no longer is a MemberExpression itself. Returns
 * the base a Name that identifies the access
 */
fun resolveMemberExpression(node: MemberExpression): Pair<Node, Name> {
    // As long as the base in itself is a MemberExpression, resolve that one
    var base: Node = node
    var newLocalname = ""
    while (base is MemberExpression) {
        val b = base.name.split("::")
        val tmp = if (b.size > 1) b[1] else ""
        newLocalname = if (newLocalname.isEmpty()) tmp else "$tmp.$newLocalname"
        base = base.base
    }

    return Pair(base, Name(newLocalname))
}

private fun isGlobal(node: Node): Boolean {
    return when (node) {
        is VariableDeclaration -> node.isGlobal
        is Reference -> (node.refersTo as? VariableDeclaration)?.isGlobal ?: false
        is MemoryAddress -> node.isGlobal
        else -> false
    }
}

/**
 * We use this map to store additional information on the DFG edges which we cannot keep in the
 * state. This is for example the case to identify if the resulting edge will receive a
 * context-sensitivity label (i.e., if the node used as key is somehow inside the called function
 * and the next usage happens inside the function under analysis right now). The key of an entry
 * works as follows: The 2nd item in the Pair is the prevDFG of the 1st item. Ultimately, it will be
 * 1st -prevDFG-> 1st.
 */
val edgePropertiesMap = mutableMapOf<Pair<Node, Node>, Any>()

// We also need a place to store the derefs of global variables.
var globalDerefs = mutableMapOf<Node, Node>()

@DependsOn(SymbolResolver::class)
@DependsOn(EvaluationOrderGraphPass::class)
@DependsOn(DFGPass::class)
open class PointsToPass(ctx: TranslationContext) : EOGStarterPass(ctx, orderDependencies = true) {
    class Configuration(
        /**
         * This specifies the maximum complexity (as calculated per
         * [Statement.cyclomaticComplexity]) a [FunctionDeclaration] must have in order to be
         * considered.
         */
        var maxComplexity: Int? = null,

        /** This specifies the address length (usually 64bit) */
        var addressLength: Int = 64,
    ) : PassConfiguration()

    // For recursive creation of FunctionSummaries, we have to make sure that we don't run in
    // circles.
    // Therefore, we store the chain of FunctionDeclarations we currently analyse
    private val functionSummaryAnalysisChain = mutableSetOf<FunctionDeclaration>()

    override fun cleanup() {
        // Nothing to do
    }

    override fun accept(node: Node) {
        functionSummaryAnalysisChain.clear()
        return acceptInternal(node)
    }

    fun acceptInternal(node: Node) {
        // For now, we only execute this for function declarations, we will support all EOG starters
        // in the future.
        if (node !is FunctionDeclaration) {
            return
        }
        // If the node already has a function summary, we have visited it before and can
        // return here.
        if (
            node.functionSummary.isNotEmpty() &&
                node.functionSummary.keys.any { it in node.parameters || it in node.returns }
        ) {
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
            // Add an empty function Summary so that we don't try again
            node.functionSummary.computeIfAbsent(ReturnStatement()) { mutableSetOf() }
            return
        }

        log.info("Analyzing function ${node.name}. Complexity: $c")

        val lattice =
            PointsToState(
                SingleState(StateEntry(PowersetLattice<Node>(), PowersetLattice<Node>())),
                SingleState(StateEntry(PowersetLattice<Node>(), PowersetLattice<Node>())),
            )

        var startState = lattice.bottom
        startState =
            lattice.pushToDeclarationsState(
                startState,
                node,
                StateEntryElement(PowersetLattice.Element(), PowersetLattice.Element()),
            )

        startState = initializeParameters(lattice, node.parameters, startState)

        var finalState = lattice.iterateEOG(node.nextEOGEdges, startState, ::transfer)

        /* Store function summary for this FunctionDeclaration. */
        finalState = storeFunctionSummary(lattice, node, finalState)

        for ((key, value) in finalState.generalState) {
            // All nodes in the state get new memoryValues, Expressions and Declarations
            // additionally get new MemoryAddresses
            val newPrevDFGs = value.second
            val newMemoryAddresses = value.first
            newPrevDFGs.forEach { prev ->
                val entry = edgePropertiesMap[Pair(key, prev)]
                var context: CallingContext? = null
                val granularity =
                    when (entry) {
                        is Granularity -> entry
                        is CallingContext -> {
                            context = entry
                            default()
                        }
                        else -> {
                            default()
                        }
                    }
                if (context == null) key.prevDFGEdges += Dataflow(prev, key, granularity)
                else key.prevDFGEdges.addContextSensitive(prev, granularity, context)
            }
            if (newMemoryAddresses.isNotEmpty()) {
                when (key) {
                    is Expression -> {
                        key.memoryAddress.clear()
                        key.memoryAddress.addAll(newMemoryAddresses)
                    }
                    is Declaration -> {
                        if (
                            newMemoryAddresses.size == 1 &&
                                newMemoryAddresses.first() is MemoryAddress
                        )
                            key.memoryAddress = newMemoryAddresses.first() as MemoryAddress
                    }
                }
            }
        }
    }

    private fun storeFunctionSummary(
        lattice: PointsToState,
        node: FunctionDeclaration,
        doubleState: PointsToStateElement,
    ): PointsToStateElement {
        var doubleState = doubleState
        node.parameters.forEach { param ->
            // Collect all addresses of the parameter that we can use as index to look up possible
            // new values
            val indexes = mutableSetOf<Pair<Node, Int>>()
            var values = doubleState.getValues(param)

            // We look at the deref and the derefderef, hence for depth 2 and 3
            // We have to look up the index of the ParameterMemoryValue to check out
            // changes on the dereferences
            values
                .filter { doubleState.hasDeclarationStateEntry(it) }
                .map { indexes.add(Pair(it, 2)) }
            // Additionally, we can check out the dereference itself to look for derefdereferences
            values
                .filter { doubleState.hasDeclarationStateEntry(it) }
                .flatMap { doubleState.getValues(it) }
                .forEach { value ->
                    if (doubleState.hasDeclarationStateEntry(value)) indexes.add(Pair(value, 3))
                }

            indexes.forEach { (index, dstValueDepth) ->
                val stateEntries =
                    doubleState.fetchElementFromDeclarationState(index, true).filter {
                        it.first.name != param.name
                    }
                stateEntries
                    // See if we can find something that is different from the initial value
                    .filter {
                        !(it.first is ParameterMemoryValue &&
                            it.first.name.localName.contains("derefvalue") &&
                            it.first.name.parent == param.name)
                    }
                    // If so, store the last write for the parameter in the FunctionSummary
                    .forEach { (value, subAccessName) ->
                        // Extract the value depth from the value's localName
                        val srcValueDepth =
                            when (value.name.localName) {
                                "value" -> 1
                                "derefvalue" -> 2
                                "derefderefvalue" -> 3
                                else -> 0
                            }
                        node.functionSummary
                            .computeIfAbsent(param) { mutableSetOf() }
                            .add(
                                FunctionDeclaration.FSEntry(
                                    dstValueDepth,
                                    value,
                                    srcValueDepth,
                                    subAccessName,
                                )
                            )
                        var pmv = param.memoryValue
                        for (i in 0..<dstValueDepth - 1) {
                            pmv = pmv?.memoryValue
                        }
                        if (pmv != null) {
                            doubleState =
                                lattice.push(
                                    doubleState,
                                    pmv,
                                    StateEntryElement(
                                        PowersetLattice.Element(),
                                        PowersetLattice.Element(value),
                                    ),
                                )
                            // If we only wrote to a field, we add an entry to the edgePropertiesMap
                            // to mark it as partial Dataflow
                            if (subAccessName.isNotEmpty()) {
                                edgePropertiesMap[Pair(pmv, value)] =
                                    PartialDataflowGranularity(
                                        FieldDeclaration().apply { name = Name(subAccessName) }
                                    )
                            }
                        }
                    }
            }
        }
        // If we don't have anything to summarize, we add a dummy entry to the functionSummary
        if (node.functionSummary.isEmpty()) {
            node.functionSummary.computeIfAbsent(ReturnStatement()) { mutableSetOf() }
        }

        return doubleState
    }

    protected fun transfer(
        lattice: Lattice<PointsToStateElement>,
        currentEdge: EvaluationOrder,
        state: PointsToStateElement,
    ): PointsToStateElement {
        val lattice = lattice as? PointsToState ?: return state
        val currentNode = currentEdge.end

        var doubleState = state

        // Used to keep iterating for steps which do not modify the alias-state otherwise
        doubleState =
            lattice.pushToDeclarationsState(
                doubleState,
                currentNode,
                doubleState.getFromDecl(currentEdge.end)
                    ?: StateEntryElement(PowersetLattice.Element(), PowersetLattice.Element()),
            )

        doubleState =
            when (currentNode) {
                is Declaration,
                is MemoryAddress -> handleDeclaration(lattice, currentNode, doubleState)
                is AssignExpression -> handleAssignExpression(lattice, currentNode, doubleState)
                is UnaryOperator -> handleUnaryOperator(lattice, currentNode, doubleState)
                is CallExpression -> handleCallExpression(lattice, currentNode, doubleState)
                is Expression -> handleExpression(lattice, currentNode, doubleState)
                is ReturnStatement -> handleReturnStatement(lattice, currentNode, doubleState)
                else -> doubleState
            }

        return doubleState
    }

    private fun handleReturnStatement(
        lattice: PointsToState,
        currentNode: ReturnStatement,
        doubleState: PointsToStateElement,
    ): PointsToStateElement {
        /* For Return Statements, all we really want to do is to collect their return values
        to add them to the FunctionSummary */
        var doubleState = doubleState
        if (currentNode.returnValues.isNotEmpty()) {
            val parentFD = currentNode.firstParentOrNull<FunctionDeclaration>()
            if (parentFD != null) {
                currentNode.returnValues.forEach { retval ->
                    parentFD.functionSummary
                        .computeIfAbsent(currentNode) { mutableSetOf() }
                        .addAll(
                            doubleState.getValues(retval).map {
                                FunctionDeclaration.FSEntry(0, it, 1, "")
                            }
                        )
                }
            }
        }
        return doubleState
    }

    /**
     * Add the data flows from the CallExpression's arguments to the FunctionDeclaration's
     * ParameterMemoryValues to the doubleState
     */
    private fun calculateIncomingCallingContexts(
        lattice: PointsToState,
        functionDeclaration: FunctionDeclaration,
        callExpression: CallExpression,
        doubleState: PointsToStateElement,
    ): PointsToStateElement {
        var doubleState = doubleState
        callExpression.arguments.forEach { arg ->
            if (arg.argumentIndex < functionDeclaration.parameters.size) {
                // Create a DFG-Edge from the argument to the parameter's memoryValue
                val p = functionDeclaration.parameters[arg.argumentIndex]
                if (p.memoryValue == null)
                    initializeParameters(lattice, mutableListOf(p), doubleState, 1)
                p.memoryValue?.let { memVal ->
                    edgePropertiesMap[Pair(memVal, arg)] = CallingContextIn(callExpression)
                    doubleState =
                        lattice.push(
                            doubleState,
                            memVal,
                            TupleLattice.Element(
                                PowersetLattice.Element(identitySetOf(memVal)),
                                PowersetLattice.Element(identitySetOf(arg)),
                            ),
                        )
                    // Also draw the edges for the (deref)derefvalues if we have any and are
                    // dealing with a pointer parameter (AKA memoryValue is not null)
                    val derefPMV = memVal.memoryValue
                    if (derefPMV != null) {
                        doubleState
                            .getNestedValues(
                                arg,
                                2,
                                fetchFields = false,
                                onlyFetchExistingEntries = true,
                            )
                            .forEach { derefValue ->
                                edgePropertiesMap[Pair(derefPMV, derefValue)] =
                                    CallingContextIn(callExpression)
                                doubleState =
                                    lattice.push(
                                        doubleState,
                                        derefPMV,
                                        TupleLattice.Element(
                                            PowersetLattice.Element(identitySetOf(derefPMV)),
                                            PowersetLattice.Element(identitySetOf(derefValue)),
                                        ),
                                    )
                                // The same for the derefderef values
                                val derefderefPMV = derefPMV.memoryValue
                                if (derefderefPMV != null) {
                                    doubleState
                                        .getNestedValues(
                                            derefValue,
                                            1,
                                            fetchFields = false,
                                            onlyFetchExistingEntries = true,
                                        )
                                        .forEach { derefderefValue ->
                                            edgePropertiesMap[
                                                Pair(derefderefPMV, derefderefValue)] =
                                                CallingContextIn(callExpression)
                                            doubleState =
                                                lattice.push(
                                                    doubleState,
                                                    derefderefPMV,
                                                    TupleLattice.Element(
                                                        PowersetLattice.Element(
                                                            identitySetOf(derefderefPMV)
                                                        ),
                                                        PowersetLattice.Element(
                                                            identitySetOf(derefderefValue)
                                                        ),
                                                    ),
                                                )
                                        }
                                }
                            }
                    }
                }
            }
        }
        return doubleState
    }

    private fun handleCallExpression(
        lattice: PointsToState,
        currentNode: CallExpression,
        doubleState: PointsToStateElement,
    ): PointsToStateElement {
        var doubleState = doubleState
        val mapDstToSrc = mutableMapOf<Node, IdentitySet<Node>>()

        // First, check if there are missing FunctionSummaries
        /*currentNode.language?.let { language ->
            currentNode.invokes.forEach { invoke ->
                if (invoke.functionSummary.isEmpty()) {
                    ctx.config.functionSummaries.run {
                        this.functionToDFGEntryMap
                            .filterKeys { it.methodName == invoke.name.localName }
                            .map { (declEntry, summary) ->
                                applyDfgEntryToFunctionDeclaration(invoke, summary)
                            }
                    }
                }
            }
        }*/

        var i = 0
        // The toIdentitySet avoids having the same elements multiple times
        val invokes = currentNode.invokes.toIdentitySet().toList()
        while (i < invokes.size) {
            val invoke = invokes[i]
            if (invoke.functionSummary.isEmpty()) {
                if (invoke.hasBody()) {
                    log.debug("functionSummaryAnalysisChain: {}", functionSummaryAnalysisChain)
                    if (invoke !in functionSummaryAnalysisChain) {
                        val summaryCopy = functionSummaryAnalysisChain.toSet()
                        functionSummaryAnalysisChain.add(invoke)
                        acceptInternal(invoke)
                        functionSummaryAnalysisChain.clear()
                        functionSummaryAnalysisChain.addAll(summaryCopy)
                    } else {
                        log.error(
                            "Cannot calculate functionSummary for $invoke as it's recursively called. callChain: $functionSummaryAnalysisChain"
                        )
                        val newValues: MutableSet<FunctionDeclaration.FSEntry> =
                            invoke.parameters
                                .map { FunctionDeclaration.FSEntry(0, it, 1, "") }
                                .toMutableSet()
                        invoke.functionSummary[ReturnStatement()] = newValues
                    }
                } else {
                    // Add a dummy function summary so that we don't try this every time
                    // In this dummy, all parameters point to the return
                    // TODO: This actually generates a new return statement but it's not part of the
                    // function. Wouldn't the edges better point to the FunctionDeclaration and in a
                    // case with a body, all returns flow to the FunctionDeclaration too?
                    val newValues: MutableSet<FunctionDeclaration.FSEntry> =
                        invoke.parameters
                            .map { FunctionDeclaration.FSEntry(0, it, 1, "") }
                            .toMutableSet()
                    invoke.functionSummary[ReturnStatement()] = newValues
                }
            }

            doubleState =
                calculateIncomingCallingContexts(lattice, invoke, currentNode, doubleState)

            // If we have a FunctionSummary, we push the values of the arguments and return value
            // after executing the function call to our doubleState.
            for ((param, fsEntries) in invoke.functionSummary) {
                val argument =
                    when (param) {
                        is ParameterDeclaration ->
                            // Dereference the parameter
                            if (param.argumentIndex < currentNode.arguments.size) {
                                currentNode.arguments[param.argumentIndex]
                            } else null
                        is ReturnStatement -> currentNode
                        else -> null
                    }
                if (argument != null) {
                    fsEntries
                        .sortedBy { it.destValueDepth }
                        .forEach { (dstValueDepth, srcNode, srcValueDepth, subAccessName) ->
                            val destAddrDepth = dstValueDepth - 1
                            // Is the destAddrDepth > 2? In this case, the DeclarationState
                            // might be outdated. So check in the mapDstToSrc for updates
                            val updatedAddresses =
                                mapDstToSrc.entries
                                    .filter {
                                        it.key in /*doubleState.getAddresses(argument)*/
                                            doubleState.getValues(argument)
                                    }
                                    .flatMap { it.value }
                                    .toIdentitySet()
                            val destination =
                                if (dstValueDepth > 2 && updatedAddresses.isNotEmpty()) {
                                    updatedAddresses
                                } else {
                                    if (subAccessName.isNotEmpty()) {
                                        val fieldAddresses = identitySetOf<Node>()
                                        // Collect the fieldAddresses for each possible value
                                        val argumentValues =
                                            doubleState.getNestedValues(
                                                argument,
                                                destAddrDepth,
                                                fetchFields = true,
                                            )
                                        argumentValues.forEach { v ->
                                            val parentName = nodeNameToString(v)
                                            val newName = Name(subAccessName, parentName)
                                            fieldAddresses.addAll(
                                                doubleState.fetchFieldAddresses(
                                                    identitySetOf(v),
                                                    newName,
                                                )
                                            )
                                        }
                                        fieldAddresses
                                    } else {
                                        doubleState.getNestedValues(argument, destAddrDepth)
                                    }
                                }
                            when (srcNode) {
                                is ParameterDeclaration ->
                                    // Add the (dereferenced) value of the respective argument in
                                    // the CallExpression
                                    if (srcNode.argumentIndex < currentNode.arguments.size) {
                                        doubleState
                                            .getNestedValues(
                                                currentNode.arguments[srcNode.argumentIndex],
                                                srcValueDepth,
                                                fetchFields = true,
                                            )
                                            .forEach { value ->
                                                destination.forEach { d ->
                                                    mapDstToSrc.computeIfAbsent(d) {
                                                        identitySetOf<Node>()
                                                    } += value
                                                }
                                            }
                                    }
                                is ParameterMemoryValue -> {
                                    // In case the FunctionSummary says that we have to use the
                                    // dereferenced value here, we look up the argument, dereference
                                    // it, and then add it to the sources
                                    currentNode.invokes
                                        .flatMap { it.parameters }
                                        .filter { it.name == srcNode.name.parent }
                                        .toIdentitySet()
                                        .forEach {
                                            if (it.argumentIndex < currentNode.arguments.size) {
                                                val arg = currentNode.arguments[it.argumentIndex]
                                                destination.forEach { d ->
                                                    mapDstToSrc.computeIfAbsent(d) {
                                                        identitySetOf()
                                                    } +=
                                                        doubleState.getNestedValues(
                                                            arg,
                                                            srcValueDepth,
                                                        )
                                                }
                                            }
                                        }
                                    //                                    }
                                }
                                is MemoryAddress -> {
                                    destination.forEach { d ->
                                        mapDstToSrc.computeIfAbsent(d) { identitySetOf() } +=
                                            srcNode
                                    }
                                }
                                else -> {
                                    destination.forEach { d ->
                                        mapDstToSrc.computeIfAbsent(d) { identitySetOf<Node>() } +=
                                            if (srcValueDepth == 0) identitySetOf(srcNode)
                                            else doubleState.getNestedValues(srcNode, srcValueDepth)
                                    }
                                }
                            }
                        }
                }
            }
            i++
        }

        mapDstToSrc.forEach { (dst, src) ->
            // If the values of the destination are the same as the destination (e.g. if dst is a
            // CallExpression), we also add destinations to update the generalState, otherwise, the
            // destinationAddresses for the DeclarationState are enough
            val dstValues = doubleState.getValues(dst)
            doubleState =
                if (dstValues.all { it == dst })
                    doubleState.updateValues(lattice, src, dstValues, identitySetOf(dst))
                else doubleState.updateValues(lattice, src, identitySetOf(), identitySetOf(dst))

            // If we draw a DFG-Edge from this CallExpression, it should be a CallingContextOut
            //            if (dst == currentNode) {
            src.map { s -> edgePropertiesMap[Pair(dst, s)] = CallingContextOut(currentNode) }
            //            }
        }

        return doubleState
    }

    private fun handleUnaryOperator(
        lattice: PointsToState,
        currentNode: UnaryOperator,
        doubleState: PointsToStateElement,
    ): PointsToStateElement {
        var doubleState = doubleState
        /* For UnaryOperators, we have to update the value if it's a ++ or -- operator
        The edges are drawn by the DFGPass */
        // TODO: Check out cases where the input is no Reference
        if (currentNode.operatorCode in (listOf("++", "--")) && currentNode.input is Reference) {
            val newDeclState = doubleState.declarationsState
            /* Update the declarationState for the refersTo */
            doubleState.getAddresses(currentNode.input).forEach { addr ->
                newDeclState.replace(
                    addr,
                    TupleLattice.Element(
                        PowersetLattice.Element(addr),
                        PowersetLattice.Element(currentNode.input),
                    ),
                )
            }
            doubleState =
                PointsToStateElement(doubleState.generalState, MapLattice.Element(newDeclState))
        }
        return doubleState
    }

    private fun handleAssignExpression(
        lattice: PointsToState,
        currentNode: AssignExpression,
        doubleState: PointsToStateElement,
    ): PointsToStateElement {
        var doubleState = doubleState
        /* For AssignExpressions, we update the value of the rhs with the lhs
         * In C(++), both the lhs and the rhs should only have one element
         */
        if (currentNode.lhs.size == 1 && currentNode.rhs.size == 1) {
            val sources = currentNode.rhs.flatMap { doubleState.getValues(it) }.toIdentitySet()
            val destinations: IdentitySet<Node> = currentNode.lhs.toIdentitySet()
            val destinationsAddresses =
                destinations.flatMap { doubleState.getAddresses(it) }.toIdentitySet()
            doubleState =
                doubleState.updateValues(lattice, sources, destinations, destinationsAddresses)
        }

        return doubleState
    }

    private fun handleExpression(
        lattice: PointsToState,
        currentNode: Expression,
        doubleState: PointsToStateElement,
    ): PointsToStateElement {
        var doubleState = doubleState

        /* If we have an Expression that is written to, we handle it later and ignore it now */
        val access =
            if (currentNode is Reference) currentNode.access
            else if (currentNode is SubscriptExpression && currentNode.arrayExpression is Reference)
                (currentNode.arrayExpression as Reference).access
            else null
        if (access == AccessValues.READ) {
            val addresses = doubleState.getAddresses(currentNode)
            val values = doubleState.getValues(currentNode).toIdentitySet()

            // If we have any information from the dereferenced value, we also fetch that
            values
                .filter { doubleState.hasDeclarationStateEntry(it) }
                .flatMap { doubleState.fetchElementFromDeclarationState(it) }
                .map { it.first }
                .forEach { derefValue ->
                    values.add(derefValue)
                    // Store the information over the type of the edge in the edgePropertiesMap
                    edgePropertiesMap[Pair(currentNode, derefValue)] =
                        PointerDataflowGranularity(PointerAccess.currentDerefValue)
                    // Let's see if we can deref once more
                    if (doubleState.hasDeclarationStateEntry(derefValue)) {
                        doubleState
                            .fetchElementFromDeclarationState(derefValue)
                            .map { it.first }
                            .forEach { derefDerefValue ->
                                values.add(derefDerefValue)
                                edgePropertiesMap[Pair(currentNode, derefDerefValue)] =
                                    PointerDataflowGranularity(PointerAccess.currentDerefDerefValue)
                            }
                    }
                }

            doubleState =
                lattice.push(
                    doubleState,
                    currentNode,
                    StateEntryElement(
                        PowersetLattice.Element(addresses),
                        PowersetLattice.Element(values),
                    ),
                )

            // When we stored previoulsy that the address was written to by a function, we use this
            // information now and attach the callingContext to the actual read
            addresses.forEach { addr ->
                values.forEach { value ->
                    val cc = edgePropertiesMap[Pair(addr, value)]
                    if (cc is CallingContext) {
                        edgePropertiesMap[Pair(currentNode, value)] = cc
                    }
                }
            }
        }
        return doubleState
    }

    private fun handleDeclaration(
        lattice: PointsToState,
        currentNode: Node,
        doubleState: PointsToStateElement,
    ): PointsToStateElement {
        /* No need to set the address, this already happens in the constructor */
        val addresses = doubleState.getAddresses(currentNode)

        val values = PowersetLattice.Element<Node>()

        (currentNode as? HasInitializer)?.initializer?.let { initializer ->
            if (initializer is Literal<*>) values.add(initializer)
            else values.addAll(doubleState.getValues(initializer))
        }

        var doubleState =
            lattice.push(
                doubleState,
                currentNode,
                StateEntryElement(PowersetLattice.Element(addresses), values),
            )
        /* In the DeclarationsState, we save the address which we wrote to the value for easier work with pointers
         * */
        addresses.forEach { addr ->
            doubleState =
                lattice.pushToDeclarationsState(
                    doubleState,
                    addr,
                    StateEntryElement(
                        PowersetLattice.Element(addresses),
                        PowersetLattice.Element(values),
                    ),
                )
        }
        return doubleState
    }

    /** Create ParameterMemoryValues up to depth `depth` */
    private fun initializeParameters(
        lattice: PointsToState,
        parameters: MutableList<ParameterDeclaration>,
        doubleState: PointsToStateElement,
        // Until which depth do we create ParameterMemoryValues
        depth: Int = 2,
    ): PointsToStateElement {
        var doubleState = doubleState
        parameters
            .filter { it.memoryValue == null }
            .forEach { param ->
                // In the first step, we have a triangle of ParameterDeclaration, the
                // ParameterDeclaration's Memory Address and the ParameterMemoryValue
                // Therefore, the src and the addresses are different. For all other depths, we set
                // both to the ParameterMemoryValue we create in the first step
                var src: Node = param
                var addresses = doubleState.getAddresses(src)
                // If we have a Pointer as param, we initialize all levels, otherwise, only the
                // first one
                val paramDepth =
                    if (
                        param.type is PointerType ||
                            (param.type as? NumericType)?.bitWidth ==
                                // TODO: passConfig<Configuration> should never be null?
                                (passConfig<Configuration>()?.addressLength ?: 64)
                    )
                        depth
                    else 0
                for (i in 0..paramDepth) {
                    val pmvName = "deref".repeat(i) + "value"
                    val pmv =
                        ParameterMemoryValue(Name(pmvName, param.name)).apply {
                            memoryAddress = addresses.first() /* TODO: might there also be more? */
                        }
                    // In the first step, we link the ParameterDeclaration to the PMV to be able to
                    // also access it outside the function
                    if (src is ParameterDeclaration) {
                        src.memoryValue = pmv
                        doubleState =
                            lattice.push(
                                doubleState,
                                pmv,
                                StateEntryElement(
                                    PowersetLattice.Element(),
                                    PowersetLattice.Element(src),
                                ),
                            )
                    } else {
                        // Link the PMVs with each other so that we can find them. This is
                        // especially important outside the respective function where we don't have
                        // a state
                        (pmv.memoryAddress as ParameterMemoryValue).memoryValue = pmv
                    }

                    // Update the states
                    val state =
                        StateEntryElement(
                            PowersetLattice.Element(addresses),
                            PowersetLattice.Element(pmv),
                        )
                    addresses.forEach { addr ->
                        doubleState = lattice.pushToDeclarationsState(doubleState, addr, state)
                    }
                    doubleState = lattice.push(doubleState, src, state)
                    // prepare for next step
                    src = pmv
                    addresses = identitySetOf(pmv)
                }
            }
        return doubleState
    }
}

/**
 * It's most likely a stub if it calls the same method and has only a call and return and maximum
 * one more statement.
 */
fun FunctionDeclaration.isStub(): Boolean {
    val bodySize = (body as? Block)?.statements?.size
    return body == null ||
        (bodySize != null &&
            bodySize <= 3 &&
            this.calls.singleOrNull()?.name == this.name &&
            this.returns.singleOrNull() != null)
}

val PointsToStateElement.generalState: SingleStateElement
    get() = this.first

val PointsToStateElement.declarationsState: SingleStateElement
    get() = this.second

fun PointsToStateElement.get(key: Node): StateEntryElement? {
    return this.generalState[key] ?: this.declarationsState[key]
}

fun PointsToStateElement.getFromDecl(key: Node): StateEntryElement? {
    return this.declarationsState[key]
}

fun PointsToState.push(
    currentState: PointsToStateElement,
    newNode: Node,
    newLatticeElement: StateEntryElement,
): PointsToStateElement {
    val newGeneralState =
        this.innerLattice1.lub(
            currentState.generalState,
            MapLattice.Element(newNode to newLatticeElement),
        )
    return PointsToStateElement(newGeneralState, currentState.declarationsState)
}

/** Pushes the [newNode] and its [newLatticeElement] to the [declarationsState]. */
fun PointsToState.pushToDeclarationsState(
    currentState: PointsToStateElement,
    newNode: Node,
    newLatticeElement: StateEntryElement,
): PointsToStateElement {
    val newDeclarationsState =
        this.innerLattice2.lub(
            currentState.declarationsState,
            MapLattice.Element(newNode to newLatticeElement),
        )
    return PointsToStateElement(currentState.generalState, newDeclarationsState)
}

/** Check if `node` has an entry in the DeclarationState */
fun PointsToStateElement.hasDeclarationStateEntry(node: Node): Boolean {
    return (this.declarationsState[node]?.second?.isNotEmpty() == true)
}

/**
 * Fetch the entry for `addr` from the DeclarationState. If there isn't any, create an
 * UnknownMemoryValue
 */
fun PointsToStateElement.fetchElementFromDeclarationState(
    addr: Node,
    fetchFields: Boolean = false,
): IdentitySet<Pair<Node, String>> {
    val ret = identitySetOf<Pair<Node, String>>()

    // For global nodes, we check the globalDerefs map
    if (isGlobal(addr)) {
        val element = globalDerefs[addr]
        if (element != null) ret.add(Pair(element, ""))
        else {
            val newName = nodeNameToString(addr)
            val newEntry =
                nodesCreatingUnknownValues.computeIfAbsent(Pair(addr, newName)) {
                    UnknownMemoryValue(newName, true)
                }
            globalDerefs[addr] = newEntry
            ret.add(Pair(newEntry, ""))
        }
    } else {

        // Otherwise, we read the declarationState.
        // Let's start with the main element
        val elements = this.declarationsState[addr]?.second
        if (elements.isNullOrEmpty()) {
            val newName = nodeNameToString(addr)
            val newEntry =
                nodesCreatingUnknownValues.computeIfAbsent(Pair(addr, newName)) {
                    UnknownMemoryValue(newName)
                }
            this.declarationsState.computeIfAbsent(addr) {
                TupleLattice.Element(
                    PowersetLattice.Element(addr),
                    PowersetLattice.Element(newEntry),
                )
            }
            val newElements = this.declarationsState[addr]?.second
            newElements?.add(newEntry)
            ret.add(Pair(newEntry, ""))
        } else elements.map { ret.add(Pair(it, "")) }

        // if fetchFields is true, we also fetch the values for fields
        if (fetchFields) {
            val fields = this.declarationsState[addr]?.first?.filter { it != addr }
            fields?.forEach { field ->
                this.declarationsState[field]?.second?.let {
                    it.map { ret.add(Pair(it, field.name.localName)) }
                }
            }
        }
    }

    return ret
}

fun PointsToStateElement.getValues(node: Node): IdentitySet<Node> {
    return when (node) {
        is PointerReference -> {
            /* For PointerReferences, the value is the address of the input
             * For example, the value of `&i` is the address of `i`
             * */
            this.getAddresses(node.input)
        }
        is PointerDereference -> {
            /* To find the value for PointerDereferences, we first check what's the current value of the input, which is probably a MemoryAddress
             * Then we look up the current value at this MemoryAddress
             */
            val inputVal =
                /*                        when (node.input) {
                is Reference -> this.getValues(node.input)
                else -> // TODO: How can we handle other cases?*/
                this.getValues(node.input)
            //                        }
            val retVal = identitySetOf<Node>()
            inputVal.forEach { input ->
                retVal.addAll(fetchElementFromDeclarationState(input, true).map { it.first })
            }
            retVal
        }
        is Declaration -> {
            /* For Declarations, we have to look up the last value written to it.
             */
            if (node.memoryAddress == null) {
                node.memoryAddress = MemoryAddress(node.name, isGlobal(node))
            }
            fetchElementFromDeclarationState(node.memoryAddress!!).map { it.first }.toIdentitySet()
        }
        is MemoryAddress -> {
            fetchElementFromDeclarationState(node).map { it.first }.toIdentitySet()
        }
        is MemberExpression -> {
            val (base, fieldName) = resolveMemberExpression(node)
            val baseAddresses = getAddresses(base).toIdentitySet()
            val fieldAddresses = fetchFieldAddresses(baseAddresses, fieldName)
            if (fieldAddresses.isNotEmpty()) {
                fieldAddresses
                    .flatMap { fetchElementFromDeclarationState(it).map { it.first } }
                    .toIdentitySet()
            } else {
                val newName = Name(nodeNameToString(node).localName, base.name)
                identitySetOf(
                    nodesCreatingUnknownValues.computeIfAbsent(Pair(node, newName)) {
                        UnknownMemoryValue(newName)
                    }
                )
            }
        }
        is Reference -> {
            /* For References, we have to look up the last value written to its declaration.
             */
            val retVals = identitySetOf<Node>()
            this.getAddresses(node).forEach { addr ->
                // For globals we draw
                // a DFG edge to the global Declaration's memory Address
                /*if (isGlobal(node)) node.refersTo?.memoryAddress?.let { retVals.add(it) }
                else*/ retVals.addAll(this.getValues(addr))
            }
            return retVals
        }
        is CastExpression -> {
            this.getValues(node.expression)
        }
        is SubscriptExpression -> {
            this.getAddresses(node).flatMap { this.getValues(it) }.toIdentitySet()
        }
        /* In these cases, we simply have to fetch the current value for the MemoryAddress from the DeclarationState */
        else -> identitySetOf(node)
    }
}

fun PointsToStateElement.getAddresses(node: Node): IdentitySet<Node> {
    return when (node) {
        is Declaration -> {
            /*
             * For declarations, we created a new MemoryAddress node, so that's the one we use here
             */
            if (node.memoryAddress == null) {
                node.memoryAddress = MemoryAddress(node.name, isGlobal(node))
            }

            identitySetOf(node.memoryAddress!!)
        }
        is ParameterMemoryValue -> {
            if (node.memoryAddress != null) identitySetOf(node.memoryAddress!!) else identitySetOf()
        }
        is MemoryAddress -> {
            identitySetOf(node)
        }
        is PointerReference -> {
            identitySetOf()
        }
        is PointerDereference -> {
            /*
            PointerDereferences have as address the value of their input.
            For example, the address of `*a` is the value of `a`
             */
            this.getValues(node.input)
        }
        is MemberExpression -> {
            /*
             * For MemberExpressions, the fieldAddresses in the MemoryAddress node of the base hold the information we are looking for
             */
            val (base, newName) = resolveMemberExpression(node)
            fetchFieldAddresses(this.getAddresses(base).toIdentitySet(), newName)
        }
        is Reference -> {
            /*
            For references, the address is the same as for the declaration, AKA the refersTo
            */
            node.refersTo?.let { refersTo ->
                /* In some cases, the refersTo might not yet have an initialized MemoryAddress, for example if it's a FunctionDeclaration. So let's to this here */
                if (refersTo.memoryAddress == null) {
                    refersTo.memoryAddress = MemoryAddress(node.name, isGlobal(node))
                }

                identitySetOf(refersTo.memoryAddress!!)
            } ?: identitySetOf()
        }
        is CastExpression -> {
            /*
            For CastExpressions we take the expression as the cast itself does not have any impact on the address
             */
            this.getAddresses(node.expression)
        }
        is SubscriptExpression -> {
            val localName = nodeNameToString(node.subscriptExpression)
            this.getValues(node.base)
                .flatMap {
                    fetchFieldAddresses(
                        identitySetOf(it),
                        Name(localName.localName, nodeNameToString(it)),
                    )
                }
                .toIdentitySet()
        }
        else -> identitySetOf(node)
    }
}

/**
 * nestingDepth 0 gets the `node`'s address. 1 fetches the current value, 2 the dereference, 3 the
 * derefdereference, etc...
 */
fun PointsToStateElement.getNestedValues(
    node: Node,
    nestingDepth: Int,
    fetchFields: Boolean = false,
    onlyFetchExistingEntries: Boolean = false,
): IdentitySet<Node> {
    if (nestingDepth == 0) return this.getAddresses(node)
    var ret = getValues(node)
    for (i in 1..<nestingDepth) {
        ret =
            ret.filter { if (onlyFetchExistingEntries) this.hasDeclarationStateEntry(it) else true }
                .flatMap { this.fetchElementFromDeclarationState(it, fetchFields) }
                .map { it.first }
                .toIdentitySet()
    }
    return ret
}

fun PointsToStateElement.fetchFieldAddresses(
    baseAddresses: IdentitySet<Node>,
    nodeName: Name,
): IdentitySet<Node> {
    val fieldAddresses = identitySetOf<Node>()

    baseAddresses.forEach { addr ->
        val elements =
            declarationsState[addr]
                ?.first
                ?.filter { it.name.localName == nodeName.localName }
                ?.toMutableList()

        if (elements.isNullOrEmpty()) {
            // val newName = nodeNameToString(addr)
            val newEntry =
                identitySetOf<Node>(
                    nodesCreatingUnknownValues.computeIfAbsent(Pair(addr, nodeName)) {
                        MemoryAddress(nodeName)
                    }
                )
            this.declarationsState.computeIfAbsent(addr) {
                TupleLattice.Element(PowersetLattice.Element(addr), PowersetLattice.Element())
            }
            val newElements = this.declarationsState[addr]?.first
            newElements?.addAll(newEntry)
            fieldAddresses.addAll(newEntry)
            /*newEntry.map { ret.add(Pair(it, "")) }*/
        } else {
            elements.let { fieldAddresses.addAll(it) }
        }
    }

    return fieldAddresses
}

/**
 * Updates the declarationState at `destinationAddresses` to the values in `sources`. Additionally,
 * updates the generalstate at `destinations` if there is any
 */
fun PointsToStateElement.updateValues(
    lattice: PointsToState,
    sources: IdentitySet<Node>,
    destinations: IdentitySet<Node>,
    destinationAddresses: IdentitySet<Node>,
): PointsToStateElement {
    val newDeclState = this.declarationsState.duplicate()
    val newGenState = this.generalState.duplicate()

    /* Update the declarationState for the addresses */
    destinationAddresses.forEach { destAddr ->
        // Clear previous entries in edgePropertiesMap
        edgePropertiesMap
            .filter { it.key.first == destAddr }
            .forEach { entry -> edgePropertiesMap.remove(entry.key) }

        if (!isGlobal(destAddr)) {
            val currentEntries =
                this.declarationsState[destAddr]?.first?.toIdentitySet() ?: identitySetOf(destAddr)

            newDeclState[destAddr] =
                TupleLattice.Element(
                    PowersetLattice.Element(currentEntries),
                    PowersetLattice.Element(sources),
                )
        } else {
            // TODO: We basically do this below, but currently we don't get the destinations
            // value from the call
            getValues(destAddr).forEach { addr ->
                newGenState[addr] =
                    TupleLattice.Element(
                        PowersetLattice.Element(destinationAddresses),
                        PowersetLattice.Element(sources),
                    )
            }
        }
    }

    /* Also update the generalState for dst (if we have any destinations) */
    destinations.forEach { d ->
        newGenState[d] =
            TupleLattice.Element(
                PowersetLattice.Element(destinationAddresses),
                PowersetLattice.Element(sources),
            )
    }

    var doubleState = PointsToStateElement(newGenState, newDeclState)

    /* When we are dealing with SubscriptExpression, we also have to initialize the arrayExpression
    , since that hasn't been done yet */
    destinations.filterIsInstance<SubscriptExpression>().forEach { d ->
        val aEaddresses = this.getAddresses(d.arrayExpression)
        val aEvalues = this.getValues(d.arrayExpression)

        doubleState =
            lattice.push(
                doubleState,
                d.arrayExpression,
                TupleLattice.Element(
                    PowersetLattice.Element(aEaddresses),
                    PowersetLattice.Element(aEvalues),
                ),
            )
    }

    return doubleState
}
