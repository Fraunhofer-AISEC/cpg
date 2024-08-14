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
import de.fraunhofer.aisec.cpg.test.*
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CXXInferenceTest {
    @Test
    fun testGlobals() {
        val file = File("src/test/resources/cxx/inference.cpp")
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
        val file = File("src/test/resources/cxx/inference.cpp")
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
        val file = File("src/test/resources/cxx/tricky_inference.cpp")
        val tu =
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
                it.loadIncludes(false)
                it.addIncludesToGraph(false)
            }
        assertNotNull(tu)

        val some = tu.namespaces["some"]
        assertNotNull(some)
        assertTrue(some.isInferred)

        val json = some.records["json"]
        assertNotNull(json)
        assertTrue(json.isInferred)

        val iterator = json.records["iterator"]
        assertNotNull(iterator)
        assertTrue(iterator.isInferred)
    }
}
