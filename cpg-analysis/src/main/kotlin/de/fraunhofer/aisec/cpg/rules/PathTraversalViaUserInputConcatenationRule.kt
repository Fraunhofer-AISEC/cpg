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
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.query.*

class PathTraversalViaUserInputConcatenationRule : Rule {
    override var queryResult: QueryTree<*>? = null

    override val id = "PTR-001"
    override val name = "Path Traversal via User Input Concatenation"
    override val cweId = "22"
    override val shortDescription =
        "User input is concatenated with file paths without proper validation"
    override val mdShortDescription = null
    override val level = Rule.Level.Warning
    override val message =
        "User input is concatenated with file paths without proper validation, leading to potential path traversal vulnerabilities."
    override val mdMessage = null
    override val messageArguments = listOf<String>()

    private val concatFunctionsRegex = Regex("concat|append|app", RegexOption.IGNORE_CASE)

    override fun run(result: TranslationResult) {
        queryResult =
            result.allExtended<CallExpression>(
                sel = { Util.isUserInput(it) },
                mustSatisfy = { outer ->
                    dataFlow( // reaches concat
                        from = outer,
                        predicate = { inner ->
                            inner is CallExpression &&
                                inner.name.localName.contains(concatFunctionsRegex) &&
                                inner.arguments.any { Util.isPathLike(it) } ||
                                inner is BinaryOperator && inner.operatorCode == "+" ||
                                inner is BinaryOperator && inner.operatorCode == "+="
                        },
                        collectFailedPaths = false,
                        findAllPossiblePaths = true
                    ) and
                        not( // and doesn't reach validation
                            dataFlow(
                                from = outer,
                                predicate = {
                                    it is CallExpression && Util.isValidationFunction(it)
                                },
                                collectFailedPaths = false,
                                findAllPossiblePaths = true
                            )
                        )
                }
            )
    }
}
