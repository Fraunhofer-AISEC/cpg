/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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

import de.fraunhofer.aisec.cpg.graph.AST
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.Companion.propertyEqualsList
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdgeDelegate
import java.util.*
import kotlin.collections.ArrayList
import org.neo4j.ogm.annotation.Relationship

/** Node representing a declaration of a ClassTemplate */
class ClassTemplateDeclaration : TemplateDeclaration() {
    /**
     * Edges pointing to all RecordDeclarations that are realized by the ClassTempalte. Before the
     * expansion pass there is only a single RecordDeclaration which is instantiated after the
     * expansion pass for each instantiation of the ClassTemplate there will be a realization
     */
    @Relationship(value = "REALIZATION", direction = Relationship.Direction.OUTGOING)
    @AST
    val realizationEdges: MutableList<PropertyEdge<RecordDeclaration>> = ArrayList()

    override val realizations: List<Declaration> by
        PropertyEdgeDelegate(ClassTemplateDeclaration::realizationEdges)

    fun addRealization(realizedRecord: RecordDeclaration) {
        val propertyEdge = PropertyEdge(this, realizedRecord)
        propertyEdge.addProperty(Properties.INDEX, realizationEdges.size)
        realizationEdges.add(propertyEdge)
    }

    fun removeRealization(realizedRecordDeclaration: RecordDeclaration) {
        realizationEdges.removeIf { it.end == realizedRecordDeclaration }
    }

    override fun addDeclaration(declaration: Declaration) {
        if (declaration is TypeParamDeclaration || declaration is ParamVariableDeclaration) {
            addIfNotContains(super.parameterEdges, declaration)
        } else if (declaration is RecordDeclaration) {
            addIfNotContains(realizationEdges, declaration)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        if (!super.equals(other)) return false
        val that = other as ClassTemplateDeclaration
        return realizations == that.realizations &&
            propertyEqualsList(realizationEdges, that.realizationEdges) &&
            parameters == that.parameters &&
            propertyEqualsList(parameterEdges, that.parameterEdges)
    }

    // Do NOT add parameters to hashcode, as they are added incrementally to the list. If the
    // parameters field is added, the ScopeManager is not able to find it anymore and we cannot
    // leave the TemplateScope. Analogous for realization
    override fun hashCode() = Objects.hash(super.hashCode())
}
