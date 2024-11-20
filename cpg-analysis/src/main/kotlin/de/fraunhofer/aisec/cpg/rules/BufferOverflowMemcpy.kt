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
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.query.*

class BufferOverflowMemcpy : Rule {
    override var queryResult: QueryTree<*>? = null // to be setv
    override val id = "cpg-0001" // TODO IDS
    override val name = "memcpy dest smaller than size"
    override val cweId = "787"
    override val shortDescription =
        "This rule detects memcpy calls where the size argument is larger than the size of the destination," +
            "which can overflow the destination buffer"
    override val level = Rule.Level.Error
    override val message = "memcpy call with destination size than size argument detected"

    override fun run(result: TranslationResult) {
        queryResult =
            result.allExtended<CallExpression>(
                { it.name.localName == "memcpy" },
                { sizeof(it.arguments[0]) lt min(it.arguments[2]) }
            )
    }
}
