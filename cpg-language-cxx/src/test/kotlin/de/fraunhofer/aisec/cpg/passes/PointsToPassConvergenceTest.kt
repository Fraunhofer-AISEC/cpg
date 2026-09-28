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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.expressions.Return
import de.fraunhofer.aisec.cpg.test.analyze
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.measureTime

/**
 * Tests for chains of synthetic field addresses and unknown values, which the [PointsToPass] limits
 * to a maximum depth and then summarizes.
 */
class PointsToPassConvergenceTest {
    /**
     * A pointer which is set to a field of the object it points to in every iteration of a loop
     * reaches a new field address in every iteration. The analysis must still reach its fixpoint
     * quickly instead of running into its timeout.
     */
    @Test
    fun testFieldChainInLoop() {
        val file = File("src/test/resources/pointsToPass/field_chain_loop.c")
        val timeout = 2.minutes

        val duration = measureTime {
            val tu =
                analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                    it.registerLanguage<CLanguage>()
                    it.registerPass<PointsToPass>()
                    it.configurePass<PointsToPass>(PointsToPass.Configuration(timeout = timeout))
                }
            assertNotNull(tu)
            assertNotNull(tu.functions["find_caps"])
        }

        assertTrue(
            duration < timeout,
            "The analysis took $duration, i.e., the PointsToPass ran into its timeout",
        )
    }

    @Test
    fun testValueSurvivesListWalk() {
        val result = analyzeSemantics()
        val function = result.function("read_after_walk")

        val read = function.allChildren<Return>().single().returnValue
        assertNotNull(read)
        assertTrue(
            read.reaches(function.argumentRef("secret")),
            "The value written before the loop does not reach the read after it",
        )
    }

    @Test
    fun testShallowFieldsStayDistinct() {
        val result = analyzeSemantics()
        val function = result.function("shallow")

        val read = function.allChildren<Call> { it.name.localName == "sink" }.single().arguments[0]
        assertTrue(read.reaches(function.argumentRef("a")))
        assertFalse(
            read.reaches(function.argumentRef("b")),
            "o->in.a and o->in.b are no longer distinguished",
        )
    }

    private fun analyzeSemantics(): TranslationResult {
        val file = File("src/test/resources/pointsToPass/field_chain_semantics.c")
        return analyze(listOf(file), file.parentFile.toPath(), true) {
            it.registerLanguage<CLanguage>()
            it.registerPass<PointsToPass>()
        }
    }

    private fun TranslationResult.function(name: String): Function {
        val function = functions.singleOrNull { it.name.localName == name && it.body != null }
        assertNotNull(function)
        return function
    }

    /** The reference to the parameter [name] on the right-hand side of an assignment. */
    private fun Function.argumentRef(name: String): Reference {
        val ref =
            allChildren<Reference> { it.name.localName == name && it.refersTo == parameters[name] }
                .singleOrNull()
        assertNotNull(ref, "No single reference to $name in ${this.name}")
        return ref
    }

    private fun Node.reaches(target: Node): Boolean =
        followPrevFullDFGEdgesUntilHit { it === target }.fulfilled.isNotEmpty()
}
