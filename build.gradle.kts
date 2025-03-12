import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.MavenPublishBaseExtension

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

//
// configure multi module dokka documentation
//
plugins {
    id("org.jetbrains.dokka")
    id("test-report-aggregation")
}

// this is needed for the plugins block
repositories {
    mavenCentral()
}

allprojects {
    plugins.apply("org.jetbrains.dokka")

    group = "de.fraunhofer.aisec"

    val dokkaPlugin by configurations
    dependencies {
        dokkaPlugin("org.jetbrains.dokka:versioning-plugin:2.0.0")
    }
}

// configure dokka for the multi-module cpg project
// this works together with the dokka configuration in the common-conventions plugin
tasks.dokkaHtmlMultiModule {
    val configuredVersion = project.version.toString()
    if(configuredVersion.isNotEmpty() && configuredVersion != "unspecified") {
        generateDokkaWithVersionTag(this, configuredVersion)
    } else {
        generateDokkaWithVersionTag(this, "main")
    }
}

/**
 * Takes the old dokka sites in build/dokkaCustomMultiModuleOutput/versions and generates a new site.
 * This new site contains the old ones, so copying the newly generated site to the gh page is enough.
 * Currently, the mkdocs plugin expects it in docs/dokka/latest. The tags in the dropdown will be
 * named based on what we configured here.
 */
fun generateDokkaWithVersionTag(dokkaMultiModuleTask: org.jetbrains.dokka.gradle.AbstractDokkaParentTask, tag: String) {
    val oldOutputPath = projectDir.resolve("previousDocs")
    val id = "org.jetbrains.dokka.versioning.VersioningPlugin"
    val config = """{ "version": "$tag", "olderVersionsDir":"${oldOutputPath.path}" }"""
    val mapOf = mapOf(id to config)

    dokkaMultiModuleTask.outputDirectory.set(file(layout.buildDirectory.asFile.get().resolve("dokkaCustomMultiModuleOutput").resolve(tag)))
    dokkaMultiModuleTask.pluginsMapConfiguration.set(mapOf)
}

dependencies {
    testReportAggregation(project(":cpg-core"))
}

//
// Load the properties that define which frontends to include
//
// this code block also exists in settings.gradle.kts
val enableJavaFrontend: Boolean by extra {
    val enableJavaFrontend: String? by project
    enableJavaFrontend.toBoolean()
}
project.logger.lifecycle("Java frontend is ${if (enableJavaFrontend) "enabled" else "disabled"}")

val enableCXXFrontend: Boolean by extra {
    val enableCXXFrontend: String? by project
    enableCXXFrontend.toBoolean()
}
project.logger.lifecycle("C/C++ frontend is ${if (enableCXXFrontend) "enabled" else "disabled"}")

val enableGoFrontend: Boolean by extra {
    val enableGoFrontend: String? by project
    enableGoFrontend.toBoolean()
}
project.logger.lifecycle("Go frontend is ${if (enableGoFrontend) "enabled" else "disabled"}")

val enablePythonFrontend: Boolean by extra {
    val enablePythonFrontend: String? by project
    enablePythonFrontend.toBoolean()
}
project.logger.lifecycle("Python frontend is ${if (enablePythonFrontend) "enabled" else "disabled"}")

val enableLLVMFrontend: Boolean by extra {
    val enableLLVMFrontend: String? by project
    enableLLVMFrontend.toBoolean()
}
project.logger.lifecycle("LLVM frontend is ${if (enableLLVMFrontend) "enabled" else "disabled"}")

val enableTypeScriptFrontend: Boolean by extra {
    val enableTypeScriptFrontend: String? by project
    enableTypeScriptFrontend.toBoolean()
}
project.logger.lifecycle("TypeScript frontend is ${if (enableTypeScriptFrontend) "enabled" else "disabled"}")

val enableRubyFrontend: Boolean by extra {
    val enableRubyFrontend: String? by project
    enableRubyFrontend.toBoolean()
}
project.logger.lifecycle("Ruby frontend is ${if (enableRubyFrontend) "enabled" else "disabled"}")

val enableJVMFrontend: Boolean by extra {
    val enableJVMFrontend: String? by project
    enableJVMFrontend.toBoolean()
}
project.logger.lifecycle("JVM frontend is ${if (enableJVMFrontend) "enabled" else "disabled"}")

val enableINIFrontend: Boolean by extra {
    val enableINIFrontend: String? by project
    enableINIFrontend.toBoolean()
}
project.logger.lifecycle("INI frontend is ${if (enableINIFrontend) "enabled" else "disabled"}")
