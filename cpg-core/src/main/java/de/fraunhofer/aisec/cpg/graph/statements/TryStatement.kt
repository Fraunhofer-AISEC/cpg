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

import de.fraunhofer.aisec.cpg.graph.SubGraph
import de.fraunhofer.aisec.cpg.graph.edge.Properties
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.Companion.propertyEqualsList
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge.Companion.unwrap
import org.neo4j.ogm.annotation.Relationship

class TryStatement : Statement() {
    @Relationship(value = "RESOURCES", direction = Relationship.Direction.OUTGOING)
    @field:SubGraph("AST")
    private var _resources: MutableList<PropertyEdge<Statement>> = mutableListOf()

    @field:SubGraph("AST") var tryBlock: CompoundStatement? = null

    @field:SubGraph("AST") var finallyBlock: CompoundStatement? = null

    @Relationship(value = "CATCH_CLAUSES", direction = Relationship.Direction.OUTGOING)
    @field:SubGraph("AST")
    private var _catchClauses: MutableList<PropertyEdge<CatchClause>> = mutableListOf()

    var catchClauses: List<CatchClause>
        get() = unwrap(_catchClauses)
        set(value) {
            _catchClauses = mutableListOf()
            for ((counter, c) in value.withIndex()) {
                val propertyEdge = PropertyEdge(this, c)
                propertyEdge.addProperty(Properties.INDEX, counter)
                _catchClauses.add(propertyEdge)
            }
        }

    var resources: List<Statement>
        get() = unwrap(_resources)
        set(value) {
            this._resources = mutableListOf()
            for ((c, s) in value.withIndex()) {
                val propertyEdge = PropertyEdge(this, s)
                propertyEdge.addProperty(Properties.INDEX, c)
                _resources.add(propertyEdge)
            }
        }

    val resourcesPropertyEdge: List<PropertyEdge<Statement>>
        get() = _resources

    val catchClausesPropertyEdge: List<PropertyEdge<CatchClause>>
        get() = _catchClauses

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TryStatement) return false
        return ((super.equals(other) &&
            resources == other.resources &&
            propertyEqualsList(_resources, other._resources) &&
            tryBlock == other.tryBlock) &&
            finallyBlock == other.finallyBlock &&
            catchClauses == other.catchClauses &&
            propertyEqualsList(_catchClauses, other._catchClauses))
    }

    override fun hashCode() = super.hashCode()
}
