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

    @Relationship(value = "FIELDS", direction = Relationship.Direction.OUTGOING)
    var fieldEdges = astEdgesOf<FieldDeclaration>()
    var fields by unwrapping(RecordDeclaration::fieldEdges)

    @Relationship(value = "METHODS", direction = Relationship.Direction.OUTGOING)
    var methodEdges = astEdgesOf<MethodDeclaration>()
    var methods by unwrapping(RecordDeclaration::methodEdges)

    @Relationship(value = "CONSTRUCTORS", direction = Relationship.Direction.OUTGOING)
    var constructorEdges = astEdgesOf<ConstructorDeclaration>()
    var constructors by unwrapping(RecordDeclaration::constructorEdges)

    @Relationship(value = "RECORDS", direction = Relationship.Direction.OUTGOING)
    var recordEdges = astEdgesOf<RecordDeclaration>()
    var records by unwrapping(RecordDeclaration::recordEdges)

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
        addIfNotContains(fieldEdges, fieldDeclaration)
    }

    fun removeField(fieldDeclaration: FieldDeclaration) {
        fieldEdges.removeIf { it.end == fieldDeclaration }
    }

    fun addMethod(methodDeclaration: MethodDeclaration) {
        addIfNotContains(methodEdges, methodDeclaration)
    }

    fun removeMethod(methodDeclaration: MethodDeclaration?) {
        methodEdges.removeIf { it.end == methodDeclaration }
    }

    fun addConstructor(constructorDeclaration: ConstructorDeclaration) {
        addIfNotContains(constructorEdges, constructorDeclaration)
    }

    fun removeConstructor(constructorDeclaration: ConstructorDeclaration?) {
        constructorEdges.removeIf { it.end == constructorDeclaration }
    }

    fun removeRecord(recordDeclaration: RecordDeclaration) {
        recordEdges.removeIf { it.end == recordDeclaration }
    }

    fun removeTemplate(templateDeclaration: TemplateDeclaration?) {
        templateEdges.removeIf { it.end == templateDeclaration }
    }

    @DoNotPersist
    override val declarations: List<Declaration>
        get() {
            val list = ArrayList<Declaration>()
            list.addAll(fields)
            list.addAll(methods)
            list.addAll(constructors)
            list.addAll(records)
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
            .append("fields", fields)
            .append("methods", methods)
            .append("constructors", constructors)
            .append("records", records)
            .toString()
    }

    @DoNotPersist
    override val eogStarters: List<Node>
        get() {
            val list = mutableListOf<Node>()

            list += fields
            list += methods
            list += constructors
            list += this

            return list
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RecordDeclaration) return false
        return super.equals(other) &&
            kind == other.kind &&
            fields == other.fields &&
            propertyEqualsList(fieldEdges, other.fieldEdges) &&
            methods == other.methods &&
            propertyEqualsList(methodEdges, other.methodEdges) &&
            constructors == other.constructors &&
            propertyEqualsList(constructorEdges, other.constructorEdges) &&
            records == other.records &&
            propertyEqualsList(recordEdges, other.recordEdges) &&
            superClasses == other.superClasses &&
            implementedInterfaces == other.implementedInterfaces &&
            superTypeDeclarations == other.superTypeDeclarations
    }

    override fun hashCode() = super.hashCode() // TODO: Which fields can be safely added?

    override fun addDeclaration(declaration: Declaration) {
        when (declaration) {
            is ConstructorDeclaration -> addIfNotContains(constructorEdges, declaration)
            is MethodDeclaration -> addIfNotContains(methodEdges, declaration)
            is FieldDeclaration -> addIfNotContains(fieldEdges, declaration)
            is RecordDeclaration -> addIfNotContains(recordEdges, declaration)
            is TemplateDeclaration -> addIfNotContains(templateEdges, declaration)
        }
    }

    /**
     * Returns a type represented by this record.
     *
     * @return the type
     */
    fun toType(): Type {
        val type = objectType(name)
        if (type is ObjectType) {
            // as a shortcut, directly set the record declaration. This will be otherwise done
            // later by a pass, but for some frontends we need this immediately, so we set
            // this here.
            type.recordDeclaration = this
        }
        type.superTypes.addAll(this.superTypes)
        return type
    }

    override val declaredType: Type
        get() = toType()

    override val secondaryTypes: List<Type>
        get() = superTypes
}
