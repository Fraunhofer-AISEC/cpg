/*
 * Copyright (c) 2022, Fraunhofer AISEC. All rights reserved.
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
package de.fraunhofer.aisec.cpg.graph.builder

import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.ParamVariableDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.newTranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.CompoundStatement
import de.fraunhofer.aisec.cpg.graph.statements.DeclarationStatement
import de.fraunhofer.aisec.cpg.graph.statements.ReturnStatement
import de.fraunhofer.aisec.cpg.graph.statements.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.statements.expressions.DeclaredReferenceExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.types.UnknownType

fun LanguageFrontend.translationUnit(
    name: CharSequence,
    init: TranslationUnitDeclaration.() -> Unit
): TranslationUnitDeclaration {
    val node = newTranslationUnitDeclaration(name)

    scopeManager.resetToGlobal(node)
    init(node)

    return node
}

context(LanguageFrontend)

fun DeclarationHolder.function(
    name: CharSequence,
    init: FunctionDeclaration.() -> Unit
): FunctionDeclaration {
    val node = newFunctionDeclaration(name)

    scopeManager.enterScope(node)
    init(node)
    scopeManager.leaveScope(node)

    scopeManager.addDeclaration(node)

    return node
}

context(LanguageFrontend)

fun FunctionDeclaration.body(
    hasScope: Boolean = true,
    init: CompoundStatement.() -> Unit
): CompoundStatement {
    val node = newCompoundStatement()

    if (hasScope) {
        scopeManager.enterScope(node)
    }
    init(node)
    if (hasScope) {
        scopeManager.leaveScope(node)
    }
    this.body = node

    return node
}

context(LanguageFrontend)

fun FunctionDeclaration.param(
    name: CharSequence,
    typeName: CharSequence? = null,
    init: (ParamVariableDeclaration.() -> Unit)? = null
): ParamVariableDeclaration {
    val node =
        newParamVariableDeclaration(
            name,
            typeName?.let { parseType(it) } ?: UnknownType.getUnknownType()
        )
    init?.let { it(node) }

    scopeManager.addDeclaration(node)

    return node
}

context(LanguageFrontend)

fun StatementHolder.returnStmt(init: ReturnStatement.() -> Unit): ReturnStatement {
    val node = newReturnStatement()
    init(node)

    this += node

    return node
}

context(LanguageFrontend)

fun StatementHolder.declare(init: DeclarationStatement.() -> Unit): DeclarationStatement {
    val node = newDeclarationStatement()
    init(node)

    this += node

    return node
}

context(LanguageFrontend)

fun <N> ArgumentHolder.literal(value: N): Literal<N> {
    val node = newLiteral(value)

    this += node

    return node
}

context(LanguageFrontend)

fun DeclarationStatement.variable(
    name: String,
    init: VariableDeclaration.() -> Unit
): VariableDeclaration {
    val node = newVariableDeclaration(name)
    init(node)

    this.addToPropertyEdgeDeclaration(node)

    scopeManager.addDeclaration(node)

    return node
}

context(LanguageFrontend)

fun ArgumentHolder.ref(name: CharSequence): DeclaredReferenceExpression {
    val node = newDeclaredReferenceExpression(name)

    this += node

    return node
}

context(LanguageFrontend, ArgumentHolder)

operator fun Expression.plus(rhs: Expression): BinaryOperator {
    val node = newBinaryOperator("+")
    node.lhs = this
    node.rhs = rhs

    plusAssign(node)

    return node
}
