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

application {
    mainClass.set("de.fraunhofer.aisec.cpg_vis_neo4j.ApplicationKt")
    // Since we are potentially persisting deeply nested graphs, we need to increase the stack and heap size.
    // Note, that if you are running this IntelliJ, you might need to manually specify this as VM arguments.
    applicationDefaultJvmArgs = listOf("-Xss515m", "-Xmx8g")
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

dependencies {
    // neo4j
    implementation(libs.bundles.neo4j)
    implementation(libs.neo4j.driver)

    // Command line interface support
    implementation(libs.picocli)
    annotationProcessor(libs.picocli.codegen)

    testImplementation(testFixtures(projects.cpgCore))
}
