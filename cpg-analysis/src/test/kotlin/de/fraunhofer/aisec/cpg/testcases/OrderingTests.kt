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
import de.fraunhofer.aisec.cpg.frontends.testFrontend
import de.fraunhofer.aisec.cpg.graph.array
import de.fraunhofer.aisec.cpg.graph.builder.*
import de.fraunhofer.aisec.cpg.passes.UnreachableEOGPass

class GraphExamples {

    companion object {
        fun getSimpleOrder(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .registerPass<UnreachableEOGPass>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("SimpleOrder.java") {
                        import("kotlin.random.URandomKt")
                        record("Botan") {
                            constructor {
                                param("i", t("int"))
                                body {}
                            }
                            method("create", void()) { body {} }
                            method("finish", void()) {
                                param("b", t("char").array())
                                body {}
                            }
                            method("init", void()) { body {} }
                            method("process", void()) { body {} }
                            method("reset", void()) { body {} }
                            method("start", void()) {
                                param("i", t("int"))
                                body {}
                            }
                            method("set_key", void()) {
                                param("i", t("int"))
                                body {}
                            }
                        }
                        record("SimpleOrder") {
                            field("cipher", t("char").array()) {}
                            field("key", t("int")) {}
                            field("iv", t("int")) {}
                            field("direction", t("Cipher_Dir")) {}
                            field("buf", t("char").array()) {}

                            method("ok", void()) {
                                body {
                                    declare {
                                        variable("p4", t("Botan")) {
                                            construct("Botan") { literal(2, t("int")) }
                                        }
                                    }
                                    memberCall("start", ref("p4")) { ref("iv") }
                                    memberCall("finish", ref("p4")) { ref("buf") }
                                    returnStmt {}
                                }
                            }

                            method("ok2", void()) {
                                body {
                                    declare {
                                        variable("p4", t("Botan")) {
                                            construct("Botan") { literal(2, t("int")) }
                                        }
                                    }
                                    memberCall("start", ref("p4")) { ref("iv") }
                                    memberCall(
                                        "foo",
                                        ref("p4"),
                                    ) // Not in the entity and therefore ignored
                                    memberCall("finish", ref("p4")) { ref("buf") }
                                    returnStmt {}
                                }
                            }

                            method("ok3", void()) {
                                body {
                                    declare {
                                        variable("p4", t("Botan")) {
                                            construct("Botan") { literal(2, t("int")) }
                                        }
                                    }
                                    declare {
                                        variable("x", t("int")) {
                                            memberCall("nextUInt", ref("URandomKt"))
                                        }
                                    }
                                    ifStmt {
                                        condition { ref("x") le literal(5, t("int")) }
                                        thenStmt { memberCall("start", ref("p4")) { ref("iv") } }
                                        elseStmt { memberCall("start", ref("p4")) { ref("iv") } }
                                    }
                                    memberCall(
                                        "foo",
                                        ref("p4"),
                                    ) // Not in the entity and therefore ignored
                                    memberCall("finish", ref("p4")) { ref("buf") }
                                    returnStmt {}
                                }
                            }

                            method("nok1", void()) {
                                body {
                                    declare {
                                        variable("p4", t("Botan")) {
                                            construct("Botan") { literal(1, t("int")) }
                                        }
                                    }
                                    memberCall("set_key", ref("p4")) {
                                        ref("key")
                                    } // Not allowed as start
                                    memberCall("start", ref("p4")) { ref("iv") }
                                    memberCall("finish", ref("p4")) { ref("buf") }
                                    memberCall(
                                        "foo",
                                        ref("p4"),
                                    ) // Not in the entity and therefore ignored
                                    memberCall("set_key", ref("p4")) { ref("key") }
                                    returnStmt {}
                                }
                            }

                            method("nok2", void()) {
                                body {
                                    declare {
                                        variable("p4", t("Botan")) {
                                            construct("Botan") { literal(2, t("int")) }
                                        }
                                    }
                                    memberCall("start", ref("p4")) { ref("iv") }
                                    // Missing: memberCall("finish", ref("p4")) {ref("buf")}
                                    returnStmt {}
                                }
                            }

                            method("nok3", void()) {
                                body {
                                    declare {
                                        variable("p4", t("Botan")) {
                                            construct("Botan") { literal(2, t("int")) }
                                        }
                                    }
                                    ifStmt {
                                        condition {
                                            memberCall("nextUInt", ref("URandomKt")) le
                                                literal(5, t("int"))
                                        }
                                        thenStmt { memberCall("start", ref("p4")) { ref("iv") } }
                                    }
                                    // start could be missing here
                                    memberCall("finish", ref("p4")) { ref("buf") }
                                    returnStmt {}
                                }
                            }

                            method("nok4", void()) {
                                body {
                                    declare {
                                        variable("p4", t("Botan")) {
                                            construct("Botan") { literal(2, t("int")) }
                                        }
                                    }
                                    ifStmt {
                                        condition { literal(true, t("boolean")) }
                                        thenStmt {
                                            memberCall("start", ref("p4")) { ref("iv") }
                                            memberCall("finish", ref("p4")) { ref("buf") }
                                        }
                                    }
                                    // Not ok because p4 is already finished
                                    memberCall("start", ref("p4")) { ref("iv") }
                                    memberCall("finish", ref("p4")) { ref("buf") }
                                    returnStmt {}
                                }
                            }

                            method("nok5", void()) {
                                body {
                                    block {
                                        declare {
                                            variable("p4", t("Botan")) {
                                                construct("Botan") { literal(2, t("int")) }
                                            }
                                        }
                                        memberCall("start", ref("p4")) { ref("iv") }
                                    }
                                    block {
                                        declare {
                                            variable("p5", t("Botan")) {
                                                construct("Botan") { literal(2, t("int")) }
                                            }
                                        }
                                        memberCall("finish", ref("p5")) { ref("buf") }
                                        returnStmt {}
                                    }
                                }
                            }
                        }
                    }
                }
            }

