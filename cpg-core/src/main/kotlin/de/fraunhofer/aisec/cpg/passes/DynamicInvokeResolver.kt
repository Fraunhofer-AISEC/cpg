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
import de.fraunhofer.aisec.cpg.graph.AccessValues
import de.fraunhofer.aisec.cpg.graph.AstNode
import de.fraunhofer.aisec.cpg.graph.Component
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.Field
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.declarations.Method
import de.fraunhofer.aisec.cpg.graph.declarations.Parameter
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.edges.flows.FullDataflowGranularity
import de.fraunhofer.aisec.cpg.graph.edges.flows.PartialDataflowGranularity
import de.fraunhofer.aisec.cpg.graph.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.expressions.Lambda
import de.fraunhofer.aisec.cpg.graph.expressions.MemberAccess
import de.fraunhofer.aisec.cpg.graph.expressions.MemberCall
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.firstParentOrNull
import de.fraunhofer.aisec.cpg.graph.types.FunctionPointerType
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.ScopedWalker
import de.fraunhofer.aisec.cpg.helpers.functional.getFunctionPointerType
import de.fraunhofer.aisec.cpg.helpers.functional.matchInvokesCandidateSignature
import de.fraunhofer.aisec.cpg.helpers.identitySetOf
import de.fraunhofer.aisec.cpg.helpers.mapFilteredTo
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy
import java.util.*
import java.util.function.Consumer

/**
 * This [Pass] is responsible for resolving dynamic function invokes, i.e.,
 * [de.fraunhofer.aisec.cpg.graph.expressions.Call] nodes that contain a reference/pointer to a
 * function and are being "called". A common example includes C/C++ function pointers.
 *
 * This pass is intentionally split from the [SymbolResolver] because it depends on DFG edges. This
 * split allows the [SymbolResolver] to be run before any DFG passes, which in turn allow us to also
 * populate DFG passes for inferred functions.
 */
