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

import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.parseName
import de.fraunhofer.aisec.cpg.graph.types.PointerType.PointerOrigin
import java.util.*
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/**
 * Abstract Type, describing all possible SubTypes, i.e. all different Subtypes are compliant with
 * this class. Contains information which is included in any Type such as name, storage, qualifier
 * and origin
 */
abstract class Type : Node {
    /** All direct supertypes of this type. */
    @Relationship(value = "SUPER_TYPE", direction = Relationship.Direction.OUTGOING)
    var superTypes: MutableSet<Type> = HashSet()
        protected set

    var isPrimitive = false
        protected set

    open var typeOrigin: Origin? = null

    constructor() {
        name = Name(EMPTY_NAME, null, language)
    }

    constructor(typeName: String?) {
        name = language.parseName(typeName!!)
        typeOrigin = Origin.UNRESOLVED
    }

    constructor(type: Type?) {
        name = type?.name?.clone() ?: Name(UNKNOWN_TYPE_STRING)
        typeOrigin = type?.typeOrigin
    }

    constructor(typeName: CharSequence, language: Language<out LanguageFrontend>?) {
        name =
            if (this is FunctionType) {
                Name(typeName.toString(), null, language)
            } else {
                language.parseName(typeName)
            }
        this.language = language
        typeOrigin = Origin.UNRESOLVED
    }

    constructor(fullTypeName: Name, language: Language<out LanguageFrontend>?) {
        name = fullTypeName.clone()
        typeOrigin = Origin.UNRESOLVED
        this.language = language
    }

    /** Type Origin describes where the Type information came from */
    enum class Origin {
        RESOLVED,
        DATAFLOW,
        GUESSED,
        UNRESOLVED
    }

    /**
     * @param pointer Reason for the reference (array of pointer)
     * @return Returns a reference to the current Type. E.g. when creating a pointer to an existing
     *   ObjectType
     */
    abstract fun reference(pointer: PointerOrigin?): Type

    /**
     * @return Dereferences the current Type by resolving the reference. E.g. when dereferencing a
     *   pointer type we obtain the type the pointer is pointing towards
     */
    abstract fun dereference(): Type

    open fun refreshNames() {
        // By default, this does nothing. TODO: Why do all types have this?? This method has
        // functionality only for PointerTypes!
    }

    /**
     * The root Type Element for a Type Chain (follows Pointer and ReferenceTypes until a Object-,
     * Incomplete-, or FunctionPtrType is reached).
     */
    var root: Type
        get() =
            if (this is SecondOrderType) {
                (this as SecondOrderType).elementType.root
            } else {
                this
            }
        set(value) {
            if (this is SecondOrderType) {
                if ((this as SecondOrderType).elementType is SecondOrderType) {
                    ((this as SecondOrderType).elementType as SecondOrderType).elementType = value
                } else {
                    (this as SecondOrderType).elementType = value
                }
            }
        }

    /** Creates an exact copy of the current type (chain) */
    abstract fun duplicate(): Type

    /** A shortcut to the fully qualified name of this type, based on [name]. */
    val typeName: String
        get() = name.toString()

    /**
     * The number of steps that are required in order to traverse the type chain until the root is
     * reached
     */
    open val referenceDepth: Int
        get() = 0

    /**
     * True if this type is a so called "first order type" (root of a chain), i.e., not a pointer or
     * reference to a type.
     */
    val isFirstOrderType: Boolean
        get() =
            (this is ObjectType ||
                this is UnknownType ||
                this is FunctionType || // TODO(oxisto): convert FunctionPointerType to second order
                // type
                this is FunctionPointerType ||
                this is IncompleteType ||
                this is ParameterizedType)

    /**
     * Required for possibleSubTypes to check if the new Type should be considered a subtype or not
     *
     * @param t other type the similarity is checked with
     * @return True if the parameter t is equal to the current type (this)
     */
    open fun isSimilar(t: Type?): Boolean {
        return if (this == t) {
            true
        } else this.root.name == t?.root?.name
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Type) return false
        return name == other.name && language == other.language
    }

    override fun hashCode() = Objects.hash(name, language)

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE).append("name", name).toString()
    }

    companion object {
        const val UNKNOWN_TYPE_STRING = "UNKNOWN"
    }
}
