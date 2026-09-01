/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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
@file:Suppress("CONTEXT_RECEIVERS_DEPRECATED")

package de.fraunhofer.aisec.cpg.passes

import de.fraunhofer.aisec.cpg.*
import de.fraunhofer.aisec.cpg.CallResolutionResult.SuccessKind.*
import de.fraunhofer.aisec.cpg.frontends.*
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder
import de.fraunhofer.aisec.cpg.graph.expressions.*
import de.fraunhofer.aisec.cpg.graph.expressions.operatorCallFromDeclaration
import de.fraunhofer.aisec.cpg.graph.scopes.LocalScope
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.graph.scopes.Symbol
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.helpers.IdentitySet
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.ScopedWalker
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.helpers.functional.ConcurrentMapLattice
import de.fraunhofer.aisec.cpg.helpers.functional.Lattice
import de.fraunhofer.aisec.cpg.helpers.functional.PowersetLattice
import de.fraunhofer.aisec.cpg.helpers.identitySetOf
import de.fraunhofer.aisec.cpg.helpers.replace
import de.fraunhofer.aisec.cpg.passes.configuration.DependsOn
import de.fraunhofer.aisec.cpg.passes.inference.startInference
import de.fraunhofer.aisec.cpg.passes.inference.tryFieldInference
import de.fraunhofer.aisec.cpg.passes.inference.tryFunctionInference
import de.fraunhofer.aisec.cpg.passes.inference.tryFunctionInferenceFromFunctionPointer
import de.fraunhofer.aisec.cpg.passes.inference.tryVariableInference
import de.fraunhofer.aisec.cpg.processing.IVisitor
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy
import kotlin.collections.firstOrNull
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A mapping of [LocalScope]s (block/loop/catch/comprehension scopes - see
 * [ScopeManager.enterScope]'s dispatch) to the set of [Declaration]s that have been reached, along
 * the specific EOG path represented by this lattice element, by the program point it is associated
 * with. This is the actual flow-sensitive state threaded through [Lattice.iterateEOG] in
 * [SymbolResolver.acceptWithIterateEOG]: unlike every other kind of [Scope]
 * (global/namespace/record/function, whose [Scope.symbols] is fully known before any pass runs and
 * is therefore read directly, unaffected by EOG position), a [LocalScope]'s visible declarations
 * genuinely depend on how far the EOG has been traversed - most simply, "used before declared"
 * within the same block.
 */
typealias LocalDeclarationLattice =
    ConcurrentMapLattice<LocalScope, PowersetLattice.Element<Declaration>>

typealias LocalDeclarationElement =
    ConcurrentMapLattice.Element<LocalScope, PowersetLattice.Element<Declaration>>

/**
 * Creates new connections between the place where a variable is declared and where it is used.
 *
 * A field access is modeled with a [MemberAccess]. After AST building, its base and member
 * references are set to [Reference] stubs. This pass resolves those references and makes the member
 * point to the appropriate [Field] and the base to the "this" [Field] of the containing class. It
 * is also capable of resolving references to fields that are inherited from a superclass and thus
 * not declared in the actual base class. When base or member declarations are not found in the
 * graph, a new "inferred" [Field] is being created that is then used to collect all usages to the
 * same unknown declaration. [Reference] stubs are removed from the graph after being resolved.
 *
 * Accessing a local variable is modeled directly with a [Reference]. This step of the pass doesn't
 * remove the [Reference] nodes like in the field usage case but rather makes their "refersTo" point
 * to the appropriate [ValueDeclaration].
 *
 * Resolves [Call] and [New] targets.
 *
 * A [Call] specifies the method that wants to be called via [Call.name]. The call target is a
 * method of the same class the caller belongs to, so the name is resolved to the appropriate
 * [Method]. This pass also takes into consideration that a method might not be present in the
 * current class, but rather has its implementation in a superclass, and sets the pointer
 * accordingly.
 *
 * Constructor calls with [Construction] are resolved in such a way that their
 * [Construction.instantiates] points to the correct [Record]. Additionally, the
 * [Construction.constructor] is set to the according [Constructor].
 *
 * This pass should NOT use any DFG edges because they are computed / adjusted in a later stage.
 */
@DependsOn(TypeResolver::class)
@DependsOn(TypeHierarchyResolver::class)
@DependsOn(EvaluationOrderGraphPass::class)
@DependsOn(ImportResolver::class)
@Description(
    "Resolves symbols in the CPG, linking variable and function usages (i.e., refersTo and calledBy/invokes edges) to their respective declarations. Generates the call graph."
)
open class SymbolResolver(ctx: TranslationContext) : EOGStarterPass(ctx) {

    /** Configuration for the [SymbolResolver]. */
    class Configuration(
        /** If set to true, the resolver will skip unreachable EOG edges. */
        val skipUnreachableEOG: Boolean = false,

        /**
         * If set to true, the resolver will ignore [Declaration] nodes that are on EOG paths that
         * are [EvaluationOrder.unreachable].
         */
        val ignoreUnreachableDeclarations: Boolean = false,

        /**
         * If set to true, the [SymbolResolver] will use an experimental feature that is based on
         * the EOG iteration. This is not yet finished and will probably not resolve all the symbols
         * that the regular resolver would resolve.
         */
        val experimentalEOGWorklist: Boolean = false,
    ) : PassConfiguration()

    protected lateinit var walker: ScopedWalker<Node>

    protected val templateList = mutableListOf<Template>()

    /** Our configuration. */
    var passConfig = passConfig<Configuration>()

    /**
     * An optional override for where a [de.fraunhofer.aisec.cpg.graph.scopes.Scope]'s directly
     * declared symbols come from during a [handleReference] lookup, forwarded to
     * [ScopeManager.lookupSymbolByNodeName] as `localSymbols`. `null` (the default) means every
     * lookup uses [de.fraunhofer.aisec.cpg.graph.scopes.Scope.symbols] as usual, which is what the
     * default, non-flow-sensitive traversal in [accept] relies on. [acceptWithIterateEOG] sets this
     * to answer "which declarations have been reached by this point in the EOG" for
     * [de.fraunhofer.aisec.cpg.graph.scopes.LocalScope]s specifically, making local (block-scoped)
     * references flow-sensitive while leaving every other kind of scope untouched.
     */
    protected var localSymbolsOverride: ((Scope, Symbol) -> List<Declaration>?)? = null

    /**
     * If [Configuration.ignoreUnreachableDeclarations] is enabled, this predicate will filter
     * candidates whether they are [EvaluationOrder.unreachable]. If the declaration has ONLY
     * unreachable incoming EOG edges, we ignore them.
     */
    private val eogPredicate: ((Declaration) -> Boolean)? =
        if (passConfig?.ignoreUnreachableDeclarations == true) {
            { declaration ->
                if (declaration is Function) {
                        declaration.astParent
                    } else {
                        declaration
                    }
                    ?.prevEOGEdges
                    ?.all { edge -> !edge.unreachable } == true
            }
        } else {
            null
        }

    override fun accept(eogStarter: Node) {
        ctx.currentComponent = eogStarter.firstParentOrNull<Component>()
        cacheTemplates(ctx.currentComponent)

        walker =
            ScopedWalker(
                scopeManager,
                if (passConfig?.skipUnreachableEOG == true) {
                    Strategy::REACHABLE_EOG_FORWARD
                } else {
                    Strategy::EOG_FORWARD
                },
            )

        if (passConfig?.experimentalEOGWorklist == true) {
            acceptWithIterateEOG(eogStarter)
        } else {
            walker.clearCallbacks()
            walker.registerHandler(this::handle)

            walker.iterate(eogStarter)
        }
    }

    override fun cleanup() {
        templateList.clear()
    }

    override fun finalCleanup() {
        componentsToTemplates.clear()
    }

    /**
     * This function resolves symbols for the given EOG starter [t] (see [EOGStarterHolder] - e.g. a
     * [Function], but also a translation unit, record, namespace, or a field/variable with an
     * initializer) by driving [handle] with [Lattice.iterateEOG] - the same worklist/fixpoint
     * engine used by [PointsToPass], [ControlDependenceGraphPass] and [UnreachableEOGPass] -
     * instead of the [ScopedWalker]-based linear traversal that the default (non-experimental) code
     * path in [accept] uses.
     *
     * Resolution is reused verbatim from [handle] and everything it dispatches to
     * (member/call/construction/operator-overload resolution, access control, implicit receivers,
     * etc.); only the visibility of [LocalScope]-declared symbols is genuinely flow-sensitive, via
     * [LocalDeclarationLattice] and [localSymbolsOverride]. Every other [Scope] kind is still read
     * directly from [Scope.symbols], exactly like the default traversal - both because that's
     * correct (a global/record/namespace symbol's visibility never depends on the EOG) and because
     * most of it (parameters, fields, globals) is never reached by any EOG walk at all.
     *
     * A node can be reached via more than one incoming [EvaluationOrder] edge - not just a loop
     * back-edge (which the engine's fixpoint machinery already only re-processes until
     * convergence), but also a genuine, non-cyclic merge, e.g. the first node after an `if`/`else`
     * where neither branch terminates. [transfer] is offered such a node once per incoming edge,
     * each with its own, separate slice of lattice state. [LocalDeclarationLattice]'s own `lub` (a
     * union, at both the map and the inner-set level) is exactly the merge semantics we want for
     * the *flow state* here - a declaration reached along *either* incoming path is considered
     * reached (matching this tool's general lean towards best-effort resolution over strict
     * soundness) - but applying [handle] itself needs a *separate*, non-lattice, all-or-nothing
     * "already handled" marker: tracking that in the per-edge lattice state would only catch the
     * loop-reconvergence case, not a true merge, since neither incoming edge's own state has a
     * record of the other having already processed their shared successor. [handledNodes] is that
     * marker: a plain, closure-captured identity set shared across every [transfer] call for this
     * starter, safe without synchronization since the engine drives them strictly sequentially.
     *
     * [handleOverloadedOperator] additionally physically replaces the [BinaryOperator]/
     * [UnaryOperator] node with an [OperatorCall], rewiring its EOG edges in the process.
     * [Lattice.iterateEOG] determines how to continue the traversal by reading
     * [EvaluationOrder.end]`.nextEOGEdges` of the edge it just processed - but a replaced node is
     * disconnected and has no outgoing EOG edges of its own anymore, so mutating the graph mid-
     * traversal would make the engine think the EOG ends right there and abandon everything after
     * it. We therefore only run [handle] on [BinaryOperator]/[UnaryOperator] nodes (which is the
     * only way [handleOverloadedOperator] is reached, since [handle] dispatches [MemberAccess] and
     * [Call] - the only other [HasOverloadedOperation] implementers - to their own handlers first)
     * *after* the EOG traversal has fully finished, in the order they were encountered.
     */
    protected open fun acceptWithIterateEOG(t: Node) {
        // Nodes for which handle has already been applied, tracked globally across the whole
        // traversal (see the KDoc above for why this can't live in the per-edge lattice state).
        val handledNodes = identitySetOf<Node>()

        // Nodes that may replace themselves in the AST/EOG (see the KDoc above) are deferred here
        // and only handled once the EOG traversal itself is done.
        val deferredOperatorNodes = mutableListOf<Node>()

        // The scope t itself introduces (e.g. a Function's FunctionScope), used to tell apart a
        // LocalScope that belongs to this starter's own traversal (and is therefore genuinely
        // flow-sensitive here) from one belonging to an *enclosing*, already fully-resolved starter
        // (e.g. a captured variable in an outer function, when t is a nested function/lambda) - the
        // latter must fall back to the ordinary, static Scope.symbols lookup instead of appearing
        // "not yet declared".
        val starterScope = ctx.scopeManager.lookupScope(t)

        val lattice = LocalDeclarationLattice(PowersetLattice<Declaration>())
        var startState = LocalDeclarationElement()

        // Some declarations belonging to a LocalScope are never themselves the *target* of an EOG
        // edge - e.g. a CatchClause's exception parameter, the individual target Variables of a
        // tuple/multiple declaration, or a locally-declared function prototype (itself its own,
        // separate EOG starter, possibly with a nonempty nextEOGEdges of its own for evaluating a
        // default argument value - but never reached *from* this starter's own traversal, since
        // nothing has an edge pointing into it) - exactly like a Function's Parameters are never
        // reached via EOG either. The relevant criterion for "never reached" is having no
        // *incoming* EOG edge; an unrelated, nonempty outgoing chain of its own doesn't change that
        // nothing in *this* traversal will ever visit it. Where exactly such a declaration should
        // become visible depends on how it's declared (see [seedPlanFor]): some are genuinely
        // atomic with entering their enclosing construct (a catch parameter is in scope for the
        // whole catch clause), while others (a local function prototype, the individual variables
        // of a tuple/multiple declaration) are declared "at a point" - like an ordinary local
        // variable - and must only become visible from there onward, not from the very start.
        val startSeeds = mutableMapOf<LocalScope, MutableList<Declaration>>()
        val anchoredSeeds = mutableMapOf<Node, MutableList<Declaration>>()
        (t as? AstNode)
            ?.allChildren<Declaration>()
            ?.filter { it.scope is LocalScope && it.prevEOGEdges.isEmpty() }
            ?.forEach { declaration ->
                val scope = declaration.scope as LocalScope
                when (val plan = seedPlanFor(declaration, t)) {
                    is SeedPlan.AtStart ->
                        startSeeds.getOrPut(scope) { mutableListOf() } += declaration
                    is SeedPlan.AtAnchor ->
                        anchoredSeeds.getOrPut(plan.anchor) { mutableListOf() } += declaration
                }
            }
        if (startSeeds.isNotEmpty()) {
            val seededElement =
                LocalDeclarationElement(
                    startSeeds.map { (scope, decls) ->
                        scope to PowersetLattice.Element(*decls.toTypedArray())
                    }
                )
            startState = runBlocking { lattice.lub(startState, seededElement, true) }
        }

        val (_, timeout) =
            lattice.iterateEOG(
                t.nextEOGEdges,
                startState,
                transformation = { l, edge, state ->
                    transfer(
                        l,
                        edge,
                        state,
                        handledNodes,
                        deferredOperatorNodes,
                        starterScope,
                        anchoredSeeds,
                    )
                },
            )
        if (timeout) {
            log.warn("Could not compute final state for EOG starter {} (due to timeout)", t.name)
        }

        deferredOperatorNodes.forEach {
            jumpToScope(it)
            handle(it)
        }

        localSymbolsOverride = null
    }

    /**
     * How a [Declaration] with no incoming EOG edge (see [acceptWithIterateEOG]) should be made
     * visible in [LocalDeclarationLattice].
     */
    private sealed class SeedPlan {
        /** Visible from the very start of the enclosing EOG starter's traversal. */
        object AtStart : SeedPlan()

        /**
         * Visible once [anchor] has *finished* being handled, i.e. from its own lexical declaration
         * point onward.
         */
        data class AtAnchor(val anchor: Node) : SeedPlan()
    }

    /**
     * Determines the [SeedPlan] for [declaration]. If [declaration] is itself a direct element of
     * some [StatementHolder]'s statement list (e.g. a locally-declared function prototype, or the
     * [de.fraunhofer.aisec.cpg.graph.declarations.Tuple] wrapping the individual variables of a
     * tuple/multiple declaration), it is declared "at a point" like an ordinary local variable.
     *
     * We anchor it to the *preceding* sibling statement that is EOG-reachable (searching further
     * back over any other EOG-invisible siblings, e.g. several prototypes declared back to back),
     * with the convention that the declaration becomes visible once that anchor has *finished*
     * being handled - not to the *next* reachable sibling with a "becomes visible before it" rule:
     * a compound statement's own sub-expressions (e.g. a call's callee reference) are reached via
     * the EOG *before* the statement node itself, so anchoring forward and seeding beforehand would
     * still be one step too late for anything referencing the declaration within that very anchor
     * statement. If no such preceding sibling exists (declaration is at/near the start of the
     * block), it is visible from the start of this traversal - nothing in this scope could have
     * referenced it earlier anyway.
     *
     * If we walk all the way up to [starterRoot] without ever finding a statement-list membership
     * for [declaration] at all, it must instead be a structural part of its enclosing construct
     * itself (e.g. a [de.fraunhofer.aisec.cpg.graph.expressions.CatchClause]'s exception
     * parameter), which is visible for that whole construct, i.e. from the start of this traversal.
     */
    private fun seedPlanFor(declaration: Declaration, starterRoot: Node): SeedPlan {
        var current: Node = declaration
        while (current !== starterRoot) {
            val parent = current.astParent ?: return SeedPlan.AtStart
            val statements = (parent as? StatementHolder)?.statements
            if (statements != null && current in statements) {
                val index = statements.indexOf(current)
                val anchor =
                    statements.subList(0, index).lastOrNull { it.prevEOGEdges.isNotEmpty() }
                return if (anchor != null) SeedPlan.AtAnchor(anchor) else SeedPlan.AtStart
            }
            current = parent
        }
        return SeedPlan.AtStart
    }

    /**
     * Whether [scope] is (transitively) nested within [starterScope], i.e. whether it belongs to
     * the EOG starter currently being processed rather than to an enclosing one. If [starterScope]
     * is `null` (the starter itself introduces no scope, e.g. a bare field/variable initializer),
     * we conservatively treat every [LocalScope] as belonging to it - such starters are simple
     * enough (and any nested comprehension/lambda-with-outer-capture inside one is rare enough)
     * that this is an acceptable simplification for now.
     */
    private fun isWithinStarter(scope: Scope, starterScope: Scope?): Boolean {
        if (starterScope == null) {
            return true
        }
        var current: Scope? = scope
        while (current != null) {
            if (current === starterScope) {
                return true
            }
            current = current.parent
        }
        return false
    }

    /**
     * The state-transfer function used by [acceptWithIterateEOG]. If [EvaluationOrder.end] is
     * itself a [Declaration] in a [LocalScope], it is pushed into [state]. [handle] is then applied
     * to the node the first time it is reached (tracked in [handledNodes], not [state] - see the
     * KDoc on [acceptWithIterateEOG]), with [localSymbolsOverride] pointed at the resulting state
     * so [handleReference] resolves flow-sensitively for [LocalScope]s. [BinaryOperator]/
     * [UnaryOperator] nodes are special-cased: their type is propagated immediately (see
     * [propagateOperatorType]), but the AST-mutating [handleOverloadedOperator] is deferred by
     * appending them to [deferredOperatorNodes] instead of calling [handle] on them right away.
     * Finally, any declarations anchored to [EvaluationOrder.end] via [anchoredSeeds] (see
     * [seedPlanFor]) are pushed into the returned state *after* handling the node, since they
     * become visible only once their anchor has finished, not before.
     */
    private suspend fun transfer(
        lattice: Lattice<LocalDeclarationElement>,
        currentEdge: EvaluationOrder,
        state: LocalDeclarationElement,
        handledNodes: IdentitySet<Node>,
        deferredOperatorNodes: MutableList<Node>,
        starterScope: Scope?,
        anchoredSeeds: Map<Node, List<Declaration>>,
    ): LocalDeclarationElement {
        val lattice = lattice as? LocalDeclarationLattice ?: return state
        val node = currentEdge.end

        var newState = state

        val declarationScope = (node as? Declaration)?.scope
        if (declarationScope is LocalScope) {
            newState =
                lattice.lub(
                    newState,
                    LocalDeclarationElement(declarationScope to PowersetLattice.Element(node)),
                    true,
                )
        }

        if (handledNodes.add(node)) {
            jumpToScope(node)
            localSymbolsOverride = { scope, symbol ->
                if (scope is LocalScope && isWithinStarter(scope, starterScope)) {
                    newState[scope]?.filter { it.name.localName == symbol } ?: emptyList()
                } else {
                    // Not a LocalScope we're tracking flow-sensitively (either not a LocalScope
                    // at all, or one belonging to an enclosing, already-resolved starter): fall
                    // back to the default, static Scope.symbols[symbol] lookup.
                    null
                }
            }

            if (node is BinaryOperator || node is UnaryOperator) {
                propagateOperatorType(node)
                deferredOperatorNodes += node
            } else {
                handle(node)
            }
        }

        val anchored = anchoredSeeds[node]
        if (!anchored.isNullOrEmpty()) {
            val byScope = anchored.groupBy { it.scope as LocalScope }
            newState =
                lattice.lub(
                    newState,
                    LocalDeclarationElement(
                        byScope.map { (scope, decls) ->
                            scope to PowersetLattice.Element(*decls.toTypedArray())
                        }
                    ),
                    true,
                )
        }

        return newState
    }

    /**
     * Several of the handlers reached through [handle] (e.g. [handleReference]'s implicit-receiver
     * fallback, which reads [ScopeManager.currentRecord]) rely on [ScopeManager.currentScope]
     * reflecting [node]'s own scope, exactly like [ScopedWalker] keeps it in sync while walking.
     * [Lattice.iterateEOG] has no notion of "current scope", so we have to update it ourselves
     * before handling each node.
     */
    private fun jumpToScope(node: Node) {
        if (scopeManager.currentScope != node.scope) {
            scopeManager.jumpTo(node.scope)
        }
    }

    /**
     * [handle] does not compute the type of a [BinaryOperator] or [UnaryOperator] itself; normally,
     * it relies on [HasType]'s reactive [HasType.TypeObserver] mechanism to propagate the type of
     * [BinaryOperator.lhs]/[BinaryOperator.rhs] (or [UnaryOperator.input]) once they are resolved.
     * That mechanism can be switched off entirely via
     * [TranslationConfiguration.Builder.disableTypeObserver], in which case nothing else computes
     * these types. Since the EOG guarantees [node]'s operands were already handled (and thus have
     * their final type) by the time [node] itself is reached, we can compute the type here
     * directly, exactly mirroring what the reactive path would have done.
     */
    private fun propagateOperatorType(node: Node) {
        when (node) {
            is BinaryOperator ->
                node.type =
                    node.language.propagateTypeOfBinaryOperation(
                        node.operatorCode,
                        node.lhs.type,
                        node.rhs.type,
                        node,
                    )
            is UnaryOperator ->
                node.type =
                    node.language.propagateTypeOfUnaryOperation(node.operatorCode, node.input.type)
        }
    }

    /**
     * This function caches all [Template]s into [templateList]. It either fetches the existing
     * result from [componentsToTemplates] or fills [templateList] for the first time and then
     * stores this result.
     */
    private fun cacheTemplates(component: Component?) {
        if (component in componentsToTemplates) {
            componentsToTemplates[component]?.let { templateList.addAll(it) }
            return
        }

        component?.let {
            it.translationUnits.forEach { tu ->
                tu.accept(
                    Strategy::AST_FORWARD,
                    object : IVisitor<AstNode>() {
                        override fun visit(t: AstNode) {
                            if (t is Template) {
                                templateList.add(t)
                            }
                        }
                    },
                )
            }
            componentsToTemplates[it] = templateList
        }
    }

    /**
     * This function handles symbol resolving for a [Reference]. After a successful lookup of the
     * symbol contained in [Reference.name], the property [Reference.refersTo] is set to the best
     * (or only) candidate.
     *
     * On a high-level, it performs the following steps:
     * - Use [ScopeManager.lookupSymbolByName] to retrieve [Declaration] candidates based on the
     *   [Reference.name]. This can either result in an "unqualified" or "qualified" lookup,
     *   depending on the name.
     * - The results of the lookup are stored in [Reference.candidates]. The purpose of this is
     *   two-fold. First, it is a good way to debug potential symbol resolution errors. Second, it
     *   is used by other functions, for example [handleCall], which then picks the best viable
     *   option out of the candidates (if the reference is part of the [Call.callee]).
     * - In the next step, we need to decide whether we are resolving a standalone reference (which
     *   most likely points to a [Variable]) or if we are part of a [Call.callee]. In the first
     *   case, we can directly assign [Reference.refersTo] based on the candidates (at the moment we
     *   only assign it if we have exactly one candidate). In the second case, we are finished and
     *   let [handleCall] take care of the rest once the EOG reaches the appropriate [Call] (which
     *   should actually be just be the next EOG node).
     */
    protected open fun handleReference(ref: Reference) {
        val language = ref.language
        val helperType = ref.resolutionHelper?.type
        val record = scopeManager.currentRecord

        // Ignore references to anonymous identifiers, if the language supports it (e.g., the _
        // identifier in Go)
        if (
            language is HasAnonymousIdentifier && ref.name.localName == language.anonymousIdentifier
        ) {
            return
        }

        // Ignore references to "super" if the language has super expressions, because they will be
        // handled separately in handleMemberAccess
        if (language is HasSuperClasses && ref.name.localName == language.superClassKeyword) {
            return
        }

        // If our resolution helper indicates that this reference is the target of a variable with a
        // function pointer, we need to take the (return) type arguments of the function pointer
        // into consideration
        val predicate: ((Declaration) -> Boolean)? =
            if (helperType is FunctionPointerType) {
                { declaration ->
                    if (declaration is Function) {
                        declaration.returnTypes == listOf(helperType.returnType) &&
                            declaration.matchesSignature(helperType.parameters) !=
                                IncompatibleSignature
                    } else {
                        false
                    } && eogPredicate?.invoke(declaration) != false
                }
            } else {
                eogPredicate
            }

        // Find a list of candidate symbols. In most cases, we can just perform a lookup by name
        // which either performs an unqualified lookup beginning from the current scope "upwards",
        // or a qualified lookup starting from the scope specified in the name.
        var candidates =
            scopeManager
                .lookupSymbolByNodeName(
                    ref,
                    localSymbols = localSymbolsOverride,
                    predicate = predicate,
                )
                .toSet()

        // But we have to consider one special case: For languages, that support implicit receivers,
        // this reference might be a member access of either the current class or a parent class.
        // While a regular lookup would only consider the current scope, we have to consider the
        // parent classes as well, which is exactly what resolveMemberByName does. We could probably
        // get around this if we would include the symbols of the parent class somehow in the child
        // class as a sort of "sibling" scope, but we do not have that (yet).
        if (
            language is HasImplicitReceiver &&
                candidates.isEmpty() &&
                !ref.name.isQualified() &&
                record != null
        ) {
            candidates = resolveMemberByName(ref.name.localName, setOf(record.toType()))
        }

        // Drop candidates that are invisible to this reference because of internal linkage: a
        // declaration with [Visibility.INTERNAL] (e.g. a file-scope `static` in C/C++) is confined
        // to its own translation unit and must not be resolved from another one.
        candidates = candidates.onlyVisibleFrom(ref)

        // Store the candidates in the reference
        ref.candidates = candidates

        // We need to choose the best viable candidate out of the ones we have for our reference.
        // Hopefully we have only one, but there might be instances where more than one is a valid
        // candidate. We let the language have a chance at overriding the default behaviour (which
        // takes only a single one).
        val wouldResolveTo = language.bestViableReferenceCandidate(ref)

        // For now, we still separate the resolving of simple variable references from call
        // resolving. Therefore, we need to stop here if we are the callee of a call and continue in
        // handleCall.
        //
        // However, there is a special case that we want to catch, that is if we are "calling" a
        // reference to a variable (or parameter). This can be done in several languages, e.g., in
        // C/C++ as function pointers or in Go as function references. In this case, we want to
        // resolve the reference of this call expression back to its original declaration, and then
        // we later continue in the DynamicInvokeResolver, which sets the invokes edge.
        if (
            ref.resolutionHelper is Call &&
                (wouldResolveTo !is Variable && wouldResolveTo !is Parameter)
        ) {
            return
        }

        // Only consider resolving, if the language frontend did not specify a resolution. If we
        // already have populated the wouldResolveTo variable, we can re-use this instead of
        // resolving again
        var refersTo = ref.refersTo ?: wouldResolveTo

        // If we did not resolve the reference up to this point, we can try to infer the declaration
        if (refersTo == null) {
            // If it's a function pointer, we can try to infer a function
            refersTo =
                if (helperType is FunctionPointerType) {
                    tryFunctionInferenceFromFunctionPointer(ref, helperType)
                } else {
                    // Otherwise, we can try to infer a variable
                    tryVariableInference(ref)
                }
        }

        if (refersTo != null) {
            ref.refersTo = refersTo
        } else {
            Util.warnWithFileLocation(ref, log, "Did not find a declaration for ${ref.name}")
        }

        ref.markClean()
    }

    /**
     * Narrows this set of resolution candidates to those that are *visible* from [ref] given their
     * linkage — the linkage-level counterpart to the access-control filter [onlyAccessibleFrom].
     * Currently the only linkage restriction modeled is internal linkage: a declaration with
     * [Visibility.INTERNAL] (in C/C++ a file-scope `static`, mapped by the frontend via
     * [de.fraunhofer.aisec.cpg.frontends.Language.applyModifiers]) is confined to its own
     * translation unit, so it must not be resolved from a reference in a different one. This is
     * what makes cross-translation-unit lookups of `static` globals and functions fail, as the
     * language semantics require. The name is intentionally kept general so that further linkage
     * kinds (should another language need them) can be folded in here without renaming.
     *
     * Candidates without internal linkage are always kept, so languages that never assign
     * [Visibility.INTERNAL] are completely unaffected. As internal linkage is comparatively rare,
     * we avoid resolving [ref]'s translation unit unless at least one candidate actually has it.
     *
     * Unlike the access-control filter [onlyAccessibleFrom], this one is intentionally *not* gated
     * behind a language trait: the meaning of [Visibility.INTERNAL] — "confined to its own
     * translation unit" — is language-independent, so a frontend only ever assigns it when it truly
     * holds. Enforcing it unconditionally therefore cannot wrongly hide a reachable declaration the
     * way enforcing a merely *recorded* `private` could, which is why access control needs the
     * [HasVisibilityModifiers] opt-in and linkage does not.
     */
    private fun Set<Declaration>.onlyVisibleFrom(ref: Reference): Set<Declaration> {
        if (none { it.hasInternalLinkage }) {
            return this
        }

        val referencingUnit = ref.translationUnit
        return filterTo(mutableSetOf()) { candidate ->
            !candidate.hasInternalLinkage || candidate.translationUnit == referencingUnit
        }
    }

    /**
     * This function handles resolving of a [MemberAccess] in the [ScopeManager.currentRecord]. This
     * works similar to [handleReference]. First, we set the [MemberAccess.candidates] based on
     * [resolveMemberByName], which internally calls [ScopeManager.lookupSymbolByName] based on the
     * current class and its parent classes. Then, if we resolve a [MemberCall], we abort (and later
     * pick up resolving in [handleCall]). In case of a field access, we set the
     * [MemberAccess.refersTo] based on [Language.bestViableReferenceCandidate].
     */
    protected open fun handleMemberAccess(current: MemberAccess) {
        // Some locals for easier smart casting
        val base = (current.base as? PointerDereference)?.input ?: current.base
        val language = current.language
        val record = scopeManager.currentRecord

        // We need to adjust certain types of the base in case of a "super" expression, and we
        // delegate this to the language. If that is successful, we can continue with regular
        // resolving.
        if (
            language is HasSuperClasses &&
                record != null &&
                base is Reference &&
                base.name.localName == language.superClassKeyword
        ) {
            with(language) { handleSuperExpression(current, record) }
        }

        // Handle a possible overloaded operator->. If we find an overloaded operator, this inserts
        // an additional operator expression in-between the existing member expression and the base
        // and also affects the base type.
        val baseType = resolveOverloadedArrowOperator(current) ?: base.type.root

        // Find candidates based on possible base types
        val (possibleTypes, _) = getPossibleContainingTypes(current)
        current.candidates = resolveMemberByName(current.name.localName, possibleTypes)

        // For legacy reasons, resolving of simple variable references (including fields) is
        // separated from call resolving. Therefore, we need to stop here if we are the callee of a
        // member call and continue in handleCall. But we can already make
        // handleCall a bit cleaner, if we set the candidates here, similar to what we do
        // in handleReference.
        val helper = current.resolutionHelper
        if (helper is MemberCall) {
            return
        }

        // We need to choose the best viable candidate out of the ones we have for our reference.
        // Hopefully we have only one, but there might be instances where more than one is a valid
        // candidate. We let the language have a chance at overriding the default behaviour (which
        // takes only a single one).
        val wouldResolveTo = language.bestViableReferenceCandidate(current)

        var refersTo = current.refersTo ?: wouldResolveTo

        if (refersTo == null && baseType is ObjectType) {
            refersTo = tryFieldInference(current, baseType)
        }

        current.refersTo = refersTo
    }

    /**
     * This function resolves a possible overloaded -> (arrow) operator, for languages which support
     * operator overloading. The implicit call to the overloaded operator function is inserted as
     * base for the MemberAccess. This can be the case for a [MemberAccess] or [MemberCall]
     */
    private fun resolveOverloadedArrowOperator(ex: Expression): Type? {
        var type: Type? = null
        if (
            ex.language is HasOperatorOverloading &&
                ex is MemberAccess &&
                ex.operatorCode == "->" &&
                ex.base.type !is PointerType
        ) {
            val result = resolveOperator(ex)
            val op = result?.bestViable?.singleOrNull()
            if (result?.success == SUCCESSFUL && op is Operator) {
                type = op.returnTypes.singleOrNull()?.root ?: unknownType()

                // We need to insert a new operator call expression in between
                val call = operatorCallFromDeclaration(op, ex)

                // Make the call our new base
                ex.base = call
            }
        }

        return type
    }

    /**
     * The central entry-point for all symbol-resolving. It dispatches the handling of the node to
     * the appropriate function based on the node type. Both traversal strategies ([accept]'s
     * default [ScopedWalker] path and [acceptWithIterateEOG]) funnel through this single
     * dispatcher.
     */
    protected open fun handle(node: Node?) {
        when (node) {
            is MemberAccess -> handleMemberAccess(node)
            is Reference -> handleReference(node)
            is Construction -> handleConstruction(node)
            is Call -> handleCall(node)
            is HasOverloadedOperation -> handleOverloadedOperator(node)
        }

        // Mark the node as "clean"
        node?.markClean()
    }

    /**
     * This function handles the resolution of a [Call] based on a list of candidates. The
     * candidates are taken from [Call.callee] which are set either in [handleReference] or
     * [handleMemberAccess], depending on the type.
     *
     * In any case, the candidates are then resolved with the arguments of the call expression using
     * [resolveWithArguments]. The result of this resolution is stored in [Call.invokes] and
     * depending on [CallResolutionResult.SuccessKind] are warning is emitted if resolution was
     * erroneous or ambiguous. Furthermore, the [Call.callee]'s [Reference.refersTo] is also set.
     *
     * If the resolution was unsuccessful, we try to infer the function based on the information
     * provided in the [CallResolutionResult] and the [Call]. This is done in
     * [tryFunctionInference].
     *
     * @param call The [Call] to resolve.
     */
    protected open fun handleCall(call: Call) {
        // Some local variables for easier smart casting
        val callee = call.callee
        val language = call.language

        // If the base type is unknown, we cannot resolve the call
        if (
            callee is MemberAccess &&
                callee.base.type is UnknownType &&
                callee.base.assignedTypes.isEmpty()
        ) {
            Util.warnWithFileLocation(
                call,
                log,
                "Cannot resolve call to ${callee.name} because the base type is unknown",
            )
            return
        }

        // Dynamic function invokes (such as function pointers) are handled by an extra pass, so we
        // are not resolving them here.
        //
        // We have a dynamic invoke in two cases:
        // a) our callee is not a reference
        // b) our reference already refers to a variable rather than a function
        if (callee !is Reference || callee.refersTo is Variable || callee.refersTo is Parameter) {
            return
        }

        // If this is a template call and our language supports templates, we need to directly
        // handle this with the template system. This will also take care of inference and
        // everything. This will stay in this way until we completely redesign the template system.
        if (call.instantiatesTemplate() && language is HasTemplates) {
            val (ok, candidates) =
                language.handleTemplateFunctionCalls(
                    scopeManager.currentRecord,
                    call,
                    true,
                    ctx,
                    call.translationUnit,
                    false,
                )
            if (ok) {
                call.invokes = candidates.toMutableList()
                return
            }
        }

        decideInvokesBasedOnCandidates(callee, call)
    }

    private fun resolveMemberByName(
        symbol: String,
        possibleContainingTypes: Set<Type>,
    ): Set<Declaration> {
        var candidates = mutableSetOf<Declaration>()
        val records =
            possibleContainingTypes.mapNotNullTo(mutableSetOf()) { it.root.recordDeclaration }
        for (record in records) {
            candidates.addAll(
                ctx.scopeManager.lookupSymbolByName(record.name.fqn(symbol), record.language)
            )
        }

        // Find invokes by supertypes
        if (candidates.isEmpty() && symbol.isNotEmpty()) {
            val records =
                possibleContainingTypes.mapNotNullTo(mutableSetOf()) { it.root.recordDeclaration }
            candidates = getInvocationCandidatesFromParents(symbol, records).toMutableSet()
        }

        // Add overridden invokes
        candidates.addAll(
            candidates.filterIsInstance<Function>().flatMap {
                getOverridingCandidates(possibleContainingTypes, it)
            }
        )

        // Drop members that are inaccessible from where the access happens (e.g. a `private` member
        // reached from outside its record), for languages that model access control.
        return candidates.onlyAccessibleFrom(scopeManager.currentRecord)
    }

    /**
     * Narrows this set of member-resolution candidates to those that are accessible from the record
     * [from] in which the access syntactically occurs, honoring member access control (e.g. C/C++
     * `private` / `protected`) for languages that declare it via [HasVisibilityModifiers].
     * Candidates in languages without that trait, and members whose visibility is
     * [Visibility.UNKNOWN] or [Visibility.PUBLIC], are always accessible, so unrelated languages
     * remain unaffected.
     *
     * The filter is intentionally conservative and only ever *narrows* an ambiguous candidate set:
     * if it would remove every candidate — for instance because the code genuinely performs an
     * access the source language forbids — the original set is returned unchanged. We would rather
     * resolve a technically-illegal access than silently drop the only edge and leave the reference
     * unresolvable. As access control only restricts [Visibility.PRIVATE] and
     * [Visibility.PROTECTED] members, we skip the work entirely unless at least one candidate
     * carries such a visibility.
     */
    private fun Set<Declaration>.onlyAccessibleFrom(from: Record?): Set<Declaration> {
        if (none { it.hasRestrictedVisibility }) {
            return this
        }

        val accessible = filterTo(mutableSetOf()) { it.isAccessibleFrom(from) }
        return accessible.ifEmpty { this }
    }

    /**
     * Whether this member declaration is accessible from the record [from] in which the access
     * occurs. A [Visibility.PRIVATE] member is only accessible from within its own declaring
     * record, a [Visibility.PROTECTED] member additionally from records that (transitively) inherit
     * from the declaring one. Any other visibility (including [Visibility.UNKNOWN]), and any
     * language without the [HasVisibilityModifiers] trait, imposes no restriction.
     *
     * "Access relationship" here means the structural relation between the record [from] where the
     * access is written and the record that declares the member, which is what decides whether the
     * access is legal. We model exactly the two that every access-controlled language shares and
     * that are derivable from [from] and the declaring record alone:
     * 1. **same record** — `from` *is* the declaring record (grants access to `private` members),
     *    e.g. a method of `class C` reading `C`'s own `private` field;
     * 2. **subclass** — `from` (transitively) inherits from the declaring record (additionally
     *    grants access to `protected` members), e.g. a method of `class D : C` reading a
     *    `protected` field declared in `C`.
     *
     * We stop at these two rather than "any number" because every further way access can be granted
     * requires modeling a *different* relationship that is not expressible from `from` and the
     * declaring record alone, and is often language-specific: a C++ `friend` declaration names an
     * unrelated grantee, a nested class reaches into its lexically enclosing one, Java adds
     * package/module membership, and so on. Those grants are *not* recognized here and such a
     * member is reported as inaccessible. That is safe because [onlyAccessibleFrom] never removes
     * the last candidate: an unambiguous access (e.g. a friend call with a single candidate) still
     * resolves; only a genuinely ambiguous candidate set could be narrowed too aggressively.
     */
    private fun Declaration.isAccessibleFrom(from: Record?): Boolean {
        if (language !is HasVisibilityModifiers) {
            return true
        }

        return when (visibility) {
            Visibility.PRIVATE -> from != null && declaringRecord == from
            Visibility.PROTECTED -> from != null && declaringRecord in from.ancestorRecords
            else -> true
        }
    }

    /**
     * The [Record] that declares this member. For any member that is *lexically* nested in its
     * record this is simply the closest enclosing [Record] in the AST ([firstParentOrNull], which
     * walks [Node.astParent]); the surrounding [de.fraunhofer.aisec.cpg.graph.scopes.RecordScope]
     * would give the same answer for those.
     *
     * A [Method], however, may be *defined out-of-line* (e.g. C++ `void C::foo() {}`), where its
     * AST parent and its scope are the enclosing namespace or translation unit, not the record. We
     * therefore prefer its explicitly-tracked [Method.recordDeclaration], which points at the
     * record even for such definitions. Returns `null` for non-members.
     */
    private val Declaration.declaringRecord: Record?
        get() = (this as? Method)?.recordDeclaration ?: firstParentOrNull<Record>()

    /** This [Record] and all records in its transitive super-type chain. */
    private val Record.ancestorRecords: Set<Record>
        get() {
            return toType().ancestors.mapNotNullTo(mutableSetOf()) { it.type.recordDeclaration }
        }

    protected open fun handleConstruction(constructExpression: Construction) {
        if (constructExpression.instantiates != null && constructExpression.constructor != null)
            return
        val recordDeclaration = constructExpression.type.root.recordDeclaration
        constructExpression.instantiates = recordDeclaration
        for (template in templateList) {
            if (
                template is RecordTemplate &&
                    recordDeclaration != null &&
                    recordDeclaration in template.realizations &&
                    (constructExpression.templateArguments.size <= template.parameters.size)
            ) {
                val defaultDifference =
                    template.parameters.size - constructExpression.templateArguments.size
                if (defaultDifference <= template.parameterDefaults.size) {
                    // Check if predefined template value is used as default in next value
                    addRecursiveDefaultTemplateArgs(constructExpression, template)

                    // Add missing defaults
                    val missingNewParams =
                        template.parameterDefaults.subList(
                            constructExpression.templateArguments.size,
                            template.parameterDefaults.size,
                        )
                    for (missingParam in missingNewParams) {
                        if (missingParam != null) {
                            constructExpression.addTemplateParameter(
                                missingParam,
                                Template.TemplateInitialization.DEFAULT,
                            )
                        }
                    }
                    constructExpression.templateInstantiation = template
                    break
                }
            }
        }
        if (recordDeclaration != null) {
            val constructor = getConstructorDeclaration(constructExpression, recordDeclaration)
            constructExpression.constructor = constructor
        }
    }

    /**
     * This function handles all nodes that have the [HasOverloadedOperation] trait. It tries to
     * resolve the overloaded operator and replace the node with the resolved operator expression.
     *
     * Which overloads are possible, is depending on whether the language implements
     * [HasOperatorOverloading] and can be specified in
     * [HasOperatorOverloading.overloadedOperatorNames].
     *
     * Internally, it takes the result of [resolveOperator] and if successful, replaces the node
     * with the resolved [OperatorCall].
     */
    protected open fun handleOverloadedOperator(op: HasOverloadedOperation) {
        val result = resolveOperator(op)
        val functionDeclaration = result?.bestViable?.singleOrNull() ?: return

        // If the result was successful, we can replace the node
        if (result.success == SUCCESSFUL && functionDeclaration is Operator && op is Expression) {
            val call = operatorCallFromDeclaration(functionDeclaration, op)
            walker.replace(op.astParent, op, call)
        }
    }

    /**
     * This function tries to resolve an overloaded operator based on the
     * [HasOverloadedOperation.operatorCode] of the [op] (if the [HasOverloadedOperation.language]
     * allows it). It first lookups the corresponding symbol in the
     * [HasOperatorOverloading.overloadedOperatorNames] of the language, for example `add` for a `+`
     * operator. In then tries to find the matching method candidates in the base class of the [op]
     * (using [resolveMemberByName]) and returns the result of the resolution. The base depends on
     * the individual operator / expression and is specified in
     * [HasOverloadedOperation.operatorBase].
     *
     * Finally, the candidates are resolved with the arguments of the operator expression using
     * [resolveWithArguments].
     */
    private fun resolveOperator(op: HasOverloadedOperation): CallResolutionResult? {
        val language = op.language
        val base = op.operatorBase
        val baseType = (base as? PointerDereference)?.input?.type ?: base.type
        if (language !is HasOperatorOverloading || language.isPrimitive(baseType)) {
            return null
        }

        val symbol = language.overloadedOperatorNames[Pair(op::class, op.operatorCode)]
        if (symbol == null) {
            log.warn(
                "Could not resolve operator overloading for unknown operatorCode ${op.operatorCode}"
            )
            return null
        }

        val possibleTypes = mutableSetOf<Type>()
        possibleTypes.add(baseType)
        val baseAssignedtype =
            (base as? PointerDereference)?.input?.assignedTypes ?: base.assignedTypes

        possibleTypes.addAll(baseAssignedtype)

        val candidates = resolveMemberByName(symbol, possibleTypes).filterIsInstance<Operator>()

        return resolveWithArguments(candidates, op.operatorArguments, op as Expression)
    }

    private fun getInvocationCandidatesFromParents(
        name: Symbol,
        possibleTypes: Set<Record>,
    ): List<Declaration> {
        val workingPossibleTypes = mutableSetOf(*possibleTypes.toTypedArray())
        return if (possibleTypes.isEmpty()) {
            listOf()
        } else {
            val firstLevelCandidates =
                possibleTypes.flatMap { record ->
                    scopeManager.lookupSymbolByName(record.name.fqn(name), record.language)
                }

            // C++ does not allow overloading at different hierarchy levels. If we find a
            // Function with the same name as the function in the Call we have
            // to stop the search in the parent even if the Function does not match with
            // the signature of the Call
            // TODO: move this to refineMethodResolution of CXXLanguage
            if (possibleTypes.firstOrNull()?.language.isCPP) { // TODO: Needs a special trait?
                workingPossibleTypes.removeIf { recordDeclaration ->
                    !shouldContinueSearchInParent(recordDeclaration, name)
                }
            }
            firstLevelCandidates.ifEmpty {
                workingPossibleTypes.flatMap {
                    getInvocationCandidatesFromParents(
                        name,
                        it.superTypeDeclarations.filter { it !in possibleTypes }.toSet(),
                    )
                }
            }
        }
    }

    protected val Language<*>?.isCPP: Boolean
        get() {
            return this != null && this::class.simpleName == "CPPLanguage"
        }

    private fun getOverridingCandidates(
        possibleSubTypes: Set<Type>,
        declaration: Function,
    ): Set<Function> {
        return declaration.overriddenBy.filterTo(mutableSetOf()) { f ->
            if (f is Method) {
                val record = f.recordDeclaration
                record != null && record.toType() in possibleSubTypes
            } else {
                false
            }
        }
    }

    /**
     * @param constructExpression we want to find an invocation target for
     * @param recordDeclaration associated with the Object the Construction constructs
     * @return a [Constructor] that is an invocation of the given Construction. If there is no valid
     *   [Constructor] we will create an implicit ConstructDeclaration that matches the
     *   Construction.
     */
    private fun getConstructorDeclaration(
        constructExpression: Construction,
        recordDeclaration: Record,
    ): Constructor? {
        val signature = constructExpression.signature
        val constructorCandidate =
            recordDeclaration.constructors.firstOrNull {
                it.matchesSignature(
                    signature,
                    constructExpression.arguments,
                    constructExpression.language is HasDefaultArguments,
                ) != IncompatibleSignature
            }

        return constructorCandidate
            ?: recordDeclaration
                .startInference(ctx)
                ?.createInferredConstructor(constructExpression.signature)
    }

    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(SymbolResolver::class.java)

        val componentsToTemplates = mutableMapOf<Component, MutableList<Template>>()

        /**
         * Adds implicit duplicates of the TemplateParams to the implicit Construction
         *
         * @param templateParams of the [Variable]/[New]
         * @param constructExpression duplicate TemplateParameters (implicit) to preserve AST, as
         *   [Construction] uses AST as well as the [Variable]/[New]
         */
        fun addImplicitTemplateParametersToCall(
            templateParams: List<Node>,
            constructExpression: Construction,
        ) {
            for (node in templateParams) {
                if (node is TypeExpression) {
                    constructExpression.addTemplateParameter(node.duplicate(true))
                } else if (node is Literal<*>) {
                    constructExpression.addTemplateParameter(node.duplicate(true))
                }
            }
        }
    }
}

