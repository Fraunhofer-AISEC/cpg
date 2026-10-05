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
package de.fraunhofer.aisec.codyze

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage
import de.fraunhofer.aisec.cpg.frontends.cxx.CPPLanguage
import de.fraunhofer.aisec.cpg.frontends.golang.GoLanguage
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguage
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage
import de.fraunhofer.aisec.cpg.frontends.rust.RustLanguage
import de.fraunhofer.aisec.cpg.graph.Backward
import de.fraunhofer.aisec.cpg.graph.GraphToFollow
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.declarations.Parameter
import de.fraunhofer.aisec.cpg.graph.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.Subscription
import de.fraunhofer.aisec.cpg.graph.firstParentOrNull
import de.fraunhofer.aisec.cpg.graph.followPrevCDGUntilHitNodes
import de.fraunhofer.aisec.cpg.graph.refs
import de.fraunhofer.aisec.cpg.passes.ControlDependenceGraphPass
import de.fraunhofer.aisec.cpg.query.QueryTree
import de.fraunhofer.aisec.cpg.query.allExtended
import de.fraunhofer.aisec.cpg.query.and
import de.fraunhofer.aisec.cpg.query.dataFlow
import de.fraunhofer.aisec.cpg.query.executionPath
import de.fraunhofer.aisec.cpg.query.not
import de.fraunhofer.aisec.cpg.query.or
import de.fraunhofer.aisec.cpg.test.analyze
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

private val comparisons = setOf("<", "<=", ">", ">=")

/** The sample code uses `_mm_lfence` as a speculation barrier in all languages. */
private val barriers = setOf("_mm_lfence", "__builtin_ia32_lfence", "__speculation_barrier")

/** The bounds checks (e.g. `x < size`) on the index of this subscription it depends on. */
val Subscription.boundsChecks: Set<Node>
    get() = followPrevCDGUntilHitNodes {
        it is BinaryOperator &&
            it.operatorCode in comparisons &&
            it.refs.any { ref -> ref.refersTo in subscriptExpression.refs.map { it.refersTo } }
    }

/** Whether this node is used as the index of an array access. */
val Node.isArrayIndex: Boolean
    get() = (astParent as? Subscription)?.subscriptExpression === this

/** Whether this node is a speculation barrier, such as `lfence`. */
val Node.isSpeculationBarrier: Boolean
    get() = this is Call && name.localName in barriers

/**
 * Checks for Spectre v1 (bounds check bypass) gadgets. A gadget consists of
 * 1. a bounds check on an attacker-controlled index `x`,
 * 2. a load `A[x]` that is control-dependent on the check (and thus executed speculatively out of
 *    bounds), and
 * 3. a second load `B[f(A[x])]` whose address depends on the loaded value and leaks it via the
 *    cache.
 *
 * Every bounds-checked array access must either not form such a gadget, or be preceded by a
 * speculation barrier.
 */
fun TranslationResult.noSpectreV1(): QueryTree<Boolean> =
    allExtended<Subscription>(sel = { it.boundsChecks.isNotEmpty() }) { load ->
        val attackerControlled =
            dataFlow(load.subscriptExpression, Backward(GraphToFollow.DFG)) { it is Parameter }
        val leaksViaCache = dataFlow(load) { it.isArrayIndex }
        val fenced =
            executionPath(
                load,
                Backward(GraphToFollow.EOG),
                earlyTermination = { it in load.boundsChecks },
            ) {
                it.isSpeculationBarrier
            }

        not(attackerControlled and leaksViaCache) or fenced
    }

/**
 * Showcases that the same query works across languages: the sample in each language contains two
 * vulnerable functions, one mitigated by a speculation barrier and one without a second load.
 */
class SpectreV1Test {
    private fun assertGadgets(file: String, language: (TranslationConfiguration.Builder) -> Unit) {
        val topLevel = File("src/integrationTest/resources/spectre")
        val result =
            analyze(listOf(topLevel.resolve(file)), topLevel.toPath(), true) {
                language(it)
                it.registerPass<ControlDependenceGraphPass>()
            }

        val query = result.noSpectreV1()
        assertFalse(query.value)

        val vulnerable =
            query.children
                .filter { it.value == false }
                .mapNotNull { it.node?.firstParentOrNull<Function>()?.name?.localName }
                .toSet()
        assertEquals(setOf("victim_function", "victim_function_split"), vulnerable)
    }

    @Test
    fun testC() {
        assertGadgets("spectre_v1.c") { it.registerLanguage<CLanguage>() }
    }

    @Test
    fun testCPP() {
        assertGadgets("spectre_v1.cpp") { it.registerLanguage<CPPLanguage>() }
    }

    @Test
    fun testJava() {
        assertGadgets("Victim.java") { it.registerLanguage<JavaLanguage>() }
    }

    @Test
    fun testGo() {
        assertGadgets("spectre_v1.go") { it.registerLanguage<GoLanguage>() }
    }

    @Test
    fun testPython() {
        assertGadgets("spectre_v1.py") { it.registerLanguage<PythonLanguage>() }
    }

    @Test
    fun testRust() {
        assertGadgets("spectre_v1.rs") { it.registerLanguage<RustLanguage>() }
    }
}
