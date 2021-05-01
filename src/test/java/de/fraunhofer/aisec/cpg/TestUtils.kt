/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
package de.fraunhofer.aisec.cpg

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.helpers.Util
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.function.Consumer
import java.util.function.Predicate
import java.util.stream.Collectors
import org.apache.commons.lang3.reflect.FieldUtils
import org.junit.jupiter.api.Assertions
import org.mockito.Mockito

object TestUtils {
    @JvmStatic
    fun <S : Node?> findByUniquePredicate(nodes: Collection<S>, predicate: Predicate<S>?): S {
        val results = findByPredicate(nodes, predicate)
        Assertions.assertEquals(1, results.size, "Expected exactly one node matching the predicate")
        return results[0]
    }

    @JvmStatic
    fun <S : Node?> findByPredicate(nodes: Collection<S>, predicate: Predicate<S>?): List<S> {
        return nodes.stream().filter(predicate).collect(Collectors.toList())
    }

    @JvmStatic
    fun <S : Node?> findByUniqueName(nodes: Collection<S>, name: String): S {
        return findByUniquePredicate(nodes) { m: S -> m!!.name == name }
    }

    @JvmStatic
    fun <S : Node?> findByName(nodes: Collection<S>, name: String): List<S> {
        return findByPredicate(nodes) { m: S -> m!!.name == name }
    }

    /**
     * Like [TestUtils.analyze], but for all files in a directory tree having a specific file
     * extension
     *
     * @param fileExtension All files found in the directory must end on this String. An empty
     * string matches all files
     * @param topLevel The directory to traverse while looking for files to parse
     * @param usePasses Whether the analysis should run passes after the initial phase
     * @param configModifier An optional modifier for the config
     *
     * @return A list of [TranslationUnitDeclaration] nodes, representing the CPG roots
     * @throws Exception Any exception thrown during the parsing process
     */
    @JvmOverloads
    @JvmStatic
    @Throws(Exception::class)
    fun analyze(
        fileExtension: String?,
        topLevel: Path,
        usePasses: Boolean,
        configModifier: Consumer<TranslationConfiguration.Builder>? = null
    ): List<TranslationUnitDeclaration> {
        val files =
            Files.walk(topLevel, Int.MAX_VALUE)
                .map { obj: Path -> obj.toFile() }
                .filter { obj: File -> obj.isFile }
                .filter { f: File -> f.name.endsWith(fileExtension!!) }
                .sorted()
                .collect(Collectors.toList())
        return analyze(files, topLevel, usePasses, configModifier)
    }

    /**
     * Default way of parsing a list of files into a full CPG. All default passes are applied
     *
     * @param topLevel The directory to traverse while looking for files to parse
     * @param usePasses Whether the analysis should run passes after the initial phase
     * @param configModifier An optional modifier for the config
     *
     * @return A list of [TranslationUnitDeclaration] nodes, representing the CPG roots
     * @throws Exception Any exception thrown during the parsing process
     */
    @JvmOverloads
    @JvmStatic
    @Throws(Exception::class)
    fun analyze(
        files: List<File>?,
        topLevel: Path,
        usePasses: Boolean,
        configModifier: Consumer<TranslationConfiguration.Builder>? = null
    ): List<TranslationUnitDeclaration> {
        val builder =
            TranslationConfiguration.builder()
                .sourceLocations(files)
                .topLevel(topLevel.toFile())
                .loadIncludes(true)
                .disableCleanup()
                .debugParser(true)
                .failOnError(true)
                .defaultLanguages()
        if (usePasses) {
            builder.defaultPasses()
        }
        configModifier?.accept(builder)
        val config = builder.build()
        val analyzer = TranslationManager.builder().config(config).build()
        return analyzer.analyze().get().translationUnits
    }

    /**
     * Default way of parsing a list of files into a full CPG. All default passes are applied
     *
     * @param builder A [TranslationConfiguration.Builder] which contains the configuration
     * @return A list of [TranslationUnitDeclaration] nodes, representing the CPG roots
     * @throws Exception Any exception thrown during the parsing process
     */
    @JvmStatic
    @Throws(Exception::class)
    fun analyzeWithBuilder(
        builder: TranslationConfiguration.Builder
    ): List<TranslationUnitDeclaration> {
        val config = builder.build()
        val analyzer = TranslationManager.builder().config(config).build()
        return analyzer.analyze().get().translationUnits
    }

    @JvmOverloads
    @JvmStatic
    @Throws(Exception::class)
    fun analyzeAndGetFirstTU(
        files: List<File>?,
        topLevel: Path,
        usePasses: Boolean,
        configModifier: Consumer<TranslationConfiguration.Builder>? = null
    ): TranslationUnitDeclaration {
        val translationUnits = analyze(files, topLevel, usePasses, configModifier)
        return translationUnits.stream().findFirst().orElseThrow()
    }

    @JvmStatic
    @Throws(IllegalAccessException::class)
    fun disableTypeManagerCleanup() {
        val spy = Mockito.spy(TypeManager.getInstance())
        Mockito.doNothing().`when`(spy).cleanup()
        FieldUtils.writeStaticField(TypeManager::class.java, "INSTANCE", spy, true)
    }

    /**
     * Returns the first element of the specified Class-type `specificClass` that has the name
     * `name` in the list `listOfNodes`.
     *
     * @param <S> Some class that extends [Node]. </S>
     */
    @JvmStatic
    fun <S : Node?> getOfTypeWithName(
        listOfNodes: List<Node?>?,
        specificClass: Class<S>?,
        name: String
    ): S? {
        val listOfNodesWithName =
            Util.filterCast(listOfNodes, specificClass)
                .stream()
                .filter { s: S -> s!!.name == name }
                .collect(Collectors.toList())
        return if (listOfNodesWithName.isEmpty()) {
            null
        } else listOfNodesWithName[0]
        // Here we return the first node, if there are more nodes
    }

