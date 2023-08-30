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

import de.fraunhofer.aisec.cpg.graph.AST
import de.fraunhofer.aisec.cpg.graph.DeclarationHolder
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.Companion.propertyEqualsList
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdgeDelegate
import java.util.*
import org.neo4j.ogm.annotation.Relationship

/**
 * This [Node] is the most basic node type that represents source code elements which represents
 * executable code.
 */
abstract class Statement : Node(), DeclarationHolder {
    /**
     * A list of local variables associated to this statement, defined by their [ ] extracted from
     * Block because for, while, if, switch can declare locals in their condition or initializers.
     *
     * TODO: This is actually an AST node just for a subset of nodes, i.e. initializers in for-loops
     */
    @Relationship(value = "LOCALS", direction = Relationship.Direction.OUTGOING)
    @AST
    var localEdges = mutableListOf<PropertyEdge<VariableDeclaration>>()

    /** Virtual property to access [localEdges] without property edges. */
    var locals by PropertyEdgeDelegate(Statement::localEdges)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Statement) return false
        return super.equals(other) &&
            locals == other.locals &&
            propertyEqualsList(localEdges, other.localEdges)
    }

    override fun hashCode() = Objects.hash(super.hashCode(), locals)

    override fun addDeclaration(declaration: Declaration) {
        if (declaration is VariableDeclaration) {
            addIfNotContains(localEdges, declaration)
        }
    }

    override val declarations: List<Declaration>
        get() = locals
}
