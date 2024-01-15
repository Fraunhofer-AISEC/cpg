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
package de.fraunhofer.aisec.cpg.helpers

import de.fraunhofer.aisec.cpg.ScopeManager
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.graph.AST
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.Companion.checkForPropertyEdge
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.Companion.unwrap
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy
import java.lang.annotation.AnnotationFormatError
import java.lang.reflect.Field
import java.util.*
import java.util.function.BiConsumer
import java.util.function.Consumer
import org.neo4j.ogm.annotation.Relationship
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
    private fun getAllFields(classType: Class<*>): Collection<Field> {
        if (classType.superclass != null) {
            val cacheKey = classType.name

            // Note: we cannot use computeIfAbsent here, because we are calling our function
            // recursively and this would result in a ConcurrentModificationException
            if (fieldCache.containsKey(cacheKey)) {
                return fieldCache[cacheKey] ?: ArrayList()
            }
            val fields = ArrayList<Field>()
            fields.addAll(getAllFields(classType.superclass))
            fields.addAll(listOf(*classType.declaredFields))

            // update the cache
            fieldCache[cacheKey] = fields
            return fields
        }
        return ArrayList()
    }

    /**
     * Retrieves a list of AST children of the specified node by iterating all fields that are
     * annotated with the [AST] annotation.
     *
     * Please note, that you SHOULD NOT call this directly in a recursive function, since the AST
     * might have loops and you will probably run into a [StackOverflowError]. Therefore, use of
     * [Node.accept] with the [Strategy.AST_FORWARD] is encouraged.
     *
     * @param node the start node
     * @return a list of children from the node's AST
     */
    @JvmStatic
    fun getAstChildren(node: Node?): List<Node> {
        val children = ArrayList<Node>()
        if (node == null) return children
        val classType: Class<*> = node.javaClass

        /*for (member in node::class.members) {
            val subGraph = member.findAnnotation<SubGraph>()
            if (subGraph != null && listOf(*subGraph.value).contains("AST")) {
                val old = member.isAccessible

                member.isAccessible = true

                val obj = member.call(node)

                // skip, if null
                if (obj == null) {
                    continue
                }

                member.isAccessible = old

                var outgoing = true // default
                var relationship = member.findAnnotation<Relationship>()
                if (relationship != null) {
                    outgoing =
                        relationship.direction ==
                                Relationship.Direction.OUTGOING)
                }
                if (checkForPropertyEdge(field, obj)) {
                    obj = unwrap(obj as List<PropertyEdge<Node>>, outgoing)
                }
                when (obj) {
                    is Node -> {
                        children.add(obj)
                    }
                    is Collection<*> -> {
                        children.addAll(obj as Collection<Node>)
                    }
                    else -> {
                        throw AnnotationFormatError(
                            "Found @field:SubGraph(\"AST\") on field of type " +
                                    obj.javaClass +
                                    " but can only used with node graph classes or collections of graph nodes"
                        )
                    }
                }
            }
        }*/

        // We currently need to stick to pure Java reflection, since Kotlin reflection
        // is EXTREMELY slow. See https://youtrack.jetbrains.com/issue/KT-32198
        for (field in getAllFields(classType)) {
            field.getAnnotation(AST::class.java) ?: continue
            try {
                // We need to synchronize access to the field, because otherwise different
                // threads might restore the isAccessible property while this thread is still
                // accessing the field
                var obj =
                    synchronized(field) {
                        // disable access mechanisms
                        field.trySetAccessible()
                        val obj = field[node]

                        // restore old state
                        field.isAccessible = false
                        obj
                    }
                        ?: continue

                // skip, if null
                var outgoing = true // default
                if (field.getAnnotation(Relationship::class.java) != null) {
                    outgoing =
                        (field.getAnnotation(Relationship::class.java).direction ==
                            Relationship.Direction.OUTGOING)
                }
                if (checkForPropertyEdge(field, obj) && obj is Collection<*>) {
                    obj = unwrap(obj.filterIsInstance<PropertyEdge<Node>>(), outgoing)
                }
                when (obj) {
                    is Node -> {
                        children.add(obj)
                    }
                    is Collection<*> -> {
                        children.addAll(obj.filterIsInstance<Node>())
                    }
                    else -> {
                        throw AnnotationFormatError(
                            "Found @field:SubGraph(\"AST\") on field of type " +
                                obj.javaClass +
                                " but can only used with node graph classes or collections of graph nodes"
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
     * @return the flattened nodes
     */
    fun flattenAST(n: Node?): List<Node> {
        if (n == null) {
            return ArrayList()
        }

        // We are using an identity set here, to avoid placing the *same* node in the identitySet
        // twice,
        // possibly resulting in loops
        val identitySet = IdentitySet<Node>()
        flattenASTInternal(identitySet, n)
        return identitySet.toSortedList()
    }

    private fun flattenASTInternal(identitySet: MutableSet<Node>, n: Node) {
        // Add the node itself and abort if its already there, to detect possible loops
        if (!identitySet.add(n)) {
            return
        }
        for (child in getAstChildren(n)) {
            flattenASTInternal(identitySet, child)
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
    fun getEOGPathEdges(n: Node?): Border {
        val border = Border()
        val flattedASTTree = flattenAST(n)
        val eogNodes =
            flattedASTTree.filter { node: Node ->
                node.prevEOG.isNotEmpty() || node.nextEOG.isNotEmpty()
            }
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
        var entries = mutableListOf<Node>()
        var exits = mutableListOf<Node>()
    }

    class IterativeGraphWalker {
        private var todo: Deque<Pair<Node, Node?>>? = null
        var backlog: Deque<Node>? = null
            private set

        var strategy: (Node) -> Iterator<Node> = Strategy::AST_FORWARD

        /**
         * This callback is triggered whenever a new node is visited for the first time. This is the
         * place where usual graph manipulation will happen. The current node is the single argument
         * passed to the function
         */
        private val onNodeVisit: MutableList<Consumer<Node>> = mutableListOf()
        private val onNodeVisit2: MutableList<BiConsumer<Node, Node?>> = mutableListOf()

        private val replacements = mutableMapOf<Node, Node>()

        /**
         * The callback that is designed to tell the user when we leave the current scope. The
         * exited node is passed as an argument to the callback function. Consider the following
         * AST:
         *
         * .........(1) parent
         *
         * ........./........\
         *
         * (2) child1....(4) child2
         *
         * ........|
         *
         * (3) subchild
         *
         * Once "parent" has been visited, we continue descending into its children. First into
         * "child1", followed by "subchild". Once we are done there, we return to "child1". At this
         * point, the exit handler notifies the user that "subchild" is being exited. Afterwards we
         * exit "child1", and after "child2" is done, "parent" is exited.
         */
        private val onNodeExit: MutableList<Consumer<Node>> = ArrayList()

        /**
         * The core iterative AST traversal algorithm: In a depth-first way we descend into the
         * tree, providing callbacks for graph modification.
         *
         * @param root The node where we should start
         */
        fun iterate(root: Node) {
            todo = ArrayDeque()
            backlog = ArrayDeque()
            val seen: MutableSet<Node> = LinkedHashSet()
            todo?.push(Pair<Node, Node?>(root, null))
            while ((todo as ArrayDeque<Pair<Node, Node?>>).isNotEmpty()) {
                var (current, parent) = (todo as ArrayDeque<Pair<Node, Node?>>).pop()
                if (
                    (backlog as ArrayDeque<Node>).isNotEmpty() &&
                        (backlog as ArrayDeque<Node>).peek() == current
                ) {
                    val exiting = (backlog as ArrayDeque<Node>).pop()
                    onNodeExit.forEach(Consumer { c: Consumer<Node> -> c.accept(exiting) })
                } else {
                    onNodeVisit.forEach(Consumer { c: Consumer<Node> -> c.accept(current) })
                    onNodeVisit2.forEach(
                        Consumer { c: BiConsumer<Node, Node?> -> c.accept(current, parent) }
                    )

                    // Check if we have a replacement node
                    val toReplace = replacements[current]
                    if (toReplace != null) {
                        current = toReplace
                        replacements.remove(toReplace)
                    }

                    val unseenChildren =
                        strategy(current).asSequence().filter { it !in seen }.toMutableList()

                    // re-place the current node as a marker for the above check to find out when we
                    // need to exit a scope
                    (todo as ArrayDeque<Pair<Node, Node?>>).push(Pair(current, parent))

                    seen.addAll(unseenChildren)
                    unseenChildren.asReversed().forEach { child: Node ->
                        (todo as ArrayDeque<Pair<Node, Node?>>).push(Pair(child, current))
                    }
                    (backlog as ArrayDeque<Node>).push(current)
                }
            }
        }

        /**
         * Sometimes during walking the graph, we are replacing the current node. This causes
         * problems, that the walker still assumes the old node. Calling this function will ensure
         * that the walker knows about the new node.
         */
        fun registerReplacement(from: Node, to: Node) {
            replacements[from] = to
        }

        fun registerOnNodeVisit(callback: Consumer<Node>) {
            onNodeVisit.add(callback)
        }

        fun registerOnNodeVisit2(callback: BiConsumer<Node, Node?>) {
            onNodeVisit2.add(callback)
        }

        fun registerOnNodeExit(callback: Consumer<Node>) {
            onNodeExit.add(callback)
        }

        fun clearCallbacks() {
            onNodeVisit.clear()
            onNodeExit.clear()
        }

        fun getTodo(): Deque<Node> {
            return ArrayDeque(todo?.map { it.first })
        }
    }

    /**
     * This class traverses the graph in a similar way as the [IterativeGraphWalker], but with the
     * added feature, that a [ScopeManager] is populated with the scope information of the current
     * node. This way, we can call functions on the supplied [scopeManager] and emulate that we are
     * currently in the scope of the "consumed" node in the callback. This can be useful for
     * resolving declarations or other scope-related tasks.
     */
    class ScopedWalker {
        lateinit var strategy: (Node) -> Iterator<Node>
        private var walker: IterativeGraphWalker? = null
        private val scopeManager: ScopeManager

        constructor(lang: LanguageFrontend<*, *>) {
            scopeManager = lang.scopeManager
        }

        constructor(
            scopeManager: ScopeManager,
            strategy: (Node) -> Iterator<Node> = Strategy::AST_FORWARD
        ) {
            this.scopeManager = scopeManager
            this.strategy = strategy
        }

        /**
         * Callback function(s) getting three arguments: the type of the class we're currently in,
         * the root node of the current declaration scope, the currently visited node.
         */
        private val handlers = mutableListOf<TriConsumer<RecordDeclaration?, Node?, Node?>>()

        fun clearCallbacks() {
            handlers.clear()
        }

        fun registerHandler(handler: TriConsumer<RecordDeclaration?, Node?, Node?>) {
            handlers.add(handler)
        }

        fun registerHandler(handler: BiConsumer<Node?, RecordDeclaration?>) {
            handlers.add(
                TriConsumer { currClass: RecordDeclaration?, _: Node?, currNode: Node? ->
                    handler.accept(currNode, currClass)
                }
            )
        }

        fun registerHandler(handler: Consumer<Node?>) {
            handlers.add(
                TriConsumer { _: RecordDeclaration?, _: Node?, currNode: Node? ->
                    handler.accept(currNode)
                }
            )
        }

        fun registerReplacement(from: Node, to: Node) {
            walker?.registerReplacement(from, to)
        }

        /**
         * Wraps [IterativeGraphWalker] to handle declaration scopes.
         *
         * @param root The node where AST descent is started
         */
        fun iterate(root: Node) {
            val walker = IterativeGraphWalker()
            walker.strategy = this.strategy
            handlers.forEach { h -> walker.registerOnNodeVisit { n -> handleNode(n, h) } }

            this.walker = walker

            walker.iterate(root)
        }

        private fun handleNode(
            current: Node,
            handler: TriConsumer<RecordDeclaration?, Node?, Node?>
        ) {
            // Jump to the node's scope, if it is different from ours.
            if (scopeManager.currentScope != current.scope) {
                scopeManager.jumpTo(current.scope)
            }

            val parent = walker?.backlog?.peek()

            handler.accept(scopeManager.currentRecord, parent, current)
        }
    }
}
