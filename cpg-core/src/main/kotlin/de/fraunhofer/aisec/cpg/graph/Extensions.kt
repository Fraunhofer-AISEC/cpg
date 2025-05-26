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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.assumptions.Assumption
import de.fraunhofer.aisec.cpg.assumptions.HasAssumptions
import de.fraunhofer.aisec.cpg.assumptions.addAssumptionDependences
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.graph.edges.flows.ControlDependence
import de.fraunhofer.aisec.cpg.graph.edges.flows.FullDataflowGranularity
import de.fraunhofer.aisec.cpg.graph.edges.flows.IndexedDataflowGranularity
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.helpers.identitySetOf
import de.fraunhofer.aisec.cpg.passes.reconstructedImportName
import kotlin.collections.filter
import kotlin.collections.firstOrNull
import kotlin.math.absoluteValue

/**
 * Flattens the AST beginning with this node and returns all nodes of type [T]. For convenience, an
 * optional predicate function [predicate] can be supplied, which will be applied via
 * [Collection.filter]
 *
 * @param stopAtNode Indicates if the node should be stopped at, i.e., we do not visit this node and
 *   its children.
 * @param predicate Indicates if the node should be included in the result.
 */
@JvmOverloads
inline fun <reified T> Node?.allChildren(
    noinline stopAtNode: (Node) -> Boolean = { false },
    noinline predicate: ((T) -> Boolean)? = null,
): List<T> {
    val nodes = SubgraphWalker.flattenAST(this, stopAtNode = stopAtNode)
    val filtered = nodes.filterIsInstance<T>()

    return if (predicate != null) {
        filtered.filter(predicate)
    } else {
        filtered
    }
}

/**
 * Flattens the AST beginning with this node and returns all nodes of type [T]. For convenience, an
 * optional predicate function [predicate] can be supplied, which will be applied via
 * [Collection.filter]
 */
@JvmOverloads
inline fun <reified T> Node?.allChildrenWithOverlays(
    noinline predicate: ((T) -> Boolean)? = null
): List<T> {
    val nodes = SubgraphWalker.flattenAST(this)
    val nodesWithOverlays = nodes + nodes.flatMap { it.overlays }
    val filtered = nodesWithOverlays.filterIsInstance<T>()

    return if (predicate != null) {
        filtered.filter(predicate)
    } else {
        filtered
    }
}

/**
 * Returns a list of all [Node]s, starting from the current [Node], which are the beginning of an
 * EOG path created by the [EvaluationOrderGraphPass]. Typical examples include all top-level
 * declarations, such as functions and variables. For a more detailed explanation, see
 * [EOGStarterHolder].
 *
 * While it is in theory possible to retrieve this property from all nodes, most use cases should
 * include retrieving it from either an individual [TranslationUnitDeclaration] or the complete
 * [TranslationResult].
 */
val Node.allEOGStarters: List<Node>
    get() {
        return this.allChildren<EOGStarterHolder>().flatMap { it.eogStarters }.distinct()
    }

/**
 * Returns all EOG starters of this node that have no predecessors in the EOG as well as the node
 * itself if it is an EOG "single", in a way that is has neither a previous nor next EOG edge.
 */
val Node.allUniqueEOGStartersOrSingles: List<Node>
    get() {
        return (allEOGStarters.filter { it.prevEOGEdges.isEmpty() } +
                if (prevEOGEdges.isEmpty() && nextEOGEdges.isEmpty()) {
                    listOf(this)
                } else {
                    emptyList()
                })
            .distinct()
    }

@JvmName("astNodes")
fun Node.ast(): List<Node> {
    return this.ast<Node>()
}

inline fun <reified T : Node> Node.ast(): List<T> {
    val children = SubgraphWalker.getAstChildren(this)

    return children.filterIsInstance<T>()
}

inline fun <reified T : Node> Node.dfgFrom(): List<T> {
    return this.prevDFG.toList().filterIsInstance<T>()
}

/**
 * This function retrieves the [CallExpression]s of [this] by their fully qualified name (FQN). The
 * match is performed on the [CallExpression.reconstructedImportName].
 *
 * @param fqn The fully qualified name of the calls to retrieve.
 * @return A list of [CallExpression] nodes matching the provided FQN.
 */
fun <T : CallExpression> Collection<T>.byFQN(fqn: String): List<T> {
    return this.filter { call -> call.reconstructedImportName.toString() == fqn }
}

/** This function returns the *first* node that matches the name on the supplied list of nodes. */
fun <T : Node> Collection<T>?.byNameOrNull(lookup: String, modifier: SearchModifier): T? {
    return if (modifier == SearchModifier.NONE) {
        this?.firstOrNull { it.name.lastPartsMatch(lookup) }
    } else {
        val nodes = this?.filter { it.name.lastPartsMatch(lookup) } ?: listOf()
        if (nodes.size > 1) {
            throw NoSuchElementException("result is not unique")
        }

        nodes.firstOrNull()
    }
}

enum class SearchModifier {
    NONE,

    /**
     * This search modifier denotes that the result returned by the search needs to be unique. If it
     * is not unique, a [NoSuchElementException] is thrown, even if a `orNull` function is used.
     */
    UNIQUE,
}

/** A shortcut to call [byNameOrNull] using the `[]` syntax. */
operator fun <T : Node> Collection<T>?.get(
    lookup: String,
    modifier: SearchModifier = SearchModifier.NONE,
): T? {
    return this.byNameOrNull(lookup, modifier)
}

/** A shortcut to call [firstOrNull] using the `[]` syntax. */
operator fun <T : Node> Collection<T>?.get(
    predicate: (T) -> Boolean,
    modifier: SearchModifier = SearchModifier.NONE,
): T? {
    return if (modifier == SearchModifier.NONE) {
        return this?.firstOrNull(predicate)
    } else {
        val nodes = this?.filter(predicate) ?: listOf()
        if (nodes.size > 1) {
            throw NoSuchElementException("result is not unique")
        }

        nodes.firstOrNull()
    }
}

/** A shortcut invoke [filter] on a list of nodes. */
operator fun <T : Node> Collection<T>.invoke(predicate: (T) -> Boolean): List<T> {
    return this.filter(predicate)
}

/** A shortcut to filter a list of nodes by their name. */
operator fun <T : Node> Collection<T>.invoke(lookup: String): List<T> {
    // TODO: I'm not sure if it wouldn't be more intuitive to use
    // call.reconstructedImportName.toString().endsWith(lookup) for CallExpressions.
    return this.filter { it.name.lastPartsMatch(lookup) }
}

