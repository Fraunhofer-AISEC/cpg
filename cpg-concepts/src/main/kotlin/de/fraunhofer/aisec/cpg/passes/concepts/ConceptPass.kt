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
import de.fraunhofer.aisec.cpg.graph.component
import de.fraunhofer.aisec.cpg.graph.conceptNodes
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.TranslationUnitPass

/**
 * An abstract pass that is used to identify and create [Concept] and [Operation] nodes in the
 * graph.
 */
abstract class ConceptPass(ctx: TranslationContext) : TranslationUnitPass(ctx) {

    lateinit var walker: SubgraphWalker.ScopedWalker

    override fun accept(tu: TranslationUnitDeclaration) {
        ctx.currentComponent = tu.component
        walker = SubgraphWalker.ScopedWalker(ctx.scopeManager)
        walker.registerHandler { _, _, node -> handleNode(node, tu) }

        walker.iterate(tu)
    }

    /**
     * This function is called for each node in the graph. It needs to be overridden by subclasses
     * to handle the specific node.
     */
    abstract fun handleNode(node: Node?, tu: TranslationUnitDeclaration)

    /**
     * Gets concept of type [T] for this [TranslationUnitDeclaration] or creates a new one if it
     * does not exist.
     */
    internal inline fun <reified T : Concept> TranslationUnitDeclaration.getConceptOrCreate(): T {
        var concept = this.conceptNodes.filterIsInstance<T>().singleOrNull()
        if (concept == null) {
            concept = T::class.constructors.first().call(this)
        }

        return concept
    }

    override fun cleanup() {
        // Nothing to do
    }
}
