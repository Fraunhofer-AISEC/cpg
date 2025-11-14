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
package de.fraunhofer.aisec.cpg.graph.ast.statements

import de.fraunhofer.aisec.cpg.graph.EOGStarterHolder
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.ast.BranchingNode
import de.fraunhofer.aisec.cpg.graph.ast.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.ast.statements.expressions.Block
import de.fraunhofer.aisec.cpg.graph.edges.ast.astOptionalEdgeOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.persistence.DoNotPersist
import java.util.Objects
import org.neo4j.ogm.annotation.Relationship

class CatchClause : Statement(), BranchingNode, EOGStarterHolder {
    @Relationship(value = "PARAMETER") var parameterEdge = astOptionalEdgeOf<VariableDeclaration>()

    var parameter by unwrapping(CatchClause::parameterEdge)

    @Relationship(value = "BODY") var bodyEdge = astOptionalEdgeOf<Block>()
    var body by unwrapping(CatchClause::bodyEdge)

    @DoNotPersist
    override val branchedBy
        get() = parameter

    @DoNotPersist
    override val eogStarters: List<Node>
        get() = listOf(this)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CatchClause) return false
        return super.equals(other) && parameter == other.parameter && body == other.body
    }

    override fun hashCode() = Objects.hash(super.hashCode(), parameter, body)
}
