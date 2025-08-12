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
package de.fraunhofer.aisec.cpg.analysis.abstracteval

import de.fraunhofer.aisec.cpg.analysis.abstracteval.value.*
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder
import de.fraunhofer.aisec.cpg.graph.firstParentOrNull
import de.fraunhofer.aisec.cpg.helpers.functional.*
import de.fraunhofer.aisec.cpg.passes.objectIdentifier
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.putAll
import kotlin.collections.set
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.to
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger(AbstractIntervalEvaluator::class.java)

class TupleState<NodeId>(
    innerLattice1: DeclarationState<NodeId>,
    innerLattice2: Lattice<NewIntervalStateElement>,
) :
    TupleLattice<DeclarationState.DeclarationStateElement<NodeId>, NewIntervalStateElement>(
        innerLattice1 as Lattice<DeclarationState.DeclarationStateElement<NodeId>>,
        innerLattice2,
    ) {

    override fun lub(
        one: TupleStateElement<NodeId>,
        two: TupleStateElement<NodeId>,
        allowModify: Boolean,
        widen: Boolean,
    ): TupleStateElement<NodeId> {
        return if (allowModify) {
            innerLattice1.lub(one = one.first, two = two.first, allowModify = true, widen = widen)
            innerLattice2.lub(one = one.second, two = two.second, allowModify = true, widen = false)
            one
        } else {
            Element(
                innerLattice1.lub(
                    one = one.first,
                    two = two.first,
                    allowModify = false,
                    widen = widen,
                ),
                innerLattice2.lub(
                    one = one.second,
                    two = two.second,
                    allowModify = false,
                    widen = false,
                ),
            )
        }
    }
}

typealias TupleStateElement<NodeId> =
    TupleLattice.Element<DeclarationState.DeclarationStateElement<NodeId>, NewIntervalStateElement>

class DeclarationState<NodeId>(innerLattice: Lattice<NewIntervalLattice.Element>) :
    MapLattice<NodeId, NewIntervalLattice.Element>(innerLattice) {
    override val bottom: DeclarationStateElement<NodeId>
        get() = DeclarationStateElement()

    override fun lub(
        one: Element<NodeId, NewIntervalLattice.Element>,
        two: Element<NodeId, NewIntervalLattice.Element>,
        allowModify: Boolean,
        widen: Boolean,
    ): Element<NodeId, NewIntervalLattice.Element> {
        val result = super.lub(one, two, allowModify, widen)
        if (result is DeclarationStateElement<NodeId>) {
            // If the result is a DeclarationStateElement, we can return it directly
            return result
        } else {
            return DeclarationStateElement<NodeId>(result)
        }
    }

    override fun glb(
        one: Element<NodeId, NewIntervalLattice.Element>,
        two: Element<NodeId, NewIntervalLattice.Element>,
    ): Element<NodeId, NewIntervalLattice.Element> {
        val result = super.glb(one, two)
        if (result is DeclarationStateElement<NodeId>) {
            // If the result is a DeclarationStateElement, we can return it directly
            return result
        } else {
            return DeclarationStateElement<NodeId>(result)
        }
    }

    class DeclarationStateElement<NodeId>(expectedMaxSize: Int) :
        Element<NodeId, NewIntervalLattice.Element>(expectedMaxSize) {
        constructor() : this(32)

        constructor(m: Map<NodeId, NewIntervalLattice.Element>) : this(m.size) {
            putAll(m)
        }

        constructor(
            entries: Collection<Pair<NodeId, NewIntervalLattice.Element>>
        ) : this(entries.size) {
            putAll(entries)
        }

        constructor(vararg entries: Pair<NodeId, NewIntervalLattice.Element>) : this(entries.size) {
            putAll(entries)
        }

        override fun equals(other: Any?): Boolean {
            return other is DeclarationStateElement<NodeId> && this.compare(other) == Order.EQUAL
        }

        override fun hashCode(): Int {
            return super.hashCode()
        }

        override fun duplicate(): DeclarationStateElement<NodeId> {
            return DeclarationStateElement(
                this.map { (k, v) -> Pair<NodeId, NewIntervalLattice.Element>(k, v.duplicate()) }
            )
        }

        fun findKey(nodeId: NodeId): NodeId {
            return if (nodeId is Integer) {
                this.entries.singleOrNull { it.key == nodeId }?.key ?: nodeId
            } else nodeId
        }

        override fun containsKey(key: NodeId?): Boolean {
            return if (key is Integer) {
                this.entries.singleOrNull { it.key == key } != null
            } else {
                super.containsKey(key)
            }
        }

        override fun put(
            key: NodeId?,
            value: NewIntervalLattice.Element?,
        ): NewIntervalLattice.Element? {
            val actualKey = key?.let { findKey(it) }
            return super.put(actualKey, value)
        }

        /**
         * Retrieves the interval element for the given [nodeId] from the declaration state element.
         *
         * @param nodeId The identifier of the node.
         * @return The [NewIntervalLattice.Element] for the node, or null if not found.
         */
        override operator fun get(nodeId: NodeId): NewIntervalLattice.Element? {
            return if (nodeId is Integer) {
                this.entries.singleOrNull { it.key == nodeId }?.value
            } else {
                super.get(nodeId)
            }
        }
    }
}

