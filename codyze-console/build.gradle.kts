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

val mcpEnabled = findProject(":cpg-mcp") != null

dependencies {
    // CPG modules
    implementation(projects.cpgConcepts)
    implementation(projects.cpgSerialization)

    // MCP dependencies - only when enabled
    if (mcpEnabled) {
        implementation(project(":cpg-mcp"))
        // MCP SDK - needed for McpServerHelper and custom MCP client
        implementation(libs.mcp)
        // MCP Client SDK - for custom MCP client implementation
        implementation(libs.mcp.client)
    }

    // Ktor server dependencies
    implementation(libs.bundles.ktor)

    // Ktor client dependencies
    implementation("io.ktor:ktor-client-core:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-client-cio:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-client-content-negotiation:${libs.versions.ktor.get()}")

    // Ktor SSE plugin
    implementation("io.ktor:ktor-server-sse:${libs.versions.ktor.get()}")

    implementation(libs.koog.agents)
    implementation(libs.koog.tools)
    implementation(libs.koog.executor.ollama.client)
    implementation(libs.koog.executor.google.client)

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
