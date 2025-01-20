import groovy.util.Node
import groovy.util.NodeList

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
    id("cpg.application-conventions")
    id("cpg.frontend-dependency-conventions")
}

application {
    mainClass.set("de.fraunhofer.aisec.cpg.codyze.ApplicationKt")
}

publishing {
    publications {
        named<MavenPublication>("codyze") {
            pom {
                artifactId = "codyze"
                name.set("Codyze")
                description.set("The one-stop shop to the code property graph")
                withXml {
                    // Modify the XML to exclude dependencies that start with "cpg-language-".
                    // This is necessary because we do not want to "leak" the dependency to our dynamically activated
                    // frontends to the outside
                    val dependenciesNode = asNode().children().filterIsInstance<Node>().firstOrNull { it.name().toString() == "{http://maven.apache.org/POM/4.0.0}dependencies" }
                    dependenciesNode?.children()?.removeIf {
                        it is Node &&
                                (it.name().toString() == "{http://maven.apache.org/POM/4.0.0}dependency") &&
                                ((it.get("artifactId") as? NodeList)?.text()?.startsWith("cpg-language-") == true)
                    }
                }
            }
        }
    }
}

dependencies {
    implementation(projects.codyzeCompliance)
    implementation(libs.clikt)
}
