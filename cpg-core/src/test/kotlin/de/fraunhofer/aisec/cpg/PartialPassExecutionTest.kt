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
import de.fraunhofer.aisec.cpg.graph.expressions.Block
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.expressions.Return
import de.fraunhofer.aisec.cpg.graph.expressions.While
import de.fraunhofer.aisec.cpg.passes.BasicBlockCollectorPass
import de.fraunhofer.aisec.cpg.passes.ControlDependenceGraphPass
import de.fraunhofer.aisec.cpg.passes.DFGPass
import de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass
import de.fraunhofer.aisec.cpg.passes.ImportResolver
import de.fraunhofer.aisec.cpg.passes.SccPass
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import de.fraunhofer.aisec.cpg.passes.TranslationResultPass
import de.fraunhofer.aisec.cpg.passes.TypeHierarchyResolver
import de.fraunhofer.aisec.cpg.passes.TypeResolver
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.markDirty
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Two passes with a circular hard dependency on each other, so [PassOrderingHelper.order] can never
 * find a valid order for them -- used to force the [ConfigurationException] fallback in
 * [runDirtyPasses].
 */
@DependsOn(CyclicPassB::class)
class CyclicPassA(ctx: TranslationContext) : TranslationResultPass(ctx) {
    override fun accept(t: TranslationResult) {}

    override fun cleanup() {}
}

@DependsOn(CyclicPassA::class)
class CyclicPassB(ctx: TranslationContext) : TranslationResultPass(ctx) {
    override fun accept(t: TranslationResult) {}

    override fun cleanup() {}
}

/**
 * A pass that never settles: every time it runs, it marks a DIFFERENT one of [proxyA]/[proxyB]
 * dirty for itself again, alternating forever -- used to exercise [runDirtyPasses]'s
 * `maxExecutions` guard against infinite loops.
 */
class PingPongPass(ctx: TranslationContext) : TranslationResultPass(ctx) {
    override fun accept(t: TranslationResult) {
        val proxy = if (executionCount % 2 == 0) proxyB else proxyA
        executionCount++
        t.markDirty(proxy, PingPongPass::class)
    }

    override fun cleanup() {}

    companion object {
        var executionCount = 0
        lateinit var proxyA: Node
        lateinit var proxyB: Node
    }
}

/**
 * When run, marks [TriggeredPass] dirty -- never itself -- so that a pass which was never part of
 * the originally-dirty set gets picked up and run within the SAME [runDirtyPasses] call.
 */
class DirtiesOtherPass(ctx: TranslationContext) : TranslationResultPass(ctx) {
    override fun accept(t: TranslationResult) {
        t.markDirty(t, TriggeredPass::class)
    }

    override fun cleanup() {}
}

/** Only ever marked dirty as a side effect of [DirtiesOtherPass] running, never directly. */
class TriggeredPass(ctx: TranslationContext) : TranslationResultPass(ctx) {
    override fun accept(t: TranslationResult) {
        ran = true
    }

    override fun cleanup() {}

    companion object {
        var ran = false
    }
}

/**
 * Simulates a host application cancelling the analysis mid-run: dirties [NeverRunsPass] (so there
 * is something left in the queue afterward) and calls [TranslationManager.cancel] in the same
 * breath, so [runDirtyPasses] must stop before ever dequeuing/running that newly-queued pass.
 */
class CancellingPass(ctx: TranslationContext) : TranslationResultPass(ctx) {
    override fun accept(t: TranslationResult) {
        ran = true
        t.markDirty(t, NeverRunsPass::class)
        manager.cancel()
    }

    override fun cleanup() {}

    companion object {
        var ran = false
        lateinit var manager: TranslationManager
    }
}

/** Must never run if [CancellingPass] correctly stops [runDirtyPasses] early. */
class NeverRunsPass(ctx: TranslationContext) : TranslationResultPass(ctx) {
    override fun accept(t: TranslationResult) {
        ran = true
    }

    override fun cleanup() {}

    companion object {
        var ran = false
    }
}

