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
import de.fraunhofer.aisec.cpg.graph.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.expressions.Return
import de.fraunhofer.aisec.cpg.graph.types.FunctionType.Companion.computeType
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Phase 6 differential soundness net: for every fixture below where [ValueEvaluator] resolves a
 * concrete [String], [StringEvaluator]'s [StringPattern] for the exact same node must be able to
 * produce that string, i.e. its [toRegex] must match it. This is cheap (both evaluators already
 * exist) and catches any regression where the pattern-producing evaluator silently diverges from
 * the concrete one on a case both can handle.
 *
 * A second group of fixtures documents the complementary, and arguably more interesting, property:
 * cases where [ValueEvaluator] gives up (branching joins, loops) but [StringEvaluator] still
 * produces a sound, non-trivial result. These are not part of the strict differential check (there
 * is no concrete `ValueEvaluator` answer to compare against) but are kept as permanent regression
 * documentation of the value this component adds over [ValueEvaluator].
 */
class DifferentialSoundnessTest {

    private fun config(): TranslationConfiguration =
        TranslationConfiguration.builder().defaultPasses().registerLanguage<TestLanguage>().build()

    private fun build(init: LanguageFrontend<*, *>.(TranslationUnit) -> Unit) =
        testFrontend(config()).build {
            this.singleTranslationUnit("test.cpp") { tu -> this.init(tu) }
        }

