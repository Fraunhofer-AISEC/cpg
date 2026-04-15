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
import java.util.Objects
import org.neo4j.ogm.annotation.Relationship

/**
 * Deconstructs an object of a specified type, if the [components] are [NamedDeconstruction], the
 * name will define how deconstruction is done, i.e. data flows based on names, if not it will be
 * done based on position.
 */
class ObjectDeconstruction : Deconstruction(), ArgumentHolder, HasType.TypeObserver {
    @Relationship("COMPONENTS") var componentEdges = astEdgesOf<Expression>()
    var components by unwrapping(ObjectDeconstruction::componentEdges)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ObjectDeconstruction) return false
        return super.equals(other) && components == other.components
    }

    override fun hashCode() = Objects.hash(super.hashCode(), components)

    override fun addArgument(expression: Expression) {
        this.components += expression
        expression.access = this.access
    }

    override fun replaceArgument(old: Expression, new: Expression): Boolean {
        val idx = componentEdges.indexOfFirst { it.end == old }
        if (idx != -1) {
            old.unregisterTypeObserver(this)
            componentEdges[idx].end = new
            new.registerTypeObserver(this)
            new.access = this.access
            return true
        }

        return false
    }

    override fun hasArgument(expression: Expression): Boolean {
        return expression in this.components
    }

    override fun typeChanged(newType: Type, src: HasType) {
        val type = type
        // Todo if my type changes i need to forward these changes to my `children`. Here Type
        // deconstruction
        // works inversely to expression evaluation.
    }

    override fun assignedTypeChanged(assignedTypes: Set<Type>, src: HasType) {
        addAssignedTypes(assignedTypes)
        // Todo if my type changes i need to forward these changes to my `children`. Here Type
        // deconstruction
        // works inversely to expression evaluation.
    }
}
