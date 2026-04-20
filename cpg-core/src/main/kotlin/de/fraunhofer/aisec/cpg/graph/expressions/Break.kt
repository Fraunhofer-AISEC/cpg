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
package de.fraunhofer.aisec.cpg.graph.expressions

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.edges.ast.astEdgeOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.graph.types.HasType
import de.fraunhofer.aisec.cpg.graph.types.Type
import java.util.Objects
import org.neo4j.ogm.annotation.Relationship

/**
 * Expression used to interrupt further execution of a loop body and exit the respective loop
 * context. Can have a loop label, e.g. in Java, to specify which of the nested loops should be
 * broken out of.
 */
class Break : Expression(false), HasType.TypeObserver {

    /** Specifies the label of the loop in a nested structure that this statement will 'break' */
    var label: String? = null

    @Relationship("EXPR")
    var exprEdge =
        astEdgeOf<Expression>(
            of = ProblemExpression("could not parse break Expression"),
            onChanged = { old, new -> exchangeTypeObserverWithAccessPropagation(old, new) },
        )
    /** The expression on which the operation is applied. */
    var expr by unwrapping(Break::exprEdge)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Break) return false
        return super.equals(other) && label == other.label && expr == other.expr
    }

    override fun hashCode() = Objects.hash(super.hashCode(), label, expr)

    override fun getStartingPrevEOG(): Collection<Node> {
        return this.prevEOG
    }

    override fun typeChanged(newType: Type, src: HasType) {
        if (src != expr) {
            return
        }

        this.type = newType
    }

    override fun assignedTypeChanged(assignedTypes: Set<Type>, src: HasType) {
        // Only accept type changes from out input
        if (src != expr) {
            return
        }

        // Apply our operator to all assigned types and forward them to us
        this.addAssignedTypes(assignedTypes)
    }
}
