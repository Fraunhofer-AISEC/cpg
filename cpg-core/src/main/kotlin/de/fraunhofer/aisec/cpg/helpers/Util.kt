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

import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration
import de.fraunhofer.aisec.cpg.graph.edges.flows.CallingContextIn
import de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemberCallExpression
import de.fraunhofer.aisec.cpg.helpers.Util.attachCallParameters
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.util.*
import org.slf4j.Logger

object Util {
    /**
     * Filters the nodes in the AST subtree at root `node` for Nodes with the specified code.
     *
     * @param node root of the subtree that is searched.
     * @param searchCode exact code that a node needs to have.
     * @return a list of nodes with the specified String.
     */
    fun subnodesOfCode(node: Node?, searchCode: String): List<Node> {
        return SubgraphWalker.flattenAST(node).filter { n: Node ->
            n.code != null && n.code == searchCode
        }
    }

    /**
     * Checks if the Node [startNode] connects to the nodes in [endNodes] over the CPGS EOG graph
     * edges that depict the evaluation order. The parameter [quantifier] defines if all edges of
     * interest to node must connect to an edge in [endNodes] or one is enough, [connectStart] and
     * [connectEnd] define whether the passed AST nodes themselves are used to search the
     * connections or the EOG Border nodes in the AST subnode. Finally, [edgeDirection] defines
     * whether the EOG edges go * from [startNode] to node in [endNodes] or the inverse.
     *
     * @param quantifier The quantifier, all or any node of [startNode] must connect to [endNodes],
     *   defaults to ALL.
     * @param connectStart NODE if [startNode] itself is the node to connect or SUBTREE if the EOG
     *   borders are of interest. Defaults to SUBTREE
     * @param edgeDirection The Edge direction and therefore the borders of [startNode] to connect
     *   to [endNodes]
     * @param startNode Node of interest
     * @param connectEnd NODE if [endNodes] nodes itself are the nodes to connect or SUBTREE if the
     *   EOG borders are of interest
     * @param predicate All edges must have the specified branch property
     * @param endNodes Multiple reference nodes that can be passed as varargs
     * @return true if all/any of the connections from node connect to the [startNode].
     */
    // TODO: this function needs a major overhaul because it was
    //  running on the false assumption of the old containsProperty
    //  return values
    fun eogConnect(
        quantifier: Quantifier = Quantifier.ALL,
        connectStart: Connect = Connect.SUBTREE,
        edgeDirection: Edge,
        startNode: Node?,
        connectEnd: Connect = Connect.SUBTREE,
        predicate: ((EvaluationOrder) -> Boolean)? = null,
        endNodes: List<Node?>,
    ): Boolean {
        if (startNode == null) {
            return false
        }

        var nodeSide = listOf(startNode)
        val er = if (edgeDirection == Edge.ENTRIES) Edge.EXITS else Edge.ENTRIES
        var refSide = endNodes
        nodeSide =
            if (connectStart == Connect.SUBTREE) {
                val border = SubgraphWalker.getEOGPathEdges(startNode)
                if (edgeDirection == Edge.ENTRIES) {
                    val pe = border.entries.flatMap { it.prevEOGEdges }
                    if (Quantifier.ALL == quantifier && pe.any { predicate?.invoke(it) == false })
                        return false
                    pe.filter { predicate?.invoke(it) != false }.map { it.start }
                } else border.exits
            } else {
                nodeSide.flatMap {
                    if (edgeDirection == Edge.ENTRIES) {
                        val pe = it.prevEOGEdges
                        if (
                            Quantifier.ALL == quantifier &&
                                pe.any { predicate?.invoke(it) == false }
                        )
                            return false
                        pe.filter { predicate?.invoke(it) != false }.map { it.start }
                    } else listOf(it)
                }
            }
        refSide =
            if (connectEnd == Connect.SUBTREE) {
                val borders = endNodes.map { SubgraphWalker.getEOGPathEdges(it) }

                borders.flatMap { border ->
                    if (Edge.ENTRIES == er) {
                        val pe = border.entries.flatMap { it.prevEOGEdges }
                        if (
                            Quantifier.ALL == quantifier &&
                                pe.any { predicate?.invoke(it) == false }
                        )
                            return false
                        pe.filter { predicate?.invoke(it) != false }.map { it.start }
                    } else border.exits
                }
            } else {
                refSide.flatMap { node ->
                    if (er == Edge.ENTRIES) {
                        val pe = node?.prevEOGEdges ?: listOf()
                        if (
                            Quantifier.ALL == quantifier &&
                                pe.any { predicate?.invoke(it) == false }
                        )
                            return false
                        pe.filter { predicate?.invoke(it) != false }.map { it.start }
                    } else listOf(node)
                }
            }
        val refNodes = refSide
        return if (Quantifier.ANY == quantifier) nodeSide.any { it in refNodes }
        else refNodes.containsAll(nodeSide)
    }

