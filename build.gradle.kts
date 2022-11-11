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
}

repositories {
    mavenCentral()
}

tasks.dokkaHtmlMultiModule {
    outputDirectory.set(buildDir.resolve("dokkaCustomMultiModuleOutput"))
}

//
// Load the properties that define which frontends to include
//
// this code block also exists in settings.gradle.kts
val enableGoFrontend by extra {
    val enableGoFrontend: String by project
    enableGoFrontend.toBoolean()
}
project.logger.lifecycle("Go frontend is ${if (enableGoFrontend) "enabled" else "disabled"}")

val enablePythonFrontend by extra {
    val enablePythonFrontend: String by project
    enablePythonFrontend.toBoolean()
}
project.logger.lifecycle("Python frontend is ${if (enablePythonFrontend) "enabled" else "disabled"}")

val enableLLVMFrontend by extra {
    val enableLLVMFrontend: String by project
    enableLLVMFrontend.toBoolean()
}
project.logger.lifecycle("LLVM frontend is ${if (enableLLVMFrontend) "enabled" else "disabled"}")

val enableTypeScriptFrontend by extra {
    val enableTypeScriptFrontend: String by project
    enableTypeScriptFrontend.toBoolean()
}
project.logger.lifecycle("TypeScript frontend is ${if (enableTypeScriptFrontend) "enabled" else "disabled"}")
