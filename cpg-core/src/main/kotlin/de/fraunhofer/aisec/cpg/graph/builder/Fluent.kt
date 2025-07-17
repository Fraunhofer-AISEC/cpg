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
@file:Suppress("CONTEXT_RECEIVERS_DEPRECATED")

package de.fraunhofer.aisec.cpg.graph.builder

import de.fraunhofer.aisec.cpg.*
import de.fraunhofer.aisec.cpg.TranslationResult.Companion.DEFAULT_APPLICATION_NAME
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.graph.*
import de.fraunhofer.aisec.cpg.graph.declarations.*
import de.fraunhofer.aisec.cpg.graph.scopes.RecordScope
import de.fraunhofer.aisec.cpg.graph.statements.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CollectionComprehension
import de.fraunhofer.aisec.cpg.graph.types.FunctionType.Companion.computeType
import de.fraunhofer.aisec.cpg.graph.types.IncompleteType
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.UnknownType
import de.fraunhofer.aisec.cpg.passes.executePassesInParallel
import de.fraunhofer.aisec.cpg.passes.executePassesSequentially
import de.fraunhofer.aisec.cpg.sarif.PhysicalLocation
import de.fraunhofer.aisec.cpg.sarif.Region
import java.net.URI

fun LanguageFrontend<*, *>.translationResult(
    init: TranslationResult.() -> Unit
): TranslationResult {
    val node = TranslationResult(TranslationManager.builder().config(ctx.config).build(), ctx)
    val component = Component()
    component.name = Name(DEFAULT_APPLICATION_NAME)
    node.addComponent(component)
    init(node)

    if (ctx.config.useParallelPasses) {
        for (list in ctx.config.registeredPasses) {
            executePassesInParallel(list, ctx, node, listOf())
        }
    } else {
        executePassesSequentially(ctx, node, mutableSetOf())
    }

    return node
}

/**
 * Creates a new [TranslationUnitDeclaration] in the Fluent Node DSL with the given [name]. The
 * declaration will be set to the [ScopeManager.globalScope]. The [init] block can be used to create
 * further sub-nodes as well as configuring the created node itself.
 */
context(result: TranslationResult)
fun LanguageFrontend<*, *>.translationUnit(
    name: CharSequence = Node.EMPTY_NAME,
    init: TranslationUnitDeclaration.() -> Unit,
): TranslationUnitDeclaration {
    val node = with(this) { newTranslationUnitDeclaration(name) }

    scopeManager.resetToGlobal(node)
    init(node)
    result.components.firstOrNull()?.translationUnits?.add(node)

    return node
}

/**
 * Creates a new [NamespaceDeclaration] in the Fluent Node DSL with the given [name]. The
 * declaration will be set to the [ScopeManager.globalScope]. The [init] block can be used to create
 * further sub-nodes as well as configuring the created node itself.
 */
context(holder: DeclarationHolder)
fun LanguageFrontend<*, *>.namespace(
    name: CharSequence,
    init: NamespaceDeclaration.() -> Unit,
): NamespaceDeclaration {
    val node = newNamespaceDeclaration(name)

    scopeManager.enterScope(node)
    init(node)
    scopeManager.leaveScope(node)
    scopeManager.addDeclaration(node)
    holder.addDeclaration(node)

    return node
}

/**
 * Creates a new [RecordDeclaration] in the Fluent Node DSL with the given [name]. The declaration
 * will be set to the [ScopeManager.currentRecord]. The [init] block can be used to create further
 * sub-nodes as well as configuring the created node itself.
 */
context(holder: DeclarationHolder)
fun LanguageFrontend<*, *>.record(
    name: CharSequence,
    kind: String = "class",
    init: RecordDeclaration.() -> Unit,
): RecordDeclaration {
    val node = newRecordDeclaration(name, kind)

    scopeManager.enterScope(node)
    init(node)
    scopeManager.leaveScope(node)
    scopeManager.addDeclaration(node)
    holder.addDeclaration(node)

    return node
}

/**
 * Creates a new [FieldDeclaration] in the Fluent Node DSL with the given [name] and optional
 * [type]. The [init] block can be used to create further sub-nodes as well as configuring the
 * created node itself.
 */
context(holder: DeclarationHolder)
fun LanguageFrontend<*, *>.field(
    name: CharSequence,
    type: Type = unknownType(),
    init: (FieldDeclaration.() -> Unit)? = null,
): FieldDeclaration {
    val node = newFieldDeclaration(name)
    node.type = type

    if (init != null) {
        init(node)
    }

    scopeManager.addDeclaration(node)
    holder.addDeclaration(node)

    return node
}

/**
 * Creates a new [IncludeDeclaration] and adds it to the surrounding [TranslationUnitDeclaration].
 */
context(tu: TranslationUnitDeclaration)
fun LanguageFrontend<*, *>.import(name: CharSequence): IncludeDeclaration {
    val node = newIncludeDeclaration(name)
    (tu).addDeclaration(node)
    return node
}

/**
 * Creates a new [FunctionDeclaration] in the Fluent Node DSL with the given [name] and optional
 * [returnType]. The [init] block can be used to create further sub-nodes as well as configuring the
 * created node itself.
 */
