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

import de.fraunhofer.aisec.cpg.commonType
import de.fraunhofer.aisec.cpg.graph.ArgumentHolder
import de.fraunhofer.aisec.cpg.graph.BranchingNode
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.edges.ast.astOptionalEdgeOf
import de.fraunhofer.aisec.cpg.graph.edges.unwrapping
import de.fraunhofer.aisec.cpg.graph.types.HasType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.unknownType
import java.util.*
import kotlin.collections.ifEmpty
import org.apache.commons.lang3.builder.ToStringBuilder
import org.neo4j.ogm.annotation.Relationship

/** Represents a condition control flow statement, usually indicating by `If`. */
class IfElse : Expression(), BranchingNode, ArgumentHolder, HasType.TypeObserver {

    override var usedAsExpression = false

    @Relationship(value = "INITIALIZER_STATEMENT")
    var initializerStatementEdge = astOptionalEdgeOf<Expression>()
    /** C++ initializer statement. */
    var initializerStatement by unwrapping(IfElse::initializerStatementEdge)

    @Relationship(value = "CONDITION_DECLARATION")
    var conditionDeclarationEdge = astOptionalEdgeOf<Declaration>()
    /** C++ alternative to the condition. */
    var conditionDeclaration by unwrapping(IfElse::conditionDeclarationEdge)

    @Relationship(value = "CONDITION") var conditionEdge = astOptionalEdgeOf<Expression>()
    /** The condition to be evaluated. */
    var condition by unwrapping(IfElse::conditionEdge)

    override val branchedBy
        get() = condition ?: conditionDeclaration

    /** C++ constexpr construct. */
    var isConstExpression = false

    @Relationship(value = "THEN_STATEMENT")
    var thenStatementEdge =
        astOptionalEdgeOf<Expression>(
            onChanged = { old, new ->
                old?.end?.unregisterTypeObserver(this)
                new?.end?.registerTypeObserver(this)
            }
        )
    /** The statement that is executed, if the condition is evaluated as true. Usually a [Block]. */
    var thenStatement by unwrapping(IfElse::thenStatementEdge)

    @Relationship(value = "ELSE_STATEMENT")
    var elseStatementEdge =
        astOptionalEdgeOf<Expression>(
            onChanged = { old, new ->
                old?.end?.unregisterTypeObserver(this)
                new?.end?.registerTypeObserver(this)
            }
        )
    /**
     * The statement that is executed, if the condition is evaluated as false. Usually a [Block].
     */
    var elseStatement by unwrapping(IfElse::elseStatementEdge)

    override fun toString(): String {
        return ToStringBuilder(this, TO_STRING_STYLE)
            .appendSuper(super.toString())
            .append("condition", condition)
            .append("thenStatement", thenStatement)
            .append("elseStatement", elseStatement)
            .toString()
    }

    override fun addArgument(expression: Expression) {
        condition = expression
    }

    override fun replaceArgument(old: Expression, new: Expression): Boolean {
        this.condition = new
        return true
    }

    override fun hasArgument(expression: Expression): Boolean {
        return this.condition == expression
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IfElse) return false
        return super.equals(other) &&
            isConstExpression == other.isConstExpression &&
            initializerStatement == other.initializerStatement &&
            conditionDeclaration == other.conditionDeclaration &&
            condition == other.condition &&
            thenStatement == other.thenStatement &&
            elseStatement == other.elseStatement
    }

    override fun hashCode() =
        Objects.hash(
            super.hashCode(),
            isConstExpression,
            initializerStatement,
            conditionDeclaration,
            condition,
            thenStatement,
            elseStatement,
        )

    override fun getStartingPrevEOG(): Collection<Node> {
        return initializerStatement?.getStartingPrevEOG()
            ?: condition?.getStartingPrevEOG()
            ?: conditionDeclaration?.getStartingPrevEOG()
            ?: this.prevEOG
    }

    override fun getExitNextEOG(): Collection<Node> {
        val thenExit = this.thenStatement?.getExitNextEOG() ?: setOf()
        val elseExit = this.elseStatement?.getExitNextEOG() ?: this.nextEOG
        return (thenExit + elseExit).ifEmpty { this.nextEOG }
    }

    override fun typeChanged(newType: Type, src: HasType) {
        val types = mutableSetOf<Type>()

        thenStatement?.type?.let { types.add(it) }
        elseStatement?.type?.let { types.add(it) }

        val alternative = if (types.isNotEmpty()) types.first() else unknownType()
        this.type = types.commonType ?: alternative
    }

    override fun assignedTypeChanged(assignedTypes: Set<Type>, src: HasType) {
        // Merge and propagate the assigned types of our branches
        if (src == thenStatement || src == elseStatement) {
            val types = mutableSetOf<Type>()
            thenStatement?.assignedTypes?.let { types.addAll(it) }
            elseStatement?.assignedTypes?.let { types.addAll(it) }
            addAssignedTypes(types)
        }
    }
}
