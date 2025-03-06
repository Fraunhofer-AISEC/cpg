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
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.python.PythonValueEvaluator
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.concepts.file.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.edges.get
import de.fraunhofer.aisec.cpg.graph.evaluate
import de.fraunhofer.aisec.cpg.graph.followPrevFullDFGEdgesUntilHit
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.passes.concepts.ConceptPass
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteLate
import de.fraunhofer.aisec.cpg.passes.configuration.RequiredFrontend

@ExecuteLate
@RequiredFrontend(PythonLanguageFrontend::class)
class PythonFileConceptPass(ctx: TranslationContext) :
    ConceptPass(ctx) { // TODO logging 2 ConceptPass

    /** The file name used if we fail to find it. */
    internal val DEFAULT_FILE_NAME = "DEFAULT_FILE_NAME"

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
             * We model this with a [File] node to represent the [Concept] and a [FileOpen] node for
             * the opening operation.
             *
             * TODO: opener https://docs.python.org/3/library/functions.html#open
             */
            val fileName = getFileName(callExpression, "file")
            val newFileNode: File = getOrCreateFile(fileName, callExpression)

            getBuiltinOpenMode(callExpression)?.let { mode ->
                val flags = translateBuiltinOpenMode(mode)
                newFileSetFlags(underlyingNode = callExpression, file = newFileNode, flags = flags)
            }
            newFileOpen(underlyingNode = callExpression, file = newFileNode)
        } else if (callExpression is MemberCallExpression) {
            callExpression.base
                ?.let { findFile(it) }
                ?.let { fileNode ->
                    when (callExpression.name.localName) {
                        "__enter__" -> {
                            /* TODO: what about this? we handle __exit__ */
                        }
                        "__exit__" -> newFileClose(underlyingNode = callExpression, file = fileNode)
                        "read" -> newFileRead(underlyingNode = callExpression, file = fileNode)
                        "write" ->
                            newFileWrite(
                                underlyingNode = callExpression,
                                file = fileNode,
                                what = callExpression.arguments,
                            )
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
                    fileCache += fileName to newFileNode

                    getOsOpenFlags(callExpression)?.let { flags ->
                        newFileSetFlags(
                            underlyingNode = callExpression,
                            file = newFileNode,
                            flags = translateOsOpenFlags(flags),
                        )
                    }
                    getOsOpenMask(callExpression)?.let { mask ->
                        newFileSetMask(
                            underlyingNode = callExpression,
                            file = newFileNode,
                            mask = mask,
                        )
                    }
                    newFileOpen(underlyingNode = callExpression, file = newFileNode)
                }
                "os.chmod" -> {
                    val fileName =
                        getArgumentValueByNameOrPosition<String>(callExpression, "path", 0)
                            as? String
                    if (fileName == null) {
                        Util.errorWithFileLocation(
                            callExpression,
                            log,
                            "Invalid path argument. Ignoring the entire `os.chmod` call.",
                        )
                        return
                    }

                    val file = getOrCreateFile(fileName, callExpression)

                    val mode = getArgumentValueByNameOrPosition<Long>(callExpression, "mode", 1)
                    if (mode == null) {
                        Util.errorWithFileLocation(
                            callExpression,
                            log,
                            "Failed to find the corresponding mode. Ignoring the entire `os.chmod` call..",
                        )
                        return
                    }
                    newFileChmod(underlyingNode = callExpression, file = file, mode = mode)
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
        val cached = fileCache[fileName]

        return if (cached != null) {
            cached
        } else {
            val new = newFile(underlyingNode = callExpression, fileName = fileName)
            fileCache += fileName to new
            new
        }
    }

    /**
     * Walks the DFG backwards until a [FileOpen] node is found.
     *
     * Note: If multiple [File] nodes are found, one is selected a random and a warning is logged.
     *
     * @param expression The start node.
     * @return The [File] node if one is found.
     */
    internal fun findFile(expression: Expression): File? {
        val fulfilledPaths =
            expression
                .followPrevFullDFGEdgesUntilHit(collectFailedPaths = false) {
                    it.overlays.any { overlay -> overlay is FileOpen }
                }
                .fulfilled
        val fileCandidates =
            fulfilledPaths
                .map { path ->
                    path.last()
                } // we're interested in the last node of the path, i.e. the node connected to
                // the [FileOpen] node
                .flatMap { it.overlays } // move to the "overlays" world
                .filterIsInstance<FileOpen>() // discard not-relevant overlays
                .map { it.concept } // move from [FileOpen] to the corresponding [File] concept node
        if (fileCandidates.size > 1) {
            Util.errorWithFileLocation(
                expression,
                log,
                "Found multiple files. Selecting one at random.",
            )
        }
        return fileCandidates.firstOrNull()
    }

    /**
     * Parses the name of the file used in a builtin-`open` call or `os.open` call. The name of the
     * parameter depends on the open function but, it's the first parameter if a call without named
     * arguments is analyzed.
     *
     * @param call The [CallExpression] (builtin-`open` or `os.open`) to be analyzed.
     * @return The name or [DEFAULT_FILE_NAME] if no name could be found.
     */
    private fun getFileName(call: CallExpression, argumentName: String): String {
        val name = getArgumentValueByNameOrPosition<String>(call, argumentName, 0)
        return if (name != null) {
            name
        } else {
            Util.warnWithFileLocation(
                call,
                log,
                "Couldn't evaluate the file name. Trying to find the last write and use its value.",
            )
            val nameArg =
                (call.argumentEdges[argumentName] ?: call.arguments.getOrNull(0)) as? Reference
            if (nameArg != null) { // todo: never executed with current tests
                (nameArg.evaluate() as? String) ?: DEFAULT_FILE_NAME
            } else {
                //  not a [Reference]
                DEFAULT_FILE_NAME
            }
        }
    }

    /**
     * Handles the `mode` parameter of Pythons builtin `open` function.
     *
     * Do not confuse with the `mode` in `os.open` (see [getOsOpenFlags]).
     * [`open`](https://docs.python.org/3/library/functions.html#open) signature:
     * ```python
     * open(file, mode='r', buffering=-1, encoding=None, errors=None, newline=None, closefd=True, opener=None)
     * ```
     */
    internal fun getBuiltinOpenMode(call: CallExpression): String? {
        return getArgumentValueByNameOrPosition<String>(call, "mode", 1)
    }

    /**
     * Handles the `mask` parameter of `os.open` function.
     *
     * Do not confuse with the builtin `open` (see [getBuiltinOpenMode]).
     * [`os.open`](https://docs.python.org/3/library/os.html#os.open) signature:
     * ```python
     * os.open(path, flags, mode=0o777, *, dir_fd=None)
     * ```
     *
     * @param call The `os.open` call.
     * @return The `mask` TODO mask <-> mode confusion
     */
    internal fun getOsOpenMask(call: CallExpression): Long? {
        return getArgumentValueByNameOrPosition<Long>(call, "mask", 2)
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
        return getArgumentValueByNameOrPosition<Long>(call, "flags", 1)
    }

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
            "w" -> setOf(FileAccessModeFlags.O_WRONLY)
            // "wb" -> setOf(FileAccessModeFlags.WRONLY,)
            "w+" -> setOf(FileAccessModeFlags.O_WRONLY) // TODO TRUNC
            "r",
            "rt" -> setOf(FileAccessModeFlags.O_RDONLY)
            else -> TODO()
        }
    }

    companion object {
        /**
         * A little helper function to find a [CallExpression]s argument first by name and if this
         * fails by position. The argument ist evaluated and the result is returned if it has the
         * expected type [T].
         *
         * @param call The [CallExpression] to analyze.
         * @param name Optionally: the [CallExpression.arguments] name.
         * @param pos Optionally: the [CallExpression.arguments] position.
         * @return The evaluated result (of type [T]) or `null`.
         */
        inline fun <reified T> getArgumentValueByNameOrPosition(
            call: CallExpression,
            name: String?,
            pos: Int?,
        ): T? {
            val arg =
                name?.let { call.argumentEdges[it] } ?: pos?.let { call.arguments.getOrNull(it) }
            val value =
                when (arg) {
                    is Literal<*> -> arg.value
                    is Reference -> arg.evaluate(PythonValueEvaluator())
                    else -> null
                }
            return if (value is T) {
                value
            } else {
                Util.errorWithFileLocation(
                    call,
                    log,
                    "Evaluated the argument to type \"{}\". Expected \"{}\". Returning \"null\".",
                    value?.let { it::class.simpleName },
                    T::class.simpleName,
                )
                null
            }
        }
    }
}