/**
 * Tests for [runDirtyPasses]: after [TranslationManager.addSource] marks nodes dirty (see
 * [IncrementalUpdateTest] / [updateIncrementally]), [runDirtyPasses] must re-resolve exactly the
 * affected call without re-running [SymbolResolver] (or any other pass) over unrelated, untouched
 * parts of the graph.
 */
class PartialPassExecutionTest {
    private fun tempSource(topLevel: File, fileName: String, content: String): File {
        return File(topLevel, fileName).apply {
            writeText(content)
            deleteOnExit()
        }
    }

    /**
     * Registers a minimal pass pipeline (no [de.fraunhofer.aisec.cpg.passes.PointsToPass] /
     * [de.fraunhofer.aisec.cpg.passes.ControlFlowSensitiveDFGPass]). Both of those passes compute a
     * cross-function, whole-call-graph fixpoint (see [DFGPass]'s `runsPointsToPassOrCfsDFG`
     * gating), which is out of scope for a correct partial/incremental re-run -- re-running such a
     * global fixpoint over only a couple of dirty nodes could produce incomplete or wrong results,
     * not just "more work than necessary". [DFGPass] itself, without either of those two passes
     * registered, deterministically attaches argument-to-parameter DFG edges per call
     * ([de.fraunhofer.aisec.cpg.helpers.Util.attachCallParameters]), which is what these tests rely
     * on to observe reconnection.
     */
    private fun TranslationConfiguration.Builder.minimalPasses(): TranslationConfiguration.Builder {
        registerPass<TypeHierarchyResolver>()
        registerPass<SymbolResolver>()
        registerPass<ImportResolver>()
        registerPass<DFGPass>()
        registerPass<EvaluationOrderGraphPass>()
        registerPass<TypeResolver>()
        return this
    }

    @Test
    fun testRunDirtyPassesResolvesOnlyAffectedCallWithoutTouchingUnrelatedGraph() {
        val topLevel =
            Files.createTempDirectory("cpg-partial-pass-execution-test").toFile().apply {
                deleteOnExit()
            }
        val callerFile = tempSource(topLevel, "caller.stale", "call:foo")
        val unrelatedCallerFile = tempSource(topLevel, "unrelated_caller.stale", "call:bar")
        val unrelatedDefFile = tempSource(topLevel, "unrelated_def.stale", "define:bar")

        // The "app" component (with the call to the not-yet-defined 'foo') and the "lib" component
        // (fully self-contained, unrelated to 'foo') are kept as *separate* components. DFGPass has
        // Component-level granularity, so this lets us prove that runDirtyPasses only reruns it on
        // the "app" component, not on "lib".
        val config =
            TranslationConfiguration.builder()
                .topLevels(mapOf("app" to topLevel, "lib" to topLevel))
                .softwareComponents(
                    mutableMapOf(
                        "app" to listOf(callerFile),
                        "lib" to listOf(unrelatedCallerFile, unrelatedDefFile),
                    )
                )
                .registerLanguage<StaleStubTestLanguage>()
                .minimalPasses()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        assertTrue(result.isLive)

        val component = result.components.single { it.name.localName == "app" }
        val libComponent = result.components.single { it.name.localName == "lib" }

        val fooCall = result.calls.single { it.name.localName == "foo" }
        val fooStub = fooCall.invokes.singleOrNull()
        assertNotNull(fooStub, "Expected the unresolved call to 'foo' to create an inferred stub")
        assertTrue(fooStub.isInferred)

        // The unrelated call to 'bar' is already fully (and correctly) resolved in the initial,
        // full analysis. It has nothing to do with 'foo' and must remain untouched by
        // runDirtyPasses.
        val barCall = result.calls.single { it.name.localName == "bar" }
        val realBar = result.functions.single { it.name.localName == "bar" }
        assertEquals(listOf(realBar), barCall.invokes)
        assertFalse(realBar.isInferred)

        // Capture identity of the actual edge object backing the (already correct) 'bar'
        // resolution. SymbolResolver's `call.invokes = ...` setter always installs fresh edge
        // objects, so if SymbolResolver were to run again on barCall's function, this specific
        // edge object would no longer be the one referenced afterward, even if it re-resolved to
        // the very same target function.
        val barInvokeEdgeBefore = barCall.invokeEdges.single()
        val barParam = realBar.parameters.single()
        val barParamPrevDfgBefore = barParam.prevDFGEdges.toList()

        // Now add the real definition of 'foo'. This marks the new function and the caller of the
        // stale call dirty for SymbolResolver (see IncrementalUpdateTest), and already tears down
        // the stale invokes/DFG edges pointing at the inferred stub.
        val fooDefFile = tempSource(topLevel, "foo.stale", "define:foo")
        val tu = manager.addSource(result, component, fooDefFile)
        assertNotNull(tu)

        val realFoo = tu.declarations.filterIsInstance<Function>().single()
        assertEquals("foo", realFoo.name.localName)
        assertFalse(realFoo.isInferred)

        // Before the partial pass re-run: updateIncrementally's batched reconciliation already
        // added the real function to `invokes` directly (removing the now-stale stub), even though
        // SymbolResolver itself has not run again yet.
        assertEquals(listOf(realFoo), fooCall.invokes)

        assertTrue(result.dirtyNodes[realFoo]?.contains(SymbolResolver::class) == true)

        runDirtyPasses(result)

        // The call now resolves against the real function, not the (now-detached) stub.
        assertEquals(listOf(realFoo), fooCall.invokes)
        assertTrue(fooCall.prevDFG.contains(realFoo))
        val fooParam = realFoo.parameters.single()
        assertTrue(fooParam.prevDFG.isNotEmpty(), "Expected DFGPass to reconnect the call argument")

        // Dirty markings for the passes we just ran must be cleared.
        assertTrue(result.dirtyNodes[realFoo].orEmpty().isEmpty())

        // The unrelated 'bar' call must be entirely untouched: same resolved target, and -
        // crucially - the very same edge/DFG objects, proving SymbolResolver/DFGPass were never
        // re-run on that part of the graph.
        assertEquals(listOf(realBar), barCall.invokes)
        assertSame(barInvokeEdgeBefore, barCall.invokeEdges.single())
        assertEquals(barParamPrevDfgBefore, barParam.prevDFGEdges.toList())
    }

