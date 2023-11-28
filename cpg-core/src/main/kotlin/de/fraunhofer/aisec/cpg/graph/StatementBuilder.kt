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

import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend
import de.fraunhofer.aisec.cpg.graph.Node.Companion.EMPTY_NAME
import de.fraunhofer.aisec.cpg.graph.NodeBuilder.log
import de.fraunhofer.aisec.cpg.graph.statements.*

/**
 * Creates a new [ReturnStatement]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun <T> RawNodeTypeProvider<T>.newReturnStatement(
    code: String? = null,
    rawNode: T? = null
): ReturnStatement {
    val node = ReturnStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [CatchClause]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun <T> RawNodeTypeProvider<T>.newCatchClause(
    code: String? = null,
    rawNode: T? = null
): CatchClause {
    val node = CatchClause()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [TryStatement]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun <T> RawNodeTypeProvider<T>.newTryStatement(
    code: String? = null,
    rawNode: T? = null
): TryStatement {
    val node = TryStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [AssertStatement]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun <T> RawNodeTypeProvider<T>.newAssertStatement(
    code: String? = null,
    rawNode: T? = null
): AssertStatement {
    val node = AssertStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [DistinctLanguageBlock]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun MetadataProvider.newDistinctLanguageBlock(
    code: String? = null,
    rawNode: Any? = null
): DistinctLanguageBlock {
    val node = DistinctLanguageBlock()
    node.applyMetadata(this, EMPTY_NAME, rawNode, code, true)

    log(node)
    return node
}

/**
 * Creates a new [SynchronizedStatement]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun <T> RawNodeTypeProvider<T>.newSynchronizedStatement(
    code: String? = null,
    rawNode: T? = null
): SynchronizedStatement {
    val node = SynchronizedStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [EmptyStatement]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun <T> RawNodeTypeProvider<T>.newEmptyStatement(
    code: String? = null,
    rawNode: T? = null
): EmptyStatement {
    val node = EmptyStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [DeclarationStatement]. The [MetadataProvider] receiver will be used to fill
 * different meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin
 * requires an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional
 * prepended argument.
 */
@JvmOverloads
fun <T> RawNodeTypeProvider<T>.newDeclarationStatement(
    code: String? = null,
    rawNode: T? = null
): DeclarationStatement {
    val node = DeclarationStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [IfStatement]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun <T> RawNodeTypeProvider<T>.newIfStatement(
    code: String? = null,
    rawNode: T? = null
): IfStatement {
    val node = IfStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [LabelStatement]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun <T> RawNodeTypeProvider<T>.newLabelStatement(
    code: String? = null,
    rawNode: T? = null
): LabelStatement {
    val node = LabelStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [GotoStatement]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun <T> RawNodeTypeProvider<T>.newGotoStatement(
    code: String? = null,
    rawNode: T? = null
): GotoStatement {
    val node = GotoStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [WhileStatement]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun <T> RawNodeTypeProvider<T>.newWhileStatement(
    code: String? = null,
    rawNode: T? = null
): WhileStatement {
    val node = WhileStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [DoStatement]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun <T> RawNodeTypeProvider<T>.newDoStatement(
    code: String? = null,
    rawNode: T? = null
): DoStatement {
    val node = DoStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [ForEachStatement]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun <T> RawNodeTypeProvider<T>.newForEachStatement(
    code: String? = null,
    rawNode: T? = null
): ForEachStatement {
    val node = ForEachStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [ForStatement]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun <T> RawNodeTypeProvider<T>.newForStatement(
    code: String? = null,
    rawNode: T? = null
): ForStatement {
    val node = ForStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [ContinueStatement]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun <T> RawNodeTypeProvider<T>.newContinueStatement(
    code: String? = null,
    rawNode: T? = null
): ContinueStatement {
    val node = ContinueStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [BreakStatement]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun <T> RawNodeTypeProvider<T>.newBreakStatement(
    code: String? = null,
    rawNode: T? = null
): BreakStatement {
    val node = BreakStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [SwitchStatement]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun <T> RawNodeTypeProvider<T>.newSwitchStatement(
    code: String? = null,
    rawNode: T? = null
): SwitchStatement {
    val node = SwitchStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [CaseStatement]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun <T> RawNodeTypeProvider<T>.newCaseStatement(
    code: String? = null,
    rawNode: T? = null
): CaseStatement {
    val node = CaseStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}

/**
 * Creates a new [DefaultStatement]. The [MetadataProvider] receiver will be used to fill different
 * meta-data using [Node.applyMetadata]. Calling this extension function outside of Kotlin requires
 * an appropriate [MetadataProvider], such as a [LanguageFrontend] as an additional prepended
 * argument.
 */
@JvmOverloads
fun <T> RawNodeTypeProvider<T>.newDefaultStatement(
    code: String? = null,
    rawNode: T? = null
): DefaultStatement {
    val node = DefaultStatement()
    node.applyMetadata(this, EMPTY_NAME, rawNode, true)

    log(node)
    return node
}
