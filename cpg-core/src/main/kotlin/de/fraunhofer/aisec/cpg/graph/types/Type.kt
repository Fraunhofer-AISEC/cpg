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
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.Companion.propertyEqualsList
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.Companion.wrap
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdgeDelegate
import de.fraunhofer.aisec.cpg.graph.parseName
import de.fraunhofer.aisec.cpg.graph.types.PointerType.PointerOrigin
import de.fraunhofer.aisec.cpg.passes.TypeHierarchyResolver
import java.util.*
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.NodeEntity
import org.neo4j.ogm.annotation.Relationship

/**
 * Abstract Type, describing all possible SubTypes, i.e. all different Subtypes are compliant with
 * this class. Contains information which is included in any Type such as name, storage, qualifier
 * and origin
 */
@NodeEntity
abstract class Type : Node {
    /** All direct supertypes of this type. */
    @PopulatedByPass(TypeHierarchyResolver::class)
    @Relationship(value = "SUPER_TYPE", direction = Relationship.Direction.OUTGOING)
    var superTypes = mutableSetOf<Type>()
        protected set

    var isPrimitive = false
        protected set

    @Relationship(value = "GENERICS", direction = Relationship.Direction.OUTGOING)
    var genericsPropertyEdges: MutableList<PropertyEdge<Type>> = mutableListOf()
        private set

    var generics by PropertyEdgeDelegate(Type::genericsPropertyEdges)
        private set

    open var typeOrigin: Origin? = null

    constructor() {
        name = Name(EMPTY_NAME, null, language)
        genericsPropertyEdges = ArrayList()
    }

    constructor(type: Type?, generics: List<Type>? = listOf(), language: Language<*>? = null) {
        type?.name?.let { name = it.clone() }
        this.language = language
        this.genericsPropertyEdges = wrap(generics ?: listOf(), this)
        typeOrigin = type?.typeOrigin
    }

    constructor(
        typeName: CharSequence,
        generics: List<Type>? = listOf(),
        language: Language<*>? = null
    ) {
        name =
            if (this is FunctionType) {
                Name(typeName.toString(), null, language)
            } else {
                language.parseName(typeName)
            }
        this.genericsPropertyEdges = wrap(generics ?: listOf(), this)
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
        if (typeReferenceEquals(other)) return true

        return other is Type &&
            generics == other.generics &&
            propertyEqualsList(genericsPropertyEdges, other.genericsPropertyEdges) &&
            name == other.name &&
            language == other.language
    }

    override fun hashCode() = Objects.hash(name, language, generics)

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE).append("name", name).toString()
    }

    companion object {
        const val UNKNOWN_TYPE_STRING = "UNKNOWN"
    }

    /**
     * An ancestor is an item in a tree of types spanning from one particular [Type] to all of its
     * [Type.superTypes] (and their [Type.superTypes], and so on). Each item holds information about
     * the current "depth" within the tree.
     */
    class Ancestor(val type: Type, var depth: Int) {
        override fun hashCode(): Int {
            return Objects.hash(type)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }
            if (other !is Ancestor) {
                return false
            }
            return type == other.type
        }

        override fun toString(): String {
            return ToStringBuilder(this, TO_STRING_STYLE)
                .append("type", type.name)
                .append("depth", depth)
                .toString()
        }
    }
}

/** A shortcut to return [ObjectType.recordDeclaration], if this is a [ObjectType]. */
var Type.recordDeclaration: RecordDeclaration?
    get() {
        return (this as? ObjectType)?.recordDeclaration
    }
    set(value) {
        if (this is ObjectType) {
            this.recordDeclaration = value
        }
    }

/**
 * In order to achieve symmetric equals, we need to peek whether the "other" is a type. reference,
 * and if yes, we need to compare with its referringType instead.
 *
 * This function needs to be called in all [Type.equals] functions and their overrides like this:
 * ```
 * if (typeReferenceEquals(other)) return true
 * ```
 */
fun Type.typeReferenceEquals(other: Any?): Boolean {
    if (other is TypeReference) {
        return other.referringType?.equals(this) ?: false
    }

    return false
}
