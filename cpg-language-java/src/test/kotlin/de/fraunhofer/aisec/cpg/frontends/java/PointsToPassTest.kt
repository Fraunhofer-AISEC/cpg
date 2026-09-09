/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.java

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.edges.flows.ContextSensitiveDataflow
import de.fraunhofer.aisec.cpg.graph.edges.flows.PartialDataflowGranularity
import de.fraunhofer.aisec.cpg.test.*
import java.io.File
import kotlin.test.*

internal class PointsToPassTest : BaseTest() {

    @Test
    fun testFunctionSummaryToBaseWrite() {
        val file = File("src/test/resources/pointsToPassTest/testAdd.java")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<JavaLanguage>()
                it.registerFunctionSummaries(File("src/test/resources/dfg-summaries.yml"))
            }
        assertNotNull(tu)

        // Functions
        val testFunc = tu.functions("testAdd").single()
        val listAddFunc = tu.functions.single { it.name.toString() == "java.util.ArrayList.add" }
        val stringListAddFunc =
            tu.functions.single { it.name.toString() == "java.util.List<java.lang.String>.add" }

        // Params
        val listAddParam = listAddFunc.parameters.single()
        val stringListAddParam = stringListAddFunc.parameters.single()

        // Calls
        val addCall1 = testFunc.calls("add")[0]
        assertNotNull(addCall1)
        val addCall2 = testFunc.calls("add")[1]
        assertNotNull(addCall2)

        // Variables
        val testVar = testFunc.variables["list"]
        assertNotNull(testVar)

        // Div
        val memberAccesses = tu.memberExpressions.filter { it.base.name.localName == "list" }
        assertNotEquals(0, memberAccesses.size)
        val memberAccess1Base = memberAccesses[0].base
        assertNotNull(memberAccess1Base)
        val memberAccess2Base = memberAccesses[1].base
        assertNotNull(memberAccess2Base)
        val memberAccess3Base = memberAccesses[2].base
        assertNotNull(memberAccess3Base)

        // Actual tests

        // The first base in Line 10 only has one full prevDFG edge, the one to the variable
        assertEquals(1, memberAccess1Base.prevDFG.size)
        assertEquals(testVar, memberAccess1Base.prevFullDFG.singleOrNull())

        // The second base in Line 11 has the full prevDFG edge, plus 2 partial prevDFG edges to
        // the parameter of the add
        assertEquals(3, memberAccess2Base.prevDFG.size)
        assertEquals(testVar, memberAccess2Base.prevFullDFG.singleOrNull())
        assertEquals(
            1,
            memberAccess2Base.prevDFGEdges
                .filter {
                    (it.granularity as? PartialDataflowGranularity<String>)?.partialTarget ==
                        "add" &&
                        (it as? ContextSensitiveDataflow)?.callingContext?.calls?.single() ==
                            addCall1 &&
                        it.start == listAddParam
                }
                .size,
        )
        assertEquals(
            1,
            memberAccess2Base.prevDFGEdges
                .filter {
                    (it.granularity as? PartialDataflowGranularity<String>)?.partialTarget ==
                        "add" &&
                        (it as? ContextSensitiveDataflow)?.callingContext?.calls?.single() ==
                            addCall1 &&
                        it.start == stringListAddParam
                }
                .size,
        )

        // The third base in Line 12 has the full prevDFG Edges plus 4 partial edges: one for each
        // call and each parameter
        assertEquals(5, memberAccess3Base.prevDFG.size)
        assertEquals(testVar, memberAccess3Base.prevFullDFG.singleOrNull())
        assertEquals(
            2,
            memberAccess3Base.prevDFGEdges
                .filter {
                    (it.granularity as? PartialDataflowGranularity<String>)?.partialTarget ==
                        "add" &&
                        (it as? ContextSensitiveDataflow)?.callingContext?.calls?.single() in
                            setOf(addCall1, addCall2) &&
                        it.start == listAddParam
                }
                .size,
        )
        assertEquals(
            2,
            memberAccess3Base.prevDFGEdges
                .filter {
                    (it.granularity as? PartialDataflowGranularity<String>)?.partialTarget ==
                        "add" &&
                        (it as? ContextSensitiveDataflow)?.callingContext?.calls?.single() in
                            setOf(addCall1, addCall2) &&
                        it.start == stringListAddParam
                }
                .size,
        )
    }
}
