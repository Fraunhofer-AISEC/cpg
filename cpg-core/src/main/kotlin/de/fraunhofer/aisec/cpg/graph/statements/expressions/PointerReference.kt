/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.statements.expressions

import de.fraunhofer.aisec.cpg.graph.edges.ast.astEdgeOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.graph.pointer
import de.fraunhofer.aisec.cpg.graph.types.HasType
import de.fraunhofer.aisec.cpg.graph.types.Type
import org.neo4j.ogm.annotation.Relationship

/** A c-style reference, such as &i. */
open class PointerReference : Reference() {
    @Relationship("INPUT")
    var inputEdge =
        astEdgeOf<Expression>(
            of = ProblemExpression("could not parse input"),
            onChanged = ::exchangeTypeObserverWithAccessPropagation,
        )
    /** The expression on which the operation is applied. */
    var input by unwrapping(PointerReference::inputEdge)

    override fun typeChanged(newType: Type, src: HasType) {
        // Only accept type changes from out input
        if (src != input) {
            return
        }

        this.type = newType.pointer()
    }

    override fun assignedTypeChanged(assignedTypes: Set<Type>, src: HasType) {
        // Only accept type changes from out input
        if (src != input) {
            return
        }

        // Apply our operator to all assigned types and forward them to us
        this.addAssignedTypes(assignedTypes.map { it.pointer() }.toSet())
    }
}