    @Test
    fun testRunDirtyPassesMatchesFullReanalysis() {
        // Incremental path: analyze with only the call, then addSource the definition, then run
        // dirty passes.
        val incrementalTopLevel =
            Files.createTempDirectory("cpg-partial-pass-execution-incremental").toFile().apply {
                deleteOnExit()
            }
        val callerFile = tempSource(incrementalTopLevel, "caller.stale", "call:foo")
        val incrementalConfig =
            TranslationConfiguration.builder()
                .topLevel(incrementalTopLevel)
                .sourceLocations(callerFile)
                .registerLanguage<StaleStubTestLanguage>()
                .minimalPasses()
                .disableCleanup()
                .build()
        val incrementalManager = TranslationManager.builder().config(incrementalConfig).build()
        val incrementalResult = incrementalManager.analyze().get()
        val component = incrementalResult.components.single()
        val fooDefFile = tempSource(incrementalTopLevel, "foo.stale", "define:foo")
        incrementalManager.addSource(incrementalResult, component, fooDefFile)
        runDirtyPasses(incrementalResult)

        val incrementalFooCall = incrementalResult.calls.single { it.name.localName == "foo" }
        val incrementalFoo = incrementalResult.functions.single { it.name.localName == "foo" }

        // Full-reanalysis path: both files present from the start.
        val fullTopLevel =
            Files.createTempDirectory("cpg-partial-pass-execution-full").toFile().apply {
                deleteOnExit()
            }
        val fullCallerFile = tempSource(fullTopLevel, "caller.stale", "call:foo")
        val fullFooFile = tempSource(fullTopLevel, "foo.stale", "define:foo")
        val fullConfig =
            TranslationConfiguration.builder()
                .topLevel(fullTopLevel)
                .sourceLocations(fullCallerFile, fullFooFile)
                .registerLanguage<StaleStubTestLanguage>()
                .minimalPasses()
                .build()
        val fullManager = TranslationManager.builder().config(fullConfig).build()
        val fullResult = fullManager.analyze().get()

        val fullFooCall = fullResult.calls.single { it.name.localName == "foo" }
        val fullFoo = fullResult.functions.single { it.name.localName == "foo" }

        // Outcome parity: in both cases the call resolves to the real, non-inferred function with
        // the same signature shape, and DFG connects the argument to the resolved function's
        // parameter.
        assertEquals(listOf(incrementalFoo), incrementalFooCall.invokes)
        assertEquals(listOf(fullFoo), fullFooCall.invokes)
        assertFalse(incrementalFoo.isInferred)
        assertFalse(fullFoo.isInferred)
        assertEquals(fullFoo.parameters.size, incrementalFoo.parameters.size)
        assertTrue(incrementalFooCall.prevDFG.contains(incrementalFoo))
        assertTrue(fullFooCall.prevDFG.contains(fullFoo))
    }