/**
 * This inline function returns the `n`-th body statement (in AST order) cast as T or `null` if it
 * does not exist or the type does not match.
 *
 * `n` can also be negative; in this case `-1` corresponds to the last statement, `-2` to the second
 * to last and so on.
 *
 * For convenience, `n` defaults to zero, so that the first statement is always easy to fetch.
 */
inline fun <reified T : Statement> FunctionDeclaration.bodyOrNull(n: Int = 0): T? {
    var body = this.body
    return if (body is Block) {
        var statements = body.statements
        var idx =
            if (n < 0) {
                statements.size - n.absoluteValue
            } else {
                n
            }
        return statements.getOrNull(idx) as? T
    } else {
        if (n == 0 && body is T) {
            body
        } else {
            return null
        }
    }
}

/**
 * This inline function returns the `n`-th body statement (in AST order) as specified in T. It
 * throws a [StatementNotFound] exception if it does not exist or match the type.
 *
 * For convenience, `n` defaults to zero, so that the first statement is always easy to fetch.
 */
@Throws(StatementNotFound::class)
inline fun <reified T : Statement> FunctionDeclaration.body(n: Int = 0): T {
    return bodyOrNull(n) ?: throw StatementNotFound()
}

class StatementNotFound : Exception()

class DeclarationNotFound(message: String) : Exception(message)

enum class FailureReason {
    PATH_ENDED,
    STEPS_EXCEEDED,
    HIT_EARLY_TERMINATION,
}

/**
 * This data class represents a path of nodes as some paths are not just lists of edges, but instead
 * can be traversed by a predicate.
 */
data class NodePath(
    val nodes: List<Node>,
    override val assumptions: MutableSet<Assumption> = mutableSetOf(),
) : HasAssumptions {
    override fun collectAssumptions(): Set<Assumption> {
        return super.collectAssumptions() + nodes.flatMap { it.assumptions }
    }
}

class FulfilledAndFailedPaths(
    val fulfilled: List<NodePath>,
    val failed: List<Pair<FailureReason, NodePath>>,
) {
    operator fun component1(): List<NodePath> = fulfilled

    operator fun component2(): List<Pair<FailureReason, NodePath>> = failed

    operator fun plus(other: FulfilledAndFailedPaths): FulfilledAndFailedPaths {
        return FulfilledAndFailedPaths(this.fulfilled + other.fulfilled, this.failed + other.failed)
    }
}

/**
 * Returns an instance of [FulfilledAndFailedPaths] where [FulfilledAndFailedPaths.fulfilled]
 * contains all possible shortest data flow paths (with [FullDataflowGranularity]) between the end
 * node [this] and the starting node fulfilling [predicate]. The paths are represented as lists of
 * nodes. Paths which do not end at such a node are included in [FulfilledAndFailedPaths.failed].
 *
 * Hence, if "fulfilled" is a non-empty list, a data flow from [this] to such a node is **possible
 * but not mandatory**. If the list "failed" is empty, the data flow is mandatory.
 */
fun Node.followPrevFullDFGEdgesUntilHit(
    collectFailedPaths: Boolean = true,
    findAllPossiblePaths: Boolean = true,
    earlyTermination: (Node, Context) -> Boolean = { _, _ -> false },
    predicate: (Node) -> Boolean,
): FulfilledAndFailedPaths {
    return followDFGEdgesUntilHit(
        collectFailedPaths = collectFailedPaths,
        findAllPossiblePaths = findAllPossiblePaths,
        earlyTermination = earlyTermination,
        predicate = predicate,
        direction = Backward(GraphToFollow.DFG),
        sensitivities = OnlyFullDFG + ContextSensitive,
        scope = Interprocedural(),
    )
}

/**
 * Iterates the prev full DFG edges until there are no more edges available (or until a loop is
 * detected). Returns a list of possible paths (each path is represented by a list of nodes).
 */
fun Node.collectAllPrevFullDFGPaths(): List<NodePath> {
    // We make everything fail to reach the end of the DFG. Then, we use the stuff collected in the
    // failed paths (everything)
    return this.followPrevFullDFGEdgesUntilHit(
            collectFailedPaths = true,
            findAllPossiblePaths = true,
        ) {
            false
        }
        .failed
        .map { it.second }
}

/**
 * Iterates the prev DFG edges until there are no more edges available (or until a loop is
 * detected). Returns a list of possible paths (each path is represented by a list of nodes).
 */
fun Node.collectAllPrevDFGPaths(): List<NodePath> {
    // We make everything fail to reach the end of the DFG. Then, we use the stuff collected in the
    // failed paths (everything)
    return this.followDFGEdgesUntilHit(
            collectFailedPaths = true,
            findAllPossiblePaths = true,
            direction = Backward(GraphToFollow.DFG),
        ) {
            false
        }
        .failed
        .map { it.second }
}

/**
 * Returns an instance of [FulfilledAndFailedPaths] where [FulfilledAndFailedPaths.fulfilled]
 * contains all possible shortest data flow paths between the starting node [this] and the end node
 * fulfilling [predicate]. The paths are represented as lists of nodes. Paths which do not end at
 * such a node are included in [FulfilledAndFailedPaths.failed].
 *
 * Hence, if "fulfilled" is a non-empty list, a data flow from [this] to such a node is **possible
 * but not mandatory**. If the list "failed" is empty, the data flow is mandatory.
 */
fun Node.followEOGEdgesUntilHit(
    collectFailedPaths: Boolean = true,
    findAllPossiblePaths: Boolean = true,
    direction: AnalysisDirection = Forward(GraphToFollow.EOG),
    vararg sensitivities: AnalysisSensitivity = FilterUnreachableEOG + ContextSensitive,
    scope: AnalysisScope = Interprocedural(),
    earlyTermination: (Node, Context) -> Boolean = { _, _ -> false },
    predicate: (Node) -> Boolean,
): FulfilledAndFailedPaths {
    return this.followXUntilHit(
        x = { currentNode, ctx, path ->
            direction.pickNextStep(currentNode, scope, ctx, sensitivities = sensitivities)
        },
        collectFailedPaths = collectFailedPaths,
        findAllPossiblePaths = findAllPossiblePaths,
        earlyTermination = earlyTermination,
        predicate = predicate,
    )
}

/**
 * Returns an instance of [FulfilledAndFailedPaths] where [FulfilledAndFailedPaths.fulfilled]
 * contains all possible shortest data flow paths between the starting node [this] and the end node
 * fulfilling [predicate]. The paths are represented as lists of nodes. Paths which do not end at
 * such a node are included in [FulfilledAndFailedPaths.failed].
 *
 * Hence, if "fulfilled" is a non-empty list, a data flow from [this] to such a node is **possible
 * but not mandatory**. If the list "failed" is empty, the data flow is mandatory.
 */
