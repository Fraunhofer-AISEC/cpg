/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage
import de.fraunhofer.aisec.cpg.frontends.cxx.CPPLanguage
import de.fraunhofer.aisec.cpg.frontends.cxx.CToCxxMapper
import de.fraunhofer.aisec.cpg.frontends.cxx.CxxToCMapper
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class LanguageInterfaceTest {
    private val topLevel = Path.of("src", "test", "resources", "crossLanguage")

    @Test
    @Throws(Exception::class)
    fun testCtoCPP() {
        val result =
            analyze(
                files =
                    listOf(
                        topLevel.resolve("simple.c").toFile(),
                        topLevel.resolve("simpleCxx.cpp").toFile(),
                    ),
                topLevel = topLevel,
                usePasses = true,
            ) {
                it.registerLanguage<CLanguage>()
                it.registerLanguage<CPPLanguage>()
                it.registerLanguageInterface<CToCxxMapper>()
                it.registerLanguageInterface<CxxToCMapper>()
            }

        val main = result.functions["main"]
        assertNotNull(main)
        val call = main.calls["hello_world"]
        assertNotNull(call)

        val helloWorlds = result.functions("hello_world")
        assertEquals(1, helloWorlds.size)
        val helloWorld = helloWorlds.single()

        assertEquals(helloWorld, call.invokes.singleOrNull())
    }
}
