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
package de.fraunhofer.aisec.cpg.graph.statements.expressions

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.types.FunctionPointerType
import de.fraunhofer.aisec.cpg.graph.types.ReferenceType
import de.fraunhofer.aisec.cpg.graph.types.Type
import java.util.*
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship
import org.neo4j.ogm.annotation.Transient

/**
 * Represents one expression. It is used as a base class for multiple different types of
 * expressions. The only constraint is, that each expression has a type.
 *
 * <p>Note: In our graph, {@link Expression} is inherited from {@link Statement}. This is a
 * constraint of the C++ language. In C++, it is valid to have an expression (for example a {@link
 * Literal}) as part of a function body, even though the expression value is not used. Consider the
 * following code: <code> int main() { 1; } </code>
 *
 * <p>This is not possible in Java, the aforementioned code example would prompt a compile error.
 */
abstract class Expression : Statement(), HasType {

    @Relationship("TYPE") private var _type: Type = newUnknownType()

    /** The type of the value after evaluation. */
    override var type: Type
        get() {
            val result: Type =
                if (TypeManager.isTypeSystemActive()) {
                    _type
                } else {
                    TypeManager.getInstance()
                        .typeCache
                        .computeIfAbsent(this) { mutableListOf() }
                        .firstOrNull()
                        ?: newUnknownType()
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
            return if (!TypeManager.isTypeSystemActive()) {
                TypeManager.getInstance().typeCache.getOrDefault(this, emptyList())
            } else _possibleSubTypes
        }
        set(value) {
            setPossibleSubTypes(value, ArrayList())
        }

    @Transient override val typeListeners: MutableSet<HasType.TypeListener> = HashSet()

    override val propagationType: Type
        get() {
            return if (type is ReferenceType) {
                (type as ReferenceType?)?.elementType ?: newUnknownType()
            } else type
        }

    @Override
    override fun setType(type: Type, root: MutableList<HasType>?) {
        var t: Type = type
        var r: MutableList<HasType>? = root

        // TODO Document this method. It is called very often (potentially for each AST node) and
        //  performs less than optimal.
        if (!TypeManager.isTypeSystemActive()) {
            this._type = t
            TypeManager.getInstance().cacheType(this, t)
            return
        }

        if (r == null) {
            r = mutableListOf()
        }

        // No (or only unknown) type given, loop detected? Stop early because there's nothing we can
        // do.
        if (
            r.contains(this) ||
                TypeManager.getInstance().isUnknown(t) ||
                TypeManager.getInstance().stopPropagation(this.type, t) ||
                (this.type is FunctionPointerType && t !is FunctionPointerType)
        ) {
            return
        }

        val oldType = this.type
        // Backup to check if something changed

        t = t.duplicate()
        val subTypes = mutableSetOf<Type>()

        // Check all current subtypes and consider only those which are "different enough" to type.
        for (s in possibleSubTypes) {
            if (!s.isSimilar(s)) {
                subTypes.add(s)
            }
        }

        subTypes.add(t)

        // Probably tries to get something like the best supertype of all possible subtypes.
        this._type =
            TypeManager.getInstance()
                .registerType(TypeManager.getInstance().getCommonType(subTypes, this).orElse(t))

        // TODO: Why do we need this loop? Shouldn't the condition be ensured by the previous line
        //  getting the common type??
        val newSubtypes = mutableListOf<Type>()
        for (s in subTypes) {
            if (TypeManager.getInstance().isSupertypeOf(this.type, s, this)) {
                newSubtypes.add(TypeManager.getInstance().registerType(s))
            }
        }

        possibleSubTypes = newSubtypes

        if (oldType == t) {
            // Nothing changed, so we do not have to notify the listeners.
            return
        }

        // Add current node to the set of "triggers" to detect potential loops.
        r.add(this)

        // Notify all listeners about the changed type
        for (l in typeListeners) {
            if (l != this) {
                l.typeChanged(this, r, oldType)
            }
        }
    }

    override fun setPossibleSubTypes(possibleSubTypes: List<Type>, root: MutableList<HasType>) {
        var list = possibleSubTypes
        list =
            list
                .filterNot { type -> TypeManager.getInstance().isUnknown(type) }
                .distinct()
                .toMutableList()
        if (!TypeManager.isTypeSystemActive()) {
            list.forEach { t -> TypeManager.getInstance().cacheType(this, t) }
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
        return ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("type", type)
            .toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is Expression) {
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
