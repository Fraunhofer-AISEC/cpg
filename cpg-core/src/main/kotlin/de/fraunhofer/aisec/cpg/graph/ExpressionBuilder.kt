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
import de.fraunhofer.aisec.cpg.graph.expressions.ArrayConstruction
import de.fraunhofer.aisec.cpg.graph.expressions.Assign
import de.fraunhofer.aisec.cpg.graph.expressions.BinaryOperator
import de.fraunhofer.aisec.cpg.graph.expressions.Block
import de.fraunhofer.aisec.cpg.graph.expressions.Call
import de.fraunhofer.aisec.cpg.graph.expressions.Cast
import de.fraunhofer.aisec.cpg.graph.expressions.CollectionComprehension
import de.fraunhofer.aisec.cpg.graph.expressions.Comprehension
import de.fraunhofer.aisec.cpg.graph.expressions.Conditional
import de.fraunhofer.aisec.cpg.graph.expressions.Construction
import de.fraunhofer.aisec.cpg.graph.expressions.Delete
import de.fraunhofer.aisec.cpg.graph.expressions.Expression
import de.fraunhofer.aisec.cpg.graph.expressions.ExpressionList
import de.fraunhofer.aisec.cpg.graph.expressions.InitializerList
import de.fraunhofer.aisec.cpg.graph.expressions.KeyValue
import de.fraunhofer.aisec.cpg.graph.expressions.Lambda
import de.fraunhofer.aisec.cpg.graph.expressions.Literal
import de.fraunhofer.aisec.cpg.graph.expressions.MemberAccess
import de.fraunhofer.aisec.cpg.graph.expressions.MemberCall
import de.fraunhofer.aisec.cpg.graph.expressions.New
import de.fraunhofer.aisec.cpg.graph.expressions.OperatorCall
import de.fraunhofer.aisec.cpg.graph.expressions.ProblemExpression
import de.fraunhofer.aisec.cpg.graph.expressions.Range
import de.fraunhofer.aisec.cpg.graph.expressions.Reference
import de.fraunhofer.aisec.cpg.graph.expressions.ShortCircuitOperator
import de.fraunhofer.aisec.cpg.graph.expressions.Subscription
import de.fraunhofer.aisec.cpg.graph.expressions.Throw
import de.fraunhofer.aisec.cpg.graph.expressions.TypeExpression
import de.fraunhofer.aisec.cpg.graph.expressions.TypeReference
import de.fraunhofer.aisec.cpg.graph.expressions.UnaryOperator
import de.fraunhofer.aisec.cpg.graph.types.ProblemType
import de.fraunhofer.aisec.cpg.graph.types.Type

