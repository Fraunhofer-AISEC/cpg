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
import kotlin.reflect.KClass

class TaggingContext(val listOfEach: MutableList<EachContext<*>> = mutableListOf()) {
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

class Selector<T : Node>(
    val klass: KClass<T>,
    val namePredicate: CharSequence?,
    val predicate: ((T) -> Boolean)?,
) {
    operator fun invoke(node: T): Boolean {
        return klass.isInstance(node) &&
            (node.name == namePredicate || predicate?.invoke(node) == true)
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

inline fun <reified T : Node> TaggingContext.each(
    namePredicate: CharSequence? = null,
    noinline predicate: ((T) -> Boolean)? = null,
): Selector<T> {
    return Selector(T::class, namePredicate = namePredicate, predicate = predicate)
}

context(TaggingContext)
fun <T : Node> Selector<T>.with(builder: BuilderContext<T>.() -> Concept): EachContext<T> {
    val ctx = EachContext(selector = this, with = { listOf(builder(it)) })
    listOfEach += ctx
    return ctx
}

context(TaggingContext)
fun <T : Node> Selector<T>.withMultiple(
    builder: BuilderContext<T>.() -> List<OverlayNode>
): EachContext<T> {
    val ctx = EachContext(selector = this, with = builder)
    listOfEach += ctx
    return ctx
}