context(holder: DeclarationHolder)
fun LanguageFrontend<*, *>.function(
    name: CharSequence,
    returnType: Type = unknownType(),
    returnTypes: List<Type>? = null,
    init: (FunctionDeclaration.() -> Unit)? = null,
): FunctionDeclaration {
    val node = newFunctionDeclaration(name)

    if (returnTypes != null) {
        node.returnTypes = returnTypes
    } else {
        node.returnTypes = listOf(returnType)
    }

    // Make sure that our function has the correct type
    node.type = with(node) { computeType(node) }

    scopeManager.enterScope(node)
    init?.let { it(node) }
    scopeManager.leaveScope(node)

    scopeManager.addDeclaration(node)
    holder.addDeclaration(node)

    return node
}

/**
 * Creates a new [MethodDeclaration] in the Fluent Node DSL with the given [name] and optional
 * [returnType]. The [init] block can be used to create further sub-nodes as well as configuring the
 * created node itself.
 */
context(record: RecordDeclaration)
fun LanguageFrontend<*, *>.method(
    name: CharSequence,
    returnType: Type = unknownType(),
    init: (MethodDeclaration.() -> Unit)? = null,
): MethodDeclaration {
    val node = newMethodDeclaration(name)
    node.returnTypes = listOf(returnType)
    node.type = with(node) { computeType(node) }

    scopeManager.enterScope(node)
    if (init != null) {
        init(node)
    }
    scopeManager.leaveScope(node)

    scopeManager.addDeclaration(node)
    record.addMethod(node)

    return node
}

/**
 * Creates a new [ConstructorDeclaration] in the Fluent Node DSL for the enclosing
 * [RecordDeclaration]. The [init] block can be used to create further sub-nodes as well as
 * configuring the created node itself.
 */
context(recordDeclaration: RecordDeclaration)
fun LanguageFrontend<*, *>.constructor(
    init: ConstructorDeclaration.() -> Unit
): ConstructorDeclaration {
    val node =
        newConstructorDeclaration(recordDeclaration.name, recordDeclaration = recordDeclaration)

    scopeManager.enterScope(node)
    init(node)
    scopeManager.leaveScope(node)

    scopeManager.addDeclaration(node)
    recordDeclaration.addConstructor(node)

    return node
}

/**
 * Creates a new [Block] in the Fluent Node DSL and sets it to the [FunctionDeclaration.body] of the
 * nearest enclosing [FunctionDeclaration]. The [init] block can be used to create further sub-nodes
 * as well as configuring the created node itself.
 */
context(func: FunctionDeclaration)
fun LanguageFrontend<*, *>.body(needsScope: Boolean = true, init: Block.() -> Unit): Block {
    val node = newBlock()

    scopeIfNecessary(needsScope, node, init)
    func.body = node

    return node
}

/**
 * Creates a new [Block] in the Fluent Node DSL and sets it to the [FunctionDeclaration.body] of the
 * nearest enclosing [FunctionDeclaration]. The [init] block can be used to create further sub-nodes
 * as well as configuring the created node itself.
 */
context(holder: StatementHolder)
fun LanguageFrontend<*, *>.block(needsScope: Boolean = true, init: Block.() -> Unit): Block {
    val node = newBlock()

    scopeIfNecessary(needsScope, node, init)
    holder.statementEdges += node

    return node
}

/**
 * Creates a new [ParameterDeclaration] in the Fluent Node DSL and adds it to the
 * [FunctionDeclaration.dParameters] of the nearest enclosing [FunctionDeclaration]. The [init]
 * block can be used to create further sub-nodes as well as configuring the created node itself.
 */
context(func: FunctionDeclaration)
fun LanguageFrontend<*, *>.param(
    name: CharSequence,
    type: Type = unknownType(),
    init: (ParameterDeclaration.() -> Unit)? = null,
): ParameterDeclaration {
    val node = newParameterDeclaration(name, type)
    init?.let { it(node) }

    scopeManager.addDeclaration(node)
    func.parameters += node

    return node
}

/**
 * Creates a new [ReturnStatement] in the Fluent Node DSL and adds it to the
 * [StatementHolder.statements] of the nearest enclosing [StatementHolder]. The [init] block can be
 * used to create further sub-nodes as well as configuring the created node itself.
 */
context(holder: StatementHolder)
fun LanguageFrontend<*, *>.returnStmt(init: ReturnStatement.() -> Unit): ReturnStatement {
    val node = newReturnStatement()
    init(node)

    (holder) += node

    return node
}

context(holder: Holder<out Statement>)
fun LanguageFrontend<*, *>.subscriptExpr(
    init: (SubscriptExpression.() -> Unit)? = null
): SubscriptExpression {
    val node = newSubscriptExpression()

    if (init != null) {
        init(node)
    }

    // Only add this to an argument holder if the nearest holder is an argument holder
    if (holder is ArgumentHolder) {
        holder += node
    }

    return node
}

context(holder: Holder<out Statement>)
fun LanguageFrontend<*, *>.listComp(
    init: (CollectionComprehension.() -> Unit)? = null
): CollectionComprehension {
    val node = newCollectionComprehension()

    if (init != null) {
        init(node)
    }

    // Only add this to an argument holder if the nearest holder is an argument holder
    if (holder is StatementHolder) {
        holder += node
    } else if (holder is ArgumentHolder) {
        holder += node
    }

    return node
}

