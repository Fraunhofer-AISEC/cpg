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

import de.fraunhofer.aisec.codyze.dsl.AssumptionDecisions
import de.fraunhofer.aisec.codyze.dsl.IncludeBuilder
import de.fraunhofer.aisec.codyze.dsl.IncludeCategory
import de.fraunhofer.aisec.codyze.dsl.ManualAssessment
import de.fraunhofer.aisec.codyze.dsl.ProjectBuilder
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.helpers.Benchmark
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.pathString
import kotlin.script.experimental.api.RefineScriptCompilationConfigurationHandler
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptConfigurationRefinementContext
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.api.constructorArgs
import kotlin.script.experimental.api.importScripts
import kotlin.script.experimental.api.scriptsInstancesSharing
import kotlin.script.experimental.host.FileBasedScriptSource
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate
import kotlin.script.experimental.jvmhost.createJvmEvaluationConfigurationFromTemplate

/**
 * Evaluates a Codyze script as a [CodyzeScript].
 *
 * Furthermore, each included file inside [IncludeBuilder] is also evaluated and added to the
 * [CodyzeScript.projectBuilder].
 */
fun evaluateScriptAndIncludes(scriptFile: String): CodyzeScript? {
    val script = evaluateScript(scriptFile)
    if (script == null) {
        return null
    }

    // Evaluate any included files based on our project builder
    for (include in script.includeBuilder.includes.values) {
        evaluateScript(
            script.projectBuilder.projectDir.resolve(include).pathString,
            script.projectBuilder,
        )
    }

    return script
}

/** Evaluates a Codyze script from the given file path. */
fun evaluateScript(
    scriptFile: String,
    projectBuilder: ProjectBuilder = ProjectBuilder(projectDir = Path(scriptFile).parent),
): CodyzeScript? {
    val b = Benchmark(TranslationResult::class.java, "Compiling query script $scriptFile")
    val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<CodyzeScript>()
    val evaluationConfiguration =
        createJvmEvaluationConfigurationFromTemplate<CodyzeScript>() {
            constructorArgs(projectBuilder)
            scriptsInstancesSharing(false)
        }

    val scriptResult =
        BasicJvmScriptingHost()
            .eval(
                File(scriptFile).toScriptSource(),
                compilationConfiguration,
                evaluationConfiguration,
            )
    b.stop()

    when (scriptResult) {
        is ResultWithDiagnostics.Failure -> {
            println("Error: ${scriptResult.reports}")
            return null
        }
        is ResultWithDiagnostics.Success -> {
            val retValue = scriptResult.value.returnValue
            val rootInstance = retValue.scriptInstance
            val rootClass =
                retValue.scriptClass
                    ?: throw IllegalStateException(
                        "Script $scriptFile did not return a script class"
                    )

            // Really stupid and dangerous workaround to "inject" the properties of imported scripts
            // into the root instance
            val imported = rootClass.members.filter { it.name.startsWith("$\$imported") }

            return scriptResult.value.returnValue.scriptInstance as? CodyzeScript
        }
    }
}

/**
 * A [RefineScriptCompilationConfigurationHandler] that handles the `include` block in Codyze
 * scripts.
 *
 * It extracts the included files and adds them to the script's compilation configuration using
 * [importScripts].
 */
object CodyzeScriptIncludeHandler : RefineScriptCompilationConfigurationHandler {
    override fun invoke(
        context: ScriptConfigurationRefinementContext
    ): ResultWithDiagnostics<ScriptCompilationConfiguration> {
        val includes = extractIncludes(context.script.text).values
        val scriptBaseDir = (context.script as? FileBasedScriptSource)?.file?.parentFile
        val importedSources =
            includes.filterNotNull().map {
                val file = (scriptBaseDir?.resolve(it) ?: File(it)).normalize()
                file
            }

        return if (includes.isEmpty()) {
            return context.compilationConfiguration.asSuccess()
        } else {
            ScriptCompilationConfiguration(context.compilationConfiguration) {
                    if (importedSources.isNotEmpty())
                        importScripts.append(importedSources.map { FileScriptSource(it) })
                }
                .asSuccess()
        }
    }
}

/** Extracts the included files from a given script content. */
fun extractIncludes(scriptText: String): Map<IncludeCategory, String?> {
    val includeBlockRegex = """include\s*\{([^}]*)}""".toRegex()
    val assumptionDecisionsRegex = """AssumptionDecisions from "([^"]+)"""".toRegex()
    val manualAssessmentRegex = """ManualAssessment from "([^"]+)"""".toRegex()

    val includeBlockContent = includeBlockRegex.find(scriptText)?.groups?.get(1)?.value ?: ""

    val assumptionDecisionsFile =
        assumptionDecisionsRegex.find(includeBlockContent)?.groups?.get(1)?.value
    val manualAssessmentFile =
        manualAssessmentRegex.find(includeBlockContent)?.groups?.get(1)?.value

    return mapOf(
        AssumptionDecisions to assumptionDecisionsFile,
        ManualAssessment to manualAssessmentFile,
    )
}
