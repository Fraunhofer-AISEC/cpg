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
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.Statement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import kotlin.jvm.Throws

fun Node?.printCode(): Unit {
    println(this?.code)
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

    // lets do a _very_ simple FQN lookup (TODO(oxisto): we could do this with a for-loop for
    // multiple nested levels)
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

    val o = base.declarations.filterIsInstance<T>().firstOrNull() { it.name == lookup }

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
