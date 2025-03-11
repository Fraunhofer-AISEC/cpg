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

import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend.Companion.log
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.OverlayNode
import de.fraunhofer.aisec.cpg.helpers.Util

/**
 * A helper function to check for execution order from one [OverlayNode] to another. These nodes are
 * usually not directly connected via the EOG, thus we have to check whether the underlying node has
 * an execution path to another node which has the desired [endNode] as [Node.overlays].
 *
 * @param startNode The [OverlayNode] to start at. Search begins at the connected
 *   [OverlayNode.underlyingNode].
 * @param endNode The [OverlayNode] to look for. We check whether the node on the EOG path has
 *   [endNode] as one of its [Node.overlays].
 * @return True, if an execution path was found. False if no pass was found or an error occurred
 *   (check the log).
 */
fun executionPathHelper(startNode: OverlayNode, endNode: OverlayNode): Boolean {
    val startUnderlyingNode = startNode.underlyingNode
    if (startUnderlyingNode == null) {
        Util.errorWithFileLocation(
            startNode,
            log, // TODO which log should be used here?
            "Expected to find a underlying node. Returning false.",
        )
        return false
    }
    return executionPath(startNode = startUnderlyingNode) { node ->
            node.overlays.any { overlay -> overlay == endNode }
        }
        .value
}
