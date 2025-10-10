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

package de.fraunhofer.aisec.cpg.helpers

import de.fraunhofer.aisec.cpg.ScopeManager
import de.fraunhofer.aisec.cpg.graph.ArgumentHolder
import de.fraunhofer.aisec.cpg.graph.AstNode
import de.fraunhofer.aisec.cpg.graph.ContextProvider
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.StatementHolder
import de.fraunhofer.aisec.cpg.graph.edges.ast.AstEdge
import de.fraunhofer.aisec.cpg.graph.edges.collections.EdgeCollection
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.HasType
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker.fieldCache
import de.fraunhofer.aisec.cpg.passes.EvaluationOrderGraphPass
import de.fraunhofer.aisec.cpg.passes.Pass
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy
import java.lang.annotation.AnnotationFormatError
import java.lang.reflect.Field
import java.util.*
import org.slf4j.LoggerFactory

/** Helper class for graph walking: Walking through ast-, cfg-, ...- edges */
object SubgraphWalker {
    private val LOGGER = LoggerFactory.getLogger(SubgraphWalker::class.java)
    private val fieldCache = HashMap<String, List<Field>>()

    /**
     * Returns all the fields for a specific class type. Because this information is static during
     * runtime, we do cache this information in [fieldCache] for performance reasons.
     *
     * @param classType the class type
     * @return its fields, including the ones from its superclass
     */
    fun getAllEdgeFields(classType: Class<*>): Collection<Field> {
        if (classType.superclass != null) {
            val cacheKey = classType.name

            // Note: we cannot use computeIfAbsent here, because we are calling our function
            // recursively and this would result in a ConcurrentModificationException
            if (fieldCache.containsKey(cacheKey)) {
                return fieldCache[cacheKey] ?: ArrayList()
            }
            val fields = ArrayList<Field>()
            fields.addAll(getAllEdgeFields(classType.superclass))
            fields.addAll(listOf(*classType.declaredFields).filter { it.name.contains("Edge") })

            // update the cache
            fieldCache[cacheKey] = fields
            return fields
        }
        return ArrayList()
    }

    /**
     * Retrieves a list of AST children of the specified node by iterating all edge fields that are
     * of type [AstEdge].
     *
     * Please note, that you SHOULD NOT call this directly in a recursive function, since the AST
     * might have loops and you will probably run into a [StackOverflowError]. Therefore, use of
     * [Node.accept] with the [Strategy.AST_FORWARD] is encouraged.
     *
     * @param node the start node
     * @param stopAtNode A predicate to stop the flattening at a specific node (i.e., we won't
     *   include that node and its ast children)
     * @return a list of children from the node's AST
     */
    @JvmStatic
    fun getAstChildren(
        node: AstNode?,
        stopAtNode: (AstNode) -> Boolean = { false },
    ): List<AstNode> {
        val children = ArrayList<AstNode>()
        if (node == null) return children
        val classType: Class<*> = node.javaClass

        // We currently need to stick to pure Java reflection, since Kotlin reflection
        // is EXTREMELY slow. See https://youtrack.jetbrains.com/issue/KT-32198
        for (field in getAllEdgeFields(classType)) {
            try {
                // We need to synchronize access to the field, because otherwise different
                // threads might restore the isAccessible property while this thread is still
                // accessing the field
                val obj =
                    synchronized(field) {
                        // disable access mechanisms
                        field.trySetAccessible()
                        val obj = field[node]

                        // restore old state
                        field.isAccessible = false
                        obj
                    } ?: continue

                when (obj) {
                    is EdgeCollection<*, *> -> {
                        obj.toNodeCollection({ it is AstEdge<*> }).filterIsInstanceTo(children)
                    }
                    else -> {
                        throw AnnotationFormatError(
                            "Found  on field of type " +
                                obj.javaClass +
                                " but can only used with edge classes or edge collections"
                        )
                    }
                }
            } catch (ex: IllegalAccessException) {
                LOGGER.error("Error while retrieving AST children: {}", ex.message)
            }
        }
        return children
    }

    /**
     * Flattens the tree, starting at Node n into a list.
     *
     * @param n the node which contains the ast children to flatten
     * @param stopAtNode A predicate to stop the flattening at a specific node (i.e., we won't
     *   include that node and its ast children)
     * @return the flattened nodes
     */
    fun flattenAST(n: AstNode?, stopAtNode: (Node) -> Boolean = { false }): List<AstNode> {
        if (n == null) {
            return ArrayList()
        }

        // We are using an identity set here, to avoid placing the *same* node in the identitySet
        // twice, possibly resulting in loops
        val identitySet = IdentitySet<AstNode>()
        flattenASTInternal(identitySet, n, stopAtNode)
        return identitySet.toSortedList()
    }

