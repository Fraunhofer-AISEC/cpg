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
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.Companion.propertyEqualsList
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdgeDelegate
import java.util.Objects
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/**
 * A [Statement], which contains a single or multiple [Declaration]s. Usually these statements occur
 * if one defines a variable within a function body. A function body is a [ ], which can only
 * contain other statements, but not declarations. Therefore, declarations are wrapped in a
 * [DeclarationStatement].
 */
open class DeclarationStatement : Statement() {
    /**
     * The list of declarations declared or defined by this statement. It is always a list, even if
     * it only contains a single [Declaration].
     */
    @Relationship(value = "DECLARATIONS", direction = Relationship.Direction.OUTGOING)
    @AST
    var declarationEdges: MutableList<PropertyEdge<Declaration>> = ArrayList()

    override var declarations by PropertyEdgeDelegate(DeclarationStatement::declarationEdges)

    var singleDeclaration: Declaration?
        get() = if (isSingleDeclaration()) declarationEdges[0].end else null
        set(value) {
            if (value == null) return
            declarationEdges.clear()
            val propertyEdge = PropertyEdge(this, value)
            propertyEdge.addProperty(Properties.INDEX, 0)
            declarationEdges.add(propertyEdge)
        }

    fun isSingleDeclaration(): Boolean {
        return declarationEdges.size == 1
    }

    fun <T : Declaration> getSingleDeclarationAs(clazz: Class<T>): T {
        return clazz.cast(singleDeclaration)
    }

    fun addToPropertyEdgeDeclaration(declaration: Declaration) {
        val propertyEdge = PropertyEdge(this, declaration)
        propertyEdge.addProperty(Properties.INDEX, declarationEdges.size)
        declarationEdges.add(propertyEdge)
    }

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("declarations", declarations)
            .toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DeclarationStatement) return false
        return super.equals(other) &&
            declarations == other.declarations &&
            propertyEqualsList(declarationEdges, other.declarationEdges)
    }

    override fun hashCode() = Objects.hash(super.hashCode(), declarations)
}
