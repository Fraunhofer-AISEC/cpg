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
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.types.recordDeclaration
import de.fraunhofer.aisec.cpg.passes.ControlFlowSensitiveDFGPass
import de.fraunhofer.aisec.cpg.passes.inference.DFGFunctionSummaries
import de.fraunhofer.aisec.cpg.passes.inference.startInference
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
    fun testMatching() {
        val code =
            GraphExamples.testFrontend(
                    TranslationConfiguration.builder()
                        .defaultPasses()
                        .registerLanguage(TestLanguage("."))
                        .registerFunctionSummary(File("src/test/resources/function-dfg2.yml"))
                        .inferenceConfiguration(
                            InferenceConfiguration.builder()
                                .inferDfgForUnresolvedCalls(true)
                                .inferFunctions(true)
                                .build()
                        )
                        .build()
                )
                .build {
                    translationResult {
                        translationUnit("DfgInferredCall.c") {
                            function("main", t("int")) {
                                body {
                                    // We need three types with a type hierarchy.
                                    val objectType = t("test.Object")
                                    val listType = t("test.List")
                                    ctx?.let {
                                        val recordDecl =
                                            listType
                                                .startInference(it)
                                                ?.inferRecordDeclaration(
                                                    listType,
                                                    this@translationUnit
                                                )
                                        listType.recordDeclaration = recordDecl
                                        recordDecl?.addSuperClass(objectType)
                                        listType.superTypes.add(objectType)
                                    }

                                    val specialListType = t("test.SpecialList")
                                    ctx?.let {
                                        val recordDecl =
                                            specialListType
                                                .startInference(it)
                                                ?.inferRecordDeclaration(
                                                    specialListType,
                                                    this@translationUnit
                                                )
                                        specialListType.recordDeclaration = recordDecl
                                        recordDecl?.addSuperClass(listType)
                                        specialListType.superTypes.add(listType)
                                    }

                                    val verySpecialListType = t("test.VerySpecialList")
                                    ctx?.let {
                                        val recordDecl =
                                            specialListType
                                                .startInference(it)
                                                ?.inferRecordDeclaration(
                                                    specialListType,
                                                    this@translationUnit
                                                )
                                        specialListType.recordDeclaration = recordDecl
                                        recordDecl?.addSuperClass(listType)
                                        specialListType.superTypes.add(listType)
                                    }

                                    memberCall("addAll", construct("test.VerySpecialList")) {
                                        literal(1, t("int"))
                                        construct("test.Object")
                                    }

                                    memberCall("addAll", construct("test.SpecialList")) {
                                        literal(1, t("int"))
                                        construct("test.List")
                                    }

                                    memberCall("addAll", construct("test.SpecialList")) {
                                        literal(1, t("int"))
                                        construct("test.Object")
                                    }

                                    memberCall("addAll", construct("test.List")) {
                                        literal(1, t("int"))
                                        construct("test.Object")
                                    }

                                    memberCall("addAll", construct("random.Type")) {
                                        literal(1, t("int"))
                                        construct("test.Object")
                                    }

                                    returnStmt { literal(0, t("int")) }
                                }
                            }
                        }
                    }
                }

        // Explicitly specified
        val listAddAllTwoArgs = code.methods["test.List.addAll"]
        assertNotNull(listAddAllTwoArgs)
        assertEquals(2, listAddAllTwoArgs.parameters.size)
        assertEquals(
            setOf<Node>(listAddAllTwoArgs.receiver!!),
            listAddAllTwoArgs.parameters[1].nextDFG
        )
        // No flow from param0 or receiver specified => Should be empty and differ from default
        // behavior
        assertEquals(setOf(), listAddAllTwoArgs.parameters[0].nextDFG)
        assertEquals(setOf(), listAddAllTwoArgs.prevDFG)

        // Specified by parent class' method List.addAll(int, Object)
        val specialListAddAllTwoArgs =
            code.methods("test.SpecialList.addAll").first {
                it.parameters[1].type.name.lastPartsMatch("test.Object")
            }
        assertNotNull(specialListAddAllTwoArgs)
        assertEquals(2, specialListAddAllTwoArgs.parameters.size)
        assertEquals(
            setOf<Node>(specialListAddAllTwoArgs.receiver!!),
            specialListAddAllTwoArgs.parameters[1].nextDFG
        )
        // No flow from param0 or receiver specified => Should be empty and differ from default
        // behavior
        assertEquals(setOf(), specialListAddAllTwoArgs.parameters[0].nextDFG)
        assertEquals(setOf(), specialListAddAllTwoArgs.prevDFG)

        // Specified by parent class' method List.addAll(int, List)
        val specialListAddAllSpecializedArgs =
            code.methods("test.SpecialList.addAll").first {
                it.parameters[1].type.name.lastPartsMatch("test.List")
            }
        assertNotNull(specialListAddAllSpecializedArgs)
        assertEquals(2, specialListAddAllSpecializedArgs.parameters.size)
        // Very weird data flow specified: receiver to param0 and param1 to return.
        assertEquals(
            setOf<Node>(specialListAddAllSpecializedArgs.parameters[0]),
            specialListAddAllSpecializedArgs.receiver?.nextDFG ?: setOf()
        )
        assertEquals(
            setOf<Node>(specialListAddAllSpecializedArgs),
            specialListAddAllSpecializedArgs.parameters[1].nextDFG
        )

        // Specified by parent class' method List.addAll(int, List)
        val verySpecialListAddAllSpecializedArgs = code.methods["test.VerySpecialList.addAll"]
        assertNotNull(verySpecialListAddAllSpecializedArgs)
        assertEquals(2, verySpecialListAddAllSpecializedArgs.parameters.size)
        // Very weird data flow specified: receiver to param0 and param1 to return.
        assertEquals(
            setOf<Node>(verySpecialListAddAllSpecializedArgs.parameters[0]),
            verySpecialListAddAllSpecializedArgs.receiver?.nextDFG ?: setOf()
        )
        assertEquals(
            setOf<Node>(verySpecialListAddAllSpecializedArgs),
            verySpecialListAddAllSpecializedArgs.parameters[1].nextDFG
        )

        // Not specified => Default behavior (param0 and param1 and receiver to method declaration).
        val randomTypeAddAllTwoArgs = code.methods["random.Type.addAll"]
        assertNotNull(randomTypeAddAllTwoArgs)
        assertEquals(2, randomTypeAddAllTwoArgs.parameters.size)
        assertEquals(
            setOf<Node>(
                randomTypeAddAllTwoArgs.parameters[1],
                randomTypeAddAllTwoArgs.parameters[0],
                randomTypeAddAllTwoArgs.receiver!!
            ),
            randomTypeAddAllTwoArgs.prevDFG
        )
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
        /*
        The flows should be as follows:
        VariableDeclaration["a"] -> Reference["a" (argument of call)] -CallingContextIn-> ParameterDeclaration -CallingContextOut-> Reference["a" (return)]
         */

        assertEquals(1, argA.nextDFG.size)
        assertEquals(1, argA.prevDFG.size)

        val nextDfg = argA.nextDFGEdges.single()
        assertEquals(
            call,
            ((nextDfg as? ContextsensitiveDataflow)?.callingContext as? CallingContextIn)
                ?.callExpression
        )
        assertEquals(param0, nextDfg.end)

        val variableA = main.variables["a"]
        assertNotNull(variableA)
        assertEquals(mutableSetOf<Node>(variableA), argA.prevDFG)

        val prevDfgOfParam0 = param0.prevDFGEdges.singleOrNull { it !is ContextsensitiveDataflow }
        assertNotNull(prevDfgOfParam0)
        assertEquals(param1, prevDfgOfParam0.start)

        val returnA = main.allChildren<ReturnStatement>().singleOrNull()?.returnValue as? Reference
        assertNotNull(returnA)

        assertEquals(mutableSetOf<Node>(returnA), param0.nextDFG)

        // Check that also the CallingContext property is set correctly
        val nextDfgOfParam0 =
            param0.nextDFGEdges.singleOrNull {
                ((it as? ContextsensitiveDataflow)?.callingContext as? CallingContextOut)
                    ?.callExpression == call
            }
        assertEquals(returnA, nextDfgOfParam0?.end)
    }

    @Test
    fun testPropagateArgumentsControlFlowInsensitive() {
        val dfgTest = getDfgInferredCall { unregisterPass<ControlFlowSensitiveDFGPass>() }
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
        /*
        The flows should be as follows:
        VariableDeclaration["a"] -> { Reference["a" (argument of call)], Reference["a" (return)] }
        Reference["a" (argument of call)] -CallingContextIn-> ParameterDeclaration -CallingContextOut-> Reference["a" (argument of call)] -> VariableDeclaration["a"]
         */

        assertEquals(2, argA.nextDFG.size)
        assertEquals(2, argA.prevDFG.size)

        val nextDfg =
            argA.nextDFGEdges.singleOrNull {
                ((it as? ContextsensitiveDataflow)?.callingContext as? CallingContextIn)
                    ?.callExpression == call
            }
        assertNotNull(nextDfg)
        assertEquals(param0, nextDfg.end)

        val variableA = main.variables["a"]
        assertNotNull(variableA)
        assertEquals(mutableSetOf<Node>(variableA, param0), argA.prevDFG)

        val prevDfgOfParam0 = param0.prevDFGEdges.singleOrNull { it !is ContextsensitiveDataflow }
        assertNotNull(prevDfgOfParam0)
        assertEquals(param1, prevDfgOfParam0.start)

        val returnA = main.allChildren<ReturnStatement>().singleOrNull()?.returnValue as? Reference
        assertNotNull(returnA)

        assertEquals(mutableSetOf<Node>(argA), param0.nextDFG)

        assertEquals(mutableSetOf<Node>(returnA, argA), variableA.nextDFG)

        // Check that also the CallingContext property is set correctly
        val nextDfgOfParam0 =
            param0.nextDFGEdges.singleOrNull {
                ((it as? ContextsensitiveDataflow)?.callingContext as? CallingContextOut)
                    ?.callExpression == call
            }
        assertEquals(argA, nextDfgOfParam0?.end)
    }

    companion object {
        fun getDfgInferredCall(
            customConfig: TranslationConfiguration.Builder.() -> TranslationConfiguration.Builder =
                {
                    this
                }
        ): TranslationResult {
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
                    .customConfig()
                    .build()
            /*
            int main() {
              char *a = 7;
              char *b = 5;
              memcpy(a, b, 1);
              return a;
            }
             */
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