/**
 * This function decides which functions to add to [Call.invokes] based on the candidates and the
 * arguments. It uses [resolveWithArguments] to resolve the best viable function based on the
 * candidates and the arguments.
 *
 * If the resolution is [SUCCESSFUL], it sets the invokes edge to the best viable functions. If it
 * is [AMBIGUOUS] or [PROBLEMATIC], it sets the invokes edge to all possible viable functions. If it
 * is unresolved, it tries to infer the function using [tryFunctionInference].
 *
 * @param callee The [Reference] of the callee.
 * @param call The [Call] to resolve.
 */
internal fun Pass<*>.decideInvokesBasedOnCandidates(callee: Reference, call: Call) {
    // Try to resolve the best viable function based on the candidates and the arguments
    val result = resolveWithArguments(callee.candidates, call.arguments, call)
    when (result.success) {
        PROBLEMATIC -> {
            Pass.log.error(
                "Resolution of ${call.name} returned an problematic result and we cannot decide correctly, the invokes edge will contain all possible viable functions"
            )
            call.invokes =
                if (result.bestViable.isEmpty()) tryFunctionInference(call, result).toMutableList()
                else result.bestViable.toMutableList()
        }
        AMBIGUOUS -> {
            Pass.log.warn(
                "Resolution of ${call.name} returned an ambiguous result and we cannot decide correctly, the invokes edge will contain the the ambiguous functions"
            )
            call.invokes = result.bestViable.toMutableList()
        }
        SUCCESSFUL -> {
            call.invokes = result.bestViable.toMutableList()
        }
        UNRESOLVED -> {
            call.invokes = tryFunctionInference(call, result).toMutableList()
        }
    }

    // We also set the callee's refersTo
    callee.refersTo = call.invokes.firstOrNull()
}

