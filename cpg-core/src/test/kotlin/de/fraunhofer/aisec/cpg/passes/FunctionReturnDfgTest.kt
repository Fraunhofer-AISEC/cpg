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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.frontends.singleTranslationUnit
import de.fraunhofer.aisec.cpg.frontends.testFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.expressions.Return
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * [DFGPass.handleCall] attaches arguments to parameters when neither [PointsToPass] nor
 * [ControlFlowSensitiveDFGPass] is registered, because those passes would otherwise do it.
 * [DFGPass.handleFunction] needs the same compensation in the outgoing direction: without it a
 * [Function] never receives the values it returns, a backward traversal from a call result reaches
 * the function and stops, and nothing flows out of a call interprocedurally.
 */
class FunctionReturnDfgTest {

    /** `int source() { return 42; }` */
    private fun buildGraph() =
        testFrontend(
                // Deliberately not defaultPasses(): that always registers PointsToPass or
                // ControlFlowSensitiveDFGPass, one of which supplies the return flow itself.
                // The compensation under test only applies when neither runs, which is how
                // consumers that register passes by hand are configured.
                TranslationConfiguration.builder()
                    .registerLanguage<TestLanguage>()
                    .registerPass<TypeHierarchyResolver>()
                    .registerPass<SymbolResolver>()
                    .registerPass<DFGPass>()
                    .registerPass<EvaluationOrderGraphPass>()
                    .registerPass<TypeResolver>()
                    .build()
            )
            .build {
                singleTranslationUnit("ret.cpp") { tu ->
                    newFunction("source", holder = tu, enterScope = true) { func ->
                        func.returnTypes = listOf(objectType("int"))
                        func.body =
                            newBlock(enterScope = true) { block ->
                                block.statements += newReturn {
                                    it.returnValue = newLiteral(42, objectType("int"))
                                }
                            }
                    }
                }
            }

    @Test
    fun functionReceivesTheValuesItReturns() {
        val result = buildGraph()
        val source = result.functions["source"]
        assertNotNull(source)

        val ret = source.allChildren<Return>().singleOrNull()
        assertNotNull(ret, "the fixture should contain exactly one return")

        assertTrue(
            ret in source.prevDFG,
            "the function must receive the value of its return statement, otherwise a backward " +
                "traversal from a call result stops at the function",
        )
    }

    @Test
    fun aReturnIsAttributedOnlyToTheFunctionEnclosingIt() {
        val result = buildGraph()
        // A return inside a nested function literal belongs to that literal, so every return
        // wired into a function must be one that function lexically encloses.
        result.functions.forEach { fn ->
            fn.prevDFG.filterIsInstance<Return>().forEach { ret ->
                assertEquals(
                    fn,
                    ret.firstParentOrNull<Function>(),
                    "a return was wired to a function that does not enclose it",
                )
            }
        }
    }
}
