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
    `java-library`
    `java-test-fixtures`
    signing
}

publishing {
    publications {
        named<MavenPublication>("cpg-core") {
            pom {
                artifactId = "cpg-core"
                name.set("Code Property Graph - Core")
                description.set("A simple library to extract a code property graph out of source code. It has support for multiple passes that can extend the analysis after the graph is constructed.")
            }

            suppressPomMetadataWarningsFor("testFixturesApiElements")
            suppressPomMetadataWarningsFor("testFixturesRuntimeElements")
        }
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform {
        if (!project.hasProperty("experimental")) {
            excludeTags("experimental")
        }
    }
    maxHeapSize = "4048m"
}

dependencies {
    api("org.apache.commons:commons-lang3:3.12.0")

    api("org.neo4j:neo4j-ogm-core:3.2.35")

    api("org.slf4j:jul-to-slf4j:1.7.36")
    api("org.slf4j:slf4j-api:1.7.36")
    implementation("org.apache.logging.log4j:log4j-slf4j18-impl:2.17.2")

    api("com.github.javaparser:javaparser-symbol-solver-core:3.24.2")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.3")

    // Eclipse dependencies
    api("org.eclipse.platform:org.eclipse.core.runtime:3.25.0") {
        // For some reason, this group name is wrong
        exclude("org.osgi.service", "org.osgi.service.prefs")
    }
    api("org.osgi", "org.osgi.service.prefs", "1.1.2")
    api("com.ibm.icu:icu4j:71.1")

    // CDT
    api("org.eclipse.cdt:core:7.2.100.202105180159")

    // openCypher
    api("org.opencypher:parser-9.0:9.0.20210312")
    api("org.scala-lang:scala-library:2.12.15") // Dependency of opencypher manually upgraded due to vulnerability

    api("commons-io:commons-io:2.11.0")

    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains:annotations:23.0.0")

    // JUnit
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testFixturesApi("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testFixturesApi("org.jetbrains.kotlin:kotlin-test")
    testFixturesApi("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.2")
    testFixturesApi("org.mockito:mockito-core:4.6.1")
    
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}
