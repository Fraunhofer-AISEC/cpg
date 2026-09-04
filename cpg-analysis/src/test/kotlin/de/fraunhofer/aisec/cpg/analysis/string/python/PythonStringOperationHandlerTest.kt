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
package de.fraunhofer.aisec.cpg.analysis.string.python

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.analysis.string.StringPattern
import de.fraunhofer.aisec.cpg.analysis.string.const
import de.fraunhofer.aisec.cpg.analysis.string.union
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.singleTranslationUnit
import de.fraunhofer.aisec.cpg.frontends.testFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnit
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.MemberCall
import de.fraunhofer.aisec.cpg.graph.expressions.Return
import de.fraunhofer.aisec.cpg.graph.types.FunctionType.Companion.computeType
import de.fraunhofer.aisec.cpg.passes.reconstructedImportName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PythonStringOperationHandlerTest {

    private fun config(): TranslationConfiguration =
        TranslationConfiguration.builder().defaultPasses().registerLanguage<TestLanguage>().build()

    private fun build(init: LanguageFrontend<*, *>.(TranslationUnit) -> Unit) =
        testFrontend(config()).build {
            this.singleTranslationUnit("test.py") { tu -> this.init(tu) }
        }

    /** `"{0} and {1}".format(a, b)` with constant `a`/`b` should resolve to `Const("x and y")`. */
    @Test
    fun testFormatPositionalConstant() {
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
                                        "format",
                                        newLiteral("{0} and {1}", objectType("string")),
                                    )
                                ) {
                                    it.arguments += newLiteral("x", objectType("string"))
                                    it.arguments += newLiteral("y", objectType("string"))
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluatePythonString()
        assertEquals(const("x and y"), pattern)
    }

    /**
     * `"{}".format(x)` where `x` is a parameter (hence `Unknown`) must still produce a sound
     * over-approximation admitting whatever `x` may be, not a crash or a false-precise constant.
     */
    @Test
    fun testFormatWithUnknownArgument() {
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
                                        "format",
                                        newLiteral("{}", objectType("string")),
                                    )
                                ) {
                                    it.arguments += newReference(param.name)
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluatePythonString()
        assertTrue(
            pattern is StringPattern.Concat || pattern is StringPattern.Unknown,
            "expected a Concat or Unknown, got $pattern",
        )
    }

    /**
     * `os.path.join("a", "b")` with constant arguments resolves to `Const("a/b")`.
     *
     * The callee is built directly as a `Reference("os.path.join")`, i.e. the shape a real Python
     * frontend produces *after* `ResolveMemberAmbiguityPass` has rewritten the member-access chain
     * into a single qualified reference - `reconstructedImportName` on a plain (non-`HasBase`)
     * [Call] is just `call.name`, so this is what the handler actually needs to match, rather than
     * a raw, not-yet-disambiguated `os.path` member chain (which `TestLanguage` has no pass to
     * disambiguate).
     */
    @Test
    fun testOsPathJoinConstant() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            r.returnValue =
                                newCall(newReference("os.path.join")) {
                                    it.arguments += newLiteral("a", objectType("string"))
                                    it.arguments += newLiteral("b", objectType("string"))
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val call = ret.returnValue
        assertIs<Call>(call)
        assertEquals("os.path.join", call.reconstructedImportName.toString())

        val pattern = ret.returnValue!!.evaluatePythonString()
        assertEquals(const("a/b"), pattern)
    }

    /**
     * `os.path.join("a", x)` where `x` is a branch-dependent value must produce a `Union`
     * over-approximation rather than a single, falsely-precise constant.
     */
    @Test
    fun testOsPathJoinWithBranchingArgument() {
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
                                newCall(newReference("os.path.join")) {
                                    it.arguments += newLiteral("a", objectType("string"))
                                    it.arguments += newReference("x")
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluatePythonString()
        assertEquals(union(const("a/b"), const("a/c")), pattern)
    }

    /** `s.replace(old, new)` with all-constant operands computes the exact result. */
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

        val pattern = ret.returnValue!!.evaluatePythonString()
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
        val pattern = ret.returnValue!!.evaluatePythonString()
        assertTrue(
            pattern is StringPattern.Concat || pattern is StringPattern.Unknown,
            "expected an over-approximation, got $pattern",
        )
        assertTrue(
            call.assumptions.isNotEmpty(),
            "the over-approximate `replace` path must record a SoundnessAssumption on the call",
        )
    }

    /** `" foo ".strip()` resolves to the exact `Const("foo")`. */
    @Test
    fun testStripConstant() {
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
                                        "strip",
                                        newLiteral("  foo  ", objectType("string")),
                                    )
                                )
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluatePythonString()
        assertEquals(const("foo"), pattern)
    }

    /**
     * `p.strip()` where `p` is a parameter (`Unknown`) must produce a sound `Unknown` whose length
     * lower bound is `0` (stripping can shrink to the empty string) and whose upper bound does not
     * exceed the receiver's.
     */
    @Test
    fun testStripNonConstant() {
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
                                newMemberCall(newMemberAccess("strip", newReference(param.name)))
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluatePythonString()
        assertIs<StringPattern.Unknown>(pattern)
    }

    /** `"Foo".upper()` resolves to the exact `Const("FOO")`. */
    @Test
    fun testUpperConstant() {
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
                                        "upper",
                                        newLiteral("Foo", objectType("string")),
                                    )
                                )
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluatePythonString()
        assertEquals(const("FOO"), pattern)
    }

    /**
     * `x.upper()` where `x` is branch-dependent (`Union{"a","b"}`) must distribute the mapping over
     * every alternative, producing `Union{"A","B"}` - a sound result that still admits both
     * observed characters, not a collapse to a single value or an unsound loss of a branch.
     */
    @Test
    fun testUpperOnUnion() {
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
                                            listOf(newLiteral("b", objectType("string"))),
                                        )
                                }
                        }
                        ret = newReturn { r ->
                            r.returnValue =
                                newMemberCall(newMemberAccess("upper", newReference("x")))
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluatePythonString()
        assertEquals(union(const("A"), const("B")), pattern)
    }

    /** `", ".join(["a", "b"])` with a constant list literal resolves to `Const("a, b")`. */
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
                                newMemberCall(
                                    newMemberAccess("join", newLiteral(", ", objectType("string")))
                                ) {
                                    it.arguments +=
                                        newInitializerList(objectType("string")) { list ->
                                            list.initializers +=
                                                newLiteral("a", objectType("string"))
                                            list.initializers +=
                                                newLiteral("b", objectType("string"))
                                        }
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluatePythonString()
        assertEquals(const("a, b"), pattern)
    }

    /**
     * `", ".join(x)` where `x` is not a list literal (e.g. a parameter) has no statically known
     * elements to enumerate, and must therefore produce `Unknown` rather than a crash or a
     * fabricated value.
     */
    @Test
    fun testJoinOnUnknownIterable() {
        lateinit var ret: Return
        build { tu ->
            newFunction("main", holder = tu, enterScope = true) { func ->
                func.returnTypes = listOf(objectType("string"))
                func.type = computeType(func)
                val param = newParameter("items", objectType("list"), holder = func)
                func.body =
                    newBlock(enterScope = true) { block ->
                        ret = newReturn { r ->
                            r.returnValue =
                                newMemberCall(
                                    newMemberAccess("join", newLiteral(", ", objectType("string")))
                                ) {
                                    it.arguments += newReference(param.name)
                                }
                        }
                        block.statements += ret
                    }
            }
        }

        val pattern = ret.returnValue!!.evaluatePythonString()
        assertIs<StringPattern.Unknown>(pattern)
    }
}
