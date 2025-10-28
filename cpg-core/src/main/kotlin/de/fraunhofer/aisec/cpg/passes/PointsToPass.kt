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
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration.FSEntry
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
import de.fraunhofer.aisec.cpg.helpers.functional.TupleLattice.Element
import de.fraunhofer.aisec.cpg.helpers.identitySetOf
import de.fraunhofer.aisec.cpg.helpers.toIdentitySet
import de.fraunhofer.aisec.cpg.passes.PointsToPass.NodeWithPropertiesKey
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.Pair
import kotlin.collections.MutableSet
import kotlin.collections.filter
import kotlin.collections.map
import kotlin.let
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis
import kotlin.text.contains
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

val nodesCreatingUnknownValues = ConcurrentHashMap<Pair<Node, Name>, MemoryAddress>()
var totalFunctionDeclarationCount = 0
var analyzedFunctionDeclarationCount = 0
var timeInHandleCallExpression: Long = 0
var timeInHandleExpression: Long = 0
var timeInHandleAssignExpression: Long = 0
var timeInHandleUnaryOperator: Long = 0
var timeInHandleDeclaration: Long = 0
var timeToPush: Long = 0
var totalTimeinTransfer: Long = 0
var timeInTransfer: Long = 0
var timeTotalIterateEOG: Long = 0
var totalTimeinAccept: Long = 0
var transferActionCounter: Long = 0
var PointsTotransferCounter: Long = 0

typealias GeneralStateEntry =
    TripleLattice<
        PowersetLattice.Element<Node>,
        PowersetLattice.Element<NodeWithPropertiesKey>,
        PowersetLattice.Element<NodeWithPropertiesKey>,
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
        PowersetLattice.Element<NodeWithPropertiesKey>,
        // prevDFG
        PowersetLattice.Element<NodeWithPropertiesKey>,
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
        PowersetLattice.Element<NodeWithPropertiesKey>,
    >

typealias SingleGeneralStateElement = MapLattice.Element<Node, GeneralStateEntryElement>

typealias SingleDeclarationStateElement = MapLattice.Element<Node, DeclarationStateEntryElement>

typealias SingleGeneralState = MapLattice<Node, GeneralStateEntryElement>

typealias SingleDeclarationState = MapLattice<Node, DeclarationStateEntryElement>

class DeclarationStateEntry(
    addresses: PowersetLattice<Node>,
    values: PowersetLattice<Pair<Node, Boolean>>,
    lastWrites: PowersetLattice<NodeWithPropertiesKey>,
) :
    TripleLattice<
        PowersetLattice.Element<Node>,
        PowersetLattice.Element<Pair<Node, Boolean>>,
        PowersetLattice.Element<NodeWithPropertiesKey>,
    >(addresses, values, lastWrites) {
    class Element(
        one: PowersetLattice.Element<Node>,
        two: PowersetLattice.Element<Pair<Node, Boolean>>,
        three: PowersetLattice.Element<NodeWithPropertiesKey>,
    ) :
        TripleLattice.Element<
            PowersetLattice.Element<Node>,
            PowersetLattice.Element<Pair<Node, Boolean>>,
            PowersetLattice.Element<NodeWithPropertiesKey>,
        >(one, two, three) {
        override fun compare(other: Lattice.Element): Order {
            if (this === other) return Order.EQUAL

            if (other !is DeclarationStateEntry.Element)
                throw IllegalArgumentException(
                    "$other should be of type Element but is of type ${other.javaClass}"
                )

            val result1 = this@Element.first.compare(other.first)
            val result2 = this@Element.second.compare(other.second)
            return compareMultiple(result1, result2)
        }
    }
}

class PointsToState(
    innerLattice1: Lattice<SingleGeneralStateElement>,
    innerLattice2: Lattice<SingleDeclarationStateElement>,
) :
    TupleLattice<SingleGeneralStateElement, SingleDeclarationStateElement>(
        innerLattice1,
        innerLattice2,
    ) {
    infix fun <A : Lattice.Element, B : Lattice.Element> A.to(that: B): TupleLattice.Element<A, B> =
        Element(this, that)

    override val bottom: Element
        get() = Element(innerLattice1.bottom, innerLattice2.bottom)

    override suspend fun lub(
        one: TupleLattice.Element<SingleGeneralStateElement, SingleDeclarationStateElement>,
        two: TupleLattice.Element<SingleGeneralStateElement, SingleDeclarationStateElement>,
        allowModify: Boolean,
        widen: Boolean,
        concurrencyCounter: Int,
    ): PointsToState.Element {
        val result =
            super.lub(
                one = one,
                two = two,
                allowModify = allowModify,
                widen = widen,
                concurrencyCounter = CPU_CORES,
            )
        return result as? PointsToState.Element ?: Element(result)
    }

    override suspend fun glb(
        one: TupleLattice.Element<SingleGeneralStateElement, SingleDeclarationStateElement>,
        two: TupleLattice.Element<SingleGeneralStateElement, SingleDeclarationStateElement>,
    ): PointsToState.Element {
        val result = super.glb(one, two)
        return result as? PointsToState.Element ?: Element(result)
    }

    override fun duplicate(
        one: TupleLattice.Element<SingleGeneralStateElement, SingleDeclarationStateElement>
    ): TupleLattice.Element<SingleGeneralStateElement, SingleDeclarationStateElement> {
        return Element(super.duplicate(one))
    }

    class Element(first: SingleGeneralStateElement, second: SingleDeclarationStateElement) :
        TupleLattice.Element<SingleGeneralStateElement, SingleDeclarationStateElement>(
            first,
            second,
        ) {
        constructor(
            tupleState:
                TupleLattice.Element<SingleGeneralStateElement, SingleDeclarationStateElement>
        ) : this(tupleState.first, tupleState.second)

        val mutex = Mutex()

        override fun compare(other: Lattice.Element): Order {
            if (this === other) return Order.EQUAL

            if (other !is Element)
                throw IllegalArgumentException(
                    "$other should be of type Element but is of type ${other.javaClass}"
                )

            return this@Element.second.compare(other.second)
        }

        suspend fun parallelCompare(other: Lattice.Element): Order {
            if (this === other) return Order.EQUAL

            if (other !is Element)
                throw IllegalArgumentException(
                    "$other should be of type Element but is of type ${other.javaClass}"
                )

            return this@Element.second.parallelCompare(other.second)
        }

        override fun duplicate():
            TupleLattice.Element<SingleGeneralStateElement, SingleDeclarationStateElement> {
            return Element(super.duplicate())
        }
    }
}

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

