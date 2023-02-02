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

import de.fraunhofer.aisec.cpg.graph.DeclarationHolder
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.StatementHolder
import de.fraunhofer.aisec.cpg.graph.SubGraph
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.Companion.propertyEqualsList
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdgeDelegate
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.types.ObjectType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import java.util.stream.Collectors
import java.util.stream.Stream
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship
import org.neo4j.ogm.annotation.Transient

/** Represents a C++ union/struct/class or Java class */
class RecordDeclaration : Declaration(), DeclarationHolder, StatementHolder {
    /** The kind, i.e. struct, class, union or enum. */
    var kind: String? = null

    @Relationship(value = "FIELDS", direction = Relationship.Direction.OUTGOING)
    @field:SubGraph("AST")
    var fieldEdges: MutableList<PropertyEdge<FieldDeclaration>> = ArrayList()

    var fields by PropertyEdgeDelegate(RecordDeclaration::fieldEdges)

    @Relationship(value = "METHODS", direction = Relationship.Direction.OUTGOING)
    @field:SubGraph("AST")
    var methodEdges: MutableList<PropertyEdge<MethodDeclaration>> = ArrayList()

    var methods by PropertyEdgeDelegate(RecordDeclaration::methodEdges)

    @Relationship(value = "CONSTRUCTORS", direction = Relationship.Direction.OUTGOING)
    @field:SubGraph("AST")
    var constructorEdges: MutableList<PropertyEdge<ConstructorDeclaration>> = ArrayList()

    var constructors by PropertyEdgeDelegate(RecordDeclaration::constructorEdges)

    @Relationship(value = "RECORDS", direction = Relationship.Direction.OUTGOING)
    @field:SubGraph("AST")
    var recordEdges: MutableList<PropertyEdge<RecordDeclaration>> = ArrayList()

    var records by PropertyEdgeDelegate(RecordDeclaration::recordEdges)

    @Relationship(value = "TEMPLATES", direction = Relationship.Direction.OUTGOING)
    @field:SubGraph("AST")
    var templateEdges: MutableList<PropertyEdge<TemplateDeclaration>> = ArrayList()

    var templates by PropertyEdgeDelegate(RecordDeclaration::templateEdges)

    /** The list of statements. */
    @Relationship(value = "STATEMENTS", direction = Relationship.Direction.OUTGOING)
    @field:SubGraph("AST")
    override var statementEdges: MutableList<PropertyEdge<Statement>> = ArrayList()

    override var statements by PropertyEdgeDelegate(RecordDeclaration::statementEdges)

    @Transient var superClasses: MutableList<Type> = ArrayList()

    /**
     * Interfaces implemented by this class. This concept is not present in C++
     *
     * @return the list of implemented interfaces
     */
    @Transient var implementedInterfaces: List<Type> = ArrayList()

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
        get() =
            Stream.of(superClasses, implementedInterfaces)
                .flatMap { obj: List<Type> -> obj.stream() }
                .collect(Collectors.toList())

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

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o !is RecordDeclaration) return false
        return super.equals(o) &&
            kind == o.kind &&
            fields == o.fields &&
            propertyEqualsList(fieldEdges, o.fieldEdges) &&
            methods == o.methods &&
            propertyEqualsList(methodEdges, o.methodEdges) &&
            constructors == o.constructors &&
            propertyEqualsList(constructorEdges, o.constructorEdges) &&
            records == o.records &&
            propertyEqualsList(recordEdges, o.recordEdges) &&
            superClasses == o.superClasses &&
            implementedInterfaces == o.implementedInterfaces &&
            superTypeDeclarations == o.superTypeDeclarations
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
        val type = TypeParser.createFrom(name, language)
        if (type is ObjectType) {
            // as a shortcut, directly set the record declaration. This will be otherwise done
            // later by a pass, but for some frontends we need this immediately, so we set
            // this here.
            type.recordDeclaration = this
        }
        return type
    }

}
