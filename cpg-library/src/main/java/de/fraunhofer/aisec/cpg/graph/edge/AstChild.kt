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
package de.fraunhofer.aisec.cpg.graph.edge

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import org.neo4j.ogm.annotation.Property
import org.neo4j.ogm.annotation.RelationshipEntity

/** Represents a child node in the Abstract Syntax Tree. */
@RelationshipEntity(type = "AST")
open class AstChild<T : Node>(start: Node, end: T) : PropertyEdge<T>(start, end) {

    /** Specifies, that this edge is a relation related to the Abstract Syntax Tree (AST). */
    @Property val ast: Boolean = true

    init {
        // Since this is an AST property FROM <start> to <end>, we can set <start> as the parent of
        // <end>. This will allow for easier traversal of the in-memory structures.
        end.parent = this
    }
}

/** Represents the body, e.g. of a function. */
@RelationshipEntity
class Body(start: Node, statement: Statement) : AstChild<Statement>(start, statement)

/** Represents an expression used as an initializer, used in field and variable declarations. */
@RelationshipEntity
class Initializer(start: Declaration, expression: Expression) :
    AstChild<Expression>(start, expression)

/**
 * Represents an relationship to an expression used as condition, e.g. in asserts, if-statements or
 * other conditional statements
 */
class Condition(start: Node, expression: Expression) : AstChild<Expression>(start, expression) {

    /**
     * Returns a constant value of this condition, if it is possible. This could be extended with a
     * proper constant resolver
     */
    val constantValue: Boolean?
        get() {
            if (end is Literal<*>) {
                return (end as Literal<*>).value as? Boolean
            }

            return null
        }
}
