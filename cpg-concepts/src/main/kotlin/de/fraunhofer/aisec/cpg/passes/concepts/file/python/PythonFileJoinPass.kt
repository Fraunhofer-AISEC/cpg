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

    /**
     * Handles the calls to `os.path.join` and creates a new [File] node from the arguments.
     *
     * To do so, it concatenates the file names of the [FileLikeObject]s and [Literal]s in the
     * arguments with the separator `"/"` and generates new [File] nodes with the computed names.
     */
    private fun handlePathJoin(callExpression: CallExpression): Collection<OverlayNode> {
        val allCombinedPaths =
            callExpression.arguments.fold(
                listOf(listOf<String>() to FileTempFileStatus.NOT_A_TEMP_FILE)
            ) { currentPaths, argument ->
                // TODO: This should be more generic and follow the prevDFG paths to find the
                // FileLikeObjects and Literals.
                val fileLikeArgs = argument.overlays.filterIsInstance<FileLikeObject>()
                if (fileLikeArgs.isNotEmpty()) {
                    // If the given argument is a FileLikeObject, we append its fileName and set
                    // that the resulting path is a temp_file if it's a temp file. Otherwise, we
                    // keep the tempFileStatus as it is.
                    fileLikeArgs.flatMap {
                        currentPaths.map { path ->
                            (path.first + it.fileName) to
                                if (it.isTempFile == FileTempFileStatus.TEMP_FILE)
                                    FileTempFileStatus.TEMP_FILE
                                else path.second
                        }
                    }
                } else if (argument is Literal<*>) {
                    // If the given argument is a Literal, we append its value as a string to the
                    // current paths. We keep the tempFileStatus as it is.
                    val evaluatedArg = argument.value.toString()
                    currentPaths.map { path -> path.first + evaluatedArg to path.second }
                } else {
                    log.warn("Unexpected argument: \"{}\". This will be ignored.", argument)
                    currentPaths
                }
            }

        // Map the paths and create the new File nodes.
        return allCombinedPaths.map { (combinedFileName, tempFileStatus) ->
            // Join the paths with "/"
            val newFileName = combinedFileName.joinToString("/")
            // If the file name starts with "/tmp/", we consider it a temporary file, otherwise we
            // use the pre-computed value.
            val isTempFile =
                if (newFileName.startsWith("/tmp/")) {
                    FileTempFileStatus.TEMP_FILE
                } else tempFileStatus

            // Create the new File node with the computed name and temp file status.
            newFile(underlyingNode = callExpression, fileName = newFileName, connect = false)
                .apply { this.isTempFile = isTempFile }
                .also { file -> log.debug("Created new file from path join: {}", file.fileName) }
        }
    }
}
