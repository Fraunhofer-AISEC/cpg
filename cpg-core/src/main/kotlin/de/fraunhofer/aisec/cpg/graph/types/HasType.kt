/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.types

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.graph.LanguageProvider
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.applyMetadata
import de.fraunhofer.aisec.cpg.graph.declarations.ValueDeclaration
import de.fraunhofer.aisec.cpg.graph.edges.ast.AstEdge
import de.fraunhofer.aisec.cpg.graph.edges.collections.EdgeSingletonList
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.statements.expressions.UnaryOperator
import de.fraunhofer.aisec.cpg.graph.unknownType

/**
 * This interfaces denotes that the given [Node] has a "type". Currently, we only have two known
 * implementations of this class, an [Expression] and a [ValueDeclaration]. All other nodes with
 * types should derive from these two base classes.
 */
interface HasType : LanguageProvider {

    /**
     * This property is used to determine if the type propagation using [TypeObserver] is enabled
     * for this node. This property is set to `true` by default. This property can be set to `false`
     * by [applyMetadata] based on [TranslationConfiguration.disableTypeObserver].
     */
    var observerEnabled: Boolean

    /**
     * This property refers to the *definite* [Type] that the [Node] has during *compile-time*. If
     * you are unsure about what it's type is, you should prefer to set it to the [UnknownType]. It
     * is usually one of the following:
     * - the type declared by the [Node], e.g., by a [ValueDeclaration]
     * - intrinsically tied to the node, e.g. an [IntegerType] in an integer [Literal]
     * - the [Type] of a declaration a node is referring to, e.g., in a [Reference]
     *
     * An implementation of this must be sure to invoke [informObservers].
     */
    var type: Type

    /**
     * This property refers to a list of [Type] nodes which are assigned to that [Node] at-runtime.
     * This could be different from the [HasType.type]. A common example is that a node could
     * contain an interface as a [HasType.type], but the actual implementation of the type as one of
     * the [assignedTypes]. This could potentially also be empty, if we don't see any assignments to
     * this expression.
     *
     * Note: in order to properly inform observers, one should NOT use the regular [MutableSet.add]
     * or [MutableSet.addAll] but rather use [addAssignedType] and [addAssignedTypes]. Otherwise, we
     * cannot watch for changes within the set. We therefore only expose this as a [Set], but an
     * implementing class MUST implement this as a [MutableSet] so that we can modify it internally.
     */
    var assignedTypes: Set<Type>

    /**
     * Adds [type] to the list of [HasType.assignedTypes] and informs all observers about the
     * change.
     */
    fun addAssignedType(type: Type) {
        if (!observerEnabled || language.shouldPropagateType(this.type, type, this) == false) {
            return
        }

        val changed = (this.assignedTypes as MutableSet).add(type)
        if (changed) {
            informObservers(TypeObserver.ChangeType.ASSIGNED_TYPE)
        }
    }

    /**
     * Adds all [types] to the list of [HasType.assignedTypes] and informs all observers about the
     * change.
     */
    fun addAssignedTypes(types: Set<Type>) {
        if (!observerEnabled) {
            return
        }

        val changed =
            (this.assignedTypes as MutableSet).addAll(
                types.filter { language.shouldPropagateType(this.type, it, this) == true }
            )
        if (changed) {
            informObservers(TypeObserver.ChangeType.ASSIGNED_TYPE)
        }
    }

    /**
     * A list of [TypeObserver] objects that will be informed about type changes, usually by
     * [informObservers].
     */
    val typeObservers: MutableSet<TypeObserver>

    /**
     * A [TypeObserver] can be used by its implementing class to observe changes to the
     * [HasType.type] and/or [HasType.assignedTypes] of a [Node] (that implements [HasType]). The
     * implementing node can then decide if and how to propagate this type information to itself
     * (and possibly to others). Examples include modifying the incoming type depending on an
     * operator, e.g., in a [UnaryOperator] expression. Changes to [HasType.type] will invoke
     * [typeChanged], changes to [HasType.assignedTypes] will invoke [assignedTypes].
     */
    interface TypeObserver {
        enum class ChangeType {
            TYPE,
            ASSIGNED_TYPE,
        }

        /**
         * This callback function will be invoked, if the observed node changes its [HasType.type].
         */
        fun typeChanged(newType: Type, src: HasType)

        /**
         * This callback function will be invoked, if the observed node changes its
         * [HasType.assignedTypes].
         */
        fun assignedTypeChanged(assignedTypes: Set<Type>, src: HasType)