context(holder: Holder<out Statement>)
fun LanguageFrontend<*, *>.compExpr(
    init: (ComprehensionExpression.() -> Unit)? = null
): ComprehensionExpression {
    val node = newComprehensionExpression()

    if (init != null) {
        init(node)
    }

    // Only add this to an argument holder if the nearest holder is an argument holder
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
context(holder: StatementHolder)
fun LanguageFrontend<*, *>.declare(init: DeclarationStatement.() -> Unit): DeclarationStatement {
    val node = newDeclarationStatement()
    init(node)

    (holder) += node

    return node
}

/**
 * Creates a new [DeclarationStatement] in the Fluent Node DSL. The [init] block can be used to
 * create further sub-nodes as well as configuring the created node itself.
 */
fun LanguageFrontend<*, *>.declareVar(
    name: String,
    type: Type,
    init: (VariableDeclaration.() -> Unit)? = null,
): DeclarationStatement {
    val node = newDeclarationStatement()
    val variableDecl = newVariableDeclaration(name, type)

    if (init != null) {
        init(variableDecl)
    }

    node.singleDeclaration = variableDecl

    return node
}

/**
 * Creates a new [VariableDeclaration] in the Fluent Node DSL and adds it to the
 * [DeclarationStatement.declarations] of the nearest enclosing [DeclarationStatement]. The [init]
 * block can be used to create further sub-nodes as well as configuring the created node itself.
 */
context(stmt: DeclarationStatement)
fun LanguageFrontend<*, *>.variable(
    name: String,
    type: Type = unknownType(),
    init: (VariableDeclaration.() -> Unit)? = null,
): VariableDeclaration {
    val node = newVariableDeclaration(name, type)
    if (init != null) init(node)

    stmt.declarations += node
    scopeManager.addDeclaration(node)

    return node
}

/**
 * Creates a new [ProblemDeclaration] in the Fluent Node DSL and adds it to the
 * [DeclarationStatement.declarations] of the nearest enclosing [DeclarationStatement]. The [init]
 * block can be used to create further sub-nodes as well as configuring the created node itself.
 */
context(stmt: DeclarationStatement)
fun LanguageFrontend<*, *>.problemDecl(
    description: String,
    type: ProblemNode.ProblemType = ProblemNode.ProblemType.TRANSLATION,
    init: (ProblemDeclaration.() -> Unit)? = null,
): ProblemDeclaration {
    val node = newProblemDeclaration(problem = description, problemType = type)
    if (init != null) init(node)

    stmt.declarations += node
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
 * [Reference] or [MemberExpression] and sets it as the [CallExpression.callee]. The [init] block
 * can be used to create further sub-nodes as well as configuring the created node itself.
 */
context(holder: Holder<out Statement>)
fun LanguageFrontend<*, *>.call(
    name: CharSequence,
    isStatic: Boolean = false,
    init: (CallExpression.() -> Unit)? = null,
): CallExpression {
    // Try to parse the name
    val parsedName = parseName(name, ".")
    val node =
        if (parsedName.parent != null) {
            newMemberCallExpression(
                newMemberExpression(parsedName.localName, memberOrRef(parsedName.parent)),
                isStatic,
            )
        } else {
            newCallExpression(newReference(parsedName))
        }
    if (init != null) {
        init(node)
    }

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
 * [Reference] or [MemberExpression] and sets it as the [CallExpression.callee]. The [init] block
 * can be used to create further sub-nodes as well as configuring the created node itself.
 */
context(holder: Holder<out Statement>)
fun LanguageFrontend<*, *>.memberCall(
    localName: CharSequence,
    base: Expression,
    isStatic: Boolean = false,
    init: (MemberCallExpression.() -> Unit)? = null,
): MemberCallExpression {
    // Try to parse the name
    val node = newMemberCallExpression(newMemberExpression(localName, base), isStatic)
    if (init != null) {
        init(node)
    }

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
context(holder: Holder<out Statement>)
fun LanguageFrontend<*, *>.construct(
    name: CharSequence,
    init: (ConstructExpression.() -> Unit)? = null,
): ConstructExpression {
    val node = newConstructExpression(parseName(name))
    node.type = t(name)

    if (init != null) {
        init(node)
    }

    if (holder is StatementHolder) {
        holder += node
    } else if (holder is ArgumentHolder) {
        holder += node
    }

    return node
}

context(holder: Holder<out Statement>)
fun LanguageFrontend<*, *>.cast(
    castType: Type,
    init: (CastExpression.() -> Unit)? = null,
): CastExpression {
    val node = newCastExpression()
    node.castType = castType
    if (init != null) init(node)

    if (holder is StatementHolder) {
        holder += node
    } else if (holder is ArgumentHolder) {
        holder += node
    }

    return node
}

context(holder: Holder<out Statement>)
fun LanguageFrontend<*, *>.new(init: (NewExpression.() -> Unit)? = null): NewExpression {
    val node = newNewExpression()
    if (init != null) init(node)

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
            newReference(name.localName)
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
context(holder: StatementHolder)
fun LanguageFrontend<*, *>.ifStmt(init: IfStatement.() -> Unit): IfStatement {
    val node = newIfStatement()
    init(node)

    (holder) += node

    return node
}

/**
 * Creates a new [ForEachStatement] in the Fluent Node DSL and adds it to the
 * [StatementHolder.statements] of the nearest enclosing [StatementHolder]. The [init] block can be
 * used to create further sub-nodes as well as configuring the created node itself.
 */
context(holder: StatementHolder)
fun LanguageFrontend<*, *>.forEachStmt(init: ForEachStatement.() -> Unit): ForEachStatement {
    val node = newForEachStatement()

    init(node)

    (holder) += node

    return node
}

/**
 * Creates a new [ForStatement] in the Fluent Node DSL and adds it to the
 * [StatementHolder.statements] of the nearest enclosing [StatementHolder]. The [init] block can be
 * used to create further sub-nodes as well as configuring the created node itself.
 */
context(holder: StatementHolder)
fun LanguageFrontend<*, *>.forStmt(init: ForStatement.() -> Unit): ForStatement {

    val node = newForStatement()

    init(node)

    (holder) += node

    return node
}

/**
 * Configures the [ForStatement.condition] in the Fluent Node DSL of the nearest enclosing
 * [ForStatement]. The [init] block can be used to create further sub-nodes as well as configuring
 * the created node itself.
 */
context(stmt: ForStatement)
fun LanguageFrontend<*, *>.forCondition(init: ForStatement.() -> Expression): Expression {

    var node = init(stmt)
    stmt.condition = node

    return node
}

/**
 * Configures the [ForStatement.condition] in the Fluent Node DSL of the nearest enclosing
 * [ForStatement]. The [init] block can be used to create further sub-nodes as well as configuring
 * the created node itself.
 */
context(stmt: ForStatement)
fun LanguageFrontend<*, *>.forInitializer(
    init: ForStatement.() -> DeclarationStatement
): DeclarationStatement {
    val node = init(stmt)
    stmt.initializerStatement = node

    val single = node.singleDeclaration
    if (single != null) {
        scopeManager.addDeclaration(single)
    }

    return node
}

/**
 * Configures the [ForStatement.iterationStatement] in the Fluent Node DSL of the nearest enclosing
 * [ForStatement]. The [init] block can be used to create further sub-nodes as well as configuring
 * the created node itself.
 */
context(stmt: ForStatement)
fun LanguageFrontend<*, *>.forIteration(init: ForStatement.() -> Statement): Statement {
    var node = init(stmt)
    stmt.iterationStatement = node

    return node
}

/**
 * Creates a new [SwitchStatement] in the Fluent Node DSL and adds it to the
 * [StatementHolder.statements] of the nearest enclosing [StatementHolder]. The [init] block can be
 * used to create further sub-nodes as well as configuring the created node itself.
 */
context(holder: StatementHolder)
fun LanguageFrontend<*, *>.switchStmt(
    selector: Expression,
    needsScope: Boolean = true,
    init: SwitchStatement.() -> Unit,
): SwitchStatement {
    val node = newSwitchStatement()
    node.selector = selector
    scopeIfNecessary(needsScope, node, init)

    (holder) += node

    return node
}

/**
 * Creates a new [WhileStatement] in the Fluent Node DSL and adds it to the
 * [StatementHolder.statements] of the nearest enclosing [StatementHolder]. The [init] block can be
 * used to create further sub-nodes as well as configuring the created node itself.
 */
context(holder: StatementHolder)
fun LanguageFrontend<*, *>.whileStmt(
    needsScope: Boolean = true,
    init: WhileStatement.() -> Unit,
): WhileStatement {
    val node = newWhileStatement()
    scopeIfNecessary(needsScope, node, init)

    (holder) += node

    return node
}

/**
 * Creates a new [DoStatement] in the Fluent Node DSL and adds it to the
 * [StatementHolder.statements] of the nearest enclosing [StatementHolder]. The [init] block can be
 * used to create further sub-nodes as well as configuring the created node itself.
 */
context(holder: StatementHolder)
fun LanguageFrontend<*, *>.doStmt(
    needsScope: Boolean = true,
    init: DoStatement.() -> Unit,
): DoStatement {
    val node = newDoStatement()
    scopeIfNecessary(needsScope, node, init)

    (holder) += node

    return node
}

// TODO: Combine the condition functions

/**
 * Configures the [IfStatement.condition] in the Fluent Node DSL of the nearest enclosing
 * [IfStatement]. The [init] block can be used to create further sub-nodes as well as configuring
 * the created node itself.
 */
context(stmt: IfStatement)
fun LanguageFrontend<*, *>.condition(init: IfStatement.() -> Expression): Expression {
    return init(stmt)
}

/**
 * Configures the [WhileStatement.condition] in the Fluent Node DSL of the nearest enclosing
 * [WhileStatement]. The [init] block can be used to create further sub-nodes as well as configuring
 * the created node itself.
 */
context(stmt: WhileStatement)
fun LanguageFrontend<*, *>.whileCondition(init: WhileStatement.() -> Expression): Expression {
    return init(stmt)
}

/**
 * Configures the [DoStatement.condition] in the Fluent Node DSL of the nearest enclosing
 * [DoStatement]. The [init] block can be used to create further sub-nodes as well as configuring
 * the created node itself.
 */
context(stmt: DoStatement)
fun LanguageFrontend<*, *>.doCondition(init: DoStatement.() -> Expression): Expression {
    return init(stmt)
}

/**
 * Creates a new [Block] in the Fluent Node DSL and sets it to the [IfStatement.thenStatement] of
 * the nearest enclosing [IfStatement]. The [init] block can be used to create further sub-nodes as
 * well as configuring the created node itself.
 */
context(stmt: IfStatement)
fun LanguageFrontend<*, *>.thenStmt(needsScope: Boolean = true, init: Block.() -> Unit): Block {
    val node = newBlock()
    scopeIfNecessary(needsScope, node, init)

    stmt.thenStatement = node

    return node
}

/**
 * Creates a new [IfStatement] in the Fluent Node DSL and sets it to the [IfStatement.elseStatement]
 * of the nearest enclosing [IfStatement]. This simulates an `else-if` scenario. The [init] block
 * can be used to create further sub-nodes as well as configuring the created node itself.
 */
context(stmt: IfStatement)
fun LanguageFrontend<*, *>.elseIf(init: IfStatement.() -> Unit): IfStatement {
    val node = newIfStatement()
    init(node)

    stmt.elseStatement = node

    return node
}

/**
 * Creates a new [Block] in the Fluent Node DSL and sets it to the [LoopStatement.statement] of the
 * nearest enclosing [LoopStatement]. The [init] block can be used to create further sub-nodes as
 * well as configuring the created node itself.
 */
context(stmt: LoopStatement)
fun LanguageFrontend<*, *>.loopBody(init: Block.() -> Unit): Block {
    val node = newBlock()
    init(node)
    stmt.statement = node

    return node
}

/**
 * Configures the [ForEachStatement.variable] in the Fluent Node DSL of the nearest enclosing
 * [ForEachStatement]. The [init] block can be used to create further sub-nodes as well as
 * configuring the created node itself.
 */
context(stmt: ForEachStatement)
fun LanguageFrontend<*, *>.variable(init: ForEachStatement.() -> Statement): Statement {
    return init(stmt)
}

/**
 * Configures the [ForEachStatement.iterable] in the Fluent Node DSL of the nearest enclosing
 * [ForEachStatement]. The [init] block can be used to create further sub-nodes as well as
 * configuring the created node itself.
 */
context(stmt: ForEachStatement)
fun LanguageFrontend<*, *>.iterable(init: ForEachStatement.() -> Statement): Statement {
    return init(stmt)
}

/**
 * Configures the [ForStatement.initializerStatement] in the Fluent Node DSL of the nearest
 * enclosing [ForStatement]. The [init] block can be used to create further sub-nodes as well as
 * configuring the created node itself.
 */
context(stmt: ForStatement)
fun LanguageFrontend<*, *>.initializer(init: ForStatement.() -> Expression): Expression {
    return init(stmt)
}

/**
 * Creates a new [Block] in the Fluent Node DSL and sets it to the [SwitchStatement.statement] of
 * the nearest enclosing [SwitchStatement]. The [init] block can be used to create further sub-nodes
 * as well as configuring the created node itself.
 */
context(stmt: SwitchStatement)
fun LanguageFrontend<*, *>.switchBody(init: Block.() -> Unit): Block {
    val node = newBlock()
    init(node)
    stmt.statement = node

    return node
}

/**
 * Creates a new [Block] in the Fluent Node DSL and sets it to the [IfStatement.elseStatement] of
 * the nearest enclosing [IfStatement]. The [init] block can be used to create further sub-nodes as
 * well as configuring the created node itself.
 */
context(stmt: IfStatement)
fun LanguageFrontend<*, *>.elseStmt(needsScope: Boolean = true, init: Block.() -> Unit): Block {
    val node = newBlock()
    scopeIfNecessary(needsScope, node, init)

    stmt.elseStatement = node

    return node
}

/**
 * Creates a new [Block] in the Fluent Node DSL and sets it to the [LoopStatement.elseStatement] of
 * the nearest enclosing [LoopStatement]. The [init] block can be used to create further sub-nodes
 * as well as configuring the created node itself.
 */
context(stmt: LoopStatement)
fun LanguageFrontend<*, *>.loopElseStmt(needsScope: Boolean = true, init: Block.() -> Unit): Block {
    val node = newBlock()
    scopeIfNecessary(needsScope, node, init)

    stmt.elseStatement = node

    return node
}

/**
 * Creates a new [LabelStatement] in the Fluent Node DSL and adds it to the nearest enclosing
 * [StatementHolder].
 */
context(holder: Holder<out Statement>)
fun LanguageFrontend<*, *>.label(
    label: String,
    init: (LabelStatement.() -> Statement)? = null,
): LabelStatement {
    val node = newLabelStatement()
    node.label = label
    if (init != null) {
        node.subStatement = init(node)
    }

    // Only add this to a statement holder if the nearest holder is a statement holder
    if (holder is StatementHolder) {
        holder += node
    }

    return node
}

/**
 * Creates a new [ContinueStatement] in the Fluent Node DSL and adds it to the nearest enclosing
 * [StatementHolder].
 */
context(holder: StatementHolder)
fun LanguageFrontend<*, *>.continueStmt(label: String? = null): ContinueStatement {
    val node = newContinueStatement()
    node.label = label

    holder += node

    return node
}

/**
 * Creates a new [BreakStatement] in the Fluent Node DSL and adds it to the nearest enclosing
 * [Holder], but only if it is an [StatementHolder].
 */
context(holder: Holder<out Statement>)
fun LanguageFrontend<*, *>.breakStmt(label: String? = null): BreakStatement {
    val node = newBreakStatement()
    node.label = label

    // Only add this to a statement holder if the nearest holder is a statement holder
    if (holder is StatementHolder) {
        holder += node
    }

    return node
}

/**
 * Creates a new [CaseStatement] in the Fluent Node DSL and adds it to the nearest enclosing
 * [Holder], but only if it is an [StatementHolder].
 */
context(holder: Holder<out Statement>)
fun LanguageFrontend<*, *>.case(caseExpression: Expression? = null): CaseStatement {
    val node = newCaseStatement()
    node.caseExpression = caseExpression

    // Only add this to a statement holder if the nearest holder is a statement holder
    if (holder is StatementHolder) {
        holder += node
    }

    return node
}

/**
 * Creates a new [DefaultStatement] in the Fluent Node DSL and adds it to the nearest enclosing
 * [StatementHolder].
 */
context(holder: Holder<out Statement>)
fun LanguageFrontend<*, *>.default(): DefaultStatement {
    val node = newDefaultStatement()

    // Only add this to a statement holder if the nearest holder is a statement holder
    if (holder is StatementHolder) {
        holder += node
    }

    return node
}

/**
 * Creates a new [Literal] in the Fluent Node DSL and invokes [ArgumentHolder.addArgument] of the
 * nearest enclosing [Holder], but only if it is an [ArgumentHolder].
 */
context(holder: Holder<out Statement>)
fun <N> LanguageFrontend<*, *>.literal(value: N, type: Type = unknownType()): Literal<N> {
    val node = newLiteral(value, type)

    // Only add this to an argument holder if the nearest holder is an argument holder
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
context(holder: Holder<out Statement>)
fun LanguageFrontend<*, *>.ile(
    targetType: Type = unknownType(),
    init: (InitializerListExpression.() -> Unit)? = null,
): InitializerListExpression {
    val node = newInitializerListExpression(targetType)

    if (init != null) {
        init(node)
    }

    // Only add this to an argument holder if the nearest holder is an argument holder
    if (holder is ArgumentHolder) {
        holder += node
    }

    return node
}

/**
 * Creates a new [Reference] in the Fluent Node DSL and invokes [ArgumentHolder.addArgument] of the
 * nearest enclosing [Holder], but only if it is an [ArgumentHolder].
 */
context(holder: Holder<out Statement>)
fun LanguageFrontend<*, *>.ref(
    name: CharSequence,
    type: Type = unknownType(),
    makeMagic: Boolean = true,
    init: (Reference.() -> Unit)? = null,
): Reference {
    val node = newReference(name)
    node.type = type

    if (init != null) {
        init(node)
    }

    if (makeMagic) {
        // Only add this to an argument holder if the nearest holder is an argument holder
        if (holder is ArgumentHolder) {
            holder += node
        }
    }

    return node
}

/**
 * This utility function tries to create a fake [PhysicalLocation] in order to somewhat
 * differentiate the different nodes. This is primarily needed for the mermaid graph printer, which
 * relies on [Node.hashCode], which in turn relies on [Node.location].
 */
context(tu: TranslationUnitDeclaration)
fun Expression.line(i: Int): Expression {
    // We just stupidly assume that the name of node is also its code
    val code = this.name

    // This is really fake, but it is ok-ish for now
    val region = Region(i, 0, i, code.length)

    this.location = PhysicalLocation(URI((tu).name.toString()), region)

    return this
}

/**
 * Creates a new [MemberExpression] in the Fluent Node DSL and invokes [ArgumentHolder.addArgument]
 * of the nearest enclosing [Holder], but only if it is an [ArgumentHolder]. If the [name] doesn't
 * already contain a fqn, we add an implicit "this" as base.
 */
context(holder: Holder<out Statement>)
fun LanguageFrontend<*, *>.member(
    name: CharSequence,
    base: Expression? = null,
    operatorCode: String = ".",
): MemberExpression {
    val parsedName = parseName(name)
    val type =
        if (parsedName.parent != null) {
            unknownType()
        } else {
            var scope = ((holder) as? ScopeProvider)?.scope
            while (scope != null && scope !is RecordScope) {
                scope = scope.parent
            }
            val scopeType = scope?.name?.let { this.t(it) } ?: unknownType()
            scopeType
        }
    val memberBase = base ?: this.memberOrRef(parsedName.parent ?: this.parseName("this"), type)

    val node = newMemberExpression(name, memberBase, operatorCode = operatorCode)

    // Only add this to an argument holder if the nearest holder is an argument holder
    if (holder is ArgumentHolder) {
        holder += node
    }

    return node
}

/**
 * Creates a new [BinaryOperator] with a `*` [BinaryOperator.operatorCode] in the Fluent Node DSL
 * and invokes [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(frontend: LanguageFrontend<*, *>, holder: ArgumentHolder)
operator fun Expression.times(rhs: Expression): BinaryOperator {
    val node = (frontend).newBinaryOperator("*")
    node.lhs = this
    node.rhs = rhs

    (holder) += node

    // We need to do a little trick here. Because of the evaluation order, lhs and rhs might also
    // been added to the argument holders arguments (and we do not want that). However, we cannot
    // prevent it, so we need to remove them again
    (holder) -= node.lhs
    (holder) -= node.rhs

    return node
}

/**
 * Creates a new [UnaryOperator] with a `-` [UnaryOperator.operatorCode] in the Fluent Node DSL and
 * invokes [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(frontend: LanguageFrontend<*, *>, holder: ArgumentHolder)
operator fun Expression.unaryMinus(): UnaryOperator {
    val node = (frontend).newUnaryOperator("-", false, false)
    node.input = this

    (holder) += node

    // We need to do a little trick here. Because of the evaluation order, lhs and rhs might also
    // been added to the argument holders arguments (and we do not want that). However, we cannot
    // prevent it, so we need to remove them again
    (holder) -= node.input

    return node
}

/**
 * Creates a new [BinaryOperator] with a `/` [BinaryOperator.operatorCode] in the Fluent Node DSL
 * and invokes [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(frontend: LanguageFrontend<*, *>, holder: ArgumentHolder)
operator fun Expression.div(rhs: Expression): BinaryOperator {
    val node = (frontend).newBinaryOperator("/")
    node.lhs = this
    node.rhs = rhs

    (holder) += node

    // We need to do a little trick here. Because of the evaluation order, lhs and rhs might also
    // been added to the argument holders arguments (and we do not want that). However, we cannot
    // prevent it, so we need to remove them again
    (holder) -= node.lhs
    (holder) -= node.rhs

    return node
}

/**
 * Creates a new [BinaryOperator] with a `+` [BinaryOperator.operatorCode] in the Fluent Node DSL
 * and invokes [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(frontend: LanguageFrontend<*, *>, holder: ArgumentHolder)
operator fun Expression.plus(rhs: Expression): BinaryOperator {
    val node = (frontend).newBinaryOperator("+")
    node.lhs = this
    node.rhs = rhs

    (holder) += node

    // We need to do a little trick here. Because of the evaluation order, lhs and rhs might also
    // been added to the argument holders arguments (and we do not want that). However, we cannot
    // prevent it, so we need to remove them again
    (holder) -= node.lhs
    (holder) -= node.rhs

    return node
}

/**
 * Creates a new [BinaryOperator] with a `+` [BinaryOperator.operatorCode] in the Fluent Node DSL
 * and adds it to the nearest enclosing [StatementHolder].
 */
context(frontend: LanguageFrontend<*, *>, holder: StatementHolder)
operator fun Expression.plusAssign(rhs: Expression) {
    val node = (frontend).newAssignExpression("+=", listOf(this), listOf(rhs))

    (holder) += node
}

/**
 * Creates a new [BinaryOperator] with a `+` [BinaryOperator.operatorCode] in the Fluent Node DSL
 * and invokes [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(frontend: LanguageFrontend<*, *>, holder: ArgumentHolder)
operator fun Expression.rem(rhs: Expression): BinaryOperator {
    val node = (frontend).newBinaryOperator("%")
    node.lhs = this
    node.rhs = rhs

    (holder) += node

    // We need to do a little trick here. Because of the evaluation order, lhs and rhs might also
    // been added to the argument holders arguments (and we do not want that). However, we cannot
    // prevent it, so we need to remove them again
    (holder) -= node.lhs
    (holder) -= node.rhs

    return node
}

/**
 * Creates a new [BinaryOperator] with a `-` [BinaryOperator.operatorCode] in the Fluent Node DSL
 * and invokes [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(frontend: LanguageFrontend<*, *>, holder: ArgumentHolder)
operator fun Expression.minus(rhs: Expression): BinaryOperator {
    val node = (frontend).newBinaryOperator("-")
    node.lhs = this
    node.rhs = rhs

    (holder) += node

    return node
}

/**
 * Creates a new [UnaryOperator] with a `&` [UnaryOperator.operatorCode] in the Fluent Node DSL and
 * invokes [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(frontend: LanguageFrontend<*, *>, holder: ArgumentHolder)
fun reference(input: Expression): UnaryOperator {
    val node = (frontend).newUnaryOperator("&", false, false)
    node.input = input

    holder += node

    return node
}

/**
 * Creates a new [UnaryOperator] with a `--` [UnaryOperator.operatorCode] in the Fluent Node DSL and
 * adds it to the nearest enclosing [StatementHolder].
 */
context(frontend: LanguageFrontend<*, *>, holder: Holder<out Statement>)
operator fun Expression.dec(): UnaryOperator {
    val node = (frontend).newUnaryOperator("--", true, false)
    node.input = this

    if (holder is StatementHolder) {
        holder += node
    }

    return node
}

/**
 * Creates a new [UnaryOperator] with a `++` [UnaryOperator.operatorCode] in the Fluent Node DSL and
 * invokes [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(frontend: LanguageFrontend<*, *>, holder: Holder<out Statement>)
operator fun Expression.inc(): UnaryOperator {
    val node = (frontend).newUnaryOperator("++", true, false)
    node.input = this

    if (holder is StatementHolder) {
        holder += node
    }

    return node
}

/**
 * Creates a new [UnaryOperator] with a `++` [UnaryOperator.operatorCode] in the Fluent Node DSL and
 * invokes [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(frontend: LanguageFrontend<*, *>)
fun Expression.incNoContext(): UnaryOperator {
    val node = (frontend).newUnaryOperator("++", true, false)
    node.input = this

    return node
}

/**
 * Creates a new [BinaryOperator] with a `==` [BinaryOperator.operatorCode] in the Fluent Node DSL
 * and invokes [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(frontend: LanguageFrontend<*, *>, holder: ArgumentHolder)
infix fun Expression.eq(rhs: Expression): BinaryOperator {
    val node = (frontend).newBinaryOperator("==")
    node.lhs = this
    node.rhs = rhs

    (holder) += node

    return node
}

/**
 * Creates a new [BinaryOperator] with a `>` [BinaryOperator.operatorCode] in the Fluent Node DSL
 * and invokes [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(frontend: LanguageFrontend<*, *>, holder: ArgumentHolder)
infix fun Expression.gt(rhs: Expression): BinaryOperator {
    val node = (frontend).newBinaryOperator(">")
    node.lhs = this
    node.rhs = rhs

    (holder) += node

    return node
}

/**
 * Creates a new [BinaryOperator] with a `>=` [BinaryOperator.operatorCode] in the Fluent Node DSL
 * and invokes [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(frontend: LanguageFrontend<*, *>, holder: ArgumentHolder)
infix fun Expression.ge(rhs: Expression): BinaryOperator {
    val node = (frontend).newBinaryOperator(">=")
    node.lhs = this
    node.rhs = rhs

    (holder) += node

    return node
}

/**
 * Creates a new [BinaryOperator] with a `<` [BinaryOperator.operatorCode] in the Fluent Node DSL
 * and invokes [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(frontend: LanguageFrontend<*, *>)
infix fun Expression.lt(rhs: Expression): BinaryOperator {
    val node = (frontend).newBinaryOperator("<")
    node.lhs = this
    node.rhs = rhs

    return node
}

/**
 * Creates a new [BinaryOperator] with a `<=` [BinaryOperator.operatorCode] in the Fluent Node DSL
 * and invokes [ArgumentHolder.addArgument] of the nearest enclosing [ArgumentHolder].
 */
context(frontend: LanguageFrontend<*, *>, holder: ArgumentHolder)
infix fun Expression.le(rhs: Expression): BinaryOperator {
    val node = (frontend).newBinaryOperator("<=")
    node.lhs = this
    node.rhs = rhs

    (holder) += node

    return node
}

/**
 * Creates a new [ConditionalExpression] with a `=` [BinaryOperator.operatorCode] in the Fluent Node
 * DSL and adds it to the nearest enclosing [StatementHolder].
 */
context(frontend: LanguageFrontend<*, *>, holder: Holder<out Node>)
fun Expression.conditional(
    condition: Expression,
    thenExpression: Expression,
    elseExpression: Expression,
): ConditionalExpression {
    val node = (frontend).newConditionalExpression(condition, thenExpression, elseExpression)

    if (holder is StatementHolder) {
        (holder) += node
    } else if (holder is ArgumentHolder) {
        holder += node
    }

    return node
}

/**
 * Creates a new [BinaryOperator] with a `=` [BinaryOperator.operatorCode] in the Fluent Node DSL
 * and adds it to the nearest enclosing [StatementHolder].
 */
context(frontend: LanguageFrontend<*, *>, holder: StatementHolder)
infix fun Expression.assign(init: AssignExpression.() -> Expression): AssignExpression {
    val node = (frontend).newAssignExpression("=")
    node.lhs = mutableListOf(this)
    init(node)
    // node.rhs = listOf(init(node))

    (holder) += node

    return node
}

/**
 * Creates a new [AssignExpression] with a `=` [AssignExpression.operatorCode] in the Fluent Node
 * DSL and adds it to the nearest enclosing [StatementHolder].
 */
context(frontend: LanguageFrontend<*, *>, holder: Holder<out Node>)
infix fun Expression.assign(rhs: Expression): AssignExpression {
    val node = (frontend).newAssignExpression("=", listOf(this), listOf(rhs))

    if (holder is StatementHolder) {
        holder += node
    }

    return node
}

/**
 * Creates a new [AssignExpression] with a `+=` [AssignExpression.operatorCode] in the Fluent Node
 * DSL and adds it to the nearest enclosing [StatementHolder].
 */
context(frontend: LanguageFrontend<*, *>, holder: Holder<out Node>)
infix fun Expression.assignPlus(rhs: Expression): AssignExpression {
    val node = (frontend).newAssignExpression("+=", listOf(this), listOf(rhs))

    if (holder is StatementHolder) {
        holder += node
    }

    return node
}

/**
 * Creates a new [AssignExpression] with a `=` [AssignExpression.operatorCode] in the Fluent Node
 * DSL and adds it to the nearest enclosing [StatementHolder].
 */
context(frontend: LanguageFrontend<*, *>, holder: Holder<out Node>)
infix fun Expression.assignAsExpr(rhs: Expression): AssignExpression {
    val node = (frontend).newAssignExpression("=", listOf(this), listOf(rhs))

    node.usedAsExpression = true

    return node
}

/**
 * Creates a new [AssignExpression] with a `=` [AssignExpression.operatorCode] in the Fluent Node
 * DSL and adds it to the nearest enclosing [StatementHolder].
 */
context(frontend: LanguageFrontend<*, *>, holder: Holder<out Node>)
infix fun Expression.assignAsExpr(rhs: AssignExpression.() -> Unit): AssignExpression {
    val node = (frontend).newAssignExpression("=", listOf(this))
    rhs(node)

    node.usedAsExpression = true

    return node
}

/**
 * Creates a new [ThrowExpression] in the Fluent Node DSL and adds it to the nearest enclosing
 * [StatementHolder].
 */
context(frontend: LanguageFrontend<*, *>, holder: Holder<out Node>)
infix fun Expression.`throw`(init: (ThrowExpression.() -> Unit)?): ThrowExpression {
    val node = (frontend).newThrowExpression()
    if (init != null) init(node)

    val holder = holder
    if (holder is StatementHolder) {
        holder += node
    }

    return node
}

/** Creates a new [Type] with the given [name] in the Fluent Node DSL. */
fun LanguageFrontend<*, *>.t(name: CharSequence, generics: List<Type> = listOf()) =
    objectType(name, generics)

/** Creates a new [IncompleteType] in the Fluent Node DSL. */
fun LanguageFrontend<*, *>.void() = incompleteType()

/**
 * Internally used to enter a new scope if [needsScope] is true before invoking [init] and leaving
 * it afterwards.
 */
private fun <T : Node> LanguageFrontend<*, *>.scopeIfNecessary(
    needsScope: Boolean,
    node: T,
    init: T.() -> Unit,
) {
    if (needsScope) {
        scopeManager.enterScope(node)
    }
    init(node)
    if (needsScope) {
        scopeManager.leaveScope(node)
    }
}

context(method: MethodDeclaration)
fun LanguageFrontend<*, *>.receiver(name: String, type: Type): VariableDeclaration {
    val node = newVariableDeclaration(name, type)

    method.receiver = node
    scopeManager.addDeclaration(node)

    return node
}
