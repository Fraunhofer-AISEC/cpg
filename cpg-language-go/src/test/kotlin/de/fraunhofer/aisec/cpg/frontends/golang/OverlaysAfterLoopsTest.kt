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

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.concepts.Concept
import de.fraunhofer.aisec.cpg.graph.concepts.Operation
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.passes.concepts.TagOverlaysPass
import de.fraunhofer.aisec.cpg.passes.concepts.each
import de.fraunhofer.aisec.cpg.passes.concepts.getOverlaysByPrevDFG
import de.fraunhofer.aisec.cpg.passes.concepts.tag
import de.fraunhofer.aisec.cpg.passes.concepts.with
import de.fraunhofer.aisec.cpg.passes.concepts.withMultiple
import de.fraunhofer.aisec.cpg.passes.executePass
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import java.util.Objects
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class OverlaysAfterLoopsTest {
    class Tagged(underlyingNode: Node? = null) : Concept(underlyingNode) {
        override fun equals(other: Any?): Boolean = other is Tagged && super.equals(other)

        override fun hashCode() = Objects.hash(super.hashCode())
    }

    class Resource(underlyingNode: Node? = null) : Concept(underlyingNode) {
        override fun equals(other: Any?): Boolean = other is Resource && super.equals(other)

        override fun hashCode() = Objects.hash(super.hashCode())
    }

    class Release(concept: Resource) : Operation(concept) {
        override fun equals(other: Any?): Boolean = other is Release && super.equals(other)

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

    @Test
    fun testOperationsAfterLoopsReferToTheAttachedConcept() {
        val topLevel = Path.of("src", "test", "resources", "golang")
        // Which of the equal overlays from the evaluations after the loop end up in the states of
        // acquire and release depends on the iteration order, so we analyze several times
        repeat(10) {
            val result =
                analyze(
                    listOf(topLevel.resolve("overlays_after_loops.go").toFile()),
                    topLevel,
                    true,
                ) {
                    it.registerLanguage<GoLanguage>()
                    it.registerPass<TagOverlaysPass>()
                    it.configurePass<TagOverlaysPass>(
                        TagOverlaysPass.Configuration(
                            tag =
                                tag {
                                    each<Call>("log.Printf").with { Tagged() }
                                    each<Call>("acquire").with { Resource() }
                                    each<Call>("release").withMultiple {
                                        node.arguments
                                            .first()
                                            .getOverlaysByPrevDFG<Resource>(state)
                                            .map { Release(it) }
                                    }
                                }
                        )
                    )
                }
            assertReleaseRefersToTheAttachedResource(result)

            // A later execution of the pass creates equal overlays again, which must not replace
            // the attached ones
            executePass(TagOverlaysPass::class, result.finalCtx, result, emptyList())
            assertReleaseRefersToTheAttachedResource(result)
        }
    }

    private fun assertReleaseRefersToTheAttachedResource(result: TranslationResult) {
        val function = result.functions["resourceAfterLoop"]
        assertNotNull(function)
        val resource = function.calls["acquire"]?.overlays?.filterIsInstance<Resource>()?.single()
        assertNotNull(resource)
        val release = function.calls["release"]?.overlays?.filterIsInstance<Release>()?.single()
        assertNotNull(release)
        assertSame(resource, release.concept)
        assertEquals(setOf<Operation>(release), resource.ops)
    }
}
