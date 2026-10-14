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
import de.fraunhofer.aisec.cpg.graph.edges.flows.ControlDependence
import de.fraunhofer.aisec.cpg.graph.edges.flows.Dataflow
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
 * TEMPORARY, ISOLATED WORKAROUND -- do not generalize, and remove this function (switching its call
 * site(s) back to [collectConceptSequences]) once it's no longer needed.
 *
 * Same contract as [collectConceptSequences], except it suppresses a specific degenerate case that
 * breaks [compareSequenceSets]/[sequenceSimilarity]-based comparisons: whenever a node has more
 * than one *not mutually exclusive* outgoing [de.fraunhofer.aisec.cpg.graph.edges.flows.Dataflow]
 * PDG edge (i.e. a value with several independent downstream readers that *all* execute on every
 * run, not alternative branches), a DFS branch that reaches a dead end without picking up a single
 * further [predicate] hit after that point is dropped instead of being added to the result. Such a
 * branch doesn't represent a real alternative execution -- every one of its sibling branches from
 * the same fork happens too -- so keeping it around only adds a content-free, pure-prefix duplicate
 * of whichever sibling branch *did* pick up more hits, and that duplicate is exactly what skews
 * [compareSequenceSets]'s `min` aggregation (see `CryptoConceptSimilarityTest`).
 *
 * Genuine [de.fraunhofer.aisec.cpg.graph.edges.flows.ControlDependence] forks (e.g. an early
 * return) are never affected: a branch that stops short because of a real, mutually-exclusive
 * control decision is always kept, regardless of length, since it reflects an execution that
 * actually differs from its sibling branches.
 */
fun <T> collectMaximalConceptSequences(
    starts: Set<Node>,
    maxDepth: Int,
    predicate: (Node) -> T?,
): Set<List<T>> {
    val results = mutableSetOf<List<T>>()

    fun dfs(
        node: Node,
        depth: Int,
        onBranch: MutableSet<Node>,
        sequence: MutableList<T>,
        droppableIn: Boolean,
    ) {
        val hit = predicate(node)
        if (hit != null) sequence.add(hit)
        // A hit always proves this branch carries information beyond its last fork; otherwise the
        // incoming state (droppable or not) still applies.
        val droppable = if (hit != null) false else droppableIn

        val edges =
            if (depth >= maxDepth) emptyList() else node.nextPDGEdges.filter { it.end !in onBranch }

        if (edges.isEmpty()) {
            if (!droppable) results.add(sequence.toList())
        } else {
            // Only a node with more than one live outgoing edge is an actual fork/choice; a single
            // successor is a mandatory hop, not something to be "redundant" relative to.
            val isRealFork = edges.size > 1
            edges.forEach { edge ->
                val next = edge.end
                val childDroppable =
                    when {
                        // A real control alternative is never discarded, however short.
                        edge is ControlDependence -> false
                        // Re-arm: this child is one of several non-exclusive readers of the same
                        // value, so it's only worth keeping if it goes on to add something new.
                        edge is Dataflow && isRealFork -> true
                        // No real choice was made reaching this child; inherit the current state.
                        else -> droppable
                    }
                onBranch.add(next)
                dfs(next, depth + 1, onBranch, sequence, childDroppable)
                onBranch.remove(next)
            }
        }

        if (hit != null) sequence.removeAt(sequence.lastIndex)
    }

    starts.forEach { start ->
        dfs(start, 0, identitySetOf(start), mutableListOf(), droppableIn = false)
    }

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
