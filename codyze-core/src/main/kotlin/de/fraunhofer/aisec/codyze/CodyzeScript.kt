/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.codyze

import de.fraunhofer.aisec.codyze.dsl.IncludeBuilder
import de.fraunhofer.aisec.codyze.dsl.ProjectBuilder
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.jvm.util.classpathFromClassloader
import kotlin.script.templates.ScriptTemplateDefinition

/**
 * Abstract base class for Codyze scripts. Codyze scripts are Kotlin scripts that can be used to
 * - Define a project structure that describes the "target of evaluation" (ToE) for the analysis as
 *   well as requirements the ToE must fulfill
 * - Define queries that evaluate whether the requirements are met
 *
 * This class is the scription definition (template) needed for the Kotlin compiler to recognize
 * this as a script.
 */
@ScriptTemplateDefinition(scriptFilePattern = ".*\\.codyze\\.kts")
@KotlinScript(
    // File extension for the script type
    fileExtension = "codyze.kts",
    // Compilation configuration for the script type
    compilationConfiguration = CodyzeScriptCompilationConfiguration::class,
)
abstract class CodyzeScript(internal var projectBuilder: ProjectBuilder) {

    internal var includeBuilder: IncludeBuilder = IncludeBuilder()
}

val baseLibraries =
    arrayOf(
        "codyze-core",
        "cpg-core",
        "cpg-concepts",
        "cpg-analysis",
        "kotlin-stdlib",
        "kotlin-reflect",
    )

/**
 * Contains the configuration for the compilation of Codyze scripts. This includes the imports that
 * are required and some specifications of the compiler options.
 */
class CodyzeScriptCompilationConfiguration :
    ScriptCompilationConfiguration({
        defaultImports.append(
            "de.fraunhofer.aisec.codyze.*",
            "de.fraunhofer.aisec.codyze.dsl.*",
            "de.fraunhofer.aisec.cpg.*",
            "de.fraunhofer.aisec.cpg.graph.*",
            "de.fraunhofer.aisec.cpg.query.*",
            "de.fraunhofer.aisec.cpg.passes.concepts.*",
            "de.fraunhofer.aisec.cpg.assumptions.*",
        )
        jvm {
            val cp = classpathFromClassloader(CodyzeScript::class.java.classLoader)
            checkNotNull(cp) { "Could not read classpath" }
            updateClasspath(cp)
        }
        compilerOptions(
            "-opt-in=kotlin.experimental.ExperimentalTypeInference",
            "-Xcontext-parameters",
            "-jvm-target=21",
        )
        ide { acceptedLocations(ScriptAcceptedLocation.Everywhere) }
    })
