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
import de.fraunhofer.aisec.cpg.graph.edge.AstChildren
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.Companion.propertyEqualsList
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdgeDelegate
import java.util.Objects
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/** This declaration represents either an include or an import, depending on the language. */
class IncludeDeclaration : Declaration() {
    @Relationship(value = "INCLUDES", direction = Relationship.Direction.OUTGOING)
    @AST
    private val includeEdges = AstChildren<IncludeDeclaration>()

    val includes: List<IncludeDeclaration> by PropertyEdgeDelegate(IncludeDeclaration::includeEdges)

    @Relationship(value = "PROBLEMS", direction = Relationship.Direction.OUTGOING)
    @AST
    private val problemEdges = AstChildren<ProblemDeclaration>()

    val problems: List<ProblemDeclaration> by PropertyEdgeDelegate(IncludeDeclaration::problemEdges)

    /**
     * This property refers to the file or directory or path. For example, in C this refers to an
     * include header file. In Go, this refers to the package path (e.g., github.com/a/b)
     */
    var filename: String? = null

    fun addInclude(includeDeclaration: IncludeDeclaration?) {
        if (includeDeclaration == null) return
        includeEdges.add(this, includeDeclaration)
    }

    fun addProblems(c: Collection<ProblemDeclaration>) {
        for (problemDeclaration in c) {
            addProblem(problemDeclaration)
        }
    }

    fun addProblem(problemDeclaration: ProblemDeclaration) {
        problemEdges.add(this, problemDeclaration)
    }

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("name", name)
            .append("filename", filename)
            .append("includes", includes)
            .append("problems", problems)
            .toString()
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

    override fun hashCode() = Objects.hash(super.hashCode(), problems, filename)
}
