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
package de.fraunhofer.aisec.cpg

import de.fraunhofer.aisec.cpg.frontends.ForeignFunctionInterface
import de.fraunhofer.aisec.cpg.frontends.cxx.CLanguage
import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.graph.scopes.Symbol
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.persistence.createJsonGraph
import de.fraunhofer.aisec.cpg.persistence.persistJson
import de.fraunhofer.aisec.cpg.test.GraphExamples
import de.fraunhofer.aisec.cpg.test.analyze
import kotlin.io.path.Path
import kotlin.io.path.createTempFile
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

/**
 * A class for integration tests. They depend on the C++ frontend, so we classify them as an
 * integration test. This might be replaced with a language-neutral test at some point.
 */
class IntegrationTest {

    class JavaCFFI :
        ForeignFunctionInterface<JavaLanguage, CLanguage>(JavaLanguage(), CLanguage()) {
        override fun mapSymbol(from: Symbol): Symbol {
            val isNativeAdd =
                from == "nativeAdd" ||
                    from.endsWith(".nativeAdd") ||
                    from.contains(".nativeAdd(") ||
                    from.contains("nativeAdd(")

            return if (isNativeAdd) {
                "Java_MyClass_nativeAdd"
            } else {
                from
            }
        }

        override fun mapType(from: Type): Type {
            return to.builtInTypes[from.name.toString()] ?: from
        }
    }

    //
    // --report-output="study/python_analysis_output.py" --path-suffix="-python"

    @Test
    fun testForeignFunctionInterface() {
        val topLevel = Path("src/integrationTest/resources/foreignFunctionInterface")
        val result =
            analyze(
                listOf(
                    topLevel.resolve("MyClass.java").toFile(),
                    topLevel.resolve("native.c").toFile(),
                ),
                topLevel,
                usePasses = true,
            ) {
                it.registerLanguage<CLanguage>()
                it.registerLanguage<JavaLanguage>()
                it.registerLanguageInterface<JavaCFFI>()
            }
        assertNotNull(result)

        val useNativeAdd = result.functions["useNativeAdd"]
        assertNotNull(useNativeAdd)

        val nativeCall = useNativeAdd.calls["nativeAdd"]
        assertNotNull(nativeCall)

        val nativeImplementation = result.functions["Java_MyClass_nativeAdd"]
        assertNotNull(nativeImplementation)

        assertTrue(nativeCall.invokes.contains(nativeImplementation))
    }

    @Test
    fun testBuildJsonGraph() {
        val translationResult = GraphExamples.getInitializerListExprDFG()

        assertEquals(2, translationResult.functions.size)

        val graph = translationResult.createJsonGraph()
        val connectToFuncDel =
            graph.nodes.firstOrNull {
                it.labels.contains(Function::class.simpleName) && it.properties["name"] == "foo"
            }
        assertNotNull(connectToFuncDel)

        val connectToCallExpr =
            graph.nodes.firstOrNull {
                it.labels.contains(Call::class.simpleName) && it.properties["name"] == "foo"
            }
        assertNotNull(connectToCallExpr)

        val invokesEdge =
            graph.edges.firstOrNull {
                it.type == "INVOKES" &&
                    it.startNode == connectToCallExpr.id &&
                    it.endNode == connectToFuncDel.id
            }
        assertNotNull(invokesEdge)
    }

    @Test
    fun testExportToJson() {
        val translationResult = GraphExamples.getInitializerListExprDFG()
        assertEquals(2, translationResult.functions.size)
        val path = createTempFile().toFile()
        translationResult.persistJson(path)
        assert(path.length() > 0)
    }
}
