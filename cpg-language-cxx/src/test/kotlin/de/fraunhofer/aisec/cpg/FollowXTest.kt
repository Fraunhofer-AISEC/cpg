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
package de.fraunhofer.aisec.cpg.frontends.cxx

import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.invoke
import de.fraunhofer.aisec.cpg.graph.variables
import de.fraunhofer.aisec.cpg.test.BaseTest
import de.fraunhofer.aisec.cpg.test.analyze
import java.io.File
import kotlin.test.Test
import org.junit.jupiter.api.assertNotNull

class FollowXTest : BaseTest() {
    @Test
    fun testFollowX() {
        val file = File("src/test/resources/complex_dfg.c")
        val result =
            analyze(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
            }
        assertNotNull(result)

        // functions
        val mainFunc = result.functions("main").single()

        // Variables
        val i = result.variables("i").single()

        // actual tests
        // This will run forever (or very long), so it's commented out.
        // TODO: Fix
        /*        val (paths, time) =
            measureTimedValue {
                i.followDFGEdgesUntilHit(
                    findAllPossiblePaths = true,
                    sensitivities = OnlyFullDFG + FieldSensitive + ContextSensitive,
                    predicate = { node -> (node as? Reference)?.name?.localName == "i" },
                )
            }
        println("Path lookup took $time")*/
    }
}
