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
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

import com.google.protobuf.gradle.*

plugins {
    // built-in
    java
    `java-library`
    jacoco
    signing
    `maven-publish`

    id("org.sonarqube") version "3.1.1"
    id("com.diffplug.spotless") version "5.11.0"
    id("com.github.johnrengelman.shadow") version "6.1.0"
    id("com.google.protobuf") version "0.8.8"
    kotlin("jvm") version "1.4.20"
}

tasks.jacocoTestReport {
    reports {
        xml.isEnabled = true
    }
}

group = "de.fraunhofer.aisec"

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
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")

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
        setUrl("https://download.eclipse.org/tools/cdt/releases/9.11/cdt-9.11.1/plugins")
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

tasks.named<Test>("test") {
    useJUnitPlatform()
    maxHeapSize = "4048m"
}

tasks.named("compileJava") {
    dependsOn(":spotlessApply")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "11"
    freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
}

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "11"
    freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
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

dependencies {
    api("org.apache.commons:commons-lang3:3.12.0")
    api("org.neo4j:neo4j-ogm-core:3.2.19")
    api("org.apache.logging.log4j:log4j-slf4j18-impl:2.14.1")
    api("org.slf4j:jul-to-slf4j:1.8.0-beta4")
    api("com.github.javaparser:javaparser-symbol-solver-core:3.20.0")

    // Eclipse dependencies
    api("org.eclipse.platform:org.eclipse.core.runtime:3.18.0")
    api("com.ibm.icu:icu4j:67.1")

    // CDT
    api("org.eclipse.cdt:core:6.11.1.202006011430")

    // openCypher
    api("org.opencypher:parser-9.0:9.0.20210312")

    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    // JUnit
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.7.1")

    testImplementation("org.mockito:mockito-core:3.8.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.1")

    // Protobuf
    api("com.google.protobuf:protobuf-java:3.13.0")
    api("io.grpc:grpc-stub:1.31.1")
    api("io.grpc:grpc-protobuf:1.31.1")
    if (JavaVersion.current().isJava9Compatible) {
        implementation("javax.annotation:javax.annotation-api:1.3.2")
    }
}

sourceSets {
    main {
        java {
            srcDir("build/generated/source/proto/main/java")
            srcDir("build/generated/source/proto/main/grpc")
        }
    }
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

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.13.0"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.31.1"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                id("grpc")
            }
        }
    }
}
