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
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.helpers.IdentitySet
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.helpers.functional.*
import de.fraunhofer.aisec.cpg.helpers.identitySetOf
import de.fraunhofer.aisec.cpg.helpers.toIdentitySet
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.Pair
import kotlin.collections.filter
import kotlin.collections.map
import kotlin.let
import kotlin.text.contains
import kotlinx.coroutines.*

val nodesCreatingUnknownValues = ConcurrentHashMap<Pair<Node, Name>, MemoryAddress>()
var totalFunctionDeclarationCount = 0
var analyzedFunctionDeclarationCount = 0

typealias GeneralStateEntry =
    TripleLattice<
        PowersetLattice.Element<Node>,
        PowersetLattice.Element<Pair<Node, EqualLinkedHashSet<Any>>>,
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
        PowersetLattice.Element<Pair<Node, EqualLinkedHashSet<Any>>>,
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

/** clear dummys from a FunctionSummary */
fun clearFSDummies(functionSummary: MutableMap<Node, MutableSet<FunctionDeclaration.FSEntry>>) {
    // Do not clear the dummies for which we don't have bodies, only the "obvious" dummies we
    // created for recursive functions
    functionSummary
        .filter { (it.key as? Literal<*>)?.value == "dummy" }
        .keys
        .forEach { functionSummary.remove(it) }
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
        is MemberExpression -> isGlobal(node.base)
        is Reference -> (node.refersTo as? VariableDeclaration)?.isGlobal == true
        is MemoryAddress -> node.isGlobal
        else -> false
    }
}

