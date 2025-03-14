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
import com.github.gradle.node.npm.task.NpmTask

plugins {
    id("cpg.frontend-conventions")
    alias(libs.plugins.node)
}

mavenPublishing {
    pom {
        name.set("Code Property Graph - JavaScript/TypeScript Frontend")
        description.set("A JavaScript/TypeScript language frontend for the CPG")
    }
}

node {
    download.set(true)
    version.set("20.11.1")
    nodeProjectDir.set(file("${project.projectDir.resolve("src/main/nodejs")}"))
}

val npmBuild by
    tasks.registering(NpmTask::class) {
        inputs.file("src/main/nodejs/package.json").withPathSensitivity(PathSensitivity.RELATIVE)
        inputs
            .file("src/main/nodejs/package-lock.json")
            .withPathSensitivity(PathSensitivity.RELATIVE)
        inputs.dir("src/main/nodejs/src").withPathSensitivity(PathSensitivity.RELATIVE)
        outputs.dir("build/resources/main/nodejs")
        outputs.cacheIf { true }

        workingDir.set(file("src/main/nodejs"))
        npmCommand.set(listOf("run", "bundle"))
        dependsOn(tasks.getByName("npmInstall"))
    }

tasks.processResources { dependsOn(npmBuild) }
tasks.sourcesJar { dependsOn(npmBuild)}