fun Node.followDFGEdgesUntilHit(
    collectFailedPaths: Boolean = true,
    findAllPossiblePaths: Boolean = true,
    direction: AnalysisDirection = Forward(GraphToFollow.DFG),
    vararg sensitivities: AnalysisSensitivity = FieldSensitive + ContextSensitive,
    scope: AnalysisScope = Interprocedural(),
    earlyTermination: (Node, Context) -> Boolean = { _, _ -> false },
    predicate: (Node) -> Boolean,
): FulfilledAndFailedPaths {
    return this.followXUntilHit(
        x = { currentNode, ctx, path ->
            direction.pickNextStep(currentNode, scope, ctx, sensitivities = sensitivities)
        },
        collectFailedPaths = collectFailedPaths,
        findAllPossiblePaths = findAllPossiblePaths,
        earlyTermination = earlyTermination,
        predicate = predicate,
    )
}

/**
 * This class holds the context for the [followXUntilHit] function. It is used to keep track of the
 * current index stack and call stack.
 *
 * This class supports copying to allow for cloning the context when needed. This is needed when we
 * need to follow more than one path at a time, otherwise the different branches would override the
 * stacks.
 */
class Context(
    val indexStack: SimpleStack<IndexedDataflowGranularity> = SimpleStack(),
    val callStack: SimpleStack<CallExpression> = SimpleStack(),
    var steps: Int,
) : HasAssumptions {

    override val assumptions: MutableSet<Assumption> = mutableSetOf()

    fun clone(): Context {
        return Context(indexStack = indexStack.clone(), callStack = callStack.clone(), steps)
    }

    operator fun inc(): Context {
        this.steps++
        return this
    }
}

/** Implementation of a simple stack, based on an [ArrayDeque] */
class SimpleStack<T> {
    private val deque = ArrayDeque<T>()

    val depth: Int
        get() = deque.size

    /** Returns true if the stack is empty. */
    fun isEmpty(): Boolean = deque.isEmpty()

    /** Pushes a new element onto the stack. */
    fun push(newElem: T) {
        deque.addFirst(newElem)
    }

    /** Returns the top element from the stack, without popping it. */
    val top: T?
        get() = deque.firstOrNull()

    /**
     * Pops the top element from the stack, if [elemToPop] is the top element.
     *
     * @return true if the element was popped, false otherwise
     */
    fun popIfOnTop(elemToPop: T): Boolean {
        if (deque.firstOrNull() == elemToPop) {
            deque.removeFirst()
            return true
        }
        return false
    }

    /** Pops the top element from the stack. */
    fun pop(): T = deque.removeFirst()

    /** Clones the stack. */
    fun clone(): SimpleStack<T> {
        return SimpleStack<T>().apply { deque.addAll(this@SimpleStack.deque) }
    }

    override fun equals(other: Any?): Boolean {
        return other is SimpleStack<T> && this.depth == other.depth && this.deque == other.deque
    }

    override fun hashCode(): Int {
        return deque.hashCode()
    }

    fun startsWith(other: SimpleStack<T>): Boolean {
        for ((idx, elem) in other.deque.withIndex()) {
            if (elem != this.deque.getOrNull(idx)) {
                return false
            }
        }
        return true
    }

    operator fun contains(elem: T): Boolean {
        return deque.contains(elem)
    }
}

/**
 * Iterates the next full DFG edges until there are no more edges available (or until a loop is
 * detected). Returns a list of possible paths (each path is represented by a list of nodes).
 */
fun Node.collectAllNextDFGPaths(
    interproceduralAnalysis: Boolean = true,
    contextSensitive: Boolean = true,
): List<NodePath> {
    // We make everything fail to reach the end of the CDG. Then, we use the stuff collected in the
    // failed paths (everything)
    return followDFGEdgesUntilHit(
            collectFailedPaths = true,
            findAllPossiblePaths = true,
            direction = Forward(GraphToFollow.DFG),
            sensitivities =
                if (contextSensitive) {
                    FieldSensitive + ContextSensitive
                } else {
                    arrayOf(FieldSensitive)
                },
            scope =
                if (interproceduralAnalysis) {
                    Interprocedural()
                } else {
                    Intraprocedural()
                },
            predicate = { false },
        )
        .failed
        .map { it.second }
}

/**
 * Iterates the next full DFG edges until there are no more edges available (or until a loop is
 * detected). Returns a list of possible paths (each path is represented by a list of nodes).
 */
fun Node.collectAllNextFullDFGPaths(): List<NodePath> {
    // We make everything fail to reach the end of the CDG. Then, we use the stuff collected in the
    // failed paths (everything)
    return this.followNextFullDFGEdgesUntilHit(
            collectFailedPaths = true,
            findAllPossiblePaths = true,
        ) {
            false
        }
        .failed
        .map { it.second }
}

/**
 * Iterates the next EOG edges until there are no more edges available (or until a loop is
 * detected). Returns a list of possible paths (each path is represented by a list of nodes).
 */
fun Node.collectAllNextEOGPaths(interproceduralAnalysis: Boolean = true): List<NodePath> {
    // We make everything fail to reach the end of the CDG. Then, we use the stuff collected in the
    // failed paths (everything)
    return this.followEOGEdgesUntilHit(
            collectFailedPaths = true,
            findAllPossiblePaths = true,
            scope = if (interproceduralAnalysis) Interprocedural() else Intraprocedural(),
        ) {
            false
        }
        .failed
        .map { it.second }
}

/**
 * Iterates the prev PDG edges until there are no more edges available (or until a loop is
 * detected). Returns a list of possible paths (each path is represented by a list of nodes).
 */
fun Node.collectAllPrevEOGPaths(interproceduralAnalysis: Boolean): List<NodePath> {
    // We make everything fail to reach the end of the CDG. Then, we use the stuff collected in the
    // failed paths (everything)
    return this.followEOGEdgesUntilHit(
            direction = Backward(GraphToFollow.EOG),
            collectFailedPaths = true,
            findAllPossiblePaths = true,
            scope = if (interproceduralAnalysis) Interprocedural() else Intraprocedural(),
        ) {
            false
        }
        .failed
        .map { it.second }
}

/**
 * Iterates the next PDG edges until there are no more edges available (or until a loop is
 * detected). Returns a list of possible paths (each path is represented by a list of nodes).
 */
