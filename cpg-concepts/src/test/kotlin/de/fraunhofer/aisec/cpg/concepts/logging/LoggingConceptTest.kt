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
import de.fraunhofer.aisec.cpg.graph.concepts.logging.LogOperationNode
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.passes.concepts.LoggingConceptPass
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyze
import de.fraunhofer.aisec.cpg.test.assertLiteralValue
import java.nio.file.Path
import kotlin.test.*

class LoggingConceptTest : BaseTest() {
    @Test
    fun test01() {
        val topLevel = Path.of("src", "test", "resources", "concepts", "logging", "python")

        val result =
            analyze(
                files = listOf(topLevel.resolve("simple_log.py").toFile()),
                topLevel = topLevel,
                usePasses = false
            ) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<LoggingConceptPass>()
            }
        assertNotNull(result)

        val loggingNodes = result.conceptNodes
        assertTrue(loggingNodes.isNotEmpty())

        /*
        MATCH (n) WHERE n:LogOperationNode OR n:LoggingNode
        RETURN n

        MATCH (l:Literal)-[d:DFG*]->(o)
        WHERE l.code = "'WARN'"
        RETURN l,d,o
        */
        val warnLiteral = result.literals.singleOrNull { it.value.toString() == "WARN" }
        assertIs<Literal<*>>(
            warnLiteral
        ) // TODO why do we need this? should already be covered by the next line
        assertLiteralValue("WARN", warnLiteral)

        val logDFG =
            warnLiteral.followNextFullDFGEdgesUntilHit(collectFailedPaths = false) {
                it is LogOperationNode
            }
        assertTrue(logDFG.fulfilled.isNotEmpty())

        val logOp = logDFG.fulfilled.lastOrNull()?.lastOrNull()
        assertIs<LogOperationNode>(logOp)
        assertEquals(LogLevel.WARN, logOp.logLevel)

        val getSecretCall = result.calls("get_secret").singleOrNull()
        assertIs<CallExpression>(getSecretCall)
        val nextDFG = getSecretCall.nextDFG
        assertTrue(nextDFG.isNotEmpty())
        val secretDFG = getSecretCall.followNextFullDFGEdgesUntilHit { it is LogOperationNode }
        assertTrue(secretDFG.fulfilled.isNotEmpty())

        /*
        val secretRef = result.refs["secret"]
        assertIs<Reference>(secretRef)
        val secretDFG = secretRef.followNextFullDFGEdgesUntilHit { it is LogOperationNode }
        assertTrue(secretDFG.fulfilled.isNotEmpty())
        */
    }

    @Test
    fun test02() {
        val topLevel = Path.of("src", "test", "resources", "concepts", "logging", "python")

        val result =
            analyze(
                files = listOf(topLevel.resolve("simple_log2.py").toFile()),
                topLevel = topLevel,
                usePasses = false
            ) {
                it.registerLanguage<PythonLanguage>()
                it.registerPass<LoggingConceptPass>()
            }
        assertNotNull(result)

        val loggingNodes = result.conceptNodes
        assertTrue(loggingNodes.isNotEmpty())

        val getSecretCall = result.calls("get_secret").singleOrNull()
        assertIs<CallExpression>(getSecretCall)
        val nextDFG = getSecretCall.nextDFG
        assertTrue(nextDFG.isNotEmpty())
        val secretDFG = getSecretCall.followNextFullDFGEdgesUntilHit { it is LogOperationNode }
        assertTrue(secretDFG.fulfilled.isNotEmpty())

        /*
        val secretRef = result.refs["secret"]
        assertIs<Reference>(secretRef)
        val secretDFG = secretRef.followNextFullDFGEdgesUntilHit { it is LogOperationNode }
        assertTrue(secretDFG.fulfilled.isNotEmpty())
        */
    }
}
