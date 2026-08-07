/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.persistence

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.frontends.TestLanguage
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.allChildrenWithOverlays
import de.fraunhofer.aisec.cpg.graph.edges.edges
import de.fraunhofer.aisec.cpg.graph.edges.flows.EvaluationOrder
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.test.GraphExamples
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Round-trip tests for [serializeToJson] / [deserializeFromJson]. They build a small,
 * self-contained graph via [GraphExamples], serialize it to JSON and deserialize it again, and then
 * check that the essential structure survives the trip.
 */
class JacksonSerializerTest {

    /**
     * A configuration without the default passes, so that the built graph stays a plain AST (no
     * EOG/DFG edges and, importantly, no pass-computed caches such as
     * [de.fraunhofer.aisec.cpg.graph.declarations.Function.functionSummary]). This keeps these
     * tests focused on the (de)serialization machinery itself.
     */
    private fun minimalConfig(): TranslationConfiguration =
        TranslationConfiguration.builder().registerLanguage<TestLanguage>().build()

    @Test
    fun testRoundTripPreservesNodes() {
        val original = GraphExamples.getShortcutClass(minimalConfig())

        val json = serializeToJson(original)
        assertTrue(json.isNotBlank(), "The serialized JSON should not be empty.")

        val restored = deserializeFromJson(json)

        // The whole graph, reachable from the restored translation result, must contain exactly the
        // same nodes (by id) as the original graph.
        val originalIds = original.allChildrenWithOverlays<Node>().map { it.id }.toSet()
        val restoredIds = restored.allChildrenWithOverlays<Node>().map { it.id }.toSet()

        assertEquals(
            originalIds,
            restoredIds,
            "The restored graph should contain exactly the same node ids as the original one.",
        )
    }

    @Test
    fun testRoundTripPreservesNames() {
        val original = GraphExamples.getShortcutClass(minimalConfig())

        val restored = deserializeFromJson(serializeToJson(original))

        val originalNames = original.allChildrenWithOverlays<Node>().map { it.name.toString() }
        val restoredNames = restored.allChildrenWithOverlays<Node>().map { it.name.toString() }

        assertEquals(
            originalNames.sorted(),
            restoredNames.sorted(),
            "The restored graph should preserve all node names.",
        )
    }

    /**
     * Counts, over all [EvaluationOrder] edges in the graph rooted at [root], how many carry each
     * `branch` value (`true`, `false`, or `null`). `branch` is a good witness for edge-property
     * round-tripping because it is set by the EOG pass (e.g. on the two out-edges of a conditional)
     * and cannot be re-derived by simply re-adding the target during relinking.
     *
     * We compare these aggregate counts rather than keying on endpoint ids: a [Node]'s id is a hash
     * of its (pass-computed) content, and the default passes do not reproduce byte-identical ids on
     * a freshly linked graph, so an id-keyed comparison would test id stability, not edge
     * properties.
     */
    private fun branchCounts(root: Node): Map<Boolean?, Int> =
        root
            .allChildrenWithOverlays<Node>()
            .flatMap { it.edges<EvaluationOrder>() }
            .groupingBy { it.branch }
            .eachCount()

    @Test
    fun testRoundTripPreservesEdgeProperties() {
        // Use the default passes so that the graph actually has EOG edges with `branch` set.
        val original = GraphExamples.getShortcutClass()

        val originalCounts = branchCounts(original)
        assertTrue(
            originalCounts.keys.any { it != null },
            "The example graph should contain at least one branch edge to make this test meaningful.",
        )

        val restored = deserializeFromJson(serializeToJson(original))

        assertEquals(
            originalCounts,
            branchCounts(restored),
            "The restored graph should preserve every EOG edge's `branch` property (true/false/unset).",
        )
    }

    /**
     * All scopes reachable from [root]: the scope of every node, plus every ancestor reached by
     * walking `parent` links upwards. This spans the whole restored scope object web.
     */
    private fun allScopes(root: Node): Set<Scope> =
        root
            .allChildrenWithOverlays<Node>()
            .mapNotNull { it.scope }
            .flatMap { generateSequence(it as Scope?) { s -> s.parent } }
            .toSet()

    @Test
    fun testRoundTripRestoresScopeObjectWeb() {
        val original = GraphExamples.getShortcutClass(minimalConfig())

        // Sanity check: the example graph really does attach scopes to its nodes.
        assertTrue(
            original.allChildrenWithOverlays<Node>().any { it.scope != null },
            "The example graph should have nodes with a scope to make this test meaningful.",
        )

        val restored = deserializeFromJson(serializeToJson(original))

        // The scope tree (node -> scope, and scope -> parent) must be reconstructed identically:
        // the
        // same set of scope names, and the same set of scopes reaching a global scope through their
        // parent chain, as in the original.
        fun scopeNames(root: Node) = allScopes(root).map { it.name.toString() }.sorted()

        fun scopesWithGlobal(root: Node) = allScopes(root).count { it.globalScope != null }

        assertEquals(
            scopeNames(original),
            scopeNames(restored),
            "The restored scope object web should preserve the original scope names.",
        )
        assertEquals(
            scopesWithGlobal(original),
            scopesWithGlobal(restored),
            "The restored scope tree should preserve the parent chains reaching a global scope.",
        )
    }
}
