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
package de.fraunhofer.aisec.cpg.graph.concepts.arch

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.OverlayNode
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import kotlin.reflect.full.isSubclassOf

/** Represents an architecture of an operating system. */
abstract class OperatingSystemArchitecture(underlyingNode: Node) :
    Concept(underlyingNode = underlyingNode) {
    override fun equalWithoutUnderlying(other: OverlayNode): Boolean {
        return other != null && other::class.isSubclassOf(this::class)
    }

    override fun hashCode(): Int {
        return 31 + this::class.hashCode()
    }
}

/** Represents an agnostic architecture, which is not tied to a specific operating system. */
class Agnostic(underlyingNode: Node) : OperatingSystemArchitecture(underlyingNode = underlyingNode)

/** Represents a Win32 architecture, commonly found on Windows systems. */
class Win32(underlyingNode: Node) : OperatingSystemArchitecture(underlyingNode = underlyingNode)

/** Represents a POSIX architecture, commonly found on Linux systems, */
open class POSIX(underlyingNode: Node) :
    OperatingSystemArchitecture(underlyingNode = underlyingNode)

/**
 * Represents a Darwin architecture, commonly found on macOS systems. macOS is a certified
 * [UNIX](https://www.opengroup.org/openbrand/register/apple.htm) and is (mostly) POSIX compatible.
 */
class Darwin(underlyingNode: Node) : POSIX(underlyingNode = underlyingNode)
