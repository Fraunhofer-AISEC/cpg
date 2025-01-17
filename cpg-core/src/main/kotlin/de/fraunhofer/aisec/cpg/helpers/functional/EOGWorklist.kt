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
import de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder
import de.fraunhofer.aisec.cpg.helpers.IdentitySet
import java.util.IdentityHashMap

inline fun <reified V> iterateEOGClean(
    startEdges: List<EvaluationOrder>,
    startState: LatticeElement<V>,
    transformation: (EvaluationOrder, LatticeElement<V>) -> LatticeElement<V>,
): LatticeElement<V> {
    val globalState = IdentityHashMap<EvaluationOrder, LatticeElement<V>>()
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
        nextEdge.end.nextEOGEdges.forEach {
            val oldGlobalIt = globalState[it]
            val newGlobalIt =
                (oldGlobalIt?.let { newState.lub(it) } ?: newState) as LatticeElement<V>
            globalState[it] = newGlobalIt
            if (it !in edgesList && (oldGlobalIt == null || newGlobalIt != oldGlobalIt)) {
                newGlobalIt.checkEqualitySummary(oldGlobalIt)
                edgesList.add(0, it)
            }
        }
    }

    return globalState.values.fold(globalState.values.firstOrNull()) { state, value ->
        state?.lub(value) as LatticeElement<V>
    } ?: startState
}

typealias specialLattice =
    LatticeElement<
        IdentityHashMap<
            Node,
            LatticeElement<IdentityHashMap<Node, LatticeElement<IdentitySet<Node>>>>,
        >
    >

fun LatticeElement<*>.checkEqualitySummary(oldGlobalIt: LatticeElement<*>?) {
    (this as? specialLattice)?.checkEqualitySummary2(oldGlobalIt as? specialLattice)
    // Nothing to see here.
}

fun specialLattice.checkEqualitySummary2(oldGlobalIt: specialLattice?) {
    val newGlobalIt = this
    oldGlobalIt ?: return

    val equalKeys =
        newGlobalIt.elements.keys.containsAll(oldGlobalIt.elements.keys) &&
            oldGlobalIt.elements.keys.containsAll(newGlobalIt.elements.keys)
    val equalValuesPerKey =
        oldGlobalIt.elements.entries.map { (key, value) ->
            value.elements to newGlobalIt.elements[key]!!.elements
        }
    val equalValuesMap =
        equalValuesPerKey.map { (old, new) ->
            Triple(
                old != new,
                old.keys.containsAll(new.keys) && new.keys.containsAll(old.keys),
                old.entries
                    .map { (k, v) -> v.elements to new[k]?.elements }
                    .filter { it.first != it.second },
            )
        }
    val shouldBeEqual =
        equalValuesMap.all { it.first == true && it.second == true && it.third.isEmpty() }
    print(shouldBeEqual)
    newGlobalIt != oldGlobalIt
}
