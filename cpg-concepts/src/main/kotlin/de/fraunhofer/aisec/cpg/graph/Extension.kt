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

import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation

/**
 * Retrieves a set of all [Concept] nodes associated with this [Node] and its AST children
 * ([Node.nodes]).
 *
 * @return A set containing all [Concept] nodes found in the overlays of the [Node] and its
 *   children.
 */
val Node.conceptNodes: Set<Concept>
    get() = this.nodes.flatMapTo(mutableSetOf()) { it.overlays.filterIsInstance<Concept>() }

/**
 * Retrieves a set of all [Operation] nodes associated with this [Node] and its AST children
 * ([Node.nodes]).
 *
 * @return A set containing all [Operation] nodes found in the overlays of the [Node] and its
 *   children.
 */
val Node.operationNodes: Set<Operation>
    get() = this.nodes.flatMapTo(mutableSetOf()) { it.overlays.filterIsInstance<Operation>() }
