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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.translationResult
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

@DependsOn(LoopingPass2::class)
class LoopingPass1(ctx: TranslationContext) : TranslationResultPass(ctx) {
    override fun cleanup() {}

    override fun accept(t: TranslationResult) {
        t.markDirty<LoopingPass2>()
        t.markClean()
        counter++
    }

    companion object {
        var counter: Int = 0
    }
}

class LoopingPass2(ctx: TranslationContext) : TranslationResultPass(ctx) {
    override fun cleanup() {}

    override fun accept(t: TranslationResult) {
        t.markDirty<LoopingPass1>()
        t.markClean()
        counter++
    }

    companion object {
        var counter: Int = 0
    }
}

class DirtyCleanTest {
    @Test
    fun testMaxExecutions() {
        val result =
            with(
                TestLanguageFrontend(
                    ctx =
                        TranslationContext(
                            config =
                                TranslationConfiguration.builder()
                                    .registerPass<LoopingPass1>()
                                    .registerPass<LoopingPass2>()
                                    .maxPassExecutions(2)
                                    .build()
                        )
                )
            ) {
                translationResult {}
            }

        assertNotNull(result)
        assertEquals(2, LoopingPass1.counter)
        assertEquals(2, LoopingPass2.counter)
    }

    /**
     * Regression test: two independent call sites marking the same node dirty for the same pass in
     * a row (e.g. [de.fraunhofer.aisec.cpg.updateIncrementally] directly marking a new function
     * dirty, plus its stale-stub cleanup separately marking the same function dirty as a call's
     * caller) must not insert duplicate entries -- otherwise a single [TranslationResult.markClean]
     * call only removes one of the duplicates and the node stays dirty forever.
     */
    @Test
    fun testMarkDirtyDedupsSamePassForSameNode() {
        val result =
            with(
                TestLanguageFrontend(
                    ctx = TranslationContext(config = TranslationConfiguration.builder().build())
                )
            ) {
                translationResult {
                    val component = components.single()
                    markDirty(component, LoopingPass1::class)
                    markDirty(component, LoopingPass1::class)
                }
            }

        val component = result.components.single()
        assertEquals(1, result.dirtyNodes[component]?.count { it == LoopingPass1::class })

        result.markClean(component, LoopingPass1::class)
        assertFalse(result.dirtyNodes[component].orEmpty().contains(LoopingPass1::class))
    }
}
