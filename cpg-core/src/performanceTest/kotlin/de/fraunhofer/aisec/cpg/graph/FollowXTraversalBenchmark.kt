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
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import java.io.File
import java.util.IdentityHashMap
import kotlin.test.Test
import kotlin.test.assertTrue
import org.junit.jupiter.api.Tag

/**
 * A micro-benchmark and stress-test harness for the core graph traversal engine [followXUntilHit].
 *
 * Instead of building a full CPG via a language frontend (which is slow and hard to shape into
 * precise loop/recursion topologies), these benchmarks drive [followXUntilHit] directly with a
 * synthetic `x` next-step callback that walks a hand-built [SyntheticGraph]. This isolates the cost
 * of the *engine* (worklist management, loop detection, path bookkeeping) from frontend cost and
 * lets us reproduce the two pathological regimes the engine currently struggles with:
 * 1. **Unbounded interprocedural context** – recursion keeps pushing new
 *    [de.fraunhofer.aisec.cpg.graph.expressions.Call]s onto the context call stack, so `(node,
 *    callStack)` never repeats and the on-path loop check never fires. This mirrors `complex_dfg.c`
 *    (mutual recursion `func2` <-> `func3`). With `findAllPossiblePaths = true` the current engine
 *    does not terminate.
 * 2. **Exponential path enumeration** – a chain of diamonds (or a loop containing a branch) has an
 *    exponential number of distinct paths. `findAllPossiblePaths = true` materializes all of them.
 *
 * Every scenario is guarded by a [Budget] that aborts the traversal after a fixed number of
 * next-step evaluations, so a pathological run reports "BUDGET EXCEEDED" in bounded time/memory
 * instead of hanging the JVM. The number of next-step evaluations ("steps") is the primary
 * efficiency metric; wall-clock time is reported as a secondary, machine-dependent signal.
 */
@Tag("experimental")
class FollowXTraversalBenchmark {

    /**
     * A minimal concrete [de.fraunhofer.aisec.cpg.graph.edges.Edge] so the synthetic graph can hand
     * real edges to the engine.
     */
    private class BenchEdge(start: Node, end: Node) : Edge<Node>(start, end) {
        override var labels: Set<String> = emptySet()

        override fun clone(): Edge<Node> = BenchEdge(start, end)
    }

    /** Thrown by the next-step callback when the [Budget] runs out. */
    private class BudgetExceeded(val steps: Long) : RuntimeException()

    /** Counts how many times the next-step callback is invoked and aborts once [limit] is hit. */
    private class Budget(val limit: Long) {
        var steps: Long = 0L
            private set

        fun tick() {
            steps++
            if (steps > limit) throw BudgetExceeded(steps)
        }
    }

    /** How the context call stack is manipulated when an edge is followed. */
    private sealed interface StackOp {
        object None : StackOp

        /** Push a call – simulates entering a callee (interprocedural). */
        data class Push(val call: Call) : StackOp

        /** Pop the given call if it is on top – simulates returning from a callee. */
        data class Pop(val call: Call) : StackOp
    }

    /**
     * A tiny directed multigraph over [Node]s where each edge optionally mutates the context call
     * stack. Turned into an `x` callback via [asNextStep].
     */
    private class SyntheticGraph {
        val adjacency = IdentityHashMap<Node, MutableList<Pair<Node, StackOp>>>()
        var nodeCount = 0

        fun edge(from: Node, to: Node, op: StackOp = StackOp.None) {
            adjacency.getOrPut(from) { mutableListOf() }.add(to to op)
        }

        /**
         * Builds the next-step callback expected by [followXUntilHit]. Each successor gets a fresh
         * cloned [Context] with the edge's [StackOp] applied, so branches never share mutable
         * state.
         */
        fun asNextStep(
            budget: Budget
        ): (
            Node, Context, List<Triple<Node, Edge<Node>?, Context>>, MutableSet<NodePath>,
        ) -> Collection<Triple<Node, Edge<Node>, Context>> = { current, ctx, _, _ ->
            budget.tick()
            adjacency[current].orEmpty().map { (next, op) ->
                val newCtx = ctx.clone()
                when (op) {
                    is StackOp.None -> {}
                    is StackOp.Push -> newCtx.callStack.push(op.call)
                    is StackOp.Pop -> newCtx.callStack.popIfOnTop(op.call)
                }
                Triple(next, BenchEdge(current, next) as Edge<Node>, newCtx)
            }
        }
    }

    private data class BenchResult(
        val scenario: String,
        val config: String,
        val steps: Long,
        val millis: Long,
        val fulfilled: Int,
        val failed: Int,
        val exceeded: Boolean,
    )

    private val results = mutableListOf<BenchResult>()