        /**
         * A helper function that can be used for [EdgeSingletonList.onChanged]. It unregisters this
         * [TypeObserver] with the [old] node and registers it with the [new] one. It updates the
         * [de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression.access] property of
         * [new.end].
         */
        fun <NodeType : Node> exchangeTypeObserverWithAccessPropagation(
            old: AstEdge<NodeType>?,
            new: AstEdge<NodeType>?,
        ) {
            exchangeTypeObserver(old, new, true)
        }

        /**
         * A helper function that can be used for [EdgeSingletonList.onChanged]. It unregisters this
         * [TypeObserver] with the [old] node and registers it with the [new] one. It also updates
         * the [de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression.access] property of
         * [new.end] if [propagateAccess] is set to `true`.
         */
        fun <NodeType : Node> exchangeTypeObserver(
            old: AstEdge<NodeType>?,
            new: AstEdge<NodeType>?,
            propagateAccess: Boolean,
        ) {
            (old?.end as? HasType)?.unregisterTypeObserver(this)
            (new?.end as? HasType)?.registerTypeObserver(this)
            if (propagateAccess && this is Expression) {
                (new?.end as? Expression)?.access = this.access
            }
        }

        /**
         * A helper function that can be used for [EdgeSingletonList.onChanged]. It unregisters this
         * [TypeObserver] with the [old] node and registers it with the [new] one. It does not
         * update the [de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression.access]
         * property of [new.end].
         */
        fun <NodeType : Node> exchangeTypeObserverWithoutAccessPropagation(
            old: AstEdge<NodeType>?,
            new: AstEdge<NodeType>?,
        ) {
            exchangeTypeObserver(old, new, false)
        }
    }

    /**
     * This function SHOULD be used be an implementing class to inform observers about type changes.
     * While the implementing class can technically do this on its own, it is strongly recommended
     * to use this function to harmonize the behaviour of propagating types.
     */
    fun informObservers(changeType: TypeObserver.ChangeType) {
        if (!observerEnabled) {
            return
        }

        if (changeType == TypeObserver.ChangeType.ASSIGNED_TYPE) {
            val assignedTypes = this.assignedTypes
            if (assignedTypes.isEmpty()) {
                return
            }
            // Inform all type observers about the changes
            for (observer in typeObservers) {
                observer.assignedTypeChanged(assignedTypes, this)
            }
        } else {
            val newType = this.type
            if (newType is UnknownType) {
                return
            }
            // Inform all type observers about the changes
            for (observer in typeObservers) {
                observer.typeChanged(newType, this)
            }
        }
    }

    /**
     * Registers the given [typeObservers] to be informed about type updates. This also immediately
     * invokes both [TypeObserver.typeChanged] and [TypeObserver.assignedTypeChanged].
     */
    fun registerTypeObserver(typeObserver: TypeObserver) {
        typeObservers += typeObserver

        // If we would only propagate the unknown type, we can also skip it
        val newType = this.type
        if (newType !is UnknownType) {
            // Immediately inform about changes
            typeObserver.typeChanged(newType, this)
        }

        // If we would propagate an empty list, we can also skip it
        val assignedTypes = this.assignedTypes
        if (assignedTypes.isNotEmpty()) {
            // Immediately inform about changes
            typeObserver.assignedTypeChanged(assignedTypes, this)
        }
    }

    /** Unregisters the given [typeObservers] from the list of observers. */
    fun unregisterTypeObserver(typeObserver: TypeObserver) {
        typeObservers -= typeObserver
    }
}

/**
 * A special [HasType.TypeObserver] that can be used in cases where we cannot directly use an
 * initializer but still want to depend on the type of the variable in [decl]. Most cases include
 * languages that have implicit declarations that are later computed in a pass, such sa Go or
 * Python.
 */
class InitializerTypePropagation(private var decl: HasType, private var tupleIdx: Int = -1) :
    HasType.TypeObserver {
    override fun typeChanged(newType: Type, src: HasType) {
        if (newType is TupleType && tupleIdx != -1) {
            decl.type = newType.types.getOrElse(tupleIdx) { decl.unknownType() }
        } else {
            decl.type = newType
        }
    }

    override fun assignedTypeChanged(assignedTypes: Set<Type>, src: HasType) {
        // Also propagate the assigned types to the declaration. This is important for languages
        // with dynamic types because we rely on the assigned types and the dynamic type always
        // stays the "dynamic type".
        val assignedTypes =
            assignedTypes.map {
                if (it is TupleType && tupleIdx != -1) {
                    it.types.getOrElse(tupleIdx) { decl.unknownType() }
                } else {
                    it
                }
            }

        decl.addAssignedTypes(assignedTypes.toSet())
    }
}
