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
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdgeDelegate
import org.neo4j.ogm.annotation.Relationship

/**
 * This represents a sequence of one or more declaration(s). The purpose of this node is primarily
 * to bridge between a single declaration and a list of declarations in the front-end handlers. It
 * will be converted into a list-structure and all its children will be added to the parent, i.e.
 * the translation unit. It should NOT end up in the final graph.
 */
class DeclarationSequence : Declaration(), DeclarationHolder {
    @Relationship(value = "CHILDREN", direction = Relationship.Direction.OUTGOING)
    val childrenPropertyEdge: MutableList<PropertyEdge<Declaration>> = mutableListOf()

    val children: List<Declaration> by
        PropertyEdgeDelegate(DeclarationSequence::childrenPropertyEdge)

    override fun addDeclaration(declaration: Declaration) {
        if (declaration is DeclarationSequence) {
            for (declarationChild in declaration.children) {
                addIfNotContains(childrenPropertyEdge, declarationChild)
            }
        }
        addIfNotContains(childrenPropertyEdge, declaration)
    }

    fun asList(): List<Declaration> {
        return children
    }

    val isSingle: Boolean
        get() = childrenPropertyEdge.size == 1

    fun first(): Declaration {
        return childrenPropertyEdge[0].end
    }

    override fun getDeclarations(): List<Declaration> {
        return children
    }
}
