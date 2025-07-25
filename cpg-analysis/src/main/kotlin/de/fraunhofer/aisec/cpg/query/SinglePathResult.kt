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
package de.fraunhofer.aisec.cpg.query

import de.fraunhofer.aisec.cpg.graph.Node

/**
 * Represents a single path result of a query evaluation. It contains the [value] of the path, the
 * [children] that were evaluated to reach this path, the [stringRepresentation] of the path, the
 * [node] that was evaluated, and the [terminationReason] that explains why this path was
 * terminated.
 */
class SinglePathResult(
    value: Boolean,
    children: List<QueryTree<*>> = emptyList(),
    stringRepresentation: String = "",
    node: Node? = null,
    val terminationReason: TerminationReason,
    operator: GenericQueryOperators,
) :
    QueryTree<Boolean>(
        value = value,
        children = children,
        stringRepresentation = stringRepresentation,
        node = node,
        operator = operator,
    )
