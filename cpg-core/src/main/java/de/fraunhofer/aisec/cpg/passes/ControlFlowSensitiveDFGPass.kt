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
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.IterativeGraphWalker

@DependsOn(EvaluationOrderGraphPass::class)
@DependsOn(DFGPass::class)
open class ControlFlowSensitiveDFGPass : Pass() {
    override fun cleanup() {
        // Nothing to do
    }

    override fun accept(translationResult: TranslationResult) {
        val walker = IterativeGraphWalker()
        walker.registerOnNodeVisit(::handle)
        for (tu in translationResult.translationUnits) {
            walker.iterate(tu)
        }
    }

    /**
     * ControlFlowSensitiveDFG Pass is performed on every method.
     *
     * @param node every node in the TranslationResult
     */
    protected fun handle(node: Node) {
        if (node is FunctionDeclaration) {
            handleStatementHolder(node)
        }
    }

    private fun findLastWrites(node: DeclaredReferenceExpression): List<Node> {
        val result =
            node
                .followPrevEOGEdgesUntilHit {
                    it is Assignment &&
                        node.refersTo != null &&
                        ((it.target as? DeclaredReferenceExpression)?.refersTo == node.refersTo ||
                            (it.target as? VariableDeclaration) == node.refersTo)
                }
                .fulfilled
                .mapNotNull { (it.last() as? Assignment) }

        // For assignments with "=" and DeclaredReferenceExpressions, we always have 2 edges: one
        // using the = and one the actual target. That's annoying for other analyses since it
        // doubles the path to check, so we remove the DFG edge across the "=".
        // This is a bad idea for conditional expressions
        /*result.forEach {
            if (
                (it as? BinaryOperator)?.operatorCode == "=" &&
                    (it.astParent as? ConditionalExpression) != null
            ) {
                it.removeNextDFG(it.lhs)
                it.removePrevDFG(it.rhs)
            }
        }*/

        return result.mapNotNull { it.target as? Node }
    }

    private fun obtainAssignmentNode(node: DeclaredReferenceExpression): BinaryOperator? {
        val alreadyVisited = mutableSetOf<Node>()
        val worklist = mutableListOf<Node>()
        worklist.addAll(node.nextEOG)
        while (worklist.isNotEmpty()) {
            val n = worklist.removeFirst()
            if (n is BinaryOperator && n.lhs == node) return n
            worklist.addAll(n.nextEOG.filter { it !in worklist && it !in alreadyVisited })
            alreadyVisited.add(n)
        }

        return null
    }

    private fun handleStatementHolder(node: Node) {
        for (ref in node.refs) {
            if (ref.access == AccessValues.WRITE) {
                // We write to the DeclaredReferenceExpression
                val assignmentNode = obtainAssignmentNode(ref)
                if (assignmentNode != null) {
                    // For assignments with "=" and DeclaredReferenceExpressions, we always have 2
                    // edges: one using the = and one the actual target. That's annoying for other
                    // analyses since it doubles the path to check, so we remove the DFG edge across
                    // the "=". This is a bad idea for conditional expressions
                    /*if ((assignmentNode.astParent as? ConditionalExpression) != null) {
                        assignmentNode.removePrevDFG(assignmentNode.rhs)
                        assignmentNode.removeNextDFG(assignmentNode.lhs)
                    }*/
                    // Add the edge from rhs to this
                    ref.addPrevDFG(assignmentNode.rhs)
                    assignmentNode.lhs.addPrevDFG(
                        assignmentNode.rhs
                    ) // TODO: I'm quite sure this is the same as the step before.
                    ref.refersTo?.removeNextDFG(ref)
                    if ((ref.refersTo as? VariableDeclaration)?.initializer != null) {
                        ref.refersTo?.removePrevDFG(ref)
                    }
                }
            } else {
                // TODO: Fix the readwrite issue. It should not flow back to the VariableDeclaration
                // (example: DFGTest.testNoOutgoingDFGFromVariableDeclaration())
                // We read the value. That's a bit different to the write case.
                val lastWrites = findLastWrites(ref)
                for (lastWrite in lastWrites) {
                    ref.addPrevDFG(lastWrite)
                    if (
                        lastWrite is DeclaredReferenceExpression &&
                            lastWrite.refersTo?.let { !lastWrites.contains(it) } == true
                    ) {
                        ref.removePrevDFG(lastWrite.refersTo)
                    }
                }
            }
        }

        for (varDecl in node.variables { it.prevDFG.size > 1 }) {
            // Remove all the data flows to the variable declaration except the initialization
            for (prev in HashSet(varDecl.prevDFG)) {
                if (prev != varDecl.initializer) {
                    prev.removeNextDFG(varDecl)
                }
            }

            // Remove all outgoing links of the variable declaration
            for (n in varDecl.nextDFG) {
                if (alwaysAssignBetween(varDecl, n)) {
                    n.removeNextDFG(varDecl)
                }
            }
        }
    }

    private fun alwaysAssignBetween(from: Node, to: Node): Boolean {
        val paths =
            to.followPrevEOGEdgesUntilHit {
                (it as? BinaryOperator)?.operatorCode == "=" &&
                    ((it as? BinaryOperator)?.lhs as? DeclaredReferenceExpression)?.refersTo == from
            }
        return paths.failed.isEmpty()
    }
}
