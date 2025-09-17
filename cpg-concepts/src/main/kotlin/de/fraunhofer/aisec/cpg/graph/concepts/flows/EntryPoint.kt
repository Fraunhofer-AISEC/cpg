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
package de.fraunhofer.aisec.cpg.graph.concepts.flows

import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.arch.OperatingSystemArchitecture
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import java.util.Objects

/**
 * Represents an entry point into the execution of the program. This can be a "local" entry point,
 * such as a main function, a library initialization function or a "remote" entry point, such as a
 * network endpoint.
 */
abstract class EntryPoint(underlyingNode: FunctionDeclaration?) : Concept()

/** Represents a local entry point into the execution of the program, such as a main function. */
abstract class LocalEntryPoint(
    underlyingNode: FunctionDeclaration?,
    /**
     * If this entry point is specifically designed to be invoked on a certain
     * [OperatingSystemArchitecture], it can be specified here.
     */
    var os: OperatingSystemArchitecture,
) : EntryPoint(underlyingNode = underlyingNode) {
    override fun equals(other: Any?): Boolean {
        return other is LocalEntryPoint && super.equals(other) && other.os == this.os
    }

    override fun hashCode() = Objects.hash(super.hashCode(), os)
}

/** The main function of a program. */
open class Main(underlyingNode: FunctionDeclaration? = null, os: OperatingSystemArchitecture) :
    LocalEntryPoint(underlyingNode = underlyingNode, os = os)

/** Represents an entry point that is triggered if the code is loaded as a (dynamic) library. */
open class LibraryEntryPoint(
    underlyingNode: FunctionDeclaration? = null,
    os: OperatingSystemArchitecture,
) : LocalEntryPoint(underlyingNode = underlyingNode, os = os)

/** Represents an entry point that can be triggered remotely, such as a network endpoint. */
abstract class RemoteEntryPoint(underlyingNode: FunctionDeclaration?) :
    EntryPoint(underlyingNode = underlyingNode)
