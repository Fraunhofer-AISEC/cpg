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
import de.undercouch.gradle.tasks.download.Download

plugins {
    id("cpg.frontend-conventions")
    alias(libs.plugins.download)
}

mavenPublishing {
    pom {
        name.set("Code Property Graph - Rust Frontend")
        description.set("A Rust language frontend for the CPG")
    }
}

sourceSets { main { kotlin { srcDir("src/main/rust") } } }

dependencies { implementation(libs.jna) }

tasks {
    val downloadLibRustAST by
        registering(Download::class) {
            val version = "v0.0.10"

            src(
                listOf(
                    "https://github.com/Fraunhofer-AISEC/libast/releases/download/${version}/librustast-arm64.dylib",
                    "https://github.com/Fraunhofer-AISEC/libast/releases/download/${version}/librustast-amd64.dylib",
                    "https://github.com/Fraunhofer-AISEC/libast/releases/download/${version}/librustast-arm64.so",
                    "https://github.com/Fraunhofer-AISEC/libast/releases/download/${version}/librustast-amd64.so",
                    "https://github.com/Fraunhofer-AISEC/libast/releases/download/${version}/librustast-amd64.dll",
                )
            )
            dest(projectDir.resolve("build/downloads/rustast"))
            onlyIfModified(true)
        }

    val prepareRustAstResources by
        registering(Copy::class) {
            dependsOn(downloadLibRustAST)

            into(projectDir.resolve("src/main/resources"))

            from("build/downloads/rustast/librustast-amd64.so") {
                into("linux-x86-64")
                rename { "librustast.so" }
            }

            from("build/downloads/rustast/librustast-arm64.so") {
                into("linux-aarch64")
                rename { "librustast.so" }
            }

            from("build/downloads/rustast/librustast-amd64.dylib") {
                into("darwin-x86-64")
                rename { "librustast.dylib" }
            }

            from("build/downloads/rustast/librustast-arm64.dylib") {
                into("darwin-aarch64")
                rename { "librustast.dylib" }
            }

            from("build/downloads/rustast/librustast-amd64.dll") {
                into("win32-x86-64")
                rename { "rustast.dll" }
            }
        }

    processResources { dependsOn(prepareRustAstResources) }

    sourcesJar { dependsOn(prepareRustAstResources) }
}
