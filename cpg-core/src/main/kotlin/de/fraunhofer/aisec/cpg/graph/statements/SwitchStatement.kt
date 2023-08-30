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

import de.fraunhofer.aisec.cpg.graph.AST
import de.fraunhofer.aisec.cpg.graph.BranchingNode
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import java.util.Objects

/**
 * Represents a Java or C++ switch statement of the `switch (selector) {...}` that can include case
 * and default statements. Break statements break out of the switch and labeled breaks in JAva are
 * handled properly.
 */
class SwitchStatement : Statement(), BranchingNode {
    /** Selector that determines the case/default statement of the subsequent execution */
    @AST var selector: Expression? = null

    /** C++ can have an initializer statement in a switch */
    @AST var initializerStatement: Statement? = null

    /** C++ allows to use a declaration instead of a expression as selector */
    @AST var selectorDeclaration: Declaration? = null

    /**
     * The compound statement that contains break/default statements with regular statements on the
     * same hierarchy
     */
    @AST var statement: Statement? = null

    override val branchedBy: Node?
        get() = selector

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SwitchStatement) return false
        return super.equals(other) &&
            initializerStatement == other.initializerStatement &&
            selectorDeclaration == other.selectorDeclaration &&
            selector == other.selector &&
            statement == other.statement
    }

    override fun hashCode() =
        Objects.hash(
            super.hashCode(),
            initializerStatement,
            selectorDeclaration,
            selector,
            statement
        )
}
