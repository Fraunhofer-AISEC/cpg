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
import de.fraunhofer.aisec.cpg.passes.TypeHierarchyResolver
import de.fraunhofer.aisec.cpg.passes.TypeResolver
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

        // Before the partial pass re-run: the call is left unresolved (stale edge already
        // removed by updateIncrementally, but SymbolResolver has not run again yet).
        assertTrue(fooCall.invokes.isEmpty())

        assertTrue(result.dirtyNodes[realFoo]?.contains(SymbolResolver::class) == true)

        manager.runDirtyPasses(result)

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
        incrementalManager.runDirtyPasses(incrementalResult)

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

        manager.runDirtyPasses(result)

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
}
