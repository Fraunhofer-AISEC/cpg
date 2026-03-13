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
        name.set("Code Property Graph - Go Frontend")
        description.set("A Go language frontend for the CPG")
    }
}

dependencies {
    implementation("net.java.dev.jna:jna:5.18.1")
    testImplementation(project(":cpg-analysis"))
}

tasks {
    val downloadLibGoAST by
        registering(Download::class) {
            val version = "v0.0.5"

            src(
                listOf(
                    "https://github.com/Fraunhofer-AISEC/libgoast/releases/download/${version}/libgoast-arm64.dylib",
                    "https://github.com/Fraunhofer-AISEC/libgoast/releases/download/${version}/libgoast-amd64.dylib",
                    "https://github.com/Fraunhofer-AISEC/libgoast/releases/download/${version}/libgoast-arm64.so",
                    "https://github.com/Fraunhofer-AISEC/libgoast/releases/download/${version}/libgoast-amd64.so",
                    "https://github.com/Fraunhofer-AISEC/libgoast/releases/download/${version}/libgoast-amd64.dll",
                )
            )
            dest(projectDir.resolve("src/main/resources"))
            onlyIfModified(true)
        }

    processResources { dependsOn(downloadLibGoAST) }

    sourcesJar { dependsOn(downloadLibGoAST) }
}
