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
import de.fraunhofer.aisec.cpg.query.QueryTree
import io.github.detekt.sarif4k.Result
import java.io.File
import kotlin.reflect.full.functions
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate
import kotlin.script.experimental.jvmhost.createJvmEvaluationConfigurationFromTemplate

data class QueryResult(val tree: QueryTree<Boolean>, val sarif: List<Result>)

/**
 * Evaluates a query script with the given query function name on the [TranslationResult]. It uses
 * the [BasicJvmScriptingHost] to execute the script. The function must be defined in the script
 * file and take a single argument of type [TranslationResult].
 *
 * @param scriptFile The script file to evaluate
 * @param queryFunc The name of the query function to call
 * @return The result of the query function
 */
fun TranslationResult.evalQuery(scriptFile: File, queryFunc: String, ruleID: String): QueryResult {
    var b = Benchmark(TranslationResult::class.java, "Compiling query script $scriptFile")
    val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<QueryScript>()
    val evaluationConfiguration = createJvmEvaluationConfigurationFromTemplate<QueryScript>()

    val scriptResult =
        BasicJvmScriptingHost()
            .eval(scriptFile.toScriptSource(), compilationConfiguration, evaluationConfiguration)

    val value = scriptResult.valueOrThrow()
    val klass = value.returnValue.scriptClass
    val func = klass?.functions?.firstOrNull { it.name == queryFunc }
    if (func == null) {
        throw IllegalArgumentException("Query function $queryFunc not found in script")
    }

    // Check, if the return type is correct
    if (
        func.returnType.classifier != QueryTree::class ||
            func.returnType.arguments.firstOrNull()?.type?.classifier != Boolean::class
    ) {
        throw IllegalArgumentException("Query function $queryFunc must return a QueryTree<Boolean>")
    }
    b.stop()

    b = Benchmark(TranslationResult::class.java, "Executing query function $queryFunc")
    @Suppress("UNCHECKED_CAST")
    val ret = func.call(value.returnValue.scriptInstance, this) as QueryTree<Boolean>
    val res = QueryResult(ret, ret.toSarif(ruleID))
    b.stop()

    return res
}
