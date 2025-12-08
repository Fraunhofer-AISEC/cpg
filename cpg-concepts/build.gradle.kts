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
plugins { id("cpg.frontend-conventions") }

mavenPublishing {
    pom {
        name.set("Code Property Graph - Concepts")
        description.set("A 'concepts' extension for the CPG")
    }
}

dependencies {
    implementation(projects.cpgAnalysis)

    // parsing YAML files
    implementation(libs.jacksonyml)

    // We depend on the Python and C/C++ frontend for the integration tests, but the frontend is
    // only available if enabled.
    // If it's not available, the integration tests fail (which is ok). But if we would directly
    // reference the
    // project here, the build system would fail any task since it will not find a non-enabled
    // project.
    findProject(":cpg-language-python")?.also { integrationTestImplementation(it) }
    findProject(":cpg-language-cxx")?.also { integrationTestImplementation(it) }
    findProject(":cpg-language-ini")?.also { integrationTestImplementation(it) }
    integrationTestImplementation(projects.cpgAnalysis)

    // concept generator deps
    implementation("com.squareup:kotlinpoet:1.16.0")
    // owl-api
    implementation("net.sourceforge.owlapi:owlapi-distribution:4.5.4")
    implementation("net.sourceforge.owlapi:org.semanticweb.hermit:1.4.5.519")

    // roaster
    implementation("org.jboss.forge.roaster:roaster-api:2.22.2.Final")
    implementation("org.jboss.forge.roaster:roaster-jdt:2.22.2.Final")
    implementation("org.apache.jena:jena-arq:3.4.0")
    implementation(libs.reflections)
}

// ---- Run the generator ----
tasks.register<JavaExec>("runSemanticNodeGenerator") {
    group = "application"
    description = "Runs de.fraunhofer.aisec.cpg.SemanticNodeGenerator"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("de.fraunhofer.aisec.cpg.SemanticNodeGenerator")
}