fun Node.collectAllNextPDGGPaths(): List<NodePath> {
    // We make everything fail to reach the end of the CDG. Then, we use the stuff collected in the
    // failed paths (everything)
    return this.followNextPDGUntilHit(collectFailedPaths = true, findAllPossiblePaths = true) {
            false
        }
        .failed
        .map { it.second }
}

/**
 * Iterates the prev PDG edges until there are no more edges available (or until a loop is
 * detected). Returns a list of possible paths (each path is represented by a list of nodes).
 */
fun Node.collectAllPrevPDGPaths(interproceduralAnalysis: Boolean): List<NodePath> {
    // We make everything fail to reach the end of the CDG. Then, we use the stuff collected in the
    // failed paths (everything)
    return this.followPrevPDGUntilHit(
            collectFailedPaths = true,
            findAllPossiblePaths = true,
            interproceduralAnalysis = interproceduralAnalysis,
        ) {
            false
        }
        .failed
        .map { it.second }
}

/**
 * Iterates the prev CDG edges until there are no more edges available (or until a loop is
 * detected). Returns a list of possible paths (each path is represented by a list of nodes).
 */
fun Node.collectAllPrevCDGPaths(interproceduralAnalysis: Boolean): List<NodePath> {
    // We make everything fail to reach the end of the CDG. Then, we use the stuff collected in the
    // failed paths (everything)
    return this.followPrevCDGUntilHit(
            collectFailedPaths = true,
            findAllPossiblePaths = true,
            interproceduralAnalysis = interproceduralAnalysis,
        ) {
            false
        }
        .failed
        .map { it.second }
}

/**
 * Iterates the next CDG edges until there are no more edges available (or until a loop is
 * detected). Returns a list of possible paths (each path is represented by a list of nodes).
 */
fun Node.collectAllNextCDGPaths(interproceduralAnalysis: Boolean): List<NodePath> {
    // We make everything fail to reach the end of the CDG. Then, we use the stuff collected in the
    // failed paths (everything)
    return this.followNextCDGUntilHit(
            collectFailedPaths = true,
            findAllPossiblePaths = true,
            interproceduralAnalysis = interproceduralAnalysis,
        ) {
            false
        }
        .failed
        .map { it.second }
}

/**
 * Returns an instance of [FulfilledAndFailedPaths] where [FulfilledAndFailedPaths.fulfilled]
 * contains all possible shortest data flow paths (with [ProgramDependencies]) between the starting
 * node [this] and the end node fulfilling [predicate]. The paths are represented as lists of nodes.
 * Paths which do not end at such a node are included in [FulfilledAndFailedPaths.failed].
 *
 * Hence, if "fulfilled" is a non-empty list, a data flow from [this] to such a node is **possible
 * but not mandatory**. If the list "failed" is empty, the data flow is mandatory.
 */
fun Node.followNextPDGUntilHit(
    collectFailedPaths: Boolean = true,
    findAllPossiblePaths: Boolean = true,
    interproceduralAnalysis: Boolean = false,
    earlyTermination: (Node, Context) -> Boolean = { _, _ -> false },
    predicate: (Node) -> Boolean,
): FulfilledAndFailedPaths {
    return followXUntilHit(
        x = { currentNode, ctx, _ ->
            val nextNodes = currentNode.nextPDG.toMutableList()
            if (interproceduralAnalysis) {
                nextNodes.addAll((currentNode as? CallExpression)?.calls ?: listOf())
            }
            nextNodes.map { it to ctx }
        },
        collectFailedPaths = collectFailedPaths,
        findAllPossiblePaths = findAllPossiblePaths,
        earlyTermination = earlyTermination,
        predicate = predicate,
    )
}

/**
 * Returns an instance of [FulfilledAndFailedPaths] where [FulfilledAndFailedPaths.fulfilled]
 * contains all possible shortest data flow paths (with [ControlDependence]) between the starting
 * node [this] and the end node fulfilling [predicate]. The paths are represented as lists of nodes.
 * Paths which do not end at such a node are included in [FulfilledAndFailedPaths.failed].
 *
 * Hence, if "fulfilled" is a non-empty list, a data flow from [this] to such a node is **possible
 * but not mandatory**. If the list "failed" is empty, the data flow is mandatory.
 */
fun Node.followNextCDGUntilHit(
    collectFailedPaths: Boolean = true,
    findAllPossiblePaths: Boolean = true,
    interproceduralAnalysis: Boolean = false,
    earlyTermination: (Node, Context) -> Boolean = { _, _ -> false },
    predicate: (Node) -> Boolean,
): FulfilledAndFailedPaths {
    return followXUntilHit(
        x = { currentNode, ctx, _ ->
            val nextNodes = currentNode.nextCDG.toMutableList()
            if (interproceduralAnalysis) {
                nextNodes.addAll((currentNode as? CallExpression)?.calls ?: listOf())
            }
            nextNodes.map { it to ctx }
        },
        collectFailedPaths = collectFailedPaths,
        findAllPossiblePaths = findAllPossiblePaths,
        earlyTermination = earlyTermination,
        predicate = predicate,
    )
}

/**
 * Returns an instance of [FulfilledAndFailedPaths] where [FulfilledAndFailedPaths.fulfilled]
 * contains all possible shortest data flow paths (with [ProgramDependencies]) between the starting
 * node [this] and the end node fulfilling [predicate] (backwards analysis). The paths are
 * represented as lists of nodes. Paths which do not end at such a node are included in
 * [FulfilledAndFailedPaths.failed].
 *
 * Hence, if "fulfilled" is a non-empty list, a CDG path from [this] to such a node is **possible
 * but not mandatory**. If the list "failed" is empty, the data flow is mandatory.
 */
fun Node.followPrevPDGUntilHit(
    collectFailedPaths: Boolean = true,
    findAllPossiblePaths: Boolean = true,
    interproceduralAnalysis: Boolean = false,
    interproceduralMaxDepth: Int? = null,
    earlyTermination: (Node, Context) -> Boolean = { _, _ -> false },
    predicate: (Node) -> Boolean,
): FulfilledAndFailedPaths {
    return followXUntilHit(
        x = { currentNode, ctx, _ ->
            val nextNodes = currentNode.prevPDG.toMutableList()
            if (interproceduralAnalysis) {
                nextNodes.addAll(
                    (currentNode as? FunctionDeclaration)?.usages?.mapNotNull {
                        val result =
                            if (interproceduralMaxDepth?.let { ctx.callStack.depth >= it } != true)
                                it.astParent as? CallExpression
                            else null
                        result?.let { ctx.callStack.push(it) }
                        result
                    } ?: listOf()
                )
            }
            nextNodes.map { it to ctx }
        },
        collectFailedPaths = collectFailedPaths,
        findAllPossiblePaths = findAllPossiblePaths,
        earlyTermination = earlyTermination,
        predicate = predicate,
    )
}

