/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
    id("cpg.application-conventions")
    id("cpg.frontend-dependency-conventions")
}

mavenPublishing {
    pom {
        name.set("Code Property Graph - Console")
        description.set(
            "An Application to translate source code into a Code Property Graph and perform different types of analysis on the resulting graph."
        )
    }
}

application {
    mainClass.set("de.fraunhofer.aisec.cpg.console.CpgConsole")
    applicationDefaultJvmArgs = listOf("-Xss515m", "-Xmx8g")
}

repositories { maven { setUrl("https://jitpack.io") } }

dependencies {
    // CPG
    api(projects.cpgCore)
    api(projects.cpgAnalysis)
    api(projects.cpgNeo4j)

    implementation(libs.kotlin.ki.shell)

    // ki-shell dependencies, that we need to specify manually because we are using the JitPack
    // "release"
    implementation(libs.kotlin.script.runtime)
    implementation(libs.bundles.kotlin.scripting)
    implementation(libs.antlr.runtime)
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.jline)

    testImplementation(testFixtures(projects.cpgCore))

    implementation(project(":cpg-concepts"))
}
