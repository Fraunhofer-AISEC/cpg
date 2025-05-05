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

import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.passes.concepts.ConceptAssignmentContext
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.jvm.util.classpathFromClassloader
import kotlin.script.templates.ScriptTemplateDefinition

/**
 * Abstract base class for concept scripts. Concept scripts are Kotlin scripts that can be used to
 * define new concept classes and "tag" them on the CPG. The script must define one.
 *
 * This class is the scription definition (template) needed for the Kotlin compiler to recognize
 * this as a script.
 */
@ScriptTemplateDefinition(scriptFilePattern = ".*\\.concept\\.kts")
@KotlinScript(
    // File extension for the script type
    fileExtension = "concept.kts",
    // Compilation configuration for the script type
    compilationConfiguration = ConceptScriptConfiguration::class,
)
abstract class ConceptScript(val c: Component) {

    fun assign(block: ConceptAssignmentContext.() -> Unit) {
        return de.fraunhofer.aisec.cpg.passes.concepts.assign(block)
    }
}

/**
 * Configuration for the Kotlin compiler to compile concept scripts.
 *
 * It configures the classpath and imports needed for the script to compile and run.
 */
object ConceptScriptConfiguration :
    ScriptCompilationConfiguration({
        baseClass(ConceptScript::class)
        jvm {
            val libraries =
                setOf(
                    "codyze-core",
                    "cpg-core",
                    "cpg-concepts",
                    "cpg-analysis",
                    "kotlin-stdlib",
                    "kotlin-reflect",
                )
            val cp = classpathFromClassloader(QueryScript::class.java.classLoader)
            checkNotNull(cp) { "Could not read classpath" }
            updateClasspath(cp.filter { element -> libraries.any { it in element.toString() } })
        }
        compilerOptions("-Xcontext-receivers", "-jvm-target=21")
        defaultImports.append(
            "de.fraunhofer.aisec.codyze.*",
            "de.fraunhofer.aisec.cpg.*",
            "de.fraunhofer.aisec.cpg.graph.*",
        )
        ide { acceptedLocations(ScriptAcceptedLocation.Everywhere) }
    }) {
    private fun readResolve(): Any = ConceptScriptConfiguration
}