    /**
     * Logs a warning with the specified file location. This is intentionally inlined, so that the
     * [Logger] will use the location of the callee of this function, rather than the [Util] class.
     */
    @Suppress("NOTHING_TO_INLINE")
    inline fun <AstNode> warnWithFileLocation(
        lang: LanguageFrontend<AstNode, *>,
        astNode: AstNode,
        log: Logger,
        format: String?,
        vararg arguments: Any?,
    ) {
        log.warn(
            String.format(
                "%s: %s",
                PhysicalLocation.locationLink(lang.locationOf(astNode)),
                format,
            ),
            *arguments,
        )
    }

    /**
     * Logs an error with the specified file location. This is intentionally inlined, so that the
     * [Logger] will use the location of the callee of this function, rather than the [Util] class.
     */
    @Suppress("NOTHING_TO_INLINE")
    inline fun <AstNode> errorWithFileLocation(
        lang: LanguageFrontend<AstNode, *>,
        astNode: AstNode,
        log: Logger,
        format: String?,
        vararg arguments: Any?,
    ) {
        log.error(
            String.format(
                "%s: %s",
                PhysicalLocation.locationLink(lang.locationOf(astNode)),
                format,
            ),
            *arguments,
        )
    }

    /**
     * Logs a warning with the specified file location. This is intentionally inlined, so that the
     * [Logger] will use the location of the callee of this function, rather than the [Util] class.
     */
    @Suppress("NOTHING_TO_INLINE")
    inline fun warnWithFileLocation(
        node: Node,
        log: Logger,
        format: String?,
        vararg arguments: Any?,
    ) {
        log.warn(
            String.format("%s: %s", PhysicalLocation.locationLink(node.location), format),
            *arguments,
        )
    }

    /**
     * Logs a warning with the specified file location. This is intentionally inlined, so that the
     * [Logger] will use the location of the callee of this function, rather than the [Util] class.
     */
    @Suppress("NOTHING_TO_INLINE")
    inline fun infoWithFileLocation(
        node: Node,
        log: Logger,
        format: String?,
        vararg arguments: Any?,
    ) {
        log.info(
            String.format("%s: %s", PhysicalLocation.locationLink(node.location), format),
            *arguments,
        )
    }

    /**
     * Logs a warning with the specified file location. This is intentionally inlined, so that the
     * [Logger] will use the location of the callee of this function, rather than the [Util] class.
     */
    @Suppress("NOTHING_TO_INLINE")
    inline fun warnWithFileLocation(
        location: PhysicalLocation?,
        log: Logger,
        format: String?,
        vararg arguments: Any?,
    ) {
        log.warn(
            String.format("%s: %s", PhysicalLocation.locationLink(location), format),
            *arguments,
        )
    }

    /**
     * Logs an error with the specified file location. This is intentionally inlined, so that the
     * [Logger] will use the location of the callee of this function, rather than the [Util] class.
     */
    @Suppress("NOTHING_TO_INLINE")
    inline fun errorWithFileLocation(
        node: Node?,
        log: Logger,
        format: String?,
        vararg arguments: Any?,
    ) {
        log.error(
            String.format("%s: %s", PhysicalLocation.locationLink(node?.location), format),
            *arguments,
        )
    }

    /**
     * Logs a debug message with the specified file location. This is intentionally inlined, so that
     * the [Logger] will use the location of the callee of this function, rather than the [Util]
     * class.
     */
    @Suppress("NOTHING_TO_INLINE")
    inline fun debugWithFileLocation(
        node: Node?,
        log: Logger,
        format: String?,
        vararg arguments: Any?,
    ) {
        log.debug(
            String.format("%s: %s", PhysicalLocation.locationLink(node?.location), format),
            *arguments,
        )
    }

