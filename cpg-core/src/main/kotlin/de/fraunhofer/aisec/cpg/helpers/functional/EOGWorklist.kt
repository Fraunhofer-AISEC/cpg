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

import de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder
import java.util.IdentityHashMap

/**
 * Computes a fixpoint by iterating over the EOG beginning with the [startEdges] and a state
 * [startState]. This means, it keeps applying [transformation] until the state does no longer
 * change. With state, we mean a mapping between the [EvaluationOrder] edges to the value of
 * [LatticeElement] which represents possible values (or abstractions thereof) that they hold.
 */
inline fun <reified V> iterateEOGNew(
    startEdges: List<EvaluationOrder>,
    startState: LatticeElement<V>,
    transformation: (EvaluationOrder, LatticeElement<V>) -> LatticeElement<V>,
): LatticeElement<V> {
    val globalState = IdentityHashMap<EvaluationOrder, LatticeElement<V>>()
    val finalState = IdentityHashMap<EvaluationOrder, LatticeElement<V>>()
    for (startEdge in startEdges) {
        globalState[startEdge] = startState
    }
    val edgesList = mutableListOf<EvaluationOrder>()
    startEdges.forEach { edgesList.add(it) }

    while (edgesList.isNotEmpty()) {
        val nextEdge = edgesList.first()
        edgesList.removeFirst()

        // Compute the effects of "nextEdge" on the state by applying the transformation to its
        // state.
        val nextGlobal = globalState[nextEdge] ?: continue
        val newState = transformation(nextEdge, nextGlobal)
        if (nextEdge.end.nextEOGEdges.isEmpty()) {
            finalState[nextEdge] = newState
        }
        nextEdge.end.nextEOGEdges.forEach {
            // We continue with the nextEOG edge if we haven't seen it before or if we updated the
            // state in comparison to the previous time we were there.
            val oldGlobalIt = globalState[it]
            val newGlobalIt =
                (oldGlobalIt?.let { newState.lub(it) } ?: newState) as LatticeElement<V>
            globalState[it] = newGlobalIt
            if (it !in edgesList && (oldGlobalIt == null || newGlobalIt != oldGlobalIt)) {
                edgesList.add(0, it)
            }
        }
    }

    return finalState.values.fold(finalState.values.firstOrNull()) { state, value ->
        state?.lub(value) as LatticeElement<V>
    } ?: startState
}
