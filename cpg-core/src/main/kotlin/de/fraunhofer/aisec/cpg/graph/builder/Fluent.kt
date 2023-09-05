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

import de.fraunhofer.aisec.cpg.*
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.scopes.RecordScope
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.FunctionType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.passes.executePassSequential

fun LanguageFrontend<*, *>.translationResult(
    init: TranslationResult.() -> Unit
): TranslationResult {
    val node =
        TranslationResult(
            TranslationManager.builder().config(ctx.config).build(),
            ctx,
        )
    val component = Component()
    node.addComponent(component)
    init(node)

    ctx.config.registeredPasses.forEach { executePassSequential(it, ctx, node, listOf()) }

    return node
}

/**
 * Creates a new [TranslationUnitDeclaration] in the Fluent Node DSL with the given [name]. The
 * declaration will be set to the [ScopeManager.globalScope]. The [init] block can be used to create
 * further sub-nodes as well as configuring the created node itself.
 */
context(TranslationResult)

fun LanguageFrontend<*, *>.translationUnit(
    name: CharSequence = Node.EMPTY_NAME,
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

fun LanguageFrontend<*, *>.namespace(
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

fun LanguageFrontend<*, *>.record(
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

fun LanguageFrontend<*, *>.field(
    name: CharSequence,
    type: Type = unknownType(),
    init: FieldDeclaration.() -> Unit
): FieldDeclaration {
    val node = newFieldDeclaration(name)
    node.type = type

    init(node)

    scopeManager.addDeclaration(node)

    return node
}

/**
 * Creates a new [IncludeDeclaration] and adds it to the surrounding [TranslationUnitDeclaration].
 */
context(TranslationUnitDeclaration)

fun LanguageFrontend<*, *>.import(name: CharSequence): IncludeDeclaration {
    val node = newIncludeDeclaration(name)
    (this@TranslationUnitDeclaration).addDeclaration(node)
    return node
}

/**
 * Creates a new [FunctionDeclaration] in the Fluent Node DSL with the given [name] and optional
 * [returnType]. The [init] block can be used to create further sub-nodes as well as configuring the
 * created node itself.
 */
context(DeclarationHolder)

fun LanguageFrontend<*, *>.function(
    name: CharSequence,
    returnType: Type = unknownType(),
    returnTypes: List<Type>? = null,
    init: (FunctionDeclaration.() -> Unit)? = null
): FunctionDeclaration {
    val node = newFunctionDeclaration(name)

    if (returnTypes != null) {
        node.returnTypes = returnTypes
    } else {
        node.returnTypes = listOf(returnType)
    }

    // Make sure that our function has the correct type
    node.type = FunctionType.computeType(node)

    scopeManager.enterScope(node)
    init?.let { it(node) }
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

fun LanguageFrontend<*, *>.method(
    name: CharSequence,
    returnType: Type = unknownType(),
    init: (MethodDeclaration.() -> Unit)? = null
): MethodDeclaration {
    val node = newMethodDeclaration(name)
    node.returnTypes = listOf(returnType)
    node.type = FunctionType.computeType(node)

    scopeManager.enterScope(node)
    if (init != null) {
        init(node)
    }
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

fun LanguageFrontend<*, *>.constructor(
    init: ConstructorDeclaration.() -> Unit
): ConstructorDeclaration {
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

fun LanguageFrontend<*, *>.body(
    needsScope: Boolean = true,
    init: CompoundStatement.() -> Unit
): CompoundStatement {
    val node = newCompoundStatement()

    scopeIfNecessary(needsScope, node, init)
    body = node

    return node
}

/**
 * Creates a new [CompoundStatement] in the Fluent Node DSL and sets it to the
 * [FunctionDeclaration.body] of the nearest enclosing [FunctionDeclaration]. The [init] block can
 * be used to create further sub-nodes as well as configuring the created node itself.
 */
context(StatementHolder)

fun LanguageFrontend<*, *>.block(
    needsScope: Boolean = true,
    init: CompoundStatement.() -> Unit
): CompoundStatement {
    val node = newCompoundStatement()

    scopeIfNecessary(needsScope, node, init)
    (this@StatementHolder).addStatement(node)

    return node
}

/**
 * Creates a new [ParamVariableDeclaration] in the Fluent Node DSL and adds it to the
 * [FunctionDeclaration.parameters] of the nearest enclosing [FunctionDeclaration]. The [init] block
 * can be used to create further sub-nodes as well as configuring the created node itself.
 */
context(FunctionDeclaration)

fun LanguageFrontend<*, *>.param(
    name: CharSequence,
    type: Type = unknownType(),
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

fun LanguageFrontend<*, *>.returnStmt(init: ReturnStatement.() -> Unit): ReturnStatement {
    val node = (this@LanguageFrontend).newReturnStatement()
    init(node)

    (this@StatementHolder) += node

    return node
}

context(Holder<out Statement>)

fun LanguageFrontend<*, *>.ase(
    init: (ArraySubscriptionExpression.() -> Unit)? = null
): ArraySubscriptionExpression {
    val node = newArraySubscriptionExpression()

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
 * Creates a new [DeclarationStatement] in the Fluent Node DSL and adds it to the
 * [StatementHolder.statements] of the nearest enclosing [StatementHolder]. The [init] block can be
 * used to create further sub-nodes as well as configuring the created node itself.
 */
context(StatementHolder)

fun LanguageFrontend<*, *>.declare(init: DeclarationStatement.() -> Unit): DeclarationStatement {
    val node = (this@LanguageFrontend).newDeclarationStatement()
    init(node)

    (this@StatementHolder) += node

    return node
}

/**
 * Creates a new [DeclarationStatement] in the Fluent Node DSL. The [init] block can be used to
 * create further sub-nodes as well as configuring the created node itself.
 */
fun LanguageFrontend<*, *>.declareVar(
    name: String,
    type: Type,
    init: VariableDeclaration.() -> Unit
): DeclarationStatement {
    val node = (this@LanguageFrontend).newDeclarationStatement()
    val variableDecl = newVariableDeclaration(name, type)
    init(variableDecl)
    node.singleDeclaration = variableDecl

    return node
}

/**
 * Creates a new [VariableDeclaration] in the Fluent Node DSL and adds it to the
 * [DeclarationStatement.declarations] of the nearest enclosing [DeclarationStatement]. The [init]
 * block can be used to create further sub-nodes as well as configuring the created node itself.
 */
context(DeclarationStatement)

fun LanguageFrontend<*, *>.variable(
    name: String,
    type: Type = unknownType(),
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

fun LanguageFrontend<*, *>.call(
    name: CharSequence,
    isStatic: Boolean = false,
    init: (CallExpression.() -> Unit)? = null
): CallExpression {
    // Try to parse the name
    val parsedName = parseName(name, ".")
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

fun LanguageFrontend<*, *>.memberCall(
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

fun LanguageFrontend<*, *>.construct(
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

fun LanguageFrontend<*, *>.cast(
    castType: Type,
    init: (CastExpression.() -> Unit)? = null
): CastExpression {
    val node = newCastExpression()
    node.castType = castType
    if (init != null) init(node)

    val holder = this@Holder
    if (holder is StatementHolder) {
        holder += node
    } else if (holder is ArgumentHolder) {
        holder += node
    }
    return node
}

context(Holder<out Statement>)

fun LanguageFrontend<*, *>.new(init: (NewExpression.() -> Unit)? = null): NewExpression {
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

fun LanguageFrontend<*, *>.memberOrRef(name: Name, type: Type = unknownType()): Expression {
    val node =
        if (name.parent != null) {
            newMemberExpression(name.localName, memberOrRef(name.parent))
        } else {
            newDeclaredReferenceExpression(name.localName)
        }
    if (type !is UnknownType) {
        node.type = type
    }

    return node
}

/**
 * Creates a new [IfStatement] in the Fluent Node DSL and adds it to the
 * [StatementHolder.statements] of the nearest enclosing [StatementHolder]. The [init] block can be
 * used to create further sub-nodes as well as configuring the created node itself.
 */
context(StatementHolder)

fun LanguageFrontend<*, *>.ifStmt(init: IfStatement.() -> Unit): IfStatement {
    val node = newIfStatement()
    init(node)

    (this@StatementHolder) += node

    return node
}

/**
 * Creates a new [ForEachStatement] in the Fluent Node DSL and adds it to the
 * [StatementHolder.statements] of the nearest enclosing [StatementHolder]. The [init] block can be
 * used to create further sub-nodes as well as configuring the created node itself.
 */
context(StatementHolder)

fun LanguageFrontend<*, *>.forEachStmt(init: ForEachStatement.() -> Unit): ForEachStatement {
    val node = newForEachStatement()

    init(node)

    (this@StatementHolder) += node

    return node
}

/**
 * Creates a new [ForStatement] in the Fluent Node DSL and adds it to the
 * [StatementHolder.statements] of the nearest enclosing [StatementHolder]. The [init] block can be
 * used to create further sub-nodes as well as configuring the created node itself.
 */
context(StatementHolder)

fun LanguageFrontend<*, *>.forStmt(
    initializer: DeclarationStatement,
    condition: Expression,
    iteration: Statement,
    init: CompoundStatement.() -> Unit
): ForStatement {
    val node = newForStatement()
    node.initializerStatement = initializer
    if (initializer.isSingleDeclaration()) {

        scopeManager.addDeclaration(initializer.singleDeclaration, false)
    }
    node.condition = condition
    node.iterationStatement = iteration

    val body = newCompoundStatement()
    init(body)
    node.statement = body

    (this@StatementHolder) += node

    return node
}

/**
 * Creates a new [SwitchStatement] in the Fluent Node DSL and adds it to the
 * [StatementHolder.statements] of the nearest enclosing [StatementHolder]. The [init] block can be
 * used to create further sub-nodes as well as configuring the created node itself.
 */
context(StatementHolder)

fun LanguageFrontend<*, *>.switchStmt(
    selector: Expression,
    needsScope: Boolean = true,
    init: SwitchStatement.() -> Unit
): SwitchStatement {
    val node = newSwitchStatement()
    node.selector = selector
    scopeIfNecessary(needsScope, node, init)

    (this@StatementHolder) += node

    return node
}

/**
 * Creates a new [WhileStatement] in the Fluent Node DSL and adds it to the
 * [StatementHolder.statements] of the nearest enclosing [StatementHolder]. The [init] block can be
 * used to create further sub-nodes as well as configuring the created node itself.
 */
context(StatementHolder)

fun LanguageFrontend<*, *>.whileStmt(
    needsScope: Boolean = true,
    init: WhileStatement.() -> Unit
): WhileStatement {
    val node = newWhileStatement()
    scopeIfNecessary(needsScope, node, init)

    (this@StatementHolder) += node

    return node
}

/**
 * Creates a new [DoStatement] in the Fluent Node DSL and adds it to the
 * [StatementHolder.statements] of the nearest enclosing [StatementHolder]. The [init] block can be
 * used to create further sub-nodes as well as configuring the created node itself.
 */
context(StatementHolder)

fun LanguageFrontend<*, *>.doStmt(
    needsScope: Boolean = true,
    init: DoStatement.() -> Unit
): DoStatement {
    val node = newDoStatement()
    scopeIfNecessary(needsScope, node, init)

    (this@StatementHolder) += node

    return node
}

// TODO: Combine the condition functions

/**
 * Configures the [IfStatement.condition] in the Fluent Node DSL of the nearest enclosing
 * [IfStatement]. The [init] block can be used to create further sub-nodes as well as configuring
 * the created node itself.
 */
context(IfStatement)

fun LanguageFrontend<*, *>.condition(init: IfStatement.() -> Expression): Expression {
    return init(this@IfStatement)
}

/**
 * Configures the [WhileStatement.condition] in the Fluent Node DSL of the nearest enclosing
 * [WhileStatement]. The [init] block can be used to create further sub-nodes as well as configuring
 * the created node itself.
 */
context(WhileStatement)

fun LanguageFrontend<*, *>.whileCondition(init: WhileStatement.() -> Expression): Expression {
    return init(this@WhileStatement)
}

/**
 * Configures the [DoStatement.condition] in the Fluent Node DSL of the nearest enclosing
 * [DoStatement]. The [init] block can be used to create further sub-nodes as well as configuring
 * the created node itself.
 */
context(DoStatement)

fun LanguageFrontend<*, *>.whileCondition(init: DoStatement.() -> Expression): Expression {
    return init(this@DoStatement)
}

/**
 * Creates a new [CompoundStatement] in the Fluent Node DSL and sets it to the
 * [IfStatement.thenStatement] of the nearest enclosing [IfStatement]. The [init] block can be used
 * to create further sub-nodes as well as configuring the created node itself.
 */
context(IfStatement)

fun LanguageFrontend<*, *>.thenStmt(
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

fun LanguageFrontend<*, *>.elseIf(init: IfStatement.() -> Unit): IfStatement {
    val node = newIfStatement()
    init(node)

    elseStatement = node

    return node
}

// TODO: Merge the bodies together

/**
 * Creates a new [CompoundStatement] in the Fluent Node DSL and sets it to the
 * [WhileStatement.statement] of the nearest enclosing [WhileStatement]. The [init] block can be
 * used to create further sub-nodes as well as configuring the created node itself.
 */
context(WhileStatement)

fun LanguageFrontend<*, *>.loopBody(init: CompoundStatement.() -> Unit): CompoundStatement {
    val node = newCompoundStatement()
    init(node)
    statement = node

    return node
}

/**
 * Creates a new [CompoundStatement] in the Fluent Node DSL and sets it to the
 * [DoStatement.statement] of the nearest enclosing [DoStatement]. The [init] block can be used to
 * create further sub-nodes as well as configuring the created node itself.
 */
context(DoStatement)

fun LanguageFrontend<*, *>.loopBody(init: CompoundStatement.() -> Unit): CompoundStatement {
    val node = newCompoundStatement()
    init(node)
    statement = node

    return node
}

/**
 * Creates a new [CompoundStatement] in the Fluent Node DSL and sets it to the
 * [WhileStatement.statement] of the nearest enclosing [WhileStatement]. The [init] block can be
 * used to create further sub-nodes as well as configuring the created node itself.
 */
context(ForEachStatement)

fun LanguageFrontend<*, *>.loopBody(init: CompoundStatement.() -> Unit): CompoundStatement {
    val node = newCompoundStatement()
    init(node)
    statement = node

    return node
}

/**
 * Creates a new [CompoundStatement] in the Fluent Node DSL and sets it to the
 * [SwitchStatement.statement] of the nearest enclosing [SwitchStatement]. The [init] block can be
 * used to create further sub-nodes as well as configuring the created node itself.
 */
context(SwitchStatement)

fun LanguageFrontend<*, *>.switchBody(init: CompoundStatement.() -> Unit): CompoundStatement {
    val node = newCompoundStatement()
    init(node)
    statement = node

    return node
}

/**
 * Creates a new [CompoundStatement] in the Fluent Node DSL and sets it to the
 * [IfStatement.elseStatement] of the nearest enclosing [IfStatement]. The [init] block can be used
 * to create further sub-nodes as well as configuring the created node itself.
 */
context(IfStatement)

fun LanguageFrontend<*, *>.elseStmt(
    needsScope: Boolean = true,
    init: CompoundStatement.() -> Unit
): CompoundStatement {
    val node = newCompoundStatement()
    scopeIfNecessary(needsScope, node, init)

    elseStatement = node

    return node
}

/**
 * Creates a new [LabelStatement] in the Fluent Node DSL and invokes [StatementHolder.addStatement]
 * of the nearest enclosing [Holder], but only if it is an [StatementHolder].
 */
context(Holder<out Statement>)

fun LanguageFrontend<*, *>.label(
    label: String,
    init: (LabelStatement.() -> Statement)? = null
): LabelStatement {
    val node = newLabelStatement()
    node.label = label
    if (init != null) {
        node.subStatement = init(node)
    }

    // Only add this to a statement holder if the nearest holder is a statement holder
    val holder = this@Holder
    if (holder is StatementHolder) {
        holder += node
    }

    return node
}

/**
 * Creates a new [ContinueStatement] in the Fluent Node DSL and invokes
 * [StatementHolder.addStatement] of the nearest enclosing [StatementHolder].
 */
context(StatementHolder)

fun LanguageFrontend<*, *>.continueStmt(label: String? = null): ContinueStatement {
    val node = newContinueStatement()
    node.label = label

    this@StatementHolder += node

    return node
}

/**
 * Creates a new [BreakStatement] in the Fluent Node DSL and invokes [StatementHolder.addStatement]
 * of the nearest enclosing [Holder], but only if it is an [StatementHolder].
 */
context(Holder<out Statement>)

fun LanguageFrontend<*, *>.breakStmt(label: String? = null): BreakStatement {
    val node = newBreakStatement()
    node.label = label

    // Only add this to a statement holder if the nearest holder is a statement holder
    val holder = this@Holder
    if (holder is StatementHolder) {
        holder += node
    }

    return node
}

/**
 * Creates a new [CaseStatement] in the Fluent Node DSL and invokes [StatementHolder.addStatement]
 * of the nearest enclosing [Holder], but only if it is an [StatementHolder].
 */
context(Holder<out Statement>)

fun LanguageFrontend<*, *>.case(caseExpr: Expression? = null): CaseStatement {
    val node = newCaseStatement()
    node.caseExpression = caseExpr

    // Only add this to a statement holder if the nearest holder is a statement holder
    val holder = this@Holder
    if (holder is StatementHolder) {
        holder += node
    }

    return node
}
/**
 * Creates a new [DefaultStatement] in the Fluent Node DSL and invokes
 * [StatementHolder.addStatement] of the nearest enclosing [Holder], but only if it is an
 * [StatementHolder].
 */
context(Holder<out Statement>)

fun LanguageFrontend<*, *>.default(): DefaultStatement {
    val node = newDefaultStatement()

    // Only add this to a statement holder if the nearest holder is a statement holder
    val holder = this@Holder
    if (holder is StatementHolder) {
        holder += node
    }

    return node
}

/**
 * Creates a new [Literal] in the Fluent Node DSL and invokes [ArgumentHolder.addArgument] of the
 * nearest enclosing [Holder], but only if it is an [ArgumentHolder].
 */
context(Holder<out Statement>)

fun <N> LanguageFrontend<*, *>.literal(value: N, type: Type = unknownType()): Literal<N> {
    val node = newLiteral(value, type)

    // Only add this to an argument holder if the nearest holder is an argument holder
    val holder = this@Holder
    if (holder is ArgumentHolder) {
        holder += node
    }

    return node
}

/**
 * Creates a new [InitializerListExpression] in the Fluent Node DSL and invokes
 * [ArgumentHolder.addArgument] of the nearest enclosing [Holder], but only if it is an
 * [ArgumentHolder].
 */
context(Holder<out Statement>)

fun LanguageFrontend<*, *>.ile(
    targetType: Type = unknownType(),
    init: (InitializerListExpression.() -> Unit)? = null
): InitializerListExpression {
    val node = newInitializerListExpression(targetType)

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
 * Creates a new [DeclaredReferenceExpression] in the Fluent Node DSL and invokes
 * [ArgumentHolder.addArgument] of the nearest enclosing [Holder], but only if it is an
 * [ArgumentHolder].
 */
context(Holder<out Statement>)

fun LanguageFrontend<*, *>.ref(
    name: CharSequence,
    type: Type = unknownType(),
    init: (DeclaredReferenceExpression.() -> Unit)? = null,
    makeMagic: Boolean = true
): DeclaredReferenceExpression {
    val node = newDeclaredReferenceExpression(name)
    node.type = type

    if (init != null) {
        init(node)
    }

    if (makeMagic) {
        // Only add this to an argument holder if the nearest holder is an argument holder
        val holder = this@Holder
        if (holder is ArgumentHolder) {
            holder += node
        }
    }

    return node
}

/**
 * Creates a new [MemberExpression] in the Fluent Node DSL and invokes [ArgumentHolder.addArgument]
 * of the nearest enclosing [Holder], but only if it is an [ArgumentHolder]. If the [name] doesn't
 * already contain a fqn, we add an implicit "this" as base.
 */
context(Holder<out Statement>)

fun LanguageFrontend<*, *>.member(
    name: CharSequence,
    base: Expression? = null,
    operatorCode: String = "."
): MemberExpression {
    val parsedName = parseName(name)
    val type =
        if (parsedName.parent != null) {
            unknownType()
        } else {
            var scope = ((this@Holder) as? ScopeProvider)?.scope
            while (scope != null && scope !is RecordScope) {
                scope = scope.parent
            }
            val scopeType = scope?.name?.let { t(it) } ?: unknownType()
            scopeType
        }
    val memberBase = base ?: memberOrRef(parsedName.parent ?: parseName("this"), type)

    val node = newMemberExpression(name, memberBase, operatorCode = operatorCode)

    // Only add this to an argument holder if the nearest holder is an argument holder
    val holder = this@Holder
    if (holder is ArgumentHolder) {
        holder += node
    }

    return node
}

/**
 * Creates a new [BinaryOperator] with a `*` [BinaryOperator.operatorCode] in the Fluent Node DSL
 * and invokes [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(LanguageFrontend<*, *>, ArgumentHolder)

operator fun Expression.times(rhs: Expression): BinaryOperator {
    val node = (this@LanguageFrontend).newBinaryOperator("*")
    node.lhs = this
    node.rhs = rhs

    (this@ArgumentHolder) += node

    // We need to do a little trick here. Because of the evaluation order, lhs and rhs might also
    // been added to the argument holders arguments (and we do not want that). However, we cannot
    // prevent it, so we need to remove them again
    (this@ArgumentHolder) -= node.lhs
    (this@ArgumentHolder) -= node.rhs

    return node
}

/**
 * Creates a new [UnaryOperator] with a `-` [UnaryOperator.operatorCode] in the Fluent Node DSL and
 * invokes [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(LanguageFrontend<*, *>, ArgumentHolder)

operator fun Expression.unaryMinus(): UnaryOperator {
    val node = (this@LanguageFrontend).newUnaryOperator("-", false, false)
    node.input = this

    (this@ArgumentHolder) += node

    // We need to do a little trick here. Because of the evaluation order, lhs and rhs might also
    // been added to the argument holders arguments (and we do not want that). However, we cannot
    // prevent it, so we need to remove them again
    (this@ArgumentHolder) -= node.input

    return node
}

/**
 * Creates a new [BinaryOperator] with a `/` [BinaryOperator.operatorCode] in the Fluent Node DSL
 * and invokes [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(LanguageFrontend<*, *>, ArgumentHolder)

operator fun Expression.div(rhs: Expression): BinaryOperator {
    val node = (this@LanguageFrontend).newBinaryOperator("/")
    node.lhs = this
    node.rhs = rhs

    (this@ArgumentHolder) += node

    // We need to do a little trick here. Because of the evaluation order, lhs and rhs might also
    // been added to the argument holders arguments (and we do not want that). However, we cannot
    // prevent it, so we need to remove them again
    (this@ArgumentHolder) -= node.lhs
    (this@ArgumentHolder) -= node.rhs

    return node
}

/**
 * Creates a new [BinaryOperator] with a `+` [BinaryOperator.operatorCode] in the Fluent Node DSL
 * and invokes [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(LanguageFrontend<*, *>, ArgumentHolder)

operator fun Expression.plus(rhs: Expression): BinaryOperator {
    val node = (this@LanguageFrontend).newBinaryOperator("+")
    node.lhs = this
    node.rhs = rhs

    (this@ArgumentHolder) += node

    // We need to do a little trick here. Because of the evaluation order, lhs and rhs might also
    // been added to the argument holders arguments (and we do not want that). However, we cannot
    // prevent it, so we need to remove them again
    (this@ArgumentHolder) -= node.lhs
    (this@ArgumentHolder) -= node.rhs

    return node
}

/**
 * Creates a new [BinaryOperator] with a `+` [BinaryOperator.operatorCode] in the Fluent Node DSL
 * and invokes [StatementHolder.addStatement] of the nearest enclosing [StatementHolder].
 */
context(LanguageFrontend<*, *>, StatementHolder)

operator fun Expression.plusAssign(rhs: Expression) {
    val node = (this@LanguageFrontend).newAssignExpression("+=", listOf(this), listOf(rhs))

    (this@StatementHolder) += node
}

/**
 * Creates a new [BinaryOperator] with a `+` [BinaryOperator.operatorCode] in the Fluent Node DSL
 * and invokes [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(LanguageFrontend<*, *>, ArgumentHolder)

operator fun Expression.rem(rhs: Expression): BinaryOperator {
    val node = (this@LanguageFrontend).newBinaryOperator("%")
    node.lhs = this
    node.rhs = rhs

    (this@ArgumentHolder) += node

    // We need to do a little trick here. Because of the evaluation order, lhs and rhs might also
    // been added to the argument holders arguments (and we do not want that). However, we cannot
    // prevent it, so we need to remove them again
    (this@ArgumentHolder) -= node.lhs
    (this@ArgumentHolder) -= node.rhs

    return node
}

/**
 * Creates a new [BinaryOperator] with a `-` [BinaryOperator.operatorCode] in the Fluent Node DSL
 * and invokes [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(LanguageFrontend<*, *>, ArgumentHolder)

operator fun Expression.minus(rhs: Expression): BinaryOperator {
    val node = (this@LanguageFrontend).newBinaryOperator("-")
    node.lhs = this
    node.rhs = rhs

    (this@ArgumentHolder) += node

    return node
}

/**
 * Creates a new [UnaryOperator] with a `&` [UnaryOperator.operatorCode] in the Fluent Node DSL and
 * invokes [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(LanguageFrontend<*, *>, ArgumentHolder)

fun reference(input: Expression): UnaryOperator {
    val node = (this@LanguageFrontend).newUnaryOperator("&", false, false)
    node.input = input

    this@ArgumentHolder += node

    return node
}

/**
 * Creates a new [UnaryOperator] with a `--` [UnaryOperator.operatorCode] in the Fluent Node DSL and
 * invokes [StatementHolder.addStatement] of the nearest enclosing [StatementHolder].
 */
context(LanguageFrontend<*, *>, Holder<out Statement>)

operator fun Expression.dec(): UnaryOperator {
    val node = (this@LanguageFrontend).newUnaryOperator("--", true, false)
    node.input = this

    if (this@Holder is StatementHolder) {
        this@Holder += node
    }

    return node
}

/**
 * Creates a new [UnaryOperator] with a `++` [UnaryOperator.operatorCode] in the Fluent Node DSL and
 * invokes [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(LanguageFrontend<*, *>, Holder<out Statement>)

operator fun Expression.inc(): UnaryOperator {
    val node = (this@LanguageFrontend).newUnaryOperator("++", true, false)
    node.input = this

    if (this@Holder is StatementHolder) {
        this@Holder += node
    }

    return node
}

/**
 * Creates a new [UnaryOperator] with a `++` [UnaryOperator.operatorCode] in the Fluent Node DSL and
 * invokes [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(LanguageFrontend<*, *>)

fun Expression.incNoContext(): UnaryOperator {
    val node = (this@LanguageFrontend).newUnaryOperator("++", true, false)
    node.input = this

    return node
}

/**
 * Creates a new [BinaryOperator] with a `==` [BinaryOperator.operatorCode] in the Fluent Node DSL
 * and invokes [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(LanguageFrontend<*, *>, ArgumentHolder)

infix fun Expression.eq(rhs: Expression): BinaryOperator {
    val node = (this@LanguageFrontend).newBinaryOperator("==")
    node.lhs = this
    node.rhs = rhs

    (this@ArgumentHolder) += node

    return node
}

/**
 * Creates a new [BinaryOperator] with a `>` [BinaryOperator.operatorCode] in the Fluent Node DSL
 * and invokes [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(LanguageFrontend<*, *>, ArgumentHolder)

infix fun Expression.gt(rhs: Expression): BinaryOperator {
    val node = (this@LanguageFrontend).newBinaryOperator(">")
    node.lhs = this
    node.rhs = rhs

    (this@ArgumentHolder) += node

    return node
}

/**
 * Creates a new [BinaryOperator] with a `>=` [BinaryOperator.operatorCode] in the Fluent Node DSL
 * and invokes [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(LanguageFrontend<*, *>, ArgumentHolder)

infix fun Expression.ge(rhs: Expression): BinaryOperator {
    val node = (this@LanguageFrontend).newBinaryOperator(">=")
    node.lhs = this
    node.rhs = rhs

    (this@ArgumentHolder) += node

    return node
}

/**
 * Creates a new [BinaryOperator] with a `<` [BinaryOperator.operatorCode] in the Fluent Node DSL
 * and invokes [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(LanguageFrontend<*, *>, Holder<out Node>)

infix fun Expression.lt(rhs: Expression): BinaryOperator {
    val node = (this@LanguageFrontend).newBinaryOperator("<")
    node.lhs = this
    node.rhs = rhs

    if (this@Holder is ArgumentHolder) {
        this@Holder += node
    }

    return node
}

context(LanguageFrontend<*, *>)

infix fun Expression.lt(rhs: Expression): BinaryOperator {
    val node = (this@LanguageFrontend).newBinaryOperator("<")
    node.lhs = this
    node.rhs = rhs

    return node
}

/**
 * Creates a new [BinaryOperator] with a `<=` [BinaryOperator.operatorCode] in the Fluent Node DSL
 * and invokes [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(LanguageFrontend<*, *>, ArgumentHolder)

infix fun Expression.le(rhs: Expression): BinaryOperator {
    val node = (this@LanguageFrontend).newBinaryOperator("<=")
    node.lhs = this
    node.rhs = rhs

    (this@ArgumentHolder) += node

    return node
}

/**
 * Creates a new [ConditionalExpression] with a `=` [BinaryOperator.operatorCode] in the Fluent Node
 * DSL and invokes [StatementHolder.addStatement] of the nearest enclosing [StatementHolder].
 */
context(LanguageFrontend<*, *>, Holder<out Node>)

fun Expression.conditional(
    condition: Expression,
    thenExpr: Expression,
    elseExpr: Expression
): ConditionalExpression {
    val node = (this@LanguageFrontend).newConditionalExpression(condition, thenExpr, elseExpr)

    if (this@Holder is StatementHolder) {
        (this@Holder) += node
    } else if (this@Holder is ArgumentHolder) {
        this@Holder += node
    }

    return node
}

/**
 * Creates a new [BinaryOperator] with a `=` [BinaryOperator.operatorCode] in the Fluent Node DSL
 * and invokes [StatementHolder.addStatement] of the nearest enclosing [StatementHolder].
 */
context(LanguageFrontend<*, *>, StatementHolder)

infix fun Expression.assign(init: AssignExpression.() -> Expression): AssignExpression {
    val node = (this@LanguageFrontend).newAssignExpression("=")
    node.lhs = listOf(this)
    init(node)
    // node.rhs = listOf(init(node))

    (this@StatementHolder) += node

    return node
}

/**
 * Creates a new [AssignExpression] with a `=` [AssignExpression.operatorCode] in the Fluent Node
 * DSL and invokes [StatementHolder.addStatement] of the nearest enclosing [StatementHolder].
 */
context(LanguageFrontend<*, *>, Holder<out Node>)

infix fun Expression.assign(rhs: Expression): AssignExpression {
    val node = (this@LanguageFrontend).newAssignExpression("=", listOf(this), listOf(rhs))

    if (this@Holder is StatementHolder) {
        this@Holder += node
    }

    return node
}

/**
 * Creates a new [AssignExpression] with a `+=` [AssignExpression.operatorCode] in the Fluent Node
 * DSL and invokes [StatementHolder.addStatement] of the nearest enclosing [StatementHolder].
 */
context(LanguageFrontend<*, *>, Holder<out Node>)

infix fun Expression.assignPlus(rhs: Expression): AssignExpression {
    val node = (this@LanguageFrontend).newAssignExpression("+=", listOf(this), listOf(rhs))

    if (this@Holder is StatementHolder) {
        this@Holder += node
    }

    return node
}

/**
 * Creates a new [AssignExpression] with a `=` [AssignExpression.operatorCode] in the Fluent Node
 * DSL and invokes [StatementHolder.addStatement] of the nearest enclosing [StatementHolder].
 */
context(LanguageFrontend<*, *>, Holder<out Node>)

infix fun Expression.assignAsExpr(rhs: Expression): AssignExpression {
    val node = (this@LanguageFrontend).newAssignExpression("=", listOf(this), listOf(rhs))

    node.usedAsExpression = true

    return node
}
/**
 * Creates a new [AssignExpression] with a `=` [AssignExpression.operatorCode] in the Fluent Node
 * DSL and invokes [StatementHolder.addStatement] of the nearest enclosing [StatementHolder].
 */
context(LanguageFrontend<*, *>, Holder<out Node>)

infix fun Expression.assignAsExpr(rhs: AssignExpression.() -> Unit): AssignExpression {
    val node = (this@LanguageFrontend).newAssignExpression("=", listOf(this))
    rhs(node)

    node.usedAsExpression = true

    return node
}

/** Creates a new [Type] with the given [name] in the Fluent Node DSL. */
fun LanguageFrontend<*, *>.t(name: CharSequence, generics: List<Type> = listOf()) =
    objectType(name, generics)

/** Creates a new [Type] with the given [name] in the Fluent Node DSL. */
fun LanguageFrontend<*, *>.void() = incompleteType()

/**
 * Internally used to enter a new scope if [needsScope] is true before invoking [init] and leaving
 * it afterwards.
 */
private fun <T : Node> LanguageFrontend<*, *>.scopeIfNecessary(
    needsScope: Boolean,
    node: T,
    init: T.() -> Unit
) {
    if (needsScope) {
        scopeManager.enterScope(node)
    }
    init(node)
    if (needsScope) {
        scopeManager.leaveScope(node)
    }
}
