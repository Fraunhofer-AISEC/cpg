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

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.edges.flows.ContextSensitiveDataflow
import de.fraunhofer.aisec.cpg.passes.PointsToPass
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests the [PointsToPass]-specific extension of [reconcileCalls] (see [IncrementalUpdate.kt]'s
 * `tearDownPointsToPassCallEdges`/`markCallerDirtyForPointsToPass`): unlike the general,
 * deliberately-accepted gap documented on [attachStandardDfgEdges] (a reconciled call's
 * argument/parameter- and function-summary-derived DFG edges are otherwise never (re)computed by
 * the incremental path once [PointsToPass]/[ControlFlowSensitiveDFGPass] owns them), a call
 * previously resolved against an inferred stub -- or previously fully unresolved -- gets its
 * [PointsToPass]-computed state correctly invalidated/(re)computed once the real function arrives,
 * specifically for [PointsToPass].
 *
 * Reuses [StaleStubTestLanguage]/[StaleStubTestLanguageFrontend] (see [IncrementalUpdateTest]) --
 * the same hand-built, parser-free test frontend already used for the general stale-stub
 * reconciliation tests -- with `.defaultPasses()` (which registers [PointsToPass], since
 * [TranslationConfiguration.Builder.enablePointsToPass] defaults to `true`), so [PointsToPass]
 * genuinely runs, end to end, exactly like it would for any other language.
 */
class PointsToPassReconciliationTest {
    private fun tempSource(topLevel: File, fileName: String, content: String): File {
        return File(topLevel, fileName).apply {
            writeText(content)
            deleteOnExit()
        }
    }

    /**
     * Every [ContextSensitiveDataflow] edge in [call]'s `prevDFGEdges`, tagged with [call] itself.
     */
    private fun contextSensitiveEdgesFor(call: de.fraunhofer.aisec.cpg.graph.expressions.Call) =
        call.prevDFGEdges.filterIsInstance<ContextSensitiveDataflow>().filter {
            it.callingContext.calls.any { c -> c === call }
        }

    @Test
    fun testStaleInferredStubPointsToEdgesAreInvalidatedAndRecomputed() {
        val topLevel =
            Files.createTempDirectory("cpg-p2p-stale-stub-test").toFile().apply { deleteOnExit() }
        val callerFile = tempSource(topLevel, "caller.stale", "call:foo")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(callerFile)
                .registerLanguage<StaleStubTestLanguage>()
                .defaultPasses()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single()

        val fooCall = result.calls.single { it.name.localName == "foo" }
        val stub = fooCall.invokes.singleOrNull()
        assertNotNull(stub, "Expected the unresolved call to 'foo' to create an inferred stub")
        assertTrue(stub.isInferred)

        // PointsToPass.handleEmptyFunction gives the (body-less) inferred stub a "dummy" function
        // summary ("all parameters flow to the return") the first time it is analyzed as a callee
        // -- which produces at least one ContextSensitiveDataflow edge on the call itself, tagged
        // with a CallingContext referencing this exact call.
        val staleEdges = contextSensitiveEdgesFor(fooCall)
        assertTrue(
            staleEdges.isNotEmpty(),
            "Expected PointsToPass to have attached at least one ContextSensitiveDataflow edge " +
                "for the call to the inferred stub",
        )

        // Now add the real (non-dummy: it has an actual "return p" body) definition of 'foo'.
        val second = tempSource(topLevel, "foo.stale", "defineidentity:foo")
        val tu = manager.addSource(result, component, second)
        assertNotNull(tu)
        val realFoo = tu.declarations.filterIsInstance<Function>().single()
        assertFalse(realFoo.isInferred)

        // Reconciliation must have already replaced invokes and marked the caller dirty for
        // PointsToPass (via markCallerDirtyForPointsToPass) -- and cleared realFoo/main's
        // functionSummary isn't observable from here directly, but the net effect after
        // runDirtyPasses is: every OLD (stale-stub-derived) edge object is gone...
        for (edge in staleEdges) {
            assertTrue(
                fooCall.prevDFGEdges.none { it === edge },
                "Expected the stale, stub-derived ContextSensitiveDataflow edge to have been " +
                    "removed",
            )
        }

        manager.runDirtyPasses(result)

        // ...replaced by fresh edges reflecting the real function's (real, non-dummy) summary.
        val freshEdges = contextSensitiveEdgesFor(fooCall)
        assertTrue(
            freshEdges.isNotEmpty(),
            "Expected PointsToPass to have recomputed ContextSensitiveDataflow edges for the " +
                "call once it was redone against the real function",
        )
        for (edge in staleEdges) {
            assertTrue(freshEdges.none { it === edge }, "Expected no stale edge to reappear")
        }
        assertTrue(
            realFoo.functionSummary.isNotEmpty(),
            "Expected the real function's own functionSummary to have been (re)computed",
        )
    }

