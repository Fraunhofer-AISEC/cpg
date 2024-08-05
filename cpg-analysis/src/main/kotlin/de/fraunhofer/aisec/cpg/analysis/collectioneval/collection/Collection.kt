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
package de.fraunhofer.aisec.cpg.analysis.collectioneval.collection

import de.fraunhofer.aisec.cpg.analysis.collectioneval.LatticeInterval
import de.fraunhofer.aisec.cpg.graph.Node

interface Collection {
    /**
     * Applies the effect of a Node to the Interval describing possible values of a collection. Also
     * returns true if the node was "valid" node that could have an influence on the Interval.
     *
     * Examples:
     * - list.add(x) on [0, 0] -> ([1, 1], true)
     * - list.clear(x) on [0, 0] -> ([0, 0], true)
     * - println(list) on [0, 0] -> ([0, 0], false)
     */
    fun applyEffect(
        current: LatticeInterval,
        node: Node,
        name: String
    ): Pair<LatticeInterval, Boolean>

    fun getInitializer(node: Node?): Node?

    fun getInitialRange(initializer: Node): LatticeInterval
}
