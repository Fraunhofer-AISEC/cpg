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
import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.frontends.Language
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.types.PointerType.PointerOrigin
import de.fraunhofer.aisec.cpg.graph.unknownType
import de.fraunhofer.aisec.cpg.passes.TypeResolver
import java.util.*
import org.neo4j.ogm.annotation.Relationship

/**
 * This is the main type in the Type system. ObjectTypes describe objects, as instances of a class.
 * This also includes primitive data types.
 */
open class ObjectType : Type {
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
        ctx: TranslationContext,
        typeName: CharSequence,
        generics: List<Type>,
        primitive: Boolean,
        language: Language<*>,
    ) : super(ctx, typeName, language) {
        this.generics = generics
        isPrimitive = primitive
        this.language = language
    }

    constructor(
        ctx: TranslationContext,
        type: Type?,
        generics: List<Type>,
        primitive: Boolean,
        language: Language<*>,
    ) : super(ctx, type) {
        this.language = language
        this.generics = generics
        isPrimitive = primitive
    }

    /** @return PointerType to a ObjectType, e.g. int* */
    override fun reference(pointer: PointerOrigin?): PointerType {
        return PointerType(ctx, this, pointer)
    }

    fun reference(): PointerType {
        return PointerType(ctx, this, PointerOrigin.POINTER)
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
}
