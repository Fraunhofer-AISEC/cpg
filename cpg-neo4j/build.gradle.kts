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
    id("cpg.application-conventions")
}

application {
    mainClass.set("de.fraunhofer.aisec.cpg_vis_neo4j.ApplicationKt")
}

java {
    registerFeature("llvmFrontend") {
        usingSourceSet(sourceSets["main"])
    }
    registerFeature("pythonFrontend") {
        usingSourceSet(sourceSets["main"])
    }
    registerFeature("goFrontend") {
        usingSourceSet(sourceSets["main"])
    }
    registerFeature("typeScriptFrontend") {
        usingSourceSet(sourceSets["main"])
    }
}

publishing {
    publications {
        named<MavenPublication>("cpg-neo4j") {
            pom {
                artifactId = "cpg-neo4j"
                name.set("Code Property Graph - Neo4j")
                description.set("An Application to translate and persist specified source code as a Code Property Graph to an installed instance of the Neo4j Graph Database.")
            }
        }
    }
}


tasks.withType<Test> {
    useJUnitPlatform {
        if (!project.hasProperty("integration")) {
            excludeTags("integration")
        }
    }
}

val enableGoFrontend: Boolean by rootProject.extra
val enablePythonFrontend: Boolean by rootProject.extra
val enableLLVMFrontend: Boolean by rootProject.extra
val enableTypeScriptFrontend: Boolean by rootProject.extra

dependencies {
    // the optional language frontends
    if (enableLLVMFrontend) "llvmFrontendImplementation"(project(":cpg-language-llvm"))
    if (enablePythonFrontend) "pythonFrontendImplementation"(project(":cpg-language-python"))
    if (enableGoFrontend) "goFrontendImplementation"(project(":cpg-language-go"))
    if (enableTypeScriptFrontend) "typeScriptFrontendImplementation"(project(":cpg-language-typescript"))

    // neo4j
    api(libs.bundles.neo4j)

    // Command line interface support
    api(libs.picocli)
    annotationProcessor(libs.picocli.codegen)
}
