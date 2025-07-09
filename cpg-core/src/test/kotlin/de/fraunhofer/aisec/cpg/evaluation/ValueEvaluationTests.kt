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
package de.fraunhofer.aisec.cpg.evaluation

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.testFrontend
import de.fraunhofer.aisec.cpg.graph.array
import de.fraunhofer.aisec.cpg.graph.builder.*
import de.fraunhofer.aisec.cpg.graph.newConditionalExpression
import de.fraunhofer.aisec.cpg.graph.newNewArrayExpression

class ValueEvaluationTests {
    companion object {
        fun getSizeExample(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
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
                                            val newExpr = newNewArrayExpression()
                                            newExpr.addDimension(literal(3, t("int")))
                                            this.initializer = newExpr
                                        }
                                    }
                                    forStmt {
                                        loopBody {
                                            subscriptExpr {
                                                ref("array")
                                                ref("i")
                                            } assign ref("i")
                                        }
                                        forInitializer {
                                            declare {
                                                variable("i", t("int")) { literal(0, t("int")) }
                                            }
                                        }
                                        forCondition { ref("i") lt member("length", ref("array")) }
                                        forIteration { ref("i").inc() }
                                    }
                                    memberCall("println", member("out", ref("System"))) {
                                        subscriptExpr {
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
                                    returnStmt {}
                                }
                            }
                        }
                    }
                }
            }

        fun getComplexExample(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("complex.java") {
                        record("MainClass") {
                            method("main") {
                                this.isStatic = true
                                param("args", t("String").array())
                                body {
                                    declare { variable("i", t("int")) { literal(3, t("int")) } }
                                    declare { variable("s", t("String")) }

                                    ifStmt {
                                        condition { ref("i") lt literal(2, t("int")) }
                                        thenStmt { ref("s") assign literal("small", t("String")) }
                                        elseStmt { ref("s") assign literal("big", t("String")) }
                                    }

                                    ref("s") assignPlus literal("!", t("String"))

                                    ref("s") assign { ref("s") + literal("?", t("string")) }

                                    ref("i").inc()

                                    memberCall("println", member("out", ref("System"))) { ref("s") }

                                    memberCall("println", member("out", ref("System"))) { ref("i") }
                                    returnStmt {}
                                }
                            }
                        }
                    }
                }
            }

        fun getExample(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("example.cpp") {
                        function("main", t("int")) {
                            body {
                                declare {
                                    variable("b", t("int")) {
                                        literal(1, t("int")) + literal(1, t("int"))
                                    }
                                }
                                call("println") { ref("b") }

                                declare { variable("a", t("int")) { literal(1, t("int")) } }
                                ref("a") assign literal(2, t("int"))
                                call("println") { ref("a") }

                                declare {
                                    variable("c", t("int")) {
                                        literal(5, t("int")) - literal(2, t("int"))
                                    }
                                }

                                declare {
                                    variable("d", t("float")) {
                                        literal(8, t("int")) / literal(3, t("int"))
                                    }
                                }

                                declare {
                                    variable("e", t("float")) {
                                        literal(7.0, t("float")) / literal(2, t("int"))
                                    }
                                }

                                declare {
                                    variable("f", t("int")) {
                                        literal(2, t("int")) * literal(5, t("int"))
                                    }
                                }

                                declare { variable("g", t("int")) { -ref("c") } }

                                call("println") {
                                    literal("Hello ", t("String")) + literal("world", t("String"))
                                }

                                declare {
                                    variable("h", t("bool")) {
                                        literal(5, t("int")) le literal(2, t("int"))
                                    }
                                }

                                declare {
                                    variable("i", t("bool")) {
                                        literal(3, t("int")) gt literal(3, t("int"))
                                    }
                                }

                                declare {
                                    variable("j", t("bool")) {
                                        literal(3, t("int")) ge literal(3.2, t("float"))
                                    }
                                }

                                declare {
                                    variable("k", t("bool")) {
                                        literal(3.1, t("float")) le literal(3, t("int"))
                                    }
                                }

                                declare {
                                    variable("l", t("bool")) {
                                        literal(3L, t("long")) ge
                                            cast(t("float")) { literal(3.1, t("float")) }
                                    }
                                }

                                declare {
                                    variable("m", t("bool")) {
                                        cast(t("char")) { literal(3, t("int")) } ge
                                            literal(3.1, t("float"))
                                    }
                                }

                                declare {
                                    variable("n", t("bool")) {
                                        literal(3, t("int")) eq literal(3.1, t("float"))
                                    }
                                }

                                returnStmt { literal(0, t("int")) }
                            }
                        }
                    }
                }
            }

        fun getCfExample(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("cfexample.cpp") {
                        import("time.h")
                        import("stdlib.h")
                        function("main", t("int")) {
                            body {
                                call("srand") { call("time") { ref("NULL") } }

                                declare { variable("b", t("int")) { literal(1, t("int")) } }

                                ifStmt {
                                    condition { call("rand") lt literal(10, t("int")) }
                                    thenStmt { ref("b") assign { ref("b") + literal(1, t("int")) } }
                                }

                                call("println") { ref("b") } // 1, 2

                                ifStmt {
                                    condition { call("rand") gt literal(5, t("int")) }
                                    thenStmt { ref("b") assign { ref("b") - literal(1, t("int")) } }
                                }

                                call("println") { ref("b") } // 0, 1, 2

                                ifStmt {
                                    condition { call("rand") gt literal(3, t("int")) }
                                    thenStmt { ref("b") assign { ref("b") * literal(2, t("int")) } }
                                }

                                call("println") { ref("b") } // 0, 1, 2, 4

                                ifStmt {
                                    condition { call("rand") lt literal(4, t("int")) }
                                    thenStmt { ref("b") assign -ref("b") }
                                }

                                call("println") { ref("b") } // -4, -2, -1, 0, 1, 2, 4

                                declare {
                                    variable("a", t("int")) {
                                        this.initializer =
                                            newConditionalExpression(
                                                ref("b") lt literal(2, t("int")),
                                                literal(3, t("int")),
                                                literal(5, t("int")).inc(),
                                            )
                                    }
                                }

                                call("println") { ref("a") } // 3, 6

                                returnStmt { literal(0, t("int")) }
                            }
                        }

                        function("loop", t("int")) {
                            body {
                                declare {
                                    variable("array", t("int").array()) {
                                        val creationExpr = newNewArrayExpression()
                                        creationExpr.addDimension(literal(6, t("int")))
                                        this.initializer = creationExpr
                                    }
                                }

                                forStmt {
                                    loopBody {
                                        subscriptExpr {
                                            ref("array")
                                            ref("i")
                                        } assign ref("i")
                                    }
                                    forInitializer {
                                        declareVar("i", t("int")) { literal(0, t("int")) }
                                    }
                                    forCondition { ref("i") lt literal(6, t("int")) }
                                    forIteration { ref("i").incNoContext() }
                                }
                                returnStmt { literal(0, t("int")) }
                            }
                        }
                    }
                }
            }
    }
}
