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
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration
import de.fraunhofer.aisec.cpg.graph.scopes.GlobalScope
import de.fraunhofer.aisec.cpg.graph.types.BooleanType
import de.fraunhofer.aisec.cpg.test.*
import java.io.File
import kotlin.test.*

class CXXInferenceTest {
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

        val global = tu.allVariables["somethingGlobal"]
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

        val someClass = util.allRecords["SomeClass"]
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

        val json = some.allRecords["json"]
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

        val log = tu.allFunctions["log"]
        assertNotNull(log)
        assertIsNot<MethodDeclaration>(log)
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

        val a = result.allRecords["A"]
        assertNotNull(a)
        assertTrue(a.isInferred)

        val n = result.allNamespaces["N"]
        assertNotNull(n)
        assertTrue(n.isInferred)

        val b = n.allRecords["N::B"]
        assertNotNull(b)
        assertTrue(b.isInferred)

        val m = result.allNamespaces["M"]
        assertNotNull(m)
        assertTrue(m.isInferred)

        val c = m.allNamespaces["M::C"]
        assertNotNull(c)
        assertTrue(c.isInferred)

        val d = c.allRecords["M::C::D"]
        assertNotNull(d)
        assertTrue(d.isInferred)

        val e = result.allRecords["E"]
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

            val pair = result.allFunctions["Pair"]
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

        val recordABCA = util.allRecords["A"]
        assertNotNull(recordABCA)
        assertTrue(recordABCA.isInferred)

        val recordA = tu.allRecords["A"]
        assertNotNull(recordA)
        val funcFoo = recordA.allFunctions["foo"]
        assertNotNull(funcFoo)
        assertTrue(funcFoo.isInferred)
        val funcBar = recordA.allFunctions["bar"]
        assertNotNull(funcBar)
        assertFalse(funcBar.isInferred)
    }
}
