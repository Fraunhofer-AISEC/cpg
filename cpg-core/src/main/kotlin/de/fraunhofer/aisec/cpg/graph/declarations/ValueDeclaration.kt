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
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.types.*
import de.fraunhofer.aisec.cpg.passes.VariableUsageResolver
import java.util.stream.Collectors
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/** A declaration who has a type. */
abstract class ValueDeclaration : Declaration(), HasType {
    /**
     * A dedicated backing field, so that [setType] can actually set the type without any loops,
     * since we are using a custom setter in [type] (which calls [setType]).
     */
    @Relationship("TYPE") override var declaredType: Type = unknownType()

    override val typeObservers = mutableListOf<HasType.TypeObserver>()

    /**
     * The type of this declaration. In order to maximize compatibility with Java legacy code
     * (primarily the type listeners), this is a virtual property which wraps around a dedicated
     * backing field [_type].
     */
    override var type: Type
        get() {
            val result: Type =
                if (isTypeSystemActive) {
                    declaredType
                } else {
                    ctx?.typeManager
                        ?.typeCache
                        ?.computeIfAbsent(this) { mutableListOf() }
                        ?.firstOrNull()
                        ?: unknownType()
                }
            return result
        }
        set(value) {
            // Trigger the type listener foo
            setType(value, mutableListOf())
        }

    /**
     * Links to all the [DeclaredReferenceExpression]s accessing the variable and the respective
     * access value (read, write, readwrite).
     */
    @PopulatedByPass(VariableUsageResolver::class)
    @Relationship(value = "USAGE")
    var usageEdges: MutableList<PropertyEdge<DeclaredReferenceExpression>> = ArrayList()

    /** All usages of the variable/field. */
    @PopulatedByPass(VariableUsageResolver::class)
    var usages: List<DeclaredReferenceExpression>
        get() = unwrap(usageEdges, true)
        /** Set all usages of the variable/field and assembles the access properties. */
        set(usages) {
            usageEdges =
                usages
                    .stream()
                    .map { ref: DeclaredReferenceExpression ->
                        val edge = PropertyEdge(this, ref)
                        edge.addProperty(Properties.ACCESS, ref.access)
                        edge
                    }
                    .collect(Collectors.toList())
        }

    /** Adds a usage of the variable/field and assembles the access property. */
    fun addUsage(reference: DeclaredReferenceExpression) {
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

    override var assignedType: Type = unknownType()

    override fun setType(type: Type, chain: MutableList<HasType>) {
        if (isTypeSystemActive) {
            declaredType = type

            informObservers(HasType.TypeObserver.ChangeType.DECLARED_TYPE, chain)

            // If our assigned type is unknown, we can also set it to our type
            if (assignedType is UnknownType) {
                setAssignedType(type, chain)
            }
        } else {
            cacheType(type)
        }
    }

    override fun setAssignedType(type: Type, chain: MutableList<HasType>) {
        assignedType = type

        informObservers(HasType.TypeObserver.ChangeType.ASSIGNED_TYPE, chain)
    }
}
