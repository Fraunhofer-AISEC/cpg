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
import kotlin.reflect.full.functions
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate
import kotlin.script.experimental.jvmhost.createJvmEvaluationConfigurationFromTemplate

fun TranslationResult.evalQuery(scriptFile: File, queryFunc: String): Any? {
    val evalCtx = QueryScriptContext(this)

    var b = Benchmark(TranslationResult::class.java, "Compiling query script $scriptFile")
    val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<QueryScript>()
    val evaluationConfiguration =
        createJvmEvaluationConfigurationFromTemplate<QueryScript> { implicitReceivers(evalCtx) }

    val scriptResult =
        BasicJvmScriptingHost()
            .eval(scriptFile.toScriptSource(), compilationConfiguration, evaluationConfiguration)

    val value = scriptResult.valueOrThrow()
    val klass = value.returnValue.scriptClass
    val func = klass?.functions?.firstOrNull { it.name == queryFunc }
    if (func == null) {
        throw IllegalArgumentException("Query function $queryFunc not found in script")
    }
    b.stop()

    b = Benchmark(TranslationResult::class.java, "Executing query function $queryFunc")
    val ret = func.call(value.returnValue.scriptInstance, this)
    b.stop()
    return ret
}
