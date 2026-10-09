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

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.graph.calls
import de.fraunhofer.aisec.cpg.graph.expressions.MemberCall
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.fields
import de.fraunhofer.aisec.cpg.graph.get
import de.fraunhofer.aisec.cpg.graph.methods
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StaticCallsTest {
    @Test
    fun testStaticCalls() {
        val config =
            TranslationConfiguration.builder()
                .sourceLocations(File("src/test/resources/StaticCalls.java"))
                .defaultPasses()
                .registerLanguage<JavaLanguage>()
                .build()
        val result = TranslationManager.builder().config(config).build().analyze().get()

        val asList = result.calls["asList"]
        assertIs<MemberCall>(asList)
        assertTrue(asList.isStatic)
        assertEquals("java.util.Arrays", asList.base?.type?.name.toString())
        assertEquals("java.util.Arrays.asList", asList.invokes.singleOrNull()?.name.toString())
        assertTrue((asList.base as? Reference)?.isStaticAccess == true)

        val doSomething = result.calls["doSomething"]
        assertIs<MemberCall>(doSomething)
        assertTrue(doSomething.isStatic)
        assertEquals("com.example.Util", doSomething.base?.type?.name.toString())
        assertEquals(
            "com.example.Util.doSomething",
            doSomething.invokes.singleOrNull()?.name.toString(),
        )

        val helper = result.calls["helper"]
        assertNotNull(helper)
        assertEquals(result.methods["helper"], helper.invokes.singleOrNull())

        // a call on a variable is not static
        val size = result.calls["size"]
        assertIs<MemberCall>(size)
        assertTrue(!size.isStatic)

        // we must not infer fields for the type names used as scope
        assertTrue(result.fields.none { it.name.localName in listOf("Arrays", "Util") })
    }
}
