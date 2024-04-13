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

import de.fraunhofer.aisec.cpg.graph.AST
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.Companion.propertyEqualsList
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdgeDelegate
import java.util.Objects
import org.neo4j.ogm.annotation.Relationship

/** This declaration represents either an include or an import, depending on the language. */
class IncludeDeclaration : Declaration() {
    @Relationship(value = "INCLUDES", direction = Relationship.Direction.OUTGOING)
    @AST
    private val includeEdges: MutableList<PropertyEdge<IncludeDeclaration>> = ArrayList()

    @Relationship(value = "PROBLEMS", direction = Relationship.Direction.OUTGOING)
    @AST
    private val problemEdges: MutableList<PropertyEdge<ProblemDeclaration>> = ArrayList()

    /**
     * This property refers to the file or directory or path. For example, in C this refers to an
     * include header file. In Go, this refers to the package path (e.g., github.com/a/b)
     */
    var filename: String? = null

    val includes: List<IncludeDeclaration> by PropertyEdgeDelegate(IncludeDeclaration::includeEdges)

    val problems: List<ProblemDeclaration> by PropertyEdgeDelegate(IncludeDeclaration::problemEdges)

    fun addInclude(includeDeclaration: IncludeDeclaration?) {
        if (includeDeclaration == null) return
        val propertyEdge = PropertyEdge(this, includeDeclaration)
        propertyEdge.addProperty(Properties.INDEX, includeEdges.size)
        includeEdges.add(propertyEdge)
    }

    fun addProblems(c: Collection<ProblemDeclaration>) {
        for (problemDeclaration in c) {
            addProblem(problemDeclaration)
        }
    }

    fun addProblem(problemDeclaration: ProblemDeclaration) {
        val propertyEdge = PropertyEdge(this, problemDeclaration)
        propertyEdge.addProperty(Properties.INDEX, problemEdges.size)
        problemEdges.add(propertyEdge)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IncludeDeclaration) return false
        return ((super.equals(other) &&
            includes == other.includes &&
            propertyEqualsList(includeEdges, other.includeEdges) &&
            problems == other.problems) &&
            propertyEqualsList(problemEdges, other.problemEdges) &&
            filename == other.filename)
    }

    override fun hashCode() = Objects.hash(super.hashCode(), includes, problems, filename)
}
