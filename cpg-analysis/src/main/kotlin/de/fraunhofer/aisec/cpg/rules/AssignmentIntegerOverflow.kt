/*
 * Copyright (c) 2024, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.rules

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.statements.expressions.AssignExpression
import de.fraunhofer.aisec.cpg.query.*

class AssignmentIntegerOverflow : Rule {
    override var queryResult: QueryTree<*>? = null // to be set
    override val id = "cpg-0013" // TODO IDS
    override val name = "Assignment Integer Overflow"
    override val cweId: String = "190"
    override val shortDescription =
        "Detects assignments that may cause the left-hand side to overflow"
    override val level = Rule.Level.Error
    override val message = "Assignment may cause overflow"

    override fun run(result: TranslationResult) {
        queryResult =
            result.allExtended<AssignExpression>(
                { it.lhs[0].type.isPrimitive },
                { max(it.rhs[0]) gt maxSizeOfType(it.lhs[0].type) }
            )
    }
}
