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
import de.fraunhofer.aisec.cpg.evaluation.ValueEvaluator
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.OverlayNode
import de.fraunhofer.aisec.cpg.graph.argumentValueByNameOrPosition
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.concepts.file.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.helpers.functional.PowersetLattice
import de.fraunhofer.aisec.cpg.passes.DFGPass
import de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass
import de.fraunhofer.aisec.cpg.passes.concepts.EOGConceptPass
import de.fraunhofer.aisec.cpg.passes.concepts.NodeToOverlayState
import de.fraunhofer.aisec.cpg.passes.concepts.NodeToOverlayStateElement
import de.fraunhofer.aisec.cpg.passes.concepts.file.python.PythonFileConceptPass.Companion.fileCache
import de.fraunhofer.aisec.cpg.passes.concepts.getOverlaysByPrevDFG
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteLate

/**
 * This pass implements the creating of [Concept] and [Operation] nodes for Python file
 * manipulation. Currently, this pass supports the builtin `open` and `os.open` with the
 * corresponding reading and writing functions.
 */
@ExecuteLate
@DependsOn(DFGPass::class, false)
@DependsOn(EvaluationOrderGraphPass::class, false)
@DependsOn(PythonFileJoinPass::class, false)
@DependsOn(PythonFileConceptPrePass::class, false)
class PythonFileConceptPass(ctx: TranslationContext) : EOGConceptPass(ctx) {
    companion object {
        /**
         * Maps file names to [File] nodes. This is required to prevent the creation of multiple
         * [File] nodes when API calls do not have a file object but a file name.
         *
         * ```python
         * os.chmod("foo.txt", ...
         * os.open("foo.txt", ...
         * ```
         *
         * should both operate on the same [File] concept node.
         *
         * This is currently done per [Component].
         */
        // TODO: Is TranslationUnitDeclaration better?
        internal val fileCache = mutableMapOf<Component?, MutableMap<String, File>>()
    }

    override fun handleMemberCallExpression(
        lattice: NodeToOverlayState,
        state: NodeToOverlayStateElement,
        node: MemberCallExpression,
    ): Collection<OverlayNode> {
        // Since we cannot directly depend on the Python frontend, we have to check the language
        // here based on the node's language.
        if (node.language.name.localName != "PythonLanguage") {
            return emptyList()
        }

        val stateChanges = mutableListOf<OverlayNode>()
        node.base
            ?.let { findFile(it, state) }
            ?.map { fileNode ->
                when (node.name.localName) {
                    "__enter__" -> {
                        // nothing to do here
                    }

                    "__exit__" -> {
                        stateChanges.addAll(handleCloseFileObject(node, fileNode))
                    }

                    "read" ->
                        stateChanges +=
                            newFileRead(underlyingNode = node, file = fileNode, connect = false)

                    "write" -> {
                        val arg = node.arguments.getOrNull(0)
                        if (node.arguments.size != 1 || arg == null) {
                            Util.errorWithFileLocation(
                                node,
                                log,
                                "Failed to identify the write argument. Ignoring the `write` call.",
                            )
                            return emptyList()
                        }
                        stateChanges +=
                            newFileWrite(
                                underlyingNode = node,
                                file = fileNode,
                                what = arg,
                                connect = false,
                            )
                    }

                    "close" -> {
                        stateChanges.addAll(handleCloseFileObject(node, fileNode))
                    }

                    else -> {
                        Util.warnWithFileLocation(
                            node = node,
                            log = log,
                            format =
                                "Handling of \"{}\" is not yet implemented. No concept node is created.",
                            node,
                        )
                    }
                }
            }
        return stateChanges
    }

    override fun handleCallExpression(
        lattice: NodeToOverlayState,
        state: NodeToOverlayStateElement,
        callExpression: CallExpression,
    ): Collection<OverlayNode> {
        // Since we cannot directly depend on the Python frontend, we have to check the language
        // here based on the node's language.
        if (callExpression.language.name.localName != "PythonLanguage") {
            return emptyList()
        }

        return when (callExpression.callee.name.toString()) {
            "open" -> {
                handleOpen(lattice, state, callExpression = callExpression)
            }
            "os.open" -> {
                handleOsOpen(lattice, state, callExpression)
            }
            "os.chmod" -> {
                handleOsChmod(lattice, state, callExpression)
            }
            "os.remove" -> {
                handleOsRemove(lattice, state, callExpression)
            }
            "tempfile.TemporaryFile",
            "tempfile.NamedTemporaryFile"
            /* TODO filedescriptor support... "tempfile.mkstemp" */ -> {
                handleTempFile(callExpression)
            }
            "tempfile.gettempdir" -> {
                emptyList()
                /** see [PythonFileConceptPrePass] */
            }
            else -> {
                emptyList()
            }
        }
    }

