/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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

publishing {
    publications {
        named<MavenPublication>("cpg-language-ruby") {
            pom {
                artifactId = "cpg-language-ruby"
                name.set("Code Property Graph - Ruby")
                description.set("A Ruby language frontend for the CPG")
            }
        }
    }
}

dependencies {
    implementation("org.jruby:jruby-core:9.4.3.0")
}