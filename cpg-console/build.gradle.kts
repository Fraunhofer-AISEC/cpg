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
    application
}

publishing {
    publications {
        named<MavenPublication>("cpg-console") {
            pom {
                artifactId = "cpg-console"
                name.set("Code Property Graph - Console")
                description.set("An Application to translate source code into a Code Property Graph and perform different types of analysis on the resulting graph.")
            }
        }
    }
}

application {
    mainClass.set("de.fraunhofer.aisec.cpg.console.CpgConsole")
}

tasks.withType<Test> {
    useJUnitPlatform {
        if (!project.hasProperty("integration")) {
            excludeTags("integration")
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8" // important, since ki is 1.8 and otherwise inlining wont work
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "11"
    targetCompatibility = "11"
}

val versions = mapOf(
    "junit5" to "5.6.0"
)

dependencies {
    // CPG
    api(project(":cpg-core"))
    api(project(":cpg-analysis"))
    api(project(":cpg-language-llvm"))
    api(project(":cpg-language-python"))
    api(project(":cpg-language-go"))
    api(project(":cpg-neo4j"))

    implementation("org.apache.logging.log4j:log4j-slf4j18-impl:2.17.0")

    // JUnit
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter", "junit-jupiter-api", versions["junit5"])
    testImplementation("org.junit.jupiter", "junit-jupiter-params", versions["junit5"])
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", versions["junit5"])

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-script-runtime")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0-RC")
    implementation("org.jline:jline:3.21.0")

    implementation("org.jetbrains.kotlinx:ki-shell:0.4.1")
}
