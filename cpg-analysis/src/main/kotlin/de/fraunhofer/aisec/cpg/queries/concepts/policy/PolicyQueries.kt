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
package de.fraunhofer.aisec.cpg.queries.concepts.policy

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.Backward
import de.fraunhofer.aisec.cpg.graph.GraphToFollow
import de.fraunhofer.aisec.cpg.graph.concepts.policy.CheckAccess
import de.fraunhofer.aisec.cpg.graph.concepts.policy.ExitBoundary
import de.fraunhofer.aisec.cpg.graph.concepts.policy.ProtectedAsset
import de.fraunhofer.aisec.cpg.graph.followPrevCDGUntilHit
import de.fraunhofer.aisec.cpg.graph.hasOverlay
import de.fraunhofer.aisec.cpg.query.QueryTree
import de.fraunhofer.aisec.cpg.query.allExtended
import de.fraunhofer.aisec.cpg.query.dataFlow
import de.fraunhofer.aisec.cpg.query.toQueryTree

/**
 * Checks, if all dataflows of a [ProtectedAsset] that flow through an [ExitBoundary] operation are
 * protected by a [CheckAccess].
 */
context(tr: TranslationResult)
fun assetsAreProtected(): QueryTree<Boolean> {
    return tr.allExtended<ExitBoundary>(
        sel = {
            dataFlow(
                    startNode = it,
                    direction = Backward(GraphToFollow.DFG),
                    predicate = { node ->
                        // We only want to consider assets that are actually protected
                        node.hasOverlay<ProtectedAsset>()
                    },
                )
                .value
        },
        mustSatisfy = {
            // TODO(oxisto): We need to check if the check access is actually matching the policy
            val paths =
                it.followPrevCDGUntilHit(predicate = { node -> node.hasOverlay<CheckAccess>() })
            paths.failed.isEmpty().toQueryTree()
        },
    )
}
