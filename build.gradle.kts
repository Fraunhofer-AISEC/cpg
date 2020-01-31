/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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
    // built-in
    java
    `java-library`
    jacoco
    signing
    `maven-publish`

    id("org.sonarqube") version "2.8"
    id("com.diffplug.gradle.spotless") version "3.27.1"
    id("com.github.johnrengelman.shadow") version "5.2.0"
    id("net.researchgate.release") version "2.6.0"
}

tasks.jacocoTestReport {
    reports {
        xml.isEnabled = true
    }
}

group = "de.fraunhofer.aisec"

extra["isReleaseVersion"] = !version.toString().endsWith("SNAPSHOT")

val mavenCentralUri: String
    get() {
        val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2"
        val snapshotsRepoUrl = "https://oss.sonatype.org/content/repositories/snapshots"
        return if (project.extra["isReleaseVersion"] as Boolean) releasesRepoUrl else snapshotsRepoUrl
    }

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set("Code Property Graph")
                description.set("A simple library to extract a code property graph out of source code. It has support for multiple passes that can extend the analysis after the graph is constructed.")
                url.set("https://github.com/Fraunhofer-AISEC/cpg")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("oxisto")
                        organization.set("Fraunhofer AISEC")
                        organizationUrl.set("https://www.aisec.fraunhofer.de")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com:Fraunhofer-AISEC/cpg.git")
                    developerConnection.set("scm:git:ssh://github.com:Fraunhofer-AISEC/cpg.git")
                    url.set("https://github.com/Fraunhofer-AISEC/cpg")
                }
            }
        }
    }

    repositories {
        maven {
            url = uri(mavenCentralUri)

            credentials {
                val mavenCentralUsername: String? by project
                val mavenCentralPassword: String? by project

                username = mavenCentralUsername
                password = mavenCentralPassword
            }
        }
    }
}

repositories {
    mavenCentral()

    ivy {
        setUrl("https://download.eclipse.org/tools/cdt/releases/9.10/cdt-9.10.0/plugins")
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

signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications["maven"])
}

tasks.withType<Sign>().configureEach {
    onlyIf { project.extra["isReleaseVersion"] as Boolean }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    maxHeapSize = "4048m"
}

tasks.named("compileJava") {
    dependsOn(":spotlessApply")
}

tasks.named("sonarqube") {
    dependsOn(":jacocoTestReport")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11

    withSourcesJar()
    withJavadocJar()
}

val versions = mapOf(
        "neo4j-ogm" to "3.1.7",
        "junit5" to "5.6.0",
        "commons-lang3" to "3.9",
        "log4j" to "2.13.0",
        "slf4j" to "1.8.0-beta4",
        "javaparser" to "3.15.10",
        "cdt" to "6.10.0.201912051559",
        "icu4j" to "65.1"
)

dependencies {
    api("org.apache.commons", "commons-lang3", versions["commons-lang3"])
    api("org.neo4j", "neo4j-ogm-core", versions["neo4j-ogm"])
    api("org.apache.logging.log4j", "log4j-slf4j18-impl", versions["log4j"])
    api("org.slf4j", "jul-to-slf4j", versions["slf4j"])
    api("com.github.javaparser", "javaparser-symbol-solver-core", versions["javaparser"])

    // Eclipse dependencies
    api("org.eclipse.platform", "org.eclipse.core.runtime", "3.17.0")

    // CDT
    api("org.eclipse.cdt", "core", versions["cdt"])

    // JUnit
    testImplementation("org.junit.jupiter", "junit-jupiter-api", versions["junit5"])
    testImplementation("org.junit.jupiter", "junit-jupiter-params", versions["junit5"])
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", versions["junit5"])
}

spotless {
    java {
        targetExclude(
                fileTree(project.projectDir) {
                    include("build/generated-src/**")
                }
        )
        googleJavaFormat()
    }
}

release {
    tagTemplate = "v${version}"
}
