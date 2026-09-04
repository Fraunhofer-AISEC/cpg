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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Timeout

/**
 * Tests for [AbstractStringEvaluator], the flow-sensitive (Phase 5) variant of the string analysis.
 * Modelled on [StringEvaluatorTest] (Phase 2, the demand-driven backward evaluator), so that the
 * same fixtures can be evaluated with both evaluators for a direct precision/soundness comparison -
 * see [testLoopBuiltStringComparedToBackwardEvaluator].
 */
class AbstractStringEvaluatorTest {

    private fun config(): TranslationConfiguration =
        TranslationConfiguration.builder().defaultPasses().registerLanguage<TestLanguage>().build()

    private fun build(init: LanguageFrontend<*, *>.(TranslationUnit) -> Unit) =
        testFrontend(config()).build {
            this.singleTranslationUnit("test.cpp") { tu -> this.init(tu) }
        }

    private fun evaluate(node: de.fraunhofer.aisec.cpg.graph.Node): StringPattern =
        AbstractStringEvaluator().evaluate(node, StringValue::class)

    /** `x = "a"; x = x + "b"; return x` should resolve to `Const("ab")`. */
    @Test
    fun testStraightLineConcatenation() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        block.statements += newDeclarationStatement { decl ->
                            newVariable("x", objectType("string"), holder = decl) {
                                it.initializer = newLiteral("a", objectType("string"))
                            }
                        }
                        block.statements +=
                            newAssign(
                                "=",
                                listOf(newReference("x")),
                                listOf(
                                    newBinaryOperator("+") { op ->
                                        op.lhs = newReference("x")
                                        op.rhs = newLiteral("b", objectType("string"))
                                    }
                                ),
                            )
                        ret = newReturn { r -> r.returnValue = newReference("x") }
                        block.statements += ret
                    }
            }
        }

        val pattern = evaluate(ret.returnValue!!)
        assertEquals("ab", pattern.asConstantOrNull(), "expected a fully-known \"ab\": $pattern")
    }

    /**
     * `x = <unknown>; if (cond) { x = "a" } else { x = "b" }; return x` should resolve to
     * `Union(Const("a"), Const("b"))` after the flow-sensitive `lub` at the merge point.
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

        val pattern = evaluate(ret.returnValue!!)
        assertEquals(union(const("a"), const("b")), pattern)
    }

    /**
     * `x = ""; while (cond) { x = x + "a" }; return x`. Exercises the loop-head widening plumbed
     * through [de.fraunhofer.aisec.cpg.helpers.functional.Lattice.iterateEOG] via
     * [de.fraunhofer.aisec.cpg.helpers.functional.Lattice.Strategy.WIDENING] - this is the case the
     * design doc calls out as handled badly by the backward (Phase 2) evaluator, and the actual
     * motivation for this flow-sensitive variant. The [Timeout] is a CI safety net; termination is
     * also checked directly via [measureTimeMillis] below.
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

        lateinit var pattern: StringPattern
        val elapsedMs = measureTimeMillis { pattern = evaluate(ret.returnValue!!) }

        assertTrue(
            elapsedMs < 2000,
            "the loop-built-string fixpoint took ${elapsedMs}ms, expected well under 2000ms",
        )
        assertFalse(pattern.isFullyKnown, "a loop-built string must not be fully known: $pattern")
        assertTrue(
            charSetContains(charSetOf(pattern), CharSet.Chars(setOf('a'))),
            "the over-approximation must still admit 'a': $pattern",
        )
    }

    /**
     * Compares the flow-sensitive [AbstractStringEvaluator] (this phase) against the backward,
     * demand-driven [StringEvaluator] (Phase 2,
     * [de.fraunhofer.aisec.cpg.analysis.string.evaluateString]) on the same loop-built-string
     * fixture. Both must be sound (never fully known, always admitting 'a'); the two evaluators are
     * not required to reach the same precision - whichever one widens "more aggressively" for this
     * particular shape may report a coarser regex, and that is fine as long as it is still sound.
     * This is asserted, not merely printed, so a soundness regression in either evaluator fails the
     * test; the *relative* precision is documented in a comment, not asserted, since the design doc
     * does not require one evaluator to dominate the other.
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    fun testLoopBuiltStringComparedToBackwardEvaluator() {
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

        val flowSensitive = evaluate(ret.returnValue!!)
        val backward = ret.returnValue!!.evaluateString()

        // Soundness for both: neither may claim full knowledge, both must still admit 'a'.
        assertFalse(
            flowSensitive.isFullyKnown,
            "AbstractStringEvaluator: a loop-built string must not be fully known: $flowSensitive",
        )
        assertTrue(
            charSetContains(charSetOf(flowSensitive), CharSet.Chars(setOf('a'))),
            "AbstractStringEvaluator: the over-approximation must still admit 'a': $flowSensitive",
        )
        assertFalse(
            backward.isFullyKnown,
            "StringEvaluator: a loop-built string must not be fully known: $backward",
        )
        assertTrue(
            charSetContains(charSetOf(backward), CharSet.Chars(setOf('a'))),
            "StringEvaluator: the over-approximation must still admit 'a': $backward",
        )
        // Precision comparison (informational, not asserted): Phase 2's hand-rolled per-cycle
        // fixpoint widens exactly once it detects the self-referential cycle through the backward
        // DFG, whereas Phase 5 widens at the EOG loop head on every widening round of the shared
        // `iterateEOG` driver. Empirically (see the task report) both collapse to a
        // `charSet = {'a'}`-admitting `Unknown`/`Concat(Const(""), Star(...))`-shaped result for
        // this fixture, i.e. comparable precision for this specific case - neither dominates the
        // other in general, so no equality/ordering is asserted here.
    }
}
