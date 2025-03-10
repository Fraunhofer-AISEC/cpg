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
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.Pair
import kotlin.collections.filter
import kotlin.collections.map
import kotlin.let
import kotlin.text.contains

val nodesCreatingUnknownValues = ConcurrentHashMap<Pair<Node, Name>, MemoryAddress>()

typealias GeneralStateEntry =
    TripleLattice<
        PowersetLattice.Element<Node>,
        PowersetLattice.Element<Node>,
        PowersetLattice.Element<Pair<Node, Set<Any>>>,
    >

typealias DeclarationStateEntry =
    TripleLattice<
        PowersetLattice.Element<Node>,
        PowersetLattice.Element<Pair<Node, Boolean>>,
        PowersetLattice.Element<Pair<Node, Boolean>>,
    >

typealias GeneralStateEntryElement =
    TripleLattice.Element<
        // Address
        PowersetLattice.Element<Node>,
        // MemoryValues
        PowersetLattice.Element<Node>,
        // prevDFG
        PowersetLattice.Element<Pair<Node, Set<Any>>>,
    >

typealias DeclarationStateEntryElement =
    TripleLattice.Element<
        // Address
        PowersetLattice.Element<Node>,
        // Values
        PowersetLattice.Element<Pair<Node, Boolean>>,
        // LastWrites
        PowersetLattice.Element<Pair<Node, Boolean>>,
    >

typealias SingleGeneralStateElement =
    MapLattice.Element<
        Node,
        TripleLattice.Element<
            PowersetLattice.Element<Node>,
            PowersetLattice.Element<Node>,
            PowersetLattice.Element<Pair<Node, Set<Any>>>,
        >,
    >

typealias SingleDeclarationStateElement = MapLattice.Element<Node, DeclarationStateEntryElement>

typealias SingleGeneralState = MapLattice<Node, GeneralStateEntryElement>

typealias SingleDeclarationState =
    MapLattice<
        Node,
        TripleLattice.Element<
            PowersetLattice.Element<Node>,
            PowersetLattice.Element<Pair<Node, Boolean>>,
            PowersetLattice.Element<Pair<Node, Boolean>>,
        >,
    >

typealias PointsToStateElement =
    TupleLattice.Element<SingleGeneralStateElement, SingleDeclarationStateElement>

typealias PointsToState = TupleLattice<SingleGeneralStateElement, SingleDeclarationStateElement>

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

/** Returns the depth of a Node based on its name */
fun stringToDepth(name: String): Int {
    return when (name) {
        "value" -> 1
        "derefvalue" -> 2
        "derefderefvalue" -> 3
        else -> 0
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
        is Reference -> (node.refersTo as? VariableDeclaration)?.isGlobal == true
        is MemoryAddress -> node.isGlobal
        else -> false
    }
}

private fun addEntryToEdgePropertiesMap(index: Pair<Node, Node>, entries: IdentitySet<Any>) {
    if (edgePropertiesMap[index] == null) edgePropertiesMap[index] = entries
    else edgePropertiesMap[index]!!.addAll(entries)
}

/**
 * We use this map to store additional information on the DFG edges which we cannot keep in the
 * state. This is for example the case to identify if the resulting edge will receive a
 * context-sensitivity label (i.e., if the node used as key is somehow inside the called function
 * and the next usage happens inside the function under analysis right now). The key of an entry
 * works as follows: The 2nd item in the Pair is the prevDFG of the 1st item. Ultimately, it will be
 * 1st -prevDFG-> 1st.
 */
val edgePropertiesMap = mutableMapOf<Pair<Node, Node>, IdentitySet<Any>>()

