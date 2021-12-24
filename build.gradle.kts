/*
 * Copyright (c) 2019-2021, Fraunhofer AISEC. All rights reserved.
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

    id("org.sonarqube") version "3.3"
    id("com.diffplug.spotless") version "6.1.0"
    kotlin("jvm") version "1.6.0" apply false
}

allprojects {
    group = "de.fraunhofer.aisec"
}

tasks.named("sonarqube") {
    subprojects.forEach {
        dependsOn(it.tasks.withType<JacocoReport>())
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "jacoco")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.diffplug.spotless")

    repositories {
        mavenCentral()

        ivy {
            setUrl("https://download.eclipse.org/tools/cdt/releases/10.3/cdt-10.3.2/plugins")
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
    }

    tasks.withType<JacocoReport> {
        reports {
            xml.required.set(true)
        }
    }

    tasks.withType<JavaCompile> {
        dependsOn("spotlessApply")
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            // Note: this is not recommended, but this is the only way to deal with the fact,
            // that the kotlin interactive shell is built with 1.8 and we need to build our
            // inline functions in 1.8 bytecode, otherwise we cannot use the fluid API in
            // the cpg-console.
            jvmTarget = "1.8"
            freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
        }
    }

    val headerWithStars = """/*
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
            googleJavaFormat("1.11.0")
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
}
