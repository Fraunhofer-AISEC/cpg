/*
 * Copyright (c) 2020, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.declarations

import de.fraunhofer.aisec.cpg.graph.AstNode
import de.fraunhofer.aisec.cpg.graph.HasMemoryAddress
import de.fraunhofer.aisec.cpg.graph.HasMemoryValue
import de.fraunhofer.aisec.cpg.graph.HasModifiers
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.edges.MemoryAddressEdges
import de.fraunhofer.aisec.cpg.graph.edges.flows.Dataflows
import de.fraunhofer.aisec.cpg.graph.edges.memoryAddressEdgesOf
import de.fraunhofer.aisec.cpg.graph.expressions.MemoryAddress
import de.fraunhofer.aisec.cpg.graph.scopes.RecordScope
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.graph.scopes.Symbol
import de.fraunhofer.aisec.cpg.persistence.DoNotPersist
import de.fraunhofer.aisec.cpg.persistence.Relationship

/**
 * Represents a single declaration or definition, i.e. of a variable ([Variable]) or function
 * ([Function]).
 *
 * Note: We do NOT (currently) distinguish between the definition and the declaration of a function.
 * This means, that if a function is first declared and later defined with a function body, we will
 * currently have two [Function] nodes. This is very similar to the behaviour of clang, however
 * clang does establish a connection between those nodes, we currently do not.
 */
abstract class Declaration : AstNode(), HasModifiers, HasMemoryAddress, HasMemoryValue {
    @DoNotPersist
    val symbol: Symbol
        get() {
            return this.name.localName
        }

    /** Lazy backing field for [memoryAddressEdges]. */
    private var _memoryAddressEdges: MemoryAddressEdges? = null

    /**
     * Each Declaration allocates new memory, AKA a new address, so we create a new MemoryAddress
     * node. Should only be a single address!
     *
     * This and the other two memory-model containers ([memoryValueUsageEdges], [memoryValueEdges])
     * are only populated by the DFG and points-to passes and stay empty on most declarations. Their
     * backing containers are therefore allocated lazily on first access. They are not part of
     * [equals]/[hashCode], so lazy-on-access is safe.
     */
    @Relationship
    override var memoryAddressEdges: MemoryAddressEdges
        get() =
            _memoryAddressEdges
                ?: memoryAddressEdgesOf(
                        mirrorProperty = MemoryAddress::usageEdges,
                        outgoing = true,
                        onAdd = { toAdd ->
                            if (this.memoryAddressEdges.size > 1) {
                                log.error(
                                    "A declaration should have only a single address but we have ${this.memoryAddressEdges.size}."
                                )
                            }
                        },
                    )
                    .also { _memoryAddressEdges = it }
        set(value) {
            _memoryAddressEdges = value
        }

    /** Virtual property for accessing [memoryAddressEdges] as plain nodes. */
    @DoNotPersist
    override var memoryAddresses: MutableSet<MemoryAddress>
        get() = memoryAddressEdges.unwrap()
        set(value) {
            memoryAddressEdges.resetTo(value)
        }

    /** Lazy backing field for [memoryValueUsageEdges]. */
    private var _memoryValueUsageEdges: Dataflows<Node>? = null

    /**
     * Where the memory value of this declaration is used (allocated lazily, see
     * [memoryAddressEdges]).
     */
    @Relationship
    override var memoryValueUsageEdges: Dataflows<Node>
        get() =
            _memoryValueUsageEdges
                ?: Dataflows<Node>(
                        this,
                        mirrorProperty = HasMemoryValue::memoryValueEdges,
                        outgoing = true,
                    )
                    .also { _memoryValueUsageEdges = it }
        set(value) {
            _memoryValueUsageEdges = value
        }

    /** Virtual property for accessing [memoryValueUsageEdges] as plain nodes. */
    @DoNotPersist
    override var memoryValueUsages: MutableSet<Node>
        get() = memoryValueUsageEdges.unwrap()
        set(value) {
            memoryValueUsageEdges.resetTo(value)
        }

    /** Lazy backing field for [memoryValueEdges]. */
    private var _memoryValueEdges: Dataflows<Node>? = null

    /**
     * Each Declaration can also have a MemoryValue (allocated lazily, see [memoryAddressEdges]).
     */
    @Relationship
    override var memoryValueEdges: Dataflows<Node>
        get() =
            _memoryValueEdges
                ?: Dataflows<Node>(
                        this,
                        mirrorProperty = HasMemoryValue::memoryValueUsageEdges,
                        outgoing = false,
                    )
                    .also { _memoryValueEdges = it }
        set(value) {
            _memoryValueEdges = value
        }

    /** Virtual property for accessing [memoryValueEdges] as plain nodes. */
    @DoNotPersist
    override var memoryValues: MutableSet<Node>
        get() = memoryValueEdges.unwrap()
        set(value) {
            memoryValueEdges.resetTo(value)
        }

    /**
     * Returns the [Scope] that this [Declaration] declares (if it does). For example, for a
     * [Record], this will return the [RecordScope] of the particular record or class.
     */
    var declaringScope: Scope? = null

    override var modifiers: Set<String> = setOf()

    override fun getExitNextEOG(): Collection<Node> {
        return setOf()
    }

    override fun getStartingPrevEOG(): Collection<Node> {
        return setOf()
    }
}