typealias NewIntervalState = MapLattice<Node, NewIntervalLattice.Element>

typealias NewIntervalStateElement = MapLattice.Element<Node, NewIntervalLattice.Element>

class NewIntervalLattice() :
    Lattice<NewIntervalLattice.Element>,
    HasWidening<NewIntervalLattice.Element>,
    HasNarrowing<NewIntervalLattice.Element> {
    override var elements: Set<Element> = setOf()
    override val bottom: Element = Element(LatticeInterval.BOTTOM)

    override fun lub(one: Element, two: Element, allowModify: Boolean, widen: Boolean): Element {
        val oneElem = one.element
        val twoElem = two.element
        if (allowModify) {
            when {
                widen -> {
                    one.element = twoElem.widen(oneElem)
                }
                oneElem == LatticeInterval.TOP || twoElem == LatticeInterval.BOTTOM -> {
                    // Nothing to do as one is already the top or bigger than two.
                }
                twoElem == LatticeInterval.TOP -> {
                    // Set one to TOP too.
                    one.element = LatticeInterval.TOP
                }
                oneElem == LatticeInterval.BOTTOM -> {
                    // Set one to two as it is the bottom.
                    one.element = twoElem
                }
                oneElem is LatticeInterval.Bounded && twoElem is LatticeInterval.Bounded -> {
                    // If both are bounded, we can calculate the new bounds.
                    val newLower = minOf(oneElem.lower, twoElem.lower)
                    val newUpper = maxOf(oneElem.upper, twoElem.upper)
                    one.element = LatticeInterval.Bounded(newLower, newUpper)
                }
                else -> {
                    log.warn(
                        "Cannot handle this case in NewIntervalLattice.lub: $oneElem and $twoElem"
                    )
                    Element(LatticeInterval.TOP)
                }
            }
            return one
        } else {
            return when {
                widen -> {
                    Element(twoElem.widen(oneElem))
                }
                oneElem == LatticeInterval.TOP || twoElem == LatticeInterval.TOP -> {
                    Element(LatticeInterval.TOP)
                }
                twoElem == LatticeInterval.BOTTOM -> {
                    one.duplicate()
                }
                oneElem == LatticeInterval.BOTTOM -> {
                    two.duplicate()
                }
                oneElem is LatticeInterval.Bounded && twoElem is LatticeInterval.Bounded -> {
                    val newLower = minOf(oneElem.lower, twoElem.lower)
                    val newUpper = maxOf(oneElem.upper, twoElem.upper)
                    Element(LatticeInterval.Bounded(newLower, newUpper))
                }
                else -> {
                    log.warn(
                        "Cannot handle this case in NewIntervalLattice.lub: $oneElem and $twoElem"
                    )
                    Element(LatticeInterval.TOP)
                }
            }
        }
    }

    override fun glb(one: Element, two: Element): Element {
        val oneElem = one.element
        val twoElem = two.element
        return when {
            oneElem == LatticeInterval.TOP && twoElem == LatticeInterval.TOP -> {
                Element(LatticeInterval.TOP)
            }

            oneElem == LatticeInterval.BOTTOM || twoElem == LatticeInterval.BOTTOM -> {
                Element(LatticeInterval.BOTTOM)
            }
            oneElem == LatticeInterval.TOP -> {
                two.duplicate()
            }
            twoElem == LatticeInterval.TOP -> {
                one.duplicate()
            }

            oneElem is LatticeInterval.Bounded && twoElem is LatticeInterval.Bounded -> {
                val newLower = maxOf(oneElem.lower, twoElem.lower)
                val newUpper = minOf(oneElem.upper, twoElem.upper)
                Element(LatticeInterval.Bounded(newLower, newUpper))
            }

            else -> {
                log.warn("Cannot handle this case in NewIntervalLattice.glb: $oneElem and $twoElem")
                Element(LatticeInterval.TOP)
            }
        }
    }

    override fun compare(one: Element, two: Element): Order {
        return one.compare(two)
    }

    override fun duplicate(one: Element): Element {
        return one.duplicate()
    }

    override fun widen(one: Element, two: Element): Element {
        return Element(one.element.widen(two.element))
    }

    override fun narrow(one: Element, two: Element): Element {
        return Element(one.element.narrow(two.element))
    }

    class Element(var element: LatticeInterval) : Lattice.Element {
        override fun toString(): String {
            return "IntervalLattice.Element(elements=$element)"
        }

        override fun compare(other: Lattice.Element): Order {
            //            var ret: Order
            //            runBlocking { ret = innerCompare(other) }
            //            return ret
            //        }
            //
            //        override suspend fun innerCompare(other: Lattice.Element): Order {
            if (other !is Element) {
                throw IllegalArgumentException("Cannot compare IntervalLattice.Element with $other")
            }
            val thisBounded = this.element as? LatticeInterval.Bounded
            val otherBounded = other.element as? LatticeInterval.Bounded
            return when {
                this.element is LatticeInterval.TOP && other.element is LatticeInterval.TOP ->
                    Order.EQUAL
                this.element is LatticeInterval.BOTTOM && other.element is LatticeInterval.BOTTOM ->
                    Order.EQUAL
                this.element is LatticeInterval.TOP -> Order.GREATER
                other.element is LatticeInterval.TOP -> Order.LESSER
                this.element is LatticeInterval.BOTTOM -> Order.LESSER
                other.element is LatticeInterval.BOTTOM -> Order.GREATER
                thisBounded != null &&
                    thisBounded.lower == otherBounded?.lower &&
                    thisBounded.upper == otherBounded.upper -> Order.EQUAL
                thisBounded != null &&
                    otherBounded != null &&
                    thisBounded.lower >= otherBounded.lower &&
                    thisBounded.upper <= otherBounded.upper -> Order.LESSER
                thisBounded != null &&
                    otherBounded != null &&
                    thisBounded.lower <= otherBounded.lower &&
                    thisBounded.upper >= otherBounded.upper -> Order.GREATER
                else -> Order.UNEQUAL
            }
        }

        override fun duplicate(): Element {
            return Element(this.element) // TODO: Implement a deep copy!
        }
    }
}

