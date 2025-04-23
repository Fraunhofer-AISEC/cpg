import io.github.masch0212.deno.RunDenoTask

plugins {
    id("codyze.module-conventions")
    kotlin("plugin.serialization")
    alias(libs.plugins.ktor)
    alias(libs.plugins.deno)
}

mavenPublishing {
    pom {
        name.set("Codyze - Console")
        description.set("The web-based console of Codyze")
    }
}

dependencies {
    // CPG modules
    implementation(projects.cpgConcepts)

    // Ktor server dependencies
    implementation(libs.bundles.ktor)

    // Serialization
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.jacksonyml)

    // Testing
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
    integrationTestImplementation(libs.ktor.server.test.host)
    integrationTestImplementation(libs.ktor.client.content.negotiation)
    integrationTestImplementation(libs.ktor.serialization.kotlinx.json)
    // We depend on the Python frontend for the integration tests, but the frontend is only
    // available if enabled.
    // If it's not available, the integration tests fail (which is ok). But if we would directly
    // reference the project here, the build system would fail any task since it will not find a
    // non-enabled project.
    findProject(":cpg-language-python")?.also { integrationTestImplementation(it) }
}

val viteBuild =
    tasks.register<RunDenoTask>("viteBuild") {
        dependsOn(tasks.installDeno)
        command("run", "build")
        workingDir = "src/main/webapp"
        outputs.dir("src/main/resources/static")
        outputs.cacheIf { true }
    }

application { mainClass.set("de.fraunhofer.aisec.codyze.console.MainKt") }

tasks.processResources { dependsOn(viteBuild) }

var jarTasks = tasks.withType<Jar>()

jarTasks.forEach { it.dependsOn(viteBuild) }
