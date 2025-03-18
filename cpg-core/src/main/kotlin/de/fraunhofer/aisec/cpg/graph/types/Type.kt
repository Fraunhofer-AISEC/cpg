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

import com.fasterxml.jackson.annotation.JsonIgnore
import de.fraunhofer.aisec.cpg.PopulatedByPass
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.Name
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.parseName
import de.fraunhofer.aisec.cpg.graph.types.PointerType.PointerOrigin
import de.fraunhofer.aisec.cpg.passes.TypeHierarchyResolver
import de.fraunhofer.aisec.cpg.passes.TypeResolver
import de.fraunhofer.aisec.cpg.persistence.DoNotPersist
import java.util.*
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.NodeEntity
import org.neo4j.ogm.annotation.Relationship

/**
 * This array holds the chain of different pointer/array operations. For example if a [PointerType]
 * is built from its element type, which in turn could be a [ReferenceType] or another pointer.
 */
typealias TypeOperations = List<TypeOperation>

/** An operation that is applied on a [Type], e.g. a pointer, an array or a reference. */
enum class TypeOperation {
    /** a [PointerType] with [PointerType.PointerOrigin.ARRAY] */
    ARRAY,
    /** a [PointerType] with [PointerType.PointerOrigin.POINTER] */
    POINTER,
    /** a [ReferenceType] */
    REFERENCE,
}

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

    open var typeOrigin: Origin? = null

    /**
     * The list of second-order types based on this type. An example might be a [PointerType], whose
     * [PointerType.elementType] is this type.
     */
    @DoNotPersist val secondOrderTypes = mutableListOf<Type>()

    /**
     * This points to the [DeclaresType] node (most likely a [Declaration]), that declares this
     * type. At some point this should replace [ObjectType.recordDeclaration].
     */
    @PopulatedByPass(TypeResolver::class) var declaredFrom: DeclaresType? = null

    constructor() : super() {
        name = Name(EMPTY_NAME, null, language)
    }

    constructor(typeName: CharSequence, language: Language<*>) : this() {
        this.language = language
        name =
            if (this is FunctionType) {
                Name(typeName.toString(), null, language)
            } else {
                parseName(typeName)
            }
        typeOrigin = Origin.UNRESOLVED
    }

    /** Type Origin describes where the Type information came from */
    enum class Origin {
        RESOLVED,
        DATAFLOW,
        GUESSED,
        UNRESOLVED,
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

    open fun refreshNames() {
        secondOrderTypes.forEach { it.refreshNames() }
    }

    @get:JsonIgnore
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is Type &&
            name == other.name &&
            scope === other.scope &&
            language == other.language
    }

    /**
     * We need a constant hashcode implementation because we need to change [name] and [scope]
     * during the [TypeResolver], so we cannot use them for hashcode.
     */
    override fun hashCode() = 1

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE).append("name", name).toString()
    }

    companion object {
        const val UNKNOWN_TYPE_STRING = "UNKNOWN"
    }

    /**
     * Calculates and returns the [TypeOperations] of the current type. A [TypeOperations] can be
     * used to compute a "wrapped" type, for example a [PointerType] back from its [Type.root].
     */
    val typeOperations: TypeOperations
        get() {
            if (this !is SecondOrderType) {
                return listOf()
            }

            // We already know the depth, so we can just set this and allocate the pointer origins
            // array
            val operations = mutableListOf<TypeOperation>()

            var type = this
            while (type is SecondOrderType) {
                var op =
                    if (type is ReferenceType) {
                        TypeOperation.REFERENCE
                    } else if (type is PointerType && type.isArray) {
                        TypeOperation.ARRAY
                    } else {
                        TypeOperation.POINTER
                    }

                operations += op

                type = type.elementType
            }

            return operations
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

/**
 * Wraps the given [Type] into a chain of [PointerType]s and [ReferenceType]s, given the operations
 * in [TypeOperations].
 */
fun TypeOperations.apply(root: Type): Type {
    var type = root

    if (this.isNotEmpty()) {
        for (i in this.size - 1 downTo 0) {
            var wrap = this[i]
            type =
                when (wrap) {
                    TypeOperation.REFERENCE -> ReferenceType(type)
                    TypeOperation.ARRAY -> type.reference(PointerOrigin.ARRAY)
                    TypeOperation.POINTER -> type.reference(PointerOrigin.POINTER)
                }
        }
    }

    return type
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
 * This interfaces specifies that this node (most likely a [Declaration]) declares a type. This is
 * used by [TypeResolver.resolveType] to find appropriate symbols and declarations.
 */
interface DeclaresType {

    /** The [Type] that is being declared. */
    val declaredType: Type
}
