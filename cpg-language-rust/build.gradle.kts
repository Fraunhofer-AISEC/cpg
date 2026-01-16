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
plugins { id("cpg.frontend-conventions") }

mavenPublishing {
    pom {
        name.set("Code Property Graph - Rust Frontend")
        description.set("A Rust language frontend for the CPG")
    }
}

sourceSets { main { kotlin { srcDir("src/main/rust") } } }

dependencies { implementation(libs.jna) }

// Register a task that runs “cargo build”
val cargoBuild by
    tasks.registering(Exec::class) {
        group = "build"
        description = "Build Rust release"
        workingDir = file("src/main/rust")
        commandLine("cargo", "build", "--release")
    }

val generateBindings by
    tasks.registering(Exec::class) {
        group = "build"
        description = "Generate uniffi bindings"
        workingDir = file("src/main/rust")
        // Run the binding generation only after a successful build
        dependsOn(cargoBuild)
        commandLine(
            "cargo",
            "run",
            "--bin",
            "uniffi-bindgen",
            "generate",
            "--library",
            "target/release/libcpgrust.so",
            "--language",
            "kotlin",
            "--out-dir",
            "out",
        )
    }

tasks.named("build") { dependsOn(cargoBuild, generateBindings) }

val nativeSrc =
    layout.projectDirectory.dir(
        "src/main/rust/target/release"
    ) // adjust path to where cargo builds .so
val nativeName = "libcpgrust.so"
val resourceSubdir = "linux-x86-64" // Todo Can I extend this to all possible targets?

tasks.register<Copy>("copyRustSharedLibToResources") {
    // Todo Extend this to also do the cargo build and bindings generation part
    from(nativeSrc.file(nativeName))
    into(layout.buildDirectory.dir("resources/main"))
    doFirst {
        println("Copying native lib from ${nativeSrc.file(nativeName).asFile} to resources...")
    }
}

tasks.named("processResources") {
    dependsOn(cargoBuild, generateBindings, "copyRustSharedLibToResources")
}
