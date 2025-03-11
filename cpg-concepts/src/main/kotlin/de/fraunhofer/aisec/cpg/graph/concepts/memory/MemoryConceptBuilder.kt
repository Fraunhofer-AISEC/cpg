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
package de.fraunhofer.aisec.cpg.graph.concepts.memory

import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.MetadataProvider
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.arch.OperatingSystemArchitecture
import de.fraunhofer.aisec.cpg.graph.concepts.flows.LibraryEntryPoint
import de.fraunhofer.aisec.cpg.graph.concepts.newConcept
import de.fraunhofer.aisec.cpg.graph.concepts.newOperation
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration

/**
 * Creates a new [Memory] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param mode The [MemoryManagementMode] which is used to manage the memory.
 * @return The created [Memory] concept.
 */
fun MetadataProvider.newMemory(underlyingNode: Node, mode: MemoryManagementMode) =
    newConcept({ Memory(it, mode = mode) }, underlyingNode = underlyingNode)

/**
 * Creates a new [DynamicLoading] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @return The created [DynamicLoading] concept.
 */
fun MetadataProvider.newDynamicLoading(underlyingNode: Node) =
    newConcept(::DynamicLoading, underlyingNode = underlyingNode)

/**
 * Creates a new [Allocate] operation.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param what Defines the object whose memory is allocated.
 * @return The created [Allocate] concept.
 */
fun Concept.newAllocate(underlyingNode: Node, what: Node?) =
    newOperation(
        { underlyingNode, concept ->
            Allocate(underlyingNode = underlyingNode, concept = concept, what = what)
        },
        underlyingNode = underlyingNode,
        concept = this,
    )

/**
 * Creates a new [DeAllocate] operation.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param what Defines the object whose memory is deallocated.
 * @return The created [DeAllocate] concept.
 */
fun Concept.newDeallocate(underlyingNode: Node, what: Node?) =
    newOperation(
        { underlyingNode, concept ->
            DeAllocate(underlyingNode = underlyingNode, concept = concept, what = what)
        },
        underlyingNode = underlyingNode,
        concept = this,
    )

/**
 * Creates a new [LoadLibrary] operation.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param what Defines which component is loaded.
 * @param entryPoints A list of the entry points of the library.
 * @param os The operating system architecture. Can be `null`.
 * @return The created [LoadLibrary] concept.
 */
fun Concept.newLoadLibrary(
    underlyingNode: Node,
    what: Component?,
    entryPoints: List<LibraryEntryPoint>,
    os: OperatingSystemArchitecture?,
) =
    newOperation(
        { underlyingNode, concept ->
            LoadLibrary(
                underlyingNode = underlyingNode,
                concept = concept,
                what = what,
                entryPoints = entryPoints,
                os = os,
            )
        },
        underlyingNode = underlyingNode,
        concept = this,
    )

/**
 * Creates a new [LoadSymbol] operation.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param what Defines which symbol is loaded.
 * @param loader If we are loading a symbol from an external library, this points to the
 *   [LoadLibrary] operation that loaded the library. Can be `null`.
 * @param os The operating system architecture. Can be `null`.
 * @return The created [LoadSymbol] concept.
 */
fun <T : Declaration> Concept.newLoadSymbol(
    underlyingNode: Node,
    what: T?,
    loader: LoadLibrary?,
    os: OperatingSystemArchitecture?,
) =
    newOperation(
        { underlyingNode, concept ->
            LoadSymbol<T>(
                underlyingNode = underlyingNode,
                concept = concept,
                what = what,
                loader = loader,
                os = os,
            )
        },
        underlyingNode = underlyingNode,
        concept = this,
    )
