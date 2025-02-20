/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.concepts

import de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage
import de.fraunhofer.aisec.cpg.graph.conceptNodes
import de.fraunhofer.aisec.cpg.graph.concepts.arch.POSIX
import de.fraunhofer.aisec.cpg.graph.concepts.arch.Win32
import de.fraunhofer.aisec.cpg.graph.concepts.flows.Main
import de.fraunhofer.aisec.cpg.graph.get
import de.fraunhofer.aisec.cpg.passes.concepts.flows.cxx.CXXEntryPointsPass
import de.fraunhofer.aisec.cpg.test.analyze
import java.io.File
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Test

class EntryPointTest {
    @Test
    fun testCXX() {
        val topLevel = File("src/integrationTest/resources/c")
        val result =
            analyze(listOf(), topLevel.toPath(), true) {
                it.registerLanguage<CLanguage>()
                it.registerPass<CXXEntryPointsPass>()
                it.softwareComponents(
                    mutableMapOf(
                        "main" to listOf(topLevel.resolve("main")),
                        "winmain" to listOf(topLevel.resolve("winmain")),
                    )
                )
            }
        assertNotNull(result)

        val main = result.components["main"]?.conceptNodes?.filterIsInstance<Main>()?.singleOrNull()
        assertNotNull(main)
        assertIs<POSIX>(main.os)

        val winMain =
            result.components["winmain"]?.conceptNodes?.filterIsInstance<Main>()?.singleOrNull()
        assertNotNull(winMain)
        assertIs<Win32>(winMain.os)
    }
}
