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
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.edges.flows.insertNodeAfterwardInEOGPath
import de.fraunhofer.aisec.cpg.passes.Pass.Companion.log
import kotlin.collections.set
import kotlin.reflect.KClass

class ConceptAssignmentContext {

    internal val assignments: MutableList<OverlayAssignment<*>> = mutableListOf()

    fun <T : Concept> ops(
        assign: OverlayAssignment<T>,
        block: OperationAssignmentContext<T>.(T) -> Unit,
    ) {
        assign.assigned.forEach { concept ->
            val ctx = OperationAssignmentContext(concept)
            block(ctx, concept)
        }
    }

    internal fun build() {}
}

class OperationAssignmentContext<ConceptClass : Concept>(val concept: ConceptClass) {}

/**  */
class OverlayAssignment<T : OverlayNode>(val assigned: List<T>) {

    companion object {
        fun <T : OverlayNode> fromBuilder(
            builder: () -> T,
            nodes: List<Node>,
        ): OverlayAssignment<T> {
            return OverlayAssignment(
                nodes.map { node ->
                    // Create the overlay from the constructor
                    val overlay = builder()

                    log.debug(
                        "Added overlay of type {} to {} '{}'",
                        overlay::class.simpleName,
                        node::class.simpleName,
                        node.name,
                    )

                    // Add overlay to underlying node
                    node.overlayEdges += overlay

                    // Set DFG (and others), if needed
                    when (node) {
                        is Concept -> node.setDFG()
                        is Operation -> {
                            node.concept.ops += node
                            node.underlyingNode?.insertNodeAfterwardInEOGPath(node)
                            node.setDFG()
                        }
                    }

                    overlay
                }
            )
        }
    }
}

class TaggingContext(val mapOfEach: MutableMap<KClass<out Node>, EachContext<*>> = mutableMapOf()) {
    fun collect(
        lattice: NodeToOverlayState,
        state: NodeToOverlayStateElement,
        node: Node,
    ): List<OverlayNode> {
        val list = mutableListOf<OverlayNode>()

        for (each in mapOfEach) {
            // Check, if the node is assignable to the "each"
            if (each.key.isInstance(node)) {
                val overlay = each.value.collect(lattice, state, node)
                list += overlay
            }
        }

        return list
    }
}

class Selector<T : Node>(val namePredicate: CharSequence?, val predicate: ((T) -> Boolean)?) {
    operator fun invoke(node: T): Boolean {
        return node.name == namePredicate || predicate?.invoke(node) == true
    }
}

@Suppress("UNCHECKED_CAST")
class EachContext<T : Node>(
    val selector: Selector<T>,
    val with: (BuilderContext<T>) -> List<OverlayNode>,
) {
    fun collect(
        lattice: NodeToOverlayState,
        state: NodeToOverlayStateElement,
        node: Node,
    ): List<OverlayNode> {
        // Check, if our selector matches
        return if (selector(node as T)) {
            val ctx = BuilderContext(lattice, state, node, with)
            ctx.build()
        } else {
            emptyList()
        }
    }
}

class BuilderContext<T : Node>(
    var lattice: NodeToOverlayState,
    var state: NodeToOverlayStateElement,
    var node: T,
    var builder: (BuilderContext<T>) -> List<OverlayNode>,
) {
    fun build(): List<OverlayNode> {
        return builder(this)
    }
}

fun tag(body: TaggingContext.() -> Unit): TaggingContext {
    val ctx = TaggingContext()
    body(ctx)
    return ctx
}

fun <T : Node> TaggingContext.each(
    namePredicate: CharSequence? = null,
    predicate: ((T) -> Boolean)? = null,
): Selector<T> {
    return Selector(namePredicate = namePredicate, predicate = predicate)
}

context(TaggingContext)
inline fun <reified T : Node> Selector<T>.with(
    noinline builder: BuilderContext<T>.() -> Concept
): EachContext<T> {
    val ctx = EachContext(selector = this, with = { listOf(builder(it)) })
    mapOfEach[T::class] = ctx
    return ctx
}

context(TaggingContext)
inline fun <reified T : Node> Selector<T>.withMultiple(
    noinline builder: BuilderContext<T>.() -> List<OverlayNode>
): EachContext<T> {
    val ctx = EachContext(selector = this, with = builder)
    mapOfEach[T::class] = ctx
    return ctx
}

fun assign(block: ConceptAssignmentContext.() -> Unit) {
    ConceptAssignmentContext().apply(block)
}

context(ConceptAssignmentContext)
infix fun <T : Concept> (() -> T).to(nodes: List<Node>): OverlayAssignment<T> {
    val pairs = mutableListOf<Pair<List<Node>, List<Node>>>()
    pairs

    val assignment = OverlayAssignment.fromBuilder(this, nodes)
    this@ConceptAssignmentContext.assignments += assignment

    return assignment
}

context(ConceptAssignmentContext)
infix fun <T : Concept> T.to(node: Node?): OverlayAssignment<T> {
    val assignment = OverlayAssignment.fromBuilder({ this }, listOfNotNull(node))

    this@ConceptAssignmentContext.assignments += assignment

    return assignment
}

infix fun <T : Operation> (() -> T).to(nodes: List<Node>): OverlayAssignment<T> {
    return OverlayAssignment.fromBuilder(this, nodes)
}

infix fun <T : Operation> T.to(node: Node): OverlayAssignment<T> {
    return OverlayAssignment.fromBuilder({ this }, listOf(node))
}
