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
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.ValueEvaluator
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.statements.IfStatement
import de.fraunhofer.aisec.cpg.graph.statements.WhileStatement
import de.fraunhofer.aisec.cpg.processing.IVisitor
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy

/**
 * A [Pass] which uses a simple logic to determine constant values and mark unreachable code regions
 * by setting the [Properties.UNREACHABLE] property of an eog-edge to true.
 */
class UnreachableEOGPass : Pass() {
    override fun accept(t: TranslationResult) {
        for (tu in t.translationUnits) {
            tu.accept(
                Strategy::AST_FORWARD,
                object : IVisitor<Node>() {
                    override fun visit(n: Node) {
                        when (n) {
                            is IfStatement -> handleIfStatement(n)
                            is WhileStatement -> handleWhileStatement(n)
                        }

                        super.visit(n)
                    }
                }
            )
        }
    }

    private fun handleIfStatement(n: IfStatement) {
        val evalResult = ValueEvaluator().evaluate(n.condition)
        if (evalResult is Boolean && evalResult == true) {
            n.nextEOGEdges
                .firstOrNull { e -> e.getProperty(Properties.INDEX) == 1 }
                ?.addProperty(Properties.UNREACHABLE, true)
        } else if (evalResult is Boolean && evalResult == false) {
            n.nextEOGEdges
                .firstOrNull { e -> e.getProperty(Properties.INDEX) == 0 }
                ?.addProperty(Properties.UNREACHABLE, true)
        }
    }

    private fun handleWhileStatement(n: WhileStatement) {
        /*
         * Note: It does not understand that code like
         * x = true; while(x) {...; x = false;}
         * makes the loop execute at least once.
         * Apparently, the CPG does not offer the required functionality to
         * differentiate between the first and subsequent evaluations of the
         * condition.
         */
        val evalResult = ValueEvaluator().evaluate(n.condition)
        if (evalResult is Boolean && evalResult == true) {
            n.nextEOGEdges
                .firstOrNull { e -> e.getProperty(Properties.INDEX) == 1 }
                ?.addProperty(Properties.UNREACHABLE, true)
        } else if (evalResult is Boolean && evalResult == false) {
            n.nextEOGEdges
                .firstOrNull { e -> e.getProperty(Properties.INDEX) == 0 }
                ?.addProperty(Properties.UNREACHABLE, true)
        }
    }

    override fun cleanup() {
        // nothing to do
    }
}
