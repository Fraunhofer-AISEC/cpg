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

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.passes.ComponentPass
import de.fraunhofer.aisec.cpg.passes.ControlFlowSensitiveDFGPass
import de.fraunhofer.aisec.cpg.passes.PassConfiguration
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import java.io.File
import kotlin.script.experimental.api.constructorArgs
import kotlin.script.experimental.api.valueOrThrow
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate
import kotlin.script.experimental.jvmhost.createJvmEvaluationConfigurationFromTemplate

@DependsOn(SymbolResolver::class)
@DependsOn(ControlFlowSensitiveDFGPass::class)
class ConceptScriptPass(ctx: TranslationContext) : ComponentPass(ctx) {

    class Configuration(val scripts: List<File> = listOf()) : PassConfiguration() {}

    override fun accept(t: Component) {
        for (script in passConfig<Configuration>()?.scripts ?: emptyList()) {
            t.executeScript(script)
        }
    }

    override fun cleanup() {
        // Nothing to do
    }
}

fun Component.executeScript(scriptFile: File) {
    val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<QueryScript>()
    val evaluationConfiguration =
        createJvmEvaluationConfigurationFromTemplate<QueryScript>(
            body = { constructorArgs(this@executeScript) }
        )

    val scriptResult =
        BasicJvmScriptingHost()
            .eval(scriptFile.toScriptSource(), compilationConfiguration, evaluationConfiguration)

    val value = scriptResult.valueOrThrow()
    val klass = value.returnValue.scriptClass
    println(klass)
}
