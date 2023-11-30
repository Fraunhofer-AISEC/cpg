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
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.statements.IfStatement
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.SwitchStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Block
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass
import de.fraunhofer.aisec.cpg.passes.astParent

/**
 * Flattens the AST beginning with this node and returns all nodes of type [T]. For convenience, an
 * optional predicate function [predicate] can be supplied, which will be applied via
 * [Collection.filter]
 */
@JvmOverloads
inline fun <reified T> Node?.allChildren(noinline predicate: ((T) -> Boolean)? = null): List<T> {
    val nodes = SubgraphWalker.flattenAST(this)
    val filtered = nodes.filterIsInstance<T>()

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
        return this.allChildren<EOGStarterHolder>().flatMap { it.eogStarters }
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
    UNIQUE
}

/** A shortcut to call [byNameOrNull] using the `[]` syntax. */
operator fun <T : Node> Collection<T>?.get(
    lookup: String,
    modifier: SearchModifier = SearchModifier.NONE
): T? {
    return this.byNameOrNull(lookup, modifier)
}

/** A shortcut to call [firstOrNull] using the `[]` syntax. */
operator fun <T : Node> Collection<T>?.get(
    predicate: (T) -> Boolean,
    modifier: SearchModifier = SearchModifier.NONE
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
    return this.filter { it.name.lastPartsMatch(lookup) }
}

inline fun <reified T : Declaration> DeclarationHolder.byNameOrNull(
    name: String,
    fqn: Boolean = false
): T? {
    var base = this
    var lookup = name

    // lets do a _very_ simple FQN lookup
    // TODO(oxisto): we could do this with a for-loop for multiple nested levels
    if (fqn && name.contains(".")) {
        // take the most left one
        val baseName = name.split(".")[0]

        base =
            this.declarations.filterIsInstance<DeclarationHolder>().firstOrNull {
                (it as Node).name.lastPartsMatch(baseName)
            }
                ?: return null
        lookup = name.split(".")[1]
    }

    return base.declarations.filterIsInstance<T>().firstOrNull { it.name.lastPartsMatch(lookup) }
}

@Throws(DeclarationNotFound::class)
inline fun <reified T : Declaration> DeclarationHolder.byName(
    name: String,
    fqn: Boolean = false
): T {
    return byNameOrNull(name, fqn)
        ?: throw DeclarationNotFound("declaration with name not found or incorrect type")
}

/**
 * This inline function returns the `n`-th body statement (in AST order) as specified in T or `null`
 * if it does not exist or the type does not match.
 *
 * For convenience, `n` defaults to zero, so that the first statement is always easy to fetch.
 */
