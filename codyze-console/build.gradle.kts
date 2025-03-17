import com.github.gradle.node.pnpm.task.PnpmTask

plugins {
    id("codyze.module-conventions")
    kotlin("plugin.serialization")
    alias(libs.plugins.ktor)
    alias(libs.plugins.node)
}

dependencies {
    // CPG modules
    implementation(projects.cpgConcepts)

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
    version.set(libs.versions.node)
    nodeProjectDir.set(file("${project.projectDir.resolve("src/main/webapp")}"))
}

val pnpmBuild by
    tasks.registering(PnpmTask::class) {
        inputs.file("src/main/webapp/package.json").withPathSensitivity(PathSensitivity.RELATIVE)
        inputs.file("src/main/webapp/pnpm-lock.yaml").withPathSensitivity(PathSensitivity.RELATIVE)
        inputs.dir("src/main/webapp/src").withPathSensitivity(PathSensitivity.RELATIVE)
        outputs.dir("src/main/resources/static")
        outputs.cacheIf { true }

        workingDir.set(file("src/main/webapp"))
        pnpmCommand.set(listOf("run", "build"))
        dependsOn(tasks.getByName("pnpmInstall"))
    }

application { mainClass.set("de.fraunhofer.aisec.codyze.console.MainKt") }

tasks.processResources { dependsOn(pnpmBuild) }

var jarTasks = tasks.withType<Jar>()

jarTasks.forEach { it.dependsOn(pnpmBuild) }