/**
 * Returns a set of types in which the [Call.callee] (which is a [Reference]) could reside in. More
 * concretely, it returns a [Pair], where the first element is the set of types and the second is
 * our best guess.
 */
internal fun Pass<*>.getPossibleContainingTypes(ref: Reference): Pair<Set<Type>, Type?> {
    val possibleTypes = mutableSetOf<Type>()
    var bestGuess: Type? = null
    if (ref is MemberAccess) {
        val base = (ref.base as? PointerDereference)?.input ?: ref.base
        bestGuess = base.type
        possibleTypes.add(base.type)
        possibleTypes.addAll(base.assignedTypes)
    } else if (ref.language is HasImplicitReceiver) {
        // This could be a member call with an implicit receiver, so let's add the current class
        // to the possible list
        scopeManager.currentRecord?.toType()?.let {
            bestGuess = it
            possibleTypes.add(it)
        }
    }

    return Pair(possibleTypes, bestGuess)
}

/**
 * This function tries to resolve a set of [candidates] (e.g. coming from a [Call.callee]) into the
 * best matching [Function] (or multiple functions, if applicable) based on the supplied
 * [arguments]. The result is returned in the form of a [CallResolutionResult] which holds detail
 * information about intermediate results as well as the kind of success the resolution had.
 *
 * The [source] expression specifies the node in the graph that triggered this resolution. This is
 * most likely a [Call], but could be other node as well. It is also the source of the scope and
 * language used in the resolution.
 */
