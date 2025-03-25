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

typealias GeneralStateEntry =
    TripleLattice<
        PowersetLattice.Element<Node>,
        PowersetLattice.Element<Node>,
        PowersetLattice.Element<Pair<Node, EqualLinkedHashSet<Any>>>,
    >

typealias DeclarationStateEntry =
    TripleLattice<
        PowersetLattice.Element<Node>,
        PowersetLattice.Element<Pair<Node, Boolean>>,
        PowersetLattice.Element<Pair<Node, EqualLinkedHashSet<Any>>>,
    >

/**
 * A typealias for an element in the generalState. The first element represent possible addresses,
 * the second element represents possible memory values and the third element represents the last
 * writes with their properties.
 */
typealias GeneralStateEntryElement =
    TripleLattice.Element<
        // Address
        PowersetLattice.Element<Node>,
        // MemoryValues
        PowersetLattice.Element<Node>,
        // prevDFG
        PowersetLattice.Element<Pair<Node, EqualLinkedHashSet<Any>>>,
    >

/**
 * A typealias for an element in the declarationState. The first element represent possible
 * addresses, the second element represents the values with an indication if it comes from a short
 * function summary and the third element represents the last writes with their properties.
 */
typealias DeclarationStateEntryElement =
    TripleLattice.Element<
        // Address
        PowersetLattice.Element<Node>,
        // Values (Node, shortFS yes or no)
        PowersetLattice.Element<Pair<Node, Boolean>>,
        // LastWrites (Node, Properties(shortFS yes or no, Granularity, ...))
        PowersetLattice.Element<Pair<Node, EqualLinkedHashSet<Any>>>,
    >

typealias SingleGeneralStateElement = MapLattice.Element<Node, GeneralStateEntryElement>

typealias SingleDeclarationStateElement = MapLattice.Element<Node, DeclarationStateEntryElement>

typealias SingleGeneralState = MapLattice<Node, GeneralStateEntryElement>

typealias SingleDeclarationState = MapLattice<Node, DeclarationStateEntryElement>

typealias PointsToStateElement =
    TupleLattice.Element<SingleGeneralStateElement, SingleDeclarationStateElement>

typealias PointsToState = TupleLattice<SingleGeneralStateElement, SingleDeclarationStateElement>

/**
 * Returns a name that allows a human to identify the node. Mostly, this is simply the node's
 * localName, but for Literals, it is their value
 */
