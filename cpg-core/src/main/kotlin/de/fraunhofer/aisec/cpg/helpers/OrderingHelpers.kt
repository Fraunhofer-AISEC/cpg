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
package de.fraunhofer.aisec.cpg.helpers

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.callees
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.passes.Pass.Companion.log
import java.util.IdentityHashMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.flatMap
import kotlin.collections.iterator

/**
 * Add all functions in [dependencies] which do not have a dependency (i.e., the value of the
 * respective entry is empty) to the [orderedList] since all of their dependencies are fulfilled. We
 * also delete the entries from the [dependencies].
 */
fun addFunctionsWithoutDependency(
    orderedList: MutableList<Node>,
    dependencies: IdentityHashMap<FunctionDeclaration, IdentitySet<FunctionDeclaration>>,
) {
    // All functions which do not have a dependency will never get one.
    // We already remove them to save a bit of time in the subsequent really slow part...
    // We also do this multiple times to have the next part as small as possible because it has
    // a much higher performance penalty
    while (dependencies.isNotEmpty() && dependencies.any { (_, v) -> v.isEmpty() }) {
        // We always try to find functions without any (unsatisfied) dependencies. That's
        // obviously the best scenario because we can be sure that all prerequisites have been
        // fulfilled.
        val nextFunctions = dependencies.filterValues { it.isEmpty() }.keys

        nextFunctions.forEach { nextFunction ->
            // It's no longer needed in the map
            dependencies.remove(nextFunction)
            // It's no longer an unsatisfied dependency.
            dependencies.forEach { (_, v) -> v.remove(nextFunction) }
        }
        orderedList.addAll(nextFunctions.sortedBy { it.name })
    }
}

/**
 * Maps a function to its callees which won't have been analyzed yet and thus represents an
 * unsatisfied dependency. Whenever we add a function to the "orderedList" (which indicates in which
 * order to analyze the functions), we remove them from the dependencies (as a key but also in the
 * value of other functions' dependencies because we will definitely analyze it before).
 */
fun prepareCallGraph(
    functions: Iterable<FunctionDeclaration>
): IdentityHashMap<FunctionDeclaration, IdentitySet<FunctionDeclaration>> {
    val functionCalleesMap = IdentityHashMap(functions.associateWith { it.callees.toIdentitySet() })

    var functionCallersMap =
        IdentityHashMap<FunctionDeclaration, IdentitySet<FunctionDeclaration>>()

    for ((k, v) in functionCalleesMap) {
        v.forEach { callee ->
            functionCallersMap.computeIfAbsent(callee) { identitySetOf() }.add(k)
        }
    }

    return IdentityHashMap(
        mutableMapOf(
            *functionCalleesMap.map { (k, v) -> Pair(k, v.toIdentitySet()) }.toTypedArray()
        )
    )
}

/**
 * Analyzes the call graph to identify an ordering for analyzing the [eogStarters] in which the
 * dependencies (in terms of required function calls which could affect the currently analyzed
 * function) are hopefully resolved most of the time. Here, a function f1 depends on function f2
 * exist if f1 calls f2. This might be unsuitable for other analyses.
 */
fun orderEOGStartersBasedOnDependencies(eogStarters: Iterable<Node>): List<Node> {
    val functions = eogStarters.filterIsInstance<FunctionDeclaration>()
    val noFunction = eogStarters.subtract(functions)

    // Maps a function to its callees which won't have been analyzed yet and thus represents an
    // unsatisfied dependency. Whenever we add a function to the "orderedList" (which indicates
    // in which order to analyze the functions), we remove them from the dependencies (as a key
    // but also in the value of other functions' dependencies because we will definitely analyze
    // it before).
    val dependencies = prepareCallGraph(functions)

    val orderedList = mutableListOf<Node>()

    addFunctionsWithoutDependency(orderedList, dependencies)

    // All remaining nodes still have some unfulfilled dependencies. We make some heuristics
    // based on how many dependencies we cannot fulfill. We therefore first collect all
    // transitive dependencies.
    val changed = dependencies.keys.toMutableList()

    while (changed.isNotEmpty()) {
        val k = changed.removeFirst()
        val additionalValues =
            dependencies[k]
                ?.flatMap { dependencies.computeIfAbsent(it, ::identitySetOf) }
                ?.toIdentitySet() ?: identitySetOf()
        if (dependencies.computeIfAbsent(k, ::identitySetOf).addAll(additionalValues)) {
            changed.addAll(dependencies.filterValues { k in it }.keys.filter { it !in changed })
        }
    }
    dependencies.forEach { (k, v) -> v.remove(k) }

    log.info("Ordering all functions according to their dependencies")

    while (dependencies.isNotEmpty()) {
        // We always try to find functions without any (unsatisfied) dependencies. That's
        // obviously the best scenario because we can be sure that all prerequisites have
        // been fulfilled.
        var nextFunctions = dependencies.filterValues { it.isEmpty() }.keys
        if (nextFunctions.isEmpty()) {
            // Each function has at least one dependency :( Then, we pick the function with the
            // smallest number of missing dependencies.
            // TODO: A more sophisticated approach could improve the results here. E.g. least
            //   dependencies but used in most other functions.
            val mappedEntries = dependencies.entries.map { Pair(it, it.value.size) }
            val minimum = mappedEntries.minOf { it.second }
            nextFunctions =
                identitySetOf(
                    mappedEntries
                        .filter { it.second == minimum }
                        .minBy { it.first.key.name }
                        .first
                        .key
                )
        }

        nextFunctions.forEach { nextFunction ->
            // It's no longer needed in the map
            dependencies.remove(nextFunction)
            // It's no longer an unsatisfied dependency.
            dependencies.forEach { (_, v) -> v.remove(nextFunction) }
        }
        orderedList.addAll(nextFunctions.sortedBy { it.name })
    }

    // We add all things which are not a function declaration to the end because they won't be
    // called at a specific point in time (we hope)
    orderedList.addAll(noFunction)
    return orderedList
}
