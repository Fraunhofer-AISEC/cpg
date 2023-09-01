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

import de.fraunhofer.aisec.cpg.ScopeManager
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.TypeManager
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.array
import de.fraunhofer.aisec.cpg.graph.builder.*
import de.fraunhofer.aisec.cpg.passes.EdgeCachePass
import de.fraunhofer.aisec.cpg.passes.UnreachableEOGPass

class GraphExamples {

    companion object {
        fun getSimpleOrder(
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
                                        ref("p4")
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
                                        ref("p4")
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
                                        ref("p4")
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

        fun testFrontend(config: TranslationConfiguration): TestLanguageFrontend {
            val ctx = TranslationContext(config, ScopeManager(), TypeManager())
            val language = config.languages.filterIsInstance<TestLanguage>().first()
            return TestLanguageFrontend(language.namespaceDelimiter, language, ctx)
        }
    }
}