    @Test
    fun testRunDirtyPassesComputesControlFlowStructureForNewFunctionWithoutTouchingUnrelatedGraph() {
        val topLevel =
            Files.createTempDirectory("cpg-partial-pass-execution-cfg-test").toFile().apply {
                deleteOnExit()
            }
        val appSeedFile = tempSource(topLevel, "app_seed.stale", "define:seed")
        val unrelatedFile = tempSource(topLevel, "unrelated.stale", "loopfunc:untouched")

        // "app" only has an unrelated, pre-existing declaration; "untouched" lives in the
        // separate "lib" component so we can prove that computing EOG/BB/CDG/SCC for the newly
        // added function does not re-touch it.
        val config =
            TranslationConfiguration.builder()
                .topLevels(mapOf("app" to topLevel, "lib" to topLevel))
                .softwareComponents(
                    mutableMapOf("app" to listOf(appSeedFile), "lib" to listOf(unrelatedFile))
                )
                .registerLanguage<StaleStubTestLanguage>()
                .defaultPasses()
                .registerPass<ControlDependenceGraphPass>()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single { it.name.localName == "app" }

        val untouchedFn = result.functions.single { it.name.localName == "untouched" }
        val untouchedWhile = untouchedFn.body.allChildren<While>().single()
        val untouchedWhileEogEdgeBefore = untouchedWhile.prevEOGEdges.single()
        val untouchedAssign = untouchedWhile.statement.allChildren<Reference>().first()
        val untouchedAssignCdgBefore = untouchedAssign.prevCDG.toList()

        // Now add a brand-new function, with its own loop, into "app". It has never been seen by
        // any pass before.
        val newSource = tempSource(topLevel, "new.stale", "loopfunc:newFn")
        val tu = manager.addSource(result, component, newSource)
        assertNotNull(tu)
        val newFn = tu.declarations.filterIsInstance<Function>().single()

        // Before runDirtyPasses: purely additive dirty-marking has happened, but no pass has run
        // on the new function yet, so it has no control-flow structure at all.
        assertTrue(newFn.nextEOG.isEmpty())
        assertNull(newFn.firstBasicBlock)

        runDirtyPasses(result)

        // EOG: the function and its loop now have EOG edges.
        assertTrue(newFn.nextEOG.isNotEmpty())
        val newWhile = (newFn.body as Block).allChildren<While>().single()
        assertTrue(newWhile.prevEOG.isNotEmpty())
        assertTrue(newWhile.nextEOG.isNotEmpty())

        // Basic blocks were computed.
        assertNotNull(newFn.firstBasicBlock)

        // CDG: the assignment inside the loop body is control-dependent on the loop condition.
        val newAssignRef = newWhile.statement.allChildren<Reference>().first()
        assertTrue(newAssignRef.prevCDG.isNotEmpty())

        // SCC: the loop introduces a back edge, so at least one EOG edge reachable from newFn must
        // be labeled with a non-null scc id.
        val sccLabeled =
            newFn.allChildren<Node>().flatMap { it.nextEOGEdges }.any { it.scc != null }
        assertTrue(sccLabeled, "Expected SccPass to label at least one EOG edge of the new loop")

        // Intraprocedural DFG: the final `return x` reads the value written in the loop.
        val returnRef = (newFn.body as Block).allChildren<Return>().single().returnValue
        assertNotNull(returnRef)
        assertTrue(
            returnRef.prevDFG.isNotEmpty(),
            "Expected ControlFlowSensitiveDFGPass to connect the loop's writes of `x` to the " +
                "final read",
        )

        // Dirty markings for the passes we just ran must be cleared.
        assertTrue(result.dirtyNodes[newFn].orEmpty().isEmpty())

        // The unrelated, pre-existing "untouched" function (in a different component) must remain
        // completely untouched: same EOG/CDG edge objects, proving none of
        // EvaluationOrderGraphPass/BasicBlockCollectorPass/ControlDependenceGraphPass/SccPass were
        // re-run on it.
        assertSame(untouchedWhileEogEdgeBefore, untouchedWhile.prevEOGEdges.single())
        assertEquals(untouchedAssignCdgBefore, untouchedAssign.prevCDG.toList())
    }