/**
 * Returns an instance of [FulfilledAndFailedPaths] where [FulfilledAndFailedPaths.fulfilled]
 * contains all possible shortest data flow paths (with [ControlDependence]) between the starting
 * node [this] and the end node fulfilling [predicate] (backwards analysis). The paths are
 * represented as lists of nodes. Paths which do not end at such a node are included in
 * [FulfilledAndFailedPaths.failed].
 *
 * Hence, if "fulfilled" is a non-empty list, a CDG path from [this] to such a node is **possible
 * but not mandatory**. If the list "failed" is empty, the data flow is mandatory.
 */
fun Node.followPrevCDGUntilHit(
    collectFailedPaths: Boolean = true,
    findAllPossiblePaths: Boolean = true,
    interproceduralAnalysis: Boolean = false,
    interproceduralMaxDepth: Int? = null,
    earlyTermination: (Node, Context) -> Boolean = { _, _ -> false },
    predicate: (Node) -> Boolean,
): FulfilledAndFailedPaths {
    return followXUntilHit(
        x = { currentNode, ctx, _ ->
            val nextNodes = currentNode.prevCDG.toMutableList()
            if (interproceduralAnalysis) {
                nextNodes.addAll(
                    (currentNode as? FunctionDeclaration)?.usages?.mapNotNull {
                        val result =
                            if (interproceduralMaxDepth?.let { ctx.callStack.depth >= it } != true)
                                it.astParent as? CallExpression
                            else null
                        result?.let { ctx.callStack.push(it) }
                        result
                    } ?: listOf()
                )
            }
            nextNodes.map { it to ctx }
        },
        collectFailedPaths = collectFailedPaths,
        findAllPossiblePaths = findAllPossiblePaths,
        earlyTermination = earlyTermination,
        predicate = predicate,
    )
}

/**
 * Returns an instance of [FulfilledAndFailedPaths] where [FulfilledAndFailedPaths.fulfilled]
 * contains all possible shortest data flow paths (with [x] specifying how to fetch more nodes)
 * between the starting node [this] and the end node fulfilling [predicate] (backwards analysis).
 * The paths are represented as lists of nodes. Paths which do not end at such a node are included
 * in [FulfilledAndFailedPaths.failed].
 *
 * Hence, if "fulfilled" is a non-empty list, a path from [this] to such a node is **possible but
 * not mandatory**. If the list "failed" is empty, the path is mandatory.
 */
fun Node.followXUntilHit(
    x: (Node, Context, List<Node>) -> Collection<Pair<Node, Context>>,
    collectFailedPaths: Boolean = true,
    findAllPossiblePaths: Boolean = true,
    context: Context = Context(steps = 0),
    earlyTermination: (Node, Context) -> Boolean,
    predicate: (Node) -> Boolean,
): FulfilledAndFailedPaths {
    // Looks complicated but at least it's not recursive...
    // result: List of paths (between from and to)
    val fulfilledPaths = mutableListOf<NodePath>()
    // failedPaths: All the paths which do not satisfy "predicate"
    val failedPaths = mutableListOf<Pair<FailureReason, NodePath>>()
    val loopingPaths = mutableListOf<NodePath>()
    // The list of paths where we're not done yet.
    val worklist = identitySetOf<List<Pair<Node, Context>>>()
    worklist.add(listOf(this to context)) // We start only with the "from" node (=this)

    val alreadySeenNodes = mutableSetOf<Pair<Node, Context>>()

    while (worklist.isNotEmpty()) {
        val currentPath = worklist.maxBy { it.size }
        worklist.remove(currentPath)
        val currentNode = currentPath.last().first
        var currentContext = currentPath.last().second
        alreadySeenNodes.add(currentNode to currentContext)
        val currentPathNodes = currentPath.map { it.first }
        // The last node of the path is where we continue. We get all of its outgoing CDG edges and
        // follow them
        val nextNodes = x(currentNode, currentContext, currentPathNodes)

        // No further nodes in the path and the path criteria are not satisfied.
        if (nextNodes.isEmpty() && collectFailedPaths) {
            // TODO: How to determine if this path is really at the end or if it exceeded the number
            // of steps?
            failedPaths.add(
                FailureReason.PATH_ENDED to
                    NodePath(currentPath.map { it.first })
                        .addAssumptionDependences(currentPath.map { it.second }.toList())
            )
        }

        for ((next, newContext) in nextNodes) {
            // Copy the path for each outgoing CDG edge and add the next node
            if (predicate(next)) {
                // We ended up in the node fulfilling "predicate", so we're done for this path. Add
                // the path to the results.
                fulfilledPaths.add(
                    NodePath(currentPathNodes.toMutableList() + next)
                        .addAssumptionDependences(currentPath.map { it.second } + newContext)
                )
                continue // Don't add this path anymore. The requirement is satisfied.
            }
            if (earlyTermination(next, currentContext)) {
                failedPaths.add(
                    FailureReason.HIT_EARLY_TERMINATION to
                        NodePath(currentPath.map { it.first } + next)
                            .addAssumptionDependences(currentPath.map { it.second } + newContext)
                )
                continue // Don't add this path anymore. We already failed.
            }
            // The next node is new in the current path (i.e., there's no loop), so we add the path
            // with the next step to the worklist.
            if (
                !isNodeWithCallStackInPath(next, newContext, currentPath) &&
                    (findAllPossiblePaths ||
                        (!isNodeWithCallStackInPath(next, newContext, alreadySeenNodes) &&
                            worklist.none { isNodeWithCallStackInPath(next, newContext, it) }))
            ) {
                worklist.add(currentPath.toMutableList() + (next to newContext.inc()))
            } else {
                // There's a loop.
                loopingPaths.add(
                    NodePath(currentPathNodes + next)
                        .addAssumptionDependences(currentPath.map { it.second } + newContext)
                )
            }
        }
    }

    val failedLoops =
        loopingPaths
            .filter { path ->
                fulfilledPaths.none {
                    it.nodes.size > path.nodes.size &&
                        it.nodes.subList(0, path.nodes.size - 1) == path.nodes
                } &&
                    failedPaths.none {
                        it.second.nodes.size > path.nodes.size &&
                            it.second.nodes.subList(0, path.nodes.size - 1) == path.nodes
                    }
            }
            .map { FailureReason.PATH_ENDED to it }

    return FulfilledAndFailedPaths(
        fulfilledPaths.toSet().toList(),
        (failedPaths + failedLoops).toSet().toList().map { Pair(it.first, it.second) },
    )
}

