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

import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.ContextProvider
import de.fraunhofer.aisec.cpg.graph.LanguageProvider
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.ValueDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import java.util.*

interface HasType : ContextProvider, LanguageProvider {

    /**
     * This property refers to the *definite* [Type] that the [Node] has. If you are unsure about
     * what it's type is, you should prefer to set it to the [UnknownType]. It is usually one of the
     * following:
     * - the type declared by the [Node], e.g., by a [ValueDeclaration]
     * - intrinsically tied to the node, e.g. an [IntegerType] in an integer [Literal]
     * - the [Type] of a declaration a node is referring to, e.g., in a
     *   [DeclaredReferenceExpression]
     *
     * An implementation of this must be sure to invoke [informObservers].
     */
    var type: Type

    /**
     * This property refers to a list of [Type] nodes which are assigned to that [Node]. This could
     * be different from the [HasType.type]. A common example is that a node could contain an
     * interface as a [HasType.type], but the actual implementation of the type as one of the
     * [assignedTypes]. This could potentially also be empty, if we don't see any assignments to
     * this expression.
     *
     * Note: in order to properly inform observers, one should NOT use the regular [MutableSet.add]
     * or [MutableSet.addAll] but rather use [addAssignedType] and [addAssignedTypes]. Otherwise, we
     * cannot watch for changes within the set. We therefore only expose this as a [Set], but an
     * implementing class MUST implement this as a [MutableSet] so that we can modify it internally.
     */
    var assignedTypes: Set<Type>

    fun addAssignedType(type: Type) {
        if (language?.shouldPropagateType(this, type) == false) {
            return
        }

        val changed = (this.assignedTypes as MutableSet).add(type)
        if (changed) {
            informObservers(TypeObserver.ChangeType.ASSIGNED_TYPE)
        }
    }

    fun addAssignedTypes(types: Set<Type>) {
        val changed =
            (this.assignedTypes as MutableSet).addAll(
                types.filter { language?.shouldPropagateType(this, it) == true }
            )
        if (changed) {
            informObservers(TypeObserver.ChangeType.ASSIGNED_TYPE)
        }
    }

    val typeObservers: MutableList<TypeObserver>

    interface TypeObserver {
        enum class ChangeType {
            TYPE,
            ASSIGNED_TYPE
        }

        fun typeChanged(newType: Type, src: HasType)

        fun assignedTypeChanged(assignedTypes: Set<Type>, src: HasType)
    }

    fun informObservers(changeType: TypeObserver.ChangeType) {
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

    fun unregisterTypeObserver(typeObserver: TypeObserver) {
        typeObservers -= typeObserver
    }
}

fun Node.isSupertypeOf(superType: Type, subType: Type): Boolean {
    val c = ctx ?: throw TranslationException("context not available")
    return c.typeManager.isSupertypeOf(superType, subType, this)
}

fun <T : Type> Node.registerType(type: T): T {
    val c = ctx ?: throw TranslationException("context not available")
    return c.typeManager.registerType(type)
}

fun Node.getCommonType(types: Collection<Type>): Optional<Type> {
    val c = ctx ?: throw TranslationException("context not available")
    return c.typeManager.getCommonType(types, this.ctx)
}
