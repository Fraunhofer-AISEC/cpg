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
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.declarations.ConstructorDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.FieldDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.MethodDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.scopes.Scope
import de.fraunhofer.aisec.cpg.graph.types.PointerType.PointerOrigin
import de.fraunhofer.aisec.cpg.graph.unknownType
import de.fraunhofer.aisec.cpg.helpers.identitySetOf
import de.fraunhofer.aisec.cpg.passes.TypeResolver
import java.util.*
import org.neo4j.ogm.annotation.Relationship

/**
 * This is the main type in the Type system. ObjectTypes describe objects, as instances of a class.
 * This also includes primitive data types.
 */
open class ObjectType : Type, HasSecondaryTypeEdge {
    /**
     * Reference from the [ObjectType] to its class ([RecordDeclaration]), only if the class is
     * available. This is set by the [TypeResolver].
     *
     * This also sets this type's [scope] to the [RecordDeclaration.scope].
     */
    @PopulatedByPass(TypeResolver::class)
    var recordDeclaration: RecordDeclaration? = null
        set(value) {
            field = value
            this.scope = value?.scope
        }

    @Relationship(value = "GENERICS", direction = Relationship.Direction.OUTGOING)
    var generics: List<Type>
        private set

    constructor(
        typeName: CharSequence,
        generics: List<Type>,
        primitive: Boolean,
        mutable: Boolean,
        language: Language<*>,
    ) : super(typeName, language) {
        this.generics = generics
        this.isPrimitive = primitive
        this.isMutable = mutable
        this.language = language
    }

    constructor(
        typeName: CharSequence,
        generics: List<Type>,
        primitive: Boolean,
        language: Language<*>,
    ) : super(typeName, language) {
        this.generics = generics
        this.isPrimitive = primitive
        this.isMutable = true
        this.language = language
    }

    /** Empty default constructor for use in Neo4J persistence. */
    constructor() : super() {
        this.isPrimitive = false
        this.isMutable = true
        this.generics = mutableListOf<Type>()
    }

    /** @return PointerType to a ObjectType, e.g. int* */
    override fun reference(pointer: PointerOrigin?): PointerType {
        return PointerType(this, pointer)
    }

    fun reference(): PointerType {
        return PointerType(this, PointerOrigin.POINTER)
    }

    /**
     * @return UnknownType, as we cannot infer any type information when de-referencing an
     *   ObjectType, as it is just some memory and its interpretation is unknown
     */
    override fun dereference(): Type {
        return unknownType()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ObjectType) return false
        if (!super.equals(other)) return false
        return generics == other.generics && isPrimitive == other.isPrimitive
    }

    override fun hashCode() = Objects.hash(super.hashCode(), generics, isPrimitive)

    override val secondaryTypes: List<Type>
        get() = generics

    /**
     * Returns all constructors that are declared in this type and its super types. See
     * [findMembers] for more details.
     */
    val constructors: Set<ConstructorDeclaration>
        get() {
            return findMembers<ConstructorDeclaration>()
        }

    /**
     * Returns all methods that are declared in this type and its super types. See [findMembers] for
     * more details.
     */
    val methods: Set<MethodDeclaration>
        get() {
            return findMembers<MethodDeclaration>()
        }

    /**
     * Returns all fields that are declared in this type and its super types. See [findMembers] for
     * more details.
     */
    val fields: Set<FieldDeclaration>
        get() {
            return findMembers<FieldDeclaration>()
        }

    /**
     * Returns all [Declaration] nodes (of type [T]) that are declared in this type and its super
     * types. We use the underlying [recordDeclaration] of the type to find the [Scope] it declares
     * and then look for appropriate symbols pointing to a [Declaration].
     */
    private inline fun <reified T : Declaration> findMembers(): Set<T> {
        // We need to gather all members that are in within the scope of the underlying record
        // declaration, as well as their super types
        val members = mutableSetOf<T>()

        // Gather all members of this type and its super-types
        val worklist = mutableListOf<Type>(this)
        val alreadySeen = identitySetOf<Type>()
        while (worklist.isNotEmpty()) {
            val next = worklist.removeFirst()

            // Add all members in the declaring scope
            next.recordDeclaration
                ?.declaringScope
                ?.symbols
                ?.values
                ?.flatten()
                ?.filterIsInstanceTo(members)

            // Add super types
            worklist += next.superTypes
            alreadySeen.add(next)
        }

        return members.toSet()
    }
}
