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
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.ValueDeclaration
import java.util.*

interface HasType : ContextProvider {

    var type: Type

    /**
     * This property refers to the [Type] that is either defined by the [Node] (e.g., by a
     * [ValueDeclaration]). If the [Node] does not define or use declared type, this will be null.
     */
    val declaredType: Type?

    /**
     * This property refers to the [Type] that the [Node] is assigned to. This could be different
     * from the [HasType.declaredType]. A common example is that a node could contain an interface
     * as a [HasType.declaredType], but the actual implementation of the type as [assignedType].
     *
     * An implementation of this must be sure to invoke [TypeObserver.typeChanged] of all
     * [typeObservers].
     */
    val assignedType: Type

    fun setType(type: Type, chain: MutableList<HasType>)

    fun setAssignedType(type: Type, chain: MutableList<HasType>)

    val typeObservers: MutableList<TypeObserver>

    interface TypeObserver {
        enum class ChangeType {
            DECLARED_TYPE,
            ASSIGNED_TYPE
        }

        fun typeChanged(
            newType: Type,
            changeType: ChangeType,
            src: HasType,
            chain: MutableList<HasType>
        )
    }

    fun informObservers(changeType: TypeObserver.ChangeType, chain: MutableList<HasType>) {
        // TODO(oxisto): this is not really working too well
        // If we are already in the chain, we are running into a loop, so we abort
        // here
        if (chain.filter { it == this }.size > 2) {
            return
        }

        // Add ourselves to the chain
        chain += this

        // If we would only propagate the unknown type, we can also skip it
        val newType =
            if (changeType == TypeObserver.ChangeType.ASSIGNED_TYPE) {
                this.assignedType
            } else {
                this.type
            }

        // Inform all type observers about the changes
        for (observer in typeObservers) {
            observer.typeChanged(newType, changeType, this, chain)
        }
    }

    fun registerTypeObserver(typeObserver: TypeObserver) {
        typeObservers += typeObserver

        // If we would only propagate the unknown type, we can also skip it
        var newType = this.type
        if (newType !is UnknownType) {
            // Immediately inform about changes
            typeObserver.typeChanged(
                newType,
                TypeObserver.ChangeType.DECLARED_TYPE,
                this,
                mutableListOf(this)
            )
        }

        // If we would only propagate the unknown type, we can also skip it
        newType = this.assignedType
        if (newType !is UnknownType) {
            // Immediately inform about changes
            typeObserver.typeChanged(
                newType,
                TypeObserver.ChangeType.ASSIGNED_TYPE,
                this,
                mutableListOf(this)
            )
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

fun Node.stopPropagation(type: Type, newType: Type): Boolean {
    val c = ctx ?: throw TranslationException("context not available")
    return c.typeManager.stopPropagation(type, newType)
}
