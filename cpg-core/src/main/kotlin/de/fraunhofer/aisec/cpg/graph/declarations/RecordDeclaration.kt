/*
 * Copyright (c) 2020, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.declarations

import de.fraunhofer.aisec.cpg.frontends.TranslationException
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.edges.Edge.Companion.propertyEqualsList
import de.fraunhofer.aisec.cpg.graph.edges.ast.astEdgesOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.types.DeclaresType
import de.fraunhofer.aisec.cpg.graph.types.HasSecondaryTypeEdge
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.persistence.DoNotPersist
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship
import org.neo4j.ogm.annotation.Transient

/** Represents a C++ union/struct/class or Java class */
open class RecordDeclaration :
    Declaration(),
    DeclarationHolder,
    StatementHolder,
    EOGStarterHolder,
    DeclaresType,
    HasSecondaryTypeEdge {
    /** The kind, i.e. struct, class, union or enum. */
    var kind: String? = null

    /**
     * The [FieldDeclaration]s that are directly contained in this record declaration's AST
     * structure. This does not include any fields that might be declared in a base class or
     * interface or fields that are declared outside the AST structure.
     */
    @Relationship(value = "INNER_FIELDS", direction = Relationship.Direction.OUTGOING)
    var innerFieldEdges = astEdgesOf<FieldDeclaration>()
    /** Virtual property to directly access the nodes in [innerFieldEdges]. */
    var innerFields by unwrapping(RecordDeclaration::innerFieldEdges)

    /**
     * The [MethodDeclaration]s that are directly contained in this record declaration's AST
     * structure. This does not include any methods that might be declared in a base class or
     * interface or methods that are declared outside the AST structure.
     */
    @Relationship(value = "INNER_METHODS", direction = Relationship.Direction.OUTGOING)
    var innerMethodEdges = astEdgesOf<MethodDeclaration>()
    /** Virtual property to directly access the nodes in [innerMethods]. */
    var innerMethods by unwrapping(RecordDeclaration::innerMethodEdges)

    /**
     * The [ConstructorDeclaration]s that are directly contained in this record declaration's AST
     * structure. This does not include any constructors that might be declared in a base class or
     * interface or constructors that are declared outside the AST structure.
     */
    @Relationship(value = "CONSTRUCTORS", direction = Relationship.Direction.OUTGOING)
    var innerConstructorEdges = astEdgesOf<ConstructorDeclaration>()
    /** Virtual property to directly access the nodes in [innerConstructors]. */
    var innerConstructors by unwrapping(RecordDeclaration::innerConstructorEdges)

    /**
     * The [RecordDeclaration]s that are directly contained in this record declaration's AST
     * structure. This does not include any records that might be declared in a base class or
     * interface or records that are declared outside the AST structure.
     */
    @Relationship(value = "RECORDS", direction = Relationship.Direction.OUTGOING)
    var innerRecordEdges = astEdgesOf<RecordDeclaration>()
    /** Virtual property to directly access the nodes in [innerRecordEdges]. */
    var innerRecords by unwrapping(RecordDeclaration::innerRecordEdges)

    @Relationship(value = "TEMPLATES", direction = Relationship.Direction.OUTGOING)
    var templateEdges = astEdgesOf<TemplateDeclaration>()
    var templates by unwrapping(RecordDeclaration::templateEdges)

    /** The list of statements. */
    @Relationship(value = "STATEMENTS", direction = Relationship.Direction.OUTGOING)
    override var statementEdges = astEdgesOf<Statement>()
    override var statements by unwrapping(RecordDeclaration::statementEdges)

    @Transient var superClasses: MutableList<Type> = ArrayList()

    /**
     * Interfaces implemented by this class. This concept is not present in C++
     *
     * @return the list of implemented interfaces
     */
    @Transient var implementedInterfaces = mutableListOf<Type>()

    @Relationship var superTypeDeclarations: Set<RecordDeclaration> = HashSet()

    var importStatements: List<String> = ArrayList()

    @Relationship var imports: MutableSet<Declaration> = HashSet()

    // Methods and fields can be imported statically
    var staticImportStatements: List<String> = ArrayList()

    @Relationship var staticImports: MutableSet<ValueDeclaration> = HashSet()

    fun addField(fieldDeclaration: FieldDeclaration) {
        addIfNotContains(innerFieldEdges, fieldDeclaration)
    }

    fun removeField(fieldDeclaration: FieldDeclaration) {
        innerFieldEdges.removeIf { it.end == fieldDeclaration }
    }

    fun addMethod(methodDeclaration: MethodDeclaration) {
        addIfNotContains(innerMethodEdges, methodDeclaration)
    }

    fun removeMethod(methodDeclaration: MethodDeclaration?) {
        innerMethodEdges.removeIf { it.end == methodDeclaration }
    }

    fun addConstructor(constructorDeclaration: ConstructorDeclaration) {
        addIfNotContains(innerConstructorEdges, constructorDeclaration)
    }

    fun removeConstructor(constructorDeclaration: ConstructorDeclaration?) {
        innerConstructorEdges.removeIf { it.end == constructorDeclaration }
    }

    fun removeRecord(recordDeclaration: RecordDeclaration) {
        innerRecordEdges.removeIf { it.end == recordDeclaration }
    }

    fun removeTemplate(templateDeclaration: TemplateDeclaration?) {
        templateEdges.removeIf { it.end == templateDeclaration }
    }

    @DoNotPersist
    override val declarations: List<Declaration>
        get() {
            val list = ArrayList<Declaration>()
            list.addAll(innerFields)
            list.addAll(innerMethods)
            list.addAll(innerConstructors)
            list.addAll(innerRecords)
            list.addAll(templates)
            return list
        }

    val superTypes: List<Type>
        /**
         * Combines both implemented interfaces and extended classes. This is most commonly what you
         * are looking for when looking for method call targets etc.
         *
         * @return concatenation of [.getSuperClasses] and [.getImplementedInterfaces]
         */
        get() = superClasses + implementedInterfaces

    /**
     * Adds a type to the list of super classes for this record declaration.
     *
     * @param superClass the super class.
     */
    fun addSuperClass(superClass: Type) {
        superClasses.add(superClass)
    }

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("name", name)
            .append("kind", kind)
            .append("superTypeDeclarations", superTypeDeclarations)
            .append("innerFields", innerFields)
            .append("innerMethods", innerMethods)
            .append("innerConstructors", innerConstructors)
            .append("innerRecords", innerRecords)
            .toString()
    }

    @DoNotPersist
    override val eogStarters: List<Node>
        get() {
            val list = mutableListOf<Node>()

            list += innerFields
            list += innerMethods
            list += innerConstructors

            return list
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RecordDeclaration) return false
        return super.equals(other) &&
            kind == other.kind &&
            innerFields == other.innerFields &&
            propertyEqualsList(innerFieldEdges, other.innerFieldEdges) &&
            innerMethods == other.innerMethods &&
            propertyEqualsList(innerMethodEdges, other.innerMethodEdges) &&
            innerConstructors == other.innerConstructors &&
            propertyEqualsList(innerConstructorEdges, other.innerConstructorEdges) &&
            innerRecords == other.innerRecords &&
            propertyEqualsList(innerRecordEdges, other.innerRecordEdges) &&
            superClasses == other.superClasses &&
            implementedInterfaces == other.implementedInterfaces &&
            superTypeDeclarations == other.superTypeDeclarations
    }

    override fun hashCode() = super.hashCode() // TODO: Which fields can be safely added?

    override fun addDeclaration(declaration: Declaration) {
        when (declaration) {
            is ConstructorDeclaration -> addIfNotContains(innerConstructorEdges, declaration)
            is MethodDeclaration -> addIfNotContains(innerMethodEdges, declaration)
            is FieldDeclaration -> addIfNotContains(innerFieldEdges, declaration)
            is RecordDeclaration -> addIfNotContains(innerRecordEdges, declaration)
            is TemplateDeclaration -> addIfNotContains(templateEdges, declaration)
        }
    }

    /**
     * Returns a type represented by this record.
     *
     * @return the type
     */
    fun toType(): ObjectType {
        val type = objectType(name)
        if (type is ObjectType) {
            // As a shortcut, directly set the record declaration. This will be otherwise done
            // later by a pass, but for some frontends we need this immediately, so we set
            // this here.
            type.recordDeclaration = this
            type.superTypes.addAll(this.superTypes)
            return type
        } else {
            throw TranslationException("Cannot create type for $this, as it is not an ObjectType")
        }
    }

    override val declaredType: Type
        get() = toType()

    override val secondaryTypes: List<Type>
        get() = superTypes
}
