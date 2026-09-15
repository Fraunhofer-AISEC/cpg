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
package de.fraunhofer.aisec.cpg.analysis.string.jvm

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.analysis.string.StringPattern
import de.fraunhofer.aisec.cpg.analysis.string.const
import de.fraunhofer.aisec.cpg.analysis.string.constantPrefix
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.singleTranslationUnit
import de.fraunhofer.aisec.cpg.frontends.testFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.expressions.MemberCall
import de.fraunhofer.aisec.cpg.graph.expressions.Return
import de.fraunhofer.aisec.cpg.graph.types.FunctionType.Companion.computeType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JvmStringOperationHandlerTest {

    private fun config(): TranslationConfiguration =
        TranslationConfiguration.builder().defaultPasses().registerLanguage<TestLanguage>().build()

    private fun build(init: LanguageFrontend<*, *>.(TranslationUnit) -> Unit) =
        testFrontend(config()).build {
            this.singleTranslationUnit("test.java") { tu -> this.init(tu) }
        }

    /** `new StringBuilder("foo").append("bar")` with constant operands resolves to `Const`. */
    @Test
    fun testAppendChainConstant() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            val construction =
                                newConstruction("StringBuilder") {
                                    it.type = objectType("StringBuilder")
                                    it.arguments += newLiteral("foo", objectType("string"))
                                }
                            r.returnValue =
                                newMemberCall(newMemberAccess("append", construction)) {
                                    it.arguments += newLiteral("bar", objectType("string"))
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateJvmString()
        assertEquals(const("foobar"), pattern)
    }

    /**
     * `new StringBuilder().append(x).append("!")` where `x` is a parameter (hence `Unknown`) must
     * produce a sound over-approximation, not a crash or a false-precise constant.
     */
    @Test
    fun testAppendChainNonConstant() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                val param = newParameter("x", objectType("string"), holder = func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            val construction =
                                newConstruction("StringBuilder") {
                                    it.type = objectType("StringBuilder")
                                }
                            val firstAppend =
                                newMemberCall(newMemberAccess("append", construction)) {
                                    it.arguments += newReference(param.name)
                                }
                            r.returnValue =
                                newMemberCall(newMemberAccess("append", firstAppend)) {
                                    it.arguments += newLiteral("!", objectType("string"))
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateJvmString()
        assertTrue(
            pattern is StringPattern.Concat || pattern is StringPattern.Unknown,
            "expected a Concat or Unknown, got $pattern",
        )
    }

    /** `"foo".concat("bar")` with constant operands resolves to `Const("foobar")`. */
    @Test
    fun testConcatConstant() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            r.returnValue =
                                newMemberCall(
                                    newMemberAccess(
                                        "concat",
                                        newLiteral("foo", objectType("string")),
                                    )
                                ) {
                                    it.arguments += newLiteral("bar", objectType("string"))
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateJvmString()
        assertEquals(const("foobar"), pattern)
    }

    /** `s.concat(x)` where `x` is a parameter must produce a sound over-approximation. */
    @Test
    fun testConcatNonConstant() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                val param = newParameter("x", objectType("string"), holder = func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            r.returnValue =
                                newMemberCall(
                                    newMemberAccess(
                                        "concat",
                                        newLiteral("foo", objectType("string")),
                                    )
                                ) {
                                    it.arguments += newReference(param.name)
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateJvmString()
        assertTrue(
            pattern is StringPattern.Concat || pattern is StringPattern.Unknown,
            "expected a Concat or Unknown, got $pattern",
        )
    }

    /** `String.format("%s-%s", "a", "b")` with constant operands resolves to `Const("a-b")`. */
    @Test
    fun testFormatConstant() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            r.returnValue =
                                newMemberCall(
                                    newMemberAccess("format", newReference("String")),
                                    isStatic = true,
                                ) {
                                    it.arguments += newLiteral("%s-%s", objectType("string"))
                                    it.arguments += newLiteral("a", objectType("string"))
                                    it.arguments += newLiteral("b", objectType("string"))
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val call = ret.returnValue
        assertIs<MemberCall>(call)
        assertTrue(call.isStatic)

        val pattern = ret.returnValue!!.evaluateJvmString()
        assertEquals(const("a-b"), pattern)
    }

    /**
     * `String.format("%s", x)` where `x` is a parameter (hence `Unknown`) must produce a sound
     * over-approximation.
     */
    @Test
    fun testFormatNonConstant() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                val param = newParameter("x", objectType("string"), holder = func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            r.returnValue =
                                newMemberCall(
                                    newMemberAccess("format", newReference("String")),
                                    isStatic = true,
                                ) {
                                    it.arguments += newLiteral("%s", objectType("string"))
                                    it.arguments += newReference(param.name)
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateJvmString()
        assertTrue(
            pattern is StringPattern.Concat || pattern is StringPattern.Unknown,
            "expected a Concat or Unknown, got $pattern",
        )
    }

    /**
     * `String.format("%s%n%s", "a", "b", "c")` regression test: `%n` takes no argument at all, so
     * it must not shift the auto-index used by the surrounding `%s` conversions. Real Java
     * semantics: the first `%s` consumes `"a"` (auto-index 0), `%n` consumes nothing, the second
     * `%s` consumes `"b"` (auto-index 1, *not* 2) - `"c"` is simply an unused trailing vararg,
     * which `String.format` permits. Before the fix, `%n` incorrectly advanced the auto-index and
     * consumed `"b"` as an `Unknown`/discarded value, so the second `%s` would resolve to `"c"`
     * instead of `"b"`.
     */
    @Test
    fun testFormatPercentNDoesNotShiftArgumentIndex() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            r.returnValue =
                                newMemberCall(
                                    newMemberAccess("format", newReference("String")),
                                    isStatic = true,
                                ) {
                                    it.arguments += newLiteral("%s%n%s", objectType("string"))
                                    it.arguments += newLiteral("a", objectType("string"))
                                    it.arguments += newLiteral("b", objectType("string"))
                                    it.arguments += newLiteral("c", objectType("string"))
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateJvmString()
        assertEquals(const("a\nb"), pattern)
    }

    /**
     * `String.format("%n", ...)` alone: a pure literal newline, with no `%s`/`%d` conversions to
     * consume any of the (irrelevant, unused) trailing varargs.
     */
    @Test
    fun testFormatPercentNAlone() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            r.returnValue =
                                newMemberCall(
                                    newMemberAccess("format", newReference("String")),
                                    isStatic = true,
                                ) {
                                    it.arguments += newLiteral("%n", objectType("string"))
                                    it.arguments += newLiteral("ignored", objectType("string"))
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateJvmString()
        assertEquals(const("\n"), pattern)
    }

    /** `String.join(", ", "a", "b")` with constant operands resolves to `Const("a, b")`. */
    @Test
    fun testJoinConstant() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            r.returnValue =
                                newMemberCall(
                                    newMemberAccess("join", newReference("String")),
                                    isStatic = true,
                                ) {
                                    it.arguments += newLiteral(", ", objectType("string"))
                                    it.arguments += newLiteral("a", objectType("string"))
                                    it.arguments += newLiteral("b", objectType("string"))
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateJvmString()
        assertEquals(const("a, b"), pattern)
    }

    /**
     * `String.join(", ", x)` where `x` is a single, non-enumerable argument (e.g. a parameter
     * holding a collection) has no statically known elements, and must produce `Unknown` rather
     * than a crash or a fabricated value.
     */
    @Test
    fun testJoinNonConstant() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                val param = newParameter("items", objectType("Iterable"), holder = func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            r.returnValue =
                                newMemberCall(
                                    newMemberAccess("join", newReference("String")),
                                    isStatic = true,
                                ) {
                                    it.arguments += newLiteral(", ", objectType("string"))
                                    it.arguments += newReference(param.name)
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateJvmString()
        assertIs<StringPattern.Unknown>(pattern)
    }

    /** `"hello".substring(1, 3)` with constant operands resolves to the exact `Const("el")`. */
    @Test
    fun testSubstringConstant() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            r.returnValue =
                                newMemberCall(
                                    newMemberAccess(
                                        "substring",
                                        newLiteral("hello", objectType("string")),
                                    )
                                ) {
                                    it.arguments += newLiteral(1, objectType("int"))
                                    it.arguments += newLiteral(3, objectType("int"))
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateJvmString()
        assertEquals(const("el"), pattern)
    }

    /**
     * `p.substring(1)` where `p` is a parameter (`Unknown`) must produce a sound `Unknown` - it
     * must not claim a bogus exact result.
     */
    @Test
    fun testSubstringNonConstant() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                val param = newParameter("p", objectType("string"), holder = func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            r.returnValue =
                                newMemberCall(
                                    newMemberAccess("substring", newReference(param.name))
                                ) {
                                    it.arguments += newLiteral(1, objectType("int"))
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateJvmString()
        assertIs<StringPattern.Unknown>(pattern)
    }

    /** `"foobar".replace("bar", "baz")` with all-constant operands computes the exact result. */
    @Test
    fun testReplaceConstant() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            r.returnValue =
                                newMemberCall(
                                    newMemberAccess(
                                        "replace",
                                        newLiteral("foobar", objectType("string")),
                                    )
                                ) {
                                    it.arguments += newLiteral("bar", objectType("string"))
                                    it.arguments += newLiteral("baz", objectType("string"))
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateJvmString()
        assertEquals(const("foobaz"), pattern)
    }

    /**
     * `s.replace(old, new)` where the receiver is a branch-dependent (`Union`) value cannot be
     * computed exactly and must fall back to the over-approximation, recording a
     * `SoundnessAssumption` on the call.
     */
    @Test
    fun testReplaceNonConstantRecordsAssumption() {
        lateinit var ret: Return
        lateinit var call: MemberCall
        build { tu ->
            newFunction("helper", holder = tu, enterScope = true) { func ->
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
                        ret = newReturn { r ->
                            call =
                                newMemberCall(newMemberAccess("replace", newReference("x"))) {
                                    it.arguments += newLiteral("o", objectType("string"))
                                    it.arguments += newLiteral("0", objectType("string"))
                                }
                            r.returnValue = call
                        }
                        block.statements += ret
                    }
            }
        }

        assertTrue(call.assumptions.isEmpty(), "no assumption should exist before evaluation")
        val pattern = ret.returnValue!!.evaluateJvmString()
        assertTrue(
            pattern is StringPattern.Concat || pattern is StringPattern.Unknown,
            "expected an over-approximation, got $pattern",
        )
        assertTrue(
            call.assumptions.isNotEmpty(),
            "the over-approximate `replace` path must record a SoundnessAssumption on the call",
        )
    }

    /**
     * `("cat" + p).replace("at", "XX")` where `p` is an unknown parameter. `"at"` occurs inside the
     * receiver's known constant prefix `"cat"` (e.g. a real receiver value `"catfoo"` would become
     * `"cXXfoo"`, which does not start with `"cat"`), so the narrow `Concat(prefix, Unknown)`
     * over-approximation would be unsound here - this is the same soundness fix as
     * `PythonStringOperationHandler.handleReplace`'s, applied to the JVM handler.
     */
    @Test
    fun testReplaceOldOverlappingPrefixFallsBackToCoarseUnknown() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                val param = newParameter("p", objectType("string"), holder = func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            r.returnValue =
                                newMemberCall(
                                    newMemberAccess(
                                        "replace",
                                        newBinaryOperator("+") { op ->
                                            op.lhs = newLiteral("cat", objectType("string"))
                                            op.rhs = newReference(param.name)
                                        },
                                    )
                                ) {
                                    it.arguments += newLiteral("at", objectType("string"))
                                    it.arguments += newLiteral("XX", objectType("string"))
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateJvmString()
        assertTrue(
            pattern.constantPrefix() != "cat",
            "expected the coarse fallback (no guaranteed \"cat\" prefix), got $pattern",
        )
    }
}