/**
 * Checks if the [node] is already in the [path] and if the last element on the call stack reaching
 * it is the same in [context] and in the [path]. This serves as an indication of a loop. We need
 * this to differentiate that a node (e.g., function declaration) can be visited through multiple
 * paths, e.g., if there are subsequent calls to the same function without indicating a loop.
 */
fun isNodeWithCallStackInPath(
    node: Node,
    context: Context,
    path: Collection<Pair<Node, Context>>,
): Boolean {
    return path.any {
        it.first == node &&
            context.callStack.top?.let { top -> top in it.second.callStack } != false
    }
}

/**
 * Returns an instance of [FulfilledAndFailedPaths] where [FulfilledAndFailedPaths.fulfilled]
 * contains all possible shortest data flow paths (with [FullDataflowGranularity]) between the
 * starting node [this] and the end node fulfilling [predicate]. The paths are represented as lists
 * of nodes. Paths which do not end at such a node are included in [FulfilledAndFailedPaths.failed].
 *
 * Hence, if "fulfilled" is a non-empty list, a data flow from [this] to such a node is **possible
 * but not mandatory**. If the list "failed" is empty, the data flow is mandatory.
 */
fun Node.followNextFullDFGEdgesUntilHit(
    collectFailedPaths: Boolean = true,
    findAllPossiblePaths: Boolean = true,
    earlyTermination: (Node, Context) -> Boolean = { _, _ -> false },
    predicate: (Node) -> Boolean,
): FulfilledAndFailedPaths {
    return followDFGEdgesUntilHit(
        collectFailedPaths = collectFailedPaths,
        findAllPossiblePaths = findAllPossiblePaths,
        earlyTermination = earlyTermination,
        predicate = predicate,
        direction = Forward(GraphToFollow.DFG),
        sensitivities = OnlyFullDFG + ContextSensitive,
        scope = Interprocedural(),
    )
}

/**
 * Returns a [Collection] of last nodes in the EOG of this [FunctionDeclaration]. If there's no
 * function body, it will return a list of this function declaration. This function does not
 * propagate assumptions currently.
 */
val FunctionDeclaration.lastEOGNodes: Collection<Node>
    get() {
        val lastEOG = collectAllNextEOGPaths(false).flatMap { it.nodes.last().prevEOGEdges }
        return if (lastEOG.isEmpty()) {
            // In some cases, we do not have a body, so we have to jump directly to the
            // function declaration.
            listOf(this)
        } else lastEOG.filter { it.unreachable != true }.map { it.start }
    }

/** Returns only potentially reachable previous EOG edges. */
val Node.reachablePrevEOG: Collection<Node>
    get() = this.prevEOGEdges.filter { it.unreachable != true }.map { it.start }

/** Returns only potentially reachable previous EOG edges. */
val Node.reachableNextEOG: Collection<Node>
    get() = this.nextEOGEdges.filter { it.unreachable != true }.map { it.end }

/**
 * Returns a list of edges which are from the evaluation order between the starting node [this] and
 * an edge fulfilling [predicate]. If the return value is not `null`, a path from [this] to such an
 * edge is **possible but not mandatory**.
 *
 * It returns only a single possible path even if multiple paths are possible.
 */
fun Node.followNextEOG(predicate: (Edge<*>) -> Boolean): List<Edge<*>>? {
    val path = mutableListOf<Edge<*>>()

    for (edge in this.nextEOGEdges.filter { it.unreachable != true }) {
        val target = edge.end

        path.add(edge)

        if (predicate(edge)) {
            return path
        }

        val subPath = target.followNextEOG(predicate)
        if (subPath != null) {
            path.addAll(subPath)

            return path
        }
    }

    return null
}

/**
 * Returns a list of edges which are from the evaluation order between the starting node [this] and
 * an edge fulfilling [predicate]. If the return value is not `null`, a path from [this] to such an
 * edge is **possible but not mandatory**.
 *
 * It returns only a single possible path even if multiple paths are possible.
 */
fun Node.followPrevEOG(predicate: (Edge<*>) -> Boolean): List<Edge<*>>? {
    val path = mutableListOf<Edge<*>>()

    for (edge in this.prevEOGEdges.filter { it.unreachable != true }) {
        val source = edge.start

        path.add(edge)

        if (predicate(edge)) {
            return path
        }

        val subPath = source.followPrevEOG(predicate)
        if (subPath != null) {
            path.addAll(subPath)

            return path
        }
    }

    return null
}

/**
 * Returns a list of nodes which are a data flow path (with [FullDataflowGranularity]) between the
 * starting node [this] and the end node fulfilling [predicate]. If the return value is not `null`,
 * a data flow from [this] to such a node is **possible but not mandatory**.
 *
 * It returns only a single possible path even if multiple paths are possible.
 */
fun Node.followPrevFullDFG(predicate: (Node) -> Boolean): NodePath? {
    return followPrevFullDFGEdgesUntilHit(
            collectFailedPaths = false,
            findAllPossiblePaths = false,
            predicate = predicate,
        )
        .fulfilled
        .minByOrNull { it.nodes.size }
}

/**
 * Returns a list of nodes which are a data flow path (with [FullDataflowGranularity]) between the
 * starting node [this] and the end node fulfilling [predicate]. If the return value is not `null`,
 * a data flow from [this] to such a node is **possible but not mandatory**.
 *
 * It returns only a single possible path even if multiple paths are possible.
 */
fun Node.followPrevDFG(predicate: (Node) -> Boolean): NodePath? {
    return followDFGEdgesUntilHit(
            collectFailedPaths = false,
            findAllPossiblePaths = false,
            predicate = predicate,
            direction = Backward(GraphToFollow.DFG),
        )
        .fulfilled
        .minByOrNull { it.nodes.size }
}

/** Returns all [Node] children in the AST-subgraph, starting with this [Node]. */
val Node?.nodes: List<Node>
    get() = this.allChildren()

/** Returns all [CallExpression] children in this graph, starting with this [Node]. */
val Node?.calls: List<CallExpression>
    get() = this.allChildren()

/** Returns all [OperatorCallExpression] children in this graph, starting with this [Node]. */
val Node?.operatorCalls: List<OperatorCallExpression>
    get() = this.allChildren()