    @Test
    fun testAddSourceDoesNotMarkUnregisteredEOGStarterPassesDirty() {
        // Regression test: BasicBlockCollectorPass/ControlDependenceGraphPass/SccPass must only be
        // marked dirty if they are actually registered -- unlike PointsToPass/
        // ControlFlowSensitiveDFGPass a few lines above them (in markDfgRelatedPassesDirty), an
        // earlier version of updateIncrementally marked all three of them dirty unconditionally.
        // ControlDependenceGraphPass in particular is not part of defaultPasses(), so this would
        // make an addSource()+runDirtyPasses() pipeline run a pass a full analyze() never would,
        // breaking incremental-vs-full parity. `minimalPasses()` here deliberately registers none
        // of the three.
        val topLevel =
            Files.createTempDirectory("cpg-partial-pass-execution-gating-test").toFile().apply {
                deleteOnExit()
            }
        val callerFile = tempSource(topLevel, "caller.stale", "define:seed")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(callerFile)
                .registerLanguage<StaleStubTestLanguage>()
                .minimalPasses()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single()

        val newSource = tempSource(topLevel, "new.stale", "loopfunc:newFn")
        val tu = manager.addSource(result, component, newSource)
        assertNotNull(tu)
        val newFn = tu.declarations.filterIsInstance<Function>().single()

        val dirtyPassesForNewFn = result.dirtyNodes[newFn].orEmpty()
        assertFalse(dirtyPassesForNewFn.contains(BasicBlockCollectorPass::class))
        assertFalse(dirtyPassesForNewFn.contains(ControlDependenceGraphPass::class))
        assertFalse(dirtyPassesForNewFn.contains(SccPass::class))

        // Sanity check: it is still correctly marked dirty for the passes that ARE registered, so
        // this isn't just a case of nothing being marked dirty at all. EvaluationOrderGraphPass is
        // marked dirty on the enclosing TranslationUnit, not on newFn itself (TranslationUnitPass
        // granularity).
        assertTrue(dirtyPassesForNewFn.contains(SymbolResolver::class))
        assertTrue(dirtyPassesForNewFn.contains(DFGPass::class))
        assertTrue(result.dirtyNodes[tu]?.contains(EvaluationOrderGraphPass::class) == true)
    }

