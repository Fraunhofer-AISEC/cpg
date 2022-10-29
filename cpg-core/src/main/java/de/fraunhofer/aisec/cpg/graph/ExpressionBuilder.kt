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
import de.fraunhofer.aisec.cpg.frontends.MetadataProvider
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.log
import de.fraunhofer.aisec.cpg.graph.statements.expressions.*
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
    type: Type?,
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
 * Creates a new [BinaryOperator]. This is the top-most [Node] that a [LanguageFrontend] or
 * [Handler] should create. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
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
    type: Type?,
    rawNode: Any? = null
): NewExpression {
    val node = NewExpression()
    node.applyMetadata(this, rawNode, code)

    node.type = type

    log(node)
    return node
}

/**
 * Creates a new [ConditionalExpression]. This is the top-most [Node] that a [LanguageFrontend] or
 * [Handler] should create. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
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
 * Creates a new [KeyValueExpression]. This is the top-most [Node] that a [LanguageFrontend] or
 * [Handler] should create. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newKeyValueExpression(
    key: Expression?,
    value: Expression?,
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
 * Creates a new [LambdaExpression]. This is the top-most [Node] that a [LanguageFrontend] or
 * [Handler] should create. The [MetadataProvider] receiver will be used to fill different meta-data
 * using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires an
 * appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended argument.
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
    duplicate.type = this.type
    duplicate.language = this.language
    duplicate.isImplicit = implicit
    return duplicate
}
