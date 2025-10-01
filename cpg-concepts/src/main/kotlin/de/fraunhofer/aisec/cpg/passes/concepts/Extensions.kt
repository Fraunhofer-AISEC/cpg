/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
@file:Suppress("CONTEXT_RECEIVERS_DEPRECATED")

package de.fraunhofer.aisec.cpg.passes.concepts

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.OverlayNode
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.helpers.functional.PowersetLattice
import de.fraunhofer.aisec.cpg.passes.concepts.EOGConceptPass.Companion.filterDuplicates
import kotlin.reflect.KClass
import kotlin.reflect.safeCast
import kotlinx.coroutines.runBlocking

/**
 * The core DSL function of our tagging API. It represents a configuration-style DSL to define a
 * tagging or mapping between nodes in the graph and desired "overlay nodes". It only represents a
 * configuration (in the form of the [TaggingContext] class) -- the actual assignment is done
 * independently, e.g., by the [TagOverlaysPass], which takes the context as an input.
 *
 * The [body] of this function is a [TaggingContext] that allows for further specification of which
 * nodes are "tagged" with [OverlayNode]s. Currently, the only allowed statements are:
 * - [each]: Applies the specified overlay nodes to "each" of the nodes selected.
 *
 * In the following example, each [CallExpression] with the name "foo" is tagged with an overlay
 * class `Bar`:
 * ```Kotlin
 * tag {
 *   each<CallExpression>("foo").with {
 *     Bar()
 *   }
 * }
 * ```
 *
 * The [TaggingContext] is also returned, which can then be used by the [TagOverlaysPass].
 */
fun tag(body: TaggingContext.() -> Unit): TaggingContext {
    val ctx = TaggingContext()
    body(ctx)
    return ctx
}

/**
 * Applies the specified overlay nodes to "each" of the nodes selected. A node is first of all
 * selected by its type [T]. Furthermore, the node selection can be further restricted by either a
 * [namePredicate] or a general [predicate] over all the node's properties.
 *
 * This function only has an effect together with [with].
 */
inline fun <reified T : Node> TaggingContext.each(
    namePredicate: CharSequence? = null,
    noinline predicate: ((T) -> Boolean)? = null,
): Selector<T> {
    return Selector(T::class, namePredicate = namePredicate, predicate = predicate)
}

inline fun <reified S : Node, reified T : Node> BuilderContext<S>.propagate(
    noinline transformation: ((S) -> T)
): Propagator<S, T> {
    val propagator = Propagator(transformation = transformation)
    this.propagators += propagator
    return propagator
}

/**
 * Specifies a [builder] that creates the actual overlay node. It is used to assign a single overlay
 * node to a single selected "underlying" node.
 */
context(_: TaggingContext)
fun <S : Node, T : Node> Propagator<S, T>.with(builder: BuilderContext<T>.() -> OverlayNode) {
    this.builders += { listOf(builder(it)) }
}

/**
 * Specifies a [builder] that creates the actual overlay nodes. It is used to assign multiple
 * overlay nodes to a single selected "underlying" node.
 */
context(_: TaggingContext)
fun <S : Node, T : Node> Propagator<S, T>.withMultiple(
    builder: BuilderContext<T>.() -> List<OverlayNode>
) {
    this.builders += builder
}

/**
 * Specifies a [builder] that creates the actual overlay node. It is used to assign a single overlay
 * node to a single selected "underlying" node.
 */
context(context: TaggingContext)
fun <T : Node> Selector<T>.with(builder: BuilderContext<T>.() -> OverlayNode?): EachContext<T> {
    val ctx = EachContext(selector = this, builder = { listOfNotNull(builder(it)) })
    context.listOfEach += ctx
    return ctx
}

/**
 * Specifies a [builder] that creates the actual overlay nodes. It is used to assign multiple
 * overlay nodes to a single selected "underlying" node.
 */
context(context: TaggingContext)
fun <T : Node> Selector<T>.withMultiple(
    builder: BuilderContext<T>.() -> List<OverlayNode>
): EachContext<T> {
    val ctx = EachContext(selector = this, builder = builder)
    context.listOfEach += ctx
    return ctx
}

