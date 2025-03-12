/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
    id("cpg.common-conventions")
    id("cpg.publishing-conventions")
}

mavenPublishing {
    pom {
        name.set("Codyze - Core")
        description.set("Core module of the Codyze static analysis tool")
    }
}

dependencies {
    api(libs.sarif4k)
    implementation(libs.clikt)
    implementation(projects.cpgCore)
    api(projects.cpgAnalysis)
    testImplementation(kotlin("test"))

    // Script definition
    api("org.jetbrains.kotlin:kotlin-scripting-common")
    api("org.jetbrains.kotlin:kotlin-scripting-jvm")

    // Scripting host
    api("org.jetbrains.kotlin:kotlin-scripting-jvm-host")

    // We depend on the Python frontend for the integration tests, but the frontend is only available if enabled.
    // If it's not available, the integration tests fail (which is ok). But if we would directly reference the
    // project here, the build system would fail any task since it will not find a non-enabled project.
    findProject(":cpg-language-python")?.also {
        integrationTestImplementation(it)
    }
    integrationTestImplementation(projects.cpgAnalysis)
}
