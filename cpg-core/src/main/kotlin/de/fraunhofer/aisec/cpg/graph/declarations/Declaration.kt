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
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.edges.MemoryAddressEdges
import de.fraunhofer.aisec.cpg.graph.edges.flows.Dataflows
import de.fraunhofer.aisec.cpg.graph.edges.memoryAddressEdgesOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.graph.scopes.RecordScope
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.graph.scopes.Symbol
import de.fraunhofer.aisec.cpg.graph.statements.expressions.MemoryAddress
import de.fraunhofer.aisec.cpg.persistence.DoNotPersist
import org.neo4j.ogm.annotation.NodeEntity
import org.neo4j.ogm.annotation.Relationship

/**
 * Represents a single declaration or definition, i.e. of a variable ([VariableDeclaration]) or
 * function ([FunctionDeclaration]).
 *
 * Note: We do NOT (currently) distinguish between the definition and the declaration of a function.
 * This means, that if a function is first declared and later defined with a function body, we will
 * currently have two [FunctionDeclaration] nodes. This is very similar to the behaviour of clang,
 * however clang does establish a connection between those nodes, we currently do not.
 */
@NodeEntity
abstract class Declaration : AstNode(), HasMemoryAddress, HasMemoryValue {
    @DoNotPersist
    val symbol: Symbol
        get() {
            return this.name.localName
        }

    /**
     * Each Declaration allocates new memory, AKA a new address, so we create a new MemoryAddress
     * node. Should only be a single address!
     */
    @Relationship
    override var memoryAddressEdges: MemoryAddressEdges =
        memoryAddressEdgesOf(
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
    override var memoryAddresses by unwrapping(Declaration::memoryAddressEdges)

    /** Where the memory value of this declaration is used. */
    @Relationship
    override var memoryValueUsageEdges =
        Dataflows<Node>(this, mirrorProperty = HasMemoryValue::memoryValueEdges, outgoing = true)
    override var memoryValueUsages by unwrapping(Declaration::memoryValueUsageEdges)

    /** Each Declaration can also have a MemoryValue. */
    @Relationship
    override var memoryValueEdges =
        Dataflows<Node>(
            this,
            mirrorProperty = HasMemoryValue::memoryValueUsageEdges,
            outgoing = false,
        )
    override var memoryValues by unwrapping(Declaration::memoryValueEdges)

    /**
     * Returns the [Scope] that this [Declaration] declares (if it does). For example, for a
     * [RecordDeclaration], this will return the [RecordScope] of the particular record or class.
     */
    var declaringScope: Scope? = null

    override fun getExitNextEOG(): Collection<Node> {
        return setOf()
    }

    override fun getStartingPrevEOG(): Collection<Node> {
        return setOf()
    }
}
