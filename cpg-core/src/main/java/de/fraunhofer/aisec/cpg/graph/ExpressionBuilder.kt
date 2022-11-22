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

import de.fraunhofer.aisec.cpg.frontends.Handler
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.log
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.types.Type
import de.fraunhofer.aisec.cpg.graph.types.UnknownType

/**
 * Creates a new [Literal]. This is the top-most [Node] that a [LanguageFrontend] or [Handler]
 * should create. The [MetadataProvider] receiver will be used to fill different meta-data using
 * [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an appropriate
 * [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun <T> MetadataProvider.newLiteral(
    value: T,
    type: Type? = UnknownType.getUnknownType(),
    code: String? = null,
    rawNode: Any? = null,
): Literal<T> {
    val node = Literal<T>()
    node.applyMetadata(this, rawNode, code)

    node.value = value
    node.type = type

    log(node)
    return node
}

/**
 * Creates a new [BinaryOperator]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newBinaryOperator(
    operatorCode: String,
    code: String? = null,
    rawNode: Any? = null
): BinaryOperator {
    val node = BinaryOperator()
    node.applyMetadata(this, rawNode, code)

    node.name = operatorCode
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
    operatorType: String,
    postfix: Boolean,
    prefix: Boolean,
    code: String? = null,
    rawNode: Any? = null
): UnaryOperator {
    val node = UnaryOperator()
    node.applyMetadata(this, rawNode, code)

    node.operatorCode = operatorType
    node.name = operatorType
    node.isPostfix = postfix
    node.isPrefix = prefix

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
    code: String? = null,
    type: Type? = UnknownType.getUnknownType(),
    rawNode: Any? = null
): NewExpression {
    val node = NewExpression()
    node.applyMetadata(this, rawNode, code)

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
    code: String? = null,
    rawNode: Any? = null
): ConstructExpression {
    val node = ConstructExpression()
    node.applyMetadata(this, rawNode, code)

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
    condition: Expression?,
    thenExpr: Expression?,
    elseExpr: Expression?,
    type: Type?,
    code: String? = null,
    rawNode: Any? = null
): ConditionalExpression {
    val node = ConditionalExpression()
    node.applyMetadata(this, rawNode, code)

    node.condition = condition
    node.thenExpr = thenExpr
    node.elseExpr = elseExpr
    node.type = type

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
    key: Expression? = null,
    value: Expression? = null,
    code: String? = null,
    rawNode: Any? = null
): KeyValueExpression {
    val node = KeyValueExpression()
    node.applyMetadata(this, rawNode, code)

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
fun MetadataProvider.newLambdaExpression(
    code: String? = null,
    rawNode: Any? = null
): LambdaExpression {
    val node = LambdaExpression()
    node.applyMetadata(this, rawNode, code)

    log(node)
    return node
}

/**
 * Creates a new [CompoundStatementExpression]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newCompoundStatementExpression(
    code: String? = null,
    rawNode: Any? = null
): CompoundStatementExpression {
    val node = CompoundStatementExpression()
    node.applyMetadata(this, rawNode, code)

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
    fqn: String? = null,
    code: String? = null,
    template: Boolean = false,
    rawNode: Any? = null
): CallExpression {
    val node = CallExpression()
    node.applyMetadata(this, rawNode, code)

    node.callee = callee
    node.fqn = fqn
    node.template = template

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
fun MetadataProvider.newExplicitConstructorInvocation(
    containingClass: String?,
    code: String? = null,
    rawNode: Any? = null
): ExplicitConstructorInvocation {
    val node = ExplicitConstructorInvocation()
    node.applyMetadata(this, rawNode, code)

    node.containingClass = containingClass

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
fun MetadataProvider.newMemberCallExpression(
    name: String?,
    fqn: String?,
    base: Expression?,
    member: Node?,
    operatorCode: String? = ".",
    code: String? = null,
    rawNode: Any? = null
): MemberCallExpression {
    val node = MemberCallExpression()
    node.applyMetadata(this, rawNode, code)

    node.name = name ?: Node.EMPTY_NAME
    node.fqn = fqn
    node.base = base
    node.member = member
    node.operatorCode = operatorCode

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
    name: String?,
    base: Expression,
    memberType: Type? = UnknownType.getUnknownType(),
    operatorCode: String? = ".",
    code: String? = null,
    rawNode: Any? = null
): MemberExpression {
    val node = MemberExpression()
    node.applyMetadata(this, rawNode, code)

    node.name = name ?: Node.EMPTY_NAME
    node.setBase(base)
    node.operatorCode = operatorCode
    node.type = memberType

    log(node)
    return node
}

/**
 * Creates a new [StaticCallExpression]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newStaticCallExpression(
    name: String?,
    fqn: String?,
    code: String? = null,
    targetRecord: String?,
    rawNode: Any? = null
): StaticCallExpression {
    val node = StaticCallExpression()
    node.applyMetadata(this, rawNode, code)

    node.name = name ?: Node.EMPTY_NAME
    node.fqn = fqn
    node.targetRecord = targetRecord

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
fun MetadataProvider.newCastExpression(code: String? = null, rawNode: Any? = null): CastExpression {
    val node = CastExpression()
    node.applyMetadata(this, rawNode, code)

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
    type: Type?,
    referencedType: Type?,
    code: String? = null,
    rawNode: Any? = null
): TypeIdExpression {
    val node = TypeIdExpression()
    node.applyMetadata(this, rawNode, code)

    node.name = operatorCode
    node.operatorCode = operatorCode
    node.type = type
    node.referencedType = referencedType

    log(node)
    return node
}

/**
 * Creates a new [ArraySubscriptionExpression]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newArraySubscriptionExpression(
    code: String? = null,
    rawNode: Any? = null
): ArraySubscriptionExpression {
    val node = ArraySubscriptionExpression()
    node.applyMetadata(this, rawNode, code)

    log(node)
    return node
}

/**
 * Creates a new [ArrayCreationExpression]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newArrayCreationExpression(
    code: String? = null,
    rawNode: Any? = null
): ArrayCreationExpression {
    val node = ArrayCreationExpression()
    node.applyMetadata(this, rawNode, code)

    log(node)
    return node
}

/**
 * Creates a new [DeclaredReferenceExpression]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newDeclaredReferenceExpression(
    name: String?,
    type: Type? = UnknownType.getUnknownType(),
    code: String? = null,
    rawNode: Any? = null
): DeclaredReferenceExpression {
    val node = DeclaredReferenceExpression()
    node.applyMetadata(this, rawNode, code)

    node.name = name ?: Node.EMPTY_NAME
    node.type = type

    log(node)
    return node
}

/**
 * Creates a new [ArrayRangeExpression]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newArrayRangeExpression(
    floor: Expression?,
    ceil: Expression?,
    code: String? = null,
    rawNode: Any? = null
): ArrayRangeExpression {
    val node = ArrayRangeExpression()
    node.applyMetadata(this, rawNode, code)

    node.floor = floor
    node.ceiling = ceil

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
fun MetadataProvider.newDeleteExpression(
    code: String? = null,
    rawNode: Any? = null
): DeleteExpression {
    val node = DeleteExpression()
    node.applyMetadata(this, rawNode, code)

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
fun MetadataProvider.newExpressionList(code: String? = null, rawNode: Any? = null): ExpressionList {
    val node = ExpressionList()
    node.applyMetadata(this, rawNode, code)

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
    code: String? = null,
    rawNode: Any? = null
): InitializerListExpression {
    val node = InitializerListExpression()
    node.applyMetadata(this, rawNode, code)

    log(node)
    return node
}

/**
 * Creates a new [DesignatedInitializerExpression]. The [MetadataProvider] receiver will be used to
 * fill different meta-data using [Node.applyMetadata]. Calling this extension function outside of
 * Kotlin requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newDesignatedInitializerExpression(
    code: String? = null,
    rawNode: Any? = null
): DesignatedInitializerExpression {
    val node = DesignatedInitializerExpression()
    node.applyMetadata(this, rawNode, code)

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
    name: String?,
    type: Type?,
    rawNode: Any? = null
): TypeExpression {
    val node = TypeExpression()
    node.applyMetadata(this, rawNode, null)

    node.name = name ?: Node.EMPTY_NAME
    node.type = type

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
    code: String? = null,
    rawNode: Any? = null
): ProblemExpression {
    val node = ProblemExpression(problem, type)
    node.applyMetadata(this, rawNode, code)

    log(node)
    return node
}

fun <T> Literal<T>.duplicate(implicit: Boolean): Literal<T> {
    val duplicate = Literal<T>()
    duplicate.language = this.language
    duplicate.value = this.value
    duplicate.type = this.type
    duplicate.code = this.code
    duplicate.location = this.location
    duplicate.locals = this.locals
    duplicate.possibleSubTypes = this.possibleSubTypes
    duplicate.argumentIndex = this.argumentIndex
    duplicate.annotations = this.annotations
    duplicate.comment = this.comment
    duplicate.file = this.file
    duplicate.name = this.name
    duplicate.nextDFG = this.nextDFG
    duplicate.prevDFG = this.prevDFG
    duplicate.nextEOG = this.nextEOG
    duplicate.prevEOG = this.prevEOG
    duplicate.isImplicit = implicit
    return duplicate
}

fun TypeExpression.duplicate(implicit: Boolean): TypeExpression {
    val duplicate = TypeExpression()
    duplicate.name = this.name
    duplicate.language = this.language
    duplicate.type = this.type
    duplicate.isImplicit = implicit
    return duplicate
}