// We also need a place to store the derefs of global variables. The Boolean indicates if this is a
// value stored for a short function Summary
var globalDerefs = mutableMapOf<Node, PowersetLattice.Element<Pair<Node, Boolean>>>()

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

        /**
         * specifies if we draw the current(deref)derefvalue-DFG Edges. Not sure if we want/need
         * them
         */
        var drawCurrentDerefDFG: Boolean = true,
    ) : PassConfiguration()

    // For recursive creation of FunctionSummaries, we have to make sure that we don't run in
    // circles. Therefore, we store the chain of FunctionDeclarations we currently analyse
    private val functionSummaryAnalysisChain = mutableListOf<FunctionDeclaration>()

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

        // If we haven't done so yet, set the total number of functions
        if (totalFunctionDeclarationCount == 0)
            totalFunctionDeclarationCount =
                node.firstParentOrNull<TranslationUnitDeclaration>().functions.size

        analyzedFunctionDeclarationCount++
        functionSummaryAnalysisChain.add(node)

        // Calculate the complexity of the function and see, if it exceeds our threshold
        val max = passConfig<Configuration>()?.maxComplexity
        val c = node.body?.cyclomaticComplexity() ?: 0
        if (max != null && c > max) {
            log.info(
                "Ignoring function ${node.name} because its complexity (${NumberFormat.getNumberInstance(Locale.US).format(c)}) is greater than the configured maximum (${max})"
            )
            // Add an empty function Summary so that we don't try again
            node.functionSummary.computeIfAbsent(ReturnStatement()) { mutableSetOf() }
            return
        }

        log.info(
            "Analyzing function ${node.name}. Complexity: ${NumberFormat.getNumberInstance(Locale.US).format(c)}. (Function $analyzedFunctionDeclarationCount / $totalFunctionDeclarationCount)"
        )

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
                key.memoryAddresses += value.first.filterIsInstance<MemoryAddress>()
            }

            // Then the memoryValues
            if (key is HasMemoryValue && value.second.isNotEmpty()) {
                value.second.forEach { (v, properties) ->
                    var granularity = default()
                    var shortFS = false
                    properties.forEach { p ->
                        when (p) {
                            is String -> granularity = PartialDataflowGranularity(p)
                            is Boolean -> shortFS = p
                            else -> TODO()
                        }
                    }
                    key.memoryValueEdges += Dataflow(v, key, granularity, shortFS)
                }
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
        if (functionSummaryAnalysisChain.last() == node) functionSummaryAnalysisChain.remove(node)
        else
            log.error(
                "finished analyzing $node, which is not at the end of the functionSummaryAnalsysis chain, which is surprising"
            )
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
        if (functionDeclaration.functionSummary.isEmpty()) {
            // Add a dummy function summary so that we don't try this every time
            // In this dummy, all parameters point to the return
            // TODO: Also add possible dereference values to the input?
            val prevDFGs = PowersetLattice.Element<Pair<Node, EqualLinkedHashSet<Any>>>()
            val newEntries =
                mutableSetOf(FunctionDeclaration.FSEntry(0, functionDeclaration, 1, ""))
            functionDeclaration.parameters.forEach { param ->
                // The short FS
                newEntries.add(
                    FunctionDeclaration.FSEntry(
                        0,
                        null,
                        1,
                        "",
                        PowersetLattice.Element(Pair(param, equalLinkedHashSetOf())),
                        equalLinkedHashSetOf(true),
                        true,
                    )
                )
                // The prevDFG edges for the function Declaration
                doubleState
                    .getValues(param, param)
                    .filter { it.first is ParameterMemoryValue }
                    .forEach { prevDFGs.add(Pair(it.first, equalLinkedHashSetOf<Any>())) }
            }
            val rets = mutableSetOf<Node>()
            if (functionDeclaration.returns.isNotEmpty()) rets.addAll(functionDeclaration.returns)
            else rets.add(functionDeclaration)
            rets.forEach { ret -> functionDeclaration.functionSummary[ret] = newEntries }
            // draw a DFG-Edge from all parameters to the FunctionDeclaration
            doubleState =
                lattice.push(
                    doubleState,
                    functionDeclaration,
                    GeneralStateEntryElement(
                        PowersetLattice.Element(),
                        PowersetLattice.Element(),
                        PowersetLattice.Element(prevDFGs),
                    ),
                )
            return doubleState
        }

        for ((param, fsEntries) in functionDeclaration.functionSummary) {
            fsEntries.forEach { entry ->
                if (param is ParameterDeclaration && entry.srcNode is ParameterDeclaration) {
                    val dst =
                        doubleState
                            .getNestedValues(param, entry.destValueDepth, false, true, true)
                            .map { it.first }
                            .singleOrNull()
                    val src =
                        doubleState
                            .getNestedValues(entry.srcNode, entry.srcValueDepth, false, true, true)
                            .map { it.first }
                            .singleOrNull()
                    if (src != null && dst != null) {
                        // We couldn't set the lastWrites when creating the functionSummary (which
                        // has to be hardcoded b/c we don't have a body), so we replace that now
                        entry.lastWrites.forEach {
                            functionDeclaration.functionSummary[param]
                                ?.singleOrNull { it == entry }
                                ?.lastWrites
                                ?.remove(it)
                        }
                        functionDeclaration.functionSummary[param]
                            ?.singleOrNull { it == entry }
                            ?.lastWrites
                            ?.add(Pair(dst, equalLinkedHashSetOf()))
                        val propertySet = equalLinkedHashSetOf<Any>()
                        if (entry.subAccessName != "")
                            propertySet.add(
                                FieldDeclaration().apply { name = Name(entry.subAccessName) }
                            )
                        val shortFsEntry = entry.properties.singleOrNull { it is Boolean }
                        if (shortFsEntry != null) propertySet.add(shortFsEntry)
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
        clearFSDummies(node.functionSummary)
        node.parameters.forEach { param ->
            // Collect all addresses of the parameter that we can use as index to look up possible
            // new values
            val indexes = mutableSetOf<Pair<Node, Int>>()
            val values = doubleState.getValues(param, param).mapTo(IdentitySet()) { it.first }

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
                .flatMap {
                    doubleState.getValues(it, it).mapTo(PowersetLattice.Element()) { it.first }
                }
                .forEach { value ->
                    if (doubleState.hasDeclarationStateEntry(value)) indexes.add(Pair(value, 3))
                }

            indexes.forEach { (index, dstValueDepth) ->
                val stateEntries =
                    doubleState.fetchValueFromDeclarationState(index, true, true).filterTo(
                        PowersetLattice.Element()
                    ) {
                        it.value.name != param.name
                    }
                stateEntries
                    /* See if we can find something that is different from the initial value*/
                    .filterTo(PowersetLattice.Element()) {
                        /* Filter the PMVs from this parameter*/
                        !(it.value is ParameterMemoryValue &&
                            it.value.name.localName.contains("derefvalue") &&
                            it.value.name.parent == param.name)
                        /* Filter the unknownMemoryValues that weren't written to*/
                        && !(it.value is UnknownMemoryValue && it.lastWrites.isEmpty())
                    }
                    // If so, store the information for the parameter in the FunctionSummary
                    .forEach { (value, shortFS, subAccessName, lastWrites) ->
                        // Extract the value depth from the value's localName
                        val srcValueDepth = stringToDepth(value.name.localName)
                        clearFSDummies(node.functionSummary)
                        // Store the information in the functionSummary
                        val existingEntry =
                            node.functionSummary.computeIfAbsent(param) { mutableSetOf() }
                        val filteredLastWrites =
                            lastWrites
                                // for shortFS,only use these, and for !shortFS, only those
                                .filterTo(PowersetLattice.Element()) { shortFS in it.second }
                        existingEntry.add(
                            FunctionDeclaration.FSEntry(
                                dstValueDepth,
                                value,
                                srcValueDepth,
                                subAccessName,
                                filteredLastWrites,
                                equalLinkedHashSetOf(shortFS),
                            )
                        )
                        // Additionally, we store this as a shortFunctionSummary were the Function
                        // writes to the parameter
                        // Fadd doesn't recognize if the entry already exists b/c it compares the
                        // hashes
                        // so we do that manually
                        if (
                            existingEntry.none {
                                it.destValueDepth == dstValueDepth &&
                                    it.srcNode == node &&
                                    it.srcValueDepth == 0 &&
                                    it.subAccessName == subAccessName &&
                                    it.lastWrites ==
                                        PowersetLattice.Element<Pair<*, *>>(
                                            Pair<Node, EqualLinkedHashSet<*>>(
                                                node,
                                                equalLinkedHashSetOf<Any>(),
                                            )
                                        ) &&
                                    it.properties == equalLinkedHashSetOf(true)
                            }
                        )
                            existingEntry.add(
                                FunctionDeclaration.FSEntry(
                                    dstValueDepth,
                                    node,
                                    0,
                                    subAccessName,
                                    PowersetLattice.Element(Pair(node, equalLinkedHashSetOf())),
                                    equalLinkedHashSetOf(true),
                                )
                            )
                        val propertySet = identitySetOf<Any>(true)
                        if (subAccessName != "")
                            propertySet.add(FieldDeclaration().apply { name = Name(subAccessName) })

                        if (!shortFS) {
                            // Check if the value is influenced by a Parameter and if so, add this
                            // information to the functionSummary
                            value
                                .followDFGEdgesUntilHit(
                                    collectFailedPaths = false,
                                    findAllPossiblePaths = false,
                                    direction = Backward(GraphToFollow.DFG),
                                    sensitivities = OnlyFullDFG + FieldSensitive + ContextSensitive,
                                    scope = Intraprocedural(),
                                    predicate = {
                                        it is ParameterMemoryValue &&
                                            /* If it's a ParameterMemoryValue from the node's
                                            parameters, it has to have a DFG Node to one
                                            of the node's parameters. Either partial to a derefvalue or full to the parameterdeclaration */
                                            it.memoryValueUsageEdges
                                                .filter {
                                                    ((it.granularity is
                                                        PartialDataflowGranularity<*> &&
                                                        ((it.granularity
                                                                    as
                                                                    PartialDataflowGranularity<*>)
                                                                .partialTarget as? String)
                                                            ?.endsWith("derefvalue") == true) ||
                                                        (it.granularity is
                                                            FullDataflowGranularity &&
                                                            it.end is ParameterDeclaration)) &&
                                                        it.end in node.parameters
                                                }
                                                .size == 1 &&
                                            node.parameters.any { param ->
                                                param.name.localName == it.name.parent?.localName
                                            }
                                    },
                                )
                                .fulfilled
                                .map { it.nodes.last() }
                                .forEach { sourceParamValue ->
                                    val matchingDeclarations =
                                        node.parameters.singleOrNull {
                                            it.name == sourceParamValue.name.parent
                                        }
                                    if (matchingDeclarations == null) TODO()
                                    node.functionSummary
                                        .computeIfAbsent(param) { mutableSetOf() }
                                        .add(
                                            FunctionDeclaration.FSEntry(
                                                dstValueDepth,
                                                matchingDeclarations,
                                                stringToDepth(sourceParamValue.name.localName),
                                                subAccessName,
                                                PowersetLattice.Element(
                                                    Pair(
                                                        matchingDeclarations,
                                                        equalLinkedHashSetOf(),
                                                    )
                                                ),
                                                equalLinkedHashSetOf(true),
                                            )
                                        )
                                }
                        }
                    }
            }
        }
        // If we don't have anything to summarize, we add a dummy entry to the functionSummary
        if (node.functionSummary.isEmpty()) {
            node.functionSummary[newLiteral("dummy")] = mutableSetOf()
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
                            doubleState.getValues(retval, retval).map {
                                FunctionDeclaration.FSEntry(
                                    0,
                                    it.first,
                                    1,
                                    "",
                                    PowersetLattice.Element(Pair(parentFD, equalLinkedHashSetOf())),
                                    equalLinkedHashSetOf(false),
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
        var callingContext =
            CallingContextIn(
                mutableListOf(callExpression)
            ) // TODO: Indicate somehow if this has already been done?
        callExpression.arguments.forEach { arg ->
            if (arg.argumentIndex < functionDeclaration.parameters.size) {
                // Create a DFG-Edge from the argument to the parameter's memoryValue
                val p = functionDeclaration.parameters[arg.argumentIndex]
                // First, check if we already assigned the PMV values in another function
                var memVals = p.fullMemoryValues
                // If this is not the case, they are still in the state
                if (memVals.isEmpty()) {
                    memVals = doubleState.getValues(p, p).map { it.first }.toSet()
                    // If they are also not yet in the state, we have to calculate them
                    if (memVals.isEmpty()) {
                        initializeParameters(lattice, mutableListOf(p), doubleState, 2)
                        memVals = doubleState.getValues(p, p).map { it.first }.toSet()
                    }
                }
                memVals.filterIsInstance<ParameterMemoryValue>().forEach { paramVal ->
                    doubleState =
                        lattice.push(
                            doubleState,
                            paramVal,
                            GeneralStateEntryElement(
                                PowersetLattice.Element(/*paramVal*/ ),
                                PowersetLattice.Element(Pair(arg, equalLinkedHashSetOf())),
                                PowersetLattice.Element(
                                    Pair(arg, equalLinkedHashSetOf(callingContext))
                                ),
                            ),
                        )
                    // Also draw the edges for the (deref)derefvalues if we have any and are
                    // dealing with a pointer parameter (AKA memoryValue is not null)
                    p.memoryValueEdges
                        .filter {
                            (it.granularity as? PartialDataflowGranularity<*>)?.partialTarget ==
                                "derefvalue"
                        }
                        .map { it.start }
                        .forEach { derefPMV ->
                            val argVals =
                                // In C(++), the reference to an array is a pointer, leading to the
                                // situation that handing "arg" or "&arg" as argument is the same
                                // We deal with this by drawing a DFG-Edge from the arg to the
                                // derefPMV in case of an array pointerType.
                                if (
                                    (arg.type as? PointerType)?.pointerOrigin ==
                                        PointerType.PointerOrigin.ARRAY
                                )
                                    PowersetLattice.Element(Pair(arg, true))
                                else
                                    doubleState.getNestedValues(
                                        arg,
                                        1,
                                        fetchFields = false,
                                        onlyFetchExistingEntries = true,
                                        excludeShortFSValues = true,
                                    )
                            argVals.forEach { (argVal, _) ->
                                val argDerefVals =
                                    if (
                                        (arg.type as? PointerType)?.pointerOrigin ==
                                            PointerType.PointerOrigin.ARRAY
                                    )
                                        equalLinkedHashSetOf<Node>(arg)
                                    else {
                                        doubleState
                                            .getNestedValues(
                                                argVal,
                                                1,
                                                fetchFields = false,
                                                onlyFetchExistingEntries = true,
                                                excludeShortFSValues = true,
                                            )
                                            .mapTo(equalLinkedHashSetOf()) { it.first }
                                    }
                                val lastDerefWrites =
                                    if (
                                        (arg.type as? PointerType)?.pointerOrigin ==
                                            PointerType.PointerOrigin.ARRAY
                                    )
                                        PowersetLattice.Element<
                                            Pair<Node, EqualLinkedHashSet<Any>>
                                        >(
                                            Pair(arg, equalLinkedHashSetOf(callingContext, false))
                                        )
                                    else {
                                        doubleState.getLastWrites(argVal).mapTo(
                                            PowersetLattice.Element()
                                        ) {
                                            Pair(
                                                it.first,
                                                equalLinkedHashSetOf(
                                                    callingContext,
                                                    true in it.second,
                                                ),
                                            )
                                        }
                                    }
                                doubleState =
                                    lattice.push(
                                        doubleState,
                                        derefPMV,
                                        GeneralStateEntryElement(
                                            PowersetLattice.Element(/*paramVal*/ ),
                                            PowersetLattice.Element(
                                                argDerefVals.mapTo(PowersetLattice.Element()) {
                                                    Pair(it, equalLinkedHashSetOf())
                                                }
                                            ),
                                            PowersetLattice.Element(lastDerefWrites),
                                        ),
                                    )
                                // The same for the derefderef values
                                p.memoryValueEdges
                                    .filter {
                                        (it.granularity as? PartialDataflowGranularity<*>)
                                            ?.partialTarget == "derefderefvalue"
                                    }
                                    .map { it.start }
                                    .forEach { derefderefPMV ->
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
                                                        .flatMapTo(PowersetLattice.Element()) {
                                                            doubleState.getLastWrites(it)
                                                        }
                                                        .mapTo(PowersetLattice.Element()) {
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
                                                                Pair(
                                                                    derefderefValue,
                                                                    equalLinkedHashSetOf(),
                                                                )
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

    data class MapDstToSrcEntry(
        val srcNode: Node?,
        val lastWrites: PowersetLattice.Element<Pair<Node, EqualLinkedHashSet<Any>>>,
        val propertySet: EqualLinkedHashSet<Any>,
        val dst: IdentitySet<Node> = identitySetOf(),
    )

    private fun handleCallExpression(
        lattice: PointsToState,
        currentNode: CallExpression,
        doubleState: PointsToStateElement,
    ): PointsToStateElement {
        var doubleState = doubleState
        var mapDstToSrc = mutableMapOf<Node, IdentitySet<MapDstToSrcEntry>>()

        var i = 0
        // The toIdentitySet avoids having the same elements multiple times
        val invokes = currentNode.invokes.toIdentitySet().toList()
        while (i < invokes.size) {
            val invoke = calculateFunctionSummaries(invokes[i])

            if (invoke == null) break

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
                        is ReturnStatement,
                        is FunctionDeclaration -> {
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
                                lastWrites,
                                properties,
                            ) ->
                            val shortFS = properties.any { it == true }
                            val (destinationAddresses, destinations) =
                                calculateCallExpressionDestinations(
                                    doubleState,
                                    mapDstToSrc,
                                    dstValueDepth,
                                    subAccessName,
                                    argument,
                                    properties,
                                )
                            // Collect the properties for the  DeclarationStateEntry
                            val propertySet = equalLinkedHashSetOf<Any>()
                            propertySet.addAll(properties)
                            if (subAccessName.isNotEmpty()) {
                                propertySet.add(
                                    PartialDataflowGranularity(
                                        FieldDeclaration().apply { name = Name(subAccessName) }
                                    )
                                )
                            }

                            // Especially for shortFS, we need to update the prevDFGs with
                            // information we didn't have when creating the functionSummary.
                            // calculatePrev does this for us
                            val prev = calculatePrevDFGs(lastWrites, shortFS, currentNode, invoke)
                            mapDstToSrc =
                                addEntryToMap(
                                    doubleState,
                                    mapDstToSrc,
                                    destinationAddresses,
                                    destinations,
                                    // To ensure that we have a unique Node, we take the
                                    // callExpression if the FS said the srcNode is the
                                    // FunctionDeclaration
                                    if (srcNode is FunctionDeclaration) currentNode else srcNode,
                                    shortFS,
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

        val callingContextOut = CallingContextOut(mutableListOf(currentNode))
        mapDstToSrc.forEach { (dstAddr, values) ->
            runBlocking {
                doubleState =
                    writeMapEntriesToState(lattice, doubleState, dstAddr, values, callingContextOut)
            }
        }

        return doubleState
    }

    private fun calculatePrevDFGs(
        lastWrites: PowersetLattice.Element<Pair<Node, EqualLinkedHashSet<Any>>>,
        shortFS: Boolean,
        currentNode: CallExpression,
        invoke: FunctionDeclaration,
    ): PowersetLattice.Element<Pair<Node, EqualLinkedHashSet<Any>>> {
        val ret = PowersetLattice.Element<Pair<Node, EqualLinkedHashSet<Any>>>()
        // If we have nothing, the last write is probably the functionDeclaration
        if (lastWrites.isEmpty()) ret.add(Pair(invoke, equalLinkedHashSetOf()))
        lastWrites.forEach { (lw, properties) ->
            val filteredProperties = properties
            if (shortFS) {
                when (lw) {
                    is FunctionDeclaration -> ret.add(Pair(currentNode, filteredProperties))
                    is ParameterDeclaration -> {
                        if (lw.argumentIndex < currentNode.arguments.size)
                            ret.add(
                                Pair(currentNode.arguments[lw.argumentIndex], filteredProperties)
                            )
                        else ret.add(Pair(lw, filteredProperties))
                    }
                    else -> ret.add(Pair(lw, filteredProperties))
                }
            } else ret.add(Pair(lw, filteredProperties))
        }
        return ret
    }

    private fun calculateFunctionSummaries(invoke: FunctionDeclaration): FunctionDeclaration? {
        if (invoke.functionSummary.isEmpty()) {
            if (invoke.hasBody()) {
                log.debug("functionSummaryAnalysisChain: {}", functionSummaryAnalysisChain)
                if (invoke !in functionSummaryAnalysisChain) {
                    //                    val summaryCopy = functionSummaryAnalysisChain.toSet()
                    acceptInternal(invoke)
                    //                    functionSummaryAnalysisChain.addAll(summaryCopy)
                } else {
                    log.error(
                        "Cannot calculate functionSummary for ${invoke.name.localName} as it's recursively called. callChain: ${functionSummaryAnalysisChain.map{it.name.localName}}"
                    )
                    invoke.functionSummary[newLiteral("dummy")] = mutableSetOf()
                    return null
                }
            } else {
                // Add a dummy function summary so that we don't try this every time
                // In this dummy, all parameters point either to returns if we have any, or the
                // FunctionDeclaration itself
                val newValues: MutableSet<FunctionDeclaration.FSEntry> =
                    invoke.parameters
                        .map { FunctionDeclaration.FSEntry(0, it, 1, "") }
                        .toMutableSet()
                val entries = mutableSetOf<Node>()
                if (invoke.returns.isNotEmpty()) entries.addAll(invoke.returns)
                else entries.add(invoke)
                entries.forEach { entry -> invoke.functionSummary[entry] = newValues }
            }
        }
        return invoke
    }

    private suspend fun writeMapEntriesToState(
        lattice: PointsToState,
        doubleState: PointsToStateElement,
        dstAddr: Node,
        values: IdentitySet<MapDstToSrcEntry>,
        callingContext: CallingContextOut,
    ): PointsToStateElement {
        // A triple: the sourceNode, a flag if this is a shortFS, and a flag if this is a
        // partialWrite
        val sources =
            values.mapTo(PowersetLattice.Element()) {
                Triple(
                    it.srcNode,
                    true in it.propertySet,
                    it.propertySet.any { it is PartialDataflowGranularity<*> },
                )
            }
        val lastWrites = PowersetLattice.Element<Pair<Node, EqualLinkedHashSet<Any>>>()
        val destinations = identitySetOf<Node>()

        val parentJob = Job()
        val limitedDispatcher = Dispatchers.Default.limitedParallelism(100)
        val scope = CoroutineScope(limitedDispatcher + parentJob)

        values.forEach { value ->
            scope.launch {
                value.lastWrites.forEach { (lw, lwProps) ->
                    // For short FunctionSummaries (AKA one of the lastWrite properties set to
                    // 'true',
                    // we don't add the callingcontext
                    val lwPropertySet = EqualLinkedHashSet<Any>()
                    lwPropertySet.addAll(value.propertySet)
                    // If this is not a shortFS edge, we add the new callingcontext and have to
                    // check if
                    // we already have a list of callingcontexts in the properties
                    if (value.propertySet.none { it == true }) {
                        val existingCallingContext =
                            lwProps.filterIsInstance<CallingContextOut>().singleOrNull()
                        if (existingCallingContext != null) {
                            if (
                                callingContext.calls.any { call ->
                                    call !in existingCallingContext.calls
                                }
                            ) {
                                val cpy = existingCallingContext.calls.toMutableList()
                                cpy.addAll(callingContext.calls)
                                lwPropertySet.add(CallingContextOut(cpy))
                            }
                        } else lwPropertySet.add(callingContext)
                    }
                    // Add all other previous properties
                    lwPropertySet.addAll(lwProps.filter { it !is CallingContextOut })
                    // Add them to the set of lastWrites if there is no same element in there yet
                    synchronized(lastWrites) {
                        if (
                            lastWrites.none {
                                it.first == lw &&
                                    it.second.all { it in lwPropertySet } &&
                                    it.second.size == lwPropertySet.size
                            }
                        )
                            lastWrites.add(Pair(lw, lwPropertySet))
                    }
                }
            }
            synchronized(destinations) { destinations.addAll(value.dst) }
        }

        runBlocking {
            parentJob.children.forEach { it.join() } // Wait for all child coroutines to finish
            parentJob.complete() // Ensure parentJob is completed
            parentJob.join() // Wait for the parentJob to complete
        }

        return doubleState.updateValues(
            lattice,
            doubleState,
            sources,
            destinations,
            identitySetOf(dstAddr),
            lastWrites,
        )
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
     * @param destinationAddresses The set of destination nodes.
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
        doubleState: PointsToStateElement,
        mapDstToSrc: MutableMap<Node, IdentitySet<MapDstToSrcEntry>>,
        destinationAddresses: IdentitySet<Node?>,
        destinations: IdentitySet<Node>,
        srcNode: Node?,
        shortFS: Boolean,
        srcValueDepth: Int,
        param: Node,
        propertySet: EqualLinkedHashSet<Any>,
        currentNode: CallExpression,
        lastWrites: PowersetLattice.Element<Pair<Node, EqualLinkedHashSet<Any>>>,
    ): MutableMap<Node, IdentitySet<MapDstToSrcEntry>> {
        var doubleState = doubleState
        when (srcNode) {
            is ParameterDeclaration -> {
                // Add the (dereferenced) value of the respective argument
                // in the CallExpression
                if (srcNode.argumentIndex < currentNode.arguments.size) {
                    // If this is a short FunctionSummary, we also
                    // update the generalState to draw the additional DFG Edges
                    if (shortFS) {
                        val newEntry =
                            Pair(
                                currentNode.arguments[srcNode.argumentIndex],
                                equalLinkedHashSetOf<Any>(true),
                            )
                        doubleState.generalState.computeIfAbsent(currentNode) {
                            TripleLattice.Element(
                                PowersetLattice.Element(),
                                PowersetLattice.Element(),
                                PowersetLattice.Element(),
                            )
                        }
                        doubleState.generalState[currentNode]?.third?.add(newEntry)
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
                        else identitySetOf(currentNode.arguments[srcNode.argumentIndex])
                    values.forEach { value ->
                        destinationAddresses.filterNotNull().forEach { d ->
                            // The extracted value might come from a state we
                            // created for a short function summary. If so, we
                            // have to store that info in the map
                            val updatedPropertySet = propertySet
                            updatedPropertySet.add(shortFS)
                            val currentSet = mapDstToSrc.computeIfAbsent(d) { identitySetOf() }
                            if (
                                currentSet.none {
                                    it.srcNode === value &&
                                        it.lastWrites == lastWrites.singleOrNull() &&
                                        it.propertySet == updatedPropertySet
                                }
                            ) {
                                currentSet +=
                                    MapDstToSrcEntry(
                                        value,
                                        lastWrites,
                                        updatedPropertySet,
                                        destinations,
                                    )
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
                            destinationAddresses.filterNotNull().forEach { d ->
                                val updatedPropertySet = propertySet
                                updatedPropertySet.add(shortFS)
                                val currentSet = mapDstToSrc.computeIfAbsent(d) { identitySetOf() }
                                doubleState.getNestedValues(arg, srcValueDepth).forEach { (value, _)
                                    ->
                                    if (
                                        currentSet.none {
                                            it.srcNode === value &&
                                                it.lastWrites == lastWrites &&
                                                it.propertySet == updatedPropertySet
                                        }
                                    ) {
                                        currentSet +=
                                            MapDstToSrcEntry(
                                                value,
                                                lastWrites,
                                                updatedPropertySet,
                                                destinations,
                                            )
                                    }
                                }
                            }
                        }
                    }
            }

            is MemoryAddress -> {
                destinationAddresses.filterNotNull().forEach { d ->
                    val currentSet = mapDstToSrc.computeIfAbsent(d) { identitySetOf() }
                    val updatedPropertySet = propertySet
                    updatedPropertySet.add(shortFS)
                    if (
                        currentSet.none {
                            it.srcNode === srcNode &&
                                it.lastWrites === lastWrites &&
                                it.propertySet == updatedPropertySet
                        }
                    ) {
                        currentSet +=
                            MapDstToSrcEntry(srcNode, lastWrites, updatedPropertySet, destinations)
                    }
                }
            }

            else -> {
                destinationAddresses.filterNotNull().forEach { d ->
                    val currentSet = mapDstToSrc.computeIfAbsent(d) { identitySetOf() }
                    val newSet =
                        if (srcValueDepth == 0) PowersetLattice.Element(Pair(srcNode, shortFS))
                        else
                            srcNode?.let {
                                doubleState.getNestedValues(it, srcValueDepth).mapTo(
                                    PowersetLattice.Element()
                                ) {
                                    Pair(it.first, shortFS)
                                }
                            } ?: PowersetLattice.Element(Pair(srcNode, shortFS))

                    newSet.forEach { pair ->
                        if (
                            currentSet.none {
                                it.srcNode === pair.first &&
                                    it.lastWrites === lastWrites &&
                                    pair.second in it.propertySet
                            }
                        ) {
                            val updatedPropertySet = propertySet
                            updatedPropertySet.add(shortFS)
                            currentSet +=
                                MapDstToSrcEntry(
                                    pair.first,
                                    lastWrites,
                                    updatedPropertySet,
                                    destinations,
                                )
                        }
                    }
                }
            }
        }
        return mapDstToSrc
    }

    /** Returns a Pair of destination (for the general State) and destinationAddresses */
    private fun calculateCallExpressionDestinations(
        doubleState: PointsToStateElement,
        mapDstToSrc: MutableMap<Node, IdentitySet<MapDstToSrcEntry>>,
        dstValueDepth: Int,
        subAccessName: String,
        argument: Node,
        properties: EqualLinkedHashSet<Any>,
    ): Pair<IdentitySet<Node?>, IdentitySet<Node>> {
        // If the dstAddr is a CallExpression, the dst is the same. Otherwise, we don't really know,
        // so we leave it empty
        val destination: IdentitySet<Node> =
            if (argument is CallExpression) identitySetOf(argument)
            // if the argument is PointerReference for a global variable, the destination is it's
            // refersTo
            // It might also be the case that argument is a Reference to an array, so then we treat
            // it like a PointerReference
            else if (
                (argument is PointerReference ||
                    argument is Reference &&
                        (argument.type as? PointerType)?.pointerOrigin?.name == "ARRAY") &&
                    isGlobal(argument) &&
                    argument.refersTo != null
            )
                identitySetOf(argument.refersTo!!)
            else identitySetOf()

        val destAddrDepth = dstValueDepth - 1
        // Is the destAddrDepth > 2? In this case, the DeclarationState
        // might be outdated. So check in the mapDstToSrc for updates
        val updatedAddresses =
            mapDstToSrc.entries
                .filter {
                    it.key in
                        doubleState.getValues(argument, argument).mapTo(IdentitySet()) { it.first }
                }
                .flatMap { it.value }
                .mapTo(IdentitySet()) { it.srcNode }

        return if (dstValueDepth > 2 && updatedAddresses.isNotEmpty()) {
            Pair(updatedAddresses, destination)
        } else {
            val partialAccess =
                properties.filterIsInstance<PartialDataflowGranularity<*>>().singleOrNull()
            if (subAccessName.isNotEmpty() || partialAccess != null) {
                val fieldAddresses = identitySetOf<Node?>()
                // Collect the fieldAddresses for each possible value
                val argumentValues =
                    doubleState.getNestedValues(argument, destAddrDepth, fetchFields = true)
                argumentValues.forEach { (v, _) ->
                    // We over approximate here and also add the main memory Address to the list of
                    // destinations
                    fieldAddresses.add(v)

                    val parentName = getNodeName(v)
                    val partialString =
                        subAccessName.ifEmpty { (partialAccess?.partialTarget as? String) ?: "" }
                    val newName = Name(partialString, parentName)
                    fieldAddresses.addAll(
                        doubleState.fetchFieldAddresses(identitySetOf(v), newName)
                    )
                }
                Pair(fieldAddresses, destination)
            } else {
                Pair(
                    doubleState.getNestedValues(argument, destAddrDepth).mapTo(IdentitySet()) {
                        it.first
                    },
                    destination,
                )
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
            doubleState.getAddresses(currentNode.input, currentNode.input).forEach { addr ->
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
                    PowersetLattice.Element(doubleState.getAddresses(currentNode, currentNode)),
                    PowersetLattice.Element(
                        doubleState.getValues(currentNode, currentNode).mapTo(
                            PowersetLattice.Element()
                        ) {
                            Pair(it.first, equalLinkedHashSetOf())
                        }
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
            val sources =
                currentNode.rhs.flatMapTo(
                    PowersetLattice.Element<Triple<Node?, Boolean, Boolean>>()
                ) {
                    doubleState.getValues(it, it).map { Triple(it.first, it.second, false) }
                }
            val destinations: IdentitySet<Node> = currentNode.lhs.toIdentitySet()
            val destinationsAddresses =
                destinations.flatMapTo(IdentitySet()) { doubleState.getAddresses(it, it) }
            val lastWrites =
                destinations.mapTo(PowersetLattice.Element()) {
                    Pair(it, equalLinkedHashSetOf<Any>(false))
                }
            doubleState =
                doubleState.updateValues(
                    lattice,
                    doubleState,
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

        /* If we have an Expression that is written to, we handle it's values later and ignore it now */
        val access =
            if (currentNode is Reference || currentNode is BinaryOperator) currentNode.access
            else if (currentNode is SubscriptExpression && currentNode.arrayExpression is Reference)
                (currentNode.arrayExpression as Reference).access
            else null
        if (access in setOf(AccessValues.READ, AccessValues.READWRITE)) {
            val addresses = doubleState.getAddresses(currentNode, currentNode)
            val values =
                doubleState
                    .getValues(currentNode, currentNode)
                    // Filter only the values that are not stored for short FunctionSummaries (aka
                    // it.second set to true)
                    .filter { !it.second }
                    .mapTo(IdentitySet()) { it.first }
            val prevDFGs = doubleState.getLastWrites(currentNode)

            // If we have any information from the dereferenced value, we also fetch that
            if ((passConfig<Configuration>()?.drawCurrentDerefDFG != false)) {
                values
                    .filterTo(identitySetOf()) { doubleState.hasDeclarationStateEntry(it, true) }
                    .forEach { value ->
                        // draw the DFG Edges
                        doubleState
                            .getLastWrites(value)
                            .filter { it.second.none { it == true } }
                            .forEach {
                                prevDFGs.add(
                                    Pair(
                                        it.first,
                                        equalLinkedHashSetOf(
                                            PointerDataflowGranularity(
                                                PointerAccess.currentDerefValue
                                            ),
                                            // Remove the FullDataGranularity, since here we only
                                            // want to indicate derefValues
                                            *it.second
                                                .filter { it !is FullDataflowGranularity }
                                                .toTypedArray(),
                                        ),
                                    )
                                )
                            }

                        // Let's see if we can deref once more
                        val derefValues =
                            doubleState
                                .fetchValueFromDeclarationState(
                                    node = value,
                                    excludeShortFSValues = true,
                                )
                                .map { it.value }
                                .forEach { derefValue ->
                                    if (doubleState.hasDeclarationStateEntry(derefValue)) {
                                        doubleState
                                            .fetchValueFromDeclarationState(derefValue)
                                            .forEach { (derefDerefValue, _, _) ->
                                                doubleState
                                                    .getLastWrites(derefValue)
                                                    .filter { it.second.none { it == true } }
                                                    .forEach {
                                                        prevDFGs.add(
                                                            Pair(
                                                                it.first,
                                                                equalLinkedHashSetOf<Any>(
                                                                    PointerDataflowGranularity(
                                                                        PointerAccess
                                                                            .currentDerefDerefValue
                                                                    ),
                                                                    // Here again, filter the
                                                                    // FullDataflowGranularity since
                                                                    // we indicate a
                                                                    // currentDerefDerefValue
                                                                    *it.second
                                                                        .filter {
                                                                            it !is
                                                                                FullDataflowGranularity
                                                                        }
                                                                        .toTypedArray(),
                                                                ),
                                                            )
                                                        )
                                                    }
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
                        PowersetLattice.Element(
                            values.mapTo(PowersetLattice.Element()) {
                                Pair(it, equalLinkedHashSetOf())
                            }
                        ),
                        PowersetLattice.Element(prevDFGs),
                    ),
                )
        } else {
            // We write to this node, but maybe we probably want to store the memory address which
            // it has right now
            doubleState =
                lattice.push(
                    doubleState,
                    currentNode,
                    GeneralStateEntryElement(
                        PowersetLattice.Element(doubleState.getAddresses(currentNode, currentNode)),
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
        var doubleState = doubleState
        /* No need to set the address, this already happens in the constructor */
        val addresses = doubleState.getAddresses(currentNode, currentNode)

        val values = PowersetLattice.Element<Pair<Node, EqualLinkedHashSet<Any>>>()

        (currentNode as? HasInitializer)?.initializer?.let { initializer ->
            if (initializer is Literal<*>) values.add(Pair(initializer, equalLinkedHashSetOf()))
            else {
                // The EOG of Declarations does not play in our favor: We will handle the
                // Declaration before we handled the initializer. So we explicitly handle the
                // initializer before continuing
                var ini = initializer
                while (ini is CastExpression) ini = ini.expression
                doubleState =
                    when (ini) {
                        is PointerReference -> handleExpression(lattice, ini.input, doubleState)
                        is PointerDereference -> handleExpression(lattice, ini.input, doubleState)
                        else -> handleExpression(lattice, ini, doubleState)
                    }
                values.addAll(
                    doubleState.getValues(ini, ini).mapTo(PowersetLattice.Element()) {
                        Pair(it.first, equalLinkedHashSetOf())
                    }
                )
            }
        }

        doubleState =
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
                        PowersetLattice.Element(
                            values.mapTo(PowersetLattice.Element()) { Pair(it.first, false) }
                        ),
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
            .filter { it.memoryValues.filterIsInstance<ParameterMemoryValue>().isEmpty() }
            //            .filter {
            // doubleState.getValues(it).filterIsInstance<ParameterMemoryValue>().isEmpty() }
            .forEach { param ->
                // In the first step, we have a triangle of ParameterDeclaration, the
                // ParameterDeclaration's Memory Address and the ParameterMemoryValue
                // Therefore, the src and the addresses are different. For all other depths, we set
                // both to the ParameterMemoryValue we create in the first step
                var src: Node = param
                var addresses = doubleState.getAddresses(src, src)
                var prevAddresses = identitySetOf<Node>()
                // If we have a Pointer as param, we initialize all levels, otherwise, only the
                // first one
                val paramDepth =
                    if (
                        param.type is PointerType ||
                            // If the type is unknown, we also initialize all levels to be sure
                            param.type is UnknownType ||
                            // Another guess we take: If the length is the same as the
                            // addressLength, again, to be sure we initialize all levels
                            (param.type as? NumericType)?.bitWidth ==
                                // TODO: passConfig<Configuration> should never be null?
                                (passConfig<Configuration>()?.addressLength ?: 64)
                    )
                        depth
                    else 0
                for (i in 0..paramDepth) {
                    val pmvName = "deref".repeat(i) + "value"
                    val pmv = ParameterMemoryValue(Name(pmvName, param.name))

                    // In the first step, we link the ParameterDeclaration to the PMV to be able to
                    // also access it outside the function
                    if (src is ParameterDeclaration) {
                        // src.memoryValue = pmv
                        doubleState =
                            lattice.push(
                                doubleState,
                                src,
                                GeneralStateEntryElement(
                                    PowersetLattice.Element(addresses),
                                    PowersetLattice.Element(Pair(pmv, equalLinkedHashSetOf())),
                                    PowersetLattice.Element(),
                                ),
                            )
                    } else {
                        // Link the PMVs with each other so that we can find them. This is
                        // especially important outside the respective function where we don't have
                        // a state
                        addresses.filterIsInstance<ParameterMemoryValue>().forEach {
                            doubleState =
                                lattice.push(
                                    doubleState,
                                    it,
                                    GeneralStateEntryElement(
                                        PowersetLattice.Element(prevAddresses),
                                        PowersetLattice.Element(Pair(pmv, equalLinkedHashSetOf())),
                                        PowersetLattice.Element(),
                                    ),
                                )
                        }
                        doubleState =
                            lattice.push(
                                doubleState,
                                pmv,
                                GeneralStateEntryElement(
                                    PowersetLattice.Element(addresses),
                                    PowersetLattice.Element(),
                                    PowersetLattice.Element(),
                                ),
                            )
                        doubleState =
                            lattice.push(
                                doubleState,
                                param,
                                GeneralStateEntryElement(
                                    PowersetLattice.Element(),
                                    PowersetLattice.Element(
                                        Pair(pmv, equalLinkedHashSetOf(pmvName))
                                    ),
                                    PowersetLattice.Element(),
                                ),
                            )
                    }

                    // Update the states
                    val declStateElement =
                        DeclarationStateEntryElement(
                            PowersetLattice.Element(prevAddresses),
                            PowersetLattice.Element(Pair(pmv, false)),
                            PowersetLattice.Element(Pair(pmv, equalLinkedHashSetOf())),
                        )
                    addresses.forEach { addr ->
                        doubleState =
                            lattice.pushToDeclarationsState(doubleState, addr, declStateElement)
                    }

                    prevAddresses = addresses
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

    this.innerLattice1.lub(
        currentState.generalState,
        MapLattice.Element(newNode to newLatticeCopy),
        true,
    )
    return currentState
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

    this.innerLattice2.lub(
        currentState.declarationsState,
        MapLattice.Element(newNode to newLatticeCopy),
        true,
    )
    return currentState
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
    val value: Node,
    val shortFS: Boolean,
    val subAccessName: String,
    val lastWrites: PowersetLattice.Element<Pair<Node, EqualLinkedHashSet<Any>>>,
)

/** Fetch the address for `node` from the GeneralState */
fun PointsToStateElement.fetchAddressFromGeneralState(node: Node): IdentitySet<Node> {
    return this.generalState[node]?.first ?: PowersetLattice.Element()
}

/** Fetch the value for `node` from the GeneralState */
fun PointsToStateElement.fetchValueFromGeneralState(
    node: Node
): PowersetLattice.Element<Pair<Node, EqualLinkedHashSet<Any>>> {
    return this.generalState[node]?.second ?: PowersetLattice.Element()
}

/** Fetch the lastWrite for `node` from the GeneralState */
fun PointsToStateElement.fetchLastWriteFromGeneralState(
    node: Node
): PowersetLattice.Element<Pair<Node, EqualLinkedHashSet<Any>>> {
    return this.generalState[node]?.third ?: PowersetLattice.Element()
}

/**
 * Fetch the value entry for `node` from the DeclarationState. If there isn't any, create an
 * UnknownMemoryValue
 */
fun PointsToStateElement.fetchValueFromDeclarationState(
    node: Node,
    fetchFields: Boolean = false,
    excludeShortFSValues: Boolean = false,
): IdentitySet<fetchElementFromDeclarationStateEntry> {
    val ret = identitySetOf<fetchElementFromDeclarationStateEntry>()

    // For global nodes, we check the globalDerefs map
    if (isGlobal(node)) {
        val element = globalDerefs[node]
        if (element != null)
            element.map {
                ret.add(
                    fetchElementFromDeclarationStateEntry(
                        it.first,
                        false,
                        "",
                        PowersetLattice.Element(),
                    )
                )
            }
        else {
            val newName = getNodeName(node)
            val newEntry =
                nodesCreatingUnknownValues.computeIfAbsent(Pair(node, newName)) {
                    UnknownMemoryValue(newName, true)
                }
            // TODO: Check if the boolean should be true sometimes
            globalDerefs[node] = PowersetLattice.Element(Pair(newEntry, false))
            ret.add(
                fetchElementFromDeclarationStateEntry(
                    newEntry,
                    false,
                    "",
                    PowersetLattice.Element(),
                )
            )
        }
    } else {

        // Otherwise, we read the declarationState.
        // Let's start with the main element
        var elements = this.declarationsState[node]?.second?.toList()
        if (excludeShortFSValues) elements = elements?.filter { !it.second }
        if (elements.isNullOrEmpty()) {
            val newName = getNodeName(node)
            val newEntry =
                nodesCreatingUnknownValues.computeIfAbsent(Pair(node, newName)) {
                    UnknownMemoryValue(newName)
                }
            val newPair = Pair(newEntry, false)
            this.declarationsState.computeIfAbsent(node) {
                TripleLattice.Element(
                    PowersetLattice.Element(node),
                    PowersetLattice.Element(),
                    PowersetLattice.Element(),
                )
            }

            val newElements = this.declarationsState[node]?.second
            if (
                newElements?.none { it.first === newPair.first && it.second == newPair.second } !=
                    false
            ) {
                newElements?.add(newPair)
            }
            ret.add(
                fetchElementFromDeclarationStateEntry(
                    newEntry,
                    false,
                    "",
                    PowersetLattice.Element(),
                )
            )
        } else
            elements.map {
                ret.add(
                    fetchElementFromDeclarationStateEntry(
                        it.first,
                        it.second,
                        "",
                        this.declarationsState[node]?.third ?: PowersetLattice.Element(),
                    )
                )
            }

        // if fetchFields is true, we also fetch the values for fields
        // TODO: handle globals
        if (fetchFields) {
            val fields =
                this.declarationsState[node]?.first?.filterTo(identitySetOf()) {
                    it != node && !this.getAddresses(node, node).contains(it)
                }
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
                                    this.declarationsState[field]?.third
                                        ?: PowersetLattice.Element(),
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
): PowersetLattice.Element<Pair<Node, EqualLinkedHashSet<Any>>> {
    if (isGlobal(node)) {
        return when (node) {
            //            is PointerReference -> { TODO()}
            is MemberExpression -> {
                // We overapproximate here: For memberExpressions, we ignore the field and only
                // consider the base
                val (base, _) = resolveMemberExpression(node)
                PowersetLattice.Element(
                    Pair<Node, EqualLinkedHashSet<Any>>(
                        (base as? Reference)?.refersTo ?: base,
                        equalLinkedHashSetOf(),
                    )
                )
            }
            is Reference ->
                PowersetLattice.Element(
                    Pair<Node, EqualLinkedHashSet<Any>>(
                        node.refersTo ?: node,
                        equalLinkedHashSetOf(),
                    )
                )
            else -> PowersetLattice.Element(Pair(node, equalLinkedHashSetOf()))
        }
    }
    return when (node) {
        is PointerReference -> {
            // TODO: Handle other input types (e.g. SubscriptExpression, MemberExpression)
            // For pointerReferences, we take the memoryAddress of the refersTo
            return (node.input as? Reference)?.refersTo?.memoryAddresses?.mapTo(
                PowersetLattice.Element()
            ) {
                Pair<Node, EqualLinkedHashSet<Any>>(it, equalLinkedHashSetOf())
            } ?: PowersetLattice.Element(Pair(node, equalLinkedHashSetOf()))
        }
        is PointerDereference -> {
            val ret = PowersetLattice.Element<Pair<Node, EqualLinkedHashSet<Any>>>()
            this.getAddresses(node, node).forEach { addr ->
                val lastWrite = this.declarationsState[addr]?.third
                // Usually, we should have a lastwrite, so we take that
                if (lastWrite?.isNotEmpty() == true)
                    lastWrite.mapTo(PowersetLattice.Element()) {
                        ret.add(Pair(it.first, it.second))
                    }
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
            this.getAddresses(node, node)
                .filterTo(PowersetLattice.Element()) {
                    this.declarationsState[it]?.third?.isNotEmpty() == true
                }
                .flatMapTo(PowersetLattice.Element()) {
                    this.declarationsState[it]?.third?.mapNotNull {
                        Pair(
                            it.first,
                            it.second.filterTo(EqualLinkedHashSet()) {
                                !(it is PartialDataflowGranularity<*> &&
                                    it.partialTarget is FieldDeclaration &&
                                    it.partialTarget.name.localName == partial.localName)
                            },
                        )
                    } ?: PowersetLattice.Element()
                }
        }
        is ParameterMemoryValue -> {
            // For parameterMemoryValues, we have to check if there was a write within the function.
            // If not, it's the deref value itself.
            val entries = this.declarationsState[node]?.third
            if (entries?.isNotEmpty() == true)
                return entries.mapTo(PowersetLattice.Element()) {
                    Pair(it.first, equalLinkedHashSetOf())
                }
            else
                return node.memoryValues
                    .filter { it.name.localName == "deref" + node.name.localName }
                    .mapTo(PowersetLattice.Element()) { Pair(it, equalLinkedHashSetOf()) }
        }
        else ->
            // For the rest, we read the declarationState to determine when the memoryAddress of the
            // node was last written to
            this.getAddresses(node, node)
                /*                .filterTo(PowersetLattice.Element()) {
                    this.declarationsState[it]?.third?.isNotEmpty() == true
                }*/
                .flatMapTo(PowersetLattice.Element()) {
                    this.declarationsState[it]?.third?.map { Pair(it.first, it.second) } ?: setOf()
                }
    }
}

fun PointsToStateElement.getValues(
    node: Node,
    startNode: Node,
): PowersetLattice.Element<Pair<Node, Boolean>> {
    return when (node) {
        is PointerReference -> {
            /*
             * For PointerReferences, the value is the address of the input
             * For example, the value of `&i` is the address of `i`
             */
            fetchAddressFromGeneralState(node.input).mapTo(PowersetLattice.Element()) {
                Pair(it, false)
            }
        }
        is PointerDereference -> {
            /* To find the value for PointerDereferences, we first fetch the values form its input from the generalstate, which is probably a MemoryAddress
             * Then we look up the current value at this MemoryAddress
             */
            val inputVals = this.fetchValueFromGeneralState(node.input).map { it.first }
            val retVal = PowersetLattice.Element<Pair<Node, Boolean>>()
            /* If the node is not the same as the startNode, we should have already assigned a value, so we fetch it from the generalstate */
            if (node != startNode && node !in startNode.astChildren)
                inputVals.forEach {
                    retVal.addAll(
                        fetchValueFromGeneralState(it).mapTo(PowersetLattice.Element()) {
                            Pair(it.first, true in it.second)
                        }
                    )
                }
            else {
                inputVals.forEach { input ->
                    retVal.addAll(
                        fetchValueFromDeclarationState(input, true).mapTo(
                            PowersetLattice.Element()
                        ) {
                            Pair(it.value, it.shortFS)
                        }
                    )
                }
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
                .flatMap { fetchValueFromDeclarationState(it) }
                .map { it.value }
                .mapTo(PowersetLattice.Element()) { Pair(it, false) }
        }
        is MemoryAddress,
        is CallExpression -> {
            fetchValueFromDeclarationState(node).mapTo(PowersetLattice.Element()) {
                Pair(it.value, it.shortFS)
            }
        }
        is MemberExpression -> {
            val (base, fieldName) = resolveMemberExpression(node)
            val baseAddresses = getAddresses(base, startNode)
            val fieldAddresses = fetchFieldAddresses(baseAddresses, fieldName)
            if (fieldAddresses.isNotEmpty()) {
                val retVal = PowersetLattice.Element<Pair<Node, Boolean>>()
                fieldAddresses.forEach { fa ->
                    if (hasDeclarationStateEntry(fa)) {
                        fetchValueFromDeclarationState(fa).map {
                            retVal.add(Pair(it.value, it.shortFS))
                        }
                    } else {
                        // Let's overapproximate here: In case we find no known value for the field,
                        // we try again with the baseAddresses
                        baseAddresses.forEach { ba ->
                            fetchValueFromDeclarationState(ba).map {
                                retVal.add(Pair(it.value, it.shortFS))
                            }
                        }
                    }
                }
                return retVal
            } else {
                val newName = Name(getNodeName(node).localName, base.name)
                PowersetLattice.Element(
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
            /* If the node is not the same as the startNode, we should have already assigned a value to the reference, so we fetch it from the generalstate */
            if (node != startNode && node !in startNode.astChildren)
                return fetchValueFromGeneralState(node).mapTo(PowersetLattice.Element()) {
                    Pair(it.first, true in it.second)
                }

            /* Otherwise, we have to look up the last value written to the reference's declaration. */
            val retVals = PowersetLattice.Element<Pair<Node, Boolean>>()
            this.getAddresses(node, startNode).forEach { addr ->
                // For globals fetch the values from the globalDeref map
                if (isGlobal(node))
                    retVals.addAll(
                        fetchValueFromDeclarationState(addr).map { Pair(it.value, false) }
                    )
                else {
                    this.getValues(addr, startNode).forEach { v ->
                        // We want to skip values that contain the node itself and therefore could
                        // cause a loop
                        // So we fetch parent AssignExpression of the value in the AST, and if the
                        // node is any of its children, we skip that value
                        var valueParentAssignExpression: Node? = v.first
                        while (
                            valueParentAssignExpression !is AssignExpression &&
                                valueParentAssignExpression != null
                        ) valueParentAssignExpression = valueParentAssignExpression.astParent
                        if (node !in SubgraphWalker.flattenAST(valueParentAssignExpression))
                            retVals.add(v)
                    }
                    //                   retVals.addAll(this.getValues(addr))
                }
            }
            return retVals
        }
        is CastExpression -> {
            this.getValues(node.expression, startNode)
        }
        is SubscriptExpression -> {
            this.getAddresses(node, startNode).flatMapTo(PowersetLattice.Element()) {
                this.getValues(it, it)
            }
        }
        else -> PowersetLattice.Element(Pair(node, false))
    }
}

fun PointsToStateElement.getAddresses(node: Node, startNode: Node): IdentitySet<Node> {
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
            // Here, it depends on our scope. When we are outside the function to which the PMV
            // belongs, we assume it has already been initialized and can simply look up the
            // `memoryAddresses` field
            // However, if we are dealing with a PMV from the function we are currently in, this
            // information has not yet been propagated to the node, so we check out the state
            val ret = node.memoryAddresses.toIdentitySet<Node>()
            if (ret.isNotEmpty()) return ret
            else return this.declarationsState[node]?.first ?: identitySetOf()
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
            val ret = identitySetOf<Node>()
            // When the node.input is not the startNode,
            val inputVals =
                if (node.input != startNode && node != startNode)
                    this.fetchValueFromGeneralState(node.input)
                else this.getValues(node.input, startNode)
            inputVals.forEach { (value, _) ->
                // In case the value is a BinaryOperator (like `*(ptr + 8)`), we treat this as a
                // SubscriptExpression for now
                // We assume that the rhs of the BinaryOperator is the pointer, and the rhs is a
                // literal that describes the offset
                if (
                    value is BinaryOperator &&
                        value.operatorCode == "+" &&
                        (value.lhs is Reference || value.lhs is CastExpression) &&
                        value.rhs is Literal<*>
                ) {
                    var lhs = value.lhs
                    // Remove possible casts
                    while (lhs is CastExpression) {
                        lhs = lhs.expression
                    }
                    val sub = SubscriptExpression()
                    sub.arrayExpression = lhs
                    sub.subscriptExpression = value.rhs
                    ret.addAll(getAddresses(sub, startNode))
                } else ret.add(value)
            }
            return ret
        }
        is MemberExpression -> {
            /*
             * For MemberExpressions, the fieldAddresses in the MemoryAddress node of the base hold the information we are looking for
             */
            val (base, newName) = resolveMemberExpression(node)
            fetchFieldAddresses(this.getAddresses(base, startNode), newName)
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
            this.getAddresses(node.expression, startNode)
        }
        is SubscriptExpression -> {
            val localName = getNodeName(node.subscriptExpression)
            // When startNode is different from the current node, we should already have an entry,
            // so we fetch that from the general state
            val baseValues =
                if (startNode != node) fetchValueFromGeneralState(node.base)
                else this.getValues(node.base, startNode)
            baseValues.flatMapTo(identitySetOf()) {
                fetchFieldAddresses(
                    identitySetOf(it.first),
                    Name(localName.localName, getNodeName(it.first)),
                )
            }
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
): PowersetLattice.Element<Pair<Node, Boolean>> {
    if (nestingDepth == -1) return PowersetLattice.Element(Pair(node, false))
    if (nestingDepth == 0)
        return this.getAddresses(node, node).mapTo(PowersetLattice.Element()) { Pair(it, false) }
    var ret =
        if (
            node !is PointerReference &&
                onlyFetchExistingEntries &&
                this.getAddresses(node, node).none { addr ->
                    this.hasDeclarationStateEntry(addr, excludeShortFSValues)
                }
        )
            PowersetLattice.Element()
        else
            getValues(node, node).filterTo(
                PowersetLattice.Element()
            ) { /*it.second != excludeShortFSValues*/
                if (excludeShortFSValues) !it.second else true
            }
    for (i in 1..<nestingDepth) {
        ret =
            ret.filterTo(PowersetLattice.Element()) {
                    if (onlyFetchExistingEntries)
                        this.hasDeclarationStateEntry(it.first, excludeShortFSValues)
                    else true
                }
                .flatMap {
                    this.fetchValueFromDeclarationState(it.first, fetchFields, excludeShortFSValues)
                }
                .mapTo(PowersetLattice.Element()) { Pair(it.value, it.shortFS) }
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
                        MemoryAddress(nodeName, isGlobal(addr))
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
    doubleState: PointsToStateElement,
    sources: PowersetLattice.Element<Triple<Node?, Boolean, Boolean>>,
    destinations: IdentitySet<Node>,
    // Node and short FS yes or no
    destinationAddresses: IdentitySet<Node>,
    lastWrites: PowersetLattice.Element<Pair<Node, EqualLinkedHashSet<Any>>>,
): PointsToStateElement {
    var doubleState = doubleState

    /* Update the declarationState for the addresses */
    destinationAddresses.forEach { destAddr ->
        if (!isGlobal(destAddr)) {
            val currentEntries =
                this.declarationsState[destAddr]?.first?.toIdentitySet() ?: identitySetOf(destAddr)

            // If we want to update the State with exactly the same elements as are already in the
            // state, we do nothing in order not to confuse the iterateEOG function
            val newSources: PowersetLattice.Element<Pair<Node, Boolean>> =
                sources
                    .mapTo(PowersetLattice.Element()) { triple ->
                        val existingPair =
                            this.declarationsState[destAddr]?.second?.firstOrNull {
                                it.first === triple.first && it.second == triple.second
                            }
                        existingPair ?: Pair(triple.first, triple.second)
                    }
                    .filterTo(PowersetLattice.Element()) { it.first != null }
                    .mapTo(PowersetLattice.Element()) { Pair(it.first!!, it.second) }

            // Check if we have any full writes
            val fullSourcesExist = sources.any { !it.third }

            /* val newPrevDFG =
            // TODO: Do we also need to fetch some properties here?
            lastWrites.mapTo(IdentitySet()) { Pair(it, false) }*/
            // If we already have exactly this value in the state for the prevDFGs, we take that in
            // order not to confuse the iterateEOG function
            val prevDFG = PowersetLattice.Element<Pair<Node, EqualLinkedHashSet<Any>>>()
            lastWrites.forEach { lw ->
                val existingEntries =
                    doubleState.declarationsState[destAddr]?.third?.filter { entry ->
                        entry.first === lw.first &&
                            lw.second.all { it in entry.second } &&
                            lw.second.size == entry.second.size
                    }
                if (existingEntries?.isNotEmpty() == true) prevDFG.addAll(existingEntries)
                else prevDFG.add(lw)
            }

            // If we have any full writes, we eliminate the previous state
            if (fullSourcesExist) {
                val newDeclState = this.declarationsState.duplicate()
                newDeclState[destAddr] =
                    DeclarationStateEntryElement(
                        PowersetLattice.Element(currentEntries),
                        PowersetLattice.Element(newSources),
                        PowersetLattice.Element(prevDFG),
                    )
                doubleState = PointsToStateElement(doubleState.generalState, newDeclState)
            } else {
                doubleState =
                    lattice.pushToDeclarationsState(
                        doubleState,
                        destAddr,
                        DeclarationStateEntryElement(
                            PowersetLattice.Element(currentEntries),
                            PowersetLattice.Element(newSources),
                            PowersetLattice.Element(prevDFG),
                        ),
                    )
            }

            /* Also update the generalState for dst (if we have any destinations) */
            // If the lastWrites are in the sources or destinations, we don't have to set the
            // prevDFG edges
            // Except for callexpressions w/o invokes body for which we have to do this to create
            // the short FS paths
            val newLastWrites = lastWrites
            newLastWrites.removeIf { lw ->
                destinations.none {
                    it is CallExpression && it.invokes.singleOrNull()?.body == null
                } &&
                    (sources.any { src -> src.first === lw.first && src.second in lw.second } ||
                        lw.first in destinations)
            }
            destinations.forEach { d ->
                val newGenState = this.generalState.duplicate()
                newGenState[d] =
                    GeneralStateEntryElement(
                        PowersetLattice.Element(destinationAddresses),
                        PowersetLattice.Element(
                            sources
                                .filter { it.first != null }
                                .mapTo(PowersetLattice.Element()) {
                                    Pair(it.first!!, equalLinkedHashSetOf(it.second))
                                }
                        ),
                        PowersetLattice.Element(newLastWrites),
                    )
                doubleState = PointsToStateElement(newGenState, doubleState.declarationsState)
            }
        } else {
            // For globals, we draw a DFG Edge from the source to the destination
            destinations.forEach { d ->
                val entry =
                    doubleState.generalState.computeIfAbsent(d) {
                        GeneralStateEntryElement(
                            PowersetLattice.Element(),
                            PowersetLattice.Element(),
                            PowersetLattice.Element(),
                        )
                    }
                sources
                    .filter { it.first != null }
                    .map { entry.third.add(Pair(it.first!!, equalLinkedHashSetOf(it.second))) }
            }
        }
    }

    return doubleState
}
