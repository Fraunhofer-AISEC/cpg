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
import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.scopes.RecordScope
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.UnknownType

fun LanguageFrontend.translationResult(
    config: TranslationConfiguration,
    init: TranslationResult.() -> Unit
): TranslationResult {
    val node = TranslationResult(TranslationManager.builder().config(config).build(), scopeManager)
    val component = Component()
    node.addComponent(component)
    init(node)

    config.registeredPasses.forEach { it.accept(node) }

    return node
}

/**
 * Creates a new [TranslationUnitDeclaration] in the Fluent Node DSL with the given [name]. The
 * declaration will be set to the [ScopeManager.globalScope]. The [init] block can be used to create
 * further sub-nodes as well as configuring the created node itself.
 */
context(TranslationResult)

fun LanguageFrontend.translationUnit(
    name: CharSequence,
    init: TranslationUnitDeclaration.() -> Unit
): TranslationUnitDeclaration {
    val node = (this@LanguageFrontend).newTranslationUnitDeclaration(name)

    scopeManager.resetToGlobal(node)
    init(node)
    this@TranslationResult.components.firstOrNull()?.translationUnits?.add(node)

    return node
}

/**
 * Creates a new [NamespaceDeclaration] in the Fluent Node DSL with the given [name]. The
 * declaration will be set to the [ScopeManager.globalScope]. The [init] block can be used to create
 * further sub-nodes as well as configuring the created node itself.
 */
context(DeclarationHolder)

fun LanguageFrontend.namespace(
    name: CharSequence,
    init: NamespaceDeclaration.() -> Unit
): NamespaceDeclaration {
    val node = (this@LanguageFrontend).newNamespaceDeclaration(name)

    scopeManager.enterScope(node)
    init(node)
    scopeManager.leaveScope(node)
    scopeManager.addDeclaration(node)
    return node
}

/**
 * Creates a new [RecordDeclaration] in the Fluent Node DSL with the given [name]. The declaration
 * will be set to the [ScopeManager.currentRecord]. The [init] block can be used to create further
 * sub-nodes as well as configuring the created node itself.
 */
context(DeclarationHolder)

fun LanguageFrontend.record(
    name: CharSequence,
    kind: String = "class",
    init: RecordDeclaration.() -> Unit
): RecordDeclaration {
    val node = (this@LanguageFrontend).newRecordDeclaration(name, kind)

    scopeManager.enterScope(node)
    init(node)
    scopeManager.leaveScope(node)
    scopeManager.addDeclaration(node)

    return node
}

/**
 * Creates a new [FieldDeclaration] in the Fluent Node DSL with the given [name] and optional
 * [type]. The [init] block can be used to create further sub-nodes as well as configuring the
 * created node itself.
 */
context(DeclarationHolder)