/**
 * This class holds the context used in the [tag] DSL function. It basically contains a list of
 * [EachContext], which represent a call to [each] within the tagging context.
 */
data class TaggingContext(val listOfEach: MutableList<EachContext<*>> = mutableListOf()) :
    OverlayCollector {
    override fun collect(
        lattice: NodeToOverlayState,
        state: NodeToOverlayStateElement,
        node: Node,
    ): List<OverlayNode> {
        val list = mutableListOf<OverlayNode>()

        for (each in listOfEach) {
            // Check, if the node is assignable to the "each"
            val overlay = each.collect(lattice, state, node)
            overlay?.let { list += it }
        }

        return list
    }
}

/**
 * This class represents the context of an [each] DSL function. It contains the [selector] that is
 * used to select a node that is brought into the [EachContext] as well as a [builder] that
 * specifies how and which [OverlayNode] is constructed based on the selected node(s).
 */
data class EachContext<T : Node>(
    val selector: Selector<T>,
    val builder: (BuilderContext<T>) -> List<OverlayNode>,
) : OverlayCollector {
    override fun collect(
        lattice: NodeToOverlayState,
        state: NodeToOverlayStateElement,
        node: Node,
    ): List<OverlayNode>? {
        // Check if the selector returns a node, and if so, then build the overlay node using the
        // builder context.
        return selector(node)?.let {
            val ctx = BuilderContext(lattice, state, it, builder)
            ctx.build()
        }
    }
}

/**
 * This class holds a context that is passed to [EachContext.builder] during execution of the
 * [EOGConceptPass]. This is needed because the [builder] might need access to the current [state].
 */
data class BuilderContext<T : Node>(
    var lattice: NodeToOverlayState,
    var state: NodeToOverlayStateElement,
    var node: T,
    var builder: (BuilderContext<T>) -> List<OverlayNode>,
) {
    val propagators: MutableList<Propagator<T, *>> = mutableListOf()

    fun build(): List<OverlayNode> {
        val overlayList = builder(this)
        propagators.forEach { it.invoke(lattice, state, node) }
        return overlayList
    }
}

/**
 * A selector that describes a possible selection of a CPG [Node] by the following properties:
 * - its [KClass] (mandatory, see [klass]),
 * - its [Node.name] (see [namePredicate]),
 * - any other property (see [predicate])
 */
data class Selector<T : Node>(
    val klass: KClass<T>,
    val namePredicate: CharSequence?,
    val predicate: ((T) -> Boolean)?,
) {
    /**
     * Allows the selector to be invoked to "test", whether [node] fulfills this selector or not.
     *
     * It returns the [node] cast to [T] or null, if the predicate fails.
     */
    operator fun invoke(node: Node): T? {
        // Try to cast the node to T, if it's not an instance of T, it is null
        val tNode = klass.safeCast(node)
        if (tNode == null) return null

        // Check, if predicate matches
        return if (
            (namePredicate == null || node.name == namePredicate) &&
                predicate?.invoke(tNode) != false
        ) {
            tNode
        } else {
            null
        }
    }
}

/**
 * A selector that describes a possible selection of a CPG [Node] by the following properties:
 * - its [KClass] (mandatory, see [klass]),
 * - its [Node.name] (see [namePredicate]),
 * - any other property (see [predicate])
 */
data class Propagator<S : Node, T : Node>(val transformation: ((S) -> T)) {
    var builders = mutableListOf<(BuilderContext<T>) -> List<OverlayNode>>()

    operator fun invoke(lattice: NodeToOverlayState, state: NodeToOverlayStateElement, node: S) {
        val changedNode = transformation(node)
        runBlocking {
            builders.forEach { builder ->
                // We compute the new overlay nodes and discard those which are already present in
                // the
                // state or in the node's overlay nodes.
                val newNodes = BuilderContext(lattice, state, changedNode, builder).build()
                val filteredNewNodes = filterDuplicates(state, changedNode, newNodes)
                // We directly add the new overlay nodes to the state, so that they are available
                // for
                // the next computations.
                lattice.lub(
                    one = state,
                    two =
                        NodeToOverlayStateElement(
                            changedNode to PowersetLattice.Element(*filteredNewNodes.toTypedArray())
                        ),
                    allowModify = true,
                )
            }
        }
    }
}
