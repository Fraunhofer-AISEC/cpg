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

    id("net.researchgate.release") version "2.6.0"
    kotlin("jvm")
}

application {
    mainClassName = "de.fraunhofer.aisec.cpg_vis_neo4j.ApplicationKt"
}

group = "de.fraunhofer.aisec"

extra["isReleaseVersion"] = !version.toString().endsWith("SNAPSHOT")

val mavenCentralUri: String
    get() {
        val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2"
        val snapshotsRepoUrl = "https://oss.sonatype.org/content/repositories/snapshots"
        return if (project.extra["isReleaseVersion"] as Boolean) releasesRepoUrl else snapshotsRepoUrl
    }

repositories {
    mavenCentral()

    ivy {
        setUrl("https://download.eclipse.org/tools/cdt/releases/10.2/cdt-10.2.0/plugins")
        metadataSources {
            artifact()
        }
        patternLayout {
            artifact("/[organisation].[module]_[revision].[ext]")
        }
    }
}

tasks.withType<GenerateModuleMetadata> {
    enabled = false
}

tasks.withType<Sign>().configureEach {
    onlyIf { project.extra["isReleaseVersion"] as Boolean }
}

tasks.named<Test>("test") {
    useJUnitPlatform {

        if (!project.hasProperty("integration")) {
            excludeTags("integration")
        }
    }
}

val versions = mapOf(
        "neo4j-ogm" to "4.0.0",
        "neo4j-ogm-old" to "3.2.8",
        "junit5" to "5.6.0",
        "cpg" to "3.5.1"
)

dependencies {
    // CPG
    api(project(":cpg-library"))

    // neo4j
    api("org.neo4j", "neo4j-ogm-core", versions["neo4j-ogm-old"])
    api("org.neo4j", "neo4j-ogm", versions["neo4j-ogm-old"])
    api("org.neo4j", "neo4j-ogm-bolt-driver", versions["neo4j-ogm-old"])

    // JUnit
    testImplementation("org.junit.jupiter", "junit-jupiter-api", versions["junit5"])
    testImplementation("org.junit.jupiter", "junit-jupiter-params", versions["junit5"])
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", versions["junit5"])

    implementation(kotlin("stdlib-jdk8"))

    // Command line interface support
    api("info.picocli:picocli:4.1.4")
    annotationProcessor("info.picocli:picocli-codegen:4.1.4")
}
