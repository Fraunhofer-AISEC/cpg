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
package de.fraunhofer.aisec.cpg.analysis.string.cxx

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
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Tests for [CStringOperationHandler].
 *
 * **On the `strcat`/`strncat`/`strcpy` tests' DFG wiring.** As documented on
 * [CStringOperationHandler]'s KDoc, whether a real C/C++ frontend models a call to one of these
 * functions as a def-site for its `dest` argument (i.e. whether a *later* read of `dest` has a DFG
 * edge flowing from the call itself) is an open question that has not been verified against a real
 * C frontend/fixture. To keep these tests' ground truth unambiguous regardless of that, they do not
 * rely on any frontend pass to wire this up: they construct two independent [Reference] nodes for
 * `dest` (one used as the call's argument, representing the read of `dest`'s *old* value; one used
 * as a later, independent read) and manually add the `prevDFG` edge that a real frontend would need
 * to add from the call to the later read for this handler to ever be consulted for it. This must be
 * re-verified against a real C fixture once one is available end-to-end.
 */
class CStringOperationHandlerTest {

    private fun config(): TranslationConfiguration =
        TranslationConfiguration.builder().defaultPasses().registerLanguage<TestLanguage>().build()

    private fun build(init: LanguageFrontend<*, *>.(TranslationUnit) -> Unit) =
        testFrontend(config()).build {
            this.singleTranslationUnit("test.c") { tu -> this.init(tu) }
        }