    @Test
    fun testPureAdditionGetsPointsToEdgesForTheFirstTimeWithoutInvalidation() {
        // Unlike "call:" (which always creates an inferred stub), disabling function inference
        // leaves the call genuinely, completely unresolved -- PointsToPass.handleCall does
        // nothing at all for a call with empty `invokes` (see the class doc), so there is nothing
        // to invalidate once the real function arrives; this is pure addition.
        val topLevel =
            Files.createTempDirectory("cpg-p2p-pure-addition-test").toFile().apply {
                deleteOnExit()
            }
        val callerFile = tempSource(topLevel, "caller.stale", "call:foo")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(callerFile)
                .registerLanguage<StaleStubTestLanguage>()
                .defaultPasses()
                .inferenceConfiguration(
                    InferenceConfiguration.Builder().inferFunctions(false).build()
                )
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single()

        val fooCall = result.calls.single { it.name.localName == "foo" }
        assertTrue(fooCall.invokes.isEmpty(), "Expected the call to remain fully unresolved")
        assertTrue(
            contextSensitiveEdgesFor(fooCall).isEmpty(),
            "Expected no ContextSensitiveDataflow edges for a call with no invokes at all",
        )

        val second = tempSource(topLevel, "foo.stale", "defineidentity:foo")
        val tu = manager.addSource(result, component, second)
        assertNotNull(tu)
        val realFoo = tu.declarations.filterIsInstance<Function>().single()

        assertEquals(listOf(realFoo), fooCall.invokes)
        manager.runDirtyPasses(result)

        assertTrue(
            contextSensitiveEdgesFor(fooCall).isNotEmpty(),
            "Expected PointsToPass to compute ContextSensitiveDataflow edges for the call now " +
                "that it has a real target, for the first time",
        )
        assertTrue(realFoo.functionSummary.isNotEmpty())
    }

    @Test
    fun testTwoNewFunctionsInSameAddSourceGetCalleeBeforeCallerOrdering() {
        // Two genuinely new functions -- caller() and callee(), where caller calls callee --
        // introduced in the SAME addSource call: no reconciliation/invalidation is involved at
        // all here (both are brand-new declarations), but both get marked dirty for PointsToPass
        // directly (see markDfgRelatedPassesDirty). Exercises PartialPassExecution.kt's
        // orderDependencies-aware ordering of the dirty EOGStarterPass target subset.
        val topLevel =
            Files.createTempDirectory("cpg-p2p-two-new-functions-test").toFile().apply {
                deleteOnExit()
            }
        val initial = tempSource(topLevel, "initial.stale", "defineidentity:unrelated")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(initial)
                .registerLanguage<StaleStubTestLanguage>()
                .defaultPasses()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single()

        val second = tempSource(topLevel, "twofuncs.stale", "twofuncs:caller:callee")
        val tu = manager.addSource(result, component, second)
        assertNotNull(tu)

        val caller =
            tu.declarations.filterIsInstance<Function>().single { it.name.localName == "caller" }
        val callee =
            tu.declarations.filterIsInstance<Function>().single { it.name.localName == "callee" }
        val call = caller.calls("callee").single()

        assertTrue(result.dirtyNodes[caller]?.contains(PointsToPass::class) == true)
        assertTrue(result.dirtyNodes[callee]?.contains(PointsToPass::class) == true)

        manager.runDirtyPasses(result)

        // callee's own functionSummary must reflect its real, non-dummy identity body ("return
        // p") -- i.e. it must actually have been analyzed (not left as an empty/dummy stand-in)
        // by the time this assertion runs.
        assertTrue(callee.functionSummary.isNotEmpty())
        assertFalse(
            callee.functionSummary.keys.any {
                (it as? de.fraunhofer.aisec.cpg.graph.expressions.Literal<*>)?.value == "dummy"
            },
            "Expected callee's functionSummary to reflect its real body, not a dummy placeholder",
        )

        // The call in caller must have picked up function-summary-derived, context-sensitive
        // state reflecting callee's real summary.
        assertTrue(contextSensitiveEdgesFor(call).isNotEmpty())
    }

