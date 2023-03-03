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
import de.fraunhofer.aisec.cpg.graph.HasType.SecondaryTypeEdge
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.Companion.propertyEqualsList
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.Companion.transformIntoOutgoingPropertyEdgeList
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdgeDelegate
import de.fraunhofer.aisec.cpg.graph.types.PointerType.PointerOrigin
import de.fraunhofer.aisec.cpg.graph.types.UnknownType.Companion.getUnknownType
import java.util.*
import org.neo4j.ogm.annotation.Relationship

/**
 * This is the main type in the Type system. ObjectTypes describe objects, as instances of a class.
 * This also includes primitive data types.
 */
open class ObjectType : Type, SecondaryTypeEdge {
    /**
     * Reference from the ObjectType to its class (RecordDeclaration) only if the class is available
     */
    var recordDeclaration: RecordDeclaration? = null

    @Relationship(value = "GENERICS", direction = Relationship.Direction.OUTGOING)
    var genericsPropertyEdges: MutableList<PropertyEdge<Type>> = mutableListOf()

    var generics by PropertyEdgeDelegate(ObjectType::genericsPropertyEdges)

    constructor(
        typeName: CharSequence,
        generics: List<Type>,
        primitive: Boolean,
        language: Language<out LanguageFrontend>?
    ) : super(typeName, language) {
        this.genericsPropertyEdges = transformIntoOutgoingPropertyEdgeList(generics, this)
        isPrimitive = primitive
        this.language = language
    }

    constructor(
        type: Type?,
        generics: List<Type>,
        primitive: Boolean,
        language: Language<out LanguageFrontend>?
    ) : super(type) {
        this.language = language
        this.genericsPropertyEdges = transformIntoOutgoingPropertyEdgeList(generics, this)
        isPrimitive = primitive
    }

    /** Empty default constructor for use in Neo4J persistence. */
    constructor() : super() {
        genericsPropertyEdges = ArrayList()
        isPrimitive = false
    }

    override fun updateType(typeState: Collection<Type>) {
        for (t in generics) {
            for (t2 in typeState) {
                if (t2 == t) {
                    replaceGenerics(t, t2)
                }
            }
        }
    }

    fun replaceGenerics(oldType: Type?, newType: Type) {
        for (i in genericsPropertyEdges.indices) {
            val propertyEdge = genericsPropertyEdges[i]
            if (propertyEdge.end.equals(oldType)) {
                propertyEdge.end = newType
            }
        }
    }

    /** @return PointerType to a ObjectType, e.g. int* */
    override fun reference(pointer: PointerOrigin?): PointerType {
        return PointerType(this, pointer)
    }

    fun reference(): PointerType {
        return PointerType(this, PointerOrigin.POINTER)
    }

    /**
     * @return UnknownType, as we cannot infer any type information when dereferencing an
     *   ObjectType, as it is just some memory and its interpretation is unknown
     */
    override fun dereference(): Type {
        return getUnknownType(language)
    }

    override fun duplicate(): Type {
        return ObjectType(this, generics, isPrimitive, language)
    }

    fun addGeneric(generic: Type) {
        val propertyEdge = PropertyEdge(this, generic)
        propertyEdge.addProperty(Properties.INDEX, genericsPropertyEdges.size)
        genericsPropertyEdges.add(propertyEdge)
    }

    fun addGenerics(generics: List<Type>) {
        for (generic in generics) {
            addGeneric(generic)
        }
    }

    override fun isSimilar(t: Type?): Boolean {
        return t is ObjectType && generics == t.generics && super.isSimilar(t)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ObjectType) return false
        if (!super.equals(other)) return false
        return generics == other.generics &&
            propertyEqualsList(genericsPropertyEdges, other.genericsPropertyEdges) &&
            isPrimitive == other.isPrimitive
    }

    override fun hashCode() = Objects.hash(super.hashCode(), generics, isPrimitive)
}
