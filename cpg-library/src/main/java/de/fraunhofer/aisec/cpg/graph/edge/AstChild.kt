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
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import org.neo4j.ogm.annotation.RelationshipEntity

/** Represents a child node in the Abstract Syntax Tree. */
@RelationshipEntity(type = "AST")
open class AstChild<T : Node>(start: Node, end: T) : PropertyEdge<T>(start, end) {

    init {
        // Since this is an AST property FROM <start> to <end>, we can set <start> as the parent of
        // <end>. This will allow for easier traversal of the in-memory structures.
        end.parent = start
    }
}

/** Represents the body, e.g. of a function. */
@RelationshipEntity
class Body(start: FunctionDeclaration, end: Statement) : AstChild<Statement>(start, end)

/** Represents an expression used as an initializer, used in field and variable declarations. */
@RelationshipEntity
class Initializer(start: Declaration, end: Expression) : AstChild<Expression>(start, end)
