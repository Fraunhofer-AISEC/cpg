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
import de.fraunhofer.aisec.cpg.graph.edges.ast.astEdgeOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import java.util.Objects
import org.neo4j.ogm.annotation.Relationship

class NamedDeconstruction : Deconstruction(), ArgumentHolder {

    @Relationship("MEMBER") var memberEdge = astEdgeOf<Expression>(ProblemExpression("missing key"))

    /**
     * A member of the object, that is identified by `member` is decomposed from the main object.
     */
    var member by unwrapping(NamedDeconstruction::memberEdge)

    @Relationship("VALUE") var valueEdge = astEdgeOf<Expression>(ProblemExpression("missing value"))

    /**
     * The value that is decomposed into, i.e. a variable the named member is bound to, or further
     * decompositions.
     */
    var value by unwrapping(NamedDeconstruction::valueEdge)

    override fun addArgument(expression: Expression) {
        if (member is ProblemExpression) {
            member = expression
        } else if (value is ProblemExpression) {
            value = expression
        }
    }

    override fun replaceArgument(old: Expression, new: Expression): Boolean {
        if (member == old) {
            member = new
            return true
        } else if (value == old) {
            value = new
            return true
        }

        return false
    }

    override fun hasArgument(expression: Expression): Boolean {
        return member == expression || value == expression
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KeyValue) return false
        return super.equals(other) && member == other.key && value == other.value
    }

    override fun hashCode() = Objects.hash(super.hashCode(), member, value)
}