    private fun handleCloseFileObject(
        callExpression: MemberCallExpression,
        fileNode: File,
    ): Collection<OverlayNode> {
        val fileClose =
            newFileClose(underlyingNode = callExpression, file = fileNode, connect = false)
        val fileDelete =
            if (fileNode.deleteOnClose) {
                newFileDelete(underlyingNode = callExpression, file = fileNode, connect = false)
            } else {
                null
            }
        return listOfNotNull(fileClose, fileDelete)
    }

    private fun handleTempFile(callExpression: CallExpression): Collection<OverlayNode> {
        val deleteOnClose =
            when (callExpression.callee.name.toString()) {
                "tempfile.TemporaryFile" -> {
                    true
                }
                "tempfile.NamedTemporaryFile" -> {
                    callExpression
                        .argumentValueByNameOrPosition<Boolean>(name = "delete", position = 8)
                        ?.result ?: true
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

    /** TODO */
    private fun handleOpen(
        lattice: NodeToOverlayState,
        state: NodeToOverlayStateElement,
        callExpression: CallExpression,
    ): Collection<OverlayNode> {

        /**
         * This matches when parsing code like:
         * ```python
         * open('foo.bar', 'r')
         * ```
         *
         * We model this with a [File] node to represent the [Concept] and a [OpenFile] node for the
         * opening operation.
         *
         * TODO: opener https://docs.python.org/3/library/functions.html#open
         */
        val file = getOrCreateFile(callExpression, "file", lattice, state)

        val mode = getBuiltinOpenMode(callExpression) ?: "r" // default is 'r'
        val flags = translateBuiltinOpenMode(mode)

        val setFlagsOps = mutableListOf<SetFileFlags>()
        val openOps = mutableListOf<OpenFile>()
        val fileHandles = mutableListOf<FileHandle>()
        file.forEach { file ->
            setFlagsOps +=
                newFileSetFlags(
                    underlyingNode = callExpression,
                    file = file,
                    flags = flags,
                    connect = false,
                )
            openOps += newFileOpen(underlyingNode = callExpression, file = file, connect = false)

            fileHandles +=
                newFileHandle(underlyingNode = callExpression, file = file, connect = false)
        }
        return listOf(setFlagsOps, openOps, fileHandles).flatten()
    }

    /** TODO */
    private fun handleOsOpen(
        lattice: NodeToOverlayState,
        state: NodeToOverlayStateElement,
        callExpression: CallExpression,
    ): Collection<OverlayNode> {
        val files = getOrCreateFile(callExpression, "path", lattice, state)

        val openFlags = mutableListOf<SetFileFlags>()
        val maskOps = mutableListOf<SetFileMask>()
        val fileOpenNodes = mutableListOf<OpenFile>()
        val fileHandles = mutableListOf<FileHandle>()
        files.forEach { file ->
            getOsOpenFlags(callExpression)?.let { flags ->
                newFileSetFlags(
                        underlyingNode = callExpression,
                        file = file,
                        flags = translateOsOpenFlags(flags),
                        connect = false,
                    )
                    .also { openFlags += it }
            }
            val mode =
                getOsOpenMode(callExpression)
                    ?: 329L // default is 511 (assuming this is octet notation)
            maskOps +=
                newFileSetMask(
                    underlyingNode = callExpression,
                    file = file,
                    mask = mode,
                    connect = false,
                )

            fileOpenNodes +=
                newFileOpen(underlyingNode = callExpression, file = file, connect = false)

            fileHandles +=
                newFileHandle(underlyingNode = callExpression, file = file, connect = false)
        }
        return listOfNotNull(openFlags, maskOps, fileOpenNodes, fileHandles).flatten()
    }

    /** TODO */
    private fun handleOsChmod(
        lattice: NodeToOverlayState,
        state: NodeToOverlayStateElement,
        callExpression: CallExpression,
    ): Collection<SetFileMask> {
        val files = getOrCreateFile(callExpression, "path", lattice, state)
        val mode = callExpression.argumentValueByNameOrPosition<Long>(name = "mode", position = 1)
        if (mode == null) {
            Util.errorWithFileLocation(
                callExpression,
                log,
                "Failed to find the corresponding mode. Ignoring the entire `os.chmod` call..",
            )
            return emptyList()
        }
        return files.map { file ->
            newFileSetMask(
                underlyingNode = callExpression,
                file = file,
                mask = mode.result,
                connect = false,
            )
        }
    }

    /** TODO */
    private fun handleOsRemove(
        lattice: NodeToOverlayState,
        state: NodeToOverlayStateElement,
        callExpression: CallExpression,
    ): Collection<OverlayNode> {
        val files = getOrCreateFile(callExpression, "path", lattice, state)

        return files.map { file ->
            newFileDelete(underlyingNode = callExpression, file = file, connect = false)
        }
    }

    /**
     * Looks for the requested file in the [fileCache]. If none is found, a new [File] is created
     * and added to the cache.
     *
     * @param callExpression The [CallExpression] triggering the call lookup. It is used as a basis
     *   ([File.underlyingNode]) if a new file has to be created.
     * @param argumentName The name of the argument to be used for the file name in the
     *   [callExpression].
     * @return The [File] found in the cache or the new file in case it had to be created.
     *   Additionally, a flag whether the [File] was created (`true`) or already existed in the
     *   cache (`false`) is returned, too.
     *
     *     TODO: update
     */
    internal fun getOrCreateFile(
        callExpression: CallExpression,
        argumentName: String,
        lattice: NodeToOverlayState,
        state: NodeToOverlayStateElement,
    ): List<File> {
        val fileName = getFileName(callExpression, argumentName) ?: TODO()
        return handleInternal(fileName, callExpression, lattice, state)
    }

    private fun handleInternal(
        fileName: ValueEvaluator.ResultWithPath<String>,
        callExpression: CallExpression,
        lattice: NodeToOverlayState,
        state: NodeToOverlayStateElement,
    ): List<File> {
        val last = fileName.path.lastOrNull()
        if (last == null) {
            return emptyList()
        }

        if (last.overlays.any { it is File }) {
            return last.overlays.filterIsInstance<File>()
        }

        if (last.prevDFG.size > 1) {
            val result = mutableListOf<File>()
            last.prevDFG.forEach {
                it.language.evaluator.evaluateAs<String>(it)?.let { fileName ->
                    result += handleInternal(fileName, callExpression, lattice, state)
                }
            }
            return result
        }

        if (last is Literal<*>) {
            val tempFileStatus =
                if (fileName.result.startsWith("/tmp/")) {
                    FileTempFileStatus.TEMP_FILE
                } else {
                    getTempFileStatus(callExpression, fileName)
                }
            val currentMap = fileCache.computeIfAbsent(currentComponent) { mutableMapOf() }
            val existingEntry = currentMap[fileName.result]
            if (existingEntry != null) {
                return listOf(existingEntry)
            }

            val newEntry =
                newFile(underlyingNode = last, fileName = fileName.result, connect = false).apply {
                    this.isTempFile = tempFileStatus
                }

            lattice.lub(
                one = state,
                two = NodeToOverlayStateElement(last to PowersetLattice.Element(newEntry)),
                allowModify = true,
            )

            currentMap[fileName.result] = newEntry
            return listOf(newEntry)
        } else {
            TODO()
        }
    }

    /** TODO */
    private fun getTempFileStatus(
        callExpression: CallExpression,
        fileName: ValueEvaluator.ResultWithPath<String>,
    ): FileTempFileStatus {
        return fileName.path.lastOrNull()?.let { node ->
            if (
                node.overlays.any { overlay ->
                    overlay is File && overlay.isTempFile == FileTempFileStatus.TEMP_FILE
                }
            ) {
                FileTempFileStatus.TEMP_FILE
            } else {
                FileTempFileStatus.NOT_A_TEMP_FILE
            }
        } ?: FileTempFileStatus.UNKNOWN
    }

    /**
     * Walks the DFG backwards until a [OpenFile] node is found.
     *
     * @param expression The start node.
     * @return A list of all [File] nodes found.
     */
    internal fun findFile(
        expression: Expression,
        stateElement: NodeToOverlayStateElement,
    ): List<File> {
        return expression.getOverlaysByPrevDFG<OpenFile>(stateElement).map { it.file }
    }

    /**
     * Parses the name of the file used in a builtin-`open` call or `os.open` call. The name of the
     * parameter depends on the open function but, it's the first parameter if a call without named
     * arguments is analyzed.
     *
     * @param call The [CallExpression] (builtin-`open` or `os.open`) to be analyzed.
     * @param argumentName The name of the argument to be used for the file name in the [call].
     * @return The name or null if no name could be found.
     */
    private fun getFileName(
        call: CallExpression,
        argumentName: String,
    ): ValueEvaluator.ResultWithPath<String>? {
        val name = call.argumentValueByNameOrPosition<String>(name = argumentName, position = 0)
        return name
    }

    /**
     * Handles the `mode` parameter of Pythons builtin `open` function.
     *
     * Do not confuse with the `flags` in `os.open` which perform similar actions (see
     * [getOsOpenFlags]).
     *
     * [`open`](https://docs.python.org/3/library/functions.html#open) signature:
     * ```python
     * open(file, mode='r', buffering=-1, encoding=None, errors=None, newline=None, closefd=True, opener=None)
     * ```
     */
    internal fun getBuiltinOpenMode(call: CallExpression): String? {
        return call.argumentValueByNameOrPosition<String>(name = "mode", position = 1)?.result
    }

    /**
     * Handles the `mode` parameter of `os.open` function.
     *
     * Do not confuse with the builtin `open` (see [getBuiltinOpenMode]).
     *
     * [`os.open`](https://docs.python.org/3/library/os.html#os.open) signature:
     * ```python
     * os.open(path, flags, mode=0o777, *, dir_fd=None)
     * ```
     *
     * @param call The `os.open` call.
     * @return The `mode`
     */
    internal fun getOsOpenMode(call: CallExpression): Long? {
        return call.argumentValueByNameOrPosition<Long>(name = "mode", position = 2)?.result
    }

    /**
     * Handles the `flags` parameter of `os.open` function.
     *
     * [`os.open`](https://docs.python.org/3/library/os.html#os.open) signature:
     * ```python
     * os.open(path, flags, mode=0o777, *, dir_fd=None)
     * ```
     */
    internal fun getOsOpenFlags(call: CallExpression): Long? {
        return call.argumentValueByNameOrPosition<Long>(name = "flags", position = 1)?.result
    }

    /**
     * Translates the mode numerical codes to [FileAccessModeFlags]. The [flags] `0b10` is
     * translated to [FileAccessModeFlags.O_RDWR] for example.
     *
     * @param flags The numerical flags to parse.
     * @return A set of corresponding [FileAccessModeFlags]
     */
    internal fun translateOsOpenFlags(flags: Long): Set<FileAccessModeFlags> {
        return FileAccessModeFlags.entries
            .filter { it.value == (flags and O_ACCMODE_MODE_MASK) }
            .toSet()
    }

    /**
     * Translates the `mode` string of the
     * [builtin `open` function](https://docs.python.org/3/library/functions.html#open)
     */
    internal fun translateBuiltinOpenMode(mode: String): Set<FileAccessModeFlags> {
        return when (mode) {
            "w",
            "wb",
            "wt" -> setOf(FileAccessModeFlags.O_WRONLY) // TODO binary vs text
            "w+" -> setOf(FileAccessModeFlags.O_WRONLY) // TODO TRUNC
            "r",
            "rb",
            "rt" -> setOf(FileAccessModeFlags.O_RDONLY) // TODO binary vs text
            "x" -> setOf(FileAccessModeFlags.O_WRONLY) // TODO create. Is there xt and xb?
            "a" -> setOf(FileAccessModeFlags.O_WRONLY) // TODO append. binary?
            "w+b",
            "r+b" ->
                setOf(
                    FileAccessModeFlags.O_RDWR
                ) // TODO BINARY, truncating (w) or no truncating (r)
            else -> {
                log.error(
                    "Failed to parse the mode string \"$mode\". Returning an empty set of file modes."
                )
                emptySet()
            }
        }
    }

    override fun finalCleanup() {
        super.finalCleanup()
        fileCache.clear()
    }
}