    /** Runs one scenario/config, guarded by [budgetLimit]; records steps, time and result sizes. */
    private fun run(
        scenario: String,
        config: String,
        graph: SyntheticGraph,
        start: Node,
        target: Node,
        findAllPossiblePaths: Boolean,
        collectFailedPaths: Boolean,
        budgetLimit: Long = 300_000L,
        continueAfterHit: Boolean = true,
        predicate: (Node) -> Boolean = { it === target },
    ): BenchResult =
        runX(
            scenario,
            config,
            start,
            target,
            findAllPossiblePaths,
            collectFailedPaths,
            budgetLimit,
            continueAfterHit = continueAfterHit,
            predicate = predicate,
        ) { budget ->
            graph.asNextStep(budget)
        }

    /** Like [run] but takes an arbitrary next-step callback factory (given the [Budget]). */
    private fun runX(
        scenario: String,
        config: String,
        start: Node,
        target: Node,
        findAllPossiblePaths: Boolean,
        collectFailedPaths: Boolean,
        budgetLimit: Long = 300_000L,
        continueAfterHit: Boolean = true,
        predicate: (Node) -> Boolean = { it === target },
        nextStepFactory:
            (Budget) -> (
                    Node, Context, List<Triple<Node, Edge<Node>?, Context>>, MutableSet<NodePath>,
                ) -> Collection<Triple<Node, Edge<Node>, Context>>,
    ): BenchResult {
        val budget = Budget(budgetLimit)
        val startNanos = System.nanoTime()
        var fulfilled = 0
        var failed = 0
        var exceeded = false
        try {
            val res =
                start.followXUntilHit(
                    x = nextStepFactory(budget),
                    collectFailedPaths = collectFailedPaths,
                    findAllPossiblePaths = findAllPossiblePaths,
                    continueAfterHit = continueAfterHit,
                    earlyTermination = { _, _ -> false },
                    predicate = predicate,
                )
            fulfilled = res.fulfilled.size
            failed = res.failed.size
        } catch (e: BudgetExceeded) {
            exceeded = true
        }
        val millis = (System.nanoTime() - startNanos) / 1_000_000
        val result =
            BenchResult(scenario, config, budget.steps, millis, fulfilled, failed, exceeded)
        results.add(result)
        return result
    }

