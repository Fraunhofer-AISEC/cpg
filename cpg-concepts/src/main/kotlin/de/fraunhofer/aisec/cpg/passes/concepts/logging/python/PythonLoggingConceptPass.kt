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
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.concepts.logging.*
import de.fraunhofer.aisec.cpg.graph.declarations.Import
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberExpression
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.ComponentPass
import de.fraunhofer.aisec.cpg.passes.DFGPass
import de.fraunhofer.aisec.cpg.passes.Description
import de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteLate
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy
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
@Description("Translates Python logging imports and calls to logging concept nodes.")
class PythonLoggingConceptPass(ctx: TranslationContext) : ComponentPass(ctx) {
    private var log: Logger = LoggerFactory.getLogger(this.javaClass)
    lateinit var walker: SubgraphWalker.ScopedWalker<Node>

    /**
     * A storage connecting CPG nodes with [de.fraunhofer.aisec.cpg.graph.concepts.logging.Log]s.
     * This is used to connect logging calls to the matching
     * [de.fraunhofer.aisec.cpg.graph.concepts.logging.Log].
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

    private val defaultLoggerName = ""

    /** The global `import logging` node. */
    private var loggingLogger: Import? = null

    override fun cleanup() {
        // nothing to do
    }

    /**
     * This pass needs to handle nodes in EOG order to make sure that log creation is handled before
     * log usage. The actual handling is done in [handleNode].
     */
    override fun accept(comp: Component) {
        walker = SubgraphWalker.ScopedWalker(ctx.scopeManager, Strategy::EOG_FORWARD)
        walker.registerHandler { node -> handleNode(node) }
        // Gather all resolution EOG starters; and make sure they really do not have a
        // predecessor, otherwise we might analyze a node multiple times
        val nodes = comp.allEOGStarters.filter { it.prevEOGEdges.isEmpty() }
        walker.iterateAll(nodes)

        // Store the global `import logging` node for later use
        loggingLogger =
            comp.imports.singleOrNull { import -> import.import.toString() == "logging" }
    }

    /**
     * This pass is interested in [Import]s and
     * [de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression]s as these are the
     * relevant parts of the Python code for logging.
     */
    private fun handleNode(node: Node) {
        when (node) {
            is Import -> handleImport(node)
            is CallExpression -> handleCall(node)
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
    private fun handleImport(importDeclaration: Import) {
        if (importDeclaration.import.toString() == "logging") {
            // Add the GetLog operation to the existing Log concept or generate a new one if there
            // is nothing available yet.
            val logger =
                loggers.computeIfAbsent(defaultLoggerName) {
                    newLog(
                        underlyingNode = importDeclaration,
                        name = defaultLoggerName,
                        connect = true,
                    )
                }
            newLogGet(underlyingNode = importDeclaration, concept = logger, connect = true)
        }
    }

    /**
     * Translates a call like `logging.error(...)` to the corresponding concept nodes.
     * - `logging.getLogger` creates a new [Log]
     * - `logging.critical(...)` (and similar for `error` / `warn` / ...) is translated to a
     *   [de.fraunhofer.aisec.cpg.graph.concepts.logging.LogWrite]
     *
     * @param callExpression The
     *   [de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression] to handle
     * @return n/a (The new node is created and added to the graph)
     */
    private fun handleCall(callExpression: CallExpression) {
        val callee = callExpression.callee

        if (callee.name.toString() == "logging.getLogger") {
            val loggerName = callExpression.arguments.firstOrNull()?.evaluate().toString()
            val normalizedLoggerName =
                when (loggerName) {
                    "",
                    "null" // Pythons `None` is translated to a Kotlin `null`
                    -> defaultLoggerName
                    else -> loggerName
                }
            val logger =
                loggers.computeIfAbsent(normalizedLoggerName) {
                    newLog(
                        underlyingNode = callExpression,
                        name = normalizedLoggerName,
                        connect = true,
                    )
                }
            newLogGet(underlyingNode = callExpression, concept = logger, connect = true)
        } else if (callee.name.toString().startsWith("logging.")) {
            loggers[defaultLoggerName]?.let { logOpHelper(callExpression, it) }
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
     * [Log].
     *
     * @param callExpression The call to handle.
     * @return The [Log] if found.
     */
    private fun findLogger(callExpression: CallExpression): Log? {
        val callee = callExpression.callee
        if (callee is MemberExpression) {
            // might be a call like `logger.error`
            val base = callee.base
            val fulfilledPaths: List<NodePath> =
                base
                    .followPrevFullDFGEdgesUntilHit(
                        collectFailedPaths = false,
                        findAllPossiblePaths = false,
                    ) {
                        it.overlays.any { overlay ->
                            overlay is LogGet
                        } // we are logging for a node which has a [LogGet] attached to it
                    }
                    .fulfilled
            val loggers =
                fulfilledPaths
                    .map { path ->
                        path.nodes.last()
                    } // we're interested in the last node of the path, i.e. the node connected to
                    // the [LogGet] node
                    .flatMap { it.overlays } // move to the "overlays" world
                    .filterIsInstance<LogGet>() // discard not-relevant overlays
                    .map {
                        it.concept
                    } // move from [LogGet] to the corresponding [Log] concept node
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
     * @param callExpression The underlying [CallExpression] to handle.
     * @param logger The [Log] this call expression operates on.
     * @return n/a (The new node is created and added to the graph)
     */
    private fun logOpHelper(callExpression: CallExpression, logger: Log) {
        val callee = callExpression.callee
        when (callee.name.localName) {
            "fatal",
            "critical",
            "error",
            "warn",
            "warning",
            "info",
            "debug" -> {
                val name = callExpression.name.localName
                val lvl = logLevelStringToEnum(name)
                newLogWrite(
                    underlyingNode = callExpression,
                    concept = logger,
                    logArguments = callExpression.arguments,
                    level = lvl,
                    connect = true,
                )
            }
            else -> {
                log.warn("Found an unhandled logging call: \"$callExpression\".")
            }
        }
    }

    /**
     * Maps a string "critical" / ... to the corresponding
     * [de.fraunhofer.aisec.cpg.graph.concepts.logging.LogLevel].
     * [de.fraunhofer.aisec.cpg.graph.concepts.logging.LogLevel.UNKNOWN] is used when the
     * translation fails and a warning is logged.
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
}
