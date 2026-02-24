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

import de.fraunhofer.aisec.cpg.InferenceConfiguration
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.testFrontend
import de.fraunhofer.aisec.cpg.graph.array
import de.fraunhofer.aisec.cpg.graph.builder.*
import de.fraunhofer.aisec.cpg.graph.newNewArrayExpression
import de.fraunhofer.aisec.cpg.graph.pointer

class Query {
    companion object {
        fun getDataflow(
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
                    .registerLanguage<TestLanguage>()
                    .inferenceConfiguration(InferenceConfiguration.builder().enabled(false).build())
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
                                        ref("Dataflow", t("Dataflow")) {
                                            isStatic = true
                                            refersTo = this@record
                                        },
                                    ) {
                                        this@memberCall.isStatic = true
                                        memberCall(
                                            "toString",
                                            ref("Integer", t("Integer"), makeMagic = false),
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
                                            member("a", ref("sc", makeMagic = false)) +
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
                    .registerLanguage<TestLanguage>()
                    .inferenceConfiguration(
                        InferenceConfiguration.builder().inferFunctions(false).build()
                    )
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
                                            member("a", ref("sc", makeMagic = false)) +
                                            literal(" into highlyCriticalOperation()", t("string"))
                                    }

                                    memberCall(
                                        "highlyCriticalOperation",
                                        ref("Dataflow", t("Dataflow")) {
                                            isStatic = true
                                            refersTo = this@record
                                        },
                                    ) {
                                        this@memberCall.isStatic = true
                                        memberCall(
                                            "toString",
                                            ref("Integer", t("Integer"), makeMagic = false),
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
                    .registerLanguage<TestLanguage>()
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
                                        ref("Dataflow", t("Dataflow")) {
                                            isStatic = true
                                            refersTo = this@record
                                        },
                                    ) {
                                        this@memberCall.isStatic = true
                                        memberCall(
                                            "toString",
                                            ref("Integer", t("Integer"), makeMagic = false),
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

        fun getArray(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("array.cpp") {
                        function("main", t("int")) {
                            body {
                                declare {
                                    variable("c", t("char").pointer()) {
                                        val creationExpr = newNewArrayExpression()
                                        creationExpr.addDimension(literal(4, t("int")))
                                        creationExpr.type = t("char")
                                        this.initializer = creationExpr
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

                                returnStmt { literal(0, t("int")) }
                            }
                        }

                        function("some_other_function", t("char")) {
                            declare {
                                variable("c", t("char").pointer()) {
                                    val creationExpr = newNewArrayExpression()
                                    creationExpr.addDimension(literal(100, t("int")))
                                    creationExpr.type = t("char")
                                    this.initializer = creationExpr
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

        fun getArray2(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("array2.cpp") {
                        function("main", t("int")) {
                            body {
                                declare {
                                    variable("c", t("char").pointer()) {
                                        val creationExpr = newNewArrayExpression()
                                        creationExpr.addDimension(literal(4, t("int")))
                                        creationExpr.type = t("char")
                                        this.initializer = creationExpr
                                    }
                                }

                                declare { variable("a", t("int")) { literal(0, t("int")) } }

                                forStmt {
                                    loopBody {
                                        ref("a") assign
                                            {
                                                ref("a") +
                                                    subscriptExpr {
                                                        ref("c")
                                                        ref("i")
                                                    }
                                            }
                                    }
                                    forInitializer {
                                        declareVar("i", t("int")) { literal(0, t("int")) }
                                    }
                                    forCondition { ref("i") lt literal(5, t("int")) }
                                    forIteration { ref("i").incNoContext() }
                                }

                                returnStmt { ref("a") }
                            }
                        }
                    }
                }
            }

        fun getArray3(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("array3.cpp") {
                        function("main", t("int")) {
                            body {
                                declare { variable("c", t("char").pointer()) }
                                ifStmt {
                                    condition { literal(5, t("int")) gt literal(4, t("int")) }
                                    thenStmt {
                                        ref("c") assign
                                            run {
                                                val creationExpr = newNewArrayExpression()
                                                creationExpr.addDimension(literal(4, t("int")))
                                                creationExpr.type = t("char")
                                                (creationExpr)
                                            }
                                    }
                                    elseStmt {
                                        ref("c") assign
                                            run {
                                                val creationExpr = newNewArrayExpression()
                                                creationExpr.addDimension(literal(5, t("int")))
                                                creationExpr.type = t("char")
                                                (creationExpr)
                                            }
                                    }
                                }

                                declare { variable("a", t("int")) { literal(0, t("int")) } }

                                forStmt {
                                    forInitializer {
                                        declareVar("i", t("int")) { literal(0, t("int")) }
                                    }
                                    forCondition { ref("i") lt literal(5, t("int")) }
                                    forIteration { ref("i").incNoContext() }

                                    loopBody {
                                        ref("a") assign
                                            {
                                                ref("a") +
                                                    subscriptExpr {
                                                        ref("c")
                                                        ref("i")
                                                    }
                                            }
                                    }
                                }

                                returnStmt { ref("a") }
                            }
                        }
                    }
                }
            }

        fun getArrayCorrect(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("array_correct.cpp") {
                        function("main", t("int")) {
                            body {
                                declare {
                                    variable("c", t("char").pointer()) {
                                        val creationExpr = newNewArrayExpression()
                                        creationExpr.addDimension(literal(4, t("int")))
                                        creationExpr.type = t("char")
                                        this.initializer = creationExpr
                                    }
                                }

                                declare { variable("a", t("int")) { literal(0, t("int")) } }

                                forStmt {
                                    loopBody {
                                        ref("a") assign
                                            {
                                                ref("a") +
                                                    subscriptExpr {
                                                        ref("c")
                                                        ref("i")
                                                    }
                                            }
                                    }
                                    forInitializer {
                                        declareVar("i", t("int")) { literal(0, t("int")) }
                                    }
                                    forCondition { ref("i") lt literal(4, t("int")) }
                                    forIteration { ref("i").incNoContext() }
                                }

                                returnStmt { ref("a") }
                            }
                        }
                    }
                }
            }

        fun getAssign(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("assign.cpp") {
                        function("main", t("int")) {
                            body {
                                declare { variable("a", t("int")) { literal(4, t("int")) } }
                                // TODO: There was a commented-out line. No idea what to do with it:
                                // int a, b = 4; // this is broken, a is missing an initializer

                                ref("a") assign literal(3, t("int"))

                                returnStmt { ref("a") }
                            }
                        }
                    }
                }
            }

        fun getDivBy0(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("assign.cpp") {
                        function("main", t("int")) {
                            body {
                                declare {
                                    variable("array", t("char").array()) {
                                        literal("hello", t("char").array())
                                    }
                                }
                                declare { variable("a", t("short")) { literal(2, t("int")) } }

                                ifStmt {
                                    condition { ref("array") eq literal("hello", t("string")) }
                                    thenStmt { ref("a") assign literal(0, t("int")) }
                                }

                                declare {
                                    variable("x", t("double")) { literal(5, t("int")) / ref("a") }
                                }

                                returnStmt { literal(0, t("int")) }
                            }
                        }
                    }
                }
            }

        fun getVulnerable(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage<TestLanguage>()
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("assign.cpp") {
                        function("main", t("int")) {
                            body {
                                declare {
                                    variable("array", t("char").array()) {
                                        literal("hello", t("char").array())
                                    }
                                }
                                call("memcpy") {
                                    ref("array")
                                    literal("Hello world", t("char").array())
                                    literal(11, t("int"))
                                }

                                call("printf") { ref("array") }

                                call("free") { ref("array") }

                                call("free") { ref("array") }

                                declare { variable("a", t("short")) { literal(2, t("int")) } }

                                ifStmt {
                                    condition { ref("array") eq literal("hello", t("string")) }
                                    thenStmt { ref("a") assign literal(1, t("int")) }
                                }

                                declare {
                                    variable("x", t("double")) { literal(5, t("int")) / ref("a") }
                                }

                                declare {
                                    variable("b", t("int")) { literal(2147483648, t("int")) }
                                }

                                ref("b") assign literal(2147483648, t("int"))

                                declare { variable("c", t("long")) { literal(-10000, t("long")) } }

                                returnStmt { literal(0, t("int")) }
                            }
                        }
                    }
                }
            }
    }
}
