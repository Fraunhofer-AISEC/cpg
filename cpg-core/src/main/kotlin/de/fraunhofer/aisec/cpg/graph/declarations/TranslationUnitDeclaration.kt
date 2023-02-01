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
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.Companion.unwrap
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import java.util.Objects
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/** The top most declaration, representing a translation unit, for example a file. */
class TranslationUnitDeclaration : Declaration(), DeclarationHolder, StatementHolder {
    /** A list of declarations within this unit. */
    @Relationship(value = "DECLARATIONS", direction = Relationship.Direction.OUTGOING)
    @field:SubGraph("AST")
    val declarationEdges: MutableList<PropertyEdge<Declaration>> = ArrayList()

    /** A list of includes within this unit. */
    @Relationship(value = "INCLUDES", direction = Relationship.Direction.OUTGOING)
    @field:SubGraph("AST")
    val includeEdges: MutableList<PropertyEdge<IncludeDeclaration>> = ArrayList()

    /** A list of namespaces within this unit. */
    @Relationship(value = "NAMESPACES", direction = Relationship.Direction.OUTGOING)
    @field:SubGraph("AST")
    val namespaceEdges: MutableList<PropertyEdge<NamespaceDeclaration>> = ArrayList()

    /** The list of statements. */
    @Relationship(value = "STATEMENTS", direction = Relationship.Direction.OUTGOING)
    @field:SubGraph("AST")
    override var statementEdges: MutableList<PropertyEdge<Statement>> = ArrayList()

    override val declarations: List<Declaration>
        get() = unwrap(declarationEdges)

    override var statements: List<Statement>
        get() = unwrap(statementEdges)
        set(value) {
            statementEdges = PropertyEdge.transformIntoOutgoingPropertyEdgeList(value, this as Node)
        }

    val includes: List<IncludeDeclaration>
        get() = unwrap(includeEdges)

    val namespaces: List<NamespaceDeclaration>
        get() = unwrap(namespaceEdges)

    /**
     * Returns the i-th declaration as a specific class, if it can be cast
     *
     * @param i the index
     * @param clazz the class
     * @param <T> the type of the class
     * @return the declaration or null, if it can not be cast to the class </T>
     */
    fun <T : Declaration?> getDeclarationAs(i: Int, clazz: Class<T>): T? {
        val declaration = declarationEdges[i].end
        return if (declaration.javaClass.isAssignableFrom(clazz))
            clazz.cast(declarationEdges[i].end)
        else null
    }

    /**
     * Returns a non-null, possibly empty `Set` of the declaration of a specified type and clazz.
     *
     * The set may contain more than one element if a declaration exists in the [ ] itself and in an
     * included header file.
     *
     * @param name the name to search for
     * @param clazz the declaration class, such as [FunctionDeclaration].
     * @param <T> the type of the declaration
     * @return a `Set` containing the declarations, if any. </T>
     */
    fun <T : Declaration?> getDeclarationsByName(name: String, clazz: Class<T>): Set<T> {
        return declarationEdges
            .map { it.end }
            .filter { it.name.toString() == name }
            .filterIsInstance(clazz)
            .toSet()
    }

    fun getIncludeByName(name: String): IncludeDeclaration? {
        return includeEdges.map { it.end }.firstOrNull { it.name.toString() == name }
    }

    override fun addDeclaration(declaration: Declaration) {
        if (declaration is IncludeDeclaration) {
            addIfNotContains(includeEdges, declaration)
        } else if (declaration is NamespaceDeclaration) {
            addIfNotContains(namespaceEdges, declaration)
        }
        addIfNotContains(declarationEdges, declaration)
    }

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .append("declarations", declarationEdges)
            .append("includes", includeEdges)
            .append("namespaces", namespaceEdges)
            .toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TranslationUnitDeclaration) return false
        // TODO: This statement doesn't make sense to me. The declarationsPropertyEdge comparison is
        // more strict than the propertyEqualsList() isn't it?
        return super.equals(other) &&
            declarations == other.declarations &&
            propertyEqualsList(declarationEdges, other.declarationEdges) &&
            includes == other.includes &&
            propertyEqualsList(includeEdges, other.includeEdges) &&
            namespaces == other.namespaces &&
            propertyEqualsList(namespaceEdges, other.namespaceEdges)
    }

    override fun hashCode() = Objects.hash(super.hashCode(), includes, namespaces, declarations)

    override fun addStatement(s: Statement) {
        super.addStatement(s)
    }

    override fun <T : Declaration> addIfNotContains(
        collection: MutableCollection<T>,
        declaration: T
    ) {
        super.addIfNotContains<T>(collection, declaration)
    }

    override fun <T : Node> addIfNotContains(
        collection: MutableCollection<PropertyEdge<T>>,
        declaration: T
    ) {
        super.addIfNotContains<T>(collection, declaration)
    }

    override fun <T : Node> addIfNotContains(
        collection: MutableCollection<PropertyEdge<T>>,
        declaration: T,
        outgoing: Boolean
    ) {
        super.addIfNotContains<T>(collection, declaration, outgoing)
    }
}
