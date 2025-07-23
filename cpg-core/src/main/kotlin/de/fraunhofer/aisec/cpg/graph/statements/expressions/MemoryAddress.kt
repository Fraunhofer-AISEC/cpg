/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.statements.expressions

import de.fraunhofer.aisec.cpg.graph.HasMemoryAddress
import de.fraunhofer.aisec.cpg.graph.HasMemoryValue
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.edges.flows.Dataflows
import de.fraunhofer.aisec.cpg.graph.edges.memoryAddressEdgesOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import org.neo4j.ogm.annotation.Relationship

open class MemoryAddress(override var name: Name, open var isGlobal: Boolean = false) :
    Node(), HasMemoryValue {

    /**
     * Each Declaration allocates new memory, AKA a new address, so we create a new MemoryAddress
     * node
     */
    @Relationship
    open var usageEdges =
        memoryAddressEdgesOf(
            mirrorProperty = HasMemoryAddress::memoryAddressEdges,
            outgoing = false,
        )
    open var usages by unwrapping(MemoryAddress::usageEdges)

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        // TODO: What else do we need to compare?
        return other is MemoryAddress && name == other.name && isGlobal == other.isGlobal
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    @Relationship
    override var memoryValueEdges =
        Dataflows<Node>(
            this,
            mirrorProperty = HasMemoryValue::memoryValueUsageEdges,
            outgoing = false,
        )
    override var memoryValues by unwrapping(MemoryAddress::memoryValueEdges)

    @Relationship
    override var memoryValueUsageEdges =
        Dataflows<Node>(this, mirrorProperty = HasMemoryValue::memoryValueEdges, outgoing = true)
    override var memoryValueUsages by unwrapping(MemoryAddress::memoryValueUsageEdges)
}

/**
 * There is a value, but we cannot determine it while processing this node. We assume that this
 * value will definitely be set when we really execute the code. E.g., it's set outside the
 * function's context. This is used for a [ParameterDeclaration] and serves as some sort of stepping
 * stone.
 */
class ParameterMemoryValue(override var name: Name) : MemoryAddress(name), HasMemoryAddress {
    /**
     * The ParameterMemoryValue is usually the Value of a parameter. Let's use this little helper to
     * get to the parameter's address
     */
    override var memoryAddressEdges =
        memoryAddressEdgesOf(mirrorProperty = MemoryAddress::usageEdges, outgoing = true)

    override var memoryAddresses by unwrapping(ParameterMemoryValue::memoryAddressEdges)
}

/** We don't know the value. It might be set somewhere else or not. No idea. */
class UnknownMemoryValue(
    override var name: Name = Name(""),
    override var isGlobal: Boolean = false,
) : MemoryAddress(name), HasMemoryAddress {
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other != null && other::class != this::class) {
            return false
        }
        // TODO: What else do we need to compare?
        return other is MemoryAddress && name == other.name
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override var memoryAddressEdges =
        memoryAddressEdgesOf(mirrorProperty = MemoryAddress::usageEdges, outgoing = true)

    override var memoryAddresses by unwrapping(UnknownMemoryValue::memoryAddressEdges)
}
