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
import de.fraunhofer.aisec.cpg.graph.types.FunctionPointerType
import de.fraunhofer.aisec.cpg.graph.types.ReferenceType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.passes.VariableUsageResolver
import java.util.stream.Collectors
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship
import org.neo4j.ogm.annotation.Transient

/** A declaration who has a type. */
abstract class ValueDeclaration : Declaration(), HasType {
    /**
     * A dedicated backing field, so that [setType] can actually set the type without any loops,
     * since we are using a custom setter in [type] (which calls [setType]).
     */
    @Relationship("TYPE") protected var _type: Type = UnknownType.getUnknownType(null)

    /**
     * The type of this declaration. In order to maximize compatibility with Java legacy code
     * (primarily the type listeners), this is a virtual property which wraps around a dedicated
     * backing field [_type].
     */
    override var type: Type
        get() {
            val result: Type =
                if (isTypeSystemActive) {
                    _type
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
            setType(value, null)
        }

    @Relationship("POSSIBLE_SUB_TYPES") protected var _possibleSubTypes = mutableListOf<Type>()
    override var possibleSubTypes: List<Type>
        get() {
            return if (!isTypeSystemActive) {
                ctx?.typeManager?.typeCache?.getOrDefault(this, emptyList()) ?: listOf()
            } else _possibleSubTypes
        }
        set(value) {
            setPossibleSubTypes(value, ArrayList())
        }

    @Transient override val typeListeners: MutableSet<HasType.TypeListener> = HashSet()

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

    /**
     * There is no case in which we would want to propagate a referenceType as in this case always
     * the underlying ObjectType should be propagated
     *
     * @return Type that should be propagated
     */
    override val propagationType: Type
        get() {
            return if (type is ReferenceType) {
                (type as ReferenceType?)?.elementType ?: unknownType()
            } else type
        }

    override fun setType(type: Type, root: MutableList<HasType>?) {
        var t: Type = type
        var r: MutableList<HasType>? = root
        if (!isTypeSystemActive) {
            cacheType(t)
            return
        }
        if (r == null) {
            r = ArrayList()
        }
        if (
            r.contains(this) ||
                t is UnknownType ||
                this._type is FunctionPointerType && t !is FunctionPointerType
        ) {
            return
        }
        val oldType = this.type
        t = t.duplicate()
        val subTypes = mutableSetOf<Type>()
        for (t in possibleSubTypes) {
            if (!t.isSimilar(t)) {
                subTypes.add(t)
            }
        }
        subTypes.add(t)
        this._type = registerType(getCommonType(subTypes).orElse(t))
        val newSubtypes: MutableList<Type> = ArrayList()
        for (s in subTypes) {
            if (isSupertypeOf(this.type, s)) {
                newSubtypes.add(registerType(s))
            }
        }
        possibleSubTypes = newSubtypes
        if (oldType == t) {
            // Nothing changed, so we do not have to notify the listeners.
            return
        }
        r.add(this) // Add current node to the set of "triggers" to detect potential loops.
        // Notify all listeners about the changed type
        for (l in typeListeners) {
            if (l != this) {
                l.typeChanged(this, r, oldType)
            }
        }
    }

    override fun setPossibleSubTypes(possibleSubTypes: List<Type>, root: MutableList<HasType>) {
        var list = possibleSubTypes
        list = list.filterNot { type -> type is UnknownType }.distinct().toMutableList()
        if (!isTypeSystemActive) {
            list.forEach { t -> cacheType(t) }

            return
        }
        if (root.contains(this)) {
            return
        }
        val oldSubTypes = this.possibleSubTypes
        this._possibleSubTypes = list

        if (HashSet(oldSubTypes).containsAll(list)) {
            // Nothing changed, so we do not have to notify the listeners.
            return
        }
        // Add current node to the set of "triggers" to detect potential loops.
        root.add(this)

        // Notify all listeners about the changed type
        for (listener in typeListeners) {
            if (listener != this) {
                listener.possibleSubTypesChanged(this, root)
            }
        }
    }

    override fun resetTypes(type: Type) {
        val oldSubTypes = possibleSubTypes
        val oldType = this._type
        this._type = type
        possibleSubTypes = listOf(type)
        val root = mutableListOf<HasType>(this)
        if (oldType != type) {
            typeListeners
                .stream()
                .filter { l: HasType.TypeListener -> l != this }
                .forEach { l: HasType.TypeListener -> l.typeChanged(this, root, oldType) }
        }
        if (oldSubTypes.size != 1 || !oldSubTypes.contains(type))
            typeListeners
                .stream()
                .filter { l: HasType.TypeListener -> l != this }
                .forEach { l: HasType.TypeListener -> l.possibleSubTypesChanged(this, root) }
    }

    override fun refreshType() {
        val root = mutableListOf<HasType>(this)
        for (l in typeListeners) {
            l.typeChanged(this, root, type)
            l.possibleSubTypesChanged(this, root)
        }
    }

    override fun updateType(type: Type) {
        this._type = type
    }

    override fun updatePossibleSubtypes(types: List<Type>) {
        this._possibleSubTypes = types.toMutableList()
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
        return (super.equals(other) &&
            type == other.type &&
            possibleSubTypes == other.possibleSubTypes)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}
