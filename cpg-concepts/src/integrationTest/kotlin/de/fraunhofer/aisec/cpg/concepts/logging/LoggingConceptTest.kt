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
package de.fraunhofer.aisec.cpg.concepts.logging

import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.concepts.logging.IsLogging
import de.fraunhofer.aisec.cpg.graph.concepts.logging.Log
import de.fraunhofer.aisec.cpg.graph.concepts.logging.LogLevel
import de.fraunhofer.aisec.cpg.graph.concepts.logging.LogWrite
import de.fraunhofer.aisec.cpg.graph.declarations.ImportDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.passes.concepts.logging.python.PythonLoggingConceptPass
import de.fraunhofer.aisec.cpg.query.dataFlow
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyze
import de.fraunhofer.aisec.cpg.test.assertLiteralValue
import java.nio.file.Path
import kotlin.test.*

/**
 * A class for integration tests. They depend on the Python frontend, so we classify them as an
 * integration test. This might be replaced with a language-neutral test at some point.
 */
class LoggingConceptTest : BaseTest() {
    @Test
    fun testSimpleLog() {
        val topLevel = Path.of("src", "integrationTest", "resources", "python", "logging")

        val result =
            analyze(
                files = listOf(topLevel.resolve("simple_log.py").toFile()),
                topLevel = topLevel,
                usePasses = true,
            ) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<PythonLoggingConceptPass>()
            }

        assertNotNull(result)

        val loggingNodes = result.conceptNodes
        assertTrue(loggingNodes.isNotEmpty())

        val warnLiteral = result.dLiterals.singleOrNull { it.value.toString() == "WARN" }
        assertLiteralValue("WARN", warnLiteral)

        val logDFG =
            warnLiteral.followNextFullDFGEdgesUntilHit(collectFailedPaths = false) {
                it is LogWrite
            }
        assertTrue(
            logDFG.fulfilled.isNotEmpty(),
            "Expected to find a dataflow from the literal \"WARN\" to a logging node.",
        )

        val logOp = logDFG.fulfilled.lastOrNull()?.nodes?.lastOrNull()
        assertIs<LogWrite>(logOp)
        assertEquals(LogLevel.WARN, logOp.logLevel)

        val getSecretCall = result.dCalls("get_secret").singleOrNull()
        assertIs<CallExpression>(getSecretCall)
        val nextDFG = getSecretCall.nextDFG
        assertTrue(nextDFG.isNotEmpty())
        val secretDFG = getSecretCall.followNextFullDFGEdgesUntilHit { it is LogWrite }
        assertTrue(
            secretDFG.fulfilled.isNotEmpty(),
            "Expected to find a dataflow from the CallExpression[get_secret] to a logging node.",
        )
    }

    @Test
    fun testSimpleLogWithGetLogger() {
        val topLevel = Path.of("src", "integrationTest", "resources", "python", "logging")

        val result =
            analyze(
                files = listOf(topLevel.resolve("simple_log_get_logger.py").toFile()),
                topLevel = topLevel,
                usePasses = true,
            ) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<PythonLoggingConceptPass>()
            }
        assertNotNull(result)

        val loggingNodes = result.conceptNodes
        assertTrue(loggingNodes.isNotEmpty())

        assertEquals(
            2,
            result.conceptNodes { it is Log }.size,
            "Expected to find 2 logging nodes. One from the `import logging` declaration (not used directly for logging) and one from the `logging.getLogger()` call (used with `logger.error(...)`).",
        )

        val literalERROR = result.dLiterals.singleOrNull { it.value.toString() == "ERROR" }
        assertNotNull(literalERROR)

        assertTrue(
            dataFlow(startNode = literalERROR) { it is Log }.value,
            "Expected to find a dataflow from the literal \"ERROR\" to a logging node.",
        )
    }

    @Test
    fun testLoggingWithAliasImport() {
        val topLevel = Path.of("src", "integrationTest", "resources", "python", "logging")

        val result =
            analyze(
                files = listOf(topLevel.resolve("simple_log_alias.py").toFile()),
                topLevel = topLevel,
                usePasses = true,
            ) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<PythonLoggingConceptPass>()
            }
        assertNotNull(result)

        val loggingNodes = result.conceptNodes
        assertTrue(loggingNodes.isNotEmpty())

        assertEquals(
            2,
            result.conceptNodes { it is Log }.size,
            "Expected to find 2 logging nodes. One from the `import logging as log` declaration (used with `log.info()`) and one from the `log.getLogger()` call (used with `logger.error(...)`).",
        )

        val literalINFO = result.dLiterals.singleOrNull { it.value.toString() == "INFO" }
        assertNotNull(literalINFO)

        assertTrue(
            dataFlow(startNode = literalINFO) {
                    it is Log && it.underlyingNode is ImportDeclaration
                }
                .value,
            "Expected to find a dataflow from the literal \"INFO\" to the logging node based on the import declaration.",
        )

        val literalERROR = result.dLiterals.singleOrNull { it.value.toString() == "ERROR" }
        assertNotNull(literalERROR)

        assertTrue(
            dataFlow(startNode = literalERROR) {
                    it is Log && it.underlyingNode?.code == "log.getLogger(__name__)"
                }
                .value,
            "Expected to find a dataflow from the literal \"ERROR\" to the logging node based on the `getLogger(__name__)` call.",
        )
    }

    @Test
    fun testLoggingMultipleLoggers() {
        val topLevel = Path.of("src", "integrationTest", "resources", "python", "logging")

        val result =
            analyze(
                files = listOf(topLevel.resolve("simple_log_get_logger_multiple.py").toFile()),
                topLevel = topLevel,
                usePasses = true,
            ) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<PythonLoggingConceptPass>()
            }
        assertNotNull(result)

        val loggingNodes = result.conceptNodes { it is IsLogging }
        assertTrue(loggingNodes.isNotEmpty())

        val allLoggers = result.conceptNodes { it is Log }
        assertEquals(
            3,
            allLoggers.size,
            "Expected to find 3 logging nodes. One from the `import logging as log` declaration and one `foo` and one `bar` logger from the `log.getLogger()` calls. The other `getLogger()` calls are duplicates and must not create new loggers.",
        )

        val defaultLogger = allLoggers.singleOrNull { it.underlyingNode is ImportDeclaration }
        assertNotNull(defaultLogger)

        val fooLogger =
            allLoggers.singleOrNull { it.underlyingNode?.code == "logging.getLogger('foo')" }
        assertNotNull(fooLogger)

        val barLogger =
            allLoggers.singleOrNull { it.underlyingNode?.code == "logging.getLogger('bar')" }
        assertNotNull(barLogger)

        // Testing setup. A map of a logger name (and thus the literal written to the logger) and
        // the corresponding logger.
        val testing =
            mapOf(
                "default logger" to defaultLogger,
                "foo logger" to fooLogger,
                "bar logger" to barLogger,
            )
        testing.entries.forEach {
            val literalString = it.key
            val goodLogger = it.value
            val badLoggers = allLoggers.filter { it !== goodLogger }

            val literals = result.dLiterals.filter { it.value == literalString }
            literals.forEach { currentLit ->
                assertTrue(
                    dataFlow(startNode = currentLit) { end -> end == goodLogger }.value,
                    "Expected to find a dataflow from the literal \"$literalString\" to the corresponding logger.",
                )
            }

            badLoggers.forEach { badLogger ->
                assertTrue(
                    literals.none { currentLit ->
                        dataFlow(startNode = currentLit) { end -> end == badLogger }.value
                    },
                    "Found a dataflow from the literal \"$literalString\" to a wrong logger.",
                )
            }
        }
    }
}
