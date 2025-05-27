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
package de.fraunhofer.aisec.cpg.testcases

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.testFrontend
import de.fraunhofer.aisec.cpg.graph.array
import de.fraunhofer.aisec.cpg.graph.builder.assign
import de.fraunhofer.aisec.cpg.graph.builder.body
import de.fraunhofer.aisec.cpg.graph.builder.call
import de.fraunhofer.aisec.cpg.graph.builder.condition
import de.fraunhofer.aisec.cpg.graph.builder.declare
import de.fraunhofer.aisec.cpg.graph.builder.elseStmt
import de.fraunhofer.aisec.cpg.graph.builder.eq
import de.fraunhofer.aisec.cpg.graph.builder.function
import de.fraunhofer.aisec.cpg.graph.builder.ifStmt
import de.fraunhofer.aisec.cpg.graph.builder.literal
import de.fraunhofer.aisec.cpg.graph.builder.param
import de.fraunhofer.aisec.cpg.graph.builder.plus
import de.fraunhofer.aisec.cpg.graph.builder.plusAssign
import de.fraunhofer.aisec.cpg.graph.builder.ref
import de.fraunhofer.aisec.cpg.graph.builder.returnStmt
import de.fraunhofer.aisec.cpg.graph.builder.t
import de.fraunhofer.aisec.cpg.graph.builder.thenStmt
import de.fraunhofer.aisec.cpg.graph.builder.translationResult
import de.fraunhofer.aisec.cpg.graph.builder.translationUnit
import de.fraunhofer.aisec.cpg.graph.builder.variable
import de.fraunhofer.aisec.cpg.graph.builder.void
import de.fraunhofer.aisec.cpg.passes.ControlDependenceGraphPass
import de.fraunhofer.aisec.cpg.passes.ProgramDependenceGraphPass

class FlowQueriesTest {

    companion object {
        fun verySimpleDataflow(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerPass<ControlDependenceGraphPass>()
                    .registerPass<ProgramDependenceGraphPass>()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("Dataflow.java") {
                        function("foo", t("string")) {
                            param("arg", t("int"))
                            body { returnStmt { call("toString") { ref("arg") } } }
                        }

                        function("main", void()) {
                            param("args", t("string").array())
                            body {
                                declare { variable("a", t("int")) { literal(5, t("int")) } }

                                declare {
                                    variable("b", t("string")) {
                                        literal("bla", t("string")) +
                                            call("foo") { ref("a") } +
                                            call("foo") { call("bar") }
                                    }
                                }
                                call("print") { ref("a") }

                                call("print") { ref("b") }

                                ref("b") += literal("added", t("string"))

                                ifStmt {
                                    condition { ref("b") eq literal("test", t("string")) }
                                    thenStmt { ref("a") assign literal(10, t("int")) }
                                    elseStmt { ref("b") assign literal("removed", t("string")) }
                                }

                                call("baz") { ref("a") + ref("b") }
                            }
                        }
                    }
                }
            }

