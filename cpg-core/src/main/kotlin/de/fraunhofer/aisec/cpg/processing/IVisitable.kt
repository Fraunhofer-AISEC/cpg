/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.processing

import de.fraunhofer.aisec.cpg.graph.Node

/**
 * An object that can be visited by a visitor.
 *
 * @param <V> </V>
 */
interface IVisitable {
    /**
     * @param strategy Traversal strategy.
     * @param visitor Instance of the visitor to call.
     */
    fun <V : Node> accept(strategy: IStrategy<V>, visitor: IVisitor<V>) {
        @Suppress("UNCHECKED_CAST")
        if (visitor.visited.add(this as V)) {
            visitor.visit(this)
            val it = strategy.getIterator(this)
            while (it.hasNext()) {
                it.next().accept(strategy, visitor)
            }
        }
    }
}
