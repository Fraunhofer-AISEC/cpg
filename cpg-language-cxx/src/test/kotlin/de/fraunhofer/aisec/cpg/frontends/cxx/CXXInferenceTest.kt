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

import de.fraunhofer.aisec.cpg.InferenceConfiguration
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.CastNotPossible
import de.fraunhofer.aisec.cpg.frontends.ImplicitCast
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Method
import de.fraunhofer.aisec.cpg.graph.scopes.GlobalScope
import de.fraunhofer.aisec.cpg.graph.types.BooleanType
import de.fraunhofer.aisec.cpg.passes.DFGPass
import de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import de.fraunhofer.aisec.cpg.passes.TypeHierarchyResolver
import de.fraunhofer.aisec.cpg.passes.TypeResolver
import de.fraunhofer.aisec.cpg.test.*
import java.io.File
import kotlin.test.*

class CXXInferenceTest {
    /**
     * Several calls to the same unknown function must all end up at a single inferred declaration,
     * no matter whether the callee is resolvable far enough to even produce a candidate.
     *
     * For an unqualified callee this follows from resolution alone: the first call infers the
     * function and the later ones simply resolve to it. For a callee behind a qualifier that does
     * not exist anywhere (`missing::ns::qualified_fn`), resolution fails before producing any
     * candidate, while inference places the function in the global scope under its unqualified
     * local name - where the next lookup of the qualified name will never find it. Without an
     * explicit guard, every call site therefore inferred a declaration of its own.
     */
    @Test
    fun testRepeatedInferenceOfSameFunction() {
        val file = File("src/test/resources/cxx/inference/repeated_inference.cpp")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
                it.loadIncludes(false)
                it.addIncludesToGraph(false)
            }

        val unqualified = result.functions("plain_global")
        assertEquals(
            1,
            unqualified.size,
            "The three calls to plain_global must share one inferred declaration",
        )
        assertTrue(unqualified.single().isInferred)

        val qualified = result.functions("qualified_fn")
        assertEquals(
            1,
            qualified.size,
            "The three calls to missing::ns::qualified_fn must share one inferred declaration",
        )
        assertTrue(qualified.single().isInferred)

