/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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
import de.fraunhofer.aisec.cpg.analysis.ValueEvaluator
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumCircuit
import de.fraunhofer.aisec.cpg.graph.quantumcpg.QuantumMeasure
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.ForEachStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ArraySubscriptionExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.order.DependsOn
import de.fraunhofer.aisec.cpg.passes.quantumcpg.QuantumDFGPass

@DependsOn(QuantumDFGPass::class)
@DependsOn(ControlFlowSensitiveDFGPass::class)
class DFGConnectionPass : Pass() {
    override fun cleanup() {
        // Nothing to do
    }

    override fun accept(t: TranslationResult) {

        val flatTr = SubgraphWalker.flattenAST(t)
        val transpileCalls = t.calls("transpile")
        for (call in transpileCalls) {
            // Get the circuit variable that we're talking about
            val circuit = call.arguments.first() as DeclaredReferenceExpression

            val thisQuantumCircuit =
                t.additionalNodes.firstOrNull {
                    it is QuantumCircuit && it.cpgNode == circuit.refersTo
                } as QuantumCircuit?

            val measures = thisQuantumCircuit?.operations?.filterIsInstance<QuantumMeasure>()

            // Get the jobs which are run using the compiled circuit
            val jobs =
                call
                    .followNextDFGEdgesUntilHit {
                        (it as? MemberCallExpression)?.name?.localName == "run"
                        // TODO: Actually, we would want to check the call's base comes from the
                        // return of the transpile call
                    }
                    .fulfilled
                    .map(List<Node>::last)
            jobs.forEach { job ->
                // Get the calls to result on these jobs
                val results =
                    job.followNextDFGEdgesUntilHit { next ->
                            (next as? MemberCallExpression)?.name?.localName == "result"
                            // TODO: Actually, we would want to check the "run" call's base comes
                            // from the return of the result call
                        }
                        .fulfilled
                        .map(List<Node>::last)

                // Add DFG for the call to result(): All classical bits which are measured flow to
                // the return value
                measures?.forEach { measure ->
                    results.forEach { result -> measure.cBit.addNextDFG(result) }
                }

                // Add DFG for access to the resulting array (single index or whole region)
                // - call get_counts() on the result
                // - access the dict: We only care about its key
                // - ArraySubscriptExpr or RangeExpr on the key
                val countsDicts =
                    results.flatMap { result ->
                        result
                            .followNextDFGEdgesUntilHit { next ->
                                (next as? MemberCallExpression)?.name?.localName == "get_counts"
                            }
                            .fulfilled
                            .map(List<Node>::last)
                    }
                val keyAccesses =
                    countsDicts.flatMap { countsDict -> findAccessToCountsDictKey(countsDict) }
                val bitAccesses =
                    keyAccesses.flatMap { keyAccess ->
                        flatTr.filterIsInstance<ArraySubscriptionExpression>().filter { flatNode ->
                            keyAccess
                                .followNextDFGEdgesUntilHit { it == flatNode }
                                .fulfilled
                                .isNotEmpty()
                        }
                    }
                // Add DFG from cbitX to bitAccess with index Y
                bitAccesses.forEach { bitAccess ->
                    val bitIndex =
                        (ValueEvaluator().evaluate(bitAccess.subscriptExpression) as? Number)
                            ?.toInt()
                    bitIndex?.let {
                        thisQuantumCircuit?.classicBits?.let {
                            // Fix index if negative or so
                            var index = bitIndex % it.size
                            if (index < 0) index += it.size
                            // The order (for Qiskit) is inverted
                            index = it.size - 1 - index
                            measures
                                ?.filter { m ->
                                    m.cBit.refersToClassicBit ==
                                        thisQuantumCircuit.getCbitByIdx(index)
                                }
                                ?.flatMap { m -> m.cBit.refersToClassicBit.references }
                                ?.forEach { cBit -> bitAccess.addPrevDFG(cBit) }
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns a list of nodes which hold the keys. Currently, it's only implemented for access in a
     * foreach loop over the dict.
     */
    private fun findAccessToCountsDictKey(countsDict: Node): List<Node> {
        return countsDict
            .followNextEOGEdgesUntilHit {
                (it as? ForEachStatement)?.iterable?.followPrevDFG { prev -> prev == countsDict } !=
                    null
            }
            .fulfilled
            .mapNotNull {
                val variable = (it.last() as? ForEachStatement)?.variable
                if (variable is DeclarationStatement) {
                    variable.variables
                } else null
            }
            .flatten()
    }
}