/** Returns all [MemberCallExpression] children in this graph, starting with this [Node]. */
val Node?.mcalls: List<MemberCallExpression>
    get() = this.allChildren()

/** Returns all [CastExpression] children in this graph, starting with this [Node]. */
val Node?.casts: List<CastExpression>
    get() = this.allChildren()

/** Returns all [MethodDeclaration] children in this graph, starting with this [Node]. */
val Node?.methods: List<MethodDeclaration>
    get() = this.allChildren()

/** Returns all [OperatorDeclaration] children in this graph, starting with this [Node]. */
val Node?.operators: List<OperatorDeclaration>
    get() = this.allChildren()

/** Returns all [FieldDeclaration] children in this graph, starting with this [Node]. */
val Node?.fields: List<FieldDeclaration>
    get() = this.allChildren()

/** Returns all [ParameterDeclaration] children in this graph, starting with this [Node]. */
val Node?.parameters: List<ParameterDeclaration>
    get() = this.allChildren()

/** Returns all [FunctionDeclaration] children in this graph, starting with this [Node]. */
val Node?.functions: List<FunctionDeclaration>
    get() = this.allChildren()

/** Returns all [RecordDeclaration] children in this graph, starting with this [Node]. */
val Node?.records: List<RecordDeclaration>
    get() = this.allChildren()

/** Returns all [RecordDeclaration] children in this graph, starting with this [Node]. */
val Node?.namespaces: List<NamespaceDeclaration>
    get() = this.allChildren()

/** Returns all [ImportDeclaration] children in this graph, starting with this [Node]. */
val Node?.imports: List<ImportDeclaration>
    get() = this.allChildren()

/** Returns all [VariableDeclaration] children in this graph, starting with this [Node]. */
val Node?.variables: List<VariableDeclaration>
    get() = this.allChildren()

/** Returns all [Literal] children in this graph, starting with this [Node]. */
val Node?.literals: List<Literal<*>>
    get() = this.allChildren()

/** Returns all [Block] child edges in this graph, starting with this [Node]. */
val Node?.blocks: List<Block>
    get() = this.allChildren()

/** Returns all [Reference] children in this graph, starting with this [Node]. */
val Node?.refs: List<Reference>
    get() = this.allChildren()

/** Returns all [MemberExpression] children in this graph, starting with this [Node]. */
val Node?.memberExpressions: List<MemberExpression>
    get() = this.allChildren()

/** Returns all [Statement] child edges in this graph, starting with this [Node]. */
val Node?.statements: List<Statement>
    get() = this.allChildren()

/** Returns all [ForStatement] child edges in this graph, starting with this [Node]. */
val Node?.forLoops: List<ForStatement>
    get() = this.allChildren()

/** Returns all [TryStatement] child edges in this graph, starting with this [Node]. */
val Node?.trys: List<TryStatement>
    get() = this.allChildren()

/** Returns all [ThrowExpression] child edges in this graph, starting with this [Node]. */
val Node?.throws: List<ThrowExpression>
    get() = this.allChildren()

/** Returns all [ForEachStatement] child edges in this graph, starting with this [Node]. */
val Node?.forEachLoops: List<ForEachStatement>
    get() = this.allChildren()

/** Returns all [SwitchStatement] child edges in this graph, starting with this [Node]. */
val Node?.switches: List<SwitchStatement>
    get() = this.allChildren()

/** Returns all [WhileStatement] child edges in this graph, starting with this [Node]. */
val Node?.whileLoops: List<WhileStatement>
    get() = this.allChildren()

/** Returns all [DoStatement] child edges in this graph, starting with this [Node]. */
val Node?.doLoops: List<DoStatement>
    get() = this.allChildren()

/** Returns all [BreakStatement] child edges in this graph, starting with this [Node]. */
val Node?.breaks: List<BreakStatement>
    get() = this.allChildren()

/** Returns all [ContinueStatement] child edges in this graph, starting with this [Node]. */
val Node?.continues: List<ContinueStatement>
    get() = this.allChildren()

/** Returns all [IfStatement] child edges in this graph, starting with this [Node]. */
val Node?.ifs: List<IfStatement>
    get() = this.allChildren()

/** Returns all [LabelStatement] child edges in this graph, starting with this [Node]. */
val Node?.labels: List<LabelStatement>
    get() = this.allChildren()

/** Returns all [ReturnStatement] child edges in this graph, starting with this [Node]. */
val Node?.returns: List<ReturnStatement>
    get() = this.allChildren()

/** Returns all [AssignExpression] child edges in this graph, starting with this [Node]. */
val Node?.assigns: List<AssignExpression>
    get() = this.allChildren()

/**
 * This function tries to find the first parent node of type [T] that satisfies the optional
 * condition specified in [predicate]. If [predicate] is `null`, the first AST parent node of type
 * [T] is returned. It starts searching in [this], moving upwards using the [Node.astParent]
 * attribute.
 *
 * @param predicate the optional search predicate
 */
inline fun <reified T : Node> Node.firstParentOrNull(
    noinline predicate: ((T) -> Boolean)? = null
): T? {

    // start at searchNodes parent
    var node = this.astParent

    while (node != null) {
        if (node is T && (predicate == null || predicate(node))) {
            return node
        }

        // go upwards in the ast tree
        node = node.astParent
    }

    return null
}

/**
 * This function tries to find the first parent scope of type [T] that satisfies the optional
 * condition specified in [predicate]. If [predicate] is `null`, the first parent scope of type [T]
 * is returned. It starts searching in [this], moving upwards using the [Scope.parent] attribute.
 *
 * If no parent scope matches the predicate, null is returned.
 *
 * @param predicate The predicate optional to match the parent scope against
 */
inline fun <reified T : Scope> Scope.firstScopeParentOrNull(
    noinline predicate: ((T) -> Boolean)? = null
): T? {
    var scope = this.parent
    while (scope != null) {
        if (scope is T && (predicate == null || predicate(scope))) {
            return scope
        }

        // go upwards in the scope tree
        scope = scope.parent
    }

    return null
}

/**
 * Return all [ProblemNode] children in this graph (either stored directly or in
 * [Node.additionalProblems]), starting with this [Node].
 */
val Node?.problems: List<ProblemNode>
    get() {
        val relevantNodes =
            this.allChildren<Node> { it is ProblemNode || it.additionalProblems.isNotEmpty() }

        val result = mutableListOf<ProblemNode>()

        relevantNodes.forEach {
            if (it.additionalProblems.isNotEmpty()) {
                result += it.additionalProblems
            }
            if (it is ProblemNode) {
                result += it
            }
        }

        return result
    }