/**
 * Creates a new [de.fraunhofer.aisec.cpg.graph.expressions.Literal]. This is the top-most [Node] that a [LanguageFrontend] or [Handler]
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
 * Creates a new [de.fraunhofer.aisec.cpg.graph.expressions.BinaryOperator] or a [de.fraunhofer.aisec.cpg.graph.expressions.ShortCircuitOperator] if the language implements
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
 * Creates a new [de.fraunhofer.aisec.cpg.graph.expressions.UnaryOperator]. This is the top-most [Node] that a [LanguageFrontend] or [Handler]
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
 * Creates a new [de.fraunhofer.aisec.cpg.graph.expressions.Assign]. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newAssign(
    operatorCode: String = "=",
    lhs: List<Expression> = listOf(),
    rhs: List<Expression> = listOf(),
    rawNode: Any? = null,
): Assign {
    val node = Assign()
    node.applyMetadata(this, operatorCode, rawNode, true)
    node.operatorCode = operatorCode
    node.lhs = lhs.toMutableList()
    node.rhs = rhs.toMutableList()

    log(node)

    return node
}

/**
 * Creates a new [de.fraunhofer.aisec.cpg.graph.expressions.New]. This is the top-most [Node] that a [LanguageFrontend] or [Handler] should
 * create. The [MetadataProvider] receiver will be used to fill different meta-data using
 * [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an appropriate
 * [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newNew(type: Type = unknownType(), rawNode: Any? = null): New {
    val node = New()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    node.type = type

    log(node)
    return node
}

/**
 * Creates a new [de.fraunhofer.aisec.cpg.graph.expressions.Construction]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newConstruction(
    name: CharSequence? = EMPTY_NAME,
    rawNode: Any? = null,
): Construction {
    val node = Construction()
    node.applyMetadata(this, name, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [de.fraunhofer.aisec.cpg.graph.expressions.Conditional]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newConditional(
    condition: Expression,
    thenExpression: Expression? = null,
    elseExpression: Expression? = null,
    type: Type = unknownType(),
    rawNode: Any? = null,
): Conditional {
    val node = Conditional()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    node.type = type
    node.condition = condition
    node.thenExpression = thenExpression
    node.elseExpression = elseExpression

    log(node)
    return node
}

/**
 * Creates a new [de.fraunhofer.aisec.cpg.graph.expressions.KeyValue]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newKeyValue(
    key: Expression,
    value: Expression,
    rawNode: Any? = null,
): KeyValue {
    val node = KeyValue()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    node.key = key
    node.value = value

    log(node)
    return node
}

/**
 * Creates a new [de.fraunhofer.aisec.cpg.graph.expressions.Lambda]. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newLambda(rawNode: Any? = null): Lambda {
    val node = Lambda()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [de.fraunhofer.aisec.cpg.graph.expressions.Block]. The [MetadataProvider] receiver will be used to fill different meta-data
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
 * Creates a new [de.fraunhofer.aisec.cpg.graph.expressions.Call]. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newCall(
    callee: Expression? = null,
    fqn: CharSequence? = null,
    template: Boolean = false,
    rawNode: Any? = null,
): Call {
    val node = Call()
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
 * Creates a new [de.fraunhofer.aisec.cpg.graph.expressions.MemberCall]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newOperatorCall(
    operatorCode: String,
    callee: Expression?,
    rawNode: Any? = null,
): OperatorCall {
    val node = OperatorCall()
    node.applyMetadata(this, operatorCode, rawNode)

    node.operatorCode = operatorCode
    if (callee != null) {
        node.callee = callee
    }

    log(node)
    return node
}

/**
 * Creates a new [de.fraunhofer.aisec.cpg.graph.expressions.MemberCall]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newMemberCall(
    callee: Expression?,
    isStatic: Boolean = false,
    rawNode: Any? = null,
): MemberCall {
    val node = MemberCall()
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
 * Creates a new [de.fraunhofer.aisec.cpg.graph.expressions.MemberAccess]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newMemberAccess(
    name: CharSequence?,
    base: Expression,
    memberType: Type = unknownType(),
    operatorCode: String? = ".",
    rawNode: Any? = null,
): MemberAccess {
    val node = MemberAccess()
    node.applyMetadata(this, name, rawNode, true)

    node.base = base
    node.operatorCode = operatorCode
    node.type = memberType

    log(node)
    return node
}

/**
 * Creates a new [de.fraunhofer.aisec.cpg.graph.expressions.Cast]. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newCast(rawNode: Any? = null): Cast {
    val node = Cast()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [de.fraunhofer.aisec.cpg.graph.expressions.TypeReference]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newTypeReference(
    operatorCode: String,
    type: Type = unknownType(),
    referencedType: Type = unknownType(),
    rawNode: Any? = null,
): TypeReference {
    val node = TypeReference()
    node.applyMetadata(this, operatorCode, rawNode, true)

    node.operatorCode = operatorCode
    node.type = type
    node.referencedType = referencedType

    log(node)
    return node
}

/**
 * Creates a new [de.fraunhofer.aisec.cpg.graph.expressions.Subscription]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newSubscription(rawNode: Any? = null): Subscription {
    val node = Subscription()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [de.fraunhofer.aisec.cpg.graph.expressions.Range]. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newRange(
    floor: Expression? = null,
    ceiling: Expression? = null,
    rawNode: Any? = null,
): Range {
    val node = Range()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    node.floor = floor
    node.ceiling = ceiling

    log(node)
    return node
}

/**
 * Creates a new [de.fraunhofer.aisec.cpg.graph.expressions.ArrayConstruction]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newArrayConstruction(rawNode: Any? = null): ArrayConstruction {
    val node = ArrayConstruction()
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
 * Creates a new [de.fraunhofer.aisec.cpg.graph.expressions.Delete]. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newDelete(rawNode: Any? = null): Delete {
    val node = Delete()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [de.fraunhofer.aisec.cpg.graph.expressions.ExpressionList]. The [MetadataProvider] receiver will be used to fill different
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
 * Creates a new [de.fraunhofer.aisec.cpg.graph.expressions.InitializerList]. This is the top-most [Node] that a [LanguageFrontend] or
 * [Handler] should create. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newInitializerList(
    targetType: Type = unknownType(),
    rawNode: Any? = null,
): InitializerList {
    val node = InitializerList()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    node.type = targetType

    log(node)
    return node
}

@JvmOverloads
fun MetadataProvider.newComprehension(rawNode: Any? = null): Comprehension {
    val node = Comprehension()
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
 * Creates a new [de.fraunhofer.aisec.cpg.graph.expressions.TypeExpression]. The [MetadataProvider] receiver will be used to fill different
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
 * Creates a new [Throw]. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newThrow(rawNode: Any? = null): Throw {
    val node = Throw()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [de.fraunhofer.aisec.cpg.graph.expressions.ProblemExpression]. The [MetadataProvider] receiver will be used to fill different
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
