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
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import java.util.Objects

/**
 * Case statement of the form `case expression :` that serves as entry point for switch statements,
 * the only allowed substatements are side effekt free primitive expression for the selector to
 * choose from. THe statements executed after the entry are on the same AST hierarchy in the parent
 * compound statement.
 */
class CaseStatement : Statement() {
    /**
     * Primitive side effect free statement that has to match with the evaluated selector in
     * SwitchStatement
     */
    @AST var caseExpression: Expression? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CaseStatement) return false
        return super.equals(other) && caseExpression == other.caseExpression
    }

    override fun hashCode() = Objects.hash(super.hashCode(), caseExpression)
}
