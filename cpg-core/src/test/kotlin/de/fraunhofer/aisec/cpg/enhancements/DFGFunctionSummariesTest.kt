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
package de.fraunhofer.aisec.cpg.enhancements

import de.fraunhofer.aisec.cpg.GraphExamples
import de.fraunhofer.aisec.cpg.InferenceConfiguration
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.builder.*
import de.fraunhofer.aisec.cpg.graph.edge.CallingContextIn
import de.fraunhofer.aisec.cpg.graph.edge.CallingContextOut
import de.fraunhofer.aisec.cpg.graph.edge.ContextsensitiveDataflow
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.pointer
import de.fraunhofer.aisec.cpg.passes.inference.DFGFunctionSummaries
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DFGFunctionSummariesTest {

    @Test
    fun testParsingFile() {
        val jsonSummaries =
            DFGFunctionSummaries.fromFiles(listOf(File("src/test/resources/function-dfg.json")))

        assertTrue(jsonSummaries.functionToDFGEntryMap.isNotEmpty())
        val yamlSummaries =
            DFGFunctionSummaries.fromFiles(listOf(File("src/test/resources/function-dfg.yml")))

        assertTrue(yamlSummaries.functionToDFGEntryMap.isNotEmpty())

        assertEquals(jsonSummaries.functionToDFGEntryMap, yamlSummaries.functionToDFGEntryMap)
    }

    @Test
    fun testPropagateArguments() {
        val dfgTest = getDfgInferredCall()
        assertNotNull(dfgTest)

        val main = dfgTest.functions["main"]
        assertNotNull(main)

        val memcpy = dfgTest.functions["memcpy"]
        assertNotNull(memcpy)
        val param0 = memcpy.parameters[0]
        val param1 = memcpy.parameters[1]

        val call = main.calls["memcpy"]
        assertNotNull(call)

        val argA = call.arguments[0]
        assertNotNull(argA)

        assertEquals(1, argA.nextDFG.size)
        assertEquals(2, argA.prevDFG.size)
        /*
        The flows should be as follows:
        VariableDeclaration["a"] -> Reference["a" (argument of call)] -CallingContextIn-> ParameterDeclaration -CallingContextOut-> Reference["a" (return)]
         */

        val nextDfg = argA.nextDFGEdges.single()
        assertEquals(
            call,
            ((nextDfg as? ContextsensitiveDataflow)?.callingContext as? CallingContextIn)
                ?.callExpression
        )
        assertEquals(param0, nextDfg.end)

        val prevDfgThroughFunction =
            argA.prevDFGEdges.singleOrNull {
                ((it as? ContextsensitiveDataflow)?.callingContext as? CallingContextOut)
                    ?.callExpression != call
            }
        assertNotNull(prevDfgThroughFunction)
        assertEquals(param0, prevDfgThroughFunction.start)

        val variableDeclA = main.variables["a"]
        assertNotNull(variableDeclA)

        val prevDfgNotThroughFunction =
            argA.prevDFGEdges.singleOrNull {
                ((it as? ContextsensitiveDataflow)?.callingContext as? CallingContextOut)
                    ?.callExpression != call
            }
        assertNotNull(prevDfgNotThroughFunction)
        assertEquals(variableDeclA, prevDfgNotThroughFunction.start)

        assertEquals(setOf<Node>(argA, param1), param0.prevDFG)
    }

    companion object {
        fun getDfgInferredCall(): TranslationResult {
            val config =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage(TestLanguage("."))
                    .registerFunctionSummary(File("src/test/resources/function-dfg.yml"))
                    .inferenceConfiguration(
                        InferenceConfiguration.builder()
                            .inferDfgForUnresolvedCalls(true)
                            .inferFunctions(true)
                            .build()
                    )
                    .build()
            return GraphExamples.testFrontend(config).build {
                translationResult {
                    translationUnit("DfgInferredCall.c") {
                        function("main", t("int")) {
                            body {
                                declare {
                                    variable("a", t("char").pointer()) { literal(7, t("char")) }
                                }

                                declare {
                                    variable("b", t("char").pointer()) { literal(5, t("char")) }
                                }

                                call("memcpy") {
                                    ref("a")
                                    ref("b")
                                    literal(1, t("int"))
                                }

                                returnStmt { ref("a") }
                            }
                        }
                    }
                }
            }
        }
    }
}
