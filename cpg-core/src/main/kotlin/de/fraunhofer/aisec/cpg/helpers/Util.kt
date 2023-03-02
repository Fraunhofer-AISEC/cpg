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

import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.function.Function
import java.util.function.Predicate
import java.util.stream.IntStream
import java.util.stream.Stream
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
     * @param props
     * - All edges must have these properties set to the provided value
     *
     * @param refs
     * - Multiple reference nodes that can be passed as varargs
     *
     * @return true if all/any of the connections from node connect to n.
     */
    fun eogConnect(
        q: Quantifier = Quantifier.ALL,
        cn: Connect = Connect.SUBTREE,
        en: Edge,
        n: Node?,
        cr: Connect = Connect.SUBTREE,
        props: Map<Properties, Any?> = mutableMapOf(),
        refs: List<Node?>
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
                    if (Quantifier.ALL == q && pe.any { !it.containsProperties(props) })
                        return false
                    pe.filter { it.containsProperties(props) }.map { it.start }
                } else border.exits
            } else {
                nodeSide.flatMap {
                    if (en == Edge.ENTRIES) {
                        val pe = it.prevEOGEdges
                        if (Quantifier.ALL == q && pe.any { !it.containsProperties(props) })
                            return false
                        pe.filter { it.containsProperties(props) }.map { it.start }
                    } else listOf(it)
                }
            }
        refSide =
            if (cr == Connect.SUBTREE) {
                val borders = refs.map { SubgraphWalker.getEOGPathEdges(it) }

                borders.flatMap { border ->
                    if (Edge.ENTRIES == er) {
                        val pe = border.entries.flatMap { it.prevEOGEdges }
                        if (Quantifier.ALL == q && pe.any { !it.containsProperties(props) })
                            return false
                        pe.filter { it.containsProperties(props) }.map { it.start }
                    } else border.exits
                }
            } else {
                refSide.flatMap { node ->
                    if (er == Edge.ENTRIES) {
                        val pe = node?.prevEOGEdges ?: listOf()
                        if (Quantifier.ALL == q && pe.any { !it.containsProperties(props) })
                            return false
                        pe.filter { it.containsProperties(props) }.map { it.start }
                    } else listOf(node)
                }
            }
        val refNodes = refSide
        return if (Quantifier.ANY == q) nodeSide.any { it in refNodes }
        else refNodes.containsAll(nodeSide)
    }

    @Throws(IOException::class)
    fun inputStreamToString(inputStream: InputStream): String {
        ByteArrayOutputStream().use { result ->
            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } != -1) {
                result.write(buffer, 0, length)
            }
            return result.toString(StandardCharsets.UTF_8)
        }
    }

    @JvmStatic
    fun <T> distinctBy(by: Function<in T, *>): Predicate<T> {
        val seen = mutableSetOf<Any>()
        return Predicate { t: T -> seen.add(by.apply(t)) }
    }

    fun getExtension(file: File): String {
        val pos = file.name.lastIndexOf('.')
        return if (pos > 0) {
            file.name.substring(pos).lowercase(Locale.getDefault())
        } else {
            ""
        }
    }

    @JvmStatic
    fun <S> warnWithFileLocation(
        lang: LanguageFrontend,
        astNode: S,
        log: Logger,
        format: String?,
        vararg arguments: Any?
    ) {
        log.warn(
            String.format(
                "%s: %s",
                PhysicalLocation.locationLink(lang.getLocationFromRawNode(astNode)),
                format
            ),
            *arguments
        )
    }

    @JvmStatic
    fun <S> errorWithFileLocation(
        lang: LanguageFrontend,
        astNode: S,
        log: Logger,
        format: String?,
        vararg arguments: Any?
    ) {
        log.error(
            String.format(
                "%s: %s",
                PhysicalLocation.locationLink(lang.getLocationFromRawNode(astNode)),
                format
            ),
            *arguments
        )
    }

    @JvmStatic
    fun warnWithFileLocation(node: Node, log: Logger, format: String?, vararg arguments: Any?) {
        log.warn(
            String.format("%s: %s", PhysicalLocation.locationLink(node.location), format),
            *arguments
        )
    }

    @JvmStatic
    fun errorWithFileLocation(node: Node, log: Logger, format: String?, vararg arguments: Any?) {
        log.error(
            String.format("%s: %s", PhysicalLocation.locationLink(node.location), format),
            *arguments
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
            if (c == '(') {
                openParentheses++
                currPart.append(c)
            } else if (c == ')') {
                if (openParentheses > 0) {
                    openParentheses--
                }
                currPart.append(c)
            } else if (delimiters.contains("" + c)) {
                if (openParentheses == 0) {
                    val toAdd = currPart.toString().strip()
                    if (toAdd.isNotEmpty()) {
                        result.add(currPart.toString().strip())
                    }
                    currPart = StringBuilder()
                } else {
                    currPart.append(c)
                }
            } else {
                currPart.append(c)
            }
        }
        if (currPart.isNotEmpty()) {
            result.add(currPart.toString().strip())
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
        for (i in 0 until original.length) {
            val c = original[i]
            when (c) {
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
            if (c == '(') {
                openParentheses++
            } else if (c == ')') {
                openParentheses--
            } else if (c == '<') {
                openTemplate++
            } else if (c == '>') {
                openTemplate--
            } else if (c == marker && openParentheses == 0 && openTemplate == 0) {
                return true
            }
        }
        return false
    }

    /**
     * Establish dataflow from call arguments to the target [FunctionDeclaration] parameters
     *
     * @param target The call's target [FunctionDeclaration]
     * @param arguments The call's arguments to be connected to the target's parameters
     */
    fun attachCallParameters(target: FunctionDeclaration, arguments: List<Expression?>) {
        target.parameterEdges.sortWith(Comparator.comparing { it.end.argumentIndex })
        var j = 0
        while (j < arguments.size) {
            val parameters = target.parameters
            if (j < parameters.size) {
                val param = parameters[j]
                if (param.isVariadic) {
                    while (j < arguments.size) {

                        // map all the following arguments to this variadic param
                        param.addPrevDFG(arguments[j]!!)
                        j++
                    }
                    break
                } else {
                    param.addPrevDFG(arguments[j]!!)
                }
            }
            j++
        }
    }

    // TODO(oxisto): Remove at some point and directly use name class
    fun getSimpleName(language: Language<out LanguageFrontend>?, name: String): String {
        var name = name
        if (language != null) {
            val delimiter = language.namespaceDelimiter
            if (name.contains(delimiter)) {
                name = name.substring(name.lastIndexOf(delimiter) + delimiter.length)
            }
        }
        return name
    }

    // TODO(oxisto): Remove at some point and directly use name class
    fun getParentName(language: Language<out LanguageFrontend>?, name: String): String {
        var name = name
        if (language != null) {
            val delimiter = language.namespaceDelimiter
            if (delimiter in name) {
                name = name.substring(0, name.lastIndexOf(delimiter))
            }
        }
        return name
    }

    /**
     * Inverse operation of [.attachCallParameters]
     *
     * @param target
     * @param arguments
     */
    fun detachCallParameters(target: FunctionDeclaration, arguments: List<Expression?>) {
        for (param in target.parameters) {
            // A param could be variadic, so multiple arguments could be set as incoming DFG
            param.prevDFG.filter { o: Node? -> o in arguments }.forEach { param.removeNextDFG(it) }
        }
    }

    @JvmStatic
    fun <T> reverse(input: Stream<T>): Stream<T> {
        val temp = input.toArray()
        return IntStream.range(0, temp.size).mapToObj { i -> temp[temp.size - i - 1] } as Stream<T>
    }

    /**
     * This function returns the set of adjacent DFG nodes that is contained in the nodes subgraph.
     *
     * @param n Node of interest
     * @param incoming whether the node connected by an incoming or, if false, outgoing DFG edge
     * @return
     */
    fun getAdjacentDFGNodes(n: Node?, incoming: Boolean): MutableList<Node> {
        val subnodes = SubgraphWalker.getAstChildren(n)
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
     * @param branchingDecl
     */
    fun addDFGEdgesForMutuallyExclusiveBranchingExpression(
        n: Node,
        branchingExp: Node?,
        branchingDecl: Node?
    ) {
        var conditionNodes = mutableListOf<Node>()
        if (branchingExp != null) {
            conditionNodes = mutableListOf()
            conditionNodes.add(branchingExp)
        } else if (branchingDecl != null) {
            conditionNodes = getAdjacentDFGNodes(branchingDecl, true)
        }
        conditionNodes.forEach { n.addPrevDFG(it) }
    }

    enum class Connect {
        NODE,
        SUBTREE
    }

    enum class Quantifier {
        ANY,
        ALL
    }

    enum class Edge {
        ENTRIES,
        EXITS
    }
}
