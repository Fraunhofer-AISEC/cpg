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
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration
import de.fraunhofer.aisec.cpg.graph.edges.flows.CallingContextIn
import de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
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
     * Checks if the Node `n` connects to the nodes in `refs` over the CPGS EOG graph edges that
     * depict the evaluation order. The parameter q defines if all edges of interest to node must
     * connect to an edge in refs or one is enough, cn and cr define whether the passed AST nodes
     * themselves are used to search the connections or the EOG Border nodes in the AST subnode.
     * Finally, en defines whether the EOG edges go * from n to r in refs or the inverse.
     *
     * @param q
     * - The quantifier, all or any node of n must connect to refs, defaults to ALL.
     *
     * @param cn
     * - NODE if n itself is the node to connect or SUBTREE if the EOG borders are of interest.
     *   Defaults to SUBTREE
     *
     * @param en
     * - The Edge direction and therefore the borders of n to connect to refs
     *
     * @param n
     * - Node of interest
     *
     * @param cr
     * - NODE if refs nodes itself are the nodes to connect or SUBTREE if the EOG borders are of
     *   interest
     *
     * @param branch
     * - All edges must have the specified branch property
     *
     * @param refs
     * - Multiple reference nodes that can be passed as varargs
     *
     * @return true if all/any of the connections from node connect to n.
     */
    // TODO: this function needs a major overhaul because it was
    //  running on the false assumption of the old containsProperty
    //  return values
    fun eogConnect(
        q: Quantifier = Quantifier.ALL,
        cn: Connect = Connect.SUBTREE,
        en: Edge,
        n: Node?,
        cr: Connect = Connect.SUBTREE,
        predicate: ((EvaluationOrder) -> Boolean)? = null,
        refs: List<Node?>,
    ): Boolean {
        if (n == null) {
            return false
        }

        var nodeSide = listOf(n)
        val er = if (en == Edge.ENTRIES) Edge.EXITS else Edge.ENTRIES
        var refSide = refs
        nodeSide =
            if (cn == Connect.SUBTREE) {
                val border = SubgraphWalker.getEOGPathEdges(n)
                if (en == Edge.ENTRIES) {
                    val pe = border.entries.flatMap { it.prevEOGEdges }
                    if (Quantifier.ALL == q && pe.any { predicate?.invoke(it) == false })
                        return false
                    pe.filter { predicate?.invoke(it) != false }.map { it.start }
                } else border.exits
            } else {
                nodeSide.flatMap {
                    if (en == Edge.ENTRIES) {
                        val pe = it.prevEOGEdges
                        if (Quantifier.ALL == q && pe.any { predicate?.invoke(it) == false })
                            return false
                        pe.filter { predicate?.invoke(it) != false }.map { it.start }
                    } else listOf(it)
                }
            }
        refSide =
            if (cr == Connect.SUBTREE) {
                val borders = refs.map { SubgraphWalker.getEOGPathEdges(it) }

                borders.flatMap { border ->
                    if (Edge.ENTRIES == er) {
                        val pe = border.entries.flatMap { it.prevEOGEdges }
                        if (Quantifier.ALL == q && pe.any { predicate?.invoke(it) == false })
                            return false
                        pe.filter { predicate?.invoke(it) != false }.map { it.start }
                    } else border.exits
                }
            } else {
                refSide.flatMap { node ->
                    if (er == Edge.ENTRIES) {
                        val pe = node?.prevEOGEdges ?: listOf()
                        if (Quantifier.ALL == q && pe.any { predicate?.invoke(it) == false })
                            return false
                        pe.filter { predicate?.invoke(it) != false }.map { it.start }
                    } else listOf(node)
                }
            }
        val refNodes = refSide
        return if (Quantifier.ANY == q) nodeSide.any { it in refNodes }
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
                    ?.addContextSensitive(receiver, callingContext = CallingContextIn(call))
            }
        }

        val callArguments = call.arguments
        val functionParameters = target.parameters

        var parameterIndex = 0

        for (param in functionParameters) {
            // Handle default parameters
            if (param.default != null) {
                val argumentEdge = call.argumentEdges.getOrNull(parameterIndex)
                if (argumentEdge != null) {
                    val isNamedArgument = argumentEdge.name != null
                    if (isNamedArgument) {
                        // If it's a named argument (part of e.g. **kwargs), we skip it since
                        // it is already handled in variadic handling
                        continue
                    }
                    // If the argument is provided, connect it to the parameter
                    param.prevDFGEdges.addContextSensitive(
                        argumentEdge.end,
                        callingContext = CallingContextIn(call),
                    )
                }
            }
            // If the parameter is variadic, map all remaining arguments to it
            if (param.isVariadic) {
                callArguments.drop(parameterIndex).forEach { arg ->
                    param.prevDFGEdges.addContextSensitive(
                        arg,
                        callingContext = CallingContextIn(call),
                    )
                }
                return
            }
            // Handle non-variadic, non-default parameters (regular positional arguments)
            if (param.default == null && parameterIndex < callArguments.size) {
                param.prevDFGEdges.addContextSensitive(
                    callArguments[parameterIndex],
                    callingContext = CallingContextIn(call),
                )
            }
            parameterIndex++
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