    private fun printTable() {
        val sb = StringBuilder()
        sb.appendLine("================ followXUntilHit benchmark ================")
        sb.appendLine(
            "%-34s | %-9s | %10s | %8s | %5s | %5s | %s"
                .format("scenario", "config", "steps", "millis", "ful", "fail", "status")
        )
        sb.appendLine("-".repeat(100))
        for (r in results) {
            sb.appendLine(
                "%-34s | %-9s | %10d | %8d | %5d | %5d | %s"
                    .format(
                        r.scenario,
                        r.config,
                        r.steps,
                        r.millis,
                        r.fulfilled,
                        r.failed,
                        if (r.exceeded) "BUDGET EXCEEDED (blowup)" else "ok",
                    )
            )
        }
        sb.appendLine("=".repeat(100))
        val table = sb.toString()
        println("\n$table")
        // Also persist to a file so the table survives Gradle swallowing test stdout.
        runCatching {
            File(System.getProperty("java.io.tmpdir"), "followx-bench.txt").writeText(table)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Scenario builders
    // ---------------------------------------------------------------------------------------------

    /** A straight chain start -> n1 -> ... -> target of [length] edges. No branching, no loops. */
    private fun TestLanguageFrontend.linearChain(length: Int): Triple<SyntheticGraph, Node, Node> {
        val g = SyntheticGraph()
        val nodes = (0..length).map { newReference("n$it") }
        for (i in 0 until length) g.edge(nodes[i], nodes[i + 1])
        g.nodeCount = nodes.size
        return Triple(g, nodes.first(), nodes.last())
    }

    /**
     * [count] diamonds in series: each diamond splits into two parallel nodes that re-merge. There
     * are 2^count distinct paths from start to target but only O(count) nodes.
     */
    private fun TestLanguageFrontend.diamondChain(count: Int): Triple<SyntheticGraph, Node, Node> {
        val g = SyntheticGraph()
        var current = newReference("d_start")
        val start = current
        for (i in 0 until count) {
            val left = newReference("d${i}_l")
            val right = newReference("d${i}_r")
            val merge = newReference("d${i}_m")
            g.edge(current, left)
            g.edge(current, right)
            g.edge(left, merge)
            g.edge(right, merge)
            current = merge
        }
        g.nodeCount = count * 3 + 1
        return Triple(g, start, current)
    }

    /**
     * A single intraprocedural self-loop: start -> head, head -> head (loop), head -> target. The
     * context never changes, so the on-path loop check should catch this immediately.
     */
    private fun TestLanguageFrontend.selfLoop(): Triple<SyntheticGraph, Node, Node> {
        val g = SyntheticGraph()
        val start = newReference("l_start")
        val head = newReference("l_head")
        val target = newReference("l_target")
        g.edge(start, head)
        g.edge(head, head)
        g.edge(head, target)
        g.nodeCount = 3
        return Triple(g, start, target)
    }

    /**
     * Interprocedural recursion whose call sites *repeat*: A -> B pushes the same [cA], B -> A
     * pushes the same [cB]. The call stack grows as [cA, cB, cA, cB, ...]; the repeating pattern is
     * what the current `isLoop()` heuristic is meant to (eventually) catch. A also branches to the
     * target, so `findAllPossiblePaths` enumerates one exit per recursion depth.
     */
    private fun TestLanguageFrontend.recursionRepeatingCalls(): Triple<SyntheticGraph, Node, Node> {
        val g = SyntheticGraph()
        val entry = newReference("r_entry")
        val a = newReference("r_a")
        val b = newReference("r_b")
        val target = newReference("r_target")
        val cA = newCall(newReference("call_a_to_b"))
        val cB = newCall(newReference("call_b_to_a"))
        g.edge(entry, a)
        g.edge(a, b, StackOp.Push(cA))
        g.edge(b, a, StackOp.Push(cB))
        g.edge(a, target)
        g.nodeCount = 4
        return Triple(g, entry, target)
    }

    /**
     * Adversarial interprocedural recursion whose context is *unbounded and never repeats*: every
     * time around the cycle a brand-new [Call] is pushed onto the call stack. `(node, callStack)`
     * therefore never repeats and `isLoop()` never sees a repeating pattern, so the current engine
     * does not terminate. This is the distilled essence of the `complex_dfg.c` hang.
     *
     * We return the four nodes plus a next-step factory that mints a fresh [Call] on every push, so
     * the call stack grows without bound. `a` also offers a direct exit to the target.
     */
    private fun TestLanguageFrontend.recursionFreshCalls():
        Triple<
            Node,
            Node,
            (Budget) -> (
                    Node, Context, List<Triple<Node, Edge<Node>?, Context>>, MutableSet<NodePath>,
                ) -> Collection<Triple<Node, Edge<Node>, Context>>,
        > {
        val entry = newReference("f_entry")
        val a = newReference("f_a")
        val b = newReference("f_b")
        val target = newReference("f_target")
        val factory:
            (Budget) -> (
                    Node, Context, List<Triple<Node, Edge<Node>?, Context>>, MutableSet<NodePath>,
                ) -> Collection<Triple<Node, Edge<Node>, Context>> =
            { budget ->
                { current, ctx, _, _ ->
                    budget.tick()
                    when (current) {
                        entry -> listOf(Triple(a, BenchEdge(entry, a) as Edge<Node>, ctx.clone()))
                        a ->
                            listOf(
                                // recurse into b, pushing a brand-new call every time
                                ctx.clone().let {
                                    it.callStack.push(newCall(newReference("c_ab")))
                                    Triple(b, BenchEdge(a, b) as Edge<Node>, it)
                                },
                                // exit to target
                                Triple(target, BenchEdge(a, target) as Edge<Node>, ctx.clone()),
                            )
                        b ->
                            listOf(
                                ctx.clone().let {
                                    it.callStack.push(newCall(newReference("c_ba")))
                                    Triple(a, BenchEdge(b, a) as Edge<Node>, it)
                                }
                            )
                        else -> emptyList()
                    }
                }
            }
        return Triple(entry, target, factory)
    }

    /**
     * A start node with two ways to reach a predicate-satisfying node: an immediate hit `t1` and a
     * second hit `t2` at the end of a long chain of [chainLength] non-target nodes. A MAY analysis
     * with `continueAfterHit = false` should stop at `t1` almost immediately (1 witness, ~1 step),
     * while `continueAfterHit = true` keeps exploring the whole chain to also record `t2` (2
     * witnesses, ~[chainLength] steps). Returns the graph, the start node and the set of target
     * nodes.
     */
    private fun TestLanguageFrontend.multiTargetFan(
        chainLength: Int
    ): Triple<SyntheticGraph, Node, Set<Node>> {
        val g = SyntheticGraph()
        val start = newReference("mt_start")
        val t1 = newReference("mt_t1")
        g.edge(start, t1)
        var current = newReference("mt_c0")
        g.edge(start, current)
        for (i in 1..chainLength) {
            val next = newReference("mt_c$i")
            g.edge(current, next)
            current = next
        }
        val t2 = newReference("mt_t2")
        g.edge(current, t2)
        g.nodeCount = chainLength + 4
        return Triple(g, start, setOf(t1, t2))
    }

    // ---------------------------------------------------------------------------------------------
    // The benchmark entry point
    // ---------------------------------------------------------------------------------------------

    @Test
    fun runBenchmarks() {
        // Budgets are kept low on purpose: the current engine stores a full path per worklist item,
        // so an exploding scenario exhausts the heap long before a large step budget is reached. A
        // low budget lets the pathological cases report "BUDGET EXCEEDED" (the blow-up signal) in
        // bounded memory instead of OOM-ing the JVM.
        val smallBudget = 40_000L
        val tinyBudget = 8_000L
        // Filled in by scenario 6 below; asserted on after the table is printed.
        var mtAll: BenchResult? = null
        var mtFirst: BenchResult? = null
        with(TestLanguageFrontend()) {
            // 1. Linear chain scaling – exposes the O(W^2) `worklist.maxBy { it.size }` scan even
            // on
            //    a trivial acyclic graph. Steps should be ~length; wall-clock should stay roughly
            //    linear for an efficient engine (it currently does not).
            for (len in listOf(500, 1000, 2000, 4000)) {
                val (g, start, target) = linearChain(len)
                run("linearChain($len)", "MAY", g, start, target, false, false, smallBudget)
            }

            // 2. Single self-loop – must terminate immediately in both regimes.
            selfLoop().let { (g, s, t) ->
                run("selfLoop", "MAY", g, s, t, false, false, smallBudget)
                run("selfLoop", "MUST", g, s, t, true, true, smallBudget)
            }

            // 3. Diamond chain – exponential number of distinct paths. MAY needs one; MUST
            // currently
            //    enumerates all 2^count of them (exponential time AND memory).
            for (count in listOf(6, 10, 14)) {
                val (g, start, target) = diamondChain(count)
                run("diamondChain($count)", "MAY", g, start, target, false, false, smallBudget)
                run("diamondChain($count)", "MUST", g, start, target, true, true, smallBudget)
            }

            // 4. Interprocedural recursion with repeating call sites.
            recursionRepeatingCalls().let { (g, s, t) ->
                run("recursion-repeating", "MAY", g, s, t, false, false, smallBudget)
                run("recursion-repeating", "MUST", g, s, t, true, true, smallBudget)
            }

            // 5. Interprocedural recursion with unbounded/never-repeating context – the core hang.
            //    Kept on a tiny budget: with the current engine neither regime terminates, so we
            //    only want enough steps to prove the blow-up in bounded memory.
            recursionFreshCalls().let { (s, t, factory) ->
                runX(
                    "recursion-fresh",
                    "MAY",
                    s,
                    t,
                    false,
                    false,
                    tinyBudget,
                    nextStepFactory = factory,
                )
                runX(
                    "recursion-fresh",
                    "MUST",
                    s,
                    t,
                    true,
                    true,
                    tinyBudget,
                    nextStepFactory = factory,
                )
            }

            // 6. Multi-target fan – demonstrates the `continueAfterHit` MAY early-exit. Two
            //    reachable targets: an immediate hit and one behind a long chain. With
            //    `continueAfterHit = false` the BFS returns on the first hit (~1 step, one
            // witness);
            //    with the default it explores the whole chain to also record the second hit.
            val (mtG, mtStart, mtTargets) = multiTargetFan(2000)
            mtAll =
                run(
                    "multiTargetFan(2000)",
                    "MAY-all",
                    mtG,
                    mtStart,
                    mtTargets.first(),
                    false,
                    false,
                    smallBudget,
                    continueAfterHit = true,
                    predicate = { it in mtTargets },
                )
            mtFirst =
                run(
                    "multiTargetFan(2000)",
                    "MAY-1st",
                    mtG,
                    mtStart,
                    mtTargets.first(),
                    false,
                    false,
                    smallBudget,
                    continueAfterHit = false,
                    predicate = { it in mtTargets },
                )
        }
        printTable()

        // Sanity invariant that always holds regardless of engine efficiency: a MAY query over a
        // finite acyclic chain must finish well within budget and find exactly one path.
        val chainMay = results.first { it.scenario == "linearChain(500)" && it.config == "MAY" }
        assertTrue(!chainMay.exceeded, "MAY over a 500-node chain must not exceed the step budget")
        assertTrue(chainMay.fulfilled == 1, "MAY over a chain must find exactly one path")

        // continueAfterHit early-exit: a full MAY traversal records both reachable targets, while
        // the early-exit variant stops on the first hit with a single witness and far fewer steps.
        val all = mtAll!!
        val first = mtFirst!!
        assertTrue(all.fulfilled == 2, "continueAfterHit=true must record both reachable targets")
        assertTrue(first.fulfilled == 1, "continueAfterHit=false must stop after the first hit")
        assertTrue(
            first.steps < all.steps,
            "continueAfterHit=false (${first.steps} steps) must expand far fewer nodes than a full " +
                "MAY traversal (${all.steps} steps)",
        )
    }
}