    /**
     * Split a String into multiple parts by using one or more delimiter characters. Any delimiters
     * that are surrounded by matching opening and closing brackets are skipped. E.g. "a,(b,c)" will
     * result in a list containing "a" and "(b,c)" when splitting on commas. Empty parts are
     * ignored, so when splitting "a,,,,(b,c)", the same result is returned as in the previous
     * example.
     *
     * @param toSplit The input String
     * @param delimiters A String containing all characters that should be treated as delimiters
     * @return A list of all parts of the input, as divided by any delimiter
     */
    fun splitLeavingParenthesisContents(toSplit: String, delimiters: String): List<String> {
        val result = mutableListOf<String>()
        var openParentheses = 0
        var currPart = StringBuilder()
        for (c in toSplit.toCharArray()) {
            when {
                c == '(' -> {
                    openParentheses++
                    currPart.append(c)
                }

                c == ')' -> {
                    if (openParentheses > 0) {
                        openParentheses--
                    }
                    currPart.append(c)
                }

                delimiters.contains("" + c) -> {
                    if (openParentheses == 0) {
                        val toAdd = currPart.toString().trim()
                        if (toAdd.isNotEmpty()) {
                            result.add(currPart.toString().trim())
                        }
                        currPart = StringBuilder()
                    } else {
                        currPart.append(c)
                    }
                }
            }
        }
        if (currPart.isNotEmpty()) {
            result.add(currPart.toString().trim())
        }
        return result
    }

    /**
     * Removes pairs of parentheses that do not provide any further separation. E.g. "(foo)" results
     * in "foo" and "(((foo))((bar)))" in "(foo)(bar)", whereas "(foo)(bar)" stays the same.
     *
     * @param original The String to clean
     * @return The modified version without excess parentheses
     */
    @JvmStatic
    fun removeRedundantParentheses(original: String): String {
        val result = original.toCharArray()
        val marker = '\uffff'
        val openingParentheses: Deque<Int> = ArrayDeque()
        for (i in original.indices) {
            when (original[i]) {
                '(' -> openingParentheses.push(i)
                ')' -> {
                    val matching = openingParentheses.pollFirst()
                    if (matching == 0 && i == original.length - 1) {
                        result[i] = marker
                        result[matching] = result[i]
                    } else if (
                        matching > 0 && result[matching - 1] == '(' && result[i + 1] == ')'
                    ) {
                        result[i] = marker
                        result[matching] = result[i]
                    }
                }
            }
        }
        return String(result).replace("" + marker, "")
    }

