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
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker

class CompressLLVMPass() : Pass() {
    override fun accept(t: TranslationResult?) {
        val flatAST = SubgraphWalker.flattenAST(t)
        // Get all goto statements
        val allGotos = flatAST.filter { n -> n is GotoStatement }
        // Get all LabelStatements which are only referenced from a single GotoStatement
        val singleEntryLabels =
            flatAST.filter { n -> n is LabelStatement }.filter { l ->
                allGotos.filter { g -> (g as GotoStatement).targetLabel == l }.size == 1
            }
        // Get all GotoStatements which have to be replaced in the AST
        val gotosToReplace =
            allGotos.filter { g -> (g as GotoStatement).targetLabel in singleEntryLabels }

        // Enforce the order: First IfStatements, then SwitchStatements, then the rest. This
        // prevents to treat the final goto in the case or default statement as a normal compound
        // statement which would lead to inlining the instructions BB but we want to keep the BB
        // inside a CompoundStatement.
        for (node in
            flatAST.sortedBy { n ->
                if (n is IfStatement) 1 else if (n is SwitchStatement) 2 else 3
            }) {
            if (node is IfStatement) {
                // Replace the then-statement with the basic block it jumps to iff we found that its
                // goto statement is the only one jumping to the target
                if (node.thenStatement in gotosToReplace) {
                    node.thenStatement =
                        (node.thenStatement as GotoStatement).targetLabel.subStatement
                }
                // Replace the else-statement with the basic block it jumps to iff we found that its
                // goto statement is the only one jumping to the target
                if (node.elseStatement in gotosToReplace) {
                    node.elseStatement =
                        (node.elseStatement as GotoStatement).targetLabel.subStatement
                }
            } else if (node is SwitchStatement) {
                // Iterate over all statements in a body of the switch/case and replace a goto
                // statement iff it is the only one jumping to the target
                val caseBodyStatements = node.statement as CompoundStatement
                val newStatements = caseBodyStatements.statements.toMutableList()
                for (i in 0 until newStatements.size) {
                    if (newStatements[i] in gotosToReplace) {
                        newStatements[i] =
                            (newStatements[i] as GotoStatement).targetLabel.subStatement
                    }
                }
                (node.statement as CompoundStatement).statements = newStatements
            } else if (node is CompoundStatement) {
                // Get the last statement in a CompoundStatement and replace a goto statement
                // iff it is the only one jumping to the target
                val goto = node.statements.lastOrNull()
                if (goto != null && goto in gotosToReplace) {
                    val subStatement = (goto as GotoStatement).targetLabel.subStatement
                    val newStatements = node.statements.dropLast(1).toMutableList()
                    newStatements.addAll((subStatement as CompoundStatement).statements)
                    node.statements = newStatements
                }
            }
        }
    }

    override fun cleanup() {
        TODO("Not yet implemented")
    }
}
