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
        requirement("Is Security Target Correctly specified") { manualAssessmentOf("SEC-TARGET") }
        requirement("Good Encryption") { result ->
            goodCryptoFunc(result) and
                goodArgumentSize(result) and
                manualAssessmentOf("THIRD-PARTY-LIBRARY")
        }
    }

    assumptions { assume { "We assume that everything is fine." } }
}
