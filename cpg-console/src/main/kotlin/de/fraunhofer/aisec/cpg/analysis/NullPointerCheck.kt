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
import de.fraunhofer.aisec.cpg.console.fancyLocationLink
import de.fraunhofer.aisec.cpg.evaluation.CouldNotResolve
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.IfStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.processing.IVisitor
import de.fraunhofer.aisec.cpg.processing.strategy.Strategy
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation.Companion.locationLink
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class NullPointerCheck {
    private val log: Logger
        get() = LoggerFactory.getLogger(OutOfBoundsCheck::class.java)

    fun run(result: TranslationResult) {
        for (tu in result.components.flatMap { it.translationUnits }) {
            tu.accept(
                Strategy::AST_FORWARD,
                object : IVisitor<Node>() {
                    fun visit(v: MemberCallExpression) {
                        handleHasBase(v)
                    }

                    fun visit(v: MemberExpression) {
                        handleHasBase(v)
                    }

                    fun visit(v: SubscriptExpression) {
                        handleHasBase(v)
                    }
                },
            )
        }
    }

    fun handleHasBase(node: HasBase) {
        try {
            // check for all incoming DFG branches
            node.base?.prevDFG?.forEach {
                var resolved: Any? = CouldNotResolve()
                val resolver = node.language.evaluator
                if (it is Expression || it is Declaration) {
                    // try to resolve them
                    resolved = resolver.evaluate(it)
                }

                if (resolved == null) {
                    println("")
                    val sb = AttributedStringBuilder()
                    sb.append("--- FINDING: Null pointer detected in ")
                    sb.append(node.javaClass.simpleName, DEFAULT.foreground(GREEN))
                    sb.append(" when accessing base ")
                    sb.append(node.base?.name, DEFAULT.foreground(YELLOW))
                    sb.append(" ---")

                    val header = sb.toAnsi()

                    println(header)
                    println(
                        "${AttributedString(locationLink((node as Node).location), DEFAULT.foreground(BLUE or BRIGHT)).toAnsi()}: ${(node as Node).fancyCode(
                            showNumbers = false
                        )
                        }"
                    )
                    println("")
                    println(
                        "The following path was discovered that leads to ${AttributedString(node.base?.name, DEFAULT.foreground(YELLOW)).toAnsi()} being null:"
                    )
                    for (p in resolver.path) {

                        println(
                            "${AttributedString(locationLink(p.location), DEFAULT.foreground(BLUE or BRIGHT)).toAnsi()}: ${p.fancyCode(
                                showNumbers = false
                            )
                            }"
                        )
                    }

                    val path =
                        it.followPrevEOG { edge ->
                            return@followPrevEOG edge.start is IfStatement ||
                                edge.start is FunctionDeclaration
                        }

                    val last = path?.last()?.start

                    if (last is IfStatement) {
                        println()
                        println(
                            "Branch depends on ${AttributedString("IfStatement", DEFAULT.foreground(GREEN)).toAnsi()} with condition ${AttributedString(last.condition?.code, DEFAULT.foreground(CYAN)).toAnsi()} in ${last.fancyLocationLink()}"
                        )
                    }

                    println("-".repeat(sb.toString().length))
                }
            }
        } catch (ex: Throwable) {
            log.error("Exception while running check", ex)
        }
    }
}