        fun getComplexOrder(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .registerPass<UnreachableEOGPass>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("ComplexOrder.java") {
                        import("kotlin.random.URandomKt")
                        record("Botan") {
                            constructor {
                                param("i", t("int"))
                                body {}
                            }
                            method("create", void()) { body {} }
                            method("finish", void()) { body {} }
                            method("init", void()) { body {} }
                            method("process", void()) { body {} }
                            method("reset", void()) { body {} }
                            method("start", void()) { body {} }
                        }
                        record("ComplexOrder") {
                            method("ok_minimal1", void()) {
                                body {
                                    declare {
                                        variable("p1", t("Botan")) {
                                            construct("Botan") { literal(2, t("int")) }
                                        }
                                    }
                                    memberCall("create", ref("p1"))
                                    memberCall("init", ref("p1"))
                                    memberCall("start", ref("p1"))
                                    memberCall("finish", ref("p1"))
                                    returnStmt {}
                                }
                            }

                            method("ok_minimal2", void()) {
                                body {
                                    declare {
                                        variable("p1", t("Botan")) {
                                            construct("Botan") { literal(2, t("int")) }
                                        }
                                    }
                                    memberCall("create", ref("p1"))
                                    memberCall("init", ref("p1"))
                                    memberCall("start", ref("p1"))
                                    memberCall("process", ref("p1"))
                                    memberCall("finish", ref("p1"))
                                    returnStmt {}
                                }
                            }

                            method("ok_minimal3", void()) {
                                body {
                                    declare {
                                        variable("p1", t("Botan")) {
                                            construct("Botan") { literal(2, t("int")) }
                                        }
                                    }
                                    memberCall("create", ref("p1"))
                                    memberCall("init", ref("p1"))
                                    memberCall("start", ref("p1"))
                                    memberCall("process", ref("p1"))
                                    memberCall("finish", ref("p1"))
                                    memberCall("reset", ref("p1"))
                                    returnStmt {}
                                }
                            }

                            method("ok2", void()) {
                                body {
                                    declare {
                                        variable("p2", t("Botan")) {
                                            construct("Botan") { literal(2, t("int")) }
                                        }
                                    }
                                    memberCall("create", ref("p2"))
                                    memberCall("init", ref("p2"))
                                    memberCall("start", ref("p2"))
                                    memberCall("process", ref("p2"))
                                    memberCall("process", ref("p2"))
                                    memberCall("process", ref("p2"))
                                    memberCall("process", ref("p2"))
                                    memberCall("finish", ref("p2"))
                                    returnStmt {}
                                }
                            }

                            method("ok3", void()) {
                                body {
                                    declare {
                                        variable("p3", t("Botan")) {
                                            construct("Botan") { literal(2, t("int")) }
                                        }
                                    }
                                    memberCall("create", ref("p3"))
                                    memberCall("init", ref("p3"))
                                    memberCall("start", ref("p3"))
                                    memberCall("process", ref("p3"))
                                    memberCall("finish", ref("p3"))
                                    memberCall("start", ref("p3"))
                                    memberCall("process", ref("p3"))
                                    memberCall("finish", ref("p3"))
                                    returnStmt {}
                                }
                            }

                            method("ok4", void()) {
                                body {
                                    declare {
                                        variable("p3", t("Botan")) {
                                            construct("Botan") { literal(2, t("int")) }
                                        }
                                    }
                                    memberCall("create", ref("p3"))
                                    memberCall("init", ref("p3"))
                                    memberCall("start", ref("p3"))
                                    memberCall("process", ref("p3"))
                                    memberCall("finish", ref("p3"))
                                    memberCall("start", ref("p3"))
                                    memberCall("process", ref("p3"))
                                    memberCall("finish", ref("p3"))
                                    memberCall("reset", ref("p3"))
                                    returnStmt {}
                                }
                            }

                            method("nok1", void()) {
                                body {
                                    declare {
                                        variable("p5", t("Botan")) {
                                            construct("Botan") { literal(2, t("int")) }
                                        }
                                    }
                                    memberCall("init", ref("p5"))
                                    memberCall("start", ref("p5"))
                                    memberCall("process", ref("p5"))
                                    memberCall("finish", ref("p5"))
                                    returnStmt {}
                                }
                            }

                            method("nok2", void()) {
                                body {
                                    declare {
                                        variable("p6", t("Botan")) {
                                            construct("Botan") { literal(2, t("int")) }
                                        }
                                    }
                                    memberCall("create", ref("p6"))
                                    memberCall("init", ref("p6"))
                                    ifStmt {
                                        condition { literal(false, t("boolean")) }
                                        thenStmt {
                                            memberCall("start", ref("p6"))
                                            memberCall("process", ref("p6"))
                                            memberCall("finish", ref("p6"))
                                        }
                                    }
                                    memberCall("reset", ref("p6"))
                                    returnStmt {}
                                }
                            }

                            method("nok3", void()) {
                                body {
                                    declare {
                                        variable("p6", t("Botan")) {
                                            construct("Botan") { literal(2, t("int")) }
                                        }
                                    }
                                    whileStmt {
                                        whileCondition { literal(true, t("boolean")) }
                                        loopBody {
                                            memberCall("create", ref("p6"))
                                            memberCall("init", ref("p6"))
                                            memberCall("start", ref("p6"))
                                            memberCall("process", ref("p6"))
                                            memberCall("finish", ref("p6"))
                                        }
                                    }
                                    memberCall("reset", ref("p6"))
                                    returnStmt {}
                                }
                            }

                            method("nokWhile", void()) {
                                body {
                                    declare {
                                        variable("p7", t("Botan")) {
                                            construct("Botan") { literal(2, t("int")) }
                                        }
                                    }
                                    memberCall("create", ref("p7"))
                                    memberCall("init", ref("p7"))
                                    whileStmt {
                                        whileCondition {
                                            memberCall("nextUInt", ref("URandomKt"), true) gt
                                                literal(5, t("int"))
                                        }
                                        loopBody {
                                            memberCall("start", ref("p7"))
                                            memberCall("process", ref("p7"))
                                            memberCall("finish", ref("p7"))
                                        }
                                    }
                                    memberCall("reset", ref("p7"))
                                    returnStmt {}
                                }
                            }

                            method("okWhile", void()) {
                                body {
                                    declare {
                                        variable("p8", t("Botan")) {
                                            construct("Botan") { literal(2, t("int")) }
                                        }
                                    }
                                    memberCall("create", ref("p8"))
                                    memberCall("init", ref("p8"))
                                    memberCall("start", ref("p8"))
                                    memberCall("process", ref("p8"))
                                    memberCall("finish", ref("p8"))
                                    whileStmt {
                                        whileCondition { literal(true, t("boolean")) }
                                        loopBody {
                                            memberCall("start", ref("p8"))
                                            memberCall("process", ref("p8"))
                                            memberCall("finish", ref("p8"))
                                        }
                                    }
                                    memberCall("reset", ref("p8"))
                                    returnStmt {}
                                }
                            }

                            method("okWhile2", void()) {
                                body {
                                    declare {
                                        variable("p8", t("Botan")) {
                                            construct("Botan") { literal(2, t("int")) }
                                        }
                                    }
                                    memberCall("create", ref("p8"))
                                    memberCall("init", ref("p8"))
                                    whileStmt {
                                        whileCondition { literal(true, t("boolean")) }
                                        loopBody {
                                            memberCall("start", ref("p8"))
                                            memberCall("process", ref("p8"))
                                            memberCall("finish", ref("p8"))
                                        }
                                    }
                                    memberCall("reset", ref("p8"))
                                    returnStmt {}
                                }
                            }

                            method("okDoWhile", void()) {
                                body {
                                    declare {
                                        variable("p6", t("Botan")) {
                                            construct("Botan") { literal(2, t("int")) }
                                        }
                                    }
                                    memberCall("create", ref("p6"))
                                    memberCall("init", ref("p6"))
                                    doStmt {
                                        loopBody {
                                            memberCall("start", ref("p6"))
                                            memberCall("process", ref("p6"))
                                            memberCall("finish", ref("p6"))
                                        }

                                        doCondition {
                                            memberCall("nextUInt", ref("URandomKt")) gt
                                                literal(5, t("int"))
                                        }
                                    }
                                    memberCall("reset", ref("p6"))
                                    returnStmt {}
                                }
                            }

                            method("minimalInterprocUnclear", void()) {
                                body {
                                    declare {
                                        variable("p1", t("Botan")) {
                                            construct("Botan") { literal(2, t("int")) }
                                        }
                                    }
                                    memberCall("create", ref("p1"))
                                    memberCall("foo", ref("this")) { ref("p1") }
                                    memberCall("start", ref("p1"))
                                    memberCall("finish", ref("p1"))
                                    returnStmt {}
                                }
                            }

                            method("minimalInterprocFail", void()) {
                                body {
                                    declare {
                                        variable("p1", t("Botan")) {
                                            construct("Botan") { literal(2, t("int")) }
                                        }
                                    }
                                    memberCall("create", ref("p1"))
                                    ifStmt {
                                        condition {
                                            memberCall("nextUInt", ref("URandomKt")) gt
                                                literal(5, t("int"))
                                        }
                                        thenStmt { memberCall("foo", ref("this")) { ref("p1") } }
                                    }
                                    memberCall("start", ref("p1"))
                                    memberCall("finish", ref("p1"))
                                    returnStmt {}
                                }
                            }

                            method("minimalInterprocFail2", void()) {
                                body {
                                    declare {
                                        variable("p1", t("Botan")) {
                                            construct("Botan") { literal(1, t("int")) }
                                        }
                                    }
                                    declare {
                                        variable("p2", t("Botan")) {
                                            construct("Botan") { literal(2, t("int")) }
                                        }
                                    }
                                    memberCall("create", ref("p1"))
                                    memberCall("create", ref("p2"))
                                    memberCall("foo", ref("this")) { ref("p2") }
                                    memberCall("start", ref("p1"))
                                    memberCall("finish", ref("p1"))
                                    returnStmt {}
                                }
                            }

                            method("foo", void()) {
                                param("p1", t("Botan"))
                                body {
                                    memberCall("init", ref("p1"))
                                    returnStmt {}
                                }
                            }

                            method("bar", void()) {
                                body {
                                    declare {
                                        variable("p1", t("Botan")) {
                                            construct("Botan") { literal(1, t("int")) }
                                        }
                                    }
                                    memberCall("create", ref("p1"))
                                    memberCall("minimalInterprocUnclearArgument", ref("this")) {
                                        ref("p1")
                                    }
                                    returnStmt {}
                                }
                            }

                            method("minimalInterprocUnclearArgument", void()) {
                                param("p1", t("Botan"))
                                body {
                                    memberCall("init", ref("p1"))
                                    memberCall("start", ref("p1"))
                                    memberCall("finish", ref("p1"))
                                    returnStmt {}
                                }
                            }

                            method("minimalInterprocUnclearReturn", t("Botan")) {
                                body {
                                    declare {
                                        variable("p1", t("Botan")) {
                                            construct("Botan") { literal(1, t("int")) }
                                        }
                                    }
                                    memberCall("create", ref("p1"))
                                    memberCall("init", ref("p1"))
                                    memberCall("start", ref("p1"))
                                    returnStmt { ref("p1") }
                                }
                            }
                        }
                    }
                }
            }
    }
}
