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

import de.fraunhofer.aisec.cpg.frontends.StructTestLanguage
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.builder.*
import de.fraunhofer.aisec.cpg.graph.newInitializerListExpression
import de.fraunhofer.aisec.cpg.graph.newVariableDeclaration
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.net.URI

class GraphExamples {
    companion object {
        fun getInitializerListExprDFG(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage(TestLanguage("."))
                    .build()
        ) =
            TestLanguageFrontend(ScopeManager(), ".").build {
                translationResult(config) {
                    translationUnit("initializerListExprDFG.cpp") {
                        function("foo", t("int")) { body { returnStmt { literal(0, t("int")) } } }
                        function("main", t("int")) {
                            body {
                                declare {
                                    variable("i", t("int")) {
                                        val initList = newInitializerListExpression()
                                        initList.initializers = listOf(call("foo"))
                                        initializer = initList
                                    }
                                }
                                returnStmt { ref("i") }
                            }
                        }
                    }
                }
            }

        fun getInferenceRecordPtr(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage(StructTestLanguage("."))
                    .inferenceConfiguration(
                        InferenceConfiguration.builder().inferRecords(true).build()
                    )
                    .build()
        ) =
            TestLanguageFrontend(
                    ScopeManager(),
                    config.languages.first().namespaceDelimiter,
                    config.languages.first()
                )
                .build {
                    translationResult(config) {
                        translationUnit("record.cpp") {
                            // The main method
                            function("main", t("int")) {
                                body {
                                    declare { variable("node", t("T*")) }
                                    member("value", ref("node"), "->") assign literal(42, t("int"))
                                    member("next", ref("node"), "->") assign ref("node")
                                    memberCall(
                                        "dump",
                                        ref("node")
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
                    .registerLanguage(StructTestLanguage("."))
                    .inferenceConfiguration(
                        InferenceConfiguration.builder().inferRecords(true).build()
                    )
                    .build()
        ) =
            TestLanguageFrontend(
                    ScopeManager(),
                    config.languages.first().namespaceDelimiter,
                    config.languages.first()
                )
                .build {
                    translationResult(config) {
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

        fun getVariables(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage(TestLanguage("."))
                    .build()
        ) =
            TestLanguageFrontend(ScopeManager(), ".").build {
                translationResult(config) {
                    translationUnit("initializerListExprDFG.cpp") {
                        function("foo", t("int")) { body { returnStmt { literal(0, t("int")) } } }
                        function("main", t("int")) {
                            body {
                                declare {
                                    variable("i", t("int")) {
                                        val initList = newInitializerListExpression()
                                        initList.initializers = listOf(call("foo"))
                                        initializer = initList
                                    }
                                }
                                returnStmt { ref("i") }

                                translationUnit("Variables.java") {
                                    record("Variables") {
                                        field("field", t("int")) {
                                            literal(42, t("int"))
                                            modifiers = listOf("private")
                                        }
                                        method("getField", t("int")) {
                                            receiver =
                                                newVariableDeclaration("this", t("Variables"))
                                            body { returnStmt { member("field") } }
                                        }
                                        method("getLocal", t("int")) {
                                            receiver =
                                                newVariableDeclaration("this", t("Variables"))
                                            body {
                                                declare {
                                                    variable("local", t("int")) {
                                                        literal(42, t("int"))
                                                    }
                                                }
                                                returnStmt { ref("local") }
                                            }
                                        }
                                        method("getShadow", t("int")) {
                                            receiver =
                                                newVariableDeclaration("this", t("Variables"))
                                            body {
                                                declare {
                                                    variable("field", t("int")) {
                                                        literal(43, t("int"))
                                                    }
                                                }
                                                returnStmt { ref("field") }
                                            }
                                        }
                                        method("getNoShadow", t("int")) {
                                            receiver =
                                                newVariableDeclaration("this", t("Variables"))
                                            body {
                                                declare {
                                                    variable("field", t("int")) {
                                                        literal(43, t("int"))
                                                    }
                                                }
                                                returnStmt { member("field", ref("this")) }
                                            }
                                        }
                                    }
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
                    .registerLanguage(TestLanguage("."))
                    .build()
        ) =
            TestLanguageFrontend(ScopeManager(), ".").build {
                translationResult(config) {
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
                    .registerLanguage(TestLanguage("."))
                    .build()
        ) =
            TestLanguageFrontend(ScopeManager(), ".").build {
                translationResult(config) {
                    translationUnit("compoundoperator.cpp") {
                        // The main method
                        function("somefunc") {
                            body {
                                declare { variable("i", t("int")) { literal(0, t("int")) } }
                                ref("i") += literal(0, t("int"))
                                returnStmt { isImplicit = true }
                            }
                        }
                    }
                }
            }

        fun getConditionalExpression(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage(TestLanguage("."))
                    .build()
        ) =
            TestLanguageFrontend(ScopeManager(), ".").build {
                translationResult(config) {
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
                                            Region(5, 3, 5, 4)
                                        )
                                } assign
                                    {
                                        conditional(
                                            ref("a") {
                                                location =
                                                    PhysicalLocation(
                                                        URI("conditional_expression.cpp"),
                                                        Region(5, 7, 5, 8)
                                                    )
                                            } eq
                                                ref("b") {
                                                    location =
                                                        PhysicalLocation(
                                                            URI("conditional_expression.cpp"),
                                                            Region(5, 12, 5, 13)
                                                        )
                                                },
                                            ref("b") {
                                                location =
                                                    PhysicalLocation(
                                                        URI("conditional_expression.cpp"),
                                                        Region(5, 16, 5, 17)
                                                    )
                                            } assign literal(2, t("int")),
                                            ref("b") {
                                                location =
                                                    PhysicalLocation(
                                                        URI("conditional_expression.cpp"),
                                                        Region(5, 23, 5, 24)
                                                    )
                                            } assign literal(3, t("int"))
                                        )
                                    }
                                ref("a") {
                                    location =
                                        PhysicalLocation(
                                            URI("conditional_expression.cpp"),
                                            Region(6, 3, 6, 4)
                                        )
                                } assign
                                    ref("b") {
                                        location =
                                            PhysicalLocation(
                                                URI("conditional_expression.cpp"),
                                                Region(6, 7, 6, 8)
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
                    .registerLanguage(TestLanguage("."))
                    .build()
        ) =
            TestLanguageFrontend(ScopeManager(), ".").build {
                translationResult(config) {
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
                    .registerLanguage(TestLanguage("."))
                    .build()
        ) =
            TestLanguageFrontend(ScopeManager(), ".").build {
                translationResult(config) {
                    translationUnit("ControlFlowSensitiveDFGIfMerge.java") {
                        record("ControlFlowSensitiveDFGIfMerge") {
                            field("bla", t("int")) {}
                            constructor {
                                isImplicit = true
                                receiver =
                                    newVariableDeclaration(
                                        "this",
                                        t("ControlFlowSensitiveDFGIfMerge")
                                    )
                                body { returnStmt { isImplicit = true } }
                            }
                            method("func") {
                                receiver =
                                    newVariableDeclaration(
                                        "this",
                                        t("ControlFlowSensitiveDFGIfMerge")
                                    )
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
                                                    ref("System") { isStaticAccess = true }
                                                )
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
                    .registerLanguage(TestLanguage("."))
                    .build()
        ) =
            TestLanguageFrontend(ScopeManager(), ".").build {
                translationResult(config) {
                    translationUnit("ControlFlowSesitiveDFGSwitch.java") {
                        record("ControlFlowSesitiveDFGSwitch") {
                            // The main method
                            method("func3") {
                                receiver =
                                    newVariableDeclaration(
                                        "this",
                                        t("ControlFlowSesitiveDFGSwitch")
                                    )
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
                                                        Region(8, 9, 8, 10)
                                                    )
                                            } assign literal(10, t("int"))
                                            breakStmt()
                                            case(literal(2, t("int")))
                                            ref("a") {
                                                location =
                                                    PhysicalLocation(
                                                        URI("ControlFlowSesitiveDFGSwitch.java"),
                                                        Region(11, 9, 11, 10)
                                                    )
                                            } assign literal(11, t("int"))
                                            breakStmt()
                                            case(literal(3, t("int")))
                                            ref("a") {
                                                location =
                                                    PhysicalLocation(
                                                        URI("ControlFlowSesitiveDFGSwitch.java"),
                                                        Region(14, 9, 14, 10)
                                                    )
                                            } assign literal(12, t("int"))
                                            default()
                                            memberCall(
                                                "println",
                                                member(
                                                    "out",
                                                    ref("System") { isStaticAccess = true }
                                                )
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
                    .registerLanguage(TestLanguage("."))
                    .build()
        ) =
            TestLanguageFrontend(ScopeManager(), ".").build {
                translationResult(config) {
                    translationUnit("ControlFlowSensitiveDFGIfNoMerge.java") {
                        record("ControlFlowSensitiveDFGIfNoMerge") {
                            // The main method
                            method("func2") {
                                receiver =
                                    newVariableDeclaration(
                                        "this",
                                        t("ControlFlowSensitiveDFGIfNoMerge")
                                    )
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
                    .registerLanguage(TestLanguage("."))
                    .build()
        ) =
            TestLanguageFrontend(ScopeManager(), ".").build {
                translationResult(config) {
                    translationUnit("LoopDFGs.java") {
                        record("LoopDFGs") {
                            // The main method
                            method("labeledBreakContinue") {
                                receiver = newVariableDeclaration("this", t("LoopDFGs"))
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
                                                                        }
                                                                    )
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
                                                        ref("System") { isStaticAccess = true }
                                                    )
                                                ) {
                                                    ref("a")
                                                }
                                                ref("a") assign literal(3, t("int"))
                                            }
                                        }
                                    }

                                    memberCall(
                                        "println",
                                        member("out", ref("System") { isStaticAccess = true })
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
                    .registerLanguage(TestLanguage("."))
                    .build()
        ) =
            TestLanguageFrontend(ScopeManager(), ".").build {
                translationResult(config) {
                    translationUnit("LoopDFGs.java") {
                        record("LoopDFGs") {
                            // The main method
                            method("looping") {
                                receiver = newVariableDeclaration("this", t("LoopDFGs"))
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
                                                            ref("System") { isStaticAccess = true }
                                                        )
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
                    .registerLanguage(TestLanguage("."))
                    .build()
        ) =
            TestLanguageFrontend(ScopeManager(), ".").build {
                translationResult(config) {
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
                    .registerLanguage(TestLanguage("."))
                    .build()
        ) =
            TestLanguageFrontend(ScopeManager(), ".").build {
                translationResult(config) {
                    translationUnit("ReturnTest.java") {
                        record("ReturnTest", "class") {
                            method("testReturn", t("int")) {
                                receiver = newVariableDeclaration("this", t("ReturnTest"))
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
                                                        Region(5, 13, 5, 21)
                                                    )
                                            }
                                        }
                                        elseStmt {
                                            returnStmt {
                                                returnValue = ref("a")
                                                location =
                                                    PhysicalLocation(
                                                        URI("ReturnTest.java"),
                                                        Region(7, 13, 7, 21)
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
                    .registerLanguage(TestLanguage("."))
                    .build()
        ) =
            TestLanguageFrontend(ScopeManager(), ".").build {
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
                                        memberCall(
                                            "println",
                                            member("out", ref("System") { isStaticAccess = true })
                                        ) {
                                            literal("Hello world")
                                        }
                                        declare { variable("x", t("int")) { literal(0) } }
                                        ifStmt {
                                            condition {
                                                memberCall(
                                                    "currentTimeMillis",
                                                    ref("System") { isStaticAccess = true }
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
                    .registerLanguage(TestLanguage("."))
                    .build()
        ) =
            TestLanguageFrontend(ScopeManager(), ".").build {
                translationResult(config) {
                    translationUnit("Dataflow.java") {
                        record("Dataflow") {
                            field("attr", t("String")) { literal("", t("String")) }
                            constructor {
                                isImplicit = true
                                receiver = newVariableDeclaration("this", t("Dataflow"))
                                body { returnStmt { isImplicit = true } }
                            }
                            method("toString", t("String")) {
                                receiver = newVariableDeclaration("this", t("Dataflow"))
                                body {
                                    returnStmt { literal("ShortcutClass: attr=") + member("attr") }
                                }
                            }

                            method("test", t("String")) {
                                receiver = newVariableDeclaration("this", t("Dataflow"))
                                body { returnStmt { literal("abcd") } }
                            }

                            method("print", t("int")) {
                                receiver = newVariableDeclaration("this", t("Dataflow"))
                                param("s", t("String"))
                                body {
                                    memberCall(
                                        "println",
                                        member("out", ref("System") { isStaticAccess = true })
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
                    .registerLanguage(TestLanguage("."))
                    .build()
        ) =
            TestLanguageFrontend(ScopeManager(), ".").build {
                translationResult(config) {
                    translationUnit("ShortcutClass.java") {
                        record("ShortcutClass") {
                            field("attr", t("int")) { literal(0, t("int")) }
                            constructor {
                                receiver = newVariableDeclaration("this", t("ShortcutClass"))
                                isImplicit = true
                                body { returnStmt { isImplicit = true } }
                            }
                            method("toString", t("String")) {
                                receiver = newVariableDeclaration("this", t("ShortcutClass"))
                                body {
                                    returnStmt { literal("ShortcutClass: attr=") + member("attr") }
                                }
                            }

                            method("print", t("int")) {
                                receiver = newVariableDeclaration("this", t("ShortcutClass"))
                                body {
                                    memberCall(
                                        "println",
                                        member("out", ref("System") { isStaticAccess = true })
                                    ) {
                                        call("this.toString")
                                    }
                                }
                            }

                            method("magic") {
                                receiver = newVariableDeclaration("this", t("ShortcutClass"))
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
    }
}