fun LanguageFrontend.field(
    name: CharSequence,
    type: Type = UnknownType.getUnknownType(),
    init: FieldDeclaration.() -> Unit
): FieldDeclaration {
    val node = newFieldDeclaration(name)
    node.type = type

    init(node)

    scopeManager.addDeclaration(node)

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
 * Creates a new [MethodDeclaration] in the Fluent Node DSL with the given [name] and optional
 * [returnType]. The [init] block can be used to create further sub-nodes as well as configuring the
 * created node itself.
 */
context(RecordDeclaration)

fun LanguageFrontend.method(
    name: CharSequence,
    returnType: Type = UnknownType.getUnknownType(),
    init: MethodDeclaration.() -> Unit
): MethodDeclaration {
    val node = newMethodDeclaration(name)
    node.returnTypes = listOf(returnType)

    scopeManager.enterScope(node)
    init(node)
    scopeManager.leaveScope(node)

    scopeManager.addDeclaration(node)
    (this@RecordDeclaration).addMethod(node)

    return node
}

/**
 * Creates a new [ConstructorDeclaration] in the Fluent Node DSL for the enclosing
 * [RecordDeclaration]. The [init] block can be used to create further sub-nodes as well as
 * configuring the created node itself.
 */
context(RecordDeclaration)

fun LanguageFrontend.constructor(init: ConstructorDeclaration.() -> Unit): ConstructorDeclaration {
    val recordDecl: RecordDeclaration = this@RecordDeclaration
    val node = newConstructorDeclaration(recordDecl.name, recordDeclaration = recordDecl)

    scopeManager.enterScope(node)
    init(node)
    scopeManager.leaveScope(node)

    scopeManager.addDeclaration(node)
    recordDecl.addConstructor(node)

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
    init: (VariableDeclaration.() -> Unit)? = null
): VariableDeclaration {
    val node = newVariableDeclaration(name, type)
    if (init != null) init(node)

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
                newMemberExpression(parsedName.localName, memberOrRef(parsedName.parent)),
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
 * Creates a new [CallExpression] (or [MemberCallExpression]) in the Fluent Node DSL with the given
 * [localName] and adds it to the nearest enclosing [Holder]. Depending on whether it is a
 * [StatementHolder] it is added to the list of [StatementHolder.statements] or in case of an
 * [ArgumentHolder], the function [ArgumentHolder.addArgument] is invoked.
 *
 * The type of expression is determined whether [localName] is either a [Name] with a [Name.parent]
 * or if it can be parsed as a FQN in the given language. It also automatically creates either a
 * [DeclaredReferenceExpression] or [MemberExpression] and sets it as the [CallExpression.callee].
 * The [init] block can be used to create further sub-nodes as well as configuring the created node
 * itself.
 */
context(Holder<out Statement>)

fun LanguageFrontend.memberCall(
    localName: CharSequence,
    member: Expression,
    isStatic: Boolean = false,
    init: (CallExpression.() -> Unit)? = null
): MemberCallExpression {
    // Try to parse the name
    val node = newMemberCallExpression(newMemberExpression(localName, member), isStatic)
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
 * Creates a new [ConstructExpression] in the Fluent Node DSL for the translation record/type with
 * the given [name] and adds it to the nearest enclosing [Holder]. Depending on whether it is a
 * [StatementHolder] it is added to the list of [StatementHolder.statements] or in case of an
 * [ArgumentHolder], the function [ArgumentHolder.addArgument] is invoked. The [init] block can be
 * used to create further sub-nodes as well as configuring the created node itself.
 */
context(Holder<out Statement>)

fun LanguageFrontend.construct(
    name: CharSequence,
    init: (ConstructExpression.() -> Unit)? = null
): ConstructExpression {
    val node = newConstructExpression(parseName(name))
    node.type = t(name)

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

context(Holder<out Statement>)

fun LanguageFrontend.new(init: (NewExpression.() -> Unit)? = null): NewExpression {
    val node = newNewExpression()
    if (init != null) init(node)

    val holder = this@Holder
    if (holder is StatementHolder) {
        holder += node
    } else if (holder is ArgumentHolder) {
        holder += node
    }
    return node
}

fun LanguageFrontend.memberOrRef(
    name: Name,
    type: Type = UnknownType.getUnknownType()
): Expression {
    val node =
        if (name.parent != null) {
            newMemberExpression(name.localName, memberOrRef(name.parent))
        } else {
            newDeclaredReferenceExpression(name.localName, parseType(name.localName))
        }
    node.type = type

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
context(IfStatement)

fun LanguageFrontend.condition(init: IfStatement.() -> BinaryOperator): BinaryOperator {
    return init(this@IfStatement)
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
 * nearest enclosing [Holder], but only if it is an [ArgumentHolder].
 */
context(Holder<out Statement>)

fun <N> LanguageFrontend.literal(value: N, type: Type = UnknownType.getUnknownType()): Literal<N> {
    val node = newLiteral(value, type)

    // Only add this to an argument holder if the nearest holder is an argument holder
    val holder = this@Holder
    if (holder is ArgumentHolder) {
        holder += node
    }

    return node
}

/**
 * Creates a new [DeclaredReferenceExpression] in the Fluent Node DSL and invokes
 * [ArgumentHolder.addArgument] of the nearest enclosing [Holder], but only if it is an
 * [ArgumentHolder].
 */
context(Holder<out Statement>)

fun LanguageFrontend.ref(
    name: CharSequence,
    type: Type = UnknownType.getUnknownType(),
    init: (DeclaredReferenceExpression.() -> Unit)? = null
): DeclaredReferenceExpression {
    val node = newDeclaredReferenceExpression(name)
    node.type = type

    if (init != null) {
        init(node)
    }

    // Only add this to an argument holder if the nearest holder is an argument holder
    val holder = this@Holder
    if (holder is ArgumentHolder) {
        holder += node
    }

    return node
}

/**
 * Creates a new [MemberExpression] in the Fluent Node DSL and invokes [ArgumentHolder.addArgument]
 * of the nearest enclosing [Holder], but only if it is an [ArgumentHolder]. If the [name] doesn't
 * already contain a fqn, we add an implicit "this" as base.
 */
context(Holder<out Statement>)

fun LanguageFrontend.member(name: CharSequence, base: Expression? = null): MemberExpression {
    val parsedName = parseName(name)
    val type =
        if (parsedName.parent != null) {
            UnknownType.getUnknownType()
        } else {
            var scope = ((this@Holder) as? ScopeProvider)?.scope
            while (scope != null && scope !is RecordScope) {
                scope = scope.parent
            }
            val scopeType = scope?.name?.let { t(it) } ?: UnknownType.getUnknownType()
            scopeType
        }
    val memberBase = base ?: memberOrRef(parsedName.parent ?: parseName("this"), type)

    val node = newMemberExpression(name, memberBase)

    // Only add this to an argument holder if the nearest holder is an argument holder
    val holder = this@Holder
    if (holder is ArgumentHolder) {
        holder += node
    }

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
 * Creates a new [BinaryOperator] with a `-` [BinaryOperator.operatorCode] in the Fluent Node DSL
 * and invokes [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(LanguageFrontend, ArgumentHolder)

operator fun Expression.minus(rhs: Expression): BinaryOperator {
    val node = (this@LanguageFrontend).newBinaryOperator("-")
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

/**
 * <<<<<<< HEAD
 * =======
 * Creates a new [BinaryOperator] with a `>` [BinaryOperator.operatorCode] in the Fluent Node DSL
 * and invokes [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(LanguageFrontend, ArgumentHolder)

infix fun Expression.gt(rhs: Expression): BinaryOperator {
    val node = (this@LanguageFrontend).newBinaryOperator(">")
    node.lhs = this
    node.rhs = rhs

    (this@ArgumentHolder) += node

    return node
}

/**
 * Creates a new [BinaryOperator] with a `=` [BinaryOperator.operatorCode] in the Fluent Node DSL
 * and invokes [ArgumentHolder.addArgument] of the nearest enclosing [StatementHolder].
 */
context(LanguageFrontend, StatementHolder)

infix fun Expression.assign(init: BinaryOperator.() -> Expression): BinaryOperator {
    val node = (this@LanguageFrontend).newBinaryOperator("=")
    node.lhs = this
    node.rhs = init(node)

    (this@StatementHolder) += node

    return node
}

/**
 * >>>>>>> main Creates a new [BinaryOperator] with a `=` [BinaryOperator.operatorCode] in the
 * Fluent Node DSL and invokes [ArgumentHolder.addArgument] of the nearest enclosing
 * [StatementHolder].
 */
context(LanguageFrontend, StatementHolder)

infix fun Expression.assign(rhs: Expression): BinaryOperator {
    val node = (this@LanguageFrontend).newBinaryOperator("=")
    node.lhs = this
    node.rhs = rhs

    (this@StatementHolder) += node

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
