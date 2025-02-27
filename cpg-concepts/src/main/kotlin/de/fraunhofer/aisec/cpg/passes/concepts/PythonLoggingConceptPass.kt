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
package de.fraunhofer.aisec.cpg.passes.concepts

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.concepts.logging.LogLevel
import de.fraunhofer.aisec.cpg.graph.concepts.logging.LoggingNode
import de.fraunhofer.aisec.cpg.graph.concepts.logging.newLogOperationNode
import de.fraunhofer.aisec.cpg.graph.concepts.logging.newLoggingNode
import de.fraunhofer.aisec.cpg.graph.declarations.ImportDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.passes.ComponentPass
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteLate
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This pass collects Python's `import logging` logs and maps them to the corresponding CPG
 * [Concept] nodes.
 */
@ExecuteLate
class PythonLoggingConceptPass(ctx: TranslationContext) : ComponentPass(ctx) {
    private var log: Logger = LoggerFactory.getLogger(this.javaClass)

    /**
     * A storage connecting CPG nodes with [LoggingNode]s. This is used to connect logging calls to
     * the matching [LoggingNode].
     *
     * The key corresponds to the loggers name. The following all map to an empty string (matching
     * Pythons behavior):
     * - `import logging`
     * - `logging.getLogger()`
     * - `logging.getLogger(None)`
     * - `logging.getLogger("")`
     *
     * Individual loggers can be obtained by providing an identifier:
     * - `logging.getLogger("foo")` -
     */
    private val loggers = mutableMapOf<String, LoggingNode>()

    private val DEFAULT_LOGGER_NAME = ""

    /** The global `import logging` node. */
    private var loggingLogger: ImportDeclaration? = null

    override fun cleanup() {
        // nothing to do
    }

    /**
     * This pass is interested in [ImportDeclaration]s and [CallExpression]s as these are the
     * relevant parts of the Python code for logging.
     */
    override fun accept(comp: Component) {
        loggingLogger =
            comp.imports.singleOrNull { import -> import.import.toString() == "logging" }
        comp.imports.forEach { import -> handleImport(import) }
        comp.calls.forEach { call -> handleCall(call) }
    }

    /**
     * Handles Python `import logging`.
     *
     * ```python
     * import logging
     * import logging as foo
     * ```
     *
     * will be translated to [LoggingNode]s.
     */
    private fun handleImport(importDeclaration: ImportDeclaration) {
        if (importDeclaration.import.toString() == "logging") {
            if (loggers[DEFAULT_LOGGER_NAME] == null) { // only add it once
                val newNode =
                    newLoggingNode(underlyingNode = importDeclaration, name = DEFAULT_LOGGER_NAME)
                loggers += DEFAULT_LOGGER_NAME to newNode
            }
        }
    }

    /**
     * Translates a call like `logging.error(...)` to the corresponding concept nodes.
     * - `logging.getLogger` creates a new [LoggingNode]
     * - `logging.critical(...)` (and similar for `error` / `warn` / ...) is translated to a
     *   [de.fraunhofer.aisec.cpg.graph.concepts.logging.LogWriteOperation]
     *
     * @param callExpression The [CallExpression] to handle
     * @return n/a (The new node is created and added to the graph)
     */
    private fun handleCall(callExpression: CallExpression) {
        val callee = callExpression.callee

        if (callee.name.toString() == "logging.getLogger") {
            val loggerName = callExpression.arguments.firstOrNull()?.evaluate().toString()
            if (loggers[loggerName] == null) { // only add it once
                val newNode = newLoggingNode(underlyingNode = callExpression, name = loggerName)
                loggers += loggerName to newNode
            }
        } else if (callee.name.toString().startsWith("logging.")) {
            loggers[DEFAULT_LOGGER_NAME]?.let { logOpHelper(callExpression, it) }
        } else {
            // might be a call like `logger.error`
            val logger = findLogger(callExpression)
            logger?.let { logOpHelper(callExpression, it) }
        }
    }

    /**
     * Finds the corresponding logger for the given [callExpression].
     *
     * This function works by walking the DFG backwards on the [callExpression]s base to find a
     * [LoggingNode].
     *
     * @param callExpression The call to handle.
     * @return The [LoggingNode] if found.
     */
    private fun findLogger(callExpression: CallExpression): LoggingNode? {
        val callee = callExpression.callee
        if (callee is MemberExpression) {
            // might be a call like `logger.error`
            val base = callee.base
            val fulfilledPaths: List<List<Node>> =
                base
                    .followPrevFullDFGEdgesUntilHit(collectFailedPaths = false) {
                        it.overlays.any { overlay ->
                            overlay is LoggingNode
                        } // we are logging for a node which has a [LoggingNode] attached to it
                    }
                    .fulfilled
            val loggers =
                fulfilledPaths
                    .map { path ->
                        path.last()
                    } // we're interested in the last node of the path, i.e. the node connected to
                    // the [LoggingNode]
                    .flatMap { it.overlays } // move to the "overlays" world
                    .filterIsInstance<LoggingNode>()
                    .toSet()
            if (loggers.size > 1) {
                log.warn("Found multiple loggers. Selecting one at random.")
            }
            return loggers.firstOrNull()
        }
        return null
    }

    /**
     * Handles a call to a log. This currently maps the "log write" calls (e.g.
     * `log.critical("...")`) to a [LogWriteOperation] node.
     *
     * A warning is logged if the call cannot be handled (i.e. not implemented).
     *
     * @param callExpression The underlying [CallExpression] to handle.
     * @param logger The [LoggingNode] this call expression operates on.
     * @return n/a (The new node is created and added to the graph)
     */
    private fun logOpHelper(callExpression: CallExpression, logger: LoggingNode) {
        val callee = callExpression.callee
        when (callee.name.localName.toString()) {
            "critical",
            "error",
            "warn",
            "warning",
            "info",
            "debug" -> {
                val name = callExpression.name.localName.toString()
                val lvl = logLevelStringToEnum(name)
                newLogOperationNode(
                    underlyingNode = callExpression,
                    logger = logger,
                    logArguments = callExpression.arguments,
                    level = lvl,
                )
            }
            else -> {
                log.warn("Found an unhandled logging call: \"$callExpression\".")
            }
        }
    }

    /**
     * Maps a string "critical" / ... to the corresponding [LogLevel]. [LogLevel.UNKNOWN] is used
     * when the translation fails and a warning is logged.
     *
     * @param loglevel The string to parse.
     * @return The corresponding log level.
     */
    private fun logLevelStringToEnum(loglevel: String): LogLevel {
        return when (loglevel) {
            "critical" -> LogLevel.CRITICAL
            "error" -> LogLevel.ERROR
            "warn",
            "warning" -> LogLevel.WARN
            "info" -> LogLevel.INFO
            "debug" -> LogLevel.DEBUG
            "trace" -> LogLevel.TRACE
            else -> {
                log.warn("Unknown log level \"$loglevel\". Using \"${LogLevel.UNKNOWN}\".")
                LogLevel.UNKNOWN
            }
        }
    }
}
