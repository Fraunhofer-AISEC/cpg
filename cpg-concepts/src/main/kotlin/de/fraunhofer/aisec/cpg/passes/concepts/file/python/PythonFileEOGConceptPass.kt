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
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.concepts.file.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.passes.DFGPass
import de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass
import de.fraunhofer.aisec.cpg.passes.concepts.EOGConceptPass
import de.fraunhofer.aisec.cpg.passes.concepts.NodeToOverlayStateElement
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteLate

/**
 * This pass implements the creating of [Concept] and [Operation] nodes for Python file
 * manipulation. Currently, this pass supports the builtin `open` and `os.open` with the
 * corresponding reading and writing functions.
 */
@ExecuteLate
@DependsOn(DFGPass::class)
@DependsOn(EvaluationOrderGraphPass::class)
class PythonFileEOGConceptPass(ctx: TranslationContext) : EOGConceptPass(ctx) {
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
        state: NodeToOverlayStateElement,
        callExpression: MemberCallExpression,
    ): Collection<OverlayNode> =
        callExpression.base
            ?.let { findFile(it, state) }
            ?.mapNotNull { fileNode ->
                when (callExpression.name.localName) {
                    "__enter__" -> {
                        /* TODO: what about this? we handle __exit__ and create a CloseFile. However, we already have a OpenFile attached at the `open` */
                        null
                    }
                    "__exit__" ->
                        newFileCloseNoConnect(underlyingNode = callExpression, file = fileNode)
                    "read" -> newFileReadNoConnect(underlyingNode = callExpression, file = fileNode)
                    "write" -> {
                        val arg = callExpression.arguments.getOrNull(0)
                        if (callExpression.arguments.size != 1 || arg == null) {
                            Util.errorWithFileLocation(
                                callExpression,
                                log,
                                "Failed to identify the write argument. Ignoring the `write` call.",
                            )
                            return emptySet()
                        }
                        newFileWriteNoConnect(
                            underlyingNode = callExpression,
                            file = fileNode,
                            what = arg,
                        )
                    }
                    else -> {
                        Util.warnWithFileLocation(
                            node = callExpression,
                            log = log,
                            format =
                                "Handling of \"{}\" is not yet implemented. No concept node is created.",
                            callExpression,
                        )
                        null
                    }
                }
            } ?: listOf()

    override fun handleCallExpression(
        state: NodeToOverlayStateElement,
        callExpression: CallExpression,
    ): Collection<OverlayNode> {
        // Since we cannot directly depend on the Python frontend, we have to check the language
        // here based on the node's language.
        if (callExpression.language.name.localName != "PythonLanguage") {
            return emptySet()
        }

        val name = callExpression.name

        if (name.toString() == "open") {
            /**
             * This matches when parsing code like:
             * ```python
             * open('foo.bar', 'r')
             * ```
             *
             * We model this with a [File] node to represent the [Concept] and a [OpenFile] node for
             * the opening operation.
             *
             * TODO: opener https://docs.python.org/3/library/functions.html#open
             */
            val fileName = getFileName(callExpression, "file")
            if (fileName == null) {
                Util.errorWithFileLocation(
                    callExpression,
                    log,
                    "Failed to parse file name. Ignoring the `open` call.",
                )
                return emptySet()
            }
            val (newFileNode, isNewFile) = getOrCreateFile(fileName, callExpression)

            val mode = getBuiltinOpenMode(callExpression) ?: "r" // default is 'r'
            val flags = translateBuiltinOpenMode(mode)
            val setFlagsOp =
                newFileSetFlagsNoConnect(
                    underlyingNode = callExpression,
                    file = newFileNode,
                    flags = flags,
                )
            val open = newFileOpenNoConnect(underlyingNode = callExpression, file = newFileNode)
            return if (isNewFile) setOf(setFlagsOp, open, newFileNode) else setOf(setFlagsOp, open)
        } else {
            when (callExpression.callee.name.toString()) {
                "os.open" -> {
                    val fileName = getFileName(callExpression, "path")
                    if (fileName == null) {
                        Util.errorWithFileLocation(
                            callExpression,
                            log,
                            "Failed to parse file name. Ignoring the `os.open` call.",
                        )
                        return emptySet()
                    }
                    val (newFileNode, isNewFile) = getOrCreateFile(fileName, callExpression)

                    val setFlags =
                        getOsOpenFlags(callExpression)?.let { flags ->
                            newFileSetFlagsNoConnect(
                                underlyingNode = callExpression,
                                file = newFileNode,
                                flags = translateOsOpenFlags(flags),
                            )
                        }
                    val mode =
                        getOsOpenMode(callExpression)
                            ?: 329L // default is 511 (assuming this is octet notation)
                    val setMask =
                        newFileSetMaskNoConnect(
                            underlyingNode = callExpression,
                            file = newFileNode,
                            mask = mode,
                        )

                    val open =
                        newFileOpenNoConnect(underlyingNode = callExpression, file = newFileNode)
                    return if (isNewFile) {
                        if (setFlags != null) {
                            setOf(setFlags, setMask, open, newFileNode)
                        } else {
                            setOf(setMask, open, newFileNode)
                        }
                    } else {
                        if (setFlags != null) {
                            setOf(setFlags, setMask, open)
                        } else {
                            setOf(setMask, open)
                        }
                    }
                }
                "os.chmod" -> {
                    val fileName =
                        callExpression.argumentValueByNameOrPosition<String>(
                            name = "path",
                            position = 0,
                        )
                    if (fileName == null) {
                        Util.errorWithFileLocation(
                            callExpression,
                            log,
                            "Failed to parse the `path` argument. Ignoring the entire `os.chmod` call.",
                        )
                        return emptySet()
                    }

                    val (file, isNewFile) = getOrCreateFile(fileName, callExpression)

                    val mode =
                        callExpression.argumentValueByNameOrPosition<Long>(
                            name = "mode",
                            position = 1,
                        )
                    if (mode == null) {
                        Util.errorWithFileLocation(
                            callExpression,
                            log,
                            "Failed to find the corresponding mode. Ignoring the entire `os.chmod` call..",
                        )
                        return if (isNewFile) {
                            setOf(file)
                        } else {
                            emptySet()
                        }
                    }
                    return if (isNewFile) {
                        setOf(
                            file,
                            newFileSetMaskNoConnect(
                                underlyingNode = callExpression,
                                file = file,
                                mask = mode,
                            ),
                        )
                    } else
                        setOf(
                            newFileSetMaskNoConnect(
                                underlyingNode = callExpression,
                                file = file,
                                mask = mode,
                            )
                        )
                }
                "os.remove" -> {
                    val fileName =
                        callExpression.argumentValueByNameOrPosition<String>(
                            name = "path",
                            position = 0,
                        )
                    if (fileName == null) {
                        Util.errorWithFileLocation(
                            callExpression,
                            log,
                            "Failed to parse the `path` argument. Ignoring the entire `os.remove` call.",
                        )
                        return emptySet()
                    }

                    val (file, isNewFile) = getOrCreateFile(fileName, callExpression)

                    return if (isNewFile)
                        setOf(
                            file,
                            newFileDeleteNoConnect(underlyingNode = callExpression, file = file),
                        )
                    else setOf(newFileDeleteNoConnect(underlyingNode = callExpression, file = file))
                }
            }
        }
        return setOf()
    }

    /**
     * Looks for the requested file in the [fileCache]. If none is found, a new [File] is created
     * and added to the cache.
     *
     * @param fileName The name/path of the file.
     * @param callExpression The [CallExpression] triggering the call lookup. It is used as a basis
     *   ([File.underlyingNode]) if a new file has to be created.
     * @return The [File] found in the cache or the new file in case it had to be created.
     */
    internal fun getOrCreateFile(
        fileName: String,
        callExpression: CallExpression,
    ): Pair<File, Boolean> {
        val currentMap = fileCache.computeIfAbsent(currentComponent) { mutableMapOf() }
        val existingEntry = currentMap[fileName]
        if (existingEntry != null) {
            return existingEntry to false
        }
        val newEntry = newFileNoConnect(underlyingNode = callExpression, fileName = fileName)
        currentMap[fileName] = newEntry
        return newEntry to true
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
        // find all nodes on a prev DFG path which have an [OpenFile] overlay node and return the
        // last node on said path (i.e. the one with the [OpenFile] overlay)
        val nodesWithOpenFileOverlay =
            expression
                .followDFGEdgesUntilHit(
                    collectFailedPaths = false,
                    findAllPossiblePaths = false,
                    direction = Backward(GraphToFollow.DFG),
                ) { node ->
                    stateElement[node]?.any { it is OpenFile } == true ||
                        node is OpenFile ||
                        node.overlays.any { it is OpenFile }
                }
                .fulfilled
                .map { it.last() }

        val files =
            nodesWithOpenFileOverlay
                .flatMap {
                    stateElement[it] ?: setOf(it, *it.overlays.toTypedArray())
                } // collect all "overlay" nodes
                .filterIsInstance<OpenFile>() // discard not-relevant overlays
                .map { it.file } // move from [OpenFile] to the corresponding [File] concept node

        return files
    }

    /**
     * Parses the name of the file used in a builtin-`open` call or `os.open` call. The name of the
     * parameter depends on the open function but, it's the first parameter if a call without named
     * arguments is analyzed.
     *
     * @param call The [CallExpression] (builtin-`open` or `os.open`) to be analyzed.
     * @return The name or null if no name could be found.
     */
    private fun getFileName(call: CallExpression, argumentName: String): String? {
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
        return call.argumentValueByNameOrPosition<String>(name = "mode", position = 1)
    }

    /**
     * Handles the `mask` parameter of `os.open` function.
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
}
