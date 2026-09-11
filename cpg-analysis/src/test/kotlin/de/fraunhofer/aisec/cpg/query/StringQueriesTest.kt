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
package de.fraunhofer.aisec.cpg.query

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.analysis.string.StringPattern
import de.fraunhofer.aisec.cpg.analysis.string.const
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.singleTranslationUnit
import de.fraunhofer.aisec.cpg.frontends.testFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.Return
import de.fraunhofer.aisec.cpg.graph.types.FunctionType.Companion.computeType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class StringQueriesTest {

    private fun config(): TranslationConfiguration =
        TranslationConfiguration.builder().defaultPasses().registerLanguage<TestLanguage>().build()

    private fun build(init: LanguageFrontend<*, *>.(TranslationUnit) -> Unit) =
        testFrontend(config()).build {
            this.singleTranslationUnit("test.cpp") { tu -> this.init(tu) }
        }

    /** `return "foo"` should surface as a `QueryTree<StringPattern>` wrapping `Const("foo")`. */
    @Test
    fun testStringValueConstant() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            r.returnValue = newLiteral("foo", objectType("string"))
                        }
                        block.statements += ret
                    }
            }
        }

        val queryTree = ret.returnValue!!.stringValue()
        assertEquals(const("foo"), queryTree.value)
        assertEquals(ret.returnValue, queryTree.node)
    }

    /** A fully-known constant must match a regex that matches it exactly. */
    @Test
    fun testStringMustMatchTrue() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            r.returnValue = newLiteral("foo", objectType("string"))
                        }
                        block.statements += ret
                    }
            }
        }

        val queryTree = ret.returnValue!!.stringMustMatch(Regex("foo"))
        assertTrue(queryTree.value)
        assertTrue(
            queryTree.assumptions.isEmpty(),
            "a fully-decided mustMatch must not carry a give-up assumption",
        )
    }

    /** A fully-known constant must not match a regex that does not match it. */
    @Test
    fun testStringMustMatchFalseProven() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            r.returnValue = newLiteral("foo", objectType("string"))
                        }
                        block.statements += ret
                    }
            }
        }

        val queryTree = ret.returnValue!!.stringMustMatch(Regex("bar"))
        assertFalse(queryTree.value)
        assertTrue(
            queryTree.assumptions.isEmpty(),
            "a proven non-match must not carry a give-up assumption",
        )
    }

    /**
     * A parameter with no reachable caller becomes `Unknown(PARAMETER)`, whose language cannot be
     * enumerated: `mustMatch` conservatively returns `false`, and this must now carry a
     * `SoundnessAssumption` since we have a node context here (the resolved Phase 2/4 TODO).
     */
    @Test
    fun testStringMustMatchFalseGivesUpWithAssumption() {
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

        val queryTree = ret.returnValue!!.stringMustMatch(Regex("foo"))
        assertFalse(queryTree.value)
        assertTrue(
            queryTree.assumptions.isNotEmpty(),
            "a give-up (unenumerable) mustMatch must carry a SoundnessAssumption",
        )
    }

    /** A fully-known constant matched by [Regex] is also reported by `mayMatch`. */
    @Test
    fun testStringMayMatchTrue() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            r.returnValue = newLiteral("foo", objectType("string"))
                        }
                        block.statements += ret
                    }
            }
        }

        val queryTree = ret.returnValue!!.stringMayMatch(Regex("foo"))
        assertTrue(queryTree.value)
    }

    /**
     * `if (cond) { x = "foo" } else { x = "bar" }; return x` never matches a regex that matches
     * neither branch.
     */
    @Test
    fun testStringMayMatchFalse() {
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
                                            listOf(newLiteral("foo", objectType("string"))),
                                        )
                                }
                            ifElse.elseStatement =
                                newBlock(enterScope = true) { elseBlock ->
                                    elseBlock.statements +=
                                        newAssign(
                                            "=",
                                            listOf(newReference("x")),
                                            listOf(newLiteral("bar", objectType("string"))),
                                        )
                                }
                        }
                        ret = newReturn { r -> r.returnValue = newReference("x") }
                        block.statements += ret
                    }
            }
        }

        val queryTree = ret.returnValue!!.stringMayMatch(Regex("baz"))
        assertFalse(queryTree.value)
    }

    /**
     * A chain of 200 functions (`f0` -> `f1` -> ... -> `f199`) exceeds the default `maxCallDepth =
     * 10`, so [de.fraunhofer.aisec.cpg.analysis.string.StringEvaluator] gives up with
     * `Unknown(BUDGET_EXCEEDED)` and records a `SoundnessAssumption` on the root node - this must
     * surface on the `QueryTree` returned by [stringValue], confirming assumptions propagate from
     * the evaluator into the query API for an over-approximated case.
     */
    @Test
    fun testStringValueSurfacesBudgetExceededAssumption() {
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

        val queryTree = topCall.stringValue()
        val value = queryTree.value

        assertIs<StringPattern.Unknown>(value)
        assertEquals(StringPattern.Reason.BUDGET_EXCEEDED, value.reason)
        assertTrue(
            queryTree.assumptions.isNotEmpty(),
            "budget exhaustion must surface a SoundnessAssumption on the returned QueryTree",
        )
    }

    /**
     * `stringMustMatch`/`stringMayMatch` attach the [de.fraunhofer.aisec.cpg.query.stringValue]
     * `QueryTree` only as a `children` entry, so an assumption recorded on that underlying tree
     * (here: the `BUDGET_EXCEEDED` `SoundnessAssumption` from the 200-deep call chain, see
     * [testStringValueSurfacesBudgetExceededAssumption]) must still be reachable via
     * `relevantAssumptions()` on the `mustMatch`/`mayMatch` result, not silently dropped.
     */
    @Test
    fun testStringMustMatchAndMayMatchSurfaceUnderlyingAssumptions() {
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

        val mustMatchTree = topCall.stringMustMatch(Regex("leaf"))
        assertTrue(
            mustMatchTree.relevantAssumptions().isNotEmpty(),
            "stringMustMatch must not drop the underlying stringValue's assumptions",
        )

        val mayMatchTree = topCall.stringMayMatch(Regex("leaf"))
        assertTrue(
            mayMatchTree.relevantAssumptions().isNotEmpty(),
            "stringMayMatch must not drop the underlying stringValue's assumptions",
        )
    }
}
