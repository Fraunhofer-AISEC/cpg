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

val compileWindowsX8664 =
    tasks.register<RunDenoTask>("compileWindowsX8664") {
        dependsOn(tasks.installDeno)
        command(
            "compile",
            "-E",
            "-R",
            "--target",
            "x86_64-pc-windows-msvc",
            "-o",
            "build/resources/main/typescript/parser-windows-x86_64",
            "src/main/typescript/src/parser.ts",
        )
        outputs.dir("build/resources/main/typescript")
        outputs.cacheIf { true }
    }

val compileMacOSX8664 =
    tasks.register<RunDenoTask>("compileMacOSX8664") {
        dependsOn(tasks.installDeno)
        command(
            "compile",
            "-E",
            "-R",
            "--target",
            "x86_64-apple-darwin",
            "-o",
            "build/resources/main/typescript/parser-macos-x86_64",
            "src/main/typescript/src/parser.ts",
        )
        outputs.dir("build/resources/main/typescript")
        outputs.cacheIf { true }
    }

val compileMacOSAarch64 =
    tasks.register<RunDenoTask>("compileMacOSAarch64") {
        dependsOn(tasks.installDeno)
        command(
            "compile",
            "-E",
            "-R",
            "--target",
            "aarch64-apple-darwin",
            "-o",
            "build/resources/main/typescript/parser-macos-aarch64",
            "src/main/typescript/src/parser.ts",
        )
        outputs.dir("build/resources/main/typescript")
        outputs.cacheIf { true }
    }

val compileLinuxX8664 =
    tasks.register<RunDenoTask>("compileLinuxX8664") {
        dependsOn(tasks.installDeno)
        command(
            "compile",
            "-E",
            "-R",
            "--target",
            "x86_64-unknown-linux-gnu",
            "-o",
            "build/resources/main/typescript/parser-linux-x86_64",
            "src/main/typescript/src/parser.ts",
        )
        outputs.dir("build/resources/main/typescript")
        outputs.cacheIf { true }
    }

val compileLinuxAarch64 =
    tasks.register<RunDenoTask>("compileLinuxAarch64") {
        dependsOn(tasks.installDeno)
        command(
            "compile",
            "-E",
            "-R",
            "--target",
            "aarch64-unknown-linux-gnu",
            "-o",
            "build/resources/main/typescript/parser-linux-aarch64",
            "src/main/typescript/src/parser.ts",
        )
        outputs.dir("build/resources/main/typescript")
        outputs.cacheIf { true }
    }

tasks.processResources {
    dependsOn(
        compileWindowsX8664,
        //        compileMacOSX8664,
        compileMacOSAarch64,
        compileLinuxX8664,
        compileLinuxAarch64,
    )
}
