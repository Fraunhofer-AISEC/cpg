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
package de.fraunhofer.aisec.cpg.test

import de.fraunhofer.aisec.cpg.InferenceConfiguration
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.ClassTestLanguage
import de.fraunhofer.aisec.cpg.frontends.StructTestLanguage
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.testFrontend
import de.fraunhofer.aisec.cpg.graph.autoType
import de.fraunhofer.aisec.cpg.graph.builder.*
import de.fraunhofer.aisec.cpg.graph.newInitializerList
import de.fraunhofer.aisec.cpg.graph.newVariable
import de.fraunhofer.aisec.cpg.graph.types.PointerType
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.net.URI

class GraphExamples {
    companion object {
        fun getInitializerListExprDFG(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("initializerListExprDFG.cpp") {
                        function("foo", t("int")) { body { returnStmt { literal(0, t("int")) } } }
                        function("main", t("int")) {
                            body {
                                declare {
                                    variable("i", t("int")) {
                                        val initList = newInitializerList()
                                        initList.initializers = mutableListOf(call("foo"))
                                        initializer = initList
                                    }
                                }
                                returnStmt { ref("i") }
                            }
                        }
                    }
                }
            }

        fun getWhileWithElseAndBreak(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("whileWithBreakAndElse.py") {
                        record("someRecord") {
                            method("func") {
                                body {
                                    whileStmt {
                                        whileCondition { literal(true, t("bool")) }
                                        loopBody {
                                            ifStmt {
                                                condition { literal(true, t("bool")) }
                                                thenStmt { breakStmt() }
                                            }
                                            call("postIf")
                                        }
                                        loopElseStmt { call("elseCall") }
                                    }
                                    call("postWhile")
                                    whileStmt {
                                        whileCondition { literal(true, t("bool")) }
                                        loopBody {
                                            ifStmt {
                                                condition { literal(true, t("bool")) }
                                                thenStmt { breakStmt() }
                                            }
                                            call("postIf")
                                        }
                                        loopElseStmt { call("elseCall") }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        fun getDoWithElseAndBreak(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("whileWithBreakAndElse.py") {
                        record("someRecord") {
                            method("func") {
                                body {
                                    doStmt {
                                        doCondition { literal(true, t("bool")) }
                                        loopBody {
                                            ifStmt {
                                                condition { literal(true, t("bool")) }
                                                thenStmt { breakStmt() }
                                            }
                                            call("postIf")
                                        }
                                        loopElseStmt { call("elseCall") }
                                    }
                                    call("postDo")
                                    doStmt {
                                        doCondition { literal(true, t("bool")) }
                                        loopBody {
                                            ifStmt {
                                                condition { literal(true, t("bool")) }
                                                thenStmt { breakStmt() }
                                            }
                                            call("postIf")
                                        }
                                        loopElseStmt { call("elseCall") }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        fun getForWithElseAndBreak(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("whileWithBreakAndElse.py") {
                        record("someRecord") {
                            method("func") {
                                body {
                                    forStmt {
                                        loopBody {
                                            ifStmt {
                                                condition { literal(true, t("bool")) }
                                                thenStmt { breakStmt() }
                                            }
                                            call("postIf")
                                        }
                                        forInitializer {
                                            declareVar("a", t("int")) { literal(0, t("int")) }
                                        }
                                        forCondition { literal(true, t("bool")) }
                                        forIteration { ref("a").inc() }
                                        loopElseStmt { call("elseCall") }
                                    }
                                    call("postFor")
                                    forStmt {
                                        loopBody {
                                            ifStmt {
                                                condition { literal(true, t("bool")) }
                                                thenStmt { breakStmt() }
                                            }
                                            call("postIf")
                                        }
                                        forInitializer {
                                            declareVar("a", t("int")) { literal(0, t("int")) }
                                        }
                                        forCondition { literal(true, t("bool")) }
                                        forIteration { ref("a").inc() }
                                        loopElseStmt { call("elseCall") }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        fun getForEachWithElseAndBreak(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("whileWithBreakAndElse.py") {
                        record("someRecord") {
                            method("func") {
                                body {
                                    forEachStmt {
                                        iterable { call("listOf") }
                                        variable { declare { variable("a") } }
                                        loopBody {
                                            ifStmt {
                                                condition { literal(true, t("bool")) }
                                                thenStmt { breakStmt() }
                                            }
                                            call("postIf")
                                        }
                                        loopElseStmt { call("elseCall") }
                                    }
                                    call("postForEach")
                                    forEachStmt {
                                        iterable { call("listOf") }
                                        variable { declare { variable("a") } }
                                        loopBody {
                                            ifStmt {
                                                condition { literal(true, t("bool")) }
                                                thenStmt { breakStmt() }
                                            }
                                            call("postIf")
                                        }
                                        loopElseStmt { call("elseCall") }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        fun getNestedComprehensions(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("whileWithBreakAndElse.py") {
                        record("someRecord") {
                            method("func") {
                                body {
                                    call("preComprehensions")
                                    listComp {
                                        ref("i")
                                        compExpr {
                                            ref("i")
                                            ref("someIterable")
                                        }
                                        compExpr {
                                            ref("j")
                                            ref("i")
                                            ref("j") gt literal(5, t("int"))
                                        }
                                    }
                                    call("postComprehensions")
                                }
                            }
                        }
                    }
                }
            }

        fun getInferenceRecordPtr(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<StructTestLanguage>()
                    .inferenceConfiguration(
                        InferenceConfiguration.builder().inferRecords(true).build()
                    )
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("record.cpp") {
                        // The main method
                        function("main", t("int")) {
                            body {
                                declare {
                                    variable(
                                        "node",
                                        t("T").reference(PointerType.PointerOrigin.POINTER),
                                    )
                                }
                                member("value", ref("node"), "->") assign literal(42, t("int"))
                                member("next", ref("node"), "->") assign ref("node")
                                memberCall(
                                    "dump",
                                    ref("node"),
                                ) // TODO: Do we have to encode the "->" here?
                                returnStmt { isImplicit = true }
                            }
                        }
                    }
                }
            }

        fun getInferenceRecord(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<StructTestLanguage>()
                    .inferenceConfiguration(
                        InferenceConfiguration.builder().inferRecords(true).build()
                    )
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("record.cpp") {
                        // The main method
                        function("main", t("int")) {
                            body {
                                declare { variable("node", t("T")) }
                                member("value", ref("node")) assign literal(42, t("int"))
                                member("next", ref("node")) assign { reference(ref("node")) }
                                returnStmt { isImplicit = true }
                            }
                        }
                    }
                }
            }

        fun getInferenceBinaryOperatorReturnType(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<StructTestLanguage>()
                    .inferenceConfiguration(
                        InferenceConfiguration.builder()
                            .inferRecords(true)
                            .inferReturnTypes(true)
                            .build()
                    )
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("test.python") {
                        function("foo", t("int")) {
                            body {
                                declare { variable("a") }
                                declare { variable("b") }
                                ref("a") assign { call("bar") + literal(2, t("int")) }
                                ref("b") assign { literal(2L, t("long")) + call("baz") }
                            }
                        }
                    }
                }
            }

        fun getInferenceTupleReturnType(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<StructTestLanguage>()
                    .inferenceConfiguration(
                        InferenceConfiguration.builder()
                            .inferRecords(true)
                            .inferReturnTypes(true)
                            .build()
                    )
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("test.python") {
                        function("foo", returnTypes = listOf(t("Foo"), t("Bar"))) {
                            body { returnStmt { call("bar") } }
                        }
                    }
                }
            }

        fun getInferenceUnaryOperatorReturnType(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<StructTestLanguage>()
                    .inferenceConfiguration(
                        InferenceConfiguration.builder()
                            .inferRecords(true)
                            .inferReturnTypes(true)
                            .build()
                    )
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("Test.java") {
                        record("Test") { method("foo") { body { returnStmt { -call("bar") } } } }
                    }
                }
            }

        fun getInferenceNestedNamespace(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<ClassTestLanguage>()
                    .inferenceConfiguration(
                        InferenceConfiguration.builder()
                            .inferRecords(true)
                            .inferNamespaces(true)
                            .build()
                    )
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("Test.java") {
                        record("Test") {
                            method("foo") {
                                body {
                                    declare { variable("node", t("java.lang.String")) }
                                    returnStmt { isImplicit = true }
                                }
                            }
                        }
                    }
                }
            }

        fun getVariables(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("Variables.java") {
                        record("Variables") {
                            field("field", t("int")) {
                                literal(42, t("int"))
                                modifiers = setOf("private")
                            }
                            method("getField", t("int")) {
                                receiver = newVariable("this", t("Variables"))
                                body { returnStmt { member("field") } }
                            }
                            method("getLocal", t("int")) {
                                receiver = newVariable("this", t("Variables"))
                                body {
                                    declare {
                                        variable("local", t("int")) { literal(42, t("int")) }
                                    }
                                    returnStmt { ref("local") }
                                }
                            }
                            method("getShadow", t("int")) {
                                receiver = newVariable("this", t("Variables"))
                                body {
                                    declare {
                                        variable("field", t("int")) { literal(43, t("int")) }
                                    }
                                    returnStmt { ref("field") }
                                }
                            }
                            method("getNoShadow", t("int")) {
                                receiver = newVariable("this", t("Variables"))
                                body {
                                    declare {
                                        variable("field", t("int")) { literal(43, t("int")) }
                                    }
                                    returnStmt { member("field", ref("this")) }
                                }
                            }
                        }
                    }
                }
            }

        fun getUnaryOperator(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("unaryoperator.cpp") {
                        // The main method
                        function("somefunc") {
                            body {
                                declare { variable("i", t("int")) { literal(0, t("int")) } }
                                ref("i").inc()
                                returnStmt { isImplicit = true }
                            }
                        }
                    }
                }
            }

        fun getCompoundOperator(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("compoundoperator.cpp") {
                        // The main method
                        function("somefunc") {
                            body {
                                declare { variable("i", t("int")) { literal(0, t("int")) } }
                                ref("i") plusAssign literal(0, t("int"))
                                returnStmt { isImplicit = true }
                            }
                        }
                    }
                }
            }

        fun getConditional(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("conditional_expression.cpp") {
                        // The main method
                        function("main", t("int")) {
                            body {
                                declare { variable("a", t("int")) { literal(0, t("int")) } }
                                declare { variable("b", t("int")) { literal(1, t("int")) } }

                                ref("a") {
                                    location =
                                        PhysicalLocation(
                                            URI("conditional_expression.cpp"),
                                            Region(5, 3, 5, 4),
                                        )
                                } assign
                                    {
                                        conditional(
                                            ref("a") {
                                                location =
                                                    PhysicalLocation(
                                                        URI("conditional_expression.cpp"),
                                                        Region(5, 7, 5, 8),
                                                    )
                                            } eq
                                                ref("b") {
                                                    location =
                                                        PhysicalLocation(
                                                            URI("conditional_expression.cpp"),
                                                            Region(5, 12, 5, 13),
                                                        )
                                                },
                                            ref("b") {
                                                location =
                                                    PhysicalLocation(
                                                        URI("conditional_expression.cpp"),
                                                        Region(5, 16, 5, 17),
                                                    )
                                            } assignAsExpr { literal(2, t("int")) },
                                            ref("b") {
                                                location =
                                                    PhysicalLocation(
                                                        URI("conditional_expression.cpp"),
                                                        Region(5, 23, 5, 24),
                                                    )
                                            } assignAsExpr { literal(3, t("int")) },
                                        )
                                    }
                                ref("a") {
                                    location =
                                        PhysicalLocation(
                                            URI("conditional_expression.cpp"),
                                            Region(6, 3, 6, 4),
                                        )
                                } assign
                                    ref("b") {
                                        location =
                                            PhysicalLocation(
                                                URI("conditional_expression.cpp"),
                                                Region(6, 7, 6, 8),
                                            )
                                    }
                                returnStmt { isImplicit = true }
                            }
                        }
                    }
                }
            }

        fun getBasicSlice(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("BasicSlice.java") {
                        record("BasicSlice") {
                            // The main method
                            method("main") {
                                this.isStatic = true
                                param("args", t("String[]"))
                                body {
                                    declare { variable("a", t("int")) { literal(0, t("int")) } }
                                    declare {
                                        variable("b", t("int")) { literal(1, t("int")) }
                                        variable("c", t("int")) { literal(0, t("int")) }
                                        variable("d", t("int")) { literal(0, t("int")) }
                                    }
                                    declare {
                                        variable("sunShines", t("boolean")) {
                                            literal(true, t("boolean"))
                                        }
                                    }

                                    ifStmt {
                                        condition { ref("a") gt literal(0, t("int")) }
                                        thenStmt {
                                            ref("d") assign literal(5, t("int"))
                                            ref("c") assign literal(2, t("int"))
                                            ifStmt {
                                                condition { ref("b") gt literal(0, t("int")) }
                                                thenStmt {
                                                    ref("d") assign ref("a") * literal(2, t("int"))
                                                    ref("a") assign
                                                        ref("a") + ref("d") * literal(2, t("int"))
                                                }
                                                elseIf {
                                                    condition { ref("b") lt literal(-2, t("int")) }
                                                    thenStmt {
                                                        ref("a") assign
                                                            ref("a") - literal(10, t("int"))
                                                    }
                                                }
                                            }
                                        }
                                        elseStmt {
                                            ref("b") assign literal(-2, t("int"))
                                            ref("d") assign literal(-2, t("int"))
                                            ref("a").dec()
                                        }
                                    }

                                    ref("a") assign { ref("a") + ref("b") }

                                    switchStmt(ref("sunShines")) {
                                        switchBody {
                                            case(
                                                ref("True")
                                            ) // No idea why it was "True" and not "true". Bug? On
                                            // purpose? I just keep it
                                            ref("a") assign { ref("a") * literal(2, t("int")) }
                                            ref("c") assign literal(-2, t("int"))
                                            breakStmt()
                                            case(
                                                ref("False")
                                            ) // No idea why it was "False" and not "false". Bug? On
                                            // purpose? I just keep it
                                            ref("a") assign literal(290, t("int"))
                                            ref("d") assign literal(-2, t("int"))
                                            ref("b") assign literal(-2, t("int"))
                                            breakStmt()
                                        }
                                    }

                                    returnStmt { isImplicit = true }
                                }
                            }
                        }
                    }
                }
            }

        fun getControlFlowSensitiveDFGIfMerge(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("ControlFlowSensitiveDFGIfMerge.java") {
                        record("ControlFlowSensitiveDFGIfMerge") {
                            field("bla", t("int")) {}
                            constructor {
                                isImplicit = true
                                receiver = newVariable("this", t("ControlFlowSensitiveDFGIfMerge"))
                                body { returnStmt { isImplicit = true } }
                            }
                            method("func") {
                                receiver = newVariable("this", t("ControlFlowSensitiveDFGIfMerge"))
                                param("args", t("int[]"))
                                body {
                                    declare { variable("a", t("int")) { literal(1, t("int")) } }
                                    ifStmt {
                                        condition {
                                            member("length", ref("args")) gt literal(3, t("int"))
                                        }
                                        thenStmt { ref("a") assign literal(2, t("int")) }
                                        elseStmt {
                                            memberCall(
                                                "println",
                                                member(
                                                    "out",
                                                    ref("System") { isStaticAccess = true },
                                                ),
                                            ) {
                                                ref("a")
                                            }
                                        }
                                    }

                                    declare { variable("b", t("int")) { ref("a") } }
                                    returnStmt { isImplicit = true }
                                }
                            }

                            // The main method
                            method("main") {
                                this.isStatic = true
                                param("args", t("String[]"))
                                body {
                                    declare {
                                        variable("obj", t("ControlFlowSensitiveDFGIfMerge")) {
                                            new { construct("ControlFlowSensitiveDFGIfMerge") }
                                        }
                                    }
                                    member("bla", ref("obj")) assign literal(3, t("int"))
                                    returnStmt { isImplicit = true }
                                }
                            }
                        }
                    }
                }
            }

        fun getControlFlowSesitiveDFGSwitch(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("ControlFlowSesitiveDFGSwitch.java") {
                        record("ControlFlowSesitiveDFGSwitch") {
                            // The main method
                            method("func3") {
                                receiver = newVariable("this", t("ControlFlowSesitiveDFGSwitch"))
                                body {
                                    declare {
                                        variable("switchVal", t("int")) { literal(3, t("int")) }
                                    }
                                    declare { variable("a", t("int")) { literal(0, t("int")) } }
                                    switchStmt(ref("switchVal")) {
                                        switchBody {
                                            case(literal(1, t("int")))
                                            ref("a") {
                                                location =
                                                    PhysicalLocation(
                                                        URI("ControlFlowSesitiveDFGSwitch.java"),
                                                        Region(8, 9, 8, 10),
                                                    )
                                            } assign literal(10, t("int"))
                                            breakStmt()
                                            case(literal(2, t("int")))
                                            ref("a") {
                                                location =
                                                    PhysicalLocation(
                                                        URI("ControlFlowSesitiveDFGSwitch.java"),
                                                        Region(11, 9, 11, 10),
                                                    )
                                            } assign literal(11, t("int"))
                                            breakStmt()
                                            case(literal(3, t("int")))
                                            ref("a") {
                                                location =
                                                    PhysicalLocation(
                                                        URI("ControlFlowSesitiveDFGSwitch.java"),
                                                        Region(14, 9, 14, 10),
                                                    )
                                            } assign literal(12, t("int"))
                                            default()
                                            memberCall(
                                                "println",
                                                member(
                                                    "out",
                                                    ref("System") { isStaticAccess = true },
                                                ),
                                            ) {
                                                ref("a")
                                            }
                                            breakStmt()
                                        }
                                    }

                                    declare { variable("b", t("int")) { ref("a") } }
                                    returnStmt { isImplicit = true }
                                }
                            }
                        }
                    }
                }
            }

        fun getControlFlowSensitiveDFGIfNoMerge(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("ControlFlowSensitiveDFGIfNoMerge.java") {
                        record("ControlFlowSensitiveDFGIfNoMerge") {
                            // The main method
                            method("func2") {
                                receiver =
                                    newVariable("this", t("ControlFlowSensitiveDFGIfNoMerge"))
                                body {
                                    declare { variable("a", t("int")) { literal(1, t("int")) } }
                                    ifStmt {
                                        condition {
                                            member("length", ref("args")) gt literal(3, t("int"))
                                        }
                                        thenStmt { ref("a") assign literal(2, t("int")) }
                                        elseStmt {
                                            ref("a") assign literal(4, t("int"))
                                            declare { variable("b", t("int")) { ref("a") } }
                                        }
                                    }
                                    returnStmt { isImplicit = true }
                                }
                            }
                        }
                    }
                }
            }

        fun getLabeledBreakContinueLoopDFG(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("LoopDFGs.java") {
                        record("LoopDFGs") {
                            // The main method
                            method("labeledBreakContinue") {
                                receiver = newVariable("this", t("LoopDFGs"))
                                param("param", t("int"))
                                body {
                                    declare { variable("a", t("int")) { literal(0, t("int")) } }
                                    label("lab1") {
                                        whileStmt {
                                            whileCondition { ref("param") lt literal(5, t("int")) }
                                            loopBody {
                                                whileStmt {
                                                    whileCondition {
                                                        ref("param") gt literal(6, t("int"))
                                                    }
                                                    loopBody {
                                                        ifStmt {
                                                            condition {
                                                                ref("param") gt literal(7, t("int"))
                                                            }
                                                            thenStmt {
                                                                ref("a") assign literal(1, t("int"))
                                                                continueStmt("lab1")
                                                            }
                                                            elseStmt {
                                                                memberCall(
                                                                    "println",
                                                                    member(
                                                                        "out",
                                                                        ref("System") {
                                                                            isStaticAccess = true
                                                                        },
                                                                    ),
                                                                ) {
                                                                    ref("a")
                                                                }
                                                                ref("a") assign literal(2, t("int"))
                                                                breakStmt("lab1")
                                                            }
                                                        }
                                                        ref("a") assign literal(4, t("int"))
                                                    }
                                                }
                                                memberCall(
                                                    "println",
                                                    member(
                                                        "out",
                                                        ref("System") { isStaticAccess = true },
                                                    ),
                                                ) {
                                                    ref("a")
                                                }
                                                ref("a") assign literal(3, t("int"))
                                            }
                                        }
                                    }

                                    memberCall(
                                        "println",
                                        member("out", ref("System") { isStaticAccess = true }),
                                    ) {
                                        ref("a")
                                    }
                                    returnStmt { isImplicit = true }
                                }
                            }
                        }
                    }
                }
            }

        fun getLoopingDFG(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("LoopDFGs.java") {
                        record("LoopDFGs") {
                            // The main method
                            method("looping") {
                                receiver = newVariable("this", t("LoopDFGs"))
                                param("param", t("int"))
                                body {
                                    declare { variable("a", t("int")) { literal(0, t("int")) } }
                                    whileStmt {
                                        whileCondition {
                                            (ref("param") % literal(6, t("int"))) eq
                                                literal(5, t("int"))
                                        }
                                        loopBody {
                                            ifStmt {
                                                condition { ref("param") gt literal(7, t("int")) }
                                                thenStmt { ref("a") assign literal(1, t("int")) }
                                                elseStmt {
                                                    memberCall(
                                                        "println",
                                                        member(
                                                            "out",
                                                            ref("System") { isStaticAccess = true },
                                                        ),
                                                    ) {
                                                        ref("a")
                                                    }
                                                    ref("a") assign literal(2, t("int"))
                                                }
                                            }
                                        }
                                    }

                                    ref("a") assign { literal(3, t("int")) }
                                    returnStmt { isImplicit = true }
                                }
                            }
                        }
                    }
                }
            }

        fun getDelayedAssignmentAfterRHS(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("DelayedAssignmentAfterRHS.java") {
                        record("DelayedAssignmentAfterRHS") {
                            // The main method
                            method("main") {
                                this.isStatic = true
                                param("args", t("String[]"))
                                body {
                                    declare { variable("a", t("int")) { literal(0, t("int")) } }
                                    declare { variable("b", t("int")) { literal(1, t("int")) } }
                                    ref("a") assign { ref("a") + ref("b") }
                                }
                            }
                        }
                    }
                }
            }

        fun getReturnTest(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("ReturnTest.java") {
                        record("ReturnTest", "class") {
                            method("testReturn", t("int")) {
                                receiver = newVariable("this", t("ReturnTest"))
                                body {
                                    declare { variable("a", t("int")) { literal(1, t("int")) } }
                                    ifStmt {
                                        condition { ref("a") eq literal(5, t("int")) }
                                        thenStmt {
                                            returnStmt {
                                                returnValue = literal(2, t("int"))
                                                location =
                                                    PhysicalLocation(
                                                        URI("ReturnTest.java"),
                                                        Region(5, 13, 5, 21),
                                                    )
                                            }
                                        }
                                        elseStmt {
                                            returnStmt {
                                                returnValue = ref("a")
                                                location =
                                                    PhysicalLocation(
                                                        URI("ReturnTest.java"),
                                                        Region(7, 13, 7, 21),
                                                    )
                                            }
                                        }
                                    }
                                    returnStmt { isImplicit = true }
                                }
                            }
                        }
                    }
                }
            }

        fun getVisitorTest(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("Record.java") {
                        namespace("compiling") {
                            record("SimpleClass", "class") {
                                field("field", t("int")) {}
                                constructor {
                                    receiver = newVariable("this", t("SimpleClass"))
                                    body { returnStmt { isImplicit = true } }
                                }
                                method("method", t("Integer")) {
                                    receiver = newVariable("this", t("SimpleClass"))
                                    body {
                                        memberCall(
                                            "println",
                                            member("out", ref("System") { isStaticAccess = true }),
                                        ) {
                                            literal("Hello world")
                                        }
                                        declare { variable("x", t("int")) { literal(0) } }
                                        ifStmt {
                                            condition {
                                                memberCall(
                                                    "currentTimeMillis",
                                                    ref("System") { isStaticAccess = true },
                                                ) gt literal(0)
                                            }
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

        fun getDataflowClass(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("Dataflow.java") {
                        record("Dataflow") {
                            field("attr", t("String")) { literal("", t("String")) }
                            constructor {
                                isImplicit = true
                                receiver = newVariable("this", t("Dataflow"))
                                body { returnStmt { isImplicit = true } }
                            }
                            method("toString", t("String")) {
                                receiver = newVariable("this", t("Dataflow"))
                                body {
                                    returnStmt { literal("ShortcutClass: attr=") + member("attr") }
                                }
                            }

                            method("test", t("String")) {
                                receiver = newVariable("this", t("Dataflow"))
                                body { returnStmt { literal("abcd") } }
                            }

                            method("print", t("int")) {
                                receiver = newVariable("this", t("Dataflow"))
                                param("s", t("String"))
                                body {
                                    memberCall(
                                        "println",
                                        member("out", ref("System") { isStaticAccess = true }),
                                    ) {
                                        ref("s")
                                    }
                                    returnStmt { isImplicit = true }
                                }
                            }

                            // The main method
                            method("main") {
                                this.isStatic = true
                                param("args", t("String[]"))
                                body {
                                    declare {
                                        variable("sc", t("Dataflow")) {
                                            new { construct("Dataflow") }
                                        }
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

        fun getShortcutClass(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("ShortcutClass.java") {
                        record("ShortcutClass") {
                            field("attr", t("int")) { literal(0, t("int")) }
                            constructor {
                                receiver = newVariable("this", t("ShortcutClass"))
                                isImplicit = true
                                body { returnStmt { isImplicit = true } }
                            }
                            method("toString", t("String")) {
                                receiver = newVariable("this", t("ShortcutClass"))
                                body {
                                    returnStmt { literal("ShortcutClass: attr=") + member("attr") }
                                }
                            }

                            method("print", t("int")) {
                                receiver = newVariable("this", t("ShortcutClass"))
                                body {
                                    memberCall(
                                        "println",
                                        member("out", ref("System") { isStaticAccess = true }),
                                    ) {
                                        call("this.toString")
                                    }
                                }
                            }

                            method("magic") {
                                receiver = newVariable("this", t("ShortcutClass"))
                                param("b", t("int"))
                                body {
                                    ifStmt {
                                        condition { ref("b") eq literal(5, t("int")) }
                                        thenStmt {
                                            ifStmt {
                                                condition { member("attr") eq literal(2, t("int")) }
                                                thenStmt {
                                                    member("attr") assign literal(3, t("int"))
                                                }
                                                elseStmt {
                                                    member("attr") assign literal(2, t("int"))
                                                }
                                            }
                                        }
                                        elseStmt { member("attr") assign ref("b") }
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
                                                condition { member("attr") eq literal(2, t("int")) }
                                                thenStmt { ref("a") assign literal(3, t("int")) }
                                                elseStmt { ref("a") assign literal(2, t("int")) }
                                            }
                                        }
                                        elseStmt { ref("a") assign ref("b") }
                                    }
                                }
                            }

                            // The main method
                            method("main") {
                                this.isStatic = true
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

        /**
         * This roughly represents the following Java Code:
         * ```java
         * public class TestClass {
         *   public TestClass(int i) {
         *
         *   };
         *
         *   public TestClass method1() {
         *     return new TestClass(4);
         *   }
         *
         *   public void method2() {
         *      var variable = this.method1();
         *      variable.method2();
         *      return;
         *   }
         * }
         * ```
         */
        fun getCombinedVariableAndCallTest(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("CombinedVariableAndCall.java") {
                        record("TestClass") {
                            constructor { param("i", t("int")) }
                            method("method1", t("TestClass")) {
                                body {
                                    returnStmt { construct("TestClass") { literal(4, t("int")) } }
                                }
                            }

                            method("method2") {
                                receiver("this", t("TestClass"))
                                body {
                                    declare {
                                        variable("variable", autoType()) {
                                            memberCall("method1", ref("this"))
                                        }
                                    }

                                    memberCall("method2", ref("variable"))
                                }
                            }
                        }
                    }
                }
            }

        /**
         * This roughly represents the following C code:
         * ```c
         * struct myStruct {
         *   int field1;
         * };
         *
         * void doSomething(int i) {}
         *
         * int main() {
         *   struct myStruct s1;
         *   struct myStruct s2;
         *
         *   doSomething(s1.field1);
         *
         *   s1.field1 = 1;
         *   s2.field1 = 2;
         *
         *   doSomething(s1.field1);
         *   doSomething(s2.field1);
         * }
         * ```
         */
        fun getSimpleFieldDataflow(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("dataflow_field.c") {
                        record("myStruct") { field("field1", t("int")) }
                        function("doSomething") { param("i", t("int")) }
                        function("main", t("int")) {
                            body {
                                declare {
                                    // Declare s1 and s2 of the same type
                                    variable("s1", t("myStruct"))
                                    variable("s2", t("myStruct"))
                                }

                                // Call doSomething on field1 of s1
                                call("doSomething") {
                                        member("field1", ref("s1", makeMagic = false).line(11))
                                            .line(11)
                                    }
                                    .line(11)

                                // Set field1 of both s1 and s2, to literal 1 and 2 respectively
                                member("field1", ref("s1", makeMagic = false).line(13))
                                    .line(13) assign literal(1)
                                member("field1", ref("s2", makeMagic = false).line(14))
                                    .line(14) assign literal(2)

                                // Call doSomething on field1 of s1 and s2
                                call("doSomething") {
                                        member("field1", ref("s1", makeMagic = false).line(15))
                                            .line(15)
                                    }
                                    .line(15)
                                call("doSomething") {
                                        member("field1", ref("s2", makeMagic = false).line(16))
                                            .line(16)
                                    }
                                    .line(16)
                            }
                        }
                    }
                }
            }

        /**
         * This roughly represents the following C code:
         * ```c
         * struct inner {
         *   int field;
         * };
         *
         * struct outer {
         *   struct inner in;
         * };
         *
         * void doSomething(int i) {}
         *
         * int main() {
         *   struct outer o;
         *   o.in.field = 1;
         *
         *   doSomething(o.in.field);
         * }
         * ```
         */
        fun getNestedFieldDataflow(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("dataflow_field.c") {
                        record("inner") { field("field", t("int")) }
                        record("outer") { field("in", t("inner")) }
                        function("doSomething") { param("i", t("int")) }
                        function("main", t("int")) {
                            body {
                                declare { variable("o", t("outer")) }

                                member(
                                        "field",
                                        member("in", ref("o", makeMagic = false).line(13)).line(13),
                                    )
                                    .line(13) assign literal(1)

                                call("doSomething") {
                                        member(
                                                "field",
                                                member("in", ref("o", makeMagic = false).line(15))
                                                    .line(15),
                                            )
                                            .line(15)
                                    }
                                    .line(15)
                            }
                        }
                    }
                }
            }

        fun prepareThrowDFGTest(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("some.file") {
                        function("foo", t("void")) {
                            body {
                                declare { variable("a", t("short")) { literal(42) } }
                                `throw` { call("SomeError") { ref("a") } }
                            }
                        }
                    }
                }
            }
    }
}
