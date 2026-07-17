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

import de.fraunhofer.aisec.cpg.InferenceConfiguration
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.TestLanguageWithColon
import de.fraunhofer.aisec.cpg.frontends.testFrontend
import de.fraunhofer.aisec.cpg.frontends.translationResult
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.edges.flows.CallingContextIn
import de.fraunhofer.aisec.cpg.graph.edges.flows.CallingContextOut
import de.fraunhofer.aisec.cpg.graph.edges.flows.ContextSensitiveDataflow
import de.fraunhofer.aisec.cpg.graph.expressions.ParameterMemoryValue
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.expressions.Return
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.types.FunctionType.Companion.computeType
import de.fraunhofer.aisec.cpg.graph.types.recordDeclaration
import de.fraunhofer.aisec.cpg.passes.*
import de.fraunhofer.aisec.cpg.passes.inference.DFGFunctionSummaries
import de.fraunhofer.aisec.cpg.passes.inference.startInference
import java.io.File
import kotlin.test.Ignore
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
            testFrontend(
                    TranslationConfiguration.builder()
                        .defaultPasses()
                        .registerLanguage<TestLanguage>()
                        .registerFunctionSummaries(File("src/test/resources/function-dfg2.yml"))
                        .inferenceConfiguration(
                            InferenceConfiguration.builder()
                                .inferDfgForUnresolvedCalls(true)
                                .inferFunctions(true)
                                .build()
                        )
                        .build()
                )
                .build {
                    val tu = newTranslationUnit("DfgInferredCall.c")
                    scopeManager.resetToGlobal(tu)

                    newNamespace("test", holder = tu, enterScope = true) { ns ->
                        // We need three types with a type hierarchy.
                        val objType = objectType("test.Object")
                        val listType = objectType("test.List")
                        var recordDecl = ns.startInference(ctx)?.inferRecordDeclaration(listType)
                        listType.recordDeclaration = recordDecl
                        recordDecl?.addSuperClass(objType)
                        listType.superTypes.add(objType)

                        val specialListType = objectType("test.SpecialList")
                        recordDecl = ns.startInference(ctx)?.inferRecordDeclaration(specialListType)
                        specialListType.recordDeclaration = recordDecl
                        recordDecl?.addSuperClass(listType)
                        specialListType.superTypes.add(listType)

                        val verySpecialListType = objectType("test.VerySpecialList")
                        recordDecl =
                            ns.startInference(ctx)?.inferRecordDeclaration(verySpecialListType)
                        verySpecialListType.recordDeclaration = recordDecl
                        recordDecl?.addSuperClass(specialListType)
                        verySpecialListType.superTypes.add(listType)
                    }

                    newFunction("main", holder = tu, enterScope = true) { func ->
                        func.returnTypes = listOf(objectType("int"))
                        func.type = computeType(func)

                        func.body =
                            newBlock(enterScope = true) { block ->
                                // memberCall("addAll", construct("test.VerySpecialList")) { ... }
                                // -- the base `construct(...)` is evaluated as a plain argument
                                // while `block` is the ambient holder, so it self-attaches to the
                                // block as an orphan statement IN ADDITION to being referenced as
                                // the member call's base. Faithfully reproduced below (and for all
                                // other `memberCall("addAll", construct(...))` occurrences).
                                val verySpecialListConstruction =
                                    newConstruction("test.VerySpecialList") {
                                        it.type = objectType("test.VerySpecialList")
                                    }
                                block.statements += verySpecialListConstruction
                                val addAll1 =
                                    newMemberCall(
                                        newMemberAccess("addAll", verySpecialListConstruction)
                                    )
                                addAll1.arguments += newLiteral(1, objectType("int"))
                                addAll1.arguments +=
                                    newConstruction("test.Object") {
                                        it.type = objectType("test.Object")
                                    }
                                block.statements += addAll1

                                val specialListConstruction1 =
                                    newConstruction("test.SpecialList") {
                                        it.type = objectType("test.SpecialList")
                                    }
                                block.statements += specialListConstruction1
                                val addAll2 =
                                    newMemberCall(
                                        newMemberAccess("addAll", specialListConstruction1)
                                    )
                                addAll2.arguments += newLiteral(1, objectType("int"))
                                addAll2.arguments +=
                                    newConstruction("test.List") {
                                        it.type = objectType("test.List")
                                    }
                                block.statements += addAll2

                                val specialListConstruction2 =
                                    newConstruction("test.SpecialList") {
                                        it.type = objectType("test.SpecialList")
                                    }
                                block.statements += specialListConstruction2
                                val addAll3 =
                                    newMemberCall(
                                        newMemberAccess("addAll", specialListConstruction2)
                                    )
                                addAll3.arguments += newLiteral(1, objectType("int"))
                                addAll3.arguments +=
                                    newConstruction("test.Object") {
                                        it.type = objectType("test.Object")
                                    }
                                block.statements += addAll3

                                val declStmtA = newDeclarationStatement()
                                newVariable("a", objectType("test.List"), holder = declStmtA) {
                                    it.initializer =
                                        newConstruction("test.List") {
                                            it.type = objectType("test.List")
                                        }
                                }
                                block.statements += declStmtA

                                // memberCall("addAll", ref("a", ...)) { ... } -- unlike
                                // `construct`, `ref` only self-attaches if the ambient holder is an
                                // ArgumentHolder, so no orphan Reference is added here.
                                val addAll4 =
                                    newMemberCall(
                                        newMemberAccess(
                                            "addAll",
                                            newReference("a", objectType("test.List")),
                                        )
                                    )
                                addAll4.arguments += newLiteral(1, objectType("int"))
                                addAll4.arguments +=
                                    newConstruction("test.Object") {
                                        it.type = objectType("test.Object")
                                    }
                                block.statements += addAll4

                                val printCall = newCall(newReference("print"))
                                printCall.arguments += newReference("a", objectType("test.List"))
                                block.statements += printCall

                                val randomTypeConstruction =
                                    newConstruction("random.Type") {
                                        it.type = objectType("random.Type")
                                    }
                                block.statements += randomTypeConstruction
                                val addAll5 =
                                    newMemberCall(newMemberAccess("addAll", randomTypeConstruction))
                                addAll5.arguments += newLiteral(1, objectType("int"))
                                addAll5.arguments +=
                                    newConstruction("test.Object") {
                                        it.type = objectType("test.Object")
                                    }
                                block.statements += addAll5

                                block.statements += newReturn {
                                    it.returnValue = newLiteral(0, objectType("int"))
                                }
                            }
                    }

                    translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
                }

        // Explicitly specified. Easiest case. Base class, directly specified. Overloaded things
        // don't match. Child entries don't match.
        val listAddAllTwoArgs = code.methods["test.List.addAll"]
        assertNotNull(listAddAllTwoArgs)
        assertEquals(2, listAddAllTwoArgs.parameters.size)
        // The param flows to this, and to all uses after
        assertEquals(
            setOf<Node>(
                listAddAllTwoArgs.receiver!!,
                code.calls("print").single().arguments.single(),
            ),
            listAddAllTwoArgs.parameters[1].nextDFG,
        )
        // No flow from param0 or receiver specified => Should be empty and differ from default
        // behavior
        assertEquals(setOf(), listAddAllTwoArgs.parameters[0].nextDFG)
        assertEquals(setOf(), listAddAllTwoArgs.prevDFG)

        // Specified by parent class' method List.addAll(int, Object). Test that parent of base is
        // also taken into account.
        val specialListAddAllTwoArgs =
            code.methods("test.SpecialList.addAll").first {
                it.parameters[1].type.name.lastPartsMatch("test.Object")
            }
        assertNotNull(specialListAddAllTwoArgs)
        assertEquals(2, specialListAddAllTwoArgs.parameters.size)
        assertEquals(
            setOf<Node>(specialListAddAllTwoArgs.receiver!!),
            specialListAddAllTwoArgs.parameters[1].nextDFG,
        )
        // No flow from param0 or receiver specified => Should be empty and differ from default
        // behavior
        assertEquals(setOf(), specialListAddAllTwoArgs.parameters[0].nextDFG)
        assertEquals(setOf(), specialListAddAllTwoArgs.prevDFG)

        // Specified by parent class' method List.addAll(int, List). Tests the most precise
        // signature matching in case of function overloading.
        val specialListAddAllSpecializedArgs =
            code.methods("test.SpecialList.addAll").first {
                it.parameters[1].type.name.lastPartsMatch("test.List")
            }
        assertNotNull(specialListAddAllSpecializedArgs)
        assertEquals(2, specialListAddAllSpecializedArgs.parameters.size)
        // Very weird data flow specified: receiver to param0 and param1 to return.
        assertEquals(
            setOf<Node>(specialListAddAllSpecializedArgs.parameters[0]),
            specialListAddAllSpecializedArgs.receiver?.nextDFG ?: setOf(),
        )
        assertEquals(
            setOf<Node>(specialListAddAllSpecializedArgs),
            specialListAddAllSpecializedArgs.parameters[1].nextDFG,
        )

        // Specified by VerySpecialList.addAll(int, Object), overrides List.addAll(int, Object).
        // Tests that we take the most precise base class. The entry of List.addAll(int, Object) is
        // also applicable but isn't the most precise one (due to the base)
        val verySpecialListAddAllSpecializedArgs = code.methods["test.VerySpecialList.addAll"]
        assertNotNull(verySpecialListAddAllSpecializedArgs)
        assertEquals(2, verySpecialListAddAllSpecializedArgs.parameters.size)
        // Very weird data flow specified: receiver to param0 and param1 to return.
        assertEquals(
            setOf<Node>(verySpecialListAddAllSpecializedArgs.parameters[0]),
            verySpecialListAddAllSpecializedArgs.receiver?.nextDFG ?: setOf(),
        )
        assertEquals(
            setOf<Node>(verySpecialListAddAllSpecializedArgs),
            verySpecialListAddAllSpecializedArgs.parameters[1].nextDFG,
        )

        // Not specified => Default behavior (param0 and param1 and receiver to method
        // declaration).
        val randomTypeAddAllTwoArgs = code.methods["random.Type.addAll"]
        assertNotNull(randomTypeAddAllTwoArgs)
        assertEquals(2, randomTypeAddAllTwoArgs.parameters.size)
        assertEquals(
            setOf<Node>(
                randomTypeAddAllTwoArgs.parameters[1],
                randomTypeAddAllTwoArgs.parameters[0],
                randomTypeAddAllTwoArgs.receiver!!,
            ),
            randomTypeAddAllTwoArgs.prevDFG,
        )
    }

    @Test
    fun testPropagateArguments() {
        val dfgTest = getDfgInferredCall() { defaultPasses() }
        assertNotNull(dfgTest)

        val main = dfgTest.functions["main"]
        assertNotNull(main)

        val memcpy = dfgTest.functions["memcpy"]
        assertNotNull(memcpy)
        val param0 = memcpy.parameters[0]
        val param0PMV =
            param0.fullMemoryValues.filterIsInstance<ParameterMemoryValue>().singleOrNull()
        assertNotNull(param0PMV)
        val param0DerefPMV = param0.memoryValues.singleOrNull { it.name.localName == "derefvalue" }
        assertNotNull(param0DerefPMV)
        val param1 = memcpy.parameters[1]
        val param1DerefPMV = param1.memoryValues.singleOrNull { it.name.localName == "derefvalue" }
        assertNotNull(param1DerefPMV)

        val call = main.calls["memcpy"]
        assertNotNull(call)

        val argA = call.arguments[0]
        assertNotNull(argA)
        /*
        The flows should be as follows:
        Variable["a"] -CallingContextIn-> ParameterMemoryvalue["memcpy.param0.derefvalue"] -CallingContextOut-> Reference["a" (return)]
         */

        assertEquals(1, argA.nextDFG.size)
        // The MemoryAddress a
        assertEquals(1, argA.prevFullDFG.size)
        assertEquals(1, argA.prevDFGEdges.filter { it.derefDepth != null }.size)

        val nextDfg = argA.nextDFGEdges.single()
        assertEquals(
            mutableListOf(call),
            ((nextDfg as? ContextSensitiveDataflow)?.callingContext as? CallingContextIn)?.calls,
        )
        assertEquals(param0, nextDfg.end)

        val variableA = main.variables["a"]
        assertNotNull(variableA)
        assertNotNull(variableA.memoryAddresses.firstOrNull())
        assertEquals(
            mutableSetOf<Node>(variableA.memoryAddresses.first()),
            argA.prevFullDFG.toMutableSet(),
        )

        val prevDfgOfParam0Deref =
            param0DerefPMV.prevDFGEdges.singleOrNull { it !is ContextSensitiveDataflow }
        assertNotNull(prevDfgOfParam0Deref)
        assertEquals(param1DerefPMV, prevDfgOfParam0Deref.start)

        val returnA = main.allChildren<Return>().singleOrNull()?.returnValue as? Reference
        assertNotNull(returnA)

        val literal5 = main.literals.first { it.value?.equals(5) == true }
        assertNotNull(literal5)

        // Check that also the CallingContext property (in and out) is set correctly
        val valA = dfgTest.variables["a"]
        assertNotNull(valA)
        val memcpySrcDerefPMV =
            dfgTest.functions("memcpy").single().parameters.first().memoryValues.singleOrNull {
                it.name.localName == "derefvalue"
            }
        assertNotNull(memcpySrcDerefPMV)

        val nextDfgOfValA =
            valA.nextDFGEdges?.singleOrNull {
                ((it as? ContextSensitiveDataflow)?.callingContext as? CallingContextIn)?.calls ==
                    listOf(call)
            }
        assertEquals(memcpySrcDerefPMV, nextDfgOfValA?.end)

        val nextDFGOfPMV =
            memcpySrcDerefPMV.nextDFGEdges.singleOrNull() {
                ((it as? ContextSensitiveDataflow)?.callingContext as? CallingContextOut)?.calls ==
                    listOf(call)
            }
        val returnedA = dfgTest.returns.single().returnValues.single()
        assertEquals(returnedA, nextDFGOfPMV?.end)
    }

    @Test
    fun testLanguageHierarchyMatching() {
        val config =
            TranslationConfiguration.builder()
                .registerLanguage<TestLanguageWithColon>()
                .registerFunctionSummaries(File("src/test/resources/function-dfg.yml"))
                .inferenceConfiguration(
                    InferenceConfiguration.builder()
                        .inferDfgForUnresolvedCalls(true)
                        .inferFunctions(true)
                        .build()
                )
                .defaultPasses()
                .build()

        val dfgTest =
            testFrontend(config).build {
                val tu = newTranslationUnit("DfgInferredCall.c")
                scopeManager.resetToGlobal(tu)

                newFunction("main", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(objectType("int"))
                    func.type = computeType(func)

                    func.body =
                        newBlock(enterScope = true) { block ->
                            val declStmtA = newDeclarationStatement()
                            newVariable("a", objectType("int"), holder = declStmtA) {
                                it.initializer = newLiteral(7, objectType("char"))
                            }
                            block.statements += declStmtA

                            val declStmtB = newDeclarationStatement()
                            newVariable("b", objectType("int"), holder = declStmtB) {
                                it.initializer = newLiteral(5, objectType("char"))
                            }
                            block.statements += declStmtB

                            val memcpyCall = newCall(newReference("memcpy"))
                            memcpyCall.arguments +=
                                newPointerReference("a").also { it.input = newReference("a") }
                            memcpyCall.arguments +=
                                newPointerReference("b").also { it.input = newReference("b") }
                            memcpyCall.arguments += newLiteral(1, objectType("int"))
                            block.statements += memcpyCall

                            block.statements += newReturn { it.returnValue = newReference("a") }
                        }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }

        val memcpy = dfgTest.functions["memcpy"]
        assertNotNull(memcpy)
        assertTrue(
            memcpy.functionSummary.isNotEmpty(),
            "Expected memcpy to have a non-empty function summary",
        )
    }

    @Ignore(
        "We keep this ignored for now as the DFGPass does not draw these edges anymore, this should be done by the PointsToPass. In the future, the DFGPass could still draw the edges and the PtP could remove them again to have a more lightweight but less precise version of the DFG"
    )
    @Test
    fun testPropagateArgumentsControlFlowInsensitive() {
        // We don't use the ControlFlowSensitiveDFGPass here to check the method
        // DFGPass.connectInferredCallArguments
        val dfgTest = getDfgInferredCall {
            this.registerPass<TypeHierarchyResolver>()
            registerPass<SymbolResolver>()
            registerPass<DFGPass>()
            registerPass<DynamicInvokeResolver>()
            registerPass<EvaluationOrderGraphPass>()
            registerPass<TypeResolver>()
        }
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
        Variable["a"] -> { Reference["a" (argument of call)], Reference["a" (return)] }
        Reference["a" (argument of call)] -CallingContextIn-> Parameter -CallingContextOut-> Reference["a" (argument of call)] -> Variable["a"]
         */

        assertEquals(2, argA.nextDFG.size)
        assertEquals(2, argA.prevDFG.size)

        val nextDfg =
            argA.nextDFGEdges.singleOrNull {
                ((it as? ContextSensitiveDataflow)?.callingContext as? CallingContextIn)?.calls ==
                    setOf(call)
            }
        assertNotNull(nextDfg)
        assertEquals(param0, nextDfg.end)

        val variableA = main.variables["a"]
        assertNotNull(variableA)
        assertEquals(mutableSetOf<Node>(variableA, param0), argA.prevDFG)

        val prevDfgOfParam0 = param0.prevDFGEdges.singleOrNull { it !is ContextSensitiveDataflow }
        assertNotNull(prevDfgOfParam0)
        assertEquals(param1, prevDfgOfParam0.start)

        val returnA = main.allChildren<Return>().singleOrNull()?.returnValue as? Reference
        assertNotNull(returnA)

        assertEquals(mutableSetOf<Node>(argA), param0.nextDFG)

        assertEquals(mutableSetOf<Node>(returnA, argA), variableA.nextDFG)

        // Check that also the CallingContext property is set correctly
        val nextDfgOfParam0 =
            param0.nextDFGEdges.singleOrNull {
                ((it as? ContextSensitiveDataflow)?.callingContext as? CallingContextOut)?.calls ==
                    setOf(call)
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
                    .registerLanguage<TestLanguage>()
                    .registerFunctionSummaries(File("src/test/resources/function-dfg.yml"))
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
              int a = 7;
              int b = 5;
              memcpy(&a, &b, 1);
              return a;
            }
             */
            return testFrontend(config).build {
                val tu = newTranslationUnit("DfgInferredCall.c")
                scopeManager.resetToGlobal(tu)

                newFunction("main", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(objectType("int"))
                    func.type = computeType(func)

                    func.body =
                        newBlock(enterScope = true) { block ->
                            val declStmtA = newDeclarationStatement()
                            newVariable("a", objectType("int"), holder = declStmtA) {
                                it.initializer = newLiteral(7, objectType("char"))
                            }
                            block.statements += declStmtA

                            val declStmtB = newDeclarationStatement()
                            newVariable("b", objectType("int"), holder = declStmtB) {
                                it.initializer = newLiteral(5, objectType("char"))
                            }
                            block.statements += declStmtB

                            val memcpyCall = newCall(newReference("memcpy"))
                            memcpyCall.arguments +=
                                newPointerReference("a").also { it.input = newReference("a") }
                            memcpyCall.arguments +=
                                newPointerReference("b").also { it.input = newReference("b") }
                            memcpyCall.arguments += newLiteral(1, objectType("int"))
                            block.statements += memcpyCall

                            block.statements += newReturn { it.returnValue = newReference("a") }
                        }
                }

                translationResult { components.firstOrNull()?.translationUnits?.add(tu) }
            }
        }
    }
}
