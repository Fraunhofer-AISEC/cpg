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
package de.fraunhofer.aisec.cpg.sarif

import de.fraunhofer.aisec.cpg.helpers.regionToOffsets
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

/**
 * A `[start, end)` char range into [content], shared by every node whose `code` happens to be
 * exactly that range of the file. Storing this instead of a dedicated copy of the substring avoids
 * the O(depth) duplication that otherwise comes from nested AST nodes each holding an overlapping
 * copy of their ancestors' code.
 */
internal class CodeSpan(val content: String, val start: Int, val end: Int) {
    fun materialize(): String = content.substring(start, end)
}

/**
 * Caches the full text of source files by [URI], so that a node's `code` can be interned as a
 * [CodeSpan] into a single shared copy instead of every node holding its own copy of (part of) it.
 *
 * The cache is opportunistic and self-verifying: [rangeOf] only ever returns a [CodeSpan] once it
 * has confirmed, character for character, that the range it computed reproduces [rawCode] exactly.
 * This makes it safe to use regardless of how a particular language frontend actually derived
 * [rawCode] (an exact substring of the file, a pretty-printed reconstruction, byte- vs. UTF-16-
 * indexed columns, a different line-break convention, ...): a mismatch simply yields `null`, and
 * the caller falls back to storing [rawCode] directly, exactly as before this cache existed.
 */
internal object FileContentCache {
    /**
     * Upper bound on the number of cached files, mirroring
     * [de.fraunhofer.aisec.cpg.sarif.PhysicalLocation.ArtifactLocation]'s bound: this only needs to
     * help within the working set of files currently being analyzed, so dropping cache entries only
     * forgoes sharing, never correctness.
     */
    private const val MAX_CACHE_SIZE = 50_000

    /** `Optional.empty()` is cached too, so an unreadable/virtual [URI] is not retried per node. */
    private val cache = ConcurrentHashMap<URI, Optional<String>>()

    /**
     * Returns a [CodeSpan] into the cached content of [location]'s file that reproduces [rawCode]
     * exactly, or `null` if the file can't be read, or the region computed from [location] does not
     * match [rawCode] verbatim.
     */
    fun rangeOf(location: PhysicalLocation, rawCode: String): CodeSpan? {
        val uri = location.artifactLocation.uri ?: return null
        val content = contentOf(uri) ?: return null

        val range =
            regionToOffsets(content, Region(startLine = 1, startColumn = 1), location.region)
        if (
            range.first < 0 ||
                range.last + 1 > content.length ||
                range.last + 1 - range.first != rawCode.length
        ) {
            return null
        }

        return if (content.regionMatches(range.first, rawCode, 0, rawCode.length)) {
            CodeSpan(content, range.first, range.last + 1)
        } else {
            null
        }
    }

    /** Returns the cached full text of [uri]'s file, reading it once on first request. */
    private fun contentOf(uri: URI): String? {
        // Cheap, racy bound. Occasional over-shoot or double-clear across threads is harmless:
        // entries are pure caches and any dropped entry is simply re-read on demand.
        if (cache.size >= MAX_CACHE_SIZE) {
            cache.clear()
        }
        return cache.computeIfAbsent(uri) { Optional.ofNullable(readOrNull(it)) }.orElse(null)
    }

    private fun readOrNull(uri: URI): String? {
        return try {
            Files.readString(Path.of(uri))
        } catch (_: Exception) {
            // Not a local, readable file (e.g. a virtual/in-memory URI, or one that no longer
            // exists) -- callers fall back to storing the literal code.
            null
        }
    }
}
