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
package de.fraunhofer.aisec.cpg.processing.strategy

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import java.util.*

/** Strategies (iterators) for traversing graphs to be used by visitors.  */
object Strategy {
    /**
     * Do not traverse any nodes.
     *
     * @param x
     * @return
     */
    fun NO_STRATEGY(x: Node): Iterator<Node> {
        return Collections.emptyIterator()
    }

    /**
     * Traverse Evaluation Order Graph in forward direction.
     *
     * @param x Current node in EOG.
     * @return Iterator over successors.
     */
    fun EOG_FORWARD(x: Node): Iterator<Node> {
        return x.nextEOG.iterator()
    }

    /**
     * Traverse Evaluation Order Graph in backward direction.
     *
     * @param x Current node in EOG.
     * @return Iterator over successors.
     */
    fun EOG_BACKWARD(x: Node): Iterator<Node> {
        return x.prevEOG.iterator()
    }

    /**
     * Traverse Data Flow Graph in forward direction.
     *
     * @param x Current node in DFG.
     * @return Iterator over successors.
     */
    fun DFG_FORWARD(x: Node): Iterator<Node> {
        return x.nextDFG.iterator()
    }

    /**
     * Traverse Data Flow Graph in backward direction.
     *
     * @param x Current node in DFG.
     * @return Iterator over successors.
     */
    fun DFG_BACKWARD(x: Node): Iterator<Node> {
        return x.prevDFG.iterator()
    }

    /**
     * Traverse AST in forward direction.
     *
     * @param x
     * @return
     */
    fun AST_FORWARD(x: Node): Iterator<Node> {
        return SubgraphWalker.getAstChildren(x).iterator()
    }
}