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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.graph.builder.*
import kotlin.test.Test
import kotlin.test.assertNotNull

class ControlFlowSensitiveDFGPassTest {
    @Test
    fun testConfiguration() {
        val result = getForEachTest()
        assertNotNull(result)
    }

    fun getForEachTest() =
        ControlDependenceGraphPassTest.testFrontend(
                TranslationConfiguration.builder()
                    .registerLanguage(TestLanguage("::"))
                    .defaultPasses()
                    .registerPass<ControlFlowSensitiveDFGPass>()
                    .configurePass<ControlFlowSensitiveDFGPass>(
                        ControlFlowSensitiveDFGPass.Configuration(maxComplexity = 0)
                    )
                    .build()
            )
            .build {
                translationResult {
                    translationUnit("forEach.cpp") {
                        // The main method
                        function("main", t("int")) {
                            body {
                                declare { variable("i", t("int")) { literal(0, t("int")) } }
                                forEachStmt {
                                    declare { variable("loopVar", t("string")) }
                                    call("magicFunction")
                                    loopBody {
                                        call("printf") {
                                            literal("loop: \${}\n", t("string"))
                                            ref("loopVar")
                                        }
                                    }
                                }
                                call("printf") { literal("1\n", t("string")) }

                                returnStmt { ref("i") }
                            }
                        }
                    }
                }
            }
}
