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

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.edges.ast.astOptionalEdgeOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import java.util.Objects
import org.neo4j.ogm.annotation.Relationship

/** Represents an assert statement */
class AssertStatement : Statement() {
    @Relationship(value = "CONDITION") var conditionEdge = astOptionalEdgeOf<Expression>()
    /** The condition to be evaluated. */
    var condition by unwrapping(AssertStatement::conditionEdge)

    @Relationship(value = "MESSAGE") var messageEdge = astOptionalEdgeOf<Statement>()
    /** The *optional* message that is shown, if the assert is evaluated as true */
    var message by unwrapping(AssertStatement::messageEdge)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AssertStatement) return false
        return super.equals(other) && condition == other.condition && message == other.message
    }

    override fun hashCode() = Objects.hash(super.hashCode(), condition, message)

    override fun getStartingPrevEOG(): Collection<Node> {
        return this.condition?.getStartingPrevEOG()
            ?: (this.prevEOG + (this.message?.getStartingPrevEOG() ?: setOf()))
    }
}
