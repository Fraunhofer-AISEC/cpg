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
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.allEOGStarters
import de.fraunhofer.aisec.cpg.graph.conceptNodes
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.ComponentPass
import de.fraunhofer.aisec.cpg.passes.ControlFlowSensitiveDFGPass
import de.fraunhofer.aisec.cpg.passes.DynamicInvokeResolver
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import de.fraunhofer.aisec.cpg.passes.Task
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteBefore
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy

typealias ConceptTask = Task<Component, ConceptPass>

/**
 * A pass that is used to identify and create [Concept] and [Operation] nodes in the graph. The idea
 * is that developers that add concepts do not need to implement a full pass, but can just implement
 * a small [Task] that handles the nodes relevant for the concept.
 */
@DependsOn(SymbolResolver::class)
@DependsOn(ControlFlowSensitiveDFGPass::class)
@ExecuteBefore(DynamicInvokeResolver::class)
class ConceptPass(ctx: TranslationContext) : ComponentPass(ctx) {

    lateinit var walker: SubgraphWalker.ScopedWalker

    override fun accept(c: Component) {
        ctx.currentComponent = c
        walker = SubgraphWalker.ScopedWalker(ctx.scopeManager)
        walker.strategy = Strategy::EOG_FORWARD
        walker.registerHandler { node -> handleNode(node) }

        // Process nodes in our translation units in the order depending on their import
        // dependencies
        for (tu in (Strategy::TRANSLATION_UNITS_LEAST_IMPORTS)(c)) {
            log.debug("Processing concepts of translation unit {}", tu.name)

            // Gather all resolution EOG starters; and make sure they really do not have a
            // predecessor, otherwise we might analyze a node multiple times
            val nodes = tu.allEOGStarters.filter { it.prevEOGEdges.isEmpty() }

            for (node in nodes) {
                walker.iterate(node)
            }
        }
    }

    /**
     * This function is called for each node in the graph. It will invoke all plugins that are
     * registered in the pass configuration.
     */
    fun handleNode(node: Node) {
        // Execute tasks
        for (task in tasks) {
            task.handleNode(node)
        }
    }

    override fun cleanup() {
        // Nothing to do
    }
}

/**
 * Gets concept of type [T] for this [TranslationUnitDeclaration] or creates a new one if it does
 * not exist.
 */
inline fun <reified T : Concept> TranslationUnitDeclaration.getConceptOrCreate(
    noinline init: ((T) -> Unit)? = null
): T {
    var concept = this.conceptNodes.filterIsInstance<T>().singleOrNull()
    if (concept == null) {
        concept = T::class.constructors.first().call(this)
        init?.invoke(concept)
    }

    return concept
}
