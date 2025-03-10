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
package de.fraunhofer.aisec.cpg.graph.statements.expressions

import de.fraunhofer.aisec.cpg.TranslationContext
import de.fraunhofer.aisec.cpg.graph.AstNode
import de.fraunhofer.aisec.cpg.graph.HasInitializer
import de.fraunhofer.aisec.cpg.graph.edges.ast.astEdgesOf
import de.fraunhofer.aisec.cpg.graph.edges.ast.astOptionalEdgeOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import java.util.Objects
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/** Represents the creation of a new object through the `new` keyword. */
class NewExpression internal constructor(ctx: TranslationContext) :
    Expression(ctx), HasInitializer {
    @Relationship("INITIALIZER") var initializerEdge = astOptionalEdgeOf<Expression>()

    /** The initializer expression. */
    override var initializer by unwrapping(NewExpression::initializerEdge)

    /**
     * We need a way to store the templateParameters that a NewExpression might have before the
     * ConstructExpression is created
     */
    @Relationship(value = "TEMPLATE_PARAMETERS", direction = Relationship.Direction.OUTGOING)
    var templateParameterEdges = astEdgesOf<AstNode>()
    var templateParameters by unwrapping(NewExpression::templateParameterEdges)

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("initializer", initializer)
            .toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NewExpression) return false
        return super.equals(other) && initializer == other.initializer
    }

    override fun hashCode() = Objects.hash(super.hashCode(), initializer)
}
