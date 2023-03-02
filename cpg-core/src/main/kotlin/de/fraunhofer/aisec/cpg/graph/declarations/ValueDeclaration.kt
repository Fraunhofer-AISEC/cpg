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

import de.fraunhofer.aisec.cpg.graph.HasType
import de.fraunhofer.aisec.cpg.graph.TypeManager
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.Companion.unwrap
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.types.FunctionPointerType
import de.fraunhofer.aisec.cpg.graph.types.ReferenceType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
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
    protected var _type: Type = UnknownType.unknownType

    /**
     * The type of this declaration. In order to maximize compatibility with Java legacy code
     * (primarily the type listeners), this is a virtual property which wraps around a dedicated
     * backing field [_type].
     */
    override var type: Type
        get() {
            val result: Type =
                if (TypeManager.isTypeSystemActive()) {
                    _type
                } else {
                    TypeManager.getInstance()
                        .typeCache
                        .computeIfAbsent(this) { mutableListOf() }
                        .stream()
                        .findAny()
                        .orElse(UnknownType.unknownType)
                }
            return result
        }
        set(value) {
            // Trigger the type listener foo
            setType(value, null)
        }

    protected var _possibleSubTypes = mutableListOf<Type>()

    override var possibleSubTypes: List<Type>
        get() {
            return if (!TypeManager.isTypeSystemActive()) {
                TypeManager.getInstance().typeCache.getOrDefault(this, emptyList())
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
    @Relationship(value = "USAGE")
    var usageEdges: MutableList<PropertyEdge<DeclaredReferenceExpression>> = ArrayList()

    /** All usages of the variable/field. */
    /** Set all usages of the variable/field and assembles the access properties. */
    var usages: List<DeclaredReferenceExpression>
        get() = unwrap(usageEdges, true)
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
                (type as ReferenceType?)?.elementType ?: UnknownType.unknownType
            } else type
        }

    override fun setType(type: Type, root: MutableList<HasType>?) {
        var type: Type? = type
        var root: MutableList<HasType>? = root
        if (!TypeManager.isTypeSystemActive()) {
            TypeManager.getInstance().cacheType(this, type)
            return
        }
        if (root == null) {
            root = ArrayList()
        }
        if (
            type == null ||
                root.contains(this) ||
                TypeManager.getInstance().isUnknown(type) ||
                this._type is FunctionPointerType && type !is FunctionPointerType
        ) {
            return
        }
        val oldType = this.type
        type = type.duplicate()
        val subTypes: MutableSet<Type?> = HashSet()
        for (t in possibleSubTypes) {
            if (!t.isSimilar(type)) {
                subTypes.add(t)
            }
        }
        subTypes.add(type)
        this._type =
            TypeManager.getInstance()
                .registerType(TypeManager.getInstance().getCommonType(subTypes, this).orElse(type))
        val newSubtypes: MutableList<Type> = ArrayList()
        for (s in subTypes) {
            if (TypeManager.getInstance().isSupertypeOf(this.type, s, this)) {
                newSubtypes.add(TypeManager.getInstance().registerType(s))
            }
        }
        possibleSubTypes = newSubtypes
        if (oldType == type) {
            // Nothing changed, so we do not have to notify the listeners.
            return
        }
        root.add(this) // Add current node to the set of "triggers" to detect potential loops.
        // Notify all listeners about the changed type
        for (l in typeListeners) {
            if (l != this) {
                l.typeChanged(this, root, oldType)
            }
        }
    }

    override fun setPossibleSubTypes(possibleSubTypes: List<Type>, root: MutableList<HasType>) {
        var possibleSubTypes = possibleSubTypes
        possibleSubTypes =
            possibleSubTypes
                .filterNot { type -> TypeManager.getInstance().isUnknown(type) }
                .distinct()
                .toMutableList()
        if (!TypeManager.isTypeSystemActive()) {
            possibleSubTypes.forEach { t -> TypeManager.getInstance().cacheType(this, t) }

            return
        }
        if (root.contains(this)) {
            return
        }
        val oldSubTypes = this.possibleSubTypes
        this._possibleSubTypes = possibleSubTypes

        if (HashSet(oldSubTypes).containsAll(possibleSubTypes)) {
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

    override fun registerTypeListener(listener: HasType.TypeListener) {
        val root = mutableListOf<HasType>(this)
        typeListeners.add(listener)
        listener.typeChanged(this, root, type)
        listener.possibleSubTypesChanged(this, root)
    }

    override fun unregisterTypeListener(listener: HasType.TypeListener) {
        typeListeners.remove(listener)
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
