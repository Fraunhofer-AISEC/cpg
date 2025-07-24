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
import de.fraunhofer.aisec.cpg.helpers.functional.TupleLattice.Element
import de.fraunhofer.aisec.cpg.passes.objectIdentifier
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.to

class TupleState<NodeId>(
    innerLattice1: Lattice<DeclarationStateElement<NodeId>>,
    innerLattice2: Lattice<NewIntervalStateElement>,
) :
    TupleLattice<DeclarationStateElement<NodeId>, NewIntervalStateElement>(
        innerLattice1,
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
    TupleLattice.Element<DeclarationStateElement<NodeId>, NewIntervalStateElement>

typealias DeclarationState<NodeId> = MapLattice<NodeId, NewIntervalLattice.Element>

typealias DeclarationStateElement<NodeId> = MapLattice.Element<NodeId, NewIntervalLattice.Element>

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
                    TODO("Cannot handle this case: $oneElem and $twoElem")
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
                else -> TODO("Not yet implemented")
            }
        }
    }

    override fun glb(one: Element, two: Element): Element {
        TODO("Not yet implemented")
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
            return Element(this.element) // TODO: Implement a deep copy
        }
    }
}

/**
 * An evaluator performing abstract evaluation for a singular [Value]. It takes a target [Node] and
 * walks back to its Declaration. From there it uses the [Worklist] to traverse the EOG graph until
 * it reaches the node. All statements encountered may influence the result as implemented in the
 * respective [Value] class. The result is a [LatticeInterval] defining both a lower and upper bound
 * for the final value.
 */
class NewAbstractIntervalEvaluator {
    /** The type of the value we are analyzing */
    private lateinit var analysisType: KClass<out Value<LatticeInterval>>

    /**
     * Takes a node (e.g. Reference) and tries to evaluate its value at this point in the program.
     */
    fun evaluate(node: Node, targetType: KClass<out Value<LatticeInterval>>): LatticeInterval {
        val startNode =
            node.firstParentOrNull<FunctionDeclaration>() ?: return LatticeInterval.BOTTOM
        return evaluate(startNode, node, targetType, LatticeInterval.BOTTOM)
    }

    /**
     * Takes a manual configuration and tries to evaluate the value of the node at the end.
     *
     * @param start The beginning of the analysis, usually the start of the target's life
     * @param targetNode The place at which we want to know the target's value
     * @param type The Type of the target
     * @param interval The starting value of the analysis, optional
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
     * This function changes the state depending on the current node. This is the handler used in
     * _iterateEOG_ to correctly handle complex statements.
     *
     * @param lattice The [State] lattice to use for the current evaluation
     * @param currentEdge The current [EvaluationOrder] edge to handle
     * @param currentState The state to use for the current evaluation
     * @return The updated state after handling the current node
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

fun <NodeId> TupleStateElement<NodeId>.intervalOf(node: Node): LatticeInterval {
    val id =
        node.objectIdentifier()?.let { tmpId ->
            this.first.keys.singleOrNull { it == tmpId } ?: (tmpId as? NodeId)
        } ?: node as? NodeId ?: TODO()
    return this.first[id]?.element ?: LatticeInterval.TOP
}

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
        DeclarationStateElement(id to NewIntervalLattice.Element(interval)),
        allowModify = true,
    )
    return current
}

operator fun DeclarationStateElement<Any>.get(nodeId: Any): NewIntervalLattice.Element? {
    return if (nodeId is Integer) {
        this.entries.singleOrNull { it.key == nodeId }?.value
    } else {
        this.entries.singleOrNull { it.key === nodeId }?.value
    }
}

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

private fun <NodeId> DeclarationState<NodeId>.push(
    current: DeclarationStateElement<NodeId>,
    start: NodeId,
    interval: LatticeInterval,
) {
    this.lub(
        current,
        DeclarationStateElement(start to NewIntervalLattice.Element(interval)),
        allowModify = true,
    )
}

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
