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
package de.fraunhofer.aisec.cpg.graph

import de.fraunhofer.aisec.cpg.frontends.HasShortCircuitOperators
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.graph.Node.Companion.EMPTY_NAME
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.log
import de.fraunhofer.aisec.cpg.graph.edges.flows.ContextSensitiveDataflow
import de.fraunhofer.aisec.cpg.graph.statements.ThrowExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.AssignExpression
import de.fraunhofer.aisec.cpg.graph.statements.expressions.CollectionComprehension
import de.fraunhofer.aisec.cpg.graph.types.ProblemType
import de.fraunhofer.aisec.cpg.graph.types.Type

/**
 * Creates a new [Literal]. This is the top-most [Node] that a [LanguageFrontend] or [Handler]
 * should create. The [MetadataProvider] receiver will be used to fill different meta-data using
 * [AstNode.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun <V> ContextProvider.newLiteral(
    value: V,
    type: Type = unknownType(),
    rawNode: Any? = null,
): Literal<V> {
    val node = Literal<V>(ctx)
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    node.value = value
    node.type = type

    log(node)
    return node
}

/**
 * Creates a new [BinaryOperator] or a [ShortCircuitOperator] if the language implements
 * [HasShortCircuitOperators] and if the [operatorCode] is contained in
 * [HasShortCircuitOperators.operatorCodes]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [AstNode.applyMetadata]. Calling this extension function outside of
 * Kotlin requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun ContextProvider.newBinaryOperator(operatorCode: String, rawNode: Any? = null): BinaryOperator {
    val node =
        if (
            this is LanguageProvider &&
                (this.language as? HasShortCircuitOperators)
                    ?.operatorCodes
                    ?.contains(operatorCode) == true
        ) {
            ShortCircuitOperator(ctx)
        } else {
            BinaryOperator(ctx)
        }
    node.applyMetadata(this, operatorCode, rawNode, true)

    node.operatorCode = operatorCode

    log(node)

    return node
}

/**
 * Creates a new [UnaryOperator]. This is the top-most [Node] that a [LanguageFrontend] or [Handler]
 * should create. The [MetadataProvider] receiver will be used to fill different meta-data using
 * [AstNode.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun ContextProvider.newUnaryOperator(
    operatorCode: String,
    postfix: Boolean,
    prefix: Boolean,
    rawNode: Any? = null,
): UnaryOperator {
    val node = UnaryOperator(ctx)
    node.applyMetadata(this, operatorCode, rawNode, true)

    node.operatorCode = operatorCode
    node.isPostfix = postfix
    node.isPrefix = prefix

    log(node)

    return node
}

/**
 * Creates a new [AssignExpression]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [AstNode.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun ContextProvider.newAssignExpression(
    operatorCode: String = "=",
    lhs: List<Expression> = listOf(),
    rhs: List<Expression> = listOf(),
    rawNode: Any? = null,
): AssignExpression {
    val node = AssignExpression(ctx)
    node.applyMetadata(this, operatorCode, rawNode, true)
    node.operatorCode = operatorCode
    node.lhs = lhs.toMutableList()
    node.rhs = rhs.toMutableList()

    log(node)

    return node
}

/**
 * Creates a new [NewExpression]. This is the top-most [Node] that a [LanguageFrontend] or [Handler]
 * should create. The [MetadataProvider] receiver will be used to fill different meta-data using
 * [AstNode.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun ContextProvider.newNewExpression(
    type: Type = unknownType(),
    rawNode: Any? = null,
): NewExpression {
    val node = NewExpression(ctx)
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    node.type = type

    log(node)
    return node
}

/**
 * Creates a new [ConstructExpression]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [AstNode.applyMetadata]. Calling this extension function outside of
 * Kotlin requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun ContextProvider.newConstructExpression(
    name: CharSequence? = EMPTY_NAME,
    rawNode: Any? = null,
): ConstructExpression {
    val node = ConstructExpression(ctx)
    node.applyMetadata(this, name, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [ConditionalExpression]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [AstNode.applyMetadata]. Calling this extension function outside of
 * Kotlin requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun ContextProvider.newConditionalExpression(
    condition: Expression,
    thenExpression: Expression? = null,
    elseExpression: Expression? = null,
    type: Type = unknownType(),
    rawNode: Any? = null,
): ConditionalExpression {
    val node = ConditionalExpression(ctx)
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    node.type = type
    node.condition = condition
    node.thenExpression = thenExpression
    node.elseExpression = elseExpression

    log(node)
    return node
}

/**
 * Creates a new [KeyValueExpression]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [AstNode.applyMetadata]. Calling this extension function outside of
 * Kotlin requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun ContextProvider.newKeyValueExpression(
    key: Expression,
    value: Expression,
    rawNode: Any? = null,
): KeyValueExpression {
    val node = KeyValueExpression(ctx)
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    node.key = key
    node.value = value

    log(node)
    return node
}

/**
 * Creates a new [LambdaExpression]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [AstNode.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun ContextProvider.newLambdaExpression(rawNode: Any? = null): LambdaExpression {
    val node = LambdaExpression(ctx)
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [Block]. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [AstNode.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun ContextProvider.newBlock(rawNode: Any? = null): Block {
    val node = Block(ctx)
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [CallExpression]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [AstNode.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun ContextProvider.newCallExpression(
    callee: Expression? = null,
    fqn: CharSequence? = null,
    template: Boolean = false,
    rawNode: Any? = null,
): CallExpression {
    val node = CallExpression(ctx)
    node.applyMetadata(this, fqn, rawNode, true)

    // Set the call expression as resolution helper for the callee
    if (callee is Reference) {
        callee.resolutionHelper = node
    }

    if (callee != null) {
        node.callee = callee
    }
    node.template = template

    log(node)
    return node
}

/**
 * Creates a new [MemberCallExpression]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [AstNode.applyMetadata]. Calling this extension function outside of
 * Kotlin requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun ContextProvider.newOperatorCallExpression(
    operatorCode: String,
    callee: Expression?,
    rawNode: Any? = null,
): OperatorCallExpression {
    val node = OperatorCallExpression(ctx)
    node.applyMetadata(this, operatorCode, rawNode)

    node.operatorCode = operatorCode
    if (callee != null) {
        node.callee = callee
    }

    log(node)
    return node
}

/**
 * Creates a new [MemberCallExpression]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [AstNode.applyMetadata]. Calling this extension function outside of
 * Kotlin requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun ContextProvider.newMemberCallExpression(
    callee: Expression?,
    isStatic: Boolean = false,
    rawNode: Any? = null,
): MemberCallExpression {
    val node = MemberCallExpression(ctx)
    node.applyMetadata(
        this,
        null, // the name will be updated later based on the callee
        rawNode,
    )

    // Set the call expression as resolution helper for the callee
    if (callee is Reference) {
        callee.resolutionHelper = node
    }

    if (callee != null) {
        node.callee = callee
    }
    node.isStatic = isStatic

    log(node)
    return node
}

/**
 * Creates a new [MemberExpression]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [AstNode.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun ContextProvider.newMemberExpression(
    name: CharSequence?,
    base: Expression,
    memberType: Type = unknownType(),
    operatorCode: String? = ".",
    rawNode: Any? = null,
): MemberExpression {
    val node = MemberExpression(ctx)
    node.applyMetadata(this, name, rawNode, true)

    node.base = base
    node.operatorCode = operatorCode
    node.type = memberType

    log(node)
    return node
}

/**
 * Creates a new [CastExpression]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [AstNode.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun ContextProvider.newCastExpression(rawNode: Any? = null): CastExpression {
    val node = CastExpression(ctx)
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [TypeIdExpression]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [AstNode.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun ContextProvider.newTypeIdExpression(
    operatorCode: String,
    type: Type = unknownType(),
    referencedType: Type = unknownType(),
    rawNode: Any? = null,
): TypeIdExpression {
    val node = TypeIdExpression(ctx)
    node.applyMetadata(this, operatorCode, rawNode, true)

    node.operatorCode = operatorCode
    node.type = type
    node.referencedType = referencedType

    log(node)
    return node
}

/**
 * Creates a new [SubscriptExpression]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [AstNode.applyMetadata]. Calling this extension function outside of
 * Kotlin requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun ContextProvider.newSubscriptExpression(rawNode: Any? = null): SubscriptExpression {
    val node = SubscriptExpression(ctx)
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [RangeExpression]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [AstNode.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun ContextProvider.newRangeExpression(
    floor: Expression? = null,
    ceiling: Expression? = null,
    rawNode: Any? = null,
): RangeExpression {
    val node = RangeExpression(ctx)
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    node.floor = floor
    node.ceiling = ceiling

    log(node)
    return node
}

/**
 * Creates a new [NewArrayExpression]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [AstNode.applyMetadata]. Calling this extension function outside of
 * Kotlin requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun ContextProvider.newNewArrayExpression(rawNode: Any? = null): NewArrayExpression {
    val node = NewArrayExpression(ctx)
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [Reference]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [AstNode.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun ContextProvider.newReference(
    name: CharSequence?,
    type: Type = unknownType(),
    rawNode: Any? = null,
): Reference {
    val node = Reference(ctx)
    node.applyMetadata(this, name, rawNode, true)

    node.type = type

    log(node)
    return node
}

/**
 * Creates a new [DeleteExpression]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [AstNode.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun ContextProvider.newDeleteExpression(rawNode: Any? = null): DeleteExpression {
    val node = DeleteExpression(ctx)
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [ExpressionList]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [AstNode.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun ContextProvider.newExpressionList(rawNode: Any? = null): ExpressionList {
    val node = ExpressionList(ctx)
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [InitializerListExpression]. This is the top-most [Node] that a [LanguageFrontend]
 * or [Handler] should create. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [AstNode.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun ContextProvider.newInitializerListExpression(
    targetType: Type = unknownType(),
    rawNode: Any? = null,
): InitializerListExpression {
    val node = InitializerListExpression(ctx)
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    node.type = targetType

    log(node)
    return node
}

@JvmOverloads
fun ContextProvider.newComprehensionExpression(rawNode: Any? = null): ComprehensionExpression {
    val node = ComprehensionExpression(ctx)
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

@JvmOverloads
fun ContextProvider.newCollectionComprehension(rawNode: Any? = null): CollectionComprehension {
    val node = CollectionComprehension(ctx)
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [TypeExpression]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [AstNode.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun ContextProvider.newTypeExpression(
    name: CharSequence?,
    type: Type = unknownType(),
    rawNode: Any? = null,
): TypeExpression {
    val node = TypeExpression(ctx)
    node.applyMetadata(this, name, rawNode)

    node.type = type

    log(node)
    return node
}

/**
 * Creates a new [ThrowExpression]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [AstNode.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun ContextProvider.newThrowExpression(rawNode: Any? = null): ThrowExpression {
    val node = ThrowExpression(ctx)
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [ProblemExpression]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [AstNode.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun ContextProvider.newProblemExpression(
    problem: String = "",
    type: ProblemNode.ProblemType = ProblemNode.ProblemType.PARSING,
    rawNode: Any? = null,
): ProblemExpression {
    val node = ProblemExpression(ctx, problem, type)
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

fun ContextProvider.newProblemType(rawNode: Any? = null): ProblemType {
    val node = ProblemType(ctx)
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

fun <T> Literal<T>.duplicate(implicit: Boolean): Literal<T> {
    val duplicate = Literal<T>(ctx)
    duplicate.language = this.language
    duplicate.value = this.value
    duplicate.type = this.type
    duplicate.assignedTypes = this.assignedTypes
    duplicate.code = this.code
    duplicate.location = this.location
    duplicate.locals = this.locals
    duplicate.argumentIndex = this.argumentIndex
    duplicate.annotations = this.annotations
    duplicate.comment = this.comment
    duplicate.file = this.file
    duplicate.name = this.name.clone()
    for (edge in this.nextDFGEdges) {
        if (edge is ContextSensitiveDataflow) {
            duplicate.nextDFGEdges.addContextSensitive(
                edge.end,
                edge.granularity,
                edge.callingContext,
            )
        } else {
            duplicate.nextDFGEdges += edge
        }
    }
    for (edge in this.prevDFGEdges) {
        if (edge is ContextSensitiveDataflow) {
            duplicate.prevDFGEdges.addContextSensitive(
                edge.start as DataflowNode,
                edge.granularity,
                edge.callingContext,
            )
        } else {
            duplicate.prevDFGEdges += edge
        }
    }
    for (edge in this.prevEOGEdges) {
        duplicate.prevEOGEdges += edge
    }
    for (edge in this.nextEOGEdges) {
        duplicate.nextEOGEdges += edge
    }
    duplicate.isImplicit = implicit
    return duplicate
}

fun TypeExpression.duplicate(implicit: Boolean): TypeExpression {
    val duplicate = TypeExpression(ctx)
    duplicate.name = this.name.clone()
    duplicate.language = this.language
    duplicate.type = this.type
    duplicate.isImplicit = implicit
    return duplicate
}
