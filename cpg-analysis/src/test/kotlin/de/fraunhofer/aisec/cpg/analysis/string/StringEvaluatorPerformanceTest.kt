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
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.singleTranslationUnit
import de.fraunhofer.aisec.cpg.frontends.testFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.expressions.Return
import de.fraunhofer.aisec.cpg.graph.types.FunctionType.Companion.computeType
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertTrue
import org.junit.jupiter.api.Timeout

/**
 * Phase 6 performance benchmark for [StringEvaluator].
 *
 * The default budget parameters (see [StringEvaluatorConfig]) are:
 * - `scope = Interprocedural(maxCallDepth = 10, maxSteps = 5_000)`
 * - `maxTermSize = 64`, `maxTermDepth = 16`, `maxUnionSize = 16`
 *
 * [StringEvaluatorTest.testManySequentialJoinsIsFast] and
 * [StringEvaluatorTest.testDeepCallChainReportsBudgetExceeded] already cover two synthetic,
 * narrowly-targeted regression cases (respectively: the exponential-blowup fix via memoization on a
 * chain of 18 sequential diamonds, and budget-exhaustion reporting on a chain of 200 nested calls).
 * This class adds a single larger, more realistic "big fixture" benchmark that combines several
 * constructs a real function might mix - branches, a bounded loop, and several interprocedural
 * calls - in one function, checked against a generous but real wall-clock budget. The `@Timeout` is
 * a CI safety net; the `measureTimeMillis` assertion below is the actual regression guard, with a
 * comment flagging (not failing the build on) timings that get close to a concerning threshold.
 */
class StringEvaluatorPerformanceTest {

    private fun config(): TranslationConfiguration =
        TranslationConfiguration.builder().defaultPasses().registerLanguage<TestLanguage>().build()

    private fun build(init: LanguageFrontend<*, *>.(TranslationUnit) -> Unit) =
        testFrontend(config()).build {
            this.singleTranslationUnit("test.cpp") { tu -> this.init(tu) }
        }

    /**
     * A "big fixture": a chain of `helperCount` helper functions (`helper0` -> `helper1` -> ... ->
     * `helperN`, each concatenating a literal onto its callee's result, well within the default
     * `maxCallDepth = 10`), called from a `main` that also has `branchCount` sequential if/else
     * diamonds (each reassigning a local variable) and a bounded loop, mixing every construct the
     * evaluator supports in one realistic-shaped function. Asserts this completes comfortably
     * within the default budget's implied cost and a generous wall-clock bound.
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    fun testLargeMixedFixtureIsFast() {
        lateinit var ret: Return
        val helperCount = 8
        val branchCount = 12

        build { tu ->
            for (i in 0 until helperCount) {
                newFunction("helper$i", holder = tu, enterScope = true) { func ->
                    func.returnTypes = listOf(objectType("string"))
                    func.type = computeType(func)
                    func.body =
                        newBlock(enterScope = true) { block ->
                            block.statements += newReturn { r ->
                                r.returnValue =
                                    if (i == helperCount - 1) {
                                        newLiteral("leaf", objectType("string"))
                                    } else {
                                        newBinaryOperator("+") { op ->
                                            op.lhs = newLiteral("h$i-", objectType("string"))
                                            op.rhs = newCall(newReference("helper${i + 1}"))
                                        }
                                    }
                            }
                        }
                }
            }

            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                repeat(branchCount) { i ->
                    newParameter("cond$i", objectType("bool"), holder = func)
                }
                newParameter("loopCond", objectType("bool"), holder = func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        block.statements += newDeclarationStatement { decl ->
                            newVariable("x", objectType("string"), holder = decl) {
                                it.initializer = newCall(newReference("helper0"))
                            }
                        }
                        repeat(branchCount) { i ->
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
                        block.statements +=
                            newWhile(enterScope = true) { w ->
                                w.condition = newReference("loopCond")
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
                                                            newLiteral("L", objectType("string"))
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

        lateinit var pattern: StringPattern
        val elapsedMs = measureTimeMillis { pattern = ret.returnValue!!.evaluateString() }

        // Generous bound: this is a regression guard against the evaluator's cost growing
        // unexpectedly, not a tight performance requirement. Flag (without failing) if we are
        // getting close to a concerning fraction of it.
        val budgetMs = 10_000L
        if (elapsedMs > budgetMs / 4) {
            println(
                "StringEvaluatorPerformanceTest.testLargeMixedFixtureIsFast took ${elapsedMs}ms, " +
                    "over a quarter of the ${budgetMs}ms regression budget - consider " +
                    "investigating for a performance regression."
            )
        }
        assertTrue(
            elapsedMs < budgetMs,
            "evaluating the large mixed fixture took ${elapsedMs}ms, expected well under " +
                "${budgetMs}ms",
        )

        // Soundness sanity check alongside the timing: the loop makes this not fully known, and it
        // must still admit the characters every construct may introduce.
        assertTrue(
            charSetContains(charSetOf(pattern), CharSet.Chars(setOf('L'))),
            "the result must still admit 'L' from the loop: $pattern",
        )
    }
}
