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
import de.fraunhofer.aisec.cpg.graph.ast.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.concepts.arch.OperatingSystemArchitecture
import de.fraunhofer.aisec.cpg.graph.concepts.flows.LibraryEntryPoint
import de.fraunhofer.aisec.cpg.graph.concepts.newConcept
import de.fraunhofer.aisec.cpg.graph.concepts.newOperation

/**
 * Creates a new [Memory] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param mode The [MemoryManagementMode] which is used to manage the memory.
 * @param connect If `true`, the created [Concept] will be connected to the underlying node by
 *   setting its `underlyingNode`.
 * @return The created [Memory] concept.
 */
fun MetadataProvider.newMemory(underlyingNode: Node, mode: MemoryManagementMode, connect: Boolean) =
    newConcept({ Memory(mode = mode) }, underlyingNode = underlyingNode, connect = connect)

/**
 * Creates a new [DynamicLoading] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param connect If `true`, the created [Concept] will be connected to the underlying node by
 *   setting its `underlyingNode`.
 * @return The created [DynamicLoading] concept.
 */
fun MetadataProvider.newDynamicLoading(underlyingNode: Node, connect: Boolean) =
    newConcept(::DynamicLoading, underlyingNode = underlyingNode, connect = connect)

/**
 * Creates a new [Allocate] operation.
 *
 * @param underlyingNode The underlying node representing this operation.
 *     * @param concept The [Concept] concept this operation belongs to.
 *
 * @param what Defines the object whose memory is allocated.
 * @param connect If `true`, the created [Operation] will be connected to the underlying node by
 *   setting its `underlyingNode` and inserting it in the EOG , to [concept] by its edge
 *   [Concept.ops].
 * @return The created [Allocate] concept.
 */
fun MetadataProvider.newAllocate(
    underlyingNode: Node,
    concept: Concept,
    what: Node?,
    connect: Boolean,
) =
    newOperation(
        { concept -> Allocate(concept = concept, what = what) },
        underlyingNode = underlyingNode,
        concept = concept,
        connect = connect,
    )

/**
 * Creates a new [DeAllocate] operation.
 *
 * @param underlyingNode The underlying node representing this operation.
 *     * @param concept The [Concept] concept this operation belongs to.
 *
 * @param what Defines the object whose memory is deallocated.
 * @param connect If `true`, the created [Operation] will be connected to the underlying node by
 *   setting its `underlyingNode` and inserting it in the EOG , to [concept] by its edge
 *   [Concept.ops].
 * @return The created [DeAllocate] concept.
 */
fun MetadataProvider.newDeallocate(
    underlyingNode: Node,
    concept: Concept,
    what: Node?,
    connect: Boolean,
) =
    newOperation(
        { concept -> DeAllocate(concept = concept, what = what) },
        underlyingNode = underlyingNode,
        concept = concept,
        connect = connect,
    )

/**
 * Creates a new [LoadLibrary] operation.
 *
 * @param underlyingNode The underlying node representing this operation.
 *     * @param concept The [Concept] concept this operation belongs to.
 *
 * @param what Defines which component is loaded.
 * @param entryPoints A list of the entry points of the library.
 * @param os The operating system architecture. Can be `null`.
 * @param connect If `true`, the created [Operation] will be connected to the underlying node by
 *   setting its `underlyingNode` and inserting it in the EOG , to [concept] by its edge
 *   [Concept.ops].
 * @return The created [LoadLibrary] concept.
 */
fun MetadataProvider.newLoadLibrary(
    underlyingNode: Node,
    concept: Concept,
    what: Component?,
    entryPoints: List<LibraryEntryPoint>,
    os: OperatingSystemArchitecture?,
    connect: Boolean,
) =
    newOperation(
        { concept ->
            LoadLibrary(concept = concept, what = what, entryPoints = entryPoints, os = os)
        },
        underlyingNode = underlyingNode,
        concept = concept,
        connect = connect,
    )

/**
 * Creates a new [LoadSymbol] operation.
 *
 * @param underlyingNode The underlying node representing this operation.
 * @param concept The [Concept] concept this operation belongs to.
 * @param what Defines which symbol is loaded.
 * @param loader If we are loading a symbol from an external library, this points to the
 *   [LoadLibrary] operation that loaded the library. Can be `null`.
 * @param os The operating system architecture. Can be `null`.
 * @return The created [LoadSymbol] concept.
 */
fun <T : Declaration> MetadataProvider.newLoadSymbol(
    underlyingNode: Node,
    concept: Concept,
    what: T?,
    loader: LoadLibrary?,
    os: OperatingSystemArchitecture?,
    connect: Boolean,
) =
    newOperation(
        { concept -> LoadSymbol<T>(concept = concept, what = what, loader = loader, os = os) },
        underlyingNode = underlyingNode,
        concept = concept,
        connect = connect,
    )
