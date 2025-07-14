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
import de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.helpers.functional.*
import de.fraunhofer.aisec.cpg.passes.objectIdentifier
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.to

typealias TupleState<NodeId> =
    TupleLattice<DeclarationStateElement<NodeId>, NewIntervalStateElement>

typealias TupleStateElement<NodeId> =
    TupleLattice.Element<DeclarationStateElement<NodeId>, NewIntervalStateElement>

typealias DeclarationState<NodeId> = MapLattice<NodeId, NewIntervalLattice.Element>

typealias DeclarationStateElement<NodeId> = MapLattice.Element<NodeId, NewIntervalLattice.Element>

typealias NewIntervalState = MapLattice<Node, NewIntervalLattice.Element>

typealias NewIntervalStateElement = MapLattice.Element<Node, NewIntervalLattice.Element>

class NewIntervalLattice() : Lattice<NewIntervalLattice.Element> {
    override var elements: Set<Element> = setOf()
    override val bottom: Element = Element(LatticeInterval.BOTTOM)

    override fun lub(one: Element, two: Element, allowModify: Boolean): Element {
        TODO("Not yet implemented")
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

    class Element(val element: LatticeInterval) : Lattice.Element {
        override fun toString(): String {
            return "IntervalLattice.Element(elements=$element)"
        }

        override fun compare(other: Lattice.Element): Order {
            if (other !is Element) {
                throw IllegalArgumentException("Cannot compare IntervalLattice.Element with $other")
            }
            // TODO: Fix the comparison logic.
            return when {
                this.element == other.element -> Order.EQUAL
                this.element < other.element -> Order.LESSER
                this.element > other.element -> Order.GREATER
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
        return evaluate(getInitializerOf(node)!!, node, targetType, LatticeInterval.BOTTOM)
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

        val finalState = startState.iterateEOG(start.nextEOGEdges, startStateElement, ::handleNode)
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
        var newState = currentState

        return newState
    }

    private fun getInitializerOf(node: Node): Node? {
        return Value.getInitializer(node)
    }

    private fun calculateEffect(
        node: Node,
        lattice: TupleState<Any>,
        state: TupleStateElement<Any>,
    ): LatticeInterval {
        val currentInterval = state.intervalOf(node)
        return analysisType.createInstance().applyEffect(currentInterval, lattice, state, node, "")
    }
}

fun <NodeId> TupleStateElement<NodeId>.intervalOf(node: Node): LatticeInterval {
    val id = (node.objectIdentifier() as? NodeId) ?: node as? NodeId ?: TODO()
    return this.first[id]?.element ?: LatticeInterval.TOP
}

fun <NodeId> TupleState<NodeId>.pushToDeclarationState(
    current: TupleStateElement<NodeId>,
    node: Node,
    interval: LatticeInterval,
): TupleStateElement<NodeId> {
    val id = (node.objectIdentifier() as? NodeId) ?: node as NodeId ?: TODO()
    this.innerLattice1.lub(
        current.first,
        DeclarationStateElement(id to NewIntervalLattice.Element(interval)),
        allowModify = true,
    )
    return current
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
