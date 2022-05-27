/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.helpers

import de.fraunhofer.aisec.cpg.graph.Node
import java.lang.UnsupportedOperationException
import java.util.*

/**
 * This class implements the [MutableSet] interface with an underlying map and reference-equality
 * instead of object-equality. That means, objects are only considered equal, if they are the *same*
 * object. This logic is primarily implemented by the underlying [IdentityHashMap].
 *
 * The use case of this [MutableSet] is quite simple: In order to avoid loops while traversing in
 * the CPG AST we often need to store [Node] objects in a work-list (usually a set), in order to
 * filter out nodes that were already visited or processed (for example, see
 * [SubgraphWalker.flattenAST]. However, using a normal set triggers object-equality functions, such
 * as [Node.hashCode] or even worse [Node.equals], if the hashcode is the same. This can potentially
 * be very resource-intensive if nodes are very similar but not the *same*, in a work-list however
 * we only want just to avoid to place the exact node twice.
 */
class IdentitySet<T : Any> : MutableSet<T> {
    /**
     * The backing hashmap for our set. The [IdentityHashMap] offers reference-equality for keys and
     * values. In this case we use it to determine, if a node is already in our set or not. The
     * value of the map is not used and is always true. A [Boolean] is used because it seems to be
     * the smallest data type possible.
     */
    private val map: IdentityHashMap<T, Boolean> = IdentityHashMap()

    override operator fun contains(element: T): Boolean {
        // We are using the backing reference-equality based map to check, if the element is already
        // in the set.
        return map.containsKey(element)
    }

    override fun add(element: T): Boolean {
        // Since we are a Set, we only want to add elements that are not already there
        if (!contains(element)) {
            map[element] = true
            return true
        }

        return false
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        return elements.all { map.containsKey(it) }
    }

    override fun isEmpty(): Boolean {
        return map.isEmpty()
    }

    override fun iterator(): MutableIterator<T> {
        return map.keys.iterator()
    }

    override fun addAll(elements: Collection<T>): Boolean {
        // We need to keep track, whether we modified the set
        var modified = false

        elements.forEach {
            if (add(it)) {
                modified = true
            }
        }

        return modified
    }

    override fun clear() {
        map.clear()
    }

    override fun remove(element: T): Boolean {
        return map.remove(element) != null
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        // We need to keep track, whether we modified the set
        var modified = false

        elements.forEach {
            if (remove(it)) {
                modified = true
            }
        }

        return modified
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        throw UnsupportedOperationException()
    }

    override val size: Int
        get() = map.size
}
