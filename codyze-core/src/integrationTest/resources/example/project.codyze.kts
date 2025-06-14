/*
 * Copyright (c) 2025, Fraunhofer AISEC. All rights reserved.
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
package example

import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.query.and

include {
    AssumptionDecisions from "assumptions.codyze.kts"
    ManualAssessment from "manual.codyze.kts"
    Tagging from "tagging.codyze.kts"
    Suppressions from "suppressions.codyze.kts"
}

project {
    name = "My Project"

    tool {
        configuration {
            includePath("src/integrationTest/resources/example/src/third-party/mylib")
            loadIncludes(true)
        }
    }

    toe {
        name = "My Mock TOE"
        architecture {
            modules {
                module("module1") {
                    directory = "src/module1"
                    includeAll()
                    exclude("tests")
                }
                module("module2") {
                    directory = "src/module2"
                    include("pkg")
                }
            }
        }
    }

    requirements {
        requirement {
            name = "Is Security Target Correctly specified"
            description = "test"

            fulfilledBy { manualAssessmentOf("SEC-TARGET") }
        }

        category("ENCRYPTION") {
            requirement {
                name = "Good Encryption"

                fulfilledBy {
                    goodCryptoFunc() and
                        goodArgumentSize() and
                        manualAssessmentOf("THIRD-PARTY-LIBRARY")
                }
            }

            requirement {
                name = "Long Function Names"

                fulfilledBy {
                    val q = veryLongFunctionName()
                    q
                }
            }
        }
    }

    assumptions { assume { "We assume that everything is fine." } }
}

/**
 * Checks if the function calls to "encrypt" are used correctly.
 *
 * This is just a demo function to illustrate how to write a query. In a real-world scenario, this
 * function would not be part of the script but rather of a separate query catalog module.
 */
fun TranslationResult.goodCryptoFunc(): QueryTree<Boolean> {
    return allExtended<CallExpression> { it.name eq "encrypt" }
}

fun TranslationResult.goodArgumentSize(): QueryTree<Boolean> {
    return allExtended<CallExpression> { it.arguments.size eq 2 }
}

fun TranslationResult.veryLongFunctionName(): QueryTree<Boolean> {
    return allExtended<FunctionDeclaration> { it.name.localName.length gt 7 }
}
