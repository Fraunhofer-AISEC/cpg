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
import de.fraunhofer.aisec.cpg.graph.concepts.file.File
import de.fraunhofer.aisec.cpg.graph.concepts.file.FileLikeObject
import de.fraunhofer.aisec.cpg.graph.concepts.file.FileTempFileStatus
import de.fraunhofer.aisec.cpg.graph.concepts.file.newFile
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.passes.DFGPass
import de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass
import de.fraunhofer.aisec.cpg.passes.concepts.EOGConceptPass
import de.fraunhofer.aisec.cpg.passes.concepts.NodeToOverlayStateElement
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteBefore
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteLate
import de.fraunhofer.aisec.cpg.passes.reconstructedImportName

/**
 * This pass handles the `os.path.join` calls in Python code and creates [File] nodes from them.
 * This is done by removing exising File nodes in the arguments and replacing them with new [File]
 * nodes representing the concatenated file. If the arguments are strings, a new [File] node is
 * created with the concatenated string as the file name. If any of the arguments is a temporary
 * file, the resulting file will also be marked as a temporary file. This pass must be executed
 * before the [PythonFileConceptPass] because the latter builds upon the nodes being available in
 * the graph. It must be executed after the [PythonTempFilePass] to ensure that the temporary-file
 * nodes are created before the join operation is handled.
 */
@ExecuteLate
@DependsOn(DFGPass::class, false)
@DependsOn(EvaluationOrderGraphPass::class, false)
@DependsOn(PythonTempFilePass::class, false)
@ExecuteBefore(PythonFileConceptPass::class, false)
class PythonFileJoinPass(ctx: TranslationContext) : EOGConceptPass(ctx) {
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
            "os.path.join" -> {
                handlePathJoin(node)
            }
            else -> {
                // We currently do not handle other calls
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
            "os.path.join" -> {
                handlePathJoin(node)
            }
            else -> {
                // We currently do not handle other calls
                emptyList()
            }
        }
    }

    private fun handlePathJoin(callExpression: CallExpression): Collection<OverlayNode> {
        val combinedFileName = mutableListOf<String>()
        var tempFileStatus = FileTempFileStatus.NOT_A_TEMP_FILE
        callExpression.arguments.forEach { argument ->
            if (argument.overlays.any { it is FileLikeObject }) {
                combinedFileName +=
                    argument.overlays.filterIsInstance<FileLikeObject>().joinToString {
                        it.fileName
                    }
                if (
                    argument.overlays.filterIsInstance<FileLikeObject>().any {
                        it.isTempFile == FileTempFileStatus.TEMP_FILE
                    }
                ) {
                    tempFileStatus = FileTempFileStatus.TEMP_FILE
                }
                argument.overlays.filterIsInstance<FileLikeObject>().forEach { file ->
                    // not disconnecting from graph because the file exists, even if we only use it
                    // in the "joined from"
                    // log.debug("Disconnecting file from graph: {}", file.fileName)
                    // file.disconnectFromGraph()
                }
            } else if (argument is Literal<*>) {
                val evaluatedArg = argument.value.toString()
                if (evaluatedArg.startsWith("/tmp")) {
                    // TODO this should only be set if the file path *starts* with /tmp, not if it
                    // contains it
                    tempFileStatus = FileTempFileStatus.TEMP_FILE
                }
                combinedFileName += evaluatedArg
            } else {
                log.warn("Unexpected argument: \"{}\". This will be ignored.", argument)
            }
        }

        return listOf(
            newFile(
                    underlyingNode = callExpression,
                    fileName = combinedFileName.joinToString("/"),
                    connect = false,
                )
                .apply { this.isTempFile = tempFileStatus }
                .also { file -> log.debug("Created new file from path join: {}", file.fileName) }
        )
    }
}
