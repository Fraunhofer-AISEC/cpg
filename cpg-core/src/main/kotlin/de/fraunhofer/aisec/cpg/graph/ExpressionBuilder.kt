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
import de.fraunhofer.aisec.cpg.graph.types.ProblemType
import de.fraunhofer.aisec.cpg.graph.types.Type

/**
 * Creates a new [Literal]. This is the top-most [Node] that a [LanguageFrontend] or [Handler]
 * should create. The [MetadataProvider] receiver will be used to fill different meta-data using
 * [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an appropriate
 * [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun <T, V> RawNodeTypeProvider<T>.newLiteral(
    value: V,
    type: Type = unknownType(),
    rawNode: Any? = null,
): Literal<V> {
    val node = Literal<V>()
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
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newBinaryOperator(operatorCode: String, rawNode: Any? = null): BinaryOperator {
    val node =
        if (
            this is LanguageProvider &&
                (this.language as? HasShortCircuitOperators)
                    ?.operatorCodes
                    ?.contains(operatorCode) == true
        ) {
            ShortCircuitOperator()
        } else {
            BinaryOperator()
        }
    node.applyMetadata(this, operatorCode, rawNode, true)

    node.operatorCode = operatorCode

    log(node)

    return node
}

/**
 * Creates a new [UnaryOperator]. This is the top-most [Node] that a [LanguageFrontend] or [Handler]
 * should create. The [MetadataProvider] receiver will be used to fill different meta-data using
 * [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an appropriate
 * [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newUnaryOperator(
    operatorCode: String,
    postfix: Boolean,
    prefix: Boolean,
    rawNode: Any? = null,
): UnaryOperator {
    val node = UnaryOperator()
    node.applyMetadata(this, operatorCode, rawNode, true)

    node.operatorCode = operatorCode
    node.isPostfix = postfix
    node.isPrefix = prefix

    log(node)

    return node
}

/**
 * Creates a new [AssignExpression]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newAssignExpression(
    operatorCode: String = "=",
    lhs: List<Expression> = listOf(),
    rhs: List<Expression> = listOf(),
    rawNode: Any? = null,
): AssignExpression {
    val node = AssignExpression()
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
 * [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an appropriate
 * [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newNewExpression(
    type: Type = unknownType(),
    rawNode: Any? = null,
): NewExpression {
    val node = NewExpression()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    node.type = type

    log(node)
    return node
}

/**
 * Creates a new [ConstructExpression]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newConstructExpression(
    name: CharSequence? = EMPTY_NAME,
    rawNode: Any? = null,
): ConstructExpression {
    val node = ConstructExpression()
    node.applyMetadata(this, name, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [ConditionalExpression]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newConditionalExpression(
    condition: Expression,
    thenExpression: Expression? = null,
    elseExpression: Expression? = null,
    type: Type = unknownType(),
    rawNode: Any? = null,
): ConditionalExpression {
    val node = ConditionalExpression()
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
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newKeyValueExpression(
    key: Expression,
    value: Expression,
    rawNode: Any? = null,
): KeyValueExpression {
    val node = KeyValueExpression()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    node.key = key
    node.value = value

    log(node)
    return node
}

/**
 * Creates a new [LambdaExpression]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newLambdaExpression(rawNode: Any? = null): LambdaExpression {
    val node = LambdaExpression()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [Block]. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newBlock(rawNode: Any? = null): Block {
    val node = Block()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [CallExpression]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newCallExpression(
    callee: Expression? = null,
    fqn: CharSequence? = null,
    template: Boolean = false,
    rawNode: Any? = null,
): CallExpression {
    val node = CallExpression()
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
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newOperatorCallExpression(
    operatorCode: String,
    callee: Expression?,
    rawNode: Any? = null,
): OperatorCallExpression {
    val node = OperatorCallExpression()
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
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newMemberCallExpression(
    callee: Expression?,
    isStatic: Boolean = false,
    rawNode: Any? = null,
): MemberCallExpression {
    val node = MemberCallExpression()
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
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newMemberExpression(
    name: CharSequence?,
    base: Expression,
    memberType: Type = unknownType(),
    operatorCode: String? = ".",
    rawNode: Any? = null,
): MemberExpression {
    val node = MemberExpression()
    node.applyMetadata(this, name, rawNode, true)

    node.base = base
    node.operatorCode = operatorCode
    node.type = memberType

    log(node)
    return node
}

/**
 * Creates a new [CastExpression]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newCastExpression(rawNode: Any? = null): CastExpression {
    val node = CastExpression()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [TypeIdExpression]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newTypeIdExpression(
    operatorCode: String,
    type: Type = unknownType(),
    referencedType: Type = unknownType(),
    rawNode: Any? = null,
): TypeIdExpression {
    val node = TypeIdExpression()
    node.applyMetadata(this, operatorCode, rawNode, true)

    node.operatorCode = operatorCode
    node.type = type
    node.referencedType = referencedType

    log(node)
    return node
}

/**
 * Creates a new [SubscriptExpression]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newSubscriptExpression(rawNode: Any? = null): SubscriptExpression {
    val node = SubscriptExpression()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [RangeExpression]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newRangeExpression(
    floor: Expression? = null,
    ceiling: Expression? = null,
    rawNode: Any? = null,
): RangeExpression {
    val node = RangeExpression()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    node.floor = floor
    node.ceiling = ceiling

    log(node)
    return node
}

/**
 * Creates a new [NewArrayExpression]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newNewArrayExpression(rawNode: Any? = null): NewArrayExpression {
    val node = NewArrayExpression()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [Reference]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newReference(
    name: CharSequence?,
    type: Type = unknownType(),
    rawNode: Any? = null,
): Reference {
    val node = Reference()
    node.applyMetadata(this, name, rawNode, true)

    node.type = type

    log(node)
    return node
}

/**
 * Creates a new [DeleteExpression]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newDeleteExpression(rawNode: Any? = null): DeleteExpression {
    val node = DeleteExpression()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [ExpressionList]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newExpressionList(rawNode: Any? = null): ExpressionList {
    val node = ExpressionList()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [InitializerListExpression]. This is the top-most [Node] that a [LanguageFrontend]
 * or [Handler] should create. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newInitializerListExpression(
    targetType: Type = unknownType(),
    rawNode: Any? = null,
): InitializerListExpression {
    val node = InitializerListExpression()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    node.type = targetType

    log(node)
    return node
}

@JvmOverloads
fun MetadataProvider.newComprehensionExpression(rawNode: Any? = null): ComprehensionExpression {
    val node = ComprehensionExpression()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

@JvmOverloads
fun MetadataProvider.newCollectionComprehension(rawNode: Any? = null): CollectionComprehension {
    val node = CollectionComprehension()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [TypeExpression]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newTypeExpression(
    name: CharSequence?,
    type: Type = unknownType(),
    rawNode: Any? = null,
): TypeExpression {
    val node = TypeExpression()
    node.applyMetadata(this, name, rawNode)

    node.type = type

    log(node)
    return node
}

/**
 * Creates a new [ThrowExpression]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newThrowExpression(rawNode: Any? = null): ThrowExpression {
    val node = ThrowExpression()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [ProblemExpression]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newProblemExpression(
    problem: String = "",
    type: ProblemNode.ProblemType = ProblemNode.ProblemType.PARSING,
    rawNode: Any? = null,
): ProblemExpression {
    val node = ProblemExpression(problem, type)
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

fun MetadataProvider.newProblemType(rawNode: Any? = null): ProblemType {
    val node = ProblemType()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

fun <T> Literal<T>.duplicate(implicit: Boolean): Literal<T> {
    val duplicate = Literal<T>()
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
                edge.start,
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
    val duplicate = TypeExpression()
    duplicate.name = this.name.clone()
    duplicate.language = this.language
    duplicate.type = this.type
    duplicate.isImplicit = implicit
    return duplicate
}
