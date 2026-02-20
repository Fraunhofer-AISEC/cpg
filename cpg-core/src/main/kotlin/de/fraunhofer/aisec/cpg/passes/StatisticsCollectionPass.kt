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
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.ProblemNode
import de.fraunhofer.aisec.cpg.helpers.MeasurementHolder
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.ScopedWalker
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy

/**
 * A [Pass] collecting statistics for the graph. Currently, it collects the number of nodes and the
 * number of problem nodes (i.e., nodes where the translation failed for some reason).
 */
@Description(
    "A pass that collects statistics (number of nodes and number of problems) about the graph."
)
class StatisticsCollectionPass(ctx: TranslationContext) : TranslationResultPass(ctx) {

    /** Iterates the nodes of the [result] to collect statistics. */
    override fun accept(result: TranslationResult) {
        var problemNodes = 0
        var nodes = 0
        val walker = ScopedWalker(ctx.scopeManager, Strategy::AST_FORWARD)
        walker.registerHandler { node: Node ->
            nodes++
            if (node is ProblemNode) {
                problemNodes++
            }
        }

        for (tu in result.components.flatMap { it.translationUnits }) {
            walker.iterate(tu)
        }

        val nodeMeasurement = MeasurementHolder(this.javaClass, "Measuring Nodes", false, result)
        nodeMeasurement.addMeasurement("Total graph nodes", nodes.toString())
        nodeMeasurement.addMeasurement("ProblemExpression nodes", problemNodes.toString())
    }

    override fun cleanup() {
        // Nothing to do here
    }
}
