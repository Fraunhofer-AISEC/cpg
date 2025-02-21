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
package de.fraunhofer.aisec.cpg.passes.concepts

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.conceptNodes
import de.fraunhofer.aisec.cpg.graph.concepts.file.FileOperationNode
import de.fraunhofer.aisec.cpg.graph.followEOGEdgesUntilHit
import de.fraunhofer.aisec.cpg.passes.ComponentPass
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteLate

@ExecuteLate
@DependsOn(FileConceptPass::class)
class FileConceptEOGPass(ctx: TranslationContext) : ComponentPass(ctx) {
    private val allCPGConceptNodes: MutableMap<Node, FileOperationNode> = HashMap()

    override fun cleanup() {
        // nothing to do
    }

    override fun accept(comp: Component) {
        val parent = comp.astParent
        if (parent is TranslationResult) {
            val conceptNodes = parent.conceptNodes.filterIsInstance<FileOperationNode>()
            conceptNodes.forEach { concept ->
                concept.underlyingNode?.let { underlyingNode ->
                    allCPGConceptNodes += underlyingNode to concept
                }
            }
            conceptNodes.forEach { handle(it) }
        } else {
            TODO("Failed to find a translation result.")
        }
    }

    private fun handle(fileOp: FileOperationNode) {
        val nextEOGFulfilled =
            fileOp.underlyingNode
                ?.followEOGEdgesUntilHit(collectFailedPaths = false) {
                    it in allCPGConceptNodes.keys
                }
                ?.fulfilled
        nextEOGFulfilled?.forEach {
            val lastCPGNodeInEOGPath =
                it.lastOrNull() ?: TODO("Fulfilled path should not be empty.")
            val nextEOGConcept =
                allCPGConceptNodes[lastCPGNodeInEOGPath]
                    ?: TODO("Failed to find the matching concept in our map.")
            fileOp.nextEOG += nextEOGConcept
        }
    }
}
