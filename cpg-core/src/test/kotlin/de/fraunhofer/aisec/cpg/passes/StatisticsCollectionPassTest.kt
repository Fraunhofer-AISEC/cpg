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
package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.GraphExamples
import de.fraunhofer.aisec.cpg.graph.nodes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class StatisticsCollectionPassTest {
    @Test
    fun testCollection() {
        val result = GraphExamples.getCombinedVariableAndCallTest()
        result.benchmarkResults.print()

        val holder =
            result.benchmarks.firstOrNull {
                it.message == StatisticsCollectionPass.MEASUREMENT_HOLDER_MEASURING_NODES
            }
        assertNotNull(holder)

        val totalNodes = holder.measurements[StatisticsCollectionPass.MEASUREMENT_TOTAL_GRAPH_NODES]
        assertNotNull(totalNodes)
        assertEquals(result.nodes.size, totalNodes)
    }
}
