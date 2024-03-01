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
package de.fraunhofer.aisec.cpg.frontends.cxx

import de.fraunhofer.aisec.cpg.TestUtils.analyzeAndGetFirstTU
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.printDFG
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CDataflowTest {
    @Test
    fun testTLSContext() {
        val file = File("src/test/resources/c/dataflow/tls.c")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
            }
        assertNotNull(tu)

        val main = tu.functions["main"]
        assertNotNull(main)

        val renegotiate = tu.functions["renegotiate"]
        assertNotNull(renegotiate)

        // Our start function and variable/parameter
        var startFunction = renegotiate
        var startVariable = renegotiate.parameters["ctx"]!!

        // In this example we want to have the list of all fields of "ctx" that
        // are written to in the renegotiate function itself. For this to achieve we can follow the
        // "FULL" dfg flow until the end and collect partial writes on the way.
        var flow = startVariable.followNextDFGEdgesUntilHit { it.nextDFG.isEmpty() }
        // We should end up with two flows, one is limited only to the function itself and the other
        // one is going into the inner_renegotiate
        assertEquals(1, flow.fulfilled.size)
        // The ctx in line 15 is only connected to the one in line 13

        println(flow)
        println("renegotiate and its direct callees writes to the following fields: ")

        println(main.variables["ctx"]?.printDFG(100))
    }
}