@DependsOn(SymbolResolver::class)
@DependsOn(DFGPass::class)
@DependsOn(ControlFlowSensitiveDFGPass::class, softDependency = true)
@DependsOn(PointsToPass::class, softDependency = true)
@Description(
    "Resolves dynamic method invocations and calls of function pointers in the CPG, enhancing the accuracy of call relationships within the graph."
)
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
            is MemberCall -> handleMemberCall(node)
            is Call -> handleCall(node)
        }
    }

    /**
     * Resolves function pointers in a [Call] node. As long as the [Call.callee] has a
     * [FunctionPointerType], we should be able to resolve it.
     */
    private fun handleCall(call: Call) {
        val callee = call.callee
        if (
            // Only try to resolve if the PointsToPass did not manage to find any invokes edge
            call.invokes.isEmpty() &&
                (callee.type is FunctionPointerType ||
                    ((callee as? Reference)?.refersTo is Parameter ||
                        (callee as? Reference)?.refersTo is Variable))
        ) {
            handleCallee(call, callee)
        }
    }

    /**
     * Resolves function pointers in a [MemberCall]. The [MemberCall.callee] is either a binary
     * operator on whose [de.fraunhofer.aisec.cpg.graph.expressions.BinaryOperator.rhs] has a
     * [FunctionPointerType], or -- the common shape -- a [MemberAccess] that reads a function-typed
     * field, as in `e.cfg.EncodeDuration(d, enc)`. The latter is not a method call at all: the
     * symbol resolver finds no method of that name on the record and invents an inferred one, so we
     * also take over when every target is inferred.
     */
    private fun handleMemberCall(call: MemberCall) {
        if (call.invokes.isNotEmpty() && !call.invokes.all { it.isInferred }) {
            // The PointsToPass or the symbol resolver found real targets, nothing to do
            return
        }
        val callee = call.callee
        if (callee is BinaryOperator && callee.rhs.type is FunctionPointerType) {
            handleCallee(call, callee.rhs)
        } else if (callee is MemberAccess && getFunctionPointerType(callee) != null) {
            handleCallee(call, callee)
        }
    }

    private fun handleCallee(call: Call, expr: Expression) {
        val pointerType = getFunctionPointerType(expr) ?: return
        val invocationCandidates = mutableListOf<Function>()
        val work: Deque<Node> = ArrayDeque()
        val seen = identitySetOf<Node>()
        work.push(expr)
        while (work.isNotEmpty()) {
            val curr = work.pop()
            if (!seen.add(curr)) {
                continue
            }

            val isLambda = curr is Variable && curr.initializer is Lambda
            val currentFunction =
                when {
                    isLambda -> (curr as Variable).initializer.let { (it as Lambda).function }
                    // A named function used as a value: `var Get = pool.Get` or
                    // `Register(addKnownTypes)`. What flows here is a reference to the function,
                    // not the declaration node itself, so without this the search walks past the
                    // only target it will ever find.
                    curr is Reference && curr.refersTo is Function -> curr.refersTo
                    else -> curr
                }

            if (currentFunction is Function) {
                // Even if it is a function declaration, the dataflow might just come from a
                // situation where the target of a fptr is passed through via a return value. Keep
                // searching if return type or signature don't match
                if (matchInvokesCandidateSignature(currentFunction, pointerType, isLambda)) {
                    invocationCandidates.add(currentFunction)
                    // We have found a target. Don't follow this path any further, but still
                    // continue the other paths that might be left, as we could have several
                    // potential targets at runtime
                    continue
                }
            }
            // A function can also reach the call site as part of a container rather than on its
            // own: stored in an array or a map, as in a table of constructors
            // `var TypeToRR = map[uint16]func() RR{...}` that is later read back and invoked. The
            // DFG does record that flow, but with a partial granularity naming the index, so
            // following only full flows stops at the container and the call appears to have no
            // target at all.
            //
            // We therefore follow partial flows out of a container as well. This
            // over-approximates -- a value stored under one index may be reported for a read of
            // another -- but the candidate still has to match the function pointer's signature,
            // and for a call graph an additional possible target is the safe direction while a
            // missing one silently removes everything reachable through it.
            //
            // We deliberately do not do the same for a member access. Its partial flow leads to
            // the *base*, i.e. to the whole object, which would merge every field of that object
            // into one set of candidates and lose which function pointer a particular field was
            // assigned. The write-usages of the field below are the precise answer for that case.
            val followPartial = curr !is MemberAccess
            val prevDFGToPush =
                curr.prevDFGEdges.mapFilteredTo(
                    mutableListOf(),
                    {
                        it.granularity is FullDataflowGranularity ||
                            (followPartial && it.granularity is PartialDataflowGranularity<*>)
                    },
                ) {
                    it.start
                }
            if (curr is MemberAccess && prevDFGToPush.isEmpty()) {
                // TODO: This is only a workaround!
                //   If there is nothing found for Members, we may have set the field
                //   somewhere else but do not yet propagate this to this location (e.g. because it
                //   happens in another function). In this case, we look at write-usages to the
                //   field and use all of those. This is only a temporary workaround until someone
                //   implements an interprocedural analysis (for example).
                (curr.refersTo as? Field)
                    ?.usages
                    ?.filter {
                        it.access == AccessValues.WRITE || it.access == AccessValues.READWRITE
                    }
                    ?.let { prevDFGToPush.addAll(it) }
                // Also add the initializer of the field (if it exists)
                (curr.refersTo as? Field)?.initializer?.let { prevDFGToPush.add(it) }
            }

            // A function value very often enters the enclosing function from outside: as the
            // receiver of a method on a defined function type -- `func (f Decorator) Apply(p
            // Processor) { f(p) }` -- or as a parameter, as in `Decorate(c Processor, ds
            // ...Decorator)`, which calls each of the decorators it was handed. Inside the
            // function there is nothing more to follow, so without stepping over the call boundary
            // the search ends at the parameter and the call looks like it has no target. The data
            // flow graph only models that step for inferred functions, so do it here, from the
            // call sites of the function this parameter belongs to.
            // If a reference has no incoming data flow of its own, continue at the declaration it
            // refers to. That is where the step over the call boundary below can pick up.
            if (prevDFGToPush.isEmpty() && curr is Reference) {
                curr.refersTo?.let { prevDFGToPush.add(it) }
            }

            if (prevDFGToPush.isEmpty() && (curr is Parameter || curr is Variable)) {
                val owner = (curr as Node).firstParentOrNull<Function>()
                for (callSite in owner?.calledBy ?: listOf()) {
                    if (curr === (owner as? Method)?.receiver) {
                        (callSite as? MemberCall)?.base?.let { prevDFGToPush.add(it) }
                    } else if (curr is Parameter) {
                        if (curr.isVariadic) {
                            // Everything from this position on is collected into the parameter
                            prevDFGToPush.addAll(callSite.arguments.drop(curr.argumentIndex))
                        } else {
                            callSite.arguments.getOrNull(curr.argumentIndex)?.let {
                                prevDFGToPush.add(it)
                            }
                        }
                    }
                }
            }

            prevDFGToPush.forEach(Consumer(work::push))
        }

        // Keep whatever was there if we found nothing: an inferred target is still better than no
        // target at all, and dropping it would also drop the DFG edges built on top of it.
        if (invocationCandidates.isEmpty() && call.invokes.isNotEmpty()) {
            return
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
