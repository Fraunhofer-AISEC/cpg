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

import de.fraunhofer.aisec.cpg.PopulatedByPass
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.UnknownLanguage
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.edges.MemoryAddressEdges
import de.fraunhofer.aisec.cpg.graph.edges.flows.Dataflows
import de.fraunhofer.aisec.cpg.graph.edges.flows.Usages
import de.fraunhofer.aisec.cpg.graph.edges.flows.dataflowsOf
import de.fraunhofer.aisec.cpg.graph.edges.memoryAddressEdgesOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.graph.expressions.MemoryAddress
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.helpers.identitySetOf
import de.fraunhofer.aisec.cpg.passes.SymbolResolver
import de.fraunhofer.aisec.cpg.persistence.DoNotPersist
import de.fraunhofer.aisec.cpg.persistence.Relationship
import org.apache.commons.lang3.builder.ToStringBuilder

/** A declaration who has a type. */
abstract class ValueDeclaration : Declaration(), HasType, HasMemoryAddress, HasMemoryValue {

    @DoNotPersist override var observerEnabled: Boolean = true

    override val typeObservers: MutableSet<HasType.TypeObserver> = identitySetOf()

    override var language: Language<*> = UnknownLanguage
        set(value) {
            // We need to adjust an eventual unknown type, once we know the language
            field = value
            if (type is UnknownType) {
                type = UnknownType.getUnknownType(value)
            }
        }

    /** The type of this declaration. */
    override var type: Type = unknownType()
        set(value) {
            val old = field
            field = value

            // Only inform our observer if the type has changed. This should not trigger if we
            // "squash" types into one, because they should still be regarded as "equal", but not
            // the "same".
            if (old != value) {
                informObservers(HasType.TypeObserver.ChangeType.TYPE)
            }

            // We also want to add the definitive type (if known) to our assigned types
            if (value !is UnknownType && value !is AutoType) {
                addAssignedType(value)
            }
        }

    override var assignedTypes: Set<Type> = mutableSetOf()
        set(value) {
            if (field == value) {
                return
            }

            field = value
            informObservers(HasType.TypeObserver.ChangeType.ASSIGNED_TYPE)
        }

    /** Lazy backing field for [memoryAddressEdges]. */
    private var _memoryAddressEdges: MemoryAddressEdges? = null

    /**
     * Each value declaration allocates new memory, AKA a new address, so we create a new
     * MemoryAddress node. Should only be a single address!
     *
     * This and the other two memory-model containers ([memoryValueUsageEdges], [memoryValueEdges])
     * are only populated by the DFG and points-to passes and stay empty on most declarations. Their
     * backing containers are therefore allocated lazily on first access. They are not part of
     * [equals]/[hashCode], so lazy-on-access is safe. These members live on [ValueDeclaration] (not
     * the more general [Declaration]) because only value-carrying declarations ever hold a memory
     * address or value.
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
                ?: dataflowsOf(HasMemoryValue::memoryValueEdges, outgoing = true).also {
                    _memoryValueUsageEdges = it
                }
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
     * Each value declaration can also have a MemoryValue (allocated lazily, see
     * [memoryAddressEdges]).
     */
    @Relationship
    override var memoryValueEdges: Dataflows<Node>
        get() =
            _memoryValueEdges
                ?: dataflowsOf(HasMemoryValue::memoryValueUsageEdges, outgoing = false).also {
                    _memoryValueEdges = it
                }
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
     * Links to all the [Reference]s accessing the variable and the respective access value (read,
     * write, readwrite).
     */
    @PopulatedByPass(SymbolResolver::class)
    @Relationship(value = "USAGE")
    var usageEdges = Usages<Reference>(this)

    /** All usages of the variable/field. */
    @PopulatedByPass(SymbolResolver::class) var usages by unwrapping(ValueDeclaration::usageEdges)

    /**
     * Defines, whether this declaration is static or not. Commonly used to declare static
     * [FieldDeclaration]s or [MethodDeclaration]s in classes (see [RecordDeclaration]).
     */
    var isStatic = false

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE).appendSuper(super.toString()).toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is ValueDeclaration) {
            return false
        }
        return (super.equals(other) && type == other.type)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}
