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
 * Creates a new [TranslationUnitDecl] in the Fluent Node DSL with the given [name]. The declaration
 * will be set to the [ScopeManager.globalScope]. The [init] block can be used to create further
 * sub-nodes as well as configuring the created node itself.
 */
context(TranslationResult)

fun LanguageFrontend<*, *>.translationUnit(
    name: CharSequence = Node.EMPTY_NAME,
    init: TranslationUnitDecl.() -> Unit
): TranslationUnitDecl {
    val node = (this@LanguageFrontend).newTranslationUnitDecl(name)

    scopeManager.resetToGlobal(node)
    init(node)
    this@TranslationResult.components.firstOrNull()?.translationUnits?.add(node)

    return node
}

/**
 * Creates a new [NamespaceDecl] in the Fluent Node DSL with the given [name]. The declaration will
 * be set to the [ScopeManager.globalScope]. The [init] block can be used to create further
 * sub-nodes as well as configuring the created node itself.
 */
context(DeclarationHolder)

fun LanguageFrontend<*, *>.namespace(
    name: CharSequence,
    init: NamespaceDecl.() -> Unit
): NamespaceDecl {
    val node = (this@LanguageFrontend).newNamespaceDecl(name)

    scopeManager.enterScope(node)
    init(node)
    scopeManager.leaveScope(node)
    scopeManager.addDeclaration(node)
    return node
}

/**
 * Creates a new [RecordDecl] in the Fluent Node DSL with the given [name]. The declaration will be
 * set to the [ScopeManager.currentRecord]. The [init] block can be used to create further sub-nodes
 * as well as configuring the created node itself.
 */
context(DeclarationHolder)

fun LanguageFrontend<*, *>.record(
    name: CharSequence,
    kind: String = "class",
    init: RecordDecl.() -> Unit
): RecordDecl {
    val node = (this@LanguageFrontend).newRecordDecl(name, kind)

    scopeManager.enterScope(node)
    init(node)
    scopeManager.leaveScope(node)
    scopeManager.addDeclaration(node)

    return node
}

/**
 * Creates a new [FieldDecl] in the Fluent Node DSL with the given [name] and optional [type]. The
 * [init] block can be used to create further sub-nodes as well as configuring the created node
 * itself.
 */
context(DeclarationHolder)

fun LanguageFrontend<*, *>.field(
    name: CharSequence,
    type: Type = unknownType(),
    init: FieldDecl.() -> Unit
): FieldDecl {
    val node = newFieldDecl(name)
    node.type = type

    init(node)

    scopeManager.addDeclaration(node)

    return node
}

/**
 * Creates a new [FunctionDecl] in the Fluent Node DSL with the given [name] and optional
 * [returnType]. The [init] block can be used to create further sub-nodes as well as configuring the
 * created node itself.
 */
context(DeclarationHolder)