    private fun flattenASTInternal(
        identitySet: MutableSet<AstNode>,
        n: AstNode,
        stopAtNode: (AstNode) -> Boolean,
    ) {
        // Add the node itself and abort if its already there, to detect possible loops
        if (stopAtNode(n) || !identitySet.add(n)) {
            return
        }
        for (child in getAstChildren(n)) {
            flattenASTInternal(identitySet, child, stopAtNode)
        }
    }

    /**
     * Function returns two lists in a list. The first list contains all eog nodes with no
     * predecessor in the subgraph with root 'n'. The second list contains eog edges that have no
     * successor in the subgraph with root 'n'. The first List marks the entry and the second marks
     * the exit nodes of the cfg in this subgraph.
     *
     * @param n - root of the subgraph.
     * @return Two lists, list 1 contains all eog entries and list 2 contains all exits.
     */
    fun getEOGPathEdges(n: AstNode?): Border {
        val border = Border()
        val flattedASTTree = flattenAST(n)
        val eogNodes =
            flattedASTTree.filter { node -> node.prevEOG.isNotEmpty() || node.nextEOG.isNotEmpty() }
        // Nodes that are incoming edges, no other node
        border.entries =
            eogNodes
                .filter { node: Node -> node.prevEOG.any { prev -> prev !in eogNodes } }
                .toMutableList()
        border.exits =
            eogNodes
                .filter { node: Node -> node.nextEOG.any { next -> next !in eogNodes } }
                .toMutableList()
        return border
    }

    /**
     * For better readability: `result.entries` instead of `result.get(0)` when working with
     * getEOGPathEdges. Can be used for all subgraphs in subgraphs, e.g. AST entries and exits in a
     * EOG subgraph, EOG entries and exits in a CFG subgraph.
     */
    class Border {
        var entries = mutableListOf<AstNode>()
        var exits = mutableListOf<AstNode>()
    }

    class IterativeGraphWalker<NodeType : Node>(var strategy: (NodeType) -> Iterator<NodeType>) {

        /**
         * This callback is triggered whenever a new node is visited for the first time. This is the
         * place where usual graph manipulation will happen. The current node and its parent are
         * passed to the consumer.
         */
        private val onNodeVisit: MutableList<(node: NodeType, parent: NodeType?) -> Unit> =
            mutableListOf()

        private val replacements = mutableMapOf<NodeType, NodeType>()

        /**
         * The core iterative AST traversal algorithm: In a depth-first way we descend into the
         * tree, providing callbacks for graph modification.
         *
         * @param root The node where we should start
         */
        fun iterate(root: NodeType) {
            iterateAll(listOf(root))
        }

        /**
         * Iteration starting from several nodes that can explore a joint graph, therefore the
         * `seen` list is shared between several entries to the potentially joined graph. The search
         * works in BFS manner from every single entry, but stops at nodes visited by other entries.
         *
         * If you require a node to be visited multiple times, i.e. once for every entry it is
         * reachable by, use [iterate].
         */
        fun iterateAll(entries: List<NodeType>) {
            val todo = ArrayDeque<Pair<NodeType, NodeType?>>()
            val seen = identitySetOf<NodeType>()

            entries.forEach { entry ->
                if (entry !in seen) {
                    todo.push(Pair(entry, null))
                }

                while (todo.isNotEmpty()) {
                    var (current, parent) = todo.pop()
                    onNodeVisit.forEach { it(current, parent) }

                    // Check if we have a replacement node
                    val toReplace = replacements[current]
                    if (toReplace != null) {
                        current = toReplace
                        replacements.remove(toReplace)
                    }

                    val unseenChildren =
                        strategy(current).asSequence().filter { it !in seen }.toMutableList()

                    seen.addAll(unseenChildren)
                    unseenChildren.asReversed().forEach { child -> todo.push(Pair(child, current)) }
                }
            }
        }

        /**
         * Sometimes during walking the graph, we are replacing the current node. This causes
         * problems, that the walker still assumes the old node. Calling this function will ensure
         * that the walker knows about the new node.
         */
        fun registerReplacement(from: NodeType, to: NodeType) {
            replacements[from] = to
        }

        /** Registers a callback. */
        fun registerOnNodeVisit(callback: (node: NodeType, parent: NodeType?) -> Unit) {
            onNodeVisit.add(callback)
        }
    }

    /**
     * This class traverses the graph in a similar way as the [IterativeGraphWalker], but with the
     * added feature, that a [ScopeManager] is populated with the scope information of the current
     * node. This way, we can call functions on the supplied [scopeManager] and emulate that we are
     * currently in the scope of the "consumed" node in the callback. This can be useful for
     * resolving declarations or other scope-related tasks.
     */
    class ScopedWalker<out NodeType : Node> {
        val strategy: (@UnsafeVariance NodeType) -> Iterator<NodeType>
        private var walker: IterativeGraphWalker<NodeType>? = null
        private val scopeManager: ScopeManager

