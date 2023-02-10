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
package de.fraunhofer.aisec.cpg

import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.builder.*
import de.fraunhofer.aisec.cpg.graph.newVariableDeclaration
import java.io.File
import java.util.function.Consumer

class GraphExamples {
    companion object {
        fun getTRWithConfig(
            path: String,
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage(TestLanguage("."))
                    .build(),
            configModifier: Consumer<TranslationConfiguration.Builder>? = null
        ): TranslationResult {
            return when (path) {
                "src/test/resources/Dataflow.java" -> getDataflowClass(config)
                "src/test/resources/ShortcutClass.java" -> getShortcutClass(config)
                "src/test/resources/compiling/RecordDeclaration.java" -> getVisitorTest(config)
                else -> {
                    val builder =
                        TranslationConfiguration.builder()
                            .sourceLocations(File(path))
                            .loadIncludes(true)
                            .disableCleanup()
                            .debugParser(true)
                            .failOnError(true)
                            .typeSystemActiveInFrontend(false)
                            .useParallelFrontends(true)
                            .defaultLanguages()
                    configModifier?.accept(builder)
                    val configuration = builder.build()
                    val analyzer = TranslationManager.builder().config(configuration).build()
                    return analyzer.analyze().get()
                }
            }
        }
    }
}

private fun getVisitorTest(config: TranslationConfiguration) =
    TestLanguageFrontend(ScopeManager(), ".").buildTR {
        translationResult(config) {
            translationUnit("RecordDeclaration.java") {
                namespace("compiling") {
                    record("SimpleClass", "class") {
                        field("field", t("int")) {}
                        constructor {
                            receiver = newVariableDeclaration("this", t("SimpleClass"))
                            body { returnStmt { isImplicit = true } }
                        }
                        method("method", t("Integer")) {
                            receiver = newVariableDeclaration("this", t("SimpleClass"))
                            body {
                                call("System.out.println") { literal("Hello world") }
                                declare { variable("x", t("int")) { literal(0) } }
                                ifStmt {
                                    condition { call("System.currentTimeMillis") gt literal(0) }
                                    thenStmt { ref("x") assign { ref("x") + literal(1) } }
                                    elseStmt { ref("x") assign { ref("x") - literal(1) } }
                                }
                                returnStmt { ref("x") }
                            }
                        }
                    }
                }
            }
        }
    }

private fun getDataflowClass(config: TranslationConfiguration) =
    TestLanguageFrontend(ScopeManager(), ".").buildTR {
        translationResult(config) {
            translationUnit("Dataflow.java") {
                record("Dataflow") {
                    field("attr", t("String")) { literal("", t("String")) }
                    constructor() { isImplicit = true }
                    method("toString", t("String")) {
                        body { returnStmt { literal("ShortcutClass: attr=") + ref("attr") } }
                    }

                    method("test", t("String")) { body { returnStmt { literal("abcd") } } }

                    method("print", t("int")) {
                        param("s", t("String"))
                        body { call("System.out.println") { ref("s") } }
                    }

                    // The main method
                    method("main") {
                        param("args", t("String[]"))
                        body {
                            declare {
                                variable("sc", t("Dataflow")) { new { construct("Dataflow") } }
                            }
                            declare { variable("s", t("String")) { call("sc.toString") } }
                            call("sc.print") { ref("s") }
                            call("sc.print") { call("sc.toString") }
                        }
                    }
                }
            }
        }
    }

private fun getShortcutClass(config: TranslationConfiguration) =
    TestLanguageFrontend(ScopeManager(), ".").buildTR {
        translationResult(config) {
            translationUnit("ShortcutClass.java") {
                record("ShortcutClass") {
                    field("attr", t("int")) { literal(0, t("int")) }
                    constructor() { isImplicit = true }
                    method("toString", t("String")) {
                        body { returnStmt { literal("ShortcutClass: attr=") + ref("attr") } }
                    }

                    method("print", t("int")) {
                        body { call("System.out.println") { call("this.toString") } }
                    }

                    method("magic") {
                        param("b", t("int"))
                        body {
                            ifStmt {
                                condition { ref("b") eq literal(5, t("int")) }
                                thenStmt {
                                    ifStmt {
                                        condition { ref("attr") eq literal(2, t("int")) }
                                        thenStmt { ref("attr") assign literal(3, t("int")) }
                                        elseStmt { ref("attr") assign literal(3, t("int")) }
                                    }
                                }
                                elseStmt { ref("attr") assign ref("b") }
                            }
                        }
                    }

                    method("magic2") {
                        param("b", t("int"))
                        body {
                            declare { variable("a") }
                            ifStmt {
                                condition { ref("b") gt literal(5, t("int")) }
                                thenStmt {
                                    ifStmt {
                                        condition { ref("attr") eq literal(2, t("int")) }
                                        thenStmt { ref("a") assign literal(3, t("int")) }
                                        elseStmt { ref("a") assign literal(3, t("int")) }
                                    }
                                }
                                elseStmt { ref("a") assign ref("b") }
                            }
                        }
                    }

                    // The main method
                    method("main") {
                        param("args", t("int[]"))
                        body {
                            declare {
                                variable("sc", t("ShortcutClass")) {
                                    new { construct("ShortcutClass") }
                                }
                            }
                            call("sc.print")
                            call("sc.magic") { literal(3, t("int")) }
                            call("sc.magic2") { literal(5, t("int")) }
                        }
                    }
                }
            }
        }
    }
