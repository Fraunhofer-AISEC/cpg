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
package de.fraunhofer.aisec.cpg.enhancements

import de.fraunhofer.aisec.cpg.passes.inference.DFGFunctionSummaries
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DFGFunctionSummariesTest {

    @Test
    fun testParsingFile() {
        val jsonSummaries =
            DFGFunctionSummaries.fromFiles(listOf(File("src/test/resources/function-dfg.json")))

        assertTrue(jsonSummaries.functionToDFGEntryMap.isNotEmpty())
        val yamlSummaries =
            DFGFunctionSummaries.fromFiles(listOf(File("src/test/resources/function-dfg.yml")))

        assertTrue(yamlSummaries.functionToDFGEntryMap.isNotEmpty())

        assertEquals(jsonSummaries.functionToDFGEntryMap, yamlSummaries.functionToDFGEntryMap)
    }
}
