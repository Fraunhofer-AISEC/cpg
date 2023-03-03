/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DFGTest {
    companion object {
        private val topLevel = Path.of("src", "test", "resources", "dfg")
    }
    @Test
    fun testReturnStatement() {
        val result =
            TestUtils.analyze(
                listOf(Path.of(topLevel.toString(), "ReturnTest.java").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage(JavaLanguage())
            }
        val returnFunction = result.functions["testReturn"]
        assertNotNull(returnFunction)

        assertEquals(2, returnFunction.prevDFG.size)

        val allRealReturns = returnFunction.allChildren<ReturnStatement> { it.location != null }
        assertEquals(allRealReturns.toSet() as Set<Node>, returnFunction.prevDFG)

        assertEquals(1, allRealReturns[0].prevDFG.size)
        assertTrue(returnFunction.literals.first { it.value == 2 } in allRealReturns[0].prevDFG)
        assertEquals(1, allRealReturns[1].prevDFG.size)
        assertTrue(
            returnFunction.refs.last { it.name.localName == "a" } in allRealReturns[1].prevDFG
        )
    }
}