    @Test
    fun testAddSourceAttachesReconciledCallDfgEdgesWithoutTouchingUnrelatedComponent() {
        // Regression test for the fix described in IncrementalUpdate.kt's
        // markCallerDirtyForSymbolResolver: reconciling a call in one component must attach that
        // call's DFG edges directly, WITHOUT marking DFGPass dirty for the caller and WITHOUT ever
        // needing runDirtyPasses -- and, since DFGPass is Component-granularity, must not touch (or
        // even schedule a rerun of) a large, unrelated pre-existing component.
        val topLevel =
            Files.createTempDirectory("cpg-partial-pass-execution-reconcile-unrelated-test")
                .toFile()
                .apply { deleteOnExit() }
        val callerFile = tempSource(topLevel, "caller.stale", "call:foo")
        val unrelatedCallerFile = tempSource(topLevel, "unrelated_caller.stale", "call:bar")
        val unrelatedDefFile = tempSource(topLevel, "unrelated_def.stale", "define:bar")

        val config =
            TranslationConfiguration.builder()
                .topLevels(mapOf("app" to topLevel, "lib" to topLevel))
                .softwareComponents(
                    mutableMapOf(
                        "app" to listOf(callerFile),
                        "lib" to listOf(unrelatedCallerFile, unrelatedDefFile),
                    )
                )
                .registerLanguage<StaleStubTestLanguage>()
                .minimalPasses()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single { it.name.localName == "app" }

        val fooCall = result.calls.single { it.name.localName == "foo" }
        assertNotNull(fooCall.invokes.singleOrNull()?.takeIf { it.isInferred })
        val main = fooCall.firstParentOrNull<Function>()!!

        val barCall = result.calls.single { it.name.localName == "bar" }
        val realBar = result.functions.single { it.name.localName == "bar" }
        val barInvokeEdgeBefore = barCall.invokeEdges.single()
        val barParam = realBar.parameters.single()
        val barParamPrevDfgBefore = barParam.prevDFGEdges.toList()

        val fooDefFile = tempSource(topLevel, "foo.stale", "define:foo")
        val tu = manager.addSource(result, component, fooDefFile)
        assertNotNull(tu)
        val realFoo = tu.declarations.filterIsInstance<Function>().single()

        // The reconciled call's own DFG edges are already correct, with no runDirtyPasses call at
        // all.
        assertEquals(listOf(realFoo), fooCall.invokes)
        assertTrue(fooCall.prevDFG.contains(realFoo))
        assertTrue(realFoo.parameters.single().prevDFG.isNotEmpty())

        // DFGPass (Component-granularity) must not have been marked dirty for the caller.
        assertFalse(result.dirtyNodes[main].orEmpty().contains(DFGPass::class))

        // The unrelated "lib" component must be entirely untouched -- proving DFGPass was never
        // scheduled to rerun on it (which a dirty-marking-based fix would have risked, since
        // resolving a dirty node to its ComponentPass target reprocesses the WHOLE component).
        assertEquals(listOf(realBar), barCall.invokes)
        assertSame(barInvokeEdgeBefore, barCall.invokeEdges.single())
        assertEquals(barParamPrevDfgBefore, barParam.prevDFGEdges.toList())
    }

    @Test
    fun testResolveEOGStarterTargetsFindsEnclosingFunctionForNestedDirtyNode() {
        // The ancestor branch of resolveEOGStarterTargets (the lines after its first `if` /
        // `return`): a dirty node that is itself NOT an EOGStarterHolder (here, the Call nested
        // inside "caller"'s body) must resolve to its nearest enclosing EOGStarterHolder ancestor
        // (the Function), not the dirty node itself. Both Function and TranslationUnit are
        // themselves EOGStarterHolders, so every actual markDirty call site for an EOGStarterPass
        // in this codebase (see IncrementalUpdate.kt) already resolves to one of those two before
        // marking -- always hitting the very first `if`, never this branch. Marking the Call
        // directly, bypassing those helpers, exercises it here.
        //
        // SymbolResolver has a hard @DependsOn EvaluationOrderGraphPass/TypeResolver/
        // TypeHierarchyResolver/ImportResolver, so those four (but deliberately NOT
        // SymbolResolver itself) are registered for the initial analyze() -- otherwise
        // SymbolResolver's own EOGStarterPass traversal has no EOG graph to walk at all and
        // silently resolves nothing, regardless of which target resolveEOGStarterTargets picks.
        // The call to 'callee' is still genuinely unresolved afterward, since SymbolResolver
        // itself never ran.
        val topLevel =
            Files.createTempDirectory("cpg-partial-pass-eog-ancestor-test").toFile().apply {
                deleteOnExit()
            }
        val sourceFile = tempSource(topLevel, "both.stale", "twofuncs:caller:callee")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(sourceFile)
                .registerLanguage<StaleStubTestLanguage>()
                .registerPass<TypeHierarchyResolver>()
                .registerPass<ImportResolver>()
                .registerPass<EvaluationOrderGraphPass>()
                .registerPass<TypeResolver>()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()

        val calleeCall = result.calls.single { it.name.localName == "callee" }
        assertTrue(calleeCall.invokes.isEmpty(), "SymbolResolver must not have run yet")

        calleeCall.markDirty<SymbolResolver>()

        runDirtyPasses(result)

        val callee = result.functions.single { it.name.localName == "callee" }
        assertEquals(
            listOf(callee),
            calleeCall.invokes,
            "Expected SymbolResolver to have run on the enclosing function and resolved the call",
        )
    }

