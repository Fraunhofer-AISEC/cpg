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
import de.fraunhofer.aisec.cpg.console.fancyCode
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.capacity
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.expressions.NewArrayExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.statements.expressions.SubscriptExpression
import de.fraunhofer.aisec.cpg.processing.Visitor
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle
import org.jline.utils.AttributedStyle.DEFAULT
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class OutOfBoundsCheck {

    private val log: Logger
        get() = LoggerFactory.getLogger(OutOfBoundsCheck::class.java)

    fun run(result: TranslationResult) {
        for (tu in result.components.flatMap { it.translationUnits }) {
            tu.accept(
                Strategy::AST_FORWARD,
                object : Visitor<Node>() {
                    fun visit(v: SubscriptExpression) {
                        val evaluator = ValueEvaluator()
                        val resolvedIndex = evaluator.evaluate(v.subscriptExpression)

                        if (resolvedIndex !is Int) {
                            println("Could not resolve ${v.subscriptExpression}")
                            return
                        }
                        // check, if we know that the array was initialized with a fixed length
                        // TODO(oxisto): it would be nice to have a helper that follows the expr
                        val decl =
                            (v.arrayExpression as? Reference)?.refersTo as? VariableDeclaration
                        (decl?.initializer as? NewArrayExpression)?.let {
                            val capacity = it.capacity

                            if (resolvedIndex >= capacity) {
                                println("")
                                val sb = AttributedStringBuilder()
                                sb.append("--- FINDING: Out of bounds access in ")
                                sb.append(
                                    it.javaClass.simpleName,
                                    DEFAULT.foreground(AttributedStyle.GREEN),
                                )
                                sb.append(
                                    " when accessing index ${AttributedString(""+resolvedIndex, DEFAULT.foreground(AttributedStyle.CYAN)).toAnsi()} of "
                                )
                                sb.append(decl.name, DEFAULT.foreground(AttributedStyle.YELLOW))
                                sb.append(
                                    ", an array of length ${AttributedString(""+capacity, DEFAULT.foreground(AttributedStyle.CYAN)).toAnsi()} ---"
                                )

                                val header = sb.toAnsi()

                                println(header)
                                println(
                                    "${
                                            AttributedString(
                                                PhysicalLocation.locationLink(v.location), DEFAULT.foreground(
                                                    AttributedStyle.BLUE or AttributedStyle.BRIGHT
                                                ),).toAnsi()}: ${v.fancyCode(showNumbers = false)}"
                                )
                                println("")
                                println(
                                    "The following path was discovered that leads to ${v.subscriptExpression.fancyCode(
                                            showNumbers = false
                                        )
                                        } being ${AttributedString(""+resolvedIndex, DEFAULT.foreground(AttributedStyle.CYAN)).toAnsi()}:"
                                )
                                for (p in evaluator.path) {

                                    println(
                                        "${AttributedString(
                                                PhysicalLocation.locationLink(p.location), DEFAULT.foreground(
                                                    AttributedStyle.BLUE or AttributedStyle.BRIGHT
                                                ),).toAnsi()}: ${p.fancyCode(showNumbers = false)}"
                                    )
                                }
                            }
                        }
                    }
                },
            )
        }
    }
}
