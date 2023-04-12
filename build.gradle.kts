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
    id("org.sonarqube")
}

// this is needed for the plugins block
repositories {
    mavenCentral()
}

// configure dokka for the multi-module cpg project
// this works together with the dokka configuration in the common-conventions plugin
tasks.dokkaHtmlMultiModule {
    outputDirectory.set(buildDir.resolve("dokkaCustomMultiModuleOutput"))
}

//
// Configure sonarqube for the whole cpg project
//
sonarqube {
    properties {
        property("sonar.sourceEncoding", "UTF-8")
        // The report part is either relative to the submodules or the main module. We want to specify our
        // aggregated jacoco report here
        property("sonar.coverage.jacoco.xmlReportPaths", "../cpg-all/build/reports/jacoco/testCodeCoverageReport/testCodeCoverageReport.xml,cpg-all/build/reports/jacoco/testCodeCoverageReport/testCodeCoverageReport.xml")
    }
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

val enablePythonQiskitFrontend by extra {
    val enablePythonQiskitFrontend: String by project
    enablePythonQiskitFrontend.toBoolean()
}
project.logger.lifecycle("Python Qiskit is ${if (enablePythonQiskitFrontend) "enabled" else "disabled"}")

val enableLLVMFrontend by extra {
    val enableLLVMFrontend: String by project
    enableLLVMFrontend.toBoolean()
}
project.logger.lifecycle("LLVM frontend is ${if (enableLLVMFrontend) "enabled" else "disabled"}")

val enableTypeScriptFrontend: Boolean by extra {
    val enableTypeScriptFrontend: String? by project
    enableTypeScriptFrontend.toBoolean()
}
project.logger.lifecycle("TypeScript frontend is ${if (enableTypeScriptFrontend) "enabled" else "disabled"}")
