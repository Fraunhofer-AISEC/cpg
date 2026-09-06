/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.analysis.string

import de.fraunhofer.aisec.cpg.analysis.abstracteval.DeclarationState
import de.fraunhofer.aisec.cpg.analysis.abstracteval.GeneralState
import de.fraunhofer.aisec.cpg.analysis.abstracteval.GeneralStateElement
import de.fraunhofer.aisec.cpg.analysis.abstracteval.TupleState
import de.fraunhofer.aisec.cpg.analysis.abstracteval.TupleStateElement
import de.fraunhofer.aisec.cpg.analysis.abstracteval.value.Value
import de.fraunhofer.aisec.cpg.evaluation.ValueEvaluator
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.Function
import de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder
import de.fraunhofer.aisec.cpg.graph.firstParentOrNull
import de.fraunhofer.aisec.cpg.helpers.functional.Lattice
import de.fraunhofer.aisec.cpg.passes.objectIdentifier
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlinx.coroutines.runBlocking

/**
 * Flow-sensitive, whole-EOG-fixpoint variant of the (demand-driven, backward) [StringEvaluator] -
 * "Phase 5" of `docs/docs/CPG/impl/string-analysis.md`. Mirrors
 * [de.fraunhofer.aisec.cpg.analysis.abstracteval.AbstractIntervalEvaluator]'s public shape and
 * internal machinery exactly, instantiated with the inner lattice element type `E = StringPattern`
 * (see design decision D8: [StringPattern] implements [Lattice.Element] itself, so - unlike
 * [de.fraunhofer.aisec.cpg.analysis.abstracteval.NewIntervalLattice.Element] for
 * [de.fraunhofer.aisec.cpg.analysis.abstracteval.LatticeInterval] - no wrapper class is needed
 * here; [Value]'s `E` and `T` type parameters coincide.
 *
 * Where the backward [StringEvaluator] recomputes a demand-driven answer per query by walking
 * predecessors (and hand-rolls a small per-cycle fixpoint, see
 * [StringEvaluator.evaluateWithFixpoint]), this evaluator computes a single, whole-function
 * fixpoint over the EOG via [Lattice.iterateEOG] and then reads the answer for [targetNode] out of
 * the resulting state. This is intended for exactly the cases the design doc calls out as handled
 * badly by the backward evaluator: strings built up across a loop, and other path-dependent values
 * where the flow-sensitive join at merge points gives a better (or at least differently-shaped)
 * answer.
 *
 * Extends [ValueEvaluator] so that the generic `Node.evaluate(evaluator)`/`Node.evaluateAs<T>()`
 * extension functions work with an [AbstractStringEvaluator] directly, via [targetType] (defaulting
 * to [StringValue], the only existing [Value]`<StringPattern, StringPattern>` implementation).
 * Unlike [StringEvaluator], there is no self-recursive, per-node machinery here whose state needs
 * careful sequencing across `ValueEvaluator`'s `evaluateAs`-bypass: the whole computation is a
 * single EOG fixpoint per [evaluate] call, with all fixpoint state kept in local variables.
 */
