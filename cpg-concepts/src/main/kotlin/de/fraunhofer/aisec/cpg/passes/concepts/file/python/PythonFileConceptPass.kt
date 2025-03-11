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
import de.fraunhofer.aisec.cpg.graph.concepts.file.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.passes.DFGPass
import de.fraunhofer.aisec.cpg.passes.concepts.ConceptPass
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteLate
import de.fraunhofer.aisec.cpg.query.QueryTree

/**
 * This pass implements the creating of [Concept] and [Operation] nodes for Python file
 * manipulation. Currently, this pass supports the builtin `open` and `os.open` with the
 * corresponding reading and writing functions.
 */
@ExecuteLate
@DependsOn(DFGPass::class)
class PythonFileConceptPass(ctx: TranslationContext) : ConceptPass(ctx) {

    /** The file name used if we fail to find it. */
    internal val UNKNOWN_FILE_NAME = "FILE_NOT_RESOLVED_UNKNOWN_FILE_NAME"

    /**
     * Maps file names to [File] nodes. This is required to prevent the creation of multiple [File]
     * nodes when API calls do not have a file object but a file name.
     *
     * ```python
     * os.chmod("foo.txt", ...
     * os.open("foo.txt", ...
     * ```
     *
     * should both operate on the same [File] concept node.
     */
    internal val fileCache = mutableMapOf<String, File>()

    override fun handleNode(node: Node, tu: TranslationUnitDeclaration) {
        // Since we cannot directly depend on the Python frontend, we have to check the language
        // here
        // based on the node's language.
        if (node.language.name.localName != "PythonLanguage") {
            return
        }
        when (node) {
            is CallExpression -> handleCall(node)
        }
    }

    private fun handleCall(callExpression: CallExpression) {
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
            val newFileNode: File = getOrCreateFile(fileName, callExpression)

            val mode = getBuiltinOpenMode(callExpression) ?: "r" // default is 'r'
            val flags = translateBuiltinOpenMode(mode)
            newFileSetFlags(underlyingNode = callExpression, file = newFileNode, flags = flags)
            newFileOpen(underlyingNode = callExpression, file = newFileNode)
        } else if (callExpression is MemberCallExpression) {
            callExpression.base
                ?.let { findFile(it) }
                ?.forEach { fileNode ->
                    when (callExpression.name.localName) {
                        "__enter__" -> {
                            /* TODO: what about this? we handle __exit__ and create a CloseFile. However, we already have a OpenFile attached at the `open` */
                        }
                        "__exit__" -> newFileClose(underlyingNode = callExpression, file = fileNode)
                        "read" -> newFileRead(underlyingNode = callExpression, file = fileNode)
                        "write" -> {
                            val arg = callExpression.arguments.getOrNull(0)
                            if (callExpression.arguments.size != 1 || arg == null) {
                                Util.errorWithFileLocation(
                                    callExpression,
                                    log,
                                    "Failed to identify the write argument. Ignoring the `write` call.",
                                )
                                return
                            }
                            newFileWrite(
                                underlyingNode = callExpression,
                                file = fileNode,
                                what = arg,
                            )
                        }
                        else ->
                            Util.warnWithFileLocation(
                                node = callExpression,
                                log = log,
                                format =
                                    "Handling of \"{}\" is not yet implemented. No concept node is created.",
                                callExpression,
                            )
                    }
                }
        } else {
            when (callExpression.callee.name.toString()) {
                "os.open" -> {
                    val fileName = getFileName(callExpression, "path")
                    val newFileNode = getOrCreateFile(fileName, callExpression)

                    getOsOpenFlags(callExpression)?.let { flags ->
                        newFileSetFlags(
                            underlyingNode = callExpression,
                            file = newFileNode,
                            flags = translateOsOpenFlags(flags),
                        )
                    }
                    val mode =
                        getOsOpenMode(callExpression)
                            ?: 329L // default is 511 (assuming this is octet notation)
                    newFileSetMask(underlyingNode = callExpression, file = newFileNode, mask = mode)

                    newFileOpen(underlyingNode = callExpression, file = newFileNode)
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
                        return
                    }

                    val file = getOrCreateFile(fileName, callExpression)

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
                        return
                    }
                    newFileSetMask(underlyingNode = callExpression, file = file, mask = mode)
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
                        return
                    }

                    val file = getOrCreateFile(fileName, callExpression)

                    newFileDelete(underlyingNode = callExpression, file = file)
                }
            }
        }
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
    internal fun getOrCreateFile(fileName: String, callExpression: CallExpression): File {
        return fileCache[fileName]
            ?: newFile(underlyingNode = callExpression, fileName = fileName).also { file ->
                fileCache += fileName to file
            }
    }

    /**
     * Walks the DFG backwards until a [OpenFile] node is found.
     *
     * @param expression The start node.
     * @return A list of all [File] nodes found.
     */
    internal fun findFile(expression: Expression): List<File> {
        // find all nodes on a prev DFG path which have an [OpenFile] overlay node and return the
        // last node on said path (i.e. the one with the [OpenFile] overlay)
        val nodesWithOpenFileOverlay =
            expression
                .followDFGEdgesUntilHit(
                    collectFailedPaths = false,
                    findAllPossiblePaths = true,
                    direction = Backward(GraphToFollow.DFG),
                ) { node ->
                    node.overlays.any { overlay -> overlay is OpenFile }
                }
                .fulfilled
                .map { it.last() }

        val files =
            nodesWithOpenFileOverlay
                .flatMap { it.overlays } // collect all "overlay" nodes
                .filterIsInstance<OpenFile>() // discard not-relevant overlays
                .map { it.concept } // move from [OpenFile] to the corresponding [File] concept node

        return files
    }

    /**
     * Parses the name of the file used in a builtin-`open` call or `os.open` call. The name of the
     * parameter depends on the open function but, it's the first parameter if a call without named
     * arguments is analyzed.
     *
     * @param call The [CallExpression] (builtin-`open` or `os.open`) to be analyzed.
     * @return The name or [UNKNOWN_FILE_NAME] if no name could be found.
     */
    private fun getFileName(call: CallExpression, argumentName: String): String {
        val name = call.argumentValueByNameOrPosition<String>(name = argumentName, position = 0)
        return if (name != null) {
            name
        } else {
            Util.errorWithFileLocation(
                call,
                log,
                "Couldn't evaluate the file name. Using \"$UNKNOWN_FILE_NAME\" instead. Expect errors.",
            )
            UNKNOWN_FILE_NAME
        }
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

    companion object {
        // Replace once #2105 is merged
        fun QueryTree<*>.successfulLastNodes(): List<Node> {
            val successfulPaths = this.children.filter { it.value == true }
            val innerPath = successfulPaths.flatMap { it.children }
            val finallyTheEntirePaths = innerPath.map { it.value }

            return finallyTheEntirePaths
                .mapNotNull { (it as? List<*>)?.last() }
                .filterIsInstance<Node>()
        }
    }
}
