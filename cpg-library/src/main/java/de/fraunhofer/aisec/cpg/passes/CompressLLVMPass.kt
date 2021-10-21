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

        for (node in flatAST) {
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
                for (i in 0 until caseBodyStatements.statements.size) {
                    if (caseBodyStatements.statements[i] in gotosToReplace) {
                        // TODO: This replacement doesn't work!
                        caseBodyStatements.statements[i] =
                            (caseBodyStatements.statements[i] as GotoStatement)
                                .targetLabel
                                .subStatement
                    }
                }
            } else if (node is CompoundStatement) {
                // Iterate over all statements in a CompoundStatement and replace a goto statement
                // iff it is the only one jumping to the target
               val iterator = node.statements.listIterator()
                while (iterator.hasNext()) {
                    val statement = iterator.next()
                    if (statement in gotosToReplace) {
                        val subStatement = (statement as GotoStatement).targetLabel.subStatement
                        iterator.set(subStatement) // TODO: This replacement doesn't work!
                    }
                }
            }
        }
    }

    override fun cleanup() {
        TODO("Not yet implemented")
    }
}