        fun validatorDataflowLinear(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("Dataflow.java") {
                        function("foo", t("string")) {
                            param("arg", t("int"))
                            body { returnStmt { call("toString") { ref("arg") } } }
                        }

                        function("main", void()) {
                            param("args", t("string").array())
                            body {
                                declare { variable("a", t("int")) { literal(5, t("int")) } }

                                declare {
                                    variable("b", t("string")) {
                                        literal("bla", t("string")) +
                                            ref("a") +
                                            call("foo") { call("bar") }
                                    }
                                }
                                call("print") { ref("b") }

                                call("baz") { ref("a") + ref("b") }
                            }
                        }
                    }
                }
            }

        fun validatorDataflowIf(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("Dataflow.java") {
                        function("foo", t("string")) {
                            param("arg", t("int"))
                            body { returnStmt { call("toString") { ref("arg") } } }
                        }

                        function("main", void()) {
                            param("args", t("string").array())
                            body {
                                declare { variable("a", t("int")) { literal(5, t("int")) } }

                                declare {
                                    variable("b", t("string")) {
                                        literal("bla", t("string")) +
                                            ref("a") +
                                            call("foo") { call("bar") }
                                    }
                                }

                                ifStmt {
                                    condition { ref("b") eq literal("test", t("string")) }
                                    thenStmt { call("print") { ref("a") } }
                                }
                                call("print") { ref("b") }

                                call("baz") { ref("a") + ref("b") }
                            }
                        }
                    }
                }
            }

        fun validatorDataflowIfElse(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("Dataflow.java") {
                        function("foo", t("string")) {
                            param("arg", t("int"))
                            body { returnStmt { call("toString") { ref("arg") } } }
                        }

                        function("main", void()) {
                            param("args", t("string").array())
                            body {
                                declare { variable("a", t("int")) { literal(5, t("int")) } }

                                declare {
                                    variable("b", t("string")) {
                                        literal("bla", t("string")) +
                                            ref("a") +
                                            call("foo") { call("bar") }
                                    }
                                }

                                ifStmt {
                                    condition { ref("b") eq literal("test", t("string")) }
                                    thenStmt { call("print") { ref("a") } }
                                    elseStmt { call("print") { ref("b") } }
                                    call("print") { ref("b") }
                                }

                                call("baz") { ref("a") + ref("b") }
                            }
                        }
                    }
                }
            }

        fun validatorDataflowLinearSimple(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("Dataflow.java") {
                        function("foo", t("string")) {
                            param("arg", t("int"))
                            body { returnStmt { call("toString") { ref("arg") } } }
                        }

                        function("main", void()) {
                            param("args", t("string").array())
                            body {
                                declare { variable("a", t("int")) { literal(5, t("int")) } }

                                declare {
                                    variable("b", t("string")) {
                                        literal("bla", t("string")) + call("foo") { call("bar") }
                                    }
                                }
                                call("print") { ref("a") }

                                call("baz") { ref("a") + ref("b") }
                            }
                        }
                    }
                }
            }

        fun validatorDataflowIfSimple(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("Dataflow.java") {
                        function("foo", t("string")) {
                            param("arg", t("int"))
                            body { returnStmt { call("toString") { ref("arg") } } }
                        }

                        function("main", void()) {
                            param("args", t("string").array())
                            body {
                                declare { variable("a", t("int")) { literal(5, t("int")) } }

                                declare {
                                    variable("b", t("string")) {
                                        literal("bla", t("string")) + call("foo") { call("bar") }
                                    }
                                }

                                ifStmt {
                                    condition { ref("b") eq literal("test", t("string")) }
                                    thenStmt { call("print") { ref("a") } }
                                }

                                call("baz") { ref("a") + ref("b") }
                            }
                        }
                    }
                }
            }

        fun validatorDataflowIfElseSimple(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("Dataflow.java") {
                        function("foo", t("string")) {
                            param("arg", t("int"))
                            body { returnStmt { call("toString") { ref("arg") } } }
                        }

                        function("main", void()) {
                            param("args", t("string").array())
                            body {
                                declare { variable("a", t("int")) { literal(5, t("int")) } }

                                declare {
                                    variable("b", t("string")) {
                                        literal("bla", t("string")) + call("foo") { call("bar") }
                                    }
                                }

                                ifStmt {
                                    condition { ref("b") eq literal("test", t("string")) }
                                    thenStmt { call("print") { ref("a") } }
                                    elseStmt { call("print") { ref("a") } }
                                }

                                call("baz") { ref("a") + ref("b") }
                            }
                        }
                    }
                }
            }

        fun validatorDataflowOnlyIfSink(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("Dataflow.java") {
                        function("foo", t("string")) {
                            param("arg", t("int"))
                            body { returnStmt { call("toString") { ref("arg") } } }
                        }

                        function("main", void()) {
                            param("args", t("string").array())
                            body {
                                declare { variable("a", t("int")) { literal(5, t("int")) } }

                                declare {
                                    variable("b", t("string")) {
                                        literal("bla", t("string")) + call("foo") { call("bar") }
                                    }
                                }

                                ifStmt {
                                    condition { ref("b") eq literal("test", t("string")) }
                                    thenStmt {
                                        call("print") { ref("a") }
                                        call("baz") { ref("a") + ref("b") }
                                    }
                                    elseStmt { call("print") { ref("c") } }
                                }
                            }
                        }
                    }
                }
            }

        fun validatorDataflowLinearWithCall(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("Dataflow.java") {
                        function("foo", t("string")) {
                            param("arg", t("int"))
                            body { returnStmt { call("toString") { ref("arg") } } }
                        }

                        function("main", void()) {
                            param("args", t("string").array())
                            body {
                                declare { variable("a", t("int")) { literal(5, t("int")) } }

                                declare {
                                    variable("b", t("string")) {
                                        literal("bla", t("string")) +
                                            call("foo") { ref("a") } +
                                            call("foo") { call("bar") }
                                    }
                                }
                                call("print") { ref("b") }

                                call("baz") { ref("a") + ref("b") }
                            }
                        }
                    }
                }
            }

        fun validatorDataflowIfWithCall(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("Dataflow.java") {
                        function("foo", t("string")) {
                            param("arg", t("int"))
                            body { returnStmt { call("toString") { ref("arg") } } }
                        }

                        function("main", void()) {
                            param("args", t("string").array())
                            body {
                                declare { variable("a", t("int")) { literal(5, t("int")) } }

                                declare {
                                    variable("b", t("string")) {
                                        literal("bla", t("string")) +
                                            call("foo") { ref("a") } +
                                            call("foo") { call("bar") }
                                    }
                                }

                                ifStmt {
                                    condition { ref("b") eq literal("test", t("string")) }
                                    thenStmt { call("print") { ref("a") } }
                                }
                                call("print") { ref("b") }

                                call("baz") { ref("a") + ref("b") }
                            }
                        }
                    }
                }
            }

        fun validatorDataflowIfElseWithCall(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("Dataflow.java") {
                        function("foo", t("string")) {
                            param("arg", t("int"))
                            body { returnStmt { call("toString") { ref("arg") } } }
                        }

                        function("main", void()) {
                            param("args", t("string").array())
                            body {
                                declare { variable("a", t("int")) { literal(5, t("int")) } }

                                declare {
                                    variable("b", t("string")) {
                                        literal("bla", t("string")) +
                                            call("foo") { ref("a") } +
                                            call("foo") { call("bar") }
                                    }
                                }

                                ifStmt {
                                    condition { ref("b") eq literal("test", t("string")) }
                                    thenStmt { call("print") { ref("a") } }
                                    elseStmt { call("print") { ref("b") } }
                                    call("print") { ref("b") }
                                }

                                call("baz") { ref("a") + ref("b") }
                            }
                        }
                    }
                }
            }
    }
}
