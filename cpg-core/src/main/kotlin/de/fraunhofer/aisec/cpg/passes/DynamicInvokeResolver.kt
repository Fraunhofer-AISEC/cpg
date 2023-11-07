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

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.ParameterDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.pointer
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.FunctionPointerType
import de.fraunhofer.aisec.cpg.graph.types.FunctionType
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.ScopedWalker
import de.fraunhofer.aisec.cpg.helpers.identitySetOf
import de.fraunhofer.aisec.cpg.passes.order.DependsOn
import java.util.*
import java.util.function.Consumer

/**
 * This [Pass] is responsible for resolving dynamic function invokes, i.e., [CallExpression] nodes
 * that contain a reference/pointer to a function and are being "called". A common example includes
 * C/C++ function pointers.
 *
 * This pass is intentionally split from the [SymbolResolver] because it depends on DFG edges. This
 * split allows the [SymbolResolver] to be run before any DFG passes, which in turn allow us to also
 * populate DFG passes for inferred functions.
 */
@DependsOn(SymbolResolver::class)
@DependsOn(DFGPass::class)
class DynamicInvokeResolver(ctx: TranslationContext) : ComponentPass(ctx) {
    private lateinit var walker: ScopedWalker
    private var inferDfgForUnresolvedCalls = false

    override fun accept(component: Component) {
        inferDfgForUnresolvedCalls = config.inferenceConfiguration.inferDfgForUnresolvedSymbols
        walker = ScopedWalker(scopeManager)
        walker.registerHandler { node, _ -> handle(node) }

        for (tu in component.translationUnits) {
            walker.iterate(tu)
        }
    }

    private fun handle(node: Node?) {
        when (node) {
            is MemberCallExpression -> handleMemberCallExpression(node)
            is CallExpression -> handleCallExpression(node)
        }
    }

    /**
     * Resolves function pointers in a [CallExpression] node. As long as the [CallExpression.callee]
     * has a [FunctionPointerType], we should be able to resolve it.
     */
    private fun handleCallExpression(call: CallExpression) {
        val callee = call.callee
        if (
            callee?.type is FunctionPointerType ||
                ((callee as? Reference)?.refersTo is ParameterDeclaration ||
                    (callee as? Reference)?.refersTo is VariableDeclaration)
        ) {
            handleCallee(call, callee)
        }
    }

    /**
     * Resolves function pointers in a [MemberCallExpression]. In this case the
     * [MemberCallExpression.callee] field is a binary operator on which [BinaryOperator.rhs] needs
     * to have a [FunctionPointerType].
     */
    private fun handleMemberCallExpression(call: MemberCallExpression) {
        val callee = call.callee
        if (callee is BinaryOperator && callee.rhs.type is FunctionPointerType) {
            handleCallee(call, callee.rhs)
        }
    }

    private fun handleCallee(call: CallExpression, expr: Expression) {
        // For now, we harmonize all types to the FunctionPointerType. In the future, we want to get
        // rid of FunctionPointerType and only deal with FunctionTypes.
        val pointerType: FunctionPointerType =
            when (val type = expr.type) {
                is FunctionType -> type.pointer() as FunctionPointerType
                is FunctionPointerType -> type
                else -> return
            }

        val invocationCandidates = mutableListOf<FunctionDeclaration>()
        val work: Deque<Node> = ArrayDeque()
        val seen = identitySetOf<Node>()
        work.push(expr)
        while (work.isNotEmpty()) {
            val curr = work.pop()
            if (!seen.add(curr)) {
                continue
            }

            val isLambda = curr is VariableDeclaration && curr.initializer is LambdaExpression
            val currentFunction =
                if (isLambda) {
                    ((curr as VariableDeclaration).initializer as LambdaExpression).function
                } else {
                    curr
                }

            if (currentFunction is FunctionDeclaration) {
                // Even if it is a function declaration, the dataflow might just come from a
                // situation where the target of a fptr is passed through via a return value. Keep
                // searching if return type or signature don't match
                val functionPointerType = currentFunction.type.pointer()
                if (
                    isLambda &&
                        currentFunction.returnTypes.isEmpty() &&
                        currentFunction.hasSignature(pointerType.parameters)
                ) {
                    invocationCandidates.add(currentFunction)
                    continue
                } else if (functionPointerType == pointerType) {
                    invocationCandidates.add(currentFunction)
                    // We have found a target. Don't follow this path any further, but still
                    // continue the other paths that might be left, as we could have several
                    // potential targets at runtime
                    continue
                }
            }
            curr.prevDFG.forEach(Consumer(work::push))
        }

        call.invokes = invocationCandidates
        call.invokeEdges.forEach { it.addProperty(Properties.DYNAMIC_INVOKE, true) }

        // We have to update the dfg edges because this call could now be resolved (which was not
        // the case before).
        DFGPass(ctx).handleCallExpression(call, inferDfgForUnresolvedCalls)
    }

    override fun cleanup() {
        // Nothing to do
    }
}
