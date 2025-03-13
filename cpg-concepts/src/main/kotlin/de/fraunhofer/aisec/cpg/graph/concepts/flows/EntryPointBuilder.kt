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

import de.fraunhofer.aisec.cpg.graph.MetadataProvider
import de.fraunhofer.aisec.cpg.graph.concepts.arch.OperatingSystemArchitecture
import de.fraunhofer.aisec.cpg.graph.concepts.newConcept
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration

/**
 * Creates a new [Main] concept.
 *
 * @param underlyingNode The underlying [FunctionDeclaration] representing the main method.
 * @param os If this entry point is specifically designed to be invoked on a certain
 *   [OperatingSystemArchitecture], it can be specified here.
 * @return The created [Main] concept.
 */
fun MetadataProvider.newMain(underlyingNode: FunctionDeclaration, os: OperatingSystemArchitecture) =
    newConcept({ Main(underlyingNode = underlyingNode, os = os) }, underlyingNode = underlyingNode)

/**
 * Creates a new [LibraryEntryPoint] concept.
 *
 * @param underlyingNode The underlying [FunctionDeclaration] representing the main method.
 * @param os If this entry point is specifically designed to be invoked on a certain
 *   [OperatingSystemArchitecture], it can be specified here.
 * @return The created [LibraryEntryPoint] concept.
 */
fun MetadataProvider.newLibraryEntryPoint(
    underlyingNode: FunctionDeclaration,
    os: OperatingSystemArchitecture,
) =
    newConcept(
        { LibraryEntryPoint(underlyingNode = underlyingNode, os = os) },
        underlyingNode = underlyingNode,
    )
