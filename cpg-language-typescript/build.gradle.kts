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

import io.github.masch0212.deno.RunDenoTask

plugins {
    id("cpg.frontend-conventions")
    alias(libs.plugins.deno)
}

mavenPublishing {
    pom {
        name.set("Code Property Graph - JavaScript/TypeScript Frontend")
        description.set("A JavaScript/TypeScript language frontend for the CPG")
    }
}

/*node {
    download.set(true)
    version.set(libs.versions.node)
    nodeProjectDir.set(file("${project.projectDir.resolve("src/main/nodejs")}"))
}

val pnpmBuild by
    tasks.registering(PnpmTask::class) {
        inputs.file("src/main/nodejs/package.json").withPathSensitivity(PathSensitivity.RELATIVE)
        inputs.file("src/main/nodejs/pnpm-lock.yaml").withPathSensitivity(PathSensitivity.RELATIVE)
        inputs.dir("src/main/nodejs/src").withPathSensitivity(PathSensitivity.RELATIVE)
        outputs.dir("build/resources/main/nodejs")
        outputs.cacheIf { true }

        workingDir.set(file("src/main/nodejs"))
        pnpmCommand.set(listOf("run", "bundle"))
        dependsOn(tasks.getByName("pnpmInstall"))
    }

tasks.processResources { dependsOn(pnpmBuild) }
*/

// deno { version.set(libs.versions.node) }

// Build parser
val compileMacOSx8664 =
    tasks.register<RunDenoTask>("compileMacOSAmd64") {
        dependsOn(tasks.installDeno)
        command(
            "compile",
            "-E",
            "-R",
            "--target",
            "x86_64-apple-darwin",
            "-o",
            "build/resources/main/nodejs/parser-macos-amd64",
            "src/main/nodejs/src/parser.ts",
        )
        outputs.dir("build/resources/main/nodejs")
        outputs.cacheIf { true }
    }

val compileMacOSArm64 =
    tasks.register<RunDenoTask>("compileMacOSArm64") {
        dependsOn(tasks.installDeno)
        command(
            "compile",
            "-E",
            "-R",
            "--target",
            "aarch64-apple-darwin",
            "-o",
            "build/resources/main/nodejs/parser-macos-arm64",
            "src/main/nodejs/src/parser.ts",
        )
        outputs.dir("build/resources/main/nodejs")
        outputs.cacheIf { true }
    }

val compileLinuxX8664 =
    tasks.register<RunDenoTask>("compileLinuxAmd64") {
        dependsOn(tasks.installDeno)
        command(
            "compile",
            "-E",
            "-R",
            "--target",
            "x86_64-unknown-linux-gnu",
            "-o",
            "build/resources/main/nodejs/parser-linux-amd64",
            "src/main/nodejs/src/parser.ts",
        )
        outputs.dir("build/resources/main/nodejs")
        outputs.cacheIf { true }
    }

val compileLinuxArm64 =
    tasks.register<RunDenoTask>("compileLinuxArm64") {
        dependsOn(tasks.installDeno)
        command(
            "compile",
            "-E",
            "-R",
            "--target",
            "aarch64-unknown-linux-gnu",
            "-o",
            "build/resources/main/nodejs/parser-linux-arm64",
            "src/main/nodejs/src/parser.ts",
        )
        outputs.dir("build/resources/main/nodejs")
        outputs.cacheIf { true }
    }

tasks.processResources {
    dependsOn(compileMacOSx8664, compileMacOSArm64, compileLinuxX8664, compileLinuxArm64)
}