/** Returns all [Assignment] child edges in this graph, starting with this [Node]. */
val Node?.assignments: List<Assignment>
    get() {
        return this?.allChildren<Node>()?.filterIsInstance<AssignmentHolder>()?.flatMap {
            it.assignments
        } ?: listOf()
    }

/**
 * Returns the [Assignment.value] of the first (by EOG order beginning from) [Assignment] that this
 * variable has as its [Assignment.target] in the scope of the variable.
 */
val VariableDeclaration.firstAssignment: Expression?
    get() {
        val start = this.scope?.astNode ?: return null
        val assignments = start.assignments.filter { (it.target as? Reference)?.refersTo == this }

        // We need to measure the distance between the start and each assignment value
        return assignments
            .map { Pair(it, start.eogDistanceTo(it.value)) }
            .minByOrNull { it.second }
            ?.first
            ?.value
    }

/** Returns the [i]-th item in this list (or null) and casts it to [T]. */
inline operator fun <reified T> List<Node>.invoke(i: Int = 0): T? {
    return this.getOrNull(i) as? T
}

operator fun <N : Expression> Expression?.invoke(): N? {
    return this as? N
}

/** Returns all [CallExpression]s in this graph which call a method with the given [name]. */
fun TranslationResult.callsByName(name: String): List<CallExpression> {
    @Suppress("UNCHECKED_CAST")
    return SubgraphWalker.flattenAST(this).filter { node ->
        node is CallExpression && node.invokes.any { it.name.lastPartsMatch(name) }
    } as List<CallExpression>
}

/** Set of all functions which are called from this function */
val FunctionDeclaration.callees: Set<FunctionDeclaration>
    get() {
        return this.calls
            .map { it.invokes }
            .foldRight(mutableListOf<FunctionDeclaration>()) { l, res ->
                res.addAll(l)
                res
            }
            .toSet()
    }

/** Retrieves the n-th statement of the body of this function declaration. */
operator fun FunctionDeclaration.get(n: Int): Statement? {
    val body = this.body

    if (body is Block) {
        return body[n]
    } else if (n == 0) {
        return body
    }

    return null
}

/** Set of all functions calling [function] */
fun TranslationResult.callersOf(function: FunctionDeclaration): Set<FunctionDeclaration> {
    return this.functions.filter { function in it.callees }.toSet()
}

/** All nodes which depend on this if statement */
fun IfStatement.controls(): List<Node> {
    val result = mutableListOf<Node>()
    result.addAll(SubgraphWalker.flattenAST(this.thenStatement))
    result.addAll(SubgraphWalker.flattenAST(this.elseStatement))
    return result
}

/** All nodes which depend on this if statement */
fun Node.controlledBy(): List<Node> {
    val result = mutableListOf<Node>()
    var checkedNode: Node? = this
    while (checkedNode !is FunctionDeclaration) {
        checkedNode = checkedNode?.astParent
        if (checkedNode == null) {
            break
        }
        if (checkedNode is IfStatement || checkedNode is SwitchStatement) {
            result.add(checkedNode)
        }
    }
    return result
}

/**
 * Returns the expression specifying the dimension (i.e., size) of the array during its
 * initialization.
 */
val SubscriptExpression.arraySize: Expression
    get() =
        (((this.arrayExpression as Reference).refersTo as VariableDeclaration).initializer
                as NewArrayExpression)
            .dimensions[0]

/**
 * This helper function calculates the "distance", i.e., number of EOG edges between this node and
 * the node specified in [to].
 */
private fun Node.eogDistanceTo(to: Node): Int {
    var i = 0
    this.followNextEOG {
        i++
        it.end == to
    }

    return i
}

/**
 * This is a small utility function to "unwrap" a [Reference] that it is wrapped in (multiple)
 * [Expression] nodes. This will only work on expression that only have one "argument" (such as a
 * unary operator), in order to avoid ambiguous results. This can be useful for data-flow analysis,
 * if you want to quickly retrieve the reference that is affected by an operation. For example in
 * C++ it is common to take an address of a variable and cast it into an appropriate type:
 * ```cpp
 * int64_t addr = (int64_t) &a;
 * ```
 *
 * When called on the right-hand side of this assignment, this function will return `a`.
 */
fun Expression?.unwrapReference(): Reference? {
    return when {
        this is Reference -> this
        this is UnaryOperator && (this.operatorCode == "*" || this.operatorCode == "&") ->
            this.input.unwrapReference()
        this is CastExpression -> this.expression.unwrapReference()
        else -> null
    }
}

/** Returns the [TranslationUnitDeclaration] where this node is located in. */
val Node.translationUnit: TranslationUnitDeclaration?
    get() {
        return this as? TranslationUnitDeclaration
            ?: firstParentOrNull<TranslationUnitDeclaration>()
    }

/** Returns the [TranslationResult] where this node is located in. */
val Node.translationResult: TranslationResult?
    get() {
        return this as? TranslationResult ?: firstParentOrNull<TranslationResult>()
    }

/** Returns the [Component] where this node is located in. */
val Node.component: Component?
    get() {
        return this as? Component ?: firstParentOrNull<Component>()
    }

/**
 * This helper function be used to find out if a particular expression (usually a [CallExpression]
 * or a [Reference]) is imported through a [ImportDeclaration].
 *
 * It returns a [Pair], with the [Pair.first] being a boolean value whether it was imported and
 * [Pair.second] the [ImportDeclaration] if applicable.
 */
val Expression.importedFrom: List<ImportDeclaration>
    get() {
        if (this is CallExpression) {
            return this.callee.importedFrom
        } else if (this is MemberExpression) {
            return this.base.importedFrom
        } else if (this is Reference) {
            val imports = this.translationUnit.imports

            return if (name.parent == null) {
                // If the name does not have a parent, this reference could directly be the name
                // of an import, let's check
                imports.filter { it.import.lastPartsMatch(name) }
            } else {
                // Otherwise, the parent name could be the import
                imports.filter { it.import == this.name.parent }
            } ?: listOf<ImportDeclaration>()
        }

        return listOf<ImportDeclaration>()
    }

/**
 * Determines whether the expression is imported from another source.
 *
 * This property evaluates to `true` if the expression originates from an external or supplemental
 * source by checking if the [importedFrom] property contains any entries. Otherwise, it evaluates
 * to `false`.
 */
val Expression.isImported: Boolean
    get() {
        return this.importedFrom.isNotEmpty()
    }
