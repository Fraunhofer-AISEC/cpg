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

import de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage
import de.fraunhofer.aisec.cpg.frontends.cxx.CPPLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Parameter
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.expressions.*
import de.fraunhofer.aisec.cpg.test.analyzeAndGetFirstTU
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Exercises [ControlDependenceGraphPass] and [ProgramDependenceGraphPass] against
 * `src/test/resources/complex_pdg.c`, a hand-annotated fixture whose comments call out specific
 * data- and control-dependencies that a correct PDG must contain. Each test below targets one of
 * those annotated dependencies and references the exact line(s) in `complex_pdg.c` it verifies, so
 * that fixture and assertions stay traceable to each other.
 */
class ComplexPdgTest {

    private fun parse() =
        File("src/test/resources/complex_pdg.c").let { file ->
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
                it.registerPass<ControlDependenceGraphPass>()
                it.registerPass<ProgramDependenceGraphPass>()
            }
        }

    /**
     * Same as [parse], but targets `dependenceGraphs/sgx_ra_get_msg3_trusted.cpp`, a decompiled
     * real-world function used as a regression fixture for
     * [forLoopDependsOnEnclosingIfNotInnerExit].
     */
    private fun parseSgx() =
        File("src/test/resources/dependenceGraphs/sgx_ra_get_msg3_trusted.cpp").let { file ->
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CPPLanguage>()
                it.registerPass<ControlDependenceGraphPass>()
                it.registerPass<ProgramDependenceGraphPass>()
            }
        }

    /**
     * Same as [parse], but additionally runs [PointsToPass] so that dependencies which only exist
     * through a pointer/alias (`*out`, `&checksum`, `alias`) are resolved precisely instead of
     * falling back to the coarser default [ControlFlowSensitiveDFGPass].
     */
    private fun parseWithPointsTo() =
        File("src/test/resources/complex_pdg.c").let { file ->
            analyzeAndGetFirstTU(listOf(file), file.parentFile.toPath(), true) {
                it.registerLanguage<CLanguage>()
                it.registerPass<PointsToPass>()
                it.registerPass<ControlDependenceGraphPass>()
                it.registerPass<ProgramDependenceGraphPass>()
            }
        }

    /**
     * `complex_pdg.c:14-27` (`transform`) is annotated with "The branch creates control dependence
     * on 'x > 0'." and "Data: tmp depends on x.". This verifies that both assignments to `tmp`
     * (line 19, in the `x > 0` branch, and line 22, in the `else` branch) are control-dependent on
     * the `x > 0` condition on the matching branch, and that the read of `x` used to compute `tmp`
     * on line 19 has a data dependency on the `x` parameter.
     */
    @Test
    fun branchesControlDependentOnX() {
        val tu = parse()
        assertNotNull(tu)

        val condition =
            tu.allChildren<IfElse> { it.location?.region?.startLine == 18 }.first().condition

        // complex_pdg.c:19 `tmp = x * 2;` only executes when `x > 0` is true.
        val tmpWriteThen =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 19 && it.name.localName == "tmp"
                }
                .first()
        val thenEdge = tmpWriteThen.prevCDGEdges.firstOrNull { it.start == condition }
        assertNotNull(
            thenEdge,
            "expected `tmp = x * 2` (line 19) to be control-dependent on `x > 0`",
        )
        assertTrue(true in thenEdge.branches)

        // complex_pdg.c:22 `tmp = -x;` only executes when `x > 0` is false.
        val tmpWriteElse =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 22 && it.name.localName == "tmp"
                }
                .first()
        val elseEdge = tmpWriteElse.prevCDGEdges.firstOrNull { it.start == condition }
        assertNotNull(elseEdge, "expected `tmp = -x` (line 22) to be control-dependent on `x > 0`")
        assertTrue(false in elseEdge.branches)

        // complex_pdg.c:19 "Data: tmp depends on x." -> the read of `x` in `x * 2` reaches back to
        // the `x` parameter.
        val xParam = tu.allChildren<Parameter> { it.name.localName == "x" }.first()
        val xRead =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 19 && it.name.localName == "x"
                }
                .first()
        assertTrue(xParam in xRead.prevDFG)
    }

    /**
     * `complex_pdg.c:53-63` (`process`) is annotated with "Multiple definitions of state make
     * reaching-definitions and phi-like merging interesting in the PDG.", and line 77 (`checksum +=
     * state;`) is annotated "Depends on branch-selected state.". This verifies that the read of
     * `state` on line 77 has a reaching data dependency on *both* prior definitions of `state`
     * (line 58, `state = 1`, and line 61, `state = -1`), i.e. that the PDG correctly merges both
     * branch definitions rather than only keeping the last one seen.
     */
    @Test
    fun ifElseMergesBranchDefinitions() {
        val tu = parse()
        assertNotNull(tu)

        val stateWriteThen =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 58 && it.name.localName == "state"
                }
                .first()
        val stateWriteElse =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 61 && it.name.localName == "state"
                }
                .first()
        val stateRead =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 77 && it.name.localName == "state"
                }
                .first()

        assertTrue(stateWriteThen in stateRead.prevDFG)
        assertTrue(stateWriteElse in stateRead.prevDFG)
    }

    /**
     * `complex_pdg.c:40-48` (`process`) is annotated with "Data dependence: cur itself depends on i
     * and items.". This verifies that the reads of `i` and `items` used on line 41 to compute
     * `&items[i]` have a data dependency on the loop counter declared on line 40 resp. the `items`
     * parameter of `process`.
     */
    @Test
    fun depLoopIndexItemParameter() {
        val tu = parse()
        assertNotNull(tu)

        val iDecl = tu.allChildren<Variable> { it.location?.region?.startLine == 40 }.first()
        val itemsParam = tu.allChildren<Parameter> { it.name.localName == "items" }.first()

        val iRead =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 41 && it.name.localName == "i"
                }
                .first()
        val itemsRead =
            tu.allChildren<Reference> {
                    it.location?.region?.startLine == 41 && it.name.localName == "items"
                }
                .first()

        assertTrue(iDecl in iRead.prevDFG)
        assertTrue(itemsParam in itemsRead.prevDFG)
    }

    /**
     * `complex_pdg.c:37` (`process`) notes "'alias' may indirectly modify sum.", and
     * `complex_pdg.c:80-87` notes "Alias-sensitive dependence: alias points to sum, so this
     * statement modifies the same abstract memory location as sum += / sum -= above.". This
     * verifies -- with [PointsToPass] enabled -- that the dereference `*alias` used on line 86
     * (`*alias += checksum;`) resolves to the exact same abstract memory location as the `sum`
     * variable declared on line 34.
     */
    @Test
    fun aliasSumSameMemoryLocation() {
        val tu = parseWithPointsTo()
        assertNotNull(tu)

        val sumDecl = tu.allChildren<Variable> { it.location?.region?.startLine == 34 }.first()
        val aliasDeref =
            tu.allChildren<PointerDereference> { it.location?.region?.startLine == 86 }.first()

        assertEquals(sumDecl.memoryAddresses.single(), aliasDeref.memoryAddresses.single())
    }

    /**
     * `complex_pdg.c:43-51` (`process`) notes "Control dependence: everything in this block depends
     * on cur->valid." for the guard `if (!cur->valid) continue;` (lines 50-51), calling it a
     * "non-local control-flow edge". `complex_pdg.c:93-94` uses `break` the same way. This verifies
     * -- with [PointsToPass] and [ControlDependenceGraphPass] enabled -- that (a) the `if
     * (cur->value > threshold)` check on line 57, which is only reached when the guard's `continue`
     * was *not* taken, is control-dependent on the guard condition `!cur->valid` on the false
     * branch, and (b) the `break` on line 94 is control-dependent on its own guarding condition
     * (`state < 0 && checksum > 100`, line 93) on the true branch.
     */
    @Test
    fun continueBreakGuards() {
        val tu = parseWithPointsTo()
        assertNotNull(tu)

        val guardCondition =
            tu.allChildren<IfElse> { it.location?.region?.startLine == 50 }.first().condition
        val thresholdCheck = tu.allChildren<IfElse> { it.location?.region?.startLine == 57 }.first()

        val guardEdge =
            listOfNotNull(
                    thresholdCheck,
                    thresholdCheck.condition,
                    thresholdCheck.conditionDeclaration,
                )
                .flatMap { it.prevCDGEdges }
                .firstOrNull { it.start == guardCondition }
        assertNotNull(
            guardEdge,
            "expected the code guarded by `if (!cur->valid) continue;` (line 57) to be " +
                "control-dependent on `!cur->valid` (line 50)",
        )
        assertTrue(false in guardEdge.branches)

        // The condition is a short-circuiting `state < 0 && checksum > 100`; depending on how the
        // EOG models short-circuit evaluation, the actual CDG branch point may be the whole `&&`
        // expression or its right-hand operand (the last one evaluated before branching) -- so we
        // accept either as the source of the dependency.
        val breakIfElse = tu.allChildren<IfElse> { it.location?.region?.startLine == 93 }.first()
        val breakConditionCandidates =
            listOfNotNull(
                breakIfElse.condition,
                tu.allChildren<BinaryOperator> {
                        it.location?.region?.startLine == 93 && it.operatorCode == ">"
                    }
                    .first(),
            )
        val breakStatement = tu.allChildren<Break> { it.location?.region?.startLine == 94 }.first()

        val breakEdge =
            breakStatement.prevCDGEdges.firstOrNull { it.start in breakConditionCandidates }
        assertNotNull(breakEdge, "expected `break` (line 94) to be control-dependent on its guard")
        assertTrue(true in breakEdge.branches)
    }

    /**
     * `dependenceGraphs/sgx_ra_get_msg3_trusted.cpp:133` is a `for` loop nested inside `if` (line
     * 129) whose body contains its own early-exit `if` (line 139, `goto joined_r0x0014db39;`),
     *      jumping out of the loop into the enclosing `if`'s `else` branch (line 156).
     *
     * Formally (Ferrante/Ottenstein/Warren postdominance-based control dependence), the loop's own
     * condition re-check *is* control-dependent on line 139: re-reaching it is exactly what
     * distinguishes 139's false outcome from its true one, and 139's own execution is in turn
     * control-dependent on the loop condition -- the two form the cycle that loop headers always
     * produce in a textbook control dependence graph. [ControlDependenceGraphPass] does not
     * reproduce that cycle: per [transfer], every dominator entry that originates inside a loop
     * body is dropped when the computation crosses that loop's back-edge (keeping only the loop's
     * own controlling condition), so dependencies on internal early-exits never reach the header.
     * This is a deliberate simplification -- a tree-shaped approximation of control dependence
     * rather than the full cyclic one -- not a claim that depending on 139 would be incorrect.
     *
     * This test pins down that approximation for a real-world regression: before the back-edge fix,
     * the loop incorrectly lost its (non-cyclic, structurally real) dependency on the enclosing
     * `if` (129) -- the condition that gates whether the loop runs at all -- while retaining the
     * cyclic-but-pruned-by-design dependency on 139. After the fix it must have (a) a dependency on
     * 129 and (b), consistent with this pass's chosen approximation, none on 139.
     */
    @Test
    fun forLoopDependsOnEnclosingIfNotInnerExit() {
        val tu = parseSgx()
        assertNotNull(tu)

        val outerIf = tu.allChildren<IfElse> { it.location?.region?.startLine == 129 }.first()
        val innerIf = tu.allChildren<IfElse> { it.location?.region?.startLine == 139 }.first()
        val forLoop = tu.allChildren<For> { it.location?.region?.startLine == 133 }.first()

        // The condition of a short-circuiting `if` may be split across several sub-operators in
        // the EOG, so accept the condition itself or any of its (sub-)expressions as the CDG
        // source, same as `continueBreakGuards` does above.
        fun conditionSubtree(ifElse: IfElse) =
            ifElse.condition?.let { setOf(it) + it.allChildren<Node> { true } } ?: emptySet()

        val outerCondition = conditionSubtree(outerIf)
        val innerCondition = conditionSubtree(innerIf)
        val forLoopDeps =
            (listOf<Node>(forLoop) + listOfNotNull(forLoop.condition)).flatMap { it.prevCDGEdges }

        val outerEdge = forLoopDeps.firstOrNull { it.start in outerCondition }
        assertNotNull(
            outerEdge,
            "expected the for-loop (line 133) to be control-dependent on the enclosing if (line 129)",
        )
        assertTrue(true in outerEdge.branches)

        val innerEdge = forLoopDeps.firstOrNull { it.start in innerCondition }
        assertNull(
            innerEdge,
            "expected no CDG edge from the inner early-exit if (line 139) to the for-loop (line " +
                "133): ControlDependenceGraphPass deliberately prunes dependencies that originate " +
                "inside a loop body when crossing the loop's back-edge, trading away the cyclic " +
                "dependency a textbook postdominance-based CDG would have here for a simpler, " +
                "tree-shaped approximation",
        )
    }
}
