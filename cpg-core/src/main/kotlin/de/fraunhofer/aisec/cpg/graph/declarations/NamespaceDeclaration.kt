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
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdgeDelegate
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import java.util.Objects
import org.neo4j.ogm.annotation.Relationship

/**
 * Declares the scope of a namespace and appends its own name to the current namespace-prefix to
 * form a new namespace prefix. While RecordDeclarations in C++ and Java have their own namespace,
 * namespace declarations can be declared multiple times. At the beginning of a Java-file, a
 * namespace declaration is used to represent the package name as namespace. In its explicit
 * appearance a namespace declaration can contain [FunctionDeclaration] and [RecordDeclaration]
 * similar to a [RecordDeclaration] and the semantic difference between NamespaceDeclaration and
 * [RecordDeclaration] lies in the non-instantiability of a namespace.
 *
 * The name property of this node need to be a FQN for property resolution.
 */
class NamespaceDeclaration : Declaration(), DeclarationHolder, StatementHolder {
    /**
     * Edges to nested namespaces, records, functions, fields etc. contained in the current
     * namespace.
     */
    @field:SubGraph("AST") override val declarations: MutableList<Declaration> = ArrayList()

    /** The list of statements. */
    @Relationship(value = "STATEMENTS", direction = Relationship.Direction.OUTGOING)
    @field:SubGraph("AST")
    override var statementEdges: MutableList<PropertyEdge<Statement>> = ArrayList()

    /**
     * Returns a non-null, possibly empty `Set` of the declaration of a specified type and clazz.
     *
     * @param name the name to search for
     * @param clazz the declaration class, such as [FunctionDeclaration].
     * @param <T> the type of the declaration
     * @return a `Set` containing the declarations, if any. </T>
     */
    fun <T : Declaration> getDeclarationsByName(name: String, clazz: Class<T>): Set<T> {
        return declarations.filterIsInstance(clazz).filter { it.name.toString() == name }.toSet()
    }

    fun <T> getDeclarationAs(i: Int, clazz: Class<T>): T {
        return clazz.cast(declarations[i])
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NamespaceDeclaration) return false
        return super.equals(other) && declarations == other.declarations
    }

    override fun hashCode() = Objects.hash(super.hashCode(), declarations)

    override fun addDeclaration(declaration: Declaration) {
        addIfNotContains(declarations, declaration)
    }

    override var statements: List<Statement> by
        PropertyEdgeDelegate(NamespaceDeclaration::statementEdges)

    override fun addStatement(s: Statement) = super.addStatement(s)

    override fun <T : Declaration> addIfNotContains(
        collection: MutableCollection<T>,
        declaration: T
    ) = super.addIfNotContains(collection, declaration)

    override fun <T : Node> addIfNotContains(
        collection: MutableCollection<PropertyEdge<T>>,
        declaration: T
    ) = super.addIfNotContains(collection, declaration)

    override fun <T : Node> addIfNotContains(
        collection: MutableCollection<PropertyEdge<T>>,
        declaration: T,
        outgoing: Boolean
    ) = super.addIfNotContains(collection, declaration, outgoing)
}