        // Every call must actually point at that single declaration.
        val calls = result.calls { it.name.localName == "qualified_fn" }
        assertEquals(3, calls.size)
        calls.forEach { assertEquals(listOf(qualified.single()), it.invokes) }
    }

    @Test
    fun testGlobals() {
        val file = File("src/test/resources/cxx/inference/inference.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
                it.loadIncludes(false)
                it.addIncludesToGraph(false)
            }
        assertNotNull(tu)

        val global = tu.variables["somethingGlobal"]
        assertNotNull(global)

        assertContains(tu.declarations, global)
    }

    @Test
    fun testInferClassInNamespace() {
        val file = File("src/test/resources/cxx/inference/inference.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
                it.loadIncludes(false)
                it.addIncludesToGraph(false)
            }
        assertNotNull(tu)

        val util = tu.namespaces["util"]
        assertNotNull(util)

        val someClass = util.records["SomeClass"]
        assertNotNull(someClass)
    }

    @Test
    fun testTrickyInference() {
        val file = File("src/test/resources/cxx/inference/tricky_inference.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
                it.loadIncludes(false)
                it.addIncludesToGraph(false)
                it.inferenceConfiguration(
                    InferenceConfiguration.builder().inferReturnTypes(true).build()
                )
            }
        assertNotNull(tu)

        val some = tu.namespaces["some"]
        assertNotNull(some)
        assertTrue(some.isInferred)

        val json = some.records["json"]
        assertNotNull(json)
        assertTrue(json.isInferred)

        val begin = json.methods["begin"]
        assertNotNull(begin)
        assertTrue(begin.isInferred)
        assertLocalName("iterator*", begin.returnTypes.singleOrNull())

        val end = json.methods["end"]
        assertNotNull(end)
        assertTrue(end.isInferred)
        assertLocalName("iterator*", end.returnTypes.singleOrNull())

        val size = json.methods["size"]
        assertNotNull(size)
        assertTrue(size.isInferred)
        assertLocalName("int", size.returnTypes.singleOrNull())

        val iterator = json.records["iterator"]
        assertNotNull(iterator)
        assertTrue(iterator.isInferred)

        val next = iterator.methods["next"]
        assertNotNull(next)
        assertTrue(next.isInferred)
        assertLocalName("iterator*", next.returnTypes.singleOrNull())

        val isValid = iterator.methods["isValid"]
        assertNotNull(isValid)
        assertTrue(isValid.isInferred)
        assertIs<BooleanType>(isValid.returnTypes.singleOrNull())

        val log = tu.functions["log"]
        assertNotNull(log)
        assertIsNot<Method>(log)
        assertIs<GlobalScope>(log.scope)
    }

    @Test
    fun testSuperClass() {
        val file = File("src/test/resources/cxx/inference/superclass.cpp")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
                it.loadIncludes(false)
                it.addIncludesToGraph(false)
                it.inferenceConfiguration(
                    InferenceConfiguration.builder().inferReturnTypes(true).build()
                )
            }
        assertNotNull(result)

        val a = result.records["A"]
        assertNotNull(a)
        assertTrue(a.isInferred)

        val n = result.namespaces["N"]
        assertNotNull(n)
        assertTrue(n.isInferred)

        val b = n.records["N::B"]
        assertNotNull(b)
        assertTrue(b.isInferred)

        val m = result.namespaces["M"]
        assertNotNull(m)
        assertTrue(m.isInferred)

        val c = m.namespaces["M::C"]
        assertNotNull(c)
        assertTrue(c.isInferred)

        val d = c.records["M::C::D"]
        assertNotNull(d)
        assertTrue(d.isInferred)

        val e = result.records["E"]
        assertNotNull(e)
        assertTrue(e.isInferred)
    }

    @Test
    fun testConstruct() {
        val file = File("src/test/resources/cxx/inference/construct.cpp")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
                it.loadIncludes(false)
                it.addIncludesToGraph(false)
                it.inferenceConfiguration(
                    InferenceConfiguration.builder().inferReturnTypes(true).build()
                )
            }
        assertNotNull(result)
        with(result) {
            val pairType = assertResolvedType("Pair")
            assertNotNull(pairType)

            val pair = result.functions["Pair"]
            assertNotNull(pair)
            assertTrue(pair.isInferred)
            assertEquals(pairType, pair.returnTypes.singleOrNull())
        }
    }

    @Test
    fun testInferParentClassInNamespace() {
        val file = File("src/test/resources/cxx/inference/parent_inference.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
                it.loadIncludes(false)
                it.addIncludesToGraph(false)
            }
        assertNotNull(tu)

        val util = tu.namespaces["ABC"]
        assertNotNull(util)

        val recordABCA = util.records["A"]
        assertNotNull(recordABCA)
        assertTrue(recordABCA.isInferred)

        val recordA = tu.records["A"]
        assertNotNull(recordA)
        val funcFoo = recordA.functions["foo"]
        assertNotNull(funcFoo)
        assertTrue(funcFoo.isInferred)
        val funcBar = recordA.functions["bar"]
        assertNotNull(funcBar)
        assertFalse(funcBar.isInferred)
    }

    @Test
    fun testVoidPointerArgumentResolvesInC() {
        val file = File("src/test/resources/c/void_pointer.c")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
            }
        assertNotNull(tu)

        val calls = tu.calls("take_pointer") + tu.calls("take_pointer_pointer")
        assertEquals(4, calls.size)
        for (call in calls) {
            assertTrue(call.invokes.isNotEmpty(), "${call.code} is not resolved")
            assertTrue(
                call.invokes.none { it.isInferred },
                "${call.code} invokes an inferred function",
            )
        }
    }

    @Test
    fun testVoidPointerArgumentDoesNotResolveInCpp() {
        val file = File("src/test/resources/cxx/inference/void_pointer.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        assertNotNull(tu)

        val call = tu.calls["take_pointer"]
        assertNotNull(call)
        assertTrue(call.invokes.isNotEmpty())
        assertTrue(call.invokes.all { it.isInferred })
    }

    @Test
    fun testNullPointerConstantResolvesInC() {
        val file = File("src/test/resources/c/null_pointer_constant.c")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
            }
        assertNotNull(tu)

        val calls = tu.calls("take_pointer") + tu.calls("take_other_pointer")
        assertEquals(6, calls.size)
        for (call in calls) {
            // In C, the name alone determines the function, so every call resolves
            val target = call.invokes.singleOrNull()
            assertNotNull(target, "${call.code} is not resolved to one function")
            assertFalse(target.isInferred, "${call.code} invokes an inferred function")

            // Only the null pointer constants can be converted to the pointer parameter
            val argument = call.arguments.single()
            val parameter = target.parameters.single()
            val cast = call.language.tryCast(argument.type, parameter.type, argument, parameter)
            val expected =
                if (call.name.localName == "take_pointer") ImplicitCast else CastNotPossible
            assertEquals(expected, cast, call.code)
        }
    }

    @Test
    fun testNullPointerConstantResolvesInCpp() {
        val file = File("src/test/resources/cxx/inference/null_pointer_constant.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        assertNotNull(tu)

        val takePointer = tu.calls["take_pointer"]
        assertNotNull(takePointer)
        assertTrue(takePointer.invokes.isNotEmpty())
        assertTrue(takePointer.invokes.none { it.isInferred })

        val overloaded = tu.calls["overloaded"]
        assertNotNull(overloaded)
        val target = overloaded.invokes.singleOrNull()
        assertNotNull(target)
        assertFalse(target.isInferred)
        assertLocalName("int", target.parameters.singleOrNull()?.type)
    }

    @Test
    fun testUnknownArgumentTypeResolvesInC() {
        val file = File("src/test/resources/c/unknown_argument_type.c")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
            }
        assertNotNull(tu)

        // C has no overloading, so the only function with that name is the target, even if we
        // cannot determine the type of an argument
        for (name in listOf("grow", "forward_declared")) {
            val calls = tu.calls(name)
            assertTrue(calls.isNotEmpty())
            for (call in calls) {
                assertTrue(call.invokes.isNotEmpty(), "${call.code} is not resolved")
                assertTrue(
                    call.invokes.none { it.isInferred },
                    "${call.code} invokes an inferred function",
                )
            }
        }
        // undeclared_fn is declared nowhere, so it is the only function we need to infer
        assertEquals(
            listOf("undeclared_fn"),
            tu.functions.filter { it.isInferred }.map { it.name.localName },
        )
    }

    @Test
    fun testInferredRecordParameterTypeResolvesInC() {
        val file = File("src/test/resources/c/unknown_parameter_type.c")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
            }
        assertNotNull(tu)

        val mystery = tu.records["mystery_t"]
        assertNotNull(mystery)
        assertTrue(mystery.isInferred)

        // We only guessed that mystery_t is a struct, so it must not make the real function
        // incompatible, neither directly nor behind a pointer
        for (name in listOf("take", "take_pointer")) {
            val calls = tu.calls(name)
            assertEquals(2, calls.size)
            for (call in calls) {
                assertTrue(call.invokes.isNotEmpty(), "${call.code} is not resolved")
                assertTrue(
                    call.invokes.none { it.isInferred },
                    "${call.code} invokes an inferred function",
                )
            }
        }

        // A declared record is known, so the argument is incompatible. But in C, the name alone
        // determines the function.
        val takeKnown = tu.calls["take_known"]
        assertNotNull(takeKnown)
        assertTrue(takeKnown.invokes.isNotEmpty())
        assertTrue(takeKnown.invokes.none { it.isInferred })
    }

    @Test
    fun testArgumentCountDoesNotChooseTheFunctionInC() {
        val file = File("src/test/resources/c/argument_count.c")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
            }
        assertNotNull(tu)

        for (name in listOf("old_style", "two")) {
            val call = tu.calls[name]
            assertNotNull(call, name)
            assertTrue(call.invokes.isNotEmpty(), "${call.code} is not resolved")
            assertTrue(
                call.invokes.none { it.isInferred },
                "${call.code} invokes an inferred function",
            )
        }
        assertTrue(tu.functions.none { it.isInferred })
    }

    @Test
    fun testFewerArgumentsThanSummarizedParametersInC() {
        val file = File("src/test/resources/c/fewer_arguments.c")
        val summaries = File("src/test/resources/c/fewer_arguments.yml")
        // The summary of a parameter without an argument is applied by the PointsToPass, by the
        // ControlFlowSensitiveDFGPass (without the PointsToPass), and by the DFGPass alone
        val configurations =
            mapOf<String, (TranslationConfiguration.Builder) -> Unit>(
                "PointsToPass" to { it.defaultPasses() },
                "ControlFlowSensitiveDFGPass" to { it.disablePointsToAnalysis().defaultPasses() },
                "DFGPass" to
                    {
                        it.registerPass<TypeHierarchyResolver>()
                        it.registerPass<SymbolResolver>()
                        it.registerPass<DFGPass>()
                        it.registerPass<EvaluationOrderGraphPass>()
                        it.registerPass<TypeResolver>()
                    },
            )
        for ((name, configure) in configurations) {
            val tu =
                analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), false) {
                    it.registerLanguage<CLanguage>()
                    it.registerFunctionSummaries(summaries)
                    configure(it)
                }
            assertNotNull(tu, name)

            for (callee in listOf("fill", "infer_fill")) {
                val calls = tu.calls(callee)
                assertEquals(2, calls.size, "$name: $callee")
                val target = calls.flatMap { it.invokes }.toSet().singleOrNull()
                assertNotNull(target, "$name: the calls to $callee invoke different functions")
                assertEquals(callee == "infer_fill", target.isInferred, "$name: $callee")
            }
        }
    }

    @Test
    fun testUnknownArgumentTypeIsAmbiguousBetweenOverloadsInCpp() {
        val file = File("src/test/resources/cxx/inference/unknown_argument_type.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        assertNotNull(tu)

        // An unknown argument type may be convertible to both overloads, so the call is ambiguous
        // between them, and C++ does not silently pick one
        val call = tu.calls["grow"]
        assertNotNull(call)
        assertEquals(tu.functions("grow").toSet(), call.invokes.toSet())
        assertEquals(2, call.invokes.size)
        assertTrue(call.invokes.none { it.isInferred })
    }

    @Test
    fun testOnlyCandidateWithUnknownArgumentTypeInCpp() {
        val file = File("src/test/resources/cxx/inference/single_candidate.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
            }
        assertNotNull(tu)

        // An unknown type may be convertible, so the only candidate is the target, whether the
        // argument or the parameter has the unknown type
        for (name in listOf("grow", "take")) {
            val call = tu.calls[name]
            assertNotNull(call, name)
            val target = call.invokes.singleOrNull()
            assertNotNull(target, "${call.code} is not resolved to one function")
            assertFalse(target.isInferred, "${call.code} invokes an inferred function")
        }

        // A known type that cannot be converted, or a number of arguments that does not fit, means
        // that the target is a function we do not know
        for (name in listOf("log", "two")) {
            val call = tu.calls[name]
            assertNotNull(call, name)
            assertTrue(call.invokes.isNotEmpty(), "${call.code} is not resolved")
            assertTrue(
                call.invokes.all { it.isInferred },
                "${call.code} resolves to the definition",
            )
        }
    }
}
