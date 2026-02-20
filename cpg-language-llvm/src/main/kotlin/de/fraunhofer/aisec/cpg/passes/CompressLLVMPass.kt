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

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.llvm.LLVMIRLanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ProblemExpression
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.configuration.ExecuteFirst
import de.fraunhofer.aisec.cpg.passes.configuration.RequiredFrontend
import java.util.*

@ExecuteFirst
@RequiredFrontend(LLVMIRLanguageFrontend::class)
@Description(
    "Re-organizes some nodes in the CPG if they originate from LLVM IR code, removing redundant nodes and edges to optimize the graph structure for analysis."
)
class CompressLLVMPass(ctx: TranslationContext) : ComponentPass(ctx) {
    override fun accept(component: Component) {
        val flatAST = SubgraphWalker.flattenAST(component)
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
        // statement which would lead to inlining the instructions BB, but we want to keep the BB
        // inside a Block.
        for (node in
            flatAST.sortedBy { n ->
                when (n) {
                    is IfStatement -> 1
                    is SwitchStatement -> 2
                    is TryStatement -> 4
                    else -> 3
                }
            }) {
            when (node) {
                is IfStatement -> {
                    handleIfStatement(node, gotosToReplace)
                }
                is SwitchStatement -> {
                    handleSwitchStatement(node, gotosToReplace)
                }
                is TryStatement -> {
                    handleTryStatement(node)
                }
                is Block -> {
                    handleBlock(node, gotosToReplace)
                }
            }
        }
    }

    /**
     * Get the last statement in a [Block] and replace a goto statement iff it is the only one
     * jumping to the target
     */
    private fun handleBlock(node: Block, gotosToReplace: List<GotoStatement>) {
        val goto = node.statements.lastOrNull() as? GotoStatement ?: return
        val gotoSubstatement = goto.targetLabel?.subStatement as? Block ?: return
        if (goto in gotosToReplace && node !in gotoSubstatement.allChildren<Block>()) {
            val newStatements = node.statements.dropLast(1).toMutableList()
            newStatements.addAll(gotoSubstatement.statements)
            node.statements = newStatements
        }
    }

    /**
     * Iterates over all statements in a body of the switch/case and replace a goto statement if it
     * is the only one jumping to the target
     */
    private fun handleSwitchStatement(node: SwitchStatement, gotosToReplace: List<GotoStatement>) {
        val caseBodyStatements = node.statement as? Block ?: return
        val newStatements = caseBodyStatements.statements.toMutableList()
        for (i in 0 until newStatements.size) {
            val subStatement = (newStatements[i] as? GotoStatement)?.targetLabel?.subStatement
            if (
                newStatements[i] in gotosToReplace &&
                    newStatements[i] !in (subStatement?.astChildren ?: listOf())
            ) {
                subStatement?.let { newStatements[i] = it }
            }
        }
        caseBodyStatements.statements = newStatements
    }

    /**
     * Replace the then-statement and else-statement with the basic block it jumps to iff we found
     * that its goto statement is the only one jumping to the target
     */
    private fun handleIfStatement(node: IfStatement, gotosToReplace: List<GotoStatement>) {

        // Replace the then-statement
        val thenGoto = (node.thenStatement as? GotoStatement)?.targetLabel?.subStatement
        if (node.thenStatement in gotosToReplace && node !in thenGoto.allChildren<IfStatement>()) {
            node.thenStatement = thenGoto
        }
        // Replace the else-statement
        val elseGoto = (node.elseStatement as? GotoStatement)?.targetLabel?.subStatement
        if (node.elseStatement in gotosToReplace && node !in elseGoto.allChildren<IfStatement>()) {
            node.elseStatement = elseGoto
        }
    }

    private fun handleTryStatement(node: TryStatement) {
        val firstCatch = node.catchClauses.singleOrNull()
        val firstStatement = firstCatch?.body?.statements?.get(0)
        when (firstStatement) {
            is CatchClause -> {
                /* Initially, we expect only a single catch clause which contains all the logic.
                 * The first statement of the clause should have been a `landingpad` instruction
                 * which has been translated to a CatchClause. We get this clause and set it as the
                 * catch clause.
                 */
                val catchClauses = mutableListOf<CatchClause>()

                val caseBody = firstCatch.body

                // This is the most generic one
                catchClauses.add(firstStatement)
                caseBody?.statements = caseBody.statements.drop(1).toMutableList()
                catchClauses[0].body = caseBody
                if (firstCatch.parameter != null) {
                    catchClauses[0].parameter = firstCatch.parameter
                }
                node.catchClauses = catchClauses
            }
            is Block -> {
                // A compound statement which is wrapped in the catchClause. We can simply move
                // it one layer up and make the compound statement the body of the catch clause.
                firstCatch.body?.statements = firstStatement.statements
            }
        }
        node.catchClauses.forEach(::fixThrowExpressionsForCatch)
    }

    /**
     * Checks if a throw expression which is included in this catch block does not have a parameter.
     * Those expressions have been artificially added e.g. by a catchswitch and need to be filled
     * now.
     */
    private fun fixThrowExpressionsForCatch(catch: CatchClause) {
        val reachableThrowNodes =
            getAllChildrenRecursively(catch).filterIsInstance<ThrowExpression>().filter { n ->
                n.exception is ProblemExpression
            }
        if (reachableThrowNodes.isNotEmpty()) {
            val catchParameter =
                catch.parameter
                    ?: newVariable(
                            "e_${catch.name}",
                            UnknownType.getUnknownType(catch.language),
                            implicitInitializerAllowed = true,
                        )
                        .apply {
                            language = catch.language
                            catch.parameter = this
                        }
                        .implicit()

            reachableThrowNodes.forEach {
                it.exception =
                    newReference(catchParameter.name, catchParameter.type)
                        .apply {
                            language = catch.language
                            refersTo = catch.parameter
                        }
                        .implicit()
            }
        }
    }

    /**
     * Iterates through all [AstNode]s which are reachable from the catch clause. Note: When
     * reaching a `TryStatement`, we do not follow the path further. This is why we can't use the
     * `allChildren` extension.
     */
    private fun getAllChildrenRecursively(node: CatchClause?): Set<AstNode> {
        if (node == null) return setOf()
        val worklist: Queue<AstNode> = LinkedList()
        worklist.add(node.body)
        val alreadyChecked = LinkedHashSet<AstNode>()
        while (worklist.isNotEmpty()) {
            val currentNode = worklist.remove()
            alreadyChecked.add(currentNode)
            // We exclude sub-try statements as they would mess up with the results
            val toAdd =
                currentNode.astChildren.filter { n ->
                    n !is TryStatement && n !in alreadyChecked && n !in worklist
                }
            worklist.addAll(toAdd)
        }
        return alreadyChecked
    }

    override fun cleanup() {
        // Nothing to do
    }
}
