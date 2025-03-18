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
package de.fraunhofer.aisec.cpg.frontends.python

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.test.analyze
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LoadIncludesTest {

    @Test
    fun testLoadIncludes() {
        val topLevel = Path.of("src", "test", "resources", "python", "load_includes", "example")
        val result =
            analyze(listOf(topLevel.resolve("program.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
                it.loadIncludes(true)
                it.includePath(
                    Path.of("src", "test", "resources", "python", "load_includes", "stdlib")
                )
            }

        assertEquals(result.finalCtx.importedSources?.size, 3)

        val stdlib = result.components("stdlib").flatMap { it.allChildren<Node>() }

        val jsonloads = result.calls("json.loads").firstOrNull()
        assertNotNull(jsonloads)
        assertTrue(jsonloads.invokes.all { !it.isInferred && stdlib.contains(it) })

        val jsonEncoder = result.memberExpressions("item_separator").firstOrNull()
        assertNotNull(jsonEncoder)
        assertTrue(jsonEncoder.refersTo?.let { !it.isInferred && stdlib.contains(it) } == true)

        val str = result.calls("str").firstOrNull()
        assertNotNull(str)
        assertTrue(str.invokes.all { !it.isInferred && stdlib.contains(it) })
    }
}
