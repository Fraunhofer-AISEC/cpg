/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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
import com.github.gradle.node.yarn.task.YarnTask

plugins {
    id("cpg.frontend-conventions")
    alias(libs.plugins.node)
}

publishing {
    publications {
        named<MavenPublication>("cpg-language-typescript") {
            pom {
                artifactId = "cpg-language-typescript"
                name.set("Code Property Graph - JavaScript/TypeScript Frontend")
                description.set("A JavaScript/TypeScript language frontend for the CPG")
            }
        }
    }
}

node {
    download.set(findProperty("nodeDownload")?.toString()?.toBoolean() ?: false)
    version.set("16.4.2")
}

val yarnInstall by tasks.registering(YarnTask::class) {
    inputs.file("src/main/nodejs/package.json").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file("src/main/nodejs/yarn.lock").withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir("src/main/nodejs/node_modules")
    outputs.cacheIf { true }

    workingDir.set(file("src/main/nodejs"))
    yarnCommand.set(listOf("install", "--ignore-optional"))
}

val yarnBuild by tasks.registering(YarnTask::class) {
    inputs.file("src/main/nodejs/package.json").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file("src/main/nodejs/yarn.lock").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.dir("src/main/nodejs/src").withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir("build/resources/main/nodejs")
    outputs.cacheIf { true }

    workingDir.set(file("src/main/nodejs"))
    yarnCommand.set(listOf("bundle"))

    dependsOn(yarnInstall)
}

tasks.processResources {
    dependsOn(yarnBuild)
}
