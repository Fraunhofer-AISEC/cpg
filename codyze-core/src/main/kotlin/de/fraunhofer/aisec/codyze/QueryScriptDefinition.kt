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

import de.fraunhofer.aisec.cpg.TranslationResult
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.jvm.util.classpathFromClassloader
import kotlin.script.templates.ScriptTemplateDefinition

@ScriptTemplateDefinition(scriptFilePattern = ".*\\.query\\.kts")
@KotlinScript(
    // File extension for the script type
    fileExtension = "query.kts",
    // Compilation configuration for the script type
    compilationConfiguration = QueryScriptConfiguration::class,
)
abstract class QueryScript

open class QueryScriptContext(val result: TranslationResult)

object QueryScriptConfiguration :
    ScriptCompilationConfiguration({
        baseClass(QueryScript::class)
        jvm {
            val libraries =
                setOf("codyze-core", "cpg-core", "cpg-analysis", "kotlin-stdlib", "kotlin-reflect")
            val cp = classpathFromClassloader(QueryScript::class.java.classLoader)
            checkNotNull(cp) { "Could not read classpath" }
            updateClasspath(cp.filter { element -> libraries.any { it in element.toString() } })
        }
        implicitReceivers(QueryScriptContext::class)
        compilerOptions("-Xcontext-receivers", "-jvm-target=17")
        defaultImports.append(
            "de.fraunhofer.aisec.codyze.*",
            "de.fraunhofer.aisec.cpg.*",
            "de.fraunhofer.aisec.cpg.graph.*",
            "de.fraunhofer.aisec.cpg.query.*",
        )
        ide { acceptedLocations(ScriptAcceptedLocation.Everywhere) }
    }) {
    private fun readResolve(): Any = QueryScriptConfiguration
}
