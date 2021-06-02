/*
 * Copyright (c) 2021, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.analysis

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ArrayCreationExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.ArraySubscriptionExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.helpers.Util
import de.fraunhofer.aisec.cpg.processing.IVisitor
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class OutOfBoundsCheck {

    private val log: Logger
        get() = LoggerFactory.getLogger(OutOfBoundsCheck::class.java)

    fun run(result: TranslationResult) {
        for (tu in result.translationUnits) {
            tu.accept(
                Strategy::AST_FORWARD,
                object : IVisitor<Node?>() {
                    fun visit(v: ArraySubscriptionExpression) {
                        val resolvedIndex = v.subscriptExpression.resolve()

                        if (resolvedIndex is Int) {
                            println("Index: $resolvedIndex")

                            // check, if we know that the array was initialized with a fixed length
                            // TODO(oxisto): it would be nice to have a helper that follows the expr
                            val decl =
                                (v.arrayExpression as? DeclaredReferenceExpression)?.refersTo as?
                                    VariableDeclaration
                            (decl?.initializer as? ArrayCreationExpression)?.let {
                                println("Found a ArrayCreationExpression")

                                val dimension = it.dimensions.first().resolve()

                                if (resolvedIndex >= dimension as Int) {
                                    Util.errorWithFileLocation(
                                        v,
                                        log,
                                        "Error: this expression will run out of bounds $resolvedIndex >= $dimension"
                                    )
                                }
                            }
                        } else {
                            println("Could not resolved ${v.subscriptExpression}")
                        }
                    }
                }
            )
        }
    }
}
