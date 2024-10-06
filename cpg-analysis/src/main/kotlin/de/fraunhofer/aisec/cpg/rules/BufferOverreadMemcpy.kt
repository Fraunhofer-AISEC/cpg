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

class BufferOverreadMemcpy : Rule {
    override var queryResult: QueryTree<*>? = null // to be set
    override val id = "cpg-0002" // TODO IDS
    override val name = "memcpy src smaller than size"
    override val shortDescription =
        "This rule detects memcpy calls where the size of the source is smaller than the size argument, which can " +
            "overread the src buffer"
    override val cweId: String = "787"
    override val level = Rule.Level.Error
    override val message = "memcpy call with source size smaller than size argument detected"

    override fun run(result: TranslationResult) {
        queryResult =
            result.allExtended<CallExpression>(
                { it.name.localName == "memcpy" },
                //          src                     n
                { sizeof(it.arguments[1]) le min(it.arguments[2]) }
            )
    }
}
