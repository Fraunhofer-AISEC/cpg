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

import de.fraunhofer.aisec.cpg.PopulatedByPass
import de.fraunhofer.aisec.cpg.TypeManager
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.parseName
import de.fraunhofer.aisec.cpg.graph.types.PointerType.PointerOrigin
import de.fraunhofer.aisec.cpg.passes.TypeHierarchyResolver
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
    @PopulatedByPass(TypeHierarchyResolver::class)
    @Relationship(value = "SUPER_TYPE", direction = Relationship.Direction.OUTGOING)
    var superTypes = mutableSetOf<Type>()
        protected set

    var isPrimitive = false
        protected set

    open var typeOrigin: Origin? = null

    constructor() {
        name = Name(EMPTY_NAME, null, language)
    }

    constructor(typeName: String?) {
        name = language.parseName(typeName ?: UNKNOWN_TYPE_STRING)
        typeOrigin = Origin.UNRESOLVED
    }

    constructor(type: Type?) {
        type?.name?.let { name = it.clone() }
        typeOrigin = type?.typeOrigin
    }

    constructor(typeName: CharSequence, language: Language<*>?) {
        name =
            if (this is FunctionType) {
                Name(typeName.toString(), null, language)
            } else {
                language.parseName(typeName)
            }
        this.language = language
        typeOrigin = Origin.UNRESOLVED
    }

    constructor(fullTypeName: Name, language: Language<*>?) {
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
     * Creates a new [Type] based on a reference of this type. The main usage is to create pointer
     * and array types. This function does NOT invoke [TypeManager.registerType] and should only be
     * used internally. For the public API, the extension functions, such as [Type.array] should be
     * used instead.
     *
     * @param pointer Reason for the reference (array of pointer)
     * @return Returns a reference to the current Type. E.g. when creating a pointer to an existing
     *   ObjectType
     *
     * TODO(oxisto) Ideally, we would make this function "internal", but there is a bug in the Go
     *   frontend, so that we still need this function :(
     */
    abstract fun reference(pointer: PointerOrigin?): Type

    /**
     * @return Dereferences the current Type by resolving the reference. E.g. when dereferencing a
     *   pointer type we obtain the type the pointer is pointing towards
     */
    abstract fun dereference(): Type

    open fun refreshNames() {}

    var root: Type
        /**
         * Obtain the root Type Element for a Type Chain (follows Pointer and ReferenceTypes until a
         * Object-, Incomplete-, or FunctionPtrType is reached).
         *
         * @return root Type
         */
        get() =
            if (this is SecondOrderType) {
                (this as SecondOrderType).elementType.root
            } else {
                this
            }
        set(newRoot) {
            if (this is SecondOrderType) {
                if ((this as SecondOrderType).elementType is SecondOrderType) {
                    ((this as SecondOrderType).elementType as SecondOrderType).elementType = newRoot
                } else {
                    (this as SecondOrderType).elementType = newRoot
                }
            }
        }

    /** @return Creates an exact copy of the current type (chain) */
    abstract fun duplicate(): Type

    val typeName: String
        get() = name.toString()

    open val referenceDepth: Int
        /**
         * @return number of steps that are required in order to traverse the type chain until the
         *   root is reached
         */
        get() = 0

    val isFirstOrderType: Boolean
        /**
         * @return True if the Type parameter t is a FirstOrderType (Root of a chain) and not a
         *   Pointer or ReferenceType
         */
        get() =
            (this is ObjectType ||
                this is AutoType ||
                this is UnknownType ||
                this is FunctionType ||
                this is ProblemType ||
                this is TupleType // TODO(oxisto): convert FunctionPointerType to second order type
                ||
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
        return other is Type && name == other.name && language == other.language
    }

    override fun hashCode() = Objects.hash(name, language)

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE).append("name", name).toString()
    }

    companion object {
        const val UNKNOWN_TYPE_STRING = "UNKNOWN"
    }
}
