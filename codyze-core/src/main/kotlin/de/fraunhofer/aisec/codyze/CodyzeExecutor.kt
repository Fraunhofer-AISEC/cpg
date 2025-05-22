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
import de.fraunhofer.aisec.cpg.helpers.Benchmark
import java.io.File
import kotlin.io.path.Path
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.constructorArgs
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate
import kotlin.script.experimental.jvmhost.createJvmEvaluationConfigurationFromTemplate

fun executeScript(scriptFile: String): CodyzeScript? {
    var b = Benchmark(TranslationResult::class.java, "Compiling query script $scriptFile")
    val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<CodyzeScript>()
    val evaluationConfiguration =
        createJvmEvaluationConfigurationFromTemplate<CodyzeScript>() {
            constructorArgs(Path(scriptFile).parent)
        }

    val scriptResult =
        BasicJvmScriptingHost()
            .eval(
                File(scriptFile).toScriptSource(),
                compilationConfiguration,
                evaluationConfiguration,
            )

    println(scriptResult)
    when (scriptResult) {
        is ResultWithDiagnostics.Failure -> {
            println("Error: ${scriptResult.reports}")
            return null
        }
        is ResultWithDiagnostics.Success -> {
            return scriptResult.value.returnValue.scriptInstance as? CodyzeScript
        }
    }
}