class AbstractStringEvaluator(
    /**
     * The [Value] implementation to use when [evaluate] is called through the [ValueEvaluator]-
     * inherited entry points (`evaluate(Any?, Boolean)`/`evaluateInternal(Node?, Int)`/
     * `evaluateAs<T>()`), which do not otherwise have a way to specify one. The existing
     * `evaluate(node, targetType)`/`evaluate(start, targetNode, type, initial)` overloads remain
     * available for callers that want to pass a different [Value] implementation explicitly.
     */
    val targetType: KClass<out Value<StringPattern, StringPattern>> = StringValue::class,
    /**
     * Not currently consulted internally - there is no `ValueEvaluator`-style hook into
     * [Value.applyEffect] (its signature does not receive the evaluator instance). Provided for API
     * consistency with [ValueEvaluator] and potential future use.
     */
    override val cannotEvaluate: (Node?, ValueEvaluator) -> StringPattern = { node, _ ->
        StringPattern.Unknown(origin = node, reason = StringPattern.Reason.UNSUPPORTED)
    },
) : ValueEvaluator() {
    /**
     * Evaluates the [StringPattern] of a value at the given [node], using the specified
     * [targetType]. Starts the fixpoint from the enclosing [Function], mirroring
     * [de.fraunhofer.aisec.cpg.analysis.abstracteval.AbstractIntervalEvaluator.evaluate].
     */
    fun evaluate(
        node: Node,
        targetType: KClass<out Value<StringPattern, StringPattern>>,
    ): StringPattern {
        val startNode = node.firstParentOrNull<Function>() ?: return StringPattern.Bottom
        return evaluate(startNode, node, targetType, StringPattern.Bottom)
    }

    /**
     * The real override of `ValueEvaluator.evaluate(node: Any?, useCache: Boolean): Any?`, with a
     * covariant `StringPattern` return type. Delegates to the 2-arg [evaluate] overload using this
     * instance's constructor-bound [targetType].
     */
    override fun evaluate(node: Any?, useCache: Boolean): StringPattern =
        if (node is Node) evaluate(node, targetType) else StringPattern.Bottom

    /**
     * The override of `ValueEvaluator.evaluateInternal`. `depth` is ignored: unlike
     * [StringEvaluator] (a recursive, demand-driven evaluator, where `depth` becomes a step
     * counter), this evaluator's computation is a single whole-function EOG fixpoint with no notion
     * of recursion depth.
     */
    override fun evaluateInternal(node: Node?, depth: Int): StringPattern =
        if (node == null) StringPattern.Bottom else evaluate(node, targetType)

    /**
     * Evaluates the [StringPattern] of a value at [targetNode], starting the EOG fixpoint from
     * [start], with the given [type] and [initial] pattern for [start].
     */
    fun evaluate(
        start: Node,
        targetNode: Node,
        type: KClass<out Value<StringPattern, StringPattern>>,
        initial: StringPattern = StringPattern.Bottom,
    ): StringPattern {
        val innerLattice = StringLattice()
        val declarationState = DeclarationState<Any, StringPattern>(innerLattice)
        val generalState = GeneralState<StringPattern>(innerLattice)
        val startState = TupleState(declarationState, generalState)

        // evaluate effect of each operation on the list until we reach "targetNode"
        val startStateElement = startState.bottom
        declarationState.push(startStateElement.first, start, initial)
        generalState.push(startStateElement.second, start, initial)

        val (finalState, _) =
            runBlocking {
                startState.iterateEOG(
                    start.nextEOGEdges,
                    startStateElement,
                    { lattice, currentEdge, currentState ->
                        handleNode(type, lattice, currentEdge, currentState)
                    },
                    strategy = Lattice.Strategy.WIDENING,
                )
            }
        return finalState?.second?.get(targetNode) ?: StringPattern.Bottom
    }

    /**
     * Handles the effect of a node during EOG traversal, delegating to [type]'s
     * [Value.applyEffect]. Mirrors
     * [de.fraunhofer.aisec.cpg.analysis.abstracteval.AbstractIntervalEvaluator.handleNode].
     *
     * Takes [type] as an explicit parameter (captured from the enclosing [evaluate] call's local
     * scope via a lambda at the call site) rather than reading it from a shared instance field: a
     * single [AbstractStringEvaluator] instance is expected to be shared and invoked concurrently
     * (per [ValueEvaluator]'s documented contract), and a mutable field set per-call would let two
     * concurrent [evaluate] calls on the same instance see each other's [type].
     */
    private fun handleNode(
        type: KClass<out Value<StringPattern, StringPattern>>,
        lattice: Lattice<TupleStateElement<Any, StringPattern>>,
        currentEdge: EvaluationOrder,
        currentState: TupleStateElement<Any, StringPattern>,
    ): TupleStateElement<Any, StringPattern> {
        val currentNode = currentEdge.end

        type
            .createInstance()
            .applyEffect(
                lattice = lattice as TupleState<Any, StringPattern>,
                state = currentState,
                node = currentNode,
                edge = currentEdge,
            )

        return currentState
    }
}

/**
 * Pushes [pattern] for [start] into the declaration state lattice. Keyed by the raw [start] node
 * itself (not its [de.fraunhofer.aisec.cpg.passes.objectIdentifier]), exactly like
 * [de.fraunhofer.aisec.cpg.analysis.abstracteval.AbstractIntervalEvaluator]'s private seeding
 * helper of the same name: this initial entry is never looked up by key match (lookups go through
 * [patternOf], which searches by `objectIdentifier`), it only needs to seed the map so the fixpoint
 * has a starting element.
 */
private fun <NodeId> DeclarationState<NodeId, StringPattern>.push(
    current: DeclarationState.DeclarationStateElement<NodeId, StringPattern>,
    start: NodeId,
    pattern: StringPattern,
) {
    runBlocking {
        this@push.lub(
            current,
            DeclarationState.DeclarationStateElement(start to pattern),
            allowModify = true,
        )
    }
}

