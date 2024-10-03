/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression

/**
 * This interface denotes that a [Node] "holds" a list of other nodes. See also [ArgumentHolder] and
 * [StatementHolder], in which [Holder] is used as a common interface.
 *
 * A primary use-case for the usage of this interface is the Node Fluent DSL in order to create node
 * objects which can either be used as a statement (e.g. in a [Block]) or as an argument (e.g. of a
 * [CallExpression]).
 */
interface Holder<NodeTypeToHold : Node> {

    /**
     * Replaces the existing node specified in [old] with the one in [new]. Implementation how to do
     * that might be specific to the holder.
     *
     * An indication whether this operation was successful needs to be returned.
     */
    fun replace(old: NodeTypeToHold, new: NodeTypeToHold): Boolean

    /** Adds a [Node] to the list of "held" nodes. */
    operator fun plusAssign(node: NodeTypeToHold)
}
