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

import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.Companion.propertyEqualsList
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdgeDelegate
import java.util.*
import org.neo4j.ogm.annotation.Relationship

/** Node representing a declaration of a FunctionTemplate */
class FunctionTemplateDecl : TemplateDecl() {
    /**
     * Edges pointing to all FunctionDeclarations that are realized by the FunctionTemplate. Before
     * the expansion pass there is only a single FunctionDeclaration which is instantiated After the
     * expansion pass for each instantiation of the FunctionTemplate there will be a realization
     */
    @Relationship(value = "REALIZATION", direction = Relationship.Direction.OUTGOING)
    private val realizationEdges: MutableList<PropertyEdge<FunctionDecl>> = ArrayList()

    val realization: List<FunctionDecl> by
        PropertyEdgeDelegate(FunctionTemplateDecl::realizationEdges)

    override val realizations: List<Declaration>
        get() = ArrayList<Declaration>(realization)

    fun addRealization(realizedFunction: FunctionDecl) {
        val propertyEdge = PropertyEdge(this, realizedFunction)
        propertyEdge.addProperty(Properties.INDEX, realizationEdges.size)
        realizationEdges.add(propertyEdge)
    }

    fun removeRealization(realizedFunction: FunctionDecl?) {
        realizationEdges.removeIf { it.end == realizedFunction }
    }

    override fun addDeclaration(declaration: Declaration) {
        if (declaration is TypeParamDecl || declaration is ParameterDecl) {
            addIfNotContains(this.parameterEdges, declaration)
        } else if (declaration is FunctionDecl) {
            addIfNotContains(realizationEdges, declaration)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        if (!super.equals(other)) return false
        val that = other as FunctionTemplateDecl
        return realization == that.realization &&
            propertyEqualsList(realizationEdges, that.realizationEdges) &&
            parameters == that.parameters &&
            propertyEqualsList(parameterEdges, that.parameterEdges)
    }

    // Do NOT add parameters to hashcode, as they are added incrementally to the list. If the
    // parameters field is added, the ScopeManager is not able to find it anymore and we cannot
    // leave the TemplateScope. Analogous for realization
    override fun hashCode() = Objects.hash(super.hashCode())
}
