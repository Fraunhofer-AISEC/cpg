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
import de.fraunhofer.aisec.cpg.graph.edge.DependenceType
import de.fraunhofer.aisec.cpg.passes.order.DependsOn
import de.fraunhofer.aisec.cpg.processing.IVisitor
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy

/**
 * This pass collects the dependence information of each node into a Program Dependence Graph (PDG)
 * by traversing through the AST.
 */
@DependsOn(ControlDependenceGraphPass::class)
@DependsOn(DFGPass::class)
@DependsOn(ControlFlowSensitiveDFGPass::class, softDependency = true)
class ProgramDependenceGraphPass(ctx: TranslationContext) : TranslationUnitPass(ctx) {
    private val visitor =
        object : IVisitor<Node>() {
            /**
             * Collects the data and control dependence edges of a node and adds them to the program
             * dependence edges
             */
            override fun visit(t: Node) {
                t.addAllPrevPDGEdges(t.prevDFGEdges, DependenceType.DATA)
                t.addAllPrevPDGEdges(t.prevCDGEdges, DependenceType.CONTROL)
            }
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
        node.accept(Strategy::AST_FORWARD, visitor)
    }
}
