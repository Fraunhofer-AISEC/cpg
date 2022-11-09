/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.InferenceConfiguration
import de.fraunhofer.aisec.cpg.TestUtils
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UnresolvedDFGPassTest {
    companion object {
        private val topLevel = Path.of("src", "test", "resources", "dfg")
    }

    @Test
    fun testUnresolvedCalls() {
        val result =
            TestUtils.analyze(
                listOf(Path.of(topLevel.toString(), "DfgUnresolvedCalls.java").toFile()),
                topLevel,
                true
            ) {
                it.inferenceConfiguration(
                    InferenceConfiguration.builder().inferDfgForUnresolvedCalls(true).build()
                )
            }

        // Flow from base to return value
        val firstCall = result.calls { it.fullName.localName == "get" }[0]
        val osDecl = result.variables["os"]
        assertEquals(1, firstCall.prevDFG.size)
        assertEquals(
            osDecl,
            (firstCall.prevDFG.firstOrNull() as? DeclaredReferenceExpression)?.refersTo
        )

        // Flow from base and argument to return value
        val callWithParam = result.calls { it.fullName.localName == "get" }[1]
        assertEquals(2, callWithParam.prevDFG.size)
        assertEquals(
            osDecl,
            callWithParam.prevDFG
                .filterIsInstance<DeclaredReferenceExpression>()
                .firstOrNull()
                ?.refersTo
        )
        assertEquals(4, callWithParam.prevDFG.filterIsInstance<Literal<*>>().firstOrNull()?.value)

        // No specific flows for resolved functions
        // => Goes through the method declaration and then follows the instructions in the method's
        // implementation
        val knownCall = result.calls { it.fullName.localName == "knownFunction" }[0]
        assertEquals(1, knownCall.prevDFG.size)
        assertTrue(knownCall.prevDFG.firstOrNull() is MethodDeclaration)
    }

    @Test
    fun testUnresolvedCallsNoInference() {
        val result =
            TestUtils.analyze(
                listOf(Path.of(topLevel.toString(), "DfgUnresolvedCalls.java").toFile()),
                topLevel,
                true
            ) {
                it.inferenceConfiguration(
                    InferenceConfiguration.builder().inferDfgForUnresolvedCalls(false).build()
                )
            }

        // No flow from base to return value
        val firstCall = result.calls { it.fullName.localName == "get" }[0]
        val osDecl = result.variables["os"]
        assertEquals(0, firstCall.prevDFG.size)

        // No flow from base or argument to return value
        val callWithParam = result.calls { it.fullName.localName == "get" }[1]
        assertEquals(0, callWithParam.prevDFG.size)

        // No specific flows for resolved functions
        // => Goes through the method declaration and then follows the instructions in the method's
        // implementation
        val knownCall = result.calls { it.fullName.localName == "knownFunction" }[0]
        assertEquals(1, knownCall.prevDFG.size)
        assertTrue(knownCall.prevDFG.firstOrNull() is MethodDeclaration)
    }
}
