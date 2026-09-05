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
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.Return
import de.fraunhofer.aisec.cpg.graph.types.FunctionType.Companion.computeType
import java.util.concurrent.TimeUnit
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
}