    @Test
    fun testResolveEOGStarterTargetsScansDescendantsWhenNoAncestorQualifies() {
        // The descendant-fallback branch of resolveEOGStarterTargets: since both Function and
        // TranslationUnit are themselves EOGStarterHolders, a dirty node only ever reaches this
        // branch if NEITHER it NOR any of its ancestors qualify -- i.e. there is no enclosing
        // Function/TranslationUnit/Namespace/Record/CatchClause at all. No real markDirty call
        // site produces that (every one resolves to a Function/TranslationUnit first), so this is
        // exercised here by marking the Component itself dirty directly: a Component is not an
        // EOGStarterHolder, and nothing above it in the AST is either, so resolution falls all the
        // way through to the allChildrenWithOverlays scan of its entire subtree.
        //
        // See the ancestor-branch test above for why TypeHierarchyResolver/ImportResolver/
        // EvaluationOrderGraphPass/TypeResolver (SymbolResolver's hard @DependsOn passes) are
        // registered for the initial analyze(), but deliberately not SymbolResolver itself.
        val topLevel =
            Files.createTempDirectory("cpg-partial-pass-eog-descendant-test").toFile().apply {
                deleteOnExit()
            }
        val sourceFile = tempSource(topLevel, "both.stale", "twofuncs:caller:callee")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(sourceFile)
                .registerLanguage<StaleStubTestLanguage>()
                .registerPass<TypeHierarchyResolver>()
                .registerPass<ImportResolver>()
                .registerPass<EvaluationOrderGraphPass>()
                .registerPass<TypeResolver>()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single()

        val calleeCall = result.calls.single { it.name.localName == "callee" }
        assertTrue(calleeCall.invokes.isEmpty(), "SymbolResolver must not have run yet")

        component.markDirty<SymbolResolver>()

        runDirtyPasses(result)

        val callee = result.functions.single { it.name.localName == "callee" }
        assertEquals(
            listOf(callee),
            calleeCall.invokes,
            "Expected the descendant scan to find 'caller' inside the dirty component and run " +
                "SymbolResolver on it",
        )
    }

    @Test
    fun testRunDirtyPassesFallsBackToUnorderedExecutionOnCyclicDependency() {
        // PartialPassExecution.kt:95-102: PassOrderingHelper.order() throws a
        // ConfigurationException for a dirty pass set with an unsatisfiable (here: circular)
        // dependency. runDirtyPasses must not propagate that exception -- it logs a warning and
        // falls back to running the dirty passes unordered, so both still get processed and
        // cleaned instead of being silently dropped or crashing the whole incremental update.
        val topLevel =
            Files.createTempDirectory("cpg-partial-pass-cyclic-order-test").toFile().apply {
                deleteOnExit()
            }
        val sourceFile = tempSource(topLevel, "caller.stale", "define:seed")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(sourceFile)
                .registerLanguage<StaleStubTestLanguage>()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()

        result.markDirty(result, CyclicPassA::class)
        result.markDirty(result, CyclicPassB::class)

        runDirtyPasses(result)

        // Despite the ordering failure, the fallback (unordered) execution still ran -- and
        // cleaned up -- both passes.
        assertFalse(result.dirtyNodes[result]?.contains(CyclicPassA::class) == true)
        assertFalse(result.dirtyNodes[result]?.contains(CyclicPassB::class) == true)
    }