fun getNodeName(node: Node): Name {
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
    edgePropertiesMap.computeIfAbsent(index) { identitySetOf() }.addAll(entries)
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
    // circles. Therefore, we store the chain of FunctionDeclarations we currently analyse
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
        // If the node has a body and a function summary, we have visited it before and can
        // return here.
        if (
            (node.functionSummary.isNotEmpty() && node.body != null) &&
                node.functionSummary.keys.any { it in node.parameters || it in node.returns }
        ) {
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

        val finalState =
            if (node.body == null) {
                handleEmptyFunctionDeclaration(lattice, startState, node)
            } else {
                lattice.iterateEOG(node.nextEOGEdges, startState, ::transfer)
            }

        for ((key, value) in finalState.generalState) {
            // The generalState values have 3 items: The address, the value, and the prevDFG-Edges
            // with a set of properties
            // Let's start with fetching the addresses
            if (key is HasMemoryAddress) {
                key.memoryAddresses.clear() // TODO: Do we really have to do this??
                key.memoryAddresses += value.first.filterIsInstance<MemoryAddress>()
            }

            // Then the memoryValues
            if (key is HasMemoryValue && value.second.isNotEmpty()) {
                value.second.forEach { v -> key.memoryValueEdges += Dataflow(v, key) }
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

    /**
     * This function draws the basic DFG-Edges based on the functionDeclaration, such as edges
     * between ParameterMemoryValues
     */
    private fun handleEmptyFunctionDeclaration(
        lattice: PointsToState,
        startState: TupleLattice.Element<SingleGeneralStateElement, SingleDeclarationStateElement>,
        functionDeclaration: FunctionDeclaration,
    ): PointsToStateElement {
        var doubleState = startState
        for ((param, fsEntries) in functionDeclaration.functionSummary) {
            fsEntries.forEach { (dstValueDepth, srcNode, srcValueDepth, subAccessName, shortFS) ->
                if (param is ParameterDeclaration && srcNode is ParameterDeclaration) {
                    var dst = param.memoryValue
                    for (i in 1..<dstValueDepth) {
                        // hop to the next deref-PMV
                        dst =
                            dst?.memoryValues
                                ?.filterIsInstance<ParameterMemoryValue>()
                                ?.singleOrNull { it.name.localName == "deref".repeat(i) + "value" }
                    }
                    var src = srcNode.memoryValue
                    for (i in 1..<srcValueDepth) {
                        src =
                            src?.memoryValues
                                ?.filterIsInstance<ParameterMemoryValue>()
                                ?.singleOrNull { it.name.localName == "deref".repeat(i) + "value" }
                    }
                    if (src != null && dst != null) {
                        val propertySet = equalLinkedHashSetOf<Any>()
                        if (subAccessName != "")
                            propertySet.add(FieldDeclaration().apply { name = Name(subAccessName) })
                        if (shortFS) propertySet.add(shortFS)
                        doubleState =
                            lattice.push(
                                doubleState,
                                dst,
                                GeneralStateEntryElement(
                                    PowersetLattice.Element(),
                                    PowersetLattice.Element(),
                                    PowersetLattice.Element(Pair(src, propertySet)),
                                ),
                            )
                    }
                }
            }
        }
        return doubleState
    }

    private fun storeFunctionSummary(node: FunctionDeclaration, doubleState: PointsToStateElement) {
        node.parameters.forEach { param ->
            // Collect all addresses of the parameter that we can use as index to look up possible
            // new values
            val indexes = mutableSetOf<Pair<Node, Int>>()
            val values = doubleState.getValues(param).mapTo(IdentitySet()) { it.first }

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
                .flatMap { doubleState.getValues(it).mapTo(IdentitySet()) { it.first } }
                .forEach { value ->
                    if (doubleState.hasDeclarationStateEntry(value)) indexes.add(Pair(value, 3))
                }

            indexes.forEach { (index, dstValueDepth) ->
                val stateEntries =
                    doubleState.fetchElementFromDeclarationState(index, true).filterTo(
                        identitySetOf()
                    ) {
                        it.addr.name != param.name
                    }
                stateEntries
                    // See if we can find something that is different from the initial value
                    .filterTo(identitySetOf()) {
                        !(it.addr is ParameterMemoryValue &&
                            it.addr.name.localName.contains("derefvalue") &&
                            it.addr.name.parent == param.name)
                    }
                    // If so, store the information for the parameter in the FunctionSummary
                    .forEach { (value, shortFS, subAccessName, lastWrites) ->
                        // TODO: Also extract the last write. To be able to do this for fields, we
                        //   need the info here on which address we are working, which we currently
                        //   lost by calling fetchElementsfromDeclarationsState.
                        // Extract the value depth from the value's localName
                        val srcValueDepth = stringToDepth(value.name.localName)
                        // Store the information in the functionSummary
                        val existingEntry =
                            node.functionSummary.computeIfAbsent(param) { mutableSetOf() }
                        existingEntry.add(
                            FunctionDeclaration.FSEntry(
                                dstValueDepth,
                                value,
                                srcValueDepth,
                                subAccessName,
                                shortFS,
                                lastWrites.mapTo(EqualLinkedHashSet()) { it.first },
                            )
                        )
                        // Additionally, we store this as a shortFunctionSummary were the Function
                        // writes to the parameter
                        existingEntry.add(
                            FunctionDeclaration.FSEntry(
                                dstValueDepth,
                                node,
                                0,
                                subAccessName,
                                true,
                                equalLinkedHashSetOf(node),
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
                                            equalLinkedHashSetOf(param),
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
                                FunctionDeclaration.FSEntry(
                                    0,
                                    it.first,
                                    1,
                                    "",
                                    false,
                                    equalLinkedHashSetOf(parentFD),
                                )
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
        var callingContext = CallingContextIn(setOf(callExpression))
        callExpression.arguments.forEach { arg ->
            if (arg.argumentIndex < functionDeclaration.parameters.size) {
                // Create a DFG-Edge from the argument to the parameter's memoryValue
                val p = functionDeclaration.parameters[arg.argumentIndex]
                if (p.memoryValue == null)
                    initializeParameters(lattice, mutableListOf(p), doubleState, 2)
                p.memoryValue?.let { paramVal ->
                    doubleState =
                        lattice.push(
                            doubleState,
                            paramVal,
                            GeneralStateEntryElement(
                                PowersetLattice.Element(paramVal),
                                PowersetLattice.Element(arg),
                                PowersetLattice.Element(
                                    Pair(arg, equalLinkedHashSetOf(callingContext))
                                ),
                            ),
                        )
                    // Also draw the edges for the (deref)derefvalues if we have any and are
                    // dealing with a pointer parameter (AKA memoryValue is not null)
                    paramVal.memoryValueEdges
                        .filter {
                            /*(it !is ContextSensitiveDataflow ||
                            it.callingContext.calls == callExpression) &&*/
                            it.start is ParameterMemoryValue &&
                                it.start.name.localName == "derefvalue"
                        }
                        .map { it.start }
                        .forEach { derefPMV ->
                            doubleState
                                .getNestedValues(
                                    arg,
                                    1,
                                    fetchFields = false,
                                    onlyFetchExistingEntries = true,
                                    excludeShortFSValues = true,
                                )
                                .forEach { (argVal, _) ->
                                    val argDerefVals =
                                        doubleState
                                            .getNestedValues(
                                                argVal,
                                                1,
                                                fetchFields = false,
                                                onlyFetchExistingEntries = true,
                                                excludeShortFSValues = true,
                                            )
                                            .mapTo(equalLinkedHashSetOf()) { it.first }
                                    val lastDerefWrites =
                                        doubleState.getLastWrites(argVal).mapTo(
                                            equalLinkedHashSetOf()
                                        ) {
                                            Pair(
                                                it.first,
                                                equalLinkedHashSetOf<Any>(callingContext),
                                            )
                                        }
                                    doubleState =
                                        lattice.push(
                                            doubleState,
                                            derefPMV,
                                            GeneralStateEntryElement(
                                                PowersetLattice.Element(paramVal),
                                                PowersetLattice.Element(argDerefVals),
                                                PowersetLattice.Element(lastDerefWrites),
                                            ),
                                        )
                                    // The same for the derefderef values
                                    (derefPMV as? HasMemoryValue)
                                        ?.memoryValues
                                        ?.filter { it.name.localName == "derefderefvalue" }
                                        ?.forEach { derefderefPMV ->
                                            argDerefVals
                                                .flatMap {
                                                    doubleState.getNestedValues(
                                                        it,
                                                        1,
                                                        fetchFields = false,
                                                        onlyFetchExistingEntries = true,
                                                        excludeShortFSValues = true,
                                                    )
                                                }
                                                .forEach { (derefderefValue, _) ->
                                                    val lastDerefDerefWrites =
                                                        argDerefVals
                                                            .flatMapTo(IdentitySet()) {
                                                                doubleState.getLastWrites(it)
                                                            }
                                                            .mapTo(IdentitySet()) {
                                                                Pair(
                                                                    it.first,
                                                                    equalLinkedHashSetOf<Any>(
                                                                        callingContext
                                                                    ),
                                                                )
                                                            }
                                                    doubleState =
                                                        lattice.push(
                                                            doubleState,
                                                            derefderefPMV,
                                                            GeneralStateEntryElement(
                                                                PowersetLattice.Element(derefPMV),
                                                                PowersetLattice.Element(
                                                                    derefderefValue
                                                                ),
                                                                PowersetLattice.Element(
                                                                    lastDerefDerefWrites
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

    //    data class MapDstToSrcEntry(val x: Node, val y: Node, val propertySet:
    // EqualLinkedHashSet<Any>, val lastWrites: IdentitySet<Node>)

    private fun handleCallExpression(
        lattice: PointsToState,
        currentNode: CallExpression,
        doubleState: PointsToStateElement,
    ): PointsToStateElement {
        var doubleState = doubleState
        var mapDstToSrc =
            mutableMapOf<
                Node,
                IdentitySet<Triple<Node, EqualLinkedHashSet<Node>, EqualLinkedHashSet<Any>>>,
            >()

        var i = 0
        // The toIdentitySet avoids having the same elements multiple times
        val invokes = currentNode.invokes.toIdentitySet().toList()
        while (i < invokes.size) {
            val invoke = calculateFunctionSummaries(invokes[i])

            doubleState =
                calculateIncomingCallingContexts(lattice, invoke, currentNode, doubleState)

            // If we have a FunctionSummary, we push the values of the arguments and return value
            // after executing the function call to our doubleState.
            for ((param, fsEntries) in invoke.functionSummary) {
                val argument =
                    when (param) {
                        is ParameterDeclaration -> {
                            // Dereference the parameter
                            if (param.argumentIndex < currentNode.arguments.size) {
                                currentNode.arguments[param.argumentIndex]
                            } else null
                        }
                        is ReturnStatement -> {
                            // param // .returnValue
                            currentNode
                        }
                        else -> null
                    }
                if (argument != null) {
                    fsEntries
                        .sortedBy { it.destValueDepth }
                        .forEach {
                            (
                                dstValueDepth,
                                srcNode,
                                srcValueDepth,
                                subAccessName,
                                shortFS,
                                lastWrites) ->
                            val destinations =
                                calculateCallExpressionDestination(
                                    doubleState,
                                    mapDstToSrc,
                                    dstValueDepth,
                                    subAccessName,
                                    argument,
                                )
                            // Collect the properties for the  DeclarationStateEntry
                            val propertySet: EqualLinkedHashSet<Any> =
                                if (subAccessName.isNotEmpty()) {
                                    equalLinkedHashSetOf(
                                        PartialDataflowGranularity(
                                            FieldDeclaration().apply { name = Name(subAccessName) }
                                        )
                                    )
                                } else equalLinkedHashSetOf()

                            // If this is a short FunctionSummary, store this info in the
                            // edgePropertiesMap
                            if (shortFS) {
                                val prev =
                                    (srcNode as? FunctionDeclaration)
                                        ?: currentNode.arguments[srcNode.argumentIndex]
                                addEntryToEdgePropertiesMap(
                                    Pair(argument, prev),
                                    identitySetOf(shortFS),
                                )
                            }
                            // Especially for shortFS, we need to update the prevDFGs with
                            // information we didn't have when creating the functionSummary.
                            // calculatePrev does this for us
                            val prev = calculatePrev(lastWrites, shortFS, currentNode)
                            mapDstToSrc =
                                addEntryToMap(
                                    lattice,
                                    doubleState,
                                    mapDstToSrc,
                                    destinations,
                                    srcNode,
                                    shortFS,
                                    argument,
                                    srcValueDepth,
                                    param,
                                    propertySet,
                                    currentNode,
                                    prev,
                                )
                        }
                }
            }
            i++
        }

        val callingContextOut = CallingContextOut(setOf(currentNode))
        mapDstToSrc.forEach { (dst, values) ->
            doubleState =
                writeMapEntriesToState(lattice, doubleState, dst, values, callingContextOut)
        }

        return doubleState
    }

    private fun calculatePrev(
        lastWrites: EqualLinkedHashSet<Node>,
        shortFS: Boolean,
        currentNode: CallExpression,
    ): EqualLinkedHashSet<Node> {
        val ret = equalLinkedHashSetOf<Node>()
        lastWrites.forEach { lw ->
            if (shortFS) {
                when (lw) {
                    is FunctionDeclaration -> ret.add(currentNode)
                    // is ParameterDeclaration -> ret.add(currentNode.arguments[lw.argumentIndex])
                    else -> ret.add(lw)
                }
            } else ret.add(lw)
        }
        return ret
    }

    private fun calculateFunctionSummaries(invoke: FunctionDeclaration): FunctionDeclaration {
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
        return invoke
    }

    private fun writeMapEntriesToState(
        lattice: PointsToState,
        doubleState: PointsToStateElement,
        dst: Node,
        values: IdentitySet<Triple<Node, EqualLinkedHashSet<Node>, EqualLinkedHashSet<Any>>>,
        callingContextOut: CallingContextOut,
    ): PointsToStateElement {
        // If the values of the destination are the same as the destination (e.g. if dst is a
        // CallExpression), we also add destinations to update the generalState, otherwise, the
        // destinationAddresses for the DeclarationState are enough
        val sources = values.mapTo(IdentitySet()) { Pair(it.first, true in it.third) }
        val lastWrites: IdentitySet<Pair<Node, EqualLinkedHashSet<Any>>> = identitySetOf()

        values.forEach { value ->
            value.second.forEach { lw ->
                if (value.third.singleOrNull() == true) lastWrites.add(Pair(lw, value.third))
                else
                    lastWrites.add(
                        Pair(
                            lw,
                            equalLinkedHashSetOf(*(value.third + callingContextOut).toTypedArray()),
                        )
                    )
            }
        }

        //        doubleState =
        //                if (dst is CallExpression)
        return doubleState.updateValues(
            lattice,
            sources,
            identitySetOf(dst),
            identitySetOf(dst),
            lastWrites,
        )
        /*        else
        doubleState.updateValues(
            lattice,
            sources,
            identitySetOf(),
            identitySetOf(dst),
            lastWrites,
        )*/
    }

    /**
     * Adds entries to the map that tracks the source nodes for each destination node.
     *
     * This method updates the `mapDstToSrc` map with the source nodes and their properties for each
     * destination node. It handles different types of source nodes, including
     * `ParameterDeclaration`, `ParameterMemoryValue`, `MemoryAddress`, and other nodes. Depending
     * on the type of source node, it may also update the general state to draw additional Data Flow
     * Graph (DFG) edges.
     *
     * @param lattice The lattice representing the points-to state.
     * @param doubleState The current state of the points-to analysis.
     * @param mapDstToSrc The map that tracks the source nodes for each destination node.
     * @param destination The set of destination nodes.
     * @param srcNode The source node to be added to the map.
     * @param shortFS A flag indicating if this is a short function summary.
     * @param argument The argument expression related to the source node.
     * @param srcValueDepth The depth of the source value.
     * @param param The parameter node related to the source node.
     * @param propertySet The set of properties associated with the source node.
     * @param currentNode The current call expression being analyzed.
     * @return The updated map that tracks the source nodes for each destination node.
     */
    private fun addEntryToMap(
        lattice: PointsToState,
        doubleState: PointsToStateElement,
        mapDstToSrc:
            MutableMap<
                Node,
                IdentitySet<Triple<Node, EqualLinkedHashSet<Node>, EqualLinkedHashSet<Any>>>,
            >,
        destination: IdentitySet<Node>,
        srcNode: Node,
        shortFS: Boolean,
        argument: Node,
        srcValueDepth: Int,
        param: Node,
        propertySet: EqualLinkedHashSet<Any>,
        currentNode: CallExpression,
        lastWrites: EqualLinkedHashSet<Node>,
    ): MutableMap<
        Node,
        IdentitySet<Triple<Node, EqualLinkedHashSet<Node>, EqualLinkedHashSet<Any>>>,
    > {
        var doubleState = doubleState
        when (srcNode) {
            is ParameterDeclaration -> {
                // Add the (dereferenced) value of the respective argument
                // in the CallExpression
                if (srcNode.argumentIndex < currentNode.arguments.size) {
                    // If this is a short FunctionSummary, we additionally
                    // update the generalState to draw the additional DFG Edges
                    if (shortFS) {
                        doubleState =
                            lattice.push(
                                doubleState,
                                argument,
                                GeneralStateEntryElement(
                                    PowersetLattice.Element(),
                                    PowersetLattice.Element(
                                        currentNode.arguments[srcNode.argumentIndex]
                                    ),
                                    PowersetLattice.Element(),
                                ),
                            )
                    }
                    val values =
                        if (!shortFS)
                            doubleState
                                .getNestedValues(
                                    currentNode.arguments[srcNode.argumentIndex],
                                    srcValueDepth,
                                    fetchFields = true,
                                    excludeShortFSValues = true,
                                )
                                .mapTo(IdentitySet()) { it.first }
                        else identitySetOf(currentNode.arguments[param.argumentIndex])
                    values.forEach { value ->
                        destination.forEach { d ->
                            // The extracted value might come from a state we
                            // created for a short function summary. If so, we
                            // have to store that info in the map
                            val updatedPropertySet = propertySet
                            updatedPropertySet.add(
                                edgePropertiesMap[
                                        Pair(currentNode.arguments[srcNode.argumentIndex], value)]
                                    ?.filterIsInstance<Boolean>()
                                    ?.any { it } == true || shortFS
                            )
                            val currentSet = mapDstToSrc.computeIfAbsent(d) { identitySetOf() }
                            if (
                                currentSet.none {
                                    it.first === value &&
                                        it.second == lastWrites.singleOrNull() /* ?: value)*/ &&
                                        it.third == updatedPropertySet
                                }
                            ) {
                                currentSet += Triple(value, lastWrites, updatedPropertySet)
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
                    .filterTo(identitySetOf()) { it.name == srcNode.name.parent }
                    .forEach {
                        if (it.argumentIndex < currentNode.arguments.size) {
                            val arg = currentNode.arguments[it.argumentIndex]
                            destination.forEach { d ->
                                val updatedPropertySet = propertySet
                                updatedPropertySet.add(shortFS)
                                val currentSet = mapDstToSrc.computeIfAbsent(d) { identitySetOf() }
                                doubleState.getNestedValues(arg, srcValueDepth).forEach { (value, _)
                                    ->
                                    if (
                                        currentSet.none {
                                            it.first === value &&
                                                it.second == lastWrites &&
                                                it.third == updatedPropertySet
                                        }
                                    ) {
                                        currentSet += Triple(value, lastWrites, updatedPropertySet)
                                    }
                                }
                            }
                        }
                    }
            }

            is MemoryAddress -> {
                destination.forEach { d ->
                    val currentSet = mapDstToSrc.computeIfAbsent(d) { identitySetOf() }
                    val updatedPropertySet = propertySet
                    updatedPropertySet.add(shortFS)
                    if (
                        currentSet.none {
                            it.first === srcNode &&
                                it.second === lastWrites &&
                                it.third == updatedPropertySet
                        }
                    ) {
                        currentSet += Triple(srcNode, lastWrites, updatedPropertySet)
                    }
                }
            }

            else -> {
                destination.forEach { d ->
                    val currentSet = mapDstToSrc.computeIfAbsent(d) { identitySetOf() }
                    val newSet =
                        if (srcValueDepth == 0) identitySetOf(Pair(srcNode, shortFS))
                        else
                            doubleState.getNestedValues(srcNode, srcValueDepth).mapTo(
                                IdentitySet()
                            ) {
                                Pair(it.first, shortFS)
                            }

                    newSet.forEach { pair ->
                        if (
                            currentSet.none {
                                it.first === pair.first &&
                                    it.second === lastWrites &&
                                    pair.second in it.third
                            }
                        ) {
                            val updatedPropertySet = propertySet
                            updatedPropertySet.add(shortFS)
                            currentSet += Triple(pair.first, lastWrites, updatedPropertySet)
                        }
                    }
                }
            }
        }
        return mapDstToSrc
    }

    private fun calculateCallExpressionDestination(
        doubleState: PointsToStateElement,
        mapDstToSrc:
            MutableMap<
                Node,
                IdentitySet<Triple<Node, EqualLinkedHashSet<Node>, EqualLinkedHashSet<Any>>>,
            >,
        dstValueDepth: Int,
        subAccessName: String,
        argument: Node,
    ): IdentitySet<Node> {
        val destAddrDepth = dstValueDepth - 1
        // Is the destAddrDepth > 2? In this case, the DeclarationState
        // might be outdated. So check in the mapDstToSrc for updates
        val updatedAddresses =
            mapDstToSrc.entries
                .filter {
                    it.key in doubleState.getValues(argument).mapTo(IdentitySet()) { it.first }
                }
                .flatMap { it.value }
                .mapTo(IdentitySet()) { it.first }

        return if (dstValueDepth > 2 && updatedAddresses.isNotEmpty()) {
            updatedAddresses
        } else {
            if (subAccessName.isNotEmpty()) {
                val fieldAddresses = identitySetOf<Node>()
                // Collect the fieldAddresses for each possible value
                val argumentValues =
                    doubleState.getNestedValues(argument, destAddrDepth, fetchFields = true)
                argumentValues.forEach { (v, _) ->
                    // We over approximate here and also add the main memory Address to the list of
                    // destinations
                    fieldAddresses.add(v)

                    val parentName = getNodeName(v)
                    val newName = Name(subAccessName, parentName)
                    fieldAddresses.addAll(
                        doubleState.fetchFieldAddresses(identitySetOf(v), newName)
                    )
                }
                fieldAddresses
            } else {
                doubleState.getNestedValues(argument, destAddrDepth).mapTo(IdentitySet()) {
                    it.first
                }
            }
        }
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
                var newValueEntry = Pair<Node, Boolean>(currentNode, false)
                var newLastWriteEntry =
                    Pair<Node, EqualLinkedHashSet<Any>>(
                        currentNode.input,
                        equalLinkedHashSetOf(false),
                    )
                // If we already have exactly that entry, no need to re-write it, otherwise we might
                // confuse the iterateEOG function
                newValueEntry =
                    newDeclState[addr]?.second?.firstOrNull {
                        it.first === newValueEntry.first && it.second == newValueEntry.second
                    } ?: newValueEntry
                newLastWriteEntry =
                    newDeclState[addr]?.third?.firstOrNull {
                        it.first === newLastWriteEntry.first &&
                            it.second == newLastWriteEntry.second
                    } ?: newLastWriteEntry

                newDeclState.replace(
                    addr,
                    DeclarationStateEntryElement(
                        PowersetLattice.Element(addr),
                        PowersetLattice.Element(newValueEntry),
                        PowersetLattice.Element(newLastWriteEntry),
                    ),
                )
            }
            doubleState =
                PointsToStateElement(doubleState.generalState, MapLattice.Element(newDeclState))
        }

        doubleState =
            lattice.push(
                doubleState,
                currentNode,
                GeneralStateEntryElement(
                    PowersetLattice.Element(doubleState.getAddresses(currentNode)),
                    PowersetLattice.Element(
                        doubleState.getValues(currentNode).mapTo(IdentitySet()) { it.first }
                    ),
                    PowersetLattice.Element(),
                ),
            )

        return doubleState
    }

    private fun handleAssignExpression(
        lattice: PointsToState,
        currentNode: AssignExpression,
        doubleState: PointsToStateElement,
    ): PointsToStateElement {
        var doubleState = doubleState
        /* For AssignExpressions, we update the value of the lhs with the rhs
         * In C(++), both the lhs and the rhs should only have one element
         */
        if (currentNode.lhs.size == 1 && currentNode.rhs.size == 1) {
            val sources = currentNode.rhs.flatMapTo(IdentitySet()) { doubleState.getValues(it) }
            // .map { Pair(it, false) }
            // .toIdentitySet()
            val destinations: IdentitySet<Node> = currentNode.lhs.toIdentitySet()
            val destinationsAddresses =
                destinations.flatMap { doubleState.getAddresses(it) }.toIdentitySet()
            val lastWrites =
                destinations.mapTo(IdentitySet()) { Pair(it, equalLinkedHashSetOf<Any>(false)) }
            doubleState =
                doubleState.updateValues(
                    lattice,
                    sources,
                    destinations,
                    destinationsAddresses,
                    lastWrites,
                )
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
            if (currentNode is Reference || currentNode is BinaryOperator) currentNode.access
            else if (currentNode is SubscriptExpression && currentNode.arrayExpression is Reference)
                (currentNode.arrayExpression as Reference).access
            else null
        if (access in setOf(AccessValues.READ, AccessValues.READWRITE)) {
            val addresses = doubleState.getAddresses(currentNode)
            val values =
                doubleState
                    .getValues(currentNode)
                    // Filter only the values that are not stored for short FunctionSummaries (aka
                    // it.second set to true)
                    .filter { !it.second }
                    .mapTo(IdentitySet()) { it.first }
            val prevDFGs = doubleState.getLastWrites(currentNode)

            // If we have any information from the dereferenced value, we also fetch that
            values
                .filterTo(identitySetOf()) { doubleState.hasDeclarationStateEntry(it, true) }
                /*                .flatMap {
                 */
                /*                    doubleState.fetchElementFromDeclarationState(
                    addr = it,
                    excludeShortFSValues = true,
                )*/
                /*
                    doubleState.getLastWrites(it).filter { it.second.none { it == true } }
                }*/
                .forEach { value ->
                    // draw the DFG Edges
                    val currentderefGranularity =
                        equalLinkedHashSetOf<Any>(
                            PointerDataflowGranularity(PointerAccess.currentDerefValue)
                        )
                    doubleState
                        .getLastWrites(value)
                        .filter { it.second.none { it == true } }
                        .forEach { prevDFGs.add(Pair(it.first, currentderefGranularity)) }

                    // Let's see if we can deref once more
                    val derefValues =
                        doubleState
                            .fetchElementFromDeclarationState(
                                addr = value,
                                excludeShortFSValues = true,
                            )
                            .map { it.addr }
                            .forEach { derefValue ->
                                if (doubleState.hasDeclarationStateEntry(derefValue)) {
                                    doubleState
                                        .fetchElementFromDeclarationState(derefValue)
                                        .forEach { (derefDerefValue, _, _) ->
                                            val currentderefderefGranularity =
                                                equalLinkedHashSetOf<Any>(
                                                    PointerDataflowGranularity(
                                                        PointerAccess.currentDerefDerefValue
                                                    )
                                                )
                                            doubleState
                                                .getLastWrites(derefValue)
                                                .filter { it.second.none { it == true } }
                                                .forEach {
                                                    prevDFGs.add(
                                                        Pair(it.first, currentderefderefGranularity)
                                                    )
                                                }
                                        }
                                }
                            }
                }

            doubleState =
                lattice.push(
                    doubleState,
                    currentNode,
                    GeneralStateEntryElement(
                        PowersetLattice.Element(addresses),
                        PowersetLattice.Element(values),
                        PowersetLattice.Element(prevDFGs),
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
        } else {
            // We write to this node, but maybe we probably want to store the memory address which
            // it has right now
            doubleState =
                lattice.push(
                    doubleState,
                    currentNode,
                    GeneralStateEntryElement(
                        PowersetLattice.Element(doubleState.getAddresses(currentNode)),
                        PowersetLattice.Element(),
                        PowersetLattice.Element(),
                    ),
                )
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
            else values.addAll(doubleState.getValues(initializer).mapTo(IdentitySet()) { it.first })
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
                        PowersetLattice.Element(Pair(currentNode, equalLinkedHashSetOf<Any>(false))),
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
                                src,
                                GeneralStateEntryElement(
                                    PowersetLattice.Element(),
                                    PowersetLattice.Element(pmv),
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
                            PowersetLattice.Element(Pair(pmv, equalLinkedHashSetOf())),
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
                    doubleState = lattice.push(doubleState, src, genStateElement)

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
            it.first === pair.first && it.second == pair.second
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
    newLatticeCopy.third.removeAll { pair ->
        currentState.declarationsState[newNode]?.third?.any {
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

data class fetchElementFromDeclarationStateEntry(
    val addr: Node,
    val shortFS: Boolean,
    val subAccessName: String,
    val lastWrites: IdentitySet<Pair<Node, EqualLinkedHashSet<Any>>>,
)

/**
 * Fetch the entry for `addr` from the DeclarationState. If there isn't any, create an
 * UnknownMemoryValue
 */
fun PointsToStateElement.fetchElementFromDeclarationState(
    addr: Node,
    fetchFields: Boolean = false,
    excludeShortFSValues: Boolean = false,
): IdentitySet<fetchElementFromDeclarationStateEntry> {
    val ret = identitySetOf<fetchElementFromDeclarationStateEntry>()

    // For global nodes, we check the globalDerefs map
    if (isGlobal(addr)) {
        val element = globalDerefs[addr]
        if (element != null)
            element.map {
                ret.add(fetchElementFromDeclarationStateEntry(it.first, false, "", identitySetOf()))
            }
        else {
            val newName = getNodeName(addr)
            val newEntry =
                nodesCreatingUnknownValues.computeIfAbsent(Pair(addr, newName)) {
                    UnknownMemoryValue(newName, true)
                }
            // TODO: Check if the boolean should be true sometimes
            globalDerefs[addr] = identitySetOf(Pair(newEntry, false))
            ret.add(fetchElementFromDeclarationStateEntry(newEntry, false, "", identitySetOf()))
        }
    } else {

        // Otherwise, we read the declarationState.
        // Let's start with the main element
        var elements = this.declarationsState[addr]?.second?.toList()
        if (excludeShortFSValues) elements = elements?.filter { !it.second }
        if (elements.isNullOrEmpty()) {
            val newName = getNodeName(addr)
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
            ret.add(fetchElementFromDeclarationStateEntry(newEntry, false, "", identitySetOf()))
        } else
            elements.map {
                ret.add(
                    fetchElementFromDeclarationStateEntry(
                        it.first,
                        it.second,
                        "",
                        this.declarationsState[addr]?.third ?: identitySetOf(),
                    )
                )
            }

        // if fetchFields is true, we also fetch the values for fields
        // TODO: handle globals
        if (fetchFields) {
            val fields =
                this.declarationsState[addr]?.first?.filterTo(identitySetOf()) { it != addr }
            fields?.forEach { field ->
                this.declarationsState[field]
                    ?.second
                    ?.filter { if (excludeShortFSValues) !it.second else true }
                    ?.let {
                        it.map {
                            ret.add(
                                fetchElementFromDeclarationStateEntry(
                                    it.first,
                                    it.second,
                                    field.name.localName,
                                    this.declarationsState[field]?.third ?: identitySetOf(),
                                )
                            )
                        }
                    }
            }
        }
    }

    return ret
}

fun PointsToStateElement.getLastWrites(
    node: Node
): EqualLinkedHashSet<Pair<Node, EqualLinkedHashSet<Any>>> {
    return when (node) {
        is PointerReference -> {
            // For pointerReferences, we take the memoryAddress of the refersTo
            return (node.input as Reference).refersTo?.memoryAddresses?.mapTo(
                EqualLinkedHashSet()
            ) {
                Pair<Node, EqualLinkedHashSet<Any>>(it, equalLinkedHashSetOf())
            } ?: equalLinkedHashSetOf(Pair(node, equalLinkedHashSetOf()))
        }
        is PointerDereference -> {
            val ret = equalLinkedHashSetOf<Pair<Node, EqualLinkedHashSet<Any>>>()
            this.getAddresses(node).forEach { addr ->
                val lastWrite = this.declarationsState[addr]?.third
                // Usually, we should have a lastwrite, so we take that
                if (lastWrite?.isNotEmpty() == true)
                    lastWrite.mapTo(IdentitySet()) { ret.add(Pair(it.first, it.second)) }
                // However, there might be cases were we don't yet have written to the dereferenced
                // value, in this case we return an UnknownMemoryValue
                else {
                    val newName = Name(getNodeName(addr).localName + ".derefvalue")
                    ret.add(
                        Pair(
                            nodesCreatingUnknownValues.computeIfAbsent(Pair(addr, newName)) {
                                UnknownMemoryValue(newName)
                            },
                            equalLinkedHashSetOf(),
                        )
                    )
                }
            }
            return ret
        }
        is SubscriptExpression -> {
            // For SubScriptExpressions, we additionally check if the partial write matches
            val partial = getNodeName(node.subscriptExpression)
            this.getAddresses(node)
                .filterTo(equalLinkedHashSetOf()) {
                    this.declarationsState[it]?.third?.isNotEmpty() == true
                }
                .flatMapTo(EqualLinkedHashSet()) {
                    this.declarationsState[it]?.third?.mapNotNull {
                        Pair(
                            it.first,
                            it.second.filterTo(EqualLinkedHashSet()) {
                                !(it is PartialDataflowGranularity<*> &&
                                    it.partialTarget is FieldDeclaration &&
                                    it.partialTarget.name.localName == partial.localName)
                            },
                        )
                    } ?: setOf()
                }
        }
        else ->
            // For the rest, we read the declarationState to determine when the memoryAddress of the
            // node was last written to
            this.getAddresses(node)
                .filterTo(equalLinkedHashSetOf()) {
                    this.declarationsState[it]?.third?.isNotEmpty() == true
                }
                .flatMapTo(EqualLinkedHashSet()) {
                    this.declarationsState[it]?.third?.map { Pair(it.first, it.second) } ?: setOf()
                }
    }
}

fun PointsToStateElement.getValues(node: Node): IdentitySet<Pair<Node, Boolean>> {
    return when (node) {
        is PointerReference -> {
            /*
             * For PointerReferences, the value is the address of the input
             * For example, the value of `&i` is the address of `i`
             */
            this.getAddresses(node.input).mapTo(IdentitySet()) { Pair(it, false) }
        }
        is PointerDereference -> {
            /* To find the value for PointerDereferences, we first check what's the current value of the input, which is probably a MemoryAddress
             * Then we look up the current value at this MemoryAddress
             */
            val inputVal =
                /*                        when (node.input) {
                is Reference -> this.getValues(node.input)
                else -> // TODO: How can we handle other cases?*/
                this.getValues(node.input).map { it.first }
            //                        }
            val retVal = identitySetOf<Pair<Node, Boolean>>()
            inputVal.forEach { input ->
                retVal.addAll(
                    fetchElementFromDeclarationState(input, true).map { Pair(it.addr, it.shortFS) }
                )
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
                .map { it.addr }
                //                .toIdentitySet()
                .mapTo(IdentitySet()) { Pair(it, false) }
        }
        is MemoryAddress,
        is CallExpression -> {
            fetchElementFromDeclarationState(node).mapTo(IdentitySet()) {
                Pair(it.addr, it.shortFS)
            }
        }
        is MemberExpression -> {
            val (base, fieldName) = resolveMemberExpression(node)
            val baseAddresses = getAddresses(base).toIdentitySet()
            val fieldAddresses = fetchFieldAddresses(baseAddresses, fieldName)
            if (fieldAddresses.isNotEmpty()) {
                fieldAddresses.flatMapTo(IdentitySet()) {
                    fetchElementFromDeclarationState(it).map { Pair(it.addr, it.shortFS) }
                }
            } else {
                val newName = Name(getNodeName(node).localName, base.name)
                identitySetOf(
                    Pair(
                        nodesCreatingUnknownValues.computeIfAbsent(Pair(node, newName)) {
                            UnknownMemoryValue(newName)
                        },
                        false,
                    )
                )
            }
        }
        is Reference -> {
            /* For References, we have to look up the last value written to its declaration. */
            val retVals = identitySetOf<Pair<Node, Boolean>>()
            this.getAddresses(node).forEach { addr ->
                // For globals fetch the values from the globalDeref map
                if (isGlobal(node))
                    retVals.addAll(
                        fetchElementFromDeclarationState(addr).map { Pair(it.addr, false) }
                    )
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
        else -> identitySetOf(Pair(node, false))
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
             * PointerDereferences have as address the value of their input.
             * For example, the address of `*a` is the value of `a`
             */
            this.getValues(node.input).mapTo(IdentitySet()) { it.first }
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
             * For references, the address is the same as for the declaration, AKA the refersTo
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
             * For CastExpressions we take the expression as the cast itself does not have any impact on the address
             */
            this.getAddresses(node.expression)
        }
        is SubscriptExpression -> {
            val localName = getNodeName(node.subscriptExpression)
            this.getValues(node.base)
                .flatMap {
                    fetchFieldAddresses(
                        identitySetOf(it.first),
                        Name(localName.localName, getNodeName(it.first)),
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
): IdentitySet<Pair<Node, Boolean>> {
    if (nestingDepth == -1) return identitySetOf(Pair(node, false))
    if (nestingDepth == 0) return this.getAddresses(node).mapTo(IdentitySet()) { Pair(it, false) }
    var ret =
        if (
            node !is PointerReference &&
                onlyFetchExistingEntries &&
                this.getAddresses(node).none { addr ->
                    this.hasDeclarationStateEntry(addr, excludeShortFSValues)
                }
        )
            identitySetOf()
        else
            getValues(node).filterTo(IdentitySet()) { /*it.second != excludeShortFSValues*/
                if (excludeShortFSValues) !it.second else true
            }
    for (i in 1..<nestingDepth) {
        ret =
            ret.filterTo(identitySetOf()) {
                    if (onlyFetchExistingEntries)
                        this.hasDeclarationStateEntry(it.first, excludeShortFSValues)
                    else true
                }
                .flatMap {
                    this.fetchElementFromDeclarationState(
                        it.first,
                        fetchFields,
                        excludeShortFSValues,
                    )
                }
                .mapTo(IdentitySet()) { Pair(it.addr, it.shortFS) }
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
    // Node and short FS yes or no
    lastWrites: IdentitySet<Pair<Node, EqualLinkedHashSet<Any>>>,
): PointsToStateElement {
    val newDeclState = this.declarationsState.duplicate()
    val newGenState = this.generalState.duplicate()

    /* Update the declarationState for the addresses */
    destinationAddresses.forEach { destAddr ->
        // Clear previous entries in edgePropertiesMap
        edgePropertiesMap
            .filter { it.key.first === destAddr }
            .forEach { entry -> edgePropertiesMap.remove(entry.key) }

        if (!isGlobal(destAddr)) {
            val currentEntries =
                this.declarationsState[destAddr]?.first?.toIdentitySet() ?: identitySetOf(destAddr)

            // If we want to update the State with exactly the same elements as are already in the
            // state, we do nothing in order not to confuse the iterateEOG function

            val newSources =
                sources.mapTo(IdentitySet()) { pair ->
                    this.declarationsState[destAddr]?.second?.firstOrNull {
                        it.first === pair.first && it.second == pair.second
                    } ?: pair
                }
            /* val newPrevDFG =
            // TODO: Do we also need to fetch some properties here?
            lastWrites.mapTo(IdentitySet()) { Pair(it, false) }*/
            // If we already have exactly this value in the state for the prevDFGs, we take that in
            // order not to confuse the iterateEOG function
            val prevDFG = identitySetOf<Pair<Node, EqualLinkedHashSet<Any>>>()
            lastWrites.forEach { lw ->
                val existingEntries =
                    newDeclState[destAddr]?.third?.filter {
                        it.first === lw.first && it.second == lw.second
                    }
                if (existingEntries?.isNotEmpty() == true) prevDFG.addAll(existingEntries)
                else prevDFG.add(lw)
            }

            newDeclState[destAddr] =
                DeclarationStateEntryElement(
                    PowersetLattice.Element(currentEntries),
                    PowersetLattice.Element(newSources),
                    PowersetLattice.Element(prevDFG),
                )
        } else {
            // TODO: We basically do the same as above, but currently we don't get the destinations
            // value from the call
            getValues(destAddr).forEach { (addr, _) ->
                newGenState[addr] =
                    GeneralStateEntryElement(
                        PowersetLattice.Element(destinationAddresses),
                        PowersetLattice.Element(sources.mapTo(IdentitySet()) { it.first }),
                        PowersetLattice.Element(),
                    )
            }

            // Add the node to the globalDerefs. Don't delete the old one (except unknown values for
            // the node itself), b/c we never know with global variables when they are used
            val globalDerefsDst = globalDerefs.computeIfAbsent(destAddr) { identitySetOf() }
            if (
                globalDerefsDst.size == 1 &&
                    globalDerefsDst.first().first is UnknownMemoryValue &&
                    globalDerefsDst.first().first.name.localName == destAddr.name.localName
            )
                globalDerefsDst.clear()
            globalDerefsDst.addAll(sources)
        }
    }

    /* Also update the generalState for dst (if we have any destinations) */
    // If the lastWrites are in the sources or destinations, we don't have to set the prevDFG edges
    lastWrites.removeIf { lw ->
        sources.any { src -> src.first === lw.first && src.second in lw.second } ||
            lw.first in destinations
    }
    destinations.forEach { d ->
        newGenState[d] =
            GeneralStateEntryElement(
                PowersetLattice.Element(destinationAddresses),
                PowersetLattice.Element(sources.mapTo(IdentitySet()) { it.first }),
                PowersetLattice.Element(lastWrites),
            )
    }

    var doubleState = PointsToStateElement(newGenState, newDeclState)

    /* When we are dealing with SubscriptExpression, we also have to initialize the arrayExpression
    , since that hasn't been done yet */
    /*destinations.filterIsInstance<SubscriptExpression>().forEach { d ->
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
    }*/

    return doubleState
}
