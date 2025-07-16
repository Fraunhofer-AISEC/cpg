/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WalkerTest {
    @Test
    fun testSingleVisitor() {
        val file = File("src/test/resources/Issue1459.java")

        val config =
            TranslationConfiguration.builder()
                .sourceLocations(listOf(file))
                .defaultPasses()
                .debugParser(true)
                .registerLanguage<JavaLanguage>()
                .failOnError(true)
                .build()

        val analyzer = TranslationManager.builder().config(config).build()
        val result = analyzer.analyze().get()
        assertNotNull(result)
        val scopeManager = result.finalCtx.scopeManager
        val walker = SubgraphWalker.ScopedWalker(scopeManager)

        val visitedNodes = mutableSetOf<Node>()

        walker.strategy = Strategy::EOG_FORWARD
        walker.registerHandler { node ->
            assertTrue(visitedNodes.add(node), "Visited node $node multiple times")
        }

        for (tu in result.components.flatMap { it.translationUnits }) {
            // gather all resolution start holders and their start nodes
            val nodes = tu.allEOGStarters.filter { it.prevEOG.isEmpty() }

            for (node in nodes) {
                walker.iterate(node)
            }
        }

        val i2 = result.dFields["i2"]
        assertNotNull(i2)
        assertContains(visitedNodes, i2)
    }
}
