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
package de.fraunhofer.aisec.cpg.passes.concepts

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.graph.Backward
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.GraphToFollow
import de.fraunhofer.aisec.cpg.graph.IfdsSummaryCache
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.OverlayNode
import de.fraunhofer.aisec.cpg.graph.component
import de.fraunhofer.aisec.cpg.graph.nodes
import de.fraunhofer.aisec.cpg.passes.ControlFlowSensitiveDFGPass
import de.fraunhofer.aisec.cpg.passes.DFGPass
import de.fraunhofer.aisec.cpg.passes.Description
import de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass
import de.fraunhofer.aisec.cpg.passes.PassConfiguration
import de.fraunhofer.aisec.cpg.passes.PointsToPass
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import java.util.Collections
import java.util.WeakHashMap

/**
 * This pass can be used to "tag" [OverlayNode]s to a specific "underlying" [Node] using the
 * [EOGConceptPass]. It basically uses the [handleNode] function to introduce the individual items
 * of a [TaggingContext] into the state of the [EOGConceptPass].
 */
@DependsOn(SymbolResolver::class)
@DependsOn(ControlFlowSensitiveDFGPass::class, true)
@DependsOn(PointsToPass::class, true)
@DependsOn(DFGPass::class, true)
@DependsOn(EvaluationOrderGraphPass::class)
@Description("Tags overlay nodes to underlying nodes based on a tagging context.")
open class TagOverlaysPass(ctx: TranslationContext) : EOGConceptPass(ctx) {

    open class Configuration(var tag: TaggingContext) : PassConfiguration()

    /**
     * Installs an [IfdsSummaryCache] on the current [Component] (once per component) before running
     * the EOG fixpoint. The cache lets [ifdsReachingSources]-backed queries (e.g. `followDFG...`)
     * reuse predicate-independent balanced callee summaries for callees that provably contain no
     * sink, which is the common case during tagging. Its soundness contract requires that every
     * potential sink is [IfdsSummaryCache.markSink]-marked: earlier concept passes may already have
     * attached overlays before this pass runs, so we seed the dirty set from all pre-existing
     * overlays here, while overlays added during this pass are marked via the hooks in
     * [EOGConceptPass].
     */
    override fun accept(node: Node) {
        node.component?.let { component ->
            if (component.ifdsSummaryCache == null) {
                val cache = IfdsSummaryCache(Backward(GraphToFollow.DFG))
                // Seed the dirty set with every node that already carries an overlay, so callees
                // containing pre-existing sinks are never short-circuited.
                for (n in component.nodes) {
                    if (n.overlays.isNotEmpty()) {
                        cache.markSink(n)
                    }
                }
                component.ifdsSummaryCache = cache
                installedCaches.getOrPut(ctx) {
                    Collections.newSetFromMap(WeakHashMap<Component, Boolean>())
                } += component
            }
        }
        super.accept(node)
    }

    /** Tears down the per-pass caches installed for this [TranslationContext]. */
    override fun finalCleanup() {
        installedCaches.remove(ctx)?.forEach { it.ifdsSummaryCache = null }
        super.finalCleanup()
    }

    companion object {
        /**
         * Tracks the [Component]s on which this pass installed an [IfdsSummaryCache], keyed by the
         * [TranslationContext] of the analysis, so [finalCleanup] (which runs on the prototype
         * instance) can clear them. Keyed weakly to avoid retaining components/contexts.
         */
        private val installedCaches: MutableMap<TranslationContext, MutableSet<Component>> =
            Collections.synchronizedMap(WeakHashMap())
    }

    override fun handleNode(
        lattice: NodeToOverlayState,
        state: NodeToOverlayStateElement,
        node: Node,
    ): Collection<OverlayNode> {
        // Collect all concept / operation nodes in the context. For now this will just be in the
        // order they are specified in the context.
        val nodes = passConfig<Configuration>()?.tag?.collect(lattice, state, node) ?: emptyList()

        return nodes
    }
}