        constructor(scopeManager: ScopeManager, strategy: (NodeType) -> Iterator<NodeType>) {
            this.scopeManager = scopeManager
            this.strategy = strategy
        }

        /**
         * Callback function(s) containing two arguments: the previous node and the currently
         * visited node.
         *
         * The previous node depends on the [strategy], for example for [Strategy.AST_FORWARD], the
         * previous node is equal to [Node.astParent]. But for a strategy like
         * [Strategy.EOG_FORWARD], the previous node was the previous EOG node.
         */
        private val handlers = mutableListOf<(node: NodeType, previous: NodeType?) -> (Unit)>()

        fun clearCallbacks() {
            handlers.clear()
        }

        /**
         * Registers a handler that is called whenever a new node is visited. The handler is passed
         * the current node.
         */
        fun registerHandler(handler: (node: NodeType) -> (Unit)) {
            handlers.add { node, previous -> handler(node) }
        }

        /**
         * Registers a handler that is called whenever a new node is visited. The handler is passed
         * the current node and the previous node (if it exists).
         */
        fun registerHandler(handler: (node: NodeType, previous: NodeType?) -> (Unit)) {
            handlers.add(handler)
        }

        /** Informs the walker that a replacement of [from] with [to] was done. */
        fun registerReplacement(from: @UnsafeVariance NodeType, to: @UnsafeVariance NodeType) {
            walker?.registerReplacement(from, to)
        }

        /**
         * Wraps [IterativeGraphWalker] to handle declaration scopes.
         *
         * @param root The node where AST descent is started
         */
        fun iterate(root: @UnsafeVariance NodeType) {
            val walker = IterativeGraphWalker(strategy)
            walker.strategy = this.strategy
            handlers.forEach { h -> walker.registerOnNodeVisit { n, p -> handleNode(n, p, h) } }

            this.walker = walker

            walker.iterate(root)
        }

        /**
         * Wraps [IterativeGraphWalker] to handle declaration scopes, In contrast to [iterate], this
         * function is here to iterate over several nodes that may be entries into a joint graph
         * reachable by the specified strategy and therefore the internal seen list of nodes has to
         * be shared to avoid duplicate visits.
         *
         * If you require a node to be visited multiple times, i.e. once for every entry it is
         * reachable by, use [iterate].
         *
         * @param entries The nodes where the exploration is started from.
         */
        fun iterateAll(entries: List<@UnsafeVariance NodeType>) {
            val walker = IterativeGraphWalker(strategy)
            walker.strategy = this.strategy
            handlers.forEach { h -> walker.registerOnNodeVisit { n, p -> handleNode(n, p, h) } }

            this.walker = walker

            walker.iterateAll(entries)
        }

        private fun handleNode(
            current: NodeType,
            previous: NodeType?,
            handler: (node: NodeType, previous: NodeType?) -> (Unit),
        ) {
            // Jump to the node's scope, if it is different from ours.
            if (scopeManager.currentScope != current.scope) {
                scopeManager.jumpTo(current.scope)
            }

            handler(current, previous)
        }
    }
}

/**
 * Tries to replace the [old] expression with a [new] one, given the [parent].
 *
 * There are different things to consider:
 * - First, this only works if [parent] is either an [ArgumentHolder] or [StatementHolder].
 *   Otherwise, we cannot instruct the parent to exchange the node
 * - Second, since exchanging the node has influence on their edges (such as EOG, DFG, etc.), we
 *   only support a replacement very early in the pass system. To be specific, we only allow
 *   replacement BEFORE any DFG edges are set. We are re-wiring EOG edges, but nothing else. If one
 *   tries to replace a node with existing [Node.nextDFG] or [Node.prevDFG], we fail.
 * - We also migrate [HasType.typeObservers] from the [old] to the [new] node.
 * - Lastly, if the [new] node is a [CallExpression.callee] of a [CallExpression] parent, and the
 *   [old] and [new] expressions are of different types (e.g., exchanging a simple [Reference] for a
 *   [MemberExpression]), we also replace the [CallExpression] with a [MemberCallExpression].
 */
