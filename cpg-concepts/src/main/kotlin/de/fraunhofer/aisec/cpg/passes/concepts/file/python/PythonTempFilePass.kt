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
package de.fraunhofer.aisec.cpg.passes.concepts.file.python

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.graph.OverlayNode
import de.fraunhofer.aisec.cpg.graph.concepts.file.FileTempFileStatus
import de.fraunhofer.aisec.cpg.graph.concepts.file.newFile
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.passes.DFGPass
import de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass
import de.fraunhofer.aisec.cpg.passes.concepts.EOGConceptPass
import de.fraunhofer.aisec.cpg.passes.concepts.NodeToOverlayStateElement
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteBefore
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteLate
import de.fraunhofer.aisec.cpg.passes.reconstructedImportName
import java.util.*

/**
 * This pass handles various library calls in Python code that are related to temporary files. It
 * must be executed before the [PythonFileJoinPass] to allow the latter to eventually concatenate
 * paths.
 */
@ExecuteLate
@DependsOn(DFGPass::class, false)
@DependsOn(EvaluationOrderGraphPass::class, false)
@ExecuteBefore(PythonFileJoinPass::class, false)
@ExecuteBefore(PythonFileConceptPass::class, false)
class PythonTempFilePass(ctx: TranslationContext) : EOGConceptPass(ctx) {
    override fun handleCallExpression(
        state: NodeToOverlayStateElement,
        node: CallExpression,
    ): Collection<OverlayNode> {
        // Since we cannot directly depend on the Python frontend, we have to check the language
        // here based on the node's language.
        if (node.language.name.localName != "PythonLanguage") {
            return emptyList()
        }

        return when (node.callee.name.toString()) {
            "tempfile.gettempdir" -> {
                handleGetTempDir(node)
            }
            else -> {
                emptyList()
            }
        }
    }

    override fun handleMemberCallExpression(
        state: NodeToOverlayStateElement,
        node: MemberCallExpression,
    ): Collection<OverlayNode> {
        // Since we cannot directly depend on the Python frontend, we have to check the language
        // here based on the node's language.
        if (node.language.name.localName != "PythonLanguage") {
            return emptyList()
        }
        return when (node.reconstructedImportName.toString()) {
            "tempfile.gettempdir" -> {
                handleGetTempDir(node)
            }
            else -> {
                emptyList()
            }
        }
    }

    private fun handleGetTempDir(callExpression: CallExpression): Collection<OverlayNode> {
        return listOf(
            newFile(
                    underlyingNode = callExpression,
                    fileName = "tempfile.gettempdir(${UUID.randomUUID()})", // Unique name to avoid
                    // collisions
                    connect = false,
                )
                .apply { this.isTempFile = FileTempFileStatus.TEMP_FILE }
        )
    }
}
