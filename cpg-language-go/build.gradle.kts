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
plugins {
    id("cpg.frontend-conventions")
}

publishing {
    publications {
        named<MavenPublication>("cpg-language-go") {
            pom {
                artifactId = "cpg-language-go"
                name.set("Code Property Graph - Go Frontend")
                description.set("A Go language frontend for the CPG")
            }
        }
    }
}

dependencies {
    implementation("net.java.dev.jna:jna:5.16.0")
    testImplementation(project(":cpg-analysis"))
}

open class DownloadTask @Inject constructor(@Internal val op: ExecOperations) : DefaultTask()

val downloadLibGoAST = tasks.register<DownloadTask>("downloadLibGoAST") {
    doLast {
        op.exec {
            commandLine("./download.sh")
                .setStandardOutput(System.out)
                .workingDir(project.projectDir.resolve("src/main/resources"))
        }
    }
}

tasks.processResources {
    dependsOn(downloadLibGoAST)
}
