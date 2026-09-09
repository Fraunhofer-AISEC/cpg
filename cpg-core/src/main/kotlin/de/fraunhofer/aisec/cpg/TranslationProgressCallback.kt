/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg

import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.passes.Pass
import kotlin.reflect.KClass

/**
 * Callback interface for reporting key progress steps of the CPG construction.
 *
 * Register one or multiple callbacks via [TranslationManager.analyze].
 */
interface TranslationProgressCallback {
    /**
     * Called after all configured frontends finished running.
     *
     * @param ctx: The global [TranslationContext], so we can access it in the callback
     * @param result: The [TranslationResult] containing the results of the analysis, so we can
     *   access it in the callback.
     * @param executedFrontends: The set of [LanguageFrontend] instances that were executed, so we
     *   can access it in the callback.
     */
    fun afterFrontends(
        ctx: TranslationContext,
        result: TranslationResult,
        executedFrontends: Set<LanguageFrontend<*, *>>,
    ) {}

    /**
     * Called after a pass has finished execution. It is executed after every [Pass].
     *
     * @param pass: The pass which has just been executed, so we can access it in the callback.
     * @param ctx: The global [TranslationContext], so we can access it in the callback
     * @param result: The [TranslationResult] containing the results of the analysis, so we can
     *   access it in the callback.
     * @param nodes: The starting nodes on which the [pass] was run.
     */
    fun afterPass(
        pass: KClass<out Pass<out Node>>,
        ctx: TranslationContext,
        result: TranslationResult,
        nodes: Collection<Node>,
    ) {}
}
