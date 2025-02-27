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
import de.fraunhofer.aisec.cpg.graph.concepts.logging.Log
import de.fraunhofer.aisec.cpg.graph.concepts.logging.LogLevel
import de.fraunhofer.aisec.cpg.graph.concepts.logging.LogWriteOperation
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

        val warnLiteral = result.literals.singleOrNull { it.value.toString() == "WARN" }
        assertLiteralValue("WARN", warnLiteral)

        val logDFG =
            warnLiteral.followNextFullDFGEdgesUntilHit(collectFailedPaths = false) {
                it is LogWriteOperation
            }
        assertTrue(
            logDFG.fulfilled.isNotEmpty(),
            "Expected to find a dataflow from the literal \"WARN\" to a logging node.",
        )

        val logOp = logDFG.fulfilled.lastOrNull()?.lastOrNull()
        assertIs<LogWriteOperation>(logOp)
        assertEquals(LogLevel.WARN, logOp.logLevel)

        val getSecretCall = result.calls("get_secret").singleOrNull()
        assertIs<CallExpression>(getSecretCall)
        val nextDFG = getSecretCall.nextDFG
        assertTrue(nextDFG.isNotEmpty())
        val secretDFG = getSecretCall.followNextFullDFGEdgesUntilHit { it is LogWriteOperation }
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

        val literalERROR = result.literals.singleOrNull { it.value.toString() == "ERROR" }
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

        val literalINFO = result.literals.singleOrNull { it.value.toString() == "INFO" }
        assertNotNull(literalINFO)

        assertTrue(
            dataFlow(startNode = literalINFO) {
                    it is Log && it.underlyingNode is ImportDeclaration
                }
                .value,
            "Expected to find a dataflow from the literal \"INFO\" to the logging node based on the import declaration.",
        )

        val literalERROR = result.literals.singleOrNull { it.value.toString() == "ERROR" }
        assertNotNull(literalERROR)

        assertTrue(
            dataFlow(startNode = literalERROR) { it is Log && it.underlyingNode is CallExpression }
                .value,
            "Expected to find a dataflow from the literal \"ERROR\" to the logging node based on the `getLogger` call.",
        )
    }
}
