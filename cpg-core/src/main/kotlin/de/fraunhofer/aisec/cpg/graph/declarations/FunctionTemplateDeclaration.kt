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

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.graph.edges.Edge.Companion.propertyEqualsList
import de.fraunhofer.aisec.cpg.graph.edges.ast.astEdgesOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import java.util.*
import org.neo4j.ogm.annotation.Relationship

/** Node representing a declaration of a FunctionTemplate */
class FunctionTemplateDeclaration internal constructor(ctx: TranslationContext) :
    TemplateDeclaration(ctx) {
    /**
     * Edges pointing to all FunctionDeclarations that are realized by the FunctionTemplate. Before
     * the expansion pass there is only a single FunctionDeclaration which is instantiated After the
     * expansion pass for each instantiation of the FunctionTemplate there will be a realization
     */
    @Relationship(value = "REALIZATION", direction = Relationship.Direction.OUTGOING)
    val realizationEdges = astEdgesOf<FunctionDeclaration>()
    val realization by unwrapping(FunctionTemplateDeclaration::realizationEdges)

    override val realizations: List<Declaration>
        get() = ArrayList<Declaration>(realization)

    override fun addDeclaration(declaration: Declaration) {
        if (declaration is TypeParameterDeclaration || declaration is ParameterDeclaration) {
            addIfNotContains(this.parameterEdges, declaration)
        } else if (declaration is FunctionDeclaration) {
            addIfNotContains(realizationEdges, declaration)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        if (!super.equals(other)) return false
        val that = other as FunctionTemplateDeclaration
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
