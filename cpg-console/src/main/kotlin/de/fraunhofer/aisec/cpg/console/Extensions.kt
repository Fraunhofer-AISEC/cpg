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
package de.fraunhofer.aisec.cpg.console

import de.fraunhofer.aisec.cpg.graph.HasType
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.IfStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.TypeParser
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.io.File
import org.jetbrains.kotlinx.ki.shell.configuration.ReplConfigurationBase
import org.jetbrains.kotlinx.ki.shell.plugins.SyntaxPlugin
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStyle
import org.jline.utils.AttributedStyle.*

fun Node.printCode(linesAhead: Int = 0, showNumbers: Boolean = false): Node {
    val header = "--- ${this.fancyLocationLink()} ---"

    println(header)
    println(this.fancyCode(linesAhead, showNumbers))
    println("-".repeat(header.length))

    return this
}

fun Collection<Node>.printCode(
    linesAhead: Int = 0,
    showNumbers: Boolean = false
): Collection<Node> {
    val it = this.iterator()

    while (it.hasNext()) {
        val next = it.next()
        next.printCode(linesAhead, showNumbers)
        println("")
    }

    return this
}

fun Node.fancyCode(linesAhead: Int = 0, showNumbers: Boolean): String? {
    // start with the code
    var code = this.code

    this.location?.region?.let {
        // we need this later for number formatting
        var startLine = it.startLine

        if (linesAhead != 0) {
            // we need to fetch the lines from the original code
            val region =
                Region(1.coerceAtLeast(it.startLine - linesAhead), 1, it.endLine, it.endColumn)

            // update the start line
            startLine = region.startLine

            this.file?.let { file -> code = getCode(file, region) }
        }

        // split it into lines
        val lines = (code?.split("\n") ?: listOf()).toMutableList()

        val extraCharsInLines = mutableMapOf<Int, Int>()

        val fancies = getFanciesFor(this, this)

        for (fancy in fancies) {
            val region = getRelativeLocation(it, fancy.second)

            fancy.first.let {
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
                val ansi = AttributedString(content, fancy.first).toAnsi()

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

        if (showNumbers) {
            var output = ""

            // display line numbers
            for (i in 0 until lines.size) {
                var line = ((i + startLine).toString() + ": ").padStart(5) + lines[i]
                if (i != lines.size - 1) {
                    line += "\n"
                }

                /*if (i >= linesAhead) {
                    output += AttributedString(line, DEFAULT.background(128, 128, 128)).toAnsi()
                } else {*/
                output += line
                // }
            }

            return output
        }

        return lines.joinToString("\n")
    }

    // no location, no fancy
    return this.code
}

fun getCode(file: String, region: Region): String {
    var code = ""

    val lines = File(file).readLines()

    for (i in region.startLine - 1 until region.endLine) {
        code +=
            when (i) {
                region.startLine - 1 -> lines[i].substring(region.startColumn - 1) + "\n"
                region.endLine - 1 -> lines[i].substring(0, region.endColumn - 1)
                else -> lines[i] + "\n"
            }
    }

    return code
}

val styles = SyntaxPlugin.HighlightStylesFromConfiguration(object : ReplConfigurationBase() {})

fun getFanciesFor(original: Node, node: Node): List<Pair<AttributedStyle, Region>> {
    val list = mutableListOf<Pair<AttributedStyle, Region>>()

    when (node) {
        is MemberCallExpression -> {
            // only color the member
            list.addAll(getFanciesFor(node, node.member))

            return list
        }
        is DeclaredReferenceExpression -> {
            if ((original as? MemberCallExpression)?.member == node) {
                node.location?.let { list += Pair(styles.identifier!!, it.region) }
            }

            // also color it, if its on its own
            if (original == node) {
                node.location?.let { list += Pair(styles.identifier!!, it.region) }
            }

            return list
        }
        is DeclarationStatement -> {
            fancyType(node, (node.singleDeclaration as? HasType)!!, list)

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
        is IfStatement -> {
            // look for the if keyword
            fancyWord("if", node, list, styles.keyword)

            list.addAll(getFanciesFor(original, node.thenStatement))

            return list
        }
        is BinaryOperator -> {
            list.addAll(getFanciesFor(original, node.lhs))
            list.addAll(getFanciesFor(original, node.rhs))
        }
        is Literal<*> -> {
            when (node.value) {
                is Number -> node.location?.let { list += Pair(styles.number, it.region) }
                null -> node.location?.let { list += Pair(styles.number, it.region) }
                is Boolean -> node.location?.let { list += Pair(styles.number, it.region) }
                is String -> node.location?.let { list += Pair(styles.string, it.region) }
            }

            return list
        }
        is ArrayCreationExpression -> {
            fancyWord("new", node, list, styles.keyword)

            // check for primitive types
            for (primitive in TypeParser.PRIMITIVES) {
                fancyWord(primitive, node, list, styles.keyword)
            }

            // color initializer, if any
            node.initializer?.let { list.addAll(getFanciesFor(original, it)) }

            // color dimensions, if any
            for (dimension in node.dimensions) {
                list.addAll(getFanciesFor(original, dimension))
            }

            return list
        }
        is FunctionDeclaration -> {
            // color some keywords
            val keywords = listOf("public", "private", "static")
            for (keyword in keywords) {
                fancyWord(keyword, node, list, styles.keyword)
            }

            // color the name
            fancyWord(node.name, node, list, styles.function)

            // forward it to the body
            list.addAll(getFanciesFor(original, node.body))

            return list
        }
    }

    return list
}

private fun fancyType(
    outer: Node,
    node: HasType,
    list: MutableList<Pair<AttributedStyle, Region>>
) {
    val types = TypeParser.PRIMITIVES.toMutableSet()
    types += node.type.name

    // check for primitive types
    for (type in types) {
        fancyWord(type, outer, list, styles.type)
    }
}

private fun fancyWord(
    word: String,
    node: Node,
    list: MutableList<Pair<AttributedStyle, Region>>,
    style: AttributedStyle
) {
    node.location?.let {
        // look for the name in code; this assumes that it is on the first line for now
        val offset = node.code?.indexOf(word) ?: -1

        if (offset != -1) {
            val region =
                Region(
                    it.region.startLine,
                    it.region.startColumn + offset,
                    it.region.startLine,
                    it.region.startColumn + offset + word.length
                )

            list += Pair(style, region)
        }
    }
}

fun getRelativeLocation(parentRegion: Region, region: Region): Region {
    // we only need a column offset, if the start line is the same
    var columnOffset =
        if (region.startLine == parentRegion.startLine) {
            parentRegion.startColumn
        } else {
            1 // not sure why
        }

    val lineOffset = parentRegion.startLine

    return Region(
        region.startLine - lineOffset,
        region.startColumn - columnOffset,
        region.endLine - lineOffset,
        region.endColumn - columnOffset
    )
}

fun Node?.fancyLocationLink(): String {
    return AttributedString(
            PhysicalLocation.locationLink(this?.location),
            DEFAULT.foreground(BLUE or BRIGHT)
        )
        .toAnsi()
}