/** Pushes [pattern] for [start] into the general state lattice. */
private fun GeneralState<StringPattern>.push(
    current: GeneralStateElement<StringPattern>,
    start: Node,
    pattern: StringPattern,
) {
    runBlocking {
        this@push.lub(current, GeneralStateElement(start to pattern), allowModify = true)
    }
}

/**
 * Retrieves the [StringPattern] for the given [node] from this tuple state element's declaration
 * state.
 *
 * **Default for an untracked entry: [StringPattern.Unknown] (i.e. [StringPattern]'s `Top`), not
 * [StringPattern.Bottom].** This mirrors [de.fraunhofer.aisec.cpg.analysis.abstracteval.intervalOf]
 * defaulting to [de.fraunhofer.aisec.cpg.analysis.abstracteval.LatticeInterval.TOP] rather than
 * `BOTTOM` for the same reason: `Bottom` means "unreachable, no value flows here" - returning it
 * for a node we simply have not (yet) recorded anything about would make the analysis silently
 * *deny* a real, reachable value instead of admitting that it does not know it. Since
 * [StringPattern]'s lattice has no separate `Top` object (`Top` is `Unknown(origin = null, charSet
 * = Any, length = TOP)`, see [StringPattern.Unknown]'s KDoc), an `Unknown()` with no origin is the
 * correct "we have not tracked this" default: sound (its language is everything), and it degrades
 * further joins gracefully (`lub(Unknown(), x) == Unknown()`-shaped, never a spurious narrowing to
 * less than what is truly possible).
 */
@Suppress("UNCHECKED_CAST")
fun <NodeId> TupleStateElement<NodeId, StringPattern>.patternOf(node: Node): StringPattern {
    val id =
        node.objectIdentifier()?.let { tmpId ->
            this.first.keys.singleOrNull { it == tmpId } ?: (tmpId as? NodeId)
        } ?: node as? NodeId ?: TODO()
    return this.first[id] ?: StringPattern.Unknown()
}

/**
 * Updates the declaration state for [node] with the specified [pattern] in the current tuple state
 * element, overwriting any existing entry (no `lub` with the previous value). Mirrors
 * [de.fraunhofer.aisec.cpg.analysis.abstracteval.changeDeclarationState].
 */
@Suppress("UNCHECKED_CAST")
fun <NodeId> TupleState<NodeId, StringPattern>.changeDeclarationState(
    current: TupleStateElement<NodeId, StringPattern>,
    node: Node,
    pattern: StringPattern,
): TupleStateElement<NodeId, StringPattern> {
    val id =
        (node.objectIdentifier() as? NodeId)?.let { tmpId ->
            current.first.keys.singleOrNull { it == tmpId } ?: tmpId
        } ?: node as NodeId ?: TODO()
    current.first[id] = pattern
    return current
}

/**
 * Pushes [pattern] for [node] into the declaration state, merging (`lub`) with any existing entry.
 * Mirrors [de.fraunhofer.aisec.cpg.analysis.abstracteval.pushToDeclarationState].
 */
@Suppress("UNCHECKED_CAST")
fun <NodeId> TupleState<NodeId, StringPattern>.pushToDeclarationState(
    current: TupleStateElement<NodeId, StringPattern>,
    node: Node,
    pattern: StringPattern,
): TupleStateElement<NodeId, StringPattern> {
    val id =
        (node.objectIdentifier() as? NodeId)?.let { tmpId ->
            current.first.keys.singleOrNull { it == tmpId } ?: tmpId
        } ?: node as NodeId ?: TODO()
    runBlocking {
        this@pushToDeclarationState.innerLattice1.lub(
            current.first,
            DeclarationState.DeclarationStateElement(id to pattern),
            allowModify = true,
        )
    }
    return current
}

/**
 * Pushes [pattern] for [node] into the general (per-[Node]) state, merging (`lub`) with any
 * existing entry. Mirrors [de.fraunhofer.aisec.cpg.analysis.abstracteval.pushToGeneralState].
 */
fun <NodeId> TupleState<NodeId, StringPattern>.pushToGeneralState(
    current: TupleStateElement<NodeId, StringPattern>,
    node: Node,
    pattern: StringPattern,
): TupleStateElement<NodeId, StringPattern> {
    runBlocking {
        this@pushToGeneralState.innerLattice2.lub(
            current.second,
            GeneralStateElement(node to pattern),
            allowModify = true,
        )
    }
    return current
}
