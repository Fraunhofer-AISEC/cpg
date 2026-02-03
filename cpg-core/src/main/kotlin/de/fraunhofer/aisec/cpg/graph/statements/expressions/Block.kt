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

import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.StatementHolder
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.edges.Edge
import de.fraunhofer.aisec.cpg.graph.edges.ast.astEdgesOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import java.util.Objects
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/**
 * A statement which contains a list of statements. A common example is a function body within a
 * [FunctionDeclaration].
 */
open class Block : Expression(), StatementHolder {
    /** The list of statements. */
    @Relationship(value = "STATEMENTS", direction = Relationship.Direction.OUTGOING)
    override var statementEdges = astEdgesOf<Statement>()
    override var statements by unwrapping(Block::statementEdges)
    /**
     * This variable helps to differentiate between static and non-static initializer blocks. Static
     * initializer blocks are executed when the enclosing declaration is first referred to, e.g.
     * loaded into the jvm or parsed. Non-static initializers are executed on Record construction.
     *
     * If a compound statement is part of a method body, this notion is not relevant.
     */
    var isStaticBlock = false

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE).appendSuper(super.toString()).toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Block) return false
        return super.equals(other) && Edge.propertyEqualsList(statementEdges, other.statementEdges)
    }

    override fun hashCode() = Objects.hash(super.hashCode())

    /** Returns the [n]-th statement in this list of statements. */
    operator fun get(n: Int): Statement {
        return statements[n]
    }

    override fun getStartingPrevEOG(): Collection<Node> {
        return this.statements.firstOrNull()?.getStartingPrevEOG() ?: this.prevEOG
    }
}
