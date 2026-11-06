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
package de.fraunhofer.aisec.cpg.frontends.golang

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.passes.concepts.TagOverlaysPass
import de.fraunhofer.aisec.cpg.passes.concepts.each
import de.fraunhofer.aisec.cpg.passes.concepts.tag
import de.fraunhofer.aisec.cpg.passes.concepts.with
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import java.util.Objects
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class OverlaysAfterLoopsTest {
    class Tagged(underlyingNode: Node? = null) : Concept(underlyingNode) {
        override fun equals(other: Any?): Boolean = other is Tagged && super.equals(other)

        override fun hashCode() = Objects.hash(super.hashCode())
    }

    @Test
    fun testCallsAfterLoopsGetOneOverlay() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        val result =
            analyze(listOf(topLevel.resolve("overlays_after_loops.go").toFile()), topLevel, true) {
                it.registerLanguage<GoLanguage>()
                it.registerPass<TagOverlaysPass>()
                it.configurePass<TagOverlaysPass>(
                    TagOverlaysPass.Configuration(
                        tag = tag { each<Call>("log.Printf").with { Tagged() } }
                    )
                )
            }

        // The EOG iteration evaluates the call after a loop once per iteration of the loop, each
        // time creating an equal overlay
        for (name in listOf("rangeLoop", "indexLoop")) {
            val function = result.functions[name]
            assertNotNull(function, name)
            for (call in function.calls("log.Printf")) {
                assertEquals(
                    1,
                    call.overlays.filterIsInstance<Tagged>().size,
                    "$name: ${call.code}",
                )
            }
        }
    }
}
