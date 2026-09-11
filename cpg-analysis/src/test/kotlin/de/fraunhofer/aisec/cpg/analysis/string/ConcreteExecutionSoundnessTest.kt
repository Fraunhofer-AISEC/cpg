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
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Phase 6 "concrete execution" soundness testing.
 *
 * The design doc asks for running small Python fixtures and asserting that every *observed*
 * (dynamically executed) string is matched by the computed pattern. Actually doing that from a JVM
 * test needs either an embedded interpreter (Jython/GraalPy - a new, heavyweight runtime
 * dependency, contrary to the project-wide "no new dependencies" constraint, D4 in the design doc)
 * or shelling out to a `python3` binary, which would make this test suite fragile and
 * environment-dependent (no `python3` guarantee in every dev/CI environment). A repo-wide search
 * (`ProcessBuilder`, `"python3"`, `"python"` invocations in test code) found no existing precedent
 * for actually *executing* Python from a test anywhere in this codebase - only parsing it into a
 * CPG - so there is no existing shell-out helper to reuse either.
 *
 * This test instead implements a lighter-weight but still meaningful version: fixtures are small
 * control-flow snippets whose finite set of possible concrete runtime values is known *by
 * construction* (we wrote the fixture, so we know exactly which branches/bounded loop unrollings
 * are reachable and what they produce - this is exactly what a real interpreter would report for
 * these specific, hand-picked inputs). We manually enumerate that ground truth and assert the
 * computed [StringPattern] matches every one of those concrete values via [StringPattern.toRegex].
 *
 * This is strictly weaker than true dynamic execution: it cannot catch a fixture whose *actual*
 * runtime behaviour differs from what we assumed when writing it (e.g. a subtle language semantics
 * bug in the snippet itself), only a divergence between the evaluator and the ground truth we
 * declared. If a `python3`-shelling test helper is ever added to this repo for other reasons, the
 * fixtures below should be revisited and re-validated against genuine dynamic execution as
 * follow-up work.
 */
class ConcreteExecutionSoundnessTest {

    private fun config(): TranslationConfiguration =
        TranslationConfiguration.builder().defaultPasses().registerLanguage<TestLanguage>().build()

    private fun build(init: LanguageFrontend<*, *>.(TranslationUnit) -> Unit) =
        testFrontend(config()).build {
            this.singleTranslationUnit("test.cpp") { tu -> this.init(tu) }
        }

    /**
     * Ground truth: `x = "a"; if (cond) { x = x + "b" }; return x` can concretely evaluate to
     * exactly `"a"` (branch not taken) or `"ab"` (branch taken) - these are the only two runtime
     * outcomes this snippet can ever produce, by construction.
     */
    @Test
    fun testSingleBranchAppend() {
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
                                it.initializer = newLiteral("a", objectType("string"))
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
                                            listOf(
                                                newBinaryOperator("+") { op ->
                                                    op.lhs = newReference("x")
                                                    op.rhs = newLiteral("b", objectType("string"))
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
        val groundTruth = setOf("a", "ab")
        assertAllMatched(pattern, groundTruth)
    }

    /**
     * Ground truth: `x = ""; for 0..2 iterations: x = x + "a"; return x` (unrolled manually as two
     * sequential, independent if/else diamonds, each optionally appending `"a"`) can concretely
     * produce `""`, `"a"`, or `"aa"` - the three reachable outcomes of 0, 1, or 2 taken branches.
     */
    @Test
    fun testBoundedUnrolledLoopAppend() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                newParameter("cond0", objectType("bool"), holder = func)
                newParameter("cond1", objectType("bool"), holder = func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        block.statements += newDeclarationStatement { decl ->
                            newVariable("x", objectType("string"), holder = decl) {
                                it.initializer = newLiteral("", objectType("string"))
                            }
                        }
                        repeat(2) { i ->
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
                                                            newLiteral("a", objectType("string"))
                                                    }
                                                ),
                                            )
                                    }
                                ifElse.elseStatement = newBlock(enterScope = true) {}
                            }
                        }
                        ret = newReturn { r -> r.returnValue = newReference("x") }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateString()
        val groundTruth = setOf("", "a", "aa")
        assertAllMatched(pattern, groundTruth)
    }

    /**
     * Ground truth: `x = "a"; if (cond1) { x = x + "b" } else { x = x + "c" }; if (cond2) { x = x +
     * "d" }; return x` reaches exactly `"abd"`, `"ab"`, `"acd"`, `"ac"` across its four path
     * combinations.
     */
    @Test
    fun testTwoSequentialBranches() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                newParameter("cond1", objectType("bool"), holder = func)
                newParameter("cond2", objectType("bool"), holder = func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        block.statements += newDeclarationStatement { decl ->
                            newVariable("x", objectType("string"), holder = decl) {
                                it.initializer = newLiteral("a", objectType("string"))
                            }
                        }
                        block.statements += newIfElse { ifElse ->
                            ifElse.condition = newReference("cond1")
                            ifElse.thenStatement =
                                newBlock(enterScope = true) { thenBlock ->
                                    thenBlock.statements +=
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
                                                    op.rhs = newLiteral("c", objectType("string"))
                                                }
                                            ),
                                        )
                                }
                        }
                        block.statements += newIfElse { ifElse ->
                            ifElse.condition = newReference("cond2")
                            ifElse.thenStatement =
                                newBlock(enterScope = true) { thenBlock ->
                                    thenBlock.statements +=
                                        newAssign(
                                            "=",
                                            listOf(newReference("x")),
                                            listOf(
                                                newBinaryOperator("+") { op ->
                                                    op.lhs = newReference("x")
                                                    op.rhs = newLiteral("d", objectType("string"))
                                                }
                                            ),
                                        )
                                }
                            ifElse.elseStatement = newBlock(enterScope = true) {}
                        }
                        ret = newReturn { r -> r.returnValue = newReference("x") }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateString()
        val groundTruth = setOf("abd", "ab", "acd", "ac")
        assertAllMatched(pattern, groundTruth)
    }

    private fun assertAllMatched(pattern: StringPattern, groundTruth: Set<String>) {
        val regex = pattern.toRegex()
        for (value in groundTruth) {
            assertTrue(
                regex.matches(value),
                "the pattern $pattern must match the ground-truth concrete value \"$value\"",
            )
        }
    }
}
