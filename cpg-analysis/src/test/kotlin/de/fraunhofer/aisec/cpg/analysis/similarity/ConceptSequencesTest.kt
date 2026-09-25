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
package de.fraunhofer.aisec.cpg.analysis.similarity

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.singleTranslationUnit
import de.fraunhofer.aisec.cpg.frontends.testFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.types.FunctionType.Companion.computeType
import de.fraunhofer.aisec.cpg.passes.ControlDependenceGraphPass
import de.fraunhofer.aisec.cpg.passes.ProgramDependenceGraphPass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests [collectConceptSequences] directly on small, hand-built CPGs (via the synthetic
 * [TestLanguage], as in [de.fraunhofer.aisec.cpg.passes.ProgramDependenceGraphPassTest]) so the DFS
 * traversal logic itself -- depth limiting, cycle safety, branching, distinctness -- can be
 * verified without needing a real language frontend or concept overlays.
 *
 * Assertions here deliberately check structural *properties* (does it terminate, does branching
 * show up as multiple sequences, is the union-of-starts identity respected) rather than exact
 * expected sequences: the precise DFG wiring that DFGPass produces for a given AST shape is an
 * implementation detail of that pass, not of [collectConceptSequences].
 */
class ConceptSequencesTest {

    /** A call's local name, used as a stand-in "concept" predicate throughout this test. */
    private val callName: (de.fraunhofer.aisec.cpg.graph.Node) -> String? = { node ->
        (node as? Call)?.name?.localName
    }

    // maxDepth of 0 only looks at the start node itself
    @Test
    fun maxDepthZeroOnlyLooksAtStart() {
        val result = buildSequentialFunction()
        val getSecret = requireNotNull(result.functions["main"]?.calls?.get("get_secret"))

        val sequences =
            collectConceptSequences(setOf(getSecret), maxDepth = 0, predicate = callName)

        // depth 0 means: evaluate the predicate on the start node, but never look at its
        // neighbors -- so there is exactly one branch, containing only the start node's own hit.
        assertEquals(setOf(listOf("get_secret")), sequences)
    }

    // straight-line dataflow reaches the downstream calls within depth
    @Test
    fun straightLineReachesDownstreamCalls() {
        val result = buildSequentialFunction()
        val getSecret = requireNotNull(result.functions["main"]?.calls?.get("get_secret"))

        val sequences =
            collectConceptSequences(setOf(getSecret), maxDepth = 10, predicate = callName)

        assertTrue(sequences.isNotEmpty())
        assertTrue(
            sequences.any { "encrypt" in it },
            "expected some branch to reach encrypt(): $sequences",
        )
        assertTrue(sequences.any { "log" in it }, "expected some branch to reach log(): $sequences")
    }

    // branching produces multiple distinct sequences
    @Test
    fun branchingProducesMultipleSequences() {
        val result = buildIfElseFunction()
        val getSecret = requireNotNull(result.functions["main"]?.calls?.get("get_secret"))

        val sequences =
            collectConceptSequences(setOf(getSecret), maxDepth = 10, predicate = callName)

        assertTrue(
            sequences.size >= 2,
            "Expected at least two distinct branches (then/else), got $sequences",
        )
        assertTrue(sequences.any { "encrypt" in it })
        assertTrue(sequences.any { "log" in it })
    }

    // loops terminate instead of recursing forever
    @Test
    fun loopsTerminate() {
        val result = buildWhileLoopFunction()
        val getSecret = requireNotNull(result.functions["main"]?.calls?.get("get_secret"))

        // A large maxDepth on a cyclic PDG (the loop body depends on the loop condition, which
        // depends on the loop body) must still terminate -- this is the point of the test.
        val sequences =
            collectConceptSequences(setOf(getSecret), maxDepth = 50, predicate = callName)

        assertTrue(sequences.isNotEmpty())
    }

    // multiple start nodes are unioned
    @Test
    fun multipleStartNodesAreUnioned() {
        val result = buildIfElseFunction()
        val main = requireNotNull(result.functions["main"])
        val encryptCall = requireNotNull(main.calls["encrypt"])
        val logCall = requireNotNull(main.calls["log"])

        val fromEncrypt =
            collectConceptSequences(setOf(encryptCall), maxDepth = 10, predicate = callName)
        val fromLog = collectConceptSequences(setOf(logCall), maxDepth = 10, predicate = callName)
        val union =
            collectConceptSequences(
                setOf(encryptCall, logCall),
                maxDepth = 10,
                predicate = callName,
            )

        assertEquals(fromEncrypt + fromLog, union)
    }

