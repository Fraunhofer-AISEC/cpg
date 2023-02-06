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

import de.fraunhofer.aisec.cpg.ScopeManager
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.ParamVariableDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.TranslationUnitDeclaration
import de.fraunhofer.aisec.cpg.graph.declarations.VariableDeclaration
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.UnknownType

/**
 * Creates a new [TranslationUnitDeclaration] in the Fluent Node DSL with the given [name]. The
 * declaration will be set to the [ScopeManager.globalScope]. The [init] block can be used to create
 * further sub-nodes as well as configuring the created node itself.
 */
fun LanguageFrontend.translationUnit(
    name: CharSequence,
    init: TranslationUnitDeclaration.() -> Unit
): TranslationUnitDeclaration {
    val node = (this@LanguageFrontend).newTranslationUnitDeclaration(name)

    scopeManager.resetToGlobal(node)
    init(node)

    return node
}

/**
 * Creates a new [FunctionDeclaration] in the Fluent Node DSL with the given [name] and optional
 * [returnType]. The [init] block can be used to create further sub-nodes as well as configuring the
 * created node itself.
 */
context(DeclarationHolder)

fun LanguageFrontend.function(
    name: CharSequence,
    returnType: Type = UnknownType.getUnknownType(),
    init: FunctionDeclaration.() -> Unit
): FunctionDeclaration {
    val node = newFunctionDeclaration(name)
    node.returnTypes = listOf(returnType)

    scopeManager.enterScope(node)
    init(node)
    scopeManager.leaveScope(node)

    scopeManager.addDeclaration(node)

    return node
}

/**
 * Creates a new [CompoundStatement] in the Fluent Node DSL and sets it to the
 * [FunctionDeclaration.body] of the nearest enclosing [FunctionDeclaration]. The [init] block can
 * be used to create further sub-nodes as well as configuring the created node itself.
 */
context(FunctionDeclaration)

fun LanguageFrontend.body(
    needsScope: Boolean = true,
    init: CompoundStatement.() -> Unit
): CompoundStatement {
    val node = newCompoundStatement()

    scopeIfNecessary(needsScope, node, init)
    body = node

    return node
}

/**
 * Creates a new [ParamVariableDeclaration] in the Fluent Node DSL and adds it to the
 * [FunctionDeclaration.parameters] of the nearest enclosing [FunctionDeclaration]. The [init] block
 * can be used to create further sub-nodes as well as configuring the created node itself.
 */
context(FunctionDeclaration)

fun LanguageFrontend.param(
    name: CharSequence,
    type: Type = UnknownType.getUnknownType(),
    init: (ParamVariableDeclaration.() -> Unit)? = null
): ParamVariableDeclaration {
    val node =
        (this@LanguageFrontend).newParamVariableDeclaration(
            name,
            type,
        )
    init?.let { it(node) }

    scopeManager.addDeclaration(node)

    return node
}

/**
 * Creates a new [ReturnStatement] in the Fluent Node DSL and adds it to the
 * [StatementHolder.statements] of the nearest enclosing [StatementHolder]. The [init] block can be
 * used to create further sub-nodes as well as configuring the created node itself.
 */
context(StatementHolder)

fun LanguageFrontend.returnStmt(init: ReturnStatement.() -> Unit): ReturnStatement {
    val node = (this@LanguageFrontend).newReturnStatement()
    init(node)

    (this@StatementHolder) += node

    return node
}

/**
 * Creates a new [DeclarationStatement] in the Fluent Node DSL and adds it to the
 * [StatementHolder.statements] of the nearest enclosing [StatementHolder]. The [init] block can be
 * used to create further sub-nodes as well as configuring the created node itself.
 */
context(StatementHolder)

fun LanguageFrontend.declare(init: DeclarationStatement.() -> Unit): DeclarationStatement {
    val node = (this@LanguageFrontend).newDeclarationStatement()
    init(node)

    (this@StatementHolder) += node

    return node
}

/**
 * Creates a new [VariableDeclaration] in the Fluent Node DSL and adds it to the
 * [DeclarationStatement.declarations] of the nearest enclosing [DeclarationStatement]. The [init]
 * block can be used to create further sub-nodes as well as configuring the created node itself.
 */
context(DeclarationStatement)

fun LanguageFrontend.variable(
    name: String,
    type: Type = UnknownType.getUnknownType(),
    init: VariableDeclaration.() -> Unit
): VariableDeclaration {
    val node = newVariableDeclaration(name, type)
    init(node)

    addToPropertyEdgeDeclaration(node)

    scopeManager.addDeclaration(node)

    return node
}

/**
 * Creates a new [CallExpression] (or [MemberCallExpression]) in the Fluent Node DSL with the given
 * [name] and adds it to the nearest enclosing [Holder]. Depending on whether it is a
 * [StatementHolder] it is added to the list of [StatementHolder.statements] or in case of an
 * [ArgumentHolder], the function [ArgumentHolder.addArgument] is invoked.
 *
 * The type of expression is determined whether [name] is either a [Name] with a [Name.parent] or if
 * it can be parsed as a FQN in the given language. It also automatically creates either a
 * [DeclaredReferenceExpression] or [MemberExpression] and sets it as the [CallExpression.callee].
 * The [init] block can be used to create further sub-nodes as well as configuring the created node
 * itself.
 */
context(Holder<out Statement>)

