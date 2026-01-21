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

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.ValueDeclaration
import de.fraunhofer.aisec.cpg.graph.edges.flows.Dataflow
import de.fraunhofer.aisec.cpg.graph.edges.flows.DependenceType
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.helpers.identitySetOf
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.processing.IVisitor
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy

/**
 * This pass collects the dependence information of each node into a Program Dependence Graph (PDG)
 * by traversing through the AST.
 */
@DependsOn(ControlDependenceGraphPass::class)
@DependsOn(DFGPass::class)
@DependsOn(ControlFlowSensitiveDFGPass::class, softDependency = true)
@DependsOn(DynamicInvokeResolver::class)
@Description(
    "Combines the Data Flow Graph (DFG) and Control Dependence Graph (CDG) into a Program Dependence Graph (PDG), providing a comprehensive view of both data and control dependencies within the program."
)
class ProgramDependenceGraphPass(ctx: TranslationContext) : TranslationUnitPass(ctx) {
    private val visitor =
        object : IVisitor<AstNode>() {
            /**
             * Collects the data and control dependence edges of a node and adds them to the program
             * dependence edges
             */
            override fun visit(t: AstNode) {
                if (t is Reference) {
                    // We filter all prevDFGEdges if the condition affects the variable of t and
                    // if there's a flow from the prevDFGEdge's node through the condition to t.
                    val prevDFGToConsider = mutableListOf<Dataflow>()
                    t.prevDFGEdges.forEach { prevDfgEdge ->
                        val prevDfgNode = prevDfgEdge.start
                        // The prevDfgNode also flows into the condition. This is suspicious because
                        // if the condition is on each path between prevDfgNode and t, the condition
                        // is more relevant.
                        if (prevDfgNode is Reference || prevDfgNode is ValueDeclaration) {
                            val cdgConditionChildren =
                                t.prevCDG.flatMap {
                                    (it as? BranchingNode)?.branchedBy?.allChildren<Reference> { c
                                        ->
                                        c in prevDfgNode.nextDFG
                                    } ?: listOf()
                                }
                            if (
                                cdgConditionChildren.isNotEmpty() &&
                                    cdgConditionChildren.all {
                                        it != t && allEOGsFromToFlowThrough(prevDfgNode, t, it)
                                    }
                            ) {
                                // All data flows from the prevDfgNode to t flow through all the
                                // relevant CDG condition's children. This means that we can safely
                                // avoid the prevDfgNode in the PDG because it will be added
                                // transitively.
                            } else {
                                prevDFGToConsider.add(prevDfgEdge)
                            }
                        } else {
                            prevDFGToConsider.add(prevDfgEdge)
                        }
                    }

                    prevDFGToConsider.forEach {
                        it.dependence = DependenceType.DATA
                        t.prevPDGEdges.add(it)
                    }
                    t.prevPDGEdges += t.prevCDGEdges
                } else {
                    t.prevDFGEdges.forEach {
                        it.dependence = DependenceType.DATA
                        t.prevPDGEdges.add(it)
                    }
                    t.prevPDGEdges += t.prevCDGEdges
                }
            }
        }

    private fun allEOGsFromToFlowThrough(from: Node, to: Node, through: Node): Boolean {
        val worklist = mutableListOf(from)
        val alreadySeenNodes = identitySetOf<Node>()

        while (worklist.isNotEmpty()) {
            val currentStatus = worklist.removeFirst()
            alreadySeenNodes.add(currentStatus)
            val nextEOG = currentStatus.nextEOG.filter { it != through }
            if (nextEOG.isEmpty()) {
                // This path always flows through "through" or has not seen "to", so we're good
                continue
            } else if (nextEOG.any { it == to }) {
                // We reached "to". This means that "through" has not been on the path for this EOG
                // path.
                return false
            } else {
                worklist.addAll(nextEOG.filter { it !in alreadySeenNodes })
            }
        }
        return true
    }

    override fun accept(tu: TranslationUnitDeclaration) {
        tu.statements.forEach(::handle)
        tu.namespaces.forEach(::handle)
        tu.declarations.forEach(::handle)
    }

    override fun cleanup() {
        // Nothing to do
    }

    private fun handle(node: Node) {
        if (node is AstNode) {
            node.accept<AstNode>(Strategy::AST_FORWARD, visitor)
        }
    }
}
