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

inline fun <reified V> iterateEOGClean(
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

        val nextGlobal = globalState[nextEdge] ?: continue
        val newState = transformation(nextEdge, nextGlobal)
        if (nextEdge.end.nextEOGEdges.isEmpty()) {
            finalState[nextEdge] = newState
        } else {
            nextEdge.end.nextEOGEdges.forEach {
                val oldGlobalIt = globalState[it]
                val newGlobalIt = oldGlobalIt?.let { newState.lub(it) } ?: newState
                globalState[it] = newGlobalIt
                if (it !in edgesList && (oldGlobalIt == null || newGlobalIt != oldGlobalIt))
                    edgesList.add(0, it)
            }
        }
    }

    return finalState.values.fold(finalState.values.firstOrNull()) { state, value ->
        state?.lub(value)
    } ?: startState
}
