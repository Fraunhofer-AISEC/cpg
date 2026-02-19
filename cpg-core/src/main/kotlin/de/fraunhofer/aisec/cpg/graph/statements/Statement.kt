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
package de.fraunhofer.aisec.cpg.graph.statements

import de.fraunhofer.aisec.cpg.graph.AstNode
import de.fraunhofer.aisec.cpg.graph.DeclarationHolder
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.ValueDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.Variable
import de.fraunhofer.aisec.cpg.graph.edges.Edge.Companion.propertyEqualsList
import de.fraunhofer.aisec.cpg.graph.edges.ast.astEdgesOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import java.util.*
import org.neo4j.ogm.annotation.NodeEntity
import org.neo4j.ogm.annotation.Relationship

/**
 * This [Node] is the most basic node type that represents source code elements which represents
 * executable code.
 */
@NodeEntity
abstract class Statement : AstNode(), DeclarationHolder {
    /**
     * A list of local variables (or other values) associated to this statement, defined by their
     * [ValueDeclaration] extracted from Block because `for`, `while`, `if`, and `switch` can
     * declare locals in their condition or initializers.
     *
     * TODO: This is actually an AST node just for a subset of nodes, i.e. initializers in for-loops
     */
    @Relationship(value = "LOCALS", direction = Relationship.Direction.OUTGOING)
    var localEdges = astEdgesOf<ValueDeclaration>()

    /** Virtual property to access [localEdges] without property edges. */
    var locals by unwrapping(Statement::localEdges)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Statement) return false
        return super.equals(other) &&
            locals == other.locals &&
            propertyEqualsList(localEdges, other.localEdges)
    }

    override fun hashCode() = Objects.hash(super.hashCode(), locals)

    override fun addDeclaration(declaration: Declaration) {
        if (declaration is Variable) {
            addIfNotContains(localEdges, declaration)
        } else if (declaration is FunctionDeclaration) {
            addIfNotContains(localEdges, declaration)
        }
    }

    override val declarations: List<Declaration>
        get() = locals
}
