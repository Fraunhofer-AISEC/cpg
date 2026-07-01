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
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.declarations.Function.FSEntry
import de.fraunhofer.aisec.cpg.graph.edges.flows.*
import de.fraunhofer.aisec.cpg.graph.expressions.*
import de.fraunhofer.aisec.cpg.graph.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.expressions.Return
import de.fraunhofer.aisec.cpg.graph.expressions.UnknownMemoryValue
import de.fraunhofer.aisec.cpg.graph.types.NumericType
import de.fraunhofer.aisec.cpg.graph.types.PointerType
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.helpers.ConcurrentIdentitySet
import de.fraunhofer.aisec.cpg.helpers.IdentitySet
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.helpers.Util.matchArgumentsToCallParameters
import de.fraunhofer.aisec.cpg.helpers.concurrentIdentitySetOf
import de.fraunhofer.aisec.cpg.helpers.functional.*
import de.fraunhofer.aisec.cpg.helpers.functional.TripleLattice
import de.fraunhofer.aisec.cpg.helpers.functional.TupleLattice.Element
import de.fraunhofer.aisec.cpg.helpers.identitySetOf
import de.fraunhofer.aisec.cpg.helpers.mapFiltered
import de.fraunhofer.aisec.cpg.helpers.mapFilteredTo
import de.fraunhofer.aisec.cpg.helpers.toIdentitySet
import de.fraunhofer.aisec.cpg.passes.PointsToPass.NodeWithPropertiesKey
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.Pair
import kotlin.collections.MutableSet
import kotlin.collections.contains
import kotlin.collections.filter
import kotlin.collections.map
import kotlin.let
import kotlin.text.contains
import kotlin.text.ifEmpty
import kotlin.time.DurationUnit
import kotlin.time.TimeSource
import kotlinx.coroutines.*

val nodesCreatingUnknownValues = ConcurrentHashMap<Pair<Node, Name>, MemoryAddress>()
var totalFunctionCount = 0
var analyzedFunctionCount = 0
private const val MAX_FIELD_ACCESS_PATH_DEPTH = 6
private const val FIELD_ACCESS_SUMMARY_SEGMENT = "<summary>"

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

typealias SingleGeneralStateElement = ConcurrentMapLattice.Element<Node, GeneralStateEntryElement>

typealias SingleDeclarationStateElement =
    ConcurrentMapLattice.Element<Node, DeclarationStateEntryElement>

typealias SingleGeneralState = ConcurrentMapLattice<Node, GeneralStateEntryElement>

