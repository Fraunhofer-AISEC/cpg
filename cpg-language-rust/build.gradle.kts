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

val nativeSrc =
    layout.projectDirectory.dir(
        "src/main/rust/target/release"
    ) // adjust path to where cargo builds .so
val nativeName = "libcpgrust.so"
val resourceSubdir = "linux-x86-64" // Todo Can I extend this to all possible targets?

tasks.register<Copy>("copyRustSharedLibToResources") {
    // Todo Extend this to also do the cargo build and bindings generation part
    from(nativeSrc.file(nativeName))
    into(layout.buildDirectory.dir("resources/main/$resourceSubdir"))
    doFirst {
        println("Copying native lib from ${nativeSrc.file(nativeName).asFile} to resources...")
    }
}

tasks.named("processResources") { dependsOn("copyRustSharedLibToResources") }
