import de.fraunhofer.aisec.cpg.helpers.Benchmark
import com.github.gradle.node.npm.task.NpmTask
import com.github.gradle.node.npm.task.NpmInstallTask

plugins {
    // Apply the common convention plugins for shared build configuration between modules.
    id("cpg.java-library-conventions")
    id("cpg.testing-conventions")
    // Add node plugin
    id("com.github.node-gradle.node") version "7.0.1"
}

// Required for accessing the Version Catalog in the plugins block
// See: https://docs.gradle.org/8.0/userguide/platforms.html#sub:using-standard-java-platform-plugins
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlin.jvm)
}

apply<Benchmark>()

dependencies {
    // Declare dependencies on other modules within this multi-project build.
    api(project(":cpg-core"))
    // Potentially needed later if reusing JS/TS parts:
    // implementation(project(":cpg-language-typescript"))

    // Other standard dependencies (check if needed)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)
    implementation(libs.slf4j.api)
    implementation(libs.jackson.core)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.annotations)
    implementation(libs.jackson.datatype.jsr310)

    // Test dependencies
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.assertj.core)
    testImplementation(libs.mockk)
    testImplementation(project(":cpg-core")).capabilities {
        // Request the test fixtures capability
        requireCapability("de.fraunhofer.aisec.cpg:cpg-core-test-fixtures")
    }
}

// --- Node.js Integration --- 
node {
    // Version of node to use.
    version.set("18.17.1")

    // Version of npm to use.
    npmVersion.set("9.6.7")

    // If true, it will download node using above parameters.
    // If false, it will use globally installed node.
    download.set(true)
    // Set the work directory to the build directory to avoid polluting the source tree
    workDir.set(file("${layout.buildDirectory}/nodejs"))
    // Set the node_modules directory
    nodeModulesDir.set(file(".")) // Use root for node_modules relative to package.json
}

// Task to install npm dependencies (runs `npm install`)
// Use NpmInstallTask for better caching and standard practice
tasks.register<NpmInstallTask>("npmInstallDev") {
    dependsOn(tasks.nodeSetup)
    args.set(listOf("--dev")) // Install devDependencies
    // Ensure package.json and potentially package-lock.json are inputs
    inputs.file("package.json")
    inputs.file("package-lock.json").optional(true)
    // Define outputs - the node_modules directory
    outputs.dir("node_modules")
}

// Task to compile the TypeScript parser script using tsc (runs `npm run build`)
tasks.register<NpmTask>("compileSvelteParser") {
    dependsOn(tasks.npmInstallDev) 
    args.set(listOf("run", "build"))
    // Define inputs: tsconfig and source files
    inputs.file("tsconfig.json")
    inputs.dir("src/main/nodejs")
    // Define outputs: the dist directory
    outputs.dir("dist")
}

// We need to compile the parser before compiling Kotlin code that might use it
tasks.compileKotlin {
    dependsOn(tasks.compileSvelteParser)
    doFirst {
        // Make sure, the compiled parser is placed into the resources folder
        copy {
            from(layout.projectDirectory.dir("dist"))
            include("parser.js")
            into(layout.buildDirectory.dir("resources/main"))
            println("Copied parser.js to resources")
        }
    }
}

java {
    // Needed to include the node parser script in the final jar
    sourceSets["main"].resources {
       srcDirs("build/resources/main")
       include("parser.js")
    }
} 