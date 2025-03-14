import com.github.gradle.node.npm.task.NpmTask

plugins {
    id("cpg.application-conventions")
    kotlin("plugin.serialization")
    id("io.ktor.plugin") version "2.3.7"
    alias(libs.plugins.node)
}

dependencies {
    // CPG modules
    implementation(projects.cpg.codyzeCore)
    implementation(projects.cpg.cpgCore)
    implementation(projects.cpg.cpgConcepts)
    implementation(projects.cpg.cpgLanguagePython)

    // Ktor server dependencies
    implementation("io.ktor:ktor-server-core-jvm:2.3.7")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.7")
    implementation("io.ktor:ktor-server-cors:2.3.7")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    implementation("io.ktor:ktor-server-html-builder:2.3.7")
    implementation("io.ktor:ktor-server-status-pages:2.3.7")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // Logging
    implementation("org.apache.logging.log4j:log4j-api:2.20.0")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.20.0")

    // Testing
    testImplementation("io.ktor:ktor-server-tests-jvm:2.3.7")
}

node {
    download.set(true)
    version.set("20.11.1")
    nodeProjectDir.set(file("${project.projectDir.resolve("src/main/webapp")}"))
}

val npmBuild by
    tasks.registering(NpmTask::class) {
        inputs.file("src/main/webapp/package.json").withPathSensitivity(PathSensitivity.RELATIVE)
        inputs
            .file("src/main/webapp/package-lock.json")
            .withPathSensitivity(PathSensitivity.RELATIVE)
        inputs.dir("src/main/webapp/src").withPathSensitivity(PathSensitivity.RELATIVE)
        outputs.dir("build/resources/static")
        outputs.cacheIf { true }

        workingDir.set(file("src/main/nodejs"))
        npmCommand.set(listOf("run", "build"))
        dependsOn(tasks.getByName("npmInstall"))
    }

application { mainClass.set("de.fraunhofer.aisec.cpg.webconsole.MainKt") }