fun LanguageFrontend<*, *>.function(
    name: CharSequence,
    returnType: Type = unknownType(),
    returnTypes: List<Type>? = null,
    init: (FunctionDecl.() -> Unit)? = null
): FunctionDecl {
    val node = newFunctionDecl(name)

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
 * Creates a new [MethodDecl] in the Fluent Node DSL with the given [name] and optional
 * [returnType]. The [init] block can be used to create further sub-nodes as well as configuring the
 * created node itself.
 */
context(RecordDecl)

fun LanguageFrontend<*, *>.method(
    name: CharSequence,
    returnType: Type = unknownType(),
    init: (MethodDecl.() -> Unit)? = null
): MethodDecl {
    val node = newMethodDecl(name)
    node.returnTypes = listOf(returnType)
    node.type = FunctionType.computeType(node)

    scopeManager.enterScope(node)
    if (init != null) {
        init(node)
    }
    scopeManager.leaveScope(node)

    scopeManager.addDeclaration(node)
    (this@RecordDecl).addMethod(node)

    return node
}

/**
 * Creates a new [ConstructorDecl] in the Fluent Node DSL for the enclosing [RecordDecl]. The [init]
 * block can be used to create further sub-nodes as well as configuring the created node itself.
 */
context(RecordDecl)

fun LanguageFrontend<*, *>.constructor(init: ConstructorDecl.() -> Unit): ConstructorDecl {
    val recordDecl: RecordDecl = this@RecordDecl
    val node = newConstructorDecl(recordDecl.name, recordDecl = recordDecl)

    scopeManager.enterScope(node)
    init(node)
    scopeManager.leaveScope(node)

    scopeManager.addDeclaration(node)
    recordDecl.addConstructor(node)

    return node
}

/**
 * Creates a new [CompoundStmt] in the Fluent Node DSL and sets it to the [FunctionDecl.body] of the
 * nearest enclosing [FunctionDecl]. The [init] block can be used to create further sub-nodes as
 * well as configuring the created node itself.
 */
context(FunctionDecl)

fun LanguageFrontend<*, *>.body(
    needsScope: Boolean = true,
    init: CompoundStmt.() -> Unit
): CompoundStmt {
    val node = newCompoundStmt()

    scopeIfNecessary(needsScope, node, init)
    body = node

    return node
}

/**
 * Creates a new [ParameterDecl] in the Fluent Node DSL and adds it to the [FunctionDecl.parameters]
 * of the nearest enclosing [FunctionDecl]. The [init] block can be used to create further sub-nodes
 * as well as configuring the created node itself.
 */
context(FunctionDecl)

fun LanguageFrontend<*, *>.param(
    name: CharSequence,
    type: Type = unknownType(),
    init: (ParameterDecl.() -> Unit)? = null
): ParameterDecl {
    val node =
        (this@LanguageFrontend).newParameterDecl(
            name,
            type,
        )
    init?.let { it(node) }

    scopeManager.addDeclaration(node)

    return node
}

/**
 * Creates a new [ReturnStmt] in the Fluent Node DSL and adds it to the [StatementHolder.statements]
 * of the nearest enclosing [StatementHolder]. The [init] block can be used to create further
 * sub-nodes as well as configuring the created node itself.
 */
context(StatementHolder)

fun LanguageFrontend<*, *>.returnStmt(init: ReturnStmt.() -> Unit): ReturnStmt {
    val node = (this@LanguageFrontend).newReturnStmt()
    init(node)

    (this@StatementHolder) += node

    return node
}

context(Holder<out Statement>)

fun LanguageFrontend<*, *>.ase(init: (SubscriptionExpr.() -> Unit)? = null): SubscriptionExpr {
    val node = newSubscriptionExpr()

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
 * Creates a new [DeclarationStmt] in the Fluent Node DSL and adds it to the
 * [StatementHolder.statements] of the nearest enclosing [StatementHolder]. The [init] block can be
 * used to create further sub-nodes as well as configuring the created node itself.
 */
context(StatementHolder)

fun LanguageFrontend<*, *>.declare(init: DeclarationStmt.() -> Unit): DeclarationStmt {
    val node = (this@LanguageFrontend).newDeclarationStmt()
    init(node)

    (this@StatementHolder) += node

    return node
}

/**
 * Creates a new [VariableDecl] in the Fluent Node DSL and adds it to the
 * [DeclarationStmt.declarations] of the nearest enclosing [DeclarationStmt]. The [init] block can
 * be used to create further sub-nodes as well as configuring the created node itself.
 */
context(DeclarationStmt)

fun LanguageFrontend<*, *>.variable(
    name: String,
    type: Type = unknownType(),
    init: (VariableDecl.() -> Unit)? = null
): VariableDecl {
    val node = newVariableDecl(name, type)
    if (init != null) init(node)

    addToPropertyEdgeDeclaration(node)

    scopeManager.addDeclaration(node)

    return node
}

/**
 * Creates a new [CallExpr] (or [MemberCallExpr]) in the Fluent Node DSL with the given [name] and
 * adds it to the nearest enclosing [Holder]. Depending on whether it is a [StatementHolder] it is
 * added to the list of [StatementHolder.statements] or in case of an [ArgumentHolder], the function
 * [ArgumentHolder.addArgument] is invoked.
 *
 * The type of expression is determined whether [name] is either a [Name] with a [Name.parent] or if
 * it can be parsed as a FQN in the given language. It also automatically creates either a
 * [Reference] or [MemberExpr] and sets it as the [CallExpr.callee]. The [init] block can be used to
 * create further sub-nodes as well as configuring the created node itself.
 */
context(Holder<out Statement>)

fun LanguageFrontend<*, *>.call(
    name: CharSequence,
    isStatic: Boolean = false,
    init: (CallExpr.() -> Unit)? = null
): CallExpr {
    // Try to parse the name
    val parsedName = parseName(name, ".")
    val node =
        if (parsedName.parent != null) {
            newMemberCallExpr(
                newMemberExpr(parsedName.localName, memberOrRef(parsedName.parent)),
                isStatic
            )
        } else {
            newCallExpr(newReference(parsedName))
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
 * Creates a new [CallExpr] (or [MemberCallExpr]) in the Fluent Node DSL with the given [localName]
 * and adds it to the nearest enclosing [Holder]. Depending on whether it is a [StatementHolder] it
 * is added to the list of [StatementHolder.statements] or in case of an [ArgumentHolder], the
 * function [ArgumentHolder.addArgument] is invoked.
 *
 * The type of expression is determined whether [localName] is either a [Name] with a [Name.parent]
 * or if it can be parsed as a FQN in the given language. It also automatically creates either a
 * [Reference] or [MemberExpr] and sets it as the [CallExpr.callee]. The [init] block can be used to
 * create further sub-nodes as well as configuring the created node itself.
 */
context(Holder<out Statement>)

fun LanguageFrontend<*, *>.memberCall(
    localName: CharSequence,
    member: Expression,
    isStatic: Boolean = false,
    init: (CallExpr.() -> Unit)? = null
): MemberCallExpr {
    // Try to parse the name
    val node = newMemberCallExpr(newMemberExpr(localName, member), isStatic)
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
 * Creates a new [ConstructExpr] in the Fluent Node DSL for the translation record/type with the
 * given [name] and adds it to the nearest enclosing [Holder]. Depending on whether it is a
 * [StatementHolder] it is added to the list of [StatementHolder.statements] or in case of an
 * [ArgumentHolder], the function [ArgumentHolder.addArgument] is invoked. The [init] block can be
 * used to create further sub-nodes as well as configuring the created node itself.
 */
context(Holder<out Statement>)

fun LanguageFrontend<*, *>.construct(
    name: CharSequence,
    init: (ConstructExpr.() -> Unit)? = null
): ConstructExpr {
    val node = newConstructExpr(parseName(name))
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

fun LanguageFrontend<*, *>.cast(castType: Type, init: (CastExpr.() -> Unit)? = null): CastExpr {
    val node = newCastExpr()
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

fun LanguageFrontend<*, *>.new(init: (NewExpr.() -> Unit)? = null): NewExpr {
    val node = newNewExpr()
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
            newMemberExpr(name.localName, memberOrRef(name.parent))
        } else {
            newReference(name.localName)
        }
    if (type !is UnknownType) {
        node.type = type
    }

    return node
}

/**
 * Creates a new [IfStmt] in the Fluent Node DSL and adds it to the [StatementHolder.statements] of
 * the nearest enclosing [StatementHolder]. The [init] block can be used to create further sub-nodes
 * as well as configuring the created node itself.
 */
context(StatementHolder)

fun LanguageFrontend<*, *>.ifStmt(init: IfStmt.() -> Unit): IfStmt {
    val node = newIfStmt()
    init(node)

    (this@StatementHolder) += node

    return node
}

/**
 * Creates a new [ForEachStmt] in the Fluent Node DSL and adds it to the
 * [StatementHolder.statements] of the nearest enclosing [StatementHolder]. The [init] block can be
 * used to create further sub-nodes as well as configuring the created node itself.
 */
context(StatementHolder)

fun LanguageFrontend<*, *>.forEachStmt(init: ForEachStmt.() -> Unit): ForEachStmt {
    val node = newForEachStmt()

    init(node)

    (this@StatementHolder) += node

    return node
}

/**
 * Creates a new [SwitchStmt] in the Fluent Node DSL and adds it to the [StatementHolder.statements]
 * of the nearest enclosing [StatementHolder]. The [init] block can be used to create further
 * sub-nodes as well as configuring the created node itself.
 */
context(StatementHolder)

fun LanguageFrontend<*, *>.switchStmt(
    selector: Expression,
    needsScope: Boolean = true,
    init: SwitchStmt.() -> Unit
): SwitchStmt {
    val node = newSwitchStmt()
    node.selector = selector
    scopeIfNecessary(needsScope, node, init)

    (this@StatementHolder) += node

    return node
}

/**
 * Creates a new [WhileStmt] in the Fluent Node DSL and adds it to the [StatementHolder.statements]
 * of the nearest enclosing [StatementHolder]. The [init] block can be used to create further
 * sub-nodes as well as configuring the created node itself.
 */
context(StatementHolder)

fun LanguageFrontend<*, *>.whileStmt(
    needsScope: Boolean = true,
    init: WhileStmt.() -> Unit
): WhileStmt {
    val node = newWhileStmt()
    scopeIfNecessary(needsScope, node, init)

    (this@StatementHolder) += node

    return node
}

// TODO: Combine the condition functions

/**
 * Configures the [IfStmt.condition] in the Fluent Node DSL of the nearest enclosing [IfStmt]. The
 * [init] block can be used to create further sub-nodes as well as configuring the created node
 * itself.
 */
context(IfStmt)

fun LanguageFrontend<*, *>.condition(init: IfStmt.() -> BinaryOp): BinaryOp {
    return init(this@IfStmt)
}

/**
 * Configures the [WhileStmt.condition] in the Fluent Node DSL of the nearest enclosing [WhileStmt].
 * The [init] block can be used to create further sub-nodes as well as configuring the created node
 * itself.
 */
context(WhileStmt)

fun LanguageFrontend<*, *>.whileCondition(init: WhileStmt.() -> BinaryOp): BinaryOp {
    return init(this@WhileStmt)
}

/**
 * Creates a new [CompoundStmt] in the Fluent Node DSL and sets it to the [IfStmt.thenStatement] of
 * the nearest enclosing [IfStmt]. The [init] block can be used to create further sub-nodes as well
 * as configuring the created node itself.
 */
context(IfStmt)

fun LanguageFrontend<*, *>.thenStmt(
    needsScope: Boolean = true,
    init: CompoundStmt.() -> Unit
): CompoundStmt {
    val node = newCompoundStmt()
    scopeIfNecessary(needsScope, node, init)

    thenStatement = node

    return node
}

/**
 * Creates a new [IfStmt] in the Fluent Node DSL and sets it to the [IfStmt.elseStatement] of the
 * nearest enclosing [IfStmt]. This simulates an `else-if` scenario. The [init] block can be used to
 * create further sub-nodes as well as configuring the created node itself.
 */
context(IfStmt)

fun LanguageFrontend<*, *>.elseIf(init: IfStmt.() -> Unit): IfStmt {
    val node = newIfStmt()
    init(node)

    elseStatement = node

    return node
}

// TODO: Merge the bodies together

/**
 * Creates a new [CompoundStmt] in the Fluent Node DSL and sets it to the [WhileStmt.statement] of
 * the nearest enclosing [WhileStmt]. The [init] block can be used to create further sub-nodes as
 * well as configuring the created node itself.
 */
context(WhileStmt)

fun LanguageFrontend<*, *>.loopBody(init: CompoundStmt.() -> Unit): CompoundStmt {
    val node = newCompoundStmt()
    init(node)
    statement = node

    return node
}
/**
 * Creates a new [CompoundStmt] in the Fluent Node DSL and sets it to the [WhileStmt.statement] of
 * the nearest enclosing [WhileStmt]. The [init] block can be used to create further sub-nodes as
 * well as configuring the created node itself.
 */
context(ForEachStmt)

fun LanguageFrontend<*, *>.loopBody(init: CompoundStmt.() -> Unit): CompoundStmt {
    val node = newCompoundStmt()
    init(node)
    statement = node

    return node
}

/**
 * Creates a new [CompoundStmt] in the Fluent Node DSL and sets it to the [SwitchStmt.statement] of
 * the nearest enclosing [SwitchStmt]. The [init] block can be used to create further sub-nodes as
 * well as configuring the created node itself.
 */
context(SwitchStmt)

fun LanguageFrontend<*, *>.switchBody(init: CompoundStmt.() -> Unit): CompoundStmt {
    val node = newCompoundStmt()
    init(node)
    statement = node

    return node
}

/**
 * Creates a new [CompoundStmt] in the Fluent Node DSL and sets it to the [IfStmt.elseStatement] of
 * the nearest enclosing [IfStmt]. The [init] block can be used to create further sub-nodes as well
 * as configuring the created node itself.
 */
context(IfStmt)

fun LanguageFrontend<*, *>.elseStmt(
    needsScope: Boolean = true,
    init: CompoundStmt.() -> Unit
): CompoundStmt {
    val node = newCompoundStmt()
    scopeIfNecessary(needsScope, node, init)

    elseStatement = node

    return node
}

/**
 * Creates a new [LabelStmt] in the Fluent Node DSL and invokes [StatementHolder.addStatement] of
 * the nearest enclosing [Holder], but only if it is an [StatementHolder].
 */
context(Holder<out Statement>)

fun LanguageFrontend<*, *>.label(
    label: String,
    init: (LabelStmt.() -> Statement)? = null
): LabelStmt {
    val node = newLabelStmt()
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
 * Creates a new [ContinueStmt] in the Fluent Node DSL and invokes [StatementHolder.addStatement] of
 * the nearest enclosing [StatementHolder].
 */
context(StatementHolder)

fun LanguageFrontend<*, *>.continueStmt(label: String? = null): ContinueStmt {
    val node = newContinueStmt()
    node.label = label

    this@StatementHolder += node

    return node
}

/**
 * Creates a new [BreakStmt] in the Fluent Node DSL and invokes [StatementHolder.addStatement] of
 * the nearest enclosing [Holder], but only if it is an [StatementHolder].
 */
context(Holder<out Statement>)

fun LanguageFrontend<*, *>.breakStmt(label: String? = null): BreakStmt {
    val node = newBreakStmt()
    node.label = label

    // Only add this to a statement holder if the nearest holder is a statement holder
    val holder = this@Holder
    if (holder is StatementHolder) {
        holder += node
    }

    return node
}

/**
 * Creates a new [CaseStmt] in the Fluent Node DSL and invokes [StatementHolder.addStatement] of the
 * nearest enclosing [Holder], but only if it is an [StatementHolder].
 */
context(Holder<out Statement>)

fun LanguageFrontend<*, *>.case(caseExpr: Expression? = null): CaseStmt {
    val node = newCaseStmt()
    node.caseExpression = caseExpr

    // Only add this to a statement holder if the nearest holder is a statement holder
    val holder = this@Holder
    if (holder is StatementHolder) {
        holder += node
    }

    return node
}
/**
 * Creates a new [DefaultStmt] in the Fluent Node DSL and invokes [StatementHolder.addStatement] of
 * the nearest enclosing [Holder], but only if it is an [StatementHolder].
 */
context(Holder<out Statement>)

fun LanguageFrontend<*, *>.default(): DefaultStmt {
    val node = newDefaultStmt()

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
 * Creates a new [InitializerListExpr] in the Fluent Node DSL and invokes
 * [ArgumentHolder.addArgument] of the nearest enclosing [Holder], but only if it is an
 * [ArgumentHolder].
 */
context(Holder<out Statement>)

fun LanguageFrontend<*, *>.ile(
    targetType: Type = unknownType(),
    init: (InitializerListExpr.() -> Unit)? = null
): InitializerListExpr {
    val node = newInitializerListExpr(targetType)

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
 * Creates a new [Reference] in the Fluent Node DSL and invokes [ArgumentHolder.addArgument] of the
 * nearest enclosing [Holder], but only if it is an [ArgumentHolder].
 */
context(Holder<out Statement>)

fun LanguageFrontend<*, *>.ref(
    name: CharSequence,
    type: Type = unknownType(),
    init: (Reference.() -> Unit)? = null
): Reference {
    val node = newReference(name)
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
 * Creates a new [MemberExpr] in the Fluent Node DSL and invokes [ArgumentHolder.addArgument] of the
 * nearest enclosing [Holder], but only if it is an [ArgumentHolder]. If the [name] doesn't already
 * contain a fqn, we add an implicit "this" as base.
 */
context(Holder<out Statement>)

fun LanguageFrontend<*, *>.member(
    name: CharSequence,
    base: Expression? = null,
    operatorCode: String = "."
): MemberExpr {
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

    val node = newMemberExpr(name, memberBase, operatorCode = operatorCode)

    // Only add this to an argument holder if the nearest holder is an argument holder
    val holder = this@Holder
    if (holder is ArgumentHolder) {
        holder += node
    }

    return node
}

/**
 * Creates a new [BinaryOp] with a `*` [BinaryOp.operatorCode] in the Fluent Node DSL and invokes
 * [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(LanguageFrontend<*, *>, ArgumentHolder)

operator fun Expression.times(rhs: Expression): BinaryOp {
    val node = (this@LanguageFrontend).newBinaryOp("*")
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
 * Creates a new [BinaryOp] with a `+` [BinaryOp.operatorCode] in the Fluent Node DSL and invokes
 * [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(LanguageFrontend<*, *>, ArgumentHolder)

operator fun Expression.plus(rhs: Expression): BinaryOp {
    val node = (this@LanguageFrontend).newBinaryOp("+")
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
 * Creates a new [BinaryOp] with a `+` [BinaryOp.operatorCode] in the Fluent Node DSL and invokes
 * [StatementHolder.addStatement] of the nearest enclosing [StatementHolder].
 */
context(LanguageFrontend<*, *>, StatementHolder)

operator fun Expression.plusAssign(rhs: Expression) {
    val node = (this@LanguageFrontend).newAssignExpr("+=", listOf(this), listOf(rhs))

    (this@StatementHolder) += node
}

/**
 * Creates a new [BinaryOp] with a `+` [BinaryOp.operatorCode] in the Fluent Node DSL and invokes
 * [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(LanguageFrontend<*, *>, ArgumentHolder)

operator fun Expression.rem(rhs: Expression): BinaryOp {
    val node = (this@LanguageFrontend).newBinaryOp("%")
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
 * Creates a new [BinaryOp] with a `-` [BinaryOp.operatorCode] in the Fluent Node DSL and invokes
 * [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(LanguageFrontend<*, *>, ArgumentHolder)

operator fun Expression.minus(rhs: Expression): BinaryOp {
    val node = (this@LanguageFrontend).newBinaryOp("-")
    node.lhs = this
    node.rhs = rhs

    (this@ArgumentHolder) += node

    return node
}

/**
 * Creates a new [UnaryOp] with a `&` [UnaryOp.operatorCode] in the Fluent Node DSL and invokes
 * [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(LanguageFrontend<*, *>, ArgumentHolder)

fun reference(input: Expression): UnaryOp {
    val node = (this@LanguageFrontend).newUnaryOp("&", false, false)
    node.input = input

    this@ArgumentHolder += node

    return node
}

/**
 * Creates a new [UnaryOp] with a `--` [UnaryOp.operatorCode] in the Fluent Node DSL and invokes
 * [StatementHolder.addStatement] of the nearest enclosing [StatementHolder].
 */
context(LanguageFrontend<*, *>, Holder<out Statement>)

operator fun Expression.dec(): UnaryOp {
    val node = (this@LanguageFrontend).newUnaryOp("--", true, false)
    node.input = this

    if (this@Holder is StatementHolder) {
        this@Holder += node
    }

    return node
}

/**
 * Creates a new [UnaryOp] with a `++` [UnaryOp.operatorCode] in the Fluent Node DSL and invokes
 * [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(LanguageFrontend<*, *>, Holder<out Statement>)

operator fun Expression.inc(): UnaryOp {
    val node = (this@LanguageFrontend).newUnaryOp("++", true, false)
    node.input = this

    if (this@Holder is StatementHolder) {
        this@Holder += node
    }

    return node
}

/**
 * Creates a new [BinaryOp] with a `==` [BinaryOp.operatorCode] in the Fluent Node DSL and invokes
 * [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(LanguageFrontend<*, *>, ArgumentHolder)

infix fun Expression.eq(rhs: Expression): BinaryOp {
    val node = (this@LanguageFrontend).newBinaryOp("==")
    node.lhs = this
    node.rhs = rhs

    (this@ArgumentHolder) += node

    return node
}

/**
 * Creates a new [BinaryOp] with a `>` [BinaryOp.operatorCode] in the Fluent Node DSL and invokes
 * [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(LanguageFrontend<*, *>, ArgumentHolder)

infix fun Expression.gt(rhs: Expression): BinaryOp {
    val node = (this@LanguageFrontend).newBinaryOp(">")
    node.lhs = this
    node.rhs = rhs

    (this@ArgumentHolder) += node

    return node
}

/**
 * Creates a new [BinaryOp] with a `<` [BinaryOp.operatorCode] in the Fluent Node DSL and invokes
 * [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(LanguageFrontend<*, *>, ArgumentHolder)

infix fun Expression.lt(rhs: Expression): BinaryOp {
    val node = (this@LanguageFrontend).newBinaryOp("<")
    node.lhs = this
    node.rhs = rhs

    (this@ArgumentHolder) += node

    return node
}

/**
 * Creates a new [ConditionalExpr] with a `=` [BinaryOp.operatorCode] in the Fluent Node DSL and
 * invokes [StatementHolder.addStatement] of the nearest enclosing [StatementHolder].
 */
context(LanguageFrontend<*, *>, Holder<out Node>)

fun Expression.conditional(
    condition: Expression,
    thenExpr: Expression,
    elseExpr: Expression
): ConditionalExpr {
    val node = (this@LanguageFrontend).newConditionalExpr(condition, thenExpr, elseExpr)

    if (this@Holder is StatementHolder) {
        (this@Holder) += node
    } else if (this@Holder is ArgumentHolder) {
        this@Holder += node
    }

    return node
}

/**
 * Creates a new [BinaryOp] with a `=` [BinaryOp.operatorCode] in the Fluent Node DSL and invokes
 * [StatementHolder.addStatement] of the nearest enclosing [StatementHolder].
 */
context(LanguageFrontend<*, *>, StatementHolder)

infix fun Expression.assign(init: AssignExpr.() -> Expression): AssignExpr {
    val node = (this@LanguageFrontend).newAssignExpr("=")
    node.lhs = listOf(this)
    init(node)
    // node.rhs = listOf(init(node))

    (this@StatementHolder) += node

    return node
}

/**
 * Creates a new [AssignExpr] with a `=` [AssignExpr.operatorCode] in the Fluent Node DSL and
 * invokes [StatementHolder.addStatement] of the nearest enclosing [StatementHolder].
 */
context(LanguageFrontend<*, *>, Holder<out Node>)

infix fun Expression.assign(rhs: Expression): AssignExpr {
    val node = (this@LanguageFrontend).newAssignExpr("=", listOf(this), listOf(rhs))

    if (this@Holder is StatementHolder) {
        this@Holder += node
    }

    return node
}

/**
 * Creates a new [AssignExpr] with a `=` [AssignExpr.operatorCode] in the Fluent Node DSL and
 * invokes [StatementHolder.addStatement] of the nearest enclosing [StatementHolder].
 */
context(LanguageFrontend<*, *>, Holder<out Node>)

infix fun Expression.assignAsExpr(rhs: Expression): AssignExpr {
    val node = (this@LanguageFrontend).newAssignExpr("=", listOf(this), listOf(rhs))

    node.usedAsExpression = true

    return node
}
/**
 * Creates a new [AssignExpr] with a `=` [AssignExpr.operatorCode] in the Fluent Node DSL and
 * invokes [StatementHolder.addStatement] of the nearest enclosing [StatementHolder].
 */
context(LanguageFrontend<*, *>, Holder<out Node>)

infix fun Expression.assignAsExpr(rhs: AssignExpr.() -> Unit): AssignExpr {
    val node = (this@LanguageFrontend).newAssignExpr("=", listOf(this))
    rhs(node)

    node.usedAsExpression = true

    return node
}

/** Creates a new [Type] with the given [name] in the Fluent Node DSL. */
fun LanguageFrontend<*, *>.t(name: CharSequence, init: (Type.() -> Unit)? = null): Type {
    val type = objectType(name)
    if (init != null) {
        init(type)
    }
    return type
}

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
