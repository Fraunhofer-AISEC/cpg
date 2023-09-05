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
import de.fraunhofer.aisec.cpg.passes.EdgeCachePass

class Query {
    companion object {
        fun getDataflow(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage(TestLanguage("."))
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("Dataflow.java") {
                        record("Dataflow") {
                            field("attr", t("string")) {}
                            method("toString", t("string")) {
                                body {
                                    returnStmt {
                                        literal("Dataflow: attr=", t("string")) + ref("attr")
                                    }
                                }
                            }

                            method("test", t("string")) {
                                body { returnStmt { literal("abcd", t("string")) } }
                            }

                            method("print", void()) {
                                param("s", t("string"))
                                body {
                                    memberCall("println", member("out", ref("System"))) { ref("s") }
                                    returnStmt {}
                                }
                            }

                            method("main", void()) {
                                isStatic = true
                                param("args", t("string").array())
                                body {
                                    declare {
                                        variable("sc", t("Dataflow")) {
                                            new { construct("Dataflow") }
                                        }
                                    }

                                    declare {
                                        variable("s", t("string")) {
                                            memberCall("toString", ref("sc"))
                                        }
                                    }
                                    memberCall("print", ref("sc")) { ref("s") }
                                    memberCall("print", ref("sc")) {
                                        memberCall("test", ref("sc", makeMagic = false))
                                    }
                                }
                            }
                        }
                    }
                }
            }

        fun getComplexDataflow(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage(TestLanguage("."))
                    .registerPass<EdgeCachePass>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("ComplexDataflow.java") {
                        record("Dataflow") {
                            field("logger", t("Logger")) {
                                // TODO: this field is static. How do we model this?
                                this.modifiers = listOf("static")
                                memberCall("getLogger", ref("Logger")) {
                                    literal("DataflowLogger", t("string"))
                                }
                            }

                            field("a", t("int")) {}

                            method("highlyCriticalOperation", void()) {
                                isStatic = true
                                param("s", t("string"))
                                body {
                                    memberCall("println", member("out", ref("System"))) { ref("s") }
                                    returnStmt {}
                                }
                            }

                            method("main", void()) {
                                isStatic = true
                                param("args", t("string").array())
                                body {
                                    declare {
                                        variable("sc", t("Dataflow")) {
                                            new { construct("Dataflow") }
                                        }
                                    }

                                    member("a", ref("sc")) assign literal(5, t("int"))

                                    memberCall(
                                        "highlyCriticalOperation",
                                        ref("Dataflow", t("Dataflow"), { refersTo = this@record })
                                    ) {
                                        this@memberCall.isStatic = true
                                        memberCall(
                                            "toString",
                                            ref("Integer", t("Integer"), makeMagic = false)
                                        ) {
                                            this.type = t("string")
                                            this@memberCall.isStatic = true
                                            isStatic = true
                                            member("a", ref("sc", makeMagic = false))
                                        }
                                    }

                                    memberCall("log", ref("logger")) {
                                        member("INFO", ref("Level", makeMagic = false))
                                        literal("put ", t("string")) +
                                            member("a", ref("a", makeMagic = false)) +
                                            literal(" into highlyCriticalOperation()", t("string"))
                                    }
                                    returnStmt {}
                                }
                            }
                        }
                    }
                }
            }

        fun getComplexDataflow2(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage(TestLanguage("."))
                    .registerPass<EdgeCachePass>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("ComplexDataflow2.java") {
                        record("Dataflow") {
                            field("logger", t("Logger")) {
                                // TODO: this field is static. How do we model this?
                                this.modifiers = listOf("static")
                                memberCall("getLogger", ref("Logger")) {
                                    literal("DataflowLogger", t("string"))
                                }
                            }

                            field("a", t("int")) {}

                            method("highlyCriticalOperation", void()) {
                                isStatic = true
                                param("s", t("string"))
                                body {
                                    memberCall("println", member("out", ref("System"))) { ref("s") }
                                    returnStmt {}
                                }
                            }

                            method("main", void()) {
                                isStatic = true
                                param("args", t("string").array())
                                body {
                                    declare {
                                        variable("sc", t("Dataflow")) {
                                            new { construct("Dataflow") }
                                        }
                                    }

                                    member("a", ref("sc")) assign literal(5, t("int"))

                                    memberCall("log", ref("logger")) {
                                        member("INFO", ref("Level", makeMagic = false))
                                        literal("put ", t("string")) +
                                            member("a", ref("a", makeMagic = false)) +
                                            literal(" into highlyCriticalOperation()", t("string"))
                                    }

                                    memberCall(
                                        "highlyCriticalOperation",
                                        ref("Dataflow", t("Dataflow"), { refersTo = this@record })
                                    ) {
                                        this@memberCall.isStatic = true
                                        memberCall(
                                            "toString",
                                            ref("Integer", t("Integer"), makeMagic = false)
                                        ) {
                                            this.type = t("string")
                                            this@memberCall.isStatic = true
                                            isStatic = true
                                            member("a", ref("sc", makeMagic = false))
                                        }
                                    }
                                    returnStmt {}
                                }
                            }
                        }
                    }
                }
            }

        fun getComplexDataflow3(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage(TestLanguage("."))
                    .registerPass<EdgeCachePass>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("ComplexDataflow3.java") {
                        record("Dataflow") {
                            field("logger", t("Logger")) {
                                // TODO: this field is static. How do we model this?
                                this.modifiers = listOf("static")
                                memberCall("getLogger", ref("Logger")) {
                                    literal("DataflowLogger", t("string"))
                                }
                            }

                            field("a", t("int")) {}

                            method("highlyCriticalOperation", void()) {
                                isStatic = true
                                param("s", t("string"))
                                body {
                                    memberCall("println", member("out", ref("System"))) { ref("s") }
                                    returnStmt {}
                                }
                            }

                            method("main", void()) {
                                isStatic = true
                                param("args", t("string").array())
                                body {
                                    declare {
                                        variable("sc", t("Dataflow")) {
                                            new { construct("Dataflow") }
                                        }
                                    }

                                    member("a", ref("sc")) assign literal(5, t("int"))

                                    memberCall("log", ref("logger")) {
                                        member("INFO", ref("Level", makeMagic = false))
                                        literal("put ", t("string")) +
                                            member("a", ref("a", makeMagic = false)) +
                                            literal(" into highlyCriticalOperation()", t("string"))
                                    }

                                    member("a", ref("sc")) assign literal(3, t("int"))

                                    memberCall(
                                        "highlyCriticalOperation",
                                        ref("Dataflow", t("Dataflow"), { refersTo = this@record })
                                    ) {
                                        this@memberCall.isStatic = true
                                        memberCall(
                                            "toString",
                                            ref("Integer", t("Integer"), makeMagic = false)
                                        ) {
                                            this.type = t("string")
                                            this@memberCall.isStatic = true
                                            isStatic = true
                                            member("a", ref("sc", makeMagic = false))
                                        }
                                    }
                                    returnStmt {}
                                }
                            }
                        }
                    }
                }
            }
    }
}
