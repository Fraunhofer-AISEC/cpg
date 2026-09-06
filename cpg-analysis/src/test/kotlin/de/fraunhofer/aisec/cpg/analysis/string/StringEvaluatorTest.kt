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
package de.fraunhofer.aisec.cpg.analysis.string

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.evaluation.ValueEvaluator
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.singleTranslationUnit
import de.fraunhofer.aisec.cpg.frontends.testFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.Return
import de.fraunhofer.aisec.cpg.graph.types.FunctionType.Companion.computeType
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.junit.jupiter.api.Timeout

class StringEvaluatorTest {

    private fun config(): TranslationConfiguration =
        TranslationConfiguration.builder().defaultPasses().registerLanguage<TestLanguage>().build()

    private fun build(init: LanguageFrontend<*, *>.(TranslationUnit) -> Unit) =
        testFrontend(config()).build {
            this.singleTranslationUnit("test.cpp") { tu -> this.init(tu) }
        }

    /** `x = "foo"; return x` should resolve to `Const("foo")`. */
    @Test
    fun testDirectConstant() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        block.statements += newDeclarationStatement { decl ->
                            newVariable("x", objectType("string"), holder = decl) {
                                it.initializer = newLiteral("foo", objectType("string"))
                            }
                        }
                        ret = newReturn { r -> r.returnValue = newReference("x") }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateString()
        assertEquals(const("foo"), pattern)
    }

    /** `return "a" + "b"` should resolve to `Const("ab")` via [concat]'s own normalisation. */
    @Test
    fun testConcatenation() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            r.returnValue =
                                newBinaryOperator("+") {
                                    it.lhs = newLiteral("a", objectType("string"))
                                    it.rhs = newLiteral("b", objectType("string"))
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateString()
        assertEquals(const("ab"), pattern)
    }

    /**
     * `if (cond) { x = "a" } else { x = "b" }; return x`. This is the case
     * `ValueEvaluator.handlePrevDFG` cannot handle (it aborts on more than one incoming DFG edge),
     * so we assert both that our evaluator succeeds, and that plain `ValueEvaluator` indeed cannot
     * resolve this to a single, correct constant.
     */
    @Test
    fun testBranchingJoin() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                newParameter("cond", objectType("bool"), holder = func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        block.statements += newDeclarationStatement { decl ->
                            newVariable("x", objectType("string"), holder = decl) {
                                it.isImplicitInitializerAllowed = true
                            }
                        }
                        block.statements += newIfElse { ifElse ->
                            ifElse.condition = newReference("cond")
                            ifElse.thenStatement =
                                newBlock(enterScope = true) { thenBlock ->
                                    thenBlock.statements +=
                                        newAssign(
                                            "=",
                                            listOf(newReference("x")),
                                            listOf(newLiteral("a", objectType("string"))),
                                        )
                                }
                            ifElse.elseStatement =
                                newBlock(enterScope = true) { elseBlock ->
                                    elseBlock.statements +=
                                        newAssign(
                                            "=",
                                            listOf(newReference("x")),
                                            listOf(newLiteral("b", objectType("string"))),
                                        )
                                }
                        }
                        ret = newReturn { r -> r.returnValue = newReference("x") }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateString()
        assertEquals(union(const("a"), const("b")), pattern)

        // ValueEvaluator must not be able to resolve this to the same, single, correct answer.
        val valueEvaluatorResult = ValueEvaluator().evaluate(ret.returnValue)
        assertFalse(
            valueEvaluatorResult == "a" || valueEvaluatorResult == "b",
            "ValueEvaluator is expected to give up on a branch-dependent value, but returned " +
                "\"$valueEvaluatorResult\"",
        )
    }

    /**
     * `x = ""; while (cond) { x = x + "a" }; return x`. This introduces a genuine cycle in the
     * backward DFG (the assignment inside the loop depends on its own previous value), which must
     * terminate via widening rather than looping forever, and must produce a sound
     * over-approximation (not a too-precise answer, and not a crash).
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    fun testLoopBuiltString() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                newParameter("cond", objectType("bool"), holder = func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        block.statements += newDeclarationStatement { decl ->
                            newVariable("x", objectType("string"), holder = decl) {
                                it.initializer = newLiteral("", objectType("string"))
                            }
                        }
                        block.statements +=
                            newWhile(enterScope = true) { w ->
                                w.condition = newReference("cond")
                                w.statement =
                                    newBlock(enterScope = true) { body ->
                                        body.statements +=
                                            newAssign(
                                                "=",
                                                listOf(newReference("x")),
                                                listOf(
                                                    newBinaryOperator("+") { op ->
                                                        op.lhs = newReference("x")
                                                        op.rhs =
                                                            newLiteral("a", objectType("string"))
                                                    }
                                                ),
                                            )
                                    }
                            }
                        ret = newReturn { r -> r.returnValue = newReference("x") }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateString()
        // Termination (checked by the surrounding @Timeout) plus soundness: the result must not
        // pretend to be a single, fully-known value, and must still admit the character 'a' that
        // every loop iteration introduces.
        assertFalse(pattern.isFullyKnown, "a loop-built string must not be fully known: $pattern")
        assertTrue(
            charSetContains(charSetOf(pattern), CharSet.Chars(setOf('a'))),
            "the over-approximation must still admit 'a': $pattern",
        )
    }

    /**
     * `fun helper() = "x"` called from `fun caller() { return helper() }` - this exercises D6
     * (interprocedural by default) via a genuine cross-function DFG traversal, not a
     * single-function fallback.
     */
    @Test
    fun testInterprocedural() {
        lateinit var ret: Return
        build { tu ->
            newFunction("helper", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        block.statements += newReturn { r ->
                            r.returnValue = newLiteral("x", objectType("string"))
                        }
                    }
            }
            newFunction("caller", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r -> r.returnValue = newCall(newReference("helper")) }
                        block.statements += ret
                    }
            }
        }

        val call = ret.returnValue
        assertIs<Call>(call)
        // Make sure call resolution actually happened, i.e. this test exercises real cross-function
        // resolution and not just an evaluator fallback.
        assertTrue(call.invokes.isNotEmpty(), "the call to \"helper\" must have been resolved")

        val pattern = call.evaluateString()
        assertEquals(const("x"), pattern)
    }

    /** A parameter of a function that is never called anywhere becomes `Unknown(PARAMETER)`. */
    @Test
    fun testParameterWithNoCaller() {
        lateinit var ret: Return
        build { tu ->
            newFunction("helper", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                newParameter("p", objectType("string"), holder = func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r -> r.returnValue = newReference("p") }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateString()
        assertIs<StringPattern.Unknown>(pattern)
        assertEquals(StringPattern.Reason.PARAMETER, pattern.reason)
    }

    /**
     * A parameter called with two different constant arguments from two call sites resolves to the
     * [union] of both call-site values.
     */
    @Test
    fun testParameterWithMultipleCallers() {
        lateinit var ret: Return
        build { tu ->
            newFunction("helper", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                newParameter("p", objectType("string"), holder = func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r -> r.returnValue = newReference("p") }
                        block.statements += ret
                    }
            }
            newFunction("caller1", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        block.statements +=
                            newCall(newReference("helper")) {
                                it.arguments += newLiteral("a", objectType("string"))
                            }
                    }
            }
            newFunction("caller2", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        block.statements +=
                            newCall(newReference("helper")) {
                                it.arguments += newLiteral("b", objectType("string"))
                            }
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateString()
        assertEquals(union(const("a"), const("b")), pattern)
    }

    /**
     * A chain of `diamondCount` sequential if/else diamonds, each reassigning the same variable via
     * concatenation (`x = x + "0"` / `x = x + "1"`), so that diamond `i` depends on the result of
     * diamond `i - 1`, not just on a leaf. Without memoization (see
     * [StringEvaluator.evaluateInternal]'s cache), evaluating the final `x` re-descends into every
     * shared predecessor on every branch of every join, causing genuine `2^diamondCount`
     * re-evaluation - this used to take seconds at `diamondCount = 18`; with memoization it must
     * complete in a small fraction of a second, and the result must still be sound: the pattern
     * must not claim to be fully known, and must still admit both `'0'` and `'1'`, the characters
     * every branch may introduce.
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    fun testManySequentialJoinsIsFast() {
        lateinit var ret: Return
        val diamondCount = 18
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                repeat(diamondCount) { i ->
                    newParameter("cond$i", objectType("bool"), holder = func)
                }
                func.body =
                    newBlock(enterScope = true) { block ->
                        block.statements += newDeclarationStatement { decl ->
                            newVariable("x", objectType("string"), holder = decl) {
                                it.initializer = newLiteral("", objectType("string"))
                            }
                        }
                        repeat(diamondCount) { i ->
                            block.statements += newIfElse { ifElse ->
                                ifElse.condition = newReference("cond$i")
                                ifElse.thenStatement =
                                    newBlock(enterScope = true) { thenBlock ->
                                        thenBlock.statements +=
                                            newAssign(
                                                "=",
                                                listOf(newReference("x")),
                                                listOf(
                                                    newBinaryOperator("+") { op ->
                                                        op.lhs = newReference("x")
                                                        op.rhs =
                                                            newLiteral("0", objectType("string"))
                                                    }
                                                ),
                                            )
                                    }
                                ifElse.elseStatement =
                                    newBlock(enterScope = true) { elseBlock ->
                                        elseBlock.statements +=
                                            newAssign(
                                                "=",
                                                listOf(newReference("x")),
                                                listOf(
                                                    newBinaryOperator("+") { op ->
                                                        op.lhs = newReference("x")
                                                        op.rhs =
                                                            newLiteral("1", objectType("string"))
                                                    }
                                                ),
                                            )
                                    }
                            }
                        }
                        ret = newReturn { r -> r.returnValue = newReference("x") }
                        block.statements += ret
                    }
            }
        }

        lateinit var pattern: StringPattern
        val elapsedMs = measureTimeMillis { pattern = ret.returnValue!!.evaluateString() }

        assertTrue(
            elapsedMs < 1000,
            "evaluating $diamondCount sequential joins took ${elapsedMs}ms, expected well " +
                "under 1000ms with memoization",
        )
        assertFalse(
            pattern.isFullyKnown,
            "a join of $diamondCount diamonds must not be fully known: $pattern",
        )
        assertTrue(
            charSetContains(charSetOf(pattern), CharSet.Chars(setOf('0'))) &&
                charSetContains(charSetOf(pattern), CharSet.Chars(setOf('1'))),
            "the over-approximation must still admit both '0' and '1': $pattern",
        )
    }

    /**
     * A chain of 200 functions, each calling the next (`f0` -> `f1` -> ... -> `f199`), where only
     * `f199` returns a known literal. With the default `maxCallDepth = 10`,
     * `Interprocedural.followEdge` cuts off the interprocedural edge well before reaching `f199`,
     * so [StringEvaluator.followPredecessors] must recognise the resulting empty predecessor set as
     * budget exhaustion (`Unknown(reason = BUDGET_EXCEEDED)`, with a recorded
     * `SoundnessAssumption`), not as a generic unsupported/leaf case - this is the case that used
     * to be missed because the old proactive check only fired `if (node is Parameter)`.
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    fun testDeepCallChainReportsBudgetExceeded() {
        lateinit var topCall: Call
        val chainDepth = 200
        build { tu ->
            for (i in 0 until chainDepth) {
                newFunction("f$i", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(objectType("string"))
                    func.type = computeType(func)
                    func.body =
                        newBlock(enterScope = true) { block ->
                            block.statements += newReturn { r ->
                                r.returnValue =
                                    if (i == chainDepth - 1) {
                                        newLiteral("leaf", objectType("string"))
                                    } else {
                                        newCall(newReference("f${i + 1}"))
                                    }
                            }
                        }
                }
            }
            newFunction("entry", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        block.statements += newReturn { r ->
                            topCall = newCall(newReference("f0"))
                            r.returnValue = topCall
                        }
                    }
            }
        }

        assertTrue(topCall.invokes.isNotEmpty(), "the call to f0 must have been resolved")
        assertTrue(topCall.assumptions.isEmpty(), "no assumption should exist before evaluation")

        val pattern = topCall.evaluateString()

        assertIs<StringPattern.Unknown>(pattern)
        assertEquals(StringPattern.Reason.BUDGET_EXCEEDED, pattern.reason)
        assertTrue(
            topCall.assumptions.isNotEmpty(),
            "budget exhaustion must record a SoundnessAssumption on the root node",
        )
    }

    /**
     * `"x" + 5`: a non-string literal in a concatenation is stringified rather than becoming
     * `Unknown`, matching `ValueEvaluator.handlePlus`'s treatment of `String + Number`.
     */
    @Test
    fun testNonStringLiteralFallthrough() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            r.returnValue =
                                newBinaryOperator("+") {
                                    it.lhs = newLiteral("x", objectType("string"))
                                    it.rhs = newLiteral(5, objectType("int"))
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateString()
        assertEquals(const("x5"), pattern)
    }

    private fun buildSimpleConstant(): Return {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        block.statements += newDeclarationStatement { decl ->
                            newVariable("x", objectType("string"), holder = decl) {
                                it.initializer = newLiteral("foo", objectType("string"))
                            }
                        }
                        ret = newReturn { r -> r.returnValue = newReference("x") }
                        block.statements += ret
                    }
            }
        }
        return ret
    }

    private fun buildBranchingJoinFixture(): Return {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                newParameter("cond", objectType("bool"), holder = func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        block.statements += newDeclarationStatement { decl ->
                            newVariable("x", objectType("string"), holder = decl) {
                                it.isImplicitInitializerAllowed = true
                            }
                        }
                        block.statements += newIfElse { ifElse ->
                            ifElse.condition = newReference("cond")
                            ifElse.thenStatement =
                                newBlock(enterScope = true) { thenBlock ->
                                    thenBlock.statements +=
                                        newAssign(
                                            "=",
                                            listOf(newReference("x")),
                                            listOf(newLiteral("a", objectType("string"))),
                                        )
                                }
                            ifElse.elseStatement =
                                newBlock(enterScope = true) { elseBlock ->
                                    elseBlock.statements +=
                                        newAssign(
                                            "=",
                                            listOf(newReference("x")),
                                            listOf(newLiteral("b", objectType("string"))),
                                        )
                                }
                        }
                        ret = newReturn { r -> r.returnValue = newReference("x") }
                        block.statements += ret
                    }
            }
        }
        return ret
    }

    private fun buildLoopBuiltStringFixture(): Return {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                newParameter("cond", objectType("bool"), holder = func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        block.statements += newDeclarationStatement { decl ->
                            newVariable("x", objectType("string"), holder = decl) {
                                it.initializer = newLiteral("", objectType("string"))
                            }
                        }
                        block.statements +=
                            newWhile(enterScope = true) { w ->
                                w.condition = newReference("cond")
                                w.statement =
                                    newBlock(enterScope = true) { body ->
                                        body.statements +=
                                            newAssign(
                                                "=",
                                                listOf(newReference("x")),
                                                listOf(
                                                    newBinaryOperator("+") { op ->
                                                        op.lhs = newReference("x")
                                                        op.rhs =
                                                            newLiteral("a", objectType("string"))
                                                    }
                                                ),
                                            )
                                    }
                            }
                        ret = newReturn { r -> r.returnValue = newReference("x") }
                        block.statements += ret
                    }
            }
        }
        return ret
    }

    private fun buildDeepCallChainFixture(chainDepth: Int = 200): Call {
        lateinit var topCall: Call
        build { tu ->
            for (i in 0 until chainDepth) {
                newFunction("f$i", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(objectType("string"))
                    func.type = computeType(func)
                    func.body =
                        newBlock(enterScope = true) { block ->
                            block.statements += newReturn { r ->
                                r.returnValue =
                                    if (i == chainDepth - 1) {
                                        newLiteral("leaf", objectType("string"))
                                    } else {
                                        newCall(newReference("f${i + 1}"))
                                    }
                            }
                        }
                }
            }
            newFunction("entry", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        block.statements += newReturn { r ->
                            topCall = newCall(newReference("f0"))
                            r.returnValue = topCall
                        }
                    }
            }
        }
        return topCall
    }

    /**
     * Tests that [StringEvaluator], now that it extends [ValueEvaluator], works correctly through
     * the generic `Node.evaluate(evaluator)` entry point, producing the identical result to
     * [evaluateString] for a simple constant, a branching-join case, and the loop-built-string
     * cycle case.
     */
    @Test
    fun testGenericEvaluateMatchesEvaluateString() {
        val simpleConstant = buildSimpleConstant()
        assertEquals(
            simpleConstant.returnValue!!.evaluateString(),
            simpleConstant.returnValue!!.evaluate(StringEvaluator()),
        )

        val branchingJoin = buildBranchingJoinFixture()
        assertEquals(
            branchingJoin.returnValue!!.evaluateString(),
            branchingJoin.returnValue!!.evaluate(StringEvaluator()),
        )

        val loop = buildLoopBuiltStringFixture()
        assertEquals(
            loop.returnValue!!.evaluateString(),
            loop.returnValue!!.evaluate(StringEvaluator()),
        )
    }

    /**
     * Calling `.evaluate(...)` twice on the same [StringEvaluator] instance, on two different,
     * unrelated fixtures, must not leak cache/path/assumed/cyclic state from the first call into
     * the second - i.e. [StringEvaluator.evaluateInternal] must fully reset this state, since it is
     * the only place both `evaluate` and `evaluateAs` funnel through.
     */
    @Test
    fun testRepeatedCallsOnSameInstanceDoNotLeakState() {
        val evaluator = StringEvaluator()
        val loop = buildLoopBuiltStringFixture()
        val simpleConstant = buildSimpleConstant()

        val loopResult = evaluator.evaluate(loop.returnValue)
        assertFalse(loopResult.isFullyKnown)

        val constantResult = evaluator.evaluate(simpleConstant.returnValue)
        assertEquals(const("foo"), constantResult)

        // And in the other order, to rule out ordering-dependent leakage.
        val evaluator2 = StringEvaluator()
        val constantResult2 = evaluator2.evaluate(simpleConstant.returnValue)
        assertEquals(const("foo"), constantResult2)
        val loopResult2 = evaluator2.evaluate(loop.returnValue)
        assertFalse(loopResult2.isFullyKnown)
    }

    /**
     * The single most important test in this suite: `ValueEvaluator.evaluateAs<T>(node)` is the
     * inherited, non-overridable `inline` function from [ValueEvaluator] - it calls
     * `evaluateInternal` directly, bypassing `StringEvaluator.evaluate` entirely. If the
     * state-resetting logic had been placed only in `evaluate` (the naive design), this test would
     * fail: running the loop-cycle fixture first would leave `cyclic`/`assumed` non-empty (or the
     * cache stale), and the second, unrelated, simple-constant fixture would then read back a
     * corrupted result via the shared thread-local state. Because the reset lives in
     * `evaluateInternal` instead, both calls are correct.
     */
    @Test
    fun testEvaluateAsDoesNotLeakStateAcrossCalls() {
        val evaluator = StringEvaluator()
        val loop = buildLoopBuiltStringFixture()
        val simpleConstant = buildSimpleConstant()

        val loopResult = evaluator.evaluateAs<StringPattern>(loop.returnValue)
        assertFalse(loopResult?.isFullyKnown ?: true)

        val constantResult = evaluator.evaluateAs<StringPattern>(simpleConstant.returnValue)
        assertEquals(const("foo"), constantResult)
    }

    /**
     * Confirms that budget-exceeded assumptions still attach to the correct root node when
     * triggered via the `evaluate(Any?, useCache)`/`evaluateAs` entry points, not just the original
     * `evaluateString()`/`StringEvaluator.evaluate(Node)` path.
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    fun testBudgetExceededViaGenericEvaluateEntryPoints() {
        val topCall = buildDeepCallChainFixture()
        assertTrue(topCall.assumptions.isEmpty())

        val evaluator = StringEvaluator()
        val pattern = topCall.evaluate(evaluator) as? StringPattern
        assertIs<StringPattern.Unknown>(pattern)
        assertEquals(StringPattern.Reason.BUDGET_EXCEEDED, pattern.reason)
        assertTrue(topCall.assumptions.isNotEmpty())

        // And via evaluateAs, on a fresh instance.
        val topCall2 = buildDeepCallChainFixture()
        val pattern2 = StringEvaluator().evaluateAs<StringPattern>(topCall2)
        assertIs<StringPattern.Unknown>(pattern2)
        assertEquals(StringPattern.Reason.BUDGET_EXCEEDED, pattern2.reason)
        assertTrue(topCall2.assumptions.isNotEmpty())
    }

    /**
     * A [StringOperationHandler] that recognises calls to a function named `"shared"`, counts every
     * time it is actually invoked (i.e. every time the `shared()` call node is genuinely
     * (re-)computed, as opposed to served from a cache), and returns `Const("shared")`. Used as the
     * "expensive, observable" shared sub-node `C` in the cross-query-cache tests below.
     */
    private class CountingSharedCallHandler(val counter: AtomicInteger) : StringOperationHandler {
        override fun handleCall(call: Call, evaluate: (Node) -> StringPattern): StringPattern? {
            if (call.name.localName != "shared") return null
            counter.incrementAndGet()
            return const("shared")
        }

        override fun handleBinaryOperator(
            op: BinaryOperator,
            evaluate: (Node) -> StringPattern,
        ): StringPattern? = null
    }

    /**
     * `c = shared(); a = c + "a"; b = c + "b"` (all in one function, so `a`'s and `b`'s backward
     * paths to the `shared()` call node share the same, empty call/index stack, i.e. the same
     * [ContextKey] once reached). Returns the two *different* top-level query targets (`a`'s and
     * `b`'s initializers) that both backward-reach the shared `shared()` call node `C`.
     */
    private fun buildSharedSubgraphFixture(): Pair<BinaryOperator, BinaryOperator> {
        lateinit var aExpr: BinaryOperator
        lateinit var bExpr: BinaryOperator
        build { tu ->
            newFunction("shared", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        block.statements += newReturn { r ->
                            r.returnValue = newLiteral("c", objectType("string"))
                        }
                    }
            }
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        block.statements += newDeclarationStatement { decl ->
                            newVariable("c", objectType("string"), holder = decl) {
                                it.initializer = newCall(newReference("shared"))
                            }
                        }
                        block.statements += newDeclarationStatement { decl ->
                            newVariable("a", objectType("string"), holder = decl) {
                                aExpr =
                                    newBinaryOperator("+") { op ->
                                        op.lhs = newReference("c")
                                        op.rhs = newLiteral("a", objectType("string"))
                                    }
                                it.initializer = aExpr
                            }
                        }
                        block.statements += newDeclarationStatement { decl ->
                            newVariable("b", objectType("string"), holder = decl) {
                                bExpr =
                                    newBinaryOperator("+") { op ->
                                        op.lhs = newReference("c")
                                        op.rhs = newLiteral("b", objectType("string"))
                                    }
                                it.initializer = bExpr
                            }
                        }
                    }
            }
        }
        return aExpr to bExpr
    }

    /**
     * With `crossQueryCache = true`, evaluating two *separate* top-level targets (`a`, `b`) that
     * share a common backward-reachable sub-node (the `shared()` call) must only actually compute
     * that shared sub-node once across both calls - the second call must be served from the
     * persistent cache.
     */
    @Test
    fun testCrossQueryCacheReusesSharedSubNode() {
        val (aExpr, bExpr) = buildSharedSubgraphFixture()
        val counter = AtomicInteger(0)
        val evaluator =
            StringEvaluator(
                operationHandlers = listOf(CountingSharedCallHandler(counter)),
                crossQueryCache = true,
            )

        val resultA = evaluator.evaluate(aExpr)
        val resultB = evaluator.evaluate(bExpr)

        assertEquals(const("shareda"), resultA)
        assertEquals(const("sharedb"), resultB)
        assertEquals(
            1,
            counter.get(),
            "the shared sub-node must only be computed once across both top-level calls " +
                "when crossQueryCache is enabled",
        )
    }

    /**
     * The contrast/regression-guard for [testCrossQueryCacheReusesSharedSubNode]: with the default
     * `crossQueryCache = false`, the exact same fixture must recompute the shared sub-node once per
     * top-level call, confirming the default really does not share state across separate [evaluate]
     * calls.
     */
    @Test
    fun testDefaultDoesNotReuseAcrossQueries() {
        val (aExpr, bExpr) = buildSharedSubgraphFixture()
        val counter = AtomicInteger(0)
        val evaluator =
            StringEvaluator(operationHandlers = listOf(CountingSharedCallHandler(counter)))

        val resultA = evaluator.evaluate(aExpr)
        val resultB = evaluator.evaluate(bExpr)

        assertEquals(const("shareda"), resultA)
        assertEquals(const("sharedb"), resultB)
        assertEquals(
            2,
            counter.get(),
            "without crossQueryCache, the shared sub-node must be recomputed for every " +
                "top-level call",
        )
    }

    /**
     * Correctness under reuse: the actual [StringPattern] results with `crossQueryCache = true`
     * must be identical to what a fresh, non-cross-query evaluator produces for the same nodes -
     * the cache must only affect performance, never the answer.
     */
    @Test
    fun testCrossQueryCacheDoesNotChangeResults() {
        val (aExpr, bExpr) = buildSharedSubgraphFixture()

        val cachedEvaluator = StringEvaluator(crossQueryCache = true)
        val cachedA = cachedEvaluator.evaluate(aExpr)
        val cachedB = cachedEvaluator.evaluate(bExpr)

        val uncachedEvaluator = StringEvaluator(crossQueryCache = false)
        val uncachedA = uncachedEvaluator.evaluate(aExpr)
        val uncachedB = uncachedEvaluator.evaluate(bExpr)

        assertEquals(uncachedA, cachedA)
        assertEquals(uncachedB, cachedB)

        // Also cross-check against the loop-built-string and branching-join fixtures, to make sure
        // the reentrancy-hazard-avoiding "compute outside, insert after" write pattern does not
        // subtly change results for non-trivial, non-shared fixtures either.
        val loop = buildLoopBuiltStringFixture()
        assertEquals(
            StringEvaluator(crossQueryCache = false).evaluate(loop.returnValue),
            StringEvaluator(crossQueryCache = true).evaluate(loop.returnValue),
        )
        val branchingJoin = buildBranchingJoinFixture()
        assertEquals(
            StringEvaluator(crossQueryCache = false).evaluate(branchingJoin.returnValue),
            StringEvaluator(crossQueryCache = true).evaluate(branchingJoin.returnValue),
        )
    }

    /**
     * The reentrancy stress test: [StringEvaluator] deliberately re-visits the same node while it
     * is still being computed further up the current call stack (the loop-built-string cycle). With
     * `crossQueryCache = true`, the result-memoization cache is a shared, cross-thread
     * [de.fraunhofer.aisec.cpg.helpers.functional.ConcurrentIdentityHashMap] rather than a
     * `ThreadLocal` one - this test confirms the "compute outside, insert after" write pattern (see
     * `StringEvaluator.storeResult`'s KDoc) avoids the `ConcurrentHashMap` recursive-update hazard
     * that a naive `computeIfAbsent`-based cache would risk here, by confirming this still
     * terminates (via `@Timeout`) and produces the same sound, non-fully-known result as the
     * `crossQueryCache = false` case - both for a single evaluation, and for many concurrent
     * evaluations of the *same* cyclic node (plus unrelated fixtures) hammering one shared instance
     * from multiple threads at once.
     */
    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    fun testCrossQueryCacheReentrancyDoesNotDeadlock() {
        val loop = buildLoopBuiltStringFixture()
        val expected = StringEvaluator(crossQueryCache = false).evaluate(loop.returnValue)
        assertFalse(expected.isFullyKnown)

        // Single-threaded: does it still terminate and agree with the non-cross-query result?
        val singleThreaded = StringEvaluator(crossQueryCache = true).evaluate(loop.returnValue)
        assertEquals(expected, singleThreaded)

        // Multi-threaded: many threads hammering one shared crossQueryCache=true instance,
        // concurrently evaluating the same cyclic node and an unrelated simple-constant fixture -
        // exactly the scenario the "compute outside, insert after" design is meant to survive.
        val sharedEvaluator = StringEvaluator(crossQueryCache = true)
        val simpleConstant = buildSimpleConstant()
        val threadCount = 16
        val iterationsPerThread = 20
        val executor = Executors.newFixedThreadPool(threadCount)
        val startLatch = CountDownLatch(1)
        val failures = java.util.Collections.synchronizedList(mutableListOf<Throwable>())
        try {
            val futures =
                (0 until threadCount).map { threadIndex ->
                    executor.submit {
                        startLatch.await()
                        repeat(iterationsPerThread) {
                            try {
                                if (threadIndex % 2 == 0) {
                                    val result = sharedEvaluator.evaluate(loop.returnValue)
                                    assertEquals(expected, result)
                                } else {
                                    val result =
                                        sharedEvaluator.evaluate(simpleConstant.returnValue)
                                    assertEquals(const("foo"), result)
                                }
                            } catch (t: Throwable) {
                                failures.add(t)
                            }
                        }
                    }
                }
            startLatch.countDown()
            futures.forEach { it.get(15, TimeUnit.SECONDS) }
        } finally {
            executor.shutdown()
        }
        assertTrue(failures.isEmpty(), "concurrent evaluation raised: $failures")
    }
}