internal fun Pass<*>.resolveWithArguments(
    candidates: Collection<Declaration>,
    arguments: List<Expression>,
    source: Expression,
): CallResolutionResult {
    val result =
        CallResolutionResult(
            source,
            arguments,
            candidates.filterIsInstanceTo<Function, IdentitySet<Function>>(
                identitySetOf<Function>()
            ),
            setOf(),
            mapOf(),
            setOf(),
            UNRESOLVED,
            source.scope,
        )
    val language = source.language

    // Set the start scope. This can either be the call's scope or a scope specified in an FQN.
    // If our base is a dynamic or unknown type, we can skip the scope extraction because it
    // will always fail
    val extractedScope =
        if (
            source is MemberCall &&
                (source.base?.type is DynamicType ||
                    source.base?.type is UnknownType ||
                    source.base?.type is AutoType)
        ) {
            ScopeManager.ScopeExtraction(null, Name(""))
        } else {
            ctx.scopeManager.extractScope(source, language, source.scope)
        }

    // If we could not extract the scope (even though one was specified), we can only return an
    // empty result
    if (extractedScope == null) {
        return result
    }

    val scope = extractedScope.scope
    result.actualStartScope = scope ?: source.scope

    // If there are no candidates, we can stop here
    if (candidates.isEmpty()) {
        return result
    }

    // If the function does not allow function overloading, and we have multiple candidate
    // symbols, the result is "problematic"
    if (source.language !is HasFunctionOverloading && result.candidateFunctions.size > 1) {
        result.success = PROBLEMATIC
    }

    // Filter functions that match the signature of our call, either directly or with casts;
    // those functions are "viable". Take default arguments into account if the language has
    // them.
    result.signatureResults =
        result.candidateFunctions
            .map {
                Pair(
                    it,
                    it.matchesSignature(
                        arguments.map(Expression::type),
                        arguments,
                        source.language is HasDefaultArguments,
                    ),
                )
            }
            .filter { it.second is SignatureMatches }
            .associate { it }
    result.viableFunctions = result.signatureResults.keys

    // If we have a "problematic" result, we can stop here. In this case we cannot really
    // determine anything more.
    if (result.success == PROBLEMATIC) {
        result.bestViable = result.viableFunctions
        return result
    }

    // Otherwise, give the language a chance to narrow down the result (ideally to one) and set
    // the success kind.
    val pair = language.bestViableResolution(result)
    result.bestViable = pair.first
    result.success = pair.second

    return result
}
