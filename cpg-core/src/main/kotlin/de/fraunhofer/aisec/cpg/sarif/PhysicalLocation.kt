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
             * Returns a (shared) [ArtifactLocation] for [uri]. Since an [ArtifactLocation] is
             * immutable and value-equal by [uri], and every node in a file shares the same URI, we
             * intern one instance per URI instead of reconstructing a wrapper (and recomputing
             * [fileName]) for every located node. The cache is bounded by the number of distinct
             * files, not nodes.
             */
            fun of(uri: URI?): ArtifactLocation =
                if (uri == null) unknown else cache.computeIfAbsent(uri) { ArtifactLocation(it) }
        }
    }

    var artifactLocation: ArtifactLocation
    var region: Region

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
        return artifactLocation == other.artifactLocation && region == other.region
    }

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
