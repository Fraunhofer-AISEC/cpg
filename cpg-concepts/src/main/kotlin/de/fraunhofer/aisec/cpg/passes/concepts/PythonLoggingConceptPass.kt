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
     */
    private val loggers = mutableMapOf<Node, LoggingNode>()
    private var loggingLogger: ImportDeclaration? = null

    override fun cleanup() {
        // nothing to do
    }

    /**
     * This pass is interested in [ImportDeclaration]s and [CallExpression]s as these are the
     * relevant parts of the Python code for logging.
     */
    override fun accept(comp: Component) {
        loggingLogger = comp.imports.singleOrNull { it.import.toString() == "logging" }
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
            val newNode = newLoggingNode(underlyingNode = importDeclaration)
            loggers += importDeclaration to newNode
        }
    }

    private fun handleCall(callExpression: CallExpression) {
        val callee = callExpression.callee

        if (callee.name.toString() == "logging.getLogger") {

            val newNode = newLoggingNode(underlyingNode = callExpression)
            loggers += callExpression to newNode
        } else if (callee.name.toString().startsWith("logging.")) {
            loggingLogger?.let { logger ->
                loggers[logger]?.let { logOpHelper(callExpression, it) }
            }
        } else {
            // might be a call like `logger.error`
            val logger = findLogger(callExpression)
            logger?.let { logOpHelper(callExpression, it) }
        }
    }

    private fun findLogger(callExpression: CallExpression): LoggingNode? {
        val callee = callExpression.callee
        if (callee is MemberExpression) {
            // might be a call like `logger.error`
            val base = callee.base
            val fulfilledPaths: List<List<Node>> =
                base
                    .followPrevFullDFGEdgesUntilHit(collectFailedPaths = false) {
                        it.overlays.any { overlay -> overlay is LoggingNode }
                    }
                    .fulfilled
            val loggers =
                fulfilledPaths
                    .map { path -> path.last() }
                    .flatMap { it.overlays }
                    .filterIsInstance<LoggingNode>()
                    .toSet()
            if (loggers.size > 1) {
                log.warn("Found multiple loggers. Selecting one at random.")
            }
            return loggers.firstOrNull()
        }
        return null
    }

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
