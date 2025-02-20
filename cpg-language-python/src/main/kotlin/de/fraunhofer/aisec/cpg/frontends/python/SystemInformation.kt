/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.python

import de.fraunhofer.aisec.cpg.graph.OverlayNode

/** Represents the contents of `sys.version_info` which contains the Python version. */
data class VersionInfo(var major: Long? = null, var minor: Long? = null, var micro: Long? = null) :
    OverlayNode(), Comparable<VersionInfo> {
    /**
     * Returns the version info as a tuple (major, minor, micro). The length of the tuple depends on
     * the information set, e.g., if only major version is set, then the list is 1 element long.
     */
    fun toList(): List<Long> {
        val list = mutableListOf<Long>()
        major?.let { major ->
            list += major
            minor?.let { minor ->
                list += minor
                micro?.let { micro -> list += micro }
            }
        }

        return list
    }

    override fun compareTo(other: VersionInfo): Int {
        val thisMajor = this.major ?: -1
        val otherMajor = other.major ?: -1
        val thisMinor = this.minor ?: -1
        val otherMinor = other.minor ?: -1
        val thisMicro = this.micro ?: -1
        val otherMicro = other.micro ?: -1
        return if (thisMajor == otherMajor && thisMinor == otherMinor && thisMicro == otherMicro) {
            0
        } else if (
            thisMajor < otherMajor ||
                (thisMajor == otherMajor &&
                    (thisMinor < otherMinor || thisMinor == otherMinor && thisMicro < otherMicro))
        ) {
            -1
        } else 1
    }
}

/**
 * Represents different system information that are used in the [PythonValueEvaluator] to evaluate
 * expressions, such as `sys.platform` and `sys.version_info`.
 *
 * We model this as an overlay node so we can access this information later in the graph.
 */
data class SystemInformation(var versionInfo: VersionInfo? = null, var platform: String? = null) :
    OverlayNode()
