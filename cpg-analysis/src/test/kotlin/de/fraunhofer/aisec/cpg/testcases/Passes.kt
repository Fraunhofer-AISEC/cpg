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
import de.fraunhofer.aisec.cpg.graph.builder.*
import de.fraunhofer.aisec.cpg.passes.UnreachableEOGPass

class Passes {
    companion object {
        fun getUnreachability(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerPass<UnreachableEOGPass>()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("Unreachability.java") {
                        import("kotlin.random.URandomKt")
                        record("TestClass") {
                            method("ifBothPossible", void()) {
                                body {
                                    declare { variable("y", t("int")) { literal(5, t("int")) } }
                                    declare {
                                        variable("x", t("int")) {
                                            memberCall("nextUInt", ref("URandomKt"))
                                        }
                                    }

                                    ifStmt {
                                        condition { ref("x") le ref("y") }
                                        thenStmt { ref("y").inc() }
                                        elseStmt { ref("y").dec() }
                                    }
                                    memberCall("println", member("out", ref("System"))) { ref("y") }
                                    returnStmt {}
                                }
                            }

                            method("ifTrue", void()) {
                                body {
                                    declare { variable("y", t("int")) { literal(6, t("int")) } }
                                    declare {
                                        variable("x", t("int")) {
                                            memberCall("nextUInt", ref("URandomKt"))
                                        }
                                    }

                                    ifStmt {
                                        condition { literal(true, t("boolean")) }
                                        thenStmt { ref("y").inc() }
                                        elseStmt { ref("y").dec() }
                                    }
                                    memberCall("println", member("out", ref("System"))) { ref("y") }
                                    returnStmt {}
                                }
                            }

                            method("ifFalse", void()) {
                                body {
                                    declare { variable("y", t("int")) { literal(6, t("int")) } }
                                    declare {
                                        variable("x", t("int")) {
                                            memberCall("nextUInt", ref("URandomKt"))
                                        }
                                    }

                                    ifStmt {
                                        condition { literal(false, t("boolean")) }
                                        thenStmt { ref("y").inc() }
                                        elseStmt { ref("y").dec() }
                                    }
                                    memberCall("println", member("out", ref("System"))) { ref("y") }
                                    returnStmt {}
                                }
                            }

                            method("ifTrueComputed", void()) {
                                body {
                                    declare { variable("y", t("int")) { literal(6, t("int")) } }
                                    declare {
                                        variable("x", t("int")) {
                                            memberCall("nextUInt", ref("URandomKt"))
                                        }
                                    }

                                    ifStmt {
                                        condition { ref("y") le literal(9, t("int")) }
                                        thenStmt { ref("y").inc() }
                                        elseStmt { ref("y").dec() }
                                    }
                                    memberCall("println", member("out", ref("System"))) { ref("y") }
                                    returnStmt {}
                                }
                            }

                            method("ifTrueComputedHard", void()) {
                                body {
                                    declare { variable("z", t("int")) { literal(2, t("int")) } }
                                    declare { variable("y", t("int")) { ref("z") } }
                                    declare {
                                        variable("x", t("int")) {
                                            memberCall("nextUInt", ref("URandomKt"))
                                        }
                                    }

                                    ifStmt {
                                        condition { ref("y") + ref("z") le literal(9, t("int")) }
                                        thenStmt { ref("y").inc() }
                                        elseStmt { ref("y").dec() }
                                    }
                                    ref("z") assign literal(10, t("int"))
                                    memberCall("println", member("out", ref("System"))) { ref("y") }
                                    returnStmt {}
                                }
                            }

                            method("ifFalseComputedHard", void()) {
                                body {
                                    declare { variable("z", t("int")) { literal(5, t("int")) } }
                                    declare { variable("y", t("int")) { ref("z") } }
                                    declare {
                                        variable("x", t("int")) {
                                            memberCall("nextUInt", ref("URandomKt"))
                                        }
                                    }

                                    ifStmt {
                                        condition { ref("y") + ref("z") le literal(9, t("int")) }
                                        thenStmt { ref("y").inc() }
                                        elseStmt { ref("y").dec() }
                                    }
                                    ref("z") assign literal(3, t("int"))
                                    memberCall("println", member("out", ref("System"))) { ref("y") }
                                    returnStmt {}
                                }
                            }

                            method("ifFalseComputed", void()) {
                                body {
                                    declare { variable("y", t("int")) { literal(6, t("int")) } }
                                    declare {
                                        variable("x", t("int")) {
                                            memberCall("nextUInt", ref("URandomKt"))
                                        }
                                    }

                                    ifStmt {
                                        condition { ref("y") le literal(-1, t("int")) }
                                        thenStmt { ref("y").inc() }
                                        elseStmt { ref("y").dec() }
                                    }
                                    memberCall("println", member("out", ref("System"))) { ref("y") }
                                    returnStmt {}
                                }
                            }

                            method("whileTrueEndless", void()) {
                                body {
                                    declare {
                                        variable("x", t("boolean")) { literal(true, t("boolean")) }
                                    }

                                    whileStmt {
                                        whileCondition { ref("x") }
                                        loopBody {
                                            memberCall("println", member("out", ref("System"))) {
                                                literal("Cool loop", t("string"))
                                            }
                                        }
                                    }

                                    memberCall("println", member("out", ref("System"))) {
                                        literal("After cool loop", t("string"))
                                    }
                                    returnStmt {}
                                }
                            }

                            method("whileTrue", void()) {
                                body {
                                    declare {
                                        variable("x", t("boolean")) { literal(true, t("boolean")) }
                                    }

                                    whileStmt {
                                        whileCondition { ref("x") }
                                        loopBody {
                                            memberCall("println", member("out", ref("System"))) {
                                                literal("Cool loop", t("string"))
                                            }
                                            ref("x") assign literal(false, t("boolean"))
                                        }
                                    }

                                    memberCall("println", member("out", ref("System"))) {
                                        literal("After cool loop", t("string"))
                                    }
                                    returnStmt {}
                                }
                            }

                            method("whileFalse", void()) {
                                body {
                                    whileStmt {
                                        whileCondition { literal(false, t("boolean")) }
                                        loopBody {
                                            memberCall("println", member("out", ref("System"))) {
                                                literal("Cool loop", t("string"))
                                            }
                                        }
                                    }

                                    memberCall("println", member("out", ref("System"))) {
                                        literal("After cool loop", t("string"))
                                    }
                                    returnStmt {}
                                }
                            }

                            method("whileComputedTrue", void()) {
                                body {
                                    declare { variable("x", t("boolean")) { literal(1, t("int")) } }

                                    whileStmt {
                                        whileCondition { ref("x") le literal(2, t("int")) }
                                        loopBody {
                                            memberCall("println", member("out", ref("System"))) {
                                                literal("Cool loop", t("string"))
                                            }
                                        }
                                    }

                                    memberCall("println", member("out", ref("System"))) {
                                        literal("After cool loop", t("string"))
                                    }
                                    returnStmt {}
                                }
                            }

                            method("whileComputedFalse", void()) {
                                body {
                                    declare { variable("x", t("boolean")) { literal(1, t("int")) } }

                                    whileStmt {
                                        whileCondition { ref("x") gt literal(3, t("int")) }
                                        loopBody {
                                            memberCall("println", member("out", ref("System"))) {
                                                literal("Cool loop", t("string"))
                                            }
                                        }
                                    }

                                    memberCall("println", member("out", ref("System"))) {
                                        literal("After cool loop", t("string"))
                                    }
                                    returnStmt {}
                                }
                            }

                            method("whileUnknown", void()) {
                                body {
                                    declare {
                                        variable("y", t("int")) {
                                            memberCall("nextUInt", ref("URandomKt"))
                                        }
                                    }

                                    whileStmt {
                                        whileCondition { ref("y") le literal(2, t("int")) }
                                        loopBody {
                                            memberCall("println", member("out", ref("System"))) {
                                                literal("Cool loop", t("string"))
                                            }
                                            ref("y") assign memberCall("nextUInt", ref("URandomKt"))
                                        }
                                    }

                                    memberCall("println", member("out", ref("System"))) {
                                        literal("After cool loop", t("string"))
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
