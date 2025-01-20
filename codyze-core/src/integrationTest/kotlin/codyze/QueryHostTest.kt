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
package codyze

import de.fraunhofer.aisec.codyze.evalQuery
import de.fraunhofer.aisec.cpg.frontends.python.PythonLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.query.QueryTree
import de.fraunhofer.aisec.cpg.test.analyze
import java.io.File
import kotlin.io.path.Path
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.valueOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class QueryHostTest {
    @Test
    fun testQuery() {
        val topLevel = Path("src/integrationTest/resources")
        val result =
            analyze(listOf(topLevel.resolve("simple.py").toFile()), topLevel, true) {
                it.registerLanguage<PythonLanguage>()
            }
        val scriptResult = result.evalQuery(File("src/integrationTest/resources/simple.query.kts"))
        val evalResult = (scriptResult.valueOrNull()?.returnValue as? ResultValue.Value)?.value

        val queryTree = evalResult as? QueryTree<*>
        assertNotNull(queryTree)
        assertEquals(true, queryTree.value)
    }
}
