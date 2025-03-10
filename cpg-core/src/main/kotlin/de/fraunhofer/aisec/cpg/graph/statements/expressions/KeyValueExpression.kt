/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
import de.fraunhofer.aisec.cpg.graph.ArgumentHolder
import de.fraunhofer.aisec.cpg.graph.edges.ast.astEdgeOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import java.util.*
import org.neo4j.ogm.annotation.Relationship

/**
 * Represents a key / value pair, often found in languages that allow associative arrays or objects,
 * such as Python, Golang or JavaScript.
 *
 * Most often used in combination with an [InitializerListExpression] to represent the creation of
 * an array.
 */
class KeyValueExpression internal constructor(ctx: TranslationContext) :
    Expression(ctx), ArgumentHolder {

    @Relationship("KEY") var keyEdge = astEdgeOf<Expression>(ProblemExpression(ctx, "missing key"))
    /**
     * The key of this pair. It is usually a literal, but some languages even allow references to
     * variables as a key.
     */
    var key by unwrapping(KeyValueExpression::keyEdge)

    @Relationship("VALUE")
    var valueEdge = astEdgeOf<Expression>(ProblemExpression(ctx, "missing value"))

    /** The value of this pair. It can be any expression */
    var value by unwrapping(KeyValueExpression::valueEdge)

    override fun addArgument(expression: Expression) {
        if (key is ProblemExpression) {
            key = expression
        } else if (value is ProblemExpression) {
            value = expression
        }
    }

    override fun replaceArgument(old: Expression, new: Expression): Boolean {
        if (key == old) {
            key = new
            return true
        } else if (value == old) {
            value = new
            return true
        }

        return false
    }

    override fun hasArgument(expression: Expression): Boolean {
        return key == expression || value == expression
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KeyValueExpression) return false
        return super.equals(other) && key == other.key && value == other.value
    }

    override fun hashCode() = Objects.hash(super.hashCode(), key, value)
}
