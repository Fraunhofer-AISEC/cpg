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

import de.fraunhofer.aisec.cpg.frontends.java.JavaLanguage
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage
import de.fraunhofer.aisec.cpg.graph.allChildren
import de.fraunhofer.aisec.cpg.graph.expressions.Return
import de.fraunhofer.aisec.cpg.graph.functions
import de.fraunhofer.aisec.cpg.test.analyze
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Confirms that [Node.evaluateString] automatically dispatches to the right
 * [StringOperationHandler] for real Python/Java frontend output, purely via
 * [StringOperationHandlerRegistry], with zero caller-side wiring - i.e. the actual end-to-end
 * behaviour [StringOperationHandlerRegistry] and its wiring into [StringEvaluator] were built for.
 * This lives in `integrationTest` (unlike
 * [de.fraunhofer.aisec.cpg.analysis.string.python .PythonStringOperationHandlerTest]/[de.fraunhofer.aisec.cpg.analysis.string.jvm
 * .JvmStringOperationHandlerTest], which use `TestLanguage` and so never exercise the registry's
 * language-name-based lookup) because it needs the real `PythonLanguage`/`JavaLanguage` frontends
 * on the classpath, which are only available there (see `cpg-analysis/build.gradle.kts`).
 */
class StringOperationHandlerRegistryIntegrationTest {

    @Test
    fun testAutomaticPythonDispatch() {
        val topLevel = Path("src/integrationTest/resources/registry/python")
        val result =
            analyze("py", topLevel, usePasses = true) { it.registerLanguage<PythonLanguage>() }
        val function = result.functions.firstOrNull { it.name.localName == "build_message" }
        assertNotNull(function)
        val ret = function.allChildren<Return> { true }.firstOrNull()
        assertNotNull(ret)
        val returnValue = ret.returnValue
        assertNotNull(returnValue)

        // No `operationHandlers` passed - the Python `str.format` handling can only have come from
        // StringOperationHandlerRegistry.forLanguage, keyed on PythonLanguage's simple class name.
        val pattern = returnValue.evaluateString()
        assertEquals(const("Hello, World!"), pattern)
    }

    @Test
    fun testAutomaticJavaDispatch() {
        val topLevel = Path("src/integrationTest/resources/registry/java")
        val result =
            analyze("java", topLevel, usePasses = true) { it.registerLanguage<JavaLanguage>() }
        val function = result.functions.firstOrNull { it.name.localName == "build" }
        assertNotNull(function)
        val ret = function.allChildren<Return> { true }.firstOrNull()
        assertNotNull(ret)
        val returnValue = ret.returnValue
        assertNotNull(returnValue)

        // No `operationHandlers` passed - the StringBuilder.append/toString handling can only have
        // come from StringOperationHandlerRegistry.forLanguage, keyed on JavaLanguage's simple
        // class name.
        val pattern = returnValue.evaluateString()
        assertEquals(const("Hello, World!"), pattern)
    }
}
