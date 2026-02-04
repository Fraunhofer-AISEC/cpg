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

import de.fraunhofer.aisec.cpg.IncompatibleSignature
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.graph.AccessValues
import de.fraunhofer.aisec.cpg.graph.AstNode
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.ParameterDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.edges.flows.FullDataflowGranularity
import de.fraunhofer.aisec.cpg.graph.pointer
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.FunctionPointerType
import de.fraunhofer.aisec.cpg.graph.types.FunctionType
import de.fraunhofer.aisec.cpg.graph.types.ProblemType
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.ScopedWalker
import de.fraunhofer.aisec.cpg.helpers.identitySetOf
import de.fraunhofer.aisec.cpg.matchesSignature
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy
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
@DependsOn(ControlFlowSensitiveDFGPass::class, softDependency = true)
@DependsOn(PointsToPass::class, softDependency = true)
class DynamicInvokeResolver(ctx: TranslationContext) : ComponentPass(ctx) {
    private lateinit var walker: ScopedWalker<AstNode>
    private var inferDfgForUnresolvedCalls = false

    override fun accept(component: Component) {
        inferDfgForUnresolvedCalls = config.inferenceConfiguration.inferDfgForUnresolvedSymbols
        walker = ScopedWalker(scopeManager, Strategy::AST_FORWARD)
        walker.registerHandler { node -> handle(node) }

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
            callee.type is FunctionPointerType ||
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
                is FunctionType -> {
                    when (val pointerType = type.pointer()) {
                        is FunctionPointerType -> pointerType
                        is ProblemType -> {
                            log.warn("Function has unexpected type: ProblemType; ignore call")
                            return
                        }
                        else -> {
                            log.warn("Unexpected function type: ${pointerType}; ignore call")
                            return
                        }
                    }
                }
                is FunctionPointerType -> type
                else -> {
                    // some languages allow other types to derive from a function type, in this case
                    // we need to look for a super type
                    val superType = type.superTypes.singleOrNull()
                    if (superType is FunctionType) {
                        superType.pointer() as FunctionPointerType
                    } else {
                        return
                    }
                }
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
                    (curr.initializer as LambdaExpression).function
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
                        currentFunction.matchesSignature(pointerType.parameters) !=
                            IncompatibleSignature
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
            // Do not consider the base for member expressions, we have to know possible values of
            // the member (e.g. field).
            val prevDFGToPush =
                curr.prevDFGEdges
                    .filter { it.granularity is FullDataflowGranularity }
                    .map { it.start }
                    .toMutableList()
            if (curr is MemberExpression && prevDFGToPush.isEmpty()) {
                // TODO: This is only a workaround!
                //   If there is nothing found for MemberExpressions, we may have set the field
                //   somewhere else but do not yet propagate this to this location (e.g. because it
                //   happens in another function). In this case, we look at write-usages to the
                //   field and use all of those. This is only a temporary workaround until someone
                //   implements an interprocedural analysis (for example).
                (curr.refersTo as? FieldDeclaration)
                    ?.usages
                    ?.filter {
                        it.access == AccessValues.WRITE || it.access == AccessValues.READWRITE
                    }
                    ?.let { prevDFGToPush.addAll(it) }
                // Also add the initializer of the field (if it exists)
                (curr.refersTo as? FieldDeclaration)?.initializer?.let { prevDFGToPush.add(it) }
            }

            prevDFGToPush.forEach(Consumer(work::push))
        }

        call.invokes = invocationCandidates
        call.invokeEdges.forEach { it.dynamicInvoke = true }

        // We have to update the dfg edges because this call could now be resolved (which was not
        // the case before).
        DFGPass(ctx).handlePreviouslyUnresolvedCallExpression(call, inferDfgForUnresolvedCalls)
    }

    override fun cleanup() {
        // Nothing to do
    }
}
