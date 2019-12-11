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

val deployUsername: String? by extra // imported from settings.gradle.kts
val deployPassword: String? by extra // imported from settings.gradle.kts

plugins {
    // built-in
    java
    `java-library`
    jacoco
    signing
    `maven-publish`

    id("org.sonarqube") version "2.6"
    id("com.diffplug.gradle.spotless") version "3.26.0"
}

group = "de.fraunhofer.aisec"
version = "1.2-SNAPSHOT"

val mavenCentralUri: String
  get() {
    val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2"
    val snapshotsRepoUrl = "https://oss.sonatype.org/content/repositories/snapshots"
    return if ((version as String).endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
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
                        name.set("Christian Banse")
                        email.set("christian.banse@aisec.fraunhofer.de")
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
        setUrl("https://download.eclipse.org/tools/cdt/releases/9.6/cdt-9.6.0/plugins")
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

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11

    withSourcesJar()
    withJavadocJar()
}

tasks {
    sonarqube {
        properties {
            "sonar.host.url" to "localhost"
        }
    }
}

val versions = mapOf(
        "neo4j-ogm" to "3.1.7",
        "junit5" to "5.3.1",
        "commons-lang3" to "3.8.1",
        "log4j" to "2.11.1",
        "javaparser" to "3.11.0"
)

dependencies {
    compile("org.apache.commons", "commons-lang3", versions["commons-lang3"])
    compile("org.neo4j", "neo4j-ogm-core", versions["neo4j-ogm"])
    compile("org.apache.logging.log4j", "log4j-slf4j18-impl", versions["log4j"])
    compile("org.slf4j", "jul-to-slf4j", "1.8.0-beta2")
    compile("com.github.javaparser", "javaparser-symbol-solver-core", versions["javaparser"])

    compile("com.ibm.icu", "icu4j", "63.1")

    // seriously eclipse...
    compile("org.eclipse.platform", "org.eclipse.osgi", "3.13.200")
    compile("org.eclipse.platform", "org.eclipse.equinox.common", "3.10.200")
    compile("org.eclipse.platform", "org.eclipse.equinox.preferences", "3.7.200")
    compile("org.eclipse.platform", "org.eclipse.core.runtime", "3.15.100")
    compile("org.eclipse.platform", "org.eclipse.core.jobs", "3.10.200")
    compile("org.eclipse.cdt", "core", "6.6.0.201812101042")
    // compile("org.eclipse.cdt", "core", "6.6.0.201812101042")

    runtime("org.neo4j", "neo4j-ogm-bolt-driver", versions["neo4j-ogm"])

    // api stuff
    //compile("org.glassfish.hk2", "hk2-core", "2.5.0-b62")
    //compile("org.glassfish.jersey.core", "jersey-server", "2.28")
    compile("org.glassfish.jersey.inject", "jersey-hk2", "2.28")
    //compile("org.glassfish.jersey.containers", "jersey-container-servlet", "2.28")
    compile("org.glassfish.jersey.containers", "jersey-container-grizzly2-http", "2.28")
    compile("org.glassfish.jersey.media", "jersey-media-json-jackson", "2.28")

    // needed for jersey, not part of JDK anymore
    compile("javax.xml.bind", "jaxb-api", "2.3.1")

    testImplementation("org.junit.jupiter", "junit-jupiter-api", versions["junit5"])
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", versions["junit5"])
}

//tasks.register<Jar>("sourcesJar") {
//    from(sourceSets.main.get().allJava)
//    archiveClassifier.set("sources")
//}

tasks.named<Test>("test") {
    useJUnitPlatform()
    maxHeapSize="4048m"
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
