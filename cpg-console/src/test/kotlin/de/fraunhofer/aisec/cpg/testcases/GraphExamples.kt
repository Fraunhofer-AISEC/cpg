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
package de.fraunhofer.aisec.cpg.testcases

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.TestLanguageWithColon
import de.fraunhofer.aisec.cpg.frontends.testFrontend
import de.fraunhofer.aisec.cpg.graph.array
import de.fraunhofer.aisec.cpg.graph.builder.*
import de.fraunhofer.aisec.cpg.graph.newNewArrayExpression
import de.fraunhofer.aisec.cpg.graph.pointer

class GraphExamples {
    companion object {
        fun ArrayJava(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("Array.java") {
                        record("Array") {
                            method("main", void()) {
                                isStatic = true
                                param("args", t("String").array())
                                body {
                                    declare {
                                        variable("c", t("char").array()) {
                                            val init = newNewArrayExpression()
                                            init.addDimension(literal(4, t("int")))
                                            this.initializer = init
                                        }
                                    }
                                    declare { variable("a", t("int")) { literal(4, t("int")) } }
                                    declare {
                                        variable("b", t("int")) { ref("a") + literal(1, t("int")) }
                                    }
                                    // obviously null
                                    declare {
                                        variable("obj", t("AnotherObject")) {
                                            literal(null, t("AnotherObject"))
                                        }
                                    }
                                    // lets make it a little bit tricky at least
                                    ref("obj") assign (ref("something"))
                                    ifStmt {
                                        condition { ref("something") }
                                        thenStmt {
                                            declare {
                                                variable("yetAnotherObject", t("AnotherObject")) {
                                                    literal(null, t("AnotherObject"))
                                                }

                                                // whoops, overriden with null again
                                                ref("obj") assign (ref("yetAnotherObject"))
                                            }
                                        }
                                    }

                                    memberCall("doSomething", ref("obj"))
                                    declare {
                                        variable("s", t("String")) {
                                            literal("some string", t("String"))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        fun ArrayCpp(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguageWithColon>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("array.cpp") {
                        function("main", t("int")) {
                            body {
                                declare {
                                    variable("c", t("char").pointer()) {
                                        val init = newNewArrayExpression()
                                        init.addDimension(literal(4, t("int")))
                                        this.initializer = init
                                    }
                                }
                                declare { variable("a", t("int")) { literal(4, t("int")) } }
                                declare {
                                    variable("b", t("int")) { ref("a") + literal(1, t("int")) }
                                }
                                declare {
                                    variable("d", t("char")) {
                                        subscriptExpr {
                                            ref("c")
                                            ref("b")
                                        }
                                    }
                                }
                            }
                        }
                        function("some_other_function", void()) {
                            body {
                                declare {
                                    variable("c", t("char").pointer()) {
                                        val init = newNewArrayExpression()
                                        init.addDimension(literal(100, t("int")))
                                        this.initializer = init
                                    }
                                }
                                returnStmt {
                                    subscriptExpr {
                                        ref("c")
                                        literal(0, t("int"))
                                    }
                                }
                            }
                        }
                    }
                }
            }
    }
}
