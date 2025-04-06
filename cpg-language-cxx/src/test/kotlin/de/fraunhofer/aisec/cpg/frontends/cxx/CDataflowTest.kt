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

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.edges.flows.Dataflow
import de.fraunhofer.aisec.cpg.graph.edges.flows.FieldDataflowGranularity
import de.fraunhofer.aisec.cpg.graph.edges.flows.PartialDataflowGranularity
import de.fraunhofer.aisec.cpg.test.*
import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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

        val i = assertNotNull(tu.fields["tls_context::i"])
        val j = assertNotNull(tu.fields["tls_context::j"])
        val k = assertNotNull(tu.fields["tls_context::k"])

        val renegotiate = tu.functions["renegotiate"]
        assertNotNull(renegotiate)

        // Our start function and variable/parameter
        val startFunction = renegotiate
        // The start variable is the deref value of the parameter, so we use
        // .memoryValue.memoryValue
        val startVariable =
            startFunction.parameters["ctx"]
                ?.memoryValueEdges
                ?.singleOrNull {
                    (it.granularity as? PartialDataflowGranularity<*>)?.partialTarget ==
                        "derefvalue"
                }
                ?.start
        assertNotNull(startVariable)

        // In this example we want to have the list of all fields of "ctx" that
        // are written to in the start function itself. For this to achieve we can follow the
        // "FULL" dfg flow until the end and collect partial writes on the way.
        val result = startVariable.collectAllNextFullDFGPaths().flatten().toSet()
        assertNotNull(result)
        val fields =
            result
                .flatMap {
                    it.prevDFGEdges
                        .map(Dataflow::granularity)
                        .filterIsInstance<FieldDataflowGranularity>()
                }
                .mapNotNull(FieldDataflowGranularity::partialTarget)
                .toSet()

        assertTrue(fields.any { it.name.localName == "i" })
        assertTrue(fields.any { it.name.localName == "j" })

        // With the PointerPass, we can also use the functionSummary to achieve the same
        startFunction.functionSummary
            .flatMap { it.value }
            .map { it.subAccessName }
            .any { it == "i" }
        startFunction.functionSummary
            .flatMap { it.value }
            .map { it.subAccessName }
            .any { it == "j" }
    }
}