typealias SingleDeclarationState = ConcurrentMapLattice<Node, DeclarationStateEntryElement>

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

        override fun compare(other: Lattice.Element): Order {
            if (this === other) return Order.EQUAL

            if (other !is Element)
                throw IllegalArgumentException(
                    "$other should be of type Element but is of type ${other.javaClass}"
                )

            // There are some occasions were we need to compare both the generalState and the
            // DeclarationState (instead of only the DeclarationState), so let's do that
            val firstResult = this@Element.first.compare(other.first)
            val secondResult = this@Element.second.compare(other.second)
            return compareMultiple(firstResult, secondResult)
        }

        suspend fun parallelCompare(other: Lattice.Element): Order {
            if (this === other) return Order.EQUAL

            if (other !is Element)
                throw IllegalArgumentException(
                    "$other should be of type Element but is of type ${other.javaClass}"
                )

            if (
                this.declarationsState.size < MIN_CHUNK_SIZE ||
                    this.generalState.size < MIN_CHUNK_SIZE
            ) {
                return compare(other)
            }

            // There are some occasions were we need to compare both the generalState and the
            // DeclarationState (instead of only the DeclarationState), so let's do that
            val firstResult = this@Element.first.parallelCompare(other.first)
            val secondResult = this@Element.second.parallelCompare(other.second)
            return compareMultiple(firstResult, secondResult)
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
fun getNodeName(node: Node?): Name {
    if (node == null) return Name("")
    return when (node) {
        is Literal<*> -> Name(node.value.toString())
        is UnknownMemoryValue -> Name(node.name.localName, Name("UnknownMemoryValue"))
        is Field -> Name(node.name.localName)
        is BinaryOperator ->
            Name(
                getNodeName(node.lhs).localName +
                    " " +
                    node.operatorCode +
                    " " +
                    getNodeName(node.rhs).localName
            )
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
fun clearFSDummies(functionSummary: ConcurrentIdentityHashMap<Node, MutableSet<FSEntry>>) {
    // Do not clear the dummies for which we don't have bodies, only the "obvious" dummies we
    // created for recursive functions
    functionSummary
        .filter { (it.key as? Literal<*>)?.value == "dummy" }
        .keys
        .forEach { functionSummary.remove(it) }
}

private val CallToMemAddrMap =
    ConcurrentIdentityHashMap<Call, ConcurrentIdentityHashMap<Name, MemoryAddress>>()

/**
 * Resolve a MemberAccess as long as it's base no longer is a MemberAccess itself. Returns the base
 * a Name that identifies the access
 */
fun resolveMemberAccess(node: MemberAccess): Pair<Node, Name> {
    // As long as the base in itself is a MemberAccess, resolve that one
    var base: Node = node
    var newLocalname = ""
    while (base is MemberAccess) {
        val b = base.name.split("::", ".")
        val tmp = if (b.size > 1) b.last() else ""
        newLocalname = if (newLocalname.isEmpty()) tmp else "$tmp.$newLocalname"
        base = base.base
    }

    return Pair(base, Name(newLocalname, base.name))
}

private fun Name.pathDepth(): Int {
    var depth = 1
    var current = this.parent
    while (current != null) {
        depth++
        current = current.parent
    }
    return depth
}

private fun normalizeFieldAccessPath(name: Name): Name {
    if (name.pathDepth() <= MAX_FIELD_ACCESS_PATH_DEPTH) {
        return name
    }

    val segments = mutableListOf<Name>()
    name.splitTo(segments)
    val rootToLeaf = segments.asReversed()
    val keep = (MAX_FIELD_ACCESS_PATH_DEPTH - 1).coerceAtLeast(1)

    var normalized: Name? = null
    for (i in 0 until minOf(keep, rootToLeaf.size)) {
        val segment = rootToLeaf[i]
        normalized = Name(segment.localName, normalized, segment.delimiter)
    }

    val delimiter = rootToLeaf.lastOrNull()?.delimiter ?: "."
    return Name(FIELD_ACCESS_SUMMARY_SEGMENT, normalized, delimiter)
}

fun removePossibleCasts(node: Expression): Expression {
    var ret = node
    while (ret is Cast) ret = ret.expression
    return ret
}

fun isGlobal(node: Node): Boolean {
    return when (node) {
        is Variable -> {
            node.isGlobal || node.isNonLocal
        }
        is MemberAccess -> isGlobal(node.base)
        is PointerDereference,
        is PointerReference -> {
            val input =
                removePossibleCasts(
                    (node as? PointerDereference)?.input ?: (node as PointerReference).input
                )
            isGlobal(input)
        }
        is Reference -> {
            node.refersTo is Function ||
                (node.refersTo as? Variable)?.isGlobal == true ||
                (node.refersTo as? Variable)?.isNonLocal == true
        }
        is MemoryAddress -> node.isGlobal
        is Function -> true
        else -> false
    }
}

/* Recursively collect the bases from MemberAccesses and SubscriptExpressions */
fun collectBasesAndOffsets(node: Node): List<Pair<Node, Any?>> {
    val ret = mutableListOf<Pair<Node, Any?>>()

    var n: Node? = node
    while (n != null) {
        when (n) {
            is MemberAccess -> {
                ret.add(n.base to (n.refersTo ?: n.name.localName))
                n = n.base
            }
            is Subscription -> {
                ret.add(n.arrayExpression to n.subscriptExpression)
                n = n.arrayExpression
            }
            is Cast -> {
                n = n.expression
            }
            else -> break
        }
    }
    return ret
}

// We also need a place to store the derefs of global variables. The Boolean indicates if this is a
// value stored for a short function Summary
var globalDerefs = ConcurrentIdentityHashMap<Node, PowersetLattice.Element<Pair<Node, Boolean>>>()

@DependsOn(SymbolResolver::class)
@DependsOn(EvaluationOrderGraphPass::class)
@DependsOn(DFGPass::class)
open class PointsToPass(ctx: TranslationContext) : EOGStarterPass(ctx, orderDependencies = true) {
    class Configuration(
        /**
         * This specifies the maximum complexity (as calculated per
         * [Statement.cyclomaticComplexity]) a [Function] must have in order to be considered.
         */
        var maxComplexity: Int? = null,

        /** This specifies the address length (usually 64bit) */
        var addressLength: Int = 64,

        /**
         * The timeout after which we stop analyzing a function. Default one hour AKA 3,600,000ms
         */
        var timeout: Long = 3600000,

        /** This specifies if we are running after DFG edges to create the detailed shortFS * */
        var detailedShortFS: Boolean = true,

        /**
         * specifies if we draw the current(deref)derefvalue-DFG Edges. Not sure if we want/need
         * them
         */
        var drawCurrentDerefDFG: Boolean = true,
    ) : PassConfiguration()

    // For recursive creation of FunctionSummaries, we have to make sure that we don't run in
    // circles. Therefore, we store the chain of Functions we currently analyze
    private val functionSummaryAnalysisChain = mutableListOf<Function>()

    override fun cleanup() {
        // Nothing to do
    }

    override fun accept(node: Node) {
        functionSummaryAnalysisChain.clear()
        if (node !is EOGStarterHolder || node.eogStarters.isEmpty()) {
            return
        }

        return runBlocking {
            val starters =
                if (node is TranslationUnit)
                // We already analyzed the functions when we got them handed directly as node, so
                // skip them now
                node.eogStarters.filter { starter ->
                        starter !is Function && starter.prevEOG.isEmpty()
                    }
                else
                    node.eogStarters
                        // To avoid analyzing starters multiple times, we only take the ones w/o
                        // prevEOG
                        .filter { starter -> starter.prevEOG.isEmpty() }

            starters.forEach { starter -> acceptInternal(starter) }
        }
    }

    suspend fun acceptInternal(node: Node) {
        var analysisTimeout = false

        if (node is Function) {
            // If we haven't done so yet, set the total number of functions
            if (totalFunctionCount == 0)
                totalFunctionCount =
                    node.firstParentOrNull<TranslationResult>()?.functions?.size ?: 0

            analyzedFunctionCount++

            // If the node has a body and a function summary, we have visited it before and can
            // return here.
            if (
                (node.functionSummary.isNotEmpty() && node.body != null) &&
                    node.functionSummary.keys.any {
                        it in node.parameters ||
                            it in node.returns ||
                            // We already analyzed the function, but it doesn't affect any
                            // parameters or return values
                            (it as? Literal<*>)?.value == "dummy"
                    }
            ) {
                if (log.isTraceEnabled) {
                    log.trace(
                        "Skipping function ${node.name} because we already have a function Summary. (Function $analyzedFunctionCount / $totalFunctionCount)"
                    )
                }
                return
            }

            functionSummaryAnalysisChain.add(node)
            // Calculate the complexity of the function and see, if it exceeds our threshold
            val max = passConfig<Configuration>()?.maxComplexity
            val c = node.body?.cyclomaticComplexity() ?: 0
            if (max != null && c > max) {
                //                if (log.isTraceEnabled) {
                log.info(
                    "Ignoring function ${node.name} because its complexity (${
                                NumberFormat.getNumberInstance(Locale.US).format(c)
                            }) is greater than the configured maximum (${max})"
                )
                //                }
                // Add an empty function Summary so that we don't try again
                node.functionSummary.computeIfAbsent(Return()) {
                    ConcurrentHashMap.newKeySet<FSEntry>()
                }
                return
            }

            //            if (log.isTraceEnabled) {
            log.info(
                "Analyzing function ${node.name}. Complexity: ${
                            NumberFormat.getNumberInstance(Locale.US).format(c)
                        }. (Function $analyzedFunctionCount / $totalFunctionCount)"
            )
            //            }
        } else {
            if (log.isTraceEnabled) {
                log.trace("Analyzing EOGStarterHolder ${node.name}. Complexity unknown")
            }
        }

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
        val addresses = startState.getAddresses(node, node)
        startState =
            lattice.pushToDeclarationsState(
                startState,
                node,
                DeclarationStateEntryElement(
                    PowersetLattice.Element(addresses),
                    PowersetLattice.Element(),
                    PowersetLattice.Element(),
                ),
            )

        startState = transferInternal(lattice, node, startState)

        val finalState =
            if (node is Function && node.body == null) {
                handleEmptyFunction(lattice, startState, node)
            } else {
                var (result, timeout) =
                    lattice.iterateEOG(
                        node.nextEOGEdges,
                        startState,
                        ::transfer,
                        timeout = passConfig<Configuration>()?.timeout,
                    )
                // If we had a timeout, treat it as an empty Function but still
                // include the results we got
                if (timeout && node is Function) {
                    analysisTimeout = true
                    result = handleEmptyFunction(lattice, result as PointsToState.Element, node)
                }
                result as PointsToState.Element
            }

        for ((key, value) in finalState.generalState) {
            // The generalState values have 3 items: The address, the value, and the
            // prevDFG-Edges with a set of properties
            // Let's start with fetching the addresses
            if (key is HasMemoryAddress) {
                key.memoryAddresses += value.first.filterIsInstance<MemoryAddress>()
            }

            // Then the memoryValues
            if (key is HasMemoryValue && value.second.isNotEmpty()) {
                value.second.forEach { (v, properties) ->
                    // We already added the parameters value in initializeParameters, so we skip
                    // that now
                    if (v !is Parameter) {
                        var granularity = default()
                        var shortFS = false
                        properties.forEach { p ->
                            when (p) {
                                // String indicated a partial target
                                is String -> granularity = PartialDataflowGranularity(p)
                                // Boolean says if this is a shortFS or not
                                is Boolean -> shortFS = p
                                else -> TODO()
                            }
                        }
                        key.memoryValueEdges += Dataflow(v, key, granularity, shortFS)
                    }
                }
            }

            // And now the prevDFGs. These are pairs, where the second item is a with a set of
            // properties for the edge
            value.third.forEach { (prev, properties) ->
                var context: CallingContext? = null
                var granularity = default()
                var functionSummary = false
                var derefDepth: PointerAccess? = null

                // the properties can contain a lot of things. A granularity, a
                // callingcontext, or a boolean indicating if this is a functionSummary edge or
                // not
                properties.forEach { property ->
                    when (property) {
                        is PointerDataflowGranularity -> derefDepth = property.pointerTarget
                        is Granularity -> granularity = property
                        is CallingContext -> context = property
                        is Boolean -> functionSummary = property
                    }
                }

                if (context == null) // TODO: add functionSummary flag for contextSensitive DFs
                 key.prevDFGEdges += Dataflow(prev, key, granularity, functionSummary, derefDepth)
                else
                    key.prevDFGEdges.addContextSensitive(
                        prev,
                        granularity,
                        context,
                        functionSummary,
                        derefDepth,
                    )
            }
        }

        if (log.isTraceEnabled) {
            log.trace("Finished drawing DFG Edges")
        }

        if (node is Function) {
            /* Store function summary for this Function. */
            if (node.body != null && !analysisTimeout) storeFunctionSummary(node, finalState)
            if (functionSummaryAnalysisChain.last() == node)
                functionSummaryAnalysisChain.remove(node)
            else
                log.error(
                    "finished analyzing $node, which is not at the end of the functionSummaryAnalysis chain, which is surprising"
                )
        }
        if (log.isTraceEnabled) {
            log.trace("Finished with acceptInternal for ${node.name.localName}")
        }
    }

    /**
     * This function draws the basic DFG-Edges based on the Function, such as edges between
     * Parameter/ParameterMemoryValues
     */
    private suspend fun handleEmptyFunction(
        lattice: PointsToState,
        startState: PointsToState.Element,
        function: Function,
    ): PointsToState.Element {
        var doubleState = startState

        if (function.functionSummary.isEmpty()) {
            // Add a dummy function summary so that we don't try this every time
            // In this dummy, all parameters point to the return
            // TODO: Also add possible dereference values to the input?
            val prevDFGs = PowersetLattice.Element<NodeWithPropertiesKey>()
            val newEntries =
                ConcurrentHashMap.newKeySet<FSEntry>().apply {
                    add(FSEntry(0, function, 1, "", isDummy = true))
                }
            function.parameters.forEach { param ->
                // The short FS
                newEntries.add(
                    FSEntry(
                        0,
                        null,
                        1,
                        "",
                        mutableSetOf(
                            NodeWithPropertiesKey(param, equalLinkedHashSetOf(param.argumentIndex))
                        ),
                        equalLinkedHashSetOf(true),
                        true,
                    )
                )
                // Since we can't determine the DFs, we draw a DFG-Edge from every parameter
                prevDFGs.add(NodeWithPropertiesKey(param, equalLinkedHashSetOf()))
            }
            // For Methods, we also add an edge to the receiver
            if (function is Method)
                function.receiver?.let {
                    prevDFGs.add(NodeWithPropertiesKey(it, equalLinkedHashSetOf()))
                }
            val rets = identitySetOf<Node>()
            if (function.returns.isNotEmpty()) rets.addAll(function.returns) else rets.add(function)
            rets.forEach { ret -> function.functionSummary.put(ret, newEntries) }
            // draw a DFG-Edge from all parameters to the Function
            doubleState =
                lattice.push(
                    doubleState,
                    function,
                    GeneralStateEntryElement(
                        PowersetLattice.Element(),
                        PowersetLattice.Element(),
                        PowersetLattice.Element(prevDFGs),
                    ),
                )
            return doubleState
        }

        for ((param, fsEntries) in function.functionSummary) {
            fsEntries.forEach { entry ->
                if (param is Parameter) {
                    // In case this is a Parameter for which we didn't create deref-PMVs for some
                    // reason (for example an unexpected type) we do create them now
                    if (
                        entry.destValueDepth > 1 &&
                            param.memoryValues.none {
                                (it as? ParameterMemoryValue)?.name?.localName ==
                                    "deref".repeat(entry.destValueDepth - 1) + "value"
                            } &&
                            doubleState.getValues(param, param).none {
                                (it.first as? ParameterMemoryValue)?.let { pmv ->
                                    doubleState.hasDeclarationStateValueEntry(pmv)
                                } ?: false
                            }
                    ) {
                        doubleState =
                            initializeParameter(
                                lattice,
                                function,
                                param,
                                doubleState,
                                forceDerefPMVCreation = true,
                            )
                    }
                    val dst =
                        doubleState
                            .getNestedValues(
                                param,
                                entry.destValueDepth,
                                fetchFields = false,
                                onlyFetchExistingEntries = true,
                                excludeShortFSValues = true,
                            )
                            .map { it.first }
                            .singleOrNull()
                    val src =
                        when (entry.srcNode) {
                            is Parameter -> {
                                doubleState
                                    .getNestedValues(
                                        entry.srcNode,
                                        entry.srcValueDepth,
                                        fetchFields = false,
                                        onlyFetchExistingEntries = true,
                                        excludeShortFSValues = true,
                                    )
                                    .map { it.first }
                                    .singleOrNull()
                            }
                            // Probably a custom defined UnknownMemoryValue from the hardcoded
                            // FunctionSummaries
                            is UnknownMemoryValue -> entry.srcNode
                            else -> null
                        }
                    if (src != null && dst != null) {
                        // In cases where the lastWrites refers to a ParameterMemoryValue, we
                        // couldn't set the lastWrites when creating the functionSummary and set a
                        // Parameter as placeholder (but we know it's not the Parameter itself when
                        // the destValueDepth is != 1, meaning it has to refer to something deferred
                        // Now let's replace that
                        if (entry.destValueDepth != 1) {
                            entry.lastWrites.forEach { lastWrite ->
                                function.functionSummary[param]
                                    ?.singleOrNull { it == entry }
                                    ?.lastWrites
                                    ?.remove(lastWrite)
                            }
                            function.functionSummary[param]
                                ?.singleOrNull { it == entry }
                                ?.lastWrites
                                ?.add(NodeWithPropertiesKey(dst, equalLinkedHashSetOf()))
                        }
                        val propertySet = equalLinkedHashSetOf<Any>()
                        if (entry.subAccessName != "")
                            propertySet.add(Field().apply { name = Name(entry.subAccessName) })
                        val shortFsEntry = entry.properties.singleOrNull { it is Boolean }
                        if (shortFsEntry != null) propertySet.add(shortFsEntry)
                        doubleState =
                            lattice.push(
                                doubleState,
                                dst,
                                GeneralStateEntryElement(
                                    PowersetLattice.Element(),
                                    PowersetLattice.Element(),
                                    PowersetLattice.Element(NodeWithPropertiesKey(src, propertySet)),
                                ),
                            )
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

    private fun addParameterInfoToFS(
        node: Function,
        param: Parameter,
        dstValueDepth: Int,
        value: Node,
        shortFS: Boolean,
        subAccessName: String,
        lastWrites: ConcurrentIdentitySet<NodeWithPropertiesKey>,
    ) {
        // Extract the value depth from the value's localName
        val srcValueDepth = stringToDepth(value.name.localName)
        // Store the information in the functionSummary
        val existingEntry =
            node.functionSummary.computeIfAbsent(param) { ConcurrentHashMap.newKeySet() }
        val filteredLastWrites =
            lastWrites
                // for shortFS,only use these, and for !shortFS, only those
                .filterTo(PowersetLattice.Element()) { shortFS in it.properties }
        // If the value is a newly created MemoryAddress, we only set the name so that we know later
        // that we have to create a new MemoryAddress for each Call
        val addressName = (value as? MemoryAddress)?.name?.localName
        val v =
            if (addressName?.startsWith("NewMemoryAddress") == true) Name(addressName, node.name)
            else value
        existingEntry.add(
            FSEntry(
                dstValueDepth,
                v,
                srcValueDepth,
                subAccessName,
                filteredLastWrites,
                equalLinkedHashSetOf(shortFS),
            )
        )
        // Additionally, we store this as a shortFunctionSummary
        // where the function writes to the parameter
        val shortFSEntry =
            FSEntry(
                dstValueDepth,
                node,
                0,
                subAccessName,
                PowersetLattice.Element(NodeWithPropertiesKey(node, equalLinkedHashSetOf())),
                equalLinkedHashSetOf(true),
            )
        // Add the new entry if it doesn't exist yet
        synchronized(existingEntry) {
            // TODO: Do we need the synchronized? Can we be more efficient in finding matching
            // entries?
            if (existingEntry.none { it == shortFSEntry }) existingEntry.add(shortFSEntry)
        }
        val propertySet = identitySetOf<Any>(true)
        if (subAccessName != "") propertySet.add(Field().apply { name = Name(subAccessName) })

        // Create the detailed shortFS. Like, which parameter
        // influences what.
        // This may take a lot of time, so this is optional
        if ((passConfig<Configuration>()?.detailedShortFS ?: true)) {
            if (!shortFS) {
                // Check if the value is influenced by a
                // Parameter and
                // if so, add this information to the
                // functionSummary
                // TODO: Use memory value edges instead of DFG because these are shortcuts.
                val paths =
                    value.followDFGEdgesUntilHit(
                        collectFailedPaths = false,
                        findAllPossiblePaths = false,
                        direction = Backward(GraphToFollow.DFG),
                        sensitivities = OnlyFullDFG + FieldSensitive + ContextSensitive,
                        // We need to search interprocedural
                        // here.
                        // In order this acceptable also in
                        // larger graphs,
                        // we limit the maxCallDepth and hop
                        // size
                        scope = Interprocedural(maxCallDepth = 1, maxSteps = 10),
                        predicate = {
                            it is ParameterMemoryValue &&
                                /* If it's a ParameterMemoryValue from the node's
                                parameters, it has to have a DFG edge to one
                                of the node's parameters. Either partial to a derefvalue or full to the Parameter */
                                it.memoryValueUsageEdges
                                    .filter { edge ->
                                        ((((edge.granularity as? PartialDataflowGranularity<*>)
                                                ?.partialTarget as? String)
                                            ?.endsWith("derefvalue") == true) ||
                                            (edge.granularity is FullDataflowGranularity &&
                                                edge.end is Parameter)) &&
                                            edge.end in node.parameters
                                    }
                                    .size == 1 &&
                                node.parameters.any { param ->
                                    param.name.localName == it.name.parent?.localName
                                } || it in node.parameters
                        },
                    )
                paths.fulfilled
                    .mapTo(IdentitySet()) { it.nodes.last() }
                    .forEach { sourceParamValue ->
                        val matchingDeclarations =
                            if (sourceParamValue is ParameterMemoryValue)
                                node.parameters.singleOrNull {
                                    it.name.localName == sourceParamValue.name.parent?.localName
                                }
                            else sourceParamValue as? Parameter
                        if (matchingDeclarations != null) {
                            node.functionSummary
                                .computeIfAbsent(param) { ConcurrentHashMap.newKeySet() }
                                .add(
                                    FSEntry(
                                        dstValueDepth,
                                        matchingDeclarations,
                                        stringToDepth(sourceParamValue.name.localName),
                                        subAccessName,
                                        mutableSetOf(
                                            NodeWithPropertiesKey(
                                                matchingDeclarations,
                                                // Add the parameter index to indicate to the
                                                // calculatePrevDFGs function that we need to
                                                // replace the value of the call argument
                                                equalLinkedHashSetOf(
                                                    matchingDeclarations.argumentIndex
                                                ),
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

    private suspend fun storeFunctionSummary(node: Function, doubleState: PointsToState.Element) {
        clearFSDummies(node.functionSummary)
        // We try to run this in outer coroutines for each param and inner coroutines per
        // param.
        // In order not to launch an immense amount of coroutines, we calculate their number before
        val outerCoroutineCounter = node.parameters.size
        val innerCoroutineCounter =
            if (outerCoroutineCounter == 0) CPU_CORES
            else if (outerCoroutineCounter > CPU_CORES) 1 else CPU_CORES / outerCoroutineCounter
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
                    values.forEach { value ->
                        if (doubleState.hasDeclarationStateValueEntry(value)) {
                            indexes.add(IndexKey(value, 2))

                            // Additionally, we can check out the "dereference" itself to look for
                            // "derefdereferences"
                            val derefValues =
                                doubleState.getValues(value, value).mapTo(
                                    PowersetLattice.Element()
                                ) {
                                    it.first
                                }
                            // We are already inside $paramCount coroutines, so we have to divide
                            // the CPU_CORES by these routines
                            derefValues.forEachMaybeParallel(parallelism = innerCoroutineCounter) {
                                value ->
                                if (doubleState.hasDeclarationStateValueEntry(value))
                                    indexes.add(IndexKey(value, 3))
                            }
                        }
                    }

                    indexes.forEach { (idx, dstValueDepth) ->
                        val stateEntries =
                            doubleState
                                .fetchValueFromDeclarationState(
                                    idx,
                                    fetchFields = true,
                                    excludeShortFSValues = true,
                                )
                                .filterTo(PowersetLattice.Element()) { it.value.name != param.name }
                        // Remove overapproximated entries that do not indicate a subAccess
                        data class KeyWithSubAccessEntry(val value: Any, val shortFS: Any)
                        val keysWithSubAccess =
                            stateEntries
                                .filter { it.subAccessName.isNotEmpty() }
                                .mapTo(mutableSetOf()) {
                                    KeyWithSubAccessEntry(it.value, it.shortFS)
                                }

                        val filteredStateEntries =
                            stateEntries.filterNot { entry ->
                                entry.subAccessName.isEmpty() &&
                                    KeyWithSubAccessEntry(entry.value, entry.shortFS) in
                                        keysWithSubAccess
                            }
                        filteredStateEntries.forEachMaybeParallel(
                            parallelism = innerCoroutineCounter,
                            minChunkSize = 1,
                        ) { (value, shortFS, subAccessName, lastWrites) ->
                            /* See if we can find something that is different from the initial value.*/
                            if (
                                value.name != param.name &&
                                    /*Filter the PMVs from this parameter*/
                                    !(value is ParameterMemoryValue &&
                                        value.name.localName.contains("derefvalue") &&
                                        value.name.parent?.localName == param.name.localName)
                                    /* Filter the unknownMemoryValues that weren't written to*/
                                    &&
                                    !(value is UnknownMemoryValue && lastWrites.isEmpty())
                            ) {

                                // If so, store the information for the parameter in the
                                // FunctionSummary
                                // We are already inside $paramCount coroutines, so we have to
                                // divide the CPU_CORES by these routines
                                addParameterInfoToFS(
                                    node,
                                    param,
                                    dstValueDepth,
                                    value,
                                    shortFS,
                                    subAccessName,
                                    lastWrites,
                                )
                            }
                        }
                    }
                }
            }
        }

        // If we don't have anything to summarize, we add a dummy entry to the functionSummary
        if (node.functionSummary.isEmpty()) {
            node.functionSummary.put(newLiteral("dummy"), ConcurrentHashMap.newKeySet<FSEntry>())
        }
    }

    private fun MutableSet<NodeWithPropertiesKey>.parallelEquals(
        other: PowersetLattice.Element<NodeWithPropertiesKey?>
    ): Boolean {
        return this == other
    }

    protected suspend fun transfer(
        lattice: Lattice<Element<SingleGeneralStateElement, SingleDeclarationStateElement>>,
        currentEdge: EvaluationOrder,
        state: Element<SingleGeneralStateElement, SingleDeclarationStateElement>,
    ): PointsToState.Element {
        var doubleState =
            state as? PointsToState.Element
                ?: throw java.lang.IllegalArgumentException(
                    "Expected the state to be of type PointsToState.Element"
                )

        val lattice = lattice as? PointsToState ?: return state
        val currentNode = currentEdge.end

        doubleState = transferInternal(lattice, currentNode, state)
        return doubleState
    }

    protected suspend fun transferInternal(
        lattice: PointsToState,
        currentNode: Node,
        doubleState: PointsToState.Element,
    ): PointsToState.Element {
        var doubleState = doubleState

        // Used to keep iterating for steps which do not modify the alias-state otherwise
        doubleState =
            lattice.pushToDeclarationsState(
                doubleState,
                currentNode,
                doubleState.getFromDecl(currentNode)
                    ?: DeclarationStateEntryElement(
                        PowersetLattice.Element(),
                        PowersetLattice.Element(),
                        PowersetLattice.Element(),
                    ),
            )
        doubleState =
            when (currentNode) {
                is Comprehension,
                is ForEach -> handleForEach(lattice, currentNode, doubleState)
                is Function -> handleFunction(lattice, currentNode, doubleState)
                is Literal<*> -> {
                    // Literals don't have any prevDFG edges, so we skip those
                    doubleState
                }

                is Delete -> handleDelete(lattice, currentNode, doubleState)
                is Declaration,
                is MemoryAddress -> {
                    handleDeclaration(lattice, currentNode, doubleState)
                }
                is New -> {
                    handleNew(lattice, currentNode, doubleState)
                }
                is Assign -> {
                    handleAssign(lattice, currentNode, doubleState)
                }

                is UnaryOperator -> {
                    handleUnaryOperator(lattice, currentNode, doubleState)
                }

                is Call -> {
                    handleCall(lattice, currentNode, doubleState)
                }

                is Return -> handleReturn(lattice, currentNode, doubleState)

                is Expression -> {
                    if (currentNode.usedAsExpression)
                        handleExpression(lattice, currentNode, doubleState)
                    else doubleState
                }
                else -> doubleState
            }
        return doubleState
    }

    protected suspend fun handleFunction(
        lattice: PointsToState,
        function: Function,
        doubleState: PointsToState.Element,
    ): PointsToState.Element {
        // For now, all we do here is to initialize the parameters
        var doubleState = doubleState
        function.parameters.forEach { param ->
            if (param.memoryValues.filterIsInstance<ParameterMemoryValue>().isEmpty()) {
                doubleState = initializeParameter(lattice, function, param, doubleState)
            }
        }
        return doubleState
    }

    protected fun handleNew(
        lattice: PointsToState,
        currentNode: New,
        doubleState: PointsToState.Element,
    ): PointsToState.Element {
        // New creates a new memory address. This would cause infinite loops b/c there would always
        // be something new in the state.
        // To avoid this, we simply throw away the existing state for new
        val doubleState = doubleState
        doubleState.declarationsState[currentNode]?.first?.clear()
        doubleState.declarationsState[currentNode]?.second?.clear()
        doubleState.declarationsState[currentNode]?.third?.clear()
        return doubleState
    }

    protected suspend fun handleForEach(
        lattice: PointsToState,
        currentNode: Expression,
        doubleState: PointsToState.Element,
    ): PointsToState.Element {
        var doubleState = doubleState
        // All we do for now is update the state of the loop variable
        val iterable =
            when (currentNode) {
                is ForEach -> currentNode.iterable as? Node
                is Comprehension -> currentNode.iterable as? Node
                else -> null
            }
        val variable =
            when (currentNode) {
                is ForEach -> currentNode.variable
                is Comprehension -> currentNode.variable
                else -> null
            }
        if (variable == null || iterable == null) {
            log.error("Unable to identify variable or log for $currentNode")
            return doubleState
        }
        val writtenTo: List<Node> =
            // Code from the ControlFlowSensitiveDFGPass suggests we should treat ForEach and
            // Comprehensions slightly different, so lets to this for now
            when (currentNode) {
                is ForEach -> {
                    when (variable) {
                        is DeclarationStatement -> {
                            if (variable.isSingleDeclaration()) {
                                listOf(variable.singleDeclaration as Node)
                            } else if (variable.variables.size == 2) {
                                // If there are two variables, we just blindly assume that the order
                                // is (key, value), so we return the second one
                                listOf(variable.declarations[1])
                            } else {
                                listOf() // null
                            }
                        }

                        else -> listOf(variable)
                    }
                }

                is Comprehension -> {
                    when (variable) {
                        is DeclarationStatement -> {
                            variable.declarations
                        }
                        is Reference -> listOf(variable)
                        is InitializerList -> variable.initializers
                        else -> {
                            log.error(
                                "The type ${variable.javaClass} is not yet supported as Comprehension::variable"
                            )
                            listOf()
                        }
                    }
                }

                else -> listOf()
            }
        writtenTo.forEach { wT ->
            // The new sources are the values of the iterable plus the already existing ones (since
            // the loop may not get executed)
            val newVals = doubleState.getValues(wT, wT) + doubleState.getValues(iterable, iterable)
            val sources =
                newVals.mapTo(PowersetLattice.Element<Triple<Node?, Boolean, Any?>>()) {
                    Triple(it.first, it.second, null)
                }
            val destinations = identitySetOf<Node>()
            if (wT is InitializerList) destinations.addAll(wT.initializers)
            else destinations.add(wT)
            val destinationsAddresses =
                destinations.flatMapTo(IdentitySet()) { doubleState.getAddresses(it, it) }
            // Also for the new lastWrites, we need the lastWrite from the wT as well as the
            // existing ones (in case the loop is not executed)
            val lastWrites =
                destinations.mapTo(mutableSetOf()) {
                    NodeWithPropertiesKey(it, equalLinkedHashSetOf<Any>(false))
                }
            lastWrites.addAll(doubleState.getLastWrites(wT))
            doubleState =
                doubleState.updateValues(
                    lattice,
                    doubleState,
                    sources,
                    // Don't actually add any destinations. That would cause the function to draw
                    // DFG-Edges, and we don't want that here.
                    identitySetOf(),
                    destinationsAddresses,
                    lastWrites,
                )
        }

        return doubleState
    }

    protected suspend fun handleDelete(
        lattice: PointsToState,
        currentNode: Delete,
        doubleState: PointsToState.Element,
    ): PointsToState.Element {
        var doubleState = doubleState
        val sources =
            PowersetLattice.Element<Triple<Node?, Boolean, Any?>>(Triple(currentNode, false, null))
        val destinationsAddresses =
            currentNode.operands.flatMapTo(identitySetOf()) {
                doubleState.getValues(it, it).mapTo(identitySetOf()) { value -> value.first }
            }
        val lastWrites =
            mutableSetOf(NodeWithPropertiesKey(currentNode, equalLinkedHashSetOf<Any>(false)))
        doubleState =
            doubleState.updateValues(
                lattice,
                doubleState,
                sources,
                identitySetOf(),
                destinationsAddresses,
                lastWrites,
            )
        return doubleState
    }

    private suspend fun handleReturn(
        lattice: PointsToState,
        currentNode: Return,
        doubleState: PointsToState.Element,
    ): PointsToState.Element {
        /* For Return Statements, all we really want to do is to collect their return values
        to add them to the FunctionSummary
        Additionally, we need a DFG-Edge to the functionSummary as required by the spec */
        var doubleState = doubleState
        if (currentNode.returnValues.isNotEmpty()) {
            val parentFD = currentNode.firstParentOrNull<Function>()
            if (parentFD != null) {
                currentNode.returnValues.forEach { rV ->
                    val fsEntry =
                        parentFD.functionSummary.computeIfAbsent(currentNode) {
                            ConcurrentHashMap.newKeySet<FSEntry>()
                        }
                    // Filter shortFS Values
                    var values =
                        doubleState.getValues(rV, rV).mapFilteredTo(
                            mutableSetOf(),
                            { !it.second },
                        ) {
                            it.first
                        }
                    var addresses = mutableSetOf<Node>()
                    for (depth in 1..3) {
                        fsEntry.addAll(
                            values.map { value ->
                                // If the value is a newly created MemoryAddress, we only set the
                                // name so that we know later that we have to create a new
                                // MemoryAddress for each Call
                                val addressName = (value as? MemoryAddress)?.name?.localName
                                val v =
                                    if (addressName?.startsWith("NewMemoryAddress") == true)
                                        Name(addressName, parentFD.name)
                                    else value
                                val lastWrite =
                                    if (depth == 1)
                                        mutableSetOf(
                                            NodeWithPropertiesKey(parentFD, equalLinkedHashSetOf())
                                        )
                                    else
                                        addresses.flatMapTo(mutableSetOf()) { address ->
                                            doubleState.getLastWrites(address)
                                        }
                                FSEntry(depth, v, 0, "", lastWrite, equalLinkedHashSetOf(false))
                            }
                        )
                        // Try to deref the values. If we have nothing there, stop, otherwise,
                        // continue
                        val derefValues =
                            values.flatMapTo(mutableSetOf()) { value ->
                                if (doubleState.hasDeclarationStateValueEntry(value)) {
                                    doubleState.getValues(value, value).mapFiltered({
                                        !it.second
                                    }) {
                                        it.first
                                    }
                                } else emptyList()
                            }
                        if (derefValues.isEmpty()) break
                        else {
                            // When we deref once, the values will become the addresses, and the
                            // derefvalues are the new values
                            addresses = values
                            values = derefValues
                        }
                    }
                }
            }
        }

        // Get the FD we are returning to
        currentNode.firstParentOrNull<Function>()?.let { fd ->
            doubleState =
                lattice.push(
                    doubleState,
                    fd,
                    GeneralStateEntryElement(
                        PowersetLattice.Element(),
                        PowersetLattice.Element(),
                        PowersetLattice.Element(
                            NodeWithPropertiesKey(currentNode, equalLinkedHashSetOf())
                        ),
                    ),
                )
        }
        return doubleState
    }

    /**
     * Add the data flows from the Call's arguments to the Function's ParameterMemoryValues to the
     * doubleState
     */
    private suspend fun calculateIncomingCallingContexts(
        lattice: PointsToState,
        function: Function,
        call: Call,
        doubleState: PointsToState.Element,
    ): PointsToState.Element {
        var doubleState = doubleState
        val callingContext =
            CallingContextIn(
                mutableListOf(call)
            ) // TODO: Indicate somehow if this has already been done?

        if (call is MemberCall && function is Method) {
            val base = call.base
            val receiver = function.receiver
            if (base != null && receiver != null) {
                doubleState =
                    lattice.push(
                        doubleState,
                        receiver,
                        GeneralStateEntryElement(
                            PowersetLattice.Element(),
                            PowersetLattice.Element(),
                            PowersetLattice.Element(NodeWithPropertiesKey(base, emptySet())),
                        ),
                    )
            }
        }

        // We use caches to avoid double work
        val getNestedValuesCache =
            ConcurrentHashMap<
                Triple<Node, Int, Boolean>,
                PowersetLattice.Element<Pair<Node, Boolean>>,
            >()

        val paramArgMatching = matchArgumentsToCallParameters(function, call)

        call.arguments.forEach { arg ->
            if (arg.argumentIndex < function.parameters.size) {
                // In C(++), the reference to an array is a pointer, leading to the situation that
                // handing "arg" or "&arg" as argument is the same
                // We deal with this by drawing a DFG-Edge from the arg to the derefPMV in case of
                // an array pointerType.
                val argVals =
                    if (
                        (arg.type as? PointerType)?.pointerOrigin == PointerType.PointerOrigin.ARRAY
                    )
                        PowersetLattice.Element(Pair(arg, true))
                    else doubleState.getCachedNestedValues(getNestedValuesCache, arg, 1, false)
                // Create a DFG-Edge from the argument to the Parameter or its
                // ParameterMemoryValue
                val p = paramArgMatching[arg]
                if (p == null) {
                    log.warn(
                        "Did not find a matching parameter for argument $arg of call $call and function $function"
                    )
                    return@forEach
                }
                val derefPMVs =
                    p.memoryValueEdges
                        .filter {
                            (it.granularity as? PartialDataflowGranularity<*>)?.partialTarget ==
                                "derefvalue"
                        }
                        .map { it.start }
                val derefderefPMVs =
                    p.memoryValueEdges
                        .filter {
                            (it.granularity as? PartialDataflowGranularity<*>)?.partialTarget ==
                                "derefderefvalue"
                        }
                        .map { it.start }
                argVals.forEachMaybeParallel(minChunkSize = MIN_CHUNK_SIZE / 10) { (argVal, _) ->
                    doubleState =
                        innerCalculateIncomingCallingContexts(
                            arg,
                            doubleState,
                            getNestedValuesCache,
                            argVal,
                            callingContext,
                            derefPMVs,
                            lattice,
                            derefderefPMVs,
                        )
                }
                doubleState =
                    lattice.push(
                        doubleState,
                        p,
                        GeneralStateEntryElement(
                            PowersetLattice.Element(),
                            PowersetLattice.Element(
                                NodeWithPropertiesKey(arg, equalLinkedHashSetOf())
                            ),
                            PowersetLattice.Element(
                                NodeWithPropertiesKey(arg, equalLinkedHashSetOf(callingContext))
                            ),
                        ),
                    )
            }
        }
        return doubleState
    }

    private suspend fun innerCalculateIncomingCallingContexts(
        arg: Expression,
        doubleState: PointsToState.Element,
        getNestedValuesCache:
            ConcurrentHashMap<
                Triple<Node, Int, Boolean>,
                PowersetLattice.Element<Pair<Node, Boolean>>,
            >,
        argVal: Node,
        callingContext: CallingContextIn,
        derefPMVs: List<Node>,
        lattice: PointsToState,
        derefderefPMVs: List<Node>,
    ): PointsToState.Element {
        var retDoubleState = doubleState
        val argDerefVals =
            if ((arg.type as? PointerType)?.pointerOrigin == PointerType.PointerOrigin.ARRAY)
                equalLinkedHashSetOf<Node>(arg)
            else {
                retDoubleState
                    .getCachedNestedValues(getNestedValuesCache, argVal, 1, fetchFields = false)
                    .mapTo(equalLinkedHashSetOf()) { it.first }
            }
        val lastDerefWrites =
            if ((arg.type as? PointerType)?.pointerOrigin == PointerType.PointerOrigin.ARRAY)
                PowersetLattice.Element(
                    NodeWithPropertiesKey(arg, equalLinkedHashSetOf(callingContext, false))
                )
            else {
                // Since we already have the argVal, AKA the memoryAddress
                // of the argDerefVal, we simply fetch the last write for the
                // argVal from the declarationState and add the properties
                retDoubleState.declarationsState[argVal]?.third?.mapTo(PowersetLattice.Element()) {
                    NodeWithPropertiesKey(
                        it.node,
                        equalLinkedHashSetOf(callingContext, true in it.properties),
                    )
                } ?: PowersetLattice.Element()
            }
        // Also draw the edges for the (deref)derefvalues if we have
        // any and are dealing with a pointer parameter (AKA memoryValue is
        // not null)
        val argDerefValsElement =
            PowersetLattice.Element(
                argDerefVals.mapTo(PowersetLattice.Element()) {
                    NodeWithPropertiesKey(it, equalLinkedHashSetOf())
                }
            )
        val argDerefDerefVals =
            argDerefVals
                .flatMap {
                    retDoubleState.getCachedNestedValues(
                        getNestedValuesCache,
                        it,
                        1,
                        fetchFields = false,
                    )
                }
                .mapTo(equalLinkedHashSetOf()) { it.first }
        val derefderefElement =
            argDerefDerefVals.mapTo(PowersetLattice.Element()) { derefderefValue ->
                NodeWithPropertiesKey(derefderefValue, equalLinkedHashSetOf())
            }
        val lastDerefDerefWrites =
            if ((arg.type as? PointerType)?.pointerOrigin == PointerType.PointerOrigin.ARRAY)
                argDerefVals.mapTo(PowersetLattice.Element()) { argDerefVal ->
                    NodeWithPropertiesKey(argDerefVal, equalLinkedHashSetOf(callingContext, false))
                }
            else {
                // As for the lastDerefWrites, we already have the ArgDerefVals
                // which we treat as the addresses, so we directly look up the
                // lastWrites for those addresses in the declarationState
                argDerefVals.flatMapTo(PowersetLattice.Element()) { argDerefVal ->
                    retDoubleState.declarationsState[argDerefVal]?.third
                        ?: PowersetLattice.Element()
                }
            }
        derefPMVs.forEach { derefPMV ->
            retDoubleState =
                lattice.push(
                    retDoubleState,
                    derefPMV,
                    GeneralStateEntryElement(
                        PowersetLattice.Element(),
                        argDerefValsElement,
                        lastDerefWrites,
                    ),
                )
            // The same for the derefderef values
            derefderefPMVs.forEach { derefderefPMV ->
                retDoubleState =
                    lattice.push(
                        retDoubleState,
                        derefderefPMV,
                        GeneralStateEntryElement(
                            PowersetLattice.Element(derefPMV),
                            derefderefElement,
                            PowersetLattice.Element(lastDerefDerefWrites),
                        ),
                    )
            }
        }
        return retDoubleState
    }

    data class MapDstToSrcEntry(
        val param: Node?, // From which parameter entry in the functionSummary did we gather the
        // information
        val srcNode: Node?,
        val lastWrites: MutableSet<NodeWithPropertiesKey>,
        val propertySet: EqualLinkedHashSet<Any>,
        val dst: IdentitySet<Node> = identitySetOf(),
    ) {
        override fun equals(other: Any?): Boolean {
            return other is MapDstToSrcEntry &&
                srcNode === other.srcNode &&
                lastWrites.size == other.lastWrites.size &&
                lastWrites.all { lw -> other.lastWrites.any { it === lw } } &&
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

    private enum class AddEntryDedupMode {
        PARAMETER_SINGLETON,
        PARAMETER_MEMORY,
        MEMORY_ADDRESS,
        GENERIC,
    }

    private data class AddEntryPropertySetKey(
        val basePropertySet: IdKey<EqualLinkedHashSet<Any>>,
        val shortFS: Boolean,
        val partialWrite: String?,
    )

    private data class AddEntryDedupKey(
        val mode: AddEntryDedupMode,
        val writesSignature: Any?,
        val propertySet: EqualLinkedHashSet<Any>?,
        val shortFS: Boolean,
    )

    private class AddEntryDedupBucket {
        val sources: ConcurrentIdentitySet<Node?> = concurrentIdentitySetOf()

        @Volatile var initialized: Boolean = false
    }

    private data class AddEntryArgumentValuesKey(
        val argument: IdKey<Node>,
        val srcValueDepth: Int,
        val shortFS: Boolean,
    )

    private data class AddEntryParameterValuesKey(
        val parameterName: String,
        val srcValueDepth: Int,
    )

    private data class AddEntryGenericSourcesKey(
        val srcNode: IdKey<Node>?,
        val srcValueDepth: Int,
        val shortFS: Boolean,
    )

    private data class PreprocessedFSEntry(
        val dstValueDepth: Int,
        val subAccessName: String,
        val srcNode: Node?,
        val srcValueDepth: Int,
        val shortFS: Boolean,
        var propertySet: EqualLinkedHashSet<Any>,
        val prev: MutableSet<NodeWithPropertiesKey>,
    )

    private data class AddEntryDestinationDedupCache(
        val buckets: ConcurrentHashMap<AddEntryDedupKey, AddEntryDedupBucket> = ConcurrentHashMap()
    )

    private class AddEntryToMapCache {
        val propertySetCache = ConcurrentHashMap<AddEntryPropertySetKey, EqualLinkedHashSet<Any>>()
        val destinationDedup = ConcurrentIdentityHashMap<Node, AddEntryDestinationDedupCache>()
        val argumentValuesCache = ConcurrentHashMap<AddEntryArgumentValuesKey, IdentitySet<Node?>>()
        val parameterValuesCache =
            ConcurrentHashMap<AddEntryParameterValuesKey, IdentitySet<Node?>>()
        val genericSourcesCache = ConcurrentHashMap<AddEntryGenericSourcesKey, IdentitySet<Node?>>()
    }

    private suspend fun handleCall(
        lattice: PointsToState,
        currentNode: Call,
        doubleState: PointsToState.Element,
    ): PointsToState.Element {
        var doubleState = doubleState
        val mapDstToSrc = ConcurrentIdentityHashMap<Node, ConcurrentIdentitySet<MapDstToSrcEntry>>()
        val addEntryToMapCache = AddEntryToMapCache()

        // The toIdentitySet avoids having the same elements multiple times
        var invokes = currentNode.invokes.toIdentitySet()
        // If we have multiple functions with the same name and the same signature and one has an
        // empty body, we assume that this is from the header so we ignore it
        invokes =
            invokes.mapFilteredTo(
                identitySetOf(),
                { inv ->
                    !(inv.body == null &&
                        // If the body is empty, check if we have the "real" Function
                        // somewhere in our list
                        invokes.any { it != inv && it.name == inv.name && it.type == inv.type })
                },
            ) { inv ->
                inv
            }
        invokes.forEach { invoke ->
            val inv = calculateFunctionSummaries(invoke)
            if (inv != null) {
                doubleState =
                    calculateIncomingCallingContexts(lattice, inv, currentNode, doubleState)

                data class ParamFsWork(
                    val param: Node,
                    val argument: Expression,
                    val entriesByDepth: Array<List<PreprocessedFSEntry>>,
                )

                val parameterWork =
                    inv.functionSummary.mapNotNull { (param, fsEntries) ->
                        val dst =
                            when (param) {
                                // If we have a record, we use the base as argument
                                is Record -> {
                                    (currentNode as? MemberCall)?.base
                                }
                                is Parameter ->
                                    if (param.argumentIndex < currentNode.arguments.size)
                                        currentNode.arguments[param.argumentIndex]
                                    else null
                                is Return,
                                is Function -> currentNode
                                else -> null
                            }

                        if (dst == null) {
                            null
                        } else {
                            val depthBuckets = Array(4) { mutableListOf<PreprocessedFSEntry>() }
                            for ((
                                dstValueDepth,
                                srcNode,
                                srcValueDepth,
                                subAccessName,
                                lastWrites,
                                properties,
                            ) in fsEntries) {
                                if (dstValueDepth in 0..3) {
                                    val shortFS = properties.any { it == true }
                                    val propertySet = properties
                                    val normalizedSrcNode =
                                        when (srcNode) {
                                            is Function -> currentNode
                                            is Name -> {
                                                val memoryAddress =
                                                    CallToMemAddrMap.computeIfAbsent(currentNode) {
                                                            ConcurrentIdentityHashMap()
                                                        }
                                                        .computeIfAbsent(srcNode) {
                                                            MemoryAddress(srcNode)
                                                        }
                                                memoryAddress.nextDFGEdges +=
                                                    Dataflow(memoryAddress, inv)
                                                memoryAddress
                                            }

                                            else -> srcNode as? Node
                                        }
                                    val prev =
                                        calculatePrevDFGs(
                                            lastWrites,
                                            shortFS,
                                            currentNode,
                                            inv,
                                            srcNode,
                                        )
                                    depthBuckets[dstValueDepth].add(
                                        PreprocessedFSEntry(
                                            dstValueDepth,
                                            subAccessName,
                                            normalizedSrcNode,
                                            srcValueDepth,
                                            shortFS,
                                            propertySet,
                                            prev,
                                        )
                                    )
                                }
                            }

                            ParamFsWork(
                                param,
                                dst,
                                Array(4) { depth -> depthBuckets[depth].toList() },
                            )
                        }
                    }

                if (parameterWork.isEmpty()) {
                    return@forEach
                }

                // If we have a FunctionSummary, we push the values of the arguments and
                // return value after executing the function call to our doubleState.
                // We can't go through all levels at once as a change at a lower level may
                // affect a higher level. So let's do this step by step
                for (depth in 0..3) {
                    coroutineScope {
                        for (work in parameterWork) {
                            if (work.entriesByDepth[depth].isEmpty()) {
                                continue
                            }

                            launch(Dispatchers.Default) {
                                work.entriesByDepth[depth].forEachMaybeParallel { entry ->
                                    val (destinationAddresses, destinations) =
                                        calculateCallDestinations(
                                            doubleState,
                                            mapDstToSrc,
                                            entry.dstValueDepth,
                                            entry.subAccessName,
                                            work.argument,
                                            entry.propertySet,
                                            work.param,
                                        )
                                    addEntryToMap(
                                        doubleState,
                                        mapDstToSrc,
                                        addEntryToMapCache,
                                        destinationAddresses,
                                        destinations,
                                        entry.srcNode,
                                        entry.shortFS,
                                        entry.srcValueDepth,
                                        entry.propertySet,
                                        currentNode,
                                        entry.prev,
                                        work.param,
                                        // TODO for merge: add subAccessName?
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (log.isTraceEnabled) {
                log.trace("inv is null, skipping")
            }
        }

        val callingContextOut = CallingContextOut(mutableListOf(currentNode))
        mapDstToSrc.forEach { (dstAddr, values) ->
            doubleState =
                writeMapEntriesToState(lattice, doubleState, dstAddr, values, callingContextOut)
        }

        return doubleState
    }

    private fun calculatePrevDFGs(
        lastWrites: MutableSet<NodeWithPropertiesKey>,
        shortFS: Boolean,
        currentNode: Call,
        invoke: Function,
        srcNode: Any?,
    ): MutableSet<NodeWithPropertiesKey> {
        val ret = mutableSetOf<NodeWithPropertiesKey>()
        // If we have nothing, the last write is probably the Function
        if (lastWrites.isEmpty()) ret.add(NodeWithPropertiesKey(invoke, equalLinkedHashSetOf()))
        lastWrites.forEach { (lw, properties) ->
            // If the lastWrite is a Record, that's a hint from the functionSummary that we have a
            // write to the base. Since we didn't know the base yet when creating the
            // functionSummary, we fetch it now
            val prev = if (lw is Record && srcNode is Node) srcNode else lw
            if (shortFS) {
                when (prev) {
                    is Function -> ret.add(NodeWithPropertiesKey(currentNode, properties))
                    is Parameter -> {
                        // For dummy functionSummary entries, we have an Integer indicating the
                        // parameter's index for which we should use the Call's argument
                        // here
                        // Otherwise, the lastWrite was the Parameter itself, so we leave
                        // it as is
                        val index = properties.filterIsInstance<Int>().singleOrNull()
                        if (index != null && index < currentNode.arguments.size)
                            ret.add(NodeWithPropertiesKey(currentNode.arguments[index], properties))
                        else ret.add(NodeWithPropertiesKey(prev, properties))
                    }
                    else -> ret.add(NodeWithPropertiesKey(prev, properties))
                }
            } else ret.add(NodeWithPropertiesKey(prev, properties))
        }
        return ret
    }

    private suspend fun calculateFunctionSummaries(invoke: Function): Function? {
        fun addDummyFS() {
            val newValues = ConcurrentHashMap.newKeySet<FSEntry>()
            invoke.parameters.forEach { newValues.add(FSEntry(0, it, 1, "", isDummy = true)) }
            val entries = identitySetOf<Node>()
            if (invoke.returns.isNotEmpty()) entries.addAll(invoke.returns) else entries.add(invoke)
            entries.forEach { entry -> invoke.functionSummary.put(entry, newValues) }
        }

        if (invoke.functionSummary.isEmpty()) {
            if (invoke.hasBody()) {
                if (log.isTraceEnabled) {
                    log.trace(
                        "functionSummaryAnalysisChain: {}",
                        functionSummaryAnalysisChain.map { it.name.localName },
                    )
                }
                if (invoke !in functionSummaryAnalysisChain) {
                    // If the node has a body and a functionSummary, we have visited it before, no
                    // need to analyze it again.
                    // Otherwise, we call acceptInternal on it
                    if (
                        (invoke.functionSummary.isNotEmpty() && invoke.body != null) &&
                            invoke.functionSummary.keys.any {
                                it in invoke.parameters || it in invoke.returns
                            }
                    ) {
                        if (log.isTraceEnabled) {
                            log.trace(
                                "Not calling acceptInternal on ${invoke.name} because we already have a functionSummary."
                            )
                        }
                    } else {
                        if (log.isTraceEnabled) {
                            log.trace("Calling acceptInternal(${invoke.name.localName})")
                        }
                        val startTime = TimeSource.Monotonic.markNow()
                        acceptInternal(invoke)
                        if (log.isTraceEnabled) {
                            log.trace("Finished with acceptInternal(${invoke.name.localName})")
                        }
                        if (timeouts.isNotEmpty()) {
                            if (log.isTraceEnabled) {
                                log.trace("Old last timeout: ${timeouts.last()}")
                            }
                            timeouts[timeouts.size - 1] =
                                timeouts.last() +
                                    startTime.elapsedNow().toLong(DurationUnit.MILLISECONDS)
                            if (log.isTraceEnabled) {
                                log.trace(
                                    "Increased last timeout to consider time spent in acceptInternal. New timeout: ${timeouts.last()}"
                                )
                            }
                        }
                    }
                } else {
                    log.error(
                        "Cannot calculate functionSummary for ${invoke.name.localName} as it's recursively called. callChain: ${functionSummaryAnalysisChain.map{it.name.localName}}"
                    )
                    addDummyFS()
                }
            } else {
                // Add a dummy function summary so that we don't try this every time
                // In this dummy, all parameters point either to returns if we have any, or the
                // Function itself
                addDummyFS()
            }
        }
        return invoke
    }

    // Internal wrapper that delegates equality / hash to object identity
    data class IdKey<T>(val ref: T) {
        override fun equals(other: Any?) = (other as? IdKey<*>)?.ref === ref // reference equality

        override fun hashCode(): Int = System.identityHashCode(ref) // identity hash
    }

    data class NodeWithPropertiesKey(val node: Node, val properties: Set<Any> = emptySet<Any>()) {
        // Since we are dealing with pairs, carefully check if we already have them
        override fun equals(other: Any?): Boolean =
            other is NodeWithPropertiesKey && node === other.node && properties == other.properties

        override fun hashCode(): Int {
            var h = System.identityHashCode(node)
            // The order of the properties doesn't matter
            for (p in properties) h += p.hashCode()
            return h
        }
    }

    private suspend fun writeMapEntriesToState(
        lattice: PointsToState,
        doubleState: PointsToState.Element,
        dstAddr: Node,
        values: ConcurrentIdentitySet<MapDstToSrcEntry>,
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
                        it.propertySet
                            .filterIsInstance<PartialDataflowGranularity<*>>()
                            .singleOrNull()
                            ?.partialTarget,
                    )
                }
            )

        val lastWrites: MutableSet<NodeWithPropertiesKey> = ConcurrentHashMap.newKeySet()
        val destinations: MutableSet<IdKey<Node>> = ConcurrentHashMap.newKeySet()

        values.forEachMaybeParallel(minChunkSize = 1) { value ->
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
                        // Keep and canonicalize call context by identity, then append missing
                        // calls.
                        // This avoids context churn and duplicate growth across iterations.
                        val mergedCalls = mutableListOf<Call>()
                        val seenCalls = hashSetOf<IdKey<Call>>()
                        existingCallingContext.calls.forEach { call ->
                            if (seenCalls.add(IdKey(call))) {
                                mergedCalls.add(call)
                            }
                        }
                        callingContext.calls.forEach { call ->
                            if (seenCalls.add(IdKey(call))) {
                                mergedCalls.add(call)
                            }
                        }
                        lwPropertySet.add(CallingContextOut(mergedCalls))
                    } else {
                        val mergedCalls = mutableListOf<Call>()
                        val seenCalls = hashSetOf<IdKey<Call>>()
                        callingContext.calls.forEach { call ->
                            if (seenCalls.add(IdKey(call))) {
                                mergedCalls.add(call)
                            }
                        }
                        lwPropertySet.add(CallingContextOut(mergedCalls))
                    }
                }
                // Add all other previous properties
                lwPropertySet.addAll(lwProps.filter { it !is CallingContextOut })
                // Add them to the set of lastWrites if there is no same element
                // in there yet
                lastWrites.add(NodeWithPropertiesKey(lw, lwPropertySet))
            }
            value.dst.forEach { destinations.add(IdKey(it)) }
        }

        return@coroutineScope doubleState.updateValues(
            lattice,
            doubleState,
            sources,
            destinations.mapTo(identitySetOf()) { it.ref },
            identitySetOf(dstAddr),
            lastWrites,
        )
    }

    /**
     * Adds entries to the map that tracks the source nodes for each destination node.
     *
     * This method updates the `mapDstToSrc` map with the source nodes and their properties for each
     * destination node. It handles different types of source nodes, including `Parameter`,
     * `ParameterMemoryValue`, `MemoryAddress`, and other nodes. Depending on the type of source
     * node, it may also update the general state to draw additional Data Flow Graph (DFG) edges.
     *
     * @param doubleState The current state of the points-to analysis.
     * @param mapDstToSrc The map that tracks the source nodes for each destination node.
     * @param destinationAddresses The set of destination nodes.
     * @param srcNode The source node to be added to the map.
     * @param shortFS A flag indicating if this is a short function summary.
     * @param srcValueDepth The depth of the source value.
     * @param propertySet The set of properties associated with the source node.
     * @param currentNode The current call expression being analyzed.
     * @return The updated map that tracks the source nodes for each destination node.
     */
    private fun addEntryToMap(
        doubleState: PointsToState.Element,
        mapDstToSrc: ConcurrentIdentityHashMap<Node, ConcurrentIdentitySet<MapDstToSrcEntry>>,
        addEntryToMapCache: AddEntryToMapCache,
        destinationAddresses: IdentitySet<Pair<Node, String?>>,
        destinations: IdentitySet<Node>,
        srcNode: Node?,
        shortFS: Boolean,
        srcValueDepth: Int,
        propertySet: EqualLinkedHashSet<Any>,
        currentNode: Call,
        lastWrites: MutableSet<NodeWithPropertiesKey>,
        param: Node,
    ): ConcurrentIdentityHashMap<Node, ConcurrentIdentitySet<MapDstToSrcEntry>> {
        val doubleState = doubleState

        data class DestinationContext(
            val currentSet: ConcurrentIdentitySet<MapDstToSrcEntry>,
            val updatedPropertySet: EqualLinkedHashSet<Any>,
            val dedupCache: AddEntryDestinationDedupCache,
        )

        if (destinationAddresses.isEmpty()) {
            return mapDstToSrc
        }

        val basePropertySetKey = IdKey(propertySet)
        fun getUpdatedPropertySet(partialWrite: String?): EqualLinkedHashSet<Any> {
            val cacheKey = AddEntryPropertySetKey(basePropertySetKey, shortFS, partialWrite)
            val cached = addEntryToMapCache.propertySetCache[cacheKey]
            if (cached != null) {
                return cached
            }

            return createPropertySet(propertySet, shortFS, partialWrite).also {
                addEntryToMapCache.propertySetCache[cacheKey] = it
            }
        }

        var destinationContexts: List<DestinationContext>? = null
        fun getDestinationContexts(): List<DestinationContext> {
            return destinationContexts
                ?: destinationAddresses
                    .map { (destination, partialWrite) ->
                        DestinationContext(
                            mapDstToSrc.computeIfAbsent(destination) { concurrentIdentitySetOf() },
                            getUpdatedPropertySet(partialWrite),
                            addEntryToMapCache.destinationDedup.computeIfAbsent(destination) {
                                AddEntryDestinationDedupCache()
                            },
                        )
                    }
                    .also { destinationContexts = it }
        }

        fun getOrBuildSources(
            context: DestinationContext,
            dedupKey: AddEntryDedupKey,
            matcher: (MapDstToSrcEntry) -> Boolean,
        ): ConcurrentIdentitySet<Node?> {
            val bucket =
                context.dedupCache.buckets.computeIfAbsent(dedupKey) { AddEntryDedupBucket() }
            if (bucket.initialized) {
                return bucket.sources
            }

            synchronized(bucket) {
                if (!bucket.initialized) {
                    for (entry in context.currentSet) {
                        if (matcher(entry)) {
                            bucket.sources.add(entry.srcNode)
                        }
                    }
                    bucket.initialized = true
                }
            }
            return bucket.sources
        }

        fun skipIfSaturated(
            existingSources: ConcurrentIdentitySet<Node?>,
            candidates: Collection<Node?>,
        ): Boolean {
            if (candidates.isEmpty()) {
                return true
            }

            for (candidate in candidates) {
                if (candidate !in existingSources) {
                    return false
                }
            }

            return true
        }

        fun insertIfNew(
            context: DestinationContext,
            dedupKey: AddEntryDedupKey,
            sources: ConcurrentIdentitySet<Node?>,
            source: Node?,
            entry: () -> MapDstToSrcEntry,
        ) {
            if (sources.add(source)) {
                context.currentSet += entry()
                context.dedupCache.buckets[dedupKey]?.sources?.add(source)
            }
        }

        when (srcNode) {
            is Parameter -> {
                // Add the (dereferenced) value of the respective argument
                // in the Call
                if (srcNode.argumentIndex < currentNode.arguments.size) {
                    val src = currentNode.arguments[srcNode.argumentIndex]
                    // If this is a short FunctionSummary, we also
                    // update the generalState to draw the additional DFG Edges
                    if (shortFS) {
                        val newEntry = NodeWithPropertiesKey(src, equalLinkedHashSetOf<Any>(true))
                        doubleState.generalState.computeIfAbsent(currentNode) {
                            TripleLattice.Element(
                                PowersetLattice.Element(),
                                PowersetLattice.Element(),
                                PowersetLattice.Element(),
                            )
                        }
                        doubleState.generalState[currentNode]?.third?.add(newEntry)
                    }
                    val argumentValuesKey =
                        AddEntryArgumentValuesKey(IdKey(src), srcValueDepth, shortFS)
                    val cachedValues = addEntryToMapCache.argumentValuesCache[argumentValuesKey]
                    val values =
                        if (cachedValues != null) {
                            cachedValues
                        } else {
                            val computedValues =
                                if (!shortFS)
                                    doubleState
                                        .getNestedValues(
                                            src,
                                            srcValueDepth,
                                            fetchFields = true,
                                            excludeShortFSValues = true,
                                        )
                                        .mapTo(identitySetOf<Node?>()) { it.first }
                                else identitySetOf<Node?>(src)
                            addEntryToMapCache.argumentValuesCache[argumentValuesKey] =
                                computedValues
                            computedValues
                        }

                    if (values.isEmpty()) {
                        return mapDstToSrc
                    }

                    val singletonLastWrite = lastWrites.singleOrNull()
                    val singletonLastWrites = PowersetLattice.Element(singletonLastWrite)
                    for (context in getDestinationContexts()) {
                        val dedupKey =
                            AddEntryDedupKey(
                                AddEntryDedupMode.PARAMETER_SINGLETON,
                                singletonLastWrite,
                                context.updatedPropertySet,
                                shortFS,
                            )
                        val existingSources =
                            getOrBuildSources(context, dedupKey) {
                                it.lastWrites.parallelEquals(singletonLastWrites) &&
                                    it.propertySet == context.updatedPropertySet
                            }

                        if (skipIfSaturated(existingSources, values)) {
                            continue
                        }

                        for (value in values) {
                            insertIfNew(context, dedupKey, existingSources, value) {
                                MapDstToSrcEntry(
                                    param,
                                    value,
                                    lastWrites,
                                    context.updatedPropertySet,
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
                val fullLastWrites =
                    PowersetLattice.Element<NodeWithPropertiesKey?>().apply { addAll(lastWrites) }
                val parameterName = srcNode.name.parent?.localName ?: return mapDstToSrc

                val parameterValuesKey = AddEntryParameterValuesKey(parameterName, srcValueDepth)
                val cachedParameterValues =
                    addEntryToMapCache.parameterValuesCache[parameterValuesKey]
                val parameterValues =
                    if (cachedParameterValues != null) {
                        cachedParameterValues
                    } else {
                        val computedParameterValues = identitySetOf<Node?>()
                        for (invokedFunction in currentNode.invokes) {
                            for (parameter in invokedFunction.parameters) {
                                if (parameter.name.localName != parameterName) {
                                    continue
                                }

                                if (parameter.argumentIndex < currentNode.arguments.size) {
                                    val arg = currentNode.arguments[parameter.argumentIndex]
                                    for ((value, _) in
                                        doubleState.getNestedValues(arg, srcValueDepth)) {
                                        computedParameterValues.add(value)
                                    }
                                }
                            }
                        }
                        addEntryToMapCache.parameterValuesCache[parameterValuesKey] =
                            computedParameterValues
                        computedParameterValues
                    }

                if (parameterValues.isEmpty()) {
                    return mapDstToSrc
                }

                val lastWritesSignature = lastWrites.mapTo(HashSet()) { it }
                for (context in getDestinationContexts()) {
                    val dedupKey =
                        AddEntryDedupKey(
                            AddEntryDedupMode.PARAMETER_MEMORY,
                            lastWritesSignature,
                            context.updatedPropertySet,
                            shortFS,
                        )
                    val existingSources =
                        getOrBuildSources(context, dedupKey) {
                            it.lastWrites.parallelEquals(fullLastWrites) &&
                                it.propertySet == context.updatedPropertySet
                        }

                    if (skipIfSaturated(existingSources, parameterValues)) {
                        continue
                    }

                    for (value in parameterValues) {
                        insertIfNew(context, dedupKey, existingSources, value) {
                            MapDstToSrcEntry(
                                param,
                                value,
                                lastWrites,
                                context.updatedPropertySet,
                                destinations,
                            )
                        }
                    }
                }
            }

            is MemoryAddress -> {
                val writesIdentity = IdKey(lastWrites)
                for (context in getDestinationContexts()) {
                    val dedupKey =
                        AddEntryDedupKey(
                            AddEntryDedupMode.MEMORY_ADDRESS,
                            writesIdentity,
                            context.updatedPropertySet,
                            shortFS,
                        )
                    val existingSources =
                        getOrBuildSources(context, dedupKey) {
                            it.lastWrites === lastWrites &&
                                it.propertySet == context.updatedPropertySet
                        }

                    insertIfNew(context, dedupKey, existingSources, srcNode) {
                        MapDstToSrcEntry(
                            param,
                            srcNode,
                            lastWrites,
                            context.updatedPropertySet,
                            destinations,
                        )
                    }
                }
            }

            else -> {
                val genericSourcesKey =
                    AddEntryGenericSourcesKey(srcNode?.let { IdKey(it) }, srcValueDepth, shortFS)
                val cachedNewSources = addEntryToMapCache.genericSourcesCache[genericSourcesKey]
                val newSources =
                    if (cachedNewSources != null) {
                        cachedNewSources
                    } else {
                        val newSet =
                            if (srcValueDepth == 0) PowersetLattice.Element(Pair(srcNode, shortFS))
                            else
                                srcNode?.let {
                                    doubleState.getNestedValues(it, srcValueDepth).mapTo(
                                        PowersetLattice.Element()
                                    ) {
                                        Pair(it.first, shortFS)
                                    }
                                } ?: PowersetLattice.Element(Pair(null, shortFS))
                        if (newSet.isEmpty()) {
                            return mapDstToSrc
                        }

                        newSet
                            .mapTo(identitySetOf<Node?>()) { it.first }
                            .also { addEntryToMapCache.genericSourcesCache[genericSourcesKey] = it }
                    }
                if (newSources.isEmpty()) {
                    return mapDstToSrc
                }

                val writesIdentity = IdKey(lastWrites)
                for (context in getDestinationContexts()) {
                    val dedupKey =
                        AddEntryDedupKey(AddEntryDedupMode.GENERIC, writesIdentity, null, shortFS)
                    val existingSources =
                        getOrBuildSources(context, dedupKey) {
                            it.lastWrites === lastWrites && shortFS in it.propertySet
                        }

                    if (skipIfSaturated(existingSources, newSources)) {
                        continue
                    }

                    for (newSource in newSources) {
                        insertIfNew(context, dedupKey, existingSources, newSource) {
                            MapDstToSrcEntry(
                                param,
                                newSource,
                                lastWrites,
                                context.updatedPropertySet,
                                destinations,
                            )
                        }
                    }
                }
            }
        }
        return mapDstToSrc
    }

    private fun createPropertySet(
        propertySet: EqualLinkedHashSet<Any>,
        shortFS: Boolean,
        partialWrite: String?,
    ): EqualLinkedHashSet<Any> {
        val hasShortFS = shortFS in propertySet

        if (partialWrite == null) {
            if (hasShortFS) return propertySet

            return equalLinkedHashSetOf<Any>().apply {
                addAll(propertySet)
                add(shortFS)
            }
        }

        val hasFullGranularity = propertySet.any { it is FullDataflowGranularity }
        val hasMatchingPartialGranularity =
            propertySet.any {
                it is PartialDataflowGranularity<*> &&
                    (it.partialTarget as? Field)?.name?.localName == partialWrite
            }

        if (hasShortFS && !hasFullGranularity && hasMatchingPartialGranularity) {
            return propertySet
        }

        return equalLinkedHashSetOf<Any>().apply {
            addAll(propertySet)
            add(shortFS)
            // If the partialWrite is not null, it means this is the base memory address and no
            // field, so we add the partial write property
            removeIf { it is FullDataflowGranularity }
            add(PartialDataflowGranularity(Field().apply { name = Name(partialWrite) }))
        }
    }

    /**
     * Returns a Pair of destination (for the general State) and destinationAddresses The return
     * address are a Pair. The string has the following coding:
     * 1) null: No partial write
     * 2) any other value: The field to which we write to
     */
    private fun calculateCallDestinations(
        doubleState: PointsToState.Element,
        mapDstToSrc: ConcurrentIdentityHashMap<Node, ConcurrentIdentitySet<MapDstToSrcEntry>>,
        dstValueDepth: Int,
        subAccessName: String,
        argument: Node,
        properties: EqualLinkedHashSet<Any>,
        param: Node,
    ): Pair<IdentitySet<Pair<Node, String?>>, IdentitySet<Node>> {
        // If the dstAddr is a Call, the dst is the same. Otherwise, we don't really know,
        // so we leave it empty
        val destination: IdentitySet<Node> =
            if (argument is Call) {
                // For calls, if the dstValueDepth is 0, the destination is the argument AKA the
                // call
                // If the dstValueDepth is larger, we leave the destination empty for now
                if (dstValueDepth == 0) identitySetOf(argument) else identitySetOf()
            }
            // If the argument is a PointerReference for a global variable, the destination is it's
            // refersTo
            // It might also be the case that argument is a Reference to an array, so then we treat
            // it like a PointerReference
            // TODO: This does make sense for C(++), but how about other languages?
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
        // Check if the declarationState might be outdated by reading the mapDstToSrc. When the
        // argument is a call, we check for a lower depth (>1) as changes already at the deref
        // (depth 2) can affect return
        // values, otherwise it's probably about a parameter, so we start at a depth of > 2 since
        // there won't be any effect on the first depth (AKA the value)
        if (argument is Call && dstValueDepth > 1) {
            val updatedAddresses: IdentitySet<Pair<Node, String?>> =
                mapDstToSrc.entries.flatMapTo(IdentitySet()) {
                    if (it.key == argument) {
                        it.value.mapFilteredTo(
                            IdentitySet(),
                            { it.param == param && it.srcNode != null },
                        ) {
                            it.srcNode!! to null
                        }
                    } else emptySet()
                }
            if (updatedAddresses.isNotEmpty()) return Pair(updatedAddresses, destination)
        } else if (dstValueDepth > 2) {
            val argumentValues =
                doubleState.getValues(argument, argument).mapTo(IdentitySet()) { it.first }
            val updatedAddresses: IdentitySet<Pair<Node, String?>> =
                mapDstToSrc.entries.flatMapTo(IdentitySet()) {
                    if (it.key in argumentValues) {
                        it.value.mapNotNullTo(IdentitySet()) { it.srcNode?.let { it to null } }
                    } else emptySet()
                }
            if (updatedAddresses.isNotEmpty()) return Pair(updatedAddresses, destination)
        }

        val partialAccess =
            properties.filterIsInstance<PartialDataflowGranularity<*>>().singleOrNull()
        // If the param is a record, we are dealing with a MemberCall, for which we don't need to
        // fetch the fieldAddresses
        if (subAccessName.isNotEmpty() || (partialAccess != null && param !is Record)) {
            val fieldAddresses = identitySetOf<Pair<Node, String?>>()
            // Collect the fieldAddresses for each possible value
            val argumentValues =
                doubleState.getNestedValues(argument, destAddrDepth, fetchFields = true)
            argumentValues.forEach { (v, _) ->
                // We over approximate here and also add the main memory Address to the list of
                // destinations
                fieldAddresses.add(v to subAccessName)

                val parentName = getNodeName(v)
                val partialString =
                    subAccessName.ifEmpty { (partialAccess?.partialTarget as? String) ?: "" }
                val newName = Name(partialString, parentName)
                fieldAddresses.addAll(
                    doubleState.fetchFieldAddresses(identitySetOf(v), newName).map { it to null }
                )
            }
            return Pair(fieldAddresses, destination)
        } else {
            val destinationAddresses =
                doubleState.getNestedValues(argument, destAddrDepth).mapTo(
                    identitySetOf<Pair<Node, String?>>()
                ) {
                    it.first to null
                }
            // If the argument is a MemberAccess, we also collect the addresses of the bases
            // We build the offsetStr from all offsets we find, so if the argument is 'a.b.c.d', the
            // offset str of the last element should be 'b.c.d'
            var offsetStr = ""
            collectBasesAndOffsets(argument).reversed().forEach { (base, offset) ->
                if (offsetStr != "") offsetStr += "."
                offsetStr +=
                    ((offset as? String)
                        ?: (offset as? Field)?.name?.toString()
                        ?: (offset as? Field)?.name?.localName)
                doubleState.getNestedValues(base, destAddrDepth).forEach { (value, isShortFS) ->
                    if (!isShortFS) {
                        destinationAddresses.add(value to offsetStr)
                    }
                }
            }

            return Pair(destinationAddresses, destination)
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

                newDeclState.put(
                    addr,
                    DeclarationStateEntryElement(
                        PowersetLattice.Element(addr),
                        PowersetLattice.Element(newValueEntry),
                        PowersetLattice.Element(newLastWriteEntry),
                    ),
                )
            }
            doubleState =
                PointsToState.Element(
                    doubleState.generalState,
                    ConcurrentMapLattice.Element(newDeclState),
                )
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

    private suspend fun handleAssign(
        lattice: PointsToState,
        currentNode: Assign,
        doubleState: PointsToState.Element,
    ): PointsToState.Element {
        var doubleState = doubleState
        /* For Assigns, we update the value of the lhs with the rhs */
        val sources =
            currentNode.rhs.flatMapTo(PowersetLattice.Element<Triple<Node?, Boolean, Any?>>()) {
                doubleState.getValues(it, it).map { Triple(it.first, it.second, null) }
            }
        val destinations = identitySetOf<Node>()
        currentNode.lhs.forEach {
            if (it is InitializerList) destinations.addAll(it.initializers)
            else destinations.add(it)
        }
        val destinationsAddresses =
            destinations.flatMapTo(identitySetOf()) { doubleState.getAddresses(it, it) }
        val lastWrites: MutableSet<NodeWithPropertiesKey> =
            destinations.mapTo(identitySetOf()) {
                NodeWithPropertiesKey(it, equalLinkedHashSetOf<Any>(false))
            }

        /* For literals, we store the address and the lastWrite as "theirs" in the DeclarationState so that we know from where we have to draw DFGEdges in the future */
        // TODO: would this make sense for everything in the lhs?
        val l = currentNode.rhs.singleOrNull() as? Literal<*>
        l?.let {
            doubleState =
                lattice.pushToDeclarationsState(
                    doubleState,
                    l,
                    DeclarationStateEntryElement(
                        PowersetLattice.Element(destinationsAddresses),
                        PowersetLattice.Element(),
                        PowersetLattice.Element(lastWrites),
                    ),
                )
        }
        /* For compound assigns, we need to a DFG Edge to the lhs, so we store that before overwriting the state */
        val destinationLastWrites =
            if (currentNode.isCompoundAssignment)
                currentNode.lhs.map { Pair(it, doubleState.getLastWrites(it)) }
            else null
        /* If the lhs is a MemberAccess or a Subscription, we additionally set the last write to the base/arrayExpression here */
        doubleState = updateBaseLastWrites(doubleState, destinations)

        doubleState =
            doubleState.updateValues(
                lattice,
                doubleState,
                sources,
                destinations,
                destinationsAddresses,
                lastWrites,
            )
        /* If we have a compoundAssignment, we restore the DFG edge from the lhs' last write to the lhs */
        destinationLastWrites?.forEach { (destination, destinationLastWrites) ->
            doubleState =
                lattice.push(
                    doubleState,
                    destination,
                    GeneralStateEntryElement(
                        PowersetLattice.Element(),
                        PowersetLattice.Element(),
                        PowersetLattice.Element(destinationLastWrites),
                    ),
                )
        }
        /* If the assignment is within a BinaryOperator, some languages treat this as DF from the rhs to the BinaryOperator, so we draw a DFG-Edge to the Assign to link the path */
        val astParent = (currentNode.astParent as? BinaryOperator)
        if (astParent != null) {
            currentNode.rhs.forEach { rhs ->
                doubleState =
                    lattice.push(
                        doubleState,
                        currentNode,
                        GeneralStateEntryElement(
                            PowersetLattice.Element(),
                            PowersetLattice.Element(),
                            PowersetLattice.Element(NodeWithPropertiesKey(rhs)),
                        ),
                    )
            }
        }
        return doubleState
    }

    /* Update MemberAccess or a Subscription listed in [destinations] to set the last write to the base/arrayExpression here
     *  Make sure to catch nested MemberAccesses/Subscriptions too */
    private fun updateBaseLastWrites(
        doubleState: PointsToState.Element,
        destinations: IdentitySet<Node>,
    ): PointsToState.Element {
        val doubleState = doubleState
        val basesAndOffsets =
            destinations.flatMap { destination -> collectBasesAndOffsets(destination) }
        basesAndOffsets.forEach { (base, offset) ->
            doubleState.getAddresses(base, base).forEach { baseAddress ->
                val entry =
                    doubleState.declarationsState.computeIfAbsent(baseAddress) {
                        TripleLattice.Element(
                            PowersetLattice.Element(baseAddress),
                            PowersetLattice.Element(),
                            PowersetLattice.Element(),
                        )
                    }
                val newProperties = equalLinkedHashSetOf(false, PartialDataflowGranularity(offset))
                // If we already have an entry with exactly the same properties (so a write to the
                // same field), we remove that one
                entry.third.removeIf { nwpk -> nwpk.properties.containsAll(newProperties) }
                entry.third.add(NodeWithPropertiesKey(base, newProperties))
            }
        }
        return doubleState
    }

    private suspend fun handleExpression(
        lattice: PointsToState,
        currentNode: Expression,
        doubleState: PointsToState.Element,
    ): PointsToState.Element {
        var doubleState = doubleState

        // TODO: If the expression is global, the DFG-Edges should have already been drawn by the
        // DFGPass. However, we still need to add for example the memoryValues

        /* If we have an Expression that is written to, we handle its values later and ignore it now */
        val access =
            when (currentNode) {
                is Reference,
                is BinaryOperator -> currentNode.access
                is Subscription if currentNode.arrayExpression is Reference ->
                    (currentNode.arrayExpression as Reference).access
                else -> null
            }
        if (access in setOf(AccessValues.READ, AccessValues.READWRITE)) {
            val addresses = doubleState.getAddresses(currentNode, currentNode)
            val values =
                doubleState
                    .getValues(currentNode, currentNode)
                    // Filter only the values that are not stored for short FunctionSummaries (aka
                    // it.second set to true)
                    .mapFilteredTo(IdentitySet(), { !it.second }) { it.first }
            val prevDFGs = doubleState.getLastWrites(currentNode)

            // If we have any information from the dereferenced value, we also fetch that (if it's
            // not written to)
            if (
                (passConfig<Configuration>()?.drawCurrentDerefDFG != false) &&
                    (currentNode.astParent as? PointerDereference)?.access != AccessValues.WRITE
            ) {
                values.forEach { value ->
                    // TODO: This probably can be optimized
                    /* If all we have here as a PMV value, we can skip it
                    Note: This only applies to the value, the deref and derefderefvalues
                    might be from different functions, so we leave those */
                    if (doubleState.hasDeclarationStateValueEntry(value, true)) {
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
                                                PointerAccess.CURRENT_DEREF_VALUE
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
                            .filter { derefValue ->
                                doubleState.hasDeclarationStateValueEntry(derefValue)
                            }
                            .forEach { derefValue ->
                                doubleState
                                    .getLastWrites(derefValue)
                                    .filter { it.properties.none { it == true } }
                                    .forEach {
                                        prevDFGs.add(
                                            NodeWithPropertiesKey(
                                                it.node,
                                                equalLinkedHashSetOf(
                                                    PointerDataflowGranularity(
                                                        PointerAccess.CURRENT_DEREF_DEREF_VALUE
                                                    ),
                                                    // Here again, filter the
                                                    // FullDataflowGranularity since
                                                    // we indicate a
                                                    // currentDerefDerefValue
                                                    *it.properties
                                                        .filter { it !is FullDataflowGranularity }
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
            // We write to this node, but we probably want to store the memory address which
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
        // The entries we want to add to the declarationState
        val declEntriesMap = ConcurrentIdentityHashMap<Node, DeclarationStateEntryElement>()
        /* No need to set the address, this already happens in the constructor */
        val addresses =
            if (currentNode is Tuple) {
                currentNode.elements.flatMapTo(IdentitySet()) { doubleState.getAddresses(it, it) }
            } else {
                doubleState.getAddresses(currentNode, currentNode)
            }

        val values = PowersetLattice.Element<NodeWithPropertiesKey>()

        val lastWrites =
            if (currentNode is Tuple) {
                currentNode.elements.mapTo(PowersetLattice.Element()) {
                    NodeWithPropertiesKey(it, equalLinkedHashSetOf<Any>(false))
                }
            } else {
                PowersetLattice.Element(
                    NodeWithPropertiesKey(currentNode, equalLinkedHashSetOf<Any>(false))
                )
            }

        addresses.forEach { addr ->
            declEntriesMap.computeIfAbsent(addr) {
                DeclarationStateEntryElement(
                    PowersetLattice.Element(addresses),
                    PowersetLattice.Element(),
                    lastWrites,
                )
            }
        }

        (currentNode as? HasInitializer)?.initializer?.let { initializer ->
            if (initializer is Literal<*>) {
                values.add(NodeWithPropertiesKey(initializer, equalLinkedHashSetOf()))
                declEntriesMap.forEach {
                    it.value.second.addAll(
                        values.mapTo(PowersetLattice.Element()) { Pair(it.node, false) }
                    )
                }
                // Hacky: Also add the info to the declarationState of the literal so that when we
                // want to draw a prevDFG later we know the "address" (we use the declaration here)
                // and the lastwrite (again, the declaration)
                declEntriesMap.computeIfAbsent(initializer) {
                    DeclarationStateEntryElement(
                        PowersetLattice.Element(addresses),
                        PowersetLattice.Element(),
                        lastWrites,
                    )
                }
            } else {
                // The EOG of Declarations does not play in our favor: We will handle the
                // Declaration before we handled the initializer. So we explicitly handle the
                // initializer before continuing
                val ini = removePossibleCasts(initializer)
                doubleState =
                    when (ini) {
                        is PointerReference -> handleExpression(lattice, ini.input, doubleState)
                        is PointerDereference -> handleExpression(lattice, ini.input, doubleState)
                        // TODO: This will cause us to handle the call twice (also afterwards in the
                        // regular EOG iteration), not sure if this is a problem
                        is Call -> handleCall(lattice, ini, doubleState)
                        else -> handleExpression(lattice, ini, doubleState)
                    }
                if (ini is InitializerList) {
                    // If we have an InitializerList that consists of only assigns, we handled
                    // it via the normal EOG path, so we can ignore it now
                    if (ini.initializers.any { it !is Assign }) {
                        // Create a field for every initializer, i.e. at offset 0 we store the first
                        // element, at offset 1 the second, etc...
                        val fieldAddresses = identitySetOf<Node>()
                        // We only analyze the first 200 elements in order not to have a too large
                        // state
                        for (i in 0..<ini.initializers.size.coerceAtMost(200)) {
                            val fieldVal = ini.initializers[i]
                            val parentName = getNodeName(currentNode)
                            val newName = Name(i.toString(), parentName)
                            doubleState.fetchFieldAddresses(addresses, newName).forEach { fieldAddr
                                ->
                                fieldAddresses.add(fieldAddr)
                                val newEntry =
                                    declEntriesMap.computeIfAbsent(fieldAddr) {
                                        DeclarationStateEntryElement(
                                            PowersetLattice.Element(),
                                            PowersetLattice.Element(),
                                            PowersetLattice.Element(),
                                        )
                                    }
                                newEntry.first.add(fieldAddr)
                                newEntry.second.add(Pair(fieldVal, false))
                                newEntry.third.add(
                                    NodeWithPropertiesKey(fieldVal, equalLinkedHashSetOf())
                                )
                            }
                        }
                        // add the entries for the fieldAddress to the main addresses
                        addresses.forEach { addr ->
                            declEntriesMap[addr]?.first?.addAll(fieldAddresses)
                            // The value of the base is the address of the first element in the
                            // initializer
                            fieldAddresses
                                .singleOrNull { it.name.localName == "0" }
                                ?.let { element0Addr ->
                                    declEntriesMap[addr]?.second?.add(Pair(element0Addr, false))
                                }
                        }
                    } else {
                        // We have assigns in the list, those should have already been handled
                        // All we need to do is to set the correct entry for the base address
                        // If we initialize a struct we don't know, we fetch all fieldAddresses from
                        // the declarationState and set those as values
                        // If we know the record, we set the value to the address of the first field
                        val rd = currentNode.type.root.declaredFrom as? Record
                        rd.fields
                        val fieldAddresses =
                            if (rd == null) {
                                // If we don't have anything at all, all we can do is fetch
                                // fieldAddresses from the declaration state
                                addresses.flatMap { address ->
                                    doubleState.declarationsState[address]?.first?.filter {
                                        it != address
                                    } ?: PowersetLattice.Element()
                                }
                            } else if (rd.isInferred) {
                                // We don't know which field is the first one, so we take all of
                                // them
                                rd.fields.flatMapTo(PowersetLattice.Element()) { field ->
                                    doubleState.fetchFieldAddresses(
                                        addresses,
                                        Name(field.name.localName, currentNode.name),
                                    )
                                }
                            } else {
                                // We know the record so we only take the first address
                                doubleState.fetchFieldAddresses(
                                    addresses,
                                    Name(
                                        rd.fields.firstOrNull()?.name?.localName
                                            ?: rd.name.localName,
                                        currentNode.name,
                                    ),
                                )
                            }
                        addresses.forEach { addr ->
                            declEntriesMap[addr]
                                ?.second
                                ?.addAll(fieldAddresses.map { Pair(it, false) })
                        }
                    }
                } else {
                    values.addAll(
                        doubleState.getValues(ini, ini).mapTo(PowersetLattice.Element()) {
                            NodeWithPropertiesKey(it.first, equalLinkedHashSetOf())
                        }
                    )
                    declEntriesMap.forEach {
                        it.value.second.addAll(
                            values.mapTo(PowersetLattice.Element()) { Pair(it.node, false) }
                        )
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
                    values,
                    PowersetLattice.Element(),
                ),
            )

        /* In the DeclarationsState, we save the address which we wrote to the value for easier work with pointers
         */
        declEntriesMap.forEach {
            doubleState = lattice.pushToDeclarationsState(doubleState, it.key, it.value)
        }
        return doubleState
    }

    /** Create ParameterMemoryValues up to depth `depth` */
    private suspend fun initializeParameter(
        lattice: PointsToState,
        function: Function,
        param: Parameter,
        doubleState: PointsToState.Element,
        // Until which depth do we create ParameterMemoryValues
        depth: Int = 2,
        // Create the PMVs independent of the parameter type
        forceDerefPMVCreation: Boolean = false,
    ): PointsToState.Element {
        var doubleState = doubleState

        // In the first step, we have a triangle of Parameter, the
        // Parameter's Memory Address and the ParameterMemoryValue
        // Therefore, the src and the addresses are different. For all other depths,
        // we set both to the ParameterMemoryValue we create in the first step
        var src: Node = param
        var addresses = doubleState.getAddresses(src, src)
        var prevAddresses: IdentitySet<Node> = identitySetOf()
        // If we have a Pointer as param, we initialize all levels, otherwise, only
        // the first one
        val paramDepth =
            if (
                param.type is PointerType ||
                    forceDerefPMVCreation ||
                    // If the type is unknown we also
                    // initialize all levels to be sure
                    param.type is UnknownType ||
                    // Another guess we take: If the length is the same as the
                    // addressLength, again, to be sure we initialize all levels
                    (param.type as? NumericType)?.bitWidth ==
                        // TODO: passConfig<Configuration> should never be null?
                        (passConfig<Configuration>()?.addressLength ?: 64)
            )
                depth
            else 0
        for (pD in 0..paramDepth) {
            val pmvName = "deref".repeat(pD) + "value"
            // If we force the DerefPMVCreation, we probably create the first PMV already, so let's
            // search it. For this, we check the memoryValues and the
            // state, and if all is null, we create a new PMV
            //            var pmv: ParameterMemoryValue?
            val pmv =
                if (forceDerefPMVCreation && pD == 0) {
                    param.memoryValues.singleOrNull { it.name.localName == pmvName }
                        as? ParameterMemoryValue
                        ?: doubleState
                            .getValues(param, param)
                            .singleOrNull { it.first.name.localName == pmvName }
                            ?.first as? ParameterMemoryValue
                        ?: ParameterMemoryValue(
                            Name(pmvName, Name(param.name.localName, function.name))
                        )
                } else {
                    ParameterMemoryValue(Name(pmvName, Name(param.name.localName, function.name)))
                }
            (src as? MemoryAddress)?.let { pmv.memoryAddresses = mutableSetOf(it) }

            // In the first step, we link the Parameter to the PMV to be
            // able to also access it outside the function
            if (src is Parameter) {
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
                // Additionally, directly add the value to the node. This is necessary on recursive
                // calls, when we are not yet finished with acceptInternal of one function, but we
                // already need to fetch the PMVs because we have a recursive call while handling
                // the function
                src.memoryValueEdges += Dataflow(pmv, src)
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
                // Here again, directly add the value to the param to be able to access it directly
                // instead of having to wait until acceptInternal is finished
                param.memoryValueEdges +=
                    Dataflow(pmv, param, granularity = PartialDataflowGranularity(pmvName))
            }

            // Update the states
            val declStateElement =
                if (src is Parameter)
                    DeclarationStateEntryElement(
                        PowersetLattice.Element(prevAddresses),
                        PowersetLattice.Element(Pair(pmv, false)),
                        PowersetLattice.Element(NodeWithPropertiesKey(src, equalLinkedHashSetOf())),
                    )
                else
                    DeclarationStateEntryElement(
                        PowersetLattice.Element(addresses),
                        PowersetLattice.Element(Pair(pmv, false)),
                        PowersetLattice.Element(NodeWithPropertiesKey(pmv, equalLinkedHashSetOf())),
                    )
            addresses.forEach { addr ->
                doubleState = lattice.pushToDeclarationsState(doubleState, addr, declStateElement)
            }

            prevAddresses = addresses
            src = pmv
            addresses = identitySetOf(pmv)
        }
        return doubleState
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
    newLatticeElement.third.forEachMaybeParallel { existingEntry ->
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
            ConcurrentMapLattice.Element(newNode to newLatticeCopy),
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

    newLatticeElement.second.forEachMaybeParallel { pair ->
        if (
            currentState.declarationsState[newNode]?.second?.any {
                it.first === pair.first && it.second == pair.second
            } == true
        )
            newLatticeCopy.second.remove(pair)
    }

    newLatticeElement.third.forEachMaybeParallel { nwpk ->
        if (currentState.declarationsState[newNode]?.third?.contains(nwpk) == true) {
            newLatticeCopy.third.remove(nwpk)
        }
    }

    this@pushToDeclarationsState.innerLattice2.lub(
        currentState.declarationsState,
        ConcurrentMapLattice.Element(newNode to newLatticeCopy),
        true,
    )
    return@coroutineScope currentState
}

/** Check if `node` has an entry for its value in the DeclarationState */
fun PointsToState.Element.hasDeclarationStateValueEntry(
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
    val lastWrites: ConcurrentIdentitySet<NodeWithPropertiesKey>,
)

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
        // The value of a Function is the Function itself
        if (node is Function) {
            ret.add(
                FetchElementFromDeclarationStateEntry(node, false, "", PowersetLattice.Element())
            )
            return ret
        }

        val element = globalDerefs[node]
        if (element != null)
            element.forEach {
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
            if (node is UnknownMemoryValue) {
                ret.add(
                    FetchElementFromDeclarationStateEntry(
                        node,
                        false,
                        "",
                        PowersetLattice.Element(),
                    )
                )
            } else {
                val newName = getNodeName(node)
                val newEntry =
                    nodesCreatingUnknownValues.computeIfAbsent(Pair(node, newName)) {
                        UnknownMemoryValue(newName, true)
                    }
                // TODO: Check if the boolean should be true sometimes
                globalDerefs.put(node, PowersetLattice.Element(Pair(newEntry, false)))
                ret.add(
                    FetchElementFromDeclarationStateEntry(
                        newEntry,
                        false,
                        "",
                        PowersetLattice.Element(),
                    )
                )
            }
        }
    } else {
        // Otherwise, we read the declarationState.
        // Let's start with the main element
        val declarationEntry = this.declarationsState[node]
        val values = declarationEntry?.second
        val hasUsableValues = values?.any { pair -> !excludeShortFSValues || !pair.second } == true
        if (!hasUsableValues) {
            // If we are already dealing with an UnknownMemoryValue, we simply return that in order
            // to avoid too much looping in the unknown
            if (node is UnknownMemoryValue) {
                ret.add(
                    FetchElementFromDeclarationStateEntry(
                        node,
                        false,
                        "",
                        PowersetLattice.Element(),
                    )
                )
            } else {
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
                    newElements?.none {
                        it.first === newPair.first && it.second == newPair.second
                    } != false
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
            }
        } else {
            val lastWrites = declarationEntry.third
            values.forEach {
                if (excludeShortFSValues && it.second) return@forEach
                ret.add(FetchElementFromDeclarationStateEntry(it.first, it.second, "", lastWrites))
            }
        }

        // if fetchFields is true, we also fetch the values for fields
        // TODO: handle globals
        if (fetchFields) {
            val nodeAddresses = this.getAddresses(node, node)
            this.declarationsState[node]?.first?.forEach { field ->
                if (field === node || nodeAddresses.contains(field)) return@forEach
                this.declarationsState[field]?.second?.forEach {
                    if (excludeShortFSValues && !it.second || !excludeShortFSValues)
                        ret.add(
                            FetchElementFromDeclarationStateEntry(
                                it.first,
                                it.second,
                                field.name.localName,
                                this.declarationsState[field]?.third ?: PowersetLattice.Element(),
                            )
                        )
                }
            }
        }
    }

    return ret
}

private fun copyNodeProperties(properties: Set<Any>): Set<Any> {
    if (properties.isEmpty()) return emptySet()
    return properties
}

private fun PointsToState.Element.collectLastWritesFromAddresses(
    addresses: Iterable<Node>
): PowersetLattice.Element<NodeWithPropertiesKey> {
    val lastWrites = PowersetLattice.Element<NodeWithPropertiesKey>()
    addresses.forEach { address ->
        this.declarationsState[address]?.third?.forEach { entry ->
            lastWrites.add(NodeWithPropertiesKey(entry.node, copyNodeProperties(entry.properties)))
        }
    }
    return lastWrites
}

fun PointsToState.Element.getLastWrites(
    node: Node
): PowersetLattice.Element<NodeWithPropertiesKey> {
    // For pointerReferences, it shouldn't make a difference if they are global or local
    if (node is PointerReference) {
        return when (val input = node.input) {
            is Subscription,
            is MemberAccess -> {
                // For Subscriptions and MemberAccesses, we have to look up an address from the
                // field or from the base,
                // and then we fetch the lastWrites from the declarationState
                // TODO: In C(++), there should be a difference for Subscriptions if the
                // arrayExpression is a pointer
                // or an array, think more about if we also have to make this differentiation here
                val base =
                    if (input is MemberAccess) {
                        input.base
                    } else {
                        (input as Subscription).arrayExpression
                    }
                val fieldName =
                    if (input is MemberAccess) {
                        getNodeName(input.refersTo)
                    } else {
                        getNodeName((input as Subscription).subscriptExpression)
                    }
                val fieldAddresses = fetchFieldAddresses(identitySetOf(base), fieldName)
                // If we have no fieldAddresses, we resort to the base address
                val addresses =
                    if (
                        fieldAddresses.isEmpty() ||
                            fieldAddresses.all { this.declarationsState[it] == null }
                    ) {
                        this.getAddresses(base, base)
                    } else fieldAddresses
                val ret = PowersetLattice.Element<NodeWithPropertiesKey>()
                addresses.forEach { address ->
                    this.declarationsState[address]?.third?.let { ret.addAll(it) }
                }
                ret
            }
            is Reference -> {
                // In case the input is a reference, the lastwrite is its memoryAddress
                getAddresses(input, input).mapTo(PowersetLattice.Element()) {
                    NodeWithPropertiesKey(it, equalLinkedHashSetOf())
                }
            }
            else -> PowersetLattice.Element()
        }
    }
    if (isGlobal(node)) {
        return when (node) {
            is MemberAccess -> {
                // We overapproximate here: For MemberAccesss, we ignore the field and only
                // consider the base
                val (base, _) = resolveMemberAccess(node)
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
        is PointerDereference -> {
            val ret = PowersetLattice.Element<NodeWithPropertiesKey>()
            this.getAddresses(node, node).forEach { addr ->
                if (isGlobal(addr)) {
                    // for globals, the last Write is the Declaration. For Functions, we
                    // find that in the globalDerefs map
                    globalDerefs[addr]?.forEach { entry ->
                        ret.add(
                            NodeWithPropertiesKey(entry.first, equalLinkedHashSetOf(entry.second))
                        )
                    }
                } else {
                    val lastWrite = this.declarationsState[addr]?.third
                    // Usually, we should have a lastwrite, so we take that
                    if (lastWrite?.isNotEmpty() == true)
                        lastWrite.forEach {
                            ret.add(
                                NodeWithPropertiesKey(it.node, copyNodeProperties(it.properties))
                            )
                        }
                    // However, there might be cases were we don't yet have written to the
                    // dereferenced
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
            }
            ret
        }
        is Subscription -> {
            // For Subscriptions, we additionally check if the partial write matches
            val partial = getNodeName(node.subscriptExpression)
            val addresses = this.getAddresses(node, node)
            val ret = PowersetLattice.Element<NodeWithPropertiesKey>()
            addresses.forEach { addr ->
                this.declarationsState[addr]?.third?.forEach { entry ->
                    ret.add(
                        NodeWithPropertiesKey(
                            entry.node,
                            entry.properties.filterTo(EqualLinkedHashSet()) {
                                !(it is PartialDataflowGranularity<*> &&
                                    it.partialTarget is Field &&
                                    it.partialTarget.name.localName == partial.localName)
                            },
                        )
                    )
                }
            }
            ret
        }
        is MemberAccess -> {
            // For MemberAccess, the lastWrite is the Field if we don't have anything
            // else
            val lastWrites = this.collectLastWritesFromAddresses(this.getAddresses(node, node))
            if (lastWrites.isEmpty()) {
                val ref = node.refersTo
                if (ref != null)
                    PowersetLattice.Element(NodeWithPropertiesKey(ref, equalLinkedHashSetOf()))
                else
                // If we don't have a referring declaration, we return the empty set
                lastWrites
            } else lastWrites
        }
        is ParameterMemoryValue -> {
            // For parameterMemoryValues, we have to check if there was a write within the function.
            // If not, it's the deref value itself.
            // For PMVs, we store this information directly for the PMV itself, so no need to fetch
            // its addresses
            // TODO: Unsure if this is consistent?
            val entries = this.declarationsState[node]?.third
            if (!entries.isNullOrEmpty()) entries
            else
                node.memoryValues
                    .filter { it.name.localName == "deref" + node.name.localName }
                    .mapTo(PowersetLattice.Element()) {
                        NodeWithPropertiesKey(it, equalLinkedHashSetOf())
                    }
        }
        else ->
            // For the rest, we read the declarationState to determine when the memoryAddress of the
            // node was last written to
            this.collectLastWritesFromAddresses(this.getAddresses(node, node))
    }
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
            this.getAddresses(node.input, node.input).mapTo(PowersetLattice.Element()) {
                Pair(it, false)
            }
        }
        is PointerDereference -> {
            /* To find the value for PointerDereferences, we first fetch the values form its input from the generalstate, which is probably a MemoryAddress
             * Then we look up the current value at this MemoryAddress
             */
            val inputVals =
                if (node.input is Reference)
                    this.fetchValueFromGeneralState(node.input).map { it.node }
                else this.getValues(node.input, node.input).map { it.first }
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
        is Call -> {
            fetchValueFromDeclarationState(node).mapTo(PowersetLattice.Element()) {
                Pair(it.value, it.shortFS)
            }
        }
        is MemberAccess -> {
            val (base, fieldName) = resolveMemberAccess(node)
            val baseAddresses = getAddresses(base, startNode)
            val fieldAddresses = fetchFieldAddresses(baseAddresses, fieldName)
            if (fieldAddresses.isNotEmpty()) {
                val retVal = PowersetLattice.Element<Pair<Node, Boolean>>()
                fieldAddresses.forEach { fa ->
                    if (hasDeclarationStateValueEntry(fa)) {
                        fetchValueFromDeclarationState(fa).forEach {
                            retVal.add(Pair(it.value, it.shortFS))
                        }
                    } else {
                        // Let's overapproximate here: In case we find no known value for the field,
                        // we try again with the baseAddresses
                        baseAddresses.forEach { ba ->
                            fetchValueFromDeclarationState(ba).forEach {
                                retVal.add(Pair(it.value, it.shortFS))
                            }
                        }
                    }
                }
                retVal
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
                if (isGlobal(node)) {
                    retVals.addAll(
                        fetchValueFromDeclarationState(addr).map { Pair(it.value, false) }
                    )
                } else {
                    this.getValues(addr, startNode).forEach { v ->
                        // We want to skip values that contain the node itself and therefore could
                        // cause a loop
                        // So we fetch parent Assign of the value in the AST, and if the
                        // node is any of its children, we skip that value
                        var valueParentAssign: Node? = v.first
                        while (
                            valueParentAssign !is Assign && valueParentAssign != null
                        ) valueParentAssign = valueParentAssign.astParent
                        if (node !in SubgraphWalker.flattenAST(valueParentAssign)) retVals.add(v)
                    }
                }
            }
            retVals
        }
        is Cast -> {
            this.getValues(node.expression, startNode)
        }
        is Subscription -> {
            this.getAddresses(node, startNode).flatMapTo(PowersetLattice.Element()) {
                this.getValues(it, it)
            }
        }
        // For BinaryOperators, UnaryOperators, Literals etc. we'll end up here
        // For those, we define that they are the values of themselves
        // However, if we have a DeclarationState entry, we will return this one.
        // This can be for example the can when a Literal depicts and address and the
        // program writes something to this address
        else ->
            if (hasDeclarationStateValueEntry(node)) {
                this.fetchValueFromDeclarationState(node).mapTo(PowersetLattice.Element()) {
                    Pair(it.value, it.shortFS)
                }
            } else {
                PowersetLattice.Element(Pair(node, false))
            }
    }
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

            if (ret.isNotEmpty()) ret
            else this.declarationsState[node]?.first?.toIdentitySet() ?: identitySetOf()
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
                // Subscription for now
                // We assume that the rhs of the BinaryOperator is the pointer, and the rhs is a
                // literal that describes the offset
                if (
                    value is BinaryOperator &&
                        value.operatorCode == "+" &&
                        (value.lhs is Reference || value.lhs is Cast) &&
                        value.rhs is Literal<*>
                ) {
                    val sub = Subscription()
                    sub.arrayExpression = removePossibleCasts(value.lhs)
                    sub.subscriptExpression = value.rhs
                    ret.addAll(getAddresses(sub, startNode))
                } else ret.add(value)
            }
            ret
        }
        is MemberAccess -> {
            /*
             * For MemberAccesss, the fieldAddresses in the MemoryAddress node of the base hold the information we are looking for
             */
            val (base, newName) = resolveMemberAccess(node)
            fetchFieldAddresses(this.getAddresses(base, startNode), newName)
        }
        is Reference -> {
            /*
             * For references, the address is the same as for the declaration, AKA the refersTo
             */
            node.refersTo?.let { refersTo ->
                /* If the refersTo is a Function, its address is the Function itself */
                if (refersTo is Function) {
                    globalDerefs.put(refersTo, PowersetLattice.Element(Pair(refersTo, false)))
                    identitySetOf(refersTo)
                } else {
                    /* In some cases, the refersTo might not yet have an initialized MemoryAddress, for example if it's a Function. So let's do this here */
                    if (refersTo.memoryAddresses.isEmpty()) {
                        val newAddress = MemoryAddress(node.name, isGlobal(node))
                        refersTo.memoryAddresses += newAddress
                    }

                    refersTo.memoryAddresses.toIdentitySet()
                }
            } ?: identitySetOf()
        }
        is Cast -> {
            /*
             * For casts, we take the expression as the cast itself does not have any impact on the address
             */
            this.getAddresses(node.expression, startNode)
        }
        is Subscription -> {
            val localName = getNodeName(node.subscriptExpression)
            if (localName.localName == "0") {
                return this.getValues(node.base, startNode).mapTo(IdentitySet()) { it.first }
            }
            // When startNode is different from the current node, we should already have an entry,
            // so we fetch that from the general state
            // TODO: Should we skip dereferencing if the base is not a pointer (The C(++) compiler
            // does that), or does it make no difference in our abstract model?
            val baseValues =
                if (startNode != node)
                    fetchValueFromGeneralState(node.base).mapTo(IdentitySet()) { it.node }
                else this.getValues(node.base, startNode).mapTo(IdentitySet()) { it.first }
            baseValues.flatMapTo(identitySetOf()) { node ->
                fetchFieldAddresses(
                    identitySetOf(node),
                    Name(localName.localName, getNodeName(node)),
                )
            }
        }
        // TODO: This should work for all HasMemoryAddresses
        //        is HasMemoryAddress -> {
        is BinaryOperator -> {
            if (node.memoryAddresses.isEmpty()) {
                node.memoryAddresses += MemoryAddress(node.name, isGlobal(node))
            }
            node.memoryAddresses.toIdentitySet()
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
    val skipInitialFetch =
        if (node !is PointerReference && onlyFetchExistingEntries) {
            val hasDirectAddressEntries =
                this.getAddresses(node, node).any { addr ->
                    this.hasDeclarationStateValueEntry(addr, excludeShortFSValues)
                }
            if (hasDirectAddressEntries) {
                false
            } else {
                var hasBaseEntries = false
                collectBasesAndOffsets(node).forEach { (base, _) ->
                    if (hasBaseEntries) return@forEach
                    if (
                        this.getAddresses(base, base).any { addr ->
                            this.hasDeclarationStateValueEntry(addr, excludeShortFSValues)
                        }
                    ) {
                        hasBaseEntries = true
                    }
                }
                !hasBaseEntries
            }
        } else {
            false
        }
    var ret = PowersetLattice.Element<Pair<Node, Boolean>>()
    if (!skipInitialFetch) {
        getValues(node, node).forEach {
            if (!excludeShortFSValues || !it.second) {
                ret.add(it)
            }
        }
    }
    for (i in 1..<nestingDepth) {
        val next = PowersetLattice.Element<Pair<Node, Boolean>>()
        ret.forEach {
            if (
                !onlyFetchExistingEntries ||
                    this.hasDeclarationStateValueEntry(it.first, excludeShortFSValues)
            ) {
                this.fetchValueFromDeclarationState(it.first, fetchFields, excludeShortFSValues)
                    .forEach { fetched -> next.add(Pair(fetched.value, fetched.shortFS)) }
            }
        }
        ret = next
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

/*
Takes a set of basenodes and fetches the field addresses if any
*/
fun PointsToState.Element.fetchFieldAddresses(
    baseAddresses: IdentitySet<Node>,
    nodeName: Name,
): IdentitySet<Node> {
    val fieldAddresses = identitySetOf<Node>()
    // Widen very deep access paths to a stable summary segment.
    val normalizedNodeName = normalizeFieldAccessPath(nodeName)

    baseAddresses.forEach { addr ->
        var foundAnyFieldAddress = false
        declarationsState[addr]?.first?.forEach { candidate ->
            if (candidate.name.localName == normalizedNodeName.localName) {
                fieldAddresses.add(candidate)
                foundAnyFieldAddress = true
            }
        }

        if (!foundAnyFieldAddress) {
            val newEntry =
                nodesCreatingUnknownValues.computeIfAbsent(Pair(addr, normalizedNodeName)) {
                    MemoryAddress(normalizedNodeName, isGlobal(addr))
                }

            // No need to update the state for values we don't know anyway
            if (addr !is UnknownMemoryValue) {
                val declarationEntry =
                    this.declarationsState.computeIfAbsent(addr) {
                        TripleLattice.Element(
                            PowersetLattice.Element(addr),
                            PowersetLattice.Element(),
                            PowersetLattice.Element(),
                        )
                    }
                declarationEntry.first.add(newEntry)
            }
            fieldAddresses.add(newEntry)
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
    sources: PowersetLattice.Element<Triple<Node?, Boolean, Any?>>,
    destinations: IdentitySet<Node>,
    // Node and short FS yes or no
    destinationAddresses: IdentitySet<Node>,
    lastWrites: MutableSet<NodeWithPropertiesKey>,
): PointsToState.Element = coroutineScope {
    var doubleState = doubleState

    val noDestinationWithBody =
        destinations.none { it is Call && it.invokes.singleOrNull()?.body == null }

    val mappedSources =
        sources.mapNotNullTo(PowersetLattice.Element()) {
            it.first?.let { first ->
                NodeWithPropertiesKey(first, equalLinkedHashSetOf<Any>().apply { add(it.second) })
            }
        }

    /* Update the declarationState for the addresses */
    destinationAddresses.forEach { destAddr ->
        if (!isGlobal(destAddr)) {
            val currentEntries =
                this@updateValues.declarationsState[destAddr]?.first?.toIdentitySet()
                    ?: identitySetOf(destAddr)

            // If we want to update the State with exactly the same elements as are already in the
            // state, we do nothing in order not to confuse the iterateEOG function
            val newSources: PowersetLattice.Element<Pair<Node, Boolean>> =
                sources.mapNotNullTo(PowersetLattice.Element()) { triple ->
                    val existingPair =
                        this@updateValues.declarationsState[destAddr]?.second?.firstOrNull {
                            it.first === triple.first && it.second == triple.second
                        } ?: Pair(triple.first, triple.second)
                    existingPair.first?.let { first -> Pair(first, existingPair.second) }
                }

            // TODO: Do we also need to fetch some properties here?
            // If we already have exactly this value in the state for the prevDFGs, we take that in
            // order not to confuse the iterateEOG function
            val prevDFG: MutableSet<NodeWithPropertiesKey> = ConcurrentHashMap.newKeySet()
            lastWrites.forEachMaybeParallel { lw ->
                val existingEntries =
                    doubleState.declarationsState[destAddr]?.third?.filter { entry -> entry == lw }
                if (existingEntries?.isNotEmpty() == true) prevDFG.addAll(existingEntries)
                else prevDFG.add(lw)
            }

            // Check if we have any full writes. This is the case if a source has as third element
            // null, AKA does not write to a field
            val fullSourcesExist = sources.any { it.third == null }

            // If we have any full writes, we eliminate the previous state
            if (fullSourcesExist) {
                doubleState.declarationsState.put(
                    destAddr,
                    DeclarationStateEntryElement(
                        PowersetLattice.Element(currentEntries),
                        PowersetLattice.Element(newSources),
                        PowersetLattice.Element(prevDFG),
                    ),
                )
            } else {
                // If the write is to a field, we check if we have any writes to the same field and
                // remove those
                // Note: If it's not to a field, we keep it, it is probably something like
                // Method.add()
                // TODO: this should be fields, but for now we deal with the names
                val writtenFields =
                    sources.mapNotNullTo(HashSet()) { (it.third as? Field)?.name?.localName }
                doubleState.declarationsState[destAddr]?.third?.removeIf {
                    it.properties.any { p ->
                        ((p as? PartialDataflowGranularity<*>)?.partialTarget as? Field)
                            ?.name
                            ?.localName in writtenFields
                    }
                }

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
            // Except for Calls w/o invokes body for which we have to do this to create
            // the short FS paths
            val newLastWrites: MutableSet<NodeWithPropertiesKey> = ConcurrentHashMap.newKeySet()
            newLastWrites.addAll(lastWrites)

            newLastWrites.forEachMaybeParallel { lw ->
                if (
                    noDestinationWithBody &&
                        (sources.any { src ->
                            src.first === lw.node && src.second in lw.properties
                        } || lw.node in destinations)
                ) {
                    newLastWrites.remove(lw)
                }
            }

            destinations.forEachMaybeParallel { d ->
                doubleState.generalState.put(
                    d,
                    GeneralStateEntryElement(
                        PowersetLattice.Element(destinationAddresses),
                        PowersetLattice.Element(mappedSources),
                        PowersetLattice.Element(newLastWrites),
                    ),
                )
            }
        } else {
            val mappedSources =
                sources.mapNotNullTo(IdentitySet()) {
                    it.first?.let { first ->
                        NodeWithPropertiesKey(
                            first,
                            equalLinkedHashSetOf<Any>().apply { add(it.second) },
                        )
                    }
                }
            // For globals, we draw a DFG Edge from the source to the destination
            destinations.forEachMaybeParallel { d ->
                val entry =
                    doubleState.generalState.computeIfAbsent(d) {
                        GeneralStateEntryElement(
                            PowersetLattice.Element(),
                            PowersetLattice.Element(),
                            PowersetLattice.Element(),
                        )
                    }
                entry.third.addAll(mappedSources)
            }
        }
    }

    return@coroutineScope doubleState
}
