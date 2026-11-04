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
package de.fraunhofer.aisec.cpg.graph

import java.util.concurrent.ConcurrentHashMap

/**
 * Interns [Name]s, so that structurally identical names ([Name.localName], [Name.parent] and
 * [Name.delimiter] all equal) share a single instance instead of every occurrence allocating its
 * own. This particularly benefits:
 * - common, unqualified identifiers (loop variables, parameters, `this`, ...) that recur across
 *   many unrelated scopes, which would otherwise each allocate a distinct `Name` + `String`, and
 * - qualified names sharing a common prefix (e.g. every member of the same class/namespace), which
 *   end up sharing the same (interned) parent [Name] instance once each part is interned as it is
 *   built.
 *
 * [Name] is fully immutable and structurally equal/hashable, so interning is invisible to
 * correctness: [Name.equals] never distinguishes an interned instance from an equal non-interned
 * one. The cache is global and bounded, like
 * [de.fraunhofer.aisec.cpg.sarif.PhysicalLocation.ArtifactLocation]'s: dropping entries only
 * forgoes sharing, never correctness.
 */
internal object NameCache {
    /**
     * Upper bound on the number of cached names. Names are far more numerous than the files
     * [de.fraunhofer.aisec.cpg.sarif.PhysicalLocation.ArtifactLocation] interns (every node has
     * one), hence the larger bound; dropping entries once it's reached only forgoes sharing for
     * subsequent lookups, never correctness.
     */
    private const val MAX_CACHE_SIZE = 1_000_000

    // Initial capacity chosen to avoid the first several resize-and-copy steps for any
    // non-trivial analysis, since Names are numerous; still cheap for one-off/small analyses.
    private val cache = ConcurrentHashMap<Name, Name>(4096)

    /** Returns a shared instance equal to [name], interning [name] itself if it's the first. */
    fun intern(name: Name): Name {
        // Cheap, racy bound. Occasional over-shoot or double-clear across threads is harmless:
        // entries are pure caches and any dropped instance is simply rebuilt.
        if (cache.size >= MAX_CACHE_SIZE) {
            cache.clear()
        }
        return cache.computeIfAbsent(name) { it }
    }
}
