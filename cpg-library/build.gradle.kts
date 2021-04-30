plugins {
    `java-library`
    `maven-publish`
    signing
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


tasks.withType<GenerateModuleMetadata> {
    enabled = false
}

tasks.named<Test>("test") {
    useJUnitPlatform {
        if (!project.hasProperty("experimental")) {
            excludeTags("experimental")
        } else {
            systemProperty("java.library.path", project.projectDir.resolve("src/main/golang"))
        }
    }
    maxHeapSize = "4048m"
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project

    useInMemoryPgpKeys(signingKey, signingPassword)

    setRequired({
        gradle.taskGraph.hasTask("publish")
    })

    sign(publishing.publications["maven"])
}

dependencies {
    api("org.apache.commons:commons-lang3:3.12.0")
    api("org.neo4j:neo4j-ogm-core:3.2.19")
    api("org.apache.logging.log4j:log4j-slf4j18-impl:2.14.1")
    api("org.slf4j:jul-to-slf4j:1.8.0-beta4")
    api("com.github.javaparser:javaparser-symbol-solver-core:3.20.2")

    // Eclipse dependencies
    api("org.eclipse.platform:org.eclipse.core.runtime:3.20.100")
    api("com.ibm.icu:icu4j:68.2")

    // CDT
    api("org.eclipse.cdt:core:7.2.0.202102251239")

    // openCypher
    api("org.opencypher:parser-9.0:9.0.20210312")

    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    // jep for python support
    api("black.ninia:jep:3.9.1")

    // JUnit
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.7.1")

    testImplementation("org.mockito:mockito-core:3.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.1")
}