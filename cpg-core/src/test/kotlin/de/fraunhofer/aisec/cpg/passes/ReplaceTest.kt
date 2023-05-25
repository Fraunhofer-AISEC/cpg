/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.ScopeManager
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.passes.order.ReplacePass
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class ReplaceTest {

    @ReplacePass(EvaluationOrderGraphPass::class, ReplaceTestLanguage::class, ReplacedPass::class)
    class ReplaceTestLanguageFrontend : TestLanguageFrontend()

    class ReplaceTestLanguage : TestLanguage() {
        override val frontend: KClass<out TestLanguageFrontend>
            get() = ReplaceTestLanguageFrontend::class

        override fun newFrontend(
            config: TranslationConfiguration,
            scopeManager: ScopeManager
        ): TestLanguageFrontend {
            return ReplaceTestLanguageFrontend()
        }
    }

    class ReplacedPass : EvaluationOrderGraphPass()

    @Test
    fun testReplaceAnnotation() {
        val config =
            TranslationConfiguration.builder().registerLanguage<ReplaceTestLanguage>().build()

        assertContains(config.replacedPasses.keys, EvaluationOrderGraphPass::class)

        val pair = config.replacedPasses[EvaluationOrderGraphPass::class]
        assertNotNull(pair)

        val pass = checkForReplacement(EvaluationOrderGraphPass(), ReplaceTestLanguage(), config)
        assertIs<ReplacedPass>(pass)
    }
}
