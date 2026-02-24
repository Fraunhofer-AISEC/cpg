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
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.OverlayNode
import de.fraunhofer.aisec.cpg.passes.ControlFlowSensitiveDFGPass
import de.fraunhofer.aisec.cpg.passes.PassConfiguration
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn

/**
 * This pass can be used to "tag" [OverlayNode]s to a specific "underlying" [Node] using the
 * [EOGConceptPass]. It basically uses the [handleNode] function to introduce the individual items
 * of a [TaggingContext] into the state of the [EOGConceptPass].
 */
@DependsOn(SymbolResolver::class)
@DependsOn(ControlFlowSensitiveDFGPass::class)
open class TagOverlaysPass(ctx: TranslationContext) : EOGConceptPass(ctx) {

    open class Configuration(var tag: TaggingContext) : PassConfiguration()

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
