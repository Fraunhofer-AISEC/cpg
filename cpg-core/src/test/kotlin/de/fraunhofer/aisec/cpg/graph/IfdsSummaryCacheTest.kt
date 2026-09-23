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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.frontends.TestLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.edges.flows.CallingContextIn
import de.fraunhofer.aisec.cpg.graph.edges.flows.CallingContextOut
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [IfdsSummaryCache]: [IfdsSummaryCache.markSink] / [IfdsSummaryCache.isDirty]
 * transitivity and the [de.fraunhofer.aisec.cpg.graph.OverlayNode] / [Node] resolution behind them,
 * and end-to-end fast-path replay through [ifdsReachingSources] once a cache is installed on a
 * [Component].
 */
class IfdsSummaryCacheTest {

    /**
     * Marking a callee dirty must propagate transitively up the call graph (caller of the callee,
     * caller of that caller, ...), matching the documented sticky/monotonic dirtiness semantics.
     */
    @Test
    fun markSinkPropagatesTransitivelyUpTheCallGraph() {
        with(TestLanguageFrontend()) {
            val main = newFunction("main")
            val mid = newFunction("mid")
            val leaf = newFunction("leaf")

            val callMainToMid = newCall(fqn = "mid")
            callMainToMid.invokes += mid
            main.body = callMainToMid

            val callMidToLeaf = newCall(fqn = "leaf")
            callMidToLeaf.invokes += leaf
            mid.body = callMidToLeaf

            val cache = IfdsSummaryCache(Backward(GraphToFollow.EOG))
            assertFalse(cache.isDirty(leaf))
            assertFalse(cache.isDirty(mid))
            assertFalse(cache.isDirty(main))

            cache.markSink(leaf)

            assertTrue(cache.isDirty(leaf))
            assertTrue(cache.isDirty(mid), "dirtiness must propagate to leaf's caller")
            assertTrue(cache.isDirty(main), "dirtiness must propagate transitively to main")
        }
    }

    /**
     * A node whose [de.fraunhofer.aisec.cpg.graph.declarations.Function] cannot be resolved (no AST
     * parent, not a Function itself) must be treated conservatively as dirty by [isDirty], and
     * [markSink] on such a node must be a safe no-op rather than throwing.
     */
    @Test
    fun unresolvableFunctionIsConservativelyDirty() {
        with(TestLanguageFrontend()) {
            val freeFloating = newLiteral(1)
            val cache = IfdsSummaryCache(Backward(GraphToFollow.DFG))

            assertTrue(cache.isDirty(freeFloating), "unknown-function callees must be dirty")
            cache.markSink(freeFloating)
            assertTrue(cache.isDirty(freeFloating))
        }
    }

    /**
     * [IfdsSummaryCache] resolves the owning function of an
     * [de.fraunhofer.aisec.cpg.graph.OverlayNode] both when its [OverlayNode.underlyingNode] is a
     * Function directly, and when it is some other node nested inside a function's AST (via
     * [firstParentOrNull]).
     */
    @Test
    fun functionOfResolvesThroughOverlayNodes() {
        with(TestLanguageFrontend()) {
            val fn = newFunction("fn")
            val overlayOnFunction = object : OverlayNode() {}
            overlayOnFunction.underlyingNode = fn

            val fn2 = newFunction("fn2")
            val innerNode2 = newLiteral(2)
            fn2.body = innerNode2
            val overlayOnInner = object : OverlayNode() {}
            overlayOnInner.underlyingNode = innerNode2

            val cache = IfdsSummaryCache(Backward(GraphToFollow.DFG))

            cache.markSink(overlayOnFunction)
            assertTrue(cache.isDirty(fn), "overlay directly on a Function must dirty that Function")

            cache.markSink(overlayOnInner)
            assertTrue(
                cache.isDirty(fn2),
                "overlay on a node nested in a function must dirty the enclosing Function",
            )
        }
    }