fun LanguageFrontend.call(
    name: CharSequence,
    isStatic: Boolean = false,
    init: (CallExpression.() -> Unit)? = null
): CallExpression {
    // Try to parse the name
    val parsedName = parseName(name)
    val node =
        if (parsedName.parent != null) {
            newMemberCallExpression(
                newMemberExpression(
                    parsedName.localName,
                    newDeclaredReferenceExpression(parsedName.parent, parseType(parsedName.parent))
                ),
                isStatic
            )
        } else {
            newCallExpression(newDeclaredReferenceExpression(parsedName))
        }
    if (init != null) {
        init(node)
    }

    val holder = this@Holder
    if (holder is StatementHolder) {
        holder += node
    } else if (holder is ArgumentHolder) {
        holder += node
    }

    return node
}

/**
 * Creates a new [IfStatement] in the Fluent Node DSL and adds it to the
 * [StatementHolder.statements] of the nearest enclosing [StatementHolder]. The [init] block can be
 * used to create further sub-nodes as well as configuring the created node itself.
 */
context(StatementHolder)

fun LanguageFrontend.ifStmt(init: IfStatement.() -> Unit): IfStatement {
    val node = newIfStatement()
    init(node)

    (this@StatementHolder) += node

    return node
}

/**
 * Configures the [IfStatement.condition] in the Fluent Node DSL of the nearest enclosing
 * [IfStatement]. The [init] block can be used to create further sub-nodes as well as configuring
 * the created node itself.
 */
fun IfStatement.condition(init: IfStatement.() -> BinaryOperator): BinaryOperator {
    return init(this)
}

/**
 * Creates a new [CompoundStatement] in the Fluent Node DSL and sets it to the
 * [IfStatement.thenStatement] of the nearest enclosing [IfStatement]. The [init] block can be used
 * to create further sub-nodes as well as configuring the created node itself.
 */
context(IfStatement)

fun LanguageFrontend.thenStmt(
    needsScope: Boolean = true,
    init: CompoundStatement.() -> Unit
): CompoundStatement {
    val node = newCompoundStatement()
    scopeIfNecessary(needsScope, node, init)

    thenStatement = node

    return node
}

/**
 * Creates a new [IfStatement] in the Fluent Node DSL and sets it to the [IfStatement.elseStatement]
 * of the nearest enclosing [IfStatement]. This simulates an `else-if` scenario. The [init] block
 * can be used to create further sub-nodes as well as configuring the created node itself.
 */
context(IfStatement)

fun LanguageFrontend.elseIf(init: IfStatement.() -> Unit): IfStatement {
    val node = newIfStatement()
    init(node)

    elseStatement = node

    return node
}

/**
 * Creates a new [CompoundStatement] in the Fluent Node DSL and sets it to the
 * [IfStatement.elseStatement] of the nearest enclosing [IfStatement]. The [init] block can be used
 * to create further sub-nodes as well as configuring the created node itself.
 */
context(IfStatement)

fun LanguageFrontend.elseStmt(
    needsScope: Boolean = true,
    init: CompoundStatement.() -> Unit
): CompoundStatement {
    val node = newCompoundStatement()
    scopeIfNecessary(needsScope, node, init)

    elseStatement = node

    return node
}

/**
 * Creates a new [Literal] in the Fluent Node DSL and invokes [ArgumentHolder.addArgument] of the
 * nearest enclosing [ArgumentHolder].
 */
context(ArgumentHolder)

fun <N> LanguageFrontend.literal(value: N): Literal<N> {
    val node = newLiteral(value)

    (this@ArgumentHolder) += node

    return node
}

/**
 * Creates a new [DeclaredReferenceExpression] in the Fluent Node DSL and invokes
 * [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(ArgumentHolder)

fun LanguageFrontend.ref(name: CharSequence): DeclaredReferenceExpression {
    val node = newDeclaredReferenceExpression(name)

    (this@ArgumentHolder) += node

    return node
}

/**
 * Creates a new [BinaryOperator] with a `+` [BinaryOperator.operatorCode] in the Fluent Node DSL
 * and invokes [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(LanguageFrontend, ArgumentHolder)

operator fun Expression.plus(rhs: Expression): BinaryOperator {
    val node = (this@LanguageFrontend).newBinaryOperator("+")
    node.lhs = this
    node.rhs = rhs

    (this@ArgumentHolder) += node

    return node
}

/**
 * Creates a new [BinaryOperator] with a `==` [BinaryOperator.operatorCode] in the Fluent Node DSL
 * and invokes [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(LanguageFrontend, ArgumentHolder)

infix fun Expression.eq(rhs: Expression): BinaryOperator {
    val node = (this@LanguageFrontend).newBinaryOperator("==")
    node.lhs = this
    node.rhs = rhs

    (this@ArgumentHolder) += node

    return node
}

/** Creates a new [Type] with the given [name] in the Fluent Node DSL. */
fun LanguageFrontend.t(name: CharSequence): Type {
    return parseType(name)
}

/**
 * Internally used to enter a new scope if [needsScope] is true before invoking [init] and leaving
 * it afterwards.
 */
private fun LanguageFrontend.scopeIfNecessary(
    needsScope: Boolean,
    node: CompoundStatement,
    init: CompoundStatement.() -> Unit
) {
    if (needsScope) {
        scopeManager.enterScope(node)
    }
    init(node)
    if (needsScope) {
        scopeManager.leaveScope(node)
    }
}
