/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // built-in
    //java
    //`java-library`
    jacoco
    signing

    id("org.sonarqube") version "3.1.1"
    id("com.diffplug.spotless") version "5.12.4"
    id("com.github.johnrengelman.shadow") version "7.0.0" apply false
    kotlin("jvm") version "1.4.32" apply false
}

group = "de.fraunhofer.aisec"

tasks.named("sonarqube") {
    dependsOn(":jacocoTestReport")
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "jacoco")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.diffplug.spotless")

    repositories {
        mavenCentral()

        ivy {
            setUrl("https://download.eclipse.org/tools/cdt/releases/10.2/cdt-10.2.0/plugins")
            metadataSources {
                artifact()
            }
            patternLayout {
                artifact("/[organisation].[module]_[revision].[ext]")
            }
        }
    }

    tasks.withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"

        //withSourcesJar()
        //withJavadocJar()
    }

    tasks.withType<JacocoReport> {
        reports {
            xml.isEnabled = true
        }
    }

    tasks.withType<JavaCompile> {
        dependsOn("spotlessApply")
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "11"
            freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
        }
    }

    var headerWithStars = """/*
 * Copyright (c) ${"$"}YEAR, Fraunhofer AISEC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *                    ${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}\  ${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}\   ${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}\
 *                   ${'$'}${'$'}  __${'$'}${'$'}\ ${'$'}${'$'}  __${'$'}${'$'}\ ${'$'}${'$'}  __${'$'}${'$'}\
 *                   ${'$'}${'$'} /  \__|${'$'}${'$'} |  ${'$'}${'$'} |${'$'}${'$'} /  \__|
 *                   ${'$'}${'$'} |      ${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}  |${'$'}${'$'} |${'$'}${'$'}${'$'}${'$'}\
 *                   ${'$'}${'$'} |      ${'$'}${'$'}  ____/ ${'$'}${'$'} |\_${'$'}${'$'} |
 *                   ${'$'}${'$'} |  ${'$'}${'$'}\ ${'$'}${'$'} |      ${'$'}${'$'} |  ${'$'}${'$'} |
 *                   \${'$'}${'$'}${'$'}${'$'}${'$'}   |${'$'}${'$'} |      \${'$'}${'$'}${'$'}${'$'}${'$'}   |
 *                    \______/ \__|       \______/
 *
 */
"""

    var headerWithHashes = """#
# Copyright (c) ${"$"}YEAR, Fraunhofer AISEC. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
#                    ${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}\  ${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}\   ${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}\
#                   ${'$'}${'$'}  __${'$'}${'$'}\ ${'$'}${'$'}  __${'$'}${'$'}\ ${'$'}${'$'}  __${'$'}${'$'}\
#                   ${'$'}${'$'} /  \__|${'$'}${'$'} |  ${'$'}${'$'} |${'$'}${'$'} /  \__|
#                   ${'$'}${'$'} |      ${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}${'$'}  |${'$'}${'$'} |${'$'}${'$'}${'$'}${'$'}\
#                   ${'$'}${'$'} |      ${'$'}${'$'}  ____/ ${'$'}${'$'} |\_${'$'}${'$'} |
#                   ${'$'}${'$'} |  ${'$'}${'$'}\ ${'$'}${'$'} |      ${'$'}${'$'} |  ${'$'}${'$'} |
#                   \${'$'}${'$'}${'$'}${'$'}${'$'}   |${'$'}${'$'} |      \${'$'}${'$'}${'$'}${'$'}${'$'}   |
#                    \______/ \__|       \______/
#
"""

    spotless {
        java {
            targetExclude(
                    fileTree(project.projectDir) {
                        include("build/generated-src/**")
                    }
            )
            googleJavaFormat()
            licenseHeader(headerWithStars).yearSeparator(" - ")
        }
        kotlin {
            ktfmt().kotlinlangStyle()
            licenseHeader(headerWithStars).yearSeparator(" - ")
        }

        python {
            target("src/main/**/*.py")
            licenseHeader(headerWithHashes, "from").yearSeparator(" - ")
        }

        format("golang") {
            target("src/main/golang/**/*.go")
            licenseHeader(headerWithStars, "package").yearSeparator(" - ")
        }
    }

    if (project.hasProperty("experimental")) {
        tasks.register("compileGolang") {
            doLast {
                project.exec {
                    commandLine("./build.sh")
                            .setStandardOutput(System.out)
                            .workingDir("src/main/golang")
                }
            }
        }

        tasks.named("compileJava") {
            dependsOn(":compileGolang")
        }
    }
}