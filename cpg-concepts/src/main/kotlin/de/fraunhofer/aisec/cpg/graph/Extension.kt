/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation

/**
 * Retrieves a set of [Concept] nodes associated with this [TranslationResult].
 *
 * This property collects all overlay nodes of type [Concept] within the `Node`s of the
 * [TranslationResult], flattening and converting them into a unique set.
 * *
 *
 * @return A set including all instances of [Concept] found in the overlays of the underlying nodes.
 */
val TranslationResult.conceptNodes: Set<Concept<*>>
    get() = this.nodes.flatMapTo(mutableSetOf()) { it.overlays.filterIsInstance<Concept<*>>() }

/**
 * A computed property that retrieves a set of all [Operation] nodes associated with this
 * [TranslationResult]. It collects [Operation] instances from the overlays of the nodes contained
 * in this result.
 *
 * @return A set containing all [Operation] instances found in the overlays of the nodes in the
 *   [TranslationResult].
 */
val TranslationResult.operationNodes: Set<Operation>
    get() = this.nodes.flatMapTo(mutableSetOf()) { it.overlays.filterIsInstance<Operation>() }
