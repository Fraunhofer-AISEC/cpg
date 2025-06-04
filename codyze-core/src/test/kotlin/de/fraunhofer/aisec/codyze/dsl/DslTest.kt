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
package de.fraunhofer.aisec.codyze.dsl

import de.fraunhofer.aisec.codyze.CodyzeScript
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.query.*
import de.fraunhofer.aisec.cpg.query.QueryTree
import de.fraunhofer.aisec.cpg.query.allExtended
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertIs

@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
class DslTest {
    context(TranslationResult)
    fun query1(): QueryTree<Boolean> {
        return allExtended<CallExpression> { it.name eq "encrypt" }
    }

    context(TranslationResult)
    fun query2(): QueryTree<Boolean> {
        return allExtended<CallExpression> { it.arguments.size eq 2 }
    }

    object Mock : CodyzeScript(projectBuilder = ProjectBuilder(projectDir = Path(".")))

    @Test
    fun testDsl() {
        with(Mock) {
            project {
                toe {
                    name = "My Mock TOES"
                    architecture {
                        modules {
                            module("module1") {
                                directory = "src/module1"
                                exclude("tests")
                            }
                        }
                    }
                }

                requirements {
                    requirement {
                        name = "Is Security Target Correctly specified"
                        description = "test"

                        fulfilledBy { manualAssessmentOf("SEC-TARGET") }
                    }

                    category("ENCRYPTION") {
                        name = "Encryption Requirements"
                        description = "Requirements related to encryption"

                        requirement("RQ-ENCRYPTION-01") {
                            name = "Good Encryption"

                            fulfilledBy { query1() and query2() }
                        }

                        requirement("RQ-ENCRYPTION-02") {
                            name = "Good Encryption or some manual analysis"

                            fulfilledBy {
                                val logic =
                                    (query1() and
                                        query2()) or
                                        manualAssessmentOf("THIRD-PARTY-LIBRARY")
                                assertIs<Decision>(logic)
                            }
                        }

                        requirement("RQ-ENCRYPTION-03") {
                            name = "Manual analysis with good encryption"

                            fulfilledBy {
                                val logic =
                                    manualAssessmentOf("SEC-TARGET") and query1() and query2()
                                assertIs<Decision>(logic)
                            }
                        }
                    }
                }

                assumptions {
                    assume { "We assume that everything is fine." }
                    decisions {
                        accept("00000000-0000-0000-0000-000000000000")
                        reject("00000000-0000-0000-0000-000000000001")
                        undecided("00000000-0000-0000-0000-000000000002")
                        ignore("00000000-0000-0000-0000-000000000003")
                    }
                }
            }

            project { manualAssessment { of("SEC-TARGET") { true } } }
        }
    }
}
