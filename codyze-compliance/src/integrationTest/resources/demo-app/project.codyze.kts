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
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.edges.get
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeleteExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference

project {
    name = "Demo Project"

    toe {
        name = "Demo TOE"
        architecture {
            modules {
                module("webapp") {
                    directory = "components/webapp"
                    include("webapp")
                }
                module("auth") {
                    directory = "components/auth"
                    include("auth")
                }
            }
        }
    }

    requirements {
        requirement {
            name = "Proper Handling of Key Material"
            description = "Sensitive material, such as keys are handled properly"

            fulfilledBy { properHandlingOfKeyMaterial() }
        }

        requirement {
            name = "Encryption Functions have Many Parameters"

            fulfilledBy {
                val q = crappyQuery()
                q
            }
        }
    }

    // comp: 00000000-40d0-3285-ffff-ffff9c7a5a6c
    // tr: 00000000-0000-0000-0000-000040d03285
    // comp hashcode: 495340980

    suppressions {
        /** I think it has enough parameters. */
        queryTree("00000000-2258-b0b4-0000-000000000533")
    }

    assumptions { assume { "Third party code is very good" } }
}

fun TranslationResult.crappyQuery(): QueryTree<Boolean> {
    return allExtended<FunctionDeclaration>(
        sel = { it.name.localName == "encrypt" },
        { it.parameters.size eq 1 },
    )
}

/** For each key K, if K is used in encryption or decryption, it must be deleted after use */
fun TranslationResult.properHandlingOfKeyMaterial(): QueryTree<Boolean> {
    val result =
        allExtended<CallExpression>(
            sel = {
                it.name.toString() == "execute" &&
                    it.arguments[0].evaluate() in listOf("encrypt", "decrypt")
            }
        ) {
            val k = it.argumentEdges["stdin"]?.end
            if (k == null) {
                QueryTree(true)
            } else {
                executionPath(k) { to ->
                    to is DeleteExpression &&
                        to.operands.any {
                            it is Reference && it.refersTo == (k as? Reference)?.refersTo
                        }
                }
            }
        }

    return result
}
