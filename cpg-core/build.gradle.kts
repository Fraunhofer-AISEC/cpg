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
    `java-test-fixtures`
    id("cpg.library-conventions")
}

publishing {
    publications {
        named<MavenPublication>("cpg-core") {
            pom {
                artifactId = "cpg-core"
                name.set("Code Property Graph - Core")
                description.set("A simple library to extract a code property graph out of source code. It has support for multiple passes that can extend the analysis after the graph is constructed.")
            }

            suppressPomMetadataWarningsFor("testFixturesApiElements")
            suppressPomMetadataWarningsFor("testFixturesRuntimeElements")
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xcontext-receivers")
    }
}

tasks.test {
    useJUnitPlatform {
        if (!project.hasProperty("experimental")) {
            excludeTags("experimental")
        }
    }
}

dependencies {
    api(libs.apache.commons.lang3)

    api(libs.neo4j.ogm.core)

    implementation(libs.bundles.log4j)

    api(libs.javaparser)
    api(libs.jackson)

    // Eclipse dependencies
    api(libs.eclipse.runtime) {
        // For some reason, this group name is wrong
        exclude("org.osgi.service", "org.osgi.service.prefs")
    }
    api(libs.osgi.service)
    api(libs.icu4j)

    // CDT
    api(libs.eclipse.cdt.core)

    api(libs.commons.io)

    implementation(libs.kotlin.reflect)
    implementation(libs.jetbrains.annotations)

    testImplementation(libs.junit.params)

    // JUnit
    testFixturesApi(libs.kotlin.test.junit5)  // somehow just using testFixturesApi(kotlin("test")) does not work for testFixtures
    testFixturesApi(libs.mockito)
}
