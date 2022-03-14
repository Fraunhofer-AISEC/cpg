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

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.ProblemNode
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.helpers.MeasurementBenchmark
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.ScopedWalker

class StatisticsCollectionPass : Pass() {
    override fun accept(translationResult: TranslationResult?) {
        var problemNodes: Int = 0
        var nodes: Int = 0
        translationResult?.let {
            val walker = ScopedWalker(lang)
            walker.registerHandler { _: RecordDeclaration?, _: Node?, currNode: Node? ->
                nodes++
                if (currNode is ProblemNode) {
                    problemNodes++
                }
            }

            for (tu in translationResult.translationUnits) {
                walker.iterate(tu)
            }
        }
        val nodeMeasurement =
            MeasurementBenchmark(this.javaClass, "Measuring Nodes", false, translationResult)
        nodeMeasurement.addMeasurement("Graph nodes", nodes.toString())
        nodeMeasurement.addMeasurement("Problem nodes", problemNodes.toString())
    }

    override fun cleanup() {}
}
