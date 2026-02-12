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
package de.fraunhofer.aisec.cpg.graph.declarations

import de.fraunhofer.aisec.cpg.graph.ArgumentHolder
import de.fraunhofer.aisec.cpg.graph.HasDefault
import de.fraunhofer.aisec.cpg.graph.edges.ast.astOptionalEdgeOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import java.util.*
import org.neo4j.ogm.annotation.Relationship

/** A declaration of a function or non-type template parameter. */
class ParameterDeclaration : ValueDeclaration(), HasDefault<Expression?>, ArgumentHolder {

    var isVariadic = false

    @Relationship(value = "DEFAULT", direction = Relationship.Direction.OUTGOING)
    var defaultValueEdge = astOptionalEdgeOf<Expression>()
    private var defaultValue by unwrapping(ParameterDeclaration::defaultValueEdge)

    var modifiers: List<String> = mutableListOf()

    override var default: Expression?
        get() = defaultValue
        set(value) {
            defaultValue = value
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ParameterDeclaration) return false
        return super.equals(other) &&
            isVariadic == other.isVariadic &&
            defaultValue == other.defaultValue
    }

    override fun hashCode() = Objects.hash(super.hashCode(), isVariadic, defaultValue)

    override fun addArgument(expression: Expression) {
        if (defaultValue == null) {
            defaultValue = expression
        }
    }

    override fun replaceArgument(old: Expression, new: Expression): Boolean {
        if (defaultValue == old) {
            defaultValue = new
            return true
        }

        return false
    }

    override fun hasArgument(expression: Expression): Boolean {
        return defaultValue == expression
    }
}
