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
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.Companion.unwrap
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.passes.VariableUsageResolver
import java.util.stream.Collectors
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/** A declaration who has a type. */
abstract class ValueDeclaration : Declaration(), HasType {
    override val typeObservers = mutableListOf<HasType.TypeObserver>()

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

    /**
     * Links to all the [Reference]s accessing the variable and the respective access value (read,
     * write, readwrite).
     */
    @PopulatedByPass(VariableUsageResolver::class)
    @Relationship(value = "USAGE")
    var usageEdges: MutableList<PropertyEdge<Reference>> = ArrayList()

    /** All usages of the variable/field. */
    @PopulatedByPass(VariableUsageResolver::class)
    var usages: List<Reference>
        get() = unwrap(usageEdges, true)
        /** Set all usages of the variable/field and assembles the access properties. */
        set(usages) {
            usageEdges =
                usages
                    .stream()
                    .map { ref: Reference ->
                        val edge = PropertyEdge(this, ref)
                        edge.addProperty(Properties.ACCESS, ref.access)
                        edge
                    }
                    .collect(Collectors.toList())
        }

    /** Adds a usage of the variable/field and assembles the access property. */
    fun addUsage(reference: Reference) {
        val usageEdge = PropertyEdge(this, reference)
        usageEdge.addProperty(Properties.ACCESS, reference.access)
        usageEdges.add(usageEdge)
    }

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
