import com.github.gradle.node.pnpm.task.PnpmTask

plugins {
    id("codyze.module-conventions")
    kotlin("plugin.serialization")
    alias(libs.plugins.ktor)
    alias(libs.plugins.node)
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
    implementation(projects.cpgSerialization)
    // cpg-ai (MCP server, ChatService, ...) is a real, mandatory dependency of codyze-console,
    // but the module itself stays optional at the settings.gradle.kts level (like the language
    // frontends) for consumers who don't need it. Fail clearly if someone tries to build
    // codyze-console without it, rather than a cryptic "project not found" error.
    implementation(
        findProject(":cpg-ai")
            ?: error(
                "codyze-console requires the cpg-ai module; set enableAIModule=true in gradle.properties"
            )
    )

    // Ktor server dependencies
    implementation(libs.bundles.ktor)

    // Ktor client dependencies
    implementation(libs.bundles.ktor.client)

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

tasks.shadowJar { setProperty("zip64", true) }

// The Shadow plugin - applied transitively by the Ktor plugin, since that also applies the
// `application` plugin - registers `shadowRuntimeElements` as a variant of the `java` component, so
// our publishing conventions upload the fat jar under the `all` classifier. That jar is >2 GB, most
// of it the per-platform native binaries of `org.bytedeco:llvm-platform`, and building plus
// uploading it took roughly 7 of the 9 minutes of the CI publish step. Nothing consumes it from
// Maven Central (the docs run the console via `installDist`), so keep it out of the publication.
// The Shadow plugin wires up the variant in an `afterEvaluate`, so we have to do the same.
afterEvaluate {
    (components["java"] as AdhocComponentWithVariants).withVariantsFromConfiguration(
        configurations["shadowRuntimeElements"]
    ) {
        skip()
    }
}