    /**
     * End-to-end: once a cache is installed on the [Component] and the callee `f` is never marked
     * dirty, [ifdsReachingSources] must take the fast (pure-summary-replay) path and still produce
     * byte-for-byte the same result as the uncached solver -- both on the first (cold) and a
     * subsequent (warm, already-[de.fraunhofer.aisec.cpg.graph.IfdsSummaryCache] complete) query,
     * and after `f` is later marked dirty (forcing the slow path).
     */
    @Test
    fun cacheFastPathPreservesResultsAndSurvivesDirtying() {
        with(TestLanguageFrontend()) {
            val tu = TranslationUnit()
            val component = Component()
            component.addTranslationUnit(tu)

            val mainFn = newFunction("main")
            val f = newFunction("f")
            tu.addDeclaration(mainFn)
            tu.addDeclaration(f)

            val q = newLiteral(1)
            val s1 = newLiteral(2)
            val s2 = newLiteral(3)
            val c1 = newCall(fqn = "f")
            val c2 = newCall(fqn = "f")
            c1.invokes += f
            c2.invokes += f
            s1.nextEOG += c1
            c1.nextEOG += q
            s2.nextEOG += c2
            mainFn.body = q

            val direction = Backward(GraphToFollow.EOG)
            val predicate: (Node) -> Boolean = { it === s1 || it === s2 }

            val uncached = q.ifdsReachingSources(direction, predicate = predicate)
            assertEquals(setOf<Node>(s1), uncached)

            val cache = IfdsSummaryCache(direction)
            component.ifdsSummaryCache = cache
            assertFalse(cache.isDirty(f), "f is never marked as a sink, so it must be clean")

            val cachedFirst = q.ifdsReachingSources(direction, predicate = predicate)
            assertEquals(uncached, cachedFirst, "cache must never change the result (cold)")

            // Re-run to exercise the already-complete pure-summary reuse path.
            val cachedSecond = q.ifdsReachingSources(direction, predicate = predicate)
            assertEquals(uncached, cachedSecond, "cache must never change the result (warm)")

            cache.markSink(f)
            assertTrue(cache.isDirty(f))
            val cachedAfterDirty = q.ifdsReachingSources(direction, predicate = predicate)
            assertEquals(
                uncached,
                cachedAfterDirty,
                "dirtying a callee must fall back to the exact tabulation, same result",
            )

            component.ifdsSummaryCache = null
        }
    }

    /**
     * A cache installed for a DIFFERENT [AnalysisDirection] shape (here DFG vs. the query's EOG)
     * must never be used, and the query must still produce the correct, uncached result.
     */
    @Test
    fun cacheWithMismatchedDirectionShapeIsIgnored() {
        with(TestLanguageFrontend()) {
            val tu = TranslationUnit()
            val component = Component()
            component.addTranslationUnit(tu)

            val mainFn = newFunction("main")
            val f = newFunction("f")
            tu.addDeclaration(mainFn)
            tu.addDeclaration(f)

            val q = newLiteral(1)
            val s1 = newLiteral(2)
            val c1 = newCall(fqn = "f")
            c1.invokes += f
            s1.nextEOG += c1
            c1.nextEOG += q
            mainFn.body = q

            // Wrong shape: cache tabulated for DFG, query follows EOG.
            component.ifdsSummaryCache = IfdsSummaryCache(Backward(GraphToFollow.DFG))

            val result = q.ifdsReachingSources(Backward(GraphToFollow.EOG)) { it === s1 }
            assertEquals(setOf<Node>(s1), result, "mismatched-shape cache must be ignored")

            component.ifdsSummaryCache = null
        }
    }

