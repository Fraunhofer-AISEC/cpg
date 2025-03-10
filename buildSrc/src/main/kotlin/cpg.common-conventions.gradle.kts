import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.services.BuildServiceParameters.None
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("cpg.formatting-conventions")

    `java-library`
    `jvm-test-suite`
    jacoco
    signing
    `maven-publish`
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jetbrains.dokka")
}

java {
    withSourcesJar()
}

//
// common repositories
//
repositories {
    mavenCentral()

    ivy {
        setUrl("https://download.eclipse.org/tools/cdt/releases/")
        metadataSources {
            artifact()
        }

        patternLayout {
            artifact("[organisation].[module]_[revision].[ext]")
        }
    }
}

//
// common documentation, signing and publishing configuration
//
// this disables gradle's alternative to POM files, which cause problems when publishing on Maven
tasks.withType<GenerateModuleMetadata> {
    enabled = false
}

val dokkaHtml by tasks.getting(DokkaTask::class)
val javadocJar by tasks.registering(Jar::class) {
    dependsOn(dokkaHtml)
    archiveClassifier.set("javadoc")
    from(dokkaHtml.outputDirectory)
}

publishing {
    publications {
        create<MavenPublication>(name) {
            artifact(javadocJar)
            from(components["java"])

            pom {
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
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Fraunhofer-AISEC/cpg")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project

    useInMemoryPgpKeys(signingKey, signingPassword)

    setRequired({
        gradle.taskGraph.hasTask("publish")
    })

    sign(publishing.publications[name])
}

//
// common compilation configuration
//
// specify Java & Kotlin JVM version
kotlin {
    jvmToolchain(17)
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn", "-opt-in=kotlin.uuid.ExperimentalUuidApi", "-Xcontext-receivers")
    }
}

// Configure our test suites
@Suppress("UnstableApiUsage")
testing {
    suites {
        // The default unit-test suite
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
            targets {
                all {
                    testTask.configure {
                        maxHeapSize = "4048m"
                    }
                }
            }
        }

        // Our integration tests
        val integrationTest by registering(JvmTestSuite::class) {
            description = "Runs the integration tests"
            dependencies {
                implementation(project())
                implementation(testFixtures(project(":cpg-core")))
            }

            // For legacy reasons we also include the unit-test resources in the integration tests,
            // because some of them are shared
            sources {
                resources {
                    srcDirs("src/test/resources")
                }
            }

            targets {
                all {
                    testTask.configure {
                        maxHeapSize = "4048m"
                    }
                }
            }
        }

        // Our performance tests
        val performanceTest by registering(JvmTestSuite::class) {
            description = "Runs the performance tests"
            dependencies {
                implementation(project())
                implementation(testFixtures(project(":cpg-core")))
            }

            targets {
                all {
                    testTask.configure {
                        // do not parallelize tests within the task
                        maxParallelForks = 1
                        // make sure that several performance tests (e.g. in different frontends) also do NOT run in parallel
                        usesService(serialExecutionService)
                    }
                }
            }
        }
    }
}

// A build service that ensures serial execution of a group of tasks
abstract class SerialExecutionService : BuildService<None>
val serialExecutionService =
    gradle.sharedServices.registerIfAbsent("serialExecution", SerialExecutionService::class.java) {
        this.maxParallelUsages.set(1)
    }

// Common dependencies that we need for all modules
val libs = the<LibrariesForLibs>()  // necessary to be able to use the version catalog in buildSrc
dependencies {
    implementation(libs.apache.commons.lang3)
    implementation(libs.neo4j.ogm.core)
    implementation(libs.jackson)
}

