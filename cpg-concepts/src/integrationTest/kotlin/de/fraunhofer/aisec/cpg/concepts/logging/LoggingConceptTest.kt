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
import de.fraunhofer.aisec.cpg.graph.concepts.logging.LogLevel
import de.fraunhofer.aisec.cpg.graph.concepts.logging.LogWriteOperation
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.passes.concepts.FileConceptPass
import de.fraunhofer.aisec.cpg.passes.concepts.PythonLoggingConceptPass
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyze
import de.fraunhofer.aisec.cpg.test.assertLiteralValue
import java.nio.file.Path
import kotlin.test.*
import org.junit.jupiter.api.Tag

/**
 * A class for integration tests. They depend on the Python frontend, so we classify them as an
 * integration test. This might be replaced with a language-neutral test at some point.
 */
@Tag("integration")
class LoggingConceptTest : BaseTest() {
    @Test
    fun test01() {
        val topLevel =
            Path.of("src", "integrationTest", "resources", "concepts", "logging", "python")

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
    fun test02() {
        val topLevel =
            Path.of("src", "integrationTest", "resources", "concepts", "logging", "python")

        val result =
            analyze(
                files = listOf(topLevel.resolve("simple_log2.py").toFile()),
                topLevel = topLevel,
                usePasses = true,
            ) {
                it.registerLanguage<PythonLanguage>()

                it.registerPass<PythonLoggingConceptPass>()
                it.registerPass<FileConceptPass>()
            }
        assertNotNull(result)

        val loggingNodes = result.conceptNodes
        assertTrue(loggingNodes.isNotEmpty())
    }
}
