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

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.arch.OperatingSystemArchitecture

/**
 * Represents an entry point into the execution of the program. This can be a "local" entry point,
 * such as a main function, a library initialization function or a "remote" entry point, such as a
 * network endpoint.
 */
abstract class EntryPoint(underlyingNode: Node) : Concept(underlyingNode = underlyingNode)

/** Represents a local entry point into the execution of the program, such as a main function. */
abstract class LocalEntryPoint(
    underlyingNode: Node,
    /**
     * If this entry point is specifically designed to be invoked on a certain
     * [OperatingSystemArchitecture], it can be specified here.
     */
    var os: OperatingSystemArchitecture? = null,
) : EntryPoint(underlyingNode = underlyingNode)

/** The main function of a program. */
class Main(underlyingNode: Node, os: OperatingSystemArchitecture? = null) :
    LocalEntryPoint(underlyingNode = underlyingNode, os = os)

/** Represents an entry point that is triggered if the code is loaded as a (dynamic) library. */
class LibraryEntryPoint(underlyingNode: Node, os: OperatingSystemArchitecture? = null) :
    LocalEntryPoint(underlyingNode = underlyingNode, os = os)

/** Represents an entry point that can be triggered remotely, such as a network endpoint. */
abstract class RemoteEntryPoint(underlyingNode: Node) : EntryPoint(underlyingNode = underlyingNode)
