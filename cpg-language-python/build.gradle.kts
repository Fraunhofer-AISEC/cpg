import java.nio.file.Path
import kotlin.io.path.*
import kotlin.io.path.Path
import kotlin.io.path.exists

/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *                    $$$$$$\  $$$$$$$\   $$$$$$\
 *                   $$  __$$\ $$  __$$\ $$  __$$\
 *                   $$ /  \__|$$ |  $$ |$$ /  \__|
 *                   $$ |      $$$$$$$  |$$ |$$$$\
 *                   $$ |      $$  ____/ $$ |\_$$ |
 *                   $$ |  $$\ $$ |      $$ |  $$ |
 *                   \$$$$$   |$$ |      \$$$$$   |
 *                    \______/ \__|       \______/
 *
 */

val jepDistroVersion = "4.1.1"
// Python version can be set with `-Ppython` property: `./gradlew cpg-language-python:build -Ppython=3.12`, default is 3.10
val pythonVersion = if (project.hasProperty("python")) project.property("python") else "3.10"
val jepCpgDir: Path = Path(projectDir.path, "build", "jep")
val regularJepPathInVirtualEnv: Path = Path(
    "lib", "python${pythonVersion}", "site-packages", "jep"
)

plugins {
    id("cpg.frontend-conventions")
}

publishing {
    publications {
        named<MavenPublication>("cpg-language-python") {
            pom {
                artifactId = "cpg-language-python"
                name.set("Code Property Graph - Python Frontend")
                description.set("A Python language frontend for the CPG")
            }
        }
    }
}

/**
 * Selecting JEP from existingExtracting JEP native binaries provided by https://github.com/icemachined/jep-distro
 * to the 'cpg-language-python/jep' directory. Python version can be set with `-Ppython` property:
 * `./gradlew cpg-language-python:build -Ppython=3.12`, the default is `3.10`
 */
tasks.register("installJep", Copy::class) {
    // If the `build/jep` directory already exists, we will remove it; otherwise, this could
    // cause cache-like issues when this directory is cached and not updated
    if (jepCpgDir.exists()) {
        @OptIn(ExperimentalPathApi::class) jepCpgDir.deleteRecursively()
    }

    // Python and JEP from a Virtual Environment: lib/python***/site-packages/jep
    val jepInVirtualEnvPath = Path(
        System.getProperty("user.home"), ".virtualenvs", System.getenv("CPG_PYTHON_VIRTUALENV") ?: "cpg"
    ) / regularJepPathInVirtualEnv

    // It's straightforward to install JEP on macOS using Homebrew, and the relative directory will align with that in a Virtual Env.
    val homeBrewPath = Path("/opt", "homebrew") / regularJepPathInVirtualEnv

    // If the user has specified the environment variable CPG_JEP_LIBRARY with the path to JEP, we will prioritize reading it
    val explicitJepLibraryPathFromEnv = System.getenv("CPG_JEP_LIBRARY")?.let { Path(it) }

    // Priority order:
    // 1. explicitly provided JEP Path from env. variable
    // 2. JEP from virtual env.
    // 3. JEP from mac's homebrew
    val jepPath = when {
        explicitJepLibraryPathFromEnv != null -> if (explicitJepLibraryPathFromEnv.exists()) explicitJepLibraryPathFromEnv else throw IllegalStateException(
            "CPG_JEP_LIBRARY environment variable set as '${explicitJepLibraryPathFromEnv}' but the path does not exist."
        )

        jepInVirtualEnvPath.exists() -> jepInVirtualEnvPath
        homeBrewPath.exists() -> homeBrewPath
        else -> null
    }

    // Based on the host OS we will determine the extension for the JEP binary
    val os = System.getProperty("os.name")
    val jepBinary = "libjep." + when {
        os.contains("Mac") -> "jnilib"
        os.contains("Linux") -> "so"
        os.contains("Windows") -> "dll"
        else -> throw IllegalStateException("Cannot install JEP for this operating system: [$os]")
    }

    // If JEP already exists on the current host machine, there's no need to copy it from the distribution archive.
    // We will copy it as-is to the build/jep directory. Otherwise, we will extract an archive from the distribution.
    // Please note that this distribution contains only x86 builds, meaning it won't work for ARM machines (like Mac M1 and later generations).
    if (jepPath != null && (jepPath / jepBinary).exists()) {
        from(fileTree(jepPath))
        into(jepCpgDir)
    } else {
        // As mentioned above, ARM machines are not supported, so you need to provide your own JEP distribution
        // in that case with CPG_JEP_LIBRARY, or other installations like homebrew, virtual env., etc.
        if (!System.getProperty("os.arch").contains("x86")) {
            throw IllegalStateException(
                """
                    | Your system architecture, identified as <${System.getProperty("os.arch")}>, is not supported in icemachined/jep-distro. 
                    | Consequently, you are required to install it manually. Here are the potential solutions:
                    |  1. Utilize Homebrew/pip package manager to facilitate the JEP installation process; 
                    |  2. Create a virtual environment with the specified environment variable CPG_PYTHON_VIRTUALENV set to /(user.home)/.virtualenv/CPG_PYTHON_VIRTUALENV;
                    |  3. Manually install JEP and specify the CPG_JEP_LIBRARY environment variable with the appropriate path to the installation.
                """.trimMargin()
            )
        }

        // We added com.icemachined:jep-distro as a dependency, so TGZ archive containing the JEP distribution is downloaded by Gradle.
        // This archive is stored in the Gradle dependency storage, allowing us to unpack it and copy its contents to the build/jep directory.
        val jepDistroFromDependencies = File(configurations.compileClasspath.get().asFileTree.map { it.path }
            .find { it.endsWith("jep-distro-cp$pythonVersion-$jepDistroVersion.tgz") }!!)
        from(tarTree(jepDistroFromDependencies))
        into(jepCpgDir.parent)
    }
}

/**
 * All tasks that are related to the compilation will depend on the installation of JEP
 */
tasks.named("compileKotlin") {
    dependsOn("installJep")
}

tasks.named("sourcesJar") {
    dependsOn("installJep")
}

tasks.named("processTestResources") {
    dependsOn("installJep")
}

tasks.named("spotlessKotlin") {
    dependsOn("installJep")
}

// In Python CPG spotlessGolang task is not needed, but anyway is incorrectly added to each project
tasks.named("spotlessGolang").configure {
    enabled = false
}

dependencies {
    // jep for python support
    api(libs.jep)
    // JNI binaries for JEP made by @icemachined and published to central (just all binaries in one archive)
    implementation("com.icemachined:jep-distro-cp$pythonVersion:$jepDistroVersion")
    // to evaluate some test cases
    testImplementation(project(":cpg-analysis"))
}

