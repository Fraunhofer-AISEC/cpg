/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.analysis.similarity

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.helpers.identitySetOf

/**
 * Explores the PDG reachable from [starts] via depth-limited DFS, following outgoing PDG edges
 * ([Node.nextPDG]), and applies [predicate] to every node visited. For every maximal DFS branch --
 * one that either reaches [maxDepth] hops or runs out of not-yet-visited-on-this-branch outgoing
 * neighbors -- the non-null [predicate] results encountered along that branch are collected, in
 * visiting order, into a `List<T>`. The distinct such lists across all branches from all [starts]
 * are returned as a `Set<List<T>>`.
 *
 * [starts] is a set rather than a single node because a "program point" of interest is often more
 * than one node (e.g. every entry statement of a function); everything reachable from any of them
 * is treated as one comparison target.
 *
 * Cycles are handled per-branch: a node already on the *current* DFS branch is not revisited
 * (avoiding infinite recursion on PDG back-edges from loops), but the same node may legitimately
 * appear on several different branches, from the same or different start nodes.
 *
 * Requires [de.fraunhofer.aisec.cpg.passes.ProgramDependenceGraphPass] to have populated
 * [Node.nextPDGEdges] beforehand.
 *
 * Note on complexity: this enumerates every DFS branch up to [maxDepth], which is
 * branching-factor^[maxDepth] in the worst case. In practice, most nodes make [predicate] return
 * `null`, so many raw branches collapse onto the same (typically much shorter) non-null `List<T>`,
 * and the result `Set` deduplicates them -- but [maxDepth] should still be kept small (e.g. 3-6) on
 * non-trivial graphs.
 */
fun <T> collectConceptSequences(
    starts: Set<Node>,
    maxDepth: Int,
    predicate: (Node) -> T?,
): Set<List<T>> {
    val results = mutableSetOf<List<T>>()

    fun dfs(node: Node, depth: Int, onBranch: MutableSet<Node>, sequence: MutableList<T>) {
        val hit = predicate(node)
        if (hit != null) sequence.add(hit)

        val neighbors =
            if (depth >= maxDepth) emptyList() else node.nextPDG.filter { it !in onBranch }

        if (neighbors.isEmpty()) {
            results.add(sequence.toList())
        } else {
            neighbors.forEach { next ->
                onBranch.add(next)
                dfs(next, depth + 1, onBranch, sequence)
                onBranch.remove(next)
            }
        }

        if (hit != null) sequence.removeAt(sequence.lastIndex)
    }

    starts.forEach { start -> dfs(start, 0, identitySetOf(start), mutableListOf()) }

    return results
}

/**
 * Applies [toLabel] to every element of every list in `this`, returning the (again deduplicated)
 * set of resulting `List<String>`s. Two elements that map to the same label collapse together, so
 * this is expected to shrink the set further on top of [collectConceptSequences]'s own
 * deduplication (e.g. two different [de.fraunhofer.aisec.cpg.graph.concepts.Concept] *instances* of
 * the same concept class/attributes become the same string).
 */
fun <T> Set<List<T>>.mapElements(toLabel: (T) -> String): Set<List<String>> =
    map { list -> list.map(toLabel) }.toSet()
