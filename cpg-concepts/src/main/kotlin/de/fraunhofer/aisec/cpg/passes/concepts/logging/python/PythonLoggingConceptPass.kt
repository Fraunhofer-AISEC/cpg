/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.passes.concepts.logging.python

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.graph.Backward
import de.fraunhofer.aisec.cpg.graph.GraphToFollow
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.argumentValueByNameOrPosition
import de.fraunhofer.aisec.cpg.graph.concepts.logging.*
import de.fraunhofer.aisec.cpg.graph.declarations.ImportDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.passes.DFGPass
import de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import de.fraunhofer.aisec.cpg.passes.concepts.ConceptPass
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteLate
import de.fraunhofer.aisec.cpg.query.dataFlow
import de.fraunhofer.aisec.cpg.query.successfulLastNodes
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This pass collects Python's `import logging` logs and maps them to the corresponding CPG
 * [IsLogging] concept nodes.
 */
@ExecuteLate
@DependsOn(SymbolResolver::class)
@DependsOn(EvaluationOrderGraphPass::class)
@DependsOn(DFGPass::class)
class PythonLoggingConceptPass(ctx: TranslationContext) : ConceptPass(ctx) {

    /**
     * A storage connecting CPG nodes with [Log]s. This is used to connect logging calls to the
     * matching [Log].
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
    private val loggers = mutableMapOf<String, Log>()

    private val DEFAULT_LOGGER_NAME = "logging"

    override fun handleNode(node: Node, tu: TranslationUnitDeclaration) {
        when (node) {
            is ImportDeclaration -> {
                handleImport(node)
            }
            is CallExpression -> {
                handleCall(node)
            }
        }
    }

    /**
     * Handles Python `import logging`.
     *
     * ```python
     * import logging
     * import logging as foo
     * ```
     *
     * will be translated to [Log]s.
     */
    private fun handleImport(importDeclaration: ImportDeclaration) {
        if (importDeclaration.import.toString() == "logging") {
            val logger = loggers[DEFAULT_LOGGER_NAME]
            if (logger == null) { // only add it once
                val newNode = newLog(underlyingNode = importDeclaration, name = DEFAULT_LOGGER_NAME)
                loggers += DEFAULT_LOGGER_NAME to newNode

                // also add a [LogGet] node
                newLogGet(underlyingNode = importDeclaration, logger = newNode)
            } else {
                // the logger is already present -> only add a [LogGet] node
                newLogGet(underlyingNode = importDeclaration, logger = logger)
            }
        }
    }

    /**
     * Translates a call like `logging.error(...)` to the corresponding concept nodes.
     * - `logging.getLogger` creates a new [Log]
     * - `logging.critical(...)` (and similar for `error` / `warn` / ...) is translated to a
     *   [de.fraunhofer.aisec.cpg.graph.concepts.logging.LogWrite]
     *
     * @param call The [CallExpression] to handle
     * @return n/a (The new node is created and added to the graph)
     */
    private fun handleCall(call: CallExpression) {
        val callee = call.callee

        if (callee.name.toString() == "logging.getLogger") {
            val loggerName = call.argumentValueByNameOrPosition<String>(name = "name", position = 0)
            val normalizedLoggerName =
                when (loggerName) {
                    "",
                    "null" // Pythons `None` is translated to a Kotlin `null`
                    -> DEFAULT_LOGGER_NAME
                    null -> {
                        Util.errorWithFileLocation(
                            call,
                            log,
                            "Failed to parse the loggers name. Using \"{}\" as a fallback.",
                            DEFAULT_LOGGER_NAME,
                        )
                        DEFAULT_LOGGER_NAME
                    }
                    else -> loggerName
                }
            val logger = loggers[normalizedLoggerName]
            if (logger == null) { // only add it once
                newLog(underlyingNode = call, name = normalizedLoggerName).also {
                    loggers += normalizedLoggerName to it
                    newLogGet(underlyingNode = call, logger = it)
                }
            } else {
                // the logger is already present -> only add a [LogGet] node
                newLogGet(underlyingNode = call, logger = logger)
            }
        } else if (callee.name.toString().startsWith("logging.")) {
            loggers[DEFAULT_LOGGER_NAME]?.let { logOpHelper(call, it) }
        } else {
            // might be a call like `logger.error`
            val logger = findLogger(call)
            logger?.let { logOpHelper(call, it) }
        }
    }

    /**
     * Finds the corresponding logger for the given [call].
     *
     * This function works by walking the DFG backwards on the [call]s base to find a [LogGet] and
     * then via [LogGet.concept] to the actual [Log] node.
     *
     * @param call The call to handle.
     * @return The [Log] if found.
     */
    private fun findLogger(call: CallExpression): Log? {
        val callee = call.callee
        if (callee is MemberExpression) {
            // might be a call like `logger.error`
            val base = callee.base
            val pathsToLogGet =
                dataFlow(startNode = base, direction = Backward(GraphToFollow.DFG)) {
                    it.overlays.any { overlay ->
                        overlay is LogGet
                    } // we are logging for a node which has a [LogGet] attached to it
                }

            val loggers =
                pathsToLogGet
                    .successfulLastNodes()
                    .flatMap {
                        it.overlays // move to the "overlays" world
                    }
                    .filterIsInstance<LogGet>() // discard not-relevant overlays
                    .map {
                        it.concept // move from [LogGet] to the corresponding [Log] concept node
                    }

            if (loggers.size > 1) {
                log.error("Found multiple loggers. Selecting one at random.")
            }
            return loggers.firstOrNull()
        }
        return null
    }

    /**
     * Handles a call to a log. This currently maps the "log write" calls (e.g.
     * `log.critical("...")`) to a [LogWrite] node.
     *
     * A warning is logged if the call cannot be handled (i.e. not implemented).
     *
     * @param call The underlying [CallExpression] to handle.
     * @param logger The [Log] this call expression operates on.
     * @return n/a (The new node is created and added to the graph)
     */
    private fun logOpHelper(call: CallExpression, logger: Log) {
        val callee = call.callee
        when (callee.name.localName.toString()) {
            "fatal",
            "critical",
            "error",
            "warn",
            "warning",
            "info",
            "debug" -> {
                val name = call.name.localName.toString()
                val lvl = logLevelStringToEnum(name)
                newLogWrite(
                    underlyingNode = call,
                    logger = logger,
                    logArguments = call.arguments,
                    level = lvl,
                )
            }
            else -> {
                log.warn("Found an unhandled logging call: \"$call\".")
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
            "fatal" -> LogLevel.FATAL
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

    companion object {
        val log: Logger = LoggerFactory.getLogger(this.javaClass)
    }
}
