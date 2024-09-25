import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.gradle.api.services.BuildServiceParameters.None;

plugins {
    id("cpg.formatting-conventions")

    `java-library`
    jacoco
    signing
    `maven-publish`
    kotlin("jvm")
    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlinx.kover")
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

//
// common testing configuration
//
tasks.test {
    useJUnitPlatform() {
        excludeTags("integration")
        excludeTags("performance")
    }

    maxHeapSize = "4048m"
}

val integrationTest = tasks.register<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"
    useJUnitPlatform() {
        includeTags("integration")
    }

    maxHeapSize = "4048m"

    shouldRunAfter(tasks.test)
}

val performanceTest = tasks.register<Test>("performanceTest") {
    description = "Runs performance tests."
    group = "verification"
    useJUnitPlatform() {
        includeTags("performance")
    }

    maxHeapSize = "4048m"

    // do not parallelize tests within the task
    maxParallelForks = 1
    // make sure that several performance tests (e.g. in different frontends) also do NOT run in parallel
    usesService(serialExecutionService)

    mustRunAfter(tasks.getByPath(":sonar"))
}

// A build service that ensures serial execution of a group of tasks
abstract class SerialExecutionService : BuildService<BuildServiceParameters.None>
val serialExecutionService =
    gradle.sharedServices.registerIfAbsent("serialExecution", SerialExecutionService::class.java) {
        this.maxParallelUsages.set(1)
    }

kover {
    currentProject {
        instrumentation {
            disabledForTestTasks.add("performanceTest")
        }
    }
}
