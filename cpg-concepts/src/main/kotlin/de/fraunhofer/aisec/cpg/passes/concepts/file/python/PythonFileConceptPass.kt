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
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.concepts.file.*
import de.fraunhofer.aisec.cpg.graph.edges.get
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.helpers.functional.PowersetLattice
import de.fraunhofer.aisec.cpg.passes.DFGPass
import de.fraunhofer.aisec.cpg.passes.Description
import de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass
import de.fraunhofer.aisec.cpg.passes.concepts.EOGConceptPass
import de.fraunhofer.aisec.cpg.passes.concepts.NodeToOverlayState
import de.fraunhofer.aisec.cpg.passes.concepts.NodeToOverlayStateElement
import de.fraunhofer.aisec.cpg.passes.concepts.file.python.PythonFileConceptPass.Companion.fileCache
import de.fraunhofer.aisec.cpg.passes.concepts.getOverlaysByPrevDFG
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteLate

// TODO: move file creation before join pass
/**
 * This pass implements the creating of [Concept] and [Operation] nodes for Python file
 * manipulation. Currently, this pass supports the builtin `open` and `os.open` with the
 * corresponding reading and writing functions.
 */
@ExecuteLate
@DependsOn(DFGPass::class, false)
@DependsOn(EvaluationOrderGraphPass::class, false)
@DependsOn(PythonFileJoinPass::class, false)
@DependsOn(PythonTempFilePass::class, false)
@Description(
    "Applies file concepts to the CPG, enriching the graph with additional semantic information relevant to handling files. It only considers code written in python."
)
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
        // TODO: Is TranslationUnit better?
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
        node: CallExpression,
    ): Collection<OverlayNode> {
        // Since we cannot directly depend on the Python frontend, we have to check the language
        // here based on the node's language.
        if (node.language.name.localName != "PythonLanguage") {
            return emptyList()
        }

        return when (node.callee.name.toString()) {
            "open" -> {
                handleOpen(lattice, state, callExpression = node)
            }
            "os.open" -> {
                handleOsOpen(lattice, state, node)
            }
            "os.chmod" -> {
                handleOsChmod(lattice, state, node)
            }
            "os.remove" -> {
                handleOsRemove(lattice, state, node)
            }
            "os.fdopen" -> {
                handleOsFdOpen(lattice, state, node)
            }
            else -> {
                emptyList()
            }
        }
    }

    /**
     * Creates a [CloseFile] and possibly a [DeleteFile] node for the given [callExpression] and on
     * the [File] [fileNode]. The [DeleteFile] node is created if [File.deleteOnClose] is set to
     * `true` for the [fileNode].
     */
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

    /**
     * This function handles the `open` call, which is used to open a file.
     *
     * It creates [OpenFile] and [SetFileFlags] and [SetFileMask] nodes for the file.
     *
     * @param lattice The [NodeToOverlayState] which the [EOGConceptPass] operates on.
     * @param state The [NodeToOverlayStateElement] which is used to store the [File] nodes.
     * @param callExpression The [CallExpression] representing the `open` call.
     * @return A collection of [OverlayNode]s representing the file open operations.
     */
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
        file.forEach { file ->
            setFlagsOps +=
                newFileSetFlags(
                    underlyingNode = callExpression,
                    file = file,
                    flags = flags,
                    connect = false,
                )
            openOps += newFileOpen(underlyingNode = callExpression, file = file, connect = false)
        }
        return listOf(setFlagsOps, openOps).flatten()
    }

    /**
     * This function handles the `os.open` call, which is used to open a file.
     *
     * It creates [OpenFile] and [SetFileFlags] and [SetFileMask] nodes for the file.
     *
     * @param lattice The [NodeToOverlayState] which the [EOGConceptPass] operates on.
     * @param state The [NodeToOverlayStateElement] which is used to store the [File] nodes.
     * @param callExpression The [CallExpression] representing the `os.open` call.
     * @return A collection of [OverlayNode]s representing the file open operations.
     */
    private fun handleOsOpen(
        lattice: NodeToOverlayState,
        state: NodeToOverlayStateElement,
        callExpression: CallExpression,
    ): Collection<OverlayNode> {
        val files = getOrCreateFile(callExpression, "path", lattice, state)

        val openFlags = mutableListOf<SetFileFlags>()
        val maskOps = mutableListOf<SetFileMask>()
        val fileOpenNodes = mutableListOf<OpenFile>()
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
        }
        return listOfNotNull(openFlags, maskOps, fileOpenNodes).flatten()
    }

    /**
     * This function handles the `os.fdopen` call, which is used to open a file descriptor as a file
     * object.
     *
     * It creates [OpenFile] and [SetFileFlags] nodes for the file descriptor.
     *
     * @param lattice The [NodeToOverlayState] which the [EOGConceptPass] operates on.
     * @param state The [NodeToOverlayStateElement] which is used to store the [File] nodes.
     * @param callExpression The [CallExpression] representing the `os.fdopen` call.
     */
    private fun handleOsFdOpen(
        lattice: NodeToOverlayState,
        state: NodeToOverlayStateElement,
        callExpression: CallExpression,
    ): Collection<OverlayNode> {
        val file = getOrCreateFile(callExpression, "fd", lattice, state)

        val mode = getBuiltinOpenMode(callExpression) ?: "r" // default is 'r'
        val flags = translateBuiltinOpenMode(mode)

        val setFlagsOps = mutableListOf<SetFileFlags>()
        val openOps = mutableListOf<OpenFile>()
        file.forEach { file ->
            setFlagsOps +=
                newFileSetFlags(
                    underlyingNode = callExpression,
                    file = file,
                    flags = flags,
                    connect = false,
                )
            openOps += newFileOpen(underlyingNode = callExpression, file = file, connect = false)
        }
        return listOf(setFlagsOps, openOps).flatten()
    }

    /**
     * This function handles the `os.chmod` call, which is used to change files.
     *
     * It creates [SetFileMask] nodes for each file that is to be removed.
     *
     * @param lattice The [NodeToOverlayState] which the [EOGConceptPass] operates on.
     * @param state The [NodeToOverlayStateElement] which is used to store the [File] nodes.
     * @param callExpression The [CallExpression] representing the `os.chmod` call.
     * @return A collection of [SetFileMask] nodes representing the file chmod operations.
     */
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
                mask = mode,
                connect = false,
            )
        }
    }

    /**
     * This function handles the `os.remove` call, which is used to delete files.
     *
     * It creates [DeleteFile] nodes for each file that is to be removed.
     *
     * @param lattice The [NodeToOverlayState] which the [EOGConceptPass] operates on.
     * @param state The [NodeToOverlayStateElement] which is used to store the [File] nodes.
     * @param callExpression The [CallExpression] representing the `os.remove` call.
     * @return A collection of [DeleteFile] nodes representing the file deletion operations.
     */
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
     * and added to the [state] and the [fileCache]. Note: As this method already adds the [File] to
     * the [state], it should not be added again to the set of overlays to create for the node.
     *
     * @param callExpression The [CallExpression] triggering the call lookup. It is used as a basis
     *   ([File.underlyingNode]) if a new file has to be created.
     * @param argumentName The name of the argument which holds the name/path of the file in the
     *   given [CallExpression]'s [CallExpression.arguments] if named arguments are used.
     * @param lattice The [NodeToOverlayState] which the [EOGConceptPass] operates on. It is used to
     *   add the [File].
     * @param state The [NodeToOverlayStateElement] which is used to store the [File]. If a new
     *   [File] has to be created, it is added to this state element.
     * @return The [File] found in the cache or the new file in case it had to be created.
     *   Additionally, a flag whether the [File] was created (`true`) or already existed in the
     *   cache (`false`) is returned, too.
     *
     * This is one of the core methods of the [PythonFileConceptPass] and contains the most
     * complexity. The general idea is to follow the DFG path of the current node's file argument to
     * find all possible values that can be assigned to the file argument. This can be as easy as an
     * `open('foo')` call or more complex like using references (with possibly multiple values) or
     * calls to other functions. The main goals are:
     * - return the same [File] node for multiple calls to the same file name (even if it is a
     *   different [Literal] node evaluating to the same string) -> this is acomplished by using the
     *   [fileCache].
     *
     * The logic is as follows:
     * - Input: a [CallExpression] that has a file argument (via `argumentName`)
     * - Output: a list of [File] objects
     * - How:
     *     - traverse the DFG and collect all [FileLikeObject] overlays that are reachable from the
     *       argument
     *         - if a [File] overlay is found, return it
     *         - if a [FileHandle] overlay is found, create a new [File] node and return it
     *     - if no [File] or [FileHandle] overlay is found, create a new [File] node with the file
     *       name
     */
    internal fun getOrCreateFile(
        callExpression: CallExpression,
        argumentName: String,
        lattice: NodeToOverlayState,
        state: NodeToOverlayStateElement,
    ): List<File> {
        val arg =
            callExpression.argumentEdges[argumentName]?.end ?: callExpression.arguments.getOrNull(0)
        if (arg == null) {
            Util.errorWithFileLocation(
                callExpression,
                log,
                "Failed to find the argument $argumentName in the call expression. Ignoring the entire `open` call.",
            )
            return emptyList()
        }
        val result = mutableListOf<File>()

        // We have a cache of files per component, so we can avoid creating multiple
        // [File]s for the same file name.
        val currentMap = fileCache.computeIfAbsent(currentComponent) { mutableMapOf() }

        val paths =
            arg.followDFGEdgesUntilHit(
                collectFailedPaths = true,
                findAllPossiblePaths = true,
                direction = Backward(GraphToFollow.DFG),
            ) { node ->
                // First we check if there is a [File] or [FileHandle] overlay on the node.
                node.overlays.any { it is FileLikeObject } ||
                    // If not, we check if there is a [File] or [FileHandle] overlay in the [state]
                    state[node]?.any { it is FileLikeObject } == true
            }

        // for the successful paths, we return the [File] overlays and create new [File] nodes if
        // there is only a [FileHandle]
        val lastNode =
            paths.fulfilled
                .map { it.nodes.last() }
                .flatMap {
                    // collect all "overlay" nodes
                    state[it] ?: setOf(it, *it.overlays.toTypedArray())
                }

        if (lastNode.filterIsInstance<File>().isNotEmpty()) {
            // TODO: better check if that all [FileHandle] have [File] nodes in the cache and use
            // them
            result += lastNode.filterIsInstance<File>()
        } else {
            // There is a [FileHandle] but no [File] overlay, so we create a new [File] node
            lastNode.filterIsInstance<FileHandle>().map { fileHandle ->
                result +=
                    newFile(
                            underlyingNode = fileHandle.underlyingNode!! /* TODO */,
                            fileName = fileHandle.fileName,
                            connect = false,
                        )
                        .apply { isTempFile = fileHandle.isTempFile }
                        .also { newFile ->
                            // store the new file in the cache
                            currentMap[fileHandle.fileName] = newFile
                            // and add it to the lattice
                            lattice.lub(
                                one = state,
                                two =
                                    NodeToOverlayStateElement(
                                        fileHandle.underlyingNode!! /* TODO*/ to
                                            PowersetLattice.Element(newFile)
                                    ),
                                allowModify = true,
                            )
                        }
            }
        }

        // for the failed paths, we create a new [File] node
        result +=
            paths.failed
                .map { it.second.nodes.last() }
                .mapNotNull map@{ cpgNode ->
                    val fileName = cpgNode.language.evaluator.evaluateAs<String>(cpgNode)

                    if (fileName == null) {
                        Util.errorWithFileLocation(
                            callExpression,
                            log,
                            "Failed to evaluate the file name for the call expression. Ignoring the entire CallExpression \"$callExpression\".",
                        )
                        return@map null
                    }
                    val existingEntry = currentMap[fileName]
                    existingEntry
                        ?: newFile(underlyingNode = cpgNode, fileName = fileName, connect = false)
                            .apply {
                                this.isTempFile =
                                    if (fileName.startsWith("/tmp/")) {
                                        FileTempFileStatus.TEMP_FILE
                                    } else {
                                        FileTempFileStatus.NOT_A_TEMP_FILE
                                    }
                            }
                            .also { newFile ->
                                // store the new file in the cache
                                currentMap[fileName] = newFile
                                // and add it to the lattice
                                lattice.lub(
                                    one = state,
                                    two =
                                        NodeToOverlayStateElement(
                                            cpgNode to PowersetLattice.Element(newFile)
                                        ),
                                    allowModify = true,
                                )
                            }
                }

        return result
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
        return call.argumentValueByNameOrPosition<String>(name = "mode", position = 1)
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
        return call.argumentValueByNameOrPosition<Long>(name = "mode", position = 2)
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
        return call.argumentValueByNameOrPosition<Long>(name = "flags", position = 1)
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
