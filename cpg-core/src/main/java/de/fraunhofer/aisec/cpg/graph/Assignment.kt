/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression

/** An assignment assigns a certain value (usually an [Expression]) to a certain target. */
interface Assignment {
    /**
     * The target of this assignment. Note that this is intentionally nullable, because while
     * [BinaryOperator] implements [Assignment], not all binary operations are assignments. Thus,
     * the target is only non-null for operations that have a == operator.
     */
    val target: AssignmentTarget?

    /**
     * The value expression that is assigned to the target. This is intentionally nullable for the
     * same reason as [target].
     */
    val value: Expression?
}

/**
 * The target of an assignment. The target is usually either a [VariableDeclaration] or a
 * [DeclaredReferenceExpression].
 */
interface AssignmentTarget {
    val name: String
}