    @Test
    fun testRunDirtyPassesStopsRequeueingAPassOnceMaxExecutionsIsReached() {
        // PartialPassExecution.kt:113-115: a pass that keeps re-dirtying itself (here: alternating
        // between two proxy nodes every time it runs) would otherwise loop forever; maxExecutions
        // caps how many times runDirtyPasses reruns the SAME pass class within one call, logging a
        // warning and skipping it (leaving it dirty) once the cap is reached.
        val topLevel =
            Files.createTempDirectory("cpg-partial-pass-max-executions-test").toFile().apply {
                deleteOnExit()
            }
        val sourceFile = tempSource(topLevel, "caller.stale", "define:seed")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(sourceFile)
                .registerLanguage<StaleStubTestLanguage>()
                .maxPassExecutions(2)
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        val component = result.components.single()

        PingPongPass.executionCount = 0
        PingPongPass.proxyA = result
        PingPongPass.proxyB = component

        result.markDirty(PingPongPass.proxyA, PingPongPass::class)

        runDirtyPasses(result)

        assertEquals(
            2,
            PingPongPass.executionCount,
            "Expected exactly maxExecutions runs, not more",
        )
        assertTrue(
            result.dirtyNodes[PingPongPass.proxyA]?.contains(PingPongPass::class) == true,
            "Expected the pass to be left dirty once the execution cap was hit",
        )
    }

    @Test
    fun testRunDirtyPassesImmediatelyProcessesAPassDirtiedDuringTheSameRun() {
        // PartialPassExecution.kt:143: a pass dirtied as a side effect of running another pass
        // (here: DirtiesOtherPass marking TriggeredPass dirty) must be picked up and run within the
        // SAME runDirtyPasses call, even though it was never part of the originally-dirty pass set
        // computed at the start of the call.
        val topLevel =
            Files.createTempDirectory("cpg-partial-pass-cascading-dirty-test").toFile().apply {
                deleteOnExit()
            }
        val sourceFile = tempSource(topLevel, "caller.stale", "define:seed")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(sourceFile)
                .registerLanguage<StaleStubTestLanguage>()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()

        TriggeredPass.ran = false
        result.markDirty(result, DirtiesOtherPass::class)

        runDirtyPasses(result)

        assertTrue(
            TriggeredPass.ran,
            "Expected TriggeredPass to run as a cascading effect within the same call",
        )
        assertFalse(result.dirtyNodes[result]?.contains(TriggeredPass::class) == true)
    }

    @Test
    fun testRunDirtyPassesStopsEarlyWhenAnalysisIsCancelled() {
        // PartialPassExecution.kt:146-149 (mirrors the batch driver's identical check in
        // Pass.kt:357): if TranslationManager.cancel() is called while runDirtyPasses is still
        // processing its queue, it must stop before running any further queued passes, instead of
        // finishing the remaining work anyway.
        //
        // CancellingPass dirties NeverRunsPass AND cancels in the same accept() call, so there is
        // no dependency on queue/pass ordering: NeverRunsPass only becomes known to the queue AFTER
        // cancellation has already been requested, exactly once runDirtyPasses's own isCancelled
        // check (reached right after) is about to see it.
        val topLevel =
            Files.createTempDirectory("cpg-partial-pass-cancel-test").toFile().apply {
                deleteOnExit()
            }
        val sourceFile = tempSource(topLevel, "caller.stale", "define:seed")

        val config =
            TranslationConfiguration.builder()
                .topLevel(topLevel)
                .sourceLocations(sourceFile)
                .registerLanguage<StaleStubTestLanguage>()
                .disableCleanup()
                .build()

        val manager = TranslationManager.builder().config(config).build()
        val result = manager.analyze().get()
        assertFalse(result.isCancelled)

        CancellingPass.ran = false
        CancellingPass.manager = manager
        NeverRunsPass.ran = false

        result.markDirty(result, CancellingPass::class)

        runDirtyPasses(result)

        assertTrue(CancellingPass.ran, "Expected CancellingPass to run and trigger cancellation")
        assertTrue(result.isCancelled)
        assertFalse(
            NeverRunsPass.ran,
            "Expected the pass dirtied after cancellation to never actually run",
        )
        assertTrue(
            result.dirtyNodes[result]?.contains(NeverRunsPass::class) == true,
            "Expected it to remain queued/dirty, just never processed",
        )
    }
}