    fun containsOnOuterLevel(input: String, marker: Char): Boolean {
        var openParentheses = 0
        var openTemplate = 0
        for (c in input.toCharArray()) {
            when (c) {
                '(' -> {
                    openParentheses++
                }

                ')' -> {
                    openParentheses--
                }

                '<' -> {
                    openTemplate++
                }

                '>' -> {
                    openTemplate--
                }

                marker -> {
                    if (openParentheses == 0 && openTemplate == 0) {
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * Establishes data-flow from the arguments of a [CallExpression] to the parameters of a
     * [FunctionDeclaration] parameters. It handles positional arguments, named/default arguments,
     * and variadic parameters. Additionally, if the call is a [MemberCallExpression], it
     * establishes a data-flow from the [MemberCallExpression.base] towards the
     * [MethodDeclaration.receiver].
     *
     * @param target The call's target [FunctionDeclaration]
     * @param call The [CallExpression]
     */
    fun attachCallParameters(target: FunctionDeclaration, call: CallExpression) {
        // Add an incoming DFG edge from a member call's base to the method's receiver
        if (target is MethodDeclaration && call is MemberCallExpression && !call.isStatic) {
            target.receiver?.let { receiver ->
                call.base
                    ?.nextDFGEdges
                    ?.addContextSensitive(
                        receiver,
                        callingContext = CallingContextIn(mutableListOf(call)),
                    )
            }
        }

        val functionParameters = target.parameters
        val argumentEdges = call.argumentEdges
        var argumentIndex = 0

        for (param in functionParameters) {
            val argumentEdge = argumentEdges.getOrNull(argumentIndex)
            // Try to find a named argument matching this parameter
            val namedEdge = argumentEdges.firstOrNull { it.name == param.name.localName }
            if (namedEdge != null) {
                param.prevDFGEdges.addContextSensitive(
                    namedEdge.end,
                    callingContext = CallingContextIn(mutableListOf(call)),
                )
                argumentIndex++
                continue // Move to next parameter
            }

            // Handle variadic parameters
            if (param.isVariadic) {
                val remainingEdges = argumentEdges.drop(argumentIndex)
                if (remainingEdges.isNotEmpty()) {
                    // If it is the last parameter, it is a keyword required parameter (e.g.
                    // **kwargs in python);
                    val isKeywordVariadic = functionParameters.lastOrNull { it.isVariadic } == param
                    remainingEdges.forEach { edge ->
                        if (isKeywordVariadic) {
                            param.prevDFGEdges.addContextSensitive(
                                edge.end,
                                callingContext = CallingContextIn(mutableListOf(call)),
                            )
                            argumentIndex++
                        } else {
                            // otherwise it is a positional variadic parameter (e.g. *args in
                            // python) without keyword
                            if (edge.name == null) {
                                param.prevDFGEdges.addContextSensitive(
                                    edge.end,
                                    callingContext = CallingContextIn(mutableListOf(call)),
                                )
                                argumentIndex++
                            }
                        }
                    }
                }
                continue // Move to next parameter
            }

            // Handle only positional parameters, ignoring named arguments
            if (argumentEdge != null && argumentEdge.name == null) {
                param.prevDFGEdges.addContextSensitive(
                    argumentEdge.end,
                    callingContext = CallingContextIn(mutableListOf(call)),
                )
                argumentIndex++
                continue // Move to next parameter
            }

            // Handle default parameters when not explicitly provided
            val default = param.default
            if (default != null) {
                param.prevDFGEdges.addContextSensitive(
                    default,
                    callingContext = CallingContextIn(mutableListOf(call)),
                )
            }
        }
    }

    /**
     * Inverse operation of [attachCallParameters]
     *
     * @param target
     * @param arguments
     */
    fun detachCallParameters(target: FunctionDeclaration, arguments: List<Expression>) {
        for (param in target.parameters) {
            // A param could be variadic, so multiple arguments could be set as incoming DFG
            param.prevDFGEdges
                .filter { it.start in arguments }
                .forEach { param.nextDFGEdges.remove(it) }
        }
    }

    /**
     * This function returns the set of adjacent DFG nodes that is contained in the nodes subgraph.
     *
     * @param n Node of interest
     * @param incoming whether the node connected by an incoming or, if false, outgoing DFG edge
     * @return
     */
    fun getAdjacentDFGNodes(n: Node?, incoming: Boolean): MutableList<Node> {
        val subnodes = n?.astChildren ?: listOf()
        val adjacentNodes =
            if (incoming) {
                subnodes.filter { it.nextDFG.contains(n) }.toMutableList()
            } else {
                subnodes.filter { it.prevDFG.contains(n) }.toMutableList()
            }
        return adjacentNodes
    }

    /**
     * Connects the node `n` with the node `branchingExp` if present or with the node
     * `branchingDecl`. The assumption is that only `branchingExp` or `branchingDecl` are present,
     * e.g. C++.
     *
     * @param n
     * @param branchingExp
     * @param branchingDeclaration
     */
    fun addDFGEdgesForMutuallyExclusiveBranchingExpression(
        n: Node,
        branchingExp: Node?,
        branchingDeclaration: Node?,
    ) {
        var conditionNodes = mutableListOf<Node>()
        if (branchingExp != null) {
            conditionNodes = mutableListOf()
            conditionNodes.add(branchingExp)
        } else if (branchingDeclaration != null) {
            conditionNodes = getAdjacentDFGNodes(branchingDeclaration, true)
        }
        conditionNodes.forEach { n.prevDFGEdges += it }
    }

    enum class Connect {
        NODE,
        SUBTREE,
    }

    enum class Quantifier {
        ANY,
        ALL,
    }

    enum class Edge {
        ENTRIES,
        EXITS,
    }
}
