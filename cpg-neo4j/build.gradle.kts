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

application {
    mainClass.set("de.fraunhofer.aisec.cpg_vis_neo4j.ApplicationKt")
}

publishing {
    publications {
        named<MavenPublication>("cpg-neo4j") {
            pom {
                artifactId = "cpg-neo4j"
                name.set("Code Property Graph - Neo4j")
                description.set("An Application to translate and persist specified source code as a Code Property Graph to an installed instance of the Neo4j Graph Database.")
            }
        }
    }
}


tasks.withType<Test> {
    useJUnitPlatform {
        if (!project.hasProperty("integration")) {
            excludeTags("integration")
        }
    }
}

val versions = mapOf(
    "neo4j-ogm" to "3.2.8",
    "junit5" to "5.6.0"
)

distributions {
    main {
        contents {
            // add the src/main/python folder to the distribution, so that our python CPG module is available
            from("../cpg-library/src/main/python")
        }
    }
}

dependencies {
    // CPG
    api(project(":cpg-core"))
    api(project(":cpg-language-llvm"))
    api(project(":cpg-language-python"))
    api(project(":cpg-language-go"))

    implementation("org.apache.logging.log4j:log4j-slf4j18-impl:2.17.0")

    // neo4j
    api("org.neo4j", "neo4j-ogm-core", versions["neo4j-ogm"])
    api("org.neo4j", "neo4j-ogm", versions["neo4j-ogm"])
    api("org.neo4j", "neo4j-ogm-bolt-driver", versions["neo4j-ogm"])

    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // JUnit
    testImplementation("org.junit.jupiter", "junit-jupiter-api", versions["junit5"])
    testImplementation("org.junit.jupiter", "junit-jupiter-params", versions["junit5"])
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", versions["junit5"])

    // Command line interface support
    api("info.picocli:picocli:4.6.1")
    annotationProcessor("info.picocli:picocli-codegen:4.6.1")
}