    @Test
    fun testUnrelatedFunctionsFunctionSummaryIsUntouchedByReconciliationElsewhere() {
        // Note: this deliberately does NOT assert edge-object identity for the unrelated call's
        // own ContextSensitiveDataflow edges (unlike the other tests here) -- adding realFoo
        // marks DFGPass dirty too (see markDfgRelatedPassesDirty), and DFGPass is
        // ComponentPass-granularity, so a later runDirtyPasses reruns DFGPass over the WHOLE
        // component, including the unrelated call: DFGPass.handleCall unconditionally clears and
        // rebuilds `call.prevDFGEdges` for every call it visits (including re-adding its own
        // "invoked function flows into the call" ContextSensitiveDataflow edge, unconditionally,
        // regardless of dfgHandlesArgumentEdgesItself) -- a real, but pre-existing and
        // out-of-scope-here, granularity limitation of DFGPass itself, completely independent of
        // this task's PointsToPass-specific work. What this test actually verifies is narrower and
        // precisely scoped to what THIS work could plausibly get wrong: that
        // markCallerDirtyForPointsToPass's `functionSummary.clear()` and `markDirty<PointsToPass>`
        // are applied ONLY to 'main' (the directly reconciled caller of the call to 'foo'), never
        // to the unrelated callerU/calleeU.
        val topLevel =
            Files.createTempDirectory("cpg-p2p-unrelated-test").toFile().apply { deleteOnExit() }
        val callerFile = tempSource(topLevel, "caller.stale", "call:foo")
        val unrelatedFile = tempSource(topLevel, "unrelated.stale", "twofuncs:callerU:calleeU")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(callerFile, unrelatedFile)
                .registerLanguage<StaleStubTestLanguage>()
                .defaultPasses()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single()

        val unrelatedCallee = result.functions.single { it.name.localName == "calleeU" }
        val unrelatedCaller = result.functions.single { it.name.localName == "callerU" }
        val unrelatedCall = result.calls.single { it.name.localName == "calleeU" }

        // Sanity check: the unrelated call is already fully, really resolved -- nothing about it
        // should ever be touched by reconciling 'foo' elsewhere.
        assertTrue(unrelatedCall.invokes.singleOrNull()?.isInferred == false)
        val unrelatedCalleeSummaryBefore = unrelatedCallee.functionSummary.keys.toSet()
        assertTrue(unrelatedCalleeSummaryBefore.isNotEmpty())

        val second = tempSource(topLevel, "foo.stale", "defineidentity:foo")
        manager.addSource(result, component, second)

        // Reconciling 'foo' must never mark PointsToPass dirty for the unrelated functions.
        assertFalse(result.dirtyNodes[unrelatedCaller]?.contains(PointsToPass::class) == true)
        assertFalse(result.dirtyNodes[unrelatedCallee]?.contains(PointsToPass::class) == true)

        manager.runDirtyPasses(result)

        // ...and their own functionSummary must never have been cleared as a side effect either.
        assertEquals(unrelatedCalleeSummaryBefore, unrelatedCallee.functionSummary.keys.toSet())
    }
}
