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
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.allChildren
import de.fraunhofer.aisec.cpg.graph.declarations.Field
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.declarations.Method
import de.fraunhofer.aisec.cpg.graph.declarations.Parameter
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.edges.flows.FullDataflowGranularity
import de.fraunhofer.aisec.cpg.graph.edges.flows.PartialDataflowGranularity
import de.fraunhofer.aisec.cpg.graph.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.expressions.ForEach
import de.fraunhofer.aisec.cpg.graph.expressions.InitializerList
import de.fraunhofer.aisec.cpg.graph.expressions.KeyValue
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
    private var keyedInitializers: Map<Pair<Name, String>, List<Expression>> = mapOf()

    /**
     * Largest set of guesses an indirect-call fallback may answer with; see
     * [fallBackToAddressTaken].
     */
    private val fallbackCap: Int = System.getenv("CPG_FALLBACK_CAP")?.toIntOrNull() ?: 40

    /**
     * Functions whose address is taken somewhere -- used as a value rather than called directly --
     * indexed by how many parameters they have, together with every function literal.
     *
     * These are the only functions an indirect call can possibly reach: a function that is never
     * referred to as a value cannot end up in a variable, a field or a container, so it cannot be
     * what `f(x)` invokes.
     */
    private var addressTakenByArity: Map<Int, List<Function>> = mapOf()

    override fun accept(component: Component) {
        inferDfgForUnresolvedCalls = config.inferenceConfiguration.inferDfgForUnresolvedSymbols
        collectKeyedInitializers(component)
        collectAddressTakenFunctions(component)
        walker = ScopedWalker(scopeManager, Strategy::AST_FORWARD)
        walker.registerHandler { node -> handle(node) }

        for (tu in component.translationUnits) {
            walker.iterate(tu)
        }
    }

    /**
     * Collects the functions that are used as a value anywhere in [component], plus every function
     * literal, indexed by parameter count.
     *
     * Following the data flow from an indirect call back to the function it will invoke is a
     * type-flow analysis in its own right, and in the shapes Go libraries are built from -- a
     * literal converted to a defined function type, handed to a registry, pulled out of a map and
     * applied through an interface -- the chain regularly exceeds what the data flow graph models.
     * When the search comes back empty we would otherwise leave the call with no target at all and
     * lose everything reachable behind it.
     *
     * Falling back to every address-taken function of a compatible signature is the standard
     * over-approximation for this, and it stays sound: a function nobody ever refers to as a value
     * cannot be the one being called.
     */
    private fun collectAddressTakenFunctions(component: Component) {
        val byArity = mutableMapOf<Int, MutableList<Function>>()
        val seen = identitySetOf<Function>()

        fun add(function: Function) {
            if (seen.add(function)) {
                byArity.getOrPut(function.parameters.size) { mutableListOf() } += function
            }
        }

        for (lambda in component.allChildren<Lambda>()) {
            lambda.function?.let { add(it) }
        }
        for (reference in component.allChildren<Reference>()) {
            val function = reference.refersTo as? Function ?: continue
            // The callee of a direct call is not an address being taken.
            if ((reference.astParent as? Call)?.callee === reference) {
                continue
            }
            add(function)
        }

        addressTakenByArity = byArity
    }

    /**
     * Collects, per (record, field name), the values that a keyed initializer assigns to that
     * field.
     *
     * Setting a function-typed field from a struct literal is how a whole family of libraries is
     * configured -- `zapcore.EncoderConfig{EncodeLevel: CapitalLevelEncoder, EncodeTime:
     * ISO8601TimeEncoder}` -- but the key of such an initializer is a bare identifier that names a
     * field of the literal's type, not a reference resolved to that field. It therefore does not
     * show up among the field's usages, and the search for the target of `enc.EncodeLevel(l, arr)`
     * has nowhere to go.
     */
    private fun collectKeyedInitializers(component: Component) {
        val collected = mutableMapOf<Pair<Name, String>, MutableList<Expression>>()
        for (list in component.allChildren<InitializerList>()) {
            val recordName = list.type.root.name
            for (keyValue in list.initializers.filterIsInstance<KeyValue>()) {
                val key = (keyValue.key as? Reference)?.name?.localName ?: continue
                collected.getOrPut(recordName to key) { mutableListOf() } += keyValue.value
            }
        }
        keyedInitializers = collected
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
        } else if (callee is MemberAccess && callee.refersTo is Field) {
            // A field we could not type, but a field all the same: `h.cb(x)` is an indirect call
            // whatever we managed to work out about `cb`.
            handleCallee(call, callee)
        }
    }

    private fun handleCallee(call: Call, expr: Expression) {
        val pointerType = getFunctionPointerType(expr)
        if (pointerType == null) {
            // We know this is an indirect call -- the callee is a variable, a parameter or a
            // field -- but not what its signature is, because the type never resolved. Signature
            // matching is then impossible and the arity of the call site is the only thing left
            // to go on. Without this the call keeps no target at all; the remaining function
            // value misses on the validation corpus are almost all of this shape.
            fallBackToAddressTaken(call, null)
            return
        }
        val invocationCandidates = mutableListOf<Function>()
        val work: Deque<Node> = ArrayDeque()
        val seen = identitySetOf<Node>()
        work.push(expr)
        while (work.isNotEmpty()) {
            val curr = work.pop()
            if (!seen.add(curr)) {
                continue
            }

            // A function literal does not have to be bound to a variable first. It is just as
            // often written where it is handed over -- returned from a factory, passed as an
            // argument, or converted to a defined function type, as in
            // `return ProcessWith(func(e string) string { ... })`. In that case the literal
            // itself is what flows here, and looking only inside variables walks straight past
            // the one target the call has.
            val lambda = curr as? Lambda ?: (curr as? Variable)?.initializer as? Lambda
            val isLambda = lambda != null
            val currentFunction =
                when {
                    lambda != null -> lambda.function
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
                // ... and whatever a struct literal assigned to it, which is not a usage of the
                // field (see collectKeyedInitializers).
                (curr.refersTo as? Field)?.let { field ->
                    field.name.parent?.let { owner ->
                        keyedInitializers[owner to field.name.localName]?.let {
                            prevDFGToPush.addAll(it)
                        }
                    }
                }
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

            // A loop variable over a container of functions holds whatever the container holds:
            // `for _, set := range settings { set(s) }` is the shape every "functional options"
            // API in Go is built on. Continue at the container, from where the partial flows out
            // of it and the step over the call boundary below can carry on.
            if (prevDFGToPush.isEmpty() && curr is Variable) {
                val loop = curr.firstParentOrNull<ForEach>()
                val declaredByLoop =
                    loop != null &&
                        (curr in loop.locals ||
                            (loop.variable as? DeclarationStatement)
                                ?.declarations
                                ?.contains(curr) == true)
                if (declaredByLoop) {
                    loop?.iterable?.let { prevDFGToPush.add(it) }
                }
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

        // The data flow did not lead anywhere. Rather than leave the call without a target, take
        // every address-taken function the pointer's signature admits; see
        // [collectAddressTakenFunctions].
        if (invocationCandidates.isEmpty()) {
            addressTakenByArity[pointerType.parameters.size]?.filterTo(invocationCandidates) {
                matchInvokesCandidateSignature(it, pointerType, false)
            }
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

    /**
     * Last resort for an indirect call we could not type: every address-taken function that could
     * accept this many arguments. With [pointerType] known the signature narrows it down, otherwise
     * only the arity does.
     */
    private fun fallBackToAddressTaken(call: Call, pointerType: FunctionPointerType?) {
        if (call.invokes.isNotEmpty()) {
            return
        }
        val candidates =
            addressTakenByArity[call.arguments.size]?.filter {
                pointerType == null || matchInvokesCandidateSignature(it, pointerType, false)
            } ?: return
        if (candidates.isEmpty() || candidates.size > fallbackCap) {
            return
        }
        call.invokes = candidates.toMutableList()
        call.invokeEdges.forEach { it.dynamicInvoke = true }
        DFGPass(ctx).handlePreviouslyUnresolvedCallExpression(call, inferDfgForUnresolvedCalls)
    }

    override fun cleanup() {
        // Nothing to do
    }
}
