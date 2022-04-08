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
    java
    //`java-library`
    jacoco
    signing
    `maven-publish`

    id("org.sonarqube") version "3.3"
    id("com.diffplug.spotless") version "6.4.0"
    kotlin("jvm") version "1.6.20" apply false
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
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

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

    publishing {
        publications {
            create<MavenPublication>(name) {
                from(components["java"])

                pom {
                    url.set("https://github.com/Fraunhofer-AISEC/cpg")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("oxisto")
                            organization.set("Fraunhofer AISEC")
                            organizationUrl.set("https://www.aisec.fraunhofer.de")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com:Fraunhofer-AISEC/cpg.git")
                        developerConnection.set("scm:git:ssh://github.com:Fraunhofer-AISEC/cpg.git")
                        url.set("https://github.com/Fraunhofer-AISEC/cpg")
                    }
                }
            }
        }

        repositories {
            maven {
                url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")

                credentials {
                    val mavenCentralUsername: String? by project
                    val mavenCentralPassword: String? by project

                    username = mavenCentralUsername
                    password = mavenCentralPassword
                }
            }
        }
    }

    signing {
        val signingKey: String? by project
        val signingPassword: String? by project

        useInMemoryPgpKeys(signingKey, signingPassword)

        setRequired({
            gradle.taskGraph.hasTask("publish")
        })

        sign(publishing.publications[name])
    }

    tasks.withType<GenerateModuleMetadata> {
        enabled = false
    }

    tasks.withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    tasks.named<Test>("test") {
        useJUnitPlatform()
    }

    java {
        withJavadocJar()
        withSourcesJar()
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
            freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
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

    val headerWithHashes = """#
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
