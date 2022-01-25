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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.declarations.Declaration
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker

@JvmName("allNodes")
fun TranslationResult.all(): List<Node> {
    return this.all<Node>()
}

inline fun <reified T : Node> TranslationResult.all(): List<T> {
    val children = SubgraphWalker.flattenAST(this)

    return children.filterIsInstance<T>()
}

@JvmName("allNodes")
fun Node.all(): List<Node> {
    return this.all<Node>()
}

inline fun <reified T : Node> Node.all(): List<T> {
    val children = SubgraphWalker.flattenAST(this)

    return children.filterIsInstance<T>()
}

@JvmName("astNodes")
fun Node.ast(): List<Node> {
    return this.ast<Node>()
}

inline fun <reified T : Node> Node.ast(): List<T> {
    val children = SubgraphWalker.getAstChildren(this)

    return children.filterIsInstance<T>()
}

inline fun <reified T : Node> Node.dfgFrom(): List<T> {
    return this.prevDFG.toList().filterIsInstance<T>()
}

inline fun <reified T : Declaration> DeclarationHolder.byNameOrNull(
    name: String,
    fqn: Boolean = false
): T? {
    var base = this
    var lookup = name

    // lets do a _very_ simple FQN lookup
    // TODO(oxisto): we could do this with a for-loop for multiple nested levels
    if (fqn && name.contains(".")) {
        // take the most left one
        val baseName = name.split(".")[0]

        base =
            this.declarations.filterIsInstance<DeclarationHolder>().firstOrNull {
                (it as? Node)?.name == baseName
            }
                ?: return null
        lookup = name.split(".")[1]
    }

    return base.declarations.filterIsInstance<T>().firstOrNull { it.name == lookup }
}

@Throws(DeclarationNotFound::class)
inline fun <reified T : Declaration> DeclarationHolder.byName(
    name: String,
    fqn: Boolean = false
): T {
    return byNameOrNull(name, fqn)
        ?: throw DeclarationNotFound("declaration with name not found or incorrect type")
}

/**
 * This inline function returns the `n`-th body statement (in AST order) as specified in T or `null`
 * if it does not exist or the type does not match.
 *
 * For convenience, `n` defaults to zero, so that the first statement is always easy to fetch.
 */
inline fun <reified T : Statement> FunctionDeclaration.bodyOrNull(n: Int = 0): T? {
    return if (this.body is CompoundStatement) {
        return (body as? CompoundStatement)?.statements?.filterIsInstance<T>()?.getOrNull(n)
    } else {
        if (n == 0 && this.body is T) {
            this.body as T
        } else {
            return null
        }
    }
}

/**
 * This inline function returns the `n`-th body statement (in AST order) as specified in T. It
 * throws a [StatementNotFound] exception if it does not exist or match the type.
 *
 * For convenience, `n` defaults to zero, so that the first statement is always easy to fetch.
 */
@Throws(StatementNotFound::class)
inline fun <reified T : Statement> FunctionDeclaration.body(n: Int = 0): T {
    return bodyOrNull(n) ?: throw StatementNotFound()
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
