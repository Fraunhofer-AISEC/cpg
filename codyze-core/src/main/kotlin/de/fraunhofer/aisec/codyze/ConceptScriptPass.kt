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
import de.fraunhofer.aisec.cpg.passes.ControlFlowSensitiveDFGPass
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import de.fraunhofer.aisec.cpg.passes.concepts.ConceptTagPass
import de.fraunhofer.aisec.cpg.passes.concepts.TaggingContext
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import java.io.File
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate
import kotlin.script.experimental.jvmhost.createJvmEvaluationConfigurationFromTemplate

@DependsOn(SymbolResolver::class)
@DependsOn(ControlFlowSensitiveDFGPass::class)
class ConceptScriptPass(ctx: TranslationContext) : ConceptTagPass(ctx) {

    class Configuration(val scriptFile: File) :
        ConceptTagPass.Configuration(tag = TaggingContext()) {
        init {
            val compilationConfiguration =
                createJvmCompilationConfigurationFromTemplate<ConceptScript>()
            val evaluationConfiguration =
                createJvmEvaluationConfigurationFromTemplate<ConceptScript>()

            val scriptResult =
                BasicJvmScriptingHost()
                    .eval(
                        scriptFile.toScriptSource(),
                        compilationConfiguration,
                        evaluationConfiguration,
                    )

            if (scriptResult is ResultWithDiagnostics.Success) {
                val ctx =
                    (scriptResult.value.returnValue as? ResultValue.Value)?.value as? TaggingContext
                if (ctx != null) {
                    tag = ctx
                    log.info("Fetched tagging concept from script")
                }
            } else {
                log.error("Error while evaluating script: {}", scriptResult)
            }
        }
    }
}
