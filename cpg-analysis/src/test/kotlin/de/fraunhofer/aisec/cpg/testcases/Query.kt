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
import de.fraunhofer.aisec.cpg.graph.newArrayCreationExpression
import de.fraunhofer.aisec.cpg.graph.pointer
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
                                        ref("Dataflow", t("Dataflow")) {
                                            isStatic = true
                                            refersTo = this@record
                                        }
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
                                        ref("Dataflow", t("Dataflow")) {
                                            isStatic = true
                                            refersTo = this@record
                                        }
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
                                        ref("Dataflow", t("Dataflow")) {
                                            isStatic = true
                                            refersTo = this@record
                                        }
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

        fun getArray(
            config: TranslationConfiguration =
                TranslationConfiguration.builder()
                    .defaultPasses()
                    .registerLanguage(TestLanguage("."))
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("array.cpp") {
                        function("main", t("int")) {
                            body {
                                declare {
                                    variable("c", t("char").pointer()) {
                                        val creationExpr = newArrayCreationExpression()
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
                                        ase {
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
                                    val creationExpr = newArrayCreationExpression()
                                    creationExpr.addDimension(literal(100, t("int")))
                                    creationExpr.type = t("char")
                                    this.initializer = creationExpr
                                }
                            }
                            returnStmt {
                                ase {
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
                    .registerPass<EdgeCachePass>()
                    .registerLanguage(TestLanguage("."))
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("array2.cpp") {
                        function("main", t("int")) {
                            body {
                                declare {
                                    variable("c", t("char").pointer()) {
                                        val creationExpr = newArrayCreationExpression()
                                        creationExpr.addDimension(literal(4, t("int")))
                                        creationExpr.type = t("char")
                                        this.initializer = creationExpr
                                    }
                                }

                                declare { variable("a", t("int")) { literal(0, t("int")) } }

                                forStmt(
                                    declareVar("i", t("int")) { literal(0, t("int")) },
                                    ref("i") le literal(4, t("int")),
                                    ref("i").incNoContext()
                                ) {
                                    ref("a") assign
                                        {
                                            ref("a") +
                                                ase {
                                                    ref("c")
                                                    ref("i")
                                                }
                                        }
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
                    .registerPass<EdgeCachePass>()
                    .registerLanguage(TestLanguage("."))
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
                                                val creationExpr = newArrayCreationExpression()
                                                creationExpr.addDimension(literal(4, t("int")))
                                                creationExpr.type = t("char")
                                                (creationExpr)
                                            }
                                    }
                                    elseStmt {
                                        ref("c") assign
                                            run {
                                                val creationExpr = newArrayCreationExpression()
                                                creationExpr.addDimension(literal(5, t("int")))
                                                creationExpr.type = t("char")
                                                (creationExpr)
                                            }
                                    }
                                }

                                declare { variable("a", t("int")) { literal(0, t("int")) } }

                                forStmt(
                                    declareVar("i", t("int")) { literal(0, t("int")) },
                                    ref("i") le literal(4, t("int")),
                                    ref("i").incNoContext()
                                ) {
                                    ref("a") assign
                                        {
                                            ref("a") +
                                                ase {
                                                    ref("c")
                                                    ref("i")
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
                    .registerPass<EdgeCachePass>()
                    .registerLanguage(TestLanguage("."))
                    .build()
        ) =
            testFrontend(config).build {
                translationResult {
                    translationUnit("array_correct.cpp") {
                        function("main", t("int")) {
                            body {
                                declare {
                                    variable("c", t("char").pointer()) {
                                        val creationExpr = newArrayCreationExpression()
                                        creationExpr.addDimension(literal(4, t("int")))
                                        creationExpr.type = t("char")
                                        this.initializer = creationExpr
                                    }
                                }

                                declare { variable("a", t("int")) { literal(0, t("int")) } }

                                forStmt(
                                    declareVar("i", t("int")) { literal(0, t("int")) },
                                    ref("i") lt literal(4, t("int")),
                                    ref("i").incNoContext()
                                ) {
                                    ref("a") assign
                                        {
                                            ref("a") +
                                                ase {
                                                    ref("c")
                                                    ref("i")
                                                }
                                        }
                                }

                                returnStmt { ref("a") }
                            }
                        }
                    }
                }
            }
    }
}
