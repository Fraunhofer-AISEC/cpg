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
import de.fraunhofer.aisec.cpg.graph.statements.expressions.SubscriptExpression
import de.fraunhofer.aisec.cpg.query.*

class ArrayOverRead : Rule {
    // cwe 126 overread
    override var queryResult: QueryTree<*>? = null // to be set
    override val id = "cpg-0010" // TODO IDS
    override val name = "array over-read"
    override val cweId: String = "126"
    override val shortDescription =
        "This rule detects Array accesses with indices larger than or equal to the size " +
            "of the Array"
    override val level = Rule.Level.Error
    override val message = "Array over-read detected"

    override fun run(result: TranslationResult) {
        queryResult =
            result.allExtended<SubscriptExpression>(
                mustSatisfy = { max(it.subscriptExpression) gt sizeof(it) }
            )
    }
}
