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
import de.fraunhofer.aisec.cpg.frontends.HasShortCircuitOperators
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.graph.Node.Companion.EMPTY_NAME
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.log
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
import de.fraunhofer.aisec.cpg.graph.statements.expressions.AssignExpr
import de.fraunhofer.aisec.cpg.graph.types.ProblemType
import de.fraunhofer.aisec.cpg.graph.types.Type

/**
 * Creates a new [Literal]. This is the top-most [Node] that a [LanguageFrontend] or [Handler]
 * should create. The [MetadataProvider] receiver will be used to fill different meta-data using
 * [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an appropriate
 * [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun <T> MetadataProvider.newLiteral(
    value: T,
    type: Type = unknownType(),
    code: String? = null,
    rawNode: Any? = null,
): Literal<T> {
    val node = Literal<T>()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

    node.value = value
    node.type = type

    log(node)
    return node
}

/**
 * Creates a new [BinaryOp] or a [ShortCircuitOp] if the language implements
 * [HasShortCircuitOperators] and if the [operatorCode] is contained in
 * [HasShortCircuitOperators.operatorCodes]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newBinaryOp(
    operatorCode: String,
    code: String? = null,
    rawNode: Any? = null
): BinaryOp {
    val node =
        if (
            this is LanguageProvider &&
                (this.language as? HasShortCircuitOperators)
                    ?.operatorCodes
                    ?.contains(operatorCode) == true
        ) {
            ShortCircuitOp()
        } else {
            BinaryOp()
        }
    node.applyMetadata(this, operatorCode, rawNode, code, true)

    node.operatorCode = operatorCode

    log(node)

    return node
}

/**
 * Creates a new [UnaryOp]. This is the top-most [Node] that a [LanguageFrontend] or [Handler]
 * should create. The [MetadataProvider] receiver will be used to fill different meta-data using
 * [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an appropriate
 * [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newUnaryOp(
    operatorCode: String,
    postfix: Boolean,
    prefix: Boolean,
    code: String? = null,
    rawNode: Any? = null
): UnaryOp {
    val node = UnaryOp()
    node.applyMetadata(this, operatorCode, rawNode, code, true)

    node.operatorCode = operatorCode
    node.isPostfix = postfix
    node.isPrefix = prefix

    log(node)

    return node
}

/**
 * Creates a new [AssignExpr]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newAssignExpr(
    operatorCode: String = "=",
    lhs: List<Expression> = listOf(),
    rhs: List<Expression> = listOf(),
    code: String? = null,
    rawNode: Any? = null
): AssignExpr {
    val node = AssignExpr()
    node.applyMetadata(this, operatorCode, rawNode, code, true)
    node.operatorCode = operatorCode
    node.lhs = lhs
    node.rhs = rhs

    log(node)

    return node
}

/**
 * Creates a new [NewExpr]. This is the top-most [Node] that a [LanguageFrontend] or [Handler]
 * should create. The [MetadataProvider] receiver will be used to fill different meta-data using
 * [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an appropriate
 * [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newNewExpr(
    code: String? = null,
    type: Type = unknownType(),
    rawNode: Any? = null
): NewExpr {
    val node = NewExpr()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

    node.type = type

    log(node)
    return node
}

/**
 * Creates a new [ConstructExpr]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newConstructExpr(
    name: CharSequence? = EMPTY_NAME,
    code: String? = null,
    rawNode: Any? = null
): ConstructExpr {
    val node = ConstructExpr()
    node.applyMetadata(this, name, rawNode, code, true)

    log(node)
    return node
}

/**
 * Creates a new [ConditionalExpr]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newConditionalExpr(
    condition: Expression,
    thenExpr: Expression?,
    elseExpr: Expression?,
    type: Type = unknownType(),
    code: String? = null,
    rawNode: Any? = null
): ConditionalExpr {
    val node = ConditionalExpr()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

    node.type = type
    node.condition = condition
    node.thenExpr = thenExpr
    node.elseExpr = elseExpr

    log(node)
    return node
}

/**
 * Creates a new [KeyValueExpr]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newKeyValueExpr(
    key: Expression? = null,
    value: Expression? = null,
    code: String? = null,
    rawNode: Any? = null
): KeyValueExpr {
    val node = KeyValueExpr()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

    node.key = key
    node.value = value

    log(node)
    return node
}

/**
 * Creates a new [LambdaExpr]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newLambdaExpr(code: String? = null, rawNode: Any? = null): LambdaExpr {
    val node = LambdaExpr()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

    log(node)
    return node
}

/**
 * Creates a new [CompoundStmtExpr]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newCompoundStatementExpr(
    code: String? = null,
    rawNode: Any? = null
): CompoundStmtExpr {
    val node = CompoundStmtExpr()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

    log(node)
    return node
}

/**
 * Creates a new [CallExpr]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newCallExpr(
    callee: Expression? = null,
    fqn: CharSequence? = null,
    code: String? = null,
    template: Boolean = false,
    rawNode: Any? = null
): CallExpr {
    val node = CallExpr()
    node.applyMetadata(this, fqn, rawNode, code, true)

    node.callee = callee
    node.template = template

    log(node)
    return node
}

/**
 * Creates a new [CallExpr]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newConstructorCallExpr(
    containingClass: String?,
    code: String? = null,
    rawNode: Any? = null
): ConstructorCallExpr {
    val node = ConstructorCallExpr()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

    node.containingClass = containingClass

    log(node)
    return node
}

/**
 * Creates a new [CallExpr]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newMemberCallExpr(
    callee: Expression?,
    isStatic: Boolean = false,
    code: String? = null,
    rawNode: Any? = null
): MemberCallExpr {
    val node = MemberCallExpr()
    node.applyMetadata(
        this,
        null, // the name will be updated later based on the callee
        rawNode,
        code,
    )

    node.callee = callee
    node.isStatic = isStatic

    log(node)
    return node
}

/**
 * Creates a new [MemberExpr]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newMemberExpr(
    name: CharSequence?,
    base: Expression,
    memberType: Type = unknownType(),
    operatorCode: String? = ".",
    code: String? = null,
    rawNode: Any? = null
): MemberExpr {
    val node = MemberExpr()
    node.applyMetadata(this, name, rawNode, code, true)

    node.base = base
    node.operatorCode = operatorCode
    node.type = memberType

    log(node)
    return node
}

/**
 * Creates a new [CastExpr]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newCastExpr(code: String? = null, rawNode: Any? = null): CastExpr {
    val node = CastExpr()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

    log(node)
    return node
}

/**
 * Creates a new [TypeIdExpr]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newTypeIdExpr(
    operatorCode: String,
    type: Type = unknownType(),
    referencedType: Type = unknownType(),
    code: String? = null,
    rawNode: Any? = null
): TypeIdExpr {
    val node = TypeIdExpr()
    node.applyMetadata(this, operatorCode, rawNode, code, true)

    node.operatorCode = operatorCode
    node.type = type
    node.referencedType = referencedType

    log(node)
    return node
}

/**
 * Creates a new [SubscriptionExpr]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newSubscriptionExpr(
    code: String? = null,
    rawNode: Any? = null
): SubscriptionExpr {
    val node = SubscriptionExpr()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

    log(node)
    return node
}

/**
 * Creates a new [RangeExpr]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newRangeExpr(
    floor: Expression? = null,
    ceiling: Expression? = null,
    code: String? = null,
    rawNode: Any? = null
): RangeExpr {
    val node = RangeExpr()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

    node.floor = floor
    node.ceiling = ceiling

    log(node)
    return node
}

/**
 * Creates a new [ArrayExpr]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newArrayExpr(code: String? = null, rawNode: Any? = null): ArrayExpr {
    val node = ArrayExpr()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

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
    code: String? = null,
    rawNode: Any? = null
): Reference {
    val node = Reference()
    node.applyMetadata(this, name, rawNode, code, true)

    node.type = type

    log(node)
    return node
}

/**
 * Creates a new [DeleteExpr]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newDeleteExpr(code: String? = null, rawNode: Any? = null): DeleteExpr {
    val node = DeleteExpr()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

    log(node)
    return node
}

/**
 * Creates a new [ExprList]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newExprList(code: String? = null, rawNode: Any? = null): ExprList {
    val node = ExprList()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

    log(node)
    return node
}

/**
 * Creates a new [InitializerListExpr]. This is the top-most [Node] that a [LanguageFrontend] or
 * [Handler] should create. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newInitializerListExpr(
    targetType: Type = unknownType(),
    code: String? = null,
    rawNode: Any? = null
): InitializerListExpr {
    val node = InitializerListExpr()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

    node.type = targetType

    log(node)
    return node
}

/**
 * Creates a new [InitializerExpr]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newInitializerExpr(
    code: String? = null,
    rawNode: Any? = null
): InitializerExpr {
    val node = InitializerExpr()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

    log(node)
    return node
}

/**
 * Creates a new [TypeExpr]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newTypeExpr(
    name: CharSequence?,
    type: Type = unknownType(),
    rawNode: Any? = null
): TypeExpr {
    val node = TypeExpr()
    node.applyMetadata(this, name, rawNode, null)

    node.type = type

    log(node)
    return node
}

/**
 * Creates a new [ProblemExpr]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun MetadataProvider.newProblemExpr(
    problem: String = "",
    type: ProblemNode.ProblemType = ProblemNode.ProblemType.PARSING,
    code: String? = null,
    rawNode: Any? = null
): ProblemExpr {
    val node = ProblemExpr(problem, type)
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

    log(node)
    return node
}

fun MetadataProvider.newProblemType(code: String? = null, rawNode: Any? = null): ProblemType {
    val node = ProblemType()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

    log(node)
    return node
}

fun <T> Literal<T>.duplicate(implicit: Boolean): Literal<T> {
    val duplicate = Literal<T>()
    duplicate.ctx = this.ctx
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
    duplicate.nextDFG = this.nextDFG
    duplicate.prevDFG = this.prevDFG
    duplicate.nextEOG = this.nextEOG
    duplicate.prevEOG = this.prevEOG
    duplicate.isImplicit = implicit
    return duplicate
}

fun TypeExpr.duplicate(implicit: Boolean): TypeExpr {
    val duplicate = TypeExpr()
    duplicate.ctx = this.ctx
    duplicate.name = this.name.clone()
    duplicate.language = this.language
    duplicate.type = this.type
    duplicate.isImplicit = implicit
    return duplicate
}
