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

import de.fraunhofer.aisec.cpg.query.and

include {
    AssumptionDecisions from "assumptions.codyze.kts"
    ManualAssessment from "manual.codyze.kts"
    Tagging from "tagging.codyze.kts"
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

            suppressions {
                /** The encrypt function has 7 characters, so its ok. */
                queryTreeById("00000000-137f-f4c6-0000-000000000540" to true)

                /**
                 * This is a suppression for a query that checks for a function named "foo" and
                 * contains a greater than sign in its string representation.
                 *
                 * Foo is so common that we do not want to report it.
                 */
                queryTree(
                    { qt: QueryTree<Boolean> ->
                        qt.node?.name?.localName == "foo" && qt.stringRepresentation.contains(">")
                    } to true
                )
            }
        }
    }

    assumptions { assume { "We assume that everything is fine." } }
}
