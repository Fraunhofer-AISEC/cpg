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
package de.fraunhofer.aisec.cpg.testcases

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.graph.array
import de.fraunhofer.aisec.cpg.graph.builder.*
import de.fraunhofer.aisec.cpg.graph.newArrayCreationExpression
import de.fraunhofer.aisec.cpg.passes.EdgeCachePass
import de.fraunhofer.aisec.cpg.passes.UnreachableEOGPass

class ValueEvaluationTests {
    companion object {
        fun getSizeExample(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage(TestLanguage("."))
                    .registerPass<UnreachableEOGPass>()
                    .registerPass<EdgeCachePass>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("size.java") {
                        record("MainClass") {
                            method("main") {
                                this.isStatic = true
                                param("args", t("String").array())
                                body {
                                    declare {
                                        variable("array", t("int").array()) {
                                            val newExpr = newArrayCreationExpression()
                                            newExpr.addDimension(literal(3, t("int")))
                                        }
                                    }
                                    forStmt(
                                        declare {
                                            variable("i", t("int")) { literal(0, t("int")) }
                                        },
                                        ref("i") lt member("length", ref("array")),
                                        ref("i").inc()
                                    ) {
                                        ase {
                                            ref("array")
                                            ref("i")
                                        } assign ref("i")
                                    }
                                    memberCall("println", member("out", ref("System"))) {
                                        ase {
                                            ref("array")
                                            literal(1, t("int"))
                                        }
                                    }

                                    declare {
                                        variable("str", t("String")) {
                                            literal("abcde", t("String"))
                                        }
                                    }

                                    memberCall("println", member("out", ref("System"))) {
                                        ref("str")
                                    }
                                    returnStmt { literal(0, t("int")) }
                                }
                            }
                        }
                    }
                }
            }
    }
}