inline fun <reified T : Statement> FunctionDeclaration.bodyOrNull(n: Int = 0): T? {
    return if (this.body is Block) {
        return (body as? Block)?.statements?.filterIsInstance<T>()?.getOrNull(n)
    } else {
        if (n == 0 && this.body is T) {
            this.body as T
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

class FulfilledAndFailedPaths(val fulfilled: List<List<Node>>, val failed: List<List<Node>>) {
    operator fun component1(): List<List<Node>> = fulfilled

    operator fun component2(): List<List<Node>> = failed
}

/**
 * Returns an instance of [FulfilledAndFailedPaths] where [FulfilledAndFailedPaths.fulfilled]
 * contains all possible shortest data flow paths between the end node [this] and the starting node
 * fulfilling [predicate]. The paths are represented as lists of nodes. Paths which do not end at
 * such a node are included in [FulfilledAndFailedPaths.failed].
 *
 * Hence, if "fulfilled" is a non-empty list, a data flow from [this] to such a node is **possible
 * but not mandatory**. If the list "failed" is empty, the data flow is mandatory.
 */
fun Node.followPrevDFGEdgesUntilHit(predicate: (Node) -> Boolean): FulfilledAndFailedPaths {
    val fulfilledPaths = mutableListOf<List<Node>>()
    val failedPaths = mutableListOf<List<Node>>()
    val worklist = mutableListOf<List<Node>>()
    worklist.add(listOf(this))

    while (worklist.isNotEmpty()) {
        val currentPath = worklist.removeFirst()
        if (currentPath.last().prevDFG.isEmpty()) {
            // No further nodes in the path and the path criteria are not satisfied.
            failedPaths.add(currentPath)
            continue
        }

        for (prev in currentPath.last().prevDFG) {
            // Copy the path for each outgoing DFG edge and add the prev node
            val nextPath = mutableListOf<Node>()
            nextPath.addAll(currentPath)
            nextPath.add(prev)

            if (predicate(prev)) {
                fulfilledPaths.add(nextPath)
                continue // Don't add this path anymore. The requirement is satisfied.
            }
            // The prev node is new in the current path (i.e., there's no loop), so we add the path
            // with the next step to the worklist.
            if (!currentPath.contains(prev)) {
                worklist.add(nextPath)
            }
        }
    }

    return FulfilledAndFailedPaths(fulfilledPaths, failedPaths)
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
fun Node.followNextDFGEdgesUntilHit(
    collectFailedPaths: Boolean = true,
    findAllPossiblePaths: Boolean = true,
    predicate: (Node) -> Boolean
): FulfilledAndFailedPaths {
    // Looks complicated but at least it's not recursive...
    // result: List of paths (between from and to)
    val fulfilledPaths = mutableListOf<List<Node>>()
    // failedPaths: All the paths which do not satisfy "predicate"
    val failedPaths = mutableListOf<List<Node>>()
    // The list of paths where we're not done yet.
    val worklist = mutableSetOf<List<Node>>()
    worklist.add(listOf(this)) // We start only with the "from" node (=this)

    val alreadySeenNodes = mutableSetOf<Node>()

    while (worklist.isNotEmpty()) {
        val currentPath = worklist.maxBy { it.size }
        worklist.remove(currentPath)
        val currentNode = currentPath.last()
        alreadySeenNodes.add(currentNode)
        // The last node of the path is where we continue. We get all of its outgoing DFG edges and
        // follow them
        if (currentNode.nextDFG.isEmpty()) {
            // No further nodes in the path and the path criteria are not satisfied.
            if (collectFailedPaths) failedPaths.add(currentPath)
            continue
        }

        for (next in currentNode.nextDFG) {
            // Copy the path for each outgoing DFG edge and add the next node
            val nextPath = currentPath.toMutableList()
            nextPath.add(next)
            if (predicate(next)) {
                // We ended up in the node fulfilling "predicate", so we're done for this path. Add
                // the path to the results.
                fulfilledPaths.add(nextPath)
                continue // Don't add this path anymore. The requirement is satisfied.
            }
            // The next node is new in the current path (i.e., there's no loop), so we add the path
            // with the next step to the worklist.
            if (
                next !in currentPath &&
                    (findAllPossiblePaths ||
                        (next !in alreadySeenNodes && worklist.none { next in it }))
            ) {
                worklist.add(nextPath)
            }
        }
    }

    return FulfilledAndFailedPaths(fulfilledPaths, failedPaths)
}

/**
 * Returns an instance of [FulfilledAndFailedPaths] where [FulfilledAndFailedPaths.fulfilled]
 * contains all possible shortest evaluation paths between the starting node [this] and the end node
 * fulfilling [predicate]. The paths are represented as lists of nodes. Paths which do not end at
 * such a node are included in [FulfilledAndFailedPaths.failed].
 *
 * Hence, if "fulfilled" is a non-empty list, the execution of a statement fulfilling the predicate
 * is possible after executing [this] **possible but not mandatory**. If the list "failed" is empty,
 * such a statement is always executed.
 */
fun Node.followNextEOGEdgesUntilHit(predicate: (Node) -> Boolean): FulfilledAndFailedPaths {
    // Looks complicated but at least it's not recursive...
    // result: List of paths (between from and to)
    val fulfilledPaths = mutableListOf<List<Node>>()
    // failedPaths: All the paths which do not satisfy "predicate"
    val failedPaths = mutableListOf<List<Node>>()
    // The list of paths where we're not done yet.
    val worklist = mutableListOf<List<Node>>()
    worklist.add(listOf(this)) // We start only with the "from" node (=this)

    while (worklist.isNotEmpty()) {
        val currentPath = worklist.removeFirst()
        // The last node of the path is where we continue. We get all of its outgoing DFG edges and
        // follow them
        if (
            currentPath.last().nextEOGEdges.none { it.getProperty(Properties.UNREACHABLE) != true }
        ) {
            // No further nodes in the path and the path criteria are not satisfied.
            failedPaths.add(currentPath)
            continue // Don't add this path anymore. The requirement is satisfied.
        }

        for (next in
            currentPath
                .last()
                .nextEOGEdges
                .filter { it.getProperty(Properties.UNREACHABLE) != true }
                .map { it.end }) {
            // Copy the path for each outgoing DFG edge and add the next node
            val nextPath = mutableListOf<Node>()
            nextPath.addAll(currentPath)
            nextPath.add(next)
            if (predicate(next)) {
                // We ended up in the node "to", so we're done. Add the path to the results.
                fulfilledPaths.add(nextPath)
                continue // Don't add this path anymore. The requirement is satisfied.
            }
            // The next node is new in the current path (i.e., there's no loop), so we add the path
            // with the next step to the worklist.
            if (!currentPath.contains(next)) {
                worklist.add(nextPath)
            }
        }
    }

    return FulfilledAndFailedPaths(fulfilledPaths, failedPaths)
}

/**
 * Returns an instance of [FulfilledAndFailedPaths] where [FulfilledAndFailedPaths.fulfilled]
 * contains all possible shortest evaluation paths between the end node [this] and the start node
 * fulfilling [predicate]. The paths are represented as lists of nodes. Paths which do not end at
 * such a node are included in [FulfilledAndFailedPaths.failed].
 *
 * Hence, if "fulfilled" is a non-empty list, the execution of a statement fulfilling the predicate
 * is possible after executing [this] **possible but not mandatory**. If the list "failed" is empty,
 * such a statement is always executed.
 */
fun Node.followPrevEOGEdgesUntilHit(predicate: (Node) -> Boolean): FulfilledAndFailedPaths {
    // Looks complicated but at least it's not recursive...
    // result: List of paths (between from and to)
    val fulfilledPaths = mutableListOf<List<Node>>()
    // failedPaths: All the paths which do not satisfy "predicate"
    val failedPaths = mutableListOf<List<Node>>()
    // The list of paths where we're not done yet.
    val worklist = mutableListOf<List<Node>>()
    worklist.add(listOf(this)) // We start only with the "from" node (=this)

    while (worklist.isNotEmpty()) {
        val currentPath = worklist.removeFirst()
        // The last node of the path is where we continue. We get all of its outgoing DFG edges and
        // follow them
        if (
            currentPath.last().prevEOGEdges.none { it.getProperty(Properties.UNREACHABLE) != true }
        ) {
            // No further nodes in the path and the path criteria are not satisfied.
            failedPaths.add(currentPath)
            continue // Don't add this path anymore. The requirement is satisfied.
        }

        for (next in
            currentPath
                .last()
                .prevEOGEdges
                .filter { it.getProperty(Properties.UNREACHABLE) != true }
                .map { it.start }) {
            // Copy the path for each outgoing DFG edge and add the next node
            val nextPath = mutableListOf<Node>()
            nextPath.addAll(currentPath)
            nextPath.add(next)
            if (predicate(next)) {
                // We ended up in the node "to", so we're done. Add the path to the results.
                fulfilledPaths.add(nextPath)
                continue // Don't add this path anymore. The requirement is satisfied.
            }
            // The next node is new in the current path (i.e., there's no loop), so we add the path
            // with the next step to the worklist.
            if (!currentPath.contains(next)) {
                worklist.add(nextPath)
            }
        }
    }

    return FulfilledAndFailedPaths(fulfilledPaths, failedPaths)
}

/**
 * Returns a list of edges which are from the evaluation order between the starting node [this] and
 * an edge fulfilling [predicate]. If the return value is not `null`, a path from [this] to such an
 * edge is **possible but not mandatory**.
 *
 * It returns only a single possible path even if multiple paths are possible.
 */
fun Node.followNextEOG(predicate: (PropertyEdge<*>) -> Boolean): List<PropertyEdge<*>>? {
    val path = mutableListOf<PropertyEdge<*>>()

    for (edge in this.nextEOGEdges.filter { it.getProperty(Properties.UNREACHABLE) != true }) {
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
fun Node.followPrevEOG(predicate: (PropertyEdge<*>) -> Boolean): List<PropertyEdge<*>>? {
    val path = mutableListOf<PropertyEdge<*>>()

    for (edge in this.prevEOGEdges.filter { it.getProperty(Properties.UNREACHABLE) != true }) {
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
 * Returns a list of nodes which are a data flow path between the starting node [this] and the end
 * node fulfilling [predicate]. If the return value is not `null`, a data flow from [this] to such a
 * node is **possible but not mandatory**.
 *
 * It returns only a single possible path even if multiple paths are possible.
 */
fun Node.followPrevDFG(predicate: (Node) -> Boolean): MutableList<Node>? {
    val path = mutableListOf<Node>()

    for (prev in this.prevDFG) {
        path.add(prev)

        if (predicate(prev)) {
            return path
        }

        val subPath = prev.followPrevDFG(predicate)
        if (subPath != null) {
            path.addAll(subPath)
        }

        return path
    }

    return null
}

/** Returns all [CallExpression] children in this graph, starting with this [Node]. */
val Node?.calls: List<CallExpression>
    get() = this.allChildren()

/** Returns all [MemberCallExpression] children in this graph, starting with this [Node]. */
val Node?.mcalls: List<MemberCallExpression>
    get() = this.allChildren()

/** Returns all [MethodDeclaration] children in this graph, starting with this [Node]. */
val Node?.methods: List<MethodDeclaration>
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

/** Returns all [VariableDeclaration] children in this graph, starting with this [Node]. */
val Node?.variables: List<VariableDeclaration>
    get() = this.allChildren()

/** Returns all [Literal] children in this graph, starting with this [Node]. */
val Node?.literals: List<Literal<*>>
    get() = this.allChildren()

/** Returns all [Reference] children in this graph, starting with this [Node]. */
val Node?.refs: List<Reference>
    get() = this.allChildren()

/** Returns all [Assignment] child edges in this graph, starting with this [Node]. */
val Node?.assignments: List<Assignment>
    get() {
        return this?.allChildren<Node>()?.filterIsInstance<AssignmentHolder>()?.flatMap {
            it.assignments
        }
            ?: listOf()
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

operator fun <N : Expression> Expression.invoke(): N? {
    return this as? N
}

/** Returns all [CallExpression]s in this graph which call a method with the given [name]. */
fun TranslationResult.callsByName(name: String): List<CallExpression> {
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
