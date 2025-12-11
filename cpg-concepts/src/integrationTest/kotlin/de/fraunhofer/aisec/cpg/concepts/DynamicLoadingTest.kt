/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.concepts

import de.fraunhofer.aisec.cpg.evaluation.MultiValueEvaluator
import de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.concepts.arch.POSIX
import de.fraunhofer.aisec.cpg.graph.concepts.memory.LoadLibrary
import de.fraunhofer.aisec.cpg.graph.concepts.memory.LoadSymbol
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.passes.concepts.memory.cxx.CXXDynamicLoadingPass
import de.fraunhofer.aisec.cpg.test.analyze
import de.fraunhofer.aisec.cpg.test.assertInvokes
import java.io.File
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class DynamicLoadingTest {

    // TODO for merge
    @Ignore
    @Test
    fun testCXXPOSIX() {
        val topLevel = File("src/integrationTest/resources/c")
        val result =
            analyze(listOf(), topLevel.toPath(), true) {
                it.registerLanguage<CLanguage>()
                it.registerPass<CXXDynamicLoadingPass>()
                it.softwareComponents(
                    mutableMapOf(
                        "main" to listOf(topLevel.resolve("main")),
                        "libexample" to listOf(topLevel.resolve("libexample")),
                    )
                )
            }
        assertNotNull(result)

        val libExample = result.components["libexample"]
        assertNotNull(libExample)

        val myFunc = result.functions["myfunc"]
        assertNotNull(myFunc)

        val lib = result.variables["lib"]
        assertNotNull(lib)

        val path =
            lib.followPrevDFG { it is CallExpression && it.overlays.any { it is LoadLibrary } }
        assertNotNull(path)

        val loadLibrary =
            path.nodes.lastOrNull()?.operationNodes?.filterIsInstance<LoadLibrary>()?.singleOrNull()
        assertNotNull(loadLibrary)
        assertIs<POSIX>(loadLibrary.os)
        assertEquals(
            libExample,
            loadLibrary.what,
            "\"what\" of the LoadLibrary should be the libexample component",
        )

        val bCall = result.calls["b"]
        assertNotNull(bCall)
        assertInvokes(bCall, myFunc, "The call to b should invoke myFunc")

        val dlSym = result.calls["dlsym"]
        assertNotNull(dlSym)

        val loadSymbol =
            dlSym.operationNodes.filterIsInstance<LoadSymbol<FunctionDeclaration>>().singleOrNull()
        assertNotNull(loadSymbol)
        assertIs<POSIX>(loadSymbol.os)
        assertEquals(myFunc, loadSymbol.what, "\"what\" of the LoadSymbol should be myFunc")

        val c = result.refs["c"]
        assertNotNull(c)

        // The multi-evaluator contains too many values for now, since we just stupidly take all DFG
        // edges into the function declaration, we need to instead look at the calling context,
        // similar to what we do with the dataflow queries.
        var values = c.evaluate(MultiValueEvaluator())
        assertIs<Set<*>>(values)
        assertContains(values, 2)

        val a = result.refs["a"]
        assertNotNull(a)

        values = a.evaluate(MultiValueEvaluator())
        assertIs<Set<*>>(values)
        assertContains(values, 3)
    }

    @Test
    fun testCXXWin32() {
        val topLevel = File("src/integrationTest/resources/c")
        val result =
            analyze(listOf(), topLevel.toPath(), true) {
                it.registerLanguage<CLanguage>()
                it.registerPass<CXXDynamicLoadingPass>()
                it.softwareComponents(
                    mutableMapOf(
                        "winmain" to listOf(topLevel.resolve("winmain")),
                        "winexample" to listOf(topLevel.resolve("winexample")),
                    )
                )
            }
        assertNotNull(result)

        val winexample = result.components["winexample"]
        assertNotNull(winexample)

        val dllMain = result.functions["DllMain"]
        assertNotNull(dllMain)

        val winmain = result.components["winmain"]
        assertNotNull(winmain)

        val loadLibrary = winmain.operationNodes.filterIsInstance<LoadLibrary>().singleOrNull()
        assertNotNull(loadLibrary)
        assertEquals(
            winexample,
            loadLibrary.what,
            "\"what\" of the LoadLibrary should be the winexample component",
        )
        assertEquals(
            dllMain,
            loadLibrary.entryPoints.singleOrNull()?.underlyingNode,
            "DllMain should be the only DLL entry point",
        )
    }
}
