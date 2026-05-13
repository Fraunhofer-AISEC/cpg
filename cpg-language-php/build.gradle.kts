/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
plugins {
    id("cpg.frontend-conventions")
    antlr
}

mavenPublishing {
    pom {
        name.set("Code Property Graph - PHP")
        description.set("A PHP language frontend for the CPG")
    }
}

dependencies {
    // ANTLR4 tool is used at build time to generate the lexer/parser; runtime is needed at runtime
    antlr("org.antlr:antlr4:4.9.3")
    implementation("org.antlr:antlr4-runtime:4.9.3")
}

tasks.generateGrammarSource {
    arguments =
        arguments +
            listOf(
                "-visitor",
                "-package",
                "de.fraunhofer.aisec.cpg.frontends.php",
                "-lib",
                outputDirectory.absolutePath,
            )
    outputDirectory =
        file(
            "${project.layout.buildDirectory.get()}/generated-src/antlr/main/de/fraunhofer/aisec/cpg/frontends/php"
        )
}

// Make sure ANTLR sources are generated before Kotlin compilation
tasks.compileKotlin { dependsOn(tasks.generateGrammarSource) }

tasks.compileTestKotlin { dependsOn(tasks.generateGrammarSource) }
