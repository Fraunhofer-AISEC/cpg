import com.github.gradle.node.npm.task.NpmTask

plugins {
    id("cpg.application-conventions")
    kotlin("plugin.serialization")
    alias(libs.plugins.ktor)
    alias(libs.plugins.node)
}

dependencies {
    // CPG modules
    implementation(projects.codyzeCore)
    implementation(projects.cpgCore)
    implementation(projects.cpgConcepts)
    implementation(projects.cpgLanguagePython)

    // Ktor server dependencies
    implementation(libs.bundles.ktor)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Testing
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
}

node {
    download.set(true)
    version.set("20.9.0")
    nodeProjectDir.set(file("${project.projectDir.resolve("src/main/webapp")}"))
}

val npmBuild by
    tasks.registering(NpmTask::class) {
        inputs.file("src/main/webapp/package.json").withPathSensitivity(PathSensitivity.RELATIVE)
        inputs
            .file("src/main/webapp/package-lock.json")
            .withPathSensitivity(PathSensitivity.RELATIVE)
        inputs.dir("src/main/webapp/src").withPathSensitivity(PathSensitivity.RELATIVE)
        outputs.dir("src/main/resources/static")
        outputs.cacheIf { true }

        workingDir.set(file("src/main/webapp"))
        npmCommand.set(listOf("run", "build"))
        dependsOn(tasks.getByName("npmInstall"))
    }

application { mainClass.set("de.fraunhofer.aisec.codyze.console.MainKt") }

tasks.processResources { dependsOn(npmBuild) }

var jarTasks = tasks.withType<Jar>()

jarTasks.forEach { it.dependsOn(npmBuild) }
