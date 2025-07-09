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
import de.fraunhofer.aisec.cpg.graph.argumentValueByNameOrPosition
import de.fraunhofer.aisec.cpg.graph.concepts.file.*
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
            "tempfile.TemporaryFile",
            "tempfile.NamedTemporaryFile" -> {
                handleTempFile(node)
            }
            "tempfile.mkstemp" -> {
                handleMkstemp(node)
            }
            "tempfile.mkdtemp" -> {
                handleMkdtemp(node)
            }
            "tempfile.mktemp" -> {
                // This is a legacy function that is not recommended to use anymore, but we still
                // handle it for completeness.
                handleMktemp(node)
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
            "tempfile.TemporaryFile",
            "tempfile.NamedTemporaryFile" -> {
                handleTempFile(node)
            }
            "tempfile.mkstemp" -> {
                handleMkstemp(node)
            }
            "tempfile.mkdtemp" -> {
                handleMkdtemp(node)
            }
            "tempfile.mktemp" -> {
                // This is a legacy function that is not recommended to use anymore, but we still
                // handle it for completeness.
                handleMktemp(node)
            }
            else -> {
                emptyList()
            }
        }
    }

    /**
     * Handles calls to `tempfile.gettempdir` and creates a new [File] concept with the name
     * `tempfile.gettempdir(<UUID>)`, where `<UUID>` is a unique identifier to avoid collisions. It
     * also sets the file as a temporary file.
     */
    private fun handleGetTempDir(callExpression: CallExpression): Collection<OverlayNode> {
        return listOf(
            newFile(
                    underlyingNode = callExpression,
                    fileName = "tempfile.gettempdir(${callExpression.id})", // Unique name to avoid
                    // collisions
                    connect = false,
                )
                .apply { this.isTempFile = FileTempFileStatus.TEMP_FILE }
        )
    }

    /**
     * Handles calls to `tempfile.TemporaryFile` and `tempfile.NamedTemporaryFile`. It checks if the
     * file is deleted when the file is closed (default is `true`) and creates a new [File] concept,
     * [OpenFile] operation and [SetFileMask] operation and returns them in the list.
     */
    private fun handleTempFile(callExpression: CallExpression): Collection<OverlayNode> {
        val deleteOnClose =
            when (callExpression.callee.name.toString()) {
                "tempfile.TemporaryFile" -> {
                    true
                }
                "tempfile.NamedTemporaryFile" -> {
                    callExpression.argumentValueByNameOrPosition<Boolean>(
                        name = "delete",
                        position = 8,
                    ) ?: true
                }
                else -> false
            }

        val file =
            newFile(
                    underlyingNode = callExpression,
                    fileName = "tempfile" + callExpression.id.toString(),
                    connect = false,
                ) // TODO: id to model random names
                .apply { this.isTempFile = FileTempFileStatus.TEMP_FILE }
                .apply { this.deleteOnClose = deleteOnClose }
        val permissions =
            newFileSetMask(
                underlyingNode = callExpression,
                file = file,
                mask = 384 /* 0600 octet to decimal */,
                connect = false,
            )
        val openTemp = newFileOpen(underlyingNode = callExpression, file = file, connect = false)
        return listOf(file, permissions, openTemp)
    }

    /**
     * Handles the `tempfile.mkstemp` function. It creates a temporary file with a unique name based
     * on the provided prefix and suffix and the id of the [callExpression].
     */
    private fun handleMkstemp(callExpression: CallExpression): Collection<OverlayNode> {
        return listOf(
            newFileHandle(
                underlyingNode = callExpression,
                fileName =
                    createFilename(
                        // Get the prefix (default is "tmp") and add it at the beginning of the file
                        // name.
                        prefix =
                            (callExpression.argumentValueByNameOrPosition<String>(
                                name = "prefix",
                                position = 1,
                            ) ?: "tmp"),
                        // We use "tempfile.mkstemp" and the call expression ID as "unique" part in
                        // the middle of the file name.
                        middle = "tempfile.mkstemp",
                        callExpression = callExpression,
                        // Get the suffix (default is empty string) and add it to the end of the
                        // file name.
                        suffix =
                            (callExpression.argumentValueByNameOrPosition<String>(
                                name = "suffix",
                                position = 0,
                            ) ?: ""),
                    ),
                tempFileStatus = FileTempFileStatus.TEMP_FILE,
                connect = false,
            )
        )
    }

    /**
     * Handles the `tempfile.mkdtemp` function. It creates a temporary directory with a unique name
     * based on the provided prefix and suffix and the id of the [callExpression].
     */
    private fun handleMkdtemp(callExpression: CallExpression): Collection<OverlayNode> {
        return listOf(
            newFileHandle(
                underlyingNode = callExpression,
                fileName =
                    createFilename(
                        // Get the prefix (default is "tmp") and add it at the beginning of the file
                        // name.
                        prefix =
                            (callExpression.argumentValueByNameOrPosition<String>(
                                name = "prefix",
                                position = 1,
                            ) ?: "tmp"),
                        // We use "tempfile.mkdtemp" and the call expression ID as "unique" part in
                        // the middle of the file name.
                        middle = "tempfile.mkdtemp",
                        callExpression = callExpression,
                        // Get the suffix (default is empty string) and add it to the end of the
                        // file name.
                        suffix =
                            (callExpression.argumentValueByNameOrPosition<String>(
                                name = "suffix",
                                position = 0,
                            ) ?: ""),
                    ),
                tempFileStatus = FileTempFileStatus.TEMP_FILE,
                connect = false,
            )
        )
    }

    /**
     * Creates a temporary file with a unique name based on the provided [prefix], [suffix],
     * [middle] aand the id of the [callExpression].
     */
    fun createFilename(
        prefix: String,
        middle: String,
        callExpression: CallExpression,
        suffix: String,
    ): String {
        return prefix + middle + callExpression.id.toString() + suffix
    }

    /**
     * Handles the legacy `tempfile.mktemp` function, which is not recommended to use anymore. It
     * creates a temporary file with a unique name based on the provided prefix and suffix and the
     * id of the [callExpression].
     */
    private fun handleMktemp(callExpression: CallExpression): Collection<OverlayNode> {
        return listOf(
            newFileHandle(
                underlyingNode = callExpression,
                fileName =
                    createFilename(
                        // Get the prefix (default is "tmp") and add it at the beginning of the file
                        // name.
                        prefix =
                            (callExpression.argumentValueByNameOrPosition<String>(
                                name = "prefix",
                                position = 1,
                            ) ?: "tmp"),
                        // We use "tempfile.mktemp" and the call expression ID as "unique" part in
                        // the middle of the file name.
                        middle = "tempfile.mktemp",
                        callExpression = callExpression,
                        // Get the suffix (default is empty string) and add it to the end of the
                        // file name.
                        suffix =
                            (callExpression.argumentValueByNameOrPosition<String>(
                                name = "suffix",
                                position = 0,
                            ) ?: ""),
                    ),
                tempFileStatus = FileTempFileStatus.TEMP_FILE,
                connect = false,
            )
        )
    }
}
