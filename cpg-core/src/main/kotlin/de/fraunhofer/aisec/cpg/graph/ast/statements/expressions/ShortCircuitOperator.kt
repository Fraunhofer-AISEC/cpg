/*
 * Copyright (c) 2023, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.ast.statements.expressions

import de.fraunhofer.aisec.cpg.frontends.HasShortCircuitOperators
import de.fraunhofer.aisec.cpg.graph.ast.BranchingNode

/**
 * A [BinaryOperator] which only evaluates [BinaryOperator.rhs] if [BinaryOperator.lhs] fulfils some
 * condition. For the operators in [HasShortCircuitOperators.conjunctiveOperators], the rhs has to
 * evaluate to "true" or so to continue on the lhs, whereas for the operators in
 * [HasShortCircuitOperators.disjunctiveOperators], the lhs has to evaluate to "false" (or similar).
 */
class ShortCircuitOperator : BinaryOperator(), BranchingNode {
    override val branchedBy
        get() = lhs
}
