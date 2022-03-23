/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
import de.fraunhofer.aisec.cpg.frontends.llvm.LLVMIRLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.NodeBuilder
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import java.util.*

class CompressLLVMPass : Pass() {
    override fun accept(t: TranslationResult?) {
        // Run only for LLVM.
        if (this.lang !is LLVMIRLanguageFrontend) {
            return
        }

        val flatAST = SubgraphWalker.flattenAST(t)
        // Get all goto statements
        val allGotos = flatAST.filterIsInstance<GotoStatement>()
        // Get all LabelStatements which are only referenced from a single GotoStatement
        val singleEntryLabels =
            flatAST.filterIsInstance<LabelStatement>().filter { l ->
                allGotos.filter { g -> g.targetLabel == l }.size == 1
            }

        // Get all GotoStatements which have to be replaced in the AST
        val gotosToReplace = allGotos.filter { g -> g.targetLabel in singleEntryLabels }

        // Enforce the order: First IfStatements, then SwitchStatements, then the rest. This
        // prevents to treat the final goto in the case or default statement as a normal
        // compound
        // statement which would lead to inlining the instructions BB but we want to keep the BB
        // inside a CompoundStatement.
        for (node in
            flatAST.sortedBy { n ->
                if (n is IfStatement) 1
                else if (n is SwitchStatement) 2 else if (n is TryStatement) 4 else 3
            }) {
            if (node is IfStatement) {
                // Replace the then-statement with the basic block it jumps to iff we found that
                // its
                // goto statement is the only one jumping to the target
                if (node.thenStatement in gotosToReplace &&
                        node !in
                            SubgraphWalker.flattenAST(
                                (node.thenStatement as GotoStatement).targetLabel.subStatement
                            )
                ) {
                    node.thenStatement =
                        (node.thenStatement as GotoStatement).targetLabel.subStatement
                }
                // Replace the else-statement with the basic block it jumps to iff we found that
                // its
                // goto statement is the only one jumping to the target
                if (node.elseStatement in gotosToReplace &&
                        node !in
                            SubgraphWalker.flattenAST(
                                (node.elseStatement as GotoStatement).targetLabel.subStatement
                            )
                ) {
                    node.elseStatement =
                        (node.elseStatement as GotoStatement).targetLabel.subStatement
                }
            } else if (node is SwitchStatement) {
                // Iterate over all statements in a body of the switch/case and replace a goto
                // statement iff it is the only one jumping to the target
                val caseBodyStatements = node.statement as CompoundStatement
                val newStatements = caseBodyStatements.statements.toMutableList()
                for (i in 0 until newStatements.size) {
                    if (newStatements[i] in gotosToReplace &&
                            newStatements[i] !in
                                SubgraphWalker.flattenAST(
                                    (newStatements[i] as GotoStatement).targetLabel.subStatement
                                )
                    ) {
                        newStatements[i] =
                            (newStatements[i] as GotoStatement).targetLabel.subStatement
                    }
                }
                (node.statement as CompoundStatement).statements = newStatements
            } else if (node is TryStatement &&
                    node.catchClauses.size == 1 &&
                    node.catchClauses[0].body.statements[0] is CatchClause
            ) {
                /* Initially, we expect only a single catch clause which contains all the logic.
                 * The first statement of the clause should have been a `landingpad` instruction
                 * which has been translated to a CatchClause. We get this clause and set it as the
                 * catch clause.
                 */
                val catchClauses = mutableListOf<CatchClause>()

                val caseBody = node.catchClauses[0].body

                // This is the most generic one
                val clauseToAdd = caseBody.statements[0] as CatchClause
                catchClauses.add(clauseToAdd)
                caseBody.statements = caseBody.statements.drop(1)
                catchClauses[0].body = caseBody
                if (node.catchClauses[0].parameter != null) {
                    catchClauses[0].setParameter(node.catchClauses[0].parameter!!)
                }
                node.catchClauses = catchClauses

                fixThrowStatementsForCatch(node.catchClauses[0])
            } else if (node is TryStatement &&
                    node.catchClauses.size == 1 &&
                    node.catchClauses[0].body.statements[0] is CompoundStatement
            ) {
                // A compound statement which is wrapped in the catchClause. We can simply move
                // it
                // one layer up and make
                // the compound statement the body of the catch clause.
                val innerCompound = node.catchClauses[0].body.statements[0] as CompoundStatement
                node.catchClauses[0].body.statements = innerCompound.statements
                fixThrowStatementsForCatch(node.catchClauses[0])
            } else if (node is TryStatement && node.catchClauses.size > 0) {
                for (catch in node.catchClauses) {
                    fixThrowStatementsForCatch(catch)
                }
            } else if (node is CompoundStatement) {
                // Get the last statement in a CompoundStatement and replace a goto statement
                // iff it is the only one jumping to the target
                val goto = node.statements.lastOrNull()
                if (goto != null &&
                        goto in gotosToReplace &&
                        node !in
                            SubgraphWalker.flattenAST(
                                (goto as GotoStatement).targetLabel.subStatement
                            )
                ) {
                    val subStatement = goto.targetLabel.subStatement
                    val newStatements = node.statements.dropLast(1).toMutableList()
                    newStatements.addAll((subStatement as CompoundStatement).statements)
                    node.statements = newStatements
                }
            }
        }
    }

    /**
     * Checks if a throw statement which is included in this catch block does not have a parameter.
     * Those statements have been artificially added e.g. by a catchswitch and need to be filled
     * now.
     */
    private fun fixThrowStatementsForCatch(catch: CatchClause) {
        val reachableThrowNodes =
            getAllChildrenRecursively(catch).filter { n ->
                n is UnaryOperator && n.operatorCode?.equals("throw") == true && n.input == null
            }
        if (reachableThrowNodes.isNotEmpty()) {
            if (catch.parameter == null) {
                val error =
                    NodeBuilder.newVariableDeclaration(
                        "e_${catch.name}",
                        UnknownType.getUnknownType(),
                        "",
                        true
                    )
                catch.setParameter(error)
            }
            val exceptionReference =
                NodeBuilder.newDeclaredReferenceExpression(
                    catch.parameter!!.name,
                    catch.parameter!!.type,
                    ""
                )
            exceptionReference.refersTo = catch.parameter
            reachableThrowNodes.forEach { n -> (n as UnaryOperator).input = exceptionReference }
        }
    }

    /** Iterates through all nodes which are reachable from the catch clause */
    private fun getAllChildrenRecursively(node: CatchClause?): Set<Node> {
        if (node == null) return LinkedHashSet()
        val worklist: Queue<Node> = LinkedList()
        worklist.add(node.body)
        val alreadyChecked = LinkedHashSet<Node>()
        while (!worklist.isEmpty()) {
            val currentNode = worklist.remove()
            alreadyChecked.add(currentNode)
            // We exclude sub-try statements as they would mess up with the results
            val toAdd =
                SubgraphWalker.getAstChildren(currentNode).filter { n ->
                    n !is TryStatement && !alreadyChecked.contains(n) && !worklist.contains(n)
                }
            worklist.addAll(toAdd)
        }
        return alreadyChecked
    }

    override fun cleanup() {
        // Nothing to do
    }
}