/**
 * Performs abstract interval analysis for a single [Value] in the code property graph (CPG).
 *
 * This evaluator computes the interval (lower and upper bounds) of a value at a specific program
 * point by traversing the EOG (Execution Order Graph) from the value's declaration to the target
 * node. It uses a tuple lattice to track both declaration-specific and general interval state.
 *
 * Typical usage:
 * ```kotlin
 * val evaluator = AbstractIntervalEvaluator()
 * val interval = evaluator.evaluate(node, MyValueType::class)
 * ```
 */
class AbstractIntervalEvaluator {
    /** The type of value being analyzed. Set during evaluation. */
    private lateinit var analysisType: KClass<out Value<LatticeInterval>>

    /**
     * Evaluates the interval of a value at the given [node], using the specified [targetType].
     *
     * @param node The node whose value interval is to be evaluated (e.g., a reference).
     * @param targetType The [Value] type to analyze.
     * @return The computed [LatticeInterval] for the value at this node, or
     *   [LatticeInterval.BOTTOM] if not found.
     */
    fun evaluate(node: Node, targetType: KClass<out Value<LatticeInterval>>): LatticeInterval {
        val startNode =
            node.firstParentOrNull<FunctionDeclaration>() ?: return LatticeInterval.BOTTOM
        return evaluate(startNode, node, targetType, LatticeInterval.BOTTOM)
    }

    /**
     * Evaluates the interval of a value at [targetNode], starting from [start], with the given
     * [type] and initial [interval].
     *
     * @param start The node where analysis begins (typically the variable's declaration).
     * @param targetNode The node at which to compute the value's interval.
     * @param type The [Value] type to analyze.
     * @param interval The initial interval value (default: [LatticeInterval.BOTTOM]).
     * @return The computed [LatticeInterval] for the value at [targetNode].
     */
    fun evaluate(
        start: Node,
        targetNode: Node,
        type: KClass<out Value<LatticeInterval>>,
        interval: LatticeInterval = LatticeInterval.BOTTOM, // TODO: Maybe should be top?
    ): LatticeInterval {
        analysisType = type
        val declarationState = DeclarationState<Any>(NewIntervalLattice())
        val intervalState = NewIntervalState(NewIntervalLattice())
        val startState = TupleState(declarationState, intervalState)

        // evaluate effect of each operation on the list until we reach "node"
        val startStateElement = startState.bottom
        val startInterval = startStateElement.second
        intervalState.push(startInterval, start, interval)
        declarationState.push(startStateElement.first, start, interval)

        val finalState =
            startState.iterateEOG(
                start.nextEOGEdges,
                startStateElement,
                ::handleNode,
                strategy = Lattice.Strategy.WIDENING_NARROWING,
            )
        return finalState.second.get(targetNode)?.element ?: LatticeInterval.BOTTOM
    }

