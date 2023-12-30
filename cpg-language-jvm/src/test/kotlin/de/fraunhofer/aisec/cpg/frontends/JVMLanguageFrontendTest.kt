/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends

import de.fraunhofer.aisec.cpg.TestUtils
import de.fraunhofer.aisec.cpg.graph.*
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JVMLanguageFrontendTest {
    @Test
    fun testHello() {
        val topLevel = Path.of("src", "test", "resources", "jimple", "helloworld")
        val tu =
            TestUtils.analyzeAndGetFirstTU(
                listOf(topLevel.resolve("HelloWorld.jimple").toFile()),
                topLevel,
                true
            ) {
                it.registerLanguage<JVMLanguage>()
            }
        assertNotNull(tu)

        val helloWorld = tu.records["HelloWorld"]
        assertNotNull(helloWorld)

        val constructor = helloWorld.constructors.firstOrNull()
        assertNotNull(constructor)

        // All references should be resolved
        val refs = constructor.refs
        refs.forEach {
            val refersTo = it.refersTo
            assertNotNull(refersTo, "${it.name} could not be resolved")
            assertFalse(
                refersTo.isInferred,
                "${it.name} should not be resolved to an inferred node"
            )
        }

        val main = helloWorld.methods["main"]
        assertNotNull(main)
        assertTrue(main.isStatic)

        val param0 = main.refs["@parameter0"]
        assertNotNull(param0)

        val refersTo = param0.refersTo
        assertNotNull(refersTo)
        assertFalse(refersTo.isInferred)
    }
}
