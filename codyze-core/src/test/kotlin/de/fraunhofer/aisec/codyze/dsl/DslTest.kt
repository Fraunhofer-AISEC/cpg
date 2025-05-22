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
import kotlin.test.Test

class DslTest {
    object Mock : CodyzeScript()

    @Test
    fun testDsl() {
        with(Mock) {
            fun query1(result: TranslationResult): QueryTree<Boolean> {
                return result.allExtended<CallExpression> { it.name eq "encrypt" }
            }

            fun query2(result: TranslationResult): QueryTree<Boolean> {
                return result.allExtended<CallExpression> { it.arguments.size eq 2 }
            }

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
                    requirement("Good Encryption") {
                        byQuery { result -> query1(result) and query2(result) }
                    }
                }

                assumptions {
                    assume { "We assume that everything is fine." }
                    accept("00000000-0000-0000-0000-000000000000")
                    reject("00000000-0000-0000-0000-000000000001")
                    undecided("00000000-0000-0000-0000-000000000002")
                    ignore("00000000-0000-0000-0000-000000000003")
                }
            }
        }
    }
}
