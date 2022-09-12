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

import de.fraunhofer.aisec.cpg.TestUtils
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals

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
            ) { it.registerPass(UnresolvedDFGPass()) }

        val firstCall = result.calls { it.name == "get" }[0]
        val osDecl = result.variables["os"]
        assertEquals(1, firstCall.prevDFG.size)
        assertEquals(
            osDecl,
            (firstCall.prevDFG.firstOrNull() as? DeclaredReferenceExpression)?.refersTo
        )

        val callWithParam = result.calls { it.name == "get" }[1]
        assertEquals(2, callWithParam.prevDFG.size)
        assertEquals(
            osDecl,
            callWithParam.prevDFG
                .filterIsInstance<DeclaredReferenceExpression>()
                .firstOrNull()
                ?.refersTo
        )
        assertEquals(4, callWithParam.prevDFG.filterIsInstance<Literal<*>>().firstOrNull()?.value)
    }
}
