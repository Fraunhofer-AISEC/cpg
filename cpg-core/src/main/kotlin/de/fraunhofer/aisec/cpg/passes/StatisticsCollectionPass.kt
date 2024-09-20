/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.nodes
import de.fraunhofer.aisec.cpg.graph.problems
import de.fraunhofer.aisec.cpg.helpers.MeasurementHolder
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteLate

/**
 * A [Pass] collecting statistics for the graph. Currently, it collects the number of nodes and the
 * number of problem nodes (i.e., nodes where the translation failed for some reason).
 */
@ExecuteLate
class StatisticsCollectionPass(ctx: TranslationContext) : TranslationResultPass(ctx) {

    companion object {
        const val MEASUREMENT_TOTAL_GRAPH_NODES = "Total graph nodes"
        const val MEASUREMENT_TOTAL_PROBLEM_NODES = "Total problem nodes"

        const val MEASUREMENT_HOLDER_MEASURING_NODES = "Measuring Nodes"
    }

    /** Iterates the nodes of the [result] to collect statistics. */
    override fun accept(result: TranslationResult) {
        val nodeMeasurement =
            MeasurementHolder(this.javaClass, MEASUREMENT_HOLDER_MEASURING_NODES, false, result)

        nodeMeasurement.addMeasurement(MEASUREMENT_TOTAL_GRAPH_NODES, result.nodes.size)
        nodeMeasurement.addMeasurement(MEASUREMENT_TOTAL_PROBLEM_NODES, result.problems.size)
    }

    override fun cleanup() {
        // Nothing to do here
    }
}