    /**
     * The fast path only ever applies at full context sensitivity (`k == Int.MAX_VALUE`): even with
     * a cache installed and the callee clean, `k = 0` must still go through the exact (here,
     * context-insensitive) tabulation, matching the uncached `k = 0` behaviour.
     */
    @Test
    fun kZeroBypassesTheFastPathEvenWithACleanCachedCallee() {
        with(TestLanguageFrontend()) {
            val tu = TranslationUnit()
            val component = Component()
            component.addTranslationUnit(tu)

            val mainFn = newFunction("main")
            val innerFn = newFunction("inner")
            tu.addDeclaration(mainFn)
            tu.addDeclaration(innerFn)

            val q = newLiteral(1)
            val retBody = newLiteral(2)
            val param = newLiteral(3)
            val arg1 = newLiteral(4)
            val arg2 = newLiteral(5)
            val c1 = Call()
            val c2 = Call()

            mainFn.body = q
            innerFn.body = retBody

            q.prevDFGEdges.addContextSensitive(
                retBody,
                callingContext = CallingContextOut(mutableListOf(c1)),
            )
            retBody.prevDFGEdges += param
            param.prevDFGEdges.addContextSensitive(
                arg1,
                callingContext = CallingContextIn(mutableListOf(c1)),
            )
            param.prevDFGEdges.addContextSensitive(
                arg2,
                callingContext = CallingContextIn(mutableListOf(c2)),
            )

            val direction = Backward(GraphToFollow.DFG)
            val predicate: (Node) -> Boolean = { it === arg1 || it === arg2 }

            component.ifdsSummaryCache = IfdsSummaryCache(direction)
            assertFalse(component.ifdsSummaryCache!!.isDirty(retBody))

            val ifdsZero = q.ifdsReachingSources(direction, k = 0, predicate = predicate)
            assertEquals(
                setOf<Node>(arg1, arg2),
                ifdsZero,
                "k=0 must ignore the pushdown regardless of the cache",
            )

            component.ifdsSummaryCache = null
        }
    }

    /**
     * A clean callee `F` that itself calls another clean callee `G` before returning: replaying
     * `F`'s pure summary must transparently resolve through the nested nested call into `G` and
     * back out again, exercising the intraprocedural, call (push), and return-with-waiting-caller
     * (pop, notifying a registered caller) branches of the pure tabulation in one pass.
     */
    @Test
    fun cacheFastPathResolvesThroughNestedCleanCallees() {
        with(TestLanguageFrontend()) {
            val tu = TranslationUnit()
            val component = Component()
            component.addTranslationUnit(tu)

            val mainFn = newFunction("main")
            val funcF = newFunction("F")
            val funcG = newFunction("G")
            tu.addDeclaration(mainFn)
            tu.addDeclaration(funcF)
            tu.addDeclaration(funcG)

            val q = newLiteral(1)
            val retF = newLiteral(2)
            val callSiteInF = newLiteral(3)
            val retG = newLiteral(4)
            val paramG = newLiteral(5)
            val resumeInF = newLiteral(6)
            val paramF = newLiteral(7)
            val arg1 = newLiteral(8)

            mainFn.body = q
            funcF.body = retF
            funcG.body = retG

            val c1 = Call()
            val c2 = Call()

            // q <-[out c1]- retF <- callSiteInF <-[out c2]- retG <- paramG <-[in c2]- resumeInF <-
            // paramF <-[in c1]- arg1
            q.prevDFGEdges.addContextSensitive(
                retF,
                callingContext = CallingContextOut(mutableListOf(c1)),
            )
            retF.prevDFGEdges += callSiteInF
            callSiteInF.prevDFGEdges.addContextSensitive(
                retG,
                callingContext = CallingContextOut(mutableListOf(c2)),
            )
            retG.prevDFGEdges += paramG
            paramG.prevDFGEdges.addContextSensitive(
                resumeInF,
                callingContext = CallingContextIn(mutableListOf(c2)),
            )
            resumeInF.prevDFGEdges += paramF
            paramF.prevDFGEdges.addContextSensitive(
                arg1,
                callingContext = CallingContextIn(mutableListOf(c1)),
            )

            val direction = Backward(GraphToFollow.DFG)
            val predicate: (Node) -> Boolean = { it === arg1 }

            val uncached = q.ifdsReachingSources(direction, predicate = predicate)
            assertEquals(setOf<Node>(arg1), uncached)

            val cache = IfdsSummaryCache(direction)
            component.ifdsSummaryCache = cache
            assertFalse(cache.isDirty(retF))
            assertFalse(cache.isDirty(retG))

            val cached = q.ifdsReachingSources(direction, predicate = predicate)
            assertEquals(uncached, cached, "nested clean callees must still resolve to {arg1}")

            component.ifdsSummaryCache = null
        }
    }
}
