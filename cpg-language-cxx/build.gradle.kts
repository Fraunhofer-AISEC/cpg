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
plugins { id("cpg.frontend-conventions") }

mavenPublishing {
    pom {
        name.set("Code Property Graph - C/C++ Frontend")
        description.set("A C/C++ language frontend for the CPG")
    }
}

dependencies {
    // Eclipse dependencies
    implementation(libs.eclipse.runtime) {
        // For some reason, this group name is wrong
        exclude("org.osgi.service", "org.osgi.service.prefs")
    }
    implementation(libs.osgi.service)
    implementation(libs.icu4j)

    // CDT
    implementation(libs.eclipse.cdt.core)

    testImplementation(libs.junit.params)
    testImplementation(project(":cpg-analysis"))
}
