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
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.conceptNodes
import de.fraunhofer.aisec.cpg.graph.concepts.file.FileOperationNode
import de.fraunhofer.aisec.cpg.graph.followNextEOGEdgesUntilHit
import de.fraunhofer.aisec.cpg.passes.TranslationResultPass
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteLate

@ExecuteLate
@DependsOn(FileConceptPass::class)
class FileConceptEOGPass(ctx: TranslationContext) : TranslationResultPass(ctx) {
    private val allCPGConceptNodes: MutableMap<Node, FileOperationNode> = HashMap()

    override fun cleanup() {
        // nothing to do
    }

    override fun accept(result: TranslationResult) {
        val conceptNodes = result.conceptNodes.filterIsInstance<FileOperationNode>()
        conceptNodes.forEach { allCPGConceptNodes += it.cpgNode to it }
        conceptNodes.forEach { handle(it) }
    }

    private fun handle(fileOp: FileOperationNode) {
        val nextEOGFulfilled =
            fileOp.cpgNode
                .followNextEOGEdgesUntilHit(collectFailedPaths = false) {
                    it in allCPGConceptNodes.keys
                }
                .fulfilled
        nextEOGFulfilled.forEach {
            val lastCPGNodeInEOGPath =
                it.lastOrNull() ?: TODO("Fulfilled path should not be empty.")
            val nextEOGConcept =
                allCPGConceptNodes[lastCPGNodeInEOGPath]
                    ?: TODO("Failed to find the matching concept in our map.")
            fileOp.nextEOG += nextEOGConcept
        }
    }
}
