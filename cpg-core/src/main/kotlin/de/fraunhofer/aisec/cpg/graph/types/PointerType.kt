/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.graph.Name
import java.util.*
import org.neo4j.ogm.annotation.Relationship

/**
 * PointerTypes represent all references to other Types. For C/CPP this includes pointers, as well
 * as arrays, since technically arrays are pointers. For JAVA the only use case are arrays as there
 * is no such pointer concept.
 */
class PointerType : Type, SecondOrderType {
    @Relationship(value = "ELEMENT_TYPE") override var elementType: Type

    enum class PointerOrigin {
        POINTER,
        ARRAY,
    }

    var pointerOrigin: PointerOrigin? = null
        private set

    constructor(
        elementType: Type,
        pointerOrigin: PointerOrigin?,
    ) : super(
        if (pointerOrigin == PointerOrigin.ARRAY) {
            elementType.name.append("[]")
        } else {
            elementType.name.append("*")
        },
        elementType.language,
    ) {
        this.pointerOrigin = pointerOrigin
        this.elementType = elementType
        this.elementType.secondOrderTypes += this
    }

    /**
     * @return referencing a PointerType results in another PointerType wrapping the first
     *   PointerType, e.g. int**
     */
    override fun reference(pointer: PointerOrigin?): PointerType {
        var origin = pointer
        if (origin == null) {
            origin = PointerOrigin.ARRAY
        }
        return PointerType(this, origin)
    }

    /** @return dereferencing a PointerType yields the type the pointer was pointing towards */
    override fun dereference(): Type {
        return elementType
    }

    override fun refreshNames() {
        if (elementType is PointerType) {
            elementType.refreshNames()
        }
        var localName = elementType.name.localName
        localName +=
            if (pointerOrigin == PointerOrigin.ARRAY) {
                "[]"
            } else {
                "*"
            }
        val fullTypeName = Name(localName, elementType.name.parent, elementType.name.delimiter)
        name = fullTypeName
    }

    val isArray: Boolean
        get() = pointerOrigin == PointerOrigin.ARRAY

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PointerType) return false
        return super.equals(other) &&
            elementType == other.elementType &&
            pointerOrigin == other.pointerOrigin
    }

    override fun hashCode() = Objects.hash(super.hashCode(), elementType, pointerOrigin)
}
