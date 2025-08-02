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
package de.fraunhofer.aisec.cpg.graph.ast.declarations

import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.ast.DeclarationHolder
import de.fraunhofer.aisec.cpg.graph.ast.statements.Statement
import de.fraunhofer.aisec.cpg.graph.edges.ast.astEdgesOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.persistence.DoNotPersist
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
class NamespaceDeclaration : Declaration(), DeclarationHolder, StatementHolder, EOGStarterHolder {
    /**
     * Edges to nested namespaces, records, functions, fields etc. contained in the current
     * namespace.
     */
    val declarationEdges = astEdgesOf<Declaration>()
    override val declarations by unwrapping(NamespaceDeclaration::declarationEdges)

    /** The list of statements. */
    @Relationship(value = "STATEMENTS", direction = Relationship.Direction.OUTGOING)
    override var statementEdges = astEdgesOf<Statement>()

    /**
     * In some languages, there is a relationship between paths / directories and the package
     * structure. Therefore, we need to be aware of the path this namespace / package is in.
     */
    var path: String? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NamespaceDeclaration) return false
        return super.equals(other) && declarations == other.declarations
    }

    override fun hashCode() = Objects.hash(super.hashCode(), declarations)

    override fun addDeclaration(declaration: Declaration) {
        addIfNotContains(declarations, declaration)
    }

    override var statements by unwrapping(NamespaceDeclaration::statementEdges)

    @DoNotPersist
    override val eogStarters: List<Node>
        get() {
            val list = mutableListOf<Node>()
            // Add all top-level declarations
            list += declarations
            // Add all top-level statements
            list += statements

            return list
        }

    override fun getStartingPrevEOG(): Collection<Node> {
        return setOf()
    }

    override fun getExitNextEOG(): Collection<Node> {
        return setOf()
    }
}