    /**
     * Returns the first element of the specified Class-type {code specifiedClass} that has the name
     * `name` in the list of nodes that are subnodes of the AST-root node `root`.
     *
     * @param <S> Some class that extends [Node]. </S>
     */
    @JvmStatic
    fun <S : Node?> getSubnodeOfTypeWithName(
        root: Node?,
        specificClass: Class<S>?,
        name: String
    ): S? {
        return getOfTypeWithName(SubgraphWalker.flattenAST(root), specificClass, name)
    }

    /**
     * Given a root node in the AST graph, all AST children of the node are filtered for a specific
     * CPG Node type and returned.
     *
     * @param node root of the searched AST subtree
     * @param specificClass Class type to be searched for
     * @param <S> Type variable that allows returning a list of specific type
     * @return a List of searched types </S>
     */
    @JvmStatic
    fun <S : Node?, T : Node> subnodesOfType(node: T, specificClass: Class<S>): List<S> {
        return Util.filterCast(SubgraphWalker.flattenAST(node), specificClass)
            .stream()
            .filter(Util.distinctByIdentity())
            .collect(Collectors.toList())
    }

    /**
     * Similar to [TestUtils.subnodesOfType] but used when working with a list of nodes that is
     * already flat. No walking to get childnodes necessary.
     */
    @JvmStatic
    fun <S : Node?> subnodesOfType(roots: Collection<Node>, specificClass: Class<S>): List<S> {
        return roots
            .stream()
            .map { subnodesOfType(it, specificClass) }
            .flatMap { it.stream() }
            .filter(Util.distinctByIdentity())
            .collect(Collectors.toList())
    }

    /**
     * Compare the given parameter `toCompare` to the start- or end-line of the given node. If the
     * node has no location `false` is returned. `startLine` is used to specify if the start-line or
     * end-line of a location is supposed to be used.
     *
     * @param n
     * @param startLine
     * @param toCompare
     * @return
     */
    @JvmStatic
    fun compareLineFromLocationIfExists(n: Node, startLine: Boolean, toCompare: Int): Boolean {
        val loc = n.location ?: return false
        return if (startLine) {
            loc.region.startLine == toCompare
        } else {
            loc.region.endLine == toCompare
        }
    }

    @JvmStatic
    fun assertGraphsAreEqual(
        original: List<TranslationUnitDeclaration>,
        other: List<TranslationUnitDeclaration>
    ) {

        val originalNodes = getAllNodes(original)
        val originalEdges = originalNodes.flatMap { it.getOutgoingEdges() }.toMutableSet()

        val otherNodes = getAllNodes(other)
        val otherEdges = otherNodes.flatMap { it.getOutgoingEdges() }.toMutableSet()

        val originalToOtherNode = getMapping(originalNodes, otherNodes)
        var missing = originalNodes.filter { it !in originalToOtherNode }
        assert(missing.isEmpty()) {
            "There are nodes in the original graph that have no analog in the other graph: $missing"
        }
        val otherToOriginalNode = getMapping(otherNodes, originalNodes)
        missing = otherNodes.filter { it !in otherToOriginalNode }
        assert(missing.isEmpty()) {
            "There are nodes in the other graph that have no analog in the original graph: $missing"
        }

        for (originalEdge in originalEdges) {
            assert(
                otherEdges.any {
                    it.label == originalEdge.label &&
                        it.properties == originalEdge.properties &&
                        originalToOtherNode[originalEdge.from]?.contains(it.from) ?: false &&
                        originalToOtherNode[originalEdge.to]?.contains(it.to) ?: false
                }
            ) { "Edge in original graph $originalEdge not present in other graph!" }
        }

        for (otherEdge in otherEdges) {
            assert(
                originalEdges.any {
                    it.label == otherEdge.label &&
                        it.properties == otherEdge.properties &&
                        otherToOriginalNode[otherEdge.from]?.contains(it.from) ?: false &&
                        otherToOriginalNode[otherEdge.to]?.contains(it.to) ?: false
                }
            ) { "Edge in other graph $otherEdge not present in original graph!" }
        }
    }

    private fun getMapping(
        originNodes: Set<Node>,
        targetNodes: Set<Node>
    ): MutableMap<Node, MutableSet<Node>> {
        val mapping = IdentityHashMap<Node, MutableSet<Node>>().toMutableMap()
        for (originalNode in originNodes) {
            val matches =
                targetNodes.filter {
                    it.javaClass == originalNode.javaClass &&
                        it.allProperties == originalNode.allProperties
                }
            val set: MutableSet<Node> = Collections.newSetFromMap(IdentityHashMap())
            set.addAll(matches)
            mapping[originalNode] = set
        }
        return mapping
    }

    @JvmStatic
    fun getAllNodes(translationUnits: Collection<TranslationUnitDeclaration>): Set<Node> {
        val nodes: MutableSet<Node> = Collections.newSetFromMap(IdentityHashMap())
        val workset: MutableSet<Node> = Collections.newSetFromMap(IdentityHashMap())
        nodes.addAll(translationUnits)
        workset.addAll(translationUnits)

        while (workset.isNotEmpty()) {
            val curr = workset.first().also { workset.remove(it) }
            val neighbors = curr.outgoingEdges.map { it.to }.filter { it !in nodes }
            nodes.addAll(neighbors)
            workset.addAll(neighbors)
        }

        return nodes
    }
}