    /**
     * `strcpy(dest, "new")` where `dest` held some old constant value beforehand: the value of a
     * later read of `dest` must be exactly `Const("new")`, with no trace of the old value -
     * `strcpy` overwrites, it does not concatenate. This is the core distinction this handler must
     * get right.
     */
    @Test
    fun testStrcpyOverwritesOldValue() {
        lateinit var call: Call
        lateinit var laterRead: Reference
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.body =
                    newBlock(enterScope = true) { block ->
                        val destArgAtCall = newReference("destArg")
                        destArgAtCall.prevDFG += newLiteral("old", objectType("char*"))

                        call =
                            newCall(newReference("strcpy")) {
                                it.arguments += destArgAtCall
                                it.arguments += newLiteral("new", objectType("char*"))
                            }
                        block.statements += call

                        laterRead = newReference("dest")
                        laterRead.prevDFG += call
                        block.statements += newReturn { it.returnValue = laterRead }
                    }
            }
        }

        val pattern = laterRead.evaluateCString()
        assertEquals(const("new"), pattern)
    }

    /**
     * `strcat(dest, "bar")` where `dest` held the constant `"old"` beforehand: the value of a later
     * read of `dest` must be `Const("oldbar")` - unlike `strcpy`, `strcat`'s result genuinely
     * depends on `dest`'s old value.
     */
    @Test
    fun testStrcatDependsOnOldValue() {
        lateinit var call: Call
        lateinit var laterRead: Reference
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.body =
                    newBlock(enterScope = true) { block ->
                        val destArgAtCall = newReference("destArg")
                        destArgAtCall.prevDFG += newLiteral("old", objectType("char*"))

                        call =
                            newCall(newReference("strcat")) {
                                it.arguments += destArgAtCall
                                it.arguments += newLiteral("bar", objectType("char*"))
                            }
                        block.statements += call

                        laterRead = newReference("dest")
                        laterRead.prevDFG += call
                        block.statements += newReturn { it.returnValue = laterRead }
                    }
            }
        }

        val pattern = laterRead.evaluateCString()
        assertEquals(const("oldbar"), pattern)
    }

    /**
     * `strncat(dest, src, 3)` where `src` is not a resolvable constant (e.g. a parameter): the
     * appended segment cannot be computed exactly, but its length is still bounded above by `3` -
     * losing that bound (falling back to a fully unbounded `Unknown`) would throw away real,
     * exploitable information.
     */
    @Test
    fun testStrncatBoundsLengthWhenSrcNonConstant() {
        lateinit var laterRead: Reference
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                val param = newParameter("src", objectType("char*"), holder = func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        val destArgAtCall = newReference("destArg")
                        destArgAtCall.prevDFG += newLiteral("old", objectType("char*"))

                        val call =
                            newCall(newReference("strncat")) {
                                it.arguments += destArgAtCall
                                it.arguments += newReference(param.name)
                                it.arguments += newLiteral(3, objectType("int"))
                            }
                        block.statements += call

                        laterRead = newReference("dest")
                        laterRead.prevDFG += call
                        block.statements += newReturn { it.returnValue = laterRead }
                    }
            }
        }

        val pattern = laterRead.evaluateCString()
        assertIs<StringPattern.Concat>(pattern)
        val appended = pattern.parts.last()
        assertIs<StringPattern.Unknown>(appended)
        val length = de.fraunhofer.aisec.cpg.analysis.string.lengthOf(appended)
        assertIs<de.fraunhofer.aisec.cpg.analysis.abstracteval.LatticeInterval.Bounded>(length)
        assertEquals(
            de.fraunhofer.aisec.cpg.analysis.abstracteval.LatticeInterval.Bound.Value(3),
            length.upper,
        )
    }

    /**
     * `snprintf(buf, 10, "%s-%s", "a", "b")` where a *later read of `buf`* resolves back to the
     * `snprintf` call as its def-site - mirroring [testStrcpyOverwritesOldValue]'s/
     * [testStrcatDependsOnOldValue]'s manually-constructed `prevDFG` pattern. This is the *primary,
     * intended* usage of [CStringOperationHandler]'s `snprintf` support: see
     * [CStringOperationHandler]'s KDoc ("`snprintf`'s return value vs. `buf`'s value") for why
     * evaluating the `Call` node's own expression value directly (as [testSnprintfExact] below
     * does) is a *different*, and per real C semantics *incorrect*, scenario - real `snprintf`
     * returns an `int` count, not the formatted string.
     */
    @Test
    fun testSnprintfDefSiteExact() {
        lateinit var laterRead: Reference
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.body =
                    newBlock(enterScope = true) { block ->
                        val call =
                            newCall(newReference("snprintf")) {
                                it.arguments += newReference("bufArg")
                                it.arguments += newLiteral(10, objectType("size_t"))
                                it.arguments += newLiteral("%s-%s", objectType("char*"))
                                it.arguments += newLiteral("a", objectType("char*"))
                                it.arguments += newLiteral("b", objectType("char*"))
                            }
                        block.statements += call

                        laterRead = newReference("buf")
                        laterRead.prevDFG += call
                        block.statements += newReturn { it.returnValue = laterRead }
                    }
            }
        }

        val pattern = laterRead.evaluateCString()
        assertEquals(const("a-b"), pattern)
    }

    /**
     * `snprintf(buf, 4, "%s-%s", "aaaa", "bbbb")` via a later read of `buf` (see
     * [testSnprintfDefSiteExact]): the logical, unbounded formatted result (`"aaaa-bbbb"`) is
     * longer than `size - 1 == 3` characters, so the exact, truncated result (`"aaa"`) must be
     * produced, mirroring real `snprintf` truncation semantics.
     */
    @Test
    fun testSnprintfDefSiteTruncatesExactConstant() {
        lateinit var laterRead: Reference
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.body =
                    newBlock(enterScope = true) { block ->
                        val call =
                            newCall(newReference("snprintf")) {
                                it.arguments += newReference("bufArg")
                                it.arguments += newLiteral(4, objectType("size_t"))
                                it.arguments += newLiteral("%s-%s", objectType("char*"))
                                it.arguments += newLiteral("aaaa", objectType("char*"))
                                it.arguments += newLiteral("bbbb", objectType("char*"))
                            }
                        block.statements += call

                        laterRead = newReference("buf")
                        laterRead.prevDFG += call
                        block.statements += newReturn { it.returnValue = laterRead }
                    }
            }
        }

        val pattern = laterRead.evaluateCString()
        assertEquals(const("aaa"), pattern)
    }

    /**
     * `snprintf(buf, size, "%s", x)` via a later read of `buf` (see [testSnprintfDefSiteExact]),
     * where `x` is a non-constant parameter and `size` is a known constant: the result cannot be
     * computed exactly, but its length must still be bounded above by `size - 1`.
     */
    @Test
    fun testSnprintfDefSiteBoundsLengthWhenNonConstant() {
        lateinit var laterRead: Reference
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                val param = newParameter("x", objectType("char*"), holder = func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        val call =
                            newCall(newReference("snprintf")) {
                                it.arguments += newReference("bufArg")
                                it.arguments += newLiteral(8, objectType("size_t"))
                                it.arguments += newLiteral("%s", objectType("char*"))
                                it.arguments += newReference(param.name)
                            }
                        block.statements += call

                        laterRead = newReference("buf")
                        laterRead.prevDFG += call
                        block.statements += newReturn { it.returnValue = laterRead }
                    }
            }
        }

        val pattern = laterRead.evaluateCString()
        assertIs<StringPattern.Unknown>(pattern)
        val length = pattern.length
        assertIs<de.fraunhofer.aisec.cpg.analysis.abstracteval.LatticeInterval.Bounded>(length)
        assertEquals(
            de.fraunhofer.aisec.cpg.analysis.abstracteval.LatticeInterval.Bound.Value(7),
            length.upper,
        )
    }

    /**
     * `snprintf(buf, size, "%s%%%s", "a", "b")` via a later read of `buf` (see
     * [testSnprintfDefSiteExact]) - regression test mirroring
     * `JvmStringOperationHandlerTest.testFormatPercentNDoesNotShiftArgumentIndex`: `%%` must not
     * consume a positional argument, so the second `%s` must still resolve to `"b"`, not miss it or
     * shift onto a nonexistent third argument.
     */
    @Test
    fun testSnprintfDefSitePercentDoesNotShiftArgumentIndex() {
        lateinit var laterRead: Reference
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.body =
                    newBlock(enterScope = true) { block ->
                        val call =
                            newCall(newReference("snprintf")) {
                                it.arguments += newReference("bufArg")
                                it.arguments += newLiteral(100, objectType("size_t"))
                                it.arguments += newLiteral("%s%%%s", objectType("char*"))
                                it.arguments += newLiteral("a", objectType("char*"))
                                it.arguments += newLiteral("b", objectType("char*"))
                            }
                        block.statements += call

                        laterRead = newReference("buf")
                        laterRead.prevDFG += call
                        block.statements += newReturn { it.returnValue = laterRead }
                    }
            }
        }

        val pattern = laterRead.evaluateCString()
        assertEquals(const("a%b"), pattern)
    }

    /**
     * `snprintf(buf, size, "%d%n%d", 1, &count, 2)` via a later read of `buf` (see
     * [testSnprintfDefSiteExact]) - regression test for C's `%n`: unlike Java's argument-less `%n`,
     * C's `%n` *does* consume a (pointer) argument - it just contributes no characters to the
     * output. The second `%d` must resolve to `2` (the third vararg), not to `&count` (the second
     * vararg, which `%n` consumes without stringifying).
     */
    @Test
    fun testSnprintfDefSitePercentNConsumesArgumentButNoOutput() {
        lateinit var laterRead: Reference
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.body =
                    newBlock(enterScope = true) { block ->
                        val call =
                            newCall(newReference("snprintf")) {
                                it.arguments += newReference("bufArg")
                                it.arguments += newLiteral(100, objectType("size_t"))
                                it.arguments += newLiteral("%d%n%d", objectType("char*"))
                                it.arguments += newLiteral(1, objectType("int"))
                                it.arguments += newReference("count")
                                it.arguments += newLiteral(2, objectType("int"))
                            }
                        block.statements += call

                        laterRead = newReference("buf")
                        laterRead.prevDFG += call
                        block.statements += newReturn { it.returnValue = laterRead }
                    }
            }
        }

        val pattern = laterRead.evaluateCString()
        assertEquals(const("12"), pattern)
    }

    /**
     * `snprintf(buf, 10, "%s-%s", "a", "b")` with all-constant arguments resolves exactly.
     *
     * This evaluates the `Call` node's own expression value directly, *not* a later read of `buf`
     * via its def-site - per [CStringOperationHandler]'s KDoc, this is the accepted-as-limited case
     * (real `snprintf` returns an `int` count here, not this formatted string); kept passing for
     * regression coverage of the handler's formatting logic, but [testSnprintfDefSiteExact] above
     * is the primary, semantically-correct test for this behaviour.
     */
    @Test
    fun testSnprintfExact() {
        lateinit var ret: de.fraunhofer.aisec.cpg.graph.expressions.Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            r.returnValue =
                                newCall(newReference("snprintf")) {
                                    it.arguments += newReference("buf")
                                    it.arguments += newLiteral(10, objectType("size_t"))
                                    it.arguments += newLiteral("%s-%s", objectType("char*"))
                                    it.arguments += newLiteral("a", objectType("char*"))
                                    it.arguments += newLiteral("b", objectType("char*"))
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateCString()
        assertEquals(const("a-b"), pattern)
    }

    /**
     * `snprintf(buf, 4, "%s-%s", "aaaa", "bbbb")` - the logical, unbounded formatted result
     * (`"aaaa-bbbb"`) is longer than `size - 1 == 3` characters, so the exact, truncated result
     * (`"aaa"`) must be produced, mirroring real `snprintf` truncation semantics.
     *
     * Evaluates the `Call` node's own expression value directly - the accepted-as-limited case, see
     * [testSnprintfExact]'s KDoc; [testSnprintfDefSiteTruncatesExactConstant] is primary.
     */
    @Test
    fun testSnprintfTruncatesExactConstant() {
        lateinit var ret: de.fraunhofer.aisec.cpg.graph.expressions.Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            r.returnValue =
                                newCall(newReference("snprintf")) {
                                    it.arguments += newReference("buf")
                                    it.arguments += newLiteral(4, objectType("size_t"))
                                    it.arguments += newLiteral("%s-%s", objectType("char*"))
                                    it.arguments += newLiteral("aaaa", objectType("char*"))
                                    it.arguments += newLiteral("bbbb", objectType("char*"))
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateCString()
        assertEquals(const("aaa"), pattern)
    }

    /**
     * `snprintf(buf, size, "%s", x)` where `x` is a non-constant parameter and `size` is a known
     * constant: the result cannot be computed exactly, but its length must still be bounded above
     * by `size - 1`.
     *
     * Evaluates the `Call` node's own expression value directly - the accepted-as-limited case, see
     * [testSnprintfExact]'s KDoc; [testSnprintfDefSiteBoundsLengthWhenNonConstant] is primary.
     */
    @Test
    fun testSnprintfBoundsLengthWhenNonConstant() {
        lateinit var ret: de.fraunhofer.aisec.cpg.graph.expressions.Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                val param = newParameter("x", objectType("char*"), holder = func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            r.returnValue =
                                newCall(newReference("snprintf")) {
                                    it.arguments += newReference("buf")
                                    it.arguments += newLiteral(8, objectType("size_t"))
                                    it.arguments += newLiteral("%s", objectType("char*"))
                                    it.arguments += newReference(param.name)
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateCString()
        assertIs<StringPattern.Unknown>(pattern)
        val length = pattern.length
        assertIs<de.fraunhofer.aisec.cpg.analysis.abstracteval.LatticeInterval.Bounded>(length)
        assertEquals(
            de.fraunhofer.aisec.cpg.analysis.abstracteval.LatticeInterval.Bound.Value(7),
            length.upper,
        )
    }

    /**
     * `snprintf(buf, size, "%s%%%s", "a", "b")` regression test mirroring
     * `JvmStringOperationHandlerTest.testFormatPercentNDoesNotShiftArgumentIndex`: `%%` must not
     * consume a positional argument, so the second `%s` must still resolve to `"b"`, not miss it or
     * shift onto a nonexistent third argument.
     *
     * Evaluates the `Call` node's own expression value directly - the accepted-as-limited case, see
     * [testSnprintfExact]'s KDoc; [testSnprintfDefSitePercentDoesNotShiftArgumentIndex] is primary.
     */
    @Test
    fun testSnprintfPercentDoesNotShiftArgumentIndex() {
        lateinit var ret: de.fraunhofer.aisec.cpg.graph.expressions.Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            r.returnValue =
                                newCall(newReference("snprintf")) {
                                    it.arguments += newReference("buf")
                                    it.arguments += newLiteral(100, objectType("size_t"))
                                    it.arguments += newLiteral("%s%%%s", objectType("char*"))
                                    it.arguments += newLiteral("a", objectType("char*"))
                                    it.arguments += newLiteral("b", objectType("char*"))
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateCString()
        assertEquals(const("a%b"), pattern)
    }

    /**
     * `snprintf(buf, size, "%d%n%d", 1, &count, 2)` regression test for C's `%n`: unlike Java's
     * argument-less `%n`, C's `%n` *does* consume a (pointer) argument - it just contributes no
     * characters to the output. The second `%d` must resolve to `2` (the third vararg), not to
     * `&count` (the second vararg, which `%n` consumes without stringifying).
     *
     * Evaluates the `Call` node's own expression value directly - the accepted-as-limited case, see
     * [testSnprintfExact]'s KDoc; [testSnprintfDefSitePercentNConsumesArgumentButNoOutput] is
     * primary.
     */
    @Test
    fun testSnprintfPercentNConsumesArgumentButNoOutput() {
        lateinit var ret: de.fraunhofer.aisec.cpg.graph.expressions.Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            r.returnValue =
                                newCall(newReference("snprintf")) {
                                    it.arguments += newReference("buf")
                                    it.arguments += newLiteral(100, objectType("size_t"))
                                    it.arguments += newLiteral("%d%n%d", objectType("char*"))
                                    it.arguments += newLiteral(1, objectType("int"))
                                    it.arguments += newReference("count")
                                    it.arguments += newLiteral(2, objectType("int"))
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluateCString()
        assertEquals(const("12"), pattern)
    }
}
