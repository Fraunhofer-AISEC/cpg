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
import de.fraunhofer.aisec.cpg.analysis.MultiValueEvaluator
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

@ExecuteLate
class PythonFileConceptPass(ctx: TranslationContext) :
    ConceptPass(ctx) { // TODO logging 2 ConceptPass

    /** Indicates a flag value we could not parse. This is an internal issue. */
    internal val FLAGS_ERROR = 42424242L

    /** The file name used if we fail to find it. */
    internal val DEFAULT_FILE_NAME = "DEFAULT_FILE_NAME"

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
            val newFileNode = newFile(underlyingNode = callExpression, fileName = fileName)
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
                                callExpression.name.localName,
                            )
                    }
                }
        } else {
            when (callExpression.callee.name.toString()) {
                "os.open" -> {
                    val newFileNode =
                        newFile(
                            underlyingNode = callExpression,
                            fileName = getFileName(callExpression, "path"),
                        )
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
            }
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
    private fun findFile(expression: Expression): File? {
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
            log.error("Found multiple files. Selecting one at random.")
        }
        return fileCandidates.firstOrNull()
    }

    /**
     * Parses the name of the file used in a builtin-`open` call or `os.open` call. The name of the
     * parameter depends on the open function but, it's the first parameter if a call without named
     * arguments is analyzed.
     *
     * @param callExpression The [CallExpression] (builtin-`open` or `os.open`) to be analyzed.
     * @return The name or [DEFAULT_FILE_NAME] if no name could be found.
     */
    private fun getFileName(callExpression: CallExpression, argumentName: String): String {
        val nameArg =
            callExpression.argumentEdges[argumentName] ?: callExpression.arguments.getOrNull(0)
        val name =
            when (nameArg) {
                is Literal<*> -> nameArg.value as? String
                is Reference -> null // TODO MultivalueEvaluator
                else -> null
            }
        return if (name != null) {
            name
        } else {
            Util.errorWithFileLocation(
                node = callExpression,
                log = log,
                format = "Failed to handle the name. Using a default name of \"{}\".",
                DEFAULT_FILE_NAME,
            )
            DEFAULT_FILE_NAME
        }
    }

    /**
     * Handles the `mode` parameter of Pythons builtin `open` function.
     *
     * Do not confuse with the `mode` in `os.open` (see [TODO]). [Signature
     * `open`](https://docs.python.org/3/library/functions.html#open):
     * ```python
     * open(file, mode='r', buffering=-1, encoding=None, errors=None, newline=None, closefd=True, opener=None)
     * ```
     */
    private fun getBuiltinOpenMode(callExpression: CallExpression): String? {
        val modeArg = callExpression.argumentEdges["mode"] ?: callExpression.arguments.getOrNull(1)
        return when (modeArg) {
            null -> null
            is Literal<*> -> {
                val v = modeArg.value
                if (v is String) {
                    v
                } else {
                    Util.errorWithFileLocation(
                        node = callExpression,
                        log = log,
                        format = "Expected a string mode.",
                    )
                    null
                }
            }
            else -> {
                Util.errorWithFileLocation(
                    node = callExpression,
                    log = log,
                    format = "Failed to handle the mode.",
                )
                return null
            }
        }
    }

    /**
     * Handles the `mask` parameter of `os.open` function.
     *
     * Do not confuse with the builtin `open` (see [getBuiltinOpenMode]). [Signature
     * `os.open`](https://docs.python.org/3/library/os.html#os.open):
     * ```python
     * os.open(path, flags, mode=0o777, *, dir_fd=None)
     * ```
     */
    private fun getOsOpenMask(callExpression: CallExpression): Long? {
        val maskArgument =
            callExpression.argumentEdges["mask"] ?: callExpression.arguments.getOrNull(2)
        return when (maskArgument) {
            null -> null
            is Literal<*> -> {
                val v = maskArgument.value
                if (v is Long) {
                    v
                } else {
                    Util.errorWithFileLocation(
                        node = callExpression,
                        log = log,
                        format = "Expected an Int mask.",
                    )
                    null
                }
            }
            else -> {
                Util.errorWithFileLocation(
                    node = callExpression,
                    log = log,
                    format = "Failed to handle the mask.",
                )
                return null
            }
        }
    }

    private fun getOsOpenFlags(callExpression: CallExpression): Long? {
        val flagsArg =
            callExpression.argumentEdges["flags"] ?: callExpression.arguments.getOrNull(1)

        return when (flagsArg) {
            null -> null
            is Literal<*> -> {
                val v = flagsArg.value
                if (v is Long) {
                    v
                } else {
                    Util.errorWithFileLocation(
                        node = callExpression,
                        log = log,
                        format = "Expected an Int flags argument.",
                    )
                    null
                }
            }
            is Reference -> {
                val evaluated = flagsArg.evaluate(MultiValueEvaluator()) // TODO
                Util.errorWithFileLocation(
                    node = callExpression,
                    log = log,
                    format = "Handling of os.open flags is not yet implemented.",
                )
                FLAGS_ERROR
            }
            else -> {
                Util.errorWithFileLocation(
                    node = callExpression,
                    log = log,
                    format = "Failed to handle the the flags argument.",
                )
                return null
            }
        }
    }

    private fun translateOsOpenFlags(flags: Long): Set<FileFlags> {
        return when (flags) {
            0L -> setOf(FileFlags.RDONLY)
            FLAGS_ERROR -> setOf(FileFlags.UNKNOWN)
            else -> TODO()
        }
    }

    /**
     * Translates the `mode` string of the
     * [builtin `open` function](https://docs.python.org/3/library/functions.html#open)
     */
    private fun translateBuiltinOpenMode(mode: String): Set<FileFlags> {
        return when (mode) {
            "w" -> setOf(FileFlags.WRONLY)
            "r",
            "rt" -> setOf(FileFlags.RDONLY)
            else -> TODO()
        }
    }
}
