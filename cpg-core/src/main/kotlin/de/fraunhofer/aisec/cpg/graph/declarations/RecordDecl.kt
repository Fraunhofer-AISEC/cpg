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
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.Companion.propertyEqualsList
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdgeDelegate
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.types.HasSecondaryTypeEdge
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.Type
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship
import org.neo4j.ogm.annotation.Transient

/** Represents a C++ union/struct/class or Java class */
class RecordDecl : Declaration(), DeclarationHolder, StatementHolder, HasSecondaryTypeEdge {
    /** The kind, i.e. struct, class, union or enum. */
    var kind: String? = null

    @Relationship(value = "FIELDS", direction = Relationship.Direction.OUTGOING)
    @AST
    var fieldEdges: MutableList<PropertyEdge<FieldDecl>> = ArrayList()

    var fields by PropertyEdgeDelegate(RecordDecl::fieldEdges)

    @Relationship(value = "METHODS", direction = Relationship.Direction.OUTGOING)
    @AST
    var methodEdges: MutableList<PropertyEdge<MethodDecl>> = ArrayList()

    var methods by PropertyEdgeDelegate(RecordDecl::methodEdges)

    @Relationship(value = "CONSTRUCTORS", direction = Relationship.Direction.OUTGOING)
    @AST
    var constructorEdges: MutableList<PropertyEdge<ConstructorDecl>> = ArrayList()

    var constructors by PropertyEdgeDelegate(RecordDecl::constructorEdges)

    @Relationship(value = "RECORDS", direction = Relationship.Direction.OUTGOING)
    @AST
    var recordEdges: MutableList<PropertyEdge<RecordDecl>> = ArrayList()

    var records by PropertyEdgeDelegate(RecordDecl::recordEdges)

    @Relationship(value = "TEMPLATES", direction = Relationship.Direction.OUTGOING)
    @AST
    var templateEdges: MutableList<PropertyEdge<TemplateDecl>> = ArrayList()

    var templates by PropertyEdgeDelegate(RecordDecl::templateEdges)

    /** The list of statements. */
    @Relationship(value = "STATEMENTS", direction = Relationship.Direction.OUTGOING)
    @AST
    override var statementEdges: MutableList<PropertyEdge<Statement>> = ArrayList()

    override var statements by PropertyEdgeDelegate(RecordDecl::statementEdges)

    @Transient var superClasses: MutableList<Type> = ArrayList()

    /**
     * Interfaces implemented by this class. This concept is not present in C++
     *
     * @return the list of implemented interfaces
     */
    @Transient var implementedInterfaces = mutableListOf<Type>()

    @Relationship var superTypeDeclarations: Set<RecordDecl> = HashSet()

    var importStatements: List<String> = ArrayList()

    @Relationship var imports: MutableSet<Declaration> = HashSet()

    // Methods and fields can be imported statically
    var staticImportStatements: List<String> = ArrayList()

    @Relationship var staticImports: MutableSet<ValueDecl> = HashSet()

    fun addField(fieldDecl: FieldDecl) {
        addIfNotContains(fieldEdges, fieldDecl)
    }

    fun removeField(fieldDecl: FieldDecl) {
        fieldEdges.removeIf { it.end == fieldDecl }
    }

    fun addMethod(methodDecl: MethodDecl) {
        addIfNotContains(methodEdges, methodDecl)
    }

    fun removeMethod(methodDecl: MethodDecl?) {
        methodEdges.removeIf { it.end == methodDecl }
    }

    fun addConstructor(constructorDecl: ConstructorDecl) {
        addIfNotContains(constructorEdges, constructorDecl)
    }

    fun removeConstructor(constructorDecl: ConstructorDecl?) {
        constructorEdges.removeIf { it.end == constructorDecl }
    }

    fun removeRecord(recordDecl: RecordDecl) {
        recordEdges.removeIf { it.end == recordDecl }
    }

    fun removeTemplate(templateDecl: TemplateDecl?) {
        templateEdges.removeIf { it.end == templateDecl }
    }

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

    override fun updateType(typeState: Collection<Type>) {
        // Replace occurrences of the super classes and interfaces with the one combined type
        replaceType(superClasses, typeState)
        replaceType(implementedInterfaces, typeState)
    }

    private fun replaceType(list: MutableList<Type>, typeState: Collection<Type>) {
        for ((idx, t) in list.withIndex()) {
            for (newType in typeState) {
                if (newType == t) {
                    list[idx] = newType
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RecordDecl) return false
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
            is ConstructorDecl -> addIfNotContains(constructorEdges, declaration)
            is MethodDecl -> addIfNotContains(methodEdges, declaration)
            is FieldDecl -> addIfNotContains(fieldEdges, declaration)
            is RecordDecl -> addIfNotContains(recordEdges, declaration)
            is TemplateDecl -> addIfNotContains(templateEdges, declaration)
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
            type.recordDecl = this
        }
        type.superTypes.addAll(this.superTypes)
        return type
    }
}
