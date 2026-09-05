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
package de.fraunhofer.aisec.cpg.analysis.string.golang

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.analysis.string.StringPattern
import de.fraunhofer.aisec.cpg.analysis.string.const
import de.fraunhofer.aisec.cpg.analysis.string.constantPrefix
import de.fraunhofer.aisec.cpg.analysis.string.union
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.singleTranslationUnit
import de.fraunhofer.aisec.cpg.frontends.testFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.Return
import de.fraunhofer.aisec.cpg.graph.types.FunctionType.Companion.computeType
import de.fraunhofer.aisec.cpg.passes.reconstructedImportName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GoStringOperationHandlerTest {

    private fun config(): TranslationConfiguration =
        TranslationConfiguration.builder().defaultPasses().registerLanguage<TestLanguage>().build()

    private fun build(init: LanguageFrontend<*, *>.(TranslationUnit) -> Unit) =
        testFrontend(config()).build {
            this.singleTranslationUnit("test.go") { tu -> this.init(tu) }
        }

    /**
     * `strings.Join([]string{"a", "b"}, ", ")` with a constant slice literal resolves to `Const("a,
     * b")`.
     *
     * The callee is built directly as a `Reference("strings.Join")`, i.e. the shape a real Go
     * frontend produces *after* `ResolveMemberAmbiguityPass` has rewritten the selector-expression
     * chain into a single qualified reference - `reconstructedImportName` on a plain
     * (non-`HasBase`) [Call] is just `call.name`.
     */
    @Test
    fun testJoinConstantList() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            r.returnValue =
                                newCall(newReference("strings.Join")) {
                                    it.arguments +=
                                        newInitializerList(objectType("string")) { list ->
                                            list.initializers +=
                                                newLiteral("a", objectType("string"))
                                            list.initializers +=
                                                newLiteral("b", objectType("string"))
                                        }
                                    it.arguments += newLiteral(", ", objectType("string"))
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val call = ret.returnValue
        assertIs<Call>(call)
        assertEquals("strings.Join", call.reconstructedImportName.toString())

        val pattern = ret.returnValue!!.evaluateGoString()
        assertEquals(const("a, b"), pattern)
    }

    /**
     * `strings.Join(items, ", ")` where `items` is not a slice composite literal (e.g. a parameter)
     * has no statically known elements to enumerate, and must produce `Unknown` rather than a crash
     * or a fabricated value.
     */
    @Test
    fun testJoinOnUnknownSlice() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                val param = newParameter("items", objectType("[]string"), holder = func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            r.returnValue =
                                newCall(newReference("strings.Join")) {
                                    it.arguments += newReference(param.name)
                                    it.arguments += newLiteral(", ", objectType("string"))
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateGoString()
        assertIs<StringPattern.Unknown>(pattern)
    }

    /**
     * `fmt.Sprintf("%s-%d-%s", "a", 1, "b")` with constant operands resolves to `Const("a-1-b")`.
     */
    @Test
    fun testSprintfConstant() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            r.returnValue =
                                newCall(newReference("fmt.Sprintf")) {
                                    it.arguments += newLiteral("%s-%d-%s", objectType("string"))
                                    it.arguments += newLiteral("a", objectType("string"))
                                    it.arguments += newLiteral(1, objectType("int"))
                                    it.arguments += newLiteral("b", objectType("string"))
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateGoString()
        assertEquals(const("a-1-b"), pattern)
    }

    /**
     * `fmt.Sprintf("%s%v%s", "a", "b", "c")` regression test for argument-index consumption: `%v`
     * (Go's "default format" verb) must consume exactly one positional argument, same as `%s`/`%d`,
     * so that the second `%s` resolves to `"c"` (index 2), not `"b"` again. This is the Go
     * equivalent of the Java `%n` regression test - except Go's `fmt` package has no verb that
     * consumes zero arguments (see `GoStringOperationHandler`'s KDoc for why), so there is no
     * analogous "must not shift" case; this test instead confirms every recognised verb consumes
     * exactly one argument, keeping subsequent placeholders aligned.
     */
    @Test
    fun testSprintfArgumentIndexAlignment() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            r.returnValue =
                                newCall(newReference("fmt.Sprintf")) {
                                    it.arguments += newLiteral("%s%v%s", objectType("string"))
                                    it.arguments += newLiteral("a", objectType("string"))
                                    it.arguments += newLiteral("b", objectType("string"))
                                    it.arguments += newLiteral("c", objectType("string"))
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateGoString()
        assertEquals(const("abc"), pattern)
    }

    /**
     * `fmt.Sprintf("%q", x)` where `x` is a parameter (hence `Unknown`): `%q` is not one of the
     * exactly-modelled verbs, but must still consume its one positional argument conservatively
     * rather than crashing or misaligning later placeholders.
     */
    @Test
    fun testSprintfUnsupportedVerbStillConsumesArgument() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            r.returnValue =
                                newCall(newReference("fmt.Sprintf")) {
                                    it.arguments += newLiteral("%q-%s", objectType("string"))
                                    it.arguments += newLiteral("a", objectType("string"))
                                    it.arguments += newLiteral("b", objectType("string"))
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateGoString()
        assertEquals(const("-b"), (pattern as? StringPattern.Concat)?.parts?.last())
    }

    /** `strings.Replace("foobar", "bar", "baz", -1)` with constant operands: exact result. */
    @Test
    fun testReplaceUnboundedConstant() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            r.returnValue =
                                newCall(newReference("strings.Replace")) {
                                    it.arguments += newLiteral("foobarbar", objectType("string"))
                                    it.arguments += newLiteral("bar", objectType("string"))
                                    it.arguments += newLiteral("baz", objectType("string"))
                                    it.arguments += newLiteral(-1, objectType("int"))
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateGoString()
        assertEquals(const("foobazbaz"), pattern)
    }

    /** `strings.Replace("foobarbar", "bar", "baz", 1)` bounded to a single replacement. */
    @Test
    fun testReplaceBoundedConstant() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            r.returnValue =
                                newCall(newReference("strings.Replace")) {
                                    it.arguments += newLiteral("foobarbar", objectType("string"))
                                    it.arguments += newLiteral("bar", objectType("string"))
                                    it.arguments += newLiteral("baz", objectType("string"))
                                    it.arguments += newLiteral(1, objectType("int"))
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateGoString()
        assertEquals(const("foobazbar"), pattern)
    }

    /** `strings.ReplaceAll("foobarbar", "bar", "baz")` replaces every occurrence. */
    @Test
    fun testReplaceAllConstant() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            r.returnValue =
                                newCall(newReference("strings.ReplaceAll")) {
                                    it.arguments += newLiteral("foobarbar", objectType("string"))
                                    it.arguments += newLiteral("bar", objectType("string"))
                                    it.arguments += newLiteral("baz", objectType("string"))
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateGoString()
        assertEquals(const("foobazbaz"), pattern)
    }

    /**
     * `("cat" + p).Replace...`-shaped overlap-safety regression: `strings.ReplaceAll(x, "at",
     * "XX")` where `x` is `"cat" + p` for an unknown parameter `p`. `"at"` occurs inside the
     * receiver's known constant prefix `"cat"` (a real value `"catfoo"` would become `"cXXfoo"`,
     * which does not start with `"cat"`), so the narrow `Concat(prefix, Unknown)`
     * over-approximation would be unsound here - this is the same soundness fix as
     * `PythonStringOperationHandler.handleReplace`'s / `JvmStringOperationHandler.handleReplace`'s,
     * applied to the Go handler, reusing the shared `cannotOccurWithinPrefix`.
     */
    @Test
    fun testReplaceAllOldOverlappingPrefixFallsBackToCoarseUnknown() {
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
                                newCall(newReference("strings.ReplaceAll")) {
                                    it.arguments +=
                                        newBinaryOperator("+") { op ->
                                            op.lhs = newLiteral("cat", objectType("string"))
                                            op.rhs = newReference(param.name)
                                        }
                                    it.arguments += newLiteral("at", objectType("string"))
                                    it.arguments += newLiteral("XX", objectType("string"))
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateGoString()
        assertTrue(
            pattern.constantPrefix() != "cat",
            "expected the coarse fallback (no guaranteed \"cat\" prefix), got $pattern",
        )
    }

    /** `strings.ToUpper("foo")` with a constant operand resolves to `Const("FOO")`. */
    @Test
    fun testToUpperConstant() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            r.returnValue =
                                newCall(newReference("strings.ToUpper")) {
                                    it.arguments += newLiteral("foo", objectType("string"))
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateGoString()
        assertEquals(const("FOO"), pattern)
    }

    /**
     * `strings.ToUpper(x)` where `x` is branch-dependent between `"a"` and `"straße"` must map the
     * case conversion over every alternative, in particular exercising the sound `ß -> "SS"`
     * multi-character mapping via the shared `mapConstLeaves`/`mapCharSet` helper (now shared with
     * `PythonStringOperationHandler`, previously private to it).
     */
    @Test
    fun testToUpperMultiCharCaseMapping() {
        lateinit var ret: Return
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
                                            listOf(newLiteral("a", objectType("string"))),
                                        )
                                }
                            ifElse.elseStatement =
                                newBlock(enterScope = true) { elseBlock ->
                                    elseBlock.statements +=
                                        newAssign(
                                            "=",
                                            listOf(newReference("x")),
                                            listOf(newLiteral("straße", objectType("string"))),
                                        )
                                }
                        }
                        ret = newReturn { r ->
                            r.returnValue =
                                newCall(newReference("strings.ToUpper")) {
                                    it.arguments += newReference("x")
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateGoString()
        assertEquals(union(const("A"), const("STRASSE")), pattern)
    }

    /** `strings.TrimSpace(" foo ")` with a constant operand resolves to `Const("foo")`. */
    @Test
    fun testTrimSpaceConstant() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            r.returnValue =
                                newCall(newReference("strings.TrimSpace")) {
                                    it.arguments += newLiteral("  foo  ", objectType("string"))
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateGoString()
        assertEquals(const("foo"), pattern)
    }

    /** `strings.Trim("xxfooxx", "x")` with constant operands resolves to `Const("foo")`. */
    @Test
    fun testTrimWithCutsetConstant() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            r.returnValue =
                                newCall(newReference("strings.Trim")) {
                                    it.arguments += newLiteral("xxfooxx", objectType("string"))
                                    it.arguments += newLiteral("x", objectType("string"))
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateGoString()
        assertEquals(const("foo"), pattern)
    }

    /**
     * `strings.TrimSpace(p)` where `p` is a parameter (`Unknown`) must produce a sound `Unknown`,
     * not a false-precise constant.
     */
    @Test
    fun testTrimSpaceNonConstant() {
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
                                newCall(newReference("strings.TrimSpace")) {
                                    it.arguments += newReference(param.name)
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateGoString()
        assertIs<StringPattern.Unknown>(pattern)
    }

    /** `path.Join("a", "b", "c")` with constant operands resolves to `Const("a/b/c")`. */
    @Test
    fun testPathJoinConstant() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            r.returnValue =
                                newCall(newReference("path.Join")) {
                                    it.arguments += newLiteral("a", objectType("string"))
                                    it.arguments += newLiteral("b", objectType("string"))
                                    it.arguments += newLiteral("c", objectType("string"))
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val call = ret.returnValue
        assertIs<Call>(call)
        assertEquals("path.Join", call.reconstructedImportName.toString())

        val pattern = ret.returnValue!!.evaluateGoString()
        assertEquals(const("a/b/c"), pattern)
    }

    /**
     * `filepath.Join("a", x)` where `x` is a branch-dependent value must produce a `Union`
     * over-approximation rather than a single, falsely-precise constant.
     */
    @Test
    fun testFilepathJoinWithBranchingArgument() {
        lateinit var ret: Return
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
                                            listOf(newLiteral("b", objectType("string"))),
                                        )
                                }
                            ifElse.elseStatement =
                                newBlock(enterScope = true) { elseBlock ->
                                    elseBlock.statements +=
                                        newAssign(
                                            "=",
                                            listOf(newReference("x")),
                                            listOf(newLiteral("c", objectType("string"))),
                                        )
                                }
                        }
                        ret = newReturn { r ->
                            r.returnValue =
                                newCall(newReference("filepath.Join")) {
                                    it.arguments += newLiteral("a", objectType("string"))
                                    it.arguments += newReference("x")
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateGoString()
        assertEquals(union(const("a/b"), const("a/c")), pattern)
    }

    /**
     * `"go" + x` (plain `+` concatenation) needs no Go-specific handler code at all: it is already
     * covered generically by `StringEvaluator.handleBinaryOperator`, which is language-agnostic.
     * This test merely confirms that generic behaviour still works when routed through
     * [evaluateGoString] (i.e. with [GoStringOperationHandler] registered alongside it).
     */
    @Test
    fun testPlusConcatenationIsGeneric() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            r.returnValue =
                                newBinaryOperator("+") { op ->
                                    op.lhs = newLiteral("go", objectType("string"))
                                    op.rhs = newLiteral("lang", objectType("string"))
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateGoString()
        assertEquals(const("golang"), pattern)
    }
}
