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

import de.fraunhofer.aisec.cpg.graph.DeclarationHolder
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation.locationLink
import de.fraunhofer.aisec.cpg.sarif.Region
import kotlin.jvm.Throws
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStyle
import org.jline.utils.AttributedStyle.*

fun Node?.printCode(): Unit {
    val header = "--- ${locationLink(this?.location)} ---"

    println(header)
    println(this?.fancyCode())
    println("-".repeat(header.length))
}

fun MutableSet<Node>.printCode(): Unit {
    val it = this.iterator()

    while (it.hasNext()) {
        val next = it.next()
        next.printCode()
        println("")
    }
}

fun Expression.resolve(): Any? {
    return ValueResolver().resolve(this)
}

fun Declaration.resolve(): Any? {
    return ValueResolver().resolveDeclaration(this)
}

@Throws(DeclarationNotFound::class)
inline fun <reified T : Declaration> DeclarationHolder.byName(name: String): T {
    var base = this
    var lookup = name

    // lets do a _very_ simple FQN lookup
    // TODO(oxisto): we could do this with a for-loop for multiple nested levels
    if (name.contains(".")) {
        // take the most left one
        val baseName = name.split(".")[0]

        base =
            this.declarations.filterIsInstance<DeclarationHolder>().firstOrNull {
                (it as? Node)?.name == baseName
            }
                ?: throw DeclarationNotFound("base not found")
        lookup = name.split(".")[1]
    }

    val o = base.declarations.filterIsInstance<T>().firstOrNull { it.name == lookup }

    return o ?: throw DeclarationNotFound("declaration with name not found or incorrect type")
}

/**
 * This inline function returns the n'th statement (in AST order) as specified in T.
 *
 * For convenience, n defaults to zero, so that the first statement is always easy to fetch.
 */
@Throws(StatementNotFound::class)
inline fun <reified T : Statement> FunctionDeclaration.body(n: Int = 0): T {
    return if (this.body is CompoundStatement) {
        val o = (this.body as? CompoundStatement)?.statements?.filterIsInstance<T>()?.get(n)

        if (o == null) {
            throw StatementNotFound()
        } else {
            return o
        }
    } else {
        if (n == 0 && this.body is T) {
            this.body as T
        } else {
            throw StatementNotFound()
        }
    }
}

class StatementNotFound : Exception()

class DeclarationNotFound(message: String) : Exception(message)

fun Node.followPrevEOG(predicate: (PropertyEdge<*>) -> Boolean): List<PropertyEdge<*>>? {
    val path = mutableListOf<PropertyEdge<*>>()

    for (edge in this.prevEOGEdges) {
        val source = edge.start

        path.add(edge)

        if (predicate(edge)) {
            return path
        }

        val subPath = source.followPrevEOG(predicate)
        if (subPath != null) {
            path.addAll(subPath)

            return path
        }
    }

    return null
}

fun Node.fancyCode(): String {
    // start with the code
    val code = this.code

    // split it into lines
    val lines = (code?.split("\n") ?: listOf()).toMutableList()

    // next, we need to get all the AST children
    val children = SubgraphWalker.getAstChildren(this)

    val extraCharsInLines = mutableMapOf<Int, Int>()

    val fancies = getFanciesFor(this, this)

    for (fancy in fancies) {
        if (fancy.first) {
            val region = getRelativeLocation(this, fancy.third)

            fancy.second.let {
                // the current line we want to tackle
                val line = lines[region.startLine]

                // the already accumulated extra chars on this line
                var extraCharsInLine = extraCharsInLines.getOrDefault(region.startLine, 0)

                // everything before the thing we want to replace. add the extra chars in line to
                // correct for ANSI chars introduced before us
                val before = line.substring(0, region.startColumn + extraCharsInLine)

                // the actual content we want to fancy
                val content =
                    line.substring(
                        region.startColumn + extraCharsInLine,
                        region.endColumn + extraCharsInLine
                    )

                // everything after the thing we want to replace. add the extra chars in line to
                // correct for ANSI chars introduced before us
                val after = line.substring(region.endColumn + extraCharsInLine)

                // fancy it
                val ansi = AttributedString(content, fancy.second).toAnsi()

                // the amount of extra chars introduced by the ANSI control chars
                val extraChars = ansi.length - content.length

                // reconstruct the line
                lines[region.startLine] = before + ansi + after

                // update extra chars in line
                extraCharsInLine += extraChars

                // store it
                extraCharsInLines.put(region.startLine, extraCharsInLine)
            }
        }
    }

    return lines.joinToString("\n")
}

fun getFanciesFor(original: Node, node: Node): List<Triple<Boolean, AttributedStyle, Region>> {
    val list = mutableListOf<Triple<Boolean, AttributedStyle, Region>>()

    when (node) {
        is MemberCallExpression -> {
            // only color the member
            list.addAll(getFanciesFor(node, node.member))

            return list
        }
        is DeclaredReferenceExpression -> {
            node.location?.let { list += Triple(true, DEFAULT.foreground(CYAN), it.region) }

            return list
        }
        is DeclarationStatement -> {
            node.location?.let {
                // lets assume, that everything left of the variable name is some sort of type
                val typeRegion =
                    Region(
                        it.region.startLine,
                        it.region.startColumn,
                        it.region.endLine,
                        node.singleDeclaration.location?.region?.startColumn ?: it.region.startLine
                    )

                list += Triple(true, DEFAULT.foreground(RED or BRIGHT), typeRegion)
            }

            for (declaration in node.declarations) {
                list.addAll(getFanciesFor(original, declaration))
            }

            return list
        }
        is VariableDeclaration -> {
            // only color initializer, if any
            node.initializer?.let { list.addAll(getFanciesFor(original, it)) }

            return list
        }
        is CompoundStatement -> {
            // loop through statements
            for (statement in node.statements) {
                list.addAll(getFanciesFor(original, statement))
            }

            return list
        }
        is Literal<*> -> {
            if (node.value is Number) {
                node.location?.let {
                    list += Triple(true, DEFAULT.foreground(BLUE or BRIGHT), it.region)
                }
            }

            return list
        }
        is ArrayCreationExpression -> {
            // color the whole expression
            node.location?.let { list += Triple(true, DEFAULT.foreground(YELLOW), it.region) }

            return list
        }
        is FunctionDeclaration -> {
            // color the name
            node.location?.let {
                // look for the name in code; this assumes that it is on the first line for now
                val offset = node.code?.indexOf(node.name) ?: -1

                if (offset != -1) {
                    val region =
                        Region(
                            it.region.startLine,
                            it.region.startColumn + offset,
                            it.region.startLine,
                            it.region.startColumn + offset + node.name.length
                        )

                    list += Triple(true, DEFAULT.foreground(YELLOW or BRIGHT), region)
                }
            }

            // forward it to the body
            list.addAll(getFanciesFor(original, node.body))

            return list
        }
    }

    return list
}

fun getRelativeLocation(parent: Node, region: Region): Region {
    var columnOffset = 0

    // we only need a column offset, if the start line is the same
    columnOffset =
        if (region.startLine == (parent.location?.region?.startLine ?: 0)) {
            (parent.location?.region?.startColumn ?: 0)
        } else {
            1 // not sure why
        }

    val lineOffset = (parent.location?.region?.startLine ?: 0)

    return Region(
        region.startLine - lineOffset,
        region.startColumn - columnOffset,
        region.endLine - lineOffset,
        region.endColumn - columnOffset
    )
}
