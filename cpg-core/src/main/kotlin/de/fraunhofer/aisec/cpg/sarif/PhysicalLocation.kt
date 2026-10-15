/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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

import java.io.File
import java.net.URI
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * A file's full text plus a precomputed line-start index, so a (line, column) region can be
 * converted to a char offset in O(1) instead of re-scanning from the start of the file for every
 * node (which would make interning O(fileSize) per node, i.e. O(fileSize * nodeCount) overall).
 */
internal class IndexedContent(val text: String) {
    /** `lineStarts[i]` is the char offset where line `i + 1` (1-indexed) begins. */
    private val lineStarts: IntArray =
        buildList {
                add(0)
                for (i in text.indices) {
                    if (text[i] == '\n') add(i + 1)
                }
            }
            .toIntArray()

    /** Converts a 1-indexed (line, column) position to a char offset, or `null` if out of range. */
    fun offsetOf(line: Int, column: Int): Int? {
        val lineStart = lineStarts.getOrNull(line - 1) ?: return null
        val offset = lineStart + (column - 1)
        return if (offset in 0..text.length) offset else null
    }
}

/** A SARIF compatible location referring to a location, i.e. file and region within the file. */
class PhysicalLocation(uri: URI?, region: Region) {
    class ArtifactLocation(val uri: URI?) {

        override fun toString(): String {
            return fileName
        }

        val fileName =
            if (uri != null) {
                uri.path.substring(uri.path.lastIndexOf('/') + 1)
            } else {
                "unknown"
            }

        /**
         * Populated once, the first time this file's
         * [de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit] is parsed (see
         * `NodeBuilder.setCodeAndLocation`), with that TU's own code -- which is the whole file's
         * text. Other nodes in the same file can then intern their `code` as an offset range into
         * it directly (via this already-interned [ArtifactLocation], reached through
         * [PhysicalLocation.artifactLocation]) without a separate cache lookup or reading the file
         * from disk a second time.
         */
        @Volatile internal var indexedContent: IndexedContent? = null

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ArtifactLocation) return false
            return uri == other.uri
        }

        override fun hashCode() = Objects.hashCode(fileName)

        companion object {
            private val unknown = ArtifactLocation(null)
            private val cache = ConcurrentHashMap<URI, ArtifactLocation>()

            /**
             * Upper bound on the number of interned [ArtifactLocation]s. The interning only needs
             * to help within the working set of files currently being analyzed; because an
             * [ArtifactLocation] is value-equal and cheap to rebuild, dropping cached entries only
             * forgoes sharing, never correctness. Bounding the cache prevents unbounded growth in
             * long-lived processes (e.g. server mode) that analyze many distinct files over time.
             */
            private const val MAX_CACHE_SIZE = 50_000

            /**
             * Returns a (shared) [ArtifactLocation] for [uri]. Since an [ArtifactLocation] is
             * immutable and value-equal by [uri], and every node in a file shares the same URI, we
             * intern one instance per URI instead of reconstructing a wrapper (and recomputing
             * [fileName]) for every located node. The cache is bounded by [MAX_CACHE_SIZE] distinct
             * URIs; once that limit is reached it is cleared and repopulated on demand.
             */
            fun of(uri: URI?): ArtifactLocation {
                if (uri == null) return unknown
                // Cheap, racy bound. Occasional over-shoot or double-clear across threads is
                // harmless: entries are pure caches and any dropped instance is simply rebuilt.
                if (cache.size >= MAX_CACHE_SIZE) {
                    cache.clear()
                }
                return cache.computeIfAbsent(uri) { ArtifactLocation(it) }
            }
        }
    }

    val artifactLocation: ArtifactLocation

    // The region is stored as four flat Int fields rather than a dedicated Region object, saving
    // one object header per location; Region (still the public type everywhere else) is
    // materialized on demand by the region getter/setter below.
    private var startLine = 0
    private var startColumn = 0
    private var endLine = 0
    private var endColumn = 0

    var region: Region
        get() = Region(startLine, startColumn, endLine, endColumn)
        set(value) {
            startLine = value.startLine
            startColumn = value.startColumn
            endLine = value.endLine
            endColumn = value.endColumn
        }

    init {
        artifactLocation = ArtifactLocation.of(uri)
        this.region = region
    }

    override fun toString(): String {
        return "$artifactLocation($region)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PhysicalLocation) return false
        // Compares the flattened fields directly rather than via the region getter (which would
        // otherwise allocate two throwaway Region instances just to compare them).
        return artifactLocation == other.artifactLocation &&
            startLine == other.startLine &&
            startColumn == other.startColumn &&
            endLine == other.endLine &&
            endColumn == other.endColumn
    }

    // Delegates to the region getter (rather than hashing the flattened fields directly) so the
    // computed value is byte-for-byte identical to before this class stored a Region object --
    // Node.id (used for persistence) is derived from this transitively, so the actual hashCode
    // *value*, not just self-consistency with equals(), matters here.
    override fun hashCode() = Objects.hash(artifactLocation, region)

    companion object {
        fun locationLink(location: PhysicalLocation?): String {
            return if (location != null) {
                "${location.artifactLocation}:${location.region.startLine}:${location.region.startColumn}"
            } else "unknown"
        }
    }
}

/** Converts a [File] to a [PhysicalLocation]. */
fun Path.toLocation(): PhysicalLocation {
    return PhysicalLocation(
        uri = this.toUri(),
        region = Region(startLine = -1, startColumn = -1, endLine = -1, endColumn = -1),
    )
}