// We also need a place to store the derefs of global variables. The Boolean indicates if this is a
// value stored for a short function Summary
var globalDerefs = mutableMapOf<Node, IdentitySet<Pair<Node, Boolean>>>()

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
                SingleGeneralState(
                    GeneralStateEntry(PowersetLattice(), PowersetLattice(), PowersetLattice())
                ),
                // SingleDeclarationState(DeclarationStateEntry(PowersetLattice<Node>(),
                // PowersetLattice<Pair<Node, Boolean>())),
                SingleDeclarationState(
                    DeclarationStateEntry(PowersetLattice(), PowersetLattice(), PowersetLattice())
                ),
            )

        var startState = lattice.bottom
        startState =
            lattice.pushToDeclarationsState(
                startState,
                node,
                DeclarationStateEntryElement(
                    PowersetLattice.Element(),
                    PowersetLattice.Element(),
                    PowersetLattice.Element(),
                ),
            )

        startState = initializeParameters(lattice, node.parameters, startState)

        var finalState = lattice.iterateEOG(node.nextEOGEdges, startState, ::transfer)

        for ((key, value) in finalState.generalState) {
            // The generalState values have 3 items: The address, the value, and the prevDFG-Edges
            // with a set of properties
            // Let's start with fetch the addresses
            if (key is HasMemoryAddress) {
                key.memoryAddresses.clear()
                key.memoryAddresses += value.first.filterIsInstance<MemoryAddress>()
            }

            // Then the memoryValues
            if (key is HasMemoryValue && value.second.isNotEmpty()) {
                value.second.forEach { v -> key.memoryValues += v }
            }

            // And now the prevDFGs. These are pairs, where the second item is a with a set of
            // properties for the edge
            value.third.forEach { (prev, properties) ->
                var context: CallingContext? = null
                var granularity = default()
                var functionSummary = false

                // the properties can contain a lot of things. A granularity, a
                // callingcontext, or a boolean indicating if this is a functionSummary edge or not
                properties.forEach { property ->
                    when (property) {
                        is Granularity -> granularity = property
                        is CallingContext -> context = property
                        is Boolean -> functionSummary = property
                    }
                }

                if (context == null) // TODO: add functionSummary flag for contextSensitive DFs
                 key.prevDFGEdges += Dataflow(prev, key, granularity, functionSummary)
                else
                    key.prevDFGEdges.addContextSensitive(
                        prev,
                        granularity,
                        context,
                        functionSummary,
                    )
            }
        }

        /* Store function summary for this FunctionDeclaration. */
        storeFunctionSummary(node, finalState)
    }

    private fun storeFunctionSummary(node: FunctionDeclaration, doubleState: PointsToStateElement) {
        node.parameters.forEach { param ->
            // Collect all addresses of the parameter that we can use as index to look up possible
            // new values
            val indexes = mutableSetOf<Pair<Node, Int>>()
            var values = doubleState.getValues(param)

            // We look at the deref and the derefderef, hence for depth 2 and 3
            // We have to look up the index of the ParameterMemoryValue to check out
            // changes on the dereferences
            values
                .filterTo(identitySetOf()) { doubleState.hasDeclarationStateEntry(it) }
                .map { indexes.add(Pair(it, 2)) }
            // Additionally, we can check out the "dereference" itself to look for
            // "derefdereferences"
            values
                .filterTo(identitySetOf()) { doubleState.hasDeclarationStateEntry(it) }
                .flatMap { doubleState.getValues(it) }
                .forEach { value ->
                    if (doubleState.hasDeclarationStateEntry(value)) indexes.add(Pair(value, 3))
                }

            indexes.forEach { (index, dstValueDepth) ->
                val stateEntries =
                    doubleState.fetchElementFromDeclarationState(index, true).filterTo(
                        identitySetOf()
                    ) {
                        it.first.name != param.name
                    }
                stateEntries
                    // See if we can find something that is different from the initial value
                    .filterTo(identitySetOf()) {
                        !(it.first is ParameterMemoryValue &&
                            it.first.name.localName.contains("derefvalue") &&
                            it.first.name.parent == param.name)
                    }
                    // If so, store the last write for the parameter in the FunctionSummary
                    .forEach { (value, subAccessName) ->
                        // Extract the value depth from the value's localName
                        val srcValueDepth = stringToDepth(value.name.localName)
                        // Store the information in the functionSummary
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
                        // Additionally, we store this as a shortFunctionSummary were the Function
                        // writes to the parameter
                        node.functionSummary
                            .computeIfAbsent(param) { mutableSetOf() }
                            .add(
                                FunctionDeclaration.FSEntry(
                                    dstValueDepth,
                                    node,
                                    0,
                                    subAccessName,
                                    true,
                                )
                            )
                        val propertySet = identitySetOf<Any>(true)
                        if (subAccessName != "")
                            propertySet.add(FieldDeclaration().apply { name = Name(subAccessName) })
                        edgePropertiesMap[Pair(param, node)] = propertySet
                        // Draw a DFG-Edge from the ParameterMemoryValue to the value
                        // TODO: I don't think we actually  need to do that
                        /*                        var pmv = param.memoryValue
                        for (i in 0..<dstValueDepth - 1) {
                            pmv = pmv?.memoryValue
                        }
                        if (pmv != null) {
                            val granularity =
                                if (subAccessName.isNotEmpty()) {
                                    PartialDataflowGranularity(
                                        FieldDeclaration().apply { name = Name(subAccessName) }
                                    )
                                } else default()
                                                        pmv.prevDFGEdges += Dataflow(value, pmv,
                             granularity)
                        }*/

                        // Check if the value is influenced by a Parameter and if so, add this
                        // information to the functionSummary
                        value
                            .followDFGEdgesUntilHit(
                                collectFailedPaths = false,
                                direction = Backward(GraphToFollow.DFG),
                                sensitivities = OnlyFullDFG + FieldSensitive + ContextSensitive,
                                scope = Intraprocedural(),
                                predicate = { it is ParameterMemoryValue },
                            )
                            .fulfilled
                            .map { it.last() }
                            .toIdentitySet()
                            .forEach { sourceParamValue ->
                                val matchingDeclarations =
                                    node.parameters.filter {
                                        it.name == sourceParamValue.name.parent
                                    }
                                if (matchingDeclarations.size != 1) TODO()
                                node.functionSummary
                                    .computeIfAbsent(param) { mutableSetOf() }
                                    .add(
                                        FunctionDeclaration.FSEntry(
                                            dstValueDepth,
                                            matchingDeclarations.first(),
                                            stringToDepth(sourceParamValue.name.localName),
                                            subAccessName,
                                            true,
                                        )
                                    )
                            }
                    }
            }
        }
        // If we don't have anything to summarize, we add a dummy entry to the functionSummary
        if (node.functionSummary.isEmpty()) {
            node.functionSummary.computeIfAbsent(ReturnStatement()) { mutableSetOf() }
        }
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
                    ?: DeclarationStateEntryElement(
                        PowersetLattice.Element(),
                        PowersetLattice.Element(),
                        PowersetLattice.Element(),
                    ),
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
                    addEntryToEdgePropertiesMap(
                        Pair(memVal, arg),
                        identitySetOf(CallingContextIn(callExpression)),
                    )
                    doubleState =
                        lattice.push(
                            doubleState,
                            memVal,
                            GeneralStateEntryElement(
                                PowersetLattice.Element(memVal),
                                PowersetLattice.Element(arg),
                                PowersetLattice.Element(),
                            ),
                        )
                    // Also draw the edges for the (deref)derefvalues if we have any and are
                    // dealing with a pointer parameter (AKA memoryValue is not null)
                    memVal.memoryValues.forEach { derefPMV ->
                        doubleState
                            .getNestedValues(
                                arg,
                                2,
                                fetchFields = false,
                                onlyFetchExistingEntries = true,
                                excludeShortFSValues = true,
                            )
                            .forEach { derefValue ->
                                addEntryToEdgePropertiesMap(
                                    Pair(derefPMV, derefValue),
                                    identitySetOf(CallingContextIn(callExpression)),
                                )
                                doubleState =
                                    lattice.push(
                                        doubleState,
                                        derefPMV,
                                        GeneralStateEntryElement(
                                            PowersetLattice.Element(derefPMV),
                                            PowersetLattice.Element(derefValue),
                                            PowersetLattice.Element(),
                                        ),
                                    )
                                // The same for the derefderef values
                                (derefPMV as? HasMemoryValue)?.memoryValues?.forEach { derefderefPMV
                                    ->
                                    // probably iterate through everything???
                                    doubleState
                                        .getNestedValues(
                                            derefValue,
                                            1,
                                            fetchFields = false,
                                            onlyFetchExistingEntries = true,
                                            excludeShortFSValues = true,
                                        )
                                        .forEach { derefderefValue ->
                                            addEntryToEdgePropertiesMap(
                                                Pair(derefderefPMV, derefderefValue),
                                                identitySetOf(CallingContextIn(callExpression)),
                                            )
                                            doubleState =
                                                lattice.push(
                                                    doubleState,
                                                    derefderefPMV,
                                                    //
                                                    //      TripleLattice.Element(
                                                    GeneralStateEntryElement(
                                                        PowersetLattice.Element(derefderefPMV),
                                                        PowersetLattice.Element(derefderefValue),
                                                        PowersetLattice.Element(),
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
        val mapDstToSrc = mutableMapOf<Node, IdentitySet<Pair<Node, Boolean>>>()

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
                        .forEach { (dstValueDepth, srcNode, srcValueDepth, subAccessName, shortFS)
                            ->
                            val destAddrDepth = dstValueDepth - 1
                            // Is the destAddrDepth > 2? In this case, the DeclarationState
                            // might be outdated. So check in the mapDstToSrc for updates
                            val updatedAddresses =
                                mapDstToSrc.entries
                                    .filter { it.key in doubleState.getValues(argument) }
                                    .flatMap { it.value }
                                    .map { it.first }
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
                            // If this is a short FunctionSummary, store this info in the
                            // edgePropertiesMap
                            if (shortFS) {
                                val prev =
                                    (srcNode as? FunctionDeclaration)
                                        ?: currentNode.arguments[srcNode.argumentIndex]
                                addEntryToEdgePropertiesMap(
                                    Pair(argument, prev),
                                    identitySetOf(true),
                                )
                            }
                            when (srcNode) {
                                is ParameterDeclaration -> {
                                    // Add the (dereferenced) value of the respective argument
                                    // in
                                    // the CallExpression
                                    if (srcNode.argumentIndex < currentNode.arguments.size) {
                                        // If this is a short FunctionSummary, we additionally
                                        // update the
                                        // generalState to draw the additional DFG Edges
                                        if (shortFS) {
                                            doubleState =
                                                lattice.push(
                                                    doubleState,
                                                    argument,
                                                    GeneralStateEntryElement(
                                                        PowersetLattice.Element(),
                                                        PowersetLattice.Element(
                                                            currentNode.arguments[
                                                                    srcNode.argumentIndex]
                                                        ),
                                                        PowersetLattice.Element(),
                                                    ),
                                                )
                                        }
                                        val values =
                                            if (!shortFS)
                                                doubleState.getNestedValues(
                                                    currentNode.arguments[srcNode.argumentIndex],
                                                    srcValueDepth,
                                                    fetchFields = true,
                                                )
                                            else
                                                identitySetOf(
                                                    currentNode.arguments[param.argumentIndex]
                                                )
                                        values.forEach { value ->
                                            destination.forEach { d ->
                                                // The extracted value might come from a state we
                                                // created for a shortfunctionSummary. If so, we
                                                // have to store that info in the map
                                                val shortFSEntry =
                                                    edgePropertiesMap[
                                                            Pair(
                                                                currentNode.arguments[
                                                                        srcNode.argumentIndex],
                                                                value,
                                                            )]
                                                        ?.filterIsInstance<Boolean>()
                                                        ?.any { it } == true || shortFS
                                                val currentSet =
                                                    mapDstToSrc.computeIfAbsent(d) {
                                                        identitySetOf()
                                                    }
                                                if (
                                                    currentSet.none {
                                                        it.first === value &&
                                                            it.second == shortFSEntry
                                                    }
                                                ) {
                                                    currentSet += Pair(value, shortFSEntry)
                                                }
                                            }
                                        }
                                    }
                                }

                                is ParameterMemoryValue -> {
                                    // In case the FunctionSummary says that we have to use the
                                    // dereferenced value here, we look up the argument,
                                    // dereference it, and then add it to the sources
                                    currentNode.invokes
                                        .flatMap { it.parameters }
                                        .filterTo(identitySetOf()) {
                                            it.name == srcNode.name.parent
                                        }
                                        .forEach {
                                            if (it.argumentIndex < currentNode.arguments.size) {
                                                val arg = currentNode.arguments[it.argumentIndex]
                                                destination.forEach { d ->
                                                    val currentSet =
                                                        mapDstToSrc.computeIfAbsent(d) {
                                                            identitySetOf()
                                                        }
                                                    doubleState
                                                        .getNestedValues(arg, srcValueDepth)
                                                        .forEach { value ->
                                                            if (
                                                                currentSet.none {
                                                                    it.first === value &&
                                                                        it.second == shortFS
                                                                }
                                                            ) {
                                                                currentSet += Pair(value, shortFS)
                                                            }
                                                        }
                                                }
                                            }
                                        }
                                    //                                    }
                                }

                                is MemoryAddress -> {
                                    destination.forEach { d ->
                                        val currentSet =
                                            mapDstToSrc.computeIfAbsent(d) { identitySetOf() }
                                        if (
                                            currentSet.none {
                                                it.first === srcNode && it.second == shortFS
                                            }
                                        ) {
                                            currentSet += Pair(srcNode, shortFS)
                                        }
                                    }
                                }

                                else -> {
                                    destination.forEach { d ->
                                        val currentSet =
                                            mapDstToSrc.computeIfAbsent(d) { identitySetOf() }
                                        val newSet =
                                            if (srcValueDepth == 0)
                                                identitySetOf(Pair(srcNode, shortFS))
                                            else
                                                doubleState
                                                    .getNestedValues(srcNode, srcValueDepth)
                                                    .map { Pair(it, shortFS) }

                                        newSet.forEach { pair ->
                                            if (
                                                currentSet.none {
                                                    it.first === pair.first &&
                                                        it.second == pair.second
                                                }
                                            ) {
                                                currentSet += pair
                                            }
                                        }
                                    }
                                }
                            }
                            // }
                        }
                }
            }
            i++
        }

        mapDstToSrc.forEach { (dst, values) ->
            // If the values of the destination are the same as the destination (e.g. if dst is a
            // CallExpression), we also add destinations to update the generalState, otherwise, the
            // destinationAddresses for the DeclarationState are enough
            val dstValues = doubleState.getValues(dst)
            doubleState =
                if (dstValues.all { it == dst })
                    doubleState.updateValues(lattice, values, dstValues, identitySetOf(dst))
                else doubleState.updateValues(lattice, values, identitySetOf(), identitySetOf(dst))

            // Add the callingContextOut to the edgePropertiesMap and check if we also need the
            // shortFS flag
            // TODO: Do we still need this?
            values.forEach { v ->
                if (v.second)
                // a shortFunctionSummary + the callingContextOut
                addEntryToEdgePropertiesMap(
                        Pair(dst, v.first),
                        identitySetOf(true, CallingContextOut(currentNode)),
                    )
                // the callingContextOut only
                else
                    addEntryToEdgePropertiesMap(
                        Pair(dst, v.first),
                        identitySetOf(CallingContextOut(currentNode)),
                    )
            }
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
                var newEntry = Pair<Node, Boolean>(currentNode.input, false)
                // If we already have exactly that entry, no need to re-write it, otherwise we might
                // confuse the iterateEOG function

                newEntry =
                    newDeclState[addr]?.second?.firstOrNull {
                        it.first === newEntry.first && it.second == newEntry.second
                    } ?: newEntry
                newDeclState.replace(
                    addr,
                    TripleLattice.Element(
                        PowersetLattice.Element(addr),
                        PowersetLattice.Element(newEntry),
                        PowersetLattice.Element(),
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
            val sources =
                currentNode.rhs
                    .flatMap { doubleState.getValues(it) }
                    .map { Pair(it, false) }
                    .toIdentitySet()
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
            val values = doubleState.getValues(currentNode)
            val lastWrites = doubleState.getLastWrites(currentNode)

            /*// If we have any information from the dereferenced value, we also fetch that
            values
                .filterTo(identitySetOf()) { doubleState.hasDeclarationStateEntry(it, true) }
                .flatMap {
                    doubleState.fetchElementFromDeclarationState(
                        addr = it,
                        excludeShortFSValues = true,
                    )
                }
                .map { it.first }
                .forEach { derefValue ->
                    values.add(derefValue)
                    // Store the information over the type of the edge in the edgePropertiesMap
                    addEntryToEdgePropertiesMap(
                        Pair(currentNode, derefValue),
                        identitySetOf(PointerDataflowGranularity(PointerAccess.currentDerefValue)),
                    )
                    // Let's see if we can deref once more
                    if (doubleState.hasDeclarationStateEntry(derefValue)) {
                        doubleState
                            .fetchElementFromDeclarationState(derefValue)
                            .map { it.first }
                            .forEach { derefDerefValue ->
                                values.add(derefDerefValue)
                                addEntryToEdgePropertiesMap(
                                    Pair(currentNode, derefDerefValue),
                                    identitySetOf(
                                        PointerDataflowGranularity(
                                            PointerAccess.currentDerefDerefValue
                                        )
                                    ),
                                )
                            }
                    }
                }*/

            doubleState =
                lattice.push(
                    doubleState,
                    currentNode,
                    GeneralStateEntryElement(
                        PowersetLattice.Element(addresses),
                        PowersetLattice.Element(values),
                        PowersetLattice.Element(lastWrites),
                    ),
                )

            // When we stored previously that the address was written to by a function, we use this
            // information now and attach the edge property to the actual read
            addresses.forEach { addr ->
                values.forEach { value ->
                    edgePropertiesMap[Pair(addr, value)]?.let {
                        addEntryToEdgePropertiesMap(Pair(currentNode, value), it)
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
                GeneralStateEntryElement(
                    PowersetLattice.Element(addresses),
                    values,
                    PowersetLattice.Element(),
                ),
            )
        /* In the DeclarationsState, we save the address which we wrote to the value for easier work with pointers
         */
        addresses.forEach { addr ->
            doubleState =
                lattice.pushToDeclarationsState(
                    doubleState,
                    addr,
                    DeclarationStateEntryElement(
                        PowersetLattice.Element(addresses),
                        PowersetLattice.Element(values.map { Pair(it, false) }.toIdentitySet()),
                        PowersetLattice.Element(Pair(currentNode, false)),
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
                            memoryAddresses.addAll(
                                addresses.filterIsInstance<MemoryAddress>()
                            ) /* TODO: might there also be more? */
                        }
                    // In the first step, we link the ParameterDeclaration to the PMV to be able to
                    // also access it outside the function
                    if (src is ParameterDeclaration) {
                        src.memoryValue = pmv
                        doubleState =
                            lattice.push(
                                doubleState,
                                pmv,
                                GeneralStateEntryElement(
                                    PowersetLattice.Element(),
                                    PowersetLattice.Element(src),
                                    PowersetLattice.Element(),
                                ),
                            )
                    } else {
                        // Link the PMVs with each other so that we can find them. This is
                        // especially important outside the respective function where we don't have
                        // a state
                        pmv.memoryAddresses.filterIsInstance<ParameterMemoryValue>().forEach {
                            it.memoryValues += pmv
                        }
                    }

                    // Update the states
                    val declStateElement =
                        DeclarationStateEntryElement(
                            PowersetLattice.Element(addresses),
                            PowersetLattice.Element(Pair(pmv, false)),
                            PowersetLattice.Element(),
                        )
                    val genStateElement =
                        GeneralStateEntryElement(
                            PowersetLattice.Element(addresses),
                            PowersetLattice.Element(pmv),
                            PowersetLattice.Element(),
                        )
                    addresses.forEach { addr ->
                        doubleState =
                            lattice.pushToDeclarationsState(doubleState, addr, declStateElement)
                    }
                    /*// We only write to the generalstate for the param-value itself
                    if (i == 0)*/ doubleState =
                        lattice.push(doubleState, src, genStateElement)

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

val PointsToStateElement.generalState: SingleGeneralStateElement
    get() = this.first

val PointsToStateElement.declarationsState: SingleDeclarationStateElement
    get() = this.second

/*fun PointsToStateElement.get(key: Node): StateEntryElement? {
    return this.generalState[key] ?: this.declarationsState[key]
}*/

fun PointsToStateElement.getFromDecl(key: Node): DeclarationStateEntryElement? {
    return this.declarationsState[key]
}

fun PointsToState.push(
    currentState: PointsToStateElement,
    newNode: Node,
    newLatticeElement: GeneralStateEntryElement,
): PointsToStateElement {
    // If we already have exactly that entry, no need to re-write it, otherwise we might confuse the
    // iterateEOG function
    val newLatticeCopy = newLatticeElement.duplicate()
    newLatticeCopy.third.removeAll { pair ->
        currentState.generalState[newNode]?.third?.any {
            it.first === pair.first && it.second.all { it in pair.second }
        } == true
    }

    val newGeneralState =
        this.innerLattice1.lub(
            currentState.generalState,
            MapLattice.Element(newNode to newLatticeCopy),
        )
    return PointsToStateElement(newGeneralState, currentState.declarationsState)
}

/** Pushes the [newNode] and its [newLatticeElement] to the [declarationsState]. */
fun PointsToState.pushToDeclarationsState(
    currentState: PointsToStateElement,
    newNode: Node,
    newLatticeElement: DeclarationStateEntryElement,
): PointsToStateElement {
    // If we already have exactly that entry, no need to re-write it, otherwise we might confuse the
    // iterateEOG function
    val newLatticeCopy = newLatticeElement.duplicate()
    newLatticeCopy.second.removeAll { pair ->
        currentState.declarationsState[newNode]?.second?.any {
            it.first === pair.first && it.second == pair.second
        } == true
    }

    val newDeclarationsState =
        this.innerLattice2.lub(
            currentState.declarationsState,
            MapLattice.Element(newNode to newLatticeCopy),
        )
    return PointsToStateElement(currentState.generalState, newDeclarationsState)
}

/** Check if `node` has an entry in the DeclarationState */
fun PointsToStateElement.hasDeclarationStateEntry(
    node: Node,
    excludeShortFSValues: Boolean = true,
): Boolean {

    return if (excludeShortFSValues)
        (this.declarationsState[node]?.second?.any { !it.second } == true)
    else (this.declarationsState[node]?.second?.isNotEmpty() == true)
}

/**
 * Fetch the entry for `addr` from the DeclarationState. If there isn't any, create an
 * UnknownMemoryValue
 */
fun PointsToStateElement.fetchElementFromDeclarationState(
    addr: Node,
    fetchFields: Boolean = false,
    excludeShortFSValues: Boolean = false,
): IdentitySet<Pair<Node, String>> {
    val ret = identitySetOf<Pair<Node, String>>()

    // For global nodes, we check the globalDerefs map
    if (isGlobal(addr)) {
        val element = globalDerefs[addr]
        if (element != null) element.map { ret.add(Pair(it.first, "")) }
        else {
            val newName = nodeNameToString(addr)
            val newEntry =
                nodesCreatingUnknownValues.computeIfAbsent(Pair(addr, newName)) {
                    UnknownMemoryValue(newName, true)
                }
            // TODO: Check if the boolean should be true sometimes
            globalDerefs[addr] = identitySetOf(Pair(newEntry, false))
            ret.add(Pair(newEntry, ""))
        }
    } else {

        // Otherwise, we read the declarationState.
        // Let's start with the main element
        var elements = this.declarationsState[addr]?.second?.toList()
        if (excludeShortFSValues) elements = elements?.filter { !it.second }
        if (elements.isNullOrEmpty()) {
            val newName = nodeNameToString(addr)
            val newEntry =
                nodesCreatingUnknownValues.computeIfAbsent(Pair(addr, newName)) {
                    UnknownMemoryValue(newName)
                }
            val newPair = Pair(newEntry, false)
            this.declarationsState.computeIfAbsent(addr) {
                TripleLattice.Element(
                    PowersetLattice.Element(addr),
                    PowersetLattice.Element(),
                    PowersetLattice.Element(),
                )
            }

            val newElements = this.declarationsState[addr]?.second
            if (
                newElements?.none { it.first === newPair.first && it.second == newPair.second } !=
                    false
            ) {
                newElements?.add(newPair)
            }
            ret.add(Pair(newEntry, ""))
        } else elements.map { ret.add(Pair(it.first, "")) }

        // if fetchFields is true, we also fetch the values for fields
        // TODO: handle globals
        if (fetchFields) {
            val fields =
                this.declarationsState[addr]?.first?.filterTo(identitySetOf()) { it != addr }
            this.declarationsState[addr]?.first?.filterTo(identitySetOf()) { it != addr }
            fields?.forEach { field ->
                this.declarationsState[field]?.second?.let {
                    it.map { ret.add(Pair(it.first, field.name.localName)) }
                }
            }
        }
    }

    return ret
}

fun PointsToStateElement.getLastWrites(node: Node): IdentitySet<Pair<Node, Set<Any>>> {
    return when (node) {
        is PointerReference -> {
            // For pointerReferences, we take the memoryAddress of the refersTo
            return (node.input as Reference).refersTo?.memoryAddresses?.mapTo(IdentitySet()) {
                Pair<Node, Set<Any>>(it, setOf())
            } ?: identitySetOf(Pair(node, setOf()))
        }
        else ->
            // For the rest, we read the declarationState to determine when the memoryAddress of the
            // node was last written to
            this.getAddresses(node)
                .filterTo(identitySetOf()) {
                    this.declarationsState[it]?.third?.isNotEmpty() == true
                }
                .flatMapTo(IdentitySet()) {
                    this.declarationsState[it]?.third!!.map {
                        Pair(it.first, setOf<Any>(it.second))
                    }
                }
    }
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
            if (node.memoryAddresses.isEmpty()) {
                node.memoryAddresses += MemoryAddress(node.name, isGlobal(node))
            }
            node.memoryAddresses
                .flatMap { fetchElementFromDeclarationState(it) }
                .map { it.first }
                .toIdentitySet()
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
                // For globals fetch the values from the globalDeref map
                if (isGlobal(node))
                    retVals.addAll(fetchElementFromDeclarationState(addr).map { it.first })
                else retVals.addAll(this.getValues(addr))
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
            if (node.memoryAddresses.isEmpty()) {
                node.memoryAddresses += MemoryAddress(node.name, isGlobal(node))
            }

            node.memoryAddresses.toIdentitySet()
        }
        is ParameterMemoryValue -> {
            node.memoryAddresses.toIdentitySet()
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
                /* In some cases, the refersTo might not yet have an initialized MemoryAddress, for example if it's a FunctionDeclaration. So let's do this here */
                if (refersTo.memoryAddresses.isEmpty()) {
                    refersTo.memoryAddresses += MemoryAddress(node.name, isGlobal(node))
                }

                refersTo.memoryAddresses.toIdentitySet()
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
 * derefdereference, etc... -1 returns the node
 */
fun PointsToStateElement.getNestedValues(
    node: Node,
    nestingDepth: Int,
    fetchFields: Boolean = false,
    onlyFetchExistingEntries: Boolean = false,
    excludeShortFSValues: Boolean = false,
): IdentitySet<Node> {
    if (nestingDepth == -1) return identitySetOf(node)
    if (nestingDepth == 0) return this.getAddresses(node)
    var ret = getValues(node)
    for (i in 1..<nestingDepth) {
        ret =
            ret.filterTo(identitySetOf()) {
                    if (onlyFetchExistingEntries)
                        this.hasDeclarationStateEntry(it, excludeShortFSValues)
                    else true
                }
                .flatMap {
                    this.fetchElementFromDeclarationState(it, fetchFields, excludeShortFSValues)
                }
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
            declarationsState[addr]?.first?.filterTo(identitySetOf()) {
                it.name.localName == nodeName.localName
            }

        if (elements.isNullOrEmpty()) {
            // val newName = nodeNameToString(addr)
            val newEntry =
                identitySetOf<Node>(
                    nodesCreatingUnknownValues.computeIfAbsent(Pair(addr, nodeName)) {
                        MemoryAddress(nodeName)
                    }
                )

            if (this.declarationsState[addr] == null) {
                this.declarationsState[addr] =
                    TripleLattice.Element(
                        PowersetLattice.Element(addr),
                        PowersetLattice.Element(),
                        PowersetLattice.Element(),
                    )
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
    sources: IdentitySet<Pair<Node, Boolean>>,
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

            // If we want to update the State with exactly the same elements as are already in the
            // state, we do nothing in order not to confuse the iterateEOG function
            /*sources.removeAll { pair ->
                this.declarationsState[destAddr]?.second?.any {
                    it.first === pair.first && it.second == pair.second
                } == true
            }*/
            val newSources =
                sources
                    .map { pair ->
                        this.declarationsState[destAddr]?.second?.firstOrNull {
                            it.first === pair.first && it.second == pair.second
                        } ?: pair
                    }
                    .toIdentitySet()
            /*            if (
             */
            /*newDeclState[destAddr]?.first != PowersetLattice.Element(currentEntries) ||*/
            /*
                    sources.all { src ->
                        newDeclState[destAddr]?.second?.none { it == src } == true
                    }
            ) {*/
            newDeclState[destAddr] =
                TripleLattice.Element(
                    PowersetLattice.Element(currentEntries),
                    PowersetLattice.Element(newSources),
                    PowersetLattice.Element(),
                )
            //            }
        } else {
            // TODO: We basically do the same as above, but currently we don't get the destinations
            // value from the call
            getValues(destAddr).forEach { addr ->
                newGenState[addr] =
                    GeneralStateEntryElement(
                        PowersetLattice.Element(destinationAddresses),
                        PowersetLattice.Element(sources.map { it.first }.toIdentitySet()),
                        PowersetLattice.Element(),
                    )
            }

            // Add the node to the globalDerefs. Don't delete the old one (except unknown values for
            // the node itself), b/c we never know with
            // global variables when they are used
            if (globalDerefs[destAddr] == null) globalDerefs[destAddr] = sources
            else {
                if (
                    globalDerefs[destAddr]!!.size == 1 &&
                        globalDerefs[destAddr]!!.first().first is UnknownMemoryValue &&
                        globalDerefs[destAddr]!!.first().first.name.localName ==
                            destAddr.name.localName
                )
                    globalDerefs[destAddr]!!.remove(globalDerefs[destAddr]!!.first())
                globalDerefs[destAddr]!!.addAll(sources)
            }
        }
    }

    /* Also update the generalState for dst (if we have any destinations) */
    destinations.forEach { d ->
        newGenState[d] =
            GeneralStateEntryElement(
                PowersetLattice.Element(destinationAddresses),
                PowersetLattice.Element(sources.map { it.first }.toIdentitySet()),
                PowersetLattice.Element(),
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
                GeneralStateEntryElement(
                    PowersetLattice.Element(aEaddresses),
                    PowersetLattice.Element(aEvalues),
                    PowersetLattice.Element(),
                ),
            )
    }

    return doubleState
}
