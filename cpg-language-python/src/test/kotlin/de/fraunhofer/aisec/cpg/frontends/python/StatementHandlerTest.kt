/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.frontends.python

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class StatementHandlerTest {

    @Test
    fun testPosOnlyArguments() {
        val topLevel = Path.of("src", "test", "resources", "python")
        val result =
            analyze(
                listOf(
                    topLevel.resolve("arguments.py").toFile(),
                ),
                topLevel,
                true
            ) {
                it.registerLanguage<PythonLanguage>()
            }
        assertNotNull(result)

        var myClass = result.records["MyClass"]
        assertNotNull(myClass)

        var func = result.functions["pos_only_and_args"]
        assertNotNull(func)

        val list = listOf("a", "b", "c")
        list.forEachIndexed { idx, name ->
            var param = func.parameterEdges.firstOrNull { it.end.name.localName == name }
            assertNotNull(param, "$name should not be empty")
            assertEquals(idx, param.getProperty(Properties.INDEX))
        }
    }
}
