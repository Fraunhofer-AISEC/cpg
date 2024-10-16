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
package de.fraunhofer.aisec.cpg.helpers.functional

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import java.util.IdentityHashMap

inline fun <reified K : Edge<Node>, V> iterateEOGClean(
    startEdges: List<K>,
    startState: LatticeElement<V>,
    transformation: (K, LatticeElement<V>) -> LatticeElement<V>
): LatticeElement<V> {
    val globalState = IdentityHashMap<K, LatticeElement<V>>()
    for (startEdge in startEdges) {
        globalState[startEdge] = startState
    }
    val edgesList = mutableListOf<K>()
    startEdges.forEach { edgesList.add(it) }

    while (edgesList.isNotEmpty()) {
        val nextEdge = edgesList.first()
        edgesList.removeFirst()

        val nextGlobal = globalState[nextEdge] ?: continue
        val newState = transformation(nextEdge, nextGlobal)
        if (newState != nextGlobal) {
            nextEdge.end.nextEOGEdges.forEach {
                if (it is K) {
                    /*val oldStateForIt = globalState[it]
                    val newStateForIt = oldStateForIt?.let { newState.lub(it) } ?: newState
                    if (oldStateForIt == null || newStateForIt != oldStateForIt) {
                        globalState[it] = newStateForIt*/
                    globalState[it] = newState
                    if (it !in edgesList) edgesList.add(0, it)
                    // }
                }
            }
        }
    }

    return globalState.values.fold(globalState.values.first()) { state, value -> state.lub(value) }
}
