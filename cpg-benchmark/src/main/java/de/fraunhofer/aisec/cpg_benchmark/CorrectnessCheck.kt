/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg_benchmark

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import java.util.*

object CorrectnessCheck {
    @JvmStatic
    fun assertGraphsAreEqual(
        original: List<TranslationUnitDeclaration>,
        other: List<TranslationUnitDeclaration>
    ) {

        val originalNodes = getAllNodes(original)
        val originalEdges = originalNodes.flatMap { it.outgoingEdges }.toMutableSet()

        val otherNodes = getAllNodes(other)
        val otherEdges = otherNodes.flatMap { it.outgoingEdges }.toMutableSet()

        val originalToOtherNode = getMapping(originalNodes, otherNodes)
        var missing = originalNodes.filter { it !in originalToOtherNode }
        assert(missing.isEmpty()) {
            "There are nodes in the original graph that have no analog in the other graph: $missing"
        }
        val otherToOriginalNode = getMapping(otherNodes, originalNodes)
        missing = otherNodes.filter { it !in otherToOriginalNode }
        assert(missing.isEmpty()) {
            "There are nodes in the other graph that have no analog in the original graph: $missing"
        }

        val missingEdges = mutableListOf<String>()
        for (originalEdge in originalEdges) {
            if (otherEdges.none {
                    it.label == originalEdge.label &&
                        it.properties == originalEdge.properties &&
                        originalToOtherNode[originalEdge.from]?.contains(it.from) == true &&
                        originalToOtherNode[originalEdge.to]?.contains(it.to) == true
                }
            ) {
                missingEdges.add("Edge in original graph $originalEdge not present in other graph!")
            }
        }

        for (otherEdge in otherEdges) {
            if (originalEdges.none {
                    it.label == otherEdge.label &&
                        it.properties == otherEdge.properties &&
                        otherToOriginalNode[otherEdge.from]?.contains(it.from) == true &&
                        otherToOriginalNode[otherEdge.to]?.contains(it.to) == true
                }
            ) {
                missingEdges.add("Edge in other graph $otherEdge not present in original graph!")
            }
        }

        assert(missingEdges.isEmpty()) {
            "${missingEdges.size} edge mismatches:\n ${missingEdges.joinToString(separator = "\n") { it }}"
        }
    }

    private fun getMapping(
        originNodes: Set<Node>,
        targetNodes: Set<Node>
    ): MutableMap<Node, MutableSet<Node>> {
        val mapping = IdentityHashMap<Node, MutableSet<Node>>().toMutableMap()
        for (originalNode in originNodes) {
            val matches =
                targetNodes.filter {
                    it.javaClass == originalNode.javaClass &&
                        it.allProperties == originalNode.allProperties
                }
            if (matches.isEmpty()) {
                continue
            }
            val set: MutableSet<Node> = Collections.newSetFromMap(IdentityHashMap())
            set.addAll(matches)
            mapping[originalNode] = set
        }
        return mapping
    }

    @JvmStatic
    fun getAllNodes(
        roots: Collection<Node>,
        disallowedNodes: Collection<Node> = emptySet()
    ): Set<Node> {
        val nodes: MutableSet<Node> = Collections.newSetFromMap(IdentityHashMap())
        val workset: MutableSet<Node> = Collections.newSetFromMap(IdentityHashMap())
        nodes.addAll(roots)
        workset.addAll(roots)

        while (workset.isNotEmpty()) {
            val curr = workset.first().also { workset.remove(it) }
            val neighbors = curr.outgoingEdges.map { it.to }.filter { it !in nodes }
            val disallowedNeighbors = neighbors.filter { n -> disallowedNodes.any { it === n } }
            assert(disallowedNeighbors.isEmpty()) {
                "$curr has disallowed neighbors $disallowedNeighbors"
            }
            nodes.addAll(neighbors)
            workset.addAll(neighbors)
        }

        return nodes
    }
}
