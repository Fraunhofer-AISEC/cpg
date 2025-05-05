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
import kotlin.reflect.KClass

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

/**
 * Specifies a [builder] that creates the actual overlay node. It is used to assign a single overlay
 * node to a single selected "underlying" node.
 */
context(TaggingContext)
fun <T : Node> Selector<T>.with(builder: BuilderContext<T>.() -> OverlayNode): EachContext<T> {
    val ctx = EachContext(selector = this, builder = { listOf(builder(it)) })
    listOfEach += ctx
    return ctx
}

/**
 * Specifies a [builder] that creates the actual overlay nodes. It is used to assign multiple
 * overlay nodes to a single selected "underlying" node.
 */
context(TaggingContext)
fun <T : Node> Selector<T>.withMultiple(
    builder: BuilderContext<T>.() -> List<OverlayNode>
): EachContext<T> {
    val ctx = EachContext(selector = this, builder = builder)
    listOfEach += ctx
    return ctx
}

/**
 * This class holds the context used in the [tag] DSL function. It basically contains a list of
 * [EachContext], which represent a call to [each] within the tagging context.
 */
data class TaggingContext(val listOfEach: MutableList<EachContext<*>> = mutableListOf()) {
    fun collect(
        lattice: NodeToOverlayState,
        state: NodeToOverlayStateElement,
        node: Node,
    ): List<OverlayNode> {
        val list = mutableListOf<OverlayNode>()

        for (each in listOfEach) {
            // Check, if the node is assignable to the "each"
            val overlay = each.collect(lattice, state, node)
            list += overlay
        }

        return list
    }
}

/**
 * This class represents the context of an [each] DSL function. It contains the [selector] that is
 * used to select a node that is brought into the [EachContext] as well as a [builder] that
 * specifies how and which [OverlayNode] is constructed based on the selected node(s).
 */
@Suppress("UNCHECKED_CAST")
data class EachContext<T : Node>(
    val selector: Selector<T>,
    val builder: (BuilderContext<T>) -> List<OverlayNode>,
) {
    fun collect(
        lattice: NodeToOverlayState,
        state: NodeToOverlayStateElement,
        node: Node,
    ): List<OverlayNode> {
        // Check, if our selector matches
        return if (selector(node as T)) {
            val ctx = BuilderContext(lattice, state, node, builder)
            ctx.build()
        } else {
            emptyList()
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
    fun build(): List<OverlayNode> {
        return builder(this)
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
     */
    operator fun invoke(node: T): Boolean {
        return klass.isInstance(node) &&
            (namePredicate == null || node.name == namePredicate) &&
            predicate?.invoke(node) != false
    }
}