fun calculateInnerConcurrencyCounter(outerConcurrencyCounter: Int): Int {
    return if (outerConcurrencyCounter == 0) CPU_CORES
    else if (outerConcurrencyCounter > CPU_CORES) 1 else CPU_CORES / outerConcurrencyCounter
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

        /** This specifies if we are running after DFG edges to create the detailed shortFS * */
        var detailedShortFS: Boolean = true,

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
        val (ret, time) = measureTimedValue { runBlocking { acceptInternal(node) } }
        totalTimeinAccept += time.toLong(DurationUnit.MILLISECONDS)
        timeInTransfer = 0
        timeInHandleExpression = 0
        timeInHandleAssignExpression = 0
        timeInHandleCallExpression = 0
        timeInHandleDeclaration = 0
        timeInHandleUnaryOperator = 0
        timeToPush = 0
        log.info(
            "Time spent in accept: ${time.toLong(DurationUnit.MILLISECONDS)}. Total: $totalTimeinAccept"
        )
        log.info("PointsToPass counter: $PointsTotransferCounter")
        return ret
    }

    suspend fun acceptInternal(node: Node) {
        // For now, we only execute this for function declarations, we will support all EOG starters
        // in the future.
        if (node !is FunctionDeclaration) {
            return
        }

        // If we haven't done so yet, set the total number of functions
        if (totalFunctionDeclarationCount == 0)
            totalFunctionDeclarationCount =
                node.firstParentOrNull<TranslationResult>().functions.size

        analyzedFunctionDeclarationCount++

        // If the node has a body and a function summary, we have visited it before and can
        // return here.
        if (
            (node.functionSummary.isNotEmpty() && node.body != null) &&
                node.functionSummary.keys.any { it in node.parameters || it in node.returns }
        ) {
            "Skipping function ${node.name} because we already have a function Summary. (Function $analyzedFunctionDeclarationCount / $totalFunctionDeclarationCount)"
            return
        }

        functionSummaryAnalysisChain.add(node)

        // Calculate the complexity of the function and see, if it exceeds our threshold
        val max = passConfig<Configuration>()?.maxComplexity
        val c = node.body?.cyclomaticComplexity() ?: 0
        if (max != null && c > max) {
            log.info(
                "Ignoring function ${node.name} because its complexity (${NumberFormat.getNumberInstance(Locale.US).format(c)}) is greater than the configured maximum (${max})"
            )
            // Add an empty function Summary so that we don't try again
            node.functionSummary.computeIfAbsent(ReturnStatement()) {
                ConcurrentHashMap.newKeySet<FSEntry>()
            }
            return
        }

        log.info(
            "Analyzing function ${node.name}. Complexity: ${NumberFormat.getNumberInstance(Locale.US).format(c)}. (Function $analyzedFunctionDeclarationCount / $totalFunctionDeclarationCount)"
        )
        log.debug(
            "Total time spent until now in handleCallExpression and transfer: $timeInHandleCallExpression and ${totalTimeinTransfer/1000000}"
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

        val (finalState, iterateEOGTime) =
            measureTimedValue {
                if (node.body == null) {
                    handleEmptyFunctionDeclaration(lattice, startState, node)
                } else {
                    lattice.iterateEOG(node.nextEOGEdges, startState, ::transfer)
                        as PointsToState.Element
                }
            }

        timeTotalIterateEOG += iterateEOGTime.toLong(DurationUnit.MILLISECONDS)
        log.info(
            "Time for EOG iteration: ${iterateEOGTime.toLong(DurationUnit.MILLISECONDS)}. Total: $timeTotalIterateEOG"
        )

        val drawEdgesTime = measureTimeMillis {
            for ((key, value) in finalState.generalState) {
                // The generalState values have 3 items: The address, the value, and the
                // prevDFG-Edges
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
                    // callingcontext, or a boolean indicating if this is a functionSummary edge or
                    // not
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
        }

        log.info("Time to draw Edges: $drawEdgesTime")

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
    private suspend fun handleEmptyFunctionDeclaration(
        lattice: PointsToState,
        startState: PointsToState.Element,
        functionDeclaration: FunctionDeclaration,
    ): PointsToState.Element {
        var doubleState = startState

        if (functionDeclaration.functionSummary.isEmpty()) {
            // Add a dummy function summary so that we don't try this every time
            // In this dummy, all parameters point to the return
            // TODO: Also add possible dereference values to the input?
            val prevDFGs = PowersetLattice.Element<NodeWithPropertiesKey>()
            val newEntries =
                ConcurrentHashMap.newKeySet<FSEntry>().apply {
                    add(FSEntry(0, functionDeclaration, 1, ""))
                }
            functionDeclaration.parameters.forEach { param ->
                // The short FS
                newEntries.add(
                    FunctionDeclaration.FSEntry(
                        0,
                        null,
                        1,
                        "",
                        mutableSetOf(NodeWithPropertiesKey(param, equalLinkedHashSetOf())),
                        equalLinkedHashSetOf(true),
                        true,
                    )
                )
                // The prevDFG edges for the function Declaration
                doubleState
                    .getValues(param, param)
                    .filter { it.first is ParameterMemoryValue }
                    .forEach {
                        prevDFGs.add(NodeWithPropertiesKey(it.first, equalLinkedHashSetOf<Any>()))
                    }
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
                        // We couldn't set the lastWrites when creating the functionSummary
                        // (which
                        // has to be hardcoded b/c we don't have a body), so we replace that
                        // now
                        entry.lastWrites.forEach {
                            functionDeclaration.functionSummary[param]
                                ?.singleOrNull { it == entry }
                                ?.lastWrites
                                ?.remove(it)
                        }
                        functionDeclaration.functionSummary[param]
                            ?.singleOrNull { it == entry }
                            ?.lastWrites
                            ?.add(NodeWithPropertiesKey(dst, equalLinkedHashSetOf()))
                        val propertySet = equalLinkedHashSetOf<Any>()
                        if (entry.subAccessName != "")
                            propertySet.add(
                                FieldDeclaration().apply { name = Name(entry.subAccessName) }
                            )
                        val shortFsEntry = entry.properties.singleOrNull { it is Boolean }
                        if (shortFsEntry != null) propertySet.add(shortFsEntry)
                        doubleState.mutex.withLock {
                            doubleState =
                                lattice.push(
                                    doubleState,
                                    dst,
                                    GeneralStateEntryElement(
                                        PowersetLattice.Element(),
                                        PowersetLattice.Element(),
                                        PowersetLattice.Element(
                                            NodeWithPropertiesKey(src, propertySet)
                                        ),
                                    ),
                                )
                        }
                    }
                }
            }
        }

        return doubleState
    }

    data class IndexKey(val node: Node, val index: Int) {
        // Since we are dealing with pairs, carefully check if we already have them
        override fun equals(other: Any?): Boolean =
            other is IndexKey && node == other.node && index == other.index
    }

    private suspend fun storeFunctionSummary(
        node: FunctionDeclaration,
        doubleState: PointsToState.Element,
    ) {
        clearFSDummies(node.functionSummary)
        var dfgSearchTime: Long = 0
        // We try to run this in outer coroutines for each param and inner coroutines per
        // param.
        // In order not to launch an immense amount of coroutines, we calculate their number before
        val outerCoroutineCounter = node.parameters.size
        val innerCoroutineCounter =
            if (outerCoroutineCounter == 0) CPU_CORES
            else if (outerCoroutineCounter > CPU_CORES) 1 else CPU_CORES / outerCoroutineCounter
        val totalTime = measureTimeMillis {
            coroutineScope {
                node.parameters.forEach { param ->
                    launch(Dispatchers.Default) {
                        // Collect all addresses of the parameter that we can use as index to look
                        // up possible new values
                        val indexes: MutableSet<IndexKey> = ConcurrentHashMap.newKeySet()
                        val values =
                            doubleState.getValues(param, param).mapTo(IdentitySet()) { it.first }

                        // We look at the deref and the derefderef, hence for depth 2 and 3
                        // We have to look up the index of the ParameterMemoryValue to check out
                        // changes on the dereferences
                        values
                            .filterTo(identitySetOf()) { doubleState.hasDeclarationStateEntry(it) }
                            .map { indexes.add(IndexKey(it, 2)) }
                        // Additionally, we can check out the "dereference" itself to look for
                        // "derefdereferences"
                        values
                            .filterTo(identitySetOf()) { doubleState.hasDeclarationStateEntry(it) }
                            .flatMap {
                                doubleState.getValues(it, it).mapTo(PowersetLattice.Element()) {
                                    it.first
                                }
                            }
                            // We are already inside $paramCount coroutines, so we have to divide
                            // the CPU_CORES by these routines
                            .splitInto(maxParts = innerCoroutineCounter)
                            .map { chunk ->
                                launch(Dispatchers.Default) {
                                    for (value in chunk) {
                                        if (doubleState.hasDeclarationStateEntry(value))
                                            indexes.add(IndexKey(value, 3))
                                    }
                                }
                            }
                            .joinAll()

                        indexes.forEach { (index, dstValueDepth) ->
                            val stateEntries =
                                doubleState
                                    .fetchValueFromDeclarationState(index, true, true)
                                    .filterTo(PowersetLattice.Element()) {
                                        it.value.name != param.name
                                    }
                            /*log.debug(
                                "In storeFunctionSummary, param ${param.argumentIndex} (${param.name.localName}. Index ${index.name.localName} (${indexes.size} in total). stateEntries: ${stateEntries.size}"
                            )*/
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
                                // If so, store the information for the parameter in the
                                // FunctionSummary
                                // We are already inside $paramCount coroutines, so we have to
                                // divide the CPU_CORES by these routines
                                .splitInto(maxParts = innerCoroutineCounter, minPartSize = 1)
                                .map { chunk ->
                                    launch(Dispatchers.Default) {
                                        chunk.forEach { (value, shortFS, subAccessName, lastWrites)
                                            ->
                                            // Extract the value depth from the value's localName
                                            val srcValueDepth = stringToDepth(value.name.localName)
                                            // Store the information in the functionSummary
                                            val existingEntry =
                                                node.functionSummary.computeIfAbsent(param) {
                                                    ConcurrentHashMap.newKeySet<FSEntry>()
                                                }
                                            val filteredLastWrites =
                                                lastWrites
                                                    // for shortFS,only use these, and for !shortFS,
                                                    // only those
                                                    .filterTo(PowersetLattice.Element()) {
                                                        shortFS in it.properties
                                                    }
                                            existingEntry.add(
                                                FSEntry(
                                                    dstValueDepth,
                                                    value,
                                                    srcValueDepth,
                                                    subAccessName,
                                                    filteredLastWrites,
                                                    equalLinkedHashSetOf(shortFS),
                                                )
                                            )
                                            // Additionally, we store this as a shortFunctionSummary
                                            // were the function writes to the parameter
                                            val shortFSEntry =
                                                FSEntry(
                                                    dstValueDepth,
                                                    node,
                                                    0,
                                                    subAccessName,
                                                    PowersetLattice.Element(
                                                        NodeWithPropertiesKey(
                                                            node,
                                                            equalLinkedHashSetOf(),
                                                        )
                                                    ),
                                                    equalLinkedHashSetOf(true),
                                                )
                                            // Add the new entry if it doesn't exist yet
                                            synchronized(existingEntry) {
                                                if (existingEntry.none { it == shortFSEntry })
                                                    existingEntry.add(shortFSEntry)
                                            }
                                            val propertySet = identitySetOf<Any>(true)
                                            if (subAccessName != "")
                                                propertySet.add(
                                                    FieldDeclaration().apply {
                                                        name = Name(subAccessName)
                                                    }
                                                )

                                            // Create the detailed shortFS. Like, which parameter
                                            // influences
                                            // what.
                                            // This may take a lot of time, so this is optional
                                            if (
                                                (passConfig<Configuration>()?.detailedShortFS
                                                    ?: true)
                                            ) {
                                                if (!shortFS) {
                                                    // Check if the value is influenced by a
                                                    // Parameter and
                                                    // if so, add this information to the
                                                    // functionSummary
                                                    val (paths, searchTime) =
                                                        measureTimedValue {
                                                            value.followDFGEdgesUntilHit(
                                                                collectFailedPaths = false,
                                                                findAllPossiblePaths = false,
                                                                direction =
                                                                    Backward(GraphToFollow.DFG),
                                                                sensitivities =
                                                                    OnlyFullDFG +
                                                                        FieldSensitive +
                                                                        ContextSensitive,
                                                                // We need to search interprocedural
                                                                // here.
                                                                // In order this acceptable also in
                                                                // larger graphs,
                                                                // we limit the maxCallDepth and hop
                                                                // size
                                                                scope =
                                                                    Interprocedural(
                                                                        maxCallDepth = 1,
                                                                        maxSteps = 10,
                                                                    ),
                                                                predicate = {
                                                                    it is ParameterMemoryValue &&
                                                                        /* If it's a ParameterMemoryValue from the node's
                                                                        parameters, it has to have a DFG Node to one
                                                                        of the node's parameters. Either partial to a derefvalue or full to the parameterdeclaration */
                                                                        it.memoryValueUsageEdges
                                                                            .filter {
                                                                                ((it.granularity is
                                                                                    PartialDataflowGranularity<
                                                                                        *
                                                                                    > &&
                                                                                    ((it.granularity
                                                                                                as
                                                                                                PartialDataflowGranularity<
                                                                                                    *
                                                                                                >)
                                                                                            .partialTarget
                                                                                            as?
                                                                                            String)
                                                                                        ?.endsWith(
                                                                                            "derefvalue"
                                                                                        ) ==
                                                                                        true) ||
                                                                                    (it
                                                                                        .granularity is
                                                                                        FullDataflowGranularity &&
                                                                                        it.end is
                                                                                            ParameterDeclaration)) &&
                                                                                    it.end in
                                                                                        node
                                                                                            .parameters
                                                                            }
                                                                            .size == 1 &&
                                                                        node.parameters.any { param
                                                                            ->
                                                                            param.name.localName ==
                                                                                it.name.parent
                                                                                    ?.localName
                                                                        }
                                                                },
                                                            )
                                                        }
                                                    dfgSearchTime +=
                                                        searchTime.toLong(DurationUnit.MILLISECONDS)
                                                    paths.fulfilled
                                                        .map { it.nodes.last() }
                                                        .forEach { sourceParamValue ->
                                                            val matchingDeclarations =
                                                                node.parameters.singleOrNull {
                                                                    it.name ==
                                                                        sourceParamValue.name.parent
                                                                }
                                                            if (matchingDeclarations == null) TODO()
                                                            node.functionSummary
                                                                .computeIfAbsent(param) {
                                                                    ConcurrentHashMap.newKeySet()
                                                                }
                                                                .add(
                                                                    FSEntry(
                                                                        dstValueDepth,
                                                                        matchingDeclarations,
                                                                        stringToDepth(
                                                                            sourceParamValue.name
                                                                                .localName
                                                                        ),
                                                                        subAccessName,
                                                                        mutableSetOf(
                                                                            NodeWithPropertiesKey(
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
                                }
                        }
                    }
                }
            }

            // If we don't have anything to summarize, we add a dummy entry to the functionSummary
            if (node.functionSummary.isEmpty()) {
                node.functionSummary[newLiteral("dummy")] = ConcurrentHashMap.newKeySet<FSEntry>()
            }
        }
        val percentage =
            try {
                dfgSearchTime * 100 / totalTime
            } catch (_: java.lang.ArithmeticException) {
                0
            }
        log.info(
            "Finished storeFunctionSummary for ${node.name.localName}. Total time: $totalTime. DFG Search Time: $dfgSearchTime ($percentage %)"
        )
    }

    private fun MutableSet<NodeWithPropertiesKey>.parallelEquals(
        other: PowersetLattice.Element<NodeWithPropertiesKey?>
    ): Boolean {
        return this == other
    }

    protected suspend fun transfer(
        lattice:
            Lattice<TupleLattice.Element<SingleGeneralStateElement, SingleDeclarationStateElement>>,
        currentEdge: EvaluationOrder,
        state: TupleLattice.Element<SingleGeneralStateElement, SingleDeclarationStateElement>,
    ): PointsToState.Element {
        var doubleState =
            state as? PointsToState.Element
                ?: throw java.lang.IllegalArgumentException(
                    "Expected the state to be of type PointsToState.Element"
                )
        transferActionCounter++
        PointsTotransferCounter++
        if (transferActionCounter == 10000L) {
            log.info(
                "Transferring. timeInTransfer: ${timeInTransfer/1000000}, timeInHandleAssignExpression: ${timeInHandleAssignExpression/1000000}, timeInHandleCallExpression: ${timeInHandleCallExpression/1000000}, timeInHandleExpression: ${timeInHandleExpression/1000000}, timeToPush: ${timeToPush/1000000}, timeInHandleDeclaration: ${timeInHandleDeclaration/1000000}, timeInHandleUnaryOperator: ${timeInHandleUnaryOperator/1000000}"
            )
            transferActionCounter = 0
        }
        timeInTransfer += measureNanoTime {
            val lattice = lattice as? PointsToState ?: return state
            val currentNode = currentEdge.end

            coroutineScope {
                // Used to keep iterating for steps which do not modify the alias-state otherwise

                val tmp = measureTimedValue {
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
                }
                doubleState = tmp.value
                timeToPush += tmp.duration.toLong(DurationUnit.NANOSECONDS)
                doubleState =
                    when (currentNode) {
                        is DeleteExpression ->
                            handleDeleteExpression(lattice, currentNode, doubleState)
                        is Declaration,
                        is MemoryAddress -> {
                            val (tmp, duration) =
                                measureTimedValue {
                                    handleDeclaration(lattice, currentNode, doubleState)
                                }
                            timeInHandleDeclaration += duration.toLong(DurationUnit.NANOSECONDS)
                            tmp
                        }

                        is AssignExpression -> {
                            val (tmp, duration) =
                                measureTimedValue {
                                    handleAssignExpression(lattice, currentNode, doubleState)
                                }
                            timeInHandleAssignExpression +=
                                duration.toLong(DurationUnit.NANOSECONDS)
                            tmp
                        }

                        is UnaryOperator -> {
                            val (tmp, duration) =
                                measureTimedValue {
                                    handleUnaryOperator(lattice, currentNode, doubleState)
                                }
                            timeInHandleUnaryOperator += duration.toLong(DurationUnit.NANOSECONDS)
                            tmp
                        }

                        is CallExpression -> {
                            val (tmp, duration) =
                                measureTimedValue {
                                    handleCallExpression(lattice, currentNode, doubleState)
                                }
                            timeInHandleCallExpression += duration.toLong(DurationUnit.NANOSECONDS)
                            tmp
                        }

                        is Expression -> {
                            val (tmp, duration) =
                                measureTimedValue {
                                    handleExpression(lattice, currentNode, doubleState)
                                }
                            timeInHandleExpression += duration.toLong(DurationUnit.NANOSECONDS)
                            tmp
                        }
                        is ReturnStatement ->
                            handleReturnStatement(lattice, currentNode, doubleState)
                        else -> doubleState
                    }
            }
        }
        totalTimeinTransfer += timeInTransfer
        //        log.info("Time in transfer: $timeinTransfer")
        return doubleState
    }

    protected suspend fun handleDeleteExpression(
        lattice: PointsToState,
        currentNode: DeleteExpression,
        doubleState: PointsToState.Element,
    ): PointsToState.Element {
        var doubleState = doubleState
        val sources =
            PowersetLattice.Element<Triple<Node?, Boolean, Boolean>>(
                Triple(currentNode, false, false)
            )
        val destinations: IdentitySet<Node> = currentNode.operands.toIdentitySet()
        val destinationsAddresses =
            destinations.flatMapTo(IdentitySet()) { doubleState.getAddresses(it, it) }
        val lastWrites: MutableSet<NodeWithPropertiesKey> = ConcurrentHashMap.newKeySet()
        lastWrites.add(NodeWithPropertiesKey(currentNode as Node, equalLinkedHashSetOf<Any>(false)))
        doubleState =
            doubleState.updateValues(
                lattice,
                doubleState,
                sources,
                destinations,
                destinationsAddresses,
                lastWrites,
            )
        return doubleState
    }

    private fun handleReturnStatement(
        lattice: PointsToState,
        currentNode: ReturnStatement,
        doubleState: PointsToState.Element,
    ): PointsToState.Element {
        /* For Return Statements, all we really want to do is to collect their return values
        to add them to the FunctionSummary */
        var doubleState = doubleState
        if (currentNode.returnValues.isNotEmpty()) {
            val parentFD = currentNode.firstParentOrNull<FunctionDeclaration>()
            if (parentFD != null) {
                currentNode.returnValues.forEach { retval ->
                    parentFD.functionSummary
                        .computeIfAbsent(currentNode) { ConcurrentHashMap.newKeySet<FSEntry>() }
                        .addAll(
                            doubleState.getValues(retval, retval).map {
                                FunctionDeclaration.FSEntry(
                                    0,
                                    it.first,
                                    1,
                                    "",
                                    mutableSetOf(
                                        NodeWithPropertiesKey(parentFD, equalLinkedHashSetOf())
                                    ),
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
    private suspend fun calculateIncomingCallingContexts(
        lattice: PointsToState,
        functionDeclaration: FunctionDeclaration,
        callExpression: CallExpression,
        doubleState: PointsToState.Element,
    ): PointsToState.Element {
        val generalStateUpdates = ConcurrentHashMap<Node, GeneralStateEntryElement>()
        var doubleState = doubleState
        val callingContext =
            CallingContextIn(
                mutableListOf(callExpression)
            ) // TODO: Indicate somehow if this has already been done?
        val innerConcurrencyCounter =
            calculateInnerConcurrencyCounter(callExpression.arguments.size)

        // We use caches to avoid double work
        val getNestedValuesCache =
            ConcurrentHashMap<
                Triple<Node, Int, Boolean>,
                PowersetLattice.Element<Pair<Node, Boolean>>,
            >()
        val getLastWritesCache =
            ConcurrentHashMap<Node, PowersetLattice.Element<PointsToPass.NodeWithPropertiesKey>>()
        val getValuesCache = ConcurrentHashMap<Node, PowersetLattice.Element<Pair<Node, Boolean>>>()

        coroutineScope {
            callExpression.arguments.forEach { arg ->
                launch(Dispatchers.Default) {
                    if (arg.argumentIndex < functionDeclaration.parameters.size) {
                        // Create a DFG-Edge from the argument to the parameter's memoryValue
                        val p = functionDeclaration.parameters[arg.argumentIndex]
                        // First, check if we already assigned the PMV values in another function
                        var memVals = p.fullMemoryValues
                        // If this is not the case, they are still in the state
                        if (memVals.isEmpty()) {
                            memVals =
                                doubleState.getCachedValues(getValuesCache, p).mapTo(HashSet()) {
                                    it.first
                                }
                            // If they are also not yet in the state, we have to calculate them
                            if (memVals.isEmpty()) {
                                initializeParameters(lattice, mutableListOf(p), doubleState, 2)
                                memVals =
                                    doubleState.getCachedValues(getValuesCache, p).mapTo(
                                        HashSet()
                                    ) {
                                        it.first
                                    }
                            }
                        }
                        memVals
                            .filterIsInstance<ParameterMemoryValue>()
                            .splitInto(maxParts = innerConcurrencyCounter)
                            .forEach { chunk ->
                                launch(Dispatchers.Default) {
                                    chunk.forEach { paramVal ->
                                        innerCalculateIncomingCallingContexts(
                                            doubleState,
                                            generalStateUpdates,
                                            paramVal,
                                            arg,
                                            callingContext,
                                            p,
                                            getLastWritesCache,
                                            getNestedValuesCache,
                                        )
                                    }
                                }
                            }
                    }
                }
            }
        }
        coroutineScope {
            lattice.innerLattice1.lub(
                doubleState.generalState,
                MapLattice.Element(generalStateUpdates),
                allowModify = true,
            )
        }
        return doubleState
    }

    private fun innerCalculateIncomingCallingContexts(
        doubleState: PointsToState.Element,
        generalStateUpdates: ConcurrentHashMap<Node, GeneralStateEntryElement>,
        paramVal: ParameterMemoryValue,
        arg: Expression,
        callingContext: CallingContextIn,
        p: ParameterDeclaration,
        getLastWritesCache: MutableMap<Node, PowersetLattice.Element<NodeWithPropertiesKey>>,
        getNestedValuesCache:
            MutableMap<Triple<Node, Int, Boolean>, PowersetLattice.Element<Pair<Node, Boolean>>>,
    ) {
        updateGeneralStateUpdatesConcurrentHashMap(
            generalStateUpdates,
            paramVal,
            PowersetLattice.Element(),
            PowersetLattice.Element(NodeWithPropertiesKey(arg, equalLinkedHashSetOf())),
            PowersetLattice.Element(
                NodeWithPropertiesKey(arg, equalLinkedHashSetOf(callingContext))
            ),
        )
        // Also draw the edges for the (deref)derefvalues if we have
        // any and are
        // dealing with a pointer parameter (AKA memoryValue is not
        // null)
        p.memoryValueEdges
            .filter {
                (it.granularity as? PartialDataflowGranularity<*>)?.partialTarget == "derefvalue"
            }
            .map { it.start }
            .forEach { derefPMV ->
                val argVals =
                    // In C(++), the reference to an array is a
                    // pointer, leading to the
                    // situation that handing "arg" or "&arg" as
                    // argument is the same
                    // We deal with this by drawing a DFG-Edge from
                    // the arg to the
                    // derefPMV in case of an array pointerType.
                    if (
                        (arg.type as? PointerType)?.pointerOrigin == PointerType.PointerOrigin.ARRAY
                    )
                        PowersetLattice.Element(Pair(arg, true))
                    else doubleState.getCachedNestedValues(getNestedValuesCache, arg, 1, false)
                argVals.forEach { (argVal, _) ->
                    val argDerefVals =
                        if (
                            (arg.type as? PointerType)?.pointerOrigin ==
                                PointerType.PointerOrigin.ARRAY
                        )
                            equalLinkedHashSetOf<Node>(arg)
                        else {
                            doubleState
                                .getCachedNestedValues(
                                    getNestedValuesCache,
                                    argVal,
                                    1,
                                    fetchFields = false,
                                )
                                .mapTo(equalLinkedHashSetOf()) { it.first }
                        }
                    val lastDerefWrites =
                        if (
                            (arg.type as? PointerType)?.pointerOrigin ==
                                PointerType.PointerOrigin.ARRAY
                        )
                            PowersetLattice.Element(
                                NodeWithPropertiesKey(
                                    arg,
                                    equalLinkedHashSetOf(callingContext, false),
                                )
                            )
                        else {
                            doubleState.getCachedLastWrites(getLastWritesCache, argVal).mapTo(
                                PowersetLattice.Element()
                            ) {
                                NodeWithPropertiesKey(
                                    it.node,
                                    equalLinkedHashSetOf(callingContext, true in it.properties),
                                )
                            }
                        }
                    updateGeneralStateUpdatesConcurrentHashMap(
                        generalStateUpdates,
                        derefPMV,
                        PowersetLattice.Element(),
                        PowersetLattice.Element(
                            argDerefVals.mapTo(PowersetLattice.Element()) {
                                NodeWithPropertiesKey(it, equalLinkedHashSetOf())
                            }
                        ),
                        lastDerefWrites,
                    )
                    // The same for the derefderef values
                    p.memoryValueEdges
                        .filter {
                            (it.granularity as? PartialDataflowGranularity<*>)?.partialTarget ==
                                "derefderefvalue"
                        }
                        .map { it.start }
                        .forEach { derefderefPMV ->
                            argDerefVals
                                .flatMap {
                                    doubleState.getCachedNestedValues(
                                        getNestedValuesCache,
                                        it,
                                        1,
                                        fetchFields = false,
                                    )
                                }
                                .forEach { (derefderefValue, _) ->
                                    val lastDerefDerefWrites =
                                        argDerefVals
                                            .flatMapTo(PowersetLattice.Element()) {
                                                doubleState.getCachedLastWrites(
                                                    getLastWritesCache,
                                                    it,
                                                )
                                            }
                                            .mapTo(PowersetLattice.Element()) {
                                                NodeWithPropertiesKey(
                                                    it.node,
                                                    equalLinkedHashSetOf<Any>(callingContext),
                                                )
                                            }
                                    updateGeneralStateUpdatesConcurrentHashMap(
                                        generalStateUpdates,
                                        derefderefPMV,
                                        PowersetLattice.Element(derefPMV),
                                        PowersetLattice.Element(
                                            NodeWithPropertiesKey(
                                                derefderefValue,
                                                equalLinkedHashSetOf(),
                                            )
                                        ),
                                        PowersetLattice.Element(lastDerefDerefWrites),
                                    )
                                }
                        }
                }
            }
    }

    data class MapDstToSrcEntry(
        val srcNode: Node?,
        val lastWrites: MutableSet<NodeWithPropertiesKey>,
        val propertySet: EqualLinkedHashSet<Any>,
        val dst: IdentitySet<Node> = identitySetOf(),
    ) {
        override fun equals(other: Any?): Boolean {
            return other is MapDstToSrcEntry &&
                srcNode === other.srcNode &&
                lastWrites.size == other.lastWrites.size &&
                lastWrites.all { lw -> other.lastWrites.any { it == lw } } &&
                propertySet.size == other.propertySet.size &&
                propertySet.all { p -> other.propertySet.any { it == p } } &&
                dst == other.dst
        }

        override fun hashCode(): Int {
            var result = srcNode?.hashCode() ?: 0
            result = 31 * result + lastWrites.hashCode()
            result = 31 * result + propertySet.hashCode()
            result = 31 * result + dst.hashCode()
            return result
        }
    }

    private suspend fun handleCallExpression(
        lattice: PointsToState,
        currentNode: CallExpression,
        doubleState: PointsToState.Element,
    ): PointsToState.Element {
        var doubleState = doubleState
        val mapDstToSrc = ConcurrentHashMap<Node, IdentitySet<MapDstToSrcEntry>>()

        log.info("Starting handleCallExpression for $currentNode")

        // The toIdentitySet avoids having the same elements multiple times
        val invokes = currentNode.invokes.toIdentitySet()
        coroutineScope {
            invokes.forEach { invoke ->
                val inv = calculateFunctionSummaries(invoke)
                if (inv != null) {
                    doubleState =
                        calculateIncomingCallingContexts(lattice, inv, currentNode, doubleState)
                    log.info("Finished with calculateIncomingCallingContexts")

                    // If we have a FunctionSummary, we push the values of the arguments and
                    // return value
                    // after executing the function call to our doubleState.
                    // We can't go through all levels at once as a change at a lower level may
                    // affect a higher level. So let's do this step by step
                    for (depth in 0..3) {
                        coroutineScope {
                            // We use coroutines in coroutines. So, in order not to launch way too
                            // many of them, we calculate the amount of inner coroutines that we can
                            // reasonably launch
                            val innerConcurrencyCounter =
                                calculateInnerConcurrencyCounter(inv.functionSummary.size)
                            for ((param, fsEntries) in inv.functionSummary) {
                                launch(Dispatchers.Default) {
                                    val argument =
                                        when (param) {
                                            is ParameterDeclaration -> {
                                                // Dereference the parameter
                                                if (
                                                    param.argumentIndex < currentNode.arguments.size
                                                ) {
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
                                            .filter { it.destValueDepth == depth }
                                            .splitInto(maxParts = innerConcurrencyCounter)
                                            .map { chunk ->
                                                launch(Dispatchers.Default) {
                                                    for ((
                                                        dstValueDepth,
                                                        srcNode,
                                                        srcValueDepth,
                                                        subAccessName,
                                                        lastWrites,
                                                        properties,
                                                    ) in chunk) {
                                                        writeEntry(
                                                            doubleState,
                                                            mapDstToSrc,
                                                            dstValueDepth,
                                                            subAccessName,
                                                            argument,
                                                            properties,
                                                            lastWrites,
                                                            currentNode,
                                                            inv,
                                                            srcNode,
                                                            srcValueDepth,
                                                            param,
                                                        )
                                                    }
                                                }
                                            }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        log.info("Collected all entries for mapDstToSrc. size: ${mapDstToSrc.size}")

        val callingContextOut = CallingContextOut(mutableListOf(currentNode))
        mapDstToSrc.forEach { (dstAddr, values) ->
            doubleState =
                writeMapEntriesToState(lattice, doubleState, dstAddr, values, callingContextOut)
        }

        log.info("Finished in handleCallExpression for $currentNode")
        return doubleState
    }

    suspend fun writeEntry(
        doubleState: PointsToState.Element,
        mapDstToSrc: ConcurrentHashMap<Node, IdentitySet<MapDstToSrcEntry>>,
        dstValueDepth: Int,
        subAccessName: String,
        argument: Expression,
        properties: EqualLinkedHashSet<Any>,
        lastWrites: MutableSet<NodeWithPropertiesKey>,
        currentNode: CallExpression,
        inv: FunctionDeclaration,
        srcNode: Node?,
        srcValueDepth: Int,
        param: Node,
    ): MutableMap<Node, IdentitySet<MapDstToSrcEntry>> {
        val shortFS = properties.any { it == true }
        val (destinationAddresses, destinations) =
            doubleState.mutex.withLock {
                calculateCallExpressionDestinations(
                    doubleState,
                    mapDstToSrc,
                    dstValueDepth,
                    subAccessName,
                    argument,
                    properties,
                )
            }
        // Collect the properties for the
        // DeclarationStateEntry
        val propertySet = equalLinkedHashSetOf<Any>()
        propertySet.addAll(properties)
        if (subAccessName.isNotEmpty()) {
            propertySet.add(
                PartialDataflowGranularity(FieldDeclaration().apply { name = Name(subAccessName) })
            )
        }

        // Especially for shortFS, we need to update the
        // prevDFGs with
        // information we didn't have when creating the
        // functionSummary.
        // calculatePrev does this for us
        val prev = calculatePrevDFGs(lastWrites, shortFS, currentNode, inv)
        // mapDstToSrc might be written, doubleState
        // will be read, so we need a mutex
        return doubleState.mutex.withLock {
            addEntryToMap(
                doubleState,
                mapDstToSrc,
                destinationAddresses,
                destinations,
                // To ensure that we have a
                // unique Node, we take
                // the allExpression if the FS
                // said the srcNode is
                // the FunctionDeclaration
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

    private fun calculatePrevDFGs(
        lastWrites: MutableSet<NodeWithPropertiesKey>,
        shortFS: Boolean,
        currentNode: CallExpression,
        invoke: FunctionDeclaration,
    ): MutableSet<NodeWithPropertiesKey> {
        val ret: MutableSet<NodeWithPropertiesKey> = ConcurrentHashMap.newKeySet()
        // If we have nothing, the last write is probably the functionDeclaration
        if (lastWrites.isEmpty()) ret.add(NodeWithPropertiesKey(invoke, equalLinkedHashSetOf()))
        lastWrites.forEach { (lw, properties) ->
            val filteredProperties = equalLinkedHashSetOf<Any>().apply { addAll(properties) }
            if (shortFS) {
                when (lw) {
                    is FunctionDeclaration ->
                        ret.add(NodeWithPropertiesKey(currentNode, filteredProperties))
                    is ParameterDeclaration -> {
                        if (lw.argumentIndex < currentNode.arguments.size)
                            ret.add(
                                NodeWithPropertiesKey(
                                    currentNode.arguments[lw.argumentIndex],
                                    filteredProperties,
                                )
                            )
                        else ret.add(NodeWithPropertiesKey(lw, filteredProperties))
                    }
                    else -> ret.add(NodeWithPropertiesKey(lw, filteredProperties))
                }
            } else ret.add(NodeWithPropertiesKey(lw, filteredProperties))
        }
        return ret
    }

    private suspend fun calculateFunctionSummaries(
        invoke: FunctionDeclaration
    ): FunctionDeclaration? {
        if (invoke.functionSummary.isEmpty()) {
            if (invoke.hasBody()) {
                log.debug(
                    "functionSummaryAnalysisChain: {}",
                    functionSummaryAnalysisChain.map { it.name.localName },
                )
                if (invoke !in functionSummaryAnalysisChain) {
                    //                    val summaryCopy = functionSummaryAnalysisChain.toSet()
                    log.info("Calling acceptInteral(${invoke.name.localName}")
                    acceptInternal(invoke)
                    log.info("Finished with acceptInteral(${invoke.name.localName})")
                    //                    functionSummaryAnalysisChain.addAll(summaryCopy)
                } else {
                    log.error(
                        "Cannot calculate functionSummary for ${invoke.name.localName} as it's recursively called. callChain: ${functionSummaryAnalysisChain.map{it.name.localName}}"
                    )
                    invoke.functionSummary[newLiteral("dummy")] =
                        ConcurrentHashMap.newKeySet<FSEntry>()
                    return null
                }
            } else {
                // Add a dummy function summary so that we don't try this every time
                // In this dummy, all parameters point either to returns if we have any, or the
                // FunctionDeclaration itself
                val newValues = ConcurrentHashMap.newKeySet<FSEntry>()
                invoke.parameters.map { newValues.add(FSEntry(0, it, 1, "")) }
                val entries = mutableSetOf<Node>()
                if (invoke.returns.isNotEmpty()) entries.addAll(invoke.returns)
                else entries.add(invoke)
                entries.forEach { entry -> invoke.functionSummary[entry] = newValues }
            }
        }
        return invoke
    }

    data class NodeWithPropertiesKey(val node: Node, val properties: Set<Any> = emptySet<Any>()) {
        // Since we are dealing with pairs, carefully check if we already have them
        override fun equals(other: Any?): Boolean =
            other is NodeWithPropertiesKey &&
                node == other.node &&
                properties.size == other.properties.size &&
                properties.all { t -> other.properties.any { o -> o == t } }

        override fun hashCode(): Int {
            var h = node.hashCode()
            // The order of the properties doesn't matter
            for (p in properties) h = h + p.hashCode()
            return h
        }
    }

    private suspend fun writeMapEntriesToState(
        lattice: PointsToState,
        doubleState: PointsToState.Element,
        dstAddr: Node,
        values: MutableSet<MapDstToSrcEntry>,
        callingContext: CallingContextOut,
    ): PointsToState.Element = coroutineScope {
        // A triple: the sourceNode, a flag if this is a shortFS, and a flag if this is a
        // partialWrite
        val sources =
            // by first mapping to a Set, we filter out doubles that may have appeared due to
            // concurrent processing of mapDstToSrc
            PowersetLattice.Element(
                values.mapTo(HashSet()) {
                    Triple(
                        it.srcNode,
                        true in it.propertySet,
                        it.propertySet.any { it is PartialDataflowGranularity<*> },
                    )
                }
            )
        /*        val sources =
        values.mapTo(PowersetLattice.Element()) {
            Triple(
                it.srcNode,
                true in it.propertySet,
                it.propertySet.any { it is PartialDataflowGranularity<*> },
            )
        }*/
        val lastWrites: MutableSet<NodeWithPropertiesKey> = ConcurrentHashMap.newKeySet()
        val destinations = identitySetOf<Node>()

        val localResults =
            values.splitInto(minPartSize = 1).map { chunk ->
                async(Dispatchers.Default) {
                    val localDestinations = identitySetOf<Node>()
                    for (value in chunk) {
                        value.lastWrites.forEach { (lw, lwProps) ->
                            // For short FunctionSummaries (AKA one of the lastWrite
                            // properties set to 'true',
                            // we don't add the callingContext
                            val lwPropertySet = EqualLinkedHashSet<Any>()
                            lwPropertySet.addAll(value.propertySet)
                            // If this is not a shortFS edge, we add the new callingcontext
                            // and have to check if
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
                            // Add them to the set of lastWrites if there is no same element
                            // in there yet
                            lastWrites.add(NodeWithPropertiesKey(lw, lwPropertySet))
                        }
                        localDestinations.addAll(value.dst)
                    }
                    localDestinations
                }
            }

        localResults.awaitAll().forEach { destinations.addAll(it) }

        return@coroutineScope doubleState.updateValues(
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
    private suspend fun addEntryToMap(
        doubleState: PointsToState.Element,
        mapDstToSrc: ConcurrentHashMap<Node, IdentitySet<MapDstToSrcEntry>>,
        destinationAddresses: IdentitySet<Node?>,
        destinations: IdentitySet<Node>,
        srcNode: Node?,
        shortFS: Boolean,
        srcValueDepth: Int,
        param: Node,
        propertySet: EqualLinkedHashSet<Any>,
        currentNode: CallExpression,
        lastWrites: MutableSet<NodeWithPropertiesKey>,
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
                            NodeWithPropertiesKey(
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
                            val updatedPropertySet =
                                equalLinkedHashSetOf<Any>().apply {
                                    addAll(propertySet)
                                    add(shortFS)
                                }
                            val currentSet = mapDstToSrc.computeIfAbsent(d) { identitySetOf() }
                            if (
                                currentSet.none {
                                    it.srcNode === value &&
                                        it.lastWrites.parallelEquals(
                                            PowersetLattice.Element(lastWrites.singleOrNull())
                                        ) &&
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
                                val updatedPropertySet =
                                    equalLinkedHashSetOf<Any>().apply {
                                        addAll(propertySet)
                                        add(shortFS)
                                    }
                                val currentSet = mapDstToSrc.computeIfAbsent(d) { identitySetOf() }
                                doubleState.getNestedValues(arg, srcValueDepth).forEach { (value, _)
                                    ->
                                    if (
                                        currentSet.none {
                                            it.srcNode === value &&
                                                it.lastWrites.parallelEquals(
                                                    PowersetLattice.Element(lastWrites)
                                                ) &&
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
                    val updatedPropertySet =
                        equalLinkedHashSetOf<Any>().apply {
                            addAll(propertySet)
                            add(shortFS)
                        }
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
                            val updatedPropertySet =
                                equalLinkedHashSetOf<Any>().apply {
                                    addAll(propertySet)
                                    add(shortFS)
                                }
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
        doubleState: PointsToState.Element,
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
                .filter { it.srcNode != null }
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

    private suspend fun handleUnaryOperator(
        lattice: PointsToState,
        currentNode: UnaryOperator,
        doubleState: PointsToState.Element,
    ): PointsToState.Element {
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
                    NodeWithPropertiesKey(currentNode.input, equalLinkedHashSetOf(false))
                // If we already have exactly that entry, no need to re-write it, otherwise we might
                // confuse the iterateEOG function
                newValueEntry =
                    newDeclState[addr]?.second?.firstOrNull {
                        it.first === newValueEntry.first && it.second == newValueEntry.second
                    } ?: newValueEntry
                newLastWriteEntry =
                    newDeclState[addr]?.third?.firstOrNull {
                        it.node === newLastWriteEntry.node &&
                            it.properties == newLastWriteEntry.properties
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
                PointsToState.Element(doubleState.generalState, MapLattice.Element(newDeclState))
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
                            NodeWithPropertiesKey(it.first, equalLinkedHashSetOf())
                        }
                    ),
                    PowersetLattice.Element(),
                ),
            )

        return doubleState
    }

    private suspend fun handleAssignExpression(
        lattice: PointsToState,
        currentNode: AssignExpression,
        doubleState: PointsToState.Element,
    ): PointsToState.Element {
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
            val lastWrites: MutableSet<NodeWithPropertiesKey> =
                destinations.mapTo(ConcurrentHashMap.newKeySet()) {
                    NodeWithPropertiesKey(it, equalLinkedHashSetOf<Any>(false))
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

    private suspend fun handleExpression(
        lattice: PointsToState,
        currentNode: Expression,
        doubleState: PointsToState.Element,
    ): PointsToState.Element {
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
                            .filter { it.properties.none { it == true } }
                            .forEach {
                                prevDFGs.add(
                                    NodeWithPropertiesKey(
                                        it.node,
                                        equalLinkedHashSetOf(
                                            PointerDataflowGranularity(
                                                PointerAccess.currentDerefValue
                                            ),
                                            // Remove the FullDataGranularity, since here we only
                                            // want to indicate derefValues
                                            *it.properties
                                                .filter { it !is FullDataflowGranularity }
                                                .toTypedArray(),
                                        ),
                                    )
                                )
                            }

                        // Let's see if we can deref once more
                        doubleState
                            .fetchValueFromDeclarationState(
                                node = value,
                                excludeShortFSValues = true,
                            )
                            .map { it.value }
                            .forEach { derefValue ->
                                if (doubleState.hasDeclarationStateEntry(derefValue)) {
                                    doubleState
                                        .getLastWrites(derefValue)
                                        .filter { it.properties.none { it == true } }
                                        .forEach {
                                            prevDFGs.add(
                                                NodeWithPropertiesKey(
                                                    it.node,
                                                    equalLinkedHashSetOf<Any>(
                                                        PointerDataflowGranularity(
                                                            PointerAccess.currentDerefDerefValue
                                                        ),
                                                        // Here again, filter the
                                                        // FullDataflowGranularity since
                                                        // we indicate a
                                                        // currentDerefDerefValue
                                                        *it.properties
                                                            .filter {
                                                                it !is FullDataflowGranularity
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

            doubleState =
                lattice.push(
                    doubleState,
                    currentNode,
                    GeneralStateEntryElement(
                        PowersetLattice.Element(addresses),
                        PowersetLattice.Element(
                            values.mapTo(PowersetLattice.Element()) {
                                NodeWithPropertiesKey(it, equalLinkedHashSetOf())
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

    private suspend fun handleDeclaration(
        lattice: PointsToState,
        currentNode: Node,
        doubleState: PointsToState.Element,
    ): PointsToState.Element {
        var doubleState = doubleState
        /* No need to set the address, this already happens in the constructor */
        val addresses = doubleState.getAddresses(currentNode, currentNode)

        val values = PowersetLattice.Element<NodeWithPropertiesKey>()

        (currentNode as? HasInitializer)?.initializer?.let { initializer ->
            if (initializer is Literal<*>)
                values.add(NodeWithPropertiesKey(initializer, equalLinkedHashSetOf()))
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
                        NodeWithPropertiesKey(it.first, equalLinkedHashSetOf())
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
                            values.mapTo(PowersetLattice.Element()) { Pair(it.node, false) }
                        ),
                        PowersetLattice.Element(
                            NodeWithPropertiesKey(currentNode, equalLinkedHashSetOf<Any>(false))
                        ),
                    ),
                )
        }
        return doubleState
    }

    /** Create ParameterMemoryValues up to depth `depth` */
    private suspend fun initializeParameters(
        lattice: PointsToState,
        parameters: MutableList<ParameterDeclaration>,
        doubleState: PointsToState.Element,
        // Until which depth do we create ParameterMemoryValues
        depth: Int = 2,
    ): PointsToState.Element {
        var doubleState = doubleState
        parameters
            .filter { it.memoryValues.filterIsInstance<ParameterMemoryValue>().isEmpty() }
            .forEach { param ->
                // In the first step, we have a triangle of ParameterDeclaration, the
                // ParameterDeclaration's Memory Address and the ParameterMemoryValue
                // Therefore, the src and the addresses are different. For all other depths,
                // we set
                // both to the ParameterMemoryValue we create in the first step
                var src: Node = param
                var addresses = doubleState.getAddresses(src, src)
                var prevAddresses = identitySetOf<Node>()
                // If we have a Pointer as param, we initialize all levels, otherwise, only
                // the first one
                val paramDepth =
                    if (
                        param.type is PointerType ||
                            // If the type is unknown, we also initialize all levels to be
                            // sure
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

                    // In the first step, we link the ParameterDeclaration to the PMV to be
                    // able to
                    // also access it outside the function
                    if (src is ParameterDeclaration) {
                        // src.memoryValue = pmv
                        doubleState =
                            lattice.push(
                                doubleState,
                                src,
                                GeneralStateEntryElement(
                                    PowersetLattice.Element(addresses),
                                    PowersetLattice.Element(
                                        NodeWithPropertiesKey(pmv, equalLinkedHashSetOf())
                                    ),
                                    PowersetLattice.Element(),
                                ),
                            )
                    } else {
                        // Link the PMVs with each other so that we can find them. This is
                        // especially important outside the respective function where we
                        // don't have
                        // a state
                        addresses.filterIsInstance<ParameterMemoryValue>().forEach {
                            doubleState =
                                lattice.push(
                                    doubleState,
                                    it,
                                    GeneralStateEntryElement(
                                        PowersetLattice.Element(prevAddresses),
                                        PowersetLattice.Element(
                                            NodeWithPropertiesKey(pmv, equalLinkedHashSetOf())
                                        ),
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
                                        NodeWithPropertiesKey(pmv, equalLinkedHashSetOf(pmvName))
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
                            PowersetLattice.Element(
                                NodeWithPropertiesKey(pmv, equalLinkedHashSetOf())
                            ),
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
        //                }
        //        }
        return doubleState
    }
}

/**
 * Update the ConcurrentHashMap. If an entry exists, add the new values, otherwise, create a new
 * entry
 */
private fun PointsToPass.updateGeneralStateUpdatesConcurrentHashMap(
    generalStateUpdates: ConcurrentHashMap<Node, GeneralStateEntryElement>,
    key: Node,
    first: PowersetLattice.Element<Node>,
    second: PowersetLattice.Element<NodeWithPropertiesKey>,
    third: PowersetLattice.Element<NodeWithPropertiesKey>,
) {
    generalStateUpdates.compute(key) { _, existingValue ->
        GeneralStateEntryElement(
            existingValue?.first?.duplicate()?.apply { addAll(first) } ?: first,
            existingValue?.second?.duplicate()?.apply { addAll(second) } ?: second,
            existingValue?.third?.duplicate()?.apply { addAll(third) } ?: third,
        )
    }
}

val PointsToState.Element.generalState: SingleGeneralStateElement
    get() = this.first

val PointsToState.Element.declarationsState: SingleDeclarationStateElement
    get() = this.second

fun PointsToState.Element.getFromDecl(key: Node): DeclarationStateEntryElement? {
    return this.declarationsState[key]
}

suspend fun PointsToState.push(
    currentState: PointsToState.Element,
    newNode: Node,
    newLatticeElement: GeneralStateEntryElement,
): PointsToState.Element {
    // If we already have exactly that entry, no need to re-write it, otherwise we might confuse the
    // iterateEOG function
    val newLatticeCopy = newLatticeElement.duplicate()
    newLatticeElement.third.forEach { existingEntry ->
        if (
            currentState.generalState[newNode]?.third?.any {
                it.node === existingEntry.node && it.properties == existingEntry.properties
            } == true
        ) {
            newLatticeCopy.third.remove(existingEntry)
        }
    }

    coroutineScope {
        this@push.innerLattice1.lub(
            currentState.generalState,
            MapLattice.Element(newNode to newLatticeCopy),
            true,
        )
    }
    return currentState
}

/** Pushes the [newNode] and its [newLatticeElement] to the [declarationsState]. */
suspend fun PointsToState.pushToDeclarationsState(
    currentState: PointsToState.Element,
    newNode: Node,
    newLatticeElement: DeclarationStateEntryElement,
): PointsToState.Element = coroutineScope {
    // If we already have exactly that entry, no need to re-write it, otherwise we might confuse the
    // iterateEOG function
    val newLatticeCopy = newLatticeElement.duplicate()
    val removeFromThird: MutableSet<NodeWithPropertiesKey> = ConcurrentHashMap.newKeySet()

    val toRemoveSecond =
        newLatticeElement.second.splitInto().map { chunk ->
            async(Dispatchers.Default) {
                val local = PowersetLattice.Element<Pair<Node, Boolean>>()
                for (pair in chunk) {
                    if (
                        currentState.declarationsState[newNode]?.second?.any {
                            it.first === pair.first && it.second == pair.second
                        } == true
                    )
                        local.add(pair)
                }
                local
            }
        }
    toRemoveSecond.awaitAll().forEach { subList -> newLatticeCopy.second.removeAll(subList) }

    newLatticeElement.third
        .splitInto()
        .map { chunk ->
            launch(Dispatchers.Default) {
                for (nwpk in chunk) {
                    if (currentState.declarationsState[newNode]?.third?.contains(nwpk) == true) {
                        removeFromThird.add(nwpk)
                    }
                }
            }
        }
        .joinAll()
    newLatticeCopy.third.removeAll(removeFromThird)

    this@pushToDeclarationsState.innerLattice2.lub(
        currentState.declarationsState,
        MapLattice.Element(newNode to newLatticeCopy),
        true,
    )
    return@coroutineScope currentState
}

/** Check if `node` has an entry in the DeclarationState */
fun PointsToState.Element.hasDeclarationStateEntry(
    node: Node,
    excludeShortFSValues: Boolean = true,
): Boolean {

    return if (excludeShortFSValues)
        (this.declarationsState[node]?.second?.any { !it.second } == true)
    else (this.declarationsState[node]?.second?.isNotEmpty() == true)
}

data class FetchElementFromDeclarationStateEntry(
    val value: Node,
    val shortFS: Boolean,
    val subAccessName: String,
    val lastWrites: IdentitySet<NodeWithPropertiesKey>,
)

/** Fetch the address for `node` from the GeneralState */
fun PointsToState.Element.fetchAddressFromGeneralState(node: Node): IdentitySet<Node> {
    return this.generalState[node]?.first ?: PowersetLattice.Element()
}

/** Fetch the value for `node` from the GeneralState */
fun PointsToState.Element.fetchValueFromGeneralState(
    node: Node
): PowersetLattice.Element<NodeWithPropertiesKey> {
    return this.generalState[node]?.second ?: PowersetLattice.Element()
}

/**
 * Fetch the value entry for `node` from the DeclarationState. If there isn't any, create an
 * UnknownMemoryValue
 */
fun PointsToState.Element.fetchValueFromDeclarationState(
    node: Node,
    fetchFields: Boolean = false,
    excludeShortFSValues: Boolean = false,
): IdentitySet<FetchElementFromDeclarationStateEntry> {
    val ret = identitySetOf<FetchElementFromDeclarationStateEntry>()

    // For global nodes, we check the globalDerefs map
    if (isGlobal(node)) {
        val element = globalDerefs[node]
        if (element != null)
            element.map {
                ret.add(
                    FetchElementFromDeclarationStateEntry(
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
                FetchElementFromDeclarationStateEntry(
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
                FetchElementFromDeclarationStateEntry(
                    newEntry,
                    false,
                    "",
                    PowersetLattice.Element(),
                )
            )
        } else
            elements.map {
                ret.add(
                    FetchElementFromDeclarationStateEntry(
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
                                FetchElementFromDeclarationStateEntry(
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

fun PointsToState.Element.getLastWrites(
    node: Node
): PowersetLattice.Element<NodeWithPropertiesKey> {
    if (isGlobal(node)) {
        return when (node) {
            //            is PointerReference -> { TODO()}
            is MemberExpression -> {
                // We overapproximate here: For memberExpressions, we ignore the field and only
                // consider the base
                val (base, _) = resolveMemberExpression(node)
                PowersetLattice.Element(
                    NodeWithPropertiesKey(
                        (base as? Reference)?.refersTo ?: base,
                        equalLinkedHashSetOf(),
                    )
                )
            }
            is Reference ->
                PowersetLattice.Element(
                    NodeWithPropertiesKey(node.refersTo ?: node, equalLinkedHashSetOf())
                )
            else -> PowersetLattice.Element(NodeWithPropertiesKey(node, equalLinkedHashSetOf()))
        }
    }
    return when (node) {
        is PointerReference -> {
            // TODO: Handle other input types (e.g. SubscriptExpression, MemberExpression)
            // For pointerReferences, we take the memoryAddress of the refersTo
            return (node.input as? Reference)?.refersTo?.memoryAddresses?.mapTo(
                PowersetLattice.Element()
            ) {
                NodeWithPropertiesKey(it, equalLinkedHashSetOf())
            } ?: PowersetLattice.Element(NodeWithPropertiesKey(node, equalLinkedHashSetOf()))
        }
        is PointerDereference -> {
            val ret = PowersetLattice.Element<NodeWithPropertiesKey>()
            this.getAddresses(node, node).forEach { addr ->
                val lastWrite = this.declarationsState[addr]?.third
                // Usually, we should have a lastwrite, so we take that
                if (lastWrite?.isNotEmpty() == true)
                    lastWrite.mapTo(PowersetLattice.Element()) {
                        val newProps = equalLinkedHashSetOf<Any>().apply { addAll(it.properties) }
                        ret.add(NodeWithPropertiesKey(it.node, newProps))
                    }
                // However, there might be cases were we don't yet have written to the dereferenced
                // value, in this case we return an UnknownMemoryValue
                else {
                    val newName = Name(getNodeName(addr).localName + ".derefvalue")
                    ret.add(
                        NodeWithPropertiesKey(
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
                    this.declarationsState[it]?.third?.map {
                        NodeWithPropertiesKey(
                            it.node,
                            it.properties.filterTo(EqualLinkedHashSet()) {
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
                    NodeWithPropertiesKey(it.node, equalLinkedHashSetOf())
                }
            else
                return node.memoryValues
                    .filter { it.name.localName == "deref" + node.name.localName }
                    .mapTo(PowersetLattice.Element()) {
                        NodeWithPropertiesKey(it, equalLinkedHashSetOf())
                    }
        }
        else ->
            // For the rest, we read the declarationState to determine when the memoryAddress of the
            // node was last written to
            this.getAddresses(node, node).flatMapTo(PowersetLattice.Element()) {
                this.declarationsState[it]?.third?.map {
                    val newProps = equalLinkedHashSetOf<Any>().apply { addAll(it.properties) }
                    NodeWithPropertiesKey(it.node, newProps)
                } ?: setOf()
            }
    }
}

fun PointsToState.Element.getCachedLastWrites(
    cache: MutableMap<Node, PowersetLattice.Element<PointsToPass.NodeWithPropertiesKey>>,
    node: Node,
): PowersetLattice.Element<PointsToPass.NodeWithPropertiesKey> {
    // We cache some results. Cache is coming from the caller, it knows better when to refresh the
    // cache.
    return cache.getOrPut(node) { this.getLastWrites(node) }
}

fun PointsToState.Element.getValues(
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
            val inputVals = this.fetchValueFromGeneralState(node.input).map { it.node }
            val retVal = PowersetLattice.Element<Pair<Node, Boolean>>()
            /* If the node is not the same as the startNode, we should have already assigned a value, so we fetch it from the generalstate */
            if (node != startNode && node !in ((startNode as? AstNode)?.astChildren ?: listOf()))
                inputVals.forEach {
                    retVal.addAll(
                        fetchValueFromGeneralState(it).mapTo(PowersetLattice.Element()) {
                            Pair(it.node, true in it.properties)
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
            if (node != startNode && node !in ((startNode as? AstNode)?.astChildren ?: emptyList()))
                return fetchValueFromGeneralState(node).mapTo(PowersetLattice.Element()) {
                    Pair(it.node, true in it.properties)
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

fun PointsToState.Element.getCachedValues(
    cache: MutableMap<Node, PowersetLattice.Element<Pair<Node, Boolean>>>,
    node: Node,
): PowersetLattice.Element<Pair<Node, Boolean>> {
    // We cache some results. Cache is coming from the caller, it knows better when to refresh the
    // cache.
    return cache.getOrPut(node) { this.getValues(node, node) }
}

fun PointsToState.Element.getAddresses(node: Node, startNode: Node): IdentitySet<Node> {
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
            // we should already now the value, so no need to fetch it from the generalState
            // This way we avoid loops
            val inputVals =
                if (node.input != startNode && node != startNode)
                    this.fetchValueFromGeneralState(node.input).map { it.node }
                else this.getValues(node.input, startNode).map { it.first }
            inputVals.forEach { value ->
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
                if (startNode != node)
                    fetchValueFromGeneralState(node.base).mapTo(IdentitySet<Node>()) { it.node }
                else this.getValues(node.base, startNode).mapTo(IdentitySet<Node>()) { it.first }
            baseValues.flatMapTo(identitySetOf()) { node ->
                fetchFieldAddresses(
                    identitySetOf(node),
                    Name(localName.localName, getNodeName(node)),
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
fun PointsToState.Element.getNestedValues(
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
            getValues(node, node).filterTo(PowersetLattice.Element()) {
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

fun PointsToState.Element.getCachedNestedValues(
    cache: MutableMap<Triple<Node, Int, Boolean>, PowersetLattice.Element<Pair<Node, Boolean>>>,
    node: Node,
    depth: Int,
    fetchFields: Boolean,
): PowersetLattice.Element<Pair<Node, Boolean>> =
    // We cache some results. Cache is coming from the caller, it knows better when to refresh the
    // cache.
    cache.getOrPut(Triple(node, depth, fetchFields)) {
        this.getNestedValues(
            node,
            depth,
            fetchFields,
            onlyFetchExistingEntries = true,
            excludeShortFSValues = true,
        )
    }

fun PointsToState.Element.fetchFieldAddresses(
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
suspend fun PointsToState.Element.updateValues(
    lattice: PointsToState,
    doubleState: PointsToState.Element,
    sources: PowersetLattice.Element<Triple<Node?, Boolean, Boolean>>,
    destinations: IdentitySet<Node>,
    // Node and short FS yes or no
    destinationAddresses: IdentitySet<Node>,
    lastWrites: MutableSet<NodeWithPropertiesKey>,
): PointsToState.Element = coroutineScope {
    var doubleState = doubleState

    /* Update the declarationState for the addresses */
    destinationAddresses.forEach { destAddr ->
        if (!isGlobal(destAddr)) {
            val currentEntries =
                this@updateValues.declarationsState[destAddr]?.first?.toIdentitySet()
                    ?: identitySetOf(destAddr)

            // If we want to update the State with exactly the same elements as are already in the
            // state, we do nothing in order not to confuse the iterateEOG function
            val newSources: PowersetLattice.Element<Pair<Node, Boolean>> =
                sources
                    .mapTo(PowersetLattice.Element()) { triple ->
                        val existingPair =
                            this@updateValues.declarationsState[destAddr]?.second?.firstOrNull {
                                it.first === triple.first && it.second == triple.second
                            }
                        existingPair ?: Pair(triple.first, triple.second)
                    }
                    .filterTo(PowersetLattice.Element()) { it.first != null }
                    .mapTo(PowersetLattice.Element()) { Pair(it.first!!, it.second) }

            // Check if we have any full writes
            val fullSourcesExist = sources.any { !it.third }

            // TODO: Do we also need to fetch some properties here?
            // If we already have exactly this value in the state for the prevDFGs, we take that in
            // order not to confuse the iterateEOG function
            val prevDFG: MutableSet<NodeWithPropertiesKey> = ConcurrentHashMap.newKeySet()
            lastWrites
                .splitInto()
                .map { chunk ->
                    launch(Dispatchers.Default) {
                        for (lw in chunk) {
                            val existingEntries =
                                doubleState.declarationsState[destAddr]?.third?.filter { entry ->
                                    entry == lw
                                }
                            if (existingEntries?.isNotEmpty() == true)
                                prevDFG.addAll(existingEntries)
                            else prevDFG.add(lw)
                        }
                    }
                }
                .joinAll()

            // If we have any full writes, we eliminate the previous state
            //            doubleState.mutex.withLock {
            if (fullSourcesExist) {
                val newDeclState = this@updateValues.declarationsState.duplicate()
                newDeclState[destAddr] =
                    DeclarationStateEntryElement(
                        PowersetLattice.Element(currentEntries),
                        PowersetLattice.Element(newSources),
                        PowersetLattice.Element(prevDFG),
                    )
                doubleState = PointsToState.Element(doubleState.generalState, newDeclState)
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
            // Except for callExpressions w/o invokes body for which we have to do this to create
            // the short FS paths
            val newLastWrites: MutableSet<NodeWithPropertiesKey> = ConcurrentHashMap.newKeySet()
            newLastWrites.addAll(lastWrites)

            newLastWrites
                .splitInto()
                .map { chunk ->
                    launch(Dispatchers.Default) {
                        for (lw in chunk) {
                            if (
                                destinations.none {
                                    it is CallExpression && it.invokes.singleOrNull()?.body == null
                                } &&
                                    (sources.any { src ->
                                        src.first === lw.node && src.second in lw.properties
                                    } || lw.node in destinations)
                            ) {
                                newLastWrites.remove(lw)
                            }
                        }
                    }
                }
                .joinAll()

            val newGenState = this@updateValues.generalState.duplicate()
            destinations
                .splitInto()
                .map { chunk ->
                    async(Dispatchers.Default) {
                        for (d in chunk) {
                            newGenState[d] =
                                GeneralStateEntryElement(
                                    PowersetLattice.Element(destinationAddresses),
                                    PowersetLattice.Element(
                                        sources
                                            .filter { it.first != null }
                                            .mapTo(PowersetLattice.Element()) {
                                                NodeWithPropertiesKey(
                                                    it.first!!,
                                                    equalLinkedHashSetOf<Any>().apply {
                                                        add(it.second)
                                                    },
                                                )
                                            }
                                    ),
                                    PowersetLattice.Element(newLastWrites),
                                )
                        }
                    }
                }
                .awaitAll()
            doubleState = PointsToState.Element(newGenState, doubleState.declarationsState)
        } else {
            // For globals, we draw a DFG Edge from the source to the destination
            destinations
                .splitInto()
                .map { chunk ->
                    async(Dispatchers.Default) {
                        for (d in chunk) {
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
                                .map {
                                    entry.third.add(
                                        NodeWithPropertiesKey(
                                            it.first!!,
                                            equalLinkedHashSetOf<Any>().apply { add(it.second) },
                                        )
                                    )
                                }
                        }
                    }
                }
                .awaitAll()
        }
    }

    return@coroutineScope doubleState
}
