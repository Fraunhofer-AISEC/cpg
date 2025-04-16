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

import de.fraunhofer.aisec.cpg.graph.MetadataProvider
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.newConcept

/**
 * Creates a new [Agnostic] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param connect If `true`, the created [Concept] will be connected to the underlying node by
 *   setting its `underlyingNode`.
 * @return The created [Agnostic] concept.
 */
fun MetadataProvider.newAgnosticOS(underlyingNode: Node, connect: Boolean) =
    newConcept(::Agnostic, underlyingNode = underlyingNode, connect = connect)

/**
 * Creates a new [Win32] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param connect If `true`, the created [Concept] will be connected to the underlying node by
 *   setting its `underlyingNode`.
 * @return The created [Win32] concept.
 */
fun MetadataProvider.newWin32OS(underlyingNode: Node, connect: Boolean) =
    newConcept(::Win32, underlyingNode = underlyingNode, connect = connect)

/**
 * Creates a new [POSIX] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param connect If `true`, the created [Concept] will be connected to the underlying node by
 *   setting its `underlyingNode`.
 * @return The created [POSIX] concept.
 */
fun MetadataProvider.newPosixOS(underlyingNode: Node, connect: Boolean) =
    newConcept(::POSIX, underlyingNode = underlyingNode, connect = connect)

/**
 * Creates a new [Darwin] concept.
 *
 * @param underlyingNode The underlying node representing this concept.
 * @param connect If `true`, the created [Concept] will be connected to the underlying node by
 *   setting its `underlyingNode`.
 * @return The created [Darwin] concept.
 */
fun MetadataProvider.newDarwinOS(underlyingNode: Node, connect: Boolean) =
    newConcept(::Darwin, underlyingNode = underlyingNode, connect = connect)