    /**
     * Handles the effect of a node during EOG traversal, updating the analysis state. This function
     * changes the state depending on the current node. This is the handler used in `iterateEOG` to
     * correctly handle complex statements.
     *
     * @param lattice The tuple lattice representing current analysis state.
     * @param currentEdge The current EOG edge being processed.
     * @param currentState The current tuple state element.
     * @return The updated tuple state element after applying the node's effect.
     */
    private fun handleNode(
        lattice: Lattice<TupleStateElement<Any>>,
        currentEdge: EvaluationOrder,
        currentState: TupleStateElement<Any>,
    ): TupleStateElement<Any> {
        val currentNode = currentEdge.end
        val newState = currentState

        analysisType
            .createInstance()
            .applyEffect(
                lattice = lattice as TupleState<Any>,
                state = newState,
                node = currentNode,
                edge = currentEdge,
            )

        return newState
    }
}

/**
 * Retrieves the interval value for the given [node] from this tuple state element.
 *
 * @param node The [Node] whose interval is to be fetched.
 * @return The [LatticeInterval] associated with the node, or [LatticeInterval.TOP] if not found.
 */
fun <NodeId> TupleStateElement<NodeId>.intervalOf(node: Node): LatticeInterval {
    val id =
        node.objectIdentifier()?.let { tmpId ->
            this.first.keys.singleOrNull { it == tmpId } ?: (tmpId as? NodeId)
        } ?: node as? NodeId ?: TODO()
    return this.first[id]?.element ?: LatticeInterval.TOP
}

/**
 * Updates the declaration state for [node] with the specified [interval] in the current tuple state
 * element. It overwrites the existing interval for the node if it exists, or adds a new entry if it
 * does not. It does not compute `lub` with the existing entry!
 *
 * @param current The current tuple state element.
 * @param node The [Node] whose declaration state is to be updated.
 * @param interval The new [LatticeInterval] to set.
 * @return The updated tuple state element.
 */
fun <NodeId> TupleState<NodeId>.changeDeclarationState(
    current: TupleStateElement<NodeId>,
    node: Node,
    interval: LatticeInterval,
): TupleStateElement<NodeId> {
    val id =
        (node.objectIdentifier() as? NodeId)?.let { tmpId ->
            current.first.keys.singleOrNull { it == tmpId } ?: tmpId
        } ?: node as NodeId ?: TODO()
    current.first[id] = NewIntervalLattice.Element(interval)
    return current
}

/**
 * Pushes a new interval for [node] into the declaration state, merging with the existing state.
 *
 * @param current The current tuple state element.
 * @param node The [Node] to update.
 * @param interval The [LatticeInterval] to push.
 * @return The updated tuple state element.
 */
fun <NodeId> TupleState<NodeId>.pushToDeclarationState(
    current: TupleStateElement<NodeId>,
    node: Node,
    interval: LatticeInterval,
): TupleStateElement<NodeId> {
    val id =
        (node.objectIdentifier() as? NodeId)?.let { tmpId ->
            current.first.keys.singleOrNull { it == tmpId } ?: tmpId
        } ?: node as NodeId ?: TODO()
    this.innerLattice1.lub(
        current.first,
        DeclarationState.DeclarationStateElement(id to NewIntervalLattice.Element(interval)),
        allowModify = true,
    )
    return current
}

/**
 * Pushes a new interval for [node] into the general state, merging with the existing state.
 *
 * @param current The current tuple state element.
 * @param node The [Node] to update.
 * @param interval The [LatticeInterval] to push.
 * @return The updated tuple state element.
 */
fun <NodeId> TupleState<NodeId>.pushToGeneralState(
    current: TupleStateElement<NodeId>,
    node: Node,
    interval: LatticeInterval,
): TupleStateElement<NodeId> {
    this.innerLattice2.lub(
        current.second,
        NewIntervalStateElement(node to NewIntervalLattice.Element(interval)),
        allowModify = true,
    )
    return current
}

/**
 * Pushes a new interval for [start] into the declaration state lattice.
 *
 * @param current The current declaration state element.
 * @param start The node identifier to update.
 * @param interval The [LatticeInterval] to push.
 */
private fun <NodeId> DeclarationState<NodeId>.push(
    current: DeclarationState.DeclarationStateElement<NodeId>,
    start: NodeId,
    interval: LatticeInterval,
) {
    this.lub(
        current,
        DeclarationState.DeclarationStateElement(start to NewIntervalLattice.Element(interval)),
        allowModify = true,
    )
}

/**
 * Pushes a new interval for [start] into the general interval state lattice.
 *
 * @param current The current interval state element.
 * @param start The [Node] to update.
 * @param interval The [LatticeInterval] to push.
 */
private fun NewIntervalState.push(
    current: NewIntervalStateElement,
    start: Node,
    interval: LatticeInterval,
) {
    this.lub(
        current,
        NewIntervalStateElement(start to NewIntervalLattice.Element(interval)),
        allowModify = true,
    )
}
