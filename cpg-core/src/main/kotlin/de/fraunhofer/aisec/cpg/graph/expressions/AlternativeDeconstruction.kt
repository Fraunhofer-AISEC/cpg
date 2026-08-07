/*
 * Copyright (c) 2026, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.expressions

import de.fraunhofer.aisec.cpg.graph.ArgumentHolder
import de.fraunhofer.aisec.cpg.graph.edges.ast.astEdgesOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.graph.types.HasType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.persistence.Relationship
import java.util.Objects
import kotlin.collections.plusAssign

class AlternativeDeconstruction : Deconstruction(), ArgumentHolder, HasType.TypeObserver {

    @Relationship("ALTERNATIVES") var alternativeEdges = astEdgesOf<Expression>()
    var alternatives by unwrapping(AlternativeDeconstruction::alternativeEdges)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AlternativeDeconstruction) return false
        return super.equals(other) && alternatives == other.alternatives
    }

    override fun hashCode() = Objects.hash(super.hashCode(), alternatives)

    override fun addArgument(expression: Expression) {
        this.alternatives += expression
        expression.access = this.access
    }

    override fun replaceArgument(old: Expression, new: Expression): Boolean {
        val idx = alternativeEdges.indexOfFirst { it.end == old }
        if (idx != -1) {
            old.unregisterTypeObserver(this)
            alternativeEdges[idx].end = new
            new.registerTypeObserver(this)
            new.access = this.access
            return true
        }

        return false
    }

    override fun hasArgument(expression: Expression): Boolean {
        return expression in this.alternatives
    }

    override fun typeChanged(newType: Type, src: HasType) {
        val type = type
        typeObservers.forEach { it.typeChanged(type, this) }
    }

    override fun assignedTypeChanged(assignedTypes: Set<Type>, src: HasType) {
        addAssignedTypes(assignedTypes)
        typeObservers.forEach { it.assignedTypeChanged(assignedTypes, this) }
    }
}