    companion object {
        /** `main() { i = get_secret(); e = encrypt(i); log(e); return e; }` */
        private fun buildSequentialFunction(): TranslationResult =
            testFrontend {
                    it.registerLanguage<TestLanguage>()
                    it.defaultPasses()
                    it.registerPass<ControlDependenceGraphPass>()
                    it.registerPass<ProgramDependenceGraphPass>()
                }
                .build {
                    singleTranslationUnit("sequential.cpp") { tu ->
                        newFunction("main", holder = tu, enterScope = true) { func ->
                            func.returnTypes = listOf(objectType("int"))
                            func.type = computeType(func)

                            func.body =
                                newBlock(enterScope = true) { block ->
                                    block.statements += newDeclarationStatement { declStmt ->
                                        newVariable("i", objectType("int"), holder = declStmt) {
                                            it.initializer = newCall(newReference("get_secret"))
                                        }
                                    }
                                    block.statements += newDeclarationStatement { declStmt ->
                                        newVariable("e", objectType("int"), holder = declStmt) {
                                            it.initializer =
                                                newCall(newReference("encrypt")) {
                                                    it.arguments += newReference("i")
                                                }
                                        }
                                    }
                                    block.statements +=
                                        newCall(newReference("log")) {
                                            it.arguments += newReference("e")
                                        }
                                    block.statements += newReturn {
                                        it.returnValue = newReference("e")
                                    }
                                }
                        }
                    }
                }

        /** `main() { s = get_secret(); if (s) { encrypt(s); } else { log(s); } return s; }` */
        private fun buildIfElseFunction(): TranslationResult =
            testFrontend {
                    it.registerLanguage<TestLanguage>()
                    it.defaultPasses()
                    it.registerPass<ControlDependenceGraphPass>()
                    it.registerPass<ProgramDependenceGraphPass>()
                }
                .build {
                    singleTranslationUnit("ifelse.cpp") { tu ->
                        newFunction("main", holder = tu, enterScope = true) { func ->
                            func.returnTypes = listOf(objectType("int"))
                            func.type = computeType(func)

                            func.body =
                                newBlock(enterScope = true) { block ->
                                    block.statements += newDeclarationStatement { declStmt ->
                                        newVariable("s", objectType("int"), holder = declStmt) {
                                            it.initializer = newCall(newReference("get_secret"))
                                        }
                                    }

                                    block.statements += newIfElse { ifElse ->
                                        ifElse.condition = newReference("s")
                                        ifElse.thenStatement =
                                            newBlock(enterScope = true) { thenBlock ->
                                                thenBlock.statements +=
                                                    newCall(newReference("encrypt")) {
                                                        it.arguments += newReference("s")
                                                    }
                                            }
                                        ifElse.elseStatement =
                                            newBlock(enterScope = true) { elseBlock ->
                                                elseBlock.statements +=
                                                    newCall(newReference("log")) {
                                                        it.arguments += newReference("s")
                                                    }
                                            }
                                    }

                                    block.statements += newReturn {
                                        it.returnValue = newReference("s")
                                    }
                                }
                        }
                    }
                }

        /** `main() { i = get_secret(); while (i) { log(i); } return i; }` */
        private fun buildWhileLoopFunction(): TranslationResult =
            testFrontend {
                    it.registerLanguage<TestLanguage>()
                    it.defaultPasses()
                    it.registerPass<ControlDependenceGraphPass>()
                    it.registerPass<ProgramDependenceGraphPass>()
                }
                .build {
                    singleTranslationUnit("loop.cpp") { tu ->
                        newFunction("main", holder = tu, enterScope = true) { func ->
                            func.returnTypes = listOf(objectType("int"))
                            func.type = computeType(func)

                            func.body =
                                newBlock(enterScope = true) { block ->
                                    block.statements += newDeclarationStatement { declStmt ->
                                        newVariable("i", objectType("int"), holder = declStmt) {
                                            it.initializer = newCall(newReference("get_secret"))
                                        }
                                    }

                                    block.statements +=
                                        newWhile(enterScope = true) { w ->
                                            w.condition = newReference("i")
                                            w.statement =
                                                newBlock(enterScope = true) { loopBody ->
                                                    loopBody.statements +=
                                                        newCall(newReference("log")) {
                                                            it.arguments += newReference("i")
                                                        }
                                                }
                                        }

                                    block.statements += newReturn {
                                        it.returnValue = newReference("i")
                                    }
                                }
                        }
                    }
                }
    }
}
