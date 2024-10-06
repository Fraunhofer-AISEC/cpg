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
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.query.*

class UseAfterFree : Rule {
    override var queryResult: QueryTree<*>? = null // to be set
    override val id = "cpg-0015"
    override val name = "Use After Free"
    override val cweId = "416"
    override val shortDescription = "Detects use of memory after it has been freed"
    override val level = Rule.Level.Error
    override val message = "Use after free detected"

    override fun run(result: TranslationResult) {
        queryResult =
            result.allExtended<CallExpression>(
                { it.name.localName == "free" },
                { outer ->
                    executionPath(outer) { inner ->
                        (outer.arguments[0] as? Reference)?.refersTo == // free argument
                            (inner as? Reference)?.refersTo // reference to free argument after the free
                    }
                }
            )
    }
}