context(provider: ContextProvider)
fun SubgraphWalker.ScopedWalker<Node>.replace(
    parent: AstNode?,
    old: Expression,
    new: Expression,
): Boolean {
    // We do not allow to replace nodes where the DFG (or other dependent nodes, such as PDG have
    // been set). The reason for that is that these edges contain a lot of information on the edges
    // themselves and replacing this edge would be very complicated.
    if (old.prevDFG.isNotEmpty() || old.nextDFG.isNotEmpty()) {
        return false
    }

    val success =
        when (parent) {
            is CallExpression -> {
                if (parent.callee == old) {
                    // Now we are running into a problem. If the previous callee and the new callee
                    // are of different types (ref/vs. member expression). We also need to replace
                    // the whole call expression instead.
                    if (parent is MemberCallExpression && new is Reference) {
                        val newCall = parent.toCallExpression(new)
                        return replace(parent.astParent, parent, newCall)
                    } else if (new is MemberExpression) {
                        val newCall = parent.toMemberCallExpression(new)
                        return replace(parent.astParent, parent, newCall)
                    } else {
                        parent.callee = new
                        true
                    }
                } else run { parent.replace(old, new) }
            }
            is ArgumentHolder -> parent.replace(old, new)
            is StatementHolder -> parent.replace(old, new)
            else -> {
                Pass.log.error(
                    "Parent AST node is not an argument or statement holder. Cannot replace node. Further analysis might not be entirely accurate."
                )
                return false
            }
        }
    if (!success) {
        Pass.log.error(
            "Replacing expression $old was not successful. Further analysis might not be entirely accurate."
        )
    } else {
        // Store any eventual EOG nodes and disconnect old node
        val oldNextEOG = old.exitNextEOG().toSet()
        val oldPrevEOG = old.startingPrevEOG().toSet()
        val hasEOG =
            oldNextEOG.isNotEmpty() ||
                oldPrevEOG.isNotEmpty() ||
                old.prevEOG.isNotEmpty() ||
                old.nextEOG.isNotEmpty()
        old.disconnectFromGraph()

        if (hasEOG) {
            // We actively re-trigger the EOG pass to handle the new node but only if it has already
            // been run before. To figure this out, we check if there's some sort of EOG edges
            // somewhere. This is required because we
            // cannot set the currentPredecessors with the incremental building logic.
            val eogPass = EvaluationOrderGraphPass(provider.ctx)
            // Set the currentPredecessors to the old prevEOG nodes. This is necessary because the
            // EOGPass takes these nodes to connect the first node in the children to the EOG and
            // then
            // constructs the remaining EOG edges in the new node's AST children.
            eogPass.currentPredecessors.addAll(oldPrevEOG)
            eogPass.handleEOG(new)
            // For the old EOG predecessors, we need to set the new exit node of the EOG of this
            // subgraph. These are stored in the currentPredecessors of the EOGPass.
            oldNextEOG.forEach {
                // TODO: It may be necessary for some nodes to also add properties (e.g. branches)
                // to the EOG edges but we do not have access to this information here.
                it.prevEOG = eogPass.currentPredecessors
            }
            eogPass.cleanup()
        }

        // Also move over any type observers
        old.typeObservers.forEach {
            old.unregisterTypeObserver(it)
            new.registerTypeObserver(it)
        }

        old.astParent = null
        new.astParent = parent

        // Make sure to inform the walker about our change
        this.registerReplacement(old, new)
    }

    return success
}

/**
 * Copies the properties of this [CallExpression] to the given [call] and sets the `call.callee` to
 * [callee]. Note that the ast children are not duplicated. This means that their `astParent` will
 * now point to [call].
 */
private fun CallExpression.duplicateTo(call: CallExpression, callee: Reference) {
    call.language = this.language
    call.scope = this.scope
    call.argumentEdges.clear()
    // This is required to set the astParent of the arguments to the new edge.
    this.argumentEdges.forEach { existingEdge ->
        call.argumentEdges.add(existingEdge.end) { name = existingEdge.name }
    }
    call.type = this.type
    call.assignedTypes = this.assignedTypes
    call.code = this.code
    call.location = this.location
    call.argumentIndex = this.argumentIndex
    call.annotations = this.annotations
    call.comment = this.comment
    call.callee = callee
    callee.resolutionHelper = call
    call.isImplicit = this.isImplicit
    call.isInferred = this.isInferred
}

/**
 * Creates a new [CallExpression] with the same properties (e.g. ast childre, etc.) except from DFG
 * and EOG edges as [this]. It sets the [CallExpression.callee] to [callee].
 */
fun MemberCallExpression.toCallExpression(callee: Reference): CallExpression {
    val call = CallExpression()
    duplicateTo(call, callee)

    return call
}

/**
 * Creates a new [MemberCallExpression] with the same properties (e.g. ast children, etc.) except
 * from DFG and EOG edges as [this]. It sets the [MemberCallExpression.callee] to [callee].
 */
fun CallExpression.toMemberCallExpression(callee: MemberExpression): MemberCallExpression {
    val call = MemberCallExpression()
    duplicateTo(call, callee)

    return call
}

/**
 * Creates a new [ConstructExpression] with the same properties (e.g. ast children, etc.) except
 * from DFG and EOG edges as [this]. It sets the [ConstructExpression.callee] to [callee].
 */
fun CallExpression.toConstructExpression(callee: Reference): ConstructExpression {
    val construct = ConstructExpression()
    duplicateTo(construct, callee)

    return construct
}
