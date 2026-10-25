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
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Method
import de.fraunhofer.aisec.cpg.graph.scopes.GlobalScope
import de.fraunhofer.aisec.cpg.graph.types.BooleanType
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
}
