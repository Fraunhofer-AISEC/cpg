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
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.edges.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CallExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeleteExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.query.QueryTree
import de.fraunhofer.aisec.cpg.query.allExtended
import de.fraunhofer.aisec.cpg.query.executionPath

fun statement1(tr: TranslationResult): QueryTree<Boolean> {
    val result =
        tr.allExtended<CallExpression>(
            sel = {
                it.name.toString() == "execute" &&
                    it.arguments[0].evaluate() in listOf("encrypt", "decrypt")
            }
        ) {
            val processInput = it.argumentEdges["stdin"]?.end
            if (processInput == null) {
                QueryTree(true)
            } else {
                executionPath(processInput) { to ->
                    to is DeleteExpression &&
                        to.operands.any {
                            it is Reference && it.refersTo == (processInput as? Reference)?.refersTo
                        }
                }
            }
        }

    return result
}