    /** Builds `fun main() { return <bodyReturnValue> }` and returns the `Return`'s value node. */
    private fun buildReturning(
        buildReturnValue: LanguageFrontend<*, *>.() -> Expression
    ): Expression {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r -> r.returnValue = buildReturnValue() }
                        block.statements += ret
                    }
            }
        }
        return ret.returnValue!!
    }

    /**
     * `true` iff [value] is [ValueEvaluator]'s default give-up placeholder for [node] (see
     * [ValueEvaluator]'s `cannotEvaluate` default: `"{${'$'}{node.name}}"`), which is a [String]
     * but not a genuinely resolved concrete value - it is indistinguishable in shape from a literal
     * of the same text, which is exactly the ambiguity the design doc's Motivation section calls
     * out. Must be excluded from the differential check, or the check would spuriously "pass" any
     * time [StringEvaluator]'s pattern happens to render the same `{name}` text, and would
     * spuriously fail whenever it (correctly) does not.
     */
    private fun isGiveUpPlaceholder(node: Expression, value: String): Boolean =
        value == "{${node.name}}"

    /**
     * Asserts the core differential property for [node]: if [ValueEvaluator] produces a concrete
     * [String] for it - and that string is not merely its give-up placeholder, see
     * [isGiveUpPlaceholder] - [StringEvaluator]'s pattern must be able to produce that exact
     * string.
     */
    private fun assertDifferential(node: Expression) {
        val concrete = ValueEvaluator().evaluate(node)
        if (concrete is String && !isGiveUpPlaceholder(node, concrete)) {
            val pattern = node.evaluateString()
            assertTrue(
                pattern.toRegex().matches(concrete),
                "ValueEvaluator resolved \"$concrete\" for $node, but StringEvaluator's pattern " +
                    "$pattern does not match it",
            )
        }
    }

    @Test
    fun testStringLiteral() {
        val node = buildReturning { newLiteral("foo", objectType("string")) }
        assertDifferential(node)
    }

    @Test
    fun testSimpleConcatenation() {
        val node = buildReturning {
            newBinaryOperator("+") {
                it.lhs = newLiteral("foo", objectType("string"))
                it.rhs = newLiteral("bar", objectType("string"))
            }
        }
        assertDifferential(node)
    }

    @Test
    fun testChainedConcatenation() {
        val node = buildReturning {
            newBinaryOperator("+") {
                it.lhs =
                    newBinaryOperator("+") { inner ->
                        inner.lhs = newLiteral("a", objectType("string"))
                        inner.rhs = newLiteral("b", objectType("string"))
                    }
                it.rhs = newLiteral("c", objectType("string"))
            }
        }
        assertDifferential(node)
    }

    @Test
    fun testStraightLineAssignment() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        block.statements += newDeclarationStatement { decl ->
                            newVariable("x", objectType("string"), holder = decl) {
                                it.initializer = newLiteral("hello", objectType("string"))
                            }
                        }
                        ret = newReturn { r -> r.returnValue = newReference("x") }
                        block.statements += ret
                    }
            }
        }
        assertDifferential(ret.returnValue!!)
    }

    @Test
    fun testCompoundAssignment() {
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
                        block.statements +=
                            newAssign(
                                "=",
                                listOf(newReference("x")),
                                listOf(
                                    newBinaryOperator("+") { op ->
                                        op.lhs = newReference("x")
                                        op.rhs = newLiteral("bar", objectType("string"))
                                    }
                                ),
                            )
                        ret = newReturn { r -> r.returnValue = newReference("x") }
                        block.statements += ret
                    }
            }
        }
        assertDifferential(ret.returnValue!!)
    }

    @Test
    fun testCastOfConstant() {
        val node = buildReturning {
            newCast {
                it.castType = objectType("string")
                it.expression = newLiteral("cast", objectType("string"))
            }
        }
        assertDifferential(node)
    }

    @Test
    fun testNonStringLiteralConcatenation() {
        val node = buildReturning {
            newBinaryOperator("+") {
                it.lhs = newLiteral("x", objectType("string"))
                it.rhs = newLiteral(5, objectType("int"))
            }
        }
        assertDifferential(node)
    }

    @Test
    fun testInterproceduralConstant() {
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
        assertDifferential(ret.returnValue!!)
    }

    @Test
    fun testUnaryDereferenceOfConstant() {
        val node = buildReturning {
            newUnaryOperator("*", postfix = false, prefix = true) {
                it.input = newLiteral("deref", objectType("string"))
            }
        }
        assertDifferential(node)
    }

    @Test
    fun testEmptyStringLiteral() {
        val node = buildReturning { newLiteral("", objectType("string")) }
        assertDifferential(node)
    }

    @Test
    fun testMultiPartConcatenationOfLiterals() {
        val node = buildReturning {
            newBinaryOperator("+") {
                it.lhs =
                    newBinaryOperator("+") { inner1 ->
                        inner1.lhs =
                            newBinaryOperator("+") { inner2 ->
                                inner2.lhs = newLiteral("a", objectType("string"))
                                inner2.rhs = newLiteral("b", objectType("string"))
                            }
                        inner1.rhs = newLiteral("c", objectType("string"))
                    }
                it.rhs = newLiteral("d", objectType("string"))
            }
        }
        assertDifferential(node)
    }

    // --- StringEvaluator succeeds where ValueEvaluator gives up (documentation, not part of the
    // strict differential check: there is no concrete ValueEvaluator answer to compare against) ---

    /**
     * `if (cond) { x = "a" } else { x = "b" }; return x`. `ValueEvaluator.handlePrevDFG` aborts on
     * more than one incoming DFG edge, so it cannot resolve this to a concrete string at all; the
     * differential check above therefore cannot apply. [StringEvaluator] still produces a sound,
     * enumerable `Union` admitting exactly `"a"` and `"b"`.
     */
    @Test
    fun testBranchingJoinValueEvaluatorGivesUp() {
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

        val concrete = ValueEvaluator().evaluate(ret.returnValue)
        assertFalse(
            concrete == "a" || concrete == "b",
            "ValueEvaluator is expected to give up on a branch-dependent value, got \"$concrete\"",
        )

        val enumerated = ret.returnValue!!.evaluateString().enumerate(10)
        assertTrue(
            enumerated == setOf("a", "b"),
            "StringEvaluator must still enumerate exactly {\"a\", \"b\"}, got $enumerated",
        )
    }

    /**
     * `x = ""; while (cond) { x = x + "a" }; return x`. Neither evaluator handles loops precisely,
     * but where plain `ValueEvaluator` loses the value entirely, `StringEvaluator` produces a sound
     * over-approximation that still admits the character every iteration introduces. There is no
     * concrete `ValueEvaluator` answer to differentially compare against here, so this asserts
     * `StringEvaluator`'s own soundness directly (mirrors
     * [StringEvaluatorTest.testLoopBuiltString]).
     */
    @Test
    fun testLoopBuiltStringValueEvaluatorGivesUp() {
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
        assertFalse(pattern.isFullyKnown, "a loop-built string must not be fully known: $pattern")
        assertTrue(
            charSetContains(charSetOf(pattern), CharSet.Chars(setOf('a'))),
            "the over-approximation must still admit 'a': $pattern",
        )
        // "aaa" (3 iterations) must be admitted by the pattern, matching every enumerable
        // ground-truth concretisation this loop could actually produce.
        assertTrue(pattern.toRegex().matches("aaa"), "must admit 3 iterations: $pattern")
    }

    /**
     * A parameter called with two different constant arguments from two call sites.
     * `ValueEvaluator` has no notion of call-site arguments flowing into a parameter, so it cannot
     * resolve this; `StringEvaluator` unions across both call sites.
     */
    @Test
    fun testParameterMultipleCallersValueEvaluatorGivesUp() {
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

        val concrete = ValueEvaluator().evaluate(ret.returnValue)
        assertTrue(
            concrete !is String || isGiveUpPlaceholder(ret.returnValue!!, concrete),
            "ValueEvaluator has no call-site-argument resolution, but resolved \"$concrete\"",
        )

        val enumerated = ret.returnValue!!.evaluateString().enumerate(10)
        assertTrue(enumerated == setOf("a", "b"), "expected {\"a\", \"b\"}, got $enumerated")
    }
}
