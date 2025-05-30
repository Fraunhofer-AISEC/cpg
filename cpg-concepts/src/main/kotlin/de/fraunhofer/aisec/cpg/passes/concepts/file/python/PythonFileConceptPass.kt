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
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.concepts.file.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
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
@DependsOn(DFGPass::class)
@DependsOn(EvaluationOrderGraphPass::class)
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

        return node.base
            ?.let { findFile(it, state) }
            ?.mapNotNull { fileNode ->
                when (node.name.localName) {
                    "__enter__" -> {
                        /* TODO: what about this? we handle __exit__ and create a CloseFile. However, we already have a OpenFile attached at the `open` */
                        null
                    }

                    "__exit__" ->
                        newFileClose(underlyingNode = node, file = fileNode, connect = false)

                    "read" -> newFileRead(underlyingNode = node, file = fileNode, connect = false)

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
                        newFileWrite(
                            underlyingNode = node,
                            file = fileNode,
                            what = arg,
                            connect = false,
                        )
                    }

                    else -> {
                        Util.warnWithFileLocation(
                            node = node,
                            log = log,
                            format =
                                "Handling of \"{}\" is not yet implemented. No concept node is created.",
                            node,
                        )
                        null
                    }
                }
            } ?: emptyList()
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
                /**
                 * This matches when parsing code like:
                 * ```python
                 * open('foo.bar', 'r')
                 * ```
                 *
                 * We model this with a [File] node to represent the [Concept] and a [OpenFile] node
                 * for the opening operation.
                 *
                 * TODO: opener https://docs.python.org/3/library/functions.html#open
                 */
                val file = getOrCreateFile(node, "file", 0, lattice, state)

                val mode = getBuiltinOpenMode(node) ?: "r" // default is 'r'
                val flags = translateBuiltinOpenMode(mode)
                val setFlagsOp =
                    newFileSetFlags(
                        underlyingNode = node,
                        file = file,
                        flags = flags,
                        connect = false,
                    )
                val open = newFileOpen(underlyingNode = node, file = file, connect = false)

                val fileHandle = newFileHandle(underlyingNode = node, file = file, connect = false)

                setOf(setFlagsOp, open, fileHandle)
            }
            "os.open" -> {
                val file = getOrCreateFile(node, "path", 0, lattice, state)

                val setFlags =
                    getOsOpenFlags(node)?.let { flags ->
                        newFileSetFlags(
                            underlyingNode = node,
                            file = file,
                            flags = translateOsOpenFlags(flags),
                            connect = false,
                        )
                    }
                val mode =
                    getOsOpenMode(node) ?: 329L // default is 511 (assuming this is octet notation)
                val setMask =
                    newFileSetMask(underlyingNode = node, file = file, mask = mode, connect = false)

                val open = newFileOpen(underlyingNode = node, file = file, connect = false)

                val fileHandle = newFileHandle(underlyingNode = node, file = file, connect = false)

                setOfNotNull(setFlags, setMask, open, fileHandle)
            }
            "os.chmod" -> {
                val file = getOrCreateFile(node, "path", 0, lattice, state)

                val mode = node.argumentValueByNameOrPosition<Long>(name = "mode", position = 1)
                if (mode == null) {
                    Util.errorWithFileLocation(
                        node,
                        log,
                        "Failed to find the corresponding mode. Ignoring the entire `os.chmod` call..",
                    )
                    return emptyList()
                }
                setOf(
                    newFileSetMask(
                        underlyingNode = node,
                        file = file,
                        mask = mode.result,
                        connect = false,
                    )
                )
            }
            "os.remove" -> {
                val file = getOrCreateFile(node, "path", 0, lattice, state)

                setOf(newFileDelete(underlyingNode = node, file = file, connect = false))
            }
            else -> {
                emptyList()
            }
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
     * @param argumentIndex The index of the argument which holds the name/path of the file in the
     *   given [CallExpression]'s [CallExpression.arguments] if no named arguments are used.
     * @param lattice The [NodeToOverlayState] which the [EOGConceptPass] operates on. It is used to
     *   add the [File].
     * @param state The [NodeToOverlayStateElement] which is used to store the [File]. If a new
     *   [File] has to be created, it is added to this state element.
     * @return The [File] found in the cache or the new file in case it had to be created.
     */
    internal fun getOrCreateFile(
        callExpression: CallExpression,
        argumentName: String,
        argumentIndex: Int,
        lattice: NodeToOverlayState,
        state: NodeToOverlayStateElement,
    ): File {
        val fileName = getFileName(callExpression, argumentName, argumentIndex) ?: TODO()
        val currentMap = fileCache.computeIfAbsent(currentComponent) { mutableMapOf() }
        val existingEntry = currentMap[fileName.result]
        if (existingEntry != null) {
            return existingEntry
        }
        val un = fileName.path.lastOrNull() ?: TODO()
        val newEntry = newFile(underlyingNode = un, fileName = fileName.result, connect = false)

        lattice.lub(
            one = state,
            two = NodeToOverlayStateElement(un to PowersetLattice.Element(newEntry)),
            allowModify = true,
        )

        currentMap[fileName.result] = newEntry
        return newEntry
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
     * @param argumentName The name of the argument which holds the file name in case named
     *   arguments are used.
     * @param argumentIndex The index of the argument which holds the file name in case no named
     *   arguments are used.
     * @return The name or null if no name could be found.
     */
    private fun getFileName(
        call: CallExpression,
        argumentName: String,
        argumentIndex: Int,
    ): ValueEvaluator.ResultWithPath<String>? {
        val name =
            call.argumentValueByNameOrPosition<String>(
                name = argumentName,
                position = argumentIndex,
            )
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
